-- Version 5: the idempotent consumer's memory. One row per (consumer, event) that has already been processed.
-- The primary key makes a second insert of the same id fail, which is the last line of defence against duplicates.
CREATE TABLE processed_event (
    id            VARCHAR(80) PRIMARY KEY,
    processed_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
