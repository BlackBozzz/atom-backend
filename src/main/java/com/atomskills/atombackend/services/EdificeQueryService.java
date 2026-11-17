package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.EdificeDto;
import com.atomskills.atombackend.entity.QEdifice;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.argent.servicecommon.querydsl.ArgentQuerydsl.paginate;
import static com.argent.servicecommon.querydsl.ArgentQuerydsl.selectDto;

@Service
public class EdificeQueryService {

    private final JPAQueryFactory queryFactory;

    public EdificeQueryService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Page<EdificeDto> page(Pageable pageable, String district, String street) {
        QEdifice e = QEdifice.edifice;

        JPAQuery<EdificeDto> query = queryFactory
                .select(selectDto(EdificeDto.class,
                        e.idEdifice,
                        e.name,
                        e.resourceType,
                        e.district,
                        e.street,
                        e.houseNumber,
                        e.syncedAt))
                .from(e)
                .where(
                        district == null || district.isBlank() ? null : e.district.eq(district),
                        street   == null || street.isBlank()   ? null : e.street.eq(street)
                );

        return paginate(query, pageable, Map.of(
                "idEdifice",   e.idEdifice,
                "name",        e.name,
                "district",    e.district,
                "street",      e.street,
                "houseNumber", e.houseNumber
        ));
    }
}