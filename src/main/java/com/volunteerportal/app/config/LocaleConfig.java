package com.volunteerportal.app.config;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * English/Arabic UI. The language is picked with {@code ?lang=en|ar} and remembered in a cookie;
 * anything other than a supported language falls back to English.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final Locale ARABIC = Locale.forLanguageTag("ar");
    public static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, ARABIC);

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("lang") {
            @Override
            public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
                super.setLocale(request, response, supported(locale));
            }
        };
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        registry.addInterceptor(interceptor);
    }

    private static Locale supported(Locale locale) {
        if (locale == null) {
            return null;
        }
        return SUPPORTED_LOCALES.stream()
                .filter(candidate -> candidate.getLanguage().equals(locale.getLanguage()))
                .findFirst()
                .orElse(Locale.ENGLISH);
    }
}
