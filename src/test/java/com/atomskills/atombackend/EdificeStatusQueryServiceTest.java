package com.atomskills.atombackend;

import com.atomskills.atombackend.controller.EdificeCurrentStatusController;
import com.atomskills.atombackend.controller.EdificeStatusAtController;
import com.atomskills.atombackend.dto.DependencyStatusDto;
import com.atomskills.atombackend.dto.EdificeStateDto;
import com.atomskills.atombackend.dto.ResourceStatusDto;
import com.atomskills.atombackend.dto.StatusDto;
import com.atomskills.atombackend.reader.EdificeStatusReader;
import com.atomskills.atombackend.services.EdificeStatusQueryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdificeStatusQueryServiceTest {

    private static EdificeStateDto edifice(int id, String... resources) {
        return new EdificeStateDto(id, "Здание " + id, List.of(resources),
                "Центр", "Мира", "1", new ArrayList<>(), new ArrayList<>());
    }

    @Test
    void currentReturnsIndependentStatusesAndAllEdgeDirections() {
        EdificeStatusReader reader = mock(EdificeStatusReader.class);
        EdificeStatusQueryService service = new EdificeStatusQueryService(reader);
        EdificeCurrentStatusController controller = new EdificeCurrentStatusController(service);
        LocalDateTime eventAt = LocalDateTime.of(2026, 11, 18, 9, 30);
        StatusDto sourceElectricity = new StatusDto((short) 1, "Норма", eventAt);
        StatusDto sourceWater = new StatusDto((short) 3, "Авария", eventAt);
        when(reader.findEdifices()).thenReturn(List.of(edifice(7, "water", "electricity"),
                edifice(8, "water", "electricity"), edifice(9, "electricity")));
        when(reader.findResourceStatuses(null)).thenReturn(Map.of(
                7, Map.of("water", sourceWater, "electricity", sourceElectricity)));
        when(reader.findDependencies()).thenReturn(List.of(
                new DependencyStatusDto(18, "water", 7, 8, null),
                new DependencyStatusDto(19, "electricity", 7, 9, null),
                new DependencyStatusDto(20, "electricity", 9, 8, null)));

        List<EdificeStateDto> result = controller.current();
        assertEquals(3, result.size());
        EdificeStateDto source = result.get(0);
        assertEquals("water", source.getResources().get(0).getResourceType());
        assertSame(sourceWater, source.getResources().get(0).getStatus());
        assertSame(sourceElectricity, source.getResources().get(1).getStatus());
        assertEquals(2, source.getDependencies().size());
        assertSame(sourceWater, source.getDependencies().get(0).getStatus());
        assertSame(sourceElectricity, source.getDependencies().get(1).getStatus());
        assertEquals(7, source.getDependencies().get(0).getSourceId());
        assertEquals(8, source.getDependencies().get(0).getDependentId());

        EdificeStateDto dependent = result.get(1);
        assertNull(dependent.getResources().get(0).getStatus());
        assertEquals(2, dependent.getDependencies().size());
        // Та же входящая связь присутствует и у получателя.
        assertEquals(18, dependent.getDependencies().get(0).getIdDependence());
        assertSame(sourceWater, dependent.getDependencies().get(0).getStatus());
        // Источник 9 не имеет событий, поэтому статус связи не выдумывается.
        assertNull(dependent.getDependencies().get(1).getStatus());
        verify(reader).findResourceStatuses(null);
    }

    @Test
    void historicalControllerForwardsExactTimestampAndKeepsUnknownStatuses() {
        EdificeStatusReader reader = mock(EdificeStatusReader.class);
        EdificeStatusQueryService service = new EdificeStatusQueryService(reader);
        EdificeStatusAtController controller = new EdificeStatusAtController(service);
        LocalDateTime at = LocalDateTime.of(2026, 11, 18, 12, 30, 45);
        when(reader.findEdifices()).thenReturn(List.of(edifice(5, "electricity")));
        when(reader.findResourceStatuses(at)).thenReturn(Map.of());
        when(reader.findDependencies()).thenReturn(List.of());

        List<EdificeStateDto> result = controller.at(at);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getResources().size());
        assertNull(result.get(0).getResources().get(0).getStatus());
        assertTrue(result.get(0).getDependencies().isEmpty());
        verify(reader).findResourceStatuses(at);
    }

    @Test
    void historicalServiceRejectsNullDate() {
        EdificeStatusQueryService service = new EdificeStatusQueryService(mock(EdificeStatusReader.class));
        assertThrows(IllegalArgumentException.class, () -> service.findAt(null));
    }
}
