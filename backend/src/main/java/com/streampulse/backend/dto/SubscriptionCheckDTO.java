package com.streampulse.backend.dto;

import lombok.*;

import java.util.Set;

@Getter
@AllArgsConstructor
@ToString
public class SubscriptionCheckDTO {

    private final Set<String> subscribedChannelIds;
    private final boolean hasAllSubscribers;

    public boolean isSubscribed(String channelId) {
        return hasAllSubscribers || subscribedChannelIds.contains(channelId);
    }
}
