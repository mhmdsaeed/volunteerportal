package com.volunteerportal.app.regression;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.CheckInCodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With app.checkin.show-link on (as in the dev profile), the QR page offers the link to test without a phone. */
@SpringBootTest(properties = "app.checkin.show-link=true")
@AutoConfigureMockMvc
class CheckInTestLinkTest {

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
    private CheckInCodes checkInCodes;

    private User supervisor;
    private Initiative initiative;
    private Event event;

    @BeforeEach
    void setUp() {
        supervisor = new User();
        supervisor.setUsername("ci_link_super_" + System.nanoTime());
        supervisor.setEmail(supervisor.getUsername() + "@example.com");
        supervisor.setPassword("irrelevant-hash");
        supervisor.setEnabled(true);
        supervisor.setRoles(new HashSet<>(Set.<Role>of(roleRepository.findByName("COORDINATOR").orElseThrow())));
        supervisor = userRepository.save(supervisor);

        initiative = new Initiative();
        initiative.setName("Link Initiative " + System.nanoTime());
        initiative.setEnabled(true);
        initiative.setSupervisor(supervisor);
        initiative = initiativeRepository.save(initiative);

        event = new Event();
        event.setName("Link Event");
        event.setInitiative(initiative);
        event.setEnabled(true);
        event = eventRepository.save(event);
    }

    @AfterEach
    void cleanUp() {
        eventRepository.delete(event);
        initiativeRepository.delete(initiative);
        userRepository.delete(supervisor);
    }

    @Test
    void qrPage_showsTheTestLink_andTheLinkIsTheSameValidCheckInUrlAsTheQr() throws Exception {
        String base = "/coordinator/initiatives/" + initiative.getId() + "/events/" + event.getId() + "/checkin";

        mockMvc.perform(get(base).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"testLink\"")))
                .andExpect(content().string(containsString(base + "/link")));

        String link = mockMvc.perform(get(base + "/link").with(user(principal())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(link).startsWith("http://localhost/checkin/" + event.getId() + "?code=");
        String code = link.substring(link.indexOf("code=") + "code=".length());
        assertThat(checkInCodes.isValid(event.getId(), code)).isTrue();
    }

    private UserPrincipal principal() {
        return new UserPrincipal(userRepository.findByUsername(supervisor.getUsername()).orElseThrow());
    }
}
