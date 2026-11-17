package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.EventStatusDto;
import com.atomskills.atombackend.entity.QEventStatus;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.argent.servicecommon.querydsl.ArgentQuerydsl.paginate;
import static com.argent.servicecommon.querydsl.ArgentQuerydsl.selectDto;

@Service
public class EventStatusQueryService {

    private final JPAQueryFactory queryFactory;

    public EventStatusQueryService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Page<EventStatusDto> page(Pageable pageable) {
        QEventStatus s = QEventStatus.eventStatus;

        JPAQuery<EventStatusDto> query = queryFactory
                .select(selectDto(EventStatusDto.class,
                        s.idStatus,
                        s.name))
                .from(s);

        return paginate(query, pageable,
                Map.of("name", s.name, "idStatus", s.idStatus));
    }
}