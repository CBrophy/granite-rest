package org.granite.rest.handler.serialzation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializer<V> extends ContentTypeSerializer<V> {

    public JsonSerializer(final TypeReference<V> itemClass) {
        super(new ObjectMapper(), itemClass);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

}
