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

    private static final int MIN_SIZE = 2, MAX_SIZE = 5, WATER = 0, ENERGY = 1;
    private static final double DEFAULT_CONFLICT = 0.9;
    private static final long TIME_BUDGET_MS = 15_000L;
    private static final double INF = Double.MAX_VALUE / 8, EPS = 1e-9;

    private final ObjectMapper mapper = new ObjectMapper();

    public String solve(InputContextDto db) {
        ProblemModel m = transform(db);
        Solver solver = new Solver(m);
        Solution s = solver.search(TIME_BUDGET_MS);
        double mk = solver.verify(s);            // независимая проверка ВСЕХ правил
        s.setMakespan(mk);
        log.info("План: бригад={}, починок={}, makespan={} сек ({} ч)",
                s.getMembers().size(), m.getRepairs().size(), Math.round(mk),
                String.format(Locale.US, "%.2f", mk / 3600.0));
        return write(m, s);
    }

    // ==================== МОДЕЛИ ====================

    @Data public static class Worker {
        private Long tabel;
        private double[] skill = new double[2];   // уже proff/5, отсутствие = 0
    }

    @Data public static class Repair {
        private Long id;
        private Long edificeId;
        private int resource;
        private double baseDuration;
        private List<Long> predecessors = new ArrayList<>();
    }

    @Data public static class ProblemModel {
        private List<Worker> workers = new ArrayList<>();
        private List<Repair> repairs = new ArrayList<>();
        private double[][] conflict;
    }

    @Data public static class Solution {
        private List<List<Integer>> members = new ArrayList<>();  // brigade -> индексы рабочих
        private List<List<Integer>> orders  = new ArrayList<>();  // brigade -> индексы починок по порядку
        private double makespan;
    }

    private static final class Eval {
        double mk; List<List<Integer>> orders; int[] assign;
    }

    private static <T> List<T> nz(List<T> l) { return l == null ? Collections.emptyList() : l; }

    private static int res(String raw) {
        String s = raw == null ? "" : raw.trim().toUpperCase();
        if (s.equals("WATER")) return WATER;
        if (s.equals("ENERGY") || s.equals("ELECTRICITY") || s.equals("POWER")) return ENERGY;
        throw new IllegalArgumentException("Неизвестный тип ресурса: " + raw);
    }

    // ==================== ТРАНСФОРМАЦИЯ ====================

    private ProblemModel transform(InputContextDto db) {
        ProblemModel m = new ProblemModel();
        Map<Long, Integer> wi = new LinkedHashMap<>();
        for (WorkerDto w : nz(db.getWorkers())) {
            if (w.getTabel() == null || wi.containsKey(w.getTabel())) continue;
            Worker x = new Worker();
            x.setTabel(w.getTabel());
            wi.put(x.getTabel(), m.getWorkers().size());
            m.getWorkers().add(x);
        }
        if (m.getWorkers().size() < MIN_SIZE)
            throw new IllegalStateException("Меньше двух рабочих — бригаду не собрать");

        for (QualificationDto q : nz(db.getQualifications())) {
            Integer i = wi.get(q.getTabel());
            if (i == null || q.getProff() == null) continue;
            int r = res(q.getResourceType());
            double v = Math.max(0.0, Math.min(10.0, q.getProff().doubleValue())) / 5.0;
            double[] sk = m.getWorkers().get(i).getSkill();
            sk[r] = Math.max(sk[r], v);
        }

        // базовые длительности
        Map<String, Long> exact = new HashMap<>();
        Map<Integer, List<Long>> byRes = new HashMap<>();
        for (BaseDurationDto d : nz(db.getBaseDurations())) {
            if (d.getAvgResolutionSeconds() == null || d.getAvgResolutionSeconds() <= 0) continue;
            int r = res(d.getResourceType());
            exact.put(d.getIdEdifice() + "|" + r, d.getAvgResolutionSeconds());
            byRes.computeIfAbsent(r, k -> new ArrayList<>()).add(d.getAvgResolutionSeconds());
        }

        // инциденты: один на (объект, ресурс)
        Map<String, Integer> incidentIdx = new LinkedHashMap<>();
        for (RepairCandidateDto c : nz(db.getRepairs())) {
            if (c.getIdEvent() == null || c.getIdEdifice() == null) continue;
            int r = res(c.getResourceType());
            String key = c.getIdEdifice() + "|" + r;
            if (incidentIdx.containsKey(key)) continue;
            Repair rp = new Repair();
            rp.setId(c.getIdEvent());
            rp.setEdificeId(c.getIdEdifice());
            rp.setResource(r);
            Long sec = exact.get(key);
            if (sec == null) {
                List<Long> l = byRes.get(r);
                sec = (l == null || l.isEmpty()) ? 3600L
                        : Math.round(l.stream().mapToLong(Long::longValue).average().orElse(3600));
            }
            rp.setBaseDuration(sec);
            incidentIdx.put(key, m.getRepairs().size());
            m.getRepairs().add(rp);
        }

        // граф объектов ПО РЕСУРСУ + транзитивное замыкание
        Map<Integer, Map<Long, Set<Long>>> parents = new HashMap<>();
        for (DependencyDto d : nz(db.getDependencies())) {
            if (d.getIdEdificeSource() == null || d.getIdEdificeDependent() == null) continue;
            int r = res(d.getResourceType());
            parents.computeIfAbsent(r, k -> new HashMap<>())
                    .computeIfAbsent(d.getIdEdificeDependent(), k -> new LinkedHashSet<>())
                    .add(d.getIdEdificeSource());
        }
        Map<String, Set<Long>> cache = new HashMap<>();
        for (Map.Entry<String, Integer> e : incidentIdx.entrySet()) {
            int childIdx = e.getValue();
            Repair child = m.getRepairs().get(childIdx);
            int r = child.getResource();
            Set<Long> anc = ancestors(r, child.getEdificeId(), parents, cache, new LinkedHashSet<>());
            for (Long a : anc) {
                Integer p = incidentIdx.get(a + "|" + r);   // ТОЛЬКО тот же ресурс
                if (p != null && p.intValue() != childIdx && !child.getPredecessors().contains(m.getRepairs().get(p).getId()))
                    child.getPredecessors().add(m.getRepairs().get(p).getId());
            }
        }

        // конфликты: 0.9 всем парам, точечные переопределения из конфига
        int n = m.getWorkers().size();
        double[][] c = new double[n][n];
        for (int i = 0; i < n; i++) { Arrays.fill(c[i], DEFAULT_CONFLICT); c[i][i] = 0.0; }
        for (Object o : nz(db.getConflictMatrix())) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> mm = (Map<?, ?>) o;
            Object a = mm.get("tabelA"), b = mm.get("tabelB"), v = mm.get("conflict");
            if (!(a instanceof Number) || !(b instanceof Number) || !(v instanceof Number)) continue;
            Integer i = wi.get(((Number) a).longValue()), j = wi.get(((Number) b).longValue());
            if (i == null || j == null) continue;
            double val = Math.max(0.0, ((Number) v).doubleValue());
            c[i][j] = val; c[j][i] = val;
        }
        m.setConflict(c);
        return m;
    }

    /** Все предки объекта по ресурсу (транзитивно), с детекцией циклов. */
    private Set<Long> ancestors(int r, Long node, Map<Integer, Map<Long, Set<Long>>> parents,
                                Map<String, Set<Long>> cache, Set<Long> path) {
        String key = r + "|" + node;
        Set<Long> done = cache.get(key);
        if (done != null) return done;
        if (!path.add(node))
            throw new IllegalStateException("Цикл зависимостей: ресурс " + r + ", объект " + node);
        Set<Long> acc = new LinkedHashSet<>();
        for (Long p : parents.getOrDefault(r, Collections.emptyMap()).getOrDefault(node, Collections.emptySet())) {
            acc.add(p);
            acc.addAll(ancestors(r, p, parents, cache, path));
        }
        path.remove(node);
        cache.put(key, acc);
        return acc;
    }

    // ==================== ЯДРО ====================

    private final class Solver {
        final ProblemModel m;
        final int n, R;
        final double[][] q, conf;
        final int[] res, topo;
        final double[] base, level;
        final int[][] preds, succ;
        final boolean[] skilled, needRes = new boolean[2];
        final int skilledCount, coverBound;
        final Random rnd = new Random(20261118L);

        Solver(ProblemModel m) {
            this.m = m;
            this.n = m.getWorkers().size();
            this.R = m.getRepairs().size();
            this.conf = m.getConflict();
            q = new double[n][];
            for (int i = 0; i < n; i++) q[i] = m.getWorkers().get(i).getSkill();

            res = new int[R]; base = new double[R];
            Map<Long, Integer> idx = new HashMap<>();
            for (int i = 0; i < R; i++) {
                Repair rp = m.getRepairs().get(i);
                idx.put(rp.getId(), i);
                res[i] = rp.getResource();
                base[i] = rp.getBaseDuration();
                needRes[res[i]] = true;
            }

            preds = new int[R][]; succ = new int[R][];
            List<List<Integer>> sc = new ArrayList<>();
            for (int i = 0; i < R; i++) sc.add(new ArrayList<>());
            int[] deg = new int[R];
            for (int i = 0; i < R; i++) {
                List<Integer> pl = new ArrayList<>();
                for (Long pid : m.getRepairs().get(i).getPredecessors()) {
                    Integer p = idx.get(pid);
                    if (p != null && p != i) { pl.add(p); sc.get(p).add(i); }
                }
                preds[i] = pl.stream().mapToInt(Integer::intValue).toArray();
                deg[i] = preds[i].length;
            }
            for (int i = 0; i < R; i++) succ[i] = sc.get(i).stream().mapToInt(Integer::intValue).toArray();

            Deque<Integer> dq = new ArrayDeque<>();
            for (int i = 0; i < R; i++) if (deg[i] == 0) dq.add(i);
            topo = new int[R];
            int k = 0;
            while (!dq.isEmpty()) {
                int v = dq.poll(); topo[k++] = v;
                for (int u : succ[v]) if (--deg[u] == 0) dq.add(u);
            }
            if (k != R) throw new IllegalStateException("Цикл в зависимостях починок");

            level = new double[R];
            for (int t = R - 1; t >= 0; t--) {
                int v = topo[t]; double mx = 0;
                for (int u : succ[v]) mx = Math.max(mx, level[u]);
                level[v] = base[v] + mx;
            }

            int[] wRes = new int[2], jRes = new int[2];
            for (int i = 0; i < n; i++) { if (q[i][0] > 0) wRes[0]++; if (q[i][1] > 0) wRes[1]++; }
            for (int i = 0; i < R; i++) jRes[res[i]]++;
            for (int r = 0; r < 2; r++)
                if (needRes[r] && wRes[r] == 0)
                    throw new IllegalStateException("Нет ни одного специалиста по ресурсу " + (r == WATER ? "WATER" : "ENERGY"));

            skilled = new boolean[n];
            int cnt = 0;
            for (int i = 0; i < n; i++) {
                skilled[i] = (needRes[WATER] && q[i][WATER] > 0) || (needRes[ENERGY] && q[i][ENERGY] > 0);
                if (skilled[i]) cnt++;
            }
            skilledCount = cnt;
            int cb = 0;
            for (int r = 0; r < 2; r++) if (needRes[r]) cb += Math.min(wRes[r], jRes[r]);
            coverBound = cb;
        }

        // ---- длительности для конкретного разбиения ----
        double[][] durations(int[] bo, int B) {
            double[][] qs = new double[B][2];
            double[] cb = new double[B];
            List<List<Integer>> mem = new ArrayList<>();
            for (int b = 0; b < B; b++) mem.add(new ArrayList<>());
            for (int i = 0; i < n; i++) {
                int b = bo[i];
                qs[b][0] += q[i][0]; qs[b][1] += q[i][1];
                mem.get(b).add(i);
            }
            for (int b = 0; b < B; b++) {
                List<Integer> ms = mem.get(b);
                for (int a = 0; a < ms.size(); a++)
                    for (int c = a + 1; c < ms.size(); c++)
                        cb[b] = Math.max(cb[b], conf[ms.get(a)][ms.get(c)]);
            }
            double[][] dur = new double[B][R];
            for (int b = 0; b < B; b++)
                for (int i = 0; i < R; i++) {
                    double s = qs[b][res[i]];                     // только квалификация по ресурсу работы
                    dur[b][i] = s > 0 ? (1 + cb[b]) * base[i] / s : INF;
                }
            return dur;
        }

        /**
         * Единственный планировщик: назначение + порядок + времена согласованно.
         * Берём только ГОТОВУЮ работу (все предшественники завершены),
         * start = max(освобождение бригады, max окончание предшественников).
         * Внутри бригады free[b] монотонно растёт => строгая последовательность.
         */
        Eval schedule(double[][] dur, int B, int[] forced, boolean coverageFirst) {
            double[] free = new double[B], end = new double[R];
            boolean[] done = new boolean[R];
            int[] load = new int[B], assign = new int[R];
            Arrays.fill(assign, -1);
            List<List<Integer>> orders = new ArrayList<>();
            for (int b = 0; b < B; b++) orders.add(new ArrayList<>());

            int cnt = 0;
            while (cnt < R) {
                int bi = -1, bb = -1;
                double bestFin = INF, bestLevel = -1;
                boolean bestEmpty = false;
                for (int i = 0; i < R; i++) {
                    if (done[i]) continue;
                    double ready = 0; boolean ok = true;
                    for (int p : preds[i]) {
                        if (!done[p]) { ok = false; break; }
                        ready = Math.max(ready, end[p]);
                    }
                    if (!ok) continue;
                    int from = 0, to = B - 1;
                    if (forced != null && forced[i] >= 0) { from = forced[i]; to = forced[i]; }
                    for (int b = from; b <= to; b++) {
                        if (dur[b][i] >= INF) continue;           // нет специалиста — нельзя
                        double fin = Math.max(free[b], ready) + dur[b][i];
                        boolean empty = load[b] == 0;
                        boolean better;
                        if (bi < 0) better = true;
                        else if (coverageFirst && empty != bestEmpty) better = empty;
                        else better = fin < bestFin - EPS
                                    || (Math.abs(fin - bestFin) <= EPS && level[i] > bestLevel + EPS);
                        if (better) { bi = i; bb = b; bestFin = fin; bestLevel = level[i]; bestEmpty = empty; }
                    }
                }
                if (bi < 0) return null;
                done[bi] = true; end[bi] = bestFin; free[bb] = bestFin;
                load[bb]++; assign[bi] = bb; orders.get(bb).add(bi); cnt++;
            }
            double mk = 0;
            for (double e : end) mk = Math.max(mk, e);
            Eval ev = new Eval(); ev.mk = mk; ev.orders = orders; ev.assign = assign;
            return ev;
        }

        /** Ремонт: если бригада осталась без работы — забираем ей задачу у перегруженной. */
        Eval fixCoverage(double[][] dur, int B, Eval e) {
            int[] forced = new int[R];
            Arrays.fill(forced, -1);
            for (int guard = 0; guard <= B; guard++) {
                int empty = -1;
                for (int b = 0; b < B; b++) if (e.orders.get(b).isEmpty()) { empty = b; break; }
                if (empty < 0) return e;
                int[] load = new int[B];
                for (int i = 0; i < R; i++) load[e.assign[i]]++;
                int pick = -1; double cost = INF;
                for (int i = 0; i < R; i++) {
                    if (dur[empty][i] >= INF || forced[i] >= 0) continue;
                    if (load[e.assign[i]] <= 1) continue;
                    if (dur[empty][i] < cost) { cost = dur[empty][i]; pick = i; }
                }
                if (pick < 0) return null;
                forced[pick] = empty;
                Eval ne = schedule(dur, B, forced, true);
                if (ne == null) return null;
                e = ne;
            }
            for (int b = 0; b < B; b++) if (e.orders.get(b).isEmpty()) return null;
            return e;
        }

        boolean feasible(int[] bo, int B) {
            int[] sz = new int[B]; int[] sk = new int[B];
            for (int i = 0; i < n; i++) { sz[bo[i]]++; if (skilled[i]) sk[bo[i]]++; }
            for (int b = 0; b < B; b++) if (sz[b] < MIN_SIZE || sz[b] > MAX_SIZE || sk[b] == 0) return false;
            return true;
        }

        Eval evaluate(int[] bo, int B) {
            if (bo == null || !feasible(bo, B)) return null;
            double[][] dur = durations(bo, B);
            Eval best = null;
            for (boolean cf : new boolean[]{false, true}) {
                Eval e = schedule(dur, B, null, cf);
                if (e == null) continue;
                e = fixCoverage(dur, B, e);
                if (e != null && (best == null || e.mk < best.mk)) best = e;
            }
            return best;
        }

        // ---- генерация разбиения: каждый получает специалиста, размеры 2..5 ----
        int[] build(int B, boolean randomized) {
            double[] w = new double[n];
            for (int i = 0; i < n; i++) w[i] = q[i][0] + q[i][1] + (randomized ? rnd.nextDouble() * 0.6 : 0);
            List<Integer> sk = new ArrayList<>(), un = new ArrayList<>();
            for (int i = 0; i < n; i++) (skilled[i] ? sk : un).add(i);
            sk.sort((a, b) -> Double.compare(w[b], w[a]));
            if (randomized) Collections.shuffle(un, rnd);

            int[] bo = new int[n]; Arrays.fill(bo, -1);
            int[] sz = new int[B];
            for (int t = 0; t < sk.size(); t++) { int b = t % B; bo[sk.get(t)] = b; sz[b]++; }
            for (int idx : un) {
                int target = -1;
                for (int b = 0; b < B; b++)
                    if (sz[b] < MAX_SIZE && (target < 0 || sz[b] < sz[target])) target = b;
                if (target < 0) return null;
                bo[idx] = target; sz[target]++;
            }
            normalize(bo, sz, B);
            return bo;
        }

        void normalize(int[] bo, int[] sz, int B) {
            for (int b = 0; b < B; b++) {
                while (sz[b] > MAX_SIZE) {
                    int to = -1;
                    for (int t = 0; t < B; t++) if (t != b && sz[t] < MAX_SIZE && (to < 0 || sz[t] < sz[to])) to = t;
                    if (to < 0) return;
                    int w = pickDonor(bo, b); if (w < 0) return;
                    bo[w] = to; sz[b]--; sz[to]++;
                }
            }
            for (int b = 0; b < B; b++) {
                while (sz[b] < MIN_SIZE) {
                    int from = -1;
                    for (int t = 0; t < B; t++) if (t != b && sz[t] > MIN_SIZE && (from < 0 || sz[t] > sz[from])) from = t;
                    if (from < 0) return;
                    int w = pickDonor(bo, from); if (w < 0) return;
                    bo[w] = b; sz[from]--; sz[b]++;
                }
            }
        }

        int pickDonor(int[] bo, int b) {
            int cntSk = 0;
            for (int i = 0; i < n; i++) if (bo[i] == b && skilled[i]) cntSk++;
            int fallback = -1;
            for (int i = 0; i < n; i++) if (bo[i] == b) {
                if (!skilled[i]) return i;
                if (cntSk > 1 && fallback < 0) fallback = i;
            }
            return fallback;
        }

        void mutate(int[] bo, int B) {
            int[] sz = new int[B];
            for (int v : bo) sz[v]++;
            if (rnd.nextBoolean()) {
                for (int t = 0; t < 30; t++) {
                    int i = rnd.nextInt(n), j = rnd.nextInt(n);
                    if (bo[i] != bo[j]) { int tmp = bo[i]; bo[i] = bo[j]; bo[j] = tmp; return; }
                }
            } else {
                for (int t = 0; t < 30; t++) {
                    int i = rnd.nextInt(n), b = rnd.nextInt(B);
                    if (bo[i] != b && sz[bo[i]] > MIN_SIZE && sz[b] < MAX_SIZE) { bo[i] = b; return; }
                }
            }
        }

        Solution toSolution(int[] bo, int B, Eval ev) {
            Solution s = new Solution();
            for (int b = 0; b < B; b++) {
                List<Integer> mem = new ArrayList<>();
                for (int i = 0; i < n; i++) if (bo[i] == b) mem.add(i);
                s.getMembers().add(mem);
                s.getOrders().add(new ArrayList<>(ev.orders.get(b)));
            }
            s.setMakespan(ev.mk);
            return s;
        }

        // ---- поиск: перебор числа бригад + hill climbing ----
        Solution search(long budget) {
            int bMin = (n + MAX_SIZE - 1) / MAX_SIZE;
            int bMax = Math.min(n / MIN_SIZE, skilledCount);
            if (R > 0) bMax = Math.min(Math.min(bMax, R), coverBound);

            if (R == 0) {
                int B = Math.max(bMin, Math.min(n / MIN_SIZE, Math.max(bMin, skilledCount)));
                int[] bo = build(B, false);
                Solution s = new Solution();
                for (int b = 0; b < B; b++) {
                    List<Integer> mem = new ArrayList<>();
                    for (int i = 0; i < n; i++) if (bo[i] == b) mem.add(i);
                    s.getMembers().add(mem);
                    s.getOrders().add(new ArrayList<>());
                }
                log.warn("Нет активных починок: бригады сформированы, event_order пустые");
                return s;
            }
            if (bMin > bMax)
                throw new IllegalStateException(String.format(
                        "Невыполнимо: размеры 2..5 требуют минимум %d бригад, а работой можно обеспечить максимум %d " +
                                "(специалистов=%d, починок=%d, граница покрытия=%d)",
                        bMin, bMax, skilledCount, R, coverBound));

            long deadline = System.currentTimeMillis() + budget;
            Solution best = null; double bestMk = INF;

            for (int B = bMax; B >= bMin; B--) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) break;
                long sliceEnd = System.currentTimeMillis() + Math.max(250L, left / (B - bMin + 1));

                int[] cur = build(B, false);
                Eval curEv = evaluate(cur, B);
                if (curEv == null) {
                    int[] alt = build(B, true);
                    Eval a = evaluate(alt, B);
                    if (a != null) { cur = alt; curEv = a; }
                }
                if (curEv != null && curEv.mk < bestMk) { bestMk = curEv.mk; best = toSolution(cur, B, curEv); }

                int stall = 0;
                while (System.currentTimeMillis() < Math.min(sliceEnd, deadline)) {
                    int[] cand;
                    if (cur == null || stall > 400) { cand = build(B, true); stall = 0; }
                    else { cand = cur.clone(); mutate(cand, B); }
                    Eval ev = evaluate(cand, B);
                    if (ev == null) { stall++; continue; }
                    if (curEv == null || ev.mk <= curEv.mk + EPS) { cur = cand; curEv = ev; }
                    stall++;
                    if (ev.mk < bestMk - EPS) { bestMk = ev.mk; best = toSolution(cand, B, ev); stall = 0; }
                }
            }
            if (best == null)
                throw new IllegalStateException("Допустимый план не найден: проверьте квалификации, зависимости и число починок");
            return best;
        }

        // ---- ЖЁСТКАЯ ПРОВЕРКА всех правил + пересчёт makespan ----
        double verify(Solution s) {
            int B = s.getMembers().size();
            boolean[] seen = new boolean[n];
            for (List<Integer> mem : s.getMembers()) {
                if (mem.size() < MIN_SIZE || mem.size() > MAX_SIZE)
                    throw new IllegalStateException("Размер бригады вне 2..5: " + mem.size());
                for (int w : mem) {
                    if (seen[w]) throw new IllegalStateException("Рабочий в двух бригадах");
                    seen[w] = true;
                }
            }
            for (boolean b : seen) if (!b) throw new IllegalStateException("Не все рабочие распределены");
            if (R == 0) return 0;

            int[] owner = new int[R];
            Arrays.fill(owner, -1);
            for (int b = 0; b < B; b++) {
                List<Integer> ord = s.getOrders().get(b);
                if (ord.isEmpty()) throw new IllegalStateException("Бригада " + (b + 1) + " без работы");
                for (int i : ord) {
                    if (owner[i] >= 0) throw new IllegalStateException("Починка назначена дважды");
                    owner[i] = b;
                }
            }
            for (int i = 0; i < R; i++) if (owner[i] < 0) throw new IllegalStateException("Починка не назначена");

            double[][] qs = new double[B][2];
            double[] cb = new double[B];
            for (int b = 0; b < B; b++) {
                List<Integer> mem = s.getMembers().get(b);
                for (int w : mem) { qs[b][0] += q[w][0]; qs[b][1] += q[w][1]; }
                for (int a = 0; a < mem.size(); a++)
                    for (int c = a + 1; c < mem.size(); c++)
                        cb[b] = Math.max(cb[b], conf[mem.get(a)][mem.get(c)]);
            }
            double[][] dur = new double[B][R];
            for (int b = 0; b < B; b++)
                for (int i = 0; i < R; i++)
                    dur[b][i] = qs[b][res[i]] > 0 ? (1 + cb[b]) * base[i] / qs[b][res[i]] : INF;
            for (int i = 0; i < R; i++)
                if (dur[owner[i]][i] >= INF)
                    throw new IllegalStateException("Нет специалиста по ресурсу для починки " + m.getRepairs().get(i).getId());

            // симуляция ровно так, как будет исполнять принимающая сторона
            int[] ptr = new int[B];
            double[] free = new double[B], end = new double[R];
            boolean[] fin = new boolean[R];
            int done = 0; double mk = 0;
            while (done < R) {
                boolean moved = false;
                for (int b = 0; b < B; b++) {
                    List<Integer> ord = s.getOrders().get(b);
                    if (ptr[b] >= ord.size()) continue;
                    int i = ord.get(ptr[b]);
                    double ready = 0; boolean ok = true;
                    for (int p : preds[i]) {
                        if (!fin[p]) { ok = false; break; }
                        ready = Math.max(ready, end[p]);
                    }
                    if (!ok) continue;                       // ждём другую бригаду
                    double st = Math.max(free[b], ready);    // простой разрешён
                    end[i] = st + dur[b][i];
                    free[b] = end[i]; fin[i] = true; ptr[b]++; done++; moved = true;
                    mk = Math.max(mk, end[i]);
                }
                if (!moved) throw new IllegalStateException("Взаимная блокировка порядков работ");
            }
            return mk;
        }
    }

    // ==================== ВЫВОД ====================

    private String write(ProblemModel m, Solution s) {
        try {
            List<Map<String, Object>> brigades = new ArrayList<>();
            for (int b = 0; b < s.getMembers().size(); b++) {
                final int bb = b;
                List<Long> emp = s.getMembers().get(bb).stream()
                        .map(w -> m.getWorkers().get(w).getTabel()).sorted().collect(Collectors.toList());
                List<Long> ord = s.getOrders().get(bb).stream()
                        .map(i -> m.getRepairs().get(i).getId()).collect(Collectors.toList());
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("brigade_id", bb + 1);
                o.put("employees", emp);
                o.put("event_order", ord);
                brigades.add(o);
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("brigades", brigades);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации JSON", e);
        }
    }
}