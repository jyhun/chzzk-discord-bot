package com.streampulse.backend.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
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

    /**
     * add + remove 를 MULTI/EXEC 으로 원자적 처리
     */
    public void updateLiveSet(Set<String> toAdd, Set<String> toRemove) {
        if (toAdd.isEmpty() && toRemove.isEmpty()) return;

        // 1) 트랜잭션 시작
        redisTemplate.multi();
        try {
            // 2) add 처리
            for (String id : toAdd) {
                redisTemplate.opsForSet().add(LIVE_SET_KEY, id);
            }
            // 3) remove 처리
            for (String id : toRemove) {
                redisTemplate.opsForSet().remove(LIVE_SET_KEY, id);
            }
        } finally {
            // 4) 트랜잭션 커밋 (모든 명령 한 번에 적용)
            redisTemplate.exec();
        }
    }

}
