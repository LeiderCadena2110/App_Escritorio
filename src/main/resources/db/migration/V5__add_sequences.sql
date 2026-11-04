-- Document numbering in its own table instead of the generic settings.
-- Each row is a series: the invoice series today; purchases or returns can be added later.
-- The values the user already had in settings are carried over.

CREATE TABLE sequences (
    name        VARCHAR(50)  PRIMARY KEY,
    prefix      VARCHAR(10)  NOT NULL DEFAULT '',
    next_value  BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_sequences_next_value CHECK (next_value >= 1)
);

INSERT INTO sequences (name, prefix, next_value) VALUES (
    'INVOICE',
    COALESCE((SELECT NULLIF(TRIM(setting_value), '') FROM settings WHERE setting_key = 'invoice.prefix'), 'FV-'),
    COALESCE((SELECT CAST(NULLIF(TRIM(setting_value), '') AS BIGINT) FROM settings WHERE setting_key = 'invoice.nextNumber'), 1)
);

DELETE FROM settings WHERE setting_key IN ('invoice.prefix', 'invoice.nextNumber');
