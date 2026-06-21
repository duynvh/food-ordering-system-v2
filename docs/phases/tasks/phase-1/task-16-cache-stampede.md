# Phase 1 · Task 16 — Cache stampede mitigation

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone C (Caching), Week 16
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 15](task-15-cache-invalidation.md)
> **Outcome:** The hot cache is protected against stampede (thundering herd on expiry/cold start), with the chosen consistency tradeoff documented.

---

## Overview

A **cache stampede** (thundering herd) happens when a hot key expires (or the cache is cold) and many concurrent requests all miss simultaneously, hammering the DB at once — sometimes worse than having no cache. This task adds protection: request coalescing (single-flight), TTL jitter, and/or background refresh, then verifies it under a burst load. It's the last hardening step before measuring the full caching win.

## Why this matters

- **Expiry is a traffic spike generator.** Without mitigation, your nicely-cached system periodically face-plants the DB exactly when busy.
- **This is the difference between "added a cache" and "engineered a cache."** Stampede handling is a senior-level concern most implementations miss.
- **It interacts with everything prior** — TTLs (Task 13/14) and invalidation (Task 15) all affect stampede behavior.

## Step-by-step

### Step 1 — Reproduce the stampede

With a hot key, force expiry (or flush Redis) and fire a concurrent burst with k6 (high VUs, same entity). Observe a spike of DB queries for the one key and a latency blip. That's the stampede — capture it.

### Step 2 — Choose mitigation(s)

- **Request coalescing / single-flight (local):** Caffeine's `LoadingCache` with `get(key, mappingFunction)` already coalesces concurrent loads per instance — only one thread computes, others wait. Prefer this for L1.
- **TTL jitter:** randomize expiry (`baseTtl ± random`) so keys don't all expire at the same instant. Cheap and effective for many-keys stampede.
- **Distributed lock (Redis):** for an expensive shared recompute, a short Redis lock (`SET NX PX`) ensures one instance refreshes while others serve stale/wait. Use sparingly — adds complexity.
- **Refresh-ahead:** Caffeine `refreshAfterWrite` recomputes hot keys in the background before they expire, so reads never see a miss.

### Step 3 — Implement and document the tradeoff

Pick the simplest combination that fixes *your* observed stampede (often: Caffeine `LoadingCache` + TTL jitter + `refreshAfterWrite` on the hottest cache). Write down the **consistency tradeoff** you accepted (e.g. "serve slightly stale during refresh-ahead window to avoid herd").

### Step 4 — Re-run the burst

Repeat Step 1's burst → DB query spike for the key should collapse to ~1 (coalesced) or be smoothed (jitter/refresh-ahead); latency blip gone.

## Verification

- Burst test before: many concurrent DB hits for one key + latency spike.
- Burst test after: single (or minimal) DB load per expiry; smooth latency.
- The accepted consistency tradeoff is documented.

## Checklist

- [ ] Stampede reproduced and captured (DB spike on concurrent miss)
- [ ] Mitigation chosen (coalescing and/or jitter and/or refresh-ahead and/or lock)
- [ ] Implemented on the hottest cache(s)
- [ ] Consistency tradeoff written down
- [ ] Burst re-test shows herd eliminated/smoothed
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Don't over-engineer.** A distributed Redis lock for every cache is overkill; most stampedes die to `LoadingCache` coalescing + TTL jitter. Match the tool to the observed problem.
- **Refresh-ahead serves stale by design** during refresh — fine for menus, not for anything correctness-critical. Align with the Task 15 boundary.
- **Cold start is a stampede too.** On deploy, all caches are empty; consider warming the hottest keys at startup (optional, note it for Phase 3 rollouts).
- **Locks can deadlock/expire.** If you use a Redis lock, always set an expiry (`PX`) so a crashed holder can't wedge the key forever.

## Learning resources (just-in-time)

- Caffeine wiki → *Population* (`LoadingCache`), *Refresh* (`refreshAfterWrite`)
- "Cache stampede / thundering herd" overview (coalescing, jitter, lock)
- Redis → *distributed locks* (`SET NX PX`) — only if you go that route

## Definition of done

The hot cache survives a concurrent-miss burst without stampeding the DB, with a documented consistency tradeoff. **Task 17 measures the total caching win and writes the Phase 1 story.**

---

*Next: **[Task 17 — Re-measure + Phase 1 write-up](task-17-phase-writeup.md)**.*
