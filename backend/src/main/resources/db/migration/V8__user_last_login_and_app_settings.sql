-- 관리자 사용자 관리(§5.6 델타): 마지막 로그인 시각 + 앱 설정 테이블(비밀번호 초기화 기본값)

ALTER TABLE users ADD COLUMN last_login_at timestamptz NULL;

CREATE TABLE app_settings (
    key        text PRIMARY KEY,
    value      text NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO app_settings (key, value) VALUES ('default_reset_password', 'planner1!');
