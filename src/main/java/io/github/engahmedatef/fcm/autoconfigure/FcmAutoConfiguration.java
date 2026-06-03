package io.github.engahmedatef.fcm.autoconfigure;

import io.github.engahmedatef.fcm.api.FcmClient;
import io.github.engahmedatef.fcm.api.FcmTokenService;
import io.github.engahmedatef.fcm.api.ReactiveFcmClient;
import io.github.engahmedatef.fcm.internal.auth.AccessTokenProviderFactory;
import io.github.engahmedatef.fcm.internal.auth.CredentialResolver;
import io.github.engahmedatef.fcm.internal.http.FcmWebClientFactory;
import io.github.engahmedatef.fcm.internal.http.ReactorNettyWebClientFactory;
import io.github.engahmedatef.fcm.internal.metrics.DefaultFcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.metrics.FcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.metrics.NoOpFcmMetricsRecorder;
import io.github.engahmedatef.fcm.internal.observability.*;
import io.github.engahmedatef.fcm.internal.service.*;
import io.github.engahmedatef.fcm.internal.service.sender.FcmMessageSender;
import io.github.engahmedatef.fcm.internal.service.sender.WebClientFcmMessageSender;
import io.github.engahmedatef.fcm.spi.*;
import com.google.auth.oauth2.GoogleCredentials;
import io.micrometer.context.ContextRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Spring Boot auto-configuration for the FCM SDK.
 * Activated when the {@code io.github.engahmedatef.fcm.project-id} property is set.
 * Registers all default beans (credentials, token provider, retry policy, reactive client, etc.)
 * and backs off whenever the application supplies a custom bean of the same type.
 */
@AutoConfiguration
@ConditionalOnProperty("io.github.engahmedatef.fcm.project-id")
@EnableConfigurationProperties(FcmProperties.class)
public class FcmAutoConfiguration {
    public static final String FCM_WEB_CLIENT_QUALIFIER = "fcmWebClient";

    private static final AtomicBoolean CONTEXT_PROPAGATION_INITIALIZED = new AtomicBoolean(false);

    @Bean
    public FcmConfigurationValidator fcmConfigurationValidator() {
        return new FcmConfigurationValidator();
    }

    @Bean
    public SmartInitializingSingleton fcmConfigurationValidationRunner(FcmConfigurationValidator validator, FcmProperties properties) {
        return () -> validator.validate(properties);
    }

    @Bean
    public ApplicationRunner fcmContextPropagationInitializer(FcmProperties properties) {
        return args -> {
            if (properties.getObservability().isCorrelationEnabled() && CONTEXT_PROPAGATION_INITIALIZED.compareAndSet(false, true)) {
                Hooks.enableAutomaticContextPropagation();
                ContextRegistry.getInstance().registerThreadLocalAccessor(new FcmCorrelationId.MdcAccessor());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmSendObservationConvention fcmSendObservationConvention() {
        return new FcmSendObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(FcmMetricsRecorder.class)
    public FcmMetricsRecorder fcmMetricsRecorder(ObjectProvider<MeterRegistry> meterRegistryProvider, FcmProperties properties) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        boolean metricsEnabled = properties.getObservability().isMetricsEnabled();

        return (metricsEnabled && registry != null) ? new DefaultFcmMetricsRecorder(registry) : new NoOpFcmMetricsRecorder();
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmWebClientFactory fcmWebClientFactory(FcmProperties properties,
                                                   ObjectProvider<FcmWebClientBuilderCustomizer> customizers,
                                                   @FcmExchangeFilter ObjectProvider<ExchangeFilterFunction> filterProvider) {
        return new ReactorNettyWebClientFactory(properties, customizers, filterProvider);
    }

    @Bean(FCM_WEB_CLIENT_QUALIFIER)
    @ConditionalOnMissingBean(name = FCM_WEB_CLIENT_QUALIFIER)
    public WebClient fcmWebClient(FcmWebClientFactory factory) {
        return factory.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public CredentialResolver credentialResolver(ResourceLoader resourceLoader) {
        return new CredentialResolver(resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessTokenProviderFactory accessTokenProviderFactory() {
        return new AccessTokenProviderFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmAccessTokenProvider fcmAccessTokenProvider(CredentialResolver resolver,
                                                          AccessTokenProviderFactory factory,
                                                          FcmProperties properties,
                                                          FcmMetricsRecorder metricsRecorder) throws IOException {
        GoogleCredentials credentials = resolver.resolve(properties.getAuth().getCredentials());
        return factory.create(credentials, properties.getAuth().getCredentials().getScope(), metricsRecorder, properties.getAuth().getJwt());
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmExecutionContextFactory fcmExecutionContextFactory(ObjectProvider<ObservationRegistry> observationRegistryProvider, FcmSendObservationConvention convention, FcmProperties properties) {
        ObservationRegistry observationRegistry = resolveObservationRegistry(observationRegistryProvider, properties.getObservability().isTracingEnabled());
        return new DefaultFcmExecutionContextFactory(observationRegistry, convention);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObservationManager fcmObservationManager() {
        return new DefaultFcmObservationManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmLogger fcmLogger() {
        return new DefaultFcmLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmPayloadBuilder fcmPayloadBuilder() {
        return new DefaultFcmPayloadBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmRetryPolicy fcmRetryPolicy(FcmProperties properties, FcmMetricsRecorder metricsRecorder, FcmLogger fcmLogger, ObservationManager observationManager) {
        FcmProperties.Send send = properties.getSend();

        return new DefaultFcmRetryPolicy(
                send.getMaxRetries(),
                Duration.ofMillis(send.getBaseBackoffMillis()),
                Duration.ofMillis(send.getMaxBackoffMillis()),
                metricsRecorder,
                fcmLogger,
                observationManager
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmResponseErrorHandler fcmResponseErrorHandler(ObjectProvider<FcmTokenService> tokenServiceProvider,
                                                            FcmLogger fcmLogger) {
        return new DefaultFcmResponseErrorHandler(tokenServiceProvider.getIfAvailable(), fcmLogger);
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmMessageSender fcmMessageSender(FcmAccessTokenProvider accessTokenProvider,
                                             @Qualifier(FCM_WEB_CLIENT_QUALIFIER) WebClient webClient,
                                             FcmProperties properties,
                                             ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        String sendUrl = String.format("%s/projects/%s/messages:send",
                properties.getSend().getBaseUrl(), properties.getProjectId());

        ObservationRegistry observationRegistry = resolveObservationRegistry(
                observationRegistryProvider, properties.getObservability().isTracingEnabled());

        return new WebClientFcmMessageSender(accessTokenProvider, webClient, sendUrl,
                observationRegistry, properties.getProjectId());
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmRequestValidator defaultFcmRequestValidator() {
        return new DefaultFcmRequestValidator();
    }

    @Bean
    public ValidatorChain fcmValidatorChain(ObjectProvider<FcmRequestValidator> validatorProvider,
                                            FcmProperties properties) {
        List<FcmRequestValidator> validators = validatorProvider.orderedStream().collect(Collectors.toList());
        return new ValidatorChain(validators, properties.getValidation().isEnabled());
    }

    @Bean
    public EnricherChain fcmEnricherChain(ObjectProvider<FcmMessageEnricher> enricherProvider,
                                          FcmProperties properties) {
        List<FcmMessageEnricher> enrichers = enricherProvider.orderedStream().collect(Collectors.toList());
        return new EnricherChain(enrichers, properties.getExtensibility().isEnrichersEnabled());
    }

    @Bean
    public InterceptorChain fcmInterceptorChain(ObjectProvider<FcmPayloadInterceptor> interceptorProvider,
                                                FcmProperties properties) {
        List<FcmPayloadInterceptor> interceptors = interceptorProvider.orderedStream().collect(Collectors.toList());
        return new InterceptorChain(interceptors, properties.getExtensibility().isInterceptorsEnabled());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveFcmClient reactiveFcmClient(FcmPayloadBuilder payloadBuilder,
                                               FcmMessageSender sender,
                                               FcmResponseErrorHandler errorHandler,
                                               FcmRetryPolicy retryPolicy,
                                               ObjectProvider<FcmTokenService> tokenServiceProvider,
                                               FcmProperties properties,
                                               FcmMetricsRecorder metricsRecorder,
                                               FcmLogger fcmLogger,
                                               ValidatorChain validatorChain,
                                               EnricherChain enricherChain,
                                               InterceptorChain interceptorChain,
                                               FcmExecutionContextFactory fcmExecutionContextFactory,
                                               ObservationManager fcmObservationManager) {
        return new DefaultReactiveFcmClient(
                payloadBuilder, sender, errorHandler, retryPolicy,
                tokenServiceProvider.getIfAvailable(),
                properties.getSend().getConcurrency(),
                properties.getSend().getBatchSize(),
                properties.getSend().getFailureThresholdPerBatch(),
                properties.getProjectId(),
                fcmLogger,
                validatorChain,
                enricherChain,
                interceptorChain,
                fcmExecutionContextFactory,
                fcmObservationManager,
                metricsRecorder
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public FcmClient fcmClient(ReactiveFcmClient reactiveFcmClient) {
        return new DefaultFcmClient(reactiveFcmClient);
    }

    private ObservationRegistry resolveObservationRegistry(ObjectProvider<ObservationRegistry> provider, boolean tracingEnabled) {
        if (!tracingEnabled)
            return ObservationRegistry.NOOP;

        return provider.getIfAvailable(() -> ObservationRegistry.NOOP);
    }
}
