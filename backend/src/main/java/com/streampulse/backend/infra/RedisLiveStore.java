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
    private static final String LIVE_PREFIX = "LIVE:";
    private static final String STATIC_PREFIX = "LIVE_STATIC:";

    public String getSnapshot(String channelId) {
        return redisTemplate.opsForValue().get(SNAPSHOT_PREFIX + channelId);
    }

    public void saveSnapshot(String channelId, String value) {
        redisTemplate.opsForValue().set(SNAPSHOT_PREFIX + channelId, value);
    }

    public void deleteSnapshot(String channelId) {
        redisTemplate.delete(SNAPSHOT_PREFIX + channelId);
    }

    public void setLiveKey(String channelId) {
        redisTemplate.opsForValue().set(LIVE_PREFIX + channelId, "1", Duration.ofMinutes(3));
    }

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

    public void setStaticKey(String channelId) {
        redisTemplate.opsForValue().set(STATIC_PREFIX + channelId, "1");
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

}
