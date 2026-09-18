package com.volunteerportal.app.regression;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Grade;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.GradeRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.repository.VolunteerProfileRepository;
import com.volunteerportal.app.security.UserPrincipal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for the LazyInitializationException that occurred when a list/detail view
 * rendered a lazy @ManyToOne association's name/username after the request's Hibernate session
 * had already closed (open-in-view is disabled). Unlike the @WebMvcTest suite - which mocks the
 * service layer and never touches a real lazy proxy - this uses the full application context
 * with real repositories, so it actually exercises Hibernate's session lifecycle the way a
 * running server would. Data is persisted directly via repository calls (each in its own
 * short-lived transaction, exactly like production traffic) rather than under a test-level
 * @Transactional, and each test cleans up what it created.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LazyAssociationRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private InitiativeRepository initiativeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AttendRepository attendRepository;

    @Autowired
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private GradeRepository gradeRepository;

    private UserPrincipal adminPrincipal() {
        return new UserPrincipal(userRepository.findByUsername("admin").orElseThrow());
    }

    private User persistVolunteer(String username) {
        Role volunteerRole = roleRepository.findByName("VOLUNTEER").orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("irrelevant-hash");
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(volunteerRole)));
        return userRepository.save(user);
    }

    @Test
    void officeList_withRoleAndUserAssigned_rendersWithoutError() throws Exception {
        Role coordinatorRole = roleRepository.findByName("COORDINATOR").orElseThrow();
        User owner = persistVolunteer("lazytest_office_owner_" + System.nanoTime());

        Office office = new Office();
        office.setName("Lazy Test Office " + System.nanoTime());
        office.setRole(coordinatorRole);
        office.setUser(owner);
        office = officeRepository.save(office);

        try {
            mockMvc.perform(get("/admin/offices").with(user(adminPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(office.getName())))
                    .andExpect(content().string(containsString("COORDINATOR")))
                    .andExpect(content().string(containsString(owner.getUsername())));
        } finally {
            officeRepository.delete(office);
            userRepository.delete(owner);
        }
    }

    @Test
    void initiativeList_withOfficeAndSupervisorAssigned_rendersWithoutError() throws Exception {
        User supervisor = persistVolunteer("lazytest_supervisor_" + System.nanoTime());

        Office office = new Office();
        office.setName("Lazy Test Initiative Office " + System.nanoTime());
        office = officeRepository.save(office);

        Initiative initiative = new Initiative();
        initiative.setName("Lazy Test Initiative " + System.nanoTime());
        initiative.setOffice(office);
        initiative.setSupervisor(supervisor);
        initiative.setEnabled(true);
        initiative = initiativeRepository.save(initiative);

        try {
            mockMvc.perform(get("/admin/initiatives").with(user(adminPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(initiative.getName())))
                    .andExpect(content().string(containsString(office.getName())))
                    .andExpect(content().string(containsString(supervisor.getUsername())));
        } finally {
            initiativeRepository.delete(initiative);
            officeRepository.delete(office);
            userRepository.delete(supervisor);
        }
    }

    @Test
    void volunteerFacingInitiativeDetail_withOfficeAssigned_rendersWithoutError() throws Exception {
        Office office = new Office();
        office.setName("Lazy Test Detail Office " + System.nanoTime());
        office = officeRepository.save(office);

        Initiative initiative = new Initiative();
        initiative.setName("Lazy Test Detail Initiative " + System.nanoTime());
        initiative.setOffice(office);
        initiative.setEnabled(true);
        initiative = initiativeRepository.save(initiative);

        try {
            mockMvc.perform(get("/initiatives/{id}", initiative.getId()).with(user(adminPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(office.getName())));
        } finally {
            initiativeRepository.delete(initiative);
            officeRepository.delete(office);
        }
    }

    @Test
    void adminVolunteersList_withGradeAssigned_rendersWithoutError() throws Exception {
        Grade grade = gradeRepository.save(newGrade("LazyTestGrade " + System.nanoTime()));
        User volunteer = persistVolunteer("lazytest_volunteer_admin_" + System.nanoTime());

        VolunteerProfile profile = new VolunteerProfile();
        profile.setUser(volunteer);
        profile.setGrade(grade);
        profile.setPoints(10L);
        profile = volunteerProfileRepository.save(profile);

        try {
            mockMvc.perform(get("/admin/volunteers").with(user(adminPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(volunteer.getUsername())))
                    .andExpect(content().string(containsString(grade.getName())));
        } finally {
            volunteerProfileRepository.delete(profile);
            userRepository.delete(volunteer);
            gradeRepository.delete(grade);
        }
    }

    @Test
    void profileSelfService_withGradeAssigned_rendersWithoutError() throws Exception {
        Grade grade = gradeRepository.save(newGrade("LazyTestGrade " + System.nanoTime()));
        User volunteer = persistVolunteer("lazytest_volunteer_self_" + System.nanoTime());

        VolunteerProfile profile = new VolunteerProfile();
        profile.setUser(volunteer);
        profile.setGrade(grade);
        profile.setPoints(5L);
        profile = volunteerProfileRepository.save(profile);

        try {
            mockMvc.perform(get("/profile").with(user(new UserPrincipal(volunteer))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(grade.getName())));
        } finally {
            volunteerProfileRepository.delete(profile);
            userRepository.delete(volunteer);
            gradeRepository.delete(grade);
        }
    }

    @Test
    void coordinatorRequestsList_withVolunteerUserAssigned_rendersWithoutError() throws Exception {
        User volunteer = persistVolunteer("lazytest_requester_" + System.nanoTime());

        Initiative initiative = new Initiative();
        initiative.setName("Lazy Test Requests Initiative " + System.nanoTime());
        initiative.setEnabled(true);
        initiative = initiativeRepository.save(initiative);

        VolunteerInitiative request = new VolunteerInitiative();
        request.setUser(volunteer);
        request.setInitiative(initiative);
        request.setRequestJoinDttm(LocalDateTime.now());
        request = volunteerInitiativeRepository.save(request);

        try {
            mockMvc.perform(get("/coordinator/initiatives/{id}/requests", initiative.getId())
                            .with(user(adminPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(volunteer.getUsername())));
        } finally {
            volunteerInitiativeRepository.delete(request);
            initiativeRepository.delete(initiative);
            userRepository.delete(volunteer);
        }
    }

    @Test
    void attendanceList_withVolunteerInitiativeUserAssigned_rendersWithoutError() throws Exception {
        User volunteer = persistVolunteer("lazytest_attendee_" + System.nanoTime());

        Initiative initiative = new Initiative();
        initiative.setName("Lazy Test Attendance Initiative " + System.nanoTime());
        initiative.setEnabled(true);
        initiative = initiativeRepository.save(initiative);

        Event event = new Event();
        event.setName("Lazy Test Event " + System.nanoTime());
        event.setInitiative(initiative);
        event = eventRepository.save(event);

        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setUser(volunteer);
        membership.setInitiative(initiative);
        membership.setRequestJoinDttm(LocalDateTime.now());
        membership = volunteerInitiativeRepository.save(membership);

        Attend attend = new Attend();
        attend.setVolunteerInitiative(membership);
        attend.setEvent(event);
        attend.setAttendInOut(1);
        attend.setAttendDttm(LocalDateTime.now());
        attend = attendRepository.save(attend);

        try {
            mockMvc.perform(get("/admin/initiatives/{initiativeId}/events/{eventId}/attendance",
                            initiative.getId(), event.getId()).with(user(adminPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(volunteer.getUsername())));
        } finally {
            attendRepository.delete(attend);
            volunteerInitiativeRepository.delete(membership);
            eventRepository.delete(event);
            initiativeRepository.delete(initiative);
            userRepository.delete(volunteer);
        }
    }

    @Test
    void eventAttendanceReport_withInitiativeAssigned_rendersWithoutError() throws Exception {
        Initiative initiative = new Initiative();
        initiative.setName("Lazy Test Report Initiative " + System.nanoTime());
        initiative.setEnabled(true);
        initiative = initiativeRepository.save(initiative);

        Event event = new Event();
        event.setName("Lazy Test Report Event " + System.nanoTime());
        event.setInitiative(initiative);
        event = eventRepository.save(event);

        try {
            mockMvc.perform(get("/admin/reports/attendance").with(user(adminPrincipal())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(initiative.getName())))
                    .andExpect(content().string(containsString(event.getName())));
        } finally {
            eventRepository.delete(event);
            initiativeRepository.delete(initiative);
        }
    }

    private Grade newGrade(String name) {
        Grade grade = new Grade();
        grade.setName(name);
        return grade;
    }
}
