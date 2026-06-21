# Phase 1 · Task 3 — Base Grafana dashboards (JVM / HTTP / Kafka lag)

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone A (Observability), Week 3
> **Estimated effort:** ~5 hrs
> **Depends on:** [Task 2](task-02-prometheus-grafana-compose.md)
> **Outcome:** Three reusable dashboards — JVM health, HTTP server, and **Kafka consumer lag** — committed as JSON so they survive container restarts and live in version control.

---

## Overview

You have metrics flowing into Prometheus; now make them legible. This task builds the three dashboards you'll stare at for the rest of the year, then **exports them as JSON** and commits them (dashboards-as-code) so they're reproducible and reviewable in a PR.

The **Kafka consumer-lag** panel is the most important one here — your system is event-driven, so consumer lag is the single best early-warning signal that something is backing up.

## Why this matters

- **A dashboard you didn't build, you don't trust.** Importing a random community dashboard teaches nothing; building these three teaches you PromQL and what "healthy" looks like for *your* app.
- **Lag is your canary.** In a Saga/Outbox + Kafka system, rising consumer lag is how you'll spot a stuck step before customers do.
- **Dashboards-as-code** is a senior habit — reproducible, diffable, peer-reviewable.

## Files you'll touch / create

| File | Change |
|---|---|
| `infrastructure/docker-compose/grafana/provisioning/dashboards/dashboards.yml` | **new** — provider that loads dashboard JSON |
| `infrastructure/docker-compose/grafana/dashboards/jvm.json` | **new** (exported) |
| `infrastructure/docker-compose/grafana/dashboards/http.json` | **new** (exported) |
| `infrastructure/docker-compose/grafana/dashboards/kafka-lag.json` | **new** (exported) |

## Step-by-step

### Step 1 — Enable file-based dashboard provisioning

`grafana/provisioning/dashboards/dashboards.yml`:

```yaml
apiVersion: 1
providers:
  - name: 'food-ordering'
    type: file
    options:
      path: /etc/grafana/dashboards
```

Mount it in `monitoring.yml` (add to the Grafana `volumes`):

```yaml
      - ./grafana/dashboards:/etc/grafana/dashboards
```

### Step 2 — Build the JVM dashboard (panels + PromQL)

Create a new dashboard in the Grafana UI with a `$application` template variable (`label_values(jvm_memory_used_bytes, application)`), then add panels:

- **Heap used vs max:** `sum(jvm_memory_used_bytes{application="$application", area="heap"})` and `...max...`
- **GC pause time:** `rate(jvm_gc_pause_seconds_sum{application="$application"}[1m])`
- **Live threads:** `jvm_threads_live_threads{application="$application"}`
- **CPU:** `system_cpu_usage` and `process_cpu_usage`

### Step 3 — Build the HTTP dashboard (order + customer)

Uses `http_server_requests_seconds_*` (present only on the web-enabled services):

- **Request rate:** `sum by (uri) (rate(http_server_requests_seconds_count{application="$application"}[1m]))`
- **p95 latency:** `histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{application="$application"}[5m])))`
- **Error rate:** rate of `outcome="SERVER_ERROR"` over total.

### Step 4 — Build the Kafka consumer-lag dashboard

Spring Kafka/Micrometer exposes consumer metrics (`kafka_consumer_fetch_manager_records_lag` and friends):

- **Records lag (max) per consumer group:** `max by (client_id) (kafka_consumer_fetch_manager_records_lag_max)`
- **Records consumed rate:** `rate(kafka_consumer_fetch_manager_records_consumed_total[1m])`
- Annotate the four consumer groups from your config: `payment-topic-consumer`, `restaurant-approval-topic-consumer`, `customer-topic-consumer`.

### Step 5 — Export and commit

For each dashboard: **Share → Export → Save to file** (toggle "Export for sharing externally" off so the datasource binds via provisioning). Drop the JSON into `grafana/dashboards/`. Restart Grafana → confirm they auto-load.

## Verification

- All three dashboards appear in Grafana **after a `docker compose restart grafana`** (proves they load from JSON, not just live edits).
- Generate traffic: place a few orders via the `order-service` REST API → HTTP panels move, JVM panels populate, and the Kafka-lag panel shows the consumer groups (lag should hover near 0 when healthy).
- The `$application` variable switches every panel between the four services.

## Checklist

- [ ] Dashboard provider config + volume mount added
- [ ] JVM dashboard (heap, GC, threads, CPU) with `$application` variable
- [ ] HTTP dashboard (rate, p95, errors) for order + customer
- [ ] Kafka consumer-lag dashboard covering all 3 consumer groups
- [ ] All three exported as JSON and committed
- [ ] Dashboards survive a Grafana restart
- [ ] Committed on the Phase 1 branch

## Notes & gotchas

- **Metric name drift:** exact Kafka metric names depend on the Micrometer/Spring Kafka version. If a query is empty, browse Prometheus' metric explorer for the real name (`kafka_consumer_*`) and adjust.
- **Histogram buckets:** `histogram_quantile` needs the `_bucket` series. Spring Boot publishes HTTP histograms by default; if percentiles look wrong, ensure `management.metrics.distribution.percentiles-histogram.http.server.requests=true`.
- **Don't over-build.** Three solid dashboards beat ten noisy ones. You'll add a Saga/Outbox dashboard in Task 4.

## Learning resources (just-in-time)

- Grafana docs → *Templating* (variables) and *Provision dashboards*
- Prometheus docs → *Querying* (rate, histogram_quantile, aggregation)
- Micrometer → *JVM and system metrics* reference

## Definition of done

Three committed, restart-surviving dashboards (JVM, HTTP, Kafka lag) that you built yourself and can read. **Task 4 (Saga/Outbox throughput dashboard with custom metrics) builds on this.**

---

*Next: **[Task 4 — Saga/Outbox throughput dashboard (custom metrics)](task-04-saga-outbox-metrics.md)**.*
