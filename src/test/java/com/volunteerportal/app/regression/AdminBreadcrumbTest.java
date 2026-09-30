package com.volunteerportal.app.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every admin page shows where it is: Admin › ... › the current page (marked, not a link). */
@SpringBootTest
@AutoConfigureMockMvc
class AdminBreadcrumbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            /admin/offices                  | Offices
            /admin/offices/new              | New Office
            /admin/initiatives              | Initiatives
            /admin/initiatives/new          | New Initiative
            /admin/question-library         | Question Library
            /admin/question-library/new     | New Category
            /admin/grades                   | Grades
            /admin/grades/new               | New Grade
            /admin/volunteers               | Volunteers
            /admin/users                    | Users
            /admin/config                   | Config
            /admin/config/new               | New Config
            /admin/reports                  | Reports
            /admin/reports/initiatives      | Initiative Participation
            /admin/reports/attendance       | Event Attendance
            /admin/reports/volunteers       | Volunteer Leaderboard
            """)
    void adminPage_showsTrailFromAdminToTheCurrentPage(String path, String currentPage) throws Exception {
        String html = page(path, "en");

        assertThat(html).contains("<ol class=\"breadcrumb\">");
        assertThat(html).contains("<li class=\"breadcrumb-item\"><a href=\"/admin\">Admin</a></li>");
        assertThat(html).contains("<li class=\"breadcrumb-item active\" aria-current=\"page\">" + currentPage + "</li>");
    }

    @Test
    void formPages_linkBackToTheirList() throws Exception {
        assertThat(page("/admin/offices/new", "en")).contains("<a href=\"/admin/offices\">Offices</a>");
        assertThat(page("/admin/reports/volunteers", "en")).contains("<a href=\"/admin/reports\">Reports</a>");
    }

    @Test
    void dashboard_isTheStartOfTheTrail() throws Exception {
        assertThat(page("/admin", "en")).contains("<li class=\"breadcrumb-item active\" aria-current=\"page\">Admin</li>");
    }

    @Test
    void trail_isTranslated() throws Exception {
        String html = page("/admin/offices/new", "ar");

        assertThat(html).contains("aria-label=\"مسار التنقل\"");
        assertThat(html).contains("<a href=\"/admin\">الإدارة</a>");
        assertThat(html).contains("aria-current=\"page\">مكتب جديد</li>");
    }

    private String page(String path, String lang) throws Exception {
        return mockMvc.perform(get(path).param("lang", lang)
                        .with(user(new UserPrincipal(userRepository.findByUsername("admin").orElseThrow()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
