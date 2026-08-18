DO $$
BEGIN
    IF to_regclass('user_access') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS user_access_profile (
            user_access_id UUID NOT NULL REFERENCES user_access(id) ON DELETE CASCADE,
            profile VARCHAR(20) NOT NULL,
            CONSTRAINT pk_user_access_profile PRIMARY KEY (user_access_id, profile),
            CONSTRAINT ck_user_access_profile CHECK (profile IN ('MEMBER', 'STAFF'))
        );

        INSERT INTO user_access_profile (user_access_id, profile)
        SELECT id, 'STAFF' FROM user_access
        ON CONFLICT DO NOTHING;

        CREATE TABLE IF NOT EXISTS member_portal_church_link (
            id UUID PRIMARY KEY,
            tenant VARCHAR NOT NULL UNIQUE,
            active BOOLEAN NOT NULL DEFAULT TRUE
        );
    END IF;

    IF to_regclass('person_member') IS NOT NULL THEN
        ALTER TABLE person_member ADD COLUMN IF NOT EXISTS access_user_hash UUID;
        CREATE UNIQUE INDEX IF NOT EXISTS uk_person_member_access_user_hash
            ON person_member (access_user_hash) WHERE access_user_hash IS NOT NULL;
    END IF;
END $$;
