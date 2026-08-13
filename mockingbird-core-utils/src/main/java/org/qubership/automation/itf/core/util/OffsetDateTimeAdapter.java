package org.qubership.automation.itf.core.util;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void write(JsonWriter out, OffsetDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.format(FORMATTER));
        }
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws IOException {
        String dateTimeString = in.nextString();
        if (dateTimeString == null) {
            return null;
        }
        return OffsetDateTime.parse(dateTimeString, FORMATTER);
    }
}
