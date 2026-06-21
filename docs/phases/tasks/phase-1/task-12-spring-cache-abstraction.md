# Phase 1 · Task 12 — Spring Cache abstraction on hot reads

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone C (Caching), Week 12
> **Estimated effort:** ~4 hrs
> **Depends on:** [Task 8](task-08-profiling-flamegraphs.md)/[Task 9](task-09-db-tuning.md) (let the data pick the cache targets)
> **Outcome:** The Spring Cache abstraction wired in (`@EnableCaching` + `@Cacheable`) on the hot read paths the profiler flagged — with a simple in-memory provider first, so the *abstraction* is proven before you add Caffeine/Redis.

---

## Overview

Caching starts with the **abstraction**, not a cache product. This task adds Spring's cache annotations to the genuinely hot, read-heavy paths (identified from traces/profiles — likely restaurant/menu and customer lookups) using the default simple provider. Getting the abstraction and cache keys right *first* means swapping in Caffeine (Task 13) and Redis (Task 14) is then a config change, not a rewrite.

## Why this matters

- **Cache the proven hot path, not a guess.** You measured in Sub-milestone B; now act on it.
- **Provider-agnostic design is senior.** `@Cacheable` lets you change backends without touching business code.
- **Foundation for the real win** — local + distributed caching land in the next two tasks.

## Where to cache (real classes)

| Candidate read path | Class |
|---|---|
| Restaurant / menu lookup | `restaurant-dataaccess` → `RestaurantRepositoryImpl` |
| Customer lookup | `customer-dataaccess` → `CustomerRepositoryImpl` |

Cache in the **application-service or adapter layer** (the port implementation), not in `*-domain-core`. Confirm against your Task 8 findings — only cache what's actually read-hot and tolerant of slight staleness.

## Step-by-step

### Step 1 — Enable caching

Add `@EnableCaching` to a config class in each relevant container/application-service module.

### Step 2 — Annotate the hot read

```java
@Cacheable(cacheNames = "restaurants", key = "#restaurantId")
public Optional<Restaurant> findRestaurant(UUID restaurantId) {
    // existing repository lookup
}
```

Choose **stable, low-cardinality keys** (entity id). Avoid caching methods that return per-request-variable data.

### Step 3 — Pick what NOT to cache

Write-through paths, order creation, saga state, outbox rows — **do not cache**. Caching mutable workflow state causes correctness bugs. Document the cache boundary explicitly.

### Step 4 — Expose cache metrics

Spring Boot auto-publishes cache metrics to Micrometer when caches are registered. Confirm `cache_gets_total{result="hit"|"miss"}` appears in `/actuator/prometheus` — you'll dashboard hit ratio in Task 17.

## Verification

- The cached method returns identical results with caching on (functional parity).
- Second identical call does **not** hit the DB (verify via SQL logs from Task 9 — the query appears once, not twice).
- `cache_*` metrics show in Prometheus.

## Checklist

- [ ] `@EnableCaching` added
- [ ] `@Cacheable` on the profiler-confirmed hot read(s) with stable keys
- [ ] Mutable/workflow data explicitly excluded from caching
- [ ] DB query fires once then is served from cache (verified via SQL log)
- [ ] Cache metrics visible in Prometheus
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Cache keys are a correctness surface.** A sloppy key (e.g. ignoring a parameter that changes the result) returns wrong data. Key on everything that affects the result.
- **`Optional`/null handling:** decide whether to cache "not found" (cache-null can prevent repeated misses but risks staleness on later creation). Note your choice; revisit in Task 15.
- **Don't cache across the hexagonal core.** Keep annotations in adapter/application layers so the domain stays pure.
- **Start with the simple provider** — proving the abstraction before adding Caffeine/Redis isolates bugs (is it the cache logic, or the backend?).

## Learning resources (just-in-time)

- Spring Framework → *Cache Abstraction* (`@Cacheable`, `@CacheEvict`, key generation, `cacheNames`)
- Spring Boot → *Caching* (auto-config, metrics)

## Definition of done

Hot reads go through the Spring Cache abstraction with correct keys and visible metrics, mutable data excluded. **Task 13 swaps the simple provider for Caffeine (local) and tunes size/TTL.**

---

*Next: **[Task 13 — Caffeine local cache](task-13-caffeine-local-cache.md)**.*
