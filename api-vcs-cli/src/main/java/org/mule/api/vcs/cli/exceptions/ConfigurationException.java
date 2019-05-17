package org.mule.api.vcs.cli.exceptions;

public class ConfigurationException extends RuntimeException {
    private String message;

    public ConfigurationException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
