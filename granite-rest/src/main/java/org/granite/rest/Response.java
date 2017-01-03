package org.granite.rest;

import org.granite.rest.handler.ContentType;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_IMPLEMENTED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Response {

    public static HttpResponse CONTINUE(){
        return new DefaultFullHttpResponse(HTTP_1_1,CONTINUE);
    }

    public static HttpResponse METHOD_NOT_ALLOWED(){
        return  createResponse(
                "NOT ALLOWED".getBytes(),
                METHOD_NOT_ALLOWED,
                ContentType.TextPlain
                        .getText());
    }

    public static HttpResponse FORBIDDEN(){
        return  createResponse(
                "FORBIDDEN".getBytes(),
                FORBIDDEN,
                ContentType.TextPlain
                        .getText());
    }

    public static HttpResponse INTERNAL_ERROR(){
        return createResponse(
                "INTERNAL ERROR".getBytes(),
                INTERNAL_SERVER_ERROR,
                ContentType.TextPlain
                        .getText());
    }

    public static HttpResponse NOT_IMPLEMENTED(){
        return createResponse(
                "NOT IMPLEMENTED".getBytes(),
                NOT_IMPLEMENTED,
                ContentType.TextPlain
                        .getText());
    }

    public static HttpResponse NOT_FOUND(){
        return createResponse(
                "NOT FOUND".getBytes(),
                NOT_FOUND,
                ContentType.TextPlain.getText());
    }

    public static HttpResponse BAD_REQUEST(){
        return createResponse(
                "BAD REQUEST".getBytes(),
                BAD_REQUEST,
                ContentType.TextPlain.getText());
    }

    public static HttpResponse NO_CONTENT(){
        return createResponse(
                new byte[]{},
                NO_CONTENT,
                ContentType.TextPlain.getText());
    }

    public static DefaultHttpResponse createResponse(
            final byte[] responseBody,
            final HttpResponseStatus httpResponseStatus,
            final String contentType) {
        checkNotNull(responseBody, "responseBody");
        checkNotNull(httpResponseStatus, "httpResponseStatus");
        checkNotNull(contentType, "contentType");

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1,
                httpResponseStatus,
                Unpooled.wrappedBuffer(responseBody));

        response.headers().set(CONTENT_TYPE, contentType);

        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

        return response;
    }
}
