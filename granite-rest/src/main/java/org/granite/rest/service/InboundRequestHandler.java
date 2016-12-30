package org.granite.rest.service;

import com.google.common.base.Throwables;

import org.granite.log.LogTools;
import org.granite.rest.RESTTools;
import org.granite.rest.model.RequestContext;
import org.granite.rest.model.RequestHandler;

import java.util.function.Function;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;

public class InboundRequestHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private final Function<RequestContext, RequestHandler> handlerFromContextFunction;
    private boolean muteSSLErrors = false;

    public InboundRequestHandler(
            final Function<RequestContext, RequestHandler> handlerFromContextFunction
    ) {
        super(true);

        this.handlerFromContextFunction = checkNotNull(handlerFromContextFunction,
                                                       "handlerFromContextFunction");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                HttpRequest httpRequest) throws Exception {

        try {
            HttpResponse httpResponse = null;

            try {
                final RequestContext requestContext = new RequestContext(httpRequest);

                httpResponse = dispatchRequest(
                        httpRequest.getMethod(),
                        requestContext,
                        handlerFromContextFunction.apply(requestContext));

            } catch (Exception ignored) {

                LogTools.error(Throwables.getStackTraceAsString(ignored));

                httpResponse = RESTTools.INTERNAL_ERROR_RESPONSE;
            }

            if (httpResponse == null) {
                httpResponse = RESTTools.NOT_FOUND_RESPONSE;
            }

            if (HttpHeaders.isKeepAlive(httpRequest)) {
                httpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }

            if (HttpHeaders.is100ContinueExpected(httpRequest)) {
                ctx.write(RESTTools.CONTINUE_RESPONSE);
            }

            ctx.writeAndFlush(httpResponse);
        } catch (Exception ignored) {
            LogTools.error(Throwables.getStackTraceAsString(ignored));
        }

    }

    private HttpResponse dispatchRequest(
            final HttpMethod httpMethod,
            final RequestContext requestContext,
            final RequestHandler requestHandler) {

        if (requestContext == null || requestHandler == null) return null;

        if (httpMethod == HttpMethod.GET) {
            return requestHandler.handleGet(requestContext);
        }

        if (httpMethod == HttpMethod.POST) {
            return requestHandler.handlePost(requestContext);
        }

        if (httpMethod == HttpMethod.PUT) {
            return requestHandler.handlePut(requestContext);
        }

        if (httpMethod == HttpMethod.DELETE) {
            return requestHandler.handleDelete(requestContext);
        }

        return RESTTools.METHOD_NOT_ALLOWED_RESPONSE;

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        super.channelReadComplete(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        // Log all exceptions except for "invalid ssl cert" exceptions
        if (!(muteSSLErrors && isInvalidSSLException(cause))) {

            LogTools.error(Throwables.getStackTraceAsString(cause));

            if (cause.getCause() != null) {
                LogTools.error(Throwables.getStackTraceAsString(cause.getCause()));
            }
        }

        ctx.close();
    }

    public InboundRequestHandler withMuteSSLErrors(final boolean muteSSLErrors) {
        this.muteSSLErrors = muteSSLErrors;
        return this;
    }

    public boolean muteSSLErrors() {
        return muteSSLErrors;
    }

    private boolean isInvalidSSLException(final Throwable cause) {
        return cause instanceof DecoderException && cause.getCause() instanceof SSLException;
    }
}
