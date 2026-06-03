package io.github.engahmedatef.fcm.internal.observability;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

public class FcmSendObservationConvention implements ObservationConvention<FcmSendObservationContext> {
    public static final String DEFAULT_NAME = "fcm.send";

    @Override
    public String getName() {
        return DEFAULT_NAME;
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof FcmSendObservationContext;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(FcmSendObservationContext context) {
        return KeyValues.of(
                "projectId", context.getProjectId(),
                "targetType", context.getTargetType()
        );
    }
}
