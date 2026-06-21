# Phase 1 · Task 4 — Saga/Outbox throughput dashboard (custom metrics)

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone A (Observability), Week 4
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 3](task-03-base-grafana-dashboards.md)
> **Outcome:** Custom Micrometer metrics on your Saga and Outbox flows, plus a dashboard showing outbox backlog and saga step timings — making your distributed-data patterns *observable*, which is rare and senior-defining.

---

## Overview

The framework gives you JVM/HTTP/Kafka metrics for free, but your **business-critical** flows — the Saga orchestration and the transactional Outbox — are invisible. This task instruments them with **custom Micrometer metrics** (counters, timers, gauges) and builds a dashboard so you can see outbox backlog growing or a saga step slowing down.

This is the panel that turns "we have a Saga/Outbox" into "we can *operate* a Saga/Outbox."

## Why this matters

- **Outbox backlog is a silent failure mode.** If the `OutboxScheduler` falls behind, events stop flowing and orders silently stall. A `pending` gauge catches it instantly.
- **Most teams never instrument their sagas.** Doing it is a strong differentiator in a promotion review.
- **Sets up Phase 2.** The chaos demo (kill payment-service) is far more convincing when you can show saga steps and outbox state reacting live.

## Where to instrument (real classes)

| Concern | Class(es) |
|---|---|
| Order-side outbox polling | `order-application-service` → `outbox/scheduler/payment/PaymentOutboxScheduler.java`, `outbox/scheduler/approval/RestaurantApprovalOutboxScheduler.java` |
| Outbox cleanup | `PaymentOutboxCleanerScheduler.java`, `RestaurantApprovalOutboxCleanerScheduler.java` |
| Payment/restaurant outbox | `payment-application-service` & `restaurant-application-service` → `outbox/scheduler/OrderOutboxScheduler.java` |
| Saga steps | `OrderPaymentSaga.java`, `OrderApprovalSaga.java`, `OrderSagaHelper.java` |
| Outbox status enum | `infrastructure/outbox` → `OutboxStatus` |

## Step-by-step

### Step 1 — Inject MeterRegistry

The container modules now have Micrometer on the classpath (Task 1). Inject `io.micrometer.core.instrument.MeterRegistry` into the scheduler/saga beans (constructor injection, matching the project's Lombok `@RequiredArgsConstructor` style).

### Step 2 — Timer around saga steps

In `OrderPaymentSaga.process(...)` / `rollback(...)` (and the `OrderApprovalSaga` equivalents), wrap the work in a timer:

```java
Timer.builder("saga.step")
     .tag("saga", "order-payment")
     .tag("step", "process")
     .register(meterRegistry)
     .record(() -> { /* existing step logic */ });
```

Record both `process` and `rollback` so compensations are visible.

### Step 3 — Counter on outbox dispatch outcomes

In the outbox schedulers, count messages published vs failed:

```java
meterRegistry.counter("outbox.dispatch", "outbox", "payment", "result", "success").increment(batch.size());
```

### Step 4 — Gauge on pending outbox backlog

Register a gauge that reads the count of `STARTED`/pending rows via the existing outbox repository (e.g. `PaymentOutboxRepository`). Gauges poll a supplier:

```java
Gauge.builder("outbox.pending", outboxRepository,
        repo -> repo.countByOutboxStatus(OutboxStatus.STARTED))
     .tag("outbox", "payment")
     .register(meterRegistry);
```

Reuse the existing repository query methods; add a lightweight count method if one doesn't exist (don't load full rows just to count).

### Step 5 — Dashboard

New Grafana dashboard `saga-outbox.json`:

- **Outbox pending (gauge/timeseries):** `outbox_pending{outbox=~"payment|approval|order"}` — the backlog canary.
- **Dispatch rate:** `rate(outbox_dispatch_total[1m])` split by `result`.
- **Saga step p95:** `histogram_quantile(0.95, sum by (le, saga, step) (rate(saga_step_seconds_bucket[5m])))`.
- **Rollback count:** `increase(saga_step_seconds_count{step="rollback"}[5m])` — compensations should normally be ~0.

Export to JSON and commit alongside the Task 3 dashboards.

## Verification

- Place several orders; watch **dispatch rate** rise and **pending** spike then drain to ~0 (proves the scheduler keeps up).
- Force a failure (e.g. stop `payment-service` briefly) → **pending** climbs and **rollback** or lag becomes visible; restart → it drains. This is a dry-run of the Phase 2 chaos demo.
- Saga step p95 shows real numbers.

## Checklist

- [ ] `MeterRegistry` injected into saga + outbox beans
- [ ] `saga.step` timer (process + rollback) on both sagas
- [ ] `outbox.dispatch` counter with success/fail + outbox tags
- [ ] `outbox.pending` gauge backed by an efficient count query
- [ ] `saga-outbox.json` dashboard committed
- [ ] Backlog visibly drains under normal load; spikes under induced failure
- [ ] Build green; committed on the Phase 1 branch

## Notes & gotchas

- **Count, don't fetch.** The pending gauge must use a `SELECT count(*)` style query, not load entities — otherwise observability hurts performance (the opposite of the goal).
- **Cardinality discipline.** Tag by saga/step/outbox/result only. Never tag by orderId/customerId — high-cardinality tags will blow up Prometheus.
- **Gauge lifecycle.** Register gauges once at startup, not per-invocation, or you'll leak meters.
- **Keep domain core clean.** Put metrics in the application-service/scheduler layer, not in `*-domain-core` entities — respect the existing hexagonal boundaries.

## Learning resources (just-in-time)

- Micrometer docs → *Concepts* (Counter, Gauge, Timer) and *Naming meters / tags*
- Micrometer → *Cardinality* guidance
- Your own code: re-read `OutboxScheduler` + `OutboxStatus` in `infrastructure/outbox`

## Definition of done

Saga steps and outbox flows emit custom metrics, and a committed dashboard shows backlog + step timings reacting to load and induced failure. Sub-milestone A is nearly complete — **next is tracing (Task 5), the hardest and most senior-defining piece of Phase 1.**

---

*Next: **[Task 5 — Distributed tracing across Kafka (OTel + Jaeger)](task-05-distributed-tracing-kafka.md)**.*
