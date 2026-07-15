ALTER TABLE user_access
    ADD COLUMN IF NOT EXISTS phone varchar(30);

ALTER TABLE user_configuration
    ADD COLUMN IF NOT EXISTS phone varchar(30);

ALTER TABLE user_access
    ALTER COLUMN id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname = 'pk_user_access'
           AND conrelid = 'user_access'::regclass
    ) THEN
        ALTER TABLE user_access
            ADD CONSTRAINT pk_user_access PRIMARY KEY (id);
    END IF;
END
$$;
