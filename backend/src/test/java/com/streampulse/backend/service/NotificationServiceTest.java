package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.NotificationRequestDTO;
import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.NotificationRepository;
import com.streampulse.backend.repository.StreamEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private StreamEventRepository streamEventRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(notificationService, "processorUrl", "https://mock.api");
    }

    @Test
    void saveNotification_shouldSaveNotificationEntity() {
        // given
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setStreamEventId(1L);
        dto.setReceiverId("user123");
        dto.setSuccess(true);
        dto.setMessage("성공");
        dto.setErrorMessage(null);

        StreamEvent streamEvent = mock(StreamEvent.class);
        when(streamEventRepository.findById(1L)).thenReturn(Optional.of(streamEvent));

        // when
        notificationService.saveNotification(dto);

        // then
        verify(notificationRepository).save(argThat(notification ->
                notification.getStreamEvent().equals(streamEvent) &&
                        notification.getReceiverId().equals("user123") &&
                        notification.isSuccess() &&
                        notification.getMessage().equals("성공") &&
                        notification.getErrorMessage() == null
        ));
    }

    @Test
    void requestStreamStatusNotification_shouldCallPostApi() {
        // given
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        notificationService.requestStreamStatusNotification("abc123", EventType.START);

        // then
        verify(restTemplate).postForEntity(eq("https://mock.api/api/stream-status"), any(), eq(Void.class));
    }

    @Test
    void requestChangeEventNotification_shouldCallPostApi() {
        // given
        LiveResponseDTO dto = new LiveResponseDTO();
        dto.setLiveTitle("방송 제목");
        dto.setLiveCategoryValue("게임");
        dto.setTags(List.of("태그1", "태그2"));

        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        notificationService.requestChangeEventNotification("abc123", "discord1", List.of("감동"), dto);

        // then
        verify(restTemplate).postForEntity(eq("https://mock.api/api/stream-change"), any(), eq(Void.class));
    }

    @Test
    void requestStreamHotNotification_shouldCallPostApi() {
        // given
        Streamer streamer = Streamer.builder()
                .channelId("abc123")
                .nickname("닉네임")
                .averageViewerCount(100)
                .build();

        StreamSession session = StreamSession.builder()
                .streamer(streamer)
                .startedAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .build();

        StreamMetrics metrics = StreamMetrics.builder()
                .title("방송 제목")
                .category("게임")
                .viewerCount(200)
                .streamSession(session)
                .build();

        StreamEvent event = StreamEvent.builder()
                .id(1L)
                .streamMetrics(metrics)
                .summary("하이라이트 요약")
                .build();

        ReflectionTestUtils.setField(event, "createdAt", LocalDateTime.of(2024, 1, 1, 13, 0));

        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // when
        notificationService.requestStreamHotNotification(event);

        // then
        verify(restTemplate).postForEntity(eq("https://mock.api/api/send-notification"), any(), eq(Void.class));
    }
}
