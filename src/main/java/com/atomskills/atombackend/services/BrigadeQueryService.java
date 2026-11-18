package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.BrigadeDto;
import com.atomskills.atombackend.dto.EmployeeDto;
import com.atomskills.atombackend.dto.EventDto;
import com.atomskills.atombackend.dto.caclulate.BrigadesResponseDto;
import com.atomskills.atombackend.entity.QBrigade;
import com.atomskills.atombackend.entity.QBrigadeEmployee;
import com.atomskills.atombackend.entity.QBrigadeEventOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BrigadeQueryService {

    private final JPAQueryFactory queryFactory;
    private final EmployeeQueryService employeeQueryService;
    private final AtomEventQueryService eventQueryService;

    public BrigadeQueryService(
            JPAQueryFactory queryFactory,
            EmployeeQueryService employeeQueryService,
            AtomEventQueryService eventQueryService) {
        this.queryFactory = queryFactory;
        this.employeeQueryService = employeeQueryService;
        this.eventQueryService = eventQueryService;
    }

    /**
     * Возвращает все бригады с полными данными сотрудников и событий
     * в том порядке, который задан в brigade_employee и brigade_event_order.
     */
    public BrigadesResponseDto getAll() {
        QBrigade b = QBrigade.brigade;
        QBrigadeEmployee be = QBrigadeEmployee.brigadeEmployee;
        QBrigadeEventOrder eo = QBrigadeEventOrder.brigadeEventOrder;

        // 1. Все бригады, отсортированные по id
        List<Long> brigadeIds = queryFactory
                .select(b.id)
                .from(b)
                .orderBy(b.id.asc())
                .fetch();

        if (brigadeIds.isEmpty()) {
            BrigadesResponseDto empty = new BrigadesResponseDto();
            empty.setBrigades(List.of());
            return empty;
        }

        // 2. Получаем tabel'ы сотрудников для каждой бригады в порядке position
        var employeesRows = queryFactory
                .select(be.brigadeId, be.employeeTabel)
                .from(be)
                .where(be.brigadeId.in(brigadeIds))
                .orderBy(be.brigadeId.asc(), be.position.asc())
                .fetch();

        Map<Long, List<Long>> tabelsByBrigade = employeesRows.stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(be.brigadeId),
                        LinkedHashMap::new,
                        Collectors.mapping(t -> t.get(be.employeeTabel), Collectors.toList())
                ));

        // 3. Получаем event_id для каждой бригады в порядке position
        var eventsRows = queryFactory
                .select(eo.brigadeId, eo.eventId)
                .from(eo)
                .where(eo.brigadeId.in(brigadeIds))
                .orderBy(eo.brigadeId.asc(), eo.position.asc())
                .fetch();

        Map<Long, List<Long>> eventIdsByBrigade = eventsRows.stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(eo.brigadeId),
                        LinkedHashMap::new,
                        Collectors.mapping(t -> t.get(eo.eventId), Collectors.toList())
                ));

        // 4. Собираем все уникальные tabel'ы и event_id
        Set<Long> allTabels = tabelsByBrigade.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Set<Long> allEventIds = eventIdsByBrigade.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // 5. Получаем полные данные сотрудников и событий (2 запроса вместо N)
        Map<Long, EmployeeDto> employeesByTabel = employeeQueryService
                .findByTabels(new ArrayList<>(allTabels)).stream()
                .collect(Collectors.toMap(EmployeeDto::getTabel, e -> e));

        Map<Long, EventDto> eventsById = eventQueryService
                .findByIds(new ArrayList<>(allEventIds)).stream()
                .collect(Collectors.toMap(EventDto::getIdEvent, e -> e));

        // 6. Собираем DTO для каждой бригады
        List<BrigadeDto> brigades = brigadeIds.stream()
                .map(id -> {
                    BrigadeDto dto = new BrigadeDto();
                    dto.setBrigadeId(id);

                    // Сотрудники в порядке из brigade_employee.position
                    List<EmployeeDto> employees = tabelsByBrigade
                            .getOrDefault(id, List.of()).stream()
                            .map(employeesByTabel::get)
                            .filter(Objects::nonNull)
                            .toList();
                    dto.setEmployees(employees);

                    // События в порядке из brigade_event_order.position
                    List<EventDto> events = eventIdsByBrigade
                            .getOrDefault(id, List.of()).stream()
                            .map(eventsById::get)
                            .filter(Objects::nonNull)
                            .toList();
                    dto.setEventOrder(events);

                    return dto;
                })
                .toList();

        BrigadesResponseDto response = new BrigadesResponseDto();
        response.setBrigades(brigades);
        return response;
    }
}
