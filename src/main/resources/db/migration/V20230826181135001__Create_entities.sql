--Entities

CREATE TABLE IF NOT EXISTS pessoa(

                                     id uuid,
                                     name varchar,
                                     is_active boolean
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS pessoa_endereco(

                                              id uuid,
                                              person uuid,
                                              address varchar,
                                              neighborhood varchar,
                                              number varchar,
                                              city uuid,
                                              complement varchar,
                                              postal_code varchar
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS city(

                                   id uuid,
                                   name varchar,
                                   ibge varchar,
                                   uf uuid
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS state(

                                    id uuid,
                                    name varchar,
                                    abreviation varchar,
                                    country uuid
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS country(

                                      id uuid,
                                      name varchar
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS pessoa_fisica(

                                            id uuid,
                                            person uuid
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

-- PKs

ALTER TABLE pessoa  ADD CONSTRAINT ok_z1Vph9pfdCeuxNGvR4MB  PRIMARY KEY (id);
ALTER TABLE pessoa_endereco  ADD CONSTRAINT ok_IF1E8ovLVYzFpI3TTd7H  PRIMARY KEY (id);
ALTER TABLE city  ADD CONSTRAINT ok_BxsHQZnhvNjCgQKNPDuD  PRIMARY KEY (id);
ALTER TABLE state  ADD CONSTRAINT ok_yo8jcmfgRUgxqamwYcYM  PRIMARY KEY (id);
ALTER TABLE country  ADD CONSTRAINT ok_CQ810BR6WKIgL5aXkSXN  PRIMARY KEY (id);
ALTER TABLE pessoa_fisica  ADD CONSTRAINT ok_LMcn7NJ24nxmTQg42YE4  PRIMARY KEY (id);
-- Fks

ALTER TABLE pessoa_endereco ADD CONSTRAINT fk_loLJTAXbLtylWxN8xnL0 FOREIGN KEY (person) REFERENCES pessoa(id);
ALTER TABLE pessoa_endereco ADD CONSTRAINT fk_ncO8NPyao1BNXNgAe0qW FOREIGN KEY (city) REFERENCES city(id);
ALTER TABLE city ADD CONSTRAINT fk_eBGhkShdR0mCl3vQaAy0 FOREIGN KEY (uf) REFERENCES state(id);
ALTER TABLE state ADD CONSTRAINT fk_OuWAPeTLamLx8neKVYMC FOREIGN KEY (country) REFERENCES country(id);
ALTER TABLE pessoa_fisica ADD CONSTRAINT fk_mBGt1rQl1O8MiwJ6SbbS FOREIGN KEY (person) REFERENCES pessoa(id);
--RelationShips

