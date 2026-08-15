DO $$
BEGIN
    IF upper(current_schema()) LIKE '%\_ADMIN' ESCAPE '\' THEN
        CREATE TABLE IF NOT EXISTS billing_discount (
            id uuid PRIMARY KEY,
            billing_cycle integer NOT NULL,
            months integer NOT NULL CHECK (months > 0),
            discount_percentage numeric(5, 2) NOT NULL CHECK (discount_percentage BETWEEN 0 AND 100),
            active boolean NOT NULL DEFAULT true,
            CONSTRAINT uk_billing_discount_cycle UNIQUE (billing_cycle)
        );
        CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_discount_cycle_idx
            ON billing_discount (billing_cycle);

        CREATE TABLE IF NOT EXISTS subscription_payment (
            id uuid PRIMARY KEY,
            tenant varchar(120) NOT NULL,
            plan_code varchar(40) NOT NULL,
            billing_cycle integer NOT NULL,
            months integer NOT NULL CHECK (months > 0),
            base_amount_cents integer NOT NULL CHECK (base_amount_cents > 0),
            discount_percentage numeric(5, 2) NOT NULL CHECK (discount_percentage BETWEEN 0 AND 100),
            amount_cents integer NOT NULL CHECK (amount_cents > 0),
            status integer NOT NULL,
            order_nsu varchar(255),
            transaction_nsu varchar(255),
            created_at timestamp NOT NULL,
            updated_at timestamp NOT NULL,
            paid_at timestamp,
            coverage_start_at timestamp,
            coverage_end_at timestamp,
            CONSTRAINT uk_subscription_payment_order_nsu UNIQUE (order_nsu),
            CONSTRAINT uk_subscription_payment_transaction_nsu UNIQUE (transaction_nsu)
        );

        -- Compatibilidade com a estrutura de cobrança criada antes desta migration.
        -- CREATE TABLE IF NOT EXISTS não acrescenta colunas a uma tabela já existente.
        ALTER TABLE subscription_payment
            ADD COLUMN IF NOT EXISTS coverage_start_at timestamp,
            ADD COLUMN IF NOT EXISTS coverage_end_at timestamp;

        CREATE INDEX IF NOT EXISTS idx_subscription_payment_tenant_created
            ON subscription_payment (tenant, created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_subscription_payment_tenant_coverage
            ON subscription_payment (tenant, coverage_start_at, coverage_end_at);
        CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_payment_order_nsu_idx
            ON subscription_payment (order_nsu) WHERE order_nsu IS NOT NULL;
        CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_payment_transaction_nsu_idx
            ON subscription_payment (transaction_nsu) WHERE transaction_nsu IS NOT NULL;

        CREATE TABLE IF NOT EXISTS payment_event_inbox (
            id uuid PRIMARY KEY,
            payment_id uuid NOT NULL,
            client_id uuid NOT NULL,
            order_nsu varchar(255) NOT NULL,
            transaction_nsu varchar(255) NOT NULL,
            status varchar(30) NOT NULL,
            failure_reason varchar(1000),
            received_at timestamp NOT NULL,
            processed_at timestamp,
            amount integer NOT NULL CHECK (amount > 0),
            paid_amount integer NOT NULL CHECK (paid_amount > 0),
            paid_at timestamp NOT NULL,
            CONSTRAINT uk_payment_event_inbox_payment_id UNIQUE (payment_id),
            CONSTRAINT uk_payment_event_inbox_transaction_nsu UNIQUE (transaction_nsu)
        );

        -- Alinha qualquer versão anterior da tabela antes de criar chaves e índices.
        -- As colunas permanecem nullable para não invalidar registros legados incompletos;
        -- eventos novos sempre são persistidos completos pela aplicação.
        ALTER TABLE payment_event_inbox
            ADD COLUMN IF NOT EXISTS payment_id uuid,
            ADD COLUMN IF NOT EXISTS client_id uuid,
            ADD COLUMN IF NOT EXISTS order_nsu varchar(255),
            ADD COLUMN IF NOT EXISTS transaction_nsu varchar(255),
            ADD COLUMN IF NOT EXISTS status varchar(30),
            ADD COLUMN IF NOT EXISTS failure_reason varchar(1000),
            ADD COLUMN IF NOT EXISTS received_at timestamp,
            ADD COLUMN IF NOT EXISTS processed_at timestamp,
            ADD COLUMN IF NOT EXISTS amount integer,
            ADD COLUMN IF NOT EXISTS paid_amount integer,
            ADD COLUMN IF NOT EXISTS paid_at timestamp;

        -- Versões iniciais do inbox usavam payment_id como PK e não possuíam id interno.
        ALTER TABLE payment_event_inbox
            ADD COLUMN IF NOT EXISTS id uuid DEFAULT gen_random_uuid();
        UPDATE payment_event_inbox SET id = gen_random_uuid() WHERE id IS NULL;
        ALTER TABLE payment_event_inbox
            ALTER COLUMN id SET NOT NULL,
            ALTER COLUMN id DROP DEFAULT;

        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint
             WHERE conrelid = 'payment_event_inbox'::regclass
               AND contype = 'p'
               AND array_length(conkey, 1) = 1
               AND conkey[1] = (
                   SELECT attnum
                     FROM pg_attribute
                    WHERE attrelid = 'payment_event_inbox'::regclass
                      AND attname = 'id'
               )
        ) THEN
            DECLARE
                current_primary_key text;
            BEGIN
                SELECT conname INTO current_primary_key
                  FROM pg_constraint
                 WHERE conrelid = 'payment_event_inbox'::regclass
                   AND contype = 'p';
                IF current_primary_key IS NOT NULL THEN
                    EXECUTE format(
                        'ALTER TABLE payment_event_inbox DROP CONSTRAINT %I',
                        current_primary_key);
                END IF;
                ALTER TABLE payment_event_inbox
                    ADD CONSTRAINT payment_event_inbox_pkey PRIMARY KEY (id);
            END;
        END IF;

        CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_event_inbox_payment_id_idx
            ON payment_event_inbox (payment_id);
        CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_event_inbox_transaction_nsu_idx
            ON payment_event_inbox (transaction_nsu);

        INSERT INTO billing_discount (id, billing_cycle, months, discount_percentage, active)
        VALUES
            ('40000000-0000-0000-0000-000000000001', 0, 1, 0.00, true),
            ('40000000-0000-0000-0000-000000000002', 1, 3, 10.00, true),
            ('40000000-0000-0000-0000-000000000003', 2, 6, 15.00, true)
        ON CONFLICT (billing_cycle) DO UPDATE SET
            months = EXCLUDED.months,
            discount_percentage = EXCLUDED.discount_percentage,
            active = EXCLUDED.active;
    END IF;
END $$;
