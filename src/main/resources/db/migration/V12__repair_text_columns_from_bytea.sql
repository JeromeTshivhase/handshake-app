DO $$
BEGIN
    -- Repair legacy databases where app_user.email was stored as bytea.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'app_user'
          AND column_name = 'email'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE app_user ALTER COLUMN email TYPE VARCHAR(255) USING encode(email, ''escape'')';
    END IF;

    -- Repair legacy listing searchable columns that may have been created as bytea.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'listing'
          AND column_name = 'title'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE listing ALTER COLUMN title TYPE VARCHAR(200) USING encode(title, ''escape'')';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'listing'
          AND column_name = 'description'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE listing ALTER COLUMN description TYPE TEXT USING encode(description, ''escape'')';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'listing'
          AND column_name = 'location'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE listing ALTER COLUMN location TYPE VARCHAR(255) USING encode(location, ''escape'')';
    END IF;
END $$;

