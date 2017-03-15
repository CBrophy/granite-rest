package org.granite.rest.handler;

import java.util.List;

public class SubListResponse<V> {

    private final int totalCount;
    private final List<V> responseValues;

    public SubListResponse(int totalCount, List<V> responseValues) {
        this.totalCount = totalCount;
        this.responseValues = responseValues;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<V> getResponseValues() {
        return responseValues;
    }
}
