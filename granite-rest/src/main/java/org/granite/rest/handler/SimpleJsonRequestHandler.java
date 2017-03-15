package org.granite.rest.handler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.granite.base.StringTools;
import org.granite.collections.ListTools;
import org.granite.log.LogTools;
import org.granite.rest.ExtendedHeader;
import org.granite.rest.Response;
import org.granite.rest.handler.serialzation.ContentTypeSerializer;
import org.granite.rest.handler.serialzation.JsonSerializer;
import org.granite.rest.model.ItemProvider;
import org.granite.rest.model.RequestContext;
import org.granite.rest.model.RequestHandler;
import org.granite.rest.model.UpdateResult;

public abstract class SimpleJsonRequestHandler<K extends Comparable, V extends Comparable> implements
    RequestHandler {

    private final static String SORT_FIELD = "_sortField";
    private final static String SORT_DIR = "_sortDir";
    private final static String PAGE = "_page";
    private final static String PER_PAGE = "_perPage";
    private final static String FILTERS = "_filters";
    private final static String SORT_DIR_ASC = "ASC";
    private final HashMap<String, ContentTypeSerializer<V>> contentTypeSerializers = new HashMap<>();

    private final ItemProvider<K, V> itemProvider;
    private final JsonSerializer<V> defaultSerializer;

    private int defaultPerPage = 30;

    protected SimpleJsonRequestHandler(
        final ItemProvider<K, V> itemProvider
    ) {
        this(itemProvider, ImmutableList.of());
    }

    protected SimpleJsonRequestHandler(
        final ItemProvider<K, V> itemProvider,
        final List<ContentTypeSerializer<V>> serializers
    ) {
        checkNotNull(serializers, "serializers");

        this.itemProvider = checkNotNull(itemProvider, "itemProvider");

        this.defaultSerializer =
            new JsonSerializer<>(
                checkNotNull(itemProvider, "itemProvider").getItemClass()
            );

        for (ContentTypeSerializer<V> serializer : serializers) {
            if (!defaultSerializer.getContentType().equalsIgnoreCase(serializer.getContentType())) {
                contentTypeSerializers.put(serializer.getContentType(), serializer);
            }
        }

    }

    @Override
    public HttpResponse handleGet(RequestContext requestContext) {
        final K key = keyFromRequestPath(requestContext.getRequestPath());

        final ContentTypeSerializer<V> serializer = findSerializer(ContentType.fromString(
            requestContext.getHttpHeaders().get(HttpHeaders.Names.ACCEPT)));

        if (key != null) {
            final V item = itemProvider.getOne(key, requestContext);

            if (item != null) {
                return Response.createResponse(
                    serializer.serializeOne(item),
                    HttpResponseStatus.OK,
                    serializer.getContentType()
                );
            }
        } else {

            final SubListResponse<V> items = itemProvider.getMany(
                getPropertyFilter(requestContext),
                requestContext);

            if (items != null) {

                final DefaultHttpResponse response = Response.createResponse(
                    serializer.serializeMany(
                        sortAndPage(items.getResponseValues(), requestContext)
                    ),
                    HttpResponseStatus.OK,
                    serializer.getContentType()
                );

                ExtendedHeader.setHeader(
                    response,
                    ExtendedHeader.TotalCount,
                    items.getTotalCount()
                );

                return response;
            }
        }

        return Response.NOT_FOUND();
    }

    private List<V> sortAndPage(final List<V> items, final RequestContext requestContext) {
        if (items == null || items.isEmpty()) {
            return ImmutableList.of();
        }

        if (items.size() > 1) {
            boolean sortDescending = getSortDescending(requestContext);

            items.sort(sortDescending ? Ordering.natural().reverse() : Ordering.natural());
        }

        final Integer pageNum = getPageNum(requestContext);

        final Integer itemsPerPage = getItemsPerPage(requestContext);

        if (pageNum != null) {
            return ListTools.sublistPaging(
                items,
                itemsPerPage,
                pageNum
            );
        } else {
            return items;
        }
    }

    @Override
    public HttpResponse handlePost(RequestContext requestContext) {
        final V item = deserializeRequestBody(
            requestContext.getRequestBody(),
            ContentType.fromString(
                requestContext.getHttpHeaders().get(HttpHeaders.Names.CONTENT_TYPE))
        );

        if (item == null) {
            return Response.BAD_REQUEST();
        }

        final UpdateResult<K> result = itemProvider.insert(item, requestContext);

        checkState(result.isSuccessful(), result.getMessage());

        return Response.NO_CONTENT();
    }

    @Override
    public HttpResponse handlePut(RequestContext requestContext) {
        final K key = keyFromRequestPath(requestContext.getRequestPath());

        final V item = deserializeRequestBody(
            requestContext.getRequestBody(),
            ContentType.fromString(
                requestContext.getHttpHeaders().get(HttpHeaders.Names.CONTENT_TYPE))
        );

        if (item == null) {
            return Response.BAD_REQUEST();
        }

        final UpdateResult<K> result = itemProvider.update(key, item, requestContext);

        if (!result.keyExists()) {
            return Response.NOT_FOUND();
        }

        checkState(result.isSuccessful(), result.getMessage());

        return Response.NO_CONTENT();
    }

    @Override
    public HttpResponse handleDelete(RequestContext requestContext) {

        final K key = keyFromRequestPath(requestContext.getRequestPath());

        if (key == null) {
            return Response.BAD_REQUEST();
        }

        final UpdateResult<K> result = itemProvider.delete(key, requestContext);

        if (!result.keyExists()) {
            return Response.NOT_FOUND();
        }

        checkState(result.isSuccessful(), result.getMessage());

        return Response.NO_CONTENT();
    }

    private HashMap<String, ContentTypeSerializer<V>> getContentTypeSerializers() {
        return contentTypeSerializers;
    }

    public SimpleJsonRequestHandler<K, V> withDefaultPerPage(final int perPage) {
        checkArgument(perPage > 0, "defaultPerPage should be a positive number");
        this.defaultPerPage = perPage;
        return this;
    }

    public int getDefaultPerPage() {
        return defaultPerPage;
    }

    public JsonSerializer<V> getDefaultSerializer() {
        return defaultSerializer;
    }

    protected abstract K keyFromRequestPath(final List<String> keyString);

    private String getSortField(final RequestContext requestContext) {
        checkNotNull(requestContext, "requestContext");

        final ImmutableCollection<String> sortFieldParameters = requestContext
            .getQueryStringParameters()
            .get(SORT_FIELD);

        if (sortFieldParameters == null || sortFieldParameters.isEmpty()) {
            return null;
        }

        return Iterables.getFirst(sortFieldParameters, null);
    }

    private boolean getSortDescending(final RequestContext requestContext) {
        checkNotNull(requestContext, "requestContext");

        final ImmutableCollection<String> sortDirectionParameters = requestContext
            .getQueryStringParameters()
            .get(SORT_DIR);

        // default to ascending
        return !(sortDirectionParameters == null ||
            SORT_DIR_ASC.equalsIgnoreCase(Iterables.getFirst(
                sortDirectionParameters,
                SORT_DIR_ASC)));
    }

    private Integer getItemsPerPage(final RequestContext requestContext) {
        checkNotNull(requestContext, "requestContext");

        final ImmutableCollection<String> perPageParameters = requestContext
            .getQueryStringParameters()
            .get(PER_PAGE);

        if (perPageParameters == null || perPageParameters.isEmpty()) {
            return defaultPerPage;
        }

        final String perPageString = Iterables.getFirst(perPageParameters, "");

        final Integer perPage = Ints.tryParse(perPageString == null ? "" : perPageString);

        return perPage == null || perPage < 1 ? defaultPerPage : perPage;
    }

    private Integer getPageNum(final RequestContext requestContext) {
        checkNotNull(requestContext, "requestContext");

        final ImmutableCollection<String> pageParameters = requestContext
            .getQueryStringParameters()
            .get(PAGE);

        if (pageParameters == null || pageParameters.isEmpty()) {
            return null;
        }

        final String pageString = Iterables.getFirst(pageParameters, null);

        return pageString == null ? null : Ints.tryParse(pageString);
    }

    private ImmutableMap<String, String> getPropertyFilter(final RequestContext requestContext) {
        checkNotNull(requestContext, "requestContext");

        ImmutableCollection<String> filtersParameters = requestContext
            .getQueryStringParameters()
            .get(FILTERS);

        if (filtersParameters == null || filtersParameters.isEmpty()) {
            return ImmutableMap.of();
        }

        final String filterJson = Iterables.getFirst(filtersParameters, "");

        if (StringTools.isNullOrEmpty(filterJson)) {
            return ImmutableMap.of();
        }

        try {
            final Map filterMap = getDefaultSerializer()
                .getObjectMapper()
                .readValue(filterJson, Map.class);

            final HashMap<String, String> results = new HashMap<>();

            for (Object keyObject : filterMap.keySet()) {

                if (keyObject instanceof String) {

                    final String key = ((String) keyObject).trim().toLowerCase();

                    if (key.isEmpty()) {
                        continue;
                    }

                    results.put(key, String.valueOf(filterMap.get(keyObject)));
                }
            }

            return ImmutableMap.copyOf(results);

        } catch (IOException e) {
            LogTools.warn("Failed to deserialize filter: {0}", filterJson);
        }

        return ImmutableMap.of();
    }


    private V deserializeRequestBody(final Object requestBody,
        final ContentType contentType) {
        if (requestBody == null) {
            return null;
        }

        final ContentTypeSerializer<V> serializer = findSerializer(contentType);

        try {
            if (requestBody instanceof String) {

                return serializer.deserializeOne(((String) requestBody).getBytes());

            }

            if (requestBody instanceof byte[]) {
                return serializer.deserializeOne((byte[]) requestBody);
            }

            LogTools.info("Content type {0} not supported",
                contentType.getText());

        } catch (Exception e) {
            LogTools.error("Error deserializing reqest: {0}", Throwables.getStackTraceAsString(e));
        }

        return null;
    }

    private ContentTypeSerializer<V> findSerializer(final ContentType contentType) {
        return getContentTypeSerializers()
            .getOrDefault(contentType.getText(),
                getDefaultSerializer());

    }

    @Override
    public boolean isHealthCheck(RequestContext requestContext) {
        return requestContext.getRequestPath().size() > 1 &&
            "health-check".equalsIgnoreCase(requestContext.getRequestPath().get(1));
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

    protected abstract void doHealthCheck(final RequestContext requestContext);
}
