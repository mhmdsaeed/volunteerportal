package com.volunteerportal.app.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.security.DisabledAccountFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** The website (form login + session). /api/** is handled first by ApiSecurityConfig. */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectProvider<UserRepository> userRepository)
            throws Exception {
        // Ends sessions of users deactivated after logging in. Optional only so @WebMvcTest slices
        // (no repositories) can still import this config; the application always has it.
        userRepository.ifAvailable(repository ->
                http.addFilterBefore(new DisabledAccountFilter(repository), AuthorizationFilter.class));
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/register", "/login", "/css/**", "/fonts/**", "/webjars/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/coordinator/**").hasAnyRole("COORDINATOR", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                // Back to the page that asked for login (e.g. a scanned QR check-in link), else /home
                .defaultSuccessUrl("/home", false)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/403")
            );

        return http.build();
    }
}
