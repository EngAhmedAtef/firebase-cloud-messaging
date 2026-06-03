package io.github.engahmedatef.fcm.unit;

import io.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import io.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import io.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import io.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatNoException;

class NoOpFcmMetricsRecorderTest {

    private final NoOpFcmMetricsRecorder recorder = new NoOpFcmMetricsRecorder();

    private FcmExecutionContext context() {
        var factory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());
        return factory.create("test-project", null);
    }

    @Test
    void allMethods_doNotThrow() {
        FcmExecutionContext ctx = context();

        assertThatNoException().isThrownBy(() -> recorder.increment(FcmMetrics.MESSAGES_SENT, ctx));
        assertThatNoException().isThrownBy(() -> recorder.increment(FcmMetrics.MESSAGES_SENT, ctx, FcmMetrics.Tags.OUTCOME, "success"));
        assertThatNoException().isThrownBy(() -> recorder.recordTimer(FcmMetrics.MESSAGES_DURATION, ctx, Duration.ofMillis(50)));
        assertThatNoException().isThrownBy(() -> recorder.recordDistribution(FcmMetrics.BATCH_SIZE, ctx, 10.0));
    }
}
