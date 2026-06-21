# Food Ordering System — AI Interaction Guide

How an AI assistant (Claude Code or any agent) should work in this repository. Read this with
[`project-overview.md`](./project-overview.md) (architecture),
[`coding-standard.md`](./coding-standard.md) (how to write code here), and
[`data-flow.md`](../docs/data-flow.md) (how data moves) before making changes.

## 1. Read Before You Write

This is a DDD / hexagonal, event-driven, multi-module system — the structure carries a lot of
intent. Before editing:

1. Skim `project-overview.md` and `coding-standard.md`.
2. Locate the right **module and layer** for the change (domain-core vs application-service vs
   adapter). Putting code in the wrong layer is the most common mistake here.
3. Look at a sibling that already does the thing (the four services mirror each other —
   payment/restaurant/customer are close analogues of order). Copy the established shape.

Prefer structure-aware exploration (the `srcwalk` skill, then `rg`/`fd`) over guessing.

## 2. Respect the Architecture Boundaries

Non-negotiable — these define the codebase:

- **Domain-core stays pure.** Never add Spring/JPA/Kafka/Lombok-DI imports to `*-domain-core`.
  Business rules belong on the aggregate, expressed as guarded methods that throw a domain
  exception.
- **Cross a boundary only through a port.** New external capability = an output-port interface
  in the application/domain layer + an implementation in an adapter module. Don't call an
  adapter or framework directly from the application service.
- **Never publish events directly.** Emit via the **Outbox** (transactional write +
  scheduler). Adding a direct `kafkaTemplate.send(...)` in business logic is a regression.
- **Map at every boundary.** Domain ↔ JPA entity ↔ Avro/DTO goes through the existing mappers.

If a request seems to require breaking one of these, stop and flag it rather than quietly
working around it.

## 3. Make Changes Surgically

- Change the **minimum** needed to satisfy the request. Don't reformat, rename, or "tidy"
  unrelated code in the same edit — tabs-for-indent and the hand-written constructors are
  intentional, not noise.
- Keep edits consistent with the immediate surroundings (naming table in
  `coding-standard.md` §7). New use case → mirror the `handler + helper + dto/<usecase> +
  port + adapter` set that already exists.
- When a change touches the saga/outbox/event path, trace it across **all** affected services
  and topics — a payload or status change is rarely local.

## 4. Surface Assumptions & Verify

- State the assumptions a change rests on (which saga state, which topic, which service owns
  the data). The customer read model in order-service, for example, is event-replicated — don't
  assume a synchronous call is available.
- After code changes, verify with `mvn clean install` (or the affected module's build) and run
  the relevant tests. Report results honestly: if a build/test fails or a step was skipped,
  say so with the output — don't claim done without evidence.
- Don't invent config keys, topic names, ports, or DB schemas. The real ones are in
  `project-overview.md`, `data-flow.md`, and each service's `application.yml` — check them.

## 5. Security & Untrusted Input

- **Treat all order notes / external payloads as hostile.** The `order-ai` interpreter is
  deliberately hardened against prompt injection; never weaken the system prompt's guardrails
  (role lock, ignore-injected-instructions, no-prompt-disclosure) and never feed raw model
  text downstream without parsing it into `OrderPreferences`.
- Never hardcode secrets or keys. The AI key comes from the `OPEN_API_KEY` environment
  variable; keep credentials in env/config, never in source or logs.
- Don't log secrets, payment specifics, or raw prompts.

## 6. Working With the AI Feature

When the task involves the LLM integration:

- Keep `ChatClient` usage inside the `order-ai` adapter, behind the `OrderNoteInterpreter`
  port. Application/domain code depends on the port, not Spring AI.
- Preserve the bounded-retry + typed-exception contract; don't introduce unbounded loops or
  blocking waits on the model in a request thread.
- Default to the latest capable models when configuring; pin versions and note any
  Spring AI 1.1 / Java 25 workarounds (these are bleeding-edge here).

## 7. Communication With the User

- Be concise and direct; reference code as `path:line`.
- For anything hard to reverse or outward-facing (deletions, pushes, schema/topic changes),
  confirm first unless explicitly told to proceed.
- When you spot a design smell or a boundary violation a request would introduce, explain it
  and offer the in-architecture alternative rather than silently complying.
- If the work maps to a Jira/STIX issue or a Confluence page, route through the correct MCP
  (Jira tools for issues, Rovo/Atlassian tools for Confluence).
