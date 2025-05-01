package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StreamSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "streamer_id", nullable = false)
    private Streamer streamer;

    @OneToMany(mappedBy = "streamSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StreamMetrics> metrics = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private int averageViewerCount;

    private int peakViewerCount;

    @OneToMany(mappedBy = "streamSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    public void addTag(Tag tag) {
        tags.add(tag);
        tag.assignStreamSession(this);
    }

    public void addTags(List<Tag> tagList) {
        if(tagList != null) {
            tagList.forEach(this::addTag);
        }
    }

    public void updateEndedAt() {
        if (endedAt == null) {
            this.endedAt = LocalDateTime.now();
        }
    }

}
