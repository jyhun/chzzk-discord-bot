package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamerRequestDTO;
import com.streampulse.backend.dto.StreamerResponseDTO;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class  StreamerServiceTest {

    @Autowired
    private StreamerService streamerService;

    @Autowired
    private StreamerRepository streamerRepository;

    @Test
    void registerStreamer_등록_성공() {
        // given
        StreamerRequestDTO streamerRequestDTO = new StreamerRequestDTO("방송자 채널 1", "방송자 닉네임 1");

        // when
        StreamerResponseDTO streamerResponseDTO = streamerService.registerStreamer(streamerRequestDTO);

        // then
        Streamer streamer = streamerRepository.findById(streamerResponseDTO.getId()).orElseThrow();
        assertThat(streamer.getChannelId()).isEqualTo(streamerResponseDTO.getChannelId());
        assertThat(streamer.getNickname()).isEqualTo(streamerResponseDTO.getNickname());
    }

    @Test
    void getStreamerByChannelId_조회_성공() {
        // given
        StreamerRequestDTO streamerRequestDTO = new StreamerRequestDTO("방송자 채널 1", "방송자 닉네임 1");
        StreamerResponseDTO savedDTO = streamerService.registerStreamer(streamerRequestDTO);

        // when
        StreamerResponseDTO foundDTO = streamerService.getStreamerByChannelId("방송자 채널 1");

        // then
        assertThat(foundDTO).isNotNull();
        assertThat(foundDTO.getId()).isEqualTo(savedDTO.getId());
        assertThat(foundDTO.getChannelId()).isEqualTo(savedDTO.getChannelId());
        assertThat(foundDTO.getNickname()).isEqualTo(savedDTO.getNickname());
    }

    @Test
    void getStreamerByChannelId_존재하지않음_예외발생() {
        // when & then
        assertThatThrownBy(() -> streamerService.getStreamerByChannelId("방송자 채널 x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 채널 ID의 방송자를 찾을 수 없습니다.");
    }

}