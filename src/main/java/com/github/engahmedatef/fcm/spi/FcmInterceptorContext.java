package com.github.engahmedatef.fcm.spi;

import com.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import com.github.engahmedatef.fcm.model.FcmMessage;

import java.util.Map;

/**
 * Contextual metadata supplied to each {@link FcmPayloadInterceptor} invocation.
 *
 * @param target          the FCM send target (device, topic, or condition)
 * @param originalMessage the domain message before payload build (read-only reference)
 * @param projectId       the configured Firebase project ID
 * @param attributes      mutable map for passing data between interceptors in the same chain
 */
public record FcmInterceptorContext(
        FcmTarget target,
        FcmMessage originalMessage,
        String projectId,
        Map<String, Object> attributes
) {}
