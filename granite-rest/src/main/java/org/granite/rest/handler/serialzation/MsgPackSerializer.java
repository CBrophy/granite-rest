package org.granite.rest.handler.serialzation;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.jackson.dataformat.MessagePackFactory;

public class MsgPackSerializer<V> extends ContentTypeSerializer<V> {

    public MsgPackSerializer(final Class<V> itemClass) {
        super(new ObjectMapper(new MessagePackFactory()), itemClass);
    }

    @Override
    public String getContentType() {
        return "application/x-msgpack";
    }
}
