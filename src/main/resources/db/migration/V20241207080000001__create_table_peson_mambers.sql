CREATE TABLE IF NOT EXISTS person_member(

    id uuid,
    person uuid,
    entry_date date,
    date_baptism date,
    position uuid
);

ALTER TABLE person_member  ADD CONSTRAINT pk_person_member  PRIMARY KEY (id);
ALTER TABLE person_member ADD CONSTRAINT fk_person_member_person_person FOREIGN KEY (person) REFERENCES person(id);
ALTER TABLE person_member ADD CONSTRAINT fk_person_member_position_members_position FOREIGN KEY (position) REFERENCES position_members(id);