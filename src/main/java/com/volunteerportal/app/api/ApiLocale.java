package com.volunteerportal.app.api;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import com.volunteerportal.app.config.LocaleConfig;

/**
 * The API's language comes from the phone's {@code Accept-Language} header (the website uses a
 * cookie instead). Only English and Arabic are supported; anything else gets English.
 */
final class ApiLocale {

    private ApiLocale() {
    }

    static Locale of(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header != null && !header.isBlank()) {
            try {
                for (Locale.LanguageRange range : Locale.LanguageRange.parse(header)) {
                    String language = range.getRange().split("-")[0];
                    for (Locale supported : LocaleConfig.SUPPORTED_LOCALES) {
                        if (supported.getLanguage().equalsIgnoreCase(language)) {
                            return supported;
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // malformed header: fall through to English
            }
        }
        return Locale.ENGLISH;
    }
}
