package org.granite.rest.service;

import com.google.common.base.Throwables;

import org.granite.log.LogTools;
import org.granite.rest.ExtendedHeader;
import org.granite.rest.Response;
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
    private final Function<String, Boolean> requestIdValidationFunction;

    public InboundRequestHandler(
            final Function<RequestContext, RequestHandler> handlerFromContextFunction
    ) {
        this(handlerFromContextFunction, requestId -> true);
    }


    public InboundRequestHandler(
            final Function<RequestContext, RequestHandler> handlerFromContextFunction,
            final Function<String, Boolean> requestIdValidationFunction
    ) {
        super(true);

        this.handlerFromContextFunction = checkNotNull(handlerFromContextFunction,
                                                       "handlerFromContextFunction");
        this.requestIdValidationFunction = checkNotNull(requestIdValidationFunction,
                                                        "requestIdValidationFunction");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                HttpRequest httpRequest) throws Exception {

        RESTService.incrementRequestCount();

        RESTService.incrementCounter(httpRequest.getMethod().name());

        try {
            HttpResponse httpResponse = null;

            try {
                final RequestContext requestContext = new RequestContext(httpRequest);

                if (!requestIdValidationFunction.apply(
                        requestContext
                                .getHttpHeaders()
                                .get(ExtendedHeader.RequestId.getHeaderKey())
                )) {

                    httpResponse = Response.FORBIDDEN();

                } else {

                    httpResponse = dispatchRequest(
                            httpRequest.getMethod(),
                            requestContext,
                            handlerFromContextFunction.apply(requestContext));

                    // If these numbers are being reported, the current
                    // request should not corrupt the response generated
                    // by the handler
                    RESTService.setLastRequestTime();
                }

            } catch (Exception ignored) {

                LogTools.error(Throwables.getStackTraceAsString(ignored));

                httpResponse = Response.INTERNAL_ERROR();

            }

            if (httpResponse == null) {
                httpResponse = Response.NOT_FOUND();
            }

            if (HttpHeaders.isKeepAlive(httpRequest)) {
                httpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }

            if (HttpHeaders.is100ContinueExpected(httpRequest)) {
                ctx.writeAndFlush(Response.CONTINUE());
            }

            RESTService.incrementResponseCount();

            RESTService.incrementCounter(String.valueOf(httpResponse.getStatus()));

            ctx.writeAndFlush(httpResponse);
        } catch (Exception ignored) {
            LogTools.error(Throwables.getStackTraceAsString(ignored));

            RESTService.incrementHiddenErrorCount();
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

        return Response.METHOD_NOT_ALLOWED();

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
