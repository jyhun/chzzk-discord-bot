package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamSessionService {

    private final StreamSessionRepository streamSessionRepository;

    public StreamSession getActiveSession(Streamer streamer) {
        return streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull(streamer.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("방송중인 방송 세션을 찾을 수 없습니다."));
    }

    public List<StreamSession> findAllByStreamerIn(Collection<Streamer> streamers) {
        return streamSessionRepository.findAllByStreamerIn(streamers);
    }

    public void saveAll(List<StreamSession> sessions) {
        streamSessionRepository.saveAll(sessions);
    }

    public List<StreamSession> findByStreamerId(Long id) {
        return streamSessionRepository.findByStreamerId(id);
    }

    public boolean existsActiveSessionByChannelId(String channelId) {
        return streamSessionRepository.existsByStreamer_ChannelIdAndEndedAtIsNull(channelId);
    }
}
