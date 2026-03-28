-- Add payload column to message table for system/handshake messages
ALTER TABLE message ADD COLUMN IF NOT EXISTS payload TEXT;
