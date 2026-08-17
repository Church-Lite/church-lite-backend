CREATE TABLE notification (
    id UUID PRIMARY KEY,
    recipient UUID NOT NULL REFERENCES user_configuration(id) ON DELETE CASCADE,
    type INTEGER NOT NULL,
    title VARCHAR NOT NULL,
    message VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    reference_type VARCHAR,
    reference_id UUID,
    action_url VARCHAR,
    deduplication_key VARCHAR NOT NULL,
    CONSTRAINT uk_notification_deduplication UNIQUE (deduplication_key)
);

CREATE INDEX idx_notification_recipient_created_at ON notification (recipient, created_at);
CREATE INDEX idx_notification_recipient_read_at ON notification (recipient, read_at);

ALTER TABLE cash_closing_approval ADD COLUMN requested_by UUID REFERENCES user_configuration(id);
