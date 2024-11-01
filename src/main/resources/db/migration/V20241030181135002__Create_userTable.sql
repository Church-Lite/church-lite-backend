DO $$
BEGIN
    IF current_schema = 'CHURCH_LITE_ADMIN' THEN
            CREATE TABLE IF NOT EXISTS pessoa_usuario(
                 id uuid,
                 name uuid,
                 email varchar,
                 password varchar,
                 tenant varchar
            );
    END IF;
END $$;

