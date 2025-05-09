package com.streampulse.backend.service;

import com.streampulse.backend.dto.ChzzkRootResponseDTO;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.ChzzkOpenApiClient;
import com.streampulse.backend.infra.RedisCursorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String NEXT_KEY = "cursor_zset:next";
    private static final String CURRENT_KEY = "cursor_zset:current";

    @Value("${app.viewer-threshold}")
    private int VIEWER_THRESHOLD;

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

    public boolean fetchAndStoreValidCursors() {
        boolean finished;
        int maxRetries = 5;
        int retries = 0;

        Map<Integer, String> indexToCursor;
        Map<String, Set<String>> cursorToBroadcasterIds;

        do {
            retries++;
            indexToCursor = new LinkedHashMap<>();
            cursorToBroadcasterIds = new LinkedHashMap<>();

            Set<String> failed = new HashSet<>();
            Set<String> visited = new HashSet<>();
            Set<String> seenCursors = new HashSet<>();
            finished = false;

            Node current = new Node(NO_CURSOR, null, 0);
            visited.add(NO_CURSOR);
            seenCursors.add(NO_CURSOR);

            while (true) {
                String cur = current.cursor;
                ChzzkRootResponseDTO resp = chzzkOpenApiClient.fetchPage(cur);

                if (resp == null || resp.getContent() == null) {
                    log.warn("API 응답 실패 or Content 없음: cursor='{}'", cur);
                    failed.add(cur);
                    Node back = handleInvalidNext(current, cur, visited, failed);
                    if (back == null) break;
                    current = back;
                    continue;
                }

                List<LiveResponseDTO> data = resp.getContent().getData();
                if (data.isEmpty()) break;

                Set<String> currIds = data.stream()
                        .map(LiveResponseDTO::getChannelId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                // 커서별 방송자 목록 출력
                log.info("[커서 응답] pageIndex={} cursor='{}' 방송자 수={} → {}",
                        current.pageIndex, cur, currIds.size(), String.join(", ", currIds));

                // 중복 응답 여부 검사
                boolean isDuplicate = cursorToBroadcasterIds.values().stream()
                        .anyMatch(prevIds -> {
                            long overlap = currIds.stream().filter(prevIds::contains).count();
                            double ratio = (double) overlap / currIds.size();
                            return ratio >= 0.8;
                        });

                if (isDuplicate) {
                    log.warn("[중복 커서 응답 감지] pageIndex={}, cursor='{}'는 이전 커서들과 80% 이상 응답이 동일하여 제외됩니다",
                            current.pageIndex, cur);
                    return false;
                }

                indexToCursor.put(current.pageIndex, cur);
                seenCursors.add(cur);
                cursorToBroadcasterIds.put(cur, currIds);

                LiveResponseDTO lastDTO = data.get(data.size() - 1);
                int lastCount = lastDTO.getConcurrentUserCount();
                if (lastCount < VIEWER_THRESHOLD) {
                    finished = true;
                    break;
                }

                String next = resp.getContent().getPage().getNext();
                if (failed.contains(next) || visited.contains(next) || seenCursors.contains(next)) {
                    log.warn("커서 중복 또는 오류로 제외됨: {}", next);
                    Node back = handleInvalidNext(current, next, visited, failed);
                    if (back == null) break;
                    current = back;
                    continue;
                }

                visited.add(next);
                seenCursors.add(next);
                current = new Node(next, current, current.pageIndex + 1);
            }

        } while (!finished && retries < maxRetries);

        if (!finished) {
            log.warn("Threshold에 도달하지 못해 커서 저장을 건너뜁니다.");
            return false;
        }

        if (indexToCursor.size() < 3) {
            log.warn("수집된 커서가 너무 적어 저장하지 않습니다. count={}", indexToCursor.size());
            return false;
        }

        Set<String> allBroadcasters = cursorToBroadcasterIds.values().stream()
                .flatMap(Set::stream).collect(Collectors.toSet());

        log.info("[DeepScan] 저장된 커서 개수 = {}, 전체 방송자 수 = {}", indexToCursor.size(), allBroadcasters.size());
        redisCursorStore.saveZSet(NEXT_KEY, indexToCursor);
        redisCursorStore.rename(NEXT_KEY, CURRENT_KEY);

        return true;
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