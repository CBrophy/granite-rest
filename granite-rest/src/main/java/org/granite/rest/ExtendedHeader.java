package org.granite.rest;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import static com.google.common.base.Preconditions.checkNotNull;

public enum ExtendedHeader {
    TotalCount("X-Total-Count"),
    ApiKey("x-api-key"),
    RequestId("X-Request-ID");

    final String headerKey;

    ExtendedHeader(String headerKey) {
        this.headerKey = headerKey;
    }

    public String getHeaderKey() {
        return headerKey;
    }

    public static Object getHeaderValue(final HttpRequest httpRequest,
                                        final ExtendedHeader extendedHeader) {
        checkNotNull(httpRequest, "httpRequest");
        checkNotNull(extendedHeader, "extendedHeader");
        return httpRequest.headers().get(extendedHeader.getHeaderKey());
    }

    public static void setHeader(final HttpResponse httpResponse,
                                 final ExtendedHeader extendedHeader,
                                 final Object value) {
        checkNotNull(httpResponse, "httpResponse");
        checkNotNull(extendedHeader, "extendedHeader");
        checkNotNull(value, "value");
        httpResponse
                .headers()
                .add(extendedHeader.getHeaderKey(), value);

    }
}
