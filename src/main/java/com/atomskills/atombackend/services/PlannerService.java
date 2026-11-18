package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.InputContextDto;
import com.atomskills.atombackend.reader.PlanningDataReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlannerService {
    private final PlanningDataReader planningDataReader;
    private final CompactPlanner compactPlanner;

    /**
     * Основной метод планирования.
     * 1. Забирает данные из БД через ридер.
     * 2. Выполняет оптимизацию в компактном планировщике.
     * 3. Возвращает JSON-строку в требуемом формате.
     */
    public String calculatePlan() {
        // Шаг 1: Получение исходных данных из БД
        InputContextDto rawData = planningDataReader.readInputData();

        // Шаг 2: Расчет и выдача результата
        // Конфликты пока не передаем, так как их нет в БД (или они придут отдельно)
        return compactPlanner.solve(rawData);
    }
}
