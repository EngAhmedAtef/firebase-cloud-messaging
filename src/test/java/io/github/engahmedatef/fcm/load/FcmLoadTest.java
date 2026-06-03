package io.github.engahmedatef.fcm.load;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.observability.DefaultFcmExecutionContextFactory;
import io.github.engahmedatef.fcm.internal.observability.DefaultFcmObservationManager;
import io.github.engahmedatef.fcm.internal.observability.FcmExecutionContext;
import io.github.engahmedatef.fcm.internal.observability.FcmSendObservationConvention;
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
import io.github.engahmedatef.fcm.spi.FcmAccessTokenProvider;
import io.github.engahmedatef.fcm.support.FakeAccessTokenProvider;
import io.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class FcmLoadTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private DefaultReactiveFcmClient client;

    @BeforeEach
    void setUp() {
        wm.stubFor(post(anyUrl())
                .willReturn(okJson("""
                        {"name":"projects/test-project/messages/msg-001"}
                        """)));

        client = buildClient(new FakeAccessTokenProvider());
    }

    private DefaultReactiveFcmClient buildClient(FcmAccessTokenProvider tokenProvider) {
        String sendUrl = wm.baseUrl() + "/v1/projects/test-project/messages:send";
        WebClient webClient = WebClient.builder().baseUrl(wm.baseUrl()).build();
        var recorder = new NoOpFcmMetricsRecorder();
        var sender = new WebClientFcmMessageSender(tokenProvider, webClient, sendUrl,
                ObservationRegistry.NOOP, "test-project");
        var logger = new DefaultFcmLogger();
        var observationManager = new DefaultFcmObservationManager();
        var retryPolicy = new DefaultFcmRetryPolicy(0, Duration.ofMillis(1), Duration.ofMillis(5), recorder, logger, observationManager);
        var contextFactory = new DefaultFcmExecutionContextFactory(ObservationRegistry.NOOP, new FcmSendObservationConvention());

        return new DefaultReactiveFcmClient(
                new DefaultFcmPayloadBuilder(),
                sender,
                new DefaultFcmResponseErrorHandler(null, logger),
                retryPolicy,
                null, 16, 100, 0.5, "test-project", logger,
                new ValidatorChain(List.of(), false),
                new EnricherChain(List.of(), false),
                new InterceptorChain(List.of(), false),
                contextFactory, observationManager, recorder
        );
    }

    @Test
    void highVolumeBatchSend_completesWithinTimeout() {
        List<FcmDevice> devices = buildDevices(1000);

        StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        wm.verify(1000, postRequestedFor(anyUrl()));
    }

    @Test
    void concurrentSingleSends_allComplete() {
        FcmDevice device = FcmTestFixtures.aDevice();

        StepVerifier.create(
                Flux.range(0, 100)
                        .flatMap(i -> client.sendToDevice(device, FcmTestFixtures.aDeviceMessage()), 50)
        )
                .expectNextCount(100)
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        wm.verify(100, postRequestedFor(anyUrl()));
    }

    @Test
    void tokenProviderCalledOnEveryRequest_allSucceed() {
        AtomicInteger tokenCallCount = new AtomicInteger(0);

        FcmAccessTokenProvider countingProvider = ctx -> {
            tokenCallCount.incrementAndGet();
            return Mono.just(FakeAccessTokenProvider.TEST_TOKEN);
        };

        DefaultReactiveFcmClient clientWithCounter = buildClient(countingProvider);

        StepVerifier.create(clientWithCounter.sendToDevices(buildDevices(50), FcmTestFixtures.aDeviceMessage()))
                .expectComplete()
                .verify(Duration.ofSeconds(20));

        wm.verify(50, postRequestedFor(anyUrl()));
        assertThat(tokenCallCount.get()).isEqualTo(50);
    }

    @Test
    void repeatedBatchRuns_allComplete() {
        List<FcmDevice> devices = buildDevices(100);

        for (int run = 0; run < 5; run++) {
            StepVerifier.create(client.sendToDevices(devices, FcmTestFixtures.aDeviceMessage()))
                    .expectComplete()
                    .verify(Duration.ofSeconds(20));
        }

        wm.verify(500, postRequestedFor(anyUrl()));
    }

    private List<FcmDevice> buildDevices(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> FcmDevice.builder()
                        .type(FcmDeviceType.ANDROID)
                        .token("load-token-" + i + "-padded123456789")
                        .build())
                .toList();
    }
}
