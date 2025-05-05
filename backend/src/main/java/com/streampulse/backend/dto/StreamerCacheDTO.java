package com.streampulse.backend.dto;


import com.streampulse.backend.entity.Streamer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public static StreamerCacheDTO fromEntity(Streamer streamer) {
        return StreamerCacheDTO.builder()
                .id(streamer.getId())
                .channelId(streamer.getChannelId())
                .nickname(streamer.getNickname())
                .live(streamer.isLive())
                .averageViewerCount(streamer.getAverageViewerCount())
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
