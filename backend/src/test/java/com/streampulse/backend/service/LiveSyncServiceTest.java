package com.streampulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveSyncServiceTest {

    @Mock private ChzzkOpenApiClient chzzkOpenApiClient;
    @Mock private StreamerService streamerService;
    @Mock private StreamSessionService streamSessionService;
    @Mock private StreamMetricsService streamMetricsService;
    @Mock private NotificationService notificationService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LiveSyncService liveSyncService;

    @Mock
    private ObjectMapper objectMapper;

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
    void 방송시작이면_상태업데이트하고_START알림보냄() throws JsonProcessingException {
        // given
        Streamer streamer = mock(Streamer.class);
        when(streamer.isLive()).thenReturn(false);
        when(streamer.getAverageViewerCount()).thenReturn(10);
        when(streamerService.getOrCreateStreamer(sampleDto)).thenReturn(streamer);

        StreamSession session = mock(StreamSession.class);
        when(session.getId()).thenReturn(1L);
        when(streamSessionService.getOrCreateSession(streamer, sampleDto)).thenReturn(session);

        when(chzzkOpenApiClient.fetchLiveList()).thenReturn(List.of(sampleDto));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("snapshot:1")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"title\":\"재밌는 방송\"}");

        // when
        liveSyncService.syncLiveBroadcasts();

        // then
        verify(streamerService).updateLiveStatus(streamer, true);
        verify(notificationService).requestStreamStatusNotification("abc123", EventType.START);
        verify(streamMetricsService).saveMetrics(session, sampleDto, 10);
    }

    @Test
    void 방송정보가_이전과_다르면_CHANGE알림보냄() throws Exception {
        // given
        Streamer streamer = mock(Streamer.class);
        when(streamer.isLive()).thenReturn(true);
        when(streamer.getAverageViewerCount()).thenReturn(20);
        when(streamerService.getOrCreateStreamer(sampleDto)).thenReturn(streamer);

        StreamSession session = mock(StreamSession.class);
        when(session.getId()).thenReturn(99L);
        when(streamSessionService.getOrCreateSession(streamer, sampleDto)).thenReturn(session);

        when(chzzkOpenApiClient.fetchLiveList()).thenReturn(List.of(sampleDto));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("snapshot:99")).thenReturn("{\"title\":\"이전 방송\",\"category\":\"기타\",\"tags\":[\"태그1\"]}");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"title\":\"재밌는 방송\",\"category\":\"게임\",\"tags\":[\"감동\",\"웃김\"]}");

        // when
        liveSyncService.syncLiveBroadcasts();

        // then
        verify(subscriptionService).detectChangeEvent(sampleDto);
    }
}
