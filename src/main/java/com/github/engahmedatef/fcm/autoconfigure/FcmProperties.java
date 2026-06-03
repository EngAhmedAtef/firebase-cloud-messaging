package com.github.engahmedatef.fcm.autoconfigure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized configuration for the FCM SDK, bound to the prefix {@code com.github.engahmedatef.fcm}.
 *
 * <p>The library is activated only when {@code com.github.engahmedatef.fcm.project-id} is set.
 * All other properties have production-safe defaults and are fully overridable.
 *
 * <p>Nested groups:
 * <ul>
 *   <li>{@link Auth} — credential type, OAuth token endpoint, JWT expiry settings</li>
 *   <li>{@link Send} — concurrency, batch size, retry limits, failure threshold, FCM base URL</li>
 *   <li>{@link Http} — WebClient timeouts and in-memory buffer limits</li>
 *   <li>{@link Observability} — Micrometer metrics, tracing, and MDC correlation settings</li>
 *   <li>{@link Extensibility} — global toggles for the enricher and interceptor chains</li>
 *   <li>{@link Validation} — global toggle for SDK-side request validation</li>
 * </ul>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "com.github.engahmedatef.fcm")
public class FcmProperties {

    private String projectId;
    private Auth auth = new Auth();
    private Send send = new Send();
    private Http http = new Http();
    private Observability observability = new Observability();
    private Extensibility extensibility = new Extensibility();
    private Validation validation = new Validation();

    /** WebClient HTTP connection and response timeout settings. */
    @Getter
    @Setter
    public static class Http {
        /** TCP connect timeout in milliseconds. Default: {@code 5000}. */
        @Positive
        private int connectTimeoutMillis = 5000;
        /** Maximum time in milliseconds to wait for a full HTTP response. Default: {@code 10000}. */
        @Positive
        private int responseTimeoutMillis = 10000;
        /** Maximum in-memory buffer size in megabytes for response bodies. Default: {@code 10}. */
        @Positive
        private int maxInMemorySizeMb = 10;
    }

    /** Authentication configuration: credential type, OAuth, and JWT settings. */
    @Getter
    @Setter
    public static class Auth {
        private Oauth oauth = new Oauth();
        private JWT jwt = new JWT();
        private Credentials credentials = new Credentials();
    }

    /** JWT self-signed token settings used when {@code credentials.type = SERVICE_ACCOUNT_JSON}. */
    @Getter
    @Setter
    public static class JWT {
        /** Number of seconds before expiry at which the SDK proactively refreshes the token. Default: {@code 60}. */
        @Positive
        private int refreshBufferSeconds = 60;
    }

    /** OAuth 2.0 token exchange settings used when {@code credentials.type = APPLICATION_DEFAULT}. */
    @Getter
    @Setter
    public static class Oauth {
        /** Google OAuth 2.0 token endpoint URL. Default: {@code https://oauth2.googleapis.com/token}. */
        @NotBlank
        @NotNull
        private String tokenEndpoint = "https://oauth2.googleapis.com/token";
    }

    /** Message delivery and batch pipeline settings. */
    @Getter
    @Setter
    public static class Send {
        /** FCM HTTP v1 API base URL. Default: {@code https://fcm.googleapis.com/v1}. */
        @NotBlank
        @NotNull
        private String baseUrl = "https://fcm.googleapis.com/v1";
        /** Maximum number of retry attempts per individual send. Default: {@code 3}. */
        @Positive
        private int maxRetries = 3;
        /** Base back-off duration in milliseconds for the exponential retry strategy. Default: {@code 500}. */
        @Positive
        private int baseBackoffMillis = 500;
        /** Maximum back-off cap in milliseconds regardless of retry count. Default: {@code 10000}. */
        @Positive
        private int maxBackoffMillis = 10000;
        /** {@code flatMap} concurrency limit for batch sends (devices processed in parallel per batch window). Default: {@code 32}. */
        @Positive
        private int concurrency = 32;
        /** Number of devices buffered into a single batch window. Default: {@code 500}. */
        @Positive
        private int batchSize = 500;
        /**
         * Fraction of failures in a batch window that triggers {@link com.github.engahmedatef.fcm.exception.FcmBatchExceededThresholdException}.
         * Range: {@code 0.0}–{@code 1.0}. Default: {@code 0.5} (abort when 50% of devices in a window fail).
         */
        @Positive
        private double failureThresholdPerBatch = 0.5;
    }

    /** Google credential settings for obtaining an OAuth 2.0 access token. */
    @Getter
    @Setter
    public static class Credentials {
        /**
         * Credential strategy. {@link FcmCredentialType#APPLICATION_DEFAULT} uses the Google
         * Application Default Credentials chain (recommended for GKE / Cloud Run).
         * {@link FcmCredentialType#SERVICE_ACCOUNT_JSON} reads a service-account key file.
         */
        private FcmCredentialType type = FcmCredentialType.APPLICATION_DEFAULT;
        /** Absolute path to a service-account JSON key file. Required when {@code type = SERVICE_ACCOUNT_JSON}. */
        private String jsonPath;
        /** OAuth 2.0 scope for the FCM API. Default: {@code https://www.googleapis.com/auth/firebase.messaging}. */
        @NotBlank
        @NotNull
        private String scope = "https://www.googleapis.com/auth/firebase.messaging";
    }

    /** Micrometer metrics, distributed tracing, and MDC correlation settings. */
    @Getter
    @Setter
    public static class Observability {
        /** Enables Micrometer counter/timer/distribution recording. Default: {@code true}. */
        private boolean metricsEnabled = true;
        /** Enables Micrometer Observation-based distributed tracing spans. Default: {@code true}. */
        private boolean tracingEnabled = true;
        /** Enables per-request correlation ID generation and MDC propagation. Default: {@code true}. */
        private boolean correlationEnabled = true;
    }

    /** Global on/off switches for the enricher and interceptor extension chains. */
    @Getter
    @Setter
    public static class Extensibility {
        /** When {@code false}, registered {@link com.github.engahmedatef.fcm.spi.FcmPayloadInterceptor} beans are bypassed. Default: {@code true}. */
        private boolean interceptorsEnabled = true;
        /** When {@code false}, registered {@link com.github.engahmedatef.fcm.spi.FcmMessageEnricher} beans are bypassed. Default: {@code true}. */
        private boolean enrichersEnabled = true;
    }

    /** SDK-side pre-send request validation settings. */
    @Getter
    @Setter
    public static class Validation {
        /**
         * Enables SDK-side request validation (topic name format, condition syntax, APNs constraints).
         * When {@code false}, messages are forwarded to FCM without local validation and Firebase
         * becomes the authoritative source for error reporting.
         * Default: {@code true}.
         */
        private boolean enabled = true;
    }
}
