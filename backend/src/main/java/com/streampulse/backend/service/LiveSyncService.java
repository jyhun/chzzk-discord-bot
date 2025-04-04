package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class LiveSyncService {

    private final ChzzkOpenApiClient chzzkOpenApiClient;
    private final StreamerService streamerService;
    private final StreamSessionService streamSessionService;
    private final StreamMetricsService streamMetricsService;

    public void syncLiveBroadcasts() {
        Set<String> liveStreamerChannelIds = new HashSet<>();

        List<LiveResponseDTO> liveResponseDTOList = chzzkOpenApiClient.fetchLiveList();

        for (LiveResponseDTO dto : liveResponseDTOList) {
            liveStreamerChannelIds.add(dto.getChannelId());

            Streamer streamer = streamerService.getOrCreateStreamer(dto);
            StreamSession session = streamSessionService.getOrCreateSession(streamer, dto);

            if (!streamer.isLive()) {
                streamerService.updateLiveStatus(streamer, true);
            }

            streamMetricsService.saveMetrics(session, dto, streamer.getAverageViewerCount());
        }

        streamerService.markOfflineStreamers(liveStreamerChannelIds);
    }

}
