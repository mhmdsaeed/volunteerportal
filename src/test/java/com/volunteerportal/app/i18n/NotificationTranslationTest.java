package com.volunteerportal.app.i18n;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
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

    @Autowired
    private DataSource dataSource;

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
        legacy("Legacy English text from before V4.");

        mockMvc.perform(get("/notifications").param("lang", "ar").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Legacy English text from before V4.")));
    }

    /**
     * Runs the V5 backfill script again (it only touches rows without a key) over old-style rows,
     * using MySQL's real regex functions.
     */
    @Test
    void backfillMigration_givesOldEnglishNotificationsAKeySoTheyAreTranslated() throws Exception {
        Notification approved = legacy("Your request to join 'Beach, Cleanup' was approved.");
        Notification rejected = legacy("Your request to join 'O'Brien Drive' was not approved.");
        Notification requested = legacy("some_user requested to join 'Food Drive'.");
        Notification graded = legacy("Your volunteer profile was updated: grade is now Gold, points: 12.");
        Notification unranked = legacy("Your volunteer profile was updated: grade is now Unranked, points: 0.");
        Notification unknown = legacy("Something the app never sent.");

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V5__backfill_notification_message_keys.sql"));
        }

        assertKeyAndArgs(approved, "notification.joinApproved", "Beach, Cleanup");
        assertKeyAndArgs(rejected, "notification.joinRejected", "O'Brien Drive");
        assertKeyAndArgs(requested, "notification.joinRequested", "some_user", "Food Drive");
        assertKeyAndArgs(graded, "notification.profileUpdated", "Gold", "12");
        assertKeyAndArgs(unranked, "notification.profileUpdatedUnranked", "0");
        assertThat(notificationRepository.findById(unknown.getId()).orElseThrow().getMessageKey()).isNull();

        mockMvc.perform(get("/notifications").param("lang", "ar").with(user(principal())))
                .andExpect(content().string(containsString("تم قبول طلب انضمامك إلى «Beach, Cleanup».")))
                .andExpect(content().string(containsString("طلب some_user الانضمام إلى «Food Drive».")))
                .andExpect(content().string(containsString("أنت الآن بدون درجة")))
                .andExpect(content().string(containsString("Something the app never sent.")));
    }

    private Notification legacy(String message) {
        Notification notification = new Notification();
        notification.setUser(volunteer);
        notification.setMessage(message);
        notification.setCreatedDttm(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    private void assertKeyAndArgs(Notification before, String expectedKey, String... expectedArgs) {
        Notification after = notificationRepository.findById(before.getId()).orElseThrow();
        assertThat(after.getMessageKey()).as(before.getMessage()).isEqualTo(expectedKey);
        assertThat(after.getMessageArgs()).as(before.getMessage()).containsExactly(expectedArgs);
    }

    private UserPrincipal principal() {
        return new UserPrincipal(userRepository.findByUsername(volunteer.getUsername()).orElseThrow());
    }
}
