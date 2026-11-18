package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.*;
import com.atomskills.atombackend.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanningDataReader {
    private final JPAQueryFactory queryFactory;

    // Q-проекции как private final
    private final QEmployee qEmployee = QEmployee.employee;
    private final QQualification qQualification = QQualification.qualification;
    private final QEdifice qEdifice = QEdifice.edifice;
    private final QEdificeDependency qEdificeDependency = QEdificeDependency.edificeDependency;
    private final QEvent qEvent = QEvent.event;
    private final QEventStatus qEventStatus = QEventStatus.eventStatus;
    private final QTimeResolve qTimeResolve = QTimeResolve.timeResolve;

    /**
     * Собирает все необходимые данные для планировщика.
     */
    public InputContextDto readInputData() {
        InputContextDto context = new InputContextDto();

        // 1. Читаем рабочих
        context.setWorkers(fetchWorkers());

        // 2. Читаем квалификации
        context.setQualifications(fetchQualifications());

        // 3. Читаем последние события (починки)
        context.setRepairs(fetchLatestRepairs());

        // 4. Читаем зависимости между объектами
        context.setDependencies(fetchDependencies());

        // 5. Читаем базовое время решения
        context.setBaseDurations(fetchBaseDurations());

        // 6. РУЧНАЯ ВСТАВКА МАТРИЦЫ КОНФЛИКТОВ
//        context.setConflictMatrix(createManualConflictMatrix());

        return context;
    }

    /**
     * Метод для ручного задания коэффициентов конфликтности.
     * Здесь вы можете прописать любые пары табельных номеров и уровень их конфликта (0.0 - 1.0).
     */
    private List<Object> createManualConflictMatrix() {
        List<Object> matrix = new ArrayList<>();

        return matrix;
    }


    private void addConflict(List<Object> matrix, Long tabelA, Long tabelB, double conflict) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("tabelA", tabelA);
        entry.put("tabelB", tabelB);
        entry.put("conflict", conflict);
        matrix.add(entry);
    }

    private List<WorkerDto> fetchWorkers() {
        List<Tuple> tuples = queryFactory
                .select(qEmployee.tabel, qEmployee.firstName)
                .from(qEmployee)
                .fetch();

        return tuples.stream().map(tuple -> {
            WorkerDto dto = new WorkerDto();
            Number tabelNum = tuple.get(qEmployee.tabel);
            if (tabelNum != null) {
                dto.setTabel(tabelNum.longValue());
            }
            dto.setName(tuple.get(qEmployee.firstName));
            return dto;
        }).collect(Collectors.toList());
    }

    private List<QualificationDto> fetchQualifications() {
        List<Tuple> tuples = queryFactory
                .select(qQualification.employee.tabel, qQualification.resourceType, qQualification.proff)
                .from(qQualification)
                .fetch();

        return tuples.stream().map(tuple -> {
            QualificationDto dto = new QualificationDto();
            Number tabelNum = tuple.get(qQualification.employee.tabel);
            if (tabelNum != null) {
                dto.setTabel(tabelNum.longValue());
            }
            dto.setResourceType(tuple.get(qQualification.resourceType));
            dto.setProff(tuple.get(qQualification.proff));
            return dto;
        }).collect(Collectors.toList());
    }

    private List<RepairCandidateDto> fetchLatestRepairs() {
        // Выбираем все активные события.
        // Если нужно только последнее по объекту/ресурсу, лучше отфильтровать в Java или использовать подзапрос.
        // Для простоты берем все, а CompactPlanner оставит только одно на (Edifice, Resource).
        List<Tuple> tuples = queryFactory
                .select(
                        qEvent.idEvent,
                        qEvent.edifice.idEdifice,
                        qEvent.resourceType,
                        qEvent.occurredAt,
                        qEvent.status.idStatus
                )
                .from(qEvent)
                .orderBy(qEvent.occurredAt.desc())
                .fetch();

        return tuples.stream().map(tuple -> {
            RepairCandidateDto dto = new RepairCandidateDto();

            Number idEventNum = tuple.get(qEvent.idEvent);
            if (idEventNum != null) dto.setIdEvent(idEventNum.longValue());

            Number idEdificeNum = tuple.get(qEvent.edifice.idEdifice);
            if (idEdificeNum != null) dto.setIdEdifice(idEdificeNum.longValue());

            dto.setResourceType(tuple.get(qEvent.resourceType));
            dto.setOccurredAt(tuple.get(qEvent.occurredAt));

            Number statusNum = tuple.get(qEvent.status.idStatus);
            if (statusNum != null) dto.setIdStatus(statusNum.longValue());

            return dto;
        }).collect(Collectors.toList());
    }

    private List<DependencyDto> fetchDependencies() {
        List<Tuple> tuples = queryFactory
                .select(
                        qEdificeDependency.resourceType,
                        qEdificeDependency.dependent.idEdifice,
                        qEdificeDependency.source.idEdifice
                )
                .from(qEdificeDependency)
                .fetch();

        return tuples.stream().map(tuple -> {
            DependencyDto dto = new DependencyDto();
            dto.setResourceType(tuple.get(qEdificeDependency.resourceType));

            Number depNum = tuple.get(qEdificeDependency.dependent.idEdifice);
            if (depNum != null) dto.setIdEdificeDependent(depNum.longValue());

            Number srcNum = tuple.get(qEdificeDependency.source.idEdifice);
            if (srcNum != null) dto.setIdEdificeSource(srcNum.longValue());

            return dto;
        }).collect(Collectors.toList());
    }

    private List<BaseDurationDto> fetchBaseDurations() {
        List<Tuple> tuples = queryFactory
                .select(
                        qTimeResolve.resourceType,
                        qTimeResolve.edifice.idEdifice,
                        qTimeResolve.avgResolutionTime
                )
                .from(qTimeResolve)
                .fetch();

        return tuples.stream().map(tuple -> {
            BaseDurationDto dto = new BaseDurationDto();
            dto.setResourceType(tuple.get(qTimeResolve.resourceType));

            // ID объекта может прийти как Integer/Short из БД, приводим к Long
            Number edificeId = tuple.get(qTimeResolve.edifice.idEdifice);
            if (edificeId != null) {
                dto.setIdEdifice(edificeId.longValue());
            }

            // Парсим строку интервала в секунды
            String intervalStr = tuple.get(qTimeResolve.avgResolutionTime);
            dto.setAvgResolutionSeconds(parseIntervalToSeconds(intervalStr));

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Парсит строку интервала (например, "00:01:30" или "PT1H30M") в секунды.
     */
    private Long parseIntervalToSeconds(String intervalStr) {
        if (intervalStr == null || intervalStr.isEmpty()) return 0L;

        try {
            // Вариант 1: Формат HH:MM:SS (часто встречается в PostgreSQL при приведении к строке)
            if (intervalStr.contains(":")) {
                String[] parts = intervalStr.split(":");
                if (parts.length == 3) {
                    long hours = Long.parseLong(parts[0]);
                    long minutes = Long.parseLong(parts[1]);
                    // В секундах могут быть доли, берем целую часть
                    long seconds = Long.parseLong(parts[2].split("\\.")[0]);
                    return (hours * 3600) + (minutes * 60) + seconds;
                }
            }

            // Вариант 2: Если это ISO-8601 формат (PT...S) или просто число
            try {
                java.time.Duration duration = java.time.Duration.parse("PT" + intervalStr.toUpperCase().replace(" ", ""));
                return duration.getSeconds();
            } catch (Exception e) {
                // Если не ISO формат, пробуем как простое число
                return (long) Double.parseDouble(intervalStr.replaceAll("[^0-9.]", ""));
            }

        } catch (Exception e) {
            System.err.println("Не удалось распарсить интервал: " + intervalStr + ". Возвращаю дефолт 3600.");
            return 3600L;
        }
    }
}