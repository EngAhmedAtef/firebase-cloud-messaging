package io.github.engahmedatef.fcm.internal.observability;

import io.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import io.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import io.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import io.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import io.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import io.github.engahmedatef.fcm.model.FcmDevice;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DefaultFcmExecutionContextFactory implements FcmExecutionContextFactory {

    private final ObservationRegistry observationRegistry;
    private final FcmSendObservationConvention convention;

    @Override
    public FcmExecutionContext create(String projectId, FcmTarget target) {
        return create(projectId, target, null);
    }

    @Override
    public FcmExecutionContext create(String projectId, FcmTarget target, FcmExecutionContext parentContext) {
        String targetType = resolveTargetType(target);

        FcmSendObservationContext observationContext = new FcmSendObservationContext(projectId, targetType);
        Observation observation;
        FcmExecutionContext context;
        if (parentContext != null) {
            observation = Observation.createNotStarted(convention, () -> observationContext, observationRegistry)
                    .parentObservation(parentContext.getObservation());

            context = new FcmExecutionContext(parentContext.getCorrelationId(), projectId, targetType, observation);
        } else {
            observation = Observation.createNotStarted(convention, () -> observationContext, observationRegistry);
            context = new FcmExecutionContext(FcmCorrelationId.generate(), projectId, targetType, observation);
        }

        enrichTargetMetadata(context, target);
        return context;
    }

    private void enrichTargetMetadata(FcmExecutionContext context, FcmTarget target) {
        if (target instanceof DeviceTarget deviceTarget) {
            FcmDevice device = deviceTarget.device();

            context.putAttribute(FcmMetrics.Tags.TARGET, "device");
            context.putAttribute(FcmMetrics.Tags.DEVICE_TOKEN_PREFIX, device.token().substring(0, 6));
            context.putAttribute(FcmMetrics.Tags.DEVICE_TYPE, device.type());
        } else if (target instanceof TopicTarget topicTarget) {
            context.putAttribute(FcmMetrics.Tags.TARGET, "topic");
            context.putAttribute(FcmMetrics.Tags.TOPIC, topicTarget.topic());
        } else if (target instanceof ConditionTarget conditionTarget) {
            context.putAttribute(FcmMetrics.Tags.TARGET, "condition");
            context.putAttribute(FcmMetrics.Tags.CONDITION, conditionTarget.condition());
        } else {
            context.putAttribute(FcmMetrics.Tags.TARGET, "batch");
        }

        context.putAttribute(FcmMetrics.Tags.PROJECT_ID, context.getProjectId());
        context.putAttribute(FcmMetrics.Tags.CORRELATION_ID, context.getCorrelationId());
    }

    private String resolveTargetType(FcmTarget target) {
        if (target == null)
            return "batch";
        return target instanceof DeviceTarget ? "device"
                : target instanceof TopicTarget ? "topic"
                : target instanceof ConditionTarget ? "condition"
                : "unknown";
    }
}
