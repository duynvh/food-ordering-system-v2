# Phase 1 · Task 10 — Java 25 virtual threads

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone B (Performance), Week 10
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 7](task-07-k6-load-baseline.md) (compare on the same load)
> **Outcome:** Virtual threads enabled on blocking I/O paths, with a measured before/after — and an honest verdict on whether they helped *this* workload.

---

## Overview

You're on **Java 25**, so virtual threads are mature and first-class. They let a service handle far more concurrent blocking calls (DB, HTTP) without the memory/scheduling cost of platform threads. This task turns them on where they help (the web request path, blocking adapters), re-runs the load test, and — importantly — **measures whether it actually moved the needle** for this app rather than cargo-culting them in.

## Why this matters

- **Right-sized concurrency is a senior judgment call**, and "I measured it both ways" is the senior answer (vs "I enabled the new thing because it's new").
- **Cheap to try on Spring Boot 3.5** — one property flips the web layer.
- **Reveals the nature of your bottleneck** — if virtual threads help a lot, you were thread-pool-bound; if not, you're CPU- or downstream-bound (useful to know either way).

## Files you'll touch

| File | Change |
|---|---|
| `order-service` + `customer-service` `application.yml` | `spring.threads.virtual.enabled: true` (web services) |
| Kafka consumer config (`KafkaConsumerConfig`) | consider virtual-thread executor for listener work (advanced/optional) |
| any custom `Executor`/`@Async` beans | switch to `Executors.newVirtualThreadPerTaskExecutor()` where blocking |
| `perf/baselines/` | record before/after |

## Step-by-step

### Step 1 — Flip the web layer (order + customer)

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

This makes Tomcat serve each request on a virtual thread — the simplest, highest-signal change for the HTTP-facing services.

### Step 2 — Audit blocking work off the request thread

Look for custom executors / `@Async` usage and the outbox schedulers. For genuinely blocking tasks, a `newVirtualThreadPerTaskExecutor()` can replace a fixed pool. **Don't** convert CPU-bound work to virtual threads — it won't help and can hurt.

### Step 3 — Watch for pinning

Virtual threads "pin" to a carrier thread inside `synchronized` blocks doing I/O, negating the benefit. Run with pinning detection:

```
-Djdk.tracePinnedThreads=full
```

Note any pinned stacks (often legacy `synchronized` around I/O or some driver internals). Record them; deep fixes are optional stretch.

### Step 4 — Re-measure (and raise the load)

Re-run Task 7's k6. Because virtual threads shine under high concurrency, also try a heavier stage (e.g. target 200–500 VUs) to see if throughput ceilings move. Record p95/p99 + max throughput before/after.

## Verification

- App boots and serves correctly with virtual threads on (functional parity — no regressions in the k6 error rate).
- Before/after numbers captured at both the baseline load and a higher-concurrency load.
- Pinning report reviewed; any hotspots noted.
- A written **verdict**: did it help, by how much, and why (or why not).

## Checklist

- [ ] `spring.threads.virtual.enabled: true` on the two web services
- [ ] Blocking custom executors reviewed (converted only where appropriate)
- [ ] `-Djdk.tracePinnedThreads=full` run; pinning noted
- [ ] k6 re-run at baseline + higher concurrency; deltas recorded
- [ ] Written verdict in the baseline doc
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Virtual threads help I/O-bound, high-concurrency workloads** — not CPU-bound ones. If your bottleneck is the DB (Task 9) or downstream Kafka, vthreads may show little gain; that's a valid, report-worthy finding.
- **Don't pool virtual threads.** The pattern is one-per-task, unlimited. Pooling them defeats the purpose.
- **Connection pool becomes the new ceiling.** With thousands of virtual threads hitting the DB, the HikariCP pool size is now your limiter — note it; you may revisit in Task 11.
- **Honesty is the deliverable.** A measured "negligible for this workload" is a *better* senior artifact than a fake win.

## Learning resources (just-in-time)

- JEP/JDK docs → *Virtual Threads* (pinning, structured guidance)
- Spring Boot reference → *Virtual threads* (`spring.threads.virtual.enabled`)
- "Virtual threads pinning" notes (`jdk.tracePinnedThreads`)

## Definition of done

Virtual threads enabled where appropriate, with measured before/after and an honest verdict. **Task 11 closes Performance with GC + JVM tuning.**

---

*Next: **[Task 11 — GC + JVM tuning](task-11-gc-jvm-tuning.md)**.*
