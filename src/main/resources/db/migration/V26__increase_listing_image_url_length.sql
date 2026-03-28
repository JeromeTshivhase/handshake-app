-- Migration: Increase listing_image.url column length to 2000, only if not already set
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'listing_image'
          AND column_name = 'url'
          AND (
            data_type = 'character varying'
            AND (character_maximum_length IS NULL OR character_maximum_length < 2000)
          )
    ) THEN
        EXECUTE 'ALTER TABLE listing_image ALTER COLUMN url TYPE VARCHAR(2000)';
    END IF;
END $$;
