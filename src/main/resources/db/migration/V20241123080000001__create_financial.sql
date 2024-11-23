CREATE TABLE IF NOT EXISTS financial(

                                        id uuid,
                                        description varchar,
                                        type_financial varchar,
                                        cash uuid,
                                        value numeric(18,2),
                                        person uuid,
                                        plan_account uuid,
                                        cost_center uuid,
                                        issue_date date,
                                        due_date date,
                                        payment_receipt_date date
);

ALTER TABLE financial  ADD CONSTRAINT pk_financial  PRIMARY KEY (id);

ALTER TABLE financial ADD CONSTRAINT fk_financial_cash_cash FOREIGN KEY (cash) REFERENCES cash(id);
ALTER TABLE financial ADD CONSTRAINT fk_financial_person_person FOREIGN KEY (person) REFERENCES person(id);
ALTER TABLE financial ADD CONSTRAINT fk_financial_plan_account_plan_account FOREIGN KEY (plan_account) REFERENCES plan_account(id);
ALTER TABLE financial ADD CONSTRAINT fk_financial_cost_center_cost_center FOREIGN KEY (cost_center) REFERENCES cost_center(id);