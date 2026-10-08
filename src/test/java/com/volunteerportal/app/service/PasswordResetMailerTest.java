package com.volunteerportal.app.service;

import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The mailer on its own (the other tests replace it): when reset emails count as available, the startup line
 * the deploy README tells admins to look for, and the email itself in English and Arabic, with the real
 * message files.
 */
@ExtendWith(OutputCaptureExtension.class)
class PasswordResetMailerTest {

    private static final String LINK = "https://portal.example.org/reset-password?token=abc123";
    private static final Duration VALIDITY = Duration.ofMinutes(60);

    private final JavaMailSender sender = mock(JavaMailSender.class);

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        return messages;
    }

    @SuppressWarnings("unchecked")
    private PasswordResetMailer mailer(String host, String from, boolean logLinks, boolean senderBean) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        given(provider.getIfAvailable()).willReturn(senderBean ? sender : null);
        given(provider.getObject()).willReturn(sender);
        return new PasswordResetMailer(provider, messages(), host, 587, from, logLinks);
    }

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        return message.getValue();
    }

    @Test
    void emailIsAvailable_withAMailHost_orWhenLinksAreLogged() {
        assertThat(mailer("smtp.example.org", "", false, true).available()).isTrue();
        // An empty MAIL_HOST (what Docker Compose passes for a blank .env line) means off
        assertThat(mailer("", "", false, true).available()).isFalse();
        assertThat(mailer("   ", "", false, true).available()).isFalse();
        assertThat(mailer("smtp.example.org", "", false, false).available()).isFalse();
        // The dev profile: no mail server, the link goes to the log
        assertThat(mailer("", "", true, false).available()).isTrue();
    }

    @Test
    void startupLine_saysWhereEmailsGo(CapturedOutput output) {
        mailer("smtp.example.org", "portal@example.org", false, true).logStatus();
        mailer("smtp.example.org", "", false, true).logStatus();

        assertThat(output).contains("Password reset emails are on: sent through smtp.example.org:587 from portal@example.org");
        assertThat(output).contains("Password reset emails are on: sent through smtp.example.org:587 from (MAIL_FROM not set)");
    }

    @Test
    void startupLine_saysWhenEmailIsOff(CapturedOutput output) {
        mailer("", "", false, true).logStatus();
        assertThat(output).contains("Password reset emails are off: MAIL_HOST is not set");
        assertThat(output).doesNotContain("written to this log");

        mailer("", "", true, false).logStatus();
        assertThat(output).contains("Password reset emails are off: MAIL_HOST is not set (links are written to this log instead)");
    }

    @Test
    void theEmail_inEnglish_greetsByUsername_withTheLinkAndHowLongItWorks() {
        mailer("smtp.example.org", "portal@example.org", false, true)
                .send("sara", "sara@example.com", LINK, VALIDITY, Locale.ENGLISH);

        SimpleMailMessage message = sentMessage();
        assertThat(message.getFrom()).isEqualTo("portal@example.org");
        assertThat(message.getTo()).containsExactly("sara@example.com");
        assertThat(message.getSubject()).isEqualTo("Reset your Volunteer Portal password");
        assertThat(message.getText())
                .startsWith("Hello sara,\n\n")
                .contains("\n\n" + LINK + "\n\n")
                .contains("It works once, for 60 minutes.")
                .contains("If you didn't ask for this"); // the apostrophe survives message formatting
    }

    @Test
    void theEmail_inArabic() {
        mailer("smtp.example.org", "portal@example.org", false, true)
                .send("sara", "sara@example.com", LINK, VALIDITY, Locale.forLanguageTag("ar"));

        SimpleMailMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo("إعادة تعيين كلمة المرور في بوابة المتطوعين");
        assertThat(message.getText())
                .startsWith("مرحباً sara،")
                .contains(LINK)
                .contains("60 دقيقة"); // Western digits, as elsewhere in the portal
    }

    @Test
    void withoutMailFrom_theSenderIsLeftToTheMailServer() {
        mailer("smtp.example.org", "", false, true).send("sara", "sara@example.com", LINK, VALIDITY, Locale.ENGLISH);

        assertThat(sentMessage().getFrom()).isNull();
    }

    @Test
    void withLinksLogged_andNoMailServer_theLinkGoesToTheLogOnly(CapturedOutput output) {
        mailer("", "", true, false).send("sara", "sara@example.com", LINK, VALIDITY, Locale.ENGLISH);

        verify(sender, never()).send(any(SimpleMailMessage.class));
        assertThat(output).contains("Password reset link for sara (logged because app.password-reset.log-links is on): " + LINK);
    }

    @Test
    void outsideDevelopment_theLinkIsNeverLogged(CapturedOutput output) {
        mailer("smtp.example.org", "portal@example.org", false, true)
                .send("sara", "sara@example.com", LINK, VALIDITY, Locale.ENGLISH);

        assertThat(output).doesNotContain(LINK);
    }

    @Test
    void aFailedSend_isLoggedWithTheReason_notThrown(CapturedOutput output) {
        willThrow(new MailSendException("Mail server connection failed")).given(sender).send(any(SimpleMailMessage.class));

        mailer("smtp.example.org", "portal@example.org", false, true)
                .send("sara", "sara@example.com", LINK, VALIDITY, Locale.ENGLISH);

        // The line the deploy README's troubleshooting table quotes
        assertThat(output).contains("Could not send the password reset email to sara: Mail server connection failed");
        assertThat(output).doesNotContain(LINK);
    }
}
