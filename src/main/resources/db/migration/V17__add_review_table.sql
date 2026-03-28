CREATE TABLE IF NOT EXISTS review (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES txn(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    reviewee_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_review_txn_reviewer_reviewee UNIQUE (transaction_id, reviewer_id, reviewee_id)
);

CREATE INDEX IF NOT EXISTS idx_review_txn ON review(transaction_id);
CREATE INDEX IF NOT EXISTS idx_review_reviewee ON review(reviewee_id);

