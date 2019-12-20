package org.granite.rest.handler;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import org.granite.log.LogTools;
import org.granite.rest.Response;
import org.granite.rest.handler.serialzation.ContentTypeSerializer;
import org.granite.rest.handler.serialzation.JsonSerializer;
import org.granite.rest.model.RequestContext;
import org.granite.rest.model.RequestHandler;

public abstract class SimpleRequestHandler<V> implements RequestHandler {

  private final ImmutableList<ContentTypeSerializer<V>> serializers;
  private final JsonSerializer<V> defaultSerializer;
  private final ImmutableMap<String, ContentTypeSerializer<V>> contentTypeSerializers;


  public SimpleRequestHandler(
      final List<ContentTypeSerializer<V>> serializers) {
    checkNotNull(serializers, "serializers");

    this.serializers = ImmutableList.copyOf(serializers);

    this.defaultSerializer =
        new JsonSerializer<>(getItemClass());

    final ImmutableMap.Builder<String, ContentTypeSerializer<V>> builder = ImmutableMap.builder();

    for (ContentTypeSerializer<V> serializer : serializers) {
      if (!defaultSerializer.getContentType().equalsIgnoreCase(serializer.getContentType())) {
        builder.put(serializer.getContentType(), serializer);
      }
    }

    contentTypeSerializers = builder.build();
  }

  protected abstract TypeReference<V> getItemClass();

  protected JsonSerializer<V> getDefaultSerializer() {
    return defaultSerializer;
  }

  protected ImmutableMap<String, ContentTypeSerializer<V>> getContentTypeSerializers() {
    return contentTypeSerializers;
  }

  protected ImmutableList<ContentTypeSerializer<V>> getSerializers() {
    return serializers;
  }

  private ContentTypeSerializer<V> findSerializer(final ContentType contentType) {
    return getContentTypeSerializers()
        .getOrDefault(contentType.getText(),
            getDefaultSerializer());

  }

  protected abstract V createGetItem(RequestContext requestContext);

  @Override
  public HttpResponse handleGet(RequestContext requestContext) {
    final ContentTypeSerializer<V> serializer = findSerializer(ContentType.fromString(
        requestContext.getHttpHeaders().get(HttpHeaderNames.ACCEPT)));

    final V item = createGetItem(requestContext);

    if (item != null) {
      return Response.createResponse(
          serializer.serializeOne(item),
          HttpResponseStatus.OK,
          serializer.getContentType()
      );
    }

    return Response.NOT_FOUND();

  }

  protected abstract HttpResponse consumePostBody(final V item);

  @Override
  public HttpResponse handlePost(RequestContext requestContext) {
    final ContentType contentType = RequestContextTools.findContentType(requestContext);

    final ContentTypeSerializer<V> serializer = findSerializer(contentType);

    final V item = RequestContextTools.deserializeRequestBody(
        requestContext,
        serializer
    );

    if (item == null) {
      return Response.BAD_REQUEST();
    }

    return consumePostBody(item);

  }

  protected abstract HttpResponse consumePutBody(final V item);

  @Override
  public HttpResponse handlePut(RequestContext requestContext) {
    final ContentType contentType = RequestContextTools.findContentType(requestContext);

    final ContentTypeSerializer<V> serializer = findSerializer(contentType);

    final V item = RequestContextTools.deserializeRequestBody(
        requestContext,
        serializer
    );

    if (item == null) {
      return Response.BAD_REQUEST();
    }

    return consumePutBody(item);

  }

  @Override
  public HttpResponse handleDelete(RequestContext requestContext) {
    return Response.NO_CONTENT();
  }

  @Override
  public HttpResponse isHealthy(RequestContext requestContext) {
    try {

      doHealthCheck(requestContext);

    } catch (Exception e) {
      LogTools.error(Throwables.getStackTraceAsString(e));
      return Response.INTERNAL_ERROR();
    }

    return Response.createResponse(
        "HEALTHY".getBytes(),
        HttpResponseStatus.OK,
        ContentType.TextPlain.getText()
    );
  }

  @Override
  public boolean isHealthCheck(RequestContext requestContext) {
    for (String pathPart : requestContext.getRequestPath().reverse()) {
      if("health-check".equalsIgnoreCase(pathPart)) {
        return true;
      }
    }
    return false;
  }

  protected abstract void doHealthCheck(final RequestContext requestContext);

}
