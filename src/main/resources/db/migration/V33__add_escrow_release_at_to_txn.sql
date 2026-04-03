ALTER TABLE txn
    ADD COLUMN IF NOT EXISTS escrow_release_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_txn_status_release_at
    ON txn(status, escrow_release_at);

