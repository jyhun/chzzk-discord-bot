package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamSessionService {

    private final StreamSessionRepository streamSessionRepository;
    private final ChunkService chunkService;

    @Transactional(readOnly = true)
    public StreamSession getActiveSession(Streamer streamer) {
        StreamSession session = streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull(streamer.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("방송중인 방송 세션을 찾을 수 없습니다."));
        Hibernate.initialize(session.getTags());
        return session;
    }

    @Transactional(readOnly = true)
    public List<StreamSession> findAllByStreamerIn(Collection<Streamer> streamers) {
        List<StreamSession> sessions = streamSessionRepository.findAllByStreamerIn(streamers);
        sessions.forEach(s -> Hibernate.initialize(s.getTags()));
        return sessions;
    }

    @Transactional(readOnly = true)
    public boolean existsActiveSessionByChannelId(String channelId) {
        return streamSessionRepository.existsByStreamer_ChannelIdAndEndedAtIsNull(channelId);
    }

    public void saveSessionsInChunks(List<StreamSession> sessions, int chunkSize) {
        for (int i = 0; i < sessions.size(); i += chunkSize) {
            chunkService.saveSessionsChunk(sessions.subList(i, Math.min(i + chunkSize, sessions.size())));
        }
    }

}
