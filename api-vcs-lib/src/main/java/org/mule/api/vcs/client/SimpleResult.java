package org.mule.api.vcs.client;

import java.util.Optional;

public class SimpleResult {
    private boolean success;
    private String message;

    private SimpleResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure(){
        return !isSuccess();
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public static SimpleResult SUCCESS = new SimpleResult(true, null);

    public static SimpleResult fail(String message) {
        return new SimpleResult(false, message);
    }
}
