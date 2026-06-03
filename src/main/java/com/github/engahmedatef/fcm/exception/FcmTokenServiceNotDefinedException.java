package com.github.engahmedatef.fcm.exception;

/**
 * Thrown when {@link com.github.engahmedatef.fcm.api.FcmClient#sendToUser} or
 * {@link com.github.engahmedatef.fcm.api.ReactiveFcmClient#sendToUser} is called but no
 * {@link com.github.engahmedatef.fcm.api.FcmTokenService} bean is registered.
 * Register an {@code FcmTokenService} bean to enable user-scoped sends.
 */
public class FcmTokenServiceNotDefinedException extends FcmException {

    public FcmTokenServiceNotDefinedException(String message) {
        super(message);
    }

    public FcmTokenServiceNotDefinedException(String message, Throwable cause) {
        super(message, cause);
    }
}
