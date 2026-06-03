package io.github.engahmedatef.fcm.internal.service;

import io.github.engahmedatef.fcm.spi.FcmLogEvent;
import io.github.engahmedatef.fcm.spi.FcmLogger;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;

/** SLF4J-backed {@link io.github.engahmedatef.fcm.spi.FcmLogger} that writes FCM log events using MDC for structured metadata. */
@Slf4j
public class DefaultFcmLogger implements FcmLogger {

    @Override
    public void info(FcmLogEvent event, Map<String, Object> metadata) {
        metadata.forEach((k, v) -> MDC.put(k, String.valueOf(v)));
        try {
            log.info(event.getValue());
        } finally {
            metadata.keySet().forEach(MDC::remove);
        }
    }

    @Override
    public void warn(FcmLogEvent event, Map<String, Object> metadata) {
        metadata.forEach((k, v) -> MDC.put(k, String.valueOf(v)));
        try {
            log.warn(event.getValue());
        } finally {
            metadata.keySet().forEach(MDC::remove);
        }
    }

    @Override
    public void error(FcmLogEvent event, Map<String, Object> metadata) {
        metadata.forEach((k, v) -> MDC.put(k, String.valueOf(v)));
        try {
            log.error(event.getValue());
        } finally {
            metadata.keySet().forEach(MDC::remove);
        }
    }

    @Override
    public void error(FcmLogEvent event, Throwable throwable, Map<String, Object> metadata) {
        metadata.forEach((k, v) -> MDC.put(k, String.valueOf(v)));
        try {
            log.error(event.getValue(), throwable);
        } finally {
            metadata.keySet().forEach(MDC::remove);
        }
    }
}
