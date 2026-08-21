# Per-Action Component Policies — Design

**Date:** 2026-08-04
**Status:** Approved design, pending implementation plan
**Predecessor:** `2026-06-20-component-policies-visibility-design.md` (component-level visibility; deferred
"Rules / Restrictions / Claims" as future work). Also promised as part of the "per-workspace policy
overrides (separate specs)" note in `2026-07-31-ai-guardrails-standalone-design.md` §5.

## Problem

Component Policies today are all-or-nothing: an organization can disable a whole component, but not a
single operation on it. Admins want to keep a component available while blocking specific operations —
e.g. allow a CRM component's reads but disable its `deleteRecord` action, or allow manual actions while
blocking a polling trigger.

## Goals

- Per-action AND per-trigger disable toggles, layered under the existing component toggle.
- Deny-list semantics: default is everything enabled; only exceptions are stored.
- Disabled operations disappear from editor pickers and fail at execution.
- Zero behavior change for CE and for deployments that never touch the new toggles.

## Non-Goals (future)

- Per-workspace / per-environment scoping (org-wide only, same as the component toggle; the
  "per-workspace policy overrides" promise from the guardrails spec remains a separate follow-up).
- Allow-list mode ("only these operations") — deny-list only.
- Policing tool-typed cluster elements independently — tools stay governed by the component toggle and
  inherit per-action policy in a later slice.
- Deploy-time validation ("this workflow references a disabled operation") and proactive trigger
  lifecycle handling (unregistering live webhooks/polls). Deployed workflows fail at next execution.
- Rules / Restrictions / Claims tabs beyond this (the page keeps its single Component Visibility tab;
  per-operation toggles live inside the existing component rows).

## Design

### 1. Data model

New table `component_operation_policy` in the existing `platform-component-policy` module's Liquibase
changelog:

| column               | type         | notes                                            |
| -------------------- | ------------ | ------------------------------------------------ |
| `id`                 | BIGINT       | surrogate PK                                     |
| `component_name`     | VARCHAR      | part of composite unique key                     |
| `operation_type`     | INT          | `OperationType` enum ordinal: `ACTION`=0, `TRIGGER`=1, append-only |
| `operation_name`     | VARCHAR      | action/trigger name                              |
| `created_by/date`, `last_modified_by/date` | audit columns | same as `component_policy` |
| `version`            | INT          | optimistic locking                               |

- Composite unique key on `(component_name, operation_type, operation_name)`; surrogate `id` PK
  (the natural key is three columns — Spring Data JDBC composite IDs are not worth it here).
- **Pure deny-list**: a row exists only while the operation is disabled. Re-enabling deletes the row.
  There is no `enabled` column — presence IS the policy. Absence of any row = enabled, mirroring the
  component-level "absent row = enabled" model.
- No FK to `component_policy`: a component needs no `component_policy` row to have a disabled
  operation, and the two tables are independently deny-listed.
- `OperationType` ordinals are pinned by an `EnumOrdinalStabilityTest`-style test; new kinds (e.g.
  `CLUSTER_ELEMENT`) append at the end.

Domain class `ComponentOperationPolicy`, Spring Data JDBC repository, and service methods on the
existing `ComponentPolicyService`:

- `Set<String> getDisabledOperationKeys()` — flat keys `componentName#operationType#operationName`
  for the visibility provider (one query).
- `List<ComponentOperationPolicy> getComponentOperationPolicies(String componentName)` — for GraphQL.
- `void updateComponentOperationPolicy(String componentName, OperationType operationType, String operationName, boolean enabled)`
  — `enabled=true` deletes the row (idempotent), `enabled=false` inserts if absent (idempotent).

### 2. SPI extension (CE seam)

`ComponentVisibilityProvider` (CE, `platform-component-api`) gains two **default methods**:

```java
default boolean isActionVisible(String componentName, String actionName) {
    return isVisible(componentName);
}

default boolean isTriggerVisible(String componentName, String triggerName) {
    return isVisible(componentName);
}
```

CE (no provider bean) and any other implementor are untouched; a disabled component already implies
every operation is invisible, which the defaults preserve.

The EE `ComponentPolicyVisibilityProvider` overrides both. Its Caffeine cache value becomes a small
record `DisabledPolicies(Set<String> componentNames, Set<String> operationKeys)` loaded in one pass per
tenant per 10s TTL — administrative toggles keep the same propagation latency as today.

### 3. Enforcement

Two layers, mirroring the component-level pattern exactly:

**Execution guards.**

- `ActionDefinitionServiceImpl` — the existing visibility check used by `executePerform` and
  `executePerformForPolyglot` gains a sibling `isActionVisible(componentName, actionName)` check;
  failure throws `ConfigurationException` with new `ActionDefinitionErrorType.ACTION_DISABLED`
  (appended to the enum, never reordered).
- `TriggerDefinitionServiceImpl` — same at its existing chokepoint, throwing new
  `TriggerDefinitionErrorType.TRIGGER_DISABLED`.
- `ClusterElementDefinitionServiceImpl` — unchanged (component toggle only; see Non-Goals).

**Listing filters.**

- `ActionDefinitionServiceImpl.getActionDefinitions(componentName, componentVersion)` and
  `TriggerDefinitionServiceImpl.getTriggerDefinitions(...)` filter out invisible operations.
- `ComponentDefinitionServiceImpl`'s component-detail mapping filters the action/trigger lists on the
  returned DTO, so editor pickers, copilot component lookups, and anything reading the detail
  definition never see a disabled operation.
- The build-time component index stubs (components-list action/trigger counts) are deliberately NOT
  policy-filtered — the list view's counts may be off by the number of disabled operations. Accepted:
  stubs are tenant-agnostic build artifacts and policy is tenant data.

**Runtime semantics.** Already-deployed workflows keep their trigger registrations; the next
execution of a disabled operation fails with the new error type. No deploy-time validation, no
lifecycle sweep. Policy rows referencing operations that no longer exist (a component update removed
an action) are inert at enforcement and invisible in every non-admin surface; the policy UI lists
them from the deny-list rows (name-only) so an admin can still re-enable or clean them up; they are
never self-deleted. (Amended 2026-08-04: the original "invisible in the UI" wording conflicted with
the union rendering in section 5.)

### 4. GraphQL

In `platform-component-policy-graphql`, same `ROLE_ADMIN` gating as the existing mutations:

```graphql
enum ComponentOperationType {
    ACTION
    TRIGGER
}

type ComponentOperationPolicy {
    componentName: String!
    operationType: ComponentOperationType!
    operationName: String!
}

componentOperationPolicies(componentName: String!): [ComponentOperationPolicy!]!   # disabled rows only

updateComponentOperationPolicy(
    componentName: String!
    operationType: ComponentOperationType!
    operationName: String!
    enabled: Boolean!
): Boolean!
```

### 5. Client (Component Policies page)

Each component row on the existing Component Visibility tab gets a chevron. Expanding:

1. Lazily fetches the component's actions and triggers (names + titles via the existing component
   definition query) and `componentOperationPolicies(componentName)`.
2. Renders "Actions" and "Triggers" subsections as the UNION of the definition's operations and the
   deny-list rows: one Switch per operation (on = enabled; a `componentOperationPolicies` row for it
   = off). The union is load-bearing — the definition query is itself policy-filtered (section 3),
   so a disabled operation vanishes from it; the deny-list row (labeled by `operationName`) keeps the
   operation visible and re-enablable in this UI. Toggling invalidates BOTH the operation-policies
   query and the cached component definition so a re-enabled operation reappears with its title once
   the provider cache TTL passes. (Amended 2026-08-04 after the whole-branch review caught the
   section 3/section 5 conflict; client-side union chosen over an unfiltered admin query.)
3. When the component's master toggle is off, the expanded operation list renders dimmed and
   non-interactive — component off overrides everything.
4. Toggling calls `updateComponentOperationPolicy` and invalidates the operation-policies query.
   Enforcement follows within the provider's 10s cache TTL, same as component toggles today.

Follows client conventions: sort-keys, `Icon`-suffixed lucide imports, `twMerge`, hook ordering.

### 6. Testing

- Ordinal-stability test for `OperationType`.
- Service tests: disable inserts a row, enable deletes it, both idempotent; key-set shape.
- Provider test: one cache entry answers both component-level and operation-level lookups; unknown
  operations default to visible.
- The existing `ActionDefinitionServiceImplVisibilityTest` / `TriggerDefinitionServiceImplVisibilityTest`
  / `ComponentDefinitionServiceImplVisibilityTest` gain per-operation cases: listing filters the
  operation out, execution throws `ACTION_DISABLED`/`TRIGGER_DISABLED`, and the SPI default methods
  fall back to component visibility (CE behavior pinned).
- GraphQL controller test for query + mutation + admin gating.
- Client Vitest for the expandable row (hoisted mocks per the repo convention).

## Rejected alternatives

- **JSON `disabled_operations` column on `component_policy`** — breaks the repo's relational style,
  loses per-row audit, and whole-column writes clobber concurrent toggles.
- **Generalizing `component_policy` to nullable operation columns** — unifies the model but rewrites
  the existing table (and its migration story) for no user-visible gain over a child table.
- **Allow-list mode** — safer for actions arriving in component updates, but a second mental model and
  more UI; deny-list matches the existing page and can be added later without schema changes (a mode
  column or marker row).
