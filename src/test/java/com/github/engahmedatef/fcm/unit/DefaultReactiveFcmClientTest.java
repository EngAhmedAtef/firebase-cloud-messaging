package com.github.engahmedatef.fcm.unit;

import com.github.engahmedatef.fcm.api.FcmTokenService;
import com.github.engahmedatef.fcm.exception.FcmBatchExceededThresholdException;
import com.github.engahmedatef.fcm.exception.FcmInvalidRequestException;
import com.github.engahmedatef.fcm.exception.FcmTokenServiceNotDefinedException;
import com.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import com.github.engahmedatef.fcm.internal.observability.DefaultFcmObservationManager;
import com.github.engahmedatef.fcm.internal.observability.FcmExecutionContextFactory;
import com.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import com.github.engahmedatef.fcm.internal.observability.ObservationManager;
import com.github.engahmedatef.fcm.internal.service.DefaultReactiveFcmClient;
import com.github.engahmedatef.fcm.internal.service.EnricherChain;
import com.github.engahmedatef.fcm.internal.service.InterceptorChain;
import com.github.engahmedatef.fcm.internal.service.ValidatorChain;
import com.github.engahmedatef.fcm.internal.service.sender.FcmMessageSender;
import com.github.engahmedatef.fcm.internal.service.sender.FcmSenderResponse;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import com.github.engahmedatef.fcm.internal.transport.FcmSendSuccessResponse;
import com.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import com.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import com.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmDeviceType;
import com.github.engahmedatef.fcm.model.FcmSendResult;
import com.github.engahmedatef.fcm.spi.FcmLogger;
import com.github.engahmedatef.fcm.spi.FcmMessageEnricher;
import com.github.engahmedatef.fcm.spi.FcmPayloadBuilder;
import com.github.engahmedatef.fcm.spi.FcmPayloadInterceptor;
import com.github.engahmedatef.fcm.spi.FcmRequestValidator;
import com.github.engahmedatef.fcm.spi.FcmResponseErrorHandler;
import com.github.engahmedatef.fcm.spi.FcmRetryPolicy;
import com.github.engahmedatef.fcm.support.FakeFcmTokenService;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultReactiveFcmClientTest {

    @Mock private FcmPayloadBuilder payloadBuilder;
    @Mock private FcmMessageSender sender;
    @Mock private FcmResponseErrorHandler errorHandler;
    @Mock private FcmRetryPolicy retryPolicy;
    @Mock private FcmLogger fcmLogger;

    private FcmExecutionContextFactory executionContextFactory;
    private ObservationManager observationManager;
    private DefaultReactiveFcmClient client;
    private FcmSenderResponse successResponse;

    @BeforeEach
    void setUp() {
        executionContextFactory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());
        observationManager = new DefaultFcmObservationManager();
        successResponse = FcmSenderResponse.success(new FcmSendSuccessResponse("projects/test-project/messages/1"));

        // Passthrough retry: re-propagates the original error without wrapping in RetryExhaustedException
        lenient().when(retryPolicy.create(any())).thenReturn(
                Retry.from(flux -> flux.flatMap(rs -> Mono.error(rs.failure()))));
        lenient().when(payloadBuilder.build(any(), any())).thenReturn(FcmTestFixtures.aDeviceMessagePayload());
        lenient().when(sender.send(any())).thenReturn(Mono.just(successResponse));
        lenient().doNothing().when(fcmLogger).info(any(), any());

        client = build(null, 4, 10, 0.5);
    }

    private DefaultReactiveFcmClient build(FcmTokenService tokenService, int concurrency, int batchSize, double threshold) {
        return new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                tokenService, concurrency, batchSize, threshold,
                "test-project", fcmLogger,
                new ValidatorChain(List.of(), false),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(), false),
                executionContextFactory, observationManager, new NoOpFcmMetricsRecorder()
        );
    }

    // --- Target routing ---

    @Test
    void sendToDeviceRoutesToDeviceTarget() {
        StepVerifier.create(client.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectNextMatches(FcmSendResult::isSuccess)
                .verifyComplete();

        verify(payloadBuilder).build(any(DeviceTarget.class), any());
    }

    @Test
    void sendToTopicRoutesToTopicTarget() {
        StepVerifier.create(client.sendToTopic("news", FcmTestFixtures.aTopicMessage()))
                .expectNextMatches(FcmSendResult::isSuccess)
                .verifyComplete();

        verify(payloadBuilder).build(any(TopicTarget.class), any());
    }

    @Test
    void sendToConditionRoutesToConditionTarget() {
        StepVerifier.create(client.sendToCondition("'a' in topics", FcmTestFixtures.aConditionMessage()))
                .expectNextMatches(FcmSendResult::isSuccess)
                .verifyComplete();

        verify(payloadBuilder).build(any(ConditionTarget.class), any());
    }

    // --- sendToUser ---

    @Test
    void sendToUserWithoutTokenServiceThrowsDefinedError() {
        StepVerifier.create(client.sendToUser("user1", FcmTestFixtures.aDeviceMessage()))
                .expectError(FcmTokenServiceNotDefinedException.class)
                .verify();
    }

    @Test
    void sendToUserWithTokenServiceSendsToEachDevice() {
        FakeFcmTokenService tokenService = new FakeFcmTokenService();
        tokenService.registerDevices("user1", List.of(FcmTestFixtures.aDevice(), FcmTestFixtures.aDevice()));

        DefaultReactiveFcmClient clientWithService = build(tokenService, 4, 10, 0.5);

        StepVerifier.create(clientWithService.sendToUser("user1", FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        verify(sender, times(2)).send(any());
    }

    @Test
    void sendToUserWithNoRegisteredDevicesCompletesWithoutSending() {
        FakeFcmTokenService tokenService = new FakeFcmTokenService();

        DefaultReactiveFcmClient clientWithService = build(tokenService, 4, 10, 0.5);

        StepVerifier.create(clientWithService.sendToUser("ghost-user", FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        verify(sender, never()).send(any());
    }

    // --- sendToDevices batching ---

    @Test
    void sendToDevicesWithEmptyListCompletesImmediately() {
        StepVerifier.create(client.sendToDevices(List.of(), FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        verify(sender, never()).send(any());
    }

    @Test
    void sendToDevicesSingleDeviceSendsOnce() {
        StepVerifier.create(client.sendToDevices(List.of(FcmTestFixtures.aDevice()), FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        verify(sender, times(1)).send(any());
    }

    @Test
    void sendToDevicesExactlyBatchSizeProducesOneBatch() {
        DefaultReactiveFcmClient batchClient = build(null, 2, 3, 0.5);

        StepVerifier.create(batchClient.sendToDevices(buildDevices(3), FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        verify(sender, times(3)).send(any());
    }

    @Test
    void sendToDevicesBatchSizePlusOneSpillsToSecondBatch() {
        DefaultReactiveFcmClient batchClient = build(null, 2, 3, 0.5);

        StepVerifier.create(batchClient.sendToDevices(buildDevices(4), FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        verify(sender, times(4)).send(any());
    }

    @Test
    void sendToDevicesFailureRateAboveThresholdAbortsPipeline() {
        when(sender.send(any())).thenReturn(Mono.just(FcmSenderResponse.error(null)));
        when(errorHandler.handle(any(), nullable(FcmSendErrorResponse.class)))
                .thenReturn(Mono.just(FcmSendResult.failure(new RuntimeException("device-rejected"))));

        // threshold=0.49 → 2 failures out of 2 = 100% > 49% → abort
        DefaultReactiveFcmClient thresholdClient = build(null, 1, 5, 0.49);

        StepVerifier.create(thresholdClient.sendToDevices(buildDevices(2), FcmTestFixtures.aDeviceMessage()))
                .expectError(FcmBatchExceededThresholdException.class)
                .verify();
    }

    // --- Chain integration ---

    @Test
    void validatorChainIsInvokedBeforeSend() {
        FcmRequestValidator validator = mock(FcmRequestValidator.class);
        when(validator.validate(any(), any())).thenReturn(Mono.empty());

        DefaultReactiveFcmClient clientWithChain = new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                null, 4, 10, 0.5, "test-project", fcmLogger,
                new ValidatorChain(List.of(validator), true),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(), false),
                executionContextFactory, observationManager, new NoOpFcmMetricsRecorder()
        );

        StepVerifier.create(clientWithChain.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectNextCount(1)
                .verifyComplete();

        verify(validator).validate(any(DeviceTarget.class), any());
    }

    @Test
    void enricherChainMutatedMessagePassedToPayloadBuilder() {
        FcmMessageEnricher enricher = mock(FcmMessageEnricher.class);
        when(enricher.enrich(any(), any())).thenReturn(Mono.just(FcmTestFixtures.aDeviceMessage()));

        DefaultReactiveFcmClient clientWithEnricher = new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                null, 4, 10, 0.5, "test-project", fcmLogger,
                new ValidatorChain(List.of(), false),
                new EnricherChain(List.of(enricher), true),
                new InterceptorChain(List.of(), false),
                executionContextFactory, observationManager, new NoOpFcmMetricsRecorder()
        );

        StepVerifier.create(clientWithEnricher.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectNextCount(1)
                .verifyComplete();

        verify(enricher).enrich(any(), any());
    }

    @Test
    void interceptorChainRunsAfterPayloadBuilderInOrder() {
        var builtPayload = FcmTestFixtures.aDeviceMessagePayload();
        FcmPayloadInterceptor interceptor = mock(FcmPayloadInterceptor.class);
        when(interceptor.intercept(any(), any())).thenReturn(Mono.just(builtPayload));

        DefaultReactiveFcmClient clientWithInterceptor = new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                null, 4, 10, 0.5, "test-project", fcmLogger,
                new ValidatorChain(List.of(), false),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(interceptor), true),
                executionContextFactory, observationManager, new NoOpFcmMetricsRecorder()
        );

        StepVerifier.create(clientWithInterceptor.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectNextCount(1)
                .verifyComplete();

        InOrder order = inOrder(payloadBuilder, interceptor);
        order.verify(payloadBuilder).build(any(), any());
        order.verify(interceptor).intercept(any(), any());
    }

    @Test
    void validationFailureShortCircuitsBeforeSend() {
        FcmRequestValidator failingValidator = mock(FcmRequestValidator.class);
        when(failingValidator.validate(any(), any()))
                .thenReturn(Mono.error(new FcmInvalidRequestException("bad topic")));

        DefaultReactiveFcmClient clientWithFailValidator = new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                null, 4, 10, 0.5, "test-project", fcmLogger,
                new ValidatorChain(List.of(failingValidator), true),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(), false),
                executionContextFactory, observationManager, new NoOpFcmMetricsRecorder()
        );

        StepVerifier.create(clientWithFailValidator.sendToDevice(FcmTestFixtures.aDevice(), FcmTestFixtures.aDeviceMessage()))
                .expectError(FcmInvalidRequestException.class)
                .verify();

        verify(sender, never()).send(any());
    }

    // --- Helpers ---

    private List<FcmDevice> buildDevices(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmDevice.builder()
                        .type(FcmDeviceType.ANDROID)
                        .token("device-token-" + i + "-padded12345678")
                        .build())
                .toList();
    }
}
