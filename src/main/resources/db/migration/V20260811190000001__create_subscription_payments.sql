CREATE TABLE IF NOT EXISTS subscription_payment (
 id uuid PRIMARY KEY, tenant varchar(120) NOT NULL, plan_code varchar(40) NOT NULL,
 billing_cycle varchar(30) NOT NULL, months integer NOT NULL, amount_cents integer NOT NULL,
 status varchar(30) NOT NULL, order_nsu varchar(255), transaction_nsu varchar(255),
 created_at timestamp NOT NULL, paid_at timestamp,
 CONSTRAINT ck_subscription_payment_amount CHECK (amount_cents > 0),
 CONSTRAINT ck_subscription_payment_cycle CHECK (billing_cycle IN ('MONTHLY','QUARTERLY','SEMIANNUAL'))
);
CREATE INDEX IF NOT EXISTS idx_subscription_payment_tenant ON subscription_payment(tenant, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_payment_transaction ON subscription_payment(transaction_nsu) WHERE transaction_nsu IS NOT NULL;

CREATE TABLE IF NOT EXISTS payment_event_inbox (
 id uuid PRIMARY KEY, payment_id uuid NOT NULL UNIQUE, client_id uuid NOT NULL,
 status varchar(30) NOT NULL, failure_reason varchar(1000), received_at timestamp NOT NULL, processed_at timestamp
);
CREATE INDEX IF NOT EXISTS idx_payment_event_inbox_status ON payment_event_inbox(status);
