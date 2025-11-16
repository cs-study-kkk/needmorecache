🧠 Mini Redis – 자바 기반 인메모리 캐시 서버

목표: Redis의 핵심 동작 원리를 직접 구현하며 네트워크 서버, 캐시 메모리 관리, TTL 및 LRU 정책을 학습한다.
기간: 약 2~3주
팀 구성: 2인 (A: 네트워크 서버 / B: 캐시 저장소)

📁 프로젝트 구조
```
mini-redis/
 ├── src/
 │   ├── server/
 │   │   ├── RedisServer.java          # A 담당: TCP 서버, 명령 파싱 및 클라이언트 연결 관리
 │   │   ├── ClientHandler.java        # 클라이언트 요청을 스레드 단위로 처리
 │   │   └── CommandParser.java        # 명령어 파싱 및 실행 분기 처리
 │   │
 │   ├── cache/
 │   │   ├── CacheStore.java           # B 담당: 핵심 캐시 로직 (HashMap 기반)
 │   │   ├── CacheEntry.java           # 데이터 + TTL 정보 저장 클래스
 │   │   ├── LRUCache.java             # LRU(Least Recently Used) 캐시 정책
 │   │   ├── TTLManager.java           # TTL(만료 시간) 관리 스레드
 │   │   └── SnapshotManager.java      # 데이터 스냅샷 저장/복원 기능
 │   │
 │   └── common/
 │       └── Utils.java                # 공통 유틸, 직렬화/역직렬화, 로그 등
 │
 ├── test/
 │   └── MiniRedisTest.java            # 단위 테스트
 │
 ├── README.md
 └── build.gradle (또는 pom.xml)
```
👥 역할 분담
🧩 A 역할 – 서버 및 명령 처리 담당

목표: 클라이언트가 Redis 서버에 명령을 보낼 수 있게 TCP 서버를 구축한다.

주요 구현 내용

RedisServer

ServerSocket을 사용해 다중 클라이언트 연결 수용

클라이언트마다 ClientHandler 스레드 생성

CommandParser

문자열 명령(SET key value, GET key, DEL key)을 파싱

CacheStore 객체에 명령을 위임

프로토콜 설계

간단한 텍스트 기반 프로토콜 (OK, ERROR, VALUE ...)

명령 수행 후 결과 반환

예시
```
Client → Server: SET user:1 name=John
Server → Client: OK

Client → Server: GET user:1
Server → Client: John

Client → Server: DEL user:1
Server → Client: OK
```
🧩 B 역할 – 내부 캐시 및 데이터 관리 담당

목표: Redis의 핵심 로직인 인메모리 저장소를 구현한다.

주요 구현 내용

CacheStore

HashMap<String, CacheEntry> 기반 저장소

set(), get(), del() 메소드 구현

TTLManager

ScheduledExecutorService를 사용해 주기적으로 만료 데이터 제거

LRUCache

LinkedHashMap을 활용해 자동 캐시 교체 정책 구현

SnapshotManager

서버 종료 시 파일로 데이터 직렬화 → 재시작 시 복원

🧩 지원 명령어
```
명령어	설명	예시
SET key value [EX seconds]	값 저장 (선택적으로 TTL 설정)	SET name John EX 10
GET key	값 조회	GET name
DEL key	키 삭제	DEL name
EXISTS key	키 존재 여부 확인	EXISTS name
TTL key	남은 TTL 조회	TTL name
DUMP	전체 캐시 출력 (디버깅용)	DUMP
🚀 실행 예시
서버 실행
$ java -jar mini-redis.jar
MiniRedis Server started on port 6379...

클라이언트 접속 (telnet 또는 netcat)
$ telnet localhost 6379
SET user:1 John
OK
GET user:1
John
SET temp 123 EX 5
OK
TTL temp
3
```
⚙️ 기술 스택

Language: Java 17+

Networking: Java Socket API

Concurrency: Thread / ExecutorService

Data Structure: HashMap, LinkedHashMap

Persistence: Object Serialization (Snapshot)

Build: Gradle 또는 Maven

💡 확장 아이디어

PERSIST / SAVE 명령 추가 (파일 저장)

JSON 직렬화 방식 도입

클러스터링 또는 Master-Slave 구조 확장

Pub/Sub 기능 구현 (간단한 메시징)

✅ 학습 포인트

네트워크 소켓 통신 구조 이해 (TCP 서버–클라이언트)

캐시 데이터 구조 설계 및 TTL, LRU 정책 구현

멀티스레드 환경에서의 동시성 제어

간단한 프로토콜 및 직렬화 설계 경험
