package io.github.engahmedatef.fcm.exception;

/**
 * Thrown when the SDK cannot obtain or use a valid OAuth2 access token.
 * Common causes: missing Application Default Credentials, an expired service account key,
 * incorrect scopes, or clock skew exceeding token validity.
 */
public class FcmAuthenticationException extends FcmException {

    /** @param message description of the authentication failure */
    public FcmAuthenticationException(String message) {
        super(message);
    }

    /**
     * @param message description of the authentication failure
     * @param cause   the underlying cause
     */
    public FcmAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
