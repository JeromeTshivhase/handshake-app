CREATE TABLE IF NOT EXISTS user_profile (
    user_id      UUID         NOT NULL PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    bio          TEXT,
    avatar_url   VARCHAR(500),
    phone        VARCHAR(30),
    location     VARCHAR(150),
    website_url  VARCHAR(500),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id              UUID         NOT NULL PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    email_notifications  BOOLEAN      NOT NULL DEFAULT TRUE,
    push_notifications   BOOLEAN      NOT NULL DEFAULT TRUE,
    theme                VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM',
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id          UUID          NOT NULL PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token       VARCHAR(1000) NOT NULL UNIQUE,
    expires_at  TIMESTAMP     NOT NULL,
    revoked     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token(user_id);

