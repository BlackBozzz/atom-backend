package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.caclulate.BrigadesRequestDto;
import com.atomskills.atombackend.entity.Brigade;
import com.atomskills.atombackend.entity.BrigadeEmployee;
import com.atomskills.atombackend.entity.BrigadeEventOrder;
import com.atomskills.atombackend.repository.BrigadeEmployeeRepository;
import com.atomskills.atombackend.repository.BrigadeEventOrderRepository;
import com.atomskills.atombackend.repository.BrigadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BrigadeSaveService {

    private final BrigadeRepository brigadeRepository;
    private final BrigadeEmployeeRepository brigadeEmployeeRepository;
    private final BrigadeEventOrderRepository brigadeEventOrderRepository;

    public BrigadeSaveService(
            BrigadeRepository brigadeRepository,
            BrigadeEmployeeRepository brigadeEmployeeRepository,
            BrigadeEventOrderRepository brigadeEventOrderRepository) {
        this.brigadeRepository = brigadeRepository;
        this.brigadeEmployeeRepository = brigadeEmployeeRepository;
        this.brigadeEventOrderRepository = brigadeEventOrderRepository;
    }

    /**
     * Полная очистка всех таблиц бригад.
     * Порядок важен из-за foreign key constraints.
     */
    @Transactional
    public void clearAll() {
        brigadeEmployeeRepository.deleteAll();
        brigadeEventOrderRepository.deleteAll();
        brigadeRepository.deleteAll();
    }

    /**
     * Сохраняет бригады из входящего JSON.
     * Для каждой бригады:
     * 1. Создаёт запись в brigade
     * 2. Вставляет связи brigade_employee с правильным position
     * 3. Вставляет связи brigade_event_order с правильным position
     */
    @Transactional
    public void saveAll(BrigadesRequestDto request) {
        if (request == null || request.getBrigades() == null || request.getBrigades().isEmpty()) {
            return;
        }

        for (BrigadesRequestDto.BrigadeRequestItem item : request.getBrigades()) {
            Long brigadeId = item.getBrigadeId();

            // 1. Сохраняем саму бригаду
            Brigade brigade = new Brigade();
            brigade.setId(brigadeId);
            brigadeRepository.save(brigade);

            // 2. Вставляем связи с сотрудниками
            List<BrigadeEmployee> employees = new ArrayList<>();
            if (item.getEmployees() != null) {
                for (int i = 0; i < item.getEmployees().size(); i++) {
                    BrigadeEmployee be = new BrigadeEmployee();
                    be.setBrigadeId(brigadeId);
                    be.setEmployeeTabel(item.getEmployees().get(i));
                    be.setPosition(i);
                    employees.add(be);
                }
            }
            brigadeEmployeeRepository.saveAll(employees);

            // 3. Вставляем связи с событиями
            List<BrigadeEventOrder> events = new ArrayList<>();
            if (item.getEventOrder() != null) {
                for (int i = 0; i < item.getEventOrder().size(); i++) {
                    BrigadeEventOrder eo = new BrigadeEventOrder();
                    eo.setBrigadeId(brigadeId);
                    eo.setEventId(item.getEventOrder().get(i));
                    eo.setPosition(i);
                    events.add(eo);
                }
            }
            brigadeEventOrderRepository.saveAll(events);
        }
    }

    /**
     * Очищает все таблицы и сохраняет новые данные.
     */
    @Transactional
    public void clearAndSaveAll(BrigadesRequestDto request) {
        clearAll();
        saveAll(request);
    }
}
