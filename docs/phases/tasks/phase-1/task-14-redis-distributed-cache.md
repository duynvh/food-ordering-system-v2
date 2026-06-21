# Phase 1 · Task 14 — Redis distributed cache

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone C (Caching), Week 14
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 13](task-13-caffeine-local-cache.md)
> **Outcome:** Redis added to compose and serving as a **distributed (shared) cache** via cache-aside, so multiple service instances share cached data — plus a deliberate local (Caffeine) vs distributed (Redis) strategy.

---

## Overview

Caffeine is per-instance; when you run multiple replicas (which you will in Phase 3), they hold independent, possibly-divergent caches. **Redis** gives all instances one shared cache. This task adds Redis to `infrastructure/docker-compose/`, wires it as a Spring cache provider using the **cache-aside** pattern, and decides which data lives in local cache vs distributed cache (or a two-tier combination).

> Note: Redis also gets reused in Phase 2 as the rate-limit store for Spring Cloud Gateway — so this setup pays off twice.

## Why this matters

- **Shared truth across replicas.** Distributed caching is what makes caching *correct* in a horizontally-scaled system.
- **Cache-aside is the canonical pattern** — knowing when to use it (and its failure modes) is core senior knowledge.
- **Two-tier caching (L1 Caffeine + L2 Redis)** is an advanced, impressive pattern if you choose to implement it.

## Files you'll touch / create

| File | Change |
|---|---|
| `infrastructure/docker-compose/monitoring.yml` (or a new `redis.yml`) | + Redis service |
| relevant `*-container/pom.xml` | + `spring-boot-starter-data-redis` |
| `application.yml` | Redis connection + `spring.cache.type` / serializer config |
| cache config class | `RedisCacheManager` (TTL per cache); optional two-tier setup |

## Step-by-step

### Step 1 — Redis in compose

```yaml
  redis:
    image: redis:7-alpine
    container_name: redis
    ports: ['6379:6379']
```

### Step 2 — Dependency + connection

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Step 3 — Choose the topology

Decide per cache (document the call):
- **Redis-only** for data that must be consistent across instances.
- **Caffeine-only** for tiny, ultra-hot, staleness-tolerant data.
- **Two-tier (L1 Caffeine → L2 Redis → DB)** for the hottest shared data — best latency + sharing, most complexity.

Configure a `RedisCacheManager` with sensible **per-cache TTLs** and a JSON serializer (avoid JDK serialization).

### Step 4 — Cache-aside semantics

Confirm the flow: read → check cache → on miss, load from DB and populate → return. `@Cacheable` already does this; your job is correct TTLs + serialization + key design (reuse Task 12 keys).

### Step 5 — Re-measure + multi-instance sanity check

Run two instances of a cached service (different ports) against shared Redis; confirm a value cached by instance A is served from cache by instance B. Re-run k6; record deltas.

## Verification

- Redis is up; `redis-cli KEYS '*'` shows your cache entries with the expected key pattern after traffic.
- A second service instance reads a value populated by the first (shared cache proven).
- TTLs expire entries as configured (`redis-cli TTL <key>`).
- k6 delta recorded; hit ratio visible (now split local vs Redis if two-tier).

## Checklist

- [ ] Redis added to compose + startup/shutdown
- [ ] `spring-boot-starter-data-redis` added; connection configured
- [ ] Topology decision documented (Redis-only / Caffeine-only / two-tier per cache)
- [ ] `RedisCacheManager` with per-cache TTLs + JSON serializer
- [ ] Cross-instance sharing verified
- [ ] k6 delta recorded
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Serialization matters.** Default JDK serialization is fragile and slow — use a JSON serializer (e.g. `GenericJackson2JsonRedisSerializer`) and ensure cached types are serializable/stable.
- **Network hop tradeoff.** Redis is slower than Caffeine (network vs in-process). Don't move ultra-hot tiny data to Redis-only; that's what two-tier solves.
- **Redis is now a dependency to keep alive.** A Redis outage shouldn't break reads — ensure cache-miss falls through to the DB gracefully (don't let cache errors propagate as request failures).
- **Key collisions across services.** Prefix keys per service/cache (`restaurants::<id>`) so multiple services sharing one Redis don't clash.
- **Don't cache write/saga state in Redis either** — same boundary as Task 12.

## Learning resources (just-in-time)

- Spring Data Redis → *Redis Cache* + serializers
- Spring Boot → *Caching: Redis* (TTL config, `RedisCacheManager`)
- Caching patterns → *cache-aside* overview

## Definition of done

Redis serves as a shared cache via cache-aside with correct TTLs/serialization and proven cross-instance sharing. **Task 15 makes it correct under writes (invalidation) — the non-negotiable part.**

---

*Next: **[Task 15 — Cache invalidation on writes](task-15-cache-invalidation.md)**.*
