package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.StreamerRequestDTO;
import com.streampulse.backend.dto.StreamerResponseDTO;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;


@Service
@Transactional
@RequiredArgsConstructor
public class StreamerService {

    private final StreamerRepository streamerRepository;
    private final StreamSessionService streamSessionService;

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

    @Transactional(readOnly = true)
    public StreamerResponseDTO getStreamerByChannelId(String channelId) {
        Streamer streamer = streamerRepository.findByChannelId(channelId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채널 ID의 방송자를 찾을 수 없습니다."));

        return StreamerResponseDTO.builder()
                .id(streamer.getId())
                .channelId(streamer.getChannelId())
                .nickname(streamer.getNickname())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StreamerResponseDTO> getAllStreamers() {
        return streamerRepository.findAll().stream()
                .map(streamer -> StreamerResponseDTO.builder()
                        .id(streamer.getId())
                        .channelId(streamer.getChannelId())
                        .nickname(streamer.getNickname())
                        .build())
                .toList();
    }

    public StreamerResponseDTO updateStreamer(Long id, StreamerRequestDTO streamerRequestDTO) {
        Streamer streamer = streamerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방송자 입니다."));

        streamer.update(streamerRequestDTO);

        Streamer updatedStreamer = streamerRepository.save(streamer);

        return StreamerResponseDTO.builder()
                .id(updatedStreamer.getId())
                .channelId(updatedStreamer.getChannelId())
                .nickname(updatedStreamer.getNickname())
                .build();
    }

    public void deleteStreamer(Long id) {
        Streamer streamer = streamerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방송자 입니다."));
        streamerRepository.delete(streamer);
    }

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

    public void updateLiveStatus(Streamer streamer, boolean isLive) {
        streamer.updateLive(isLive);
        streamerRepository.save(streamer);
    }

    public void markOfflineStreamers(Set<String> liveStreamerChannelIds) {
        List<Streamer> streamerList = streamerRepository.findByLiveIsTrue();
        for (Streamer streamer : streamerList) {
            if (!liveStreamerChannelIds.contains(streamer.getChannelId())) {
                streamer.updateLive(false);
                streamerRepository.save(streamer);

                streamSessionService.handleStreamEnd(streamer);
            }
        }
    }
}
