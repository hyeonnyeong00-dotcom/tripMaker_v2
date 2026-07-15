package com.tripplanner.common;

/**
 * 응답 계약의 {@code code} 필드에 실리는 세분화된 에러 코드 카탈로그.
 * <p>기존 {@link ErrorCode} enum(= {@code error} 필드 + HTTP 상태 매핑)은 그대로 두고, 이 코드는
 * 같은 상황이라도 어떤 원인인지 구분하기 위한 추가 식별자다. 전체 표는 {@code docs/error-codes.md} 참고.
 * <p>대역: 000 시스템 / 001~019 인증·세션·권한 / 020~039 입력 검증 / 040~059 AI 생성 /
 * 060~079 저장·DB / 080~ 관리자 기능.
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    // ERR_000: 미분류 내부 오류 — 어떤 핸들러에도 매핑되지 않은 예상 밖 예외(최종 안전망)
    public static final String INTERNAL = "ERR_000";

    // ── 001~019 인증 / 세션 / 권한 ──────────────────────────────────────────────
    // ERR_001: 세션 만료·토큰 무효 — 보호 자원에 미인증/만료·서명 오류 토큰으로 접근
    public static final String SESSION_EXPIRED = "ERR_001";
    // ERR_002: 로그인 실패 — 이메일 또는 비밀번호 불일치
    public static final String LOGIN_FAILED = "ERR_002";
    // ERR_003: 사용자 조회 실패 — 인증은 됐으나 해당 사용자 레코드가 없음
    public static final String USER_NOT_FOUND = "ERR_003";
    // ERR_004: 현재 비밀번호 불일치 — 비밀번호 변경 시(강제 로그아웃 방지 위해 의도적으로 400)
    public static final String CURRENT_PASSWORD_MISMATCH = "ERR_004";
    // ERR_005: 접근 권한 없음 — role 미달로 관리자 자원 접근 거부(403)
    public static final String FORBIDDEN_ROLE = "ERR_005";
    // ERR_006: 리소스 소유자 아님 — 타인 소유 여행에 접근
    public static final String NOT_RESOURCE_OWNER = "ERR_006";
    // ERR_007: 대상 여행 없음 — 존재하지 않는 tripId(존재 은닉 위해 403으로 응답)
    public static final String TRIP_NOT_FOUND = "ERR_007";

    // ── 020~039 입력 검증 ──────────────────────────────────────────────────────
    // ERR_020: 요청 값 검증 실패 — Bean Validation 일반 위반
    public static final String VALIDATION_GENERIC = "ERR_020";
    // ERR_021: 요청 본문 파싱 불가 — 깨진/읽을 수 없는 JSON 본문
    public static final String BODY_UNREADABLE = "ERR_021";
    // ERR_022: 경로/파라미터 타입 오류 — UUID 등 타입 변환 실패
    public static final String PARAM_TYPE_MISMATCH = "ERR_022";
    // ERR_023: 이메일 중복 — 회원가입 시 이미 사용 중인 이메일
    public static final String EMAIL_DUPLICATED = "ERR_023";
    // ERR_024: 여행 기간 30일 초과 — duration_days > 30
    public static final String TRIP_DURATION_EXCEEDED = "ERR_024";
    // ERR_025: 종료일이 시작일 이전 — end_date < start_date
    public static final String TRIP_DATE_INVERTED = "ERR_025";
    // ERR_026: 예산 최소>최대 — budget_max < budget_min
    public static final String BUDGET_RANGE_INVERTED = "ERR_026";
    // ERR_027: 활동 시작≥종료 — active_start_time >= active_end_time
    public static final String ACTIVE_TIME_INVERTED = "ERR_027";
    // ERR_028: day 범위 벗어남 — 재조정 대상 day가 해당 여행에 없음
    public static final String DAY_OUT_OF_RANGE = "ERR_028";
    // ERR_029: 활동 순서 불일치 — new_activity_order가 해당 day의 활동 집합과 다름
    public static final String ACTIVITY_ORDER_MISMATCH = "ERR_029";

    // ── 040~059 AI 생성 ────────────────────────────────────────────────────────
    // ERR_040: AI 호출 실패 — 네트워크/타임아웃/비정상 상태코드/응답 파싱 불가
    public static final String AI_CALL_FAILED = "ERR_040";
    // ERR_041: AI 응답 파싱/검증 실패 — JSON 파싱 또는 스키마(day 수/필수값) 위반
    public static final String AI_PARSE_FAILED = "ERR_041";
    // ERR_042: 부분 재생성 검증 위반 — 요청 안 한 day 변경 또는 사용자 지정 순서 임의 변경
    public static final String PARTIAL_REGEN_VIOLATION = "ERR_042";
    // ERR_043: 초기 일정 생성 실패 — 위 AI 실패를 사용자에게 노출하는 래퍼(502)
    public static final String ITINERARY_GENERATION_FAILED = "ERR_043";
    // ERR_044: 재조정 실패 — 위 AI 실패를 사용자에게 노출하는 래퍼(502)
    public static final String REORDER_FAILED = "ERR_044";
    // ERR_045: 활성 프롬프트 템플릿 없음 — 시드/배포 오류(initial_generation·reorder 미존재)
    public static final String PROMPT_TEMPLATE_MISSING = "ERR_045";

    // ── 060~079 저장 / DB ──────────────────────────────────────────────────────
    // ERR_060: DB 접근 실패 — 연결 끊김/제약 위반 등 DataAccessException
    public static final String DB_ACCESS_FAILED = "ERR_060";
    // ERR_061: 여행 이력 저장 실패 — trip_revisions 스냅샷 적재 실패
    public static final String REVISION_SAVE_FAILED = "ERR_061";
    // ERR_062: 앱 설정 없음 — 비밀번호 초기화 기본값(default_reset_password) 미존재
    public static final String APP_SETTING_MISSING = "ERR_062";
    // ERR_063: 캐시 조회 실패(폴백) — 예외를 삼키고 AI 직접 호출로 폴백, WARN 로그만
    public static final String CACHE_LOOKUP_FAILED = "ERR_063";
    // ERR_064: 캐시 저장 실패 — upsert 실패, 기능엔 영향 없이 WARN 로그만
    public static final String CACHE_UPSERT_FAILED = "ERR_064";

    // ── 080~ 관리자 기능 ───────────────────────────────────────────────────────
    // ERR_080: 본인 역할 변경 불가 — 관리자가 자기 역할을 변경 시도
    public static final String ADMIN_SELF_ROLE_CHANGE = "ERR_080";
    // ERR_081: 마지막 admin 강등 불가 — 남은 admin이 1명일 때 역할 변경 차단
    public static final String ADMIN_LAST_ROLE_CHANGE = "ERR_081";
    // ERR_082: 본인 계정 삭제 불가 — 관리자가 자기 계정을 삭제 시도
    public static final String ADMIN_SELF_DELETE = "ERR_082";
    // ERR_083: 마지막 admin 삭제 불가 — 남은 admin이 1명일 때 삭제 차단
    public static final String ADMIN_LAST_DELETE = "ERR_083";
    // ERR_084: 존재하지 않는 사용자 — 관리자 대상 사용자 id 없음
    public static final String ADMIN_USER_NOT_FOUND = "ERR_084";
    // ERR_085: 존재하지 않는 템플릿 — 관리자 대상 prompt_template id 없음
    public static final String ADMIN_TEMPLATE_NOT_FOUND = "ERR_085";
}
