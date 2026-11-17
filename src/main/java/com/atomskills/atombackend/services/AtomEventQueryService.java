package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.EventDto;
import com.atomskills.atombackend.entity.QEdifice;
import com.atomskills.atombackend.entity.QEvent;
import com.atomskills.atombackend.entity.QEventStatus;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.argent.servicecommon.querydsl.ArgentQuerydsl.paginate;
import static com.argent.servicecommon.querydsl.ArgentQuerydsl.selectDto;

@Service
public class AtomEventQueryService {

    private final JPAQueryFactory queryFactory;

    public AtomEventQueryService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Page<EventDto> page(Pageable pageable,
                               Integer idEdifice,
                               Short idStatus,
                               LocalDateTime from,
                               LocalDateTime to) {
        QEvent e  = QEvent.event;
        QEdifice ed = QEdifice.edifice;
        QEventStatus s = QEventStatus.eventStatus;

        JPAQuery<EventDto> query = queryFactory
                .select(selectDto(EventDto.class,
                        e.idEvent,
                        e.edifice.idEdifice.as("idEdifice"),
                        ed.name.as("edificeName"),
                        e.resourceType,
                        e.occurredAt,
                        e.status.idStatus.as("idStatus"),
                        s.name.as("statusName"),
                        e.syncedAt))
                .from(e)
                .join(e.edifice, ed)
                .join(e.status, s)
                .where(
                        idEdifice == null ? null : e.edifice.idEdifice.eq(idEdifice),
                        idStatus  == null ? null : e.status.idStatus.eq(idStatus),
                        from      == null ? null : e.occurredAt.goe(from),
                        to        == null ? null : e.occurredAt.loe(to)
                );

        return paginate(query, pageable, Map.of(
                "idEvent",    e.idEvent,
                "occurredAt", e.occurredAt,
                "resourceType", e.resourceType
        ));
    }
}