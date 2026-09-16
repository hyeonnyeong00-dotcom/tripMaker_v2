# 에러 코드 카탈로그

응답 계약은 `{ "error": "...", "code": "ERR_xxx", "message": "..." }` 형태다.

- `error`(기존 계약, **변경 금지**): 프론트 분기·HTTP 상태 매핑용 대분류 — `VALIDATION_ERROR|GENERATION_FAILED|AUTH_ERROR|FORBIDDEN|STORAGE_ERROR|INTERNAL_ERROR`
- `code`: 같은 상황이라도 원인을 구분하는 세분화 식별자 — 아래 표. 상수 정의는 `common/ErrorCodes.java`.

일부 `code`는 클라이언트로 노출되지 않고 **로그에만** 남는다(내부 예외를 상위에서 사용자 대면 코드로 감싸는 경우, 또는 폴백/사용량 기록). HTTP 열의 괄호 표기가 그 경우다.

## 대역

| 대역 | 용도 |
|---|---|
| 000 | 시스템(미분류 내부 오류) |
| 001~019 | 인증 / 세션 / 권한 |
| 020~039 | 입력 검증 |
| 040~059 | AI 생성 |
| 060~079 | 저장 / DB |
| 080~ | 관리자 기능 |

## 전체 코드

| 코드 | 이름 | 설명 | HTTP (error) |
|---|---|---|---|
| ERR_000 | 미분류 내부 오류 | 어떤 핸들러에도 매핑되지 않은 예상 밖 예외(최종 안전망) | 500 INTERNAL_ERROR |
| ERR_001 | 세션 만료·토큰 무효 | 보호 자원에 미인증/만료·서명 오류 토큰으로 접근 | 401 AUTH_ERROR |
| ERR_002 | 로그인 실패 | 이메일 또는 비밀번호 불일치 | 401 AUTH_ERROR |
| ERR_003 | 사용자 조회 실패 | 인증은 됐으나 해당 사용자 레코드 없음 | 401 AUTH_ERROR |
| ERR_004 | 현재 비밀번호 불일치 | 비밀번호 변경 시(강제 로그아웃 방지 위해 의도적 400) | 400 VALIDATION_ERROR |
| ERR_005 | 접근 권한 없음 | role 미달로 관리자 자원 접근 거부 | 403 FORBIDDEN |
| ERR_006 | 리소스 소유자 아님 | 타인 소유 여행에 접근 | 403 FORBIDDEN |
| ERR_007 | 대상 여행 없음 | 존재하지 않는 tripId(존재 은닉 위해 403) | 403 FORBIDDEN |
| ERR_020 | 요청 값 검증 실패 | Bean Validation 일반 위반 | 400 VALIDATION_ERROR |
| ERR_021 | 요청 본문 파싱 불가 | 깨진/읽을 수 없는 JSON 본문 | 400 VALIDATION_ERROR |
| ERR_022 | 경로/파라미터 타입 오류 | UUID 등 타입 변환 실패 | 400 VALIDATION_ERROR |
| ERR_023 | 이메일 중복 | 회원가입 시 이미 사용 중인 이메일 | 400 VALIDATION_ERROR |
| ERR_024 | 여행 기간 30일 초과 | duration_days > 30 | 400 VALIDATION_ERROR |
| ERR_025 | 종료일이 시작일 이전 | end_date < start_date | 400 VALIDATION_ERROR |
| ERR_026 | 예산 최소>최대 | budget_max < budget_min | 400 VALIDATION_ERROR |
| ERR_027 | 활동 시작≥종료 | active_start_time >= active_end_time | 400 VALIDATION_ERROR |
| ERR_028 | day 범위 벗어남 | 재조정 대상 day가 해당 여행에 없음 | 400 VALIDATION_ERROR |
| ERR_029 | 활동 순서 불일치 | new_activity_order가 해당 day 활동 집합과 다름 | 400 VALIDATION_ERROR |
| ERR_040 | AI 호출 실패 | 네트워크/타임아웃/비정상 상태코드/응답 파싱 불가 | (내부 → 502) |
| ERR_041 | AI 응답 파싱/검증 실패 | JSON 파싱 또는 스키마(day 수/필수값) 위반 | (내부 → 502) |
| ERR_042 | 부분 재생성 검증 위반 | 요청 안 한 day 변경 또는 사용자 지정 순서 임의 변경 | (내부 → 502) |
| ERR_043 | 초기 일정 생성 실패 | 위 AI 실패를 사용자에게 노출하는 래퍼 | 502 GENERATION_FAILED |
| ERR_044 | 재조정 실패 | 위 AI 실패를 사용자에게 노출하는 래퍼 | 502 GENERATION_FAILED |
| ERR_045 | 활성 프롬프트 템플릿 없음 | 시드/배포 오류(initial_generation·reorder 미존재) | 500 INTERNAL_ERROR |
| ERR_060 | DB 접근 실패 | 연결 끊김/제약 위반 등 DataAccessException | 503 STORAGE_ERROR |
| ERR_061 | 여행 이력 저장 실패 | trip_revisions 스냅샷 적재 실패 | 503 STORAGE_ERROR |
| ERR_062 | 앱 설정 없음 | 비밀번호 초기화 기본값(default_reset_password) 미존재 | 503 STORAGE_ERROR |
| ERR_063 | 캐시 조회 실패(폴백) | 예외를 삼키고 AI 직접 호출로 폴백, WARN 로그만 | (미노출) |
| ERR_064 | 캐시 저장 실패 | upsert 실패, 기능엔 영향 없이 WARN 로그만 | (미노출) |
| ERR_080 | 본인 역할 변경 불가 | 관리자가 자기 역할을 변경 시도 | 400 VALIDATION_ERROR |
| ERR_081 | 마지막 admin 강등 불가 | 남은 admin이 1명일 때 역할 변경 차단 | 400 VALIDATION_ERROR |
| ERR_082 | 본인 계정 삭제 불가 | 관리자가 자기 계정을 삭제 시도 | 400 VALIDATION_ERROR |
| ERR_083 | 마지막 admin 삭제 불가 | 남은 admin이 1명일 때 삭제 차단 | 400 VALIDATION_ERROR |
| ERR_084 | 존재하지 않는 사용자 | 관리자 대상 사용자 id 없음 | 400 VALIDATION_ERROR |
| ERR_085 | 존재하지 않는 템플릿 | 관리자 대상 prompt_template id 없음 | 400 VALIDATION_ERROR |
| ERR_086 | 존재하지 않는 템플릿 버전 | prompt_template_revisions에 해당 version 스냅샷 없음(조회/롤백) | 400 VALIDATION_ERROR |

## 로그 파일

- `backend/logs/error.log` — WARN 이상. 각 줄에 `[ERR_xxx]` 코드 포함. 일 롤링·14일 보관.
- `backend/logs/ai-usage.log` — AI 호출/캐시 히트마다 한 줄. 일 롤링·14일 보관.
  - 형식: `<시각> | trip_id=<uuid|-> | template=<name> v<version> | cache_hit=<bool> | in_tokens=<n> out_tokens=<n> | elapsed_ms=<n> | success=<bool>[ ERR_xxx]`
  - 초기 생성은 AI 호출이 저장보다 앞서므로 `trip_id=-`, 재조정은 실제 trip_id 기록.
  - **보안:** 프롬프트 본문/API 키/비밀번호는 어떤 로그에도 남기지 않는다(템플릿은 이름+버전만).
