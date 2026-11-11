-- Version 4: the READ MODEL (the query side of CQRS). Built only from events, shaped for one question:
-- "which trades were settled, per instrument?". No foreign key to trade: it could live in another service's database.
CREATE TABLE settled_trade_report (
    trade_id    BIGINT PRIMARY KEY,
    isin        VARCHAR(12)    NOT NULL,
    quantity    NUMERIC(19, 0) NOT NULL,
    notional    NUMERIC(19, 4) NOT NULL,
    settled_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_report_isin ON settled_trade_report (isin);
