package com.invest.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(InvestProperties.class)
public class AppConfig {

    @Bean
    @Qualifier("yahooRestClient")
    RestClient yahooRestClient(InvestProperties investProperties) {
        var yf = investProperties.getYahooFinance();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1, yf.getConnectTimeoutMs()));
        factory.setReadTimeout(Math.max(1, yf.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(factory).build();
    }
}
