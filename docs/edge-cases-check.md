# 엣지케이스 점검 결과 (PRD §10)

점검일: 2026-07-15 · 방법: 로컬 백엔드(실 Supabase + 실 Anthropic) 기동 후 실 API 호출 / 일부는 코드 근거.
런타임 검증에 사용한 테스트 데이터는 점검 후 삭제함.

| # | PRD §10 항목 | 기대 동작 | 점검 방법 | 결과 |
|---|---|---|---|---|
| 1 | AI 호출/파싱 실패 구분 재시도 + 동적 max_tokens/타임아웃 | 각 1회 재시도, duration_days 비례 예산 | 코드 근거 | ✅ `ItineraryGenerationService`/`ReorderGenerationService` `MAX_ATTEMPTS=2`, `AiCallException`(ERR_040)/`AiParseException`(ERR_041) 분기 재시도, `BASE_MAX_TOKENS+TOKENS_PER_DAY*days`·타임아웃 동적 산정 |
| 2 | 재조정 day 범위 밖 | 400 VALIDATION_ERROR | `PATCH /reorder {day:99}` | ✅ `HTTP 400 {"error":"VALIDATION_ERROR","code":"ERR_028"}` |
| 3 | 재조정 결과에서 요청 안 한 날짜 변경 | 원본과 깊은 비교, 다르면 재시도 후 502 | 코드 + 단위 테스트 | ✅ `PartialRegenerationValidator`(ERR_042) + `PartialRegenerationValidatorTest`(필수 테스트 통과). 런타임 재조정에서 요청 안 한 day2가 원본과 **완전 동일**함(t0==재조정후) 확인 |
| 4 | Supabase 연결 실패 | 503 STORAGE_ERROR, AI 실패와 별도 로깅 | 코드 근거 | ✅ `GlobalExceptionHandler.handleDataAccess` → `STORAGE_ERROR(503)`/ERR_060, `error.log`에 `[ERR_060]` 별도 기록 |
| 5 | AI 응답 캐시 조회 실패 | 캐시 건너뛰고 AI 직접 호출 폴백 | 코드 근거 | ✅ `AiResponseCacheService.lookup` try/catch → `Optional.empty()` 반환(ERR_063, WARN 로그만), 서비스 중단 없음 |
| 6 | 미인증 사용자 저장 시도 | 401 AUTH_ERROR | `POST /api/trips` (토큰 없음) | ✅ `HTTP 401 {"error":"AUTH_ERROR","code":"ERR_001"}` |
| 7 | 일반 사용자 관리자 API 접근 | 403 FORBIDDEN | `GET /api/admin/flagged-trips` (user 토큰) | ✅ `HTTP 403 {"error":"FORBIDDEN","code":"ERR_005"}` |
| 8 | 동시 편집 충돌 | 마지막 저장이 덮어쓰기(락 없음, 비목표) | 설계/코드 근거 | ✅ 재조정은 대상 day를 delete-후-insert로 last-write-wins. 잠금 없음(PRD 명시 비목표) |

## 런타임 근거 요약(항목 2·3·6·7)
```
미인증 저장:        POST /api/trips (no token)        → 401 ERR_001
일반→관리자 API:    GET /api/admin/flagged-trips(user) → 403 ERR_005
잘못된 day 재조정:  PATCH /reorder {day:99}            → 400 ERR_028
부분 재생성 불변성: 재조정1 후 요청 안 한 day2 활동 == 생성 시점(t0) : True
```
