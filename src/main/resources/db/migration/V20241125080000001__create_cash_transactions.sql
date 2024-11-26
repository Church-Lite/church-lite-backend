alter table transactions add   cash_transaction uuid;

CREATE TABLE IF NOT EXISTS cash_transactions(

    id uuid,
    start_date date,
    balance numeric(18,2),
    initial_balance numeric(18,2),
    final_balance numeric(18,2),
    cash uuid,
    end_date date
);

ALTER TABLE cash_transactions  ADD CONSTRAINT pk_cash_transactions  PRIMARY KEY (id);
ALTER TABLE cash_transactions ADD CONSTRAINT fk_cash_transactions_cash_cash FOREIGN KEY (cash) REFERENCES cash(id);
