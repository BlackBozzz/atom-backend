-- Индекс для выбора последнего события отдельно по каждому ресурсу объекта.
CREATE INDEX IF NOT EXISTS idx_event_edifice_resource_latest
    ON event (id_edifice, resource_type, occurred_at DESC, id_event DESC);
