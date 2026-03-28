CREATE TABLE IF NOT EXISTS favorite (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_favorite_user_listing UNIQUE (user_id, listing_id)
);

CREATE INDEX IF NOT EXISTS idx_fav_user ON favorite(user_id);
CREATE INDEX IF NOT EXISTS idx_fav_listing ON favorite(listing_id);

CREATE TABLE IF NOT EXISTS report (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    target_type VARCHAR(30) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(120) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    admin_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_report_status ON report(status);
CREATE INDEX IF NOT EXISTS idx_report_target ON report(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_report_reporter ON report(reporter_id);
