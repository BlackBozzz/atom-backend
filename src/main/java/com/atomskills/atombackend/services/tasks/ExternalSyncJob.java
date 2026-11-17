package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueService;
import com.atomskills.atombackend.dto.EdificeDependencyTask;
import com.atomskills.atombackend.dto.EdificeTask;
import com.atomskills.atombackend.dto.EventStatusTask;
import com.atomskills.atombackend.dto.EventTask;
import com.atomskills.atombackend.reader.EdificeDependencyReader;
import com.atomskills.atombackend.reader.EdificeReader;
import com.atomskills.atombackend.reader.EventReader;
import com.atomskills.atombackend.reader.EventStatusReader;
import com.atomskills.atombackend.repository.ExternalCursorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

@Component
@ConditionalOnProperty(name = "app.external-sync.enabled", havingValue = "true", matchIfMissing = true)
public class ExternalSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ExternalSyncJob.class);

    private final ExternalCursorRepository cursor;
    private final ArgentQueueService queue;
    private final EdificeReader edificeReader;
    private final EventStatusReader statusReader;
    private final EventReader eventReader;
    private final EdificeDependencyReader depReader;
    private final int batchSize;
    private final long pollIntervalMs;

    public ExternalSyncJob(ExternalCursorRepository cursor,
                         ArgentQueueService queue,
                         EdificeReader edificeReader,
                         EventStatusReader statusReader,
                         EventReader eventReader,
                         EdificeDependencyReader depReader,
                         @Value("${app.external-sync.batch-size:500}") int batchSize,
                         @Value("${app.external-sync.poll-interval-ms:60000}") long pollIntervalMs) {
        this.cursor = cursor;
        this.queue = queue;
        this.edificeReader = edificeReader;
        this.statusReader = statusReader;
        this.eventReader = eventReader;
        this.depReader = depReader;
        this.batchSize = batchSize;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Scheduled(
            fixedDelayString   = "${app.external-sync.poll-interval-ms:60000}",
            initialDelayString = "${app.external-sync.poll-interval-ms:60000}"
    )
    public void poll() {
        // Порядок: родители раньше детей (FK).
        syncTable("edifices", "external-edifice",
                l -> edificeReader.fetchSince(l, batchSize),
                EdificeTask::idEdifice);                 // int → long, widening

        syncTable("event_statuses", "external-event-status",
                l -> statusReader.fetchSince(l, batchSize),
                EventStatusTask::idStatus);              // short → long, widening

        syncTable("events", "external-event",
                l -> eventReader.fetchSince(l, batchSize),
                EventTask::idEvent);                 // int → long

        syncTable("edifice_dependencies", "external-edifice-dependency",
                l -> depReader.fetchSince(l, batchSize),
                EdificeDependencyTask::idDependence);    // int → long
    }

    private <T> void syncTable(String source, String handlerKey,
                               Function<Long, List<T>> fetch,
                               ToLongFunction<T> pk) {
        long lastSeen = cursor.getLastSeenId(source);
        long initialCursor = lastSeen;
        int total = 0;
        try {
            while (true) {
                List<T> batch = fetch.apply(lastSeen);
                if (batch.isEmpty()) break;

                for (T item : batch) {
                    long id = pk.applyAsLong(item);
                    queue.createQueueTask(
                            "legacy-sync-" + source,     // своя очередь на таблицу — параллелизм
                            handlerKey,
                            item,
                            0,
                            source + "-" + id             // idempotencyKey
                    );
                }

                long newLastSeen = pk.applyAsLong(batch.get(batch.size() - 1));
                cursor.advance(source, newLastSeen);
                lastSeen = newLastSeen;
                total += batch.size();

                if (batch.size() < batchSize) break;
            }
            if (total > 0) {
                log.info("Синхронизация {}: {} задач, курсор {} → {}",
                        source, total, initialCursor, lastSeen);
            }
        } catch (DataAccessException e) {
            log.warn("Синхронизация {}: внешняя БД недоступна ({}). Курсор {}, повтор через {} мс",
                    source, e.getMostSpecificCause().getMessage(), initialCursor, pollIntervalMs);
        } catch (Exception e) {
            log.error("Синхронизация {}: непредвиденная ошибка. Курсор {}",
                    source, initialCursor, e);
        }
    }
}