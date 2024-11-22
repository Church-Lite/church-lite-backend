CREATE TABLE IF NOT EXISTS cash(

                                   id uuid,
                                   description varchar,
                                   type_cash varchar,
                                   bank uuid,
                                   number_account varchar,
                                   digit varchar
);

ALTER TABLE cash  ADD CONSTRAINT pk_cash  PRIMARY KEY (id);

ALTER TABLE cash ADD CONSTRAINT fk_cash_bank_bank FOREIGN KEY (bank) REFERENCES bank(id);