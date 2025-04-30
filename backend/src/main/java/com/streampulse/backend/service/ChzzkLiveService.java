package com.streampulse.backend.service;

import com.streampulse.backend.dto.ChzzkRootResponseDTO;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import com.streampulse.backend.infra.RedisCursorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChzzkLiveService {

    private final ChzzkOpenApiClient chzzkOpenApiClient;
    private final RedisCursorStore redisCursorStore;

    private static final int RETRY_WAIT_MS = 10000;
    private static final String NO_CURSOR = "";
    private static final int VIEWER_THRESHOLD = 1;
    private static final String NEXT_KEY    = "cursor_zset:next";
    private static final String CURRENT_KEY = "cursor_zset:current";

    private static class Node {
        final String cursor;
        final Node parent;
        final int pageIndex;

        Node(String cursor, Node parent, int pageIndex) {
            this.cursor = cursor;
            this.parent = parent;
            this.pageIndex = pageIndex;
        }
    }

    public void fetchAndStoreValidCursors() {
        Map<Integer, String> indexToCursor = new LinkedHashMap<>();
        Set<String> failed = new HashSet<>();
        Set<String> visited = new HashSet<>();

        Node current = new Node(NO_CURSOR, null, 0);
        visited.add(NO_CURSOR);

        while (true) {
            String cur = current.cursor;
            // 1) API 호출
            ChzzkRootResponseDTO resp = chzzkOpenApiClient.fetchPage(cur);

            // 2) 실패 시: handleInvalidNext로 백트랙
            if (resp == null || resp.getContent() == null) {
                failed.add(cur);
                Node back = handleInvalidNext(current, cur, visited, failed);
                if (back == null) break;
                current = back;
                continue;
            }

            List<LiveResponseDTO> data = resp.getContent().getData();
            // 3) 데이터 비었으면 종료
            if (data.isEmpty()) break;

            // 4) 성공한 커서 저장
            indexToCursor.put(current.pageIndex, cur);

            // 5) 종료 조건: 마지막 뷰어 수
            int lastCount = data.get(data.size() - 1).getConcurrentUserCount();
            if (lastCount < VIEWER_THRESHOLD) break;

            String next = resp.getContent().getPage().getNext();
            // 6) 중복/실패 커서 처리
            if (failed.contains(next) || visited.contains(next)) {
                Node back = handleInvalidNext(current, next, visited, failed);
                if (back == null) break;
                current = back;
                continue;
            }

            // 7) 다음으로 전진
            visited.add(next);
            current = new Node(next, current, current.pageIndex + 1);
        }

        log.info("저장된 커서 개수 = {}", indexToCursor.size());
        redisCursorStore.saveZSet(NEXT_KEY, indexToCursor);
        redisCursorStore.rename(NEXT_KEY, CURRENT_KEY);
    }

    public List<LiveResponseDTO> collectLiveBroadcastersFromRedis() {
        return redisCursorStore.loadZSet(CURRENT_KEY)
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .map(chzzkOpenApiClient::fetchPage)
                .filter(rootResponseDTO -> rootResponseDTO != null && rootResponseDTO.getContent() != null)
                .flatMap(rootResponseDTO -> rootResponseDTO.getContent().getData().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private Node handleInvalidNext(Node current, String targetCursor,
                                   Set<String> visited, Set<String> failed) {
        Node parent = current.parent;
        if (parent == null) return null;

        // 재시도: 부모에서 새로운 next 받기
        ChzzkRootResponseDTO chzzkRootResponseDTO;
        String parentNext;
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            chzzkRootResponseDTO = chzzkOpenApiClient.fetchPage(parent.cursor);
            if (chzzkRootResponseDTO == null || chzzkRootResponseDTO.getContent() == null) {
                failed.add(parent.cursor);
                return parent.parent;
            }
            parentNext = chzzkRootResponseDTO.getContent().getPage().getNext();
        } while (parentNext.equals(targetCursor));

        if (failed.contains(parentNext) || visited.contains(parentNext)) {
            return parent.parent;
        }
        visited.add(parentNext);
        return new Node(parentNext, parent, parent.pageIndex + 1);
    }
}