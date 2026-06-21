# Phase 1 · Task 6 — Structured JSON logging + trace correlation

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone A (Observability), Week 6
> **Estimated effort:** ~4 hrs (+ optional Loki stretch)
> **Depends on:** [Task 5](task-05-distributed-tracing-kafka.md) (so logs can carry the trace id)
> **Outcome:** Every log line is structured JSON carrying `traceId`/`spanId`, so you can jump from a Jaeger trace to the exact logs for that request. Closes out the Observability sub-milestone.

---

## Overview

Today the codebase logs with `@Slf4j` in plaintext at `DEBUG` for `com.food.ordering.system`. Plaintext logs don't correlate to traces and aren't machine-queryable. This task switches logging to **structured JSON** and injects the **trace/span IDs** (from Task 5's MDC) into every line, so logs and traces are joined by a shared id. Optionally, ship logs to **Loki** so you can query them in Grafana next to your metrics and traces (the "three pillars" in one pane).

## Why this matters

- **Correlation is the payoff.** "Here's a slow trace" → one click → "here are that request's logs" is the senior debugging loop. Without the trace id in logs, you're grepping by timestamp.
- **Structured = queryable.** JSON logs can be filtered by field (service, level, traceId) instead of regex archaeology.
- **Completes the three pillars** (metrics + traces + logs), which is exactly the Phase 1 promise.

## Files you'll touch / create

| File | Change |
|---|---|
| root `pom.xml` (or per-container) | + `logstash-logback-encoder` |
| each `*-container/src/main/resources/logback-spring.xml` | **new** — JSON encoder + MDC fields |
| each `application.yml` | optional: keep `logging.level` overrides |
| `infrastructure/docker-compose/monitoring.yml` | optional: + Loki + Promtail |

## Step-by-step

### Step 1 — Add the JSON encoder

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.0</version>   <!-- not in the Spring BOM; pin a version -->
</dependency>
```

### Step 2 — `logback-spring.xml` per container

```xml
<configuration>
  <springProperty name="appName" source="spring.application.name"/>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>traceId</includeMdcKeyName>
      <includeMdcKeyName>spanId</includeMdcKeyName>
      <customFields>{"application":"${appName}"}</customFields>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
  <logger name="com.food.ordering.system" level="DEBUG"/>
</configuration>
```

Micrometer Tracing populates the MDC with `traceId`/`spanId` automatically once Task 5 is in place — the encoder just needs to include them.

### Step 3 — Confirm MDC propagation

If `traceId` is missing from logs, set `management.tracing.baggage.correlation.enabled=true` and ensure the Micrometer Tracing bridge is on the classpath (it is, from Task 5).

### Step 4 — (Optional stretch) Loki + Promtail

Add Loki + Promtail to `monitoring.yml`, point Promtail at the container logs, add Loki as a Grafana datasource. Then a Grafana "Explore" view lets you pivot trace → logs by `traceId`.

## Verification

- Start a service → console output is **one JSON object per line** with `@timestamp`, `level`, `logger_name`, `message`, `application`, and (under load) `traceId`/`spanId`.
- Place an order, copy its `traceId` from a log line, paste into Jaeger → it resolves to the matching trace. (And vice-versa.)
- (If Loki) In Grafana Explore, filter `{application="order-service"}` and find the same line.

## Checklist

- [ ] `logstash-logback-encoder` added (versioned)
- [ ] `logback-spring.xml` per container emitting JSON
- [ ] `traceId`/`spanId` present in log lines under request load
- [ ] `application` field set per service
- [ ] trace id in a log line resolves in Jaeger
- [ ] (optional) Loki + Promtail wired into Grafana
- [ ] Build green; committed on the Phase 1 branch

## Notes & gotchas

- **`logback-spring.xml`, not `logback.xml`** — the `-spring` variant lets you read `spring.application.name` via `<springProperty>`.
- **Drop DEBUG noise for JSON.** `com.food.ordering.system` at DEBUG plus `show-sql: true` is loud; keep DEBUG for your packages but consider INFO for the root so JSON output stays readable. You'll want SQL logging in Task 9 (DB tuning) — toggle it then.
- **JSON in the console is ugly to read by eye.** That's expected; use Grafana/Jaeger to read. For pure local dev you can keep a plaintext profile (`logback-spring.xml` with `<springProfile>`).
- **Encoder version isn't BOM-managed** — pin it explicitly (only third-party dep so far that needs a version).

## Learning resources (just-in-time)

- `logstash-logback-encoder` README (fields, MDC inclusion, custom fields)
- Spring Boot reference → *Logging* (logback-spring, springProperty/springProfile)
- (Optional) Grafana Loki docs → *Get started* + *Promtail*

## Definition of done

All services emit structured JSON logs correlated to traces by `traceId`, and you can pivot between a trace and its logs. **Observability (Sub-milestone A) is complete** — the metrics/traces/logs foundation is in place. Next sub-milestone: Performance, starting with a load-test baseline.

---

*Next: **[Task 7 — k6 load test + baseline](task-07-k6-load-baseline.md)** (start of Performance).*
