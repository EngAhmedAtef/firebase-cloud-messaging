package com.github.engahmedatef.fcm.spi;

import java.util.Map;

/**
 * Structured logging sink for SDK-internal events. Override the default bean to route log output
 * to a different framework, add fields, or suppress specific events.
 *
 * <p>Each method receives a {@link FcmLogEvent} constant and a metadata map. Implementations
 * must be non-blocking — avoid I/O on the calling thread.
 */
public interface FcmLogger {

    /**
     * Logs an informational event.
     *
     * @param event    the log event constant
     * @param metadata key-value pairs providing structured context (e.g. target, messageId)
     */
    void info(FcmLogEvent event, Map<String, Object> metadata);

    /**
     * Logs a warning event.
     *
     * @param event    the log event constant
     * @param metadata key-value pairs providing structured context
     */
    void warn(FcmLogEvent event, Map<String, Object> metadata);

    /**
     * Logs an error event without a cause.
     *
     * @param event    the log event constant
     * @param metadata key-value pairs providing structured context
     */
    void error(FcmLogEvent event, Map<String, Object> metadata);

    /**
     * Logs an error event with a cause.
     *
     * @param event     the log event constant
     * @param throwable the underlying exception
     * @param metadata  key-value pairs providing structured context
     */
    void error(FcmLogEvent event, Throwable throwable, Map<String, Object> metadata);
}
