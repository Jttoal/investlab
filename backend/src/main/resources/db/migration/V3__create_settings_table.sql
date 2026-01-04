CREATE TABLE setting (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(150) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'string',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_setting_key ON setting(config_key);
