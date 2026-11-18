package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.InputContextDto;
import com.atomskills.atombackend.reader.PlanningDataReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerService {
    private final PlanningDataReader planningDataReader;
    private final CompactPlanner compactPlanner;

    /**
     * Забирает данные из БД и сразу отдает их в оптимизатор.
     * Оптимизатор сам сгенерирует бригады и расписание.
     */
    public String calculatePlan() {
        log.info("Начало загрузки данных из БД...");
        InputContextDto rawData = planningDataReader.readInputData();

        log.info("Данные загружены. Запуск оптимизации...");
        String resultJson = compactPlanner.solve(rawData);

        log.info("Оптимизация завершена успешно.");
        return resultJson;
    }
}
