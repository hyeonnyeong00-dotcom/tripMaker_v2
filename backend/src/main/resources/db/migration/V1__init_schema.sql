-- AI 여행 일정 플래너 — 초기 스키마
-- 기반: docs/ERD.md 3절 + CLAUDE.md 5.6 ERD 델타

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 3.1 users
CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    role          text NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- 3.2 trips (+ CLAUDE.md 5.6-2: include_nearby)
CREATE TABLE trips (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    destination     text NOT NULL,
    start_date      date NOT NULL,
    end_date        date NOT NULL,
    duration_days   int NOT NULL,
    budget_level    varchar(30) NOT NULL,
    preferences     text[] NOT NULL,
    summary         text,
    include_nearby  boolean NOT NULL DEFAULT false,
    revision        int NOT NULL DEFAULT 1,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

-- 3.3 itinerary_days
CREATE TABLE itinerary_days (
    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id                uuid NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    day_number             int NOT NULL,
    theme                  text,
    route_warning_flagged  boolean NOT NULL DEFAULT false,
    route_warning_reason   text,
    last_modified_at       timestamptz,
    UNIQUE (trip_id, day_number)
);

-- 3.4 itinerary_activities (+ CLAUDE.md 5.6-1: lat, lng)
CREATE TABLE itinerary_activities (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_day_id   uuid NOT NULL REFERENCES itinerary_days (id) ON DELETE CASCADE,
    activity_key       text NOT NULL,
    order_index        int NOT NULL,
    time               time,
    title              text NOT NULL,
    description        text,
    category           text NOT NULL,
    duration_minutes   int,
    location           text,
    estimated_cost     int,
    tips               text,
    lat                double precision,
    lng                double precision,
    UNIQUE (itinerary_day_id, order_index)
);

-- 3.5 trip_revisions
CREATE TABLE trip_revisions (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id              uuid NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    revision_number      int NOT NULL,
    changed_day_number   int,
    days_snapshot        jsonb NOT NULL,
    created_at           timestamptz NOT NULL DEFAULT now()
);

-- 3.6 prompt_templates
CREATE TABLE prompt_templates (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name        text NOT NULL UNIQUE,
    content     text NOT NULL,
    version     int NOT NULL DEFAULT 1,
    is_active   boolean NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

-- 3.7 ai_response_cache (CLAUDE.md 5.6-4: expires_at 미사용, lazy expiry는 created_at 기준으로 조회 시 판정)
CREATE TABLE ai_response_cache (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cache_key       text NOT NULL UNIQUE,
    request_params  jsonb NOT NULL,
    response_json   jsonb NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    expires_at      timestamptz
);

-- 5. 인덱스
CREATE INDEX idx_trips_user_id ON trips (user_id);
CREATE INDEX idx_trips_destination ON trips (destination);
CREATE INDEX idx_itinerary_days_route_warning_flagged ON itinerary_days (route_warning_flagged);
