package io.github.engahmedatef.fcm.concurrency;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import io.github.engahmedatef.fcm.internal.observability.DefaultFcmObservationManager;
import io.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
import io.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmLogger;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmPayloadBuilder;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmResponseErrorHandler;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmRetryPolicy;
import io.github.engahmedatef.fcm.internal.service.DefaultReactiveFcmClient;
import io.github.engahmedatef.fcm.internal.service.EnricherChain;
import io.github.engahmedatef.fcm.internal.service.InterceptorChain;
import io.github.engahmedatef.fcm.internal.service.ValidatorChain;
import io.github.engahmedatef.fcm.internal.service.sender.WebClientFcmMessageSender;
import io.github.engahmedatef.fcm.model.FcmDevice;
import io.github.engahmedatef.fcm.model.FcmDeviceType;
import io.github.engahmedatef.fcm.support.FakeAccessTokenProvider;
import io.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class BatchSendConcurrencyTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private DefaultReactiveFcmClient client;

    @BeforeEach
    void setUp() {
        String sendUrl = wm.baseUrl() + "/v1/projects/test-project/messages:send";
        WebClient webClient = WebClient.builder().baseUrl(wm.baseUrl()).build();
        var tokenProvider = new FakeAccessTokenProvider();
        var recorder = new NoOpFcmMetricsRecorder();
        var sender = new WebClientFcmMessageSender(tokenProvider, webClient, sendUrl,
                ObservationRegistry.NOOP, "test-project");
        var logger = new DefaultFcmLogger();
        var observationManager = new DefaultFcmObservationManager();
        var retryPolicy = new DefaultFcmRetryPolicy(0, Duration.ofMillis(1), Duration.ofMillis(10), recorder, logger, observationManager);
        var errorHandler = new DefaultFcmResponseErrorHandler(null, logger);
        var payloadBuilder = new DefaultFcmPayloadBuilder();
        var contextFactory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());

        var validatorChain = new ValidatorChain(List.of(), false);
        var enricherChain = new EnricherChain(List.of(), false);
        var interceptorChain = new InterceptorChain(List.of(), false);

        client = new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                null, 4, 10, 0.5, "test-project", logger,
                validatorChain, enricherChain, interceptorChain,
                contextFactory, observationManager, recorder
        );
    }

    @Test
    void emptyDeviceCollectionProducesNoRequests() {
        StepVerifier.create(client.sendToDevices(List.of(), FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        wm.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void allSuccessfulSendsCompletesNormally() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        List<FcmDevice> devices = buildDevices(5);
        StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        wm.verify(5, postRequestedFor(anyUrl()));
    }

    @Test
    void sendDevicesReachesAllDevicesOnSuccess() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        int deviceCount = 25;
        List<FcmDevice> devices = buildDevices(deviceCount);
        StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        wm.verify(deviceCount, postRequestedFor(anyUrl()));
    }

    @Test
    void fcmErrorOnDeviceExceedingThresholdAbortsBatch() {
        // All 5 devices fail → 100% failure rate ≥ 0.5 threshold → FcmBatchExceededThresholdException.
        // Per-device errors are converted to FcmSendResult.failure() via onErrorResume before threshold check.
        wm.stubFor(post(anyUrl())
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error": {
                                    "code": 400,
                                    "message": "Invalid argument",
                                    "details": [{"errorCode": "INVALID_ARGUMENT"}]
                                  }
                                }
                                """)));

        List<FcmDevice> devices = buildDevices(5);
        StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                .expectError(io.github.engahmedatef.fcm.exception.FcmBatchExceededThresholdException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void largeDeviceBatch_allDevicesReceiveRequests() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        List<FcmDevice> devices = buildDevices(500);

        // batchSize=10 from setUp; 500/10 = 50 batches processed sequentially
        StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        wm.verify(500, postRequestedFor(anyUrl()));
    }

    @Test
    void multiWindowBatch_spill_sendsAllDevices() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        // Build a client with batchSize=3: 7 devices → 2 full batches (3+3) + 1 partial (1)
        String sendUrl = wm.baseUrl() + "/v1/projects/test-project/messages:send";
        WebClient webClient = WebClient.builder().baseUrl(wm.baseUrl()).build();
        var recorder = new NoOpFcmMetricsRecorder();
        var sender = new WebClientFcmMessageSender(new FakeAccessTokenProvider(), webClient, sendUrl,
                ObservationRegistry.NOOP, "test-project");
        var logger = new DefaultFcmLogger();
        var observationManager = new DefaultFcmObservationManager();
        var retryPolicy = new DefaultFcmRetryPolicy(0, Duration.ofMillis(1), Duration.ofMillis(10), recorder, logger, observationManager);
        var contextFactory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());

        var spillClient = new DefaultReactiveFcmClient(
                new DefaultFcmPayloadBuilder(),
                sender,
                new DefaultFcmResponseErrorHandler(null, logger),
                retryPolicy,
                null, 4, 3, 0.5, "test-project", logger,
                new ValidatorChain(List.of(), false),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(), false),
                contextFactory, observationManager, recorder
        );

        StepVerifier.create(spillClient.sendToDevices(buildDevices(7), FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();

        wm.verify(7, postRequestedFor(anyUrl()));
    }

    @Test
    void failureThresholdBelowRate_batchCompletes() {
        // All succeed → 0% failure rate, well below threshold
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        StepVerifier.create(client.sendToDevices(buildDevices(10), FcmTestFixtures.aDeviceMessage()))
                .verifyComplete();
    }

    private List<FcmDevice> buildDevices(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmDevice.builder()
                        .type(FcmDeviceType.ANDROID)
                        .token("device-token-" + i + "-padded123456789")
                        .build())
                .toList();
    }
}
