# 데이터 스펙

> 출처: `docs/PRD.md` 8절 원본에 CLAUDE.md 4절 확장분(lat/lng, include_nearby, active_start_time/active_end_time)을 반영.
> 충돌 시 우선순위는 CLAUDE.md 상단 명시(CLAUDE.md > ERD 델타 > ERD.md > PRD.md)를 따른다.

## 1. 최초 생성 입력 (`POST /api/trips`)

- `destination`(텍스트, 제한 없음, 필수)
- `start_date`, `end_date`(YYYY-MM-DD, 필수)
- `budget_min`(정수, 필수), `budget_max`(정수 또는 null=상한 없음) — 예산 범위. CLAUDE.md 5.6-2-2
- `companion`(텍스트, 필수) — `parent`/`friend`/`solo`/`couple`/`kid`/`etc` 6종 중 하나. CLAUDE.md 5.6-2-3
- `preferences`(문자열 배열, 다중 선택, 필수)
- `include_nearby`(boolean, 필수) — 근교 포함 토글. CLAUDE.md 4절/5.5절
- `active_start_time`, `active_end_time`("HH:mm", 생략 시 기본값 "09:00"/"21:00") — 하루 활동 시간대. CLAUDE.md 4절/5.6-2-1

```json
{
  "destination": "부산",
  "start_date": "2026-08-01",
  "end_date": "2026-08-03",
  "budget_min": 100000,
  "budget_max": 300000,
  "companion": "couple",
  "preferences": ["힐링", "먹방"],
  "include_nearby": false,
  "active_start_time": "09:00",
  "active_end_time": "21:00"
}
```

## 2. 재조정 요청 입력 (`PATCH /api/trips/{trip_id}/reorder`)

- `trip_id`(Supabase trips 테이블 PK, 필수, 경로 파라미터)
- `day`(재조정할 일차, 1부터 시작, 필수)
- `new_activity_order`(드래그 후 activity id 배열, 필수)

```json
{
  "day": 2,
  "new_activity_order": ["d2-a3", "d2-a1", "d2-a2"]
}
```

## 3. 출력 데이터 스펙 (JSON)

최초 생성(`POST /api/trips`) 및 재조정(`PATCH /api/trips/{trip_id}/reorder`) 응답 공통 스키마.

```json
{
  "trip_id": "trip_abc123",
  "destination": "부산",
  "duration_days": 3,
  "summary": "부산 2박 3일 힐링+먹방 일정",
  "days": [
    {
      "day": 1,
      "theme": "도착 및 시내 탐방",
      "activities": [
        {
          "id": "d1-a1",
          "time": "09:00",
          "title": "해운대 도착",
          "description": "...",
          "category": "유명 관광지",
          "duration_minutes": 90,
          "location": "해운대구",
          "estimated_cost": 0,
          "tips": null,
          "lat": 35.1587,
          "lng": 129.1604
        }
      ],
      "last_modified": false,
      "route_warning": { "flagged": false, "reason": null }
    }
  ],
  "meta": { "generated_at": "2026-07-10T10:00:00Z", "revision": 2 }
}
```

- `activities[].lat`, `activities[].lng`: 지도 마커/Polyline 렌더링용 좌표(필수). CLAUDE.md 4절 — AI 생성 시 실제 장소의 근사 좌표를 함께 반환하도록 프롬프트에 명시
- `days[].last_modified`: 이번 재조정에서 실제로 재계산된 날짜인지(프론트 "변경됨" 뱃지용)
- `days[].route_warning`: AI 판정 결과 — 사용자 화면 경고 배지 및 관리자 모니터링 목록에 공통으로 사용
- `meta.revision`: 재조정 반영 횟수

## 4. 에러 응답 공통 포맷

```json
{ "error": "VALIDATION_ERROR", "message": "..." }
```

`error` 값과 HTTP 상태 매핑(CLAUDE.md 4절):

| error | status |
|---|---|
| VALIDATION_ERROR | 400 |
| GENERATION_FAILED | 502 |
| AUTH_ERROR | 401 |
| FORBIDDEN | 403 |
| STORAGE_ERROR | 503 |
