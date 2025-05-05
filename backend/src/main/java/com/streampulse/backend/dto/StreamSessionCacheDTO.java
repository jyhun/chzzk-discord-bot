package com.streampulse.backend.dto;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StreamSessionCacheDTO {

    private Long id;
    private String streamerChannelId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static StreamSessionCacheDTO fromEntity(StreamSession session) {
        return StreamSessionCacheDTO.builder()
                .id(session.getId())
                .streamerChannelId(session.getStreamer().getChannelId())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .tags(session.getTags().stream()
                        .map(Tag::getValue)
                        .collect(Collectors.toList()))
                .createdAt(session.getCreatedAt())
                .build();
    }

    public StreamSession toEntity(Streamer streamer) {
        StreamSession session = StreamSession.builder()
                .id(id)
                .streamer(streamer)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .build();

        if (tags != null && !tags.isEmpty()) {
            List<Tag> tagEntities = tags.stream()
                    .map(tagValue -> Tag.builder().streamSession(session).value(tagValue).build())
                    .collect(Collectors.toList());
            session.addTags(tagEntities);
        }

        return session;
    }

}