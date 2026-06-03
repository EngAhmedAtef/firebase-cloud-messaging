![Java](https://img.shields.io/badge/java-17-blue)
![Spring Boot](https://img.shields.io/badge/spring%20boot-3.x-green)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)
![Release](https://img.shields.io/badge/version-1.0.0-orange)

# firebase-cloud-messaging

A production-grade Spring Boot SDK for sending push notifications through the **Firebase Cloud Messaging (FCM) HTTP v1 API**. The library is built on Project Reactor, exposes both reactive and blocking APIs, and is designed to handle high-throughput batch notification delivery with per-device fault isolation, automatic retries, and first-class observability.

The SDK calls the FCM HTTP v1 endpoint directly — it does **not** depend on the Firebase Admin SDK — keeping the dependency footprint small and the reactive pipeline unblocked.

## Architecture

The SDK is designed around a reactive, pipeline-based architecture optimized for high-throughput batch delivery.

Key design goals:
- Per-device fault isolation
- Fully non-blocking execution model
- Pluggable extension points (SPI-based)
- Production-grade observability

Full design details: [Architecture Overview](./ARCHITECTURE.md)

---

## Key Features

- **Reactive-first** — all logic runs on a non-blocking pipeline (`Mono` / `Flux`); the blocking API is a thin adapter over the reactive core
- **Batch delivery at scale** — configurable window size, concurrency limit, and per-batch failure-rate threshold with automatic abort-on-breach
- **Per-device fault isolation** — individual device failures within a batch are captured as `FcmSendResult` rather than terminating the stream
- **Automatic retry with exponential back-off** — jitter, configurable max attempts, and a pluggable `FcmRetryPolicy` SPI
- **Typed exception hierarchy** — every FCM error code maps to a specific subtype (`FcmInvalidTokenException`, `FcmUnavailableException`, `FcmQuotaExceededException`, etc.)
- **Automatic stale token cleanup** — when `FcmInvalidTokenException` is raised and an `FcmTokenService` bean is registered, the SDK removes the dead token before completing
- **Composable extension pipeline** — three ordered SPI chains: `FcmRequestValidator` → `FcmMessageEnricher` → `FcmPayloadInterceptor`
- **Full observability** — Micrometer counters, timers, distribution summaries, Micrometer Observation spans (OpenTelemetry-compatible), and MDC correlation ID propagation
- **Spring Boot Auto-configuration** — zero boilerplate wiring; activate with a single annotation and two config properties
- **Interface-driven** — every major component is `@ConditionalOnMissingBean` and replaceable with a custom bean

---

## Requirements

| Dependency | Minimum Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.x |
| Project Reactor | (transitive via Spring WebFlux) |
| Micrometer | (transitive via Spring Boot Actuator, optional) |

---

## Installation

**Maven**

```xml
<dependency>
    <groupId>com.github.engahmedatef</groupId>
    <artifactId>firebase-cloud-messaging</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'com.github.engahmedatef:firebase-cloud-messaging:1.0.0'
```

---

## Quick Start

### 1. Set the required properties

```yaml
fcm:
  project-id: your-firebase-project-id
  auth:
    credentials:
      type: APPLICATION_DEFAULT   # or SERVICE_ACCOUNT_JSON
```

### 2. Inject and use

```java
@Service
public class NotificationService {

    private final ReactiveFcmClient reactiveFcmClient; // reactive API
    private final FcmClient fcmClient;                 // blocking API

    public NotificationService(ReactiveFcmClient reactiveFcmClient, FcmClient fcmClient) {
        this.reactiveFcmClient = reactiveFcmClient;
        this.fcmClient = fcmClient;
    }
}
```

---

## Usage Examples

### Sending a single notification

```java
FcmDevice device = FcmDevice.builder()
        .type(FcmDeviceType.ANDROID)
        .token("device-registration-token")
        .build();

FcmMessage message = FcmMessage.builder()
        .notification(FcmNotification.builder()
                .title("Your order has shipped")
                .body("Order #12345 is on its way.")
                .build())
        .data(Map.of("orderId", "12345", "screen", "order_tracking"))
        .build();

// Reactive
Mono<FcmSendResult> reactiveResult = reactiveFcmClient.sendToDevice(device, message);

// Blocking
FcmSendResult blockingResult = fcmClient.sendToDevice(device, message);
```

### Sending to multiple devices (batch)

```java
List<FcmDevice> devices = fetchAllDevices(); // your data source

// Reactive — processes up to `send.concurrency` devices in parallel within each `send.batch-size` window
reactiveFcmClient.sendToDevices(devices, message)
        .doOnSuccess(v -> log.info("Batch completed"))
        .doOnError(FcmBatchExceededThresholdException.class,
                e -> log.warn("Batch aborted — failure rate exceeded threshold"))
        .subscribe();

// Blocking
try {
    fcmClient.sendToDevices(devices, message);
    log.info("Batch completed");
} catch (FcmBatchExceededThresholdException e) {
    log.warn("Batch aborted — failure rate exceeded threshold");
}
```

### Handling per-device results in a batch

For visibility into individual outcomes, process each device individually and collect results:

```java
Flux.fromIterable(devices)
    .flatMap(device ->
        reactiveFcmClient.sendToDevice(device, message)
            .map(r -> Map.entry(device, r))
            .onErrorResume(ex -> Mono.just(Map.entry(device, FcmSendResult.failure(ex)))),
        32 // concurrency
    )
    .doOnNext(entry -> {
        if (entry.getValue().isSuccess()) {
            log.info("Sent to {}: messageId={}", entry.getKey().token(), entry.getValue().getMessageId());
        } else {
            log.warn("Failed for {}: {}", entry.getKey().token(), entry.getValue().getError().getMessage());
        }
    })
    .then()
    .subscribe();
```

### Sending to a topic

```java
// Reactive
reactiveFcmClient.sendToTopic("breaking-news", message)
        .subscribe(result -> log.info("Topic send: {}", result.getMessageId()));

// Blocking
FcmSendResult result = fcmClient.sendToTopic("breaking-news", message);
log.info("Topic send: {}", result.getMessageId());
```

### Sending to a topic condition

```java
String condition = "'TopicA' in topics && 'TopicB' in topics";

// Reactive
reactiveFcmClient.sendToCondition(condition, message)
        .subscribe(result -> log.info("Condition send: {}", result.getMessageId()));

// Blocking
FcmSendResult result = fcmClient.sendToCondition(condition, message);
log.info("Condition send: {}", result.getMessageId());
```

### Sending to all devices of a user

Requires a registered `FcmTokenService` bean (see [Token Service](#token-service)):

```java
// Reactive
reactiveFcmClient.sendToUser(userId, message)
        .doOnSuccess(v -> log.info("All user devices notified"))
        .subscribe();

// Blocking
fcmClient.sendToUser(userId, message);
log.info("All user devices notified");
```

### Android-specific configuration

```java
FcmMessage message = FcmMessage.builder()
        .notification(FcmNotification.builder().title("Alert").body("Details here").build())
        .androidConfig(FcmAndroidConfig.builder()
                .priority(FcmAndroidMessagePriority.HIGH)
                .ttl(Duration.ofHours(1))
                .notification(FcmAndroidNotification.builder()
                        .channelId("alerts")
                        .icon("ic_notification")
                        .color("#FF5722")
                        .clickAction("OPEN_ALERT_SCREEN")
                        .build())
                .build())
        .build();
```

### iOS (APNs) configuration

```java
FcmMessage message = FcmMessage.builder()
        .notification(FcmNotification.builder().title("New message").body("You have 3 new messages").build())
        .apnsConfig(FcmApnsConfig.builder()
                .headers(Map.of("apns-priority", "10"))
                .aps(FcmAps.builder()
                        .badge(3)
                        .sound("default")
                        .mutableContent(true)
                        .build())
                .build())
        .build();
```

### Integration inside a Spring Boot service

**Reactive service** (use in reactive controllers and services; never call `.block()` on the Reactor scheduler thread):

```java
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final ReactiveFcmClient reactiveFcmClient;
    private final DeviceRepository deviceRepository;

    public Mono<Void> notifyOrderShipped(String userId, String orderId) {
        FcmMessage message = FcmMessage.builder()
                .notification(FcmNotification.builder()
                        .title("Order Shipped")
                        .body("Your order " + orderId + " has been shipped.")
                        .build())
                .data(Map.of("orderId", orderId, "event", "ORDER_SHIPPED"))
                .build();

        return deviceRepository.findDevicesByUserId(userId)
                .collectList()
                .flatMap(devices -> reactiveFcmClient.sendToDevices(devices, message));
    }
}
```

**Blocking service** (use in synchronous code paths such as scheduled jobs running on a dedicated thread pool):

```java
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final FcmClient fcmClient;
    private final DeviceRepository deviceRepository;

    public void notifyOrderShipped(String userId, String orderId) {
        FcmMessage message = FcmMessage.builder()
                .notification(FcmNotification.builder()
                        .title("Order Shipped")
                        .body("Your order " + orderId + " has been shipped.")
                        .build())
                .data(Map.of("orderId", orderId, "event", "ORDER_SHIPPED"))
                .build();

        List<FcmDevice> devices = deviceRepository.findDevicesByUserId(userId);
        fcmClient.sendToDevices(devices, message);
    }
}
```

---

## Configuration Reference

All properties are under the prefix `fcm`. The library is inactive unless `project-id` is set.

```yaml
fcm:
  project-id: your-firebase-project-id          # required

  auth:
    credentials:
      type: APPLICATION_DEFAULT                  # APPLICATION_DEFAULT | SERVICE_ACCOUNT_JSON
      json-path: /secrets/service-account.json  # only for SERVICE_ACCOUNT_JSON
      scope: https://www.googleapis.com/auth/firebase.messaging
    oauth:
      token-endpoint: https://oauth2.googleapis.com/token
    jwt:
      refresh-buffer-seconds: 60

  send:
    base-url: https://fcm.googleapis.com/v1
    max-retries: 3                               # retry budget per individual send
    base-backoff-millis: 500
    max-backoff-millis: 10000
    concurrency: 32                              # parallel devices per batch window
    batch-size: 500                              # devices buffered per window
    failure-threshold-per-batch: 0.5            # abort batch if failure rate >= this

  http:
    connect-timeout-millis: 5000
    response-timeout-millis: 10000
    max-in-memory-size-mb: 10

  observability:
    metrics-enabled: true
    tracing-enabled: true
    correlation-enabled: true

  extensibility:
    interceptors-enabled: true
    enrichers-enabled: true

  validation:
    enabled: true
```

---

## Error Handling

All SDK exceptions extend `FcmException` (unchecked). Catch specific subtypes rather than the base class.

| Exception | FCM Error Code | Retryable | Description |
|---|---|---|---|
| `FcmInvalidTokenException` | `UNREGISTERED` | No | Device token is invalid or the app was uninstalled |
| `FcmAuthenticationException` | — | No | OAuth 2.0 token acquisition failed |
| `FcmInvalidRequestException` | `INVALID_ARGUMENT` | No | Message failed SDK validation or FCM rejected the payload |
| `FcmUnavailableException` | `UNAVAILABLE` | Yes | FCM returned 503; back-off and retry |
| `FcmQuotaExceededException` | `QUOTA_EXCEEDED` | No | Project daily quota exhausted |
| `FcmTransportException` | — | Yes | Network-level error (timeout, connection reset) |
| `FcmBatchExceededThresholdException` | — | No | Batch failure rate exceeded `failure-threshold-per-batch` |
| `FcmTokenServiceNotDefinedException` | — | No | `sendToUser()` called without an `FcmTokenService` bean |
| `FcmInternalException` | `INTERNAL` | Yes | Unexpected SDK or FCM internal error |
| `FcmUnknownException` | unknown | No | FCM error code not recognised by the SDK |

---

## Retry Behaviour

The default `FcmRetryPolicy` applies exponential back-off with jitter to any exception that is a `FcmRetryableException` subtype or a `WebClientRequestException` (network-level error):

- Up to `send.max-retries` attempts (default: **3**)
- Initial delay: `send.base-backoff-millis` (default: **500 ms**)
- Maximum delay cap: `send.max-backoff-millis` (default: **10 000 ms**)
- Jitter factor: **30%** (prevents thundering-herd during outages)
- Each retry increments the `FcmExecutionContext` retry counter and emits a `fcm.messages.retried` metric
- When the retry budget is exhausted, a `fcm.messages.retries.exhausted` metric is recorded and the original exception is propagated

Override the retry strategy by registering a custom `FcmRetryPolicy` bean:

```java
@Bean
public FcmRetryPolicy customRetryPolicy() {
    return executionContext -> Retry.fixedDelay(5, Duration.ofSeconds(2))
            .filter(ex -> ex instanceof FcmRetryableException);
}
```

---

## Observability

### Metrics

All metrics use the Micrometer API and are registered with the `MeterRegistry` in the application context (disabled when `observability.metrics-enabled=false`).

| Metric | Type | Tags | Description |
|---|---|---|---|
| `fcm.messages.sent` | Counter | `outcome`, `projectId`, `target`, `deviceType` | Total send attempts |
| `fcm.messages.duration` | Timer | `projectId`, `target`, `deviceType` | End-to-end send latency |
| `fcm.messages.retried` | Counter | `exception`, `projectId`, `target` | Individual retry attempts |
| `fcm.messages.retries.exhausted` | Counter | `exception`, `projectId`, `target` | Sends that used the full retry budget |
| `fcm.errors.classified` | Counter | `errorCategory`, `projectId`, `target` | Failures grouped by exception type |
| `fcm.batch.size` | Distribution | `projectId` | Device count per batch window |
| `fcm.batch.failure.rate` | Distribution | `projectId` | Failure fraction per batch window |
| `fcm.batch.aborted` | Counter | `projectId` | Batches aborted due to threshold breach |
| `fcm.token.refresh` | Counter | `tokenProvider`, `projectId` | OAuth token refresh events |
| `fcm.token.refresh.duration` | Timer | `tokenProvider`, `projectId` | Token refresh latency |

### Distributed Tracing

When `observability.tracing-enabled=true` and a Micrometer `ObservationRegistry` is present, the SDK creates a Micrometer `Observation` per send operation. This integrates transparently with Zipkin, Jaeger, or any OpenTelemetry-compatible collector via the micrometer-tracing bridge already configured by Spring Boot Actuator.

Parent–child span relationships are preserved for batch sends: the batch creates one root observation and each per-device send creates a child observation linked via the Reactor `Context`.

### Correlation ID

When `observability.correlation-enabled=true`, the SDK generates a UUID correlation ID for every operation and:

1. Stores it in the Reactor `Context` under `FcmExecutionContext.KEY`
2. Adds it as the `correlationId` tag on all metrics
3. Propagates it to the SLF4J MDC under the key `fcmId`

---

## Extension Points (SPI)

Every major component is declared `@ConditionalOnMissingBean`. Register a bean of the corresponding type to replace the default behaviour.

| SPI Interface | Role | Override to... |
|---|---|---|
| `FcmRequestValidator` | Pre-send validation | Add custom message constraints |
| `FcmMessageEnricher` | Domain-level mutation | Stamp locale, default TTL, tenant ID, etc. |
| `FcmPayloadInterceptor` | Wire-level mutation | Audit logging, payload encryption, header injection |
| `FcmRetryPolicy` | Retry strategy | Custom back-off, circuit breaker, or dead-letter routing |
| `FcmResponseErrorHandler` | FCM error translation | Custom error mapping or stale token cleanup hooks |
| `FcmAccessTokenProvider` | OAuth token supply | Custom token caching or non-Google credentials |
| `FcmPayloadBuilder` | Domain → wire translation | Custom payload construction |
| `FcmLogger` | Structured log sink | Route SDK events to a different framework |
| `FcmWebClientBuilderCustomizer` | WebClient construction hook | Custom TLS, proxy, or codec configuration |
| `@FcmExchangeFilter` (qualifier on `ExchangeFilterFunction` beans) | Per-request exchange filter | Add request/response logging, custom headers, or tracing filters |
| `FcmTokenService` | User → device resolution | Enable `sendToUser()` and auto token cleanup |

Use `@Order` on beans to control execution order within each chain.

### Request Validator Example

```java
@Component
@Order(1)
public class TenantFcmValidator implements FcmRequestValidator {

    @Override
    public Mono<Void> validate(FcmTarget target, FcmMessage message) {
        if (!message.getData().containsKey("tenantId")) {
            return Mono.error(new FcmInvalidRequestException("tenantId is required in message data"));
        }
        return Mono.empty();
    }
}
```

### Message Enricher Example

```java
@Component
@Order(10)
public class LocaleEnricher implements FcmMessageEnricher {

    @Override
    public Mono<FcmMessage> enrich(FcmEnricherContext context, FcmMessage message) {
        message.getData().put("locale", resolveLocale(context));
        return Mono.just(message);
    }
}
```

### Payload Interceptor Example

```java
@Component
@Order(5)
public class AuditInterceptor implements FcmPayloadInterceptor {

    @Override
    public Mono<MessagePayload> intercept(FcmInterceptorContext context, MessagePayload payload) {
        auditLog.record(context.projectId(), context.target(), payload);
        return Mono.just(payload);
    }
}
```

### WebClient Builder Customizer Example

`FcmWebClientBuilderCustomizer` is a one-time hook invoked during `WebClient` construction. Use it to configure TLS context, proxy settings, codecs, or base headers that apply to every FCM HTTP call.

```java
@Component
@Order(1)
public class FcmProxyCustomizer implements FcmWebClientBuilderCustomizer {

    @Override
    public void customize(WebClient.Builder builder) {
        builder.defaultHeader("X-App-Version", "2.1.0");
    }
}
```

Multiple customizers are applied in `@Order` sequence. For _per-request_ mutation, use `@FcmExchangeFilter` instead.

### Exchange Filter Example

`@FcmExchangeFilter` is a qualifier annotation that marks `ExchangeFilterFunction` beans for inclusion in the FCM `WebClient`'s filter chain. This lets you add per-request logic (logging, header injection, tracing) without interfering with any other `ExchangeFilterFunction` beans registered elsewhere in the application context.

```java
@Bean
@FcmExchangeFilter
@Order(1)
public ExchangeFilterFunction fcmRequestLoggingFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(request -> {
        log.debug("FCM request: {} {}", request.method(), request.url());
        return Mono.just(request);
    });
}
```

```java
@Bean
@FcmExchangeFilter
@Order(2)
public ExchangeFilterFunction fcmResponseLoggingFilter() {
    return ExchangeFilterFunction.ofResponseProcessor(response -> {
        log.debug("FCM response: {}", response.statusCode());
        return Mono.just(response);
    });
}
```

Register any number of `@FcmExchangeFilter`-qualified beans; they are applied in `@Order` sequence. Only beans carrying this qualifier are added to the FCM `WebClient` — other `ExchangeFilterFunction` beans in the context are unaffected.

---

## Token Service

Implement `FcmTokenService` to enable user-scoped sends and automatic stale token removal:

```java
@Service
public class MyFcmTokenService implements FcmTokenService {

    private final DeviceRepository deviceRepository;

    @Override
    public Mono<Collection<FcmDevice>> getUserDevices(Object userId) {
        return deviceRepository.findByUserId((String) userId)
                .map(d -> FcmDevice.builder()
                        .type(d.getPlatform())
                        .token(d.getFcmToken())
                        .build())
                .collectList()
                .map(Collection.class::cast);
    }

    @Override
    public Mono<Void> deleteToken(String token) {
        return deviceRepository.deleteByFcmToken(token);
    }
}
```

When `FcmInvalidTokenException` is thrown and this bean is present, the SDK automatically calls `deleteToken()` before propagating the error.

---

## Best Practices

**Credential management**
- Use `APPLICATION_DEFAULT` in containerised environments (GKE, Cloud Run) via Workload Identity rather than mounting service-account JSON files.
- If a JSON key is required, mount it as a Kubernetes Secret and reference it via `auth.credentials.json-path`.

**Batch sizing**
- Set `send.batch-size` to 500 and `send.concurrency` to 32 as a starting point; profile under load and adjust based on FCM quota limits and observed latency.
- Set `send.failure-threshold-per-batch` conservatively (0.3–0.5) to abort early on systemic outages rather than burning through the entire device list.

**Retry configuration**
- Do not increase `send.max-retries` above 5 for user-visible notifications; prefer dead-letter queue patterns for guaranteed delivery.
- Monitor `fcm.messages.retries.exhausted` in production; a sustained non-zero rate indicates a systemic issue.

**Observability**
- Expose the Micrometer metrics endpoint and alert on `fcm.batch.aborted` and `fcm.messages.retries.exhausted`.
- Include the `fcmId` MDC key in log patterns to correlate SDK logs with distributed traces.

**Token lifecycle**
- Register an `FcmTokenService` even for simple deployments to avoid accumulating stale tokens in your database.
- Periodically audit `FcmInvalidTokenException` counts per project to detect mass uninstalls or token rotation cycles.

**Thread model**
- Inject `ReactiveFcmClient` in reactive controllers and services; never call `.block()` on the Reactor scheduler thread.
- Use `FcmClient` only in non-reactive, synchronous code paths (e.g. scheduled jobs running on a dedicated thread pool).

---

## License

[Apache License 2.0](LICENSE)
