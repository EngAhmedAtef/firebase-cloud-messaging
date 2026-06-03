package com.github.engahmedatef.fcm.api;

import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.model.FcmSendResult;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Reactive (non-blocking) FCM send API. This is the primary interface — all send logic lives here.
 * {@link FcmClient} is a thin blocking adapter over this interface.
 *
 * <p>All operations run on the Reactor scheduler configured by the underlying WebClient.
 * Thread-safe. Inject and reuse a single instance.
 */
public interface ReactiveFcmClient {

    /**
     * Sends a message to a single device.
     *
     * @param device  the target device (token + device type)
     * @param message the message payload
     * @return a {@link Mono} emitting the send result; errors are typed {@link com.github.engahmedatef.fcm.exception.FcmException} subtypes
     */
    Mono<FcmSendResult> sendToDevice(FcmDevice device, FcmMessage message);

    /**
     * Sends a message to multiple devices in concurrent batches.
     *
     * @param devices the target devices
     * @param message the message payload
     * @return a {@link Mono} that completes when all batches finish; errors with
     *         {@link com.github.engahmedatef.fcm.exception.FcmBatchExceededThresholdException} if the
     *         failure rate exceeds the configured threshold
     */
    Mono<Void> sendToDevices(Collection<FcmDevice> devices, FcmMessage message);

    /**
     * Sends a message to all registered devices of a user, resolved via {@link FcmTokenService}.
     *
     * @param userId  the application-defined user identifier
     * @param message the message payload
     * @return a {@link Mono} that completes when all devices have been attempted; errors with
     *         {@link com.github.engahmedatef.fcm.exception.FcmTokenServiceNotDefinedException} if no
     *         {@link FcmTokenService} bean is registered
     */
    Mono<Void> sendToUser(Object userId, FcmMessage message);

    /**
     * Sends a message to all devices subscribed to the given FCM topic.
     *
     * @param topic   FCM topic name (without the {@code /topics/} prefix)
     * @param message the message payload
     * @return a {@link Mono} emitting the send result
     */
    Mono<FcmSendResult> sendToTopic(String topic, FcmMessage message);

    /**
     * Sends a message to all devices matching a boolean topic condition expression.
     * See the <a href="https://firebase.google.com/docs/cloud-messaging/send-message#send_messages_to_topics">FCM documentation</a>
     * for condition syntax.
     *
     * @param condition FCM condition expression (e.g. {@code "'TopicA' in topics && 'TopicB' in topics"})
     * @param message   the message payload
     * @return a {@link Mono} emitting the send result
     */
    Mono<FcmSendResult> sendToCondition(String condition, FcmMessage message);
}
