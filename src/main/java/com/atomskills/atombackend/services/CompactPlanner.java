package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CompactPlanner {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Основной метод расчета. Принимает стандартный InputContextDto.
     */
    public String solve(InputContextDto dbData) {
        // 1. Трансформация во внутреннюю модель
        ProblemModel model = transform(dbData);

        // 2. Решение (GA + Heuristics)
        Solution solution = optimize(model);

        // 3. Форматирование вывода
        return formatOutput(solution);
    }

    // --- Внутренняя бизнес-модель (упрощенная для расчетов) ---

    @Data
    public static class ProblemModel {
        private List<Worker> workers;
        private List<Repair> repairs;
        private double[][] conflictMatrix;
        private Map<Long, Integer> workerIndexMap; // tabel (Long) -> index
    }

    @Data
    public static class Worker {
        private Long tabel; // Изменено на Long
        private Map<String, Double> skills; // WATER/ENERGY -> [0..10]
    }

    @Data
    public static class Repair {
        private Integer id;
        private String resource;
        private double baseDuration;
        private List<Integer> predecessors;
    }

    @Data
    public static class Solution {
        private Map<Integer, List<Long>> brigadeWorkers; // brigadeId -> list of tabels (Long)
        private Map<Integer, List<Integer>> brigadeSchedule; // brigadeId -> list of repairIds
    }

    // --- Логика Трансформации из InputContextDto ---

    private ProblemModel transform(InputContextDto db) {
        ProblemModel model = new ProblemModel();
        if (db.getWorkers() == null || db.getWorkers().isEmpty()) {
            return model;
        }

        // Индексация рабочих
        Map<Long, Integer> wIndex = new HashMap<>();
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i < db.getWorkers().size(); i++) {
            WorkerDto rawW = db.getWorkers().get(i);
            Worker w = new Worker();
            w.setTabel(rawW.getTabel());
            w.setSkills(new HashMap<>());
            workers.add(w);
            wIndex.put(w.getTabel(), i);
        }

        // Заполнение навыков из QualificationDto
        if (db.getQualifications() != null) {
            for (QualificationDto q : db.getQualifications()) {
                Integer idx = wIndex.get(q.getTabel());
                if (idx != null) {
                    workers.get(idx).getSkills().merge(q.getResourceType(), q.getProff().doubleValue(), Math::max);
                }
            }
        }

        // Подготовка починок из RepairCandidateDto
        List<Repair> repairs = new ArrayList<>();
        Map<Integer, Repair> repairMap = new HashMap<>();

        if (db.getRepairs() != null) {
            for (RepairCandidateDto r : db.getRepairs()) {
                Repair rep = new Repair();
                rep.setId(r.getIdEvent());
                rep.setResource(r.getResourceType());

                // Базовая длительность
                double duration = findBaseDuration(r, db.getBaseDurations());
                rep.setBaseDuration(duration);
                rep.setPredecessors(new ArrayList<>());

                repairs.add(rep);
                repairMap.put(rep.getId(), rep);
            }
        }

        // Зависимости из DependencyDto
        if (db.getDependencies() != null) {
            for (DependencyDto d : db.getDependencies()) {
                // Маппинг зависимостей: предполагаем, что в DTO зависимости уже ссылаются на ID починок (idEvent)
                // Если там idEdifice, то нужна дополнительная логика связи Edifice -> Event
                // Для примера используем заглушку, так как точные поля DependencyDto неизвестны
            }
        }

        // Матрица конфликтов (пока пустая)
        int n = workers.size();
        double[][] cMatrix = new double[n][n];

        model.setWorkers(workers);
        model.setRepairs(repairs);
        model.setConflictMatrix(cMatrix);
        model.setWorkerIndexMap(wIndex);
        return model;
    }

    private double findBaseDuration(RepairCandidateDto r, List<BaseDurationDto> durations) {
        if (durations != null) {
            for (BaseDurationDto bd : durations) {
                if (bd.getResourceType().equals(r.getResourceType()) && bd.getIdEdifice().equals(r.getIdEdifice())) {
                    return bd.getAvgResolutionSeconds().doubleValue();
                }
            }
        }
        return 3600.0; // Дефолт 1 час
    }

    // --- Логика Оптимизации (Greedy) ---

    private Solution optimize(ProblemModel model) {
        int nWorkers = model.getWorkers().size();
        int nRepairs = model.getRepairs().size();

        if (nWorkers == 0 || nRepairs == 0) return new Solution();

        // 1. Жадное разбиение на бригады (по 2-5 человек)
        List<List<Integer>> partitions = new ArrayList<>();
        List<Integer> indices = IntStream.range(0, nWorkers).boxed().collect(Collectors.toList());

        int size = 2;
        for (int i = 0; i < nWorkers; i += size) {
            int end = Math.min(i + size, nWorkers);
            if (end - i < 2 && !partitions.isEmpty()) {
                partitions.get(partitions.size() - 1).add(indices.get(i));
            } else if (end - i >= 2) {
                partitions.add(indices.subList(i, end));
            }
        }

        Solution sol = new Solution();
        sol.setBrigadeWorkers(new HashMap<>());
        sol.setBrigadeSchedule(new HashMap<>());

        for (int bIdx = 0; bIdx < partitions.size(); bIdx++) {
            int bId = bIdx + 1;
            List<Integer> wIndices = partitions.get(bIdx);

            // Получаем табельные номера (Long)
            List<Long> tabels = wIndices.stream()
                    .map(idx -> model.getWorkers().get(idx).getTabel())
                    .collect(Collectors.toList());

            sol.getBrigadeWorkers().put(bId, tabels);
            sol.getBrigadeSchedule().put(bId, new ArrayList<>());
        }

        // Распределение задач (жадно по длительности)
        List<Repair> sortedRepairs = new ArrayList<>(model.getRepairs());
        sortedRepairs.sort((r1, r2) -> Double.compare(r2.getBaseDuration(), r1.getBaseDuration()));

        for (Repair r : sortedRepairs) {
            int bestBrigade = -1;
            double minTime = Double.MAX_VALUE;

            for (int bId : sol.getBrigadeWorkers().keySet()) {
                List<Integer> wIndicesInBrigade = partitions.get(bId - 1);

                boolean hasSkill = wIndicesInBrigade.stream()
                        .anyMatch(idx -> model.getWorkers().get(idx).getSkills().getOrDefault(r.getResource(), 0.0) > 0);

                if (!hasSkill) continue;

                double time = calculateDuration(model, wIndicesInBrigade, r);
                if (time < minTime) {
                    minTime = time;
                    bestBrigade = bId;
                }
            }

            if (bestBrigade != -1) {
                sol.getBrigadeSchedule().get(bestBrigade).add(r.getId());
            }
        }

        return sol;
    }

    private double calculateDuration(ProblemModel model, List<Integer> wIndices, Repair r) {
        double qSum = 0;
        double maxConflict = 0;

        for (int i = 0; i < wIndices.size(); i++) {
            int idx = wIndices.get(i);
            qSum += model.getWorkers().get(idx).getSkills().getOrDefault(r.getResource(), 0.0) / 5.0;

            for (int j = i + 1; j < wIndices.size(); j++) {
                int idx2 = wIndices.get(j);
                double c = model.getConflictMatrix()[idx][idx2];
                if (c > maxConflict) maxConflict = c;
            }
        }

        if (qSum == 0) return Double.MAX_VALUE;
        return (1 + maxConflict) * (r.getBaseDuration() / qSum);
    }

    // --- Форматирование Вывода ---

    private String formatOutput(Solution sol) {
        try {
            // Структура для JSON
            Map<String, Object> root = new LinkedHashMap<>();
            List<Map<String, Object>> brigadesList = new ArrayList<>();

            for (Map.Entry<Integer, List<Long>> entry : sol.getBrigadeWorkers().entrySet()) {
                int bId = entry.getKey();
                List<Integer> schedule = sol.getBrigadeSchedule().getOrDefault(bId, Collections.emptyList());

                List<Long> sortedEmps = new ArrayList<>(entry.getValue());
                Collections.sort(sortedEmps);

                Map<String, Object> b = new LinkedHashMap<>();
                b.put("brigade_id", bId);
                b.put("employees", sortedEmps);
                b.put("event_order", schedule);

                brigadesList.add(b);
            }

            brigadesList.sort(Comparator.comparingInt(b -> (Integer) b.get("brigade_id")));
            root.put("brigades", brigadesList);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("JSON Error", e);
        }
    }
}