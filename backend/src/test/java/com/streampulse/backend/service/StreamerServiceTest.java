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

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class  StreamerServiceTest {

    @Autowired
    private StreamerService streamerService;

    @Autowired
    private StreamerRepository streamerRepository;

    @Test
    void registerStreamer() {
        // given
        StreamerRequestDTO streamerRequestDTO = new StreamerRequestDTO("방송자 채널 1", "방송자 닉네임 1");

        // when
        StreamerResponseDTO streamerResponseDTO = streamerService.registerStreamer(streamerRequestDTO);

        // then
        Streamer streamer = streamerRepository.findById(streamerResponseDTO.getId()).orElseThrow();
        assertThat(streamer.getChannelId()).isEqualTo(streamerResponseDTO.getChannelId());
        assertThat(streamer.getNickname()).isEqualTo(streamerResponseDTO.getNickname());
    }

}