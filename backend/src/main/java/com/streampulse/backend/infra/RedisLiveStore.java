package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisLiveStore {

    private final RedisTemplate<String, String> redisTemplate;
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

}
