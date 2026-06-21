# Phase 2 — Resilient & Secure Edge

> **Combines:** Microservices (gateway/resilience) + Security
> **Window:** ≈ late Oct 2026 → late Feb 2027 (~17 weeks, ~83 hrs)
> **Mission:** Make the system *safe to expose*. A gateway is where routing, resilience, and authentication naturally converge — so the operational-microservices gap and the security gap are one piece of work, not two.

Part of the [one-year senior-Java plan](../plan.md). Depends on Phase 1: the chaos demo and auth flows here are validated on the [Phase 1](phase-1-observable-and-fast.md) dashboards/traces.

---

## Why these two together

The **API gateway** is the single seam where:

- routing + rate limiting live (operational microservices), **and**
- authentication is enforced and **JWT propagation** begins (security).

Building a gateway without auth leaves an open door; bolting auth on without a gateway scatters it across every service. Done together, the gateway *is* the security boundary, and resilience policies protect it.

## Starting point in the codebase

| Concern | Current state | Where |
|---|---|---|
| API gateway | None — services expose controllers directly | `*Controller` in each `*-application` |
| Service discovery / config | None | — |
| Resilience (circuit breaker/retry) | None | cross-service calls |
| AuthN / AuthZ | None | all endpoints open |
| TLS | None | all plaintext |
| Secrets | Plaintext in `application.yml` | e.g. `postgres/admin` |
| Contract tests | None | event + REST contracts |

## Sub-milestones & week-by-week

### Sub-milestone A — Resilient edge (weeks 1–8, ~40 hrs)
- **W1** — Add a **Spring Cloud Gateway** module/service; route to `order`, `payment`, `restaurant`, `customer` controllers. Confirm traffic flows through it (visible in Phase-1 traces).
- **W2** — Gateway concerns: path routing, **rate limiting** (Redis from Phase 1 as the rate-limit store), request logging with trace propagation.
- **W3** — **Resilience4j circuit breaker** on cross-service / downstream calls; expose its metrics to Phase-1 Grafana.
- **W4** — **Retry + timeout + bulkhead** policies; tune against the k6 load test from Phase 1.
- **W5** — Externalized configuration (Spring Cloud Config or profile/env-based); prepare for k8s-native config in Phase 3.
- **W6** — Service discovery (Eureka/Consul now, or defer to k8s DNS in Phase 3 — decide and document).
- **W7–W8** — **Contract testing** (Spring Cloud Contract or Pact) for the Kafka event contracts (Avro in `kafka-model`) and the REST endpoints. Wire into CI.

**Exit check:** kill `payment-service`; the circuit breaker opens, the gateway degrades gracefully, and you can *see* it happen on the Phase-1 dashboards.

### Sub-milestone B — Security (weeks 9–17, ~43 hrs)
- **W9** — Stand up **Keycloak** in `infrastructure/docker-compose/`; create realm, clients, roles, test users.
- **W10** — Add **Spring Security + OAuth2 Resource Server** to the gateway; validate JWTs at the edge.
- **W11** — **JWT propagation**: forward the token (or a derived service token) downstream so each service can authorize; validate in `order/payment/restaurant/customer` services.
- **W12** — **Role-based authorization** on endpoints (method security / route rules); map realm roles to actions.
- **W13** — **TLS**: terminate at the gateway and/or enable service-to-service TLS; generate/manage local certs.
- **W14** — **Secrets out of `application.yml`**: move DB/Kafka/Keycloak credentials to env vars + a secrets mechanism (Vault locally now, k8s secrets in Phase 3).
- **W15** — **Secure Kafka** (SASL/SSL) at least in the local cluster; update producers/consumers in `infrastructure/kafka`.
- **W16** — Extend the existing **LLM injection hardening** in `order-ai` with explicit input validation + a short threat model of the order flow.
- **W17** — End-to-end verification + phase write-up.

## Depth marker (definition of done)

Both, demonstrated together on the Phase-1 dashboards:

1. An **authenticated order** flows end-to-end *through the gateway* with **JWT propagation** across all services (visible in a trace), **and**
2. A **chaos demo** — kill `payment-service`, watch the circuit breaker open and the system degrade gracefully — captured on Grafana.

## Learning resources (just-in-time)

- Spring Cloud Gateway reference docs
- Resilience4j docs (circuit breaker, retry, bulkhead, time limiter)
- Spring Security + OAuth2 Resource Server docs
- Keycloak getting-started / realm setup
- Spring Cloud Contract (or Pact) docs

## Deliverables

- [ ] Gateway service/module routing all traffic, with rate limiting
- [ ] Resilience4j policies on cross-service calls, metrics on Grafana
- [ ] Contract tests for Avro events + REST, running in CI
- [ ] Keycloak in compose; OAuth2 at gateway; JWT propagation across services
- [ ] Role-based authz, TLS, secrets externalized, Kafka SASL/SSL (local)
- [ ] Threat-model note for the order flow (`docs/`)
- [ ] **One merged PR** + **~700-word write-up**

## Phase-specific risks

- **Auth touches every service** — introduce it behind the gateway first, then propagate inward incrementally; don't big-bang it.
- **TLS/secrets locally vs cloud** — keep the local setup simple (self-signed, Vault dev mode); the production-grade version is finished in Phase 3 with k8s.
- **Circuit-breaker tuning needs load** — reuse the Phase-1 k6 test so thresholds are based on real numbers.
