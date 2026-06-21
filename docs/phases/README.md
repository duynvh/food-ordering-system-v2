# Phases

Detailed, week-by-week breakdowns for the [one-year senior-Java plan](../plan.md). Each phase combines several topics into one integrated mission and ends in a single end-to-end depth marker (merged PR + write-up).

| # | Phase | Topics combined | Window | Depth marker |
|---|---|---|---|---|
| 1 | [Observable & Fast](phase-1-observable-and-fast.md) | Observability + Performance + Caching | Jun→Oct 2026 | Before/after dashboard story (trace + flame graph + cache hit ratio + p99 drop) |
| 2 | [Resilient & Secure Edge](phase-2-resilient-and-secure-edge.md) | Microservices (gateway/resilience) + Security | Oct 2026→Feb 2027 | Authenticated order through the gateway w/ JWT propagation **+** chaos demo |
| 3 | [Cloud-Native AI Delivery](phase-3-cloud-native-ai-delivery.md) | Cloud + AI | Feb→Jun 2027 | `terraform apply` stands up the RAG feature on AWS, observed + secured |

**Order matters:** each phase depends on the previous one — Phase 2's chaos demo is validated on Phase 1's dashboards; Phase 3 deploys behind Phase 2's secured edge, observed by Phase 1's stack.
