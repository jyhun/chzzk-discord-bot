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
    private static final String LIVE_STATIC_PREFIX = "LIVE_STATIC:"; // 방송 상태 저장 (no TTL)
    private static final String LIVE_PREFIX = "LIVE:";               // START 중복 방지용 TTL 키

    public String getSnapshot(String channelId) {
        return redisTemplate.opsForValue().get(SNAPSHOT_PREFIX + channelId);
    }

    public void saveSnapshot(String channelId, String value) {
        redisTemplate.opsForValue().set(SNAPSHOT_PREFIX + channelId, value);
    }

    public void deleteSnapshot(String channelId) {
        redisTemplate.delete(SNAPSHOT_PREFIX + channelId);
    }

    public void saveLiveKey(String channelId) {
        redisTemplate.opsForValue().set(LIVE_PREFIX + channelId, "1", Duration.ofMinutes(5));
    }

    public void updateLiveTtl(String channelId) {
        redisTemplate.expire(LIVE_PREFIX + channelId, Duration.ofMinutes(5));
    }

    public void deleteLiveKey(String channelId) {
        redisTemplate.delete(LIVE_PREFIX + channelId);
    }

    public Boolean hasLiveKey(String channelId) {
        return redisTemplate.hasKey(LIVE_PREFIX + channelId);
    }

    public void setStaticKey(String channelId) {
        redisTemplate.opsForValue().set(LIVE_STATIC_PREFIX + channelId, "1");
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

    // 앱 재시작 또는 전체 초기화 시 staticKeys 일괄 삭제
    public void clearAllStaticKeys() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(LIVE_STATIC_PREFIX + "*")
                    .count(1000)
                    .build();
            try (var cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    connection.keyCommands().del(cursor.next());
                }
            }
            return null;
        });
    }

    public void clearAllLiveKeys() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(LIVE_PREFIX + "*")
                    .count(1000)
                    .build();
            try (var cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    connection.keyCommands().del(cursor.next());
                }
            }
            return null;
        });
    }
}
