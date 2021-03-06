package org.granite.rest.handler.serialzation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDate;

public class LocalDateStringSerializer extends JsonSerializer<LocalDate> {

  @Override
  public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeString(TimeTools.getDateFormatter().format(value));
  }
}
