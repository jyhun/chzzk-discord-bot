package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.SubscriptionRequestDTO;
import com.streampulse.backend.entity.*;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.DiscordChannelRepository;
import com.streampulse.backend.repository.StreamerRepository;
import com.streampulse.backend.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private StreamerRepository streamerRepository;
    @Mock private DiscordChannelRepository discordChannelRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void createSubscription_shouldCreateNewSubscription_withKeyword() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setDiscordChannelId("channel1");
        dto.setDiscordGuildId("guild1");
        dto.setEventType(EventType.CHANGE);
        dto.setStreamerId("abc123");
        dto.setKeyword("게임");

        DiscordChannel discordChannel = DiscordChannel.builder()
                .discordChannelId("channel1")
                .discordGuildId("guild1")
                .active(true)
                .build();
        Streamer streamer = Streamer.builder().channelId("abc123").build();

        when(discordChannelRepository.findByDiscordChannelId("channel1")).thenReturn(Optional.of(discordChannel));
        when(streamerRepository.findByChannelId("abc123")).thenReturn(Optional.of(streamer));
        when(subscriptionRepository.findActiveByChannelAndStreamerAndEventType(any(), any(), any())).thenReturn(Optional.empty());
        when(subscriptionRepository.existsGlobalSubscription(any(), any())).thenReturn(false);

        subscriptionService.createSubscription(dto);

        verify(subscriptionRepository).save(argThat(sub ->
                sub.getEventType() == EventType.CHANGE &&
                        sub.getStreamer() == streamer &&
                        sub.getDiscordChannel() == discordChannel &&
                        sub.getKeywords().stream().anyMatch(k -> k.getValue().equals("게임"))
        ));
    }

    @Test
    void createSubscription_shouldThrow_ifKeywordMissingForChangeEvent() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setEventType(EventType.CHANGE);
        dto.setKeyword("  ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> subscriptionService.createSubscription(dto));
        assertTrue(ex.getMessage().contains("키워드가 필수"));
    }

    @Test
    void createSubscription_shouldThrow_ifAlreadyHasGlobalSubscription() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setDiscordChannelId("channel1");
        dto.setStreamerId("abc123");
        dto.setEventType(EventType.HOT);

        when(discordChannelRepository.findByDiscordChannelId("channel1"))
                .thenReturn(Optional.of(DiscordChannel.builder().build()));
        when(streamerRepository.findByChannelId("abc123"))
                .thenReturn(Optional.of(Streamer.builder().build()));
        when(subscriptionRepository.existsGlobalSubscription("channel1", EventType.HOT))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> subscriptionService.createSubscription(dto));
    }

    @Test
    void createSubscription_shouldThrow_ifAlreadyHasPerStreamerSubscription() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setDiscordChannelId("channel1");
        dto.setEventType(EventType.START);

        when(discordChannelRepository.findByDiscordChannelId("channel1"))
                .thenReturn(Optional.of(DiscordChannel.builder().build()));
        when(subscriptionRepository.existsPerStreamerSubscription("channel1", EventType.START))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> subscriptionService.createSubscription(dto));
    }

    @Test
    void createSubscription_shouldThrow_ifKeywordAlreadyExists() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setDiscordChannelId("channel1");
        dto.setStreamerId("abc123");
        dto.setEventType(EventType.CHANGE);
        dto.setKeyword("게임");

        DiscordChannel discordChannel = DiscordChannel.builder().discordChannelId("channel1").build();
        Streamer streamer = Streamer.builder().channelId("abc123").build();
        Keyword keyword = Keyword.builder().value("게임").build();
        Subscription existing = Subscription.builder()
                .discordChannel(discordChannel)
                .streamer(streamer)
                .eventType(EventType.CHANGE)
                .keywords(new ArrayList<>(List.of(keyword)))
                .build();

        when(discordChannelRepository.findByDiscordChannelId("channel1")).thenReturn(Optional.of(discordChannel));
        when(streamerRepository.findByChannelId("abc123")).thenReturn(Optional.of(streamer));
        when(subscriptionRepository.existsGlobalSubscription(any(), any())).thenReturn(false);
        when(subscriptionRepository.findActiveByChannelAndStreamerAndEventType("channel1", "abc123", EventType.CHANGE))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> subscriptionService.createSubscription(dto));
    }

    @Test
    void deactivateSubscription_shouldThrow_ifNotFound() {
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setDiscordChannelId("ch1");
        when(subscriptionRepository.findActiveByChannel("ch1")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> subscriptionService.deactivateSubscription(dto));
    }

    @Test
    void deactivateSubscription_shouldCallDeactivate() {
        Subscription sub = mock(Subscription.class);
        SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setDiscordChannelId("ch1");
        when(subscriptionRepository.findActiveByChannel("ch1")).thenReturn(List.of(sub));

        subscriptionService.deactivateSubscription(dto);
        verify(sub).deactivate();
    }

    @Test
    void detectChangeEvent_shouldSendNotificationForMatchingKeyword() {
        Keyword keyword = Keyword.builder().value("게임").build();
        DiscordChannel channel = DiscordChannel.builder().discordChannelId("discord1").build();
        Subscription subscription = Subscription.builder()
                .keywords(new ArrayList<>(List.of(keyword)))
                .discordChannel(channel)
                .build();

        LiveResponseDTO dto = new LiveResponseDTO();
        dto.setChannelId("abc123");
        dto.setLiveTitle("이 게임 너무 재밌다!");
        dto.setLiveCategoryValue("엔터");
        dto.setTags(List.of("tag1"));

        when(subscriptionRepository.findByStreamer_ChannelIdAndEventTypeAndActiveTrue("abc123", EventType.CHANGE))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.findByStreamerIsNullAndEventTypeAndActiveTrue(EventType.CHANGE))
                .thenReturn(Collections.emptyList());

        subscriptionService.detectChangeEvent(dto);

        verify(notificationService).requestChangeEventNotification(
                eq("abc123"),
                eq("discord1"),
                eq(List.of("게임")),
                eq(dto)
        );
    }
}