package com.github.engahmedatef.fcm.internal.service;

import com.github.engahmedatef.fcm.api.FcmClient;
import com.github.engahmedatef.fcm.api.ReactiveFcmClient;
import com.github.engahmedatef.fcm.model.FcmDevice;
import com.github.engahmedatef.fcm.model.FcmMessage;
import com.github.engahmedatef.fcm.model.FcmSendResult;
import lombok.AllArgsConstructor;

import java.util.Collection;

@AllArgsConstructor
public class DefaultFcmClient implements FcmClient {
    private final ReactiveFcmClient reactiveFcmClient;

    @Override
    public FcmSendResult sendToDevice(FcmDevice device, FcmMessage message) {
        return reactiveFcmClient.sendToDevice(device, message).block();
    }

    @Override
    public void sendToDevices(Collection<FcmDevice> devices, FcmMessage message) {
        reactiveFcmClient.sendToDevices(devices, message).block();
    }

    @Override
    public void sendToUser(Object userId, FcmMessage message) {
        reactiveFcmClient.sendToUser(userId, message).block();
    }
}
