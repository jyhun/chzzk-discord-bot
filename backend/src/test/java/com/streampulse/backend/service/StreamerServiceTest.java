package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.StreamerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamerServiceTest {

    @Mock private StreamerRepository streamerRepository;
    @Mock private StreamSessionService streamSessionService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private StreamerService streamerService;

    @Test
    void getOrCreateStreamer_shouldReturnExistingStreamer() {
        // given
        LiveResponseDTO dto = new LiveResponseDTO();
        dto.setChannelId("abc123");
        Streamer existing = Streamer.builder().channelId("abc123").build();
        when(streamerRepository.findByChannelId("abc123")).thenReturn(Optional.of(existing));

        // when
        Streamer result = streamerService.getOrCreateStreamer(dto);

        // then
        assertEquals(existing, result);
        verify(streamerRepository, never()).save(any());
    }

    @Test
    void getOrCreateStreamer_shouldCreateAndSaveNewStreamer() {
        // given
        LiveResponseDTO dto = new LiveResponseDTO();
        dto.setChannelId("abc123");
        dto.setChannelName("닉네임");
        dto.setConcurrentUserCount(123);

        when(streamerRepository.findByChannelId("abc123")).thenReturn(Optional.empty());

        Streamer saved = Streamer.builder().channelId("abc123").nickname("닉네임").averageViewerCount(123).build();
        when(streamerRepository.save(any())).thenReturn(saved);

        // when
        Streamer result = streamerService.getOrCreateStreamer(dto);

        // then
        assertEquals("abc123", result.getChannelId());
        assertEquals("닉네임", result.getNickname());
        assertEquals(123, result.getAverageViewerCount());
        verify(streamerRepository).save(any());
    }

    @Test
    void updateLiveStatus_shouldCallUpdateAndSave() {
        // given
        Streamer streamer = mock(Streamer.class);

        // when
        streamerService.updateLiveStatus(streamer, true);

        // then
        verify(streamer).updateLive(true);
        verify(streamerRepository).save(streamer);
    }

    @Test
    void markOfflineStreamers_shouldUpdateStatusAndNotifyAndEndSession() {
        // given
        Streamer streamer1 = mock(Streamer.class);
        when(streamer1.getChannelId()).thenReturn("abc123");

        List<Streamer> onlineStreamers = List.of(streamer1);
        when(streamerRepository.findByLiveIsTrue()).thenReturn(onlineStreamers);

        Set<String> liveIds = Set.of(); // 아무도 현재 방송 안함

        // when
        streamerService.markOfflineStreamers(liveIds);

        // then
        verify(streamerRepository).findByLiveIsTrue();
        verify(streamer1).updateLive(false);
        verify(streamerRepository).save(streamer1);
        verify(notificationService).requestStreamStatusNotification("abc123", EventType.END);
        verify(streamSessionService).handleStreamEnd(streamer1);
    }
}
