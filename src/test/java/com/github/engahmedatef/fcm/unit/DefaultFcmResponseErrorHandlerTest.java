package com.github.engahmedatef.fcm.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.engahmedatef.fcm.exception.FcmInvalidTokenException;
import com.github.engahmedatef.fcm.exception.FcmInvalidRequestException;
import com.github.engahmedatef.fcm.internal.service.DefaultFcmResponseErrorHandler;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse.FcmErrorCodes;
import com.github.engahmedatef.fcm.internal.transport.target.DeviceTarget;
import com.github.engahmedatef.fcm.internal.transport.target.TopicTarget;
import com.github.engahmedatef.fcm.spi.FcmLogger;
import com.github.engahmedatef.fcm.support.FakeFcmTokenService;
import com.github.engahmedatef.fcm.support.FcmTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefaultFcmResponseErrorHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private FcmLogger fcmLogger;

    private FakeFcmTokenService tokenService;
    private DefaultFcmResponseErrorHandler handler;
    private DefaultFcmResponseErrorHandler handlerWithoutTokenService;

    @BeforeEach
    void setUp() {
        tokenService = new FakeFcmTokenService();
        handler = new DefaultFcmResponseErrorHandler(tokenService, fcmLogger);
        handlerWithoutTokenService = new DefaultFcmResponseErrorHandler(null, fcmLogger);
        lenient().doNothing().when(fcmLogger).error(any(), any());
        lenient().doNothing().when(fcmLogger).warn(any(), any());
    }

    @Test
    void unregisteredWithDeviceTargetDeletesTokenAndErrors() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        FcmSendErrorResponse errorResponse = buildErrorResponse(FcmErrorCodes.UNREGISTERED, "Token unregistered");

        StepVerifier.create(handler.handle(target, errorResponse))
                .expectError(FcmInvalidTokenException.class)
                .verify();

        assertThat(tokenService.wasTokenDeleted(FcmTestFixtures.DEVICE_TOKEN)).isTrue();
    }

    @Test
    void senderIdMismatchWithDeviceTargetDeletesTokenAndErrors() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        FcmSendErrorResponse errorResponse = buildErrorResponse(FcmErrorCodes.SENDER_ID_MISMATCH, "Sender mismatch");

        StepVerifier.create(handler.handle(target, errorResponse))
                .expectError(FcmInvalidTokenException.class)
                .verify();

        assertThat(tokenService.wasTokenDeleted(FcmTestFixtures.DEVICE_TOKEN)).isTrue();
    }

    @Test
    void unregisteredWithTopicTargetDoesNotCleanUpToken() {
        TopicTarget target = FcmTestFixtures.aTopicTarget();
        FcmSendErrorResponse errorResponse = buildErrorResponse(FcmErrorCodes.UNREGISTERED, "Unregistered");

        StepVerifier.create(handler.handle(target, errorResponse))
                .expectError(FcmInvalidTokenException.class)
                .verify();

        assertThat(tokenService.getDeletedTokens()).isEmpty();
    }

    @Test
    void invalidArgumentDoesNotCleanUpAndPropagatesError() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        FcmSendErrorResponse errorResponse = buildErrorResponse(FcmErrorCodes.INVALID_ARGUMENT, "Bad request");

        StepVerifier.create(handler.handle(target, errorResponse))
                .expectError(FcmInvalidRequestException.class)
                .verify();

        assertThat(tokenService.getDeletedTokens()).isEmpty();
    }

    @Test
    void nullTokenServiceWithUnregisteredDoesNotThrowNpe() {
        DeviceTarget target = FcmTestFixtures.aDeviceTarget();
        FcmSendErrorResponse errorResponse = buildErrorResponse(FcmErrorCodes.UNREGISTERED, "Unregistered");

        StepVerifier.create(handlerWithoutTokenService.handle(target, errorResponse))
                .expectError(FcmInvalidTokenException.class)
                .verify();
    }

    private FcmSendErrorResponse buildErrorResponse(FcmErrorCodes code, String message) {
        ArrayNode details = MAPPER.createArrayNode();
        ObjectNode detail = MAPPER.createObjectNode();
        detail.put("errorCode", code.name());
        details.add(detail);

        FcmSendErrorResponse.FcmSendError error = new FcmSendErrorResponse.FcmSendError();
        error.setCode(400);
        error.setMessage(message);
        error.setDetails(details);
        return new FcmSendErrorResponse(error);
    }
}
