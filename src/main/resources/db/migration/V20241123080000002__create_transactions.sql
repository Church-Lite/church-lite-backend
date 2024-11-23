CREATE TABLE IF NOT EXISTS transactions(

    id uuid,
    description varchar,
    value numeric(18,2),
    transaction_operation varchar,
    person uuid,
    financial uuid,
    date_transaction date
);

ALTER TABLE transactions  ADD CONSTRAINT pk_transactions  PRIMARY KEY (id);
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_person_person FOREIGN KEY (person) REFERENCES person(id);
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_financial_financial FOREIGN KEY (financial) REFERENCES financial(id);