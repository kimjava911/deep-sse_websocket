# SSE & WebSocket 실시간 통신 데모

Spring Boot 기반 실시간 알림 시스템과 1:1 채팅 시스템 데모 프로젝트입니다.

## 기술 스택

- **Backend**: Spring Boot 3.5, Spring Security, Spring WebSocket (STOMP)
- **Database**: MySQL (운영) / H2 (테스트)
- **Cache & Pub/Sub**: Redis
- **Frontend**: Thymeleaf, SockJS, STOMP.js

## 테스트 계정

| 역할 | 아이디 | 비밀번호 |
|------|--------|----------|
| 관리자 | `admin` | `admin1234` |
| 일반 사용자 | `user1` | `user1234` |

## 시연 플로우

### 1. 알림 시스템 (SSE + WebSocket)

관리자가 WebSocket으로 알림을 발송하고, 사용자가 SSE로 실시간 수신하는 시나리오입니다.

#### Step 1: 브라우저 2개 준비

```
브라우저 A (Chrome)        브라우저 B (Firefox 또는 시크릿)
├── admin 로그인           ├── user1 로그인
└── 알림 발송 역할          └── 알림 수신 역할
```

#### Step 2: user1 - SSE 구독 시작

1. 브라우저 B에서 `http://localhost:8080` 접속
2. `user1 / user1234` 로그인
3. **SSE 구독** 링크 클릭 → `/notifications/sse`
4. 화면에 `안읽음: 0` 표시 확인
5. 개발자 도구(F12) > Network 탭에서 `notifications` 요청이 `EventStream` 타입인지 확인

#### Step 3: admin - 전체 알림 발송

1. 브라우저 A에서 `http://localhost:8080` 접속
2. `admin / admin1234` 로그인
3. **어드민 콘솔** 링크 클릭 → `/admin/console`
4. 전체 발송 섹션에서:
    - title: `공지사항`
    - body: `서버 점검 예정입니다`
5. **Broadcast** 버튼 클릭

#### Step 4: user1 - 실시간 수신 확인

브라우저 B의 알림 인박스에서 즉시 확인:
- 안읽음 카운트가 `1`로 증가
- 목록 최상단에 `[UNREAD] 공지사항` 표시
- **읽음** 버튼 클릭 시 `[READ]`로 변경, 카운트 감소

#### Step 5: admin - 특정 사용자 알림 발송

1. 브라우저 A의 어드민 콘솔에서 유저 지정 발송 섹션:
    - target username: `user1`
    - title: `개인 알림`
    - body: `포인트가 적립되었습니다`
2. **Send To User** 버튼 클릭
3. 브라우저 B에서 즉시 수신 확인

---

### 2. 채팅 시스템 (WebSocket + Redis Pub/Sub)

두 사용자 간 1:1 실시간 채팅 시나리오입니다.

#### Step 1: 양쪽 브라우저에서 채팅 화면 진입

```
브라우저 A (admin)          브라우저 B (user1)
├── /chat 접속              ├── /chat 접속
└── 상대: user1 입력         └── 상대: admin 입력
```

#### Step 2: admin - 채팅방 생성

1. 브라우저 A에서 `admin` 로그인 후 **채팅으로 이동** 클릭
2. 상대 username 입력란에 `user1` 입력
3. **방 생성/조회** 버튼 클릭
4. `roomId: 1` (또는 생성된 번호) 표시 확인
5. 로그에 `stomp connected`, `subscribed: /topic/chat/rooms/1` 출력

#### Step 3: user1 - 동일 채팅방 입장

1. 브라우저 B에서 `user1` 로그인 후 채팅 화면 진입
2. 상대 username에 `admin` 입력
3. **방 생성/조회** 클릭
4. 동일한 `roomId`가 표시됨 (1:1 방은 중복 생성되지 않음)

#### Step 4: 실시간 메시지 교환

| 브라우저 A (admin) | 브라우저 B (user1) |
|-------------------|-------------------|
| 메시지 입력: `안녕하세요!` → 전송 | |
| | 즉시 수신: `admin: 안녕하세요!` |
| | 메시지 입력: `네, 반갑습니다` → 전송 |
| 즉시 수신: `user1: 네, 반갑습니다` | |

#### Step 5: 새로고침 후 메시지 복원 확인

1. 브라우저 B 새로고침
2. 다시 채팅방 입장 (상대: `admin` → 방 생성/조회)
3. 이전 대화 내역이 자동 로딩됨 (Redis 캐시 또는 DB fallback)

---

### 3. 디버그 & 모니터링

#### SSE 연결 상태 확인 (Admin 전용)

```
GET /api/admin/sse/active-users
```

현재 SSE 연결된 사용자 목록을 반환합니다.

```bash
curl -u admin:admin1234 http://localhost:8080/api/admin/sse/active-users
# 응답 예: ["user1"]
```

#### 개발자 도구 활용

| 기능 | 확인 위치 |
|------|----------|
| SSE 이벤트 | Network 탭 > `notifications` > EventStream |
| WebSocket 프레임 | Network 탭 > `websocket` > Messages |
| STOMP 디버그 | Console 탭 (stomp.debug 활성화 시) |

---

## 주요 엔드포인트 정리

### 페이지 라우팅

| URL | 설명 | 권한 |
|-----|------|------|
| `/` | 메인 페이지 | 전체 |
| `/admin` | 어드민 페이지 | ADMIN |
| `/admin/console` | 알림 발송 콘솔 | ADMIN |
| `/chat` | 1:1 채팅 | 로그인 |
| `/notifications/sse` | 알림 인박스 | 로그인 |

### REST API

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/notifications` | 알림 목록 조회 |
| GET | `/api/notifications/unread-count` | 안읽음 카운트 |
| POST | `/api/notifications/{id}/read` | 읽음 처리 |
| POST | `/api/chat/room` | 채팅방 생성/조회 |
| GET | `/api/chat/rooms/{roomId}/messages` | 최근 메시지 조회 |

### WebSocket (STOMP)

| 방향 | Destination | 설명 |
|------|-------------|------|
| Client → Server | `/app/admin/notifications/broadcast` | 전체 알림 발송 |
| Client → Server | `/app/admin/notifications/target` | 타겟 알림 발송 |
| Client → Server | `/app/chat/rooms/{roomId}/send` | 채팅 메시지 전송 |
| Server → Client | `/topic/chat/rooms/{roomId}` | 채팅 메시지 수신 |

### SSE

| URL | 설명 |
|-----|------|
| `/sse/notifications` | 알림 실시간 구독 |

---

## 실행 방법

```bash
# 1. 환경설정 파일 복사
cp src/main/resources/application-dev-sample.yml src/main/resources/application-dev.yml

# 2. application-dev.yml에 MySQL, Redis 연결 정보 입력

# 3. 실행
./gradlew bootRun

# 4. 브라우저에서 http://localhost:8080 접속
```

---

## 아키텍처 요약

```
┌─────────────┐    WebSocket     ┌─────────────────┐
│   Admin     │ ───────────────► │  Spring Boot    │
│  (발송자)    │   STOMP          │                 │
└─────────────┘                  │  ┌───────────┐  │
                                 │  │ Redis     │  │
┌─────────────┐    SSE           │  │ Pub/Sub   │  │
│   User      │ ◄─────────────── │  └───────────┘  │
│  (수신자)    │   EventStream    │                 │
└─────────────┘                  │  ┌───────────┐  │
                                 │  │ MySQL     │  │
┌─────────────┐    WebSocket     │  │ (영속성)   │  │
│   User A    │ ◄──────────────► │  └───────────┘  │
│   User B    │   채팅            └─────────────────┘
└─────────────┘
```