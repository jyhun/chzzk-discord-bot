package com.streampulse.backend.service;

import com.streampulse.backend.dto.ChatMessagesRequestDTO;
import com.streampulse.backend.entity.Highlight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ChatService {

    public void collectChatsForHighlight(Highlight highlight) {
        try {
            String channelId = highlight.getMetrics().getSession().getStreamer().getChannelId();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "node", "index.js", channelId
            );

            processBuilder.directory(new java.io.File("../collector/crawler"));

            processBuilder.start();

            log.info("채팅 수집 프로세스 실행됨 - 채널:{}", channelId);
        }catch (Exception e) {
            log.error("채팅 수집 실행 실패", e);
        }
    }

    public void collectChats(String channelId, ChatMessagesRequestDTO chatMessagesRequestDTO) {
        log.info("channelId: {}", channelId);
        log.info("chatMessagesRequestDTO: {}", chatMessagesRequestDTO.getMessages().toString());
    }

}
