package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NotificationRequestDTO {
    private Long streamEventId;
    private String receiverId;
    private boolean success;
    private String message;
    private String errorMessage;
}
