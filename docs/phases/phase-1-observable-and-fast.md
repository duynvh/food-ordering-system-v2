# Phase 1 — Observable & Fast

> **Combines:** Observability + Performance + Caching
> **Window:** ≈ late Jun → late Oct 2026 (~17 weeks, ~83 hrs)
> **Mission:** Make the order flow *measurable*, then make it *fast*. You cannot tune or cache credibly without first being able to measure — so these three topics are one continuous loop, not three courses.

Part of the [one-year senior-Java plan](../plan.md). Builds the measurement + speed foundation that every later phase relies on.

---

## Why these three together

Observability → Performance → Caching is a single feedback loop:

1. **Observability** gives you eyes (metrics, traces, logs).
2. **Performance** uses those eyes to find the real bottleneck (not the guessed one).
3. **Caching** fixes the bottleneck, and you *prove* the fix on the same dashboards.

Doing caching without measurement is cargo-culting; doing performance without observability is guessing. Bundled, they reinforce each other.

## Starting point in the codebase

| Concern | Current state | Where |
|---|---|---|
| Metrics / health | None (no Actuator, no Micrometer) | all `*-container` modules |
| Tracing | None | cross-service Kafka calls in `infrastructure/kafka` |
| Logging | SLF4J `@Slf4j` only, unstructured | throughout |
| Caching | None | hot reads in restaurant/customer services |
| Load testing | None | — |

## Sub-milestones & week-by-week

### Sub-milestone A — Observability (weeks 1–6, ~30 hrs)
- **W1** — Add `spring-boot-starter-actuator` + `micrometer-registry-prometheus` to every `*-container` module's `pom.xml`. Expose `/actuator/health`, `/actuator/prometheus`. Configure readiness/liveness groups.
- **W2** — Stand up **Prometheus + Grafana** in `infrastructure/docker-compose/` (new `monitoring.yml` next to `kafka_cluster.yml`). Wire Prometheus to scrape all services; hook into `startup.sh`/`shutdown.sh`.
- **W3** — Build base Grafana dashboards: JVM (heap, GC, threads), HTTP server (rate/latency/errors), and a **Kafka consumer-lag** panel (critical for your event-driven flow).
- **W4** — Add a **Saga/Outbox throughput** dashboard: outbox poll rate, pending vs completed (from `infrastructure/outbox`), saga step durations (`OrderPaymentSaga`, `OrderApprovalSaga`).
- **W5** — Distributed tracing: add OpenTelemetry (Micrometer Tracing bridge) → **Jaeger or Tempo** in compose. Critical hard part: **propagate trace context across Kafka** producers/consumers in `infrastructure/kafka` (inject/extract on message headers).
- **W6** — Structured **JSON logging** + `traceId`/`spanId`/correlation-id in every log line (logback encoder). Optional: Loki for log aggregation.

**Exit check:** one real order produces a single connected trace spanning `order → payment → restaurant` in Jaeger, and every service shows up in Grafana.

### Sub-milestone B — Performance (weeks 7–11, ~25 hrs)
- **W7** — Write a **k6** (or Gatling) load test for the order-creation flow. Establish a baseline: throughput + p50/p95/p99, saved as a committed report.
- **W8** — Profile under load with **JFR + async-profiler**; capture flame graphs. Identify the top 2–3 hotspots.
- **W9** — Database pass: enable SQL logging, find **N+1 queries** and missing indexes in the JPA dataaccess modules; add indexes via migration; re-measure.
- **W10** — **Java 25 virtual threads** on blocking I/O paths (`spring.threads.virtual.enabled` and/or explicit executors); measure before/after on the same load test.
- **W11** — GC + JVM flag tuning (heap sizing, GC choice); document the deltas. Re-run k6 and snapshot the new percentiles.

**Exit check:** a documented, reproducible before/after with flame graphs and percentile numbers.

### Sub-milestone C — Caching (weeks 12–17, ~28 hrs)
- **W12** — Identify hot read paths from the traces/profiles (likely restaurant/menu lookups, customer lookups). Add Spring Cache abstraction (`@EnableCaching`, `@Cacheable`).
- **W13** — **Caffeine** as local cache for the hottest, low-churn reads; tune size + TTL.
- **W14** — Add **Redis** to compose; configure as a distributed cache; move shared/cross-instance reads to Redis (cache-aside).
- **W15** — Correct **invalidation on writes** (`@CacheEvict`/`@CachePut`) — prove a stale-read scenario is fixed.
- **W16** — Mitigate **cache stampede** (request coalescing / short-TTL jitter / lock-on-miss); document the consistency tradeoff you chose.
- **W17** — Re-run the Phase-B k6 load test; capture cache hit ratio + the latency/throughput delta on Grafana. Write the phase post.

## Depth marker (definition of done)

A **single before/after dashboard story** for the order-creation path, all read off your own Grafana:

- a distributed trace of one order across all services, **and**
- a flame graph identifying the original bottleneck, **and**
- a cache hit-ratio panel, **and**
- a measured p50/p95/p99 latency + throughput improvement (numbers, not vibes).

## Learning resources (just-in-time, ~⅓ of time)

- Spring Boot Actuator & Observability reference docs
- Micrometer + Micrometer Tracing + OpenTelemetry docs
- One Grafana dashboard tutorial
- k6 documentation (load testing)
- Redis docs + Spring Cache reference

## Deliverables

- [ ] `monitoring.yml` in `infrastructure/docker-compose/` (Prometheus, Grafana, Jaeger/Tempo, Redis)
- [ ] Actuator + Micrometer wired into all `*-container` modules
- [ ] Trace context propagated across Kafka in `infrastructure/kafka`
- [ ] Committed Grafana dashboard JSON + k6 scripts + JFR/flame-graph artifacts under `docs/` or a `perf/` dir
- [ ] Caching (Caffeine + Redis) with correct invalidation on hot read paths
- [ ] **One merged PR** + **~700-word write-up** with before/after numbers

## Phase-specific risks

- **Trace propagation across Kafka is the hardest part** — budget extra time in W5; it's also the most senior-defining piece, so don't skip it.
- **Load-test environment skew** — run k6 against the docker-compose stack consistently so before/after numbers are comparable.
- **Caching correctness > caching speed** — a fast wrong answer is worse than a slow right one; the invalidation work in W15 is non-negotiable.
