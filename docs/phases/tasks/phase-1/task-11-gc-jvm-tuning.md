# Phase 1 · Task 11 — GC + JVM tuning

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone B (Performance), Week 11
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 8](task-08-profiling-flamegraphs.md) (allocation profile), [Task 7](task-07-k6-load-baseline.md) (load)
> **Outcome:** Sensible heap sizing + GC choice for each service, validated against the load test, with documented deltas. Closes the Performance sub-milestone.

---

## Overview

This task tunes the JVM itself: **heap sizing**, **GC algorithm** (G1 vs ZGC/Generational ZGC), and a few high-value flags — then proves the change on the GC dashboard (Task 3) and the k6 load test (Task 7). On Java 25 you have modern collectors available; the goal is lower/steadier pause times without wasting memory.

## Why this matters

- **GC pauses are tail-latency.** Your p99 is often a GC pause story; tuning it is the most direct lever on p99.
- **"I chose ZGC because the allocation profile showed X and pauses dropped from Yms to Zms" is a textbook senior artifact.**
- **Closes the loop** from Task 8's allocation flame graph to a measured outcome.

## Files you'll touch

| File | Change |
|---|---|
| run configs / `JAVA_TOOL_OPTIONS` / container env | JVM flags per service |
| `perf/baselines/` | GC before/after, pause distribution, k6 deltas |
| (Phase 3 note) | these flags will move into Dockerfiles/k8s in Phase 3 |

## Step-by-step

### Step 1 — Baseline the GC

With current defaults under k6 load, capture from the Grafana JVM dashboard: GC pause rate, pause durations (`jvm_gc_pause_seconds`), allocation rate, heap after-GC. Optionally enable GC logging:

```
-Xlog:gc*:file=perf/profiles/gc-order.log:time,uptime,level,tags
```

### Step 2 — Right-size the heap

Set explicit `-Xms`/`-Xmx` (equal, to avoid resize churn) based on observed live-set after GC. Don't over-allocate — oversized heaps waste RAM and lengthen some pauses.

### Step 3 — Choose a collector and compare

- **G1** (default) — balanced; good baseline.
- **Generational ZGC** (`-XX:+UseZGC -XX:+ZGenerational`) — sub-millisecond pauses, great if your p99 is pause-driven and you have allocation pressure.

Run k6 with each, holding everything else constant. Compare pause distribution + p99.

### Step 4 — A few targeted flags

Consider (measure each): `-XX:MaxGCPauseMillis` (G1 target), `-XX:+AlwaysPreTouch` (steadier latency at startup cost), and string dedup if heap shows many duplicate strings. **One change at a time** — bundled changes make attribution impossible.

### Step 5 — Lock in and document

Pick the winning config per service. Record the chosen flags and the before/after (pauses + p99 + throughput) in the baseline doc. Leave a note that these move into Phase 3 container configs.

## Verification

- GC dashboard shows the chosen collector's pause profile, improved vs baseline (or a documented "no meaningful difference," which is also valid).
- k6 p99 improvement (or neutral) recorded under identical load.
- Final per-service JVM flags committed/documented.

## Checklist

- [ ] GC baseline captured under load (dashboard + optional gc log)
- [ ] Heap sized from observed live-set (`-Xms` = `-Xmx`)
- [ ] G1 vs Generational ZGC compared on the same load
- [ ] Targeted flags tested one at a time
- [ ] Winning config + before/after documented in `perf/baselines/`
- [ ] Note added: flags migrate to Dockerfiles/k8s in Phase 3
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **One variable at a time.** Change collector OR heap OR a flag, never several — otherwise you can't attribute the result.
- **Don't tune to a benchmark you won't see in prod.** Tune to your k6 profile, which should resemble realistic traffic.
- **Container awareness (Phase 3 foreshadow).** In k8s the JVM reads cgroup limits; prefer `-XX:MaxRAMPercentage` over hard `-Xmx` once containerized. For now, explicit sizing on the host is fine.
- **Diminishing returns.** If Tasks 9–10 already hit your targets, a quick "G1 defaults are fine, here's the evidence" is a legitimate close. Don't over-tune.

## Learning resources (just-in-time)

- JDK docs → *G1* and *ZGC* tuning guides
- "Java GC logging / `-Xlog:gc*`" reference
- Spring Boot / container JVM memory (`MaxRAMPercentage`) — skim for Phase 3

## Definition of done

Each service has a justified heap/GC configuration validated on the load test, with documented deltas. **Performance (Sub-milestone B) is complete.** Next: Caching — fixing the bottlenecks you've now measured.

---

*Next: **[Task 12 — Spring Cache abstraction on hot reads](task-12-spring-cache-abstraction.md)** (start of Caching).*
