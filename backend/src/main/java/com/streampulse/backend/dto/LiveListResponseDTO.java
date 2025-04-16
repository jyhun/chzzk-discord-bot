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

    @Override
    public String toString() {
        return "LiveListResponseDTO{dataSize=" + (data != null ? data.size() : 0) + ", page=" + page + "}";
    }
}
