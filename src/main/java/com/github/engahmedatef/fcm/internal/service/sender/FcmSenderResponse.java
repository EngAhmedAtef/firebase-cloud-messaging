package com.github.engahmedatef.fcm.internal.service.sender;

import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import com.github.engahmedatef.fcm.internal.transport.FcmSendSuccessResponse;

public record FcmSenderResponse(boolean success, FcmSendSuccessResponse successResponse, FcmSendErrorResponse errorResponse) {

    public static FcmSenderResponse success(FcmSendSuccessResponse successResponse) {
        return new FcmSenderResponse(true, successResponse, null);
    }

    public static FcmSenderResponse error(FcmSendErrorResponse errorResponse) {
        return new FcmSenderResponse(false, null, errorResponse);
    }
}
