package com.atomskills.atombackend.services;

import com.atomskills.atombackend.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompactPlanner {

    private static final double DEFAULT_CONFLICT = 0.9;
    private static final double INF = Double.MAX_VALUE / 4;
    private static final int MIN_BRIGADE_SIZE = 2;
    private static final int MAX_BRIGADE_SIZE = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Единственная точка входа. Принимает только сырые данные из БД.
     */
    public String solve(InputContextDto dbData) {
        ProblemModel model = transform(dbData);
        Solution solution = optimize(model);

        log.info("Расчет завершен: makespan={} сек, бригад={}, распределено задач={}",
                Math.round(solution.getMakespan()),
                solution.getBrigadeWorkers().size(),
                solution.getBrigadeSchedule().values().stream().mapToInt(List::size).sum());

        return formatOutput(solution);
    }

    // ======================= МОДЕЛИ ДАННЫХ =======================

    @Data
    public static class ProblemModel {
        private List<Worker> workers;
        private List<Repair> repairs;
        private double[][] conflictMatrix;
        private Map<Long, Integer> workerIndexMap;
    }

    @Data
    public static class Worker {
        private Long tabel;
        private Map<String, Double> skills; // "WATER" / "ENERGY" -> proff / 5.0
    }

    @Data
    public static class Repair {
        private Long id;
        private Long edificeId;
        private String resource;
        private double baseDuration;
        private List<Long> predecessors;
    }

    @Data
    public static class Solution {
        private Map<Integer, List<Long>> brigadeWorkers = new LinkedHashMap<>();
        private Map<Integer, List<Long>> brigadeSchedule = new LinkedHashMap<>();
        private double makespan;
    }

    // ======================= ТРАНСФОРМАЦИЯ =======================

    private ProblemModel transform(InputContextDto db) {
        ProblemModel model = new ProblemModel();
        List<WorkerDto> rawWorkers = db.getWorkers() == null ? List.of() : db.getWorkers();
        if (rawWorkers.isEmpty()) throw new IllegalStateException("Нет данных о рабочих");

        Map<Long, Integer> wIndex = new LinkedHashMap<>();
        List<Worker> workers = new ArrayList<>();
        for (WorkerDto rw : rawWorkers) {
            if (wIndex.containsKey(rw.getTabel())) continue;
            Worker w = new Worker();
            w.setTabel(rw.getTabel());
            w.setSkills(new HashMap<>());
            wIndex.put(w.getTabel(), workers.size());
            workers.add(w);
        }

        if (db.getQualifications() != null) {
            for (QualificationDto q : db.getQualifications()) {
                Integer idx = wIndex.get(q.getTabel());
                if (idx == null || q.getProff() == null) continue;
                String res = normalizeResource(q.getResourceType());
                double normalizedSkill = q.getProff().doubleValue() / 5.0;
                workers.get(idx).getSkills().merge(res, normalizedSkill, Math::max);
            }
        }

        // Длительности
        Map<String, Long> exactDurations = new HashMap<>();
        Map<String, List<Long>> avgByResource = new HashMap<>();
        if (db.getBaseDurations() != null) {
            for (BaseDurationDto bd : db.getBaseDurations()) {
                if (bd.getAvgResolutionSeconds() == null || bd.getAvgResolutionSeconds() <= 0) continue;
                String res = normalizeResource(bd.getResourceType());
                exactDurations.put(bd.getIdEdifice() + "|" + res, bd.getAvgResolutionSeconds());
                avgByResource.computeIfAbsent(res, k -> new ArrayList<>()).add(bd.getAvgResolutionSeconds());
            }
        }

        List<Repair> repairs = new ArrayList<>();
        Map<Long, Repair> repairById = new LinkedHashMap<>();
        Map<String, Long> eventByEdificeRes = new HashMap<>();

        if (db.getRepairs() != null) {
            for (RepairCandidateDto r : db.getRepairs()) {
                if (repairById.containsKey(r.getIdEvent())) continue;
                Repair rep = new Repair();
                rep.setId(r.getIdEvent());
                rep.setEdificeId(r.getIdEdifice());
                rep.setResource(normalizeResource(r.getResourceType()));

                String res = rep.getResource();
                Long sec = exactDurations.get(r.getIdEdifice() + "|" + res);
                if (sec == null) {
                    List<Long> lst = avgByResource.get(res);
                    sec = lst == null || lst.isEmpty() ? 3600L
                            : Math.round(lst.stream().mapToLong(Long::longValue).average().orElse(3600));
                }
                rep.setBaseDuration(sec);
                rep.setPredecessors(new ArrayList<>());

                repairs.add(rep);
                repairById.put(rep.getId(), rep);
                eventByEdificeRes.put(r.getIdEdifice() + "|" + res, rep.getId());
            }
        }

        // Зависимости
        if (db.getDependencies() != null) {
            for (DependencyDto d : db.getDependencies()) {
                String res = normalizeResource(d.getResourceType());
                Long src = eventByEdificeRes.get(d.getIdEdificeSource() + "|" + res);
                Long dst = eventByEdificeRes.get(d.getIdEdificeDependent() + "|" + res);
                if (src == null || dst == null || src.equals(dst)) continue;

                Repair target = repairById.get(dst);
                if (target != null && !target.getPredecessors().contains(src)) {
                    target.getPredecessors().add(src);
                }
            }
        }

        // Матрица конфликтов: 0.9 по умолчанию
        int n = workers.size();
        double[][] c = new double[n][n];
        for (int i = 0; i < n; i++) Arrays.fill(c[i], DEFAULT_CONFLICT);

        if (db.getConflictMatrix() != null) {
            for (Object o : db.getConflictMatrix()) {
                if (o instanceof Map<?, ?> m) {
                    Long a = ((Number) m.get("tabelA")).longValue();
                    Long b = ((Number) m.get("tabelB")).longValue();
                    double v = ((Number) m.get("conflict")).doubleValue();
                    Integer i = wIndex.get(a), j = wIndex.get(b);
                    if (i != null && j != null) {
                        c[i][j] = v;
                        c[j][i] = v;
                    }
                }
            }
        }

        model.setWorkers(workers);
        model.setRepairs(repairs);
        model.setConflictMatrix(c);
        model.setWorkerIndexMap(wIndex);
        return model;
    }

    private String normalizeResource(String r) {
        if (r == null) return "UNKNOWN";
        String s = r.trim().toUpperCase();
        if (s.equals("ELECTRICITY") || s.equals("POWER") || s.equals("ENERGY")) return "ENERGY";
        return s; // WATER или другое
    }

    // ======================= ОПТИМИЗАЦИЯ =======================

    private Solution optimize(ProblemModel m) {
        int n = m.getWorkers().size();
        int R = m.getRepairs().size();
        if (R == 0) return new Solution(); // Нет задач

        // 1. Генерация разбиения на бригады (2..5 чел, баланс навыков)
        List<List<Integer>> partitions = generateBalancedPartitions(m, n);
        int B = partitions.size();

        // 2. Индексация и подготовка данных для планирования
        Map<Long, Integer> rIdx = new HashMap<>();
        int[] res = new int[R];
        double[] base = new double[R];
        for (int i = 0; i < R; i++) {
            rIdx.put(m.getRepairs().get(i).getId(), i);
            res[i] = m.getRepairs().get(i).getResource().equals("WATER") ? 0 : 1;
            base[i] = m.getRepairs().get(i).getBaseDuration();
        }

        int[][] preds = new int[R][];
        List<List<Integer>> succ = new ArrayList<>();
        for (int i = 0; i < R; i++) succ.add(new ArrayList<>());

        for (int i = 0; i < R; i++) {
            List<Integer> pList = new ArrayList<>();
            for (Long pId : m.getRepairs().get(i).getPredecessors()) {
                Integer p = rIdx.get(pId);
                if (p != null) {
                    pList.add(p);
                    succ.get(p).add(i);
                }
            }
            preds[i] = pList.stream().mapToInt(Integer::intValue).toArray();
        }

        // Топологическая сортировка
        int[] deg = new int[R];
        for (int i = 0; i < R; i++) deg[i] = preds[i].length;
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < R; i++) if (deg[i] == 0) queue.add(i);

        int[] topo = new int[R];
        int k = 0;
        while (!queue.isEmpty()) {
            int v = queue.poll();
            topo[k++] = v;
            for (int u : succ.get(v)) if (--deg[u] == 0) queue.add(u);
        }
        if (k != R) throw new IllegalStateException("Граф зависимостей содержит цикл!");

        // Расчет длительностей T[brigade][repair]
        double[][] dur = new double[B][R];
        double[] maxConflict = new double[B];
        double[][] qs = new double[B][2]; // [brigade][resource]

        for (int b = 0; b < B; b++) {
            List<Integer> members = partitions.get(b);
            double mxC = 0;
            for (int i = 0; i < members.size(); i++) {
                int wi = members.get(i);
                qs[b][0] += m.getWorkers().get(wi).getSkills().getOrDefault("WATER", 0.0);
                qs[b][1] += m.getWorkers().get(wi).getSkills().getOrDefault("ENERGY", 0.0);
                for (int j = i + 1; j < members.size(); j++) {
                    mxC = Math.max(mxC, m.getConflictMatrix()[wi][members.get(j)]);
                }
            }
            maxConflict[b] = mxC;
            for (int i = 0; i < R; i++) {
                double skillSum = qs[b][res[i]];
                dur[b][i] = skillSum > 0 ? (1 + maxConflict[b]) * base[i] / skillSum : INF;
            }
        }

        // 3. Двухфазное назначение задач
        int[] assigned = new int[R];
        Arrays.fill(assigned, -1);
        List<List<Integer>> orders = new ArrayList<>();
        for (int b = 0; b < B; b++) orders.add(new ArrayList<>());

        // Фаза 1: Гарантируем минимум 1 задачу на каждую бригаду
        for (int b = 0; b < B; b++) {
            boolean found = false;
            for (int i : topo) {
                if (assigned[i] >= 0) continue;
                if (dur[b][i] >= INF) continue; // Нет навыка

                // Проверяем, назначены ли предшественники
                boolean predsOk = true;
                for (int p : preds[i]) {
                    if (assigned[p] < 0) { predsOk = false; break; }
                }

                if (predsOk) {
                    assigned[i] = b;
                    orders.get(b).add(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("Невозможно назначить ни одной задачи бригаде " + (b + 1) +
                        ". Проверьте, есть ли в ней специалисты под доступные задачи.");
            }
        }

        // Фаза 2: Жадное распределение остальных задач по минимальному времени завершения (Earliest Finish)
        double[] freeTime = new double[B];
        double[] endTime = new double[R];
        Arrays.fill(endTime, -1);

        // Инициализируем время для задач из Фазы 1
        for (int b = 0; b < B; b++) {
            for (int i : orders.get(b)) {
                double ready = 0;
                for (int p : preds[i]) ready = Math.max(ready, endTime[p]);
                double start = Math.max(freeTime[b], ready);
                endTime[i] = start + dur[b][i];
                freeTime[b] = endTime[i];
            }
        }

        // Распределяем оставшиеся
        for (int i : topo) {
            if (assigned[i] >= 0) continue;

            double ready = 0;
            for (int p : preds[i]) ready = Math.max(ready, endTime[p]);

            int bestB = -1;
            double bestFinish = INF;

            for (int b = 0; b < B; b++) {
                if (dur[b][i] >= INF) continue;
                double finish = Math.max(freeTime[b], ready) + dur[b][i];
                if (finish < bestFinish) {
                    bestFinish = finish;
                    bestB = b;
                }
            }

            if (bestB < 0) {
                throw new IllegalStateException("Задача " + m.getRepairs().get(i).getId() + " не может быть выполнена: нет квалифицированных бригад.");
            }

            assigned[i] = bestB;
            orders.get(bestB).add(i);
            endTime[i] = bestFinish;
            freeTime[bestB] = bestFinish;
        }

        // 4. Формирование ответа
        Solution sol = new Solution();
        double makespan = 0;
        for (int b = 0; b < B; b++) {
            List<Long> tabels = partitions.get(b).stream()
                    .map(wi -> m.getWorkers().get(wi).getTabel())
                    .sorted()
                    .collect(Collectors.toList());
            sol.getBrigadeWorkers().put(b + 1, tabels);

            List<Long> ids = orders.get(b).stream()
                    .map(i -> m.getRepairs().get(i).getId())
                    .collect(Collectors.toList());
            sol.getBrigadeSchedule().put(b + 1, ids);

            makespan = Math.max(makespan, freeTime[b]);
        }
        sol.setMakespan(makespan);
        return sol;
    }

    /**
     * Генерирует сбалансированное разбиение рабочих на бригады (2..5 чел).
     * Использует сортировку по суммарной квалификации и распределение "змейкой"
     * для равномерного разброса сильных специалистов.
     */
    private List<List<Integer>> generateBalancedPartitions(ProblemModel m, int n) {
        // Оцениваем "силу" каждого рабочего (сумма нормализованных навыков)
        Double[] scores = new Double[n];
        for (int i = 0; i < n; i++) {
            double s = m.getWorkers().get(i).getSkills().getOrDefault("WATER", 0.0) +
                    m.getWorkers().get(i).getSkills().getOrDefault("ENERGY", 0.0);
            scores[i] = s;
        }

        // Сортируем индексы рабочих по убыванию силы
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));

        // Определяем количество бригад (целимся в ~3-4 человека, но строго 2..5)
        int targetSize = 3;
        int numBrigades = Math.max(1, (int) Math.ceil((double) n / targetSize));

        // Корректировка, чтобы не нарушить лимит 5
        while (n > numBrigades * MAX_BRIGADE_SIZE) {
            numBrigades++;
        }
        // Корректировка, чтобы не нарушить лимит 2
        while (n < numBrigades * MIN_BRIGADE_SIZE && numBrigades > 1) {
            numBrigades--;
        }

        List<List<Integer>> partitions = new ArrayList<>();
        for (int b = 0; b < numBrigades; b++) {
            partitions.add(new ArrayList<>());
        }

        // Распределение "змейкой" для баланса
        for (int t = 0; t < n; t++) {
            int row = t / numBrigades;
            int col = t % numBrigades;
            int b = (row % 2 == 0) ? col : (numBrigades - 1 - col);
            partitions.get(b).add(order[t]);
        }

        // Финальная проверка и корректировка размеров (если "змейка" дала сбой на краях)
        // Перераспределяем из переполненных в недогруженные
        boolean changed;
        do {
            changed = false;
            for (int b = 0; b < numBrigades; b++) {
                if (partitions.get(b).size() > MAX_BRIGADE_SIZE) {
                    // Ищем бригаду с размером < MAX и забираем оттуда человека
                    for (int other = 0; other < numBrigades; other++) {
                        if (partitions.get(other).size() < MAX_BRIGADE_SIZE) {
                            int worker = partitions.get(b).remove(partitions.get(b).size() - 1);
                            partitions.get(other).add(worker);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        } while (changed);

        return partitions;
    }

    // ======================= ВЫВОД =======================

    private String formatOutput(Solution sol) {
        try {
            List<Map<String, Object>> brigades = new ArrayList<>();
            for (Map.Entry<Integer, List<Long>> e : sol.getBrigadeWorkers().entrySet()) {
                int bId = e.getKey();
                List<Long> order = sol.getBrigadeSchedule().getOrDefault(bId, List.of());

                Map<String, Object> b = new LinkedHashMap<>();
                b.put("brigade_id", bId);
                b.put("employees", e.getValue());
                b.put("event_order", order);
                brigades.add(b);
            }

            // Сортировка по ID бригады
            brigades.sort(Comparator.comparingInt(b -> (Integer) b.get("brigade_id")));

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("brigades", brigades);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации JSON", e);
        }
    }
}