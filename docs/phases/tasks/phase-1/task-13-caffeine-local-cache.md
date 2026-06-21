# Phase 1 · Task 13 — Caffeine local cache

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone C (Caching), Week 13
> **Estimated effort:** ~4 hrs
> **Depends on:** [Task 12](task-12-spring-cache-abstraction.md)
> **Outcome:** Caffeine backing the cache abstraction for the hottest, low-churn reads, with tuned **size + TTL** and visible hit ratios.

---

## Overview

Caffeine is a high-performance in-JVM cache — ideal for small, frequently-read, slowly-changing data (e.g. restaurant/menu metadata). This task makes Caffeine the provider behind the `@Cacheable` annotations from Task 12 and tunes **maximum size** and **TTL/expiry** so the cache is effective without unbounded memory growth.

## Why this matters

- **Local cache = nanosecond reads, zero network.** For hot lookups it's the single biggest latency win available.
- **Bounded + TTL'd is the senior version.** An unbounded cache is a memory leak; a no-TTL cache is a staleness bug. Tuning both is the skill.
- **Sets up the local-vs-distributed tradeoff** you'll resolve in Task 14 (Redis).

## Files you'll touch

| File | Change |
|---|---|
| relevant `*-container/pom.xml` | + `com.github.ben-manes.caffeine:caffeine` |
| cache config class | define `CaffeineCacheManager` with per-cache specs |
| `application.yml` | optional: `spring.cache.caffeine.spec` |

## Step-by-step

### Step 1 — Dependency

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

(Version is BOM-managed by Spring Boot.)

### Step 2 — Configure the cache manager

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m,recordStats
```

Or per-cache via a `CaffeineCacheManager` bean if different caches need different specs (e.g. menus 10m, customers 2m). `recordStats` is what feeds hit-ratio metrics.

### Step 3 — Tune from data

Set `maximumSize` from how many distinct entities are actually hot (don't cache the whole table). Set `expireAfterWrite` from how stale you can tolerate the data being — short enough to be safe, long enough to get hits. Document the reasoning per cache.

### Step 4 — Re-measure on the load test

Re-run Task 7's k6 with Caffeine on. Watch hit ratio climb and DB query rate fall on Grafana.

## Verification

- `cache_gets_total{result="hit"}` rises over a sustained run (warm cache → high hit ratio).
- DB query rate for the cached entity drops materially under load.
- Memory stays bounded (heap doesn't grow unbounded — `maximumSize` enforced).
- k6 p95/p99 for the cached path improves; numbers recorded.

## Checklist

- [ ] Caffeine dependency added
- [ ] Cache manager configured with `maximumSize` + `expireAfterWrite` + `recordStats`
- [ ] Per-cache TTLs justified by staleness tolerance
- [ ] Hit ratio rises; DB load drops (shown on Grafana)
- [ ] Heap stays bounded under load
- [ ] k6 delta recorded
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **`recordStats` is required for metrics** — without it, hit ratio is invisible and Task 17's dashboard is empty.
- **Local cache ≠ shared.** Each service instance has its own Caffeine. With multiple replicas (Phase 3), they can disagree until TTL expiry — that's the exact reason Task 14 adds Redis. Note the tradeoff now.
- **`expireAfterWrite` vs `expireAfterAccess`:** write-based expiry bounds staleness deterministically (usually what you want for correctness); access-based keeps hot keys forever (better hit ratio, looser staleness). Choose deliberately.
- **Don't over-size.** A huge `maximumSize` just trades the DB problem for a GC problem.

## Learning resources (just-in-time)

- Caffeine wiki → *Eviction*, *Statistics*
- Spring Boot → *Caching: Caffeine* (`spring.cache.caffeine.spec`)

## Definition of done

Caffeine backs the hot reads with tuned, justified size/TTL and rising hit ratios proven on the load test. **Task 14 adds Redis for cross-instance (distributed) caching and the local↔distributed strategy.**

---

*Next: **[Task 14 — Redis distributed cache](task-14-redis-distributed-cache.md)**.*
