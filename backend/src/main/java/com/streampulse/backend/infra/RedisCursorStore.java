package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisCursorStore {

    private final RedisTemplate<String, String> redisTemplate;

    public void save(String key, List<String> cursors) {
        redisTemplate.delete(key);
        if (cursors != null && !cursors.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, cursors);
        }
    }

    public List<String> load(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void rename(String fromKey, String toKey) {
        redisTemplate.rename(fromKey, toKey);
    }

}
