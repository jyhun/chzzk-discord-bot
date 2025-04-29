package com.streampulse.backend.dto;

import com.streampulse.backend.entity.StreamSession;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StreamMetricsInputDTO {
    StreamSession session;
    LiveResponseDTO dto;
    int averageViewerCount;
}
