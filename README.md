# 치지직 디스코드 알림 봇

치지직 공식 API 기반으로 실시간 방송 이벤트(시작/종료/급상승/키워드 포함)를 감지하고  
디스코드 채널에 자동으로 알림을 전송하는 실시간 방송 반응 감지 시스템입니다.

---

## 디스코드 봇 초대 링크

[디스코드 초대하기](https://discord.com/oauth2/authorize?client_id=1360521991580553226&permissions=2048&scope=bot%20applications.commands)

---

## 사용 가능한 명령어

### `/subscribe` 구독 명령어

- `/subscribe HOT` : 전체 방송자 급상승 감지 구독  
- `/subscribe HOT <채널ID>` : 특정 방송자 급상승 감지 구독  
- `/subscribe START` : 전체 방송자 방송 시작 구독  
- `/subscribe START <채널ID>` : 특정 방송자 방송 시작 구독  
- `/subscribe END` : 전체 방송자 방송 종료 구독  
- `/subscribe END <채널ID>` : 특정 방송자 방송 종료 구독  
- `/subscribe TOPIC <채널ID> <키워드>` : 특정 방송자 키워드 포함시 감지  
- `/subscribe TOPIC <키워드>` : 전체 방송자 키워드 포함시 감지  

### `/unsubscribe` 구독 해제 명령어

- `/unsubscribe` : 전체 구독 해제  
- `/unsubscribe <이벤트>` : 특정 이벤트 해제  
- `/unsubscribe <이벤트> <채널ID>` : 특정 방송자의 이벤트 해제  

### `/subscriptions` 구독 조회 명령어

- `/subscriptions` : 전체 구독 목록  
- `/subscriptions <이벤트>` : 특정 이벤트 목록  
- `/subscriptions <이벤트> <채널ID>` : 특정 방송자 구독 여부  
- `/subscriptions TOPIC <채널ID> <키워드>` : 키워드 포함 여부 확인  
