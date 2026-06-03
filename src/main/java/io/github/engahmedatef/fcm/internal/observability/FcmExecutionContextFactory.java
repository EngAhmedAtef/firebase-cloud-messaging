package io.github.engahmedatef.fcm.internal.observability;

import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;

/** Internal factory for creating per-send {@link FcmExecutionContext} instances. */
public interface FcmExecutionContextFactory {

    FcmExecutionContext create(String projectId, FcmTarget target);

    FcmExecutionContext create(String projectId, FcmTarget target, FcmExecutionContext parentContext);
}
