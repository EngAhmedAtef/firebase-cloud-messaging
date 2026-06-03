package com.github.engahmedatef.fcm.exception;

/**
 * Thrown when the SDK cannot find the resource specified by {@code com.github.engahmedatef.fcm.auth.json-path}
 * when using {@code com.github.engahmedatef.fcm.auth.type=SERVICE_ACCOUNT_JSON}
 */
public class FcmResourceNotFoundException extends FcmException {

    public FcmResourceNotFoundException(String message) {
        super(message);
    }

    public FcmResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
