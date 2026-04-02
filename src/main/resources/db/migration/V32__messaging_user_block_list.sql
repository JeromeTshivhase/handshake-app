CREATE TABLE IF NOT EXISTS user_block (
    id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_block_blocker_blocked UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_user_block_not_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_user_block_blocker ON user_block(blocker_id);
CREATE INDEX IF NOT EXISTS idx_user_block_blocked ON user_block(blocked_id);

