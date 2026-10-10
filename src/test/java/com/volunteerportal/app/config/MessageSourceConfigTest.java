package com.volunteerportal.app.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSourceConfigTest {

    private final MessageSource messages = messages();

    private static MessageSource messages() {
        MessageSourceProperties properties = new MessageSourceProperties();
        properties.setBasename(List.of("messages"));
        properties.setEncoding(StandardCharsets.UTF_8);
        properties.setFallbackToSystemLocale(false);
        return new MessageSourceConfig().messageSource(properties);
    }

    @Test
    void numbersInArabicMessages_useWesternDigits() {
        assertThat(messages.getMessage("joinRequests.viewAnswers", new Object[] { 3 }, LocaleConfig.ARABIC))
                .isEqualTo("عرض الإجابات (3)");
        assertThat(messages.getMessage("checkinQr.intro", new Object[] { 30 }, LocaleConfig.ARABIC))
                .contains("كل 30 ثانية");
    }

    @Test
    void englishMessages_keepChoiceFormats() {
        assertThat(messages.getMessage("home.requestsWaiting", new Object[] { 1 }, Locale.ENGLISH))
                .isEqualTo("1 join request is waiting for your decision.");
        assertThat(messages.getMessage("home.requestsWaiting", new Object[] { 1200 }, Locale.ENGLISH))
                .isEqualTo("1,200 join requests are waiting for your decision.");
    }

    @Test
    void englishRequest_doesNotFallBackToArabic() {
        assertThat(messages.getMessage("common.yes", null, Locale.ENGLISH)).isEqualTo("Yes");
    }
}
