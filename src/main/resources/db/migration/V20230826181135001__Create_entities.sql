--Entities

CREATE TABLE IF NOT EXISTS pessoa(

                                     id serial,
                                     name varchar
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS pessoa_juridica(

                                              id serial,
                                              cnpj varchar,
                                              pessoa integer
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS city(

                                   id uuid,
                                   name varchar,
                                   code varchar,
                                   state uuid
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS state(

                                    id uuid,
                                    name varchar,
                                    code varchar,
                                    country uuid
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS country(

                                      id uuid,
                                      name varchar,
                                      code varchar
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS fiscal(

                                     id serial,
                                     optante_simples boolean,
                                     mod_tributacao varchar,
                                     cod_regime_tributario integer
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

CREATE TABLE IF NOT EXISTS pessoa_telefone(

                                              id serial,
                                              fone varchar,
                                              pessoa integer
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

-- PKs

ALTER TABLE pessoa  ADD CONSTRAINT ok_XQLGbzSKO9dRMiwq6eH9  PRIMARY KEY (id);
ALTER TABLE pessoa_juridica  ADD CONSTRAINT ok_dtC6lZaJH7rzxyCpymOs  PRIMARY KEY (id);
ALTER TABLE city  ADD CONSTRAINT ok_wLA8yQx76lOfOBZqiF0l  PRIMARY KEY (id);
ALTER TABLE state  ADD CONSTRAINT ok_CT9G6aiKtze92sPofeWl  PRIMARY KEY (id);
ALTER TABLE country  ADD CONSTRAINT ok_p367BQ0u8hTcSxJBmxCR  PRIMARY KEY (id);
ALTER TABLE fiscal  ADD CONSTRAINT ok_ursHbypX10Gtog3etJgT  PRIMARY KEY (id);
ALTER TABLE pessoa_telefone  ADD CONSTRAINT ok_uTx2nPBpzc3OU8sxVKPJ  PRIMARY KEY (id);
-- Fks

ALTER TABLE pessoa_juridica ADD CONSTRAINT fk_MA6UZnTlFxL9oBS2vRhK FOREIGN KEY (pessoa) REFERENCES pessoa(id);
ALTER TABLE city ADD CONSTRAINT fk_0vVBj71RRDltQkmS4iBn FOREIGN KEY (state) REFERENCES state(id);
ALTER TABLE state ADD CONSTRAINT fk_gWgMfiK5AdttjPfLoiGU FOREIGN KEY (country) REFERENCES country(id);
ALTER TABLE pessoa_telefone ADD CONSTRAINT fk_xKotXfE8WN3FB5pmUbkR FOREIGN KEY (pessoa) REFERENCES pessoa(id);
--RelationShips

