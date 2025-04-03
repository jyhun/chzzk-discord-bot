package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
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
import java.time.format.DateTimeFormatter;

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

    public StreamSessionResponseDTO endSession(String channelId) {
        StreamSession streamSession = streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull(channelId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채널 ID의 방송 세션을 찾을 수 없습니다."));

        streamSession.updateEndedAt();
        streamSessionRepository.save(streamSession);

        return StreamSessionResponseDTO.builder()
                .id(streamSession.getId())
                .channelId(channelId)
                .title(streamSession.getTitle())
                .startedAt(streamSession.getStartedAt())
                .endedAt(streamSession.getEndedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public StreamSessionResponseDTO getSessionById(Long id) {
        StreamSession streamSession = streamSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 방송 세션을 찾을 수 없습니다."));

        return StreamSessionResponseDTO.builder()
                .id(streamSession.getId())
                .channelId(streamSession.getStreamer().getChannelId())
                .title(streamSession.getTitle())
                .startedAt(streamSession.getStartedAt())
                .endedAt(streamSession.getEndedAt())
                .build();
    }

    public StreamSession getOrCreateSession(Streamer streamer, LiveResponseDTO dto) {
        if (!streamer.isLive()) {
            StreamSession session = StreamSession.builder()
                    .streamer(streamer)
                    .title(dto.getLiveTitle())
                    .startedAt(LocalDateTime.parse(dto.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
            return streamSessionRepository.save(session);
        }
        return streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull(streamer.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("방송 세션을 찾을 수 없습니다."));
    }
}
