CREATE TABLE church_configuration (
    id UUID PRIMARY KEY,
    cnpj VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    foundation_date DATE,
    postal_code VARCHAR(10),
    address VARCHAR(255),
    number VARCHAR(30),
    complement VARCHAR(255),
    neighborhood VARCHAR(255),
    city UUID REFERENCES city(id),
    phone VARCHAR(30),
    leader UUID REFERENCES user_configuration(id),
    cash_approval_policy INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_church_configuration_policy CHECK (cash_approval_policy BETWEEN 0 AND 2)
);

CREATE UNIQUE INDEX uk_church_configuration_singleton ON church_configuration ((true));

CREATE TABLE church_responsible_user (
    id UUID PRIMARY KEY,
    "user" UUID NOT NULL REFERENCES user_configuration(id) ON DELETE CASCADE,
    treasurer BOOLEAN NOT NULL DEFAULT FALSE,
    financial_approver BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_church_responsible_user UNIQUE ("user"),
    CONSTRAINT ck_church_responsible_user_role CHECK (treasurer OR financial_approver)
);
