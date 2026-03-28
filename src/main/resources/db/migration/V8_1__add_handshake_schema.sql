CREATE TABLE IF NOT EXISTS handshake (
    id             UUID           NOT NULL PRIMARY KEY,
    listing_id     UUID           NOT NULL REFERENCES listing(id) ON DELETE RESTRICT,
    buyer_id       UUID           NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    seller_id      UUID           NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    agreed_price   NUMERIC(12,2)  NOT NULL,
    status         VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    expires_at     TIMESTAMP,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_handshake_listing ON handshake(listing_id);
CREATE INDEX IF NOT EXISTS idx_handshake_buyer   ON handshake(buyer_id);
CREATE INDEX IF NOT EXISTS idx_handshake_seller  ON handshake(seller_id);
CREATE INDEX IF NOT EXISTS idx_handshake_status  ON handshake(status);

