package com.streampulse.backend.service;

import com.streampulse.backend.dto.ChzzkRootResponseDTO;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import com.streampulse.backend.infra.RedisCursorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChzzkLiveService {

    private final ChzzkOpenApiClient chzzkOpenApiClient;
    private final RedisCursorStore redisCursorStore;

    private static final int RETRY_WAIT_MS = 10000;
    private static final String NO_CURSOR = "";
    private static final int viewerThreshold = 1000;


    private static class Node {
        String cursor;
        Node parent;

        Node(String cursor, Node parent) {
            this.cursor = cursor;
            this.parent = parent;
        }
    }

    public void fetchAndStoreValidCursors() {
        List<String> validCursors = new ArrayList<>();
        Set<String> failedCursors = new HashSet<>();
        Set<String> visitedCursors = new HashSet<>();
        Node currentNode = new Node(NO_CURSOR, null);
        visitedCursors.add(NO_CURSOR);

        while (true) {
            String curCursor = currentNode.cursor;
            ChzzkRootResponseDTO response = chzzkOpenApiClient.fetchPage(curCursor);

            if (response == null || response.getContent() == null) {
                Node newNode = handleInvalidNext(currentNode, curCursor, visitedCursors, failedCursors);
                if (newNode == null) break;
                currentNode = newNode;
                continue;
            }

            String nextCursor = response.getContent().getPage().getNext();
            if (failedCursors.contains(nextCursor) || visitedCursors.contains(nextCursor)) {
                currentNode = (currentNode.parent != null) ? currentNode.parent : currentNode;
                continue;
            }

            visitedCursors.add(nextCursor);
            List<LiveResponseDTO> data = response.getContent().getData();
            if (data.isEmpty()) break;

            int topViewerCount = data.get(0).getConcurrentUserCount();
            int lastViewerCount = data.get(data.size() - 1).getConcurrentUserCount();

            if (topViewerCount >= viewerThreshold) {
                validCursors.add(curCursor);
            }

            currentNode = new Node(nextCursor, currentNode);

            if (lastViewerCount < viewerThreshold) break;
        }

        redisCursorStore.save("cursor_list:next",validCursors);
        redisCursorStore.rename("cursor_list:next", "cursor_list:current");
    }


    public List<LiveResponseDTO> collectLiveBroadcastersFromRedis() {
        List<LiveResponseDTO> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<String> cursors = redisCursorStore.load("cursor_list:current");

        for (String cursor : cursors) {
            ChzzkRootResponseDTO response = chzzkOpenApiClient.fetchPage(cursor);
            if (response == null || response.getContent() == null) continue;

            for (LiveResponseDTO dto : response.getContent().getData()) {
                String channelId = dto.getChannelId();
                if (!visited.add(channelId)) continue;
                result.add(dto);
            }
        }

        return result;
    }

    private Node handleInvalidNext(Node currentNode, String errorNext,
                                   Set<String> visited, Set<String> failed) {
        Node parent = currentNode.parent;
        if (parent == null) return null;

        ChzzkRootResponseDTO parentResp = chzzkOpenApiClient.fetchPage(parent.cursor);
        if (parentResp == null || parentResp.getContent() == null) {
            failed.add(parent.cursor);
            return (parent.parent != null) ? parent.parent : currentNode;
        }

        String parentNext = parentResp.getContent().getPage().getNext();
        while (parentNext.equals(errorNext)) {
            try {
                Thread.sleep(RETRY_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            parentResp = chzzkOpenApiClient.fetchPage(parent.cursor);
            if (parentResp == null || parentResp.getContent() == null) break;
            parentNext = parentResp.getContent().getPage().getNext();
        }

        if (parentResp == null || parentResp.getContent() == null) {
            return (parent.parent != null) ? parent.parent : currentNode;
        }

        if (visited.contains(parentNext)) {
            return parent.parent;
        }

        visited.add(parentNext);
        return new Node(parentNext, parent);
    }

}
