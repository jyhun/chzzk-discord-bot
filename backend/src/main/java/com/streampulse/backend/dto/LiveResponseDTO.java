package com.streampulse.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LiveResponseDTO {
    private Long liveId;
    private String liveTitle;
    private String liveThumbnailImageUrl;
    private Integer concurrentUserCount;
    private String openDate;
    private Boolean adult;
    private List<String> tags;
    private String categoryType;
    private String liveCategory;
    private String liveCategoryValue;
    private String channelId;
    private String channelName;
    private String channelImageUrl;
}
