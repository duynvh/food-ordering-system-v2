# Phase 1 · Task 1 — Wire Actuator + Micrometer into every service

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone A (Observability), Week 1
> **Estimated effort:** ~5 hrs (one week of the plan)
> **Outcome:** Every service exposes `/actuator/health` (with readiness/liveness probes) and `/actuator/prometheus` so the next task (stand up Prometheus + Grafana) has something to scrape.

---

## Overview

Right now none of the four services expose any operational endpoints — no health, no metrics. This task adds **Spring Boot Actuator** (the endpoint framework) and the **Micrometer Prometheus registry** (which renders metrics in the format Prometheus scrapes) to all four `*-container` modules, then configures which endpoints are exposed and turns on Kubernetes-style **readiness/liveness** health groups (you'll need those in Phase 3 anyway).

This is the foundation stone of the entire year: you can't tune, cache, or trace anything you can't measure, and every later task reads off the metrics you switch on here.

## Why this matters

- **Unblocks everything downstream.** Task 2 (Prometheus + Grafana) literally has nothing to scrape until these endpoints exist.
- **Senior signal.** "We had no idea what production was doing" → "every service publishes JVM, HTTP, and Kafka metrics" is exactly the operability jump promotion committees read as seniority.
- **Near-zero risk.** Actuator is additive — no business logic changes, no API changes. A safe, high-value first move.

## Prerequisites

- The stack builds and runs locally (`mvn clean install` at the root, infra up via `infrastructure/docker-compose/`).
- You can start at least the `order-service` and hit its REST API on `:8181`.
- No new version pins needed — the root `pom.xml` uses `spring-boot-starter-parent`, so the Spring Boot **BOM manages all versions** (Actuator and Micrometer included). Add dependencies *without* `<version>`.

## ⚠️ Read this first — the one non-obvious gotcha

Actuator's `/actuator/prometheus` is served over **HTTP**, so a service needs a **web server** to be scrapable. In this codebase:

| Service | Port | Web server today? | Action |
|---|---|---|---|
| `order-service` | 8181 | ✅ yes (`OrderController`) | Actuator works as-is |
| `customer-service` | 8184 | ✅ yes (`CustomerController`) | Actuator works as-is |
| `payment-service` | 8182 | ❌ **Kafka-only, no web server** | **must add `spring-boot-starter-web`** |
| `restaurant-service` | 8183 | ❌ **Kafka-only, no web server** | **must add `spring-boot-starter-web`** |

If you skip the web starter on `payment`/`restaurant`, the app boots fine but Prometheus can never reach it and you'll lose two services from your dashboards in Task 2. Don't skip it.

## Files you'll touch

| File | Change |
|---|---|
| `order-service/order-container/pom.xml` | + actuator, + micrometer-prometheus |
| `customer-service/customer-container/pom.xml` | + actuator, + micrometer-prometheus |
| `payment-service/payment-container/pom.xml` | + actuator, + micrometer-prometheus, **+ web** |
| `restaurant-service/restaurant-container/pom.xml` | + actuator, + micrometer-prometheus, **+ web** |
| `*/.../src/main/resources/application.yml` (all 4) | management endpoint exposure + health probes |

## Step-by-step

### Step 1 — Add dependencies to all four container poms

In each `*-container/pom.xml`, inside `<dependencies>`, add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**For `payment-container` and `restaurant-container` only**, also add a web server so the endpoints are reachable:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

> No `<version>` anywhere — the Spring Boot parent BOM supplies them. Keeps you consistent with the rest of the project.

### Step 2 — Expose the endpoints + enable health probes (all four `application.yml`)

Add this block to each container's `src/main/resources/application.yml` (top level, sibling of `server:` / `spring:`):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true          # adds /actuator/health/readiness and /actuator/health/liveness
  metrics:
    tags:
      application: ${spring.application.name:order-service}   # set per service
  prometheus:
    metrics:
      export:
        enabled: true
```

Set the `application` tag per service (`order-service`, `payment-service`, `restaurant-service`, `customer-service`) — that tag is how Grafana will let you filter one service from another in Task 3. If `spring.application.name` isn't set yet, add it under `spring:` in each file:

```yaml
spring:
  application:
    name: order-service   # change per service
```

### Step 3 — Rebuild

From the repo root:

```bash
mvn clean install -DskipTests
```

(Skip tests just for this fast iteration; the full build runs them.)

### Step 4 — Start the services and the infra

Bring up Kafka/Postgres via `infrastructure/docker-compose/startup.sh`, then run each service (IDE run config or `java -jar */*-container/target/*-container-1.0-SNAPSHOT.jar`). Start with `order-service` to validate the happy path before doing the Kafka-only two.

## Verification

Hit each endpoint and confirm output:

```bash
# order-service (already has web)
curl http://localhost:8181/actuator/health
curl http://localhost:8181/actuator/health/readiness
curl http://localhost:8181/actuator/health/liveness
curl http://localhost:8181/actuator/prometheus | head -n 20

# customer-service
curl http://localhost:8184/actuator/health

# payment-service (only works AFTER adding spring-boot-starter-web)
curl http://localhost:8182/actuator/prometheus | head -n 20

# restaurant-service
curl http://localhost:8183/actuator/prometheus | head -n 20
```

**Pass criteria:**
- `/actuator/health` returns `{"status":"UP", ...}` with component details.
- `/actuator/health/readiness` and `/actuator/health/liveness` both return `UP`.
- `/actuator/prometheus` returns plaintext metrics including `jvm_memory_used_bytes`, `http_server_requests_seconds_*` (order/customer), and Kafka client metrics on all four.
- Each `/actuator/prometheus` output carries your `application="..."` tag.

## Checklist

- [ ] Actuator + micrometer-prometheus added to all 4 container poms (no versions)
- [ ] `spring-boot-starter-web` added to **payment** and **restaurant** containers
- [ ] `management:` block added to all 4 `application.yml`
- [ ] `spring.application.name` set distinctly per service
- [ ] `mvn clean install` is green
- [ ] All 4 services expose `/actuator/health` returning `UP`
- [ ] Readiness + liveness probes return `UP` on all 4
- [ ] All 4 expose `/actuator/prometheus` with the `application` tag (including payment + restaurant)
- [ ] Committed on a feature branch (this is part of the Phase 1 PR)

## Notes & gotchas

- **Don't expose everything.** `include: "*"` would publish sensitive endpoints (env, heapdump, etc.). Keep the explicit allow-list above. Real security comes in Phase 2 — until then, these ports stay local only.
- **`show-details: always` is fine locally** but you'll tighten it to `when-authorized` in Phase 2. Leave a `// TODO Phase 2` note so you remember.
- **Kafka-only services don't have `http_server_requests` metrics** — that's expected; they'll still emit JVM + Kafka consumer metrics, which is what you actually want from them.
- **Management port:** by default Actuator shares the app's `server.port`. That's fine for now. If you later want metrics on a separate port, set `management.server.port` — but don't bother this week.
- **Adding `spring-boot-starter-web` to payment/restaurant starts an embedded Tomcat** on their `server.port` (8182/8183). Confirm nothing else expects those ports to be closed.

## Learning resources (just-in-time)

- Spring Boot reference → **Actuator** chapter (endpoints, exposure, health groups/probes)
- Micrometer docs → **Prometheus registry** + **common tags**
- Spring Boot → **Kubernetes probes** (`management.endpoint.health.probes`) — you'll reuse this in Phase 3
- Skim only what you need to complete the steps; this is a ~⅓-of-time reading task.

## Definition of done

All four services expose health (with readiness/liveness) and a Prometheus-format metrics endpoint carrying a per-service `application` tag, the full build is green, and it's committed on the Phase 1 branch. When this is true, **Task 2 (Prometheus + Grafana in `infrastructure/docker-compose/`) has real targets to scrape.**

---

*Next up (not yet written — review this format first): **Phase 1 · Task 2 — Prometheus + Grafana in docker-compose**.*
