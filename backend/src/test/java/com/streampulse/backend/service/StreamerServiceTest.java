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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class  StreamerServiceTest {

    @Autowired
    private StreamerService streamerService;

    @Autowired
    private StreamerRepository streamerRepository;

    @Test
    void registerStreamer_성공() {
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
    void getStreamerByChannelId_성공() {
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

    @Test
    void getAllStreamers_성공() {
        // given
        StreamerRequestDTO streamerRequestDTO1 = new StreamerRequestDTO("방송자 채널 1", "방송자 닉네임 1");
        StreamerRequestDTO streamerRequestDTO2 = new StreamerRequestDTO("방송자 채널 2", "방송자 닉네임 2");

        streamerService.registerStreamer(streamerRequestDTO1);
        streamerService.registerStreamer(streamerRequestDTO2);

        // when
        List<StreamerResponseDTO> streamerResponseDTOList = streamerService.getAllStreamers();

        // then
        assertThat(streamerResponseDTOList.size()).isEqualTo(2);

    }

    @Test
    void updateStreamer_성공() {
        // given
        StreamerRequestDTO originalRequestDTO = new StreamerRequestDTO("방송자 채널 1", "방송자 닉네임 1");
        StreamerResponseDTO savedResponseDTO = streamerService.registerStreamer(originalRequestDTO);

        StreamerRequestDTO updatedRequestDTO = new StreamerRequestDTO("방송자 채널 2", "방송자 닉네임 2");

        // when
        StreamerResponseDTO updatedResponseDTO = streamerService.updateStreamer(savedResponseDTO.getId(), updatedRequestDTO);

        // then
        assertThat(updatedResponseDTO.getId()).isEqualTo(savedResponseDTO.getId());
        assertThat(updatedResponseDTO.getChannelId()).isEqualTo(updatedRequestDTO.getChannelId());
        assertThat(updatedResponseDTO.getNickname()).isEqualTo(updatedRequestDTO.getNickname());

        Streamer streamer = streamerRepository.findById(savedResponseDTO.getId()).orElseThrow();
        assertThat(streamer.getChannelId()).isEqualTo(updatedRequestDTO.getChannelId());
        assertThat(streamer.getNickname()).isEqualTo(updatedRequestDTO.getNickname());

    }

    @Test
    void deleteStreamer_성공() {
        // given
        StreamerRequestDTO streamerRequestDTO = new StreamerRequestDTO("방송자 채널 1", "방송자 닉네임 1");
        StreamerResponseDTO streamerResponseDTO = streamerService.registerStreamer(streamerRequestDTO);

        // when
        streamerService.deleteStreamer(streamerResponseDTO.getId());

        // then
        Optional<Streamer> deleted = streamerRepository.findById(streamerResponseDTO.getId());
        assertThat(deleted).isNotPresent();
    }

    @Test
    void deleteStreamer_존재하지않음_예외발생() {
        // given
        Long nonExistentStreamerId = 1L;

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                streamerService.deleteStreamer(nonExistentStreamerId)
        );
    }

}