package com.github.engahmedatef.fcm.exception;

/**
 * Thrown when FCM returns an error code not recognised by the SDK.
 * Inspect the message for the raw FCM error code.
 */
public class FcmUnknownException extends FcmException {

    public FcmUnknownException(String message) {
        super(message);
    }

    public FcmUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
