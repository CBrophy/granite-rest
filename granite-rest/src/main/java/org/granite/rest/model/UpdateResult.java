package org.granite.rest.model;

public class UpdateResult<K> {

    private final K updateKey;
    private final boolean keyExists;
    private final boolean successful;
    private final String message;

    public UpdateResult(K updateKey, boolean keyExists, boolean successful, String message) {
        this.updateKey = updateKey;
        this.keyExists = keyExists;
        this.successful = successful;
        this.message = message;
    }

    public K getUpdateKey() {
        return updateKey;
    }

    public boolean keyExists() {
        return keyExists;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}
