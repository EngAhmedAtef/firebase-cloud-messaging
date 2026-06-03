package io.github.engahmedatef.fcm.internal.observability;

import io.micrometer.observation.Observation;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FcmSendObservationContext extends Observation.Context {
    private final String projectId;
    private final String targetType;
}
