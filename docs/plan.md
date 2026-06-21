# One-Year Plan: Sharpen Senior-Level Java Skills → Promotion

## Context

**Goal:** Earn the *senior Java engineer* promotion at the current job within ~12 months by deepening 7 skill areas: microservices, security, performance, observability, caching, AI, cloud.

**Why this shape:** A promotion is decided by people on evidence, so the plan must produce *visible proof*, not just private learning. The existing `food-ordering-system` codebase shows a clear pattern:

- **Already strong (hard to fake):** clean/hexagonal architecture + DDD across 4 services, Saga orchestration, transactional Outbox (with cleaners), Kafka + Avro + Schema Registry, and a thoughtful Spring AI integration (`order-ai`). Java 25 / Spring Boot 3.5.8.
- **Total gaps (max learning ROI, most senior-defining):** observability (no Actuator/Micrometer/tracing), security (no auth/TLS/secrets), caching (none), operational microservices (no gateway/discovery/resilience), containerization of the apps + k8s/cloud/IaC (none), test breadth (4 unit tests).

The gaps *are* the curriculum. The strategy: use the existing system as a **single spine** and add capability as **3 integrated phases that each combine several topics into one coherent mission** — the way senior engineers actually deliver. Knowledge compounds across phases and you finish with one portfolio-grade artifact instead of 7 throwaways.

## Constraints & Decisions

| Decision | Choice |
|---|---|
| Real target | Promotion at current job |
| Time budget | 4–6 hrs/week (~250 hrs/year) |
| Structure | **3 combined phases**, each bundling 2–3 topics into one integrated mission (not 7 separate cycles) |
| Topic coverage | All 7 covered; depth where topics reinforce each other |
| Cadence | ~4 months (~17 weeks, ~83 hrs) per phase → 3 phases across the year |
| Vehicle | Existing `food-ordering-system` spine **+ just-in-time targeted courses/docs** |
| Sequence | Dependency-driven (measure before tuning; secure before exposing; AI as cloud-deployed capstone) |
| Cloud | **AWS** |
| Cert | None (demonstrated impact > badge for internal promotion) |
| Proof | Public repo + per-phase write-up; quarterly real-work initiative; monthly self-review |

**Hard rule:** every phase must end in **one merged PR on the spine + one short write-up** demonstrating the phase's combined capability working end-to-end. "Watched a course" does not count as completion. This is the guard against tutorial drift.

## The 3 Phases (combined topics, dependency-ordered)

Each phase ≈ 83 hrs over ~17 weeks. Budget guide: ~⅔ building on the spine, ~⅓ targeted learning. Each phase is one **integrated mission** with a single end-to-end **depth marker** that only succeeds if all its bundled topics work together. **Full week-by-week detail lives in [`docs/phases/`](phases/README.md)** — the summary below is the map.

| # | Phase | Topics combined | Window | Depth marker | Detail |
|---|---|---|---|---|---|
| 1 | Observable & Fast | Observability + Performance + Caching | Jun→Oct 2026 | Before/after dashboard story: trace + flame graph + cache hit ratio + p99 drop | [→](phases/phase-1-observable-and-fast.md) |
| 2 | Resilient & Secure Edge | Microservices (gateway/resilience) + Security | Oct 2026→Feb 2027 | Authenticated order through the gateway w/ JWT propagation **+** chaos demo | [→](phases/phase-2-resilient-and-secure-edge.md) |
| 3 | Cloud-Native AI Delivery | Cloud + AI | Feb→Jun 2027 | `terraform apply` stands up the RAG feature on AWS, observed + secured | [→](phases/phase-3-cloud-native-ai-delivery.md) |

**Phases interlock:** Phase 2's chaos demo is validated on Phase 1's dashboards; Phase 3 deploys behind Phase 2's secured edge, observed by Phase 1's stack. Each phase consumes the previous phase's output — that compounding is the whole point of combining topics.

## Rhythm & Accountability

- **Weekly (~5 hrs):** ~⅔ build on spine, ~⅓ targeted learning. One small PR-in-progress.
- **Within a phase:** treat each bundled topic as a sub-milestone (e.g. Phase 1 = instrument → load-test → cache), but they share one branch and one end-to-end goal.
- **End of each phase:** merge the PR + write a ~700-word post (the mission, the topics it combined, tradeoffs, before/after metrics). Builds the public portfolio and sharpens articulation.
- **Monthly (30 min):** review shipped vs slipped; re-plan next month. Absorbs bad weeks before they derail the year.
- **Quarterly — bring 1 capability into real work** (strongest promotion signal): ~Sep 2026 observability/perf, ~Dec 2026 caching + resilience, ~Mar 2027 security + gateway, ~Jun 2027 cloud + AI. Keep a running **impact log** for the promo packet.

## Risks & Mitigations

- **Phase too big / scope creep** → each phase has ONE end-to-end depth marker; bundled topics are sequenced as sub-milestones so a phase still ships even if a stretch goal is dropped.
- **Promotion disconnect** (self-study nobody sees) → the quarterly real-work initiative + impact log is the bridge; align each initiative with a competency on your company's ladder.
- **Cloud cost** → local k8s first, AWS free tier, tear down between sessions.
- **Bad weeks / burnout** → 4–6 hr range is built in; monthly re-plan absorbs slippage; phases are independent enough to swap order if one is blocked.
- **Bleeding edge (Java 25, Spring AI 1.1)** → some libs may lag; pin versions and note workarounds in write-ups.

## Verification (how you'll know it worked)

This is a learning plan, so "verification" = objective completion signals, not test runs:

1. **Per phase:** the integrated depth marker is demonstrable (observable+fast dashboard story / secured+resilient gateway demo / cloud-deployed RAG feature) **AND** the PR is merged **AND** the write-up is published.
2. **Per quarter:** at least one capability is running in (or proposed to) real production work, logged in the impact log.
3. **Monthly:** the self-review is completed and next month re-planned.
4. **End of year:** the spine repo demonstrably runs an observed, tuned, cached, resilient, secured, cloud-deployed, AI-augmented system — and you have a written impact log mapping each phase to your promotion criteria.
