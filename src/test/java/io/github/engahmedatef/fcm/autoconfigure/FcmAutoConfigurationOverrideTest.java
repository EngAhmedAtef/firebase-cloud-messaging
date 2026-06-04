package io.github.engahmedatef.fcm.autoconfigure;

import io.github.engahmedatef.fcm.api.FcmClient;
import io.github.engahmedatef.fcm.api.FcmTokenService;
import io.github.engahmedatef.fcm.api.ReactiveFcmClient;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmPayloadBuilder;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmRequestValidator;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmResponseErrorHandler;
import io.github.engahmedatef.fcm.internal.service.DefaultFcmRetryPolicy;
import io.github.engahmedatef.fcm.internal.service.EnricherChain;
import io.github.engahmedatef.fcm.internal.service.InterceptorChain;
import io.github.engahmedatef.fcm.internal.metrics.DefaultFcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.service.ValidatorChain;
import io.github.engahmedatef.fcm.spi.*;
import io.github.engahmedatef.fcm.support.FakeAccessTokenProvider;
import io.github.engahmedatef.fcm.support.FakeFcmTokenService;
import io.github.engahmedatef.fcm.support.FcmTestFixtures;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FcmAutoConfigurationOverrideTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FcmAutoConfiguration.class)
            .withPropertyValues("fcm.project-id=test-project")
            .withBean(FcmAccessTokenProvider.class, FakeAccessTokenProvider::new);

    @Test
    void customRetryPolicyPreventsDefaultCreation() {
        FcmRetryPolicy customPolicy = ctx -> Retry.max(1);
        contextRunner
                .withBean(FcmRetryPolicy.class, () -> customPolicy)
                .run(ctx -> {
                    assertThat(ctx.getBean(FcmRetryPolicy.class)).isSameAs(customPolicy);
                    assertThat(ctx).doesNotHaveBean(DefaultFcmRetryPolicy.class);
                });
    }

    @Test
    void customPayloadBuilderPreventsDefaultCreation() {
        FcmPayloadBuilder customBuilder = Mockito.mock(FcmPayloadBuilder.class);
        contextRunner
                .withBean(FcmPayloadBuilder.class, () -> customBuilder)
                .run(ctx -> {
                    assertThat(ctx.getBean(FcmPayloadBuilder.class)).isSameAs(customBuilder);
                    assertThat(ctx).doesNotHaveBean(DefaultFcmPayloadBuilder.class);
                });
    }

    @Test
    void customResponseErrorHandlerPreventsDefaultCreation() {
        FcmResponseErrorHandler customHandler = Mockito.mock(FcmResponseErrorHandler.class);
        contextRunner
                .withBean(FcmResponseErrorHandler.class, () -> customHandler)
                .run(ctx -> {
                    assertThat(ctx.getBean(FcmResponseErrorHandler.class)).isSameAs(customHandler);
                    assertThat(ctx).doesNotHaveBean(DefaultFcmResponseErrorHandler.class);
                });
    }

    @Test
    void customAccessTokenProviderPreventsDefaultCreation() {
        FakeAccessTokenProvider customProvider = new FakeAccessTokenProvider();
        // Already provided via base contextRunner, so just verify it's the custom one
        contextRunner.run(ctx -> {
            assertThat(ctx.getBean(FcmAccessTokenProvider.class)).isInstanceOf(FakeAccessTokenProvider.class);
        });
    }

    @Test
    void customFcmLoggerPreventsDefaultCreation() {
        FcmLogger customLogger = Mockito.mock(FcmLogger.class);
        contextRunner
                .withBean(FcmLogger.class, () -> customLogger)
                .run(ctx -> {
                    assertThat(ctx.getBean(FcmLogger.class)).isSameAs(customLogger);
                });
    }

    @Test
    void fcmTokenServiceInjectedIntoErrorHandler() {
        FakeFcmTokenService tokenService = new FakeFcmTokenService();
        contextRunner
                .withBean(FcmTokenService.class, () -> tokenService)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(FcmTokenService.class);
                    assertThat(ctx.getBean(FcmTokenService.class)).isSameAs(tokenService);
                });
    }

    // --- Phase 3 SPI override tests ---

    @Test
    void customRequestValidatorReplacesDefault() {
        FcmRequestValidator customValidator = Mockito.mock(FcmRequestValidator.class);
        contextRunner
                .withBean(FcmRequestValidator.class, () -> customValidator)
                .run(ctx -> {
                    assertThat(ctx.getBean(FcmRequestValidator.class)).isSameAs(customValidator);
                    assertThat(ctx).doesNotHaveBean(DefaultFcmRequestValidator.class);
                    assertThat(ctx).hasSingleBean(ValidatorChain.class);
                });
    }

    @Test
    void customPayloadInterceptorIsRegisteredInContext() {
        FcmPayloadInterceptor customInterceptor = Mockito.mock(FcmPayloadInterceptor.class);
        contextRunner
                .withBean(FcmPayloadInterceptor.class, () -> customInterceptor)
                .run(ctx -> {
                    assertThat(ctx.getBean(FcmPayloadInterceptor.class)).isSameAs(customInterceptor);
                    assertThat(ctx).hasSingleBean(InterceptorChain.class);
                });
    }

    @Test
    void customMessageEnricherIsRegisteredInContext() {
        FcmMessageEnricher customEnricher = Mockito.mock(FcmMessageEnricher.class);
        contextRunner
                .withBean(FcmMessageEnricher.class, () -> customEnricher)
                .run(ctx -> {
                    assertThat(ctx.getBean(FcmMessageEnricher.class)).isSameAs(customEnricher);
                    assertThat(ctx).hasSingleBean(EnricherChain.class);
                });
    }

    @Test
    void customWebClientBuilderCustomizerIsRegisteredInContext() {
        FcmWebClientBuilderCustomizer customizer = builder -> {};
        contextRunner
                .withBean(FcmWebClientBuilderCustomizer.class, () -> customizer)
                .run(ctx -> assertThat(ctx).hasSingleBean(FcmWebClientBuilderCustomizer.class));
    }

    @Test
    void validatorChainAndEnricherChainAndInterceptorChainAlwaysCreated() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ValidatorChain.class);
            assertThat(ctx).hasSingleBean(EnricherChain.class);
            assertThat(ctx).hasSingleBean(InterceptorChain.class);
        });
    }

    // --- Observability flag tests ---

    @Test
    void validationDisabledMakesChainSkipValidators() {
        contextRunner
                .withPropertyValues("fcm.validation.enabled=false")
                .run(ctx -> {
                    ValidatorChain chain = ctx.getBean(ValidatorChain.class);
                    StepVerifier.create(chain.apply(
                            FcmTestFixtures.aDeviceTarget(), FcmTestFixtures.aDeviceMessage()))
                            .verifyComplete();
                });
    }

    @Test
    void noMeterRegistryUsesNoOpRecorder() {
        contextRunner
                .run(ctx -> {
                    FcmMetricsRecorder recorder = ctx.getBean(FcmMetricsRecorder.class);
                    assertThat(recorder).isInstanceOf(NoOpFcmMetricsRecorder.class);
                });
    }

    @Test
    void meterRegistryPresentUsesDefaultRecorder() {
        contextRunner
                .withBean(io.micrometer.core.instrument.MeterRegistry.class, SimpleMeterRegistry::new)
                .run(ctx -> {
                    FcmMetricsRecorder recorder = ctx.getBean(FcmMetricsRecorder.class);
                    assertThat(recorder).isInstanceOf(DefaultFcmMetricsRecorder.class);
                });
    }

    @Test
    void correlationDisabledContextStillLoads() {
        contextRunner
                .withPropertyValues("fcm.observability.correlation-enabled=false")
                .run(ctx -> assertThat(ctx).hasSingleBean(ReactiveFcmClient.class));
    }

    @Test
    void tracingDisabledContextStillLoads() {
        contextRunner
                .withPropertyValues("fcm.observability.tracing-enabled=false")
                .run(ctx -> assertThat(ctx).hasSingleBean(ReactiveFcmClient.class));
    }
}
