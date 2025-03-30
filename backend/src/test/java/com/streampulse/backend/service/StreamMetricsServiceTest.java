package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamMetricsRequestDTO;
import com.streampulse.backend.dto.StreamMetricsResponseDTO;
import com.streampulse.backend.dto.StreamSessionRequestDTO;
import com.streampulse.backend.dto.StreamSessionResponseDTO;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamMetricsRepository;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StreamMetricsServiceTest {

    @Autowired
    private StreamSessionService streamSessionService;

    @Autowired
    private StreamSessionRepository streamSessionRepository;

    @Autowired
    private StreamerRepository streamerRepository;

    @Autowired
    private StreamMetricsService streamMetricsService;

    @Autowired
    private StreamMetricsRepository streamMetricsRepository;

    @Test
    void saveMetrics_성공() {
        // given
        Streamer streamer = Streamer.builder()
                .channelId("방송자 채널 1")
                .nickname("방송자 닉네임 1")
                .build();

        streamerRepository.save(streamer);

        StreamSessionRequestDTO streamSessionRequestDTO = new StreamSessionRequestDTO("방송자 채널 1", "방송자 제목 1");
        StreamSessionResponseDTO streamSessionResponseDTO = streamSessionService.startSession(streamSessionRequestDTO);

        StreamMetricsRequestDTO streamMetricsRequestDTO = new StreamMetricsRequestDTO(streamSessionResponseDTO.getId(), 10, 100);

        // when
        StreamMetricsResponseDTO streamMetricsResponseDTO = streamMetricsService.saveMetrics(streamMetricsRequestDTO);

        // then
        assertThat(streamMetricsResponseDTO.getId()).isNotNull();
        assertThat(streamMetricsResponseDTO.getSessionId()).isEqualTo(streamSessionResponseDTO.getId());
        assertThat(streamMetricsResponseDTO.getChatCount()).isEqualTo(10);
        assertThat(streamMetricsResponseDTO.getViewerCount()).isEqualTo(100);
        assertThat(streamMetricsResponseDTO.getCollectedAt()).isNotNull();

    }

}