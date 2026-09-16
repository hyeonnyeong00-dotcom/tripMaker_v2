-- 운영 고도화(CLAUDE.md 5.6-a + 5.6 델타 0-2/0-3/0-4)
-- 파일 로그(logs/*.log)는 그대로 두고, 대시보드 집계용으로 동일 정보를 DB에도 적재한다(파일 파싱 금지).

-- 0-2. ai_call_log — AI 호출 1건당 1행. 캐시 히트도 1건으로 기록(tokens=0, cost=0)해 히트율 계산에 쓴다.
CREATE TABLE ai_call_log (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id          uuid NULL REFERENCES trips (id) ON DELETE SET NULL,
    template_name    text,
    template_version int,
    cache_hit        boolean NOT NULL DEFAULT false,
    input_tokens     int NOT NULL DEFAULT 0,
    output_tokens    int NOT NULL DEFAULT 0,
    cost_estimate    numeric(10, 4) NOT NULL DEFAULT 0,
    duration_ms      int NOT NULL DEFAULT 0,
    success          boolean NOT NULL DEFAULT false,
    error_code       text NULL,
    created_at       timestamptz NOT NULL DEFAULT now()
);

-- 일별 추이(ai-usage/daily)와 요약(summary)이 기간으로 자르므로 시간 인덱스를 둔다.
CREATE INDEX idx_ai_call_log_created_at ON ai_call_log (created_at DESC);

-- 0-3. error_log — 전역 예외 처리기의 응답 생성 지점 한 곳에서만 insert한다.
CREATE TABLE error_log (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    error_code     text NOT NULL,
    error_category text NOT NULL,
    message        text,
    path           text,
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_error_log_created_at ON error_log (created_at DESC);
CREATE INDEX idx_error_log_error_code ON error_log (error_code);

-- 0-4. prompt_template_revisions — 템플릿 덮어쓰기 직전의 내용을 스냅샷으로 남긴다.
CREATE TABLE prompt_template_revisions (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id uuid NOT NULL REFERENCES prompt_templates (id) ON DELETE CASCADE,
    version     int NOT NULL,
    content     text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (template_id, version)
);

CREATE INDEX idx_prompt_template_revisions_template ON prompt_template_revisions (template_id, version DESC);
