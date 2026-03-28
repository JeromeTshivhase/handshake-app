-- Add two_fa_enabled column to app_user table for 2FA support
ALTER TABLE app_user ADD COLUMN two_fa_enabled BOOLEAN DEFAULT FALSE;
