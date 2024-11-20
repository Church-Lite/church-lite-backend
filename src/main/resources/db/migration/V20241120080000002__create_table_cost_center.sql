
CREATE TABLE IF NOT EXISTS cost_center(

                                          id uuid,
                                          description varchar,
                                          code_tree varchar,
                                          parent_code uuid
);

ALTER TABLE cost_center  ADD CONSTRAINT pk_cost_center  PRIMARY KEY (id);

ALTER TABLE cost_center ADD CONSTRAINT fk_cost_center_cost_center_parent_code FOREIGN KEY (parent_code) REFERENCES cost_center(id);