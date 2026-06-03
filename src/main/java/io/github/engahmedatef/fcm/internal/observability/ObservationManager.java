package io.github.engahmedatef.fcm.internal.observability;

/** Manages the lifecycle of a send observation: start, success, failure, retry, cancel, stop. */
public interface ObservationManager {

    void start(FcmExecutionContext context);

    void success(FcmExecutionContext context);

    void failure(FcmExecutionContext context, Throwable error);

    void retry(FcmExecutionContext context, Throwable error);

    void cancel(FcmExecutionContext context);

    void stop(FcmExecutionContext context);
}
