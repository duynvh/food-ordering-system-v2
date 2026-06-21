# Phase 1 · Task 5 — Distributed tracing across Kafka (OTel + Jaeger)

> **Phase:** [Phase 1 — Observable & Fast](../../phase-1-observable-and-fast.md) → Sub-milestone A (Observability), Week 5
> **Estimated effort:** ~6 hrs (budget extra — this is the hard one)
> **Depends on:** [Task 1](task-01-actuator-micrometer.md) (Micrometer present)
> **Outcome:** One order produces a **single connected trace** spanning `order → payment → restaurant → order`, with spans correctly propagated **across Kafka** message boundaries, viewable in Jaeger.

---

## Overview

Metrics tell you *that* something is slow; **traces** tell you *where*. This task adds distributed tracing via **Micrometer Tracing + OpenTelemetry**, exports spans to **Jaeger**, and — the genuinely hard part — **propagates trace context across Kafka**, so an order's journey through four services and multiple async hops shows up as one trace instead of four disconnected ones.

This is the most senior-defining task in Phase 1. Async context propagation is where most teams give up; doing it well is a standout signal.

## Why this matters

- **Async breaks naive tracing.** HTTP tracing is automatic; Kafka is not — context must be manually carried on message headers across the producer/consumer gap.
- **It's the Phase 1 depth marker's centerpiece** (the end-to-end trace).
- **It pays compounding dividends** — every future debugging session in this system gets dramatically easier.

## ⚠️ The core challenge

A trace is a tree of spans linked by a shared trace-id + parent span-id. Within one JVM, context rides a thread-local. The moment you publish to Kafka and a *different* service consumes on a *different* thread, that thread-local is empty — so you must **inject** the context into Kafka headers on send and **extract** it on receive. Spring Kafka + Micrometer Tracing can do most of this via observation instrumentation, but you must turn it on and verify it actually links.

## Files you'll touch

| File | Change |
|---|---|
| all 4 `*-container/pom.xml` | + `micrometer-tracing-bridge-otel`, + `opentelemetry-exporter-otlp` |
| all 4 `application.yml` | tracing sampling + OTLP endpoint config |
| `infrastructure/docker-compose/monitoring.yml` | + Jaeger service |
| `infrastructure/kafka/kafka-producer` → `KafkaProducerImpl.java` | enable observation / inject context |
| `infrastructure/kafka/kafka-consumer` → `KafkaConsumerConfig.java` | enable observation on listener containers |

## Step-by-step

### Step 1 — Dependencies (all 4 containers)

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### Step 2 — Jaeger in compose

Add to `monitoring.yml`:

```yaml
  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: jaeger
    ports:
      - '16686:16686'    # UI
      - '4317:4317'      # OTLP gRPC
    environment:
      - COLLECTOR_OTLP_ENABLED=true
```

### Step 3 — Tracing config (all 4 application.yml)

```yaml
management:
  tracing:
    sampling:
      probability: 1.0          # sample everything locally; lower in prod
  otlp:
    tracing:
      endpoint: http://localhost:4317
```

### Step 4 — Turn on Kafka observation (the crux)

- **Producer:** enable observation on the `KafkaTemplate` used by `KafkaProducerImpl` (`kafkaTemplate.setObservationEnabled(true)`).
- **Consumer:** enable observation on the listener container factory in `KafkaConsumerConfig` (`factory.getContainerProperties().setObservationEnabled(true)`).

With both enabled, Micrometer injects/extracts the W3C `traceparent` on Kafka headers automatically. If you need manual control, use OpenTelemetry's `TextMapPropagator` to inject into `ProducerRecord.headers()` and extract in the listener.

### Step 5 — Verify linkage, not just presence

Place one order and confirm in Jaeger that the spans across services share **one trace id** and form a parent/child tree — not four separate traces.

## Verification

- Open Jaeger UI `http://localhost:16686`, select service `order-service`, find the latest trace.
- **Pass:** a single trace contains spans from order → (Kafka) → payment → (Kafka) → restaurant → (Kafka) → order, visibly nested. The Kafka hops appear as producer/consumer spans linking the services.
- **Fail signature:** multiple separate traces, or consumer spans with a fresh trace id → context isn't propagating; revisit Step 4.

## Checklist

- [ ] Tracing deps added to all 4 containers
- [ ] Jaeger running in compose, OTLP enabled
- [ ] Sampling + OTLP endpoint configured in all 4 services
- [ ] Producer observation enabled (`KafkaProducerImpl`/`KafkaTemplate`)
- [ ] Consumer observation enabled (`KafkaConsumerConfig` factory)
- [ ] One order = one connected trace across all services in Jaeger
- [ ] Build green; committed on the Phase 1 branch

## Notes & gotchas

- **`@Async`/manual executors lose context** unless wrapped — if any custom executor sits between produce/consume, instrument it (`ContextSnapshot`/`ContextPropagators`).
- **Batch listeners:** your consumers use `batch-listener: true`. Tracing a batch links the batch span; per-record correlation may need extracting context per record. Note this tradeoff; full per-record tracing is optional stretch.
- **Sampling 1.0 is local-only.** In prod you'd sample ~1–10%. Leave a `# TODO: lower in cloud` comment for Phase 3.
- **Overhead is real but small** — note it; you'll re-measure in the perf sub-milestone.
- **Verify linkage, never just "spans exist."** Disconnected spans are the classic false-success here.

## Learning resources (just-in-time)

- Spring Boot reference → *Observability / Tracing*
- Micrometer Tracing docs → *OpenTelemetry bridge* + *Kafka instrumentation*
- Jaeger docs → *Getting started (all-in-one)*
- W3C Trace Context primer (`traceparent` header) — skim

## Definition of done

A single order yields one end-to-end trace across all services through Kafka, visible in Jaeger. This is the trace half of the Phase 1 depth marker. **Task 6 (structured logging with trace IDs) ties logs to these traces and closes out Observability.**

---

*Next: **[Task 6 — Structured JSON logging + trace correlation](task-06-structured-logging.md)**.*
