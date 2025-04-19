package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisCursorStore {

    private static final String REDIS_KEY = "stream:valid-cursors";
    private final RedisTemplate<String, String> redisTemplate;

    public void save(List<String> cursors) {
        redisTemplate.delete(REDIS_KEY);
        if (cursors != null && !cursors.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY, cursors);
        }
    }

    public List<String> load() {
        return redisTemplate.opsForList().range(REDIS_KEY, 0, -1);
    }

}
