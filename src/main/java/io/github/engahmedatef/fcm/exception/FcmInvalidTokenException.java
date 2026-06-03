package io.github.engahmedatef.fcm.exception;

/**
 * Thrown when FCM rejects a device registration token as invalid or unregistered.
 * This typically means the app was uninstalled or the token was rotated.
 * If a {@link io.github.engahmedatef.fcm.api.FcmTokenService} bean is registered the SDK automatically removes the stale token.
 */
public class FcmInvalidTokenException extends FcmException {

    public FcmInvalidTokenException(String message) {
        super(message);
    }

    public FcmInvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
