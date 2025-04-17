package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.repository.StreamMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamMetricsServiceTest {

    @Mock private StreamMetricsRepository streamMetricsRepository;
    @Mock private StreamEventService streamEventService;

    @InjectMocks
    private StreamMetricsService streamMetricsService;

    @Test
    void saveMetrics_shouldSaveMetrics_andTriggerHotEvent() {
        // given
        StreamSession session = mock(StreamSession.class);
        LiveResponseDTO dto = new LiveResponseDTO();
        dto.setConcurrentUserCount(1200); // HOT 조건 만족 (>=1000, > 750)
        dto.setLiveCategoryValue("게임");
        dto.setLiveTitle("방송 제목");
        dto.setTags(List.of("태그1", "태그2"));

        StreamMetrics savedMetrics = StreamMetrics.builder()
                .viewerCount(1200)
                .title("방송 제목")
                .category("게임")
                .build();

        when(streamMetricsRepository.save(any())).thenReturn(savedMetrics);

        // when
        streamMetricsService.saveMetrics(session, dto, 500);

        // then
        verify(streamMetricsRepository).save(any(StreamMetrics.class));
        verify(streamEventService).saveStreamEvent(savedMetrics, 500);
    }

    @Test
    void saveMetrics_shouldNotTriggerHotEvent_ifViewerCountTooLow() {
        // given
        StreamSession session = mock(StreamSession.class);
        LiveResponseDTO dto = new LiveResponseDTO();
        dto.setConcurrentUserCount(300); // 300 < 1000 → HOT 조건 미충족
        dto.setLiveCategoryValue("카테고리");
        dto.setLiveTitle("제목");

        StreamMetrics savedMetrics = StreamMetrics.builder().viewerCount(300).build();
        when(streamMetricsRepository.save(any())).thenReturn(savedMetrics);

        // when
        streamMetricsService.saveMetrics(session, dto, 200);

        // then
        verify(streamEventService, never()).saveStreamEvent(any(), anyInt());
    }
}
