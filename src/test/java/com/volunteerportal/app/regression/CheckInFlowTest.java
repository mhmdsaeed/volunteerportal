package com.volunteerportal.app.regression;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.CheckInCodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QR self check-in end to end against the real database and templates: the coordinator's QR
 * image points at /checkin, a volunteer who scans it logs in and comes back, checks in and out,
 * and every refusal case is covered.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckInFlowTest {

    private static final double LAT = 24.7136;
    private static final double LON = 46.6753;
    private static final String PASSWORD = "secret-pass-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InitiativeRepository initiativeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Autowired
    private AttendRepository attendRepository;

    @Autowired
    private CheckInCodes checkInCodes;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String suffix = String.valueOf(System.nanoTime());
    private User member;
    private User pending;
    private User supervisor;
    private Initiative initiative;
    private Event event;
    private VolunteerInitiative approvedMembership;
    private VolunteerInitiative pendingMembership;

    @BeforeEach
    void setUp() {
        member = persistUser("ci_member_" + suffix, "VOLUNTEER");
        pending = persistUser("ci_pending_" + suffix, "VOLUNTEER");
        supervisor = persistUser("ci_super_" + suffix, "COORDINATOR");

        initiative = new Initiative();
        initiative.setName("Check-in Initiative " + suffix);
        initiative.setEnabled(true);
        initiative.setSupervisor(supervisor);
        initiative = initiativeRepository.save(initiative);

        event = new Event();
        event.setName("Check-in Event " + suffix);
        event.setInitiative(initiative);
        event.setEnabled(true);
        event.setLocLatitude(LAT);
        event.setLocLongitude(LON);
        event = eventRepository.save(event);

        approvedMembership = membership(member, true);
        pendingMembership = membership(pending, null);
    }

    @AfterEach
    void cleanUp() {
        attendRepository.deleteAll(attendRepository.findByEventId(event.getId()));
        volunteerInitiativeRepository.delete(approvedMembership);
        volunteerInitiativeRepository.delete(pendingMembership);
        eventRepository.delete(event);
        initiativeRepository.delete(initiative);
        for (User u : List.of(member, pending, supervisor)) {
            userRepository.delete(u);
        }
    }

    @Test
    void scannedLink_whileLoggedOut_logsInAndComesBackToTheCheckInPage() throws Exception {
        String link = "/checkin/" + event.getId() + "?code=" + checkInCodes.currentCode(event.getId());
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get(link).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        MvcResult login = mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", member.getUsername())
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(login.getResponse().getRedirectedUrl()).contains("/checkin/" + event.getId()).contains("code=");
    }

    @Test
    void member_checksIn_thenOut_thenIsDone() throws Exception {
        String code = checkInCodes.currentCode(event.getId());

        mockMvc.perform(get("/checkin/{id}", event.getId()).param("code", code).with(user(principal(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(event.getName())))
                .andExpect(content().string(containsString("Tap the button to check in")))
                .andExpect(content().string(containsString("data-needs-location=\"true\"")));

        checkIn(member, code, LAT + 0.0005, LON).andExpect(flash().attribute("result", "CHECKED_IN"));

        List<Attend> records = attendRepository.findByEventIdAndVolunteerInitiativeId(event.getId(), approvedMembership.getId());
        assertThat(records).extracting(Attend::getAttendInOut).containsExactly(1);
        assertThat(records.get(0).getNote()).isEqualTo("QR self check-in");

        mockMvc.perform(get("/checkin/{id}", event.getId()).param("code", code).with(user(principal(member))))
                .andExpect(content().string(containsString("Tap the button to check out")));
        checkIn(member, code, LAT, LON).andExpect(flash().attribute("result", "CHECKED_OUT"));
        checkIn(member, code, LAT, LON).andExpect(flash().attribute("result", "ALREADY_DONE"));

        assertThat(attendRepository.findByEventIdAndVolunteerInitiativeId(event.getId(), approvedMembership.getId()))
                .extracting(Attend::getAttendInOut).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void resultPage_isShownInTheVolunteersLanguage() throws Exception {
        String code = checkInCodes.currentCode(event.getId());
        MvcResult posted = checkIn(member, code, LAT, LON).andReturn();

        mockMvc.perform(get("/checkin/{id}/done", event.getId()).param("lang", "ar")
                        .flashAttrs(posted.getFlashMap()).with(user(principal(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("تم تسجيل حضورك. أهلاً بك!")));
    }

    @Test
    void refusals_recordNothing() throws Exception {
        String code = checkInCodes.currentCode(event.getId());

        checkIn(member, "123-forged", LAT, LON).andExpect(flash().attribute("result", "INVALID_CODE"));
        checkIn(member, code, null, null).andExpect(flash().attribute("result", "LOCATION_REQUIRED"));
        checkIn(member, code, LAT + 0.02, LON).andExpect(flash().attribute("result", "TOO_FAR"));
        checkIn(pending, code, LAT, LON).andExpect(flash().attribute("result", "NOT_MEMBER"));

        event.setEnabled(false);
        eventRepository.save(event);
        checkIn(member, code, LAT, LON).andExpect(flash().attribute("result", "EVENT_CLOSED"));

        assertThat(attendRepository.findByEventId(event.getId())).isEmpty();
    }

    @Test
    void coordinator_seesAQrPointingAtTheCheckInLink_andLiveCounts() throws Exception {
        String base = "/coordinator/initiatives/" + initiative.getId() + "/events/" + event.getId() + "/checkin";

        mockMvc.perform(get(base).with(user(principal(supervisor))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(base + "/qr.svg")));

        mockMvc.perform(get(base + "/qr.svg").with(user(principal(supervisor))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(startsWith("<svg")));

        checkIn(member, checkInCodes.currentCode(event.getId()), LAT, LON);
        mockMvc.perform(get(base + "/counts").with(user(principal(supervisor))))
                .andExpect(jsonPath("$.checkedIn").value(1))
                .andExpect(jsonPath("$.checkedOut").value(0));

        // A volunteer (or another coordinator) can't open the QR page
        mockMvc.perform(get(base).with(user(principal(member))))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions checkIn(User who, String code, Double lat, Double lon)
            throws Exception {
        var request = post("/checkin/{id}", event.getId()).with(user(principal(who))).with(csrf()).param("code", code);
        if (lat != null) {
            request.param("latitude", lat.toString()).param("longitude", lon.toString());
        }
        return mockMvc.perform(request).andExpect(redirectedUrl("/checkin/" + event.getId() + "/done"));
    }

    private VolunteerInitiative membership(User user, Boolean approved) {
        VolunteerInitiative vi = new VolunteerInitiative();
        vi.setUser(user);
        vi.setInitiative(initiative);
        vi.setRequestJoinDttm(LocalDateTime.now());
        vi.setEnabled(approved);
        if (approved != null) {
            vi.setResponseJoinDttm(LocalDateTime.now());
        }
        return volunteerInitiativeRepository.save(vi);
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(userRepository.findByUsername(user.getUsername()).orElseThrow());
    }

    private User persistUser(String username, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.save(user);
    }
}
