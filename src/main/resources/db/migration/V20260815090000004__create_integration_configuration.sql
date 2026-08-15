DO $$
BEGIN
    IF upper(current_schema()) LIKE '%\_ADMIN' ESCAPE '\' THEN
        CREATE TABLE IF NOT EXISTS integration_configuration (
            id uuid PRIMARY KEY,
            service integer NOT NULL,
            configuration text NOT NULL,
            CONSTRAINT uk_integration_configuration_service UNIQUE (service),
            CONSTRAINT ck_integration_configuration_not_blank
                CHECK (length(trim(configuration)) > 0)
        );
    END IF;
END $$;
