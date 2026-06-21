# Phase 1 · Task 15 — Cache invalidation on writes

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone C (Caching), Week 15
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 14](task-14-redis-distributed-cache.md)
> **Outcome:** Writes correctly invalidate (or update) cached entries, with a **proven stale-read scenario fixed** — the non-negotiable correctness gate of the caching sub-milestone.

---

## Overview

Caching's hard problem isn't speed — it's **staleness**. When data changes, the cache must reflect it. This task adds `@CacheEvict`/`@CachePut` on the write paths that mutate cached entities, then *proves* a previously-broken stale-read scenario is now correct. The phase's own risk note says it plainly: **caching correctness > caching speed; a fast wrong answer is worse than a slow right one.**

## Why this matters

- **Correctness is the gate.** A cache that serves stale data is a bug factory; fixing invalidation is what makes the whole sub-milestone trustworthy.
- **Demonstrating the bug, then the fix** is exactly the kind of rigor a promotion review rewards.
- **Distributed invalidation** (across instances + Redis) is genuinely tricky — handling it is a senior signal.

## Step-by-step

### Step 1 — Map writes to caches

For each cached entity (restaurant, customer), find every path that creates/updates/deletes it. In this codebase, customer changes flow via the `CustomerCreateCommandHandler` / customer write path, and restaurant data via its admin/update path. Each write that changes a cached entity must touch the cache.

### Step 2 — Annotate write paths

```java
@CacheEvict(cacheNames = "customers", key = "#customer.id")
public Customer updateCustomer(Customer customer) { ... }
```

Use `@CachePut` when you want to refresh the cached value with the write's result instead of evicting; use `@CacheEvict` when simply dropping it is safer. Prefer evict for correctness unless you can guarantee the put value is canonical.

### Step 3 — Reproduce a stale read FIRST

Before fixing, demonstrate the bug: read entity (caches it) → update it directly in DB or via API → read again → observe the **stale** cached value. Capture this as a failing test. This is your proof.

### Step 4 — Fix and confirm

Add the eviction; rerun the scenario → second read now reflects the update. Turn the reproduction into a **passing automated test** so it can't regress.

### Step 5 — Distributed invalidation check

With two instances + Redis (from Task 14): instance A updates and evicts the Redis key; confirm instance B no longer serves stale. Note: instance B's **local Caffeine** (if two-tier) may still be stale until its TTL — decide how to handle (short L1 TTL, or pub/sub eviction). Document the chosen tradeoff.

## Verification

- A test proves: read → update → read returns the **new** value (no staleness) on the cached path.
- Redis key is evicted/updated on write (`redis-cli` before/after).
- Multi-instance: B reflects A's write (within the documented L1 TTL window if two-tier).

## Checklist

- [ ] All write paths for cached entities mapped
- [ ] `@CacheEvict`/`@CachePut` applied appropriately
- [ ] Stale-read scenario reproduced (failing test) **then** fixed (passing test)
- [ ] Redis eviction confirmed on write
- [ ] Distributed/L1 staleness window documented with chosen handling
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **The local-cache staleness window is the subtle one.** Redis evicts globally, but each instance's Caffeine doesn't know. Options: keep L1 TTL short, use Redis pub/sub to broadcast evictions, or accept a bounded window. **Pick and document** — pretending it doesn't exist is the failure mode.
- **Evict on the transaction commit, not before.** If you evict then the write rolls back, you've thrown away a valid cache entry (minor) — but if you populate from a not-yet-committed value, you cache uncommitted data (serious). Be careful with ordering around `@Transactional`.
- **Don't over-evict.** `allEntries=true` on every write nukes the cache and kills your hit ratio. Evict by key.
- **Tests are the deliverable here**, more than code — they're what proves correctness now and prevents regressions later.

## Learning resources (just-in-time)

- Spring Framework → *Cache Abstraction*: `@CacheEvict`, `@CachePut`, `@Caching`, condition/unless
- Redis → *pub/sub* (if you implement broadcast eviction)
- "Cache invalidation strategies" overview

## Definition of done

Writes keep the cache correct, a reproduced stale-read is fixed and guarded by a test, and the distributed staleness window is documented. **Task 16 hardens the remaining failure mode: cache stampede.**

---

*Next: **[Task 16 — Cache stampede mitigation](task-16-cache-stampede.md)**.*
