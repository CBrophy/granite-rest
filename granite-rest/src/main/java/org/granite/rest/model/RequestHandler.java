package org.granite.rest.model;

import io.netty.handler.codec.http.HttpResponse;

public interface RequestHandler {

    HttpResponse handleGet(final RequestContext requestContext);
    HttpResponse handlePost(final RequestContext requestContext);
    HttpResponse handlePut(final RequestContext requestContext);
    HttpResponse handleDelete(final RequestContext requestContext);
    String getRootPath();
}
