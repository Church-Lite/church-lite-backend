CREATE TABLE IF NOT EXISTS user_confirmation(

    id uuid,
    user_id uuid,
    hash varchar
);