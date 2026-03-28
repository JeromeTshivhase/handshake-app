DO $$
BEGIN
    -- Repair listing.title
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'listing'
          AND column_name = 'title'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE listing ALTER COLUMN title TYPE VARCHAR(200) USING convert_from(title, ''UTF8'')';
    END IF;

    -- Repair listing.description
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'listing'
          AND column_name = 'description'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE listing ALTER COLUMN description TYPE TEXT USING convert_from(description, ''UTF8'')';
    END IF;

    -- Repair listing.location
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'listing'
          AND column_name = 'location'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE listing ALTER COLUMN location TYPE VARCHAR(255) USING convert_from(location, ''UTF8'')';
    END IF;

    -- Repair category.name
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'category'
          AND column_name = 'name'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE category ALTER COLUMN name TYPE VARCHAR(120) USING convert_from(name, ''UTF8'')';
    END IF;
END $$;

