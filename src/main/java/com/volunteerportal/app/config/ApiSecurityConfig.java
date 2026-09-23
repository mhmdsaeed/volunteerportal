package com.volunteerportal.app.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.volunteerportal.app.security.ApiTokenAuthenticationFilter;
import com.volunteerportal.app.service.ApiTokenService;

/**
 * Security for the mobile app API (/api/**), checked before the website's chain in SecurityConfig.
 * Stateless: requests are authenticated only by their bearer token, never by the website's session
 * cookie - which is also why CSRF protection isn't needed here. Errors are JSON, not redirects.
 */
@Configuration
public class ApiSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, ApiTokenService apiTokenService,
            @Value("${app.api.cors-allowed-origin-patterns:}") List<String> corsOrigins) throws Exception {
        http
            .securityMatcher("/api/**")
            .cors(cors -> cors.configurationSource(corsSource(corsOrigins)))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .requestCache(cache -> cache.disable())
            .addFilterBefore(new ApiTokenAuthenticationFilter(apiTokenService), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, e) -> json(response, 401, "unauthorized",
                        "Missing, invalid or expired token"))
                .accessDeniedHandler((request, response, e) -> json(response, 403, "forbidden",
                        "You don't have access to this"))
            );
        return http.build();
    }

    /**
     * Native phone apps don't need CORS; only the app running in a browser does (for testing, the
     * dev profile allows http://localhost:*). Empty by default, i.e. no cross-origin access.
     */
    private static CorsConfigurationSource corsSource(List<String> originPatterns) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> patterns = originPatterns.stream().map(String::trim).filter(p -> !p.isEmpty()).toList();
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language"));
        config.setAllowCredentials(false); // bearer tokens, not cookies
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void json(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
