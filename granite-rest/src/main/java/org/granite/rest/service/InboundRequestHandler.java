package org.granite.rest.service;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;

import com.google.common.base.Throwables;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import java.util.function.Function;
import javax.net.ssl.SSLException;
import org.granite.log.LogTools;
import org.granite.rest.ExtendedHeader;
import org.granite.rest.Response;
import org.granite.rest.model.RequestContext;
import org.granite.rest.model.RequestHandler;

public class InboundRequestHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private final Function<RequestContext, RequestHandler> handlerFromContextFunction;
  private boolean muteSSLErrors = false;
  private final Function<String, Boolean> apiKeyValidationFunction;
  private boolean corsEnabled = false;

  public InboundRequestHandler(
      final Function<RequestContext, RequestHandler> handlerFromContextFunction
  ) {
    this(handlerFromContextFunction, requestId -> true);
  }


  public InboundRequestHandler(
      final Function<RequestContext, RequestHandler> handlerFromContextFunction,
      final Function<String, Boolean> apiKeyValidationFunction
  ) {
    super(true);

    this.handlerFromContextFunction = checkNotNull(handlerFromContextFunction,
        "handlerFromContextFunction");
    this.apiKeyValidationFunction = checkNotNull(apiKeyValidationFunction,
        "apiKeyValidationFunction");
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx,
      HttpRequest httpRequest) {

    RESTService.incrementRequestCount();

    RESTService.incrementCounter(httpRequest.method().name());

    try {
      HttpResponse httpResponse;

      try {

        logRequest(ctx, httpRequest);

        final RequestContext requestContext = new RequestContext(httpRequest);

        if (!apiKeyValidationFunction.apply(
            requestContext
                .getHttpHeaders()
                .get(ExtendedHeader.ApiKey.getHeaderKey())
        )) {

          httpResponse = Response.FORBIDDEN();

        } else {

          httpResponse = dispatchRequest(
              httpRequest.method(),
              requestContext,
              handlerFromContextFunction.apply(requestContext));

          // If these numbers are being reported, the current
          // request should not corrupt the response generated
          // by the handler
          RESTService.setLastRequestTime();
        }

      } catch (Exception e) {

        LogTools.error(Throwables.getStackTraceAsString(e));

        httpResponse = Response.INTERNAL_ERROR();

      }

      if (httpResponse == null) {
        httpResponse = Response.NOT_FOUND();
      }

      if (HttpUtil.isKeepAlive(httpRequest)) {
        httpResponse.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      }

      if (corsEnabled) {
        httpResponse.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      }

      if (HttpUtil.is100ContinueExpected(httpRequest)) {
        ctx.writeAndFlush(Response.CONTINUE());
      }

      final Object requestId = ExtendedHeader.getHeaderValue(
          httpRequest,
          ExtendedHeader.RequestId);

      if (requestId != null) {
        ExtendedHeader.setHeader(
            httpResponse,
            ExtendedHeader.RequestId,
            requestId
        );
      }

      RESTService.incrementResponseCount();

      RESTService.incrementCounter(String.valueOf(httpResponse.status()));

      ctx.writeAndFlush(httpResponse);
    } catch (Exception e) {
      LogTools.error(Throwables.getStackTraceAsString(e));

      RESTService.incrementHiddenErrorCount();
    }

  }

  protected void logRequest(
      ChannelHandlerContext channelHandlerContext,
      HttpRequest httpRequest) {
    LogTools.info("{0}\t{1}\t{2}",
        channelHandlerContext
            .channel()
            .remoteAddress(),
        httpRequest.method().name(),
        httpRequest.uri()
    );
  }

  private HttpResponse dispatchRequest(
      final HttpMethod httpMethod,
      final RequestContext requestContext,
      final RequestHandler requestHandler) {

    if (requestContext == null || requestHandler == null) {
      return null;
    }

    if (httpMethod == HttpMethod.GET) {

      if (requestHandler.isHealthCheck(requestContext)) {
        return requestHandler.isHealthy(requestContext);
      } else {
        return requestHandler.handleGet(requestContext);
      }

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
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

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

  public InboundRequestHandler withCorsEnabled(final boolean corsEnabled) {
    this.corsEnabled = corsEnabled;
    return this;
  }

  public boolean isCorsEnabled() {
    return corsEnabled;
  }

  public boolean muteSSLErrors() {
    return muteSSLErrors;
  }

  private boolean isInvalidSSLException(final Throwable cause) {
    return cause instanceof DecoderException && cause.getCause() instanceof SSLException;
  }
}
