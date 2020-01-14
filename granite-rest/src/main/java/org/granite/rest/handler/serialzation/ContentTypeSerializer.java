package org.granite.rest.handler.serialzation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.granite.base.ExceptionTools;

public abstract class ContentTypeSerializer<V> {

  private final ObjectMapper objectMapper;
  private final TypeReference<V> itemClass;
  private final TypeReference<List<V>> listTypeReference;

  protected ContentTypeSerializer(
      final ObjectMapper objectMapper,
      final TypeReference<V> itemClass) {
    this.objectMapper = checkNotNull(objectMapper, "objectMapper");
    this.itemClass = checkNotNull(itemClass, "itemClass");
    this.listTypeReference = new TypeReference<List<V>>() {
    };
  }

  public abstract String getContentType();

  public byte[] serializeOne(V item) {
    if (item == null) {
      return new byte[]{};
    }

    try {
      return objectMapper.writeValueAsBytes(item);
    } catch (JsonProcessingException e) {
      throw ExceptionTools.checkedToRuntime(e);
    }
  }

  public byte[] serializeMany(List<V> items) {
    if (items == null) {
      return new byte[]{};
    }

    try {
      return objectMapper.writeValueAsBytes(items);
    } catch (JsonProcessingException e) {
      throw ExceptionTools.checkedToRuntime(e);
    }
  }

  public V deserializeOne(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }

    try {
      return objectMapper.readValue(bytes, itemClass);
    } catch (IOException e) {
      throw ExceptionTools.checkedToRuntime(e);
    }
  }

  public List<V> deserializeMany(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return ImmutableList.of();
    }

    try {
      return objectMapper.readValue(bytes, listTypeReference);
    } catch (IOException e) {
      throw ExceptionTools.checkedToRuntime(e);
    }
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }
}
