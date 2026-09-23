package com.volunteerportal.app.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageArgsConverterTest {

    private final MessageArgsConverter converter = new MessageArgsConverter();

    @Test
    void roundTrip_keepsArgumentsIncludingCommasQuotesArabicAndEmptyOnes() {
        List<String> args = List.of("Beach, Cleanup", "O'Brien \"x\"", "تنظيف الشاطئ", "");

        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(args))).containsExactlyElementsOf(args);
    }

    @Test
    void noArguments_isStoredAsNullAndReadBackAsEmpty() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    void separatorInsideAnArgument_isStrippedSoItCannotSplitTheArgument() {
        String stored = converter.convertToDatabaseColumn(List.of("a" + MessageArgsConverter.SEPARATOR + "b", "c"));

        assertThat(converter.convertToEntityAttribute(stored)).containsExactly("ab", "c");
    }
}
