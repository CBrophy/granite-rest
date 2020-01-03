package org.granite.rest.handler.serialzation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;

public class LocalDateStringDeserializer extends JsonDeserializer<LocalDate> implements
    Serializable {

  @Override
  public LocalDate deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    return LocalDate.parse(p.getValueAsString(), TimeTools.getDateFormatter());
  }
}
