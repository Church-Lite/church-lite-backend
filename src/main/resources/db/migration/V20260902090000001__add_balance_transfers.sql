ALTER TABLE transactions
    ADD COLUMN cash UUID,
    ADD COLUMN transfer_id UUID,
    ADD COLUMN observation TEXT;

UPDATE transactions transaction_row
SET cash = financial.cash
FROM financial
WHERE transaction_row.financial = financial.id;

ALTER TABLE transactions
    ALTER COLUMN cash SET NOT NULL,
    ADD CONSTRAINT fk_transactions_cash FOREIGN KEY (cash) REFERENCES cash (id),
    ADD CONSTRAINT ck_transactions_transfer_pair CHECK (
        (transaction_operation IN (5, 6) AND transfer_id IS NOT NULL)
        OR (transaction_operation NOT IN (5, 6) AND transfer_id IS NULL)
    );

CREATE INDEX idx_transactions_cash ON transactions (cash);
CREATE INDEX idx_transactions_transfer_id ON transactions (transfer_id);
CREATE UNIQUE INDEX uk_transactions_transfer_operation
    ON transactions (transfer_id, transaction_operation)
    WHERE transfer_id IS NOT NULL;

CREATE OR REPLACE FUNCTION calculate_balance()
RETURNS TRIGGER AS $$
DECLARE
    session_id UUID;
    calculated_balance NUMERIC(18, 2);
BEGIN
    session_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.cash_transaction ELSE NEW.cash_transaction END;
    IF session_id IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT COALESCE(SUM(CASE
        WHEN transaction_operation IN (2, 6) THEN value
        WHEN transaction_operation IN (3, 5) THEN -value
        ELSE 0
    END), 0)
    INTO calculated_balance
    FROM transactions
    WHERE cash_transaction = session_id;

    UPDATE cash_transactions SET balance = calculated_balance WHERE id = session_id;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
