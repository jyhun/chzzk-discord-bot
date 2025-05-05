package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@Service
@Transactional
@RequiredArgsConstructor
public class StreamerService {

    private final StreamerRepository streamerRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChunkService chunkService;
    private static final Duration STREAMER_TTL = Duration.ofHours(1);

    @Transactional(readOnly = true)
    public Streamer findByChannelId(String channelId) {
        String cacheKey = "streamer:" + channelId;
        Streamer cached = (Streamer) redisTemplate.opsForValue().get(cacheKey);
        if(cached != null) {
            return cached;
        }
        Streamer streamer = streamerRepository.findByChannelId(channelId).orElse(null);
        if(streamer != null) {
            redisTemplate.opsForValue().set(cacheKey, streamer, STREAMER_TTL);
        }
        return streamer;
    }

    @LogExecution
    public void updateLiveStatus(Streamer streamer, boolean isLive) {
        if (streamer.isLive() != isLive) {
            streamer.updateLive(isLive);
            streamerRepository.save(streamer);
            String cacheKey = "streamer:" + streamer.getChannelId();
            redisTemplate.delete(cacheKey);
        }
    }

    @Transactional(readOnly = true)
    public List<Streamer> findAllByChannelIdIn(Collection<String> channelIds) {
        return streamerRepository.findAllByChannelIdIn(channelIds);
    }

    public void markOffline(Set<String> endIds) {
        streamerRepository.markOffline(endIds);
        endIds.forEach(id -> redisTemplate.delete("streamer:" + id));
    }

    public void saveStreamersInChunks(List<Streamer> streamers, int chunkSize) {
        for (int i = 0; i < streamers.size(); i += chunkSize) {
            chunkService.saveStreamersChunk(streamers.subList(i, Math.min(i + chunkSize, streamers.size())));
        }
    }

}
