package com.github.engahmedatef.fcm.internal.observability;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

import java.util.UUID;

public final class FcmCorrelationId {
    public static final String CONTEXT_KEY = "fcmId";
    public static final String HEADER = "X-Fcm-Request-Id";

    private FcmCorrelationId() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static final class MdcAccessor implements ThreadLocalAccessor<String> {
        @Override
        public Object key() {
            return CONTEXT_KEY;
        }

        @Override
        public String getValue() {
            return MDC.get(CONTEXT_KEY);
        }

        @Override
        public void setValue(String value) {
            MDC.put(CONTEXT_KEY, value);
        }

        @Override
        public void reset() {
            MDC.remove(CONTEXT_KEY);
        }
    }
}
