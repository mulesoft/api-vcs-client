package org.mule.api.vcs.client.diff;

import java.util.Optional;

public class ApplyResult {
    private boolean success;
    private String message;

    private ApplyResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public static ApplyResult SUCCESSFUL = new ApplyResult(true, null);

    public static ApplyResult fail(String message) {
        return new ApplyResult(false, message);
    }
}
