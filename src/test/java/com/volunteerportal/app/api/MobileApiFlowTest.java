package com.volunteerportal.app.api;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.NotificationRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.service.CheckInCodes;
import com.volunteerportal.app.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The mobile API used the way the app will: log in for a token, then call the endpoints with
 * "Authorization: Bearer ..." - against the real database, security chain and services.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MobileApiFlowTest {

    private static final String PASSWORD = "api-pass-123";

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
    private NotificationRepository notificationRepository;

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CheckInCodes checkInCodes;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String suffix = String.valueOf(System.nanoTime());
    private User volunteer;
    private Initiative memberOf;
    private Initiative pendingIn;
    private Event memberEvent;
    private Event pendingEvent;
    private VolunteerInitiative approved;
    private VolunteerInitiative pending;

    @BeforeEach
    void setUp() {
        volunteer = new User();
        volunteer.setUsername("api_vol_" + suffix);
        volunteer.setEmail(volunteer.getUsername() + "@example.com");
        volunteer.setPassword(passwordEncoder.encode(PASSWORD));
        volunteer.setEnabled(true);
        volunteer.setRoles(new HashSet<>(Set.<Role>of(roleRepository.findByName("VOLUNTEER").orElseThrow())));
        volunteer = userRepository.save(volunteer);

        memberOf = initiative("API Member Initiative " + suffix);
        pendingIn = initiative("API Pending Initiative " + suffix);
        memberEvent = event(memberOf, "API Member Event " + suffix);
        pendingEvent = event(pendingIn, "API Pending Event " + suffix);
        approved = membership(memberOf, true);
        pending = membership(pendingIn, false);
    }

    @AfterEach
    void cleanUp() {
        attendRepository.deleteAll(attendRepository.findByEventId(memberEvent.getId()));
        volunteerInitiativeRepository.deleteAll(List.of(approved, pending));
        eventRepository.deleteAll(List.of(memberEvent, pendingEvent));
        initiativeRepository.deleteAll(List.of(memberOf, pendingIn));
        notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedDttmDesc(volunteer.getId()));
        apiTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(volunteer.getId()))
                .forEach(apiTokenRepository::delete);
        userRepository.delete(volunteer);
    }

    @Test
    void login_rightPassword_givesATokenThatOpensTheApi() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + volunteer.getUsername() + "\",\"password\":\"" + PASSWORD
                                + "\",\"deviceName\":\"Test phone\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(jsonPath("$.user.username").value(volunteer.getUsername()))
                .andExpect(jsonPath("$.user.roles[0]").value("VOLUNTEER"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");

        api(get("/api/me"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(volunteer.getUsername()))
                .andExpect(jsonPath("$.points").value(0));

        assertThat(apiTokenRepository.countByUserId(volunteer.getId())).isEqualTo(1);
    }

    @Test
    void login_wrongPassword_is401Json() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + volunteer.getUsername() + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void withoutValidToken_everyEndpointIs401Json_notALoginRedirect() throws Exception {
        for (String path : List.of("/api/me", "/api/events", "/api/initiatives", "/api/attendance", "/api/notifications")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("unauthorized"));
            api(get(path), "vp_not-a-real-token").andExpect(status().isUnauthorized());
        }
    }

    @Test
    void websiteSessionCookie_doesNotAuthenticateTheApi() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", volunteer.getUsername()).param("password", PASSWORD))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/home").session(session)).andExpect(status().isOk()); // logged in on the website

        mockMvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTheToken() throws Exception {
        String token = login();
        api(get("/api/me"), token).andExpect(status().isOk());

        api(post("/api/auth/logout"), token).andExpect(status().isNoContent());

        api(get("/api/me"), token).andExpect(status().isUnauthorized());
        assertThat(apiTokenRepository.countByUserId(volunteer.getId())).isZero();
    }

    @Test
    void initiatives_showMyMembershipStatus() throws Exception {
        String token = login();

        String json = api(get("/api/initiatives"), token).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<String>>read(json, "$[?(@.id == " + memberOf.getId() + ")].membership")).containsExactly("APPROVED");
        assertThat(JsonPath.<List<String>>read(json, "$[?(@.id == " + pendingIn.getId() + ")].membership")).containsExactly("PENDING");
    }

    @Test
    void scanToCheckIn_thenOut_withEventsAndHistoryFollowing() throws Exception {
        String token = login();
        String qr = "https://portal.example.org/checkin/" + memberEvent.getId() + "?code=" + checkInCodes.currentCode(memberEvent.getId());

        // Only events of initiatives I'm an approved member of
        api(get("/api/events"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(memberEvent.getId().intValue())))
                .andExpect(jsonPath("$[*].id", not(hasItem(pendingEvent.getId().intValue()))))
                .andExpect(jsonPath("$[?(@.id == " + memberEvent.getId() + ")].myStatus").value("NOT_CHECKED_IN"))
                .andExpect(jsonPath("$[?(@.id == " + memberEvent.getId() + ")].requiresLocation").value(false));

        checkIn(token, qr, "en")
                .andExpect(jsonPath("$.result").value("CHECKED_IN"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.eventId").value(memberEvent.getId().intValue()))
                .andExpect(jsonPath("$.message").value("You are checked in. Welcome!"));

        api(get("/api/events"), token)
                .andExpect(jsonPath("$[?(@.id == " + memberEvent.getId() + ")].myStatus").value("CHECKED_IN"));

        checkIn(token, qr, "ar-SA,ar;q=0.9")
                .andExpect(jsonPath("$.result").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.message").value("تم تسجيل انصرافك. شكراً لتطوعك!"));

        checkIn(token, qr, "en").andExpect(jsonPath("$.result").value("ALREADY_DONE")).andExpect(jsonPath("$.success").value(false));

        api(get("/api/attendance"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].type", hasItem("CHECK_IN")))
                .andExpect(jsonPath("$[*].type", hasItem("CHECK_OUT")))
                .andExpect(jsonPath("$[0].event").value(memberEvent.getName()));
    }

    @Test
    void checkIn_toAnEventImNotAMemberOf_isRefused() throws Exception {
        String token = login();
        String qr = "https://x/checkin/" + pendingEvent.getId() + "?code=" + checkInCodes.currentCode(pendingEvent.getId());

        checkIn(token, qr, "en").andExpect(jsonPath("$.result").value("NOT_MEMBER"));
        assertThat(attendRepository.findByEventId(pendingEvent.getId())).isEmpty();
    }

    @Test
    void checkIn_withSomethingThatIsNotACheckInQr_is400_andUnknownEventIs404() throws Exception {
        String token = login();

        checkInRaw(token, "{\"qr\":\"https://example.com/menu\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_qr"));
        checkInRaw(token, "not json").andExpect(status().isBadRequest());
        checkInRaw(token, "{\"qr\":\"https://x/checkin/999999999?code=1-a\"}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void notifications_areInThePhonesLanguage_andCanBeMarkedRead() throws Exception {
        notificationService.notify(volunteer, "notification.joinApproved", "/initiatives/" + memberOf.getId(), "Beach Cleanup");
        String token = login();

        api(get("/api/notifications").header("Accept-Language", "ar"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("تم قبول طلب انضمامك إلى «Beach Cleanup»."))
                .andExpect(jsonPath("$[0].read").value(false));
        String json = api(get("/api/notifications"), token)
                .andExpect(jsonPath("$[0].message").value("Your request to join 'Beach Cleanup' was approved."))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(json, "$[0].id");

        api(post("/api/notifications/{id}/read", id), token).andExpect(status().isNoContent());
        api(get("/api/notifications"), token).andExpect(jsonPath("$[0].read").value(true));
        api(post("/api/notifications/read-all"), token).andExpect(status().isNoContent());
    }

    @Test
    void theWebsiteStillUsesItsOwnLogin() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mockMvc.perform(get("/home")).andExpect(status().is3xxRedirection());
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + volunteer.getUsername() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private ResultActions api(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private ResultActions checkIn(String token, String qr, String language) throws Exception {
        return api(post("/api/checkin").contentType(MediaType.APPLICATION_JSON).header("Accept-Language", language)
                .content("{\"qr\":\"" + qr + "\"}"), token).andExpect(status().isOk());
    }

    private ResultActions checkInRaw(String token, String body) throws Exception {
        return api(post("/api/checkin").contentType(MediaType.APPLICATION_JSON).content(body), token);
    }

    private Initiative initiative(String name) {
        Initiative initiative = new Initiative();
        initiative.setName(name);
        initiative.setEnabled(true);
        return initiativeRepository.save(initiative);
    }

    private Event event(Initiative initiative, String name) {
        Event event = new Event();
        event.setName(name);
        event.setInitiative(initiative);
        event.setEnabled(true);
        event.setFromDttm(LocalDateTime.now().minusHours(1));
        event.setToDttm(LocalDateTime.now().plusHours(3));
        return eventRepository.save(event);
    }

    private VolunteerInitiative membership(Initiative initiative, boolean isApproved) {
        VolunteerInitiative vi = new VolunteerInitiative();
        vi.setUser(volunteer);
        vi.setInitiative(initiative);
        vi.setRequestJoinDttm(LocalDateTime.now());
        if (isApproved) {
            vi.setEnabled(true);
            vi.setResponseJoinDttm(LocalDateTime.now());
        }
        return volunteerInitiativeRepository.save(vi);
    }
}
