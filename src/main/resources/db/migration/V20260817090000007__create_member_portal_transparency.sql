CREATE TABLE IF NOT EXISTS member_portal_transparency_configuration (
    id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
    mode VARCHAR(20) NOT NULL DEFAULT 'DISABLED',
    CONSTRAINT ck_member_portal_transparency_mode CHECK (mode IN ('DISABLED', 'FULL', 'PARTIAL'))
);

INSERT INTO member_portal_transparency_configuration (id, mode)
VALUES (TRUE, 'DISABLED')
ON CONFLICT (id) DO NOTHING;

DO $$
BEGIN
    IF to_regclass('plan_account') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS member_portal_plan_account_visibility (
            plan_account_id UUID PRIMARY KEY REFERENCES plan_account(id) ON DELETE CASCADE,
            visibility VARCHAR(20) NOT NULL DEFAULT 'HIDDEN',
            CONSTRAINT ck_member_portal_plan_visibility CHECK (visibility IN ('HIDDEN', 'TOTAL_ONLY', 'DETAILED'))
        );
    END IF;
END $$;
