package org.granite.rest.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.time.Clock;
import java.util.List;

public class RequestContext {

    private final ImmutableList<String> requestPath;
    private final long timestamp;
    private final ImmutableMultimap<String, String> queryStringParameters;
    private final HttpHeaders httpHeaders;
    private final byte[] requestBody;

    public RequestContext(
        final HttpRequest httpRequest
    ) {
        checkNotNull(httpRequest, "httpRequest");
        this.requestPath = extractRequestPath(httpRequest);
        this.queryStringParameters = extractQueryString(httpRequest);
        this.httpHeaders =
            httpRequest.headers() == null ? new DefaultHttpHeaders() : httpRequest.headers();
        this.requestBody = extractRequestBody(httpRequest);
        this.timestamp = Clock.systemUTC().millis();
    }

    public ImmutableList<String> getRequestPath() {
        return requestPath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getRequestBody() {
        return requestBody;
    }

    public ImmutableMultimap<String, String> getQueryStringParameters() {
        return queryStringParameters;
    }

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    private static ImmutableList<String> extractRequestPath(final HttpRequest httpRequest) {
        checkNotNull(httpRequest, "httpRequest");

        final Splitter slashSplitter = Splitter.on('/').omitEmptyStrings().trimResults();

        final Splitter questionSplitter = Splitter.on('?').omitEmptyStrings().trimResults();

        final ImmutableList.Builder<String> builder = ImmutableList.builder();

        final List<String> uriParts = questionSplitter.splitToList(httpRequest.getUri());

        if (uriParts.isEmpty()) {
            return ImmutableList.of();
        }

        slashSplitter
            .splitToList(uriParts.get(0))
            .stream()
            .map(String::toLowerCase)
            .forEach(builder::add);

        return builder.build();
    }


    protected static byte[] extractRequestBody(final HttpRequest httpRequest) {
        checkNotNull(httpRequest, "httpRequest");

        if (httpRequest instanceof FullHttpRequest) {
            if (((FullHttpRequest) httpRequest).content().hasArray()) {
                return ((FullHttpRequest) httpRequest).content().array();
            }

            final byte[] buf = new byte[((FullHttpRequest) httpRequest).content().readableBytes()];

            ((FullHttpRequest) httpRequest).content().readBytes(buf);

            return buf;
        }

        return new byte[]{};
    }

    private static ImmutableMultimap<String, String> extractQueryString(
        final HttpRequest httpRequest) {
        checkNotNull(httpRequest, "httpRequest");

        final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();

        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.getUri());

        queryStringDecoder
            .parameters()
            .keySet()
            .forEach(
                key -> builder.putAll(key.toLowerCase(), queryStringDecoder.parameters().get(key)));

        return builder.build();
    }
}
