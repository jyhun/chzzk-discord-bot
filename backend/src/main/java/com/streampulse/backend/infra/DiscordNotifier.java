package com.streampulse.backend.infra;

import com.streampulse.backend.aop.LogExecution;
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

    @LogExecution
    public boolean sendMessage(String message) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("content", message), httpHeaders
            );

            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}

