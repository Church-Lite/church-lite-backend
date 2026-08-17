DO $$
BEGIN
    IF to_regclass('user_access') IS NOT NULL THEN
        ALTER TABLE user_access ADD COLUMN IF NOT EXISTS cpf VARCHAR(11);
        CREATE INDEX IF NOT EXISTS idx_user_access_cpf ON user_access (cpf);
    END IF;
END $$;
