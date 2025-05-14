package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisLiveStore {

    private final StringRedisTemplate redisTemplate;
    private static final String SNAPSHOT_PREFIX = "snapshot:";
    private static final String STATIC_PREFIX = "LIVE_STATIC:";
    private static final String LAST_SEEN_PREFIX = "lastSeen:";
    private static final Duration TTL_1_HOUR = Duration.ofHours(1);

    public String getSnapshot(String channelId) {
        return redisTemplate.opsForValue().get(SNAPSHOT_PREFIX + channelId);
    }

    public void saveSnapshot(String channelId, String value) {
        redisTemplate.opsForValue().set(SNAPSHOT_PREFIX + channelId, value, TTL_1_HOUR);
    }

    public void deleteSnapshot(String channelId) {
        redisTemplate.delete(SNAPSHOT_PREFIX + channelId);
    }

    public void setStaticKey(String channelId) {
        redisTemplate.opsForValue().set(STATIC_PREFIX + channelId, "1", TTL_1_HOUR);
    }

    public Set<String> getStaticKeys() {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(STATIC_PREFIX + "*")
                    .count(1000)
                    .build();
            try (var cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    String fullKey = new String(cursor.next(), StandardCharsets.UTF_8);
                    result.add(fullKey.substring(STATIC_PREFIX.length()));
                }
            }
            return result;
        });
    }

    public void deleteStaticKey(String channelId) {
        redisTemplate.delete(STATIC_PREFIX + channelId);
    }

    /**
     * 업데이트된 마지막 수집 시각을 Unix epoch seconds 형태로 저장합니다.
     */
    public void updateLastSeen(String channelId) {
        String key = LAST_SEEN_PREFIX + channelId;
        String now = String.valueOf(Instant.now().getEpochSecond());
        redisTemplate.opsForValue().set(key, now, TTL_1_HOUR);
    }

    /**
     * 마지막 수집 시각을 반환합니다 (seconds).
     */
    public Long getLastSeen(String channelId) {
        String val = redisTemplate.opsForValue().get(LAST_SEEN_PREFIX + channelId);
        if (val == null) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * lastSeen 키를 삭제합니다.
     */
    public void deleteLastSeen(String channelId) {
        redisTemplate.delete(LAST_SEEN_PREFIX + channelId);
    }

}
