package com.kangcode.pojo.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 兼容前端日期控件：yyyy-MM-dd HH:mm:ss、yyyy-MM-dd、yyyy-MM
 */
public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();
        try {
            return LocalDateTime.parse(text, DATETIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(text, DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(text + "-01", DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}
