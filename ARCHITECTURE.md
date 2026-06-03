# Architecture

This document describes the internal design of the `firebase-cloud-messaging` SDK. It is intended for contributors and for consumers who need to extend or replace SDK components.

---

## Design Goals

1. **Reactive by default** — the entire pipeline is non-blocking. Blocking callers are served by a thin adapter; no internal code calls `.block()`.
2. **Zero cascade failures in batch** — a single device failure must never abort the batch; results are wrapped, not propagated.
3. **Replaceable components** — every major bean is `@ConditionalOnMissingBean`. The SPI boundary is stable across releases.
4. **Transparent observability** — every operation emits Micrometer metrics, an Observation span, and an MDC-correlated log, with no opt-in required from the caller.
5. **No Firebase Admin SDK dependency** — the SDK calls the FCM HTTP v1 REST API directly, keeping the classpath lightweight and preserving reactive back-pressure.

---

## Package Structure

```
com.gizasystems.fcm
├── api                     Public send interfaces (FcmClient, ReactiveFcmClient, FcmTokenService)
├── model                   Public domain models (FcmMessage, FcmDevice, FcmNotification, FcmSendResult)
│   ├── android             Android-specific models
│   └── apns                APNs-specific models
├── spi                     Extension-point interfaces and context records
├── exception               Typed exception hierarchy
├── autoconfigure           Spring Boot auto-configuration (FcmAutoConfiguration, FcmProperties)
└── internal                NOT public API — subject to change
    ├── auth                Credential resolution and token providers
    ├── http                WebClient factory
    ├── metrics             FcmMetrics constants, FcmMetricsRecorder
    ├── observability       FcmExecutionContext, ObservationManager
    ├── service             DefaultReactiveFcmClient, chain helpers, retry policy
    │   └── sender          FcmMessageSender, WebClientFcmMessageSender
    └──transport           Wire models (MessagePayload, error/success responses)
        └── target          FcmTarget sealed hierarchy (DeviceTarget, TopicTarget, ConditionTarget)
```

---

## Send Pipeline — Full Execution Flow

Each call to `sendToDevice`, `sendToTopic`, or `sendToCondition` goes through the following synchronous assembly of Reactor operators followed by asynchronous execution on the Reactor scheduler:

```
Caller
  │
  ▼
DefaultReactiveFcmClient.send()
  │  creates FcmExecutionContext (correlationId, Observation, startTime)
  │  subscribes to ObservationManager.start()
  │
  ▼
ValidatorChain.apply(target, message)            ← FcmRequestValidator beans in @Order
  │  returns Mono<Void>; signals error on constraint violation
  │
  ▼
EnricherChain.apply(enricherContext, message)    ← FcmMessageEnricher beans in @Order
  │  returns Mono<FcmMessage> (enriched copy)
  │
  ▼
FcmPayloadBuilder.build(target, enrichedMessage) ← domain → wire model translation
  │  returns MessagePayload (synchronous)
  │
  ▼
InterceptorChain.apply(interceptorContext, payload)  ← FcmPayloadInterceptor beans in @Order
  │  returns Mono<MessagePayload> (wire-level mutation)
  │
  ▼
FcmMessageSender.send(payload)                   ← WebClient HTTP POST to FCM
  │  obtains OAuth token via FcmAccessTokenProvider
  │  returns Mono<FcmSenderResponse>
  │
  ▼
DefaultReactiveFcmClient.handleResponse()
  │  on success  → FcmSendResult.success(messageId)
  │  on FCM error → FcmResponseErrorHandler.handle() → typed FcmException
  │
  ▼
retryWhen(FcmRetryPolicy.create(executionContext))
  │  applies exponential back-off on FcmRetryableException / WebClientRequestException
  │
  ▼
doOnSuccess  → metrics increment (outcome=success), logger, update attributes
doOnError    → metrics increment (outcome=failure, errorCategory), logger
doFinally    → ObservationManager.success|failure|cancel, ObservationManager.stop,
               metricsRecorder.recordTimer(MESSAGES_DURATION)
  │
  ▼
Mono<FcmSendResult> emitted to caller
```

---

## Batch Pipeline

`sendToDevices(Collection<FcmDevice>, FcmMessage)` runs a separate code path optimised for high-cardinality device lists:

```
Flux.fromIterable(devices)
  │
  ▼
.buffer(batchSize)                    ← groups devices into windows of send.batch-size
  │
  ▼
.concatMap(batch ->                   ← windows are processed sequentially
    Flux.fromIterable(batch)
      .flatMap(device ->              ← within each window, up to `concurrency` parallel sends
          send(DeviceTarget(device), message)
            .onErrorResume(           ← fault isolation: errors are wrapped, not propagated
                ex -> Mono.just(FcmSendResult.failure(ex))),
          concurrency)
      .collectList()
      .flatMap(results -> handleBatchResults(context, batch, results))
  )
  │
  ▼
handleBatchResults():
  computes failureRate = failures / batchSize
  records fcm.batch.size, fcm.batch.failure.rate distributions
  if failureRate >= failureThresholdPerBatch:
      records fcm.batch.aborted counter
      signals FcmBatchExceededThresholdException   ← aborts remaining windows
  else:
      returns Mono.empty()                          ← window accepted; advance to next
```

Key design decisions:
- **`concatMap` at the window level, `flatMap` within a window** — this ensures back-pressure against the window pipeline while maximising parallelism within each window.
- **`onErrorResume` wraps per-device failures** — a single expired token or transient error does not prevent the remaining 499 devices in the window from being processed.
- **Threshold abort is per-window, not cumulative** — each `buffer` window is evaluated independently; a partial-outage in one window does not infect subsequent windows unless the failure rate persists.

---

## Target Polymorphism

`FcmTarget` is a sealed marker interface with three record implementations:

```java
sealed interface FcmTarget permits DeviceTarget, TopicTarget, ConditionTarget {}

record DeviceTarget(FcmDevice device) implements FcmTarget {}
record TopicTarget(String topic)      implements FcmTarget {}
record ConditionTarget(String cond)   implements FcmTarget {}
```

`instanceof` pattern matching is used in `DefaultFcmPayloadBuilder` and `DefaultFcmResponseErrorHandler` to branch on target type. This avoids virtual dispatch through a common `build()` method and keeps the payload logic co-located.

---

## Authentication

The SDK resolves credentials once at startup via `CredentialResolver` and instantiates the appropriate `FcmAccessTokenProvider`:

| `credentials.type` | Token provider | Notes |
|---|---|---|
| `APPLICATION_DEFAULT` | `GoogleCredentialsAccessTokenProvider` | Uses Google ADC chain (env var → metadata server → gcloud) |
| `SERVICE_ACCOUNT_JSON` | `JwtAccessTokenProvider` | Self-signs a JWT from the JSON key; proactively refreshes `refreshBufferSeconds` before expiry |

The token is obtained asynchronously per-send by calling `FcmAccessTokenProvider.getAccessToken(executionContext)` inside `WebClientFcmMessageSender`. The default implementation for ADC caches the token and refreshes only when the remaining validity falls below the buffer threshold.

---

## Retry Model

```
send attempt 1  ─────────────────────────── success → done
                                              │
                                         FcmRetryableException
                                              │
                                        wait: base * 2^n + jitter
                                              │
send attempt 2  ─────────────────────────── success → done
                                              │
                                         FcmRetryableException
                                              │
send attempt 3  ─────────────────────────── FcmRetryableException
                                              │
                                        retries exhausted
                                        metric: fcm.messages.retries.exhausted
                                        propagate original exception
```

**Retryable exceptions:**
- `FcmRetryableException` and all subtypes (`FcmUnavailableException`, `FcmInternalException`, `FcmTransportException`)
- `WebClientRequestException` (network-level errors: timeouts, connection reset, DNS failure)

**Non-retryable exceptions** (propagated immediately):
- `FcmInvalidTokenException` — invalid token; retrying will never succeed
- `FcmAuthenticationException` — token failure; retrying without fixing auth is futile
- `FcmInvalidRequestException` — payload rejected; the message itself is malformed
- `FcmQuotaExceededException` — quota exhausted; back-off must be much longer than `maxBackoffMillis`

---

## Error Classification

`DefaultFcmResponseErrorHandler` maps FCM HTTP v1 error codes to typed exceptions:

| FCM Error Code | HTTP Status | Exception |
|---|---|---|
| `UNREGISTERED` / `SENDER_ID_MISMATCH` | 200 with error body | `FcmInvalidTokenException` |
| `INVALID_ARGUMENT` | 400 | `FcmInvalidRequestException` |
| `QUOTA_EXCEEDED` | 429 | `FcmQuotaExceededException` |
| `UNAVAILABLE` | 503 | `FcmUnavailableException` |
| `INTERNAL` | 500 | `FcmInternalException` |
| `THIRD_PARTY_AUTH_ERROR` | 401 | `FcmAuthenticationException` |
| unknown code | any | `FcmUnknownException` |

When `FcmInvalidTokenException` is thrown and an `FcmTokenService` bean is present, the error handler invokes `tokenService.deleteToken(deviceToken)` before signalling the error downstream.

---

## Observability Model

Every individual send creates one `FcmExecutionContext`:

```
FcmExecutionContext
  ├── correlationId       (UUID, propagated to MDC and metrics tag)
  ├── projectId           (from FcmProperties)
  ├── targetType          (device | topic | condition)
  ├── startTime           (Instant.now() at creation)
  ├── retryCount          (AtomicInteger, incremented by retry policy)
  ├── observation         (Micrometer Observation, linked to ObservationRegistry)
  └── attributes          (ConcurrentHashMap for cross-component data sharing)
```

The `ObservationManager` wraps the Micrometer `Observation` lifecycle:

| Signal | Observation call | Effect |
|---|---|---|
| pipeline starts | `start()` | opens span; starts timer |
| terminal `ON_COMPLETE` | `success()` | sets outcome=success on span |
| terminal `ON_ERROR` | `failure(exception)` | records exception on span |
| terminal `CANCEL` | `cancel()` | marks span as cancelled |
| always in `doFinally` | `stop()` | closes span; flushes to tracer |

For batch operations, the parent `FcmExecutionContext` is placed in the Reactor `Context`. Each per-device call to `send()` reads the parent via `contextView.getOrDefault(FcmExecutionContext.KEY, null)` and creates a child context via `FcmExecutionContextFactory.create(projectId, target, parentContext)`, establishing the parent–child span relationship.

---

## Concurrency Model

| Layer | Concurrency mechanism | Notes |
|---|---|---|
| Single-device send | Reactor event loop (non-blocking) | No dedicated thread; all I/O is NIO via Reactor Netty |
| Batch window | `Flux.flatMap` with bounded concurrency | Up to `send.concurrency` in-flight sends per window |
| Window sequencing | `Flux.concatMap` | Windows are processed one at a time to enable threshold evaluation before advancing |
| Token refresh | `Mono.fromCallable` + Reactor scheduler | Blocks only a bounded thread from the elastic scheduler; normal operation uses cached token |
| Blocking `FcmClient` | Calls `.block()` | Must only be used outside the Reactor event loop |

---

## Two Model Layers

The SDK maintains a strict boundary between two model layers:

| Layer | Package | Produced by | Consumed by |
|---|---|---|---|
| Domain model | `model.*` | Callers / enrichers | `FcmPayloadBuilder` |
| Wire model | `internal.transport.*` | `FcmPayloadBuilder` | `FcmMessageSender`, interceptors |

`FcmPayloadMapper` handles primitive field translation. `DefaultFcmPayloadBuilder` assembles the full `MessagePayload` record. The separation ensures:
- FCM HTTP API schema changes are isolated to `internal.transport.*` and `DefaultFcmPayloadBuilder`
- Callers and enrichers are never exposed to wire-format details
- Interceptors operate on the final serialization-ready object, not on domain concepts

---

## Why Project Reactor?

| Requirement | Reactor capability used |
|---|---|
| Non-blocking HTTP | `WebClient` backed by Reactor Netty (NIO) |
| Per-device fault isolation in batch | `onErrorResume` on individual `Mono<FcmSendResult>` within `flatMap` |
| Bounded concurrency | `flatMap(mapper, concurrency)` |
| Context propagation without ThreadLocal | `Mono.deferContextual` + `contextWrite` |
| Retry with back-off | `retryWhen(Retry.backoff(...))` |
| Composable pipeline | Lazy `Mono` assembly executed once per subscription |

An imperative approach would require explicit thread-pool management, `CompletableFuture` chaining, and `ThreadLocal`-based context propagation — all of which scale poorly under high batch concurrency and integrate poorly with non-blocking Spring WebFlux applications.

---

## Design Trade-offs

**Reactive-only internal API**
The blocking `FcmClient` is a thin adapter. All logic is in the reactive path. This means the blocking client cannot be used inside reactive pipelines without risking event-loop starvation, but it is an explicit trade-off documented in the `FcmClient` Javadoc.

**No built-in dead-letter queue**
The SDK provides retry with back-off but does not include a DLQ. Messages that exhaust the retry budget propagate as exceptions. Callers requiring guaranteed delivery should catch `FcmRetryableException` (or all `FcmException`) at the batch level and route failures to an application-level queue.

**Per-window threshold (not cumulative)**
The batch failure threshold is evaluated per window, not across the entire device list. This means a transient spike in failures can abort one window while subsequent windows proceed normally. If cumulative failure tracking is required, it must be implemented at the `FcmTokenService` or application layer.

**No built-in token de-duplication**
The SDK sends to every `FcmDevice` it receives. Duplicate tokens in the input list result in duplicate sends. De-duplication is the responsibility of the `FcmTokenService` or the caller's device list.
