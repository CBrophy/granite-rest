package org.granite.rest.handler;

public enum ContentType {
    TextPlain("text/plain"),
    ApplicationJson("application/json"),
    ApplicationMsgPack("application/x-msgpack"),
    Unknown("");

    private String text;

    ContentType(final String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static ContentType fromString(final String contentTypeText) {
        if (contentTypeText == null) {
            return Unknown;
        }

        final String trimmed = contentTypeText.trim();

        for (ContentType contentType : ContentType.values()) {
            if (contentType.getText().equalsIgnoreCase(trimmed)) {
                return contentType;
            }
        }

        return Unknown;

    }
}
