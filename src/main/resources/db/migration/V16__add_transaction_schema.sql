CREATE TABLE IF NOT EXISTS txn (
    id             UUID           NOT NULL PRIMARY KEY,
    handshake_id   UUID           NOT NULL UNIQUE REFERENCES handshake(id) ON DELETE RESTRICT,
    listing_id     UUID           NOT NULL REFERENCES listing(id) ON DELETE RESTRICT,
    buyer_id       UUID           NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    seller_id      UUID           NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
    amount         NUMERIC(12,2)  NOT NULL,
    currency       VARCHAR(10)    NOT NULL DEFAULT 'ZAR',
    status         VARCHAR(30)    NOT NULL DEFAULT 'INITIATED',
    escrow_held_at TIMESTAMP,
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_txn_handshake ON txn(handshake_id);
CREATE INDEX IF NOT EXISTS idx_txn_listing   ON txn(listing_id);
CREATE INDEX IF NOT EXISTS idx_txn_status    ON txn(status);

CREATE TABLE IF NOT EXISTS payment (
    id                UUID           NOT NULL PRIMARY KEY,
    transaction_id    UUID           NOT NULL REFERENCES txn(id) ON DELETE RESTRICT,
    amount            NUMERIC(12,2)  NOT NULL,
    currency          VARCHAR(10)    NOT NULL DEFAULT 'ZAR',
    provider          VARCHAR(30)    NOT NULL DEFAULT 'PAYFAST',
    status            VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    payment_reference VARCHAR(120),
    raw_payload       TEXT,
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_txn       ON payment(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_status    ON payment(status);
CREATE INDEX IF NOT EXISTS idx_payment_reference ON payment(payment_reference);
