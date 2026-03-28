-- Add two_fa_secret column to app_user table for 2FA TOTP secret storage
ALTER TABLE app_user ADD COLUMN two_fa_secret VARCHAR(255);
