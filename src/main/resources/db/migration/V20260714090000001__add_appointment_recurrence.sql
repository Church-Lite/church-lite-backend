ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS status integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS recurrence_type integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS recurrence_days varchar(100),
    ADD COLUMN IF NOT EXISTS recurrence_end_date date,
    ADD COLUMN IF NOT EXISTS recurrence_group_id uuid;

CREATE INDEX IF NOT EXISTS idx_appointments_initial_date
    ON appointments (initial_date);

CREATE INDEX IF NOT EXISTS idx_appointments_recurrence_group
    ON appointments (recurrence_group_id);
