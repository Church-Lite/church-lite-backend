DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell_organization_unit' AND column_name = 'level_type_id') THEN
        ALTER TABLE cell_organization_unit RENAME COLUMN level_type_id TO level_type;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell_organization_unit' AND column_name = 'parent_unit_id') THEN
        ALTER TABLE cell_organization_unit RENAME COLUMN parent_unit_id TO parent_unit;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell_organization_unit' AND column_name = 'responsible_id') THEN
        ALTER TABLE cell_organization_unit RENAME COLUMN responsible_id TO responsible;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell' AND column_name = 'organization_unit_id') THEN
        ALTER TABLE cell RENAME COLUMN organization_unit_id TO organization_unit;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell' AND column_name = 'city_id') THEN
        ALTER TABLE cell RENAME COLUMN city_id TO city;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell_leadership' AND column_name = 'cell_id') THEN
        ALTER TABLE cell_leadership RENAME COLUMN cell_id TO cell;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell_leadership' AND column_name = 'person_id') THEN
        ALTER TABLE cell_leadership RENAME COLUMN person_id TO person;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell_member' AND column_name = 'cell_id') THEN
        ALTER TABLE cell_member RENAME COLUMN cell_id TO cell;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cell_member' AND column_name = 'person_id') THEN
        ALTER TABLE cell_member RENAME COLUMN person_id TO person;
    END IF;
END $$;
