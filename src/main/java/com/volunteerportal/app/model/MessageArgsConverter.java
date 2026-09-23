package com.volunteerportal.app.model;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores a notification's message arguments in one column, separated by the ASCII unit
 * separator (U+001F), a control character that can't appear in names typed into the forms.
 */
@Converter
public class MessageArgsConverter implements AttributeConverter<List<String>, String> {

    static final String SEPARATOR = "\u001F";

    @Override
    public String convertToDatabaseColumn(List<String> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        return String.join(SEPARATOR, args.stream().map(arg -> arg == null ? "" : arg.replace(SEPARATOR, "")).toList());
    }

    @Override
    public List<String> convertToEntityAttribute(String column) {
        if (column == null || column.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(column.split(SEPARATOR, -1));
    }
}
