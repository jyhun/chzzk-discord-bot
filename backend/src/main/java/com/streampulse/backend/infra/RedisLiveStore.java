package com.streampulse.backend.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.entity.StreamMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RedisLiveStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String LIVE_SET_KEY = "live:set";
    private static final String SNAPSHOT_PREFIX = "snapshot:";

    public Set<String> getLiveStreamerIds() {
        return Optional.ofNullable(redisTemplate.opsForSet().members(LIVE_SET_KEY)).orElse(Collections.emptySet());
    }

    public String getSnapshot(Long sessionId) {
        return redisTemplate.opsForValue().get(SNAPSHOT_PREFIX + sessionId);
    }

    public void saveSnapshot(Long sessionId, String value) {
        redisTemplate.opsForValue().set(SNAPSHOT_PREFIX + sessionId, value, Duration.ofHours(6));
    }

    public void updateLiveSet(Set<String> add, Set<String> remove) {
        if (add.isEmpty() && remove.isEmpty()) return;
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public <K, V> List<Object> execute(RedisOperations<K, V> ops) {
                ops.multi();
                @SuppressWarnings("unchecked")
                RedisOperations<String, String> o = (RedisOperations<String, String>) ops;
                add.forEach(id -> o.opsForSet().add(LIVE_SET_KEY, id));
                remove.forEach(id -> o.opsForSet().remove(LIVE_SET_KEY, id));
                return ops.exec();
            }
        });
    }

    public void clearLiveSet() {
        redisTemplate.delete(LIVE_SET_KEY);
    }

    // RedisLiveStore.java

    public List<StreamMetrics> getMetrics(Long sessionId) {
        String key = "metrics:session:" + sessionId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            // StreamMetricsDTO로 변환하는 게 안전 (엔티티 직접 캐싱 X)
            return Arrays.asList(objectMapper.readValue(json, StreamMetrics[].class));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void saveMetrics(Long sessionId, List<StreamMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) return;
        String key = "metrics:session:" + sessionId;
        try {
            String json = objectMapper.writeValueAsString(metrics);
            redisTemplate.opsForValue().set(key, json);
            // TTL은 예: 하루. (불변 데이터라 무제한도 가능)
            redisTemplate.expire(key, Duration.ofDays(1));
        } catch (JsonProcessingException e) {
            // 로깅만 하고 무시
        }
    }


}
