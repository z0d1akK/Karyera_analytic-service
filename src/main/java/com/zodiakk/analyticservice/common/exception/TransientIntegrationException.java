package com.zodiakk.analyticservice.common.exception;

public class TransientIntegrationException extends RuntimeException {

    public TransientIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
