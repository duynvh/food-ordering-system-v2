# Phase 1 · Task 7 — k6 load test + baseline

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone B (Performance), Week 7
> **Estimated effort:** ~5 hrs
> **Depends on:** Sub-milestone A (you need dashboards to read the load on)
> **Outcome:** A repeatable k6 load test for the order-creation flow and a **committed baseline** (throughput + p50/p95/p99) — the "before" half of the Phase 1 depth marker.

---

## Overview

You can't claim a performance improvement without a number to beat. This task writes a **k6** load test that drives the order-creation flow and records a baseline against the *current* (un-tuned, un-cached) system. Every later perf/caching task re-runs this exact script so the before/after comparison is honest.

## Why this matters

- **No baseline = no story.** "It feels faster" is not a promotion artifact; "p95 dropped from 420ms to 110ms under 50 RPS" is.
- **Repeatability is the whole point.** Same script, same environment, every run — otherwise you're comparing noise.
- **Reveals the real bottleneck** for Task 8's profiler to confirm.

## Files you'll touch / create

| File | Change |
|---|---|
| `perf/k6/order-create.js` | **new** — the load script |
| `perf/k6/README.md` | **new** — how to run + environment notes |
| `perf/baselines/phase1-baseline.md` | **new** — committed numbers + run conditions |

(Use a top-level `perf/` dir so load assets aren't buried in `docs/`.)

## Step-by-step

### Step 1 — Capture a valid order request

Use the real `order-service` REST endpoint (`OrderController`, `:8181`). Grab a working request body from the existing tests/`init-schema.sql` seed data (valid customerId, restaurantId, product ids/prices) so orders actually succeed, not 400.

### Step 2 — Write the k6 script

`perf/k6/order-create.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },   // ramp
    { duration: '2m',  target: 50 },   // sustained load
    { duration: '30s', target: 0 },    // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],  // starting guardrail; tighten after tuning
    http_req_failed:   ['rate<0.01'],
  },
};

const payload = JSON.stringify({ /* valid order body */ });
const params = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  const res = http.post('http://localhost:8181/orders', payload, params);
  check(res, { 'status is 2xx': (r) => r.status >= 200 && r.status < 300 });
  sleep(1);
}
```

### Step 3 — Run against the full stack

Bring up infra + all four services + the monitoring stack. Run:

```bash
k6 run perf/k6/order-create.js
```

Watch your Grafana dashboards **during** the run — JVM, HTTP p95, and Kafka consumer lag should all move. The async saga means order *acceptance* is fast but downstream completion shows as consumer lag; note both.

### Step 4 — Record the baseline

In `perf/baselines/phase1-baseline.md` capture: machine specs, JVM flags, k6 stage config, and k6's summary (iterations, RPS, p50/p90/p95/p99, error rate), plus screenshots of the dashboards at peak.

## Verification

- k6 completes with error rate < 1% (orders actually succeed — if not, fix the payload before trusting numbers).
- Grafana clearly shows the load window.
- Baseline file committed with reproducible run conditions.

## Checklist

- [ ] `perf/k6/order-create.js` posting valid orders
- [ ] Thresholds defined (starting guardrails)
- [ ] Run executed against the full local stack with monitoring up
- [ ] Baseline numbers + dashboard screenshots committed
- [ ] Run conditions documented (specs, flags, stages) for reproducibility
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Async vs sync latency.** The order endpoint likely returns once the order is *accepted* (saga runs async via Kafka). So HTTP p95 measures acceptance, while end-to-end completion is visible as consumer lag draining. Measure and report **both** — conflating them is a classic mistake.
- **Warm up the JVM.** The first seconds include JIT warm-up; the ramp stage handles this — report steady-state numbers.
- **Environment skew is the enemy.** Always run with the same services up, same data volume. Note if your laptop is on battery/thermal-throttling.
- **Seed enough data.** A tiny DB hides N+1 and index problems you want Task 9 to find — consider seeding realistic row counts.

## Learning resources (just-in-time)

- k6 docs → *Running k6*, *Test life cycle*, *Thresholds*, *Metrics*
- k6 docs → *HTTP requests* + *Checks*

## Definition of done

A committed, repeatable k6 test and a documented baseline with p50/p95/p99 and throughput. **Task 8 (profiling) uses this load to find where the time actually goes.**

---

*Next: **[Task 8 — JFR + async-profiler flame graphs](task-08-profiling-flamegraphs.md)**.*
