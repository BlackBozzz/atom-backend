package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.EmployeeDto;
import com.atomskills.atombackend.dto.QualificationItemDto;
import com.atomskills.atombackend.entity.QEmployee;
import com.atomskills.atombackend.entity.QQualification;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.argent.servicecommon.querydsl.ArgentQuerydsl.paginate;
import static com.argent.servicecommon.querydsl.ArgentQuerydsl.selectDto;

@Service
public class EmployeeQueryService {

    private final JPAQueryFactory queryFactory;

    public EmployeeQueryService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Page<EmployeeDto> pageWithQualifications(
            Pageable pageable, String lastName, String firstName) {

        QEmployee e = QEmployee.employee;
        QQualification q = QQualification.qualification;

        // 1. Страница рабочих — плоский DTO через selectDto.
        JPAQuery<EmployeeDto> employeesQuery = queryFactory
                .select(selectDto(EmployeeDto.class,
                        e.tabel,
                        e.lastName,
                        e.firstName,
                        e.middleName,
                        e.startDateExperience))
                .from(e)
                .where(
                        lastName  == null || lastName.isBlank()  ? null : e.lastName.eq(lastName),
                        firstName == null || firstName.isBlank() ? null : e.firstName.eq(firstName)
                );

        Page<EmployeeDto> page = paginate(employeesQuery, pageable, Map.of(
                "tabel",     e.tabel,
                "lastName",  e.lastName,
                "firstName", e.firstName
        ));

        if (page.isEmpty()) {
            return page;
        }

        // 2. Квалификации только для tabel'ов текущей страницы — тоже selectDto.
        List<Long> tabels = page.getContent().stream()
                .map(EmployeeDto::getTabel)
                .toList();

        List<QualificationItemDto> qualifications = queryFactory
                .select(selectDto(QualificationItemDto.class,
                        q.employee.tabel.as("tabel"),
                        q.idQualification,
                        q.resourceType,
                        q.proff))
                .from(q)
                .where(q.employee.tabel.in(tabels))
                .orderBy(q.idQualification.asc())
                .fetch();

        // 3. Группировка в памяти — короткая и без Tuple.
        Map<Long, List<QualificationItemDto>> byTabel = qualifications.stream()
                .collect(Collectors.groupingBy(QualificationItemDto::getTabel));

        page.getContent().forEach(dto ->
                dto.setQualifications(byTabel.getOrDefault(dto.getTabel(), List.of())));

        return page;
    }
}
