package com.github.engahmedatef.fcm.unit;

import com.github.engahmedatef.fcm.exception.FcmInvalidTokenException;
import com.github.engahmedatef.fcm.exception.FcmUnavailableException;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmObservationManager;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import com.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import com.github.engahmedatef.fcm.internal.metrics.DefaultFcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmRetryPolicy;
import com.github.engahmedatef.fcm.spi.FcmLogger;
import com.github.engahmedatef.fcm.support.TestFcmRetryableException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefaultFcmRetryPolicyTest {

    @Mock
    private FcmLogger fcmLogger;

    private SimpleMeterRegistry meterRegistry;
    private DefaultFcmRetryPolicy retryPolicy;
    private DefaultFcmExecutionContextFactory contextFactory;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        contextFactory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());
        retryPolicy = new DefaultFcmRetryPolicy(2, Duration.ofMillis(10), Duration.ofMillis(100), new DefaultFcmMetricsRecorder(meterRegistry), fcmLogger, new DefaultFcmObservationManager());
        lenient().doNothing().when(fcmLogger).warn(any(), any());
    }

    private FcmExecutionContext aContext() {
        return contextFactory.create("test-project", null);
    }

    @Test
    void fcmRetryableExceptionIsRetried() {
        AtomicInteger attempts = new AtomicInteger(0);
        Retry policy = retryPolicy.create(aContext());

        StepVerifier.create(
                Mono.defer(() -> {
                    attempts.incrementAndGet();
                    return Mono.error(new TestFcmRetryableException("transient error"));
                }).retryWhen(policy)
        )
                .expectError(TestFcmRetryableException.class)
                .verify(Duration.ofSeconds(5));

        assertThat(attempts.get()).isEqualTo(3); // 1 initial + 2 retries
    }

    @Test
    void fcmUnavailableExceptionIsNotRetryableByDefault() {
        // FcmUnavailableException extends FcmException (not FcmRetryableException),
        // so the retry policy does not retry it. This documents current behavior.
        AtomicInteger attempts = new AtomicInteger(0);
        Retry policy = retryPolicy.create(aContext());

        StepVerifier.create(
                Mono.defer(() -> {
                    attempts.incrementAndGet();
                    return Mono.error(new FcmUnavailableException("unavailable"));
                }).retryWhen(policy)
        )
                .expectError(FcmUnavailableException.class)
                .verify(Duration.ofSeconds(2));

        assertThat(attempts.get()).isEqualTo(1); // No retry for non-FcmRetryableException
    }

    @Test
    void nonRetryableExceptionIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger(0);
        Retry policy = retryPolicy.create(aContext());

        StepVerifier.create(
                Mono.defer(() -> {
                    attempts.incrementAndGet();
                    return Mono.error(new FcmInvalidTokenException("invalid token"));
                }).retryWhen(policy)
        )
                .expectError(FcmInvalidTokenException.class)
                .verify(Duration.ofSeconds(2));

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void retryCounterIsIncrementedInMeterRegistry() {
        Retry policy = retryPolicy.create(aContext());

        StepVerifier.create(
                Mono.error(new TestFcmRetryableException("transient")).retryWhen(policy)
        )
                .expectError()
                .verify(Duration.ofSeconds(5));

        double retryCount = meterRegistry.find("fcm.messages.retried").counter().count();
        assertThat(retryCount).isEqualTo(2.0);
    }
}
