# Phase 1 · Task 2 — Prometheus + Grafana in docker-compose

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone A (Observability), Week 2
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 1](task-01-actuator-micrometer.md) (services must expose `/actuator/prometheus`)
> **Outcome:** A one-command monitoring stack — Prometheus scraping all four services, Grafana wired to it — running alongside your existing Kafka/Postgres infra.

---

## Overview

Task 1 made each service publish metrics. This task stands up the things that **collect and display** them: **Prometheus** (scrapes and stores the metrics) and **Grafana** (queries Prometheus and draws dashboards). You'll add them as a new compose file in `infrastructure/docker-compose/`, reusing the pattern already established by `kafka_cluster.yml`, and hook them into `startup.sh`/`shutdown.sh` so the whole observability stack comes up with one command.

## Why this matters

- **Turns raw endpoints into a system.** A `/actuator/prometheus` page nobody scrapes is useless; Prometheus + Grafana is the difference between "metrics exist" and "I can see my system."
- **Reused all year.** Every later task (perf before/after, cache hit ratio, circuit-breaker demo in Phase 2, cloud dashboards in Phase 3) renders on this stack.
- **No new infra tax.** You already run docker-compose for Kafka/Postgres — this is the same muscle.

## ⚠️ Networking gotcha (the thing that wastes an afternoon)

Prometheus runs **inside a docker container**, but your services run **on the host** (from your IDE / `java -jar`). So `localhost` inside the Prometheus container is *not* your host. On Docker Desktop (Windows/Mac) scrape targets must use **`host.docker.internal:<port>`**, not `localhost:<port>`. (On Linux, add `extra_hosts: ["host.docker.internal:host-gateway"]`.)

## Files you'll touch / create

| File | Change |
|---|---|
| `infrastructure/docker-compose/monitoring.yml` | **new** — Prometheus + Grafana services |
| `infrastructure/docker-compose/prometheus/prometheus.yml` | **new** — scrape config |
| `infrastructure/docker-compose/grafana/provisioning/datasources/prometheus.yml` | **new** — auto-add Prometheus datasource |
| `infrastructure/docker-compose/startup.sh` / `shutdown.sh` | add `monitoring.yml` to the up/down commands |

## Step-by-step

### Step 1 — Prometheus scrape config

`infrastructure/docker-compose/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 10s
scrape_configs:
  - job_name: 'food-ordering-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'host.docker.internal:8181'   # order
          - 'host.docker.internal:8182'   # payment
          - 'host.docker.internal:8183'   # restaurant
          - 'host.docker.internal:8184'   # customer
```

### Step 2 — The compose file

`infrastructure/docker-compose/monitoring.yml`:

```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports: ['9090:9090']
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    extra_hosts: ['host.docker.internal:host-gateway']   # needed on Linux; harmless elsewhere
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports: ['3000:3000']
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
    depends_on: [prometheus]
```

### Step 3 — Auto-provision the Grafana datasource

`infrastructure/docker-compose/grafana/provisioning/datasources/prometheus.yml`:

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090     # container-to-container: use the service name
    isDefault: true
```

### Step 4 — Wire into startup/shutdown

Add `-f monitoring.yml` to the existing `docker-compose ... up`/`down` invocations in `startup.sh`/`shutdown.sh` (match how `kafka_cluster.yml` is referenced).

### Step 5 — Bring it up

Run the services (Task 1), then `startup.sh`.

## Verification

- **Prometheus targets:** open `http://localhost:9090/targets` → all 4 services show **State = UP**. (If any is DOWN, it's almost always the `host.docker.internal` vs `localhost` issue, or you forgot the web starter on payment/restaurant in Task 1.)
- **A query works:** in Prometheus, run `jvm_memory_used_bytes` → returns series tagged with your `application` label.
- **Grafana up:** `http://localhost:3000` (admin/admin), Prometheus datasource present and "Test" passes.

## Checklist

- [ ] `prometheus.yml` scrape config with all 4 targets via `host.docker.internal`
- [ ] `monitoring.yml` with Prometheus + Grafana
- [ ] Grafana datasource auto-provisioned
- [ ] `startup.sh`/`shutdown.sh` updated
- [ ] All 4 targets UP in Prometheus
- [ ] Grafana reaches Prometheus (Test OK)
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Scrape interval 10s** is fine for local; don't go below 5s or you add noise/load.
- **No persistence yet** — restarting wipes Grafana/Prometheus state. That's fine now; dashboards become code in Task 3 (exported JSON), so you don't rely on the container's disk.
- **Keep credentials trivial (admin/admin) locally.** This stack is not exposed to the internet; real auth is a Phase 2/Phase 3 concern.
- **Port 3000/9090 conflicts?** Adjust the host-side port mapping if something already uses them.

## Learning resources (just-in-time)

- Prometheus docs → *Configuration* (scrape_configs) and *Getting started*
- Grafana docs → *Provisioning* (datasources) and *Add a Prometheus data source*

## Definition of done

`startup.sh` brings up Prometheus + Grafana with your existing infra; all four services are scraped (targets UP); Grafana can query Prometheus. **Task 3 (build the base dashboards) can now begin.**

---

*Next: **[Task 3 — Base Grafana dashboards (JVM / HTTP / Kafka lag)](task-03-base-grafana-dashboards.md)**.*
