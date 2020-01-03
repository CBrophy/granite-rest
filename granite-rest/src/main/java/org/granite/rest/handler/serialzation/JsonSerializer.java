package org.granite.rest.handler.serialzation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class JsonSerializer<V> extends ContentTypeSerializer<V> {

    public JsonSerializer(final TypeReference<V> itemClass) {
        super(createObjectMapper(), itemClass);
    }

    protected static ObjectMapper createObjectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateStringSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeStringSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateStringDeserializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeStringDeserializer());
        return objectMapper;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

}
