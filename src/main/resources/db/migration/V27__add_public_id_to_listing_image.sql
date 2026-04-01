ALTER TABLE listing_image
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(255);

