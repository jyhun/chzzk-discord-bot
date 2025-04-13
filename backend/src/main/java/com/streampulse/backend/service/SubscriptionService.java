package com.streampulse.backend.service;

import com.streampulse.backend.dto.SubscriptionRequestDTO;
import com.streampulse.backend.dto.SubscriptionResponseDTO;
import com.streampulse.backend.entity.DiscordUser;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.entity.Subscription;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.DiscordUserRepository;
import com.streampulse.backend.repository.StreamerRepository;
import com.streampulse.backend.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StreamerRepository streamerRepository;
    private final DiscordUserRepository discordUserRepository;

    public void createSubscription(SubscriptionRequestDTO subscriptionRequestDTO) {
        DiscordUser discordUser = discordUserRepository.findByDiscordUserId(subscriptionRequestDTO.getDiscordUserId())
                .orElseGet(() -> discordUserRepository.save(
                        DiscordUser.builder()
                                .discordUserId(subscriptionRequestDTO.getDiscordUserId())
                                .username(subscriptionRequestDTO.getUsername())
                                .active(true)
                                .build()));

        Streamer streamer = null;

        if(subscriptionRequestDTO.getStreamerId() != null) {
            streamer = streamerRepository.findByChannelId(subscriptionRequestDTO.getStreamerId())
                    .orElseThrow(() -> new IllegalArgumentException("방송자를 찾을 수 없습니다."));
        }

        Subscription subscription = Subscription.builder()
                .discordUser(discordUser)
                .streamer(streamer)
                .eventType(subscriptionRequestDTO.getEventType())
                .keyword(subscriptionRequestDTO.getKeyword())
                .active(true)
                .build();

        subscriptionRepository.save(subscription);


    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> getSubscriptions(String streamerId, EventType eventType) {

        log.info("streamerId:{}, eventType:{}", streamerId, eventType);

        List<Subscription> subscriptions = new ArrayList<>();

        // 방송자 구독자
        subscriptions.addAll(subscriptionRepository.findByStreamer_ChannelIdAndEventTypeAndActiveTrue(streamerId, eventType));

        // global 구독자
        subscriptions.addAll(subscriptionRepository.findByStreamerIsNullAndEventTypeAndActiveTrue(eventType));

        return subscriptions.stream()
                .map(sub -> new SubscriptionResponseDTO(
                        sub.getDiscordUser().getDiscordUserId(),
                        sub.getDiscordUser().getUsername()
                ))
                .collect(Collectors.toList());
    }
}
