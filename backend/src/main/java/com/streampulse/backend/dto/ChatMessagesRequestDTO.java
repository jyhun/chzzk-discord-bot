package com.streampulse.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessagesRequestDTO {
    private List<String> messages;

    @Override
    public String toString() {
        return "ChatMessagesRequestDTO{messagesSize=" + (messages != null ? messages.size() : 0) + "}";
    }
}
