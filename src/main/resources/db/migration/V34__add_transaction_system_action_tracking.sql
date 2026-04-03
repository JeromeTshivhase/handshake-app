ALTER TABLE txn
    ADD COLUMN IF NOT EXISTS last_system_action VARCHAR(30),
    ADD COLUMN IF NOT EXISTS last_system_action_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_txn_last_system_action
    ON txn(last_system_action);

