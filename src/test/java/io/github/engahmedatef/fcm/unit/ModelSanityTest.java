package io.github.engahmedatef.fcm.unit;

import io.github.engahmedatef.fcm.model.FcmDevice;
import io.github.engahmedatef.fcm.model.FcmDeviceType;
import io.github.engahmedatef.fcm.model.FcmSendResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelSanityTest {

    @Test
    void fcmSendResultSuccessFactory() {
        String messageId = "projects/test/messages/123";
        FcmSendResult result = FcmSendResult.success(messageId);

        assertTrue(result.isSuccess());
        assertEquals(messageId, result.getMessageId());
        assertNull(result.getError());
    }

    @Test
    void fcmSendResultFailureFactory() {
        RuntimeException error = new RuntimeException("Token is invalid");
        FcmSendResult result = FcmSendResult.failure(error);

        assertFalse(result.isSuccess());
        assertNull(result.getMessageId());
        assertEquals(error, result.getError());
    }

    @Test
    void fcmDeviceRecordEquality() {
        FcmDeviceType type = FcmDeviceType.ANDROID;
        String token = "token";

        FcmDevice d1 = FcmDevice.builder().type(type).token(token).build();
        FcmDevice d2 = FcmDevice.builder().type(type).token(token).build();

        assertEquals(d1, d2);
    }
}
