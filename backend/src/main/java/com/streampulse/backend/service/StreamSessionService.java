package com.streampulse.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.dto.StreamSessionCacheDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamSessionService {

    private final StreamSessionRepository streamSessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChunkService chunkService;
    private final ObjectMapper objectMapper;

    private static final Duration SESSION_TTL = Duration.ofMinutes(10);

    @Transactional(readOnly = true)
    public StreamSession getActiveSession(Streamer streamer) {
        String sessionKey = "active_session:" + streamer.getChannelId();

        Object cachedObj = redisTemplate.opsForValue().get(sessionKey);
        if (cachedObj != null) {
            StreamSessionCacheDTO dto = objectMapper.convertValue(cachedObj, StreamSessionCacheDTO.class);
            return dto.toEntity(streamer);
        }
        StreamSession session = streamSessionRepository.findFirstByStreamer_ChannelIdAndEndedAtIsNullOrderByStartedAtDesc(streamer.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("방송중인 방송 세션을 찾을 수 없습니다."));
        Hibernate.initialize(session.getTags());

        redisTemplate.opsForValue().set(sessionKey, StreamSessionCacheDTO.fromEntity(session), SESSION_TTL);
        return session;
    }

    @Transactional(readOnly = true)
    public boolean existsActiveSessionByChannelId(String channelId) {
        String existsKey = "active_session_exists:" + channelId;

        Object cachedObj = redisTemplate.opsForValue().get(existsKey);
        if (cachedObj != null) {
            return objectMapper.convertValue(cachedObj, Boolean.class);
        }
        boolean exists = streamSessionRepository.existsByStreamer_ChannelIdAndEndedAtIsNull(channelId);
        redisTemplate.opsForValue().set(existsKey, exists, SESSION_TTL);
        return exists;
    }

    @Transactional(readOnly = true)
    public List<StreamSession> findAllByStreamerIn(Collection<Streamer> streamers) {
        List<StreamSession> sessions = streamSessionRepository.findAllByStreamerIn(streamers);
        sessions.forEach(s -> Hibernate.initialize(s.getTags()));
        return sessions;
    }

    public void saveSessionsInChunks(List<StreamSession> sessions, int chunkSize) {
        for (int i = 0; i < sessions.size(); i += chunkSize) {
            chunkService.saveSessionsChunk(sessions.subList(i, Math.min(i + chunkSize, sessions.size())));
        }
    }

}
