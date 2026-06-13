# AGENTS.md — Food Ordering System

Java 17, Spring Boot 2.6.3, Maven multi-module. DDD + Clean Architecture.
Build: `com.food.ordering.system` / `1.0-SNAPSHOT`.

## Module Map

```
common-domain                  ← BaseEntity, AggregateRoot, DomainEvent, value objects, DomainException
order-domain-core              ← Order (aggregate), OrderItem, Product, Restaurant; OrderItemId, StreetAddress, TrackingId
order-application-service      ← [EMPTY] use cases — depends on order-domain-core
order-application              ← OrderController [EMPTY], OrderGlobalExceptionHandler [EMPTY] — depends on order-application-service
order-dataaccess               ← [EMPTY] persistence — depends on order-application-service
order-messaging                ← [EMPTY] Kafka — depends on order-application-service
order-container                ← [EMPTY] Spring Boot main, wires everything
```

Dependency chain: `common-domain` → `order-domain-core` → `order-application-service` → `order-application | order-dataaccess | order-messaging` → `order-container`.

## Conventions

- **Entities** extend `BaseEntity<ID>` (identity-based equals/hashCode) or `AggregateRoot<ID>`. Constructed via **private constructor + static `Builder` inner class** — no public constructors, no setters on final fields.
- **Value Object IDs** extend `BaseId<T>`. Protected ctor = domain-created; public ctor = externally-created. UUID-backed except `OrderItemId` (`Long`, sequential). `Money` is immutable, `BigDecimal` with scale 2 (HALF_EVEN), arithmetic returns new instances.
- **Domain exceptions** extend `DomainException` (RuntimeException). Per-bounded-context subclasses (e.g. `OrderDomainException`).
- **Lombok** — root `<scope>provided</scope>`, all modules inherit it. Do NOT use `@EqualsAndHashCode` on entities; they inherit it from `BaseEntity`.

## Order State Machine

PENDING → pay() → PAID → approve() → APPROVED
PAID → initCancel() → CANCELLING → cancel() → CANCELLED
PENDING → cancel() → CANCELLED
FAILED is defined but not yet used.

Every transition validates current state; throws `OrderDomainException` on invalid moves. `initializeOrder()` assigns UUID IDs + PENDING. `validateOrder()` checks price > 0 and items subtotal = total.

## Empty Modules

`order-application-service`, `order-dataaccess`, `order-messaging`, `order-container` have no Java source yet.
`OrderController.java` and `OrderGlobalExceptionHandler.java` are 0-byte stubs.

## Testing

JUnit 5. `src/test/java` per module. Plain unit tests for domain logic (no Spring context). Spring Boot test slices for adapters.
