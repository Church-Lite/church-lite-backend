--Entities

CREATE TABLE IF NOT EXISTS pessoa(

                                     id uuid,
                                     name varchar,
                                     active varchar
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
                                   state uuid
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

CREATE TABLE IF NOT EXISTS pessoa_documentos(

                                                id uuid,
                                                person uuid,
                                                cpf varchar,
                                                rg varchar,
                                                cnpj varchar,
                                                birth_date date,
                                                type_person varchar
    --created_by varchar(80),
    --created_date timestamp,
    --last_modified_by varchar(80),
    --last_modified_date timestamp
);

-- PKs

ALTER TABLE pessoa  ADD CONSTRAINT pk_pessoa  PRIMARY KEY (id);
ALTER TABLE pessoa_endereco  ADD CONSTRAINT pk_pessoa_endereco  PRIMARY KEY (id);
ALTER TABLE city  ADD CONSTRAINT pk_city  PRIMARY KEY (id);
ALTER TABLE state  ADD CONSTRAINT pk_state  PRIMARY KEY (id);
ALTER TABLE country  ADD CONSTRAINT pk_country  PRIMARY KEY (id);
ALTER TABLE pessoa_documentos  ADD CONSTRAINT pk_pessoa_documentos  PRIMARY KEY (id);
-- Fks

ALTER TABLE pessoa_endereco ADD CONSTRAINT fk_pessoa_endereco_pessoa_person FOREIGN KEY (person) REFERENCES pessoa(id);
ALTER TABLE pessoa_endereco ADD CONSTRAINT fk_pessoa_endereco_city_city FOREIGN KEY (city) REFERENCES city(id);
ALTER TABLE city ADD CONSTRAINT fk_city_state_state FOREIGN KEY (state) REFERENCES state(id);
ALTER TABLE state ADD CONSTRAINT fk_state_country_country FOREIGN KEY (country) REFERENCES country(id);
ALTER TABLE pessoa_documentos ADD CONSTRAINT fk_pessoa_documentos_pessoa_person FOREIGN KEY (person) REFERENCES pessoa(id);
--RelationShips

