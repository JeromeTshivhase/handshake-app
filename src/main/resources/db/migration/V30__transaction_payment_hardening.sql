-- Add optimistic-lock version column to handshake
ALTER TABLE handshake
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
-- Partial unique index prevents duplicate active handshakes for the same buyer+listing.
-- Replaces the application-level race-prone check-then-insert pattern.
CREATE UNIQUE INDEX IF NOT EXISTS idx_handshake_unique_active_buyer_listing
    ON handshake(listing_id, buyer_id)
    WHERE status IN ('PENDING', 'ACCEPTED');
-- payment_reference should be unique (one PayFast payment ID per payment row).
-- Use a partial index to ignore NULL references (pre-payment rows).
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_reference_unique
    ON payment(payment_reference)
    WHERE payment_reference IS NOT NULL;
