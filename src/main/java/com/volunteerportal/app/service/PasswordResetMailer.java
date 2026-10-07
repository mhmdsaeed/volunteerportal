package com.volunteerportal.app.service;

import java.time.Duration;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Sends the password reset email in the user's language. Mail is set up with the {@code MAIL_*} settings
 * (spring.mail.* in application.yml); without a mail host it is off, unless {@code app.password-reset.log-links}
 * (the dev profile) writes the link to the server log instead. Sending runs in the background, so the form
 * answers at once and takes as long whether or not the email belongs to an account.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MessageSource messageSource;
    private final String host;
    private final String from;
    private final boolean logLinks;

    public PasswordResetMailer(ObjectProvider<JavaMailSender> mailSender, MessageSource messageSource,
            @Value("${spring.mail.host:}") String host, @Value("${app.mail.from:}") String from,
            @Value("${app.password-reset.log-links:false}") boolean logLinks) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.host = host;
        this.from = from;
        this.logLinks = logLinks;
    }

    private boolean mailConfigured() {
        return StringUtils.hasText(host) && mailSender.getIfAvailable() != null;
    }

    public boolean available() {
        return mailConfigured() || logLinks;
    }

    @Async
    public void send(String username, String email, String link, Duration validity, Locale locale) {
        if (logLinks) {
            log.info("Password reset link for {} (logged because app.password-reset.log-links is on): {}", username, link);
        }
        if (!mailConfigured()) {
            if (!logLinks) {
                log.warn("Password reset requested for {}, but mail is not set up (MAIL_HOST); no email sent", username);
            }
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject(messageSource.getMessage("passwordReset.email.subject",
                new Object[] {messageSource.getMessage("app.name", null, locale)}, locale));
        message.setText(messageSource.getMessage("passwordReset.email.body",
                new Object[] {username, link, String.valueOf(validity.toMinutes())}, locale));
        try {
            mailSender.getObject().send(message);
        } catch (MailException e) {
            log.error("Could not send the password reset email to {}: {}", username, e.getMessage());
        }
    }
}
