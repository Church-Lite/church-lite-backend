DO $$
DECLARE
    constraint_record record;
BEGIN
    IF upper(current_schema()) LIKE '%\_ADMIN' ESCAPE '\' THEN
        -- Remove checks textuais legados que impediriam a conversão para enum ordinal.
        FOR constraint_record IN
            SELECT conname
              FROM pg_constraint
             WHERE conrelid = 'billing_discount'::regclass
               AND contype = 'c'
               AND pg_get_constraintdef(oid) ILIKE '%billing_cycle%'
        LOOP
            EXECUTE format(
                'ALTER TABLE billing_discount DROP CONSTRAINT %I',
                constraint_record.conname);
        END LOOP;

        FOR constraint_record IN
            SELECT conname
              FROM pg_constraint
             WHERE conrelid = 'subscription_payment'::regclass
               AND contype = 'c'
               AND (
                   pg_get_constraintdef(oid) ILIKE '%billing_cycle%'
                   OR pg_get_constraintdef(oid) ILIKE '%status%'
               )
        LOOP
            EXECUTE format(
                'ALTER TABLE subscription_payment DROP CONSTRAINT %I',
                constraint_record.conname);
        END LOOP;

        ALTER TABLE billing_discount
            ALTER COLUMN billing_cycle DROP DEFAULT;
        ALTER TABLE subscription_payment
            ALTER COLUMN billing_cycle DROP DEFAULT,
            ALTER COLUMN status DROP DEFAULT;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = current_schema()
               AND table_name = 'billing_discount'
               AND column_name = 'billing_cycle'
               AND data_type IN ('character varying', 'character', 'text')
        ) THEN
            ALTER TABLE billing_discount
                ALTER COLUMN billing_cycle TYPE integer
                USING CASE upper(trim(billing_cycle::text))
                    WHEN 'MONTHLY' THEN 0
                    WHEN 'QUARTERLY' THEN 1
                    WHEN 'SEMIANNUAL' THEN 2
                    ELSE NULLIF(trim(billing_cycle::text), '')::integer
                END;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = current_schema()
               AND table_name = 'subscription_payment'
               AND column_name = 'billing_cycle'
               AND data_type IN ('character varying', 'character', 'text')
        ) THEN
            ALTER TABLE subscription_payment
                ALTER COLUMN billing_cycle TYPE integer
                USING CASE upper(trim(billing_cycle::text))
                    WHEN 'MONTHLY' THEN 0
                    WHEN 'QUARTERLY' THEN 1
                    WHEN 'SEMIANNUAL' THEN 2
                    ELSE NULLIF(trim(billing_cycle::text), '')::integer
                END;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = current_schema()
               AND table_name = 'subscription_payment'
               AND column_name = 'status'
               AND data_type IN ('character varying', 'character', 'text')
        ) THEN
            ALTER TABLE subscription_payment
                ALTER COLUMN status TYPE integer
                USING CASE upper(trim(status::text))
                    WHEN 'PENDING' THEN 0
                    WHEN 'PAID' THEN 1
                    WHEN 'FAILED' THEN 2
                    WHEN 'CANCELLED' THEN 3
                    WHEN 'EXPIRED' THEN 4
                    ELSE NULLIF(trim(status::text), '')::integer
                END;
        END IF;

        CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_discount_cycle_idx
            ON billing_discount (billing_cycle);
    END IF;
END $$;
