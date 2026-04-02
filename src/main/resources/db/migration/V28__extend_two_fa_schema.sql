ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS two_fa_method VARCHAR(20),
    ADD COLUMN IF NOT EXISTS two_fa_setup_secret VARCHAR(255),
    ADD COLUMN IF NOT EXISTS two_fa_setup_created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS two_fa_enabled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS recovery_codes TEXT;

