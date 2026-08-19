ALTER TABLE user_configuration
    ADD COLUMN IF NOT EXISTS cpf varchar(14);
