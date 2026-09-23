package com.volunteerportal.app.i18n;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.NotificationRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A notification is stored as a message key + arguments and rendered in the viewer's language,
 * round-tripping through the real database (V4 columns) and the notifications page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationTranslationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User volunteer;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.findByName("VOLUNTEER").orElseThrow();
        volunteer = new User();
        volunteer.setUsername("notif_i18n_" + System.nanoTime());
        volunteer.setEmail(volunteer.getUsername() + "@example.com");
        volunteer.setPassword("irrelevant-hash");
        volunteer.setEnabled(true);
        volunteer.setRoles(new HashSet<>(Set.of(role)));
        volunteer = userRepository.save(volunteer);
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedDttmDesc(volunteer.getId()));
        userRepository.delete(volunteer);
    }

    @Test
    void notification_isShownInTheViewersLanguage() throws Exception {
        notificationService.notify(volunteer, "notification.joinApproved", "/initiatives/1", "Beach Cleanup");

        Notification stored = notificationRepository.findByUserIdOrderByCreatedDttmDesc(volunteer.getId()).get(0);
        assertThat(stored.getMessageKey()).isEqualTo("notification.joinApproved");
        assertThat(stored.getMessageArgs()).containsExactly("Beach Cleanup");

        mockMvc.perform(get("/notifications").param("lang", "ar").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("تم قبول طلب انضمامك إلى «Beach Cleanup».")));

        mockMvc.perform(get("/notifications").param("lang", "en").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your request to join &#39;Beach Cleanup&#39; was approved.")))
                .andExpect(content().string(not(containsString("تم قبول"))));
    }

    @Test
    void notificationWithMultipleArguments_keepsThemInOrder() throws Exception {
        notificationService.notify(volunteer, "notification.joinRequested", "/coordinator/requests", "some_user", "Food Drive");

        mockMvc.perform(get("/notifications").param("lang", "ar").with(user(principal())))
                .andExpect(content().string(containsString("طلب some_user الانضمام إلى «Food Drive».")));
    }

    @Test
    void olderNotificationWithoutKey_showsItsStoredText() throws Exception {
        Notification legacy = new Notification();
        legacy.setUser(volunteer);
        legacy.setMessage("Legacy English text from before V4.");
        legacy.setCreatedDttm(LocalDateTime.now());
        notificationRepository.save(legacy);

        mockMvc.perform(get("/notifications").param("lang", "ar").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Legacy English text from before V4.")));
    }

    private UserPrincipal principal() {
        return new UserPrincipal(userRepository.findByUsername(volunteer.getUsername()).orElseThrow());
    }
}
