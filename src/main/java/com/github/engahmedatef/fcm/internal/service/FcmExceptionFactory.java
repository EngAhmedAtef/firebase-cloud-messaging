package com.github.engahmedatef.fcm.internal.service;

import com.github.engahmedatef.fcm.exception.*;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;

public final class FcmExceptionFactory {

    private FcmExceptionFactory() {}

    public static FcmException from(FcmSendErrorResponse errorResponse) {
        FcmSendErrorResponse.FcmSendError error = errorResponse.getError();
        FcmSendErrorResponse.FcmErrorCodes code = error.getFcmErrorCode().orElse(null);
        String message = error.getMessage();

        if (code == null)
            return new FcmUnknownException(message);

        return switch (code) {
            case INVALID_ARGUMENT -> new FcmInvalidRequestException(message);
            case UNREGISTERED, SENDER_ID_MISMATCH -> new FcmInvalidTokenException(message);
            case QUOTA_EXCEEDED -> new FcmQuotaExceededException(message);
            case UNAVAILABLE -> new FcmUnavailableException(message);
            case INTERNAL -> new FcmInternalException(message);
            case THIRD_PARTY_AUTH_ERROR -> new FcmAuthenticationException(message);
            case UNSPECIFIED_ERROR -> new FcmUnknownException(message);
        };
    }
}
