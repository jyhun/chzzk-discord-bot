package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@Transactional
@RequiredArgsConstructor
public class StreamerService {

    private final StreamerRepository streamerRepository;

    public Streamer getOrCreateStreamer(LiveResponseDTO dto) {
        return streamerRepository.findByChannelId(dto.getChannelId()).orElseGet(
                () -> streamerRepository.save(
                        Streamer.builder()
                                .channelId(dto.getChannelId())
                                .nickname(dto.getChannelName())
                                .averageViewerCount(dto.getConcurrentUserCount())
                                .live(false)
                                .build()));
    }

    @LogExecution
    public void updateLiveStatus(Streamer streamer, boolean isLive) {
        streamer.updateLive(isLive);
        streamerRepository.save(streamer);
    }

    @Transactional(readOnly = true)
    public Optional<Streamer> findByChannelId(String channelId) {
        return streamerRepository.findByChannelId(channelId);
    }
}
