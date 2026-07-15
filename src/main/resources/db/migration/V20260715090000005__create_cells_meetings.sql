CREATE TABLE IF NOT EXISTS cell_visitor (
    id UUID NOT NULL,
    cell UUID NOT NULL,
    person UUID,
    name VARCHAR(180) NOT NULL,
    phone VARCHAR(40),
    email VARCHAR(180),
    first_visit_date DATE NOT NULL,
    last_visit_date DATE NOT NULL,
    visit_count INTEGER NOT NULL,
    invited_by UUID,
    follow_up_responsible UUID,
    status INTEGER NOT NULL,
    next_action_date DATE,
    notes TEXT,
    CONSTRAINT pk_cell_visitor PRIMARY KEY (id),
    CONSTRAINT fk_cell_visitor_cell FOREIGN KEY (cell) REFERENCES cell(id),
    CONSTRAINT fk_cell_visitor_person FOREIGN KEY (person) REFERENCES person(id),
    CONSTRAINT fk_cell_visitor_invited_by FOREIGN KEY (invited_by) REFERENCES person(id),
    CONSTRAINT fk_cell_visitor_follow_up FOREIGN KEY (follow_up_responsible) REFERENCES person(id),
    CONSTRAINT ck_cell_visitor_dates CHECK (last_visit_date >= first_visit_date),
    CONSTRAINT ck_cell_visitor_count CHECK (visit_count >= 1)
);
CREATE INDEX IF NOT EXISTS idx_cell_visitor_cell_status ON cell_visitor(cell, status);
CREATE INDEX IF NOT EXISTS idx_cell_visitor_follow_up ON cell_visitor(follow_up_responsible, next_action_date);

CREATE TABLE IF NOT EXISTS cell_meeting (
    id UUID NOT NULL,
    cell UUID NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    location VARCHAR(255),
    theme VARCHAR(255),
    study VARCHAR(255),
    responsible_leader UUID NOT NULL,
    adult_count INTEGER NOT NULL,
    children_count INTEGER NOT NULL,
    member_count INTEGER NOT NULL,
    visitor_count INTEGER NOT NULL,
    decisions INTEGER NOT NULL,
    reconciliations INTEGER NOT NULL,
    offering DOUBLE PRECISION,
    notes TEXT,
    status INTEGER NOT NULL,
    submitted_at TIMESTAMP,
    submitted_by UUID,
    reviewed_at TIMESTAMP,
    review_reason TEXT,
    cancellation_reason TEXT,
    CONSTRAINT pk_cell_meeting PRIMARY KEY (id),
    CONSTRAINT fk_cell_meeting_cell FOREIGN KEY (cell) REFERENCES cell(id),
    CONSTRAINT fk_cell_meeting_leader FOREIGN KEY (responsible_leader) REFERENCES person(id),
    CONSTRAINT fk_cell_meeting_submitted_by FOREIGN KEY (submitted_by) REFERENCES user_configuration(id),
    CONSTRAINT ck_cell_meeting_dates CHECK (end_at >= start_at),
    CONSTRAINT ck_cell_meeting_counts CHECK (adult_count >= 0 AND children_count >= 0 AND member_count >= 0 AND visitor_count >= 0 AND decisions >= 0 AND reconciliations >= 0),
    CONSTRAINT ck_cell_meeting_offering CHECK (offering IS NULL OR offering >= 0)
);
CREATE INDEX IF NOT EXISTS idx_cell_meeting_cell_start ON cell_meeting(cell, start_at);
CREATE INDEX IF NOT EXISTS idx_cell_meeting_status ON cell_meeting(status);

CREATE TABLE IF NOT EXISTS cell_attendance (
    id UUID NOT NULL,
    meeting UUID NOT NULL,
    person UUID,
    visitor UUID,
    type INTEGER NOT NULL,
    present BOOLEAN NOT NULL,
    notes TEXT,
    CONSTRAINT pk_cell_attendance PRIMARY KEY (id),
    CONSTRAINT fk_cell_attendance_meeting FOREIGN KEY (meeting) REFERENCES cell_meeting(id) ON DELETE CASCADE,
    CONSTRAINT fk_cell_attendance_person FOREIGN KEY (person) REFERENCES person(id),
    CONSTRAINT fk_cell_attendance_visitor FOREIGN KEY (visitor) REFERENCES cell_visitor(id),
    CONSTRAINT ck_cell_attendance_subject CHECK ((person IS NOT NULL AND visitor IS NULL) OR (person IS NULL AND visitor IS NOT NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cell_attendance_person ON cell_attendance(meeting, person) WHERE person IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cell_attendance_visitor ON cell_attendance(meeting, visitor) WHERE visitor IS NOT NULL;

CREATE TABLE IF NOT EXISTS cell_prayer_request (
    id UUID NOT NULL,
    meeting UUID NOT NULL,
    person UUID,
    visitor UUID,
    description TEXT NOT NULL,
    status INTEGER NOT NULL,
    confidential BOOLEAN NOT NULL,
    follow_up_responsible UUID,
    answered_at TIMESTAMP,
    notes TEXT,
    CONSTRAINT pk_cell_prayer_request PRIMARY KEY (id),
    CONSTRAINT fk_cell_prayer_meeting FOREIGN KEY (meeting) REFERENCES cell_meeting(id) ON DELETE CASCADE,
    CONSTRAINT fk_cell_prayer_person FOREIGN KEY (person) REFERENCES person(id),
    CONSTRAINT fk_cell_prayer_visitor FOREIGN KEY (visitor) REFERENCES cell_visitor(id),
    CONSTRAINT fk_cell_prayer_follow_up FOREIGN KEY (follow_up_responsible) REFERENCES person(id),
    CONSTRAINT ck_cell_prayer_subject CHECK (person IS NULL OR visitor IS NULL)
);
CREATE INDEX IF NOT EXISTS idx_cell_prayer_status ON cell_prayer_request(status, follow_up_responsible);
