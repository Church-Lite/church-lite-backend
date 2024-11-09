--Entities

CREATE TABLE IF NOT EXISTS person(

    id uuid,
    name varchar,
    active varchar
);

CREATE TABLE IF NOT EXISTS person_address(

    id uuid,
    person uuid,
    address varchar,
    neighborhood varchar,
    number varchar,
    city uuid,
    complement varchar,
    postal_code varchar
);

CREATE TABLE IF NOT EXISTS city(

    id uuid,
    name varchar,
    ibge varchar,
    state uuid
);

CREATE TABLE IF NOT EXISTS state(

    id uuid,
    name varchar,
    abreviation varchar,
    country uuid
);

CREATE TABLE IF NOT EXISTS country(

    id uuid,
    name varchar,
    code_iso varchar,
    ddi varchar
);

CREATE TABLE IF NOT EXISTS person_docs(

    id uuid,
    person uuid,
    cpf varchar,
    rg varchar,
    cnpj varchar,
    birth_date date,
    type_person varchar
);

CREATE TABLE IF NOT EXISTS user_confirmation(

    id uuid,
    user_id uuid,
    hash varchar
);

-- PKs

ALTER TABLE person  ADD CONSTRAINT pk_person  PRIMARY KEY (id);
ALTER TABLE person_address  ADD CONSTRAINT pk_person_address  PRIMARY KEY (id);
ALTER TABLE city  ADD CONSTRAINT pk_city  PRIMARY KEY (id);
ALTER TABLE state  ADD CONSTRAINT pk_state  PRIMARY KEY (id);
ALTER TABLE country  ADD CONSTRAINT pk_country  PRIMARY KEY (id);
ALTER TABLE person_docs  ADD CONSTRAINT pk_person_docs  PRIMARY KEY (id);
ALTER TABLE user_confirmation  ADD CONSTRAINT pk_user_confirmation  PRIMARY KEY (id);
-- Fks

ALTER TABLE person_address ADD CONSTRAINT fk_person_address_person_person FOREIGN KEY (person) REFERENCES person(id);
ALTER TABLE person_address ADD CONSTRAINT fk_person_address_city_city FOREIGN KEY (city) REFERENCES city(id);
ALTER TABLE city ADD CONSTRAINT fk_city_state_state FOREIGN KEY (state) REFERENCES state(id);
ALTER TABLE state ADD CONSTRAINT fk_state_country_country FOREIGN KEY (country) REFERENCES country(id);
ALTER TABLE person_docs ADD CONSTRAINT fk_person_docs_person_person FOREIGN KEY (person) REFERENCES person(id);
--RelationShips

