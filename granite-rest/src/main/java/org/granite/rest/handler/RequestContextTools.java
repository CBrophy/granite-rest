package org.granite.rest.handler;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.granite.log.LogTools;
import org.granite.rest.handler.serialzation.ContentTypeSerializer;
import org.granite.rest.model.RequestContext;

public class RequestContextTools {

  public static ContentType findContentType(final RequestContext requestContext) {
    checkNotNull(requestContext, "requestContext");

    return ContentType
        .fromString(
            requestContext
                .getHttpHeaders()
                .get(HttpHeaderNames.CONTENT_TYPE));
  }

  public static <V> V deserializeRequestBody(
      final RequestContext requestContext,
      final ContentTypeSerializer<V> serializer) {

    checkNotNull(requestContext, "requestContext");
    checkNotNull(serializer, "serializer");

    final byte[] requestBody = requestContext.getRequestBody();

    if (requestBody == null) {
      return null;
    }

    try {

      return serializer.deserializeOne(requestBody);

    } catch (Exception e) {
      LogTools.error("Error deserializing reqest: {0}", Throwables.getStackTraceAsString(e));
    }

    return null;
  }

}
