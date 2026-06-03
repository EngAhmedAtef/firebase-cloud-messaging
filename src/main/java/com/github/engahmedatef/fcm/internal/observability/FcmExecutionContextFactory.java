package com.github.engahmedatef.fcm.internal.observability;

import com.github.engahmedatef.fcm.internal.transport.target.FcmTarget;

public interface FcmExecutionContextFactory {

    FcmExecutionContext create(String projectId, FcmTarget target);

    FcmExecutionContext create(String projectId, FcmTarget target, FcmExecutionContext parentContext);
}
