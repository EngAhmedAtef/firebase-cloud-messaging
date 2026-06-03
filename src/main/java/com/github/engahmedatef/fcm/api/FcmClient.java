package com.github.engahmedatef.fcm.api;

import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.model.FcmSendResult;

import java.util.Collection;

/**
 * Blocking variant of the FCM send API. All methods delegate to {@link ReactiveFcmClient}
 * and block until the underlying reactive pipeline completes.
 *
 * <p>Thread-safe. Inject and reuse a single instance.
 */
public interface FcmClient {

    /**
     * Sends a message to a single device.
     *
     * @param device  the target device (token + device type)
     * @param message the message payload
     * @return the send result containing the FCM message ID on success
     * @throws com.github.engahmedatef.fcm.exception.FcmInvalidTokenException   if the token is invalid or unregistered
     * @throws com.github.engahmedatef.fcm.exception.FcmAuthenticationException if authentication fails
     * @throws com.github.engahmedatef.fcm.exception.FcmUnavailableException    if FCM is temporarily unavailable
     */
    FcmSendResult sendToDevice(FcmDevice device, FcmMessage message);

    /**
     * Sends a message to multiple devices in batches. Each batch is processed concurrently up to
     * the configured {@code send.concurrency} limit.
     *
     * @param devices the target devices
     * @param message the message payload
     * @throws com.github.engahmedatef.fcm.exception.FcmBatchExceededThresholdException if the failure rate
     *         exceeds {@code send.failure-threshold-per-batch}
     */
    void sendToDevices(Collection<FcmDevice> devices, FcmMessage message);

    /**
     * Sends a message to all devices associated with a user, resolved via {@link FcmTokenService}.
     *
     * @param userId  the application-defined user identifier
     * @param message the message payload
     * @throws com.github.engahmedatef.fcm.exception.FcmTokenServiceNotDefinedException if no
     *         {@link FcmTokenService} bean is registered
     */
    void sendToUser(Object userId, FcmMessage message);
}
