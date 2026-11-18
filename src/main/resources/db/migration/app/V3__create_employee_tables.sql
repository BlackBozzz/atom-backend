CREATE TABLE employee (
    tabel        numeric(6) PRIMARY KEY,
    first_name   varchar(100) NOT NULL,
    last_name    varchar(100) NOT NULL,
    middle_name  varchar(100),
    start_date_experience date,
    synced_at    timestamptz  NOT NULL DEFAULT now()
);

-- 2. Таблица квалификаций (qualification)
CREATE TABLE qualification (
    id_qualification int4 PRIMARY KEY,
    id_employee      numeric(6) NOT NULL REFERENCES employee(tabel),
    resource_type    varchar(20) NOT NULL,
    proff            numeric(3,1) NOT NULL,
    synced_at        timestamptz NOT NULL DEFAULT now()
);

-- Индекс для быстрого поиска квалификаций по рабочему
CREATE INDEX IF NOT EXISTS idx_qualification_employee
    ON qualification (id_employee);

-- 3. Таблица среднего времени ликвидации аварии (time_resolve)
CREATE TABLE time_resolve (
    id_time              int4 PRIMARY KEY,
    resource_type        varchar(20) NOT NULL,
    avg_resolution_time  varchar(40),          -- было interval
    id_edifice           int4 NOT NULL REFERENCES edifice(id_edifice),
    synced_at            timestamptz NOT NULL DEFAULT now()
);

INSERT INTO external_sync_cursor (source, last_seen_id) VALUES
    ('employees', 0),
    ('qualifications', 0),
    ('time_resolve', 0)
ON CONFLICT (source) DO NOTHING;