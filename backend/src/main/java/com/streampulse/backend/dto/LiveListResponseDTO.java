package com.streampulse.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LiveListResponseDTO {
    private List<LiveResponseDTO> data;
    private PageInfo page;

    @Data
    public static class PageInfo {
        private String next;
    }
}
