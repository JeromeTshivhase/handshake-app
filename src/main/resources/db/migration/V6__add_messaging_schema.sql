CREATE TABLE IF NOT EXISTS conversation (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listing(id) ON DELETE CASCADE,
    buyer_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_message_preview TEXT,
    last_message_at TIMESTAMP,
    unread_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_conversation_listing_buyer_seller UNIQUE (listing_id, buyer_id, seller_id)
);

CREATE INDEX IF NOT EXISTS idx_convo_listing ON conversation(listing_id);
CREATE INDEX IF NOT EXISTS idx_convo_buyer ON conversation(buyer_id);
CREATE INDEX IF NOT EXISTS idx_convo_seller ON conversation(seller_id);
CREATE INDEX IF NOT EXISTS idx_convo_last_message_at ON conversation(last_message_at);

CREATE TABLE IF NOT EXISTS message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content TEXT NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_msg_convo ON message(conversation_id);
CREATE INDEX IF NOT EXISTS idx_msg_created_at ON message(created_at);

