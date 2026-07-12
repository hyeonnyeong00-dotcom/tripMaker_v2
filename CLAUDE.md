# CLAUDE.md — AI 여행 일정 플래너

> 이 파일은 모든 세션에서 자동으로 읽힌다. 여기 적힌 규칙과 계약은 별도 지시 없이도 항상 준수한다.
> 상세 요구사항: `docs/PRD.md` / DB 스키마: `docs/ERD.md` / 데이터 스펙: `docs/data-spec.md`
> 문서 간 충돌 시 우선순위: **CLAUDE.md > ERD 델타(§5.6) > ERD.md > PRD.md** (최신 결정 반영)

## 1. 프로젝트 개요

AI 기반 여행 일정 플래너. 목적지/기간/예산/취향 입력 → AI가 일자별 동선 생성 →
지도+타임라인으로 확인 → 드래그로 순서 변경 → **변경된 날짜만 부분 재생성**(다른 날짜는 절대 수정 금지) → 저장/재열람.
포트폴리오 목적, 10일 개발, AI 실비용 $10 이내. 확장성 고려하지 않음(단일 인스턴스).

## 2. 기술 스택 (확정 — 변경 금지)

| 영역 | 스택 |
|---|---|
| 프론트엔드 | React 18 + TypeScript + Vite |
| 백엔드 | Java 17 + Spring Boot 3 + Spring MVC + Spring Data JPA |
| DB | Supabase (PostgreSQL) — 단일 저장소, Redis 없음 |
| 인증 | JWT (access token만, 만료 60분, sessionStorage 저장, refresh token 없음), BCrypt 해시 |
| 지도 | Google Maps JavaScript API — Marker + Polyline만 사용. **Directions API 사용 금지**(비용) |
| API 문서 | springdoc-openapi (Swagger UI) |
| 배포 | 프론트 → Vercel / 백엔드 → Render (Docker 멀티스테이지: gradle build → JRE 17 slim) |

- 프론트 드래그: `@dnd-kit/core` + `@dnd-kit/sortable` / 서버 상태: `@tanstack/react-query` / HTTP: `axios`
- 지도: `@vis.gl/react-google-maps`, 환경변수 `VITE_GOOGLE_MAPS_API_KEY`

## 3. 저장소 구조 (모노레포)

```
/
├── CLAUDE.md
├── docs/
│   ├── PRD.md            # 원본 PRD (읽기 전용)
│   ├── ERD.md            # DB 스키마 (읽기 전용, §5.6 델타 적용해 구현)
│   └── data-spec.md      # 8.3 JSON 스키마 + lat/lng 확장
├── frontend/src/
│   ├── api/              # axios 인스턴스, 엔드포인트당 1함수
│   ├── components/       # 공용 컴포넌트 (DestinationPicker, DayMap, ActivityCard 등)
│   ├── pages/            # 라우트 단위 페이지
│   ├── data/
│   │   └── destinations.ts  # 목적지 큐레이션 정적 데이터 (API 아님, 하드코딩 확정)
│   ├── styles/tokens.css # 디자인 토큰 (§7)
│   └── types/            # API 응답 타입 (data-spec과 snake_case 그대로 1:1)
└── backend/
    ├── Dockerfile
    └── src/main/java/com/tripplanner/
        ├── auth/  ├── trip/  ├── ai/   # ai/ = 프롬프트 빌드·파싱·검증 (핵심)
        ├── cache/ └── admin/
```

## 4. API 계약 (PRD 9절 기반 — 절대 준수)

- `POST /api/auth/signup` · `POST /api/auth/login` · `POST /api/auth/logout`
- `POST /api/auth/change-password` — `{ "current_password", "new_password" }`, 인증 필요(user/admin 공용).
  현재 비밀번호 불일치는 400 VALIDATION_ERROR(401이면 프론트 인터셉터가 강제 로그아웃하므로)
- `POST /api/trips` — 최초 생성. 요청 바디에 **`include_nearby: boolean`**(근교 포함 토글) +
  **`active_start_time: "HH:mm"`, `active_end_time: "HH:mm"`**(하루 활동 시작~종료 시간대, 기본값 "09:00"/"21:00") 추가.
  캐시 히트 시 캐시 반환. 응답: data-spec 스키마(revision=1)
- `PATCH /api/trips/{trip_id}/reorder` — `{ "day": 2, "new_activity_order": [...] }`
  응답: 전체 스키마(해당 day만 `last_modified: true`, revision +1)
- `GET /api/trips` · `GET /api/trips/{trip_id}`
- `GET /api/admin/flagged-trips` · `GET /api/admin/stats/destinations`
- `GET/PUT /api/admin/prompt-templates`, `/api/admin/prompt-templates/{id}`
- 관리자 API는 `role=admin`만, user는 403

**에러 형식(공통):** `{ "error": "VALIDATION_ERROR|GENERATION_FAILED|AUTH_ERROR|FORBIDDEN|STORAGE_ERROR", "message": "..." }`
(400 / 502 / 401 / 403 / 503 순 매핑)

**activity 스키마 확장(지도용):** 각 activity에 `lat: number`, `lng: number` 필수 포함.
AI 생성 시 실제 장소의 근사 좌표를 함께 반환하도록 프롬프트에 명시.

## 5. 핵심 도메인 규칙 (위반 금지)

### 5.1 일자 단위 부분 재생성
- 재조정 시 AI에는 변경된 day 정보와 새 순서만 전달, "다른 날짜는 원본 그대로"를 프롬프트로 강제
- **응답 검증**: 요청하지 않은 day가 원본과 다르면(깊은 비교) 위반 → 1회 재시도 → 재실패 시 502
- 검증 로직은 `ai/PartialRegenerationValidator`로 분리, **단위 테스트 필수**(프로젝트 유일 필수 테스트)
- 재조정마다 revision +1, `trip_revisions`에 스냅샷 적재

### 5.2 AI 판정 (route_warning)
- 룰 체크(좌표 기반 이동 거리/순서) + AI 자연어 사유 → `route_warning: { flagged, reason }`
- 같은 데이터를 사용자 배지와 관리자 flagged-trips가 공통 소비. DB는 `itinerary_days`의 두 컬럼
- 사용자가 정한 순서는 절대 임의 변경 금지. 이동 시간에 현실만 반영

### 5.3 AI 응답 캐시 (확정 설계 — ERD보다 이 정의가 우선)
- 캐시 키 = SHA-256( 정규화 결합 문자열 ), 정규화:
  destination `trim`+소문자 / 날짜 대신 **duration_days** / budget_min+budget_max(NULL=상한없음, 그대로 정수 결합) /
  companion `trim`+소문자 /
  preferences **정렬 후** `,` 결합 / **include_nearby 포함** / **active_start_time+active_end_time 포함**
- TTL 30일: 조회 시 `created_at` 30일 초과면 미스 처리 후 새 응답 upsert (lazy expiry, 배치 없음)
- 최초 생성에만 적용, 재조정 미적용. 캐시 조회 실패 시 AI 직접 호출 폴백

### 5.4 AI 호출 공통
- 모든 activity의 시작 시간은 `active_start_time`~`active_end_time` 범위 안에서만 배치하도록 프롬프트에 명시(범위 밖 배치 금지)
- 예산은 budget_min~budget_max(NULL이면 "50만원 이상, 상한 없음") 범위를 자연어로 변환해 프롬프트에 포함
- companion 값에 따라 장소 성격을 조정하도록 프롬프트에 명시(예: kid→아이 동반 가능 장소 우선, couple→로맨틱한 장소, friend→그룹 액티비티 등)
- 저비용 모델, `max_tokens`/타임아웃은 duration_days 비례 동적 설정
- 호출 실패/파싱 실패 구분 로깅, 각 1회 재시도
- 프롬프트는 하드코딩하지 않고 `prompt_templates` 테이블에서 로드 (`initial_generation`, `reorder` 2종)

### 5.5-a 로그인/랜딩 분기 (완전 분리형, 확정 — 로그인 경로 분리로 갱신)
- 로그인 화면은 경로로 분리: 일반 사용자 `/login`, 관리자 `/admin/login`
- `/admin/login`은 사용자 화면과 시각적으로 구분(어두운 배경 + "관리자 전용" 배지 + "관리자 로그인" 헤드라인), 회원가입 링크 없음
- 역할 교차 로그인은 에러로 차단(세션 저장 안 함): `/login`에 admin 계정 → "관리자 계정입니다" + /admin/login 링크 안내, `/admin/login`에 user 계정 → "관리자 계정이 아닙니다" + /login 링크 안내
- 로그인 성공 시 `role=user` → "내 여행" 목록, `role=admin` → 관리자 대시보드. 일반 사용자 화면과 관리자 화면은 서로 노출하지 않음(완전 분리)
- 프론트 라우트 가드: `/admin/**`는 role=admin만 접근. 미인증이면 `/admin/login`으로, 사용자 라우트 미인증은 `/login`으로 리다이렉트 (백엔드 403과 별개 이중 방어). 401 인터셉터도 현재 경로 기준으로 두 로그인 페이지에 각각 복귀

### 5.5 목적지 선택 (확정 설계)
- 외부 API 없이 `frontend/src/data/destinations.ts` 정적 데이터로 드릴다운 모달 구현
- 구조: 좌측 탭 [국내 | 해외] → 우측 지역 리스트 → 지역 클릭 시 상세 리스트(국내: 세부 지역 / 해외: 도시 칩) → 선택 시 모달 닫힘
- 상세 진입 시 헤더는 뒤로가기 화살표 + breadcrumb("해외 > 일본")
- 목록에 없는 곳은 목적지 필드에 자유 텍스트 직접 입력 허용 (PRD "제한 없음" 유지)
- **근교 포함 토글**: 모달이 아닌 생성 폼에서, 목적지 확정 후 필드 아래에 등장.
  문구는 실제 지명 반영형("교토·고베도 함께 볼까요?" 식), 스위치 토글 UI. 값은 `include_nearby`로 전송
  (근교 지명 매핑은 destinations.ts의 `nearby` 필드에서 조회, 없으면 "근교 지역도 함께 볼까요?" 폴백)

### 5.6 ERD 델타 (docs/ERD.md에 아래를 추가/수정해 구현)
1. `itinerary_activities`에 `lat double precision NULL`, `lng double precision NULL` 컬럼 추가
2. `trips`에 `include_nearby boolean NOT NULL default false` 컬럼 추가
2-1. `trips`에 `active_start_time time NOT NULL default '09:00'`, `active_end_time time NOT NULL default '21:00'` 컬럼 추가
2-2. `trips.budget_level(text)` 컬럼을 **제거**하고 대신 `budget_min int NOT NULL default 0`,
   `budget_max int NULL`(NULL = "50만원 이상", 상한 없음) 컬럼 추가
2-3. `trips`에 `companion text NOT NULL` 컬럼 추가 (값은 애플리케이션 레벨에서 6종으로 고정:
   parent/friend/solo/couple/kid/etc — 프론트 표시 라벨은 6절 참고)
3. `ai_response_cache.cache_key`의 해시 구성은 ERD 표기(start_date+end_date)가 아니라 §5.3 정의를 따름
4. `ai_response_cache.expires_at`은 사용하지 않음(NULL 유지) — 만료는 §5.3 lazy expiry로 처리

## 6. 화면 목록 및 확정 UI 결정

| 화면 | 필수 상태 |
|---|---|
| 로그인/회원가입 | 브랜드 마크+인사형 헤드라인, 입력 검증 에러, 비밀번호 강도 바, 입력 완료 전 버튼 비활성 톤 |
| 일정 생성 폼 | 목적지 선택(§5.5, 별도 "선택" 버튼 없이 입력창 자체를 탭하면 모달 오픈 + 자유 텍스트 입력도 그대로 가능), 기간 선택 시 "N박 M일" 자동 배지(달력은 박스 전체 클릭으로 오픈, 오늘 이전 날짜 선택 불가), **누구와**(단일 선택 칩: 혼자/연인과/가족과/친구와/아이와/기타), **예산**(듀얼 핸들 range 슬라이더 + 최소/최대 입력 박스, 최대 핸들이 트랙 오른쪽 끝에 완전히 붙으면 "50만원 이상"으로 표기·상한 없음 처리, 히스토그램 막대는 사용 안 함), 취향 칩(**0개 선택 시 카운트 라벨 숨김, 1개 이상일 때만 "N개 선택됨" 표시**), **활동 시간대**(시작/종료 시간 입력창 탭 시 휠 피커 팝업: 오전·오후 / 시 / 분 3열, 분은 00·30분만 선택 가능, 확인 버튼으로 닫힘, 기본값 09:00~21:00), CTA에 목적지 반영("부산 일정 만들기"), 로딩=단계 체크리스트 |
| 일정표 | **상단 Google 지도**(마커 번호=타임라인 순번 동일, Polyline 동선, 비효율 구간은 노란 점선) → Day 탭 → 테마+변경됨 배지 → route_warning 참고 배지 → 타임라인 → 하단 [동선 최적화]+[재조정] 버튼 쌍. 드래그 시 지도 동선 동시 갱신 |
| 저장한 여행 목록 | 카드=목적지 썸네일 블록(목적지 해시 기반 색)+D-day 배지+기간+취향 칩. **revision/수정 횟수 노출 금지**. 빈 상태(아래) |
| 관리자 3화면 | 상단 요약 지표 카드(생성 수/flagged 수·비율/활성 템플릿) → 모니터링 테이블(행 클릭→상세) → 인기 목적지 막대 → 템플릿 카드(활성 버전 배지, 편집/이력) |

**빈 상태(첫 로그인, 일정 0개) — 문구 고정:**
중앙 배치, 캐리어 아이콘 + "일정이 존재하지 않네요.\n일정을 만들까요?" + 보조 문구 "목적지만 정해오세요. 동선은 AI가 짤게요." + 파란 버튼 **[AI 추천 일정 만들기]**

**route_warning 배지 — 참고/제안 톤 고정(경고 아님):**
노란 배경(#FFF7E0) + 전구 아이콘 + "이 순서면 이동 시간이 길어질 수 있어요" + reason 자연어 + 동선 최적화 버튼 연결

## 7. 디자인 시스템 (트리플 레이아웃 벤치마킹, 브랜드 자산 미사용)

`frontend/src/styles/tokens.css`에 CSS 변수로 정의, 컴포넌트에 색상 하드코딩 금지:
```css
--color-primary: #0062F4;      /* 확정 메인 컬러 */
--color-primary-light: #E6F0FE;
--color-primary-bg: #F5F9FF;    /* 드래그 중 카드/토글 배경 */
--color-warning-bg: #FFF7E0;  --color-warning-text: #9A6700;
--color-modified-bg: #E4F4EA; --color-modified-text: #1D7A46;
--color-error: #E24B4A;
--color-text: #191F28; --color-text-sub: #5F6B7A; --color-text-muted: #8B95A1;
--color-bg: #FFFFFF; --color-bg-sub: #F7F8FA; --color-border: #E5E8EB;
--radius-card: 16px; --radius-container: 20px; --radius-button: 12px; --radius-chip: 999px;
--shadow-card: 0 1px 4px rgba(0,0,0,0.06);
```
- 폰트 Pretendard, 제목 500~700 / 본문 400. 간격은 8px 그리드
- 버튼: 단색 파랑+흰 글자, 높이 48~52px, 그라데이션/그림자 없음. 보조 버튼은 파란 테두리 아웃라인
- 타임라인: 좌측 시간+순번 원형 뱃지+세로 연결선, 카드 사이 이동시간 회색 텍스트("🚕 약 25분")
- 모바일 우선(기준 폭 480px 중앙), 데스크톱은 지도 좌측 고정+타임라인 우측 2단 허용
- **모달/팝업 공통 규칙**: 화면 하단 고정이 아니라 뷰포트 기준 수직/수평 중앙 정렬. 내용이 뷰포트보다 길면 모달 자체는 최대 높이(예: 90vh)를 넘지 않고 내부만 스크롤. 화면 크기가 바뀌어도(반응형) 항상 잘리지 않고 중앙 유지

## 8. 코딩 컨벤션

- 프론트 타입은 data-spec과 snake_case 그대로 1:1 (변환 레이어 없음)
- 백엔드 DTO는 record + `@JsonProperty` snake_case 매핑
- 환경변수: 프론트 `VITE_API_BASE_URL`, `VITE_GOOGLE_MAPS_API_KEY` / 백엔드 `SUPABASE_DB_URL`, `JWT_SECRET`, `AI_API_KEY`. 시크릿 하드코딩 금지, `.env.example`만 커밋
- 커밋: 마일스톤당 1개 이상, `feat/fix/chore: 요약`
- 자동화 테스트는 §5.1 검증 로직 외 작성하지 않음

## 9. 작업 방식 (토큰 효율 규칙)

- 새 마일스톤 착수 시 **파일 단위 계획 먼저 제시 → 승인 후 구현**
- 이 파일과 docs/ 내용을 응답에서 반복 설명하지 말 것 — 경로 참조로 대체
- UI 작업은 2단계: ① 더미 데이터 정적 목업 → 확인 → ② API 연결
- 마일스톤 완료 시: 커밋 → 해당 DoD 항목 자체 점검 결과만 짧게 보고 → 종료
