package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamerRequestDTO;
import com.streampulse.backend.dto.StreamerResponseDTO;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
@RequiredArgsConstructor
public class StreamerService {

    private final StreamerRepository streamerRepository;

    public StreamerResponseDTO registerStreamer(StreamerRequestDTO streamerRequestDTO) {
        Streamer streamer = Streamer.builder()
                .channelId(streamerRequestDTO.getChannelId())
                .nickname(streamerRequestDTO.getNickname())
                .build();

        Streamer savedStreamer = streamerRepository.save(streamer);

        return StreamerResponseDTO.builder()
                .id(savedStreamer.getId())
                .channelId(savedStreamer.getChannelId())
                .nickname(savedStreamer.getNickname())
                .build();
    }

}
