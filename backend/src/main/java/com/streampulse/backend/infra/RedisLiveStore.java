package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisLiveStore {

    private final StringRedisTemplate redisTemplate;
    private static final String SNAPSHOT_PREFIX = "snapshot:";
    private static final String LIVE_STATIC_PREFIX = "LIVE_STATIC:"; // TTL 없음
    private static final String LIVE_PREFIX = "LIVE:";               // TTL 있음

    public String getSnapshot(Long sessionId) {
        return redisTemplate.opsForValue().get(SNAPSHOT_PREFIX + sessionId);
    }

    public void saveSnapshot(Long sessionId, String value) {
        redisTemplate.opsForValue().set(SNAPSHOT_PREFIX + sessionId, value, Duration.ofHours(6));
    }

    public void updateLiveTtl(String channelId) {
        redisTemplate.expire(LIVE_PREFIX + channelId, Duration.ofMinutes(2));
    }

    public void saveLiveKey(String channelId) {
        redisTemplate.opsForValue().set(LIVE_PREFIX + channelId, "1", Duration.ofMinutes(2));
    }

    public void setStaticKey(String channelId) {
        redisTemplate.opsForValue().set(LIVE_STATIC_PREFIX + channelId, "1");
    }

    /**
     * LIVE:<channelId> 키를 SCAN으로 안전하게 조회합니다.
     */
    public Set<String> getLiveKeys() {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(LIVE_PREFIX + "*")
                    .count(1000)
                    .build();
            try (var cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    String fullKey = new String(cursor.next(), StandardCharsets.UTF_8);
                    result.add(fullKey.substring(LIVE_PREFIX.length()));
                }
            }
            return result;
        });
    }

    public Boolean hasLiveKey(String channelId) {
        return redisTemplate.hasKey(LIVE_PREFIX + channelId);
    }

    public Set<String> getStaticKeys() {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(LIVE_STATIC_PREFIX + "*")
                    .count(1000)
                    .build();
            try (var cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    String fullKey = new String(cursor.next(), StandardCharsets.UTF_8);
                    result.add(fullKey.substring(LIVE_STATIC_PREFIX.length()));
                }
            }
            return result;
        });
    }

    public void deleteStaticKey(String channelId) {
        redisTemplate.delete(LIVE_STATIC_PREFIX + channelId);
    }

    public void clearAllStaticKeys() {
        redisTemplate.delete(LIVE_STATIC_PREFIX + "*");
    }
}
