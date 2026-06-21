# Phase 1 — Task Index

Detailed, executable tasks for [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md). One task ≈ one week (~5 hrs). Do them in order — each unblocks the next. All work lands on a single Phase 1 branch and is merged in [Task 17](task-17-phase-writeup.md).

## Sub-milestone A — Observability (weeks 1–6)

| # | Task | Outcome |
|---|---|---|
| 01 | [Actuator + Micrometer](task-01-actuator-micrometer.md) | Every service exposes health + `/actuator/prometheus` |
| 02 | [Prometheus + Grafana in compose](task-02-prometheus-grafana-compose.md) | One-command monitoring stack scraping all 4 services |
| 03 | [Base Grafana dashboards](task-03-base-grafana-dashboards.md) | JVM / HTTP / Kafka-lag dashboards as committed JSON |
| 04 | [Saga/Outbox metrics](task-04-saga-outbox-metrics.md) | Custom metrics + dashboard for the distributed-data flows |
| 05 | [Distributed tracing across Kafka](task-05-distributed-tracing-kafka.md) | One order = one connected trace in Jaeger *(hardest task)* |
| 06 | [Structured JSON logging](task-06-structured-logging.md) | Logs in JSON, correlated to traces by `traceId` |

## Sub-milestone B — Performance (weeks 7–11)

| # | Task | Outcome |
|---|---|---|
| 07 | [k6 load test + baseline](task-07-k6-load-baseline.md) | Repeatable load test + committed "before" numbers |
| 08 | [JFR + async-profiler flame graphs](task-08-profiling-flamegraphs.md) | Top 2–3 hotspots identified from real profiles |
| 09 | [DB: N+1, indexes, query tuning](task-09-db-tuning.md) | N+1 gone, indexes added, re-measured |
| 10 | [Java 25 virtual threads](task-10-virtual-threads.md) | Vthreads on I/O paths + honest before/after verdict |
| 11 | [GC + JVM tuning](task-11-gc-jvm-tuning.md) | Justified heap/GC config validated on the load test |

## Sub-milestone C — Caching (weeks 12–17)

| # | Task | Outcome |
|---|---|---|
| 12 | [Spring Cache abstraction](task-12-spring-cache-abstraction.md) | `@Cacheable` on profiler-confirmed hot reads |
| 13 | [Caffeine local cache](task-13-caffeine-local-cache.md) | Tuned local cache (size + TTL) with rising hit ratio |
| 14 | [Redis distributed cache](task-14-redis-distributed-cache.md) | Shared cache via cache-aside across instances |
| 15 | [Cache invalidation on writes](task-15-cache-invalidation.md) | Stale-read reproduced then fixed + guarded by a test |
| 16 | [Cache stampede mitigation](task-16-cache-stampede.md) | Hot cache survives a concurrent-miss burst |
| 17 | [Re-measure + Phase write-up](task-17-phase-writeup.md) | Depth marker assembled, PR merged, post published |

---

**Depth marker (whole phase):** a single before/after dashboard story for the order-creation path — distributed trace + flame graph + cache hit ratio + measured p50/p95/p99 + throughput improvement, all on your own Grafana.
