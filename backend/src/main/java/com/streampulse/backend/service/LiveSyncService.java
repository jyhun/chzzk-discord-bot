package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LiveSyncService {

    private final ChzzkOpenApiClient chzzkOpenApiClient;
    private final StreamerService streamerService;
    private final StreamSessionService streamSessionService;

    public void syncLiveBroadcasts() {
        List<LiveResponseDTO> liveResponseDTOList = chzzkOpenApiClient.fetchLiveList();
    }

}
