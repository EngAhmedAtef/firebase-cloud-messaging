package com.github.engahmedatef.fcm.unit;

import com.github.engahmedatef.fcm.exception.*;
import com.github.engahmedatef.fcm.internal.service.FcmExceptionFactory;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse;
import com.github.engahmedatef.fcm.internal.transport.FcmSendErrorResponse.FcmErrorCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FcmExceptionFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Stream<Arguments> errorCodeMappings() {
        return Stream.of(
                Arguments.of(FcmErrorCodes.INVALID_ARGUMENT, FcmInvalidRequestException.class),
                Arguments.of(FcmErrorCodes.UNREGISTERED, FcmInvalidTokenException.class),
                Arguments.of(FcmErrorCodes.SENDER_ID_MISMATCH, FcmInvalidTokenException.class),
                Arguments.of(FcmErrorCodes.QUOTA_EXCEEDED, FcmQuotaExceededException.class),
                Arguments.of(FcmErrorCodes.UNAVAILABLE, FcmUnavailableException.class),
                Arguments.of(FcmErrorCodes.INTERNAL, FcmInternalException.class),
                Arguments.of(FcmErrorCodes.THIRD_PARTY_AUTH_ERROR, FcmAuthenticationException.class),
                Arguments.of(FcmErrorCodes.UNSPECIFIED_ERROR, FcmUnknownException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("errorCodeMappings")
    void mapsErrorCodeToCorrectExceptionType(FcmErrorCodes errorCode, Class<? extends FcmException> expectedType) {
        FcmSendErrorResponse response = buildErrorResponse(errorCode.name(), "Error message");
        FcmException exception = FcmExceptionFactory.from(response);
        assertThat(exception).isInstanceOf(expectedType);
        assertThat(exception.getMessage()).isEqualTo("Error message");
    }

    @Test
    void returnsUnknownExceptionWhenCodeIsNull() {
        FcmSendErrorResponse response = buildErrorResponseNoCode("Something went wrong");
        FcmException exception = FcmExceptionFactory.from(response);
        assertThat(exception).isInstanceOf(FcmUnknownException.class);
    }

    @Test
    void returnsUnknownExceptionWhenDetailsIsMissing() {
        FcmSendErrorResponse.FcmSendError error = new FcmSendErrorResponse.FcmSendError();
        error.setCode(400);
        error.setMessage("Bad request");
        error.setDetails(null);
        FcmSendErrorResponse response = new FcmSendErrorResponse(error);
        FcmException exception = FcmExceptionFactory.from(response);
        assertThat(exception).isInstanceOf(FcmUnknownException.class);
    }

    @Test
    void returnsUnknownExceptionForUnrecognizedCode() {
        FcmSendErrorResponse response = buildErrorResponse("NONEXISTENT_CODE", "Unknown error");
        FcmException exception = FcmExceptionFactory.from(response);
        assertThat(exception).isInstanceOf(FcmUnknownException.class);
    }

    private FcmSendErrorResponse buildErrorResponse(String errorCode, String message) {
        ArrayNode details = MAPPER.createArrayNode();
        ObjectNode detail = MAPPER.createObjectNode();
        detail.put("errorCode", errorCode);
        details.add(detail);

        FcmSendErrorResponse.FcmSendError error = new FcmSendErrorResponse.FcmSendError();
        error.setCode(400);
        error.setMessage(message);
        error.setDetails(details);
        return new FcmSendErrorResponse(error);
    }

    private FcmSendErrorResponse buildErrorResponseNoCode(String message) {
        ArrayNode details = MAPPER.createArrayNode();
        ObjectNode detail = MAPPER.createObjectNode();
        detail.put("someOtherField", "value");
        details.add(detail);

        FcmSendErrorResponse.FcmSendError error = new FcmSendErrorResponse.FcmSendError();
        error.setCode(500);
        error.setMessage(message);
        error.setDetails(details);
        return new FcmSendErrorResponse(error);
    }
}
