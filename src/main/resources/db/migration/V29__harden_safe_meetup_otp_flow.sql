ALTER TABLE handshake
    ADD COLUMN IF NOT EXISTS otp_reveal_ciphertext VARCHAR(512);
ALTER TABLE meetup
    ADD COLUMN IF NOT EXISTS time_window_minutes INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS radius_meters INTEGER NOT NULL DEFAULT 200,
    ADD COLUMN IF NOT EXISTS otp_code_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS otp_enabled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS otp_failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS otp_locked_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS otp_verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS buyer_last_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS buyer_last_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS buyer_last_checkin TIMESTAMP,
    ADD COLUMN IF NOT EXISTS seller_last_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS seller_last_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS seller_last_checkin TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_meetup_handshake ON meetup(handshake_id);
CREATE INDEX IF NOT EXISTS idx_meetup_status_time ON meetup(status, scheduled_time);
CREATE INDEX IF NOT EXISTS idx_meetup_otp_expiry ON meetup(otp_expires_at);
CREATE INDEX IF NOT EXISTS idx_handshake_otp_locked_until ON handshake(otp_locked_until);
