package io.github.engahmedatef.fcm.internal.http;

import io.github.engahmedatef.fcm.autoconfigure.FcmProperties;
import io.github.engahmedatef.fcm.spi.FcmWebClientBuilderCustomizer;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

public class ReactorNettyWebClientFactory implements FcmWebClientFactory {
    private final FcmProperties properties;
    private final ObjectProvider<FcmWebClientBuilderCustomizer> customizers;
    private final ObjectProvider<ExchangeFilterFunction> filterProvider;

    public ReactorNettyWebClientFactory(FcmProperties properties,
                                        ObjectProvider<FcmWebClientBuilderCustomizer> customizers,
                                        ObjectProvider<ExchangeFilterFunction> filterProvider) {
        this.properties = properties;
        this.customizers = customizers;
        this.filterProvider = filterProvider;
    }

    @Override
    public WebClient create() {
        FcmProperties.Http http = properties.getHttp();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, http.getConnectTimeoutMillis())
                .responseTimeout(Duration.ofMillis(http.getResponseTimeoutMillis()));

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(http.getMaxInMemorySizeMb() * 1024 * 1024))
                .build();

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies);

        customizers.orderedStream().forEach(c -> c.customize(builder));
        builder.filters(filters -> filterProvider.orderedStream().forEach(filters::add));

        return builder.build();
    }
}
