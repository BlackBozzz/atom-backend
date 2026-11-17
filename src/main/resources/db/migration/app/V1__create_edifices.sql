-- Курсоры синхронизации
CREATE TABLE external_sync_cursor (
    source       varchar(50) PRIMARY KEY,
    last_seen_id bigint      NOT NULL DEFAULT 0,
    updated_at   timestamptz NOT NULL DEFAULT now()
);

INSERT INTO external_sync_cursor (source, last_seen_id) VALUES
    ('edifices', 0),
    ('event_statuses', 0),
    ('events', 0),
    ('edifice_dependencies', 0)
ON CONFLICT (source) DO NOTHING;

-- Реплики
CREATE TABLE edifice (
    id_edifice    int4 PRIMARY KEY,
    name          varchar(120) NOT NULL,
    resource_type varchar[]    NOT NULL,
    district      varchar(50),
    street        varchar(80),
    house_number  varchar(10),
    synced_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE event_status (
    id_status int2 PRIMARY KEY,
    name      varchar(20) NOT NULL UNIQUE,
    synced_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE edifice_dependency (
    id_dependence        int4 PRIMARY KEY,
    resource_type        varchar(15) NOT NULL,
    id_edifice_source    int4 NOT NULL REFERENCES edifice(id_edifice),
    id_edifice_dependent int4 NOT NULL REFERENCES edifice(id_edifice),
    synced_at            timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT edifice_dependency_check CHECK (id_edifice_source <> id_edifice_dependent)
);

CREATE TABLE event (
    id_event      int4 PRIMARY KEY,
    id_edifice    int4 NOT NULL REFERENCES edifice(id_edifice),
    resource_type varchar(15) NOT NULL,
    occurred_at   timestamp NOT NULL,
    id_status     int2 NOT NULL REFERENCES event_status(id_status),
    synced_at     timestamptz NOT NULL DEFAULT now()
);