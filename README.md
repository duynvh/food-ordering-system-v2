# Food Ordering System

A microservices-based food-ordering platform built with **Java 25** and **Spring Boot 3.5.8**, demonstrating Domain-Driven Design (DDD), Clean/Hexagonal Architecture, event-driven communication over **Apache Kafka**, and reliable distributed transactions using the **Saga** and **Transactional Outbox** patterns (with **Debezium** Change Data Capture).

It also includes an AI-powered order-note interpreter built on **Spring AI** that turns free-text customer notes (e.g. *"extra spicy, no onions, ring the bell"*) into structured order preferences.

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [Key Patterns](#key-patterns)
- [AI Order-Note Interpreter](#ai-order-note-interpreter)
- [Module Layout](#module-layout)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [REST API](#rest-api)
- [Configuration](#configuration)

---

## Architecture

The system is composed of four independent microservices that never call each other synchronously. Instead, they collaborate asynchronously through Kafka topics. The **order-service** acts as the Saga orchestrator, coordinating payment and restaurant approval to complete (or compensate) an order.

```
                 ┌──────────────┐
   HTTP          │   Customer   │   customer events
 ────────────►   │   Service    │ ───────────────────┐
                 │   :8182      │                    │
                 └──────────────┘                    ▼
                                              ┌──────────────┐
   HTTP                                       │              │
 ────────────────────────────────────────►   │    Order     │
   POST /orders                               │   Service    │  (Saga orchestrator)
   GET  /orders/{trackingId}                  │    :8181     │
                                              └──────┬───────┘
                                                     │
                       payment-request / response    │   restaurant-approval-request / response
                  ┌──────────────────────────────────┼───────────────────────────────────┐
                  ▼                                   │                                   ▼
          ┌──────────────┐                            │                          ┌──────────────┐
          │   Payment    │                            │                          │  Restaurant  │
          │   Service    │                            │                          │   Service    │
          │    :8183     │                            │                          │    :8184     │
          └──────────────┘                            │                          └──────────────┘
                                                       │
              All inter-service messaging flows through a 3-broker Kafka cluster
                  (Avro + Schema Registry) with Debezium CDC on outbox tables
```

Each service follows the same internal **Hexagonal / Clean Architecture** layering, so the domain logic stays free of framework and infrastructure concerns:

| Layer | Module suffix | Responsibility |
|-------|---------------|----------------|
| Domain core | `*-domain-core` | Entities, value objects, domain events, pure business rules |
| Application service | `*-application-service` | Use cases, command handlers, saga steps, input/output ports |
| Data access | `*-dataaccess` | JPA entities, repositories (output port adapters) |
| Messaging | `*-messaging` | Kafka publishers/listeners (output/input port adapters) |
| REST | `*-application` | REST controllers (input adapters) |
| Container | `*-container` | Spring Boot entry point, wiring, configuration, SQL/init |

---

## Services

| Service | Port | Schema | Responsibility |
|---------|------|--------|----------------|
| **order-service** | `8181` | `order` | Accepts and tracks orders; orchestrates the Saga across payment and restaurant approval |
| **customer-service** | `8182` | `customer` | Manages customers; publishes customer events consumed by order-service |
| **payment-service** | `8183` | `payment` | Processes payments and credit, responds with payment status |
| **restaurant-service** | `8184` | `restaurant` | Approves/rejects orders against restaurant availability |

---

## Key Patterns

### Saga (Orchestration)
The order-service drives an orchestrated saga to keep the order, payment, and restaurant-approval state consistent across services:

1. **Order created** → order persisted as `PENDING`, payment requested.
2. **Payment completed** → restaurant approval requested.
3. **Restaurant approved** → order moves to `APPROVED`.
4. **Failure at any step** → compensating actions roll the order back (`CANCELLING` → `CANCELLED`), e.g. a payment is reversed if the restaurant rejects the order.

### Transactional Outbox + Debezium CDC
To publish events reliably (no lost or double-published messages), each service writes domain events to an **outbox table** in the same local transaction as its business data. Events are then propagated to Kafka via two complementary mechanisms:

- **Scheduler-based pollers** (`OutboxScheduler`) that read pending outbox rows and publish them.
- **Debezium connectors** that tail the Postgres write-ahead log on the outbox tables (`payment_outbox`, `restaurant_approval_outbox`, `order_outbox`) and stream changes to `debezium.*` topics.

Outbox rows are tracked with a saga/outbox status and cleaned up by dedicated cleaner schedulers once completed.

---

## AI Order-Note Interpreter

The `order-ai` module (Spring AI + OpenAI) interprets free-text `orderNotes` on an order into a structured `OrderPreferences` object:

- **Spice level** — `NONE`, `MILD`, `MEDIUM`, `HOT`, `EXTRA_HOT`, `UNKNOWN`
- **Ingredients to add / remove**
- **Special instructions** for the kitchen or delivery (e.g. *"leave at the door"*, *"apartment 5B"*)

It is hardened against prompt-injection: the system prompt strictly scopes the model to order interpretation, treats notes as untrusted input, and ignores any embedded instructions. Invalid model output is retried up to a configurable number of attempts (`order.ai.interpreter.max-attempts`, default `3`). Interpreted preferences are stored on the order as a JSONB column.

> Requires an OpenAI API key exported as `OPEN_API_KEY`. Default model: `gpt-4.1-mini`.

---

## Module Layout

```
food-ordering-system/
├── common/                  # Shared domain, application, and data-access building blocks
│   ├── common-domain/        #   AggregateRoot, BaseEntity, value objects (Money, OrderId, ...)
│   ├── common-application/   #   Global exception handling, error DTOs
│   └── common-dataaccess/    #   Shared JPA entities (restaurant)
│
├── infrastructure/
│   ├── kafka/               # Kafka config, Avro model, producer & consumer adapters
│   ├── saga/                # Saga step abstraction & status
│   ├── outbox/              # Outbox scheduler, status, scheduling config
│   └── docker-compose/      # Kafka cluster, Zookeeper, Postgres, Debezium, startup scripts
│
├── order-service/          # Saga orchestrator (+ order-ai Spring AI module)
├── customer-service/
├── payment-service/
└── restaurant-service/
```

---

## Tech Stack

- **Language / Runtime:** Java 25
- **Framework:** Spring Boot 3.5.8 (Web, Data JPA, Kafka), Spring AI 1.1.7
- **Build:** Maven (multi-module reactor)
- **Messaging:** Apache Kafka 7.0.1 (Confluent), Avro + Schema Registry, 3-broker cluster
- **CDC:** Debezium 2.2 (Postgres connector)
- **Database:** PostgreSQL (schema-per-service)
- **Libraries:** Lombok, Mockito
- **Orchestration:** Docker Compose

---

## Prerequisites

- JDK 25
- Maven 3.9+
- Docker & Docker Compose
- (Optional) `kcat`, `ncat`, `curl` — used by the infrastructure health-check scripts
- An OpenAI API key (for the AI order-note interpreter)

---

## Getting Started

### 1. Start the infrastructure

The Docker Compose stack brings up Zookeeper, a 3-broker Kafka cluster, Schema Registry, Postgres, and Debezium, then registers the outbox CDC connectors.

```bash
cd infrastructure/docker-compose
./startup.sh
```

To shut everything down (and optionally remove volumes):

```bash
./shutdown.sh
./remove_volume.sh   # wipes persisted data
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Provide the OpenAI key (for order-service)

```bash
export OPEN_API_KEY=sk-...
```

### 4. Run the services

Each service is a standalone Spring Boot application launched from its `*-container` module. For example:

```bash
mvn spring-boot:run -pl order-service/order-container
mvn spring-boot:run -pl customer-service/customer-container
mvn spring-boot:run -pl payment-service/payment-container
mvn spring-boot:run -pl restaurant-service/restaurant-container
```

Schemas and seed data (`init-schema.sql`, `init-data.sql`) are applied automatically on startup.

---

## REST API

All endpoints produce the media type `application/vnd.api.v1+json`.

### Create a customer — `POST /customers` (`:8182`)

```json
{
  "customerId": "d215b5f8-0249-4dc5-89a3-51fd148cfb45",
  "username": "user_1",
  "firstName": "First",
  "lastName": "User"
}
```

### Create an order — `POST /orders` (`:8181`)

```json
{
  "customerId": "d215b5f8-0249-4dc5-89a3-51fd148cfb45",
  "restaurantId": "d215b5f8-0249-4dc5-89a3-51fd148cfb41",
  "price": 200.00,
  "items": [
    {
      "productId": "d215b5f8-0249-4dc5-89a3-51fd148cfb48",
      "quantity": 1,
      "price": 50.00,
      "subTotal": 50.00
    }
  ],
  "address": {
    "street": "street_1",
    "postalCode": "1000AB",
    "city": "Amsterdam"
  },
  "orderNotes": "extra spicy, no onions, please ring the bell"
}
```

The response includes an `orderTrackingId`.

### Track an order — `GET /orders/{trackingId}` (`:8181`)

Returns the current `orderStatus` (`PENDING`, `PAID`, `APPROVED`, `CANCELLING`, `CANCELLED`) and any failure messages.

---

## Configuration

Each service's `application.yml` lives in its `*-container/src/main/resources` directory. Notable settings:

- **Datasources** — order, customer, and payment use Postgres on `localhost:5434`; restaurant uses Postgres on `localhost:5432`. Each uses its own schema.
- **Kafka** — bootstrap servers `localhost:19092,29092,39092`; Schema Registry at `localhost:8081`; 3 partitions, replication factor 3.
- **Topics** — `payment-request`, `payment-response`, `restaurant-approval-request`, `restaurant-approval-response`, `customer`.
- **Outbox scheduler** — fixed-rate polling (`outbox-scheduler-fixed-rate`, default `10000` ms).
- **AI** — `spring.ai.openai.*` (model, temperature, max tokens) and `order.ai.interpreter.max-attempts`.
