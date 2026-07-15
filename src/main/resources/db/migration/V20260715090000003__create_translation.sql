CREATE TABLE IF NOT EXISTS translation (
    id UUID NOT NULL,
    language VARCHAR(10) NOT NULL,
    translation_key VARCHAR(255) NOT NULL,
    value TEXT NOT NULL,
    CONSTRAINT pk_translation PRIMARY KEY (id),
    CONSTRAINT uk_translation_language_key UNIQUE (language, translation_key)
);

CREATE INDEX IF NOT EXISTS idx_translation_language ON translation (language);
