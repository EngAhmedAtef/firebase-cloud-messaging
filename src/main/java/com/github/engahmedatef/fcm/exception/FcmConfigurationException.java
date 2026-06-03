package com.github.engahmedatef.fcm.exception;

/**
 * Thrown when there is a misconfiguration of {@link com.github.engahmedatef.fcm.autoconfigure.FcmProperties}
 */
public class FcmConfigurationException extends FcmException {

    public FcmConfigurationException(String message) {
        super(message);
    }

    public FcmConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
