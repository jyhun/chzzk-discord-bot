package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class LiveSyncService {

    private final ChzzkOpenApiClient chzzkOpenApiClient;
    private final StreamerRepository streamerRepository;
    private final StreamSessionRepository streamSessionRepository;

    private Set<String> liveStreamerChannelIds = new HashSet<>();

    public void syncLiveBroadcasts() {
        List<LiveResponseDTO> liveResponseDTOList = chzzkOpenApiClient.fetchLiveList();
        for (LiveResponseDTO liveResponseDTO : liveResponseDTOList) {
            liveStreamerChannelIds.add(liveResponseDTO.getChannelId());
            String channelId = liveResponseDTO.getChannelId();
            Streamer streamer;

            Optional<Streamer> optionalStreamer = streamerRepository.findByChannelId(channelId);
            if (optionalStreamer.isEmpty()) {
                streamer = Streamer.builder()
                        .nickname(liveResponseDTO.getChannelName())
                        .channelId(channelId)
                        .live(false)
                        .build();
                streamer = streamerRepository.save(streamer);
            } else {
                streamer = optionalStreamer.get();
            }

            if (!streamer.isLive()) {
                StreamSession streamSession = StreamSession.builder()
                        .streamer(streamer)
                        .title(liveResponseDTO.getLiveTitle())
                        .startedAt(LocalDateTime.parse(liveResponseDTO.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build();
                streamSessionRepository.save(streamSession);
                streamer.updateLive(true);
                streamerRepository.save(streamer);
            }
        }

        List<Streamer> streamerList = streamerRepository.findByLiveIsTrue();
        for (Streamer streamer : streamerList) {
            if (!liveStreamerChannelIds.contains(streamer.getChannelId())) {
                streamer.updateLive(false);
                streamerRepository.save(streamer);
            }
        }
    }

}
