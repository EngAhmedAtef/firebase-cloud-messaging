package com.github.engahmedatef.fcm.internal.service;

import com.github.engahmedatef.fcm.api.FcmTokenService;
import com.github.engahmedatef.fcm.exception.FcmException;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import com.github.engahmedatef.fcm.internal.transport.target.ConditionTarget;
import com.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import com.github.engahmedatef.fcm.internal.transport.target.FcmTarget;
import com.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmSendResult;
import com.github.engahmedatef.fcm.spi.FcmLogEvent;
import com.github.engahmedatef.fcm.spi.FcmLogger;
import com.github.engahmedatef.fcm.spi.FcmResponseErrorHandler;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse.FcmErrorCodes.SENDER_ID_MISMATCH;
import static com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse.FcmErrorCodes.UNREGISTERED;

@AllArgsConstructor
public class DefaultFcmResponseErrorHandler implements FcmResponseErrorHandler {
    private final FcmTokenService tokenService;
    private final FcmLogger fcmLogger;

    @Override
    public Mono<FcmSendResult> handle(FcmTarget target, FcmSendErrorResponse err) {
        String errorMessage = err.getError().getMessage();
        Optional<FcmSendErrorResponse.FcmErrorCodes> errorCode = err.getError().getFcmErrorCode();

        Map<String, Object> metadata = new HashMap<>();
        if (target instanceof DeviceTarget deviceTarget) {
            FcmDevice device = deviceTarget.device();
            metadata.put("targetType", "device");
            metadata.put("deviceTokenPrefix", device.token().substring(0, 6));
            metadata.put("deviceType", device.type());
        } else if (target instanceof TopicTarget topicTarget) {
            metadata.put("targetType", "topic");
            metadata.put("topicName", topicTarget.topic());
        } else if (target instanceof ConditionTarget conditionTarget) {
            metadata.put("targetType", "condition");
            metadata.put("condition", conditionTarget.condition());
        }
        metadata.put("reason", errorMessage);

        if (errorCode.isPresent()) {
            metadata.put("code", errorCode.get());
            if (errorCode.get() == UNREGISTERED || errorCode.get() == SENDER_ID_MISMATCH) {
                fcmLogger.error(FcmLogEvent.SEND_FAILED, metadata);
                FcmException fcmException = FcmExceptionFactory.from(err);
                return (target instanceof DeviceTarget deviceTarget) ?
                        cleanupToken(deviceTarget.device().token())
                                .then(Mono.error(fcmException)) :
                        Mono.error(fcmException);
            }
        }

        metadata.put("code", "UNKNOWN");
        fcmLogger.error(FcmLogEvent.SEND_FAILED, metadata);
        return Mono.error(FcmExceptionFactory.from(err));
    }

    private Mono<Void> cleanupToken(String token) {
        if (tokenService == null)
            return Mono.empty();

        return tokenService.deleteToken(token)
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
