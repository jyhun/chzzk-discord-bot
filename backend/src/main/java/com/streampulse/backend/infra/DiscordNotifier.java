package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordNotifier {

    @Value("${discord.webhook-url}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    public void sendMessage(String message) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("content", message), httpHeaders
        );
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("디스코드 알림 전송 완료. 응답 상태: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("디스코드 알림 전송 실패", e);
        }
    }

}
