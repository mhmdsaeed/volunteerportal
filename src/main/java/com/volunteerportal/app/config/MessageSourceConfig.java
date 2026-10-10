package com.volunteerportal.app.config;

import java.text.MessageFormat;
import java.util.Locale;

import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Spring Boot's message source (still configured by {@code spring.messages.*}), except that numbers in messages
 * keep Western digits in Arabic, as dates already do. Otherwise {@code MessageFormat} writes "View answers (٣)"
 * and "every ٣٠ seconds" with Arabic-Indic digits next to dates and counts written with Western ones.
 */
@Configuration
@EnableConfigurationProperties(MessageSourceProperties.class)
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource(MessageSourceProperties properties) {
        ResourceBundleMessageSource messageSource = new LatinDigitsMessageSource();
        messageSource.setBasenames(properties.getBasename().toArray(new String[0]));
        if (properties.getEncoding() != null) {
            messageSource.setDefaultEncoding(properties.getEncoding().name());
        }
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        if (properties.getCacheDuration() != null) {
            messageSource.setCacheMillis(properties.getCacheDuration().toMillis());
        }
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
        return messageSource;
    }

    /** Formats message arguments with the Latin numbering system ({@code -u-nu-latn}) in every language. */
    static class LatinDigitsMessageSource extends ResourceBundleMessageSource {

        @Override
        protected MessageFormat createMessageFormat(String msg, Locale locale) {
            return super.createMessageFormat(msg, locale == null ? null
                    : new Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("nu", "latn").build());
        }
    }
}
