package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamSessionRequestDTO;
import com.streampulse.backend.dto.StreamSessionResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StreamSessionServiceTest {

    @Autowired
    private StreamSessionService streamSessionService;

    @Autowired
    private StreamSessionRepository streamSessionRepository;

    @Autowired
    private StreamerRepository streamerRepository;

    @Test
    void startSession_성공() {
        // given
        Streamer streamer = Streamer.builder()
                .channelId("방송자 채널 1")
                .nickname("방송자 닉네임 1")
                .build();

        streamerRepository.save(streamer);

        StreamSessionRequestDTO streamSessionRequestDTO = new StreamSessionRequestDTO("방송자 채널 1", "방송자 제목 1");

        // when
        StreamSessionResponseDTO streamSessionResponseDTO = streamSessionService.startSession(streamSessionRequestDTO);

        // then
        assertThat(streamSessionResponseDTO).isNotNull();
        assertThat(streamSessionResponseDTO.getId()).isEqualTo(1);
        assertThat(streamSessionResponseDTO.getChannelId()).isEqualTo("방송자 채널 1");
        assertThat(streamSessionResponseDTO.getTitle()).isEqualTo("방송자 제목 1");

        Optional<StreamSession> streamSession = streamSessionRepository.findById(streamSessionResponseDTO.getId());
        assertThat(streamSession).isPresent();
        assertThat(streamer.getId()).isEqualTo(streamSession.get().getStreamer().getId());

    }

}