# Food Ordering System — Sequence Diagrams

Detailed interaction flows for the main use cases. These mirror the actual implementation
(`OrderCreateCommandHandler`, `OrderPaymentSaga`, `OrderApprovalSaga`, the outbox schedulers,
and the Kafka listeners/publishers). See [`data-flow.md`](./data-flow.md) for the topic-level
view and [`project-overview.md`](./project-overview.md) for architecture.

Diagrams use Mermaid. The recurring **Outbox + Scheduler** hop (write row in TX → scheduler
polls → publisher emits Avro → status set to COMPLETED) is shown explicitly the first time and
abbreviated as "via outbox → topic" afterwards.

---

## 1. Create Order (synchronous request + AI interpretation)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant OC as OrderController
    participant CH as OrderCreateCommandHandler
    participant H as OrderCreateHelper
    participant DS as OrderDomainService
    participant AI as AIOrderNoteInterpreter
    participant OAI as OpenAI API
    participant ORepo as OrderRepository
    participant POH as PaymentOutboxHelper
    participant DB as (order schema)

    Client->>OC: POST /orders (CreateOrderCommand)
    OC->>CH: createOrder(command)
    activate CH
    Note over CH: @Transactional
    CH->>H: persistOrder(command)
    H->>H: checkCustomer() / checkRestaurant()<br/>(local read models)
    H->>AI: interpret(orderNotes)
    AI->>OAI: prompt (system + template, retry up to N)
    OAI-->>AI: structured OrderPreferences
    AI-->>H: OrderPreferences
    H->>DS: validateAndInitiateOrder(order, restaurant)
    DS-->>H: OrderCreatedEvent (status=PENDING)
    H->>ORepo: save(order)
    ORepo->>DB: INSERT order
    H-->>CH: OrderCreatedEvent
    CH->>POH: savePaymentOutboxMessage(payload, PENDING, STARTED)
    POH->>DB: INSERT payment_outbox (OutboxStatus=STARTED)
    CH-->>OC: CreateOrderResponse (trackingId)
    deactivate CH
    OC-->>Client: 200 OK (trackingId, "Order Created Successfully")
```

> The customer/restaurant checks read **local replicas** in the order schema (populated via the
> `customer` topic and seeded restaurant data) — no synchronous cross-service call.
> If the AI interpreter fails after its retries, order creation aborts (the whole TX rolls back).

---

## 2. Payment Step (order → payment → order)

```mermaid
sequenceDiagram
    autonumber
    participant PScheduler as PaymentOutboxScheduler
    participant PPub as PaymentRequestKafkaPublisher
    participant K as Kafka
    participant PListen as PaymentRequestKafkaListener
    participant PSvc as Payment App Service
    participant PRespPub as payment-response (via outbox)
    participant ORespListen as PaymentResponseKafkaListener
    participant Saga as OrderPaymentSaga

    Note over PScheduler: @Scheduled every 10s
    PScheduler->>PScheduler: poll outbox (STARTED, saga STARTED/COMPENSATING)
    PScheduler->>PPub: publish(outboxMessage, ackCallback)
    PPub->>K: payment-request (Avro)
    PPub-->>PScheduler: ack → set OutboxStatus=COMPLETED

    K->>PListen: payment-request
    PListen->>PSvc: completePayment(paymentRequest)
    Note over PSvc: debit customer credit,<br/>save Payment + payment outbox (same TX)
    PSvc->>PRespPub: payment-response (COMPLETED)
    PRespPub->>K: payment-response (Avro)

    K->>ORespListen: payment-response
    ORespListen->>Saga: process(paymentResponse)
    activate Saga
    Note over Saga: @Transactional
    Saga->>Saga: getPaymentOutboxMessage(sagaId, STARTED)
    alt already processed (empty)
        Saga-->>ORespListen: return (idempotent no-op)
    else first time
        Saga->>Saga: payOrder(order) → status PAID
        Saga->>Saga: update payment outbox (PROCESSING)
        Saga->>Saga: saveApprovalOutboxMessage(STARTED)
    end
    deactivate Saga
```

---

## 3. Restaurant Approval Step (order → restaurant → order) — Happy Path

```mermaid
sequenceDiagram
    autonumber
    participant AScheduler as RestaurantApprovalOutboxScheduler
    participant K as Kafka
    participant RListen as RestaurantApprovalRequestKafkaListener
    participant RSvc as Restaurant App Service
    participant AListen as RestaurantApprovalResponseKafkaListener
    participant Saga as OrderApprovalSaga

    AScheduler->>K: restaurant-approval-request (via outbox)
    K->>RListen: restaurant-approval-request
    RListen->>RSvc: approveOrder(request)
    Note over RSvc: check items/availability,<br/>save approval + outbox (same TX)
    RSvc->>K: restaurant-approval-response (APPROVED)

    K->>AListen: restaurant-approval-response
    AListen->>Saga: process(restaurantApprovalResponse)
    activate Saga
    Note over Saga: @Transactional
    Saga->>Saga: getApprovalOutbox(sagaId, PROCESSING)
    alt already processed
        Saga-->>AListen: return (idempotent no-op)
    else first time
        Saga->>Saga: approveOrder(order) → status APPROVED
        Saga->>Saga: update approval outbox (SUCCEEDED)
        Saga->>Saga: update payment outbox (SUCCEEDED)
    end
    deactivate Saga
    Note over Saga: Order APPROVED — saga complete
```

---

## 4. Compensation — Restaurant Rejects the Order

When the restaurant rejects an already-**PAID** order, `OrderApprovalSaga.rollback` runs and
emits a payment-cancel request; payment-service refunds, and `OrderPaymentSaga.rollback`
finalizes the order as **CANCELLED**.

```mermaid
sequenceDiagram
    autonumber
    participant K as Kafka
    participant AListen as RestaurantApprovalResponseKafkaListener
    participant ApprSaga as OrderApprovalSaga
    participant PaySaga as OrderPaymentSaga
    participant PSvc as Payment App Service

    K->>AListen: restaurant-approval-response (REJECTED)
    AListen->>ApprSaga: rollback(response)
    activate ApprSaga
    Note over ApprSaga: @Transactional
    ApprSaga->>ApprSaga: cancelOrderPayment(order) → status CANCELLING
    ApprSaga->>ApprSaga: update approval outbox (COMPENSATING)
    ApprSaga->>ApprSaga: savePaymentOutboxMessage(cancel, COMPENSATING)
    deactivate ApprSaga

    Note over ApprSaga,K: PaymentOutboxScheduler publishes<br/>payment-request (cancel) — picks up COMPENSATING rows
    ApprSaga-->>K: payment-request (CANCEL) via outbox
    K->>PSvc: cancelPayment(request)
    Note over PSvc: credit refunded to customer
    PSvc->>K: payment-response (CANCELLED) via outbox

    K->>PaySaga: rollback(paymentResponse)
    activate PaySaga
    Note over PaySaga: @Transactional
    PaySaga->>PaySaga: cancelOrder(order, failureMessages) → status CANCELLED
    PaySaga->>PaySaga: update payment outbox (COMPENSATED)
    PaySaga->>PaySaga: update approval outbox (COMPENSATED)
    deactivate PaySaga
    Note over PaySaga: Order CANCELLED — compensation complete
```

> A **payment failure** at step 2 (instead of restaurant rejection) follows the same pattern:
> `OrderPaymentSaga.rollback` is invoked directly with `PaymentStatus.FAILED`, cancelling the
> order without ever reaching the restaurant. `getCurrentSagaStatus()` maps the payment status
> to the saga states eligible for rollback (`STARTED` / `PROCESSING`).

---

## 5. End-to-End Happy Path (condensed)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Order as order-service
    participant Pay as payment-service
    participant Rest as restaurant-service

    Client->>Order: POST /orders
    Order-->>Client: 200 OK (PENDING, trackingId)
    Order->>Pay: payment-request (via outbox)
    Pay->>Order: payment-response COMPLETED
    Note over Order: Order → PAID
    Order->>Rest: restaurant-approval-request (via outbox)
    Rest->>Order: restaurant-approval-response APPROVED
    Note over Order: Order → APPROVED ✅
    Client->>Order: GET /orders/{trackingId}
    Order-->>Client: TrackOrderResponse (APPROVED)
```

---

## 6. Create Customer & Replication to order-service

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant CC as CustomerController
    participant CAS as Customer App Service
    participant CDB as (customer schema)
    participant K as Kafka
    participant OL as CustomerKafkaListener (order-service)
    participant ODB as (order schema)

    Client->>CC: POST /customers (CreateCustomerCommand)
    CC->>CAS: createCustomer(command)
    Note over CAS: save customer + outbox (same TX)
    CAS->>CDB: INSERT customer
    CAS-->>CC: CreateCustomerResponse
    CC-->>Client: 200 OK

    CAS->>K: customer (via outbox)
    K->>OL: customer event
    OL->>ODB: upsert customer read model
    Note over ODB: order-service can now validate<br/>this customer at order creation
```

---

## Notes on Idempotency & Reliability

- **Idempotent saga steps** — every `process`/`rollback` first looks up the outbox row by
  `(sagaId, expected SagaStatus)`. If it's missing/already advanced, the step is a no-op, so
  duplicate Kafka deliveries (at-least-once) are safe.
- **Atomicity** — domain state and the outbox row are written in one `@Transactional` unit;
  publishing happens later from the committed row, so a crash can never lose an event or emit
  one for an uncommitted state.
- **Cleanup** — `*OutboxCleanerScheduler` deletes `COMPLETED` outbox rows to bound table growth.
