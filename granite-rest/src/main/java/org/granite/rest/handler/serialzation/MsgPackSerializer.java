package org.granite.rest.handler.serialzation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.msgpack.jackson.dataformat.MessagePackFactory;

public class MsgPackSerializer<V> extends ContentTypeSerializer<V> {

    public MsgPackSerializer(final TypeReference<V> itemClass) {
        super(createObjectMapper(), itemClass);
    }

    protected static ObjectMapper createObjectMapper(){
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateStringSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeStringSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateStringDeserializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeStringDeserializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
    @Override
    public String getContentType() {
        return "application/x-msgpack";
    }
}
