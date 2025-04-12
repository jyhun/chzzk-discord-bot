package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamMetricsRequestDTO;
import com.streampulse.backend.dto.StreamMetricsResponseDTO;
import com.streampulse.backend.dto.StreamSessionRequestDTO;
import com.streampulse.backend.dto.StreamSessionResponseDTO;
import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.infra.DiscordNotifier;
import com.streampulse.backend.repository.StreamEventRepository;
import com.streampulse.backend.repository.StreamerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StreamMetricsServiceTest {

    @Autowired
    private StreamSessionService streamSessionService;

    @Autowired
    private StreamerRepository streamerRepository;

    @Autowired
    private StreamMetricsService streamMetricsService;

    @Autowired
    private StreamEventRepository streamEventRepository;

    @MockBean
    private DiscordNotifier discordNotifier;

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

    @Test
    void getMetrics_성공() {
        // given
        Streamer streamer = Streamer.builder()
                .channelId("방송자 채널 1")
                .nickname("방송자 닉네임 1")
                .build();

        streamerRepository.save(streamer);

        StreamSessionRequestDTO streamSessionRequestDTO = new StreamSessionRequestDTO("방송자 채널 1", "방송자 제목 1");
        StreamSessionResponseDTO streamSessionResponseDTO = streamSessionService.startSession(streamSessionRequestDTO);

        StreamMetricsRequestDTO streamMetricsRequestDTO1 = new StreamMetricsRequestDTO(streamSessionResponseDTO.getId(), 10, 100);
        StreamMetricsRequestDTO streamMetricsRequestDTO2 = new StreamMetricsRequestDTO(streamSessionResponseDTO.getId(), 20, 200);

        StreamMetricsResponseDTO streamMetricsResponseDTO1 = streamMetricsService.saveMetrics(streamMetricsRequestDTO1);
        StreamMetricsResponseDTO streamMetricsResponseDTO2 = streamMetricsService.saveMetrics(streamMetricsRequestDTO2);
        // when
        List<StreamMetricsResponseDTO> streamMetricsResponseDTOList = streamMetricsService.getStreamMetrics(streamSessionResponseDTO.getId());
        // then
        assertThat(streamMetricsResponseDTOList.size()).isEqualTo(2);
    }

    @Test
    void saveMetrics_하이라이트_감지_성공() {
        // given
        Streamer streamer = Streamer.builder()
                .channelId("방송자 채널 1")
                .nickname("방송자 닉네임 1")
                .build();

        streamerRepository.save(streamer);

        StreamSessionRequestDTO streamSessionRequestDTO = new StreamSessionRequestDTO("방송자 채널 1", "방송자 제목 1");
        StreamSessionResponseDTO streamSessionResponseDTO = streamSessionService.startSession(streamSessionRequestDTO);

        StreamMetricsRequestDTO streamMetricsRequestDTO1 = new StreamMetricsRequestDTO(streamSessionResponseDTO.getId(), 10, 100);
        StreamMetricsRequestDTO streamMetricsRequestDTO2 = new StreamMetricsRequestDTO(streamSessionResponseDTO.getId(), 20, 200);

        streamMetricsService.saveMetrics(streamMetricsRequestDTO1);
        streamMetricsService.saveMetrics(streamMetricsRequestDTO2);

        // when
        List<StreamEvent> streamEventList = streamEventRepository.findAll();

        // then
        assertThat(streamEventList.size()).isEqualTo(1);

        Mockito.verify(discordNotifier, Mockito.atLeastOnce()).sendMessage(Mockito.anyString());

    }

    @Test
    void saveMetrics_하이라이트_감지_실패() {
        // given
        Streamer streamer = Streamer.builder()
                .channelId("방송자 채널 1")
                .nickname("방송자 닉네임 1")
                .build();

        streamerRepository.save(streamer);

        StreamSessionRequestDTO streamSessionRequestDTO = new StreamSessionRequestDTO("방송자 채널 1", "방송자 제목 1");
        StreamSessionResponseDTO streamSessionResponseDTO = streamSessionService.startSession(streamSessionRequestDTO);

        StreamMetricsRequestDTO streamMetricsRequestDTO1 = new StreamMetricsRequestDTO(streamSessionResponseDTO.getId(), 10, 100);
        StreamMetricsRequestDTO streamMetricsRequestDTO2 = new StreamMetricsRequestDTO(streamSessionResponseDTO.getId(), 15, 150);

        streamMetricsService.saveMetrics(streamMetricsRequestDTO1);
        streamMetricsService.saveMetrics(streamMetricsRequestDTO2);

        // when
        List<StreamEvent> streamEventList = streamEventRepository.findAll();

        // then
        assertThat(streamEventList).isEmpty();

    }

}
