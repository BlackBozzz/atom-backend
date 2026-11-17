package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.EdificeDependencyDto;
import com.atomskills.atombackend.entity.QEdifice;
import com.atomskills.atombackend.entity.QEdificeDependency;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.argent.servicecommon.querydsl.ArgentQuerydsl.paginate;
import static com.argent.servicecommon.querydsl.ArgentQuerydsl.selectDto;

@Service
public class EdificeDependencyQueryService {

    private final JPAQueryFactory queryFactory;

    public EdificeDependencyQueryService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Page<EdificeDependencyDto> page(Pageable pageable, Integer idSource, Integer idDependent) {
        QEdificeDependency d  = QEdificeDependency.edificeDependency;
        QEdifice            es = QEdifice.edifice;   // source
        QEdifice            ed = new QEdifice("edifice_dependent"); // alias для второго JOIN

        JPAQuery<EdificeDependencyDto> query = queryFactory
                .select(selectDto(EdificeDependencyDto.class,
                        d.idDependence,
                        d.resourceType,
                        d.source.idEdifice.as("idEdificeSource"),
                        es.name.as("sourceName"),
                        d.dependent.idEdifice.as("idEdificeDependent"),
                        ed.name.as("dependentName"),
                        d.syncedAt))
                .from(d)
                .join(d.source, es)
                .join(d.dependent, ed)
                .where(
                        idSource    == null ? null : d.source.idEdifice.eq(idSource),
                        idDependent == null ? null : d.dependent.idEdifice.eq(idDependent)
                );

        return paginate(query, pageable, Map.of(
                "idDependence", d.idDependence,
                "resourceType", d.resourceType
        ));
    }
}