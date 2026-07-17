CREATE TABLE cash_closing_approval (
    id UUID PRIMARY KEY,
    cash_transaction UUID NOT NULL REFERENCES cash_transactions(id) ON DELETE CASCADE,
    approver UUID NOT NULL REFERENCES user_configuration(id) ON DELETE CASCADE,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    approved_at TIMESTAMP,
    CONSTRAINT uk_cash_closing_approval UNIQUE (cash_transaction, approver)
);

CREATE INDEX idx_cash_closing_approval_approver ON cash_closing_approval (approver, approved);
