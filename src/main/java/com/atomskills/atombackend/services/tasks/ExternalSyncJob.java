package com.atomskills.atombackend.services.tasks;

import com.argent.servicecommon.queue.service.ArgentQueueService;
import com.atomskills.atombackend.dto.task.*;
import com.atomskills.atombackend.reader.*;
import com.atomskills.atombackend.repository.ExternalCursorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Единственная строгая очередь: здания -> статусы -> события -> зависимости.
 * Все четыре потока публикуются в одной транзакции основной БД.
 */
@Component
@ConditionalOnProperty(name = "app.external-sync.enabled", havingValue = "true", matchIfMissing = true)
public class ExternalSyncJob {
    private static final Logger log = LoggerFactory.getLogger(ExternalSyncJob.class);
    private static final String ORDERED_QUEUE = "legacy-sync-ordered";

    private final ExternalCursorRepository cursor;
    private final ArgentQueueService queue;
    private final JdbcTemplate mainJdbc;
    private final TransactionTemplate transaction;
    private final EdificeReader edificeReader;
    private final EventStatusReader statusReader;
    private final EventReader eventReader;
    private final EdificeDependencyReader depReader;
    private final EmployeeReader employeeReader;
    private final QualificationReader qualificationReader;
    private final TimeResolveReader timeResolveReader;
    private final int batchSize;
    private final int maxBatchesPerTable;

    public ExternalSyncJob(ExternalCursorRepository cursor,
                           ArgentQueueService queue,
                           JdbcTemplate mainJdbc,
                           PlatformTransactionManager transactionManager,
                           EdificeReader edificeReader,
                           EventStatusReader statusReader,
                           EventReader eventReader,
                           EdificeDependencyReader depReader,
                           EmployeeReader employeeReader,
                           QualificationReader qualificationReader,
                           TimeResolveReader timeResolveReader,
                           @Value("${app.external-sync.batch-size:500}") int batchSize,
                           @Value("${app.external-sync.max-batches-per-table:20}") int maxBatchesPerTable) {
        if (batchSize < 1 || maxBatchesPerTable < 1) {
            throw new IllegalArgumentException("Параметры batch-size и max-batches-per-table должны быть > 0");
        }
        this.cursor = cursor;
        this.queue = queue;
        this.mainJdbc = mainJdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.edificeReader = edificeReader;
        this.statusReader = statusReader;
        this.eventReader = eventReader;
        this.depReader = depReader;
        this.employeeReader = employeeReader;
        this.qualificationReader = qualificationReader;
        this.timeResolveReader = timeResolveReader;
        this.batchSize = batchSize;
        this.maxBatchesPerTable = maxBatchesPerTable;
    }

    @Scheduled(
            fixedDelayString = "${app.external-sync.poll-interval-ms:60000}",
            initialDelayString = "${app.external-sync.poll-interval-ms:60000}")
    public void poll() {
        try {
            transaction.executeWithoutResult(tx -> {
                // Одна активная синхронизация на все экземпляры приложения.
                Boolean acquired = mainJdbc.queryForObject(
                        "SELECT pg_try_advisory_xact_lock(14561, 1001)", Boolean.class);
                if (!Boolean.TRUE.equals(acquired)) return;

                // Однократно отключает старые независимые очереди и ставит все строки на повтор.
                // Если старый worker ещё выполняет задачу, новая отправка не начинается.
                if (!cursor.prepareOrderedQueue()) {
                    log.info("Ожидание завершения задачи в старой очереди синхронизации");
                    return;
                }
                queue.configureQueue(ORDERED_QUEUE, false); // строгий FIFO, без пропуска retry

                if (!syncTable("edifices", "external-edifice-batch",
                        l -> edificeReader.fetchSince(l, batchSize), EdificeTask::getIdEdifice,
                        EdificeBatchTask::new)) return;
                if (!syncTable("event_statuses", "external-event-status-batch",
                        l -> statusReader.fetchSince(l, batchSize), EventStatusTask::getIdStatus,
                        EventStatusBatchTask::new)) return;
                if (!syncTable("events", "external-event-batch",
                        l -> eventReader.fetchSince(l, batchSize), EventTask::getIdEvent,
                        EventBatchTask::new)) return;
                syncTable("edifice_dependencies", "external-edifice-dependency-batch",
                        l -> depReader.fetchSince(l, batchSize), EdificeDependencyTask::getIdDependence,
                        EdificeDependencyBatchTask::new);

                if (!syncTable("employees", "external-employee-batch",
                        l -> employeeReader.fetchSince(l, batchSize), EmployeeTask::getTabel,
                        EmployeeBatchTask::new)) return;

                if (!syncTable("qualifications", "external-qualification-batch",
                        l -> qualificationReader.fetchSince(l, batchSize), QualificationTask::getIdQualification,
                        QualificationBatchTask::new)) return;

                syncTable("time_resolve", "external-time-resolve-batch",
                        l -> timeResolveReader.fetchSince(l, batchSize), TimeResolveTask::getIdTime,
                        TimeResolveBatchTask::new);
            });
        } catch (DataAccessException e) {
            log.warn("Синхронизация отложена: недоступна БД ({})", e.getMostSpecificCause().getMessage());
        } catch (Exception e) {
            // Транзакция откатывает сразу и постановку задач, и курсоры.
            log.error("Не удалось синхронизировать внешнюю БД; задания и курсоры не изменены", e);
        }
    }

    /** Возвращает true только когда таблица дочитана до конца. */
    private <T> boolean syncTable(String source, String handler,
                                  Function<Long, List<T>> fetch, ToLongFunction<T> pk,
                                  Function<List<T>, Object> wrap) {
        long lastSeen = cursor.getLastSeenId(source);
        long initial = lastSeen;
        int total = 0;
        for (int batchNumber = 0; batchNumber < maxBatchesPerTable; batchNumber++) {
            List<T> batch = fetch.apply(lastSeen);
            if (batch.isEmpty()) {
                logProgress(source, initial, lastSeen, total);
                return true;
            }
            long batchStart = lastSeen;
            for (T item : batch) {
                long id = pk.applyAsLong(item);
                if (id <= lastSeen) {
                    throw new IllegalStateException(source + ": source не отсортирован строго по id, " + id + " <= " + lastSeen);
                }
                lastSeen = id;
            }
            // Один атомарный handler на batch: повышает скорость строгой очереди
            // и сохраняет порядок строк внутри каждой таблицы.
            queue.createQueueTask(ORDERED_QUEUE, handler, wrap.apply(batch), 0,
                    source + "-batch-" + batchStart + "-" + lastSeen);
            cursor.advance(source, lastSeen);
            total += batch.size();
            if (batch.size() < batchSize) {
                logProgress(source, initial, lastSeen, total);
                return true;
            }
        }
        logProgress(source, initial, lastSeen, total);
        log.debug("{}: достигнут лимит батчей; зависимые таблицы дождутся следующего опроса", source);
        return false;
    }

    private void logProgress(String source, long initial, long lastSeen, int total) {
        if (total > 0) log.info("Синхронизация {}: поставлено {} задач, курсор {} -> {}",
                source, total, initial, lastSeen);
    }
}
