package com.volunteerportal.app.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.config.SecurityConfig;
import com.volunteerportal.app.dto.MemberCounts;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.MembershipFilter;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.VolunteerInitiativeService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(VolunteerInitiativeController.class)
@Import(SecurityConfig.class)
class VolunteerInitiativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VolunteerInitiativeService volunteerInitiativeService;

    // GlobalModelAttributes (a @ControllerAdvice picked up by the web slice) depends on this.
    @MockitoBean
    private NotificationService notificationService;

    private final Initiative beach = initiative(1L, "Beach clean-up");
    private final Initiative foodBank = initiative(2L, "Food bank");
    private final Initiative library = initiative(3L, "Library reading");
    private final Initiative park = initiative(4L, "Park planting");

    private static Initiative initiative(Long id, String name) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        initiative.setName(name);
        initiative.setEnabled(true);
        return initiative;
    }

    private static VolunteerInitiative membership(Initiative initiative, LocalDateTime respondedAt, Boolean enabled) {
        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setInitiative(initiative);
        membership.setRequestJoinDttm(LocalDateTime.of(2026, 10, 1, 9, 0));
        membership.setResponseJoinDttm(respondedAt);
        membership.setEnabled(enabled);
        return membership;
    }

    private UserPrincipal volunteer() {
        User user = new User();
        user.setId(42L);
        user.setUsername("vol1");
        user.setRoles(new HashSet<>(Set.of(new Role("VOLUNTEER"))));
        return new UserPrincipal(user);
    }

    @BeforeEach
    void setUp() {
        given(volunteerInitiativeService.findAvailableInitiatives()).willReturn(List.of(beach, foodBank, library, park));
    }

    /** beach approved, food bank pending, library turned down, park never asked. */
    private void givenMemberships() {
        LocalDateTime respondedAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        given(volunteerInitiativeService.findMembershipsForUser(42L)).willReturn(Map.of(
                1L, membership(beach, respondedAt, true),
                2L, membership(foodBank, null, null),
                3L, membership(library, respondedAt, false)));
    }

    @Test
    void list_opensOnJoinedInitiatives() throws Exception {
        givenMemberships();

        mockMvc.perform(get("/initiatives").with(user(volunteer())))
                .andExpect(status().isOk())
                .andExpect(view().name("initiatives/list"))
                .andExpect(model().attribute("filter", MembershipFilter.JOINED))
                .andExpect(model().attribute("initiatives", contains(beach)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.JOINED, 1L)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.PENDING, 1L)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.REJECTED, 1L)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.NOT_JOINED, 1L)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.ALL, 4L)))
                .andExpect(content().string(containsString("Beach clean-up")))
                .andExpect(content().string(not(containsString("Park planting"))))
                .andExpect(content().string(containsString("href=\"/initiatives?show=all\"")))
                .andExpect(content().string(containsString("<a class=\"nav-link active\" href=\"/initiatives?show=joined\" aria-current=\"page\">")));
    }

    @Test
    void list_showsEachStatusOnRequest() throws Exception {
        givenMemberships();

        mockMvc.perform(get("/initiatives").param("show", "pending").with(user(volunteer())))
                .andExpect(model().attribute("initiatives", contains(foodBank)));
        mockMvc.perform(get("/initiatives").param("show", "rejected").with(user(volunteer())))
                .andExpect(model().attribute("initiatives", contains(library)));
        mockMvc.perform(get("/initiatives").param("show", "notJoined").with(user(volunteer())))
                .andExpect(model().attribute("initiatives", contains(park)))
                .andExpect(content().string(containsString("Park planting")))
                .andExpect(content().string(not(containsString("Beach clean-up"))));
        mockMvc.perform(get("/initiatives").param("show", "all").with(user(volunteer())))
                .andExpect(model().attribute("initiatives", contains(beach, foodBank, library, park)));
    }

    @Test
    void list_withUnknownFilter_fallsBackToJoined() throws Exception {
        givenMemberships();

        mockMvc.perform(get("/initiatives").param("show", "bogus").with(user(volunteer())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filter", MembershipFilter.JOINED))
                .andExpect(model().attribute("initiatives", contains(beach)));
    }

    @Test
    void list_withNothingJoined_pointsToInitiativesToJoin() throws Exception {
        given(volunteerInitiativeService.findMembershipsForUser(42L)).willReturn(Map.of());

        mockMvc.perform(get("/initiatives").with(user(volunteer())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("initiatives", empty()))
                .andExpect(content().string(containsString("You haven&#39;t joined any initiatives yet.")))
                .andExpect(content().string(containsString("Browse initiatives you can join")));
    }

    @Test
    void list_emptyOtherFilter_saysSo() throws Exception {
        given(volunteerInitiativeService.findMembershipsForUser(42L)).willReturn(Map.of());

        mockMvc.perform(get("/initiatives").param("show", "pending").with(user(volunteer())))
                .andExpect(content().string(containsString("No initiatives here.")))
                .andExpect(content().string(not(containsString("Browse initiatives you can join"))));
    }

    private static Office office(Long id, String name) {
        Office office = new Office();
        office.setId(id);
        office.setName(name);
        return office;
    }

    /** Youth Office: beach and park; Coast Office: food bank; the library belongs to no office. */
    private void givenOffices() {
        Office youth = office(10L, "Youth Office");
        Office coast = office(11L, "Coast Office");
        beach.setOffice(youth);
        park.setOffice(youth);
        foodBank.setOffice(coast);
    }

    @Test
    void list_groupsByOfficeName_withTheNoOfficeGroupLast_andShowsTheOfficeOnEachCard() throws Exception {
        givenMemberships();
        givenOffices();

        mockMvc.perform(get("/initiatives").param("show", "all").with(user(volunteer())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("office", nullValue()))
                .andExpect(content().string(stringContainsInOrder(
                        "class=\"vp-office-heading\"", "Coast Office", "Food bank",
                        "class=\"vp-office-heading\"", "Youth Office", "Beach clean-up", "Park planting",
                        "class=\"vp-office-heading\"", "No office", "Library reading")))
                // The card says its office too
                .andExpect(content().string(stringContainsInOrder("Beach clean-up", "vp-card-office", "Youth Office")))
                // The picker lists the offices by name, and "No office" because one has none
                .andExpect(content().string(stringContainsInOrder(
                        "<option value=\"\" selected=\"selected\">All offices</option>",
                        "<option value=\"11\">Coast Office</option>",
                        "<option value=\"10\">Youth Office</option>",
                        "<option value=\"none\">No office</option>")));
    }

    @Test
    void list_filtersByOffice_andCountsWithinIt_keepingTheOfficeInThePills() throws Exception {
        givenMemberships();
        givenOffices();

        mockMvc.perform(get("/initiatives").param("show", "all").param("office", "10").with(user(volunteer())))
                .andExpect(model().attribute("office", "10"))
                .andExpect(model().attribute("initiatives", contains(beach, park)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.ALL, 2L)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.JOINED, 1L)))
                .andExpect(model().attribute("counts", hasEntry(MembershipFilter.PENDING, 0L)))
                .andExpect(content().string(containsString("<option value=\"10\" selected=\"selected\">Youth Office</option>")))
                .andExpect(content().string(containsString("href=\"/initiatives?show=pending&amp;office=10\"")))
                .andExpect(content().string(not(containsString("Food bank"))));

        mockMvc.perform(get("/initiatives").param("show", "all").param("office", "none").with(user(volunteer())))
                .andExpect(model().attribute("initiatives", contains(library)));
    }

    @Test
    void list_withAnUnknownOffice_showsAllOffices() throws Exception {
        givenMemberships();
        givenOffices();

        mockMvc.perform(get("/initiatives").param("show", "all").param("office", "999").with(user(volunteer())))
                .andExpect(model().attribute("office", nullValue()))
                .andExpect(model().attribute("initiatives", contains(beach, foodBank, library, park)));
        // "none" is only offered (and honoured) when some initiative has no office
        library.setOffice(office(10L, "Youth Office"));
        mockMvc.perform(get("/initiatives").param("show", "all").param("office", "none").with(user(volunteer())))
                .andExpect(model().attribute("office", nullValue()))
                .andExpect(content().string(not(containsString("<option value=\"none\""))));
    }

    @Test
    void list_emptyJoinedWithinAnOffice_doesNotClaimNothingIsJoined() throws Exception {
        givenMemberships();
        givenOffices();

        mockMvc.perform(get("/initiatives").param("office", "11").with(user(volunteer())))
                .andExpect(model().attribute("initiatives", empty()))
                .andExpect(content().string(containsString("No initiatives here.")))
                .andExpect(content().string(not(containsStringIgnoringCase("You haven&#39;t joined"))));
    }

    @Test
    void list_showsEachInitiativesMembersAndPendingRequests_andZeroForNoRequests() throws Exception {
        givenMemberships();
        given(volunteerInitiativeService.countMembersByInitiative())
                .willReturn(Map.of(1L, new MemberCounts(1L, 12L, 3L)));

        mockMvc.perform(get("/initiatives").param("show", "all").with(user(volunteer())))
                .andExpect(status().isOk())
                .andExpect(content().string(stringContainsInOrder("Beach clean-up",
                        "Members", "<span class=\"vp-filter-count\">12</span>",
                        "Pending requests", "<span class=\"vp-filter-count\">3</span>", "Food bank")))
                // The park has no requests at all, so it has no row: shown as 0
                .andExpect(content().string(stringContainsInOrder("Park planting",
                        "Members", "<span class=\"vp-filter-count\">0</span>",
                        "Pending requests", "<span class=\"vp-filter-count\">0</span>")));
    }
}
