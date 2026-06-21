# Phase 1 · Task 8 — JFR + async-profiler flame graphs

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone B (Performance), Week 8
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 7](task-07-k6-load-baseline.md) (you profile *under* the load test)
> **Outcome:** Flame graphs captured while the system is under k6 load, with the **top 2–3 hotspots identified** — the evidence that drives Tasks 9–11 (don't guess, measure).

---

## Overview

Metrics say "the order path is slow"; a **profiler** says "62% of that time is in Avro serialization / JPA flush / JSON mapping." This task runs **JDK Flight Recorder (JFR)** and **async-profiler** against a service *while k6 drives it*, then reads the **flame graphs** to pinpoint the real hotspots so your tuning effort lands where it matters.

## Why this matters

- **Stops you optimizing the wrong thing.** Most "obvious" bottlenecks are wrong; the profiler is ground truth.
- **The flame graph is part of the depth marker.** "Here's the hotspot, here's the fix, here's the after" is the whole story.
- **JFR/async-profiler fluency is a senior skill** — many engineers have never captured one.

## Files you'll touch / create

| File | Change |
|---|---|
| `perf/profiles/` | **new** — store `.jfr` recordings + flame-graph SVGs/HTML |
| `perf/baselines/phase1-baseline.md` | append the hotspot findings |
| (run config / launch flags) | enable JFR; attach async-profiler |

## Step-by-step

### Step 1 — Pick the target & start load

Profile the busiest JVM first — `order-service` (orchestrator) is the usual suspect. Start the stack, start the k6 sustained stage from Task 7 so the JVM is genuinely busy while you record.

### Step 2 — JFR recording

Either start with flags or attach via `jcmd`:

```bash
# attach to a running pid for 120s, dump to file
jcmd <pid> JFR.start name=order duration=120s filename=perf/profiles/order.jfr settings=profile
```

Open `order.jfr` in JDK Mission Control → look at *Method Profiling*, *Hot Methods*, *Allocation*, and *Garbage Collection*.

### Step 3 — async-profiler flame graph (CPU + alloc)

```bash
# CPU flame graph
./asprof -d 120 -f perf/profiles/order-cpu.html <pid>
# allocation flame graph
./asprof -d 120 -e alloc -f perf/profiles/order-alloc.html <pid>
```

### Step 4 — Read the graphs

Wide frames = where time/allocations concentrate. Likely candidates in this stack:
- **Avro serialization / Schema Registry** round-trips (Kafka path)
- **JPA/Hibernate** flush, dirty-checking, or N+1 query loops (→ Task 9)
- **JSON (de)serialization** on the REST boundary
- **Outbox polling** overhead if scheduler intervals are aggressive

Record the **top 2–3** with a one-line hypothesis + intended fix for each.

## Verification

- At least one `.jfr` and one CPU flame graph captured **under load** (not idle — idle profiles are useless).
- A written shortlist of the top 2–3 hotspots, each mapped to a planned task (DB → Task 9, threading → Task 10, GC → Task 11, caching → Sub-milestone C).

## Checklist

- [ ] JFR recording captured under k6 load, committed to `perf/profiles/`
- [ ] async-profiler CPU flame graph captured
- [ ] (bonus) allocation flame graph captured
- [ ] Top 2–3 hotspots written down with hypotheses + target tasks
- [ ] Findings appended to the baseline doc
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Profile under load or don't bother.** An idle JVM's flame graph tells you nothing about the order path.
- **async-profiler needs perf permissions.** On Linux you may need `kernel.perf_event_paranoid` lowered or `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` for accurate stacks. On Windows, run via WSL or use JFR alone.
- **JFR overhead is low (~1–2%)** so its numbers are trustworthy; async-profiler is great for flame-graph shape.
- **Don't fix anything yet.** This task is diagnosis only. Resist the urge to start tuning mid-profile — capture first, then fix in the dedicated tasks.
- **Allocation pressure → GC** — if the alloc flame graph is hot, that feeds Task 11 (GC tuning).

## Learning resources (just-in-time)

- JDK Mission Control docs → *Method profiling*, *Automated analysis*
- async-profiler README → *CPU profiling*, *Allocation profiling*, *Flame graphs*
- "Reading flame graphs" (Brendan Gregg) — short primer

## Definition of done

Flame graphs captured under load and a prioritized hotspot shortlist exist, each pointing at a concrete follow-up task. **Task 9 tackles the most common winner: database access.**

---

*Next: **[Task 9 — Database: N+1, indexes, query tuning](task-09-db-tuning.md)**.*
