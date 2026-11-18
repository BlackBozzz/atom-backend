package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.InputContextDto;
import com.atomskills.atombackend.dto.caclulate.BrigadesRequestDto;
import com.atomskills.atombackend.dto.caclulate.BrigadesResponseDto;
import com.atomskills.atombackend.reader.PlanningDataReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerService {
    private final BrigadeSaveService brigadeSaveService;
    private final BrigadeQueryService brigadeQueryService;
    private final PlanningDataReader planningDataReader;
    private final CompactPlanner compactPlanner;
    private final ObjectMapper objectMapper;

    /**
     * Забирает данные из БД, запускает оптимизатор,
     * сохраняет результат в БД и возвращает список бригад.
     */
    public BrigadesResponseDto calculateAndSavePlan() {
        log.info("Начало загрузки данных из БД...");
        InputContextDto rawData = planningDataReader.readInputData();
        log.info("Данные загружены. Запуск оптимизации...");

        String resultJson = compactPlanner.solve(rawData);
        log.info("Оптимизация завершена успешно.");

        // Парсим JSON в DTO
        BrigadesRequestDto request = parseBrigadesJson(resultJson);

        // Очищаем старые данные и сохраняем новые
        log.info("Очистка старых данных бригад...");
        brigadeSaveService.clearAll();

        log.info("Сохранение новых бригад в БД...");
        brigadeSaveService.saveAll(request);

        // Возвращаем сохранённые данные
        log.info("Получение сохранённых бригад из БД...");
        return brigadeQueryService.getAll();
    }

    /**
     * Парсит JSON от оптимизатора в BrigadesRequestDto.
     * Ожидается формат:
     * {
     *   "brigades": [
     *     {
     *       "brigade_id": 1,
     *       "employees": [610019, 610026],
     *       "event_order": [880062, 880016]
     *     }
     *   ]
     * }
     */
    private BrigadesRequestDto parseBrigadesJson(String json) {
        try {
            return objectMapper.readValue(json, BrigadesRequestDto.class);
        } catch (Exception e) {
            log.error("Ошибка парсинга JSON бригад: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось распарсить результат оптимизации", e);
        }
    }
}
