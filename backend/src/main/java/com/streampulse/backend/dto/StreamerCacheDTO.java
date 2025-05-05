package com.streampulse.backend.dto;


import com.streampulse.backend.entity.Streamer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamerCacheDTO {

    private Long id;
    private String channelId;
    private String nickname;
    private boolean live;
    private int averageViewerCount;
    private LocalDateTime createdAt;

    public static StreamerCacheDTO fromEntity(Streamer streamer) {
        return StreamerCacheDTO.builder()
                .id(streamer.getId())
                .channelId(streamer.getChannelId())
                .nickname(streamer.getNickname())
                .live(streamer.isLive())
                .averageViewerCount(streamer.getAverageViewerCount())
                .createdAt(streamer.getCreatedAt())
                .build();
    }

    public Streamer toEntity() {
        return Streamer.builder()
                .id(id)
                .channelId(channelId)
                .nickname(nickname)
                .live(live)
                .averageViewerCount(averageViewerCount)
                .build();
    }
}
