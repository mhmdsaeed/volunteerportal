package com.volunteerportal.app.i18n;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of the English/Arabic switch (LocaleConfig) against the real templates:
 * direction, stylesheet, cookie persistence, and that no page references a missing message key.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LocaleSwitchingTest {

    // Thymeleaf renders a missing #{key} as "??key_locale??"
    private static final String MISSING_KEY_MARKER = "??";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultLanguage_isEnglishLeftToRight() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\" dir=\"ltr\"")))
                .andExpect(content().string(containsString("/css/bootstrap.min.css")))
                .andExpect(content().string(containsString(">Login<")));
    }

    @Test
    void langParam_switchesToArabicRightToLeft_andRemembersItInACookie() throws Exception {
        mockMvc.perform(get("/login").param("lang", "ar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"ar\" dir=\"rtl\"")))
                .andExpect(content().string(containsString("/css/bootstrap.rtl.min.css")))
                .andExpect(content().string(containsString("تسجيل الدخول")))
                .andExpect(cookie().value("lang", "ar"));
    }

    @Test
    void langCookie_keepsArabicOnLaterRequests() throws Exception {
        mockMvc.perform(get("/register").cookie(new Cookie("lang", "ar")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("dir=\"rtl\"")))
                .andExpect(content().string(containsString("إنشاء حساب")));
    }

    @Test
    void unsupportedLanguage_fallsBackToEnglish() throws Exception {
        mockMvc.perform(get("/login").param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\" dir=\"ltr\"")))
                .andExpect(cookie().value("lang", "en"));
    }

    @Test
    void languageSwitchLink_offersTheOtherLanguage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("?lang=ar")));
        mockMvc.perform(get("/login").param("lang", "ar"))
                .andExpect(content().string(containsString("?lang=en")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/login", "/register" })
    void publicPages_haveNoMissingMessageKeys(String path) throws Exception {
        assertNoMissingKeys(path);
    }

    @ParameterizedTest
    @WithMockUser(roles = "ADMIN")
    @ValueSource(strings = {
            "/admin", "/admin/offices", "/admin/offices/new", "/admin/initiatives", "/admin/initiatives/new",
            "/admin/question-library", "/admin/question-library/new", "/admin/grades", "/admin/grades/new",
            "/admin/volunteers", "/admin/config", "/admin/config/new", "/admin/reports",
            "/admin/reports/initiatives", "/admin/reports/attendance", "/admin/reports/volunteers" })
    void adminPages_haveNoMissingMessageKeys(String path) throws Exception {
        assertNoMissingKeys(path);
    }

    private void assertNoMissingKeys(String path) throws Exception {
        for (String lang : new String[] { "en", "ar" }) {
            mockMvc.perform(get(path).param("lang", lang))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(MISSING_KEY_MARKER))));
        }
    }
}
