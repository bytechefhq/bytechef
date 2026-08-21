# Audit Events Settings Page — Design

**Date:** 2026-04-15
**Edition:** EE only
**Scope:** Organization-level platform settings → new `Audit Events` page.

## 1. Goal

Give organization admins a read-only UI to browse the `persistent_audit_event` table. Support filtering by principal, event type, and date range. Show event metadata on row expand.

Additionally, bridge the existing `ConnectionAuditEvent` application events (published by CE-side `ConnectionAuditAspect`) into `persistent_audit_event` so connection activity actually appears on the page alongside `PERMISSION_CHECK` events.

## 2. Non-goals

- No write operations (no delete/export/retention policy in this iteration).
- No free-text search across the `data` key/value map.
- No workspace-level scoping — page is organization-wide.
- No audit of actions that currently have no publisher (outside `@PreAuthorize` and `@AuditConnection`).

## 3. Backend

### 3.1 Service rename

Rename `PermissionAuditEventService` → `AuditEventService` (module `platform-audit-service`). It is no longer permission-specific; it will be called by the connection-event listener too.

- File rename + package-local imports updated (`PermissionAuditAspect`, any test).
- Keep `@Transactional(propagation = REQUIRES_NEW)` on `save()` — needed because `ConnectionAuditAspect` runs `@AfterReturning` on facade methods that may themselves be inside read-only transactions (same reason permission aspect requires it).
- Add read-side methods:
  - `Page<PersistentAuditEvent> fetchAuditEvents(String principal, String eventType, LocalDateTime from, LocalDateTime to, Pageable pageable)`
  - `List<String> fetchEventTypes()` — `SELECT DISTINCT event_type ...` for the filter dropdown.

### 3.2 Repository extension

`PersistenceAuditEventRepository` gains:

```java
@Query("""
    SELECT * FROM persistent_audit_event
    WHERE (:principal IS NULL OR principal = :principal)
      AND (:eventType IS NULL OR event_type = :eventType)
      AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR event_date >= :fromDate)
      AND (CAST(:toDate   AS TIMESTAMP) IS NULL OR event_date <= :toDate)
    """)
Page<PersistentAuditEvent> findAllFiltered(
    @Param("principal") String principal,
    @Param("eventType") String eventType,
    @Param("fromDate") LocalDateTime fromDate,
    @Param("toDate") LocalDateTime toDate,
    Pageable pageable);

@Query("SELECT DISTINCT event_type FROM persistent_audit_event ORDER BY event_type")
List<String> findDistinctEventTypes();
```

(Spring Data JDBC supports `@Query` with named params; `CAST(:x AS TIMESTAMP) IS NULL` is the PostgreSQL-friendly way to null-guard optional date params.)

Existing derived-name methods (`findByPrincipal`, `findByEventDateAfter`, etc.) are retained — removing them is out of scope and risks breaking unseen callers.

### 3.3 Connection-event bridge

New `ConnectionAuditEventListener` in `platform-audit-service` (EE), `@ConditionalOnEEVersion`:

```java
@EventListener
public void onConnectionAuditEvent(ConnectionAuditEvent event) {
    String principal = SecurityUtils.fetchCurrentUserLogin().orElse("anonymoususer");

    PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

    persistentAuditEvent.setEventDate(LocalDateTime.now());
    persistentAuditEvent.setEventType("CONNECTION_" + event.getEvent());
    persistentAuditEvent.setPrincipal(principal);

    Map<String, String> data = new HashMap<>();
    data.put("connectionId", String.valueOf(event.getConnectionId()));
    event.getData().forEach((k, v) -> data.put(k, v == null ? "null" : v.toString()));
    persistentAuditEvent.setData(data);

    auditEventService.save(persistentAuditEvent);
}
```

Failures are caught and logged (same defensive pattern as `PermissionAuditAspect`) so audit persistence cannot break the originating business operation.

Unit test: verify listener maps event → `PersistentAuditEvent` correctly and calls `AuditEventService.save()`.

### 3.4 GraphQL module

New Gradle module: `server/ee/libs/platform/platform-audit/platform-audit-graphql/` (mirror structure of `platform-user-graphql`).

Schema `src/main/resources/graphql/audit-event.graphqls`:

```graphql
extend type Query {
    auditEvents(
        principal: String
        eventType: String
        fromDate: Long
        toDate: Long
        page: Int = 0
        size: Int = 25
    ): AuditEventPageType!

    auditEventTypes: [String!]!
}

type AuditEventType {
    id: ID!
    principal: String
    eventDate: Long!
    eventType: String!
    data: [AuditEventDataEntryType!]!
}

type AuditEventDataEntryType {
    key: String!
    value: String!
}

type AuditEventPageType {
    content: [AuditEventType!]!
    number: Int!
    size: Int!
    totalElements: Int!
    totalPages: Int!
}
```

Controller: `AuditEventGraphQlController` with `@PreAuthorize("hasRole('ADMIN')")` on both queries. Sorted by `eventDate DESC` by default.

Date params are GraphQL `Long` (epoch millis) → converted to `LocalDateTime` in controller (UTC), matching existing `createdDate` convention across schemas.

`data` map is serialized as a sorted list of `{key, value}` entries so GraphQL has a stable output shape.

### 3.5 Module registration

- Add `platform-audit-graphql` to relevant `settings.gradle.kts`.
- EE microservice apps that need the GraphQL endpoint (configuration-app, api-gateway, etc.) pick it up via their existing EE remote-client / graphql aggregation — verify during implementation.

## 4. Frontend

### 4.1 GraphQL codegen

- Add `server/ee/libs/platform/platform-audit/platform-audit-graphql/src/main/resources/graphql/*.graphqls` to `client/codegen.ts` `schema` array.
- New operation file: `client/src/graphql/auditEvents/auditEvents.graphql` with `auditEvents` query + `auditEventTypes` query.
- Run `npx graphql-codegen` to regenerate `src/shared/middleware/graphql.ts`. Commit operation + generated file in separate commits.

### 4.2 Page

New directory: `client/src/ee/pages/settings/platform/audit-events/`.

- `AuditEvents.tsx` — top-level page with filter bar + paginated table + detail drawer.
- `components/AuditEventsFilterBar.tsx` — three controls:
  - Principal: text input (debounced).
  - Event type: `<Select>` populated from `auditEventTypes` query.
  - Date range: existing `DateRangePicker` component if present, else two date inputs.
- `components/AuditEventsTable.tsx` — columns: Date, Principal, Event Type, Actions (expand button). Row click / expand button opens detail drawer.
- `components/AuditEventDetailSheet.tsx` — `Sheet` from shadcn showing full event metadata and a `<dl>` list of `data` key/value pairs. Empty state when no data.
- Pagination: `Pagination` component at table footer; page size fixed at 25 for v1.

Follow existing settings-page conventions:
- Interface names end in `I` or `Props`.
- Lucide icons imported with `Icon` suffix.
- Object keys sorted alphabetically (`sort-keys`).
- `twMerge` not `cn`.
- Hook ordering per CLAUDE.md.

### 4.3 Route + nav wiring

`client/src/routes.tsx`:

```tsx
const AuditEvents = lazy(() => import('@/ee/pages/settings/platform/audit-events/AuditEvents'));

// in platform settings children[]:
{
    element: (
        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
            <EEVersion>
                <LazyLoadWrapper>
                    <AuditEvents />
                </LazyLoadWrapper>
            </EEVersion>
        </PrivateRoute>
    ),
    path: 'audit-events',
},

// in navItems[], after admin-api-keys:
{href: 'audit-events', title: 'Audit Events'},
```

No feature-flag gating — the page is admin-only + EE-only, which is sufficient.

## 5. Data flow

```
@PreAuthorize method call ──▶ PermissionAuditAspect ──▶ AuditEventService.save ──▶ persistent_audit_event
@AuditConnection method call ─▶ ConnectionAuditAspect ─▶ ConnectionAuditPublisher (Spring event)
                                                               │
                                                               ▼
                                         ConnectionAuditEventListener (EE) ──▶ AuditEventService.save ──▶ persistent_audit_event

AuditEvents.tsx ──▶ auditEvents GraphQL query ──▶ AuditEventGraphQlController ──▶ AuditEventService.fetchAuditEvents ──▶ repository
```

## 6. Testing

- **Repository test** (`PersistenceAuditEventRepositoryIntTest`): insert a few events across principals/types/dates, assert `findAllFiltered` with various filter combinations returns expected pages. Testcontainers PostgreSQL.
- **Service test** (`AuditEventServiceTest`): thin unit test of new read methods with mocked repository.
- **Listener test** (`ConnectionAuditEventListenerTest`): publishes a `ConnectionAuditEvent` via `ApplicationEventPublisher`, verifies a `PersistentAuditEvent` is saved with expected eventType / data / principal.
- **Controller test** (`AuditEventGraphQlControllerTest`): GraphQL query test verifying shape, pagination metadata, and `@PreAuthorize` enforcement (non-admin → access denied).
- **Client**: component render test for filter bar + table rendering a few rows. No E2E for v1.

## 7. Open risks / follow-ups

- **Table growth**: `persistent_audit_event` has no retention policy. Every `@PreAuthorize`-annotated call persists a row. A retention / archival job is out of scope here but worth flagging.
- **`data` column performance**: filtering by `data` contents (e.g. `connectionId`) would require a join or JSON column. Not needed for v1 but may come up.
- **Principal anonymization**: events include raw logins. If GDPR/PII rules matter, a future iteration should mask or hash principal for non-super-admins.

## 8. Acceptance criteria

- Admin sees `Audit Events` nav item in platform settings under EE.
- Non-admin cannot reach the page (route-guard) and cannot call the query (GraphQL `@PreAuthorize`).
- Page lists events paginated, newest first.
- Filtering by principal / event type / date range works independently and in combination.
- A connection mutation that already triggers `@AuditConnection` (e.g. share connection with project) produces a row visible in the UI within seconds.
- All existing permission-check rows still appear (no regression from the service rename).
