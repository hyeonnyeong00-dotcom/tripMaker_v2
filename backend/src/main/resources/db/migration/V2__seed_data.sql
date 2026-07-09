-- 시드 데이터: admin 계정 1개 + prompt_templates 2행(본문은 placeholder, M4/M6에서 채움)

INSERT INTO users (email, password_hash, role)
VALUES (
    'admin@tripplanner.local',
    '$2y$10$u3o9/AyRo8jY5T3el1AIjObXVfqF4UfqwWBVaxAurjmYvEIpnywui', -- 초기 비밀번호: Admin1234! (최초 로그인 후 변경 권장)
    'admin'
);

INSERT INTO prompt_templates (name, content, version, is_active)
VALUES
    ('initial_generation', 'PLACEHOLDER — M4에서 채움', 1, true),
    ('reorder', 'PLACEHOLDER — M6에서 채움', 1, true);
