package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.DependencyStatusDto;
import com.atomskills.atombackend.dto.EdificeStateDto;
import com.atomskills.atombackend.dto.ResourceStatusDto;
import com.atomskills.atombackend.dto.StatusDto;
import com.atomskills.atombackend.reader.EdificeStatusReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EdificeStatusQueryService {

    private final EdificeStatusReader reader;

    public EdificeStatusQueryService(EdificeStatusReader reader) {
        this.reader = reader;
    }

    public List<EdificeStateDto> findCurrent() {
        return find(null);
    }

    public List<EdificeStateDto> findAt(LocalDateTime at) {
        if (at == null) {
            throw new IllegalArgumentException("at is required");
        }
        return find(at);
    }

    private List<EdificeStateDto> find(LocalDateTime at) {
        List<EdificeStateDto> edifices = reader.findEdifices();
        Map<Integer, Map<String, StatusDto>> statuses = reader.findResourceStatuses(at);
        List<DependencyStatusDto> dependencies = reader.findDependencies();

        Map<Long, EdificeStateDto> byId = new HashMap<>();
        for (EdificeStateDto edifice : edifices) {
            byId.put(edifice.getIdEdifice(), edifice);
            Map<String, StatusDto> resourceStatuses =
                    statuses.getOrDefault(edifice.getIdEdifice(), Map.of());
            for (String type : edifice.getResourceTypes()) {
                edifice.getResources().add(new ResourceStatusDto(type, resourceStatuses.get(type)));
            }
        }

        // Каждая связь видна с обоих концов. Её статус = статус источника по ресурсу связи.
        for (DependencyStatusDto dep : dependencies) {
            StatusDto status = statuses
                    .getOrDefault(dep.getSourceId(), Map.of())
                    .get(dep.getResourceType());
            DependencyStatusDto link = new DependencyStatusDto(
                    dep.getIdDependence(), dep.getResourceType(),
                    dep.getSourceId(), dep.getDependentId(), status);

            EdificeStateDto source = byId.get(link.getSourceId());
            EdificeStateDto dependent = byId.get(link.getDependentId());
            if (source != null) {
                source.getDependencies().add(link);
            }
            if (dependent != null) {
                dependent.getDependencies().add(link);
            }
        }
        return edifices;
    }
}
