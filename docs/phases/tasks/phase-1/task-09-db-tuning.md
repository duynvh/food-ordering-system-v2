# Phase 1 · Task 9 — Database: N+1, indexes, query tuning

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone B (Performance), Week 9
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 8](task-08-profiling-flamegraphs.md) (let the profiler/SQL logs point you)
> **Outcome:** N+1 query patterns eliminated and missing indexes added (via migration), with re-measured latency proving the win.

---

## Overview

The database is the most common hotspot in a service like this. This task hunts down **N+1 queries** (the classic JPA trap — one query becomes N+1 because of lazy associations in a loop) and **missing indexes** on the columns your queries filter/join on, across the `*-dataaccess` modules. Then you re-run the Task 7 load test to quantify the improvement.

## Why this matters

- **Cheapest big win.** An index or a fetch-join often beats every other optimization combined.
- **Profilers love to point here.** If Task 8's flame graph was hot in Hibernate, this is your fix.
- **Index reasoning is a senior interview/promotion staple.**

## Where to look (real modules)

| Service | Data-access module / repos |
|---|---|
| order | `order-dataaccess` (order + payment/approval outbox entities & JPA repos) |
| payment | `payment-dataaccess` (`OrderOutboxEntity` + repos) |
| restaurant | `restaurant-dataaccess` → `RestaurantRepositoryImpl`, `OrderApprovalJpaRepository` |
| customer | `customer-dataaccess` → `CustomerJpaRepository`, `CustomerRepositoryImpl` |

The hot read paths (restaurant/menu lookups, customer lookups, outbox polling) are the prime suspects.

## Step-by-step

### Step 1 — Turn on SQL visibility

`show-sql: true` is already set. Add statement stats to count queries per request:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.stat: DEBUG
```

(Toggle off after this task — it's noisy and slows things down.)

### Step 2 — Find N+1

Run one order through the flow and read the logs: if you see the *same* SELECT repeated once per child row (e.g. per order item, per outbox row), that's N+1. Confirm with Hibernate statistics (queries-per-request count).

Typical fixes:
- **Fetch joins** / `@EntityGraph` for associations read together.
- **Batch fetching** (`hibernate.default_batch_fetch_size`) for collections.
- For outbox polling, ensure the scheduler query selects a bounded batch with a `WHERE outbox_status = ...` predicate (and that column is indexed — Step 3).

### Step 3 — Add indexes via migration

The codebase initializes schema via `init-schema.sql` per service. Add indexes for:
- **Outbox tables:** `(outbox_status)` and/or `(outbox_status, type)` — the scheduler polls these constantly.
- **Foreign keys / lookup columns** used in joins and `WHERE` clauses (restaurant id, customer id, saga id, order id).

Use `EXPLAIN ANALYZE` in psql to confirm a query switches from `Seq Scan` to `Index Scan`.

### Step 4 — Re-measure

Re-run `perf/k6/order-create.js` (Task 7) under identical conditions. Capture the delta in the baseline doc.

## Verification

- Queries-per-request count drops (N+1 gone) — shown via Hibernate statistics before/after.
- `EXPLAIN ANALYZE` shows index usage on the outbox/lookup queries.
- k6 re-run shows reduced p95/p99 and/or higher throughput; numbers recorded.

## Checklist

- [ ] SQL + statistics logging enabled (temporarily)
- [ ] N+1 patterns identified and fixed (fetch join / entity graph / batch size)
- [ ] Indexes added via `init-schema.sql` (outbox status + key lookups/joins)
- [ ] `EXPLAIN ANALYZE` confirms index scans
- [ ] k6 re-run delta recorded in `perf/baselines/`
- [ ] SQL/stat logging turned back off
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Indexes aren't free.** Each one slows writes and uses space — add only those a real query uses (proven by EXPLAIN), not speculative ones.
- **`open-in-view: false` is already set** (good) — lazy-load failures surface at the right boundary; don't "fix" them by re-enabling OIV.
- **Don't break the hexagonal boundary.** Query tuning lives in `*-dataaccess` adapters/repos and SQL, not in `*-domain-core`.
- **Outbox polling is a recurring query** — its index is arguably the highest-ROI change in this task because it runs on a fixed schedule for every service.
- **Reproducibility:** re-measure with the same seeded data volume as the baseline, or the comparison lies.

## Learning resources (just-in-time)

- Hibernate User Guide → *Fetching* (N+1, fetch joins, `@EntityGraph`, batch fetching)
- PostgreSQL docs → *Using EXPLAIN*, *Indexes*
- Vlad Mihalcea's N+1 / batch-fetching articles (skim)

## Definition of done

N+1 eliminated and justified indexes added, with a measured improvement on the same load test. **Task 10 attacks concurrency with Java 25 virtual threads.**

---

*Next: **[Task 10 — Java 25 virtual threads](task-10-virtual-threads.md)**.*
