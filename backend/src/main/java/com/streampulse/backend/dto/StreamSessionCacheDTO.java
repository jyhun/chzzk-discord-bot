package com.streampulse.backend.dto;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StreamSessionCacheDTO {

    private Long id;
    private String streamerChannelId;
    private String title;
    private String category;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int averageViewerCount;
    private int peakViewerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StreamSessionCacheDTO fromEntity(StreamSession session) {
        return StreamSessionCacheDTO.builder()
                .id(session.getId())
                .streamerChannelId(session.getStreamer().getChannelId())
                .title(session.getTitle())
                .category(session.getCategory())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .averageViewerCount(session.getAverageViewerCount())
                .peakViewerCount(session.getPeakViewerCount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    public StreamSession toEntity(Streamer streamer) {
        StreamSession session = StreamSession.builder()
                .id(id)
                .streamer(streamer)
                .title(title)
                .category(category)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .averageViewerCount(averageViewerCount)
                .peakViewerCount(peakViewerCount)
                .build();

        return session;
    }
}