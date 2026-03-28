CREATE TABLE IF NOT EXISTS meetup (
    id UUID PRIMARY KEY,
    handshake_id UUID NOT NULL UNIQUE REFERENCES handshake(id) ON DELETE CASCADE,
    safe_location_id UUID,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address_label VARCHAR(255),
    scheduled_time TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_meetup_scheduled_time ON meetup(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_meetup_status ON meetup(status);

