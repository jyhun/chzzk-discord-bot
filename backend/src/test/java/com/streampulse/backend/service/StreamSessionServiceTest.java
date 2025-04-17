package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.entity.Tag;
import com.streampulse.backend.repository.StreamMetricsRepository;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamSessionServiceTest {

    @Mock private StreamSessionRepository streamSessionRepository;
    @Mock private StreamerRepository streamerRepository;
    @Mock private StreamMetricsRepository streamMetricsRepository;

    @InjectMocks
    private StreamSessionService streamSessionService;

    @Test
    void getOrCreateSession_shouldCreateNewSession_ifNotLive() {
        // given
        Streamer streamer = mock(Streamer.class);
        when(streamer.isLive()).thenReturn(false);

        LiveResponseDTO dto = new LiveResponseDTO();
        dto.setLiveTitle("제목");
        dto.setLiveCategoryValue("카테고리");
        dto.setOpenDate("2024-01-01 10:00:00");

        StreamSession session = StreamSession.builder().build();
        when(streamSessionRepository.save(any())).thenReturn(session);

        // when
        StreamSession result = streamSessionService.getOrCreateSession(streamer, dto);

        // then
        verify(streamSessionRepository).save(any());
        assertEquals(session, result);
    }

    @Test
    void getOrCreateSession_shouldReturnOngoingSession_ifLive() {
        // given
        Streamer streamer = mock(Streamer.class);
        when(streamer.isLive()).thenReturn(true);
        when(streamer.getChannelId()).thenReturn("abc123");

        StreamSession session = StreamSession.builder().build();

        when(streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull("abc123"))
                .thenReturn(Optional.of(session));

        // when
        StreamSession result = streamSessionService.getOrCreateSession(streamer, new LiveResponseDTO());

        // then
        assertEquals(session, result);
    }

    @Test
    void handleStreamEnd_shouldUpdateAndSaveSessionAndStreamer() {
        // given
        Streamer streamer = mock(Streamer.class);
        when(streamer.getChannelId()).thenReturn("abc123");
        when(streamer.getId()).thenReturn(1L);

        StreamSession session = mock(StreamSession.class);
        when(session.getId()).thenReturn(99L);

        when(streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull("abc123"))
                .thenReturn(Optional.of(session));

        List<StreamMetrics> metricsList = List.of(
                StreamMetrics.builder().viewerCount(100).tags(List.of(Tag.builder().value("태그1").build())).build(),
                StreamMetrics.builder().viewerCount(200).tags(List.of(Tag.builder().value("태그1").build())).build()
        );
        when(streamMetricsRepository.findByStreamSessionId(99L)).thenReturn(metricsList);

        List<StreamSession> allSessions = List.of(
                StreamSession.builder().averageViewerCount(100).build(),
                StreamSession.builder().averageViewerCount(300).build()
        );
        when(streamSessionRepository.findByStreamerId(1L)).thenReturn(allSessions);

        // when
        streamSessionService.handleStreamEnd(streamer);

        // then
        verify(session).updateEndedAt();
        verify(session).updateAverageViewerCount(150); // (100+200)/2
        verify(session).addTags(anyList());
        verify(session).updatePeekViewerCount(200);     // 최대 시청자 수

        verify(streamSessionRepository).save(session);
        verify(streamer).updateAverageViewerCount(200); // (100+300)/2
        verify(streamerRepository).save(streamer);
    }
}
