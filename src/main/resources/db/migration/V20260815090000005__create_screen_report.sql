DO $$
BEGIN
    IF upper(current_schema()) NOT LIKE '%\_ADMIN' ESCAPE '\' THEN
        CREATE TABLE IF NOT EXISTS screen_report (
            id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
            name varchar(160) NOT NULL,
            screen varchar(300) NOT NULL,
            smart_report_id uuid NOT NULL,
            display_order integer NOT NULL DEFAULT 0,
            active boolean NOT NULL DEFAULT true,
            CONSTRAINT ck_screen_report_name_not_blank CHECK (length(trim(name)) > 0),
            CONSTRAINT ck_screen_report_screen_not_blank CHECK (length(trim(screen)) > 0),
            CONSTRAINT uk_screen_report_screen_remote UNIQUE (screen, smart_report_id)
        );

        CREATE INDEX IF NOT EXISTS idx_screen_report_screen_active_order
            ON screen_report (screen, active, display_order, name);
    END IF;
END $$;
