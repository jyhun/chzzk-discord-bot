package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisCursorStore {

    private final StringRedisTemplate redisTemplate;

    public void saveZSet(String key, Map<Integer, String> indexToCursor) {
        redisTemplate.delete(key);
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        for (Map.Entry<Integer, String> entry : indexToCursor.entrySet()) {
            zSetOps.add(key, entry.getValue(), entry.getKey());
        }
    }

    public Map<Integer, String> loadZSet(String key) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
        if (tuples == null) return Collections.emptyMap();

        Map<Integer, String> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String cursor = tuple.getValue();
            Double score = tuple.getScore();

            if (cursor == null || score == null) continue;

            result.put(score.intValue(), cursor);
        }
        return result;
    }

    public void rename(String fromKey, String toKey) {
        redisTemplate.rename(fromKey, toKey);
    }

}
