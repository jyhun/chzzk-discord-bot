package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamSessionRequestDTO;
import com.streampulse.backend.dto.StreamSessionResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamSessionService {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamerRepository streamerRepository;

    public StreamSessionResponseDTO startSession(StreamSessionRequestDTO streamSessionRequestDTO) {
        Streamer streamer = streamerRepository.findByChannelId(streamSessionRequestDTO.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("해당 채널 ID의 방송자를 찾을 수 없습니다."));

        StreamSession streamSession = StreamSession.builder()
                .streamer(streamer)
                .title(streamSessionRequestDTO.getTitle())
                .startedAt(LocalDateTime.now())
                .build();

        StreamSession savedStreamSession = streamSessionRepository.save(streamSession);

        return StreamSessionResponseDTO.builder()
                .id(savedStreamSession.getId())
                .channelId(streamer.getChannelId())
                .title(savedStreamSession.getTitle())
                .startedAt(savedStreamSession.getStartedAt())
                .build();
    }

}
