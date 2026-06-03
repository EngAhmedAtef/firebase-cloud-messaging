package com.github.engahmedatef.fcm.support;

import com.github.engahmedatef.fcm.exception.FcmRetryableException;

public class TestFcmRetryableException extends FcmRetryableException {
    public TestFcmRetryableException(String message) {
        super(message);
    }
}
