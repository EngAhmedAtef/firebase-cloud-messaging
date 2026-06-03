package com.github.engahmedatef.fcm.unit;

import com.github.engahmedatef.fcm.internal.metrics.FcmMetrics;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmObservationManager;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFcmObservationManagerTest {

    private TestObservationRegistry observationRegistry;
    private DefaultFcmObservationManager observationManager;
    private DefaultFcmExecutionContextFactory factory;

    @BeforeEach
    void setUp() {
        observationRegistry = TestObservationRegistry.create();
        observationManager = new DefaultFcmObservationManager();
        factory = new DefaultFcmExecutionContextFactory(observationRegistry, new FcmSendObservationConvention());
    }

    private FcmExecutionContext aContext() {
        return factory.create("test-project", null);
    }

    @Test
    void start_beginsObservation() {
        FcmExecutionContext ctx = aContext();
        observationManager.start(ctx);
        observationManager.success(ctx);
        observationManager.stop(ctx);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.send");
    }

    @Test
    void retry_incrementsRetryCountOnContext() {
        FcmExecutionContext ctx = aContext();
        observationManager.start(ctx);

        observationManager.retry(ctx, new RuntimeException("transient"));
        observationManager.retry(ctx, new RuntimeException("transient"));

        assertThat(ctx.getRetryCount().get()).isEqualTo(2);
    }

    @Test
    void retry_updatesHighCardinalityRetryCount() {
        FcmExecutionContext ctx = aContext();
        observationManager.start(ctx);
        observationManager.retry(ctx, new RuntimeException("transient"));
        observationManager.success(ctx);
        observationManager.stop(ctx);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.send")
                .that()
                .hasHighCardinalityKeyValue(FcmMetrics.Tags.RETRY_COUNT, "1");
    }

    @Test
    void success_setsOutcomeTag() {
        FcmExecutionContext ctx = aContext();
        observationManager.start(ctx);
        observationManager.success(ctx);
        observationManager.stop(ctx);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.send")
                .that()
                .hasLowCardinalityKeyValue(FcmMetrics.Tags.OUTCOME, "success");
    }

    @Test
    void failure_setsOutcomeTagAndRecordsError() {
        FcmExecutionContext ctx = aContext();
        RuntimeException boom = new RuntimeException("boom");
        observationManager.start(ctx);
        observationManager.failure(ctx, boom);
        observationManager.stop(ctx);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.send")
                .that()
                .hasLowCardinalityKeyValue(FcmMetrics.Tags.OUTCOME, "failure")
                .hasError(boom);
    }

    @Test
    void cancel_setsOutcomeToCancelled() {
        FcmExecutionContext ctx = aContext();
        observationManager.start(ctx);
        observationManager.cancel(ctx);
        observationManager.stop(ctx);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("fcm.send")
                .that()
                .hasLowCardinalityKeyValue(FcmMetrics.Tags.OUTCOME, "cancelled");
    }
}
