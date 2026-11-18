CREATE TABLE brigade (
    id BIGINT PRIMARY KEY
);

CREATE TABLE brigade_employee (
    brigade_id     BIGINT NOT NULL,
    employee_tabel BIGINT NOT NULL,
    position       INT    NOT NULL,
    CONSTRAINT pk_brigade_employee PRIMARY KEY (brigade_id, employee_tabel)
);
CREATE INDEX idx_brigade_employee_pos
    ON brigade_employee (brigade_id, position);

CREATE TABLE brigade_event_order (
    brigade_id BIGINT NOT NULL,
    event_id   BIGINT NOT NULL,
    position   INT    NOT NULL,
    CONSTRAINT pk_brigade_event_order PRIMARY KEY (brigade_id, event_id)
);
CREATE INDEX idx_brigade_event_order_pos
    ON brigade_event_order (brigade_id, position);