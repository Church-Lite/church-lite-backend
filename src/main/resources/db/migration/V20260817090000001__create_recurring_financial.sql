CREATE TABLE recurring_financial (
    id UUID PRIMARY KEY,
    description VARCHAR NOT NULL,
    type_financial INTEGER NOT NULL,
    recurrence_mode INTEGER NOT NULL,
    frequency INTEGER NOT NULL,
    value_type INTEGER NOT NULL,
    value NUMERIC NOT NULL CHECK (value > 0),
    first_due_date DATE NOT NULL,
    end_date DATE,
    occurrence_count INTEGER,
    status INTEGER NOT NULL,
    generated_occurrences INTEGER NOT NULL DEFAULT 0,
    cash UUID NOT NULL REFERENCES cash(id),
    person UUID REFERENCES person(id),
    plan_account UUID NOT NULL REFERENCES plan_account(id),
    cost_center UUID NOT NULL REFERENCES cost_center(id),
    CONSTRAINT ck_recurring_financial_dates CHECK (end_date IS NULL OR end_date >= first_due_date),
    CONSTRAINT ck_recurring_financial_count CHECK (occurrence_count IS NULL OR occurrence_count BETWEEN 2 AND 60)
);

ALTER TABLE financial ADD COLUMN recurring_financial UUID;
ALTER TABLE financial ADD COLUMN recurrence_number INTEGER;
ALTER TABLE financial ADD CONSTRAINT fk_financial_recurring_financial
    FOREIGN KEY (recurring_financial) REFERENCES recurring_financial(id);
ALTER TABLE financial ADD CONSTRAINT uq_financial_recurrence_number
    UNIQUE (recurring_financial, recurrence_number);

CREATE INDEX idx_recurring_financial_status ON recurring_financial(status);
CREATE INDEX idx_financial_recurring_financial ON financial(recurring_financial);
