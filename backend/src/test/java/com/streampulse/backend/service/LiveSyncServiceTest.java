package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveSyncServiceTest {

    @Mock private ChzzkOpenApiClient chzzkOpenApiClient;
    @Mock private StreamerService streamerService;
    @Mock private StreamSessionService streamSessionService;
    @Mock private StreamMetricsService streamMetricsService;
    @Mock private NotificationService notificationService;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks
    private LiveSyncService liveSyncService;

    private LiveResponseDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = new LiveResponseDTO();
        sampleDto.setChannelId("abc123");
        sampleDto.setLiveTitle("재밌는 방송");
        sampleDto.setLiveCategoryValue("게임");
        sampleDto.setTags(List.of("감동", "웃김"));
    }

    @Test
    void 방송시작이면_상태업데이트하고_START알림보냄() {
        // given
        Streamer streamer = mock(Streamer.class);
        when(streamer.isLive()).thenReturn(false);
        when(streamer.getAverageViewerCount()).thenReturn(10);
        when(streamerService.getOrCreateStreamer(sampleDto)).thenReturn(streamer);

        StreamSession session = mock(StreamSession.class);
        when(session.getId()).thenReturn(1L);
        when(streamSessionService.getOrCreateSession(streamer, sampleDto)).thenReturn(session);

        when(chzzkOpenApiClient.fetchLiveList()).thenReturn(List.of(sampleDto));

        // when
        liveSyncService.syncLiveBroadcasts();

        // then
        verify(streamerService).updateLiveStatus(streamer, true);
        verify(notificationService).requestStreamStatusNotification("abc123", EventType.START);
        verify(streamMetricsService).saveMetrics(session, sampleDto, 10);
    }

    @Test
    void 방송정보가_이전과_다르면_CHANGE알림보냄() throws ClassNotFoundException {
        // given
        Streamer streamer = mock(Streamer.class);
        when(streamer.isLive()).thenReturn(true);
        when(streamer.getAverageViewerCount()).thenReturn(20);
        when(streamerService.getOrCreateStreamer(sampleDto)).thenReturn(streamer);

        StreamSession session = mock(StreamSession.class);
        when(session.getId()).thenReturn(99L);
        when(streamSessionService.getOrCreateSession(streamer, sampleDto)).thenReturn(session);

        when(chzzkOpenApiClient.fetchLiveList()).thenReturn(List.of(sampleDto));

        // 이전 상태와 다르게 만들어서 변경 감지 테스트
        LiveResponseDTO prevDto = new LiveResponseDTO();
        prevDto.setLiveTitle("이전 방송");
        prevDto.setLiveCategoryValue("기타");
        prevDto.setTags(List.of("태그1"));

        // BroadcastSnapshot.from(prevDto)를 호출해서 이전 상태 생성
        Object snapshot = ReflectionTestUtils.invokeMethod(
                Class.forName("com.streampulse.backend.service.LiveSyncService$BroadcastSnapshot"),
                "from",
                prevDto
        );

        Map<Long, Object> cache = new java.util.concurrent.ConcurrentHashMap<>();
        cache.put(99L, snapshot);
        ReflectionTestUtils.setField(liveSyncService, "changeCache", cache);

        // when
        liveSyncService.syncLiveBroadcasts();

        // then
        verify(subscriptionService).detectChangeEvent(sampleDto);
    }

}
