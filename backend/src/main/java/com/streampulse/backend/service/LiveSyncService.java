package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class LiveSyncService {

    private final ChzzkOpenApiClient chzzkOpenApiClient;
    private final StreamerService streamerService;
    private final StreamSessionService streamSessionService;
    private final StreamMetricsService streamMetricsService;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;

    // 세션별 방송 상태 캐시 (변경 감지용)
    private final Map<Long, BroadcastSnapshot> changeCache = new ConcurrentHashMap<>();

    public void syncLiveBroadcasts() {
        Set<String> liveStreamerIds = new HashSet<>();

        List<LiveResponseDTO> liveList = chzzkOpenApiClient.fetchLiveList();

        for (LiveResponseDTO dto : liveList) {
            String channelId = dto.getChannelId();
            liveStreamerIds.add(channelId);

            Streamer streamer = streamerService.getOrCreateStreamer(dto);
            StreamSession session = streamSessionService.getOrCreateSession(streamer, dto);

            if (!streamer.isLive()) {
                streamerService.updateLiveStatus(streamer, true);
                notificationService.requestStreamStatusNotification(channelId, EventType.START);
            }

            streamMetricsService.saveMetrics(session, dto, streamer.getAverageViewerCount());

            // 변경 사항 있을 경우 알림
            if (hasChanged(session, dto)) {
                subscriptionService.detectChangeEvent(dto);
            }
        }

        streamerService.markOfflineStreamers(liveStreamerIds);
    }

    /**
     * 이전 상태와 비교해 변경 여부 판단 & 캐시 갱신
     */
    private boolean hasChanged(StreamSession session, LiveResponseDTO dto) {
        Long sessionId = session.getId();
        BroadcastSnapshot prev = changeCache.get(sessionId);
        BroadcastSnapshot curr = BroadcastSnapshot.from(dto);

        if (!curr.equals(prev)) {
            changeCache.put(sessionId, curr);
            return true;
        }
        return false;
    }

    /**
     * 방송 상태 스냅샷 클래스 (동등성 비교에 사용)
     */
    private static class BroadcastSnapshot {
        private final String title;
        private final String category;
        private final List<String> tags;

        private BroadcastSnapshot(String title, String category, List<String> tags) {
            this.title = title;
            this.category = category;
            this.tags = tags != null ? List.copyOf(tags) : List.of();
        }

        public static BroadcastSnapshot from(LiveResponseDTO dto) {
            return new BroadcastSnapshot(
                    dto.getLiveTitle(),
                    dto.getLiveCategoryValue(),
                    dto.getTags()
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BroadcastSnapshot that)) return false;
            return Objects.equals(title, that.title) &&
                    Objects.equals(category, that.category) &&
                    Objects.equals(tags, that.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, category, tags);
        }
    }
}
