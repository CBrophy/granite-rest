package org.granite.rest.model;

import org.granite.rest.handler.SubListResponse;

import java.util.Map;

public interface ItemProvider<K, V> {

    V getOne(final K key, final RequestContext requestContext);

    SubListResponse<V> getMany(final Map<String, String> propertyFilter, final RequestContext requestContext);

    UpdateResult<K> insert(final V item, final RequestContext requestContext);

    UpdateResult<K> update(final K key, final V item, final RequestContext requestContext);

    UpdateResult<K> delete(final K key, final RequestContext requestContext);

    Class<V> getItemClass();
}
