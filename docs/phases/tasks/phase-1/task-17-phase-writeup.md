# Phase 1 · Task 17 — Re-measure + Phase 1 write-up

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone C (Caching), Week 17
> **Estimated effort:** ~5 hrs
> **Depends on:** all prior Phase 1 tasks
> **Outcome:** The full before/after story assembled into the Phase 1 **depth marker**, the PR merged, and a ~700-word write-up published. This is what turns 17 weeks of work into promotion evidence.

---

## Overview

This is the closing task: run the load test one final time with everything in place (observability + tuning + caching), capture the **before/after** against the Task 7 baseline, assemble the depth-marker artifacts, merge the Phase 1 PR, and write the public post. The hard rule from the plan: **a phase isn't done until the PR is merged and the write-up exists** — "watched a course" doesn't count.

## Why this matters

- **This is the promotion artifact.** Numbers + dashboards + a clear narrative is exactly what a committee can read.
- **Writing forces understanding.** Explaining the tradeoffs out loud exposes anything you faked your way through.
- **It feeds the quarterly real-work initiative** — the observability/perf capability you can now pitch at your day job.

## The Phase 1 depth marker (assemble all four)

A single **before/after dashboard story** for the order-creation path:

1. **Distributed trace** of one order across all services (Jaeger, from Task 5).
2. **Flame graph** identifying the original bottleneck (Task 8).
3. **Cache hit-ratio** panel (Tasks 13/14, dashboard here).
4. **Measured p50/p95/p99 latency + throughput improvement** vs the Task 7 baseline — real numbers.

## Step-by-step

### Step 1 — Build the cache hit-ratio panel

Add to the Grafana dashboards (commit the JSON): `sum(rate(cache_gets_total{result="hit"}[1m])) / sum(rate(cache_gets_total[1m]))` per cache. Split local (Caffeine) vs Redis if two-tier.

### Step 2 — Final load run

Run `perf/k6/order-create.js` under identical conditions to Task 7 (same data volume, same machine state). Capture the full summary + dashboard screenshots at peak.

### Step 3 — Compile before/after

In `perf/baselines/phase1-final.md`, put baseline vs final side by side: p50/p95/p99, throughput, error rate, cache hit ratio, DB query rate, GC pauses. Call out which task drove which gain (DB index → X, caching → Y, etc.).

### Step 4 — Assemble depth-marker artifacts

Collect the trace screenshot, flame graph, hit-ratio panel, and the numbers table into one place (`docs/` or the PR description).

### Step 5 — Merge the PR

Open/finish the Phase 1 PR (the whole sub-milestone work on one branch). Self-review against every prior task's checklist. Squash-or-merge per your repo convention.

### Step 6 — Write the ~700-word post

Structure: the mission (observable→fast) → what each topic contributed → the hardest part (Kafka trace propagation) → the numbers → tradeoffs you accepted (staleness windows, vthread verdict) → what you'd do next. Publish to your public repo/blog.

## Verification

- All four depth-marker artifacts exist and are linked from the PR/write-up.
- Before/after numbers are real, reproducible, and from identical conditions.
- PR merged; write-up published.
- The Phase 1 [deliverables checklist](../../phase-1-observable-and-fast.md#deliverables) is fully ticked.

## Checklist

- [ ] Cache hit-ratio panel added + dashboards committed
- [ ] Final k6 run under baseline-identical conditions
- [ ] Before/after table compiled with per-task attribution
- [ ] Depth marker assembled (trace + flame graph + hit ratio + numbers)
- [ ] Phase 1 PR merged
- [ ] ~700-word write-up published
- [ ] Phase 1 deliverables checklist complete
- [ ] Capability noted for the quarterly real-work initiative + impact log

## Notes & gotchas

- **Apples to apples or it's worthless.** Same seed data, same machine, same load stages as Task 7. If conditions drifted, re-run the baseline too.
- **Attribute honestly.** If caching gave the big win and virtual threads didn't, say so. The credibility *is* the value.
- **Don't bury the lede.** Lead the write-up with the headline number (e.g. "p95 down 73% at 50 RPS").
- **Update the impact log** (from the plan's accountability section) — this is the bridge to the promotion case.

## Learning resources (just-in-time)

- Your own `perf/baselines/` and dashboards — the primary sources
- "How to write an engineering brag/impact doc" (skim) for the write-up structure

## Definition of done

The before/after depth marker is assembled and demonstrable, the PR is merged, and the write-up is published. **Phase 1 — Observable & Fast is complete.** Next phase: [Resilient & Secure Edge](../../phase-2-resilient-and-secure-edge.md).

---

*End of Phase 1 tasks. See the [Phase 1 task index](README.md).*
