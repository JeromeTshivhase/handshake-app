DO $$
BEGIN
    -- Repair legacy databases where category.name was created as bytea.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'category'
          AND column_name = 'name'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE category ALTER COLUMN name TYPE VARCHAR(120) USING encode(name, ''escape'')';
    END IF;

    -- Defensive: repair parent_id if ever stored as bytea in broken schemas.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'category'
          AND column_name = 'parent_id'
          AND data_type = 'bytea'
    ) THEN
        EXECUTE 'ALTER TABLE category ALTER COLUMN parent_id TYPE UUID USING (encode(parent_id, ''escape'')::uuid)';
    END IF;
END $$;

