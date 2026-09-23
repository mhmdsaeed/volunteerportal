package com.volunteerportal.app.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the English/Arabic bundles against drift: a key added to one file but not the other
 * would render as "??key_ar??" (or fall back to English) on the untranslated side.
 */
class I18nMessagesTest {

    @Test
    void englishAndArabicBundles_haveTheSameKeys() throws IOException {
        Properties english = load("messages.properties");
        Properties arabic = load("messages_ar.properties");

        assertThat(arabic.stringPropertyNames())
                .as("keys missing from messages_ar.properties")
                .containsAll(english.stringPropertyNames());
        assertThat(english.stringPropertyNames())
                .as("keys missing from messages.properties")
                .containsAll(arabic.stringPropertyNames());
    }

    @Test
    void bundles_haveNoBlankValues() throws IOException {
        for (String bundle : new String[] { "messages.properties", "messages_ar.properties" }) {
            Properties messages = load(bundle);
            assertThat(messages.stringPropertyNames())
                    .as("blank values in " + bundle)
                    .allSatisfy(key -> assertThat(messages.getProperty(key)).isNotBlank());
        }
    }

    @Test
    void bundles_declareTextDirection() throws IOException {
        assertThat(load("messages.properties").getProperty("app.dir")).isEqualTo("ltr");
        assertThat(load("messages_ar.properties").getProperty("app.dir")).isEqualTo("rtl");
    }

    private Properties load(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(in).as(name + " on classpath").isNotNull();
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }
}
