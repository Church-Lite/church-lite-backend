DO $$
BEGIN
    IF upper(current_schema()) LIKE '%\_ADMIN' ESCAPE '\' THEN
        ALTER TABLE billing_discount
            ADD COLUMN IF NOT EXISTS billing_cycle integer,
            ADD COLUMN IF NOT EXISTS months integer,
            ADD COLUMN IF NOT EXISTS discount_percentage numeric(5, 2),
            ADD COLUMN IF NOT EXISTS active boolean DEFAULT true;

        ALTER TABLE subscription_payment
            ADD COLUMN IF NOT EXISTS tenant varchar(120),
            ADD COLUMN IF NOT EXISTS plan_code varchar(40),
            ADD COLUMN IF NOT EXISTS billing_cycle integer,
            ADD COLUMN IF NOT EXISTS months integer,
            ADD COLUMN IF NOT EXISTS base_amount_cents integer,
            ADD COLUMN IF NOT EXISTS discount_percentage numeric(5, 2),
            ADD COLUMN IF NOT EXISTS amount_cents integer,
            ADD COLUMN IF NOT EXISTS status integer,
            ADD COLUMN IF NOT EXISTS order_nsu varchar(255),
            ADD COLUMN IF NOT EXISTS transaction_nsu varchar(255),
            ADD COLUMN IF NOT EXISTS created_at timestamp,
            ADD COLUMN IF NOT EXISTS updated_at timestamp,
            ADD COLUMN IF NOT EXISTS paid_at timestamp,
            ADD COLUMN IF NOT EXISTS coverage_start_at timestamp,
            ADD COLUMN IF NOT EXISTS coverage_end_at timestamp;

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

        CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_discount_cycle_idx
            ON billing_discount (billing_cycle);
        CREATE INDEX IF NOT EXISTS idx_subscription_payment_tenant_created
            ON subscription_payment (tenant, created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_subscription_payment_tenant_coverage
            ON subscription_payment (tenant, coverage_start_at, coverage_end_at);
        CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_payment_order_nsu_idx
            ON subscription_payment (order_nsu) WHERE order_nsu IS NOT NULL;
        CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_payment_transaction_nsu_idx
            ON subscription_payment (transaction_nsu) WHERE transaction_nsu IS NOT NULL;
        CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_event_inbox_payment_id_idx
            ON payment_event_inbox (payment_id) WHERE payment_id IS NOT NULL;
        CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_event_inbox_transaction_nsu_idx
            ON payment_event_inbox (transaction_nsu) WHERE transaction_nsu IS NOT NULL;

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
