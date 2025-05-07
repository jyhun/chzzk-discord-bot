package com.streampulse.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.dto.StreamerCacheDTO;
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
    private final ObjectMapper objectMapper;

    private static final String STREAMER_KEY = "streamer:";
    private static final Duration STREAMER_TTL = Duration.ofHours(1);

    @Transactional(readOnly = true)
    public Streamer findByChannelId(String channelId) {
        String cacheKey = STREAMER_KEY + channelId;
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if(cachedObj != null) {
            StreamerCacheDTO dto = objectMapper.convertValue(cachedObj, StreamerCacheDTO.class);
            return dto.toEntity();
        }

        Streamer streamer = streamerRepository.findByChannelId(channelId).orElse(null);
        if(streamer != null) {
            redisTemplate.opsForValue().set(cacheKey, StreamerCacheDTO.fromEntity(streamer), STREAMER_TTL);
        }
        return streamer;
    }

    public void updateLiveStatus(Streamer streamer, boolean isLive) {
        if (streamer.isLive() != isLive) {
            streamer.updateLive(isLive);
            streamerRepository.save(streamer);
            String cacheKey = STREAMER_KEY + streamer.getChannelId();
            redisTemplate.delete(cacheKey);
        }
    }

    @Transactional(readOnly = true)
    public List<Streamer> findAllByChannelIdIn(Collection<String> channelIds) {
        return streamerRepository.findAllByChannelIdInWithFetchJoin(channelIds);
    }

    public void markOffline(Set<String> endIds) {
        if (endIds.isEmpty()) return;
        List<String> idsToUpdate = streamerRepository.findLiveChannelIds(endIds);
        if (!idsToUpdate.isEmpty()) {
            streamerRepository.markOffline(idsToUpdate);
            idsToUpdate.forEach(id -> redisTemplate.delete(STREAMER_KEY + id));
        }
    }

    public void saveStreamersInChunks(List<Streamer> streamers, int chunkSize) {
        for (int i = 0; i < streamers.size(); i += chunkSize) {
            chunkService.saveStreamersChunk(streamers.subList(i, Math.min(i + chunkSize, streamers.size())));
        }
    }

}
