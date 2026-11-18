package com.atomskills.atombackend.reader;

import com.atomskills.atombackend.dto.*;
import com.atomskills.atombackend.entity.*;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
        // Логика: берем последние события по каждому объекту и ресурсу,
        // которые требуют внимания (статус не "выполнено" или специфичный статус "требует ремонта")
        context.setRepairs(fetchLatestRepairs());

        // 4. Читаем зависимости между объектами
        context.setDependencies(fetchDependencies());

        // 5. Читаем базовое время решения
        context.setBaseDurations(fetchBaseDurations());

        return context;
    }

    private List<WorkerDto> fetchWorkers() {
        return queryFactory
                .select(Projections.fields(WorkerDto.class,
                        qEmployee.tabel,
                        qEmployee.firstName.as("name")))
                .from(qEmployee)
                .fetch();
    }

    private List<QualificationDto> fetchQualifications() {
        return queryFactory
                .select(Projections.fields(QualificationDto.class,
                        qQualification.employee.tabel,
                        qQualification.resourceType,
                        qQualification.proff))
                .from(qQualification)
                .fetch();
    }

    private List<RepairCandidateDto> fetchLatestRepairs() {
        // Сложный запрос: найти последние события по паре (idEdifice, resourceType)
        // Предполагаем, что есть статус "OPEN" или "PENDING" с кодом, например, 1
        // Или просто берем самые свежие события, а фильтрацию сделаем в Java

        return queryFactory
                .select(Projections.fields(RepairCandidateDto.class,
                        qEvent.idEvent,
                        qEvent.edifice.idEdifice,
                        qEvent.resourceType,
                        qEvent.occurredAt,
                        qEvent.status.idStatus,
                        qEventStatus.name.as("statusName")))
                .from(qEvent)
                .join(qEvent.status, qEventStatus)
                .where(
                        // Пример фильтрации: только активные проблемы
                        // qEvent.status.code.eq("ACTIVE")
                        // Или подзапрос на максимальную дату, если нужно строго последнее
                )
                .orderBy(qEvent.occurredAt.desc())
                .fetch();

        // Примечание: В реальном проекте здесь нужен более сложный запрос
        // с DISTINCT ON или подзапросом MAX(occurredAt) GROUP BY edifice, resource
    }

    private List<DependencyDto> fetchDependencies() {
        return queryFactory
                .select(Projections.fields(DependencyDto.class,
                        qEdificeDependency.resourceType,
                        qEdificeDependency.dependent.idEdifice.as("idEdificeDependent"),
                        qEdificeDependency.source.idEdifice.as("idEdificeSource")))
                .from(qEdificeDependency)
                .fetch();
    }

    private List<BaseDurationDto> fetchBaseDurations() {
        return queryFactory
                .select(Projections.fields(BaseDurationDto.class,
                        qTimeResolve.resourceType,
                        qTimeResolve.edifice.idEdifice,
                        // Преобразование Interval в секунды зависит от диалекта БД.
                        // Для PostgreSQL часто используют extract(epoch from ...)
                        // Здесь псевдо-код, так как JPA/QueryDSL плохо дружат с Interval напрямую в проекциях
                        qTimeResolve.avgResolutionTime.stringValue().as("avgResolutionSeconds")
                ))
                .from(qTimeResolve)
                .fetch();
    }
}