ALTER TABLE person
    ALTER COLUMN type TYPE integer
    USING type::integer;
