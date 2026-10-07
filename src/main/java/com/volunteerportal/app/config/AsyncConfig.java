package com.volunteerportal.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Lets {@code @Async} methods (sending email) run on Spring Boot's task executor instead of the request thread. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
