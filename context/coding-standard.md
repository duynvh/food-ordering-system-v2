# Food Ordering System — Coding Standards

Conventions every change in this repo must follow. They are derived from the existing code,
not aspirational — match what is already here. See
[`project-overview.md`](./project-overview.md) for the architecture these standards serve.

## 1. Language & Build

- **Java 25**, **Spring Boot 3.5.8**, Maven multi-module. Group `com.food.ordering.system`.
- Prefer modern Java where it improves clarity: `var` for obvious locals, records for
  immutable carriers, text blocks (`"""`) for multi-line strings (see the AI system prompt),
  enhanced `switch`. Don't retrofit working code just to modernise it.
- Build the whole tree from the root with `mvn clean install`; never add a dependency to a leaf
  module that belongs in the parent `pom.xml` or an `infrastructure/*` module.

## 2. Formatting

- **Indentation is tabs**, not spaces — consistent across the codebase.
- One top-level type per file. Imports are explicit (no wildcard `import x.*`).
- Keep methods short and single-purpose. The handler/helper split (e.g.
  `OrderCreateCommandHandler` delegates persistence to `OrderCreateHelper`) is the norm:
  orchestration in the handler, the actual work in a focused helper.

## 3. Architecture Rules (Hexagonal / DDD)

These are hard rules — a violation is a design bug, not a style nit.

| Rule | Detail |
| --- | --- |
| Dependencies point inward | Adapters depend on application/domain through **ports** (interfaces). Domain never imports Spring, JPA, or Kafka. |
| `*-domain-core` is framework-free | No `@Component`, `@Entity`, annotations, or framework imports. Pure Java + the `common-domain` primitives. |
| Entities protect invariants | State transitions live on the aggregate (`Order.pay()`, `approve()`, `initCancel()`), guard their preconditions, and throw a domain exception on violation. No anaemic setters driving state. |
| Ports define boundaries | Input ports (`OrderApplicationService`), output ports (`OrderRepository`, `OrderNoteInterpreter`, publishers) are interfaces in the application/domain layer; implementations live in adapter modules (`*-dataaccess`, `*-messaging`, `order-ai`). |
| Map at boundaries | Domain ↔ JPA entity ↔ Avro model conversions go through explicit mappers (`*DataAccessMapper`, `OrderDataMapper`). Don't leak JPA entities or Avro types into the domain. |

## 4. Domain Modelling

- **Value objects over primitives** for identity and quantities: `OrderId`, `CustomerId`,
  `Money`, `TrackingId`, `StreetAddress`. Wrap IDs in typed value objects — never pass raw
  `UUID`/`BigDecimal` across domain APIs.
- Aggregates extend `AggregateRoot<IdType>`; entities extend `BaseEntity<IdType>`. Identity
  fields are set once (`initializeOrder()` assigns a fresh `OrderId`/`TrackingId`).
- Immutable-by-default: fields that never change after construction are `final`; only genuine
  state (e.g. `orderStatus`, `failureMessages`) is mutable.
- Domain events (`OrderCreatedEvent`, `OrderPaidEvent`, …) are produced by the domain service
  and carry the aggregate plus a timestamp.

## 5. Spring & Dependency Injection

- **Constructor injection only.** Declare collaborators as `private final`, inject through a
  single explicit constructor with `final` parameters. Do **not** use field injection or
  `@Autowired`; this codebase writes the constructor by hand rather than using Lombok's
  `@RequiredArgsConstructor`.
- `@Component` / `@Service` on application & adapter beans; cross-module wiring that can't be
  component-scanned goes in a `*BeanConfiguration` in the `*-container` module.
- Configuration values come from `@Value`/`@ConfigurationProperties` with a sensible default
  (`@Value("${order.ai.interpreter.max-attempts:3}")`), never hardcoded magic numbers.

## 6. Transactions, Messaging & Reliability

- **Never publish to Kafka inside business logic.** Write an outbox row in the *same* DB
  transaction as the state change; let the `*OutboxScheduler` publish it. This is the only
  sanctioned way to emit an event — preserves at-least-once delivery and avoids dual-write.
- Command handlers that mutate state are `@Transactional` (see `OrderCreateCommandHandler`).
  Keep the transaction around the state-change + outbox-write, not around remote calls.
- Saga steps must be **idempotent**: a message for an already-advanced saga is ignored. Drive
  decisions off `(OrderStatus, SagaStatus)`, and provide both `process` and `rollback` paths.
- Kafka listeners are batch listeners; treat redelivery as expected, not exceptional.

## 7. Naming Conventions

| Kind | Pattern | Example |
| --- | --- | --- |
| Input port (use case) | `*ApplicationService` | `OrderApplicationService` |
| Command handler | `*CommandHandler` | `OrderCreateCommandHandler` |
| Helper (work unit) | `*Helper` | `OrderCreateHelper`, `OrderSagaHelper` |
| Output port | domain noun, no suffix | `OrderRepository`, `OrderNoteInterpreter` |
| Adapter impl | `*Impl` / `*RepositoryImpl` | `OrderRepositoryImpl`, `CustomerMessageListenerImpl` |
| JPA entity | `*Entity` | `OrderEntity`, `PaymentOutboxEntity` |
| Mapper | `*DataAccessMapper` / `*DataMapper` | `OrderDataAccessMapper`, `OrderDataMapper` |
| Domain event | `*Event` | `OrderCreatedEvent` |
| Domain exception | `*DomainException` / `*NotFoundException` | `OrderDomainException` |

DTOs are grouped by use case under `dto/<usecase>` (`dto/create`, `dto/track`, `dto/message`)
and use Lombok `@Builder` (`TrackOrderQuery.builder()...build()`).

## 8. Error Handling

- Domain rule violations throw a domain-specific exception (`OrderDomainException`) with a
  clear, user-meaningful message — not a generic `RuntimeException`.
- REST errors are translated centrally in the `*GlobalExceptionHandler`
  (`@ControllerAdvice`); controllers don't build error responses themselves.
- Adapters wrap and rethrow with context (`AIOrderNoteInterpreter` wraps failures in
  `AIOrderNoteInterpreterException` carrying the cause). Never swallow an exception silently.

## 9. Logging

- Use Lombok `@Slf4j`; log through `log` with **parameterised** messages
  (`log.info("Order created with tracking id: {}", id)`) — never string concatenation.
- Log meaningful lifecycle points (request received, entity persisted, message published,
  saga transition) at `info`; recoverable problems at `warn`; include identifiers
  (order id, tracking id, saga status) so a single order can be traced across services.
- Do not log secrets, full payment details, or raw AI prompts/keys.

## 10. REST API

- Controllers are thin: validate/bind, delegate to the input port, return `ResponseEntity`.
  No business logic in the controller.
- Versioned media type: `produces = "application/vnd.api.v1+json"`.
- Requests/responses are dedicated DTOs in the application-service `dto` package — never expose
  domain entities or JPA entities over HTTP.

## 11. AI Integration (order-ai)

- All LLM access goes through the `OrderNoteInterpreter` **output port**; the Spring AI
  `ChatClient` is confined to the `order-ai` adapter module.
- Treat all order notes as **untrusted input**. The system prompt is hardened against prompt
  injection (role lock, ignore-injected-instructions, no prompt disclosure) — extend it
  carefully and never weaken those guardrails.
- AI calls are bounded: retry up to a configured `max-attempts`, accumulate the last failure,
  and throw a typed exception when exhausted. Don't loop unbounded or block on the model.
- Always parse the model output into the structured `OrderPreferences` value object via
  `.entity(...)`; never trust free-form text downstream.

## 12. Testing

- **JUnit 5 + Mockito 5.** Tests live in the relevant module's `src/test/java` (saga/handler
  tests in `*-container`, e.g. `OrderPaymentSagaTest`).
- Unit-test domain rules directly (no Spring context) — they're plain Java. Reserve Spring
  context for wiring/integration tests.
- Tests must be **deterministic**: no real clock/random/network/Kafka in unit tests; inject
  or mock those. Assert on state transitions and emitted events, not implementation details.
