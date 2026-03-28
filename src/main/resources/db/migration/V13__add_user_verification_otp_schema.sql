ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS verification_otp_challenge (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    purpose VARCHAR(20) NOT NULL,
    target_value VARCHAR(255) NOT NULL,
    otp_code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    last_sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verification_otp_user_purpose
    ON verification_otp_challenge(user_id, purpose);

CREATE INDEX IF NOT EXISTS idx_verification_otp_expires_at
    ON verification_otp_challenge(expires_at);

