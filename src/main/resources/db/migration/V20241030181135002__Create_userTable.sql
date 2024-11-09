
CREATE TABLE IF NOT EXISTS user_access(
     id uuid,
     name varchar,
     email varchar,
     password varchar,
     tenant varchar,
     active boolean,
     user_confirm boolean
);