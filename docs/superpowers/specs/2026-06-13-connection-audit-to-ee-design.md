# Connection Audit → EE (Audit Severance) — Design

**Date:** 2026-06-13
**Status:** Draft (pending review)
**Branch:** 0_732
**Predecessor:** `2026-06-13-connection-visibility-to-ee-design.md` (Slices A–E complete). This is the deferred "audit severance" follow-on.

## Goal

Remove **all** connection-audit code from the CE (Apache) artifact. Connection auditing becomes EE-only. After this work, a CE build contains no `@AuditConnection`, no aspect, no audit publisher, no audit contract — only plain domain events that CE legitimately emits and that EE chooses to audit.

Strict-audit synchronous rollback for CE-origin events is **not** preserved (after-commit auditing is acceptable, confirmed during brainstorming). EE-facade `@AuditConnection` methods retain the aspect's existing behavior unchanged.

## Current CE audit footprint (after visibility Slices A–E)

- **Contract** (`platform-connection-api`, CE): `@AuditConnection`, `AuditConnection.AuditData`, `ConnectionAuditEvent` (enum), `ConnectionAuditPublisher`, `ConnectionAuditPayload`, `AuditCorrelation`, `AuditCaptureFailedException`.
- **Aspect** (`platform-connection-service`, CE): `ConnectionAuditAspect` (SpEL eval, boot validation, afterCommit publish, strict rethrow, correlation scope).
- **CE call sites that still audit:**
  1. `WorkspaceConnectionFacadeImpl.create` (`@AuditConnection CONNECTION_CREATED`, reads persisted visibility via SpEL) and `.delete` (`@AuditConnection CONNECTION_DELETED`) — CRUD that stayed CE.
  2. `ProjectDeploymentFacadeImpl` and `ProjectDeploymentJobPrincipalAccessor` — imperative `connectionAuditPublisher.publish(WORKFLOW_PAUSED, connectionId, data)` during execution when a connection is unusable.
- **EE dependents on the contract:** `AiHubAuditAspect`/`AuditAiHub`/`AiHubAuditEvent` reuse the generic `AuditCorrelation` + `AuditCaptureFailedException`; the EE facades moved in Slices B–D carry `@AuditConnection` and rely on the aspect.

## What stays in CE

- The connection lifecycle itself (`create`/`delete`) and the workflow-pause execution logic — these are CE features; only their *auditing* is EE.
- **New plain CE domain events** (records, no audit semantics) emitted via the standard `ApplicationEventPublisher`. Domain events are legitimate CE signals, not audit code.

## The three seams

### 1. Generic audit primitives → shared EE home (`platform-audit`)

`AuditCorrelation` and `AuditCaptureFailedException` are generic (not connection-specific) and are reused by AiHub. Move them to the **EE `platform-audit`** module (`server/ee/libs/platform/platform-audit`, package `com.bytechef.ee.platform.audit.correlation` / `.exception`), which is already the EE audit home (`PersistentAuditEvent`, `PermissionAuditAspect`). Both `ConnectionAuditAspect` (moving, seam 2) and `AiHubAuditAspect` then depend on this shared EE location. This also sets up the eventual aspect-unification (out of scope here, noted as future).

**AiHub rehoming:** repoint `AiHubAuditAspect`'s imports of `AuditCorrelation`/`AuditCaptureFailedException` from `com.bytechef.platform.connection.audit` to the new EE package. AiHub is EE, so EE→EE is fine.

### 2. Connection-specific audit → new EE module

Move `@AuditConnection`, `ConnectionAuditEvent`, `ConnectionAuditPublisher`, `ConnectionAuditPayload`, and `ConnectionAuditAspect` to a **new EE module `platform-connection-audit`** under `server/ee/libs/platform/platform-connection/platform-connection-audit` (api + service, or a single service module), package `com.bytechef.ee.platform.connection.audit`. The EE facades moved in Slices B–D update their `@AuditConnection`/`ConnectionAuditEvent` imports to this EE package and otherwise keep their annotations and behavior. The aspect stays component-scanned by EE apps.

`ConnectionAuditPublisher` retains its current behavior (builds a Spring Boot Actuator `AuditEvent`, fires `AuditApplicationEvent`); it's now an EE bean.

### 3. CE call sites → domain-event seam

Replace every CE reference to the audit contract with publication of a plain CE domain event; an EE `@TransactionalEventListener(AFTER_COMMIT)` (or plain `@EventListener` where no transaction is active) translates it into an audit record via the EE `ConnectionAuditPublisher`.

New CE domain events (records in `platform-connection-api`, package `com.bytechef.platform.connection.event`):
- `ConnectionCreatedEvent(long connectionId, ConnectionVisibility visibility)` — fired by `WorkspaceConnectionFacadeImpl.create` after the connection is persisted (read persisted visibility, matching today's SpEL).
- `ConnectionDeletedEvent(long connectionId)` — fired by `WorkspaceConnectionFacadeImpl.delete`.
- `ConnectionWorkflowPausedEvent(long connectionId, Map<String,Object> data)` — fired by `ProjectDeploymentFacadeImpl` and `ProjectDeploymentJobPrincipalAccessor` where they currently call `publish(WORKFLOW_PAUSED, ...)`.

CE changes: drop the `@AuditConnection` annotations (create/delete), drop the injected `ConnectionAuditPublisher` field/usage (project-deployment sites), and instead `applicationEventPublisher.publishEvent(new ...Event(...))`. Remove the now-dead audit imports.

EE listener (new, in `platform-connection-audit` EE service): `ConnectionAuditEventListener` with handlers mapping each domain event → `connectionAuditPublisher.publish(CONNECTION_CREATED|CONNECTION_DELETED|WORKFLOW_PAUSED, connectionId, data)`. Use `@TransactionalEventListener(AFTER_COMMIT)` for create/delete (transactional facade methods); for the workflow-paused execution path, verify whether a transaction is active — if not, use a plain `@EventListener` (synchronous) so the audit still records. (Implementer confirms per call site.)

## Module placement summary

- `server/ee/libs/platform/platform-audit/...` — `AuditCorrelation`, `AuditCaptureFailedException` (generic primitives).
- NEW `server/ee/libs/platform/platform-connection/platform-connection-audit/...` — `@AuditConnection`, `ConnectionAuditEvent`, `ConnectionAuditPublisher`, `ConnectionAuditPayload`, `ConnectionAuditAspect`, `ConnectionAuditEventListener`. Registered in settings.gradle.kts; EE apps depend on it.
- `platform-connection-api` (CE) — gains the plain domain-event records; loses the entire `audit` package.
- `platform-connection-service` (CE) — loses `ConnectionAuditAspect`.

## Strict-audit behavior

CE-origin events (create/delete/workflow-paused) become after-commit (or synchronous-but-non-blocking) audits — a failure to audit no longer rolls back the operation. This is the accepted tradeoff. EE-facade `@AuditConnection` methods keep the aspect's strict rethrow behavior unchanged (the aspect moves intact). Document the asymmetry in the listener.

## Implementation slices (each its own plan → impl, in order)

**Ordering constraint (load-bearing):** the audit contract can only move to EE *after* every CE
reference to it is gone. The CE call sites (create/delete, project-deployment) reference the contract,
so they must be severed FIRST; the contract+aspect relocate LAST. (The aspect is the last CE user of
the generic primitives once the call sites are severed.) During the sever phase the new EE listeners
call the still-CE `ConnectionAuditPublisher` (EE→CE is fine); the relocate phase repoints them.

- **F1. Sever CE create/delete → domain events.** Add CE `ConnectionCreatedEvent`/`ConnectionDeletedEvent`
  (plain records, `platform-connection-api` `event` package). `WorkspaceConnectionFacadeImpl.create/delete`
  drop `@AuditConnection` and instead `applicationEventPublisher.publishEvent(...)`. Add an EE
  `@TransactionalEventListener(AFTER_COMMIT)` listener (temporary home: EE automation-configuration-service
  alongside the other EE listeners) that maps the events → `connectionAuditPublisher.publish(...)` using the
  still-CE publisher.
- **F2. Sever project-deployment WORKFLOW_PAUSED → domain events.** Add CE `ConnectionWorkflowPausedEvent`;
  `ProjectDeploymentFacadeImpl` + `ProjectDeploymentJobPrincipalAccessor` drop the injected
  `ConnectionAuditPublisher` and publish the event; EE listener handles it (verify tx context → choose
  `@TransactionalEventListener` vs plain `@EventListener`). After F2, CE has NO `@AuditConnection` and NO
  direct publisher usage — the only CE audit code left is the contract + aspect (used solely by EE facades
  + EE listeners now).
- **F3. Relocate the audit contract+aspect+primitives to EE.** Move generic `AuditCorrelation` +
  `AuditCaptureFailedException` → EE `platform-audit`; move connection-specific `@AuditConnection`,
  `ConnectionAuditEvent`, `ConnectionAuditPublisher`, `ConnectionAuditPayload`, `ConnectionAuditAspect` →
  new EE `platform-connection-audit` module. Repoint the EE facades (Org/Reassignment/WorkspaceConnection
  EE), the EE listeners from F1/F2, and AiHub's generic-primitive imports. After F3, `grep` confirms zero
  `connection.audit` / `@AuditConnection` references in CE production code.

## Testing

- EE listener tests: each domain event → expected `ConnectionAuditPublisher.publish(...)` call (mock the publisher).
- Keep/move the existing `ConnectionAuditAspectTest`, `ConnectionAuditPublisherTest` to the EE module.
- CE facade tests: assert the domain event is published (verify `ApplicationEventPublisher.publishEvent`), not that audit happens.
- AiHub aspect tests unaffected beyond import changes.
- Cross-edition `compileJava` after each slice; final `grep -rn "connection.audit\|@AuditConnection\|ConnectionAuditPublisher\|AuditCorrelation" server/libs` returns nothing in CE production code.
- Per-app context-load: CE app starts with no audit beans; EE app wires the aspect + listener.

## Risks & open points

- **New EE module** (`platform-connection-audit`) requires settings.gradle.kts registration + EE app dependency wiring; confirm the EE app aggregates it (mirror how `platform-audit` is pulled in).
- **Workflow-paused tx context**: confirm whether the execution path is transactional; pick `@TransactionalEventListener` vs `@EventListener` accordingly so the audit isn't silently dropped.
- **Boot-time SpEL validation** in `ConnectionAuditAspect` scans all beans for `@AuditConnection`; after the move it only sees EE beans — fine, but verify the validation listener still fires in EE.
- **Actuator dependency**: `ConnectionAuditPublisher` builds a Spring Boot Actuator `AuditEvent`; ensure the new EE module has the actuator dep (it did in CE).
- Future: with `AuditCorrelation`/`AuditCaptureFailedException` shared in EE and both aspects (`ConnectionAuditAspect`, `AiHubAuditAspect`) co-located in EE, the long-discussed **aspect unification** (shared SpEL audit engine) becomes a clean follow-on — explicitly out of scope here.
