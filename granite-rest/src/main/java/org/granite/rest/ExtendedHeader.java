package org.granite.rest;

public enum ExtendedHeader {
    TotalCount("X-Total-Count"),
    RequestId("X-Request-ID");

    final String headerKey;

    ExtendedHeader(String headerKey) {
        this.headerKey = headerKey;
    }

    public String getHeaderKey() {
        return headerKey;
    }
}
