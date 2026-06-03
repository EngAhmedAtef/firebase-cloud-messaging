package io.github.engahmedatef.fcm.spi;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * One-time hook for customising the {@link WebClient.Builder} used by the SDK before the
 * WebClient is constructed. Applied once at application startup.
 *
 * <p>Use this SPI to configure timeouts, SSL context, codecs, or base headers that apply to all
 * FCM HTTP calls. For per-request mutation, register an {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}
 * bean annotated with {@link FcmExchangeFilter} instead.
 *
 * <p>Multiple customisers are applied in {@link org.springframework.core.annotation.Order} sequence.
 */
@FunctionalInterface
public interface FcmWebClientBuilderCustomizer {

    /**
     * Applies customisations to the provided builder.
     *
     * @param builder the {@link WebClient.Builder} to customise; never {@code null}
     */
    void customize(WebClient.Builder builder);
}
