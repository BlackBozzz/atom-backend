package com.atomskills.atombackend.dto;

import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.argent.servicecommon.querydsl.ArgentQuerydsl.selectDto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DtoClassContractTest {

    @Test
    void allDtosAndQueueTasksAreMutableClassesWithNoArgsConstructors() throws Exception {
        for (Class<?> type : List.of(
                EdificeDto.class,
                EdificeDependencyDto.class,
                EventDto.class,
                EventStatusDto.class,
                LastEventDto.class,
                EdificeStateDto.class,
                ResourceStatusDto.class,
                DependencyStatusDto.class,
                StatusDto.class,
                EdificeTask.class,
                EdificeDependencyTask.class,
                EventTask.class,
                EventStatusTask.class
        )) {
            assertFalse(type.isRecord(), type.getSimpleName());
            assertNotNull(type.getDeclaredConstructor(), type.getSimpleName());
        }
    }

    @Test
    void queueTasksKeepAllArgsConstructorAndJavaBeanProperties() {
        var task = new EventTask(12, 35, "gas", LocalDateTime.of(2026, 11, 17, 10, 15), (short) 3);
        assertEquals(12, task.getIdEvent());
        task.setIdEdifice(36);
        assertEquals(36, task.getIdEdifice());
        assertEquals("gas", task.getResourceType());
    }

    @Test
    void querydslProjectionPopulatesNestedDtoFields() {
        FactoryExpression<EdificeDependencyDto> projection = selectDto(EdificeDependencyDto.class,
                Expressions.numberPath(Integer.class, "idDependence"),
                Expressions.numberPath(Integer.class, "idEdificeSource").as("source$idEdifice"),
                Expressions.stringPath("sourceName").as("source$name"),
                Expressions.numberPath(Integer.class, "idEdificeDependent").as("dependent$idEdifice"),
                Expressions.stringPath("dependentName").as("dependent$name")
        );

        EdificeDependencyDto dto = projection.newInstance(7, 101, "Source", 202, "Dependent");
        assertEquals(7, dto.getIdDependence());
        assertEquals(101, dto.getSource().getIdEdifice());
        assertEquals("Source", dto.getSource().getName());
        assertEquals(202, dto.getDependent().getIdEdifice());
        assertEquals("Dependent", dto.getDependent().getName());
    }
}
