package com.streampulse.backend.dto;

import com.streampulse.backend.enums.EventType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NotificationRequestDTO {
    private EventType eventType;
    private String receiverId;
    private boolean success;
    private String message;
}
