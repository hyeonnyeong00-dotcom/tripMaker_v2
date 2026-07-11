-- CLAUDE.md 5.6-2-1/2-2/2-3 반영: 활동 시간대, 예산 범위, 동반자 컬럼 추가 및 budget_level 제거.
-- 기존 trips 행은 포트폴리오 테스트 데이터로 간주해 budget_level 값은 버리고 기본값으로 채운다.

ALTER TABLE trips
    ADD COLUMN active_start_time time NOT NULL DEFAULT '09:00',
    ADD COLUMN active_end_time   time NOT NULL DEFAULT '21:00',
    ADD COLUMN budget_min        int NOT NULL DEFAULT 0,
    ADD COLUMN budget_max        int NULL,
    ADD COLUMN companion         text NOT NULL DEFAULT 'etc'
        CHECK (companion IN ('parent', 'friend', 'solo', 'couple', 'kid', 'etc'));

ALTER TABLE trips
    DROP COLUMN budget_level;
