package io.github.engahmedatef.fcm.support;

import io.github.engahmedatef.fcm.exception.FcmRetryableException;

public class TestFcmRetryableException extends FcmRetryableException {
    public TestFcmRetryableException(String message) {
        super(message);
    }
}
