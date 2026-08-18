ALTER TABLE member_portal_transparency_configuration
    ADD COLUMN IF NOT EXISTS member_approval_enabled BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
IF to_regclass('cash_transactions') IS NOT NULL THEN
CREATE TABLE member_financial_statement (
    id UUID PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    anonymous_salt UUID NOT NULL,
    published_at TIMESTAMP,
    closed_at TIMESTAMP,
    CONSTRAINT ck_member_financial_statement_status CHECK (status IN ('DRAFT','OPEN','CLOSED'))
);
CREATE TABLE member_financial_statement_cash (
    statement_id UUID NOT NULL REFERENCES member_financial_statement(id) ON DELETE CASCADE,
    cash_transaction_id UUID NOT NULL REFERENCES cash_transactions(id),
    PRIMARY KEY (statement_id, cash_transaction_id)
);
CREATE TABLE member_financial_statement_vote (
    id UUID PRIMARY KEY,
    statement_id UUID NOT NULL REFERENCES member_financial_statement(id) ON DELETE CASCADE,
    voter_fingerprint VARCHAR(64) NOT NULL,
    approved BOOLEAN NOT NULL,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_statement_anonymous_vote UNIQUE (statement_id, voter_fingerprint)
);
CREATE INDEX idx_member_statement_vote_result ON member_financial_statement_vote(statement_id, approved);
END IF;
END $$;
