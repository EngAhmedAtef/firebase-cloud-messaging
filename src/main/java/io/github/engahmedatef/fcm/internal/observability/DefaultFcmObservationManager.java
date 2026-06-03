package io.github.engahmedatef.fcm.internal.observability;

import io.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import io.micrometer.observation.Observation;

/** Default {@link ObservationManager} that delegates lifecycle events to Micrometer {@link io.micrometer.observation.Observation}. */
public class DefaultFcmObservationManager implements ObservationManager {

    @Override
    public void start(FcmExecutionContext context) {
        context.getObservation().start();
    }

    @Override
    public void success(FcmExecutionContext context) {
        context.getObservation().lowCardinalityKeyValue(FcmMetrics.Tags.OUTCOME, "success");
    }

    @Override
    public void failure(FcmExecutionContext context, Throwable error) {
        context.getObservation().lowCardinalityKeyValue(FcmMetrics.Tags.OUTCOME, "failure");
        context.getObservation().error(error);
    }

    @Override
    public void retry(FcmExecutionContext context, Throwable error) {
        int retryCount = context.incrementRetryCount();
        context.getObservation().highCardinalityKeyValue(FcmMetrics.Tags.RETRY_COUNT, String.valueOf(retryCount));
        context.getObservation().event(Observation.Event.of("retry"));
    }

    @Override
    public void cancel(FcmExecutionContext context) {
        context.getObservation().lowCardinalityKeyValue(FcmMetrics.Tags.OUTCOME, "cancelled");
    }

    @Override
    public void stop(FcmExecutionContext context) {
        context.getObservation().stop();
    }
}
