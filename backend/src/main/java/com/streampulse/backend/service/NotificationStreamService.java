package com.streampulse.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationStreamService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${processor.url}")
    private String processorUrl;

    @Value("${consumer.id}")
    private String CONSUMER;

    private static final int BATCH_SIZE = 10;
    private static final String GROUP = "notification-group";

    private static final Map<String, String> streamToEndpoint = Map.of(
            "stream:notification:start", "/api/stream-start",
            "stream:notification:end", "/api/stream-end",
            "stream:notification:topic", "/api/stream-topic",
            "stream:notification:hot", "/api/stream-hot"
    );

    // 재사용 가능한 TypeReference
    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() {
    };

    // 애플리케이션 기동 시 Consumer 그룹 생성 (한 번만 실행)
    @PostConstruct
    public void initConsumerGroup() {
        streamToEndpoint.keySet().forEach(streamKey -> {
            try {
                redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), GROUP);
            } catch (Exception ignored) {
            }
        });
    }

    @Scheduled(fixedDelay = 1000)
    public void processStreams() {
        for (String streamKey : streamToEndpoint.keySet()) {
            try {
                List<MapRecord<String, Object, Object>> records =
                        redisTemplate.opsForStream().read(
                                Consumer.from(GROUP, CONSUMER),
                                StreamReadOptions.empty()
                                        .count(BATCH_SIZE)
                                        .block(Duration.ofSeconds(2)),
                                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                        );

                for (MapRecord<String, Object, Object> record : records) {
                    handleRecord(streamKey, record);
                }
            } catch (Exception ex) {
                log.error("[NotificationStreamService] stream 처리 예외 – {}", streamKey, ex);
            }
        }
    }

    public void handleRecord(String streamKey, MapRecord<String, Object, Object> record) {
        String payloadJson = (String) record.getValue().get("payload");
        if (payloadJson == null) {
            // payload 없으면 ack 후 종료
            redisTemplate.opsForStream().acknowledge(streamKey, GROUP, record.getId());
            log.warn("[NotificationStreamService] payload 없음 – {}, id={}", streamKey, record.getId());
            return;
        }

        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payloadJson, TYPE_REF);
            String endpoint = streamToEndpoint.get(streamKey);
            if (endpoint == null) {
                log.error("[NotificationStreamService] endpoint 매핑 실패 – streamKey={}, id={}", streamKey, record.getId());
                return;
            }

            restTemplate.postForEntity(processorUrl + endpoint, payloadMap, Void.class);

            redisTemplate.opsForStream().acknowledge(streamKey, GROUP, record.getId());

        } catch (Exception e) {
            log.warn("[NotificationStreamService] 처리 실패 – {}, id={}, message={}, type={}",
                    streamKey, record.getId(), e.getMessage(), e.getClass().getSimpleName(), e);
        }
    }
}