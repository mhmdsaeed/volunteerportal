package com.volunteerportal.app.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Only the configured browser origins may call the API; by default none may (phone apps don't need CORS). */
class ApiCorsTest {

    private static final String PREFLIGHT_PATH = "/api/events";

    @Nested
    @SpringBootTest(properties = "app.api.cors-allowed-origin-patterns=http://localhost:*")
    @AutoConfigureMockMvc
    class WithLocalhostAllowed {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void localhostOrigin_isAllowedWithTheAuthorizationHeader() throws Exception {
            mockMvc.perform(options(PREFLIGHT_PATH)
                            .header("Origin", "http://localhost:5000")
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "authorization,accept-language"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5000"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }

        @Test
        void otherOrigins_areRefused() throws Exception {
            mockMvc.perform(options(PREFLIGHT_PATH)
                            .header("Origin", "https://evil.example")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class ByDefault {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void noBrowserOriginIsAllowed() throws Exception {
            mockMvc.perform(options(PREFLIGHT_PATH)
                            .header("Origin", "http://localhost:5000")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }
}
