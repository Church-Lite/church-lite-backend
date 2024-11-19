CREATE TABLE IF NOT EXISTS plan_account(

                                           id uuid,
                                           description varchar,
                                           code_tree varchar,
                                           type varchar,
                                           parent_code uuid
);

ALTER TABLE plan_account  ADD CONSTRAINT pk_plan_account  PRIMARY KEY (id);
ALTER TABLE plan_account ADD CONSTRAINT fk_plan_account_plan_account_parent_code FOREIGN KEY (parent_code) REFERENCES plan_account(id);