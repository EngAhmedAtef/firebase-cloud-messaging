package io.github.engahmedatef.fcm.support;

import io.github.engahmedatef.fcm.api.FcmTokenService;
import io.github.engahmedatef.fcm.model.FcmDevice;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FakeFcmTokenService implements FcmTokenService {

    private final Map<Object, List<FcmDevice>> userDevices = new ConcurrentHashMap<>();
    private final Set<String> deletedTokens = ConcurrentHashMap.newKeySet();

    public void registerDevices(Object userId, List<FcmDevice> devices) {
        userDevices.put(userId, devices);
    }

    public Set<String> getDeletedTokens() {
        return deletedTokens;
    }

    public boolean wasTokenDeleted(String token) {
        return deletedTokens.contains(token);
    }

    @Override
    public Mono<Collection<FcmDevice>> getUserDevices(Object userId) {
        Collection<FcmDevice> devices = userDevices.getOrDefault(userId, List.of());
        return Mono.just(devices);
    }

    @Override
    public Mono<Void> deleteToken(String token) {
        deletedTokens.add(token);
        return Mono.empty();
    }
}
