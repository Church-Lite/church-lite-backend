CREATE TABLE member_function (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE person_member_member_function (
    person_member_id UUID NOT NULL,
    member_function_id UUID NOT NULL,
    CONSTRAINT pk_person_member_member_function PRIMARY KEY (person_member_id, member_function_id),
    CONSTRAINT fk_member_function_member FOREIGN KEY (person_member_id) REFERENCES person_member (id) ON DELETE CASCADE,
    CONSTRAINT fk_member_function_function FOREIGN KEY (member_function_id) REFERENCES member_function (id) ON DELETE CASCADE
);

CREATE INDEX idx_member_function_name ON member_function (name);
CREATE INDEX idx_member_function_member ON person_member_member_function (person_member_id);
CREATE INDEX idx_member_function_function ON person_member_member_function (member_function_id);
