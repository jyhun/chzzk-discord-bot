package com.streampulse.backend.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 1) 커넥션 풀 매니저 설정
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(20);

        // 2) RequestConfig로 타임아웃 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))   // 연결 시도 최대 3초
                .setResponseTimeout(Timeout.ofSeconds(5))  // 서버 응답 바디 최대 5초
                .build();

        // 3) HttpClient 생성 (커넥션 풀 + 타임아웃 반영)
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 4) HttpComponentsClientHttpRequestFactory에 HttpClient 주입
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // 5) RestTemplateBuilder에 팩토리 적용
        return builder
                .requestFactory(() -> factory)
                // (추가로 builder.setConnectTimeout / setReadTimeout 사용 가능)
                .build();
    }
}
