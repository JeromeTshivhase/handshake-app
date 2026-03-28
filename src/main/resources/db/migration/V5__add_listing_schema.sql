CREATE TABLE IF NOT EXISTS category (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    parent_id UUID NULL REFERENCES category(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_category_parent ON category(parent_id);
CREATE INDEX IF NOT EXISTS idx_category_name ON category(name);

CREATE TABLE IF NOT EXISTS listing (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    condition VARCHAR(20) NOT NULL,
    category_id UUID NULL REFERENCES category(id) ON DELETE SET NULL,
    seller_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_listing_seller ON listing(seller_id);
CREATE INDEX IF NOT EXISTS idx_listing_category ON listing(category_id);
CREATE INDEX IF NOT EXISTS idx_listing_status ON listing(status);
CREATE INDEX IF NOT EXISTS idx_listing_price ON listing(price);
CREATE INDEX IF NOT EXISTS idx_listing_created_at ON listing(created_at);

CREATE TABLE IF NOT EXISTS listing_image (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_listing_image_listing ON listing_image(listing_id);

