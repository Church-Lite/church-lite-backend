CREATE TABLE permission_group (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL,
    description varchar(500),
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_permission_group_name UNIQUE (name)
);

CREATE TABLE permission_group_member (
    id uuid PRIMARY KEY,
    permission_group uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT fk_permission_group_member_group FOREIGN KEY (permission_group)
        REFERENCES permission_group(id) ON DELETE CASCADE,
    CONSTRAINT uk_permission_group_member UNIQUE (permission_group, user_id)
);

CREATE INDEX idx_permission_group_member_user ON permission_group_member(user_id);

CREATE TABLE permission_group_denial (
    id uuid PRIMARY KEY,
    permission_group uuid NOT NULL,
    resource varchar(180) NOT NULL,
    permission varchar(20) NOT NULL,
    CONSTRAINT fk_permission_group_denial_group FOREIGN KEY (permission_group)
        REFERENCES permission_group(id) ON DELETE CASCADE,
    CONSTRAINT uk_permission_group_denial UNIQUE (permission_group, resource, permission),
    CONSTRAINT ck_permission_group_denial_permission
        CHECK (permission IN ('CREATE', 'VIEW', 'UPDATE', 'DELETE'))
);

CREATE INDEX idx_permission_group_denial_lookup
    ON permission_group_denial(resource, permission, permission_group);
