ALTER TABLE handshake
    ADD COLUMN IF NOT EXISTS otp_code_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS otp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS otp_verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS otp_failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS otp_locked_until TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_handshake_otp_enabled ON handshake(otp_enabled);
CREATE INDEX IF NOT EXISTS idx_handshake_otp_verified ON handshake(otp_verified);

