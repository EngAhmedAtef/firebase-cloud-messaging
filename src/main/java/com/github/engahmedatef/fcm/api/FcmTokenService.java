package com.github.engahmedatef.fcm.api;

import com.github.engahmedatef.fcm.model.FcmDevice;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Optional SPI for resolving FCM device tokens from an application-level user identity.
 *
 * <p>When a bean of this type is registered, the SDK uses it to:
 * <ul>
 *   <li>Resolve all devices for a user in {@link ReactiveFcmClient#sendToUser} /
 *       {@link FcmClient#sendToUser}.</li>
 *   <li>Automatically remove stale tokens when FCM returns
 *       {@link com.github.engahmedatef.fcm.exception.FcmInvalidTokenException}.</li>
 * </ul>
 *
 * <p>If no bean is registered, {@code sendToUser()} throws
 * {@link com.github.engahmedatef.fcm.exception.FcmTokenServiceNotDefinedException} and stale token
 * cleanup is silently skipped.
 *
 * <p>Implementations must be non-blocking; all methods return reactive types.
 */
public interface FcmTokenService {

    /**
     * Retrieves all FCM devices associated with a specific user.
     *
     * @param userId the unique identifier of the user whose FCM devices should be retrieved
     * @return a {@link Mono} emitting a collection of {@link FcmDevice} for the given user
     */
    Mono<Collection<FcmDevice>> getUserDevices(Object userId);

    /**
     * Deletes the specified FCM token from the system.
     *
     * @param token the FCM token to be deleted
     * @return a {@link Mono} that completes when the token has been deleted
     */
    Mono<Void> deleteToken(String token);
}
