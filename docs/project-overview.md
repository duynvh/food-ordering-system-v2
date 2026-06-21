# Food Ordering System — Project Overview

A microservices-based food ordering platform built with **Spring Boot 3.5.8** and **Java 25**,
following **Domain-Driven Design (DDD)**, **Clean/Hexagonal Architecture**, and event-driven
patterns. Services communicate asynchronously over **Apache Kafka** (Avro-serialized messages),
coordinated by the **Saga** pattern and made reliable with the **Outbox** pattern.

- **Group:** `com.food.ordering.system`
- **Version:** `1.0-SNAPSHOT`
- **Build:** Maven multi-module (parent `pom.xml`, packaging `pom`)

> **Related docs:** [`data-flow.md`](./data-flow.md) — topic-level & outbox data flow ·
> [`sequence-diagrams.md`](./sequence-diagrams.md) — per-use-case interaction sequences.

## Architecture at a Glance

The system is split into four business microservices plus shared and infrastructure modules.
Each business service follows the same hexagonal layering:

| Layer / Module | Responsibility |
| --- | --- |
| `*-domain-core` | Pure domain model: entities, value objects, domain events, business rules |
| `*-application-service` | Use-case orchestration, input/output ports, saga & listener logic |
| `*-application` | Inbound adapters (REST controllers) |
| `*-dataaccess` | Outbound adapters: JPA entities, repositories (PostgreSQL) |
| `*-messaging` | Kafka publishers and listeners (Avro) |
| `*-container` | Spring Boot application entry point + configuration (`application.yml`) |

Dependencies point **inward**: adapters depend on the application/domain layers through ports
(interfaces), never the reverse.

## Microservices

| Service | HTTP Port | DB Schema | Role |
| --- | --- | --- | --- |
| **order-service** | 8181 | `order` (pg:5434) | Receives orders, drives the saga, tracks order status |
| **payment-service** | 8182 | `payment` (pg:5434) | Processes payments (debit/credit), maintains credit balances |
| **restaurant-service** | 8183 | `restaurant` (pg:5432) | Approves/rejects orders based on restaurant availability |
| **customer-service** | 8184 | `customer` (pg:5434) | Manages customers; publishes customer events to order-service |

### order-service
The orchestrator. Exposes a REST API and coordinates the distributed transaction.

- **`OrderController`** (`/orders`) — `POST` to create an order, `GET /{trackingId}` to track it.
- **Domain entities:** `Order`, `OrderItem`, `Product`, `Restaurant`, `Customer`.
- **Sagas:** `OrderPaymentSaga`, `OrderApprovalSaga` — process/compensate steps across services.
- **Inbound listeners:** `PaymentResponseMessageListener`,
  `RestaurantApprovalResponseMessageListener`, `CustomerMessageListener`.
- **order-ai:** Spring AI integration (OpenAI `gpt-4.1-mini`) that interprets free-text order
  notes into structured `OrderPreferences` (spice level, ingredient add/remove, special
  instructions). Hardened with a strict system prompt against prompt injection, with bounded
  retry attempts (`order.ai.interpreter.max-attempts`).

## Distributed Transaction Flow (Saga)

The order lifecycle is a choreographed/orchestrated saga over Kafka topics:

```
Customer → POST /orders → order-service (Order created, PENDING)
        → payment-request topic        → payment-service (charges customer)
        ← payment-response topic        ← (PAID / FAILED)
        → restaurant-approval-request   → restaurant-service (approve/reject)
        ← restaurant-approval-response  ← (APPROVED / REJECTED)
        → Order APPROVED  (or compensating actions on failure → CANCELLED)
```

Failures trigger **compensating transactions** (e.g. payment is rolled back if the restaurant
rejects the order).

### Kafka Topics
- `payment-request` / `payment-response`
- `restaurant-approval-request` / `restaurant-approval-response`
- `customer`

## Reliability Patterns

- **Outbox pattern** (`infrastructure/outbox`) — domain changes and the events to publish are
  written in the same DB transaction; a scheduler (`OutboxScheduler`, fixed-rate polling)
  publishes pending outbox rows to Kafka, guaranteeing at-least-once delivery and avoiding
  dual-write inconsistency. Implemented in order, payment, and restaurant services.
- **Saga pattern** (`infrastructure/saga`) — `SagaStep` / `SagaStatus` abstractions drive the
  process and rollback steps for the order workflow.

## Shared & Infrastructure Modules

| Module | Contents |
| --- | --- |
| `common/common-domain` | Base domain primitives, value objects, domain exceptions |
| `common/common-application` | Shared application-layer helpers (e.g. global error handling) |
| `common/common-dataaccess` | Shared persistence helpers |
| `infrastructure/kafka` | `kafka-config-data`, `kafka-model` (Avro), `kafka-producer`, `kafka-consumer` |
| `infrastructure/saga` | Saga step/status abstractions |
| `infrastructure/outbox` | Outbox status, scheduler, config |
| `infrastructure/docker-compose` | Local stack: Kafka cluster, Zookeeper, Schema Registry, PostgreSQL + Debezium |

## Technology Stack

- **Java 25**, **Spring Boot 3.5.8**, **Spring Kafka 3.2.2**, **Spring AI 1.1.7**
- **Apache Kafka** with **Confluent Schema Registry** + **Avro 1.12.0** serialization
- **PostgreSQL** (with Debezium for CDC) via Spring Data JPA / Hibernate
- **Lombok** for boilerplate reduction
- **JUnit 5** + **Mockito 5.12** for testing

## Local Development

Infrastructure is provisioned through Docker Compose under `infrastructure/docker-compose`:

```bash
cd infrastructure/docker-compose
./startup.sh        # brings up Zookeeper, Kafka cluster, Schema Registry, Postgres, topics
./shutdown.sh       # tears the stack down
```

Build the whole project from the repository root:

```bash
mvn clean install
```

Each service is then runnable as a standalone Spring Boot app from its `*-container` module.
Set `OPEN_API_KEY` in the environment for the order-service AI note interpreter.

## REST API Summary

| Service | Method & Path | Description |
| --- | --- | --- |
| order-service | `POST /orders` | Create a new order |
| order-service | `GET /orders/{trackingId}` | Track an order by tracking id |
| customer-service | `POST /customers` | Create a customer |

> Responses use the media type `application/vnd.api.v1+json`.
