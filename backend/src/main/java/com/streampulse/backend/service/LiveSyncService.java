package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import com.streampulse.backend.repository.StreamMetricsRepository;
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
    private final StreamMetricsRepository streamMetricsRepository;

    private Set<String> liveStreamerChannelIds = new HashSet<>();

    public void syncLiveBroadcasts() {
        List<LiveResponseDTO> liveResponseDTOList = chzzkOpenApiClient.fetchLiveList(); // 실시간 방송중인 목록 조회
        /**
         * 실시간 방송 리스트를 순회하면서
         * 방송자가 DB에 없으면 저장
         * 방송 중이 아니라면 방송 세션 생성
         */
        for (LiveResponseDTO liveResponseDTO : liveResponseDTOList) {
            liveStreamerChannelIds.add(liveResponseDTO.getChannelId());
            String channelId = liveResponseDTO.getChannelId();
            Streamer streamer;
            StreamSession streamSession;

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

            // 방송자가 live 상태가 아니면 새 방송 세션을 생성
            if (!streamer.isLive()) {
                streamSession = StreamSession.builder()
                        .streamer(streamer)
                        .title(liveResponseDTO.getLiveTitle())
                        .startedAt(LocalDateTime.parse(liveResponseDTO.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build();
                streamSession = streamSessionRepository.save(streamSession);
                streamer.updateLive(true);
                streamerRepository.save(streamer);
            }else {
                // 방송자가 이미 방송 중이면 현재 진행 중인 세션을 가져옴
                streamSession = streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull(channelId).orElseThrow(
                        () -> new IllegalArgumentException("방송 세션을 찾을 수 없습니다.")
                );
            }

            // 현재 방송 시점의 매트릭 데이터를 저장
            StreamMetrics streamMetrics = StreamMetrics.builder()
                    .session(streamSession)
                    .collectedAt(LocalDateTime.now())
                    .viewerCount(liveResponseDTO.getConcurrentUserCount())
                    .category(liveResponseDTO.getLiveCategoryValue())
                    .title(liveResponseDTO.getLiveTitle())
                    .thumbnailUrl(liveResponseDTO.getLiveThumbnailImageUrl())
                    .build();
            streamMetricsRepository.save(streamMetrics);
        }

        // live 상태였던 방송자 중에서 현재 목록에 없는 경우 -> 방송 종료 처리
        List<Streamer> streamerList = streamerRepository.findByLiveIsTrue();
        for (Streamer streamer : streamerList) {
            if (!liveStreamerChannelIds.contains(streamer.getChannelId())) {
                streamer.updateLive(false);
                streamerRepository.save(streamer);
            }
        }
    }

}
