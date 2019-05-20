package org.mule.api.vcs.client;

import java.util.Optional;

public class ValueResult<T> {
    private boolean success;
    private String message;
    private T value;

    public ValueResult(boolean success, String message, T value) {
        this.success = success;
        this.message = message;
        this.value = value;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public T doGetValue() {
        return value;
    }

    public static <T> ValueResult<T> success(T value) {
        return new ValueResult<T>(true, null, value);
    }

    public <T> ValueResult<T> asFailure() {
        return fail(message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public static ValueResult<Void> SUCCESS = new ValueResult<Void>(true, null, null);

    public static <T> ValueResult<T> fail(String message) {
        return new ValueResult<T>(false, message, null);
    }
}
