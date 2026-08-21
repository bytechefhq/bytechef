# Component Policies — Component Visibility (Design)

**Date:** 2026-06-20
**Status:** Approved (pending spec review)
**Edition:** Enterprise (EE)

## Summary

Add a new **Component Policies** page under platform Settings with a single **Component
Visibility** tab. A tenant administrator can enable or disable each component. Disabled
components are:

1. **Hidden** from component listings (workflow editor picker, search), and
2. **Blocked at execution** — any workflow that tries to run a disabled component's action
   or trigger fails with a clear error, regardless of how the workflow definition was
   authored (UI, direct API edit, YAML import).

This is the next filtering layer on top of the two that already exist
(`bytechef.component.registry.exclude` and per-`PlatformType` `ComponentDefinitionFilter`).

The page is structured so future tabs — **Rules**, **Restrictions**, **Claims** (à la
Gumloop App Policies) — can be added later without rework. Those are out of scope here.

## Decisions

| Decision | Choice |
|---|---|
| Scope of a visibility setting | **Tenant-wide (global)** — one setting per component for the whole tenant |
| Enforcement | **Hide from listing AND block at execution** |
| Edition | **EE** (`server/ee`, `client/src/ee`, `@ConditionalOnEEVersion`, `@version ee`) |
| Save model | **Immediate per-toggle** mutation with optimistic update (no Save button) |
| Component set | **All registry components** (built-in + OpenAPI + custom/JDBC), minus static `registry.exclude` |
| Permission | **Admin-only** (page + query + mutation) |
| Atlas modules | **Not touched.** Enforcement added via SPI extension point in `platform-component`. |

## Goals / Non-Goals

**Goals**
- Tenant admins can toggle component visibility from Settings.
- Disabled components disappear from the editor and fail at runtime.
- Zero behavior change for CE (no SPI implementation present).
- Extensible page layout for future policy tabs.

**Non-Goals (future)**
- Per-workspace or per-environment scoping.
- Rules / Restrictions / Claims tabs.
- Migrating existing workflows off a disabled component, or surfacing "this workflow uses a
  disabled component" warnings (could be a later enhancement).

## Architecture

Three filter layers, only the third is new:

```
ServiceLoader / @AutoService discovery
  └─ Layer 1: registry.exclude (static app-properties)        [CE, unchanged]
       └─ Layer 2: ComponentDefinitionFilter per PlatformType  [CE, unchanged]
            └─ Layer 3: ComponentVisibilityProvider (DB-backed) [NEW, EE only]
```

### New SPI (CE)

`com.bytechef.platform.component.visibility.ComponentVisibilityProvider` lives in
`platform-component-api`:

```java
public interface ComponentVisibilityProvider {

    /** @return true if the component is visible/enabled for the current tenant. */
    boolean isVisible(String componentName);
}
```

- Injected as a `List<ComponentVisibilityProvider>` everywhere it is consumed.
- **CE ships zero implementations** → all components visible → behavior identical to today.
- **EE ships exactly one** implementation backed by the `component_policy` table.

A component is treated as visible iff **all** providers report it visible (`allMatch`). An
empty list ⇒ visible.

### Layer 3a — Listing filter (`ComponentDefinitionServiceImpl`, CE)

`ComponentDefinitionServiceImpl` gains the injected `List<ComponentVisibilityProvider>` and
applies an extra `.filter(...)` on the **listing/search** paths only:

- `getComponentDefinitions(Boolean…, PlatformType)` (editor picker)
- `getComponentDefinitions(String query, PlatformType)` (search)

It must **NOT** be applied to the no-arg `getComponentDefinitions()` used by execution/metadata
resolution — that path stays complete so component resolution and the admin page still work.

### Layer 3b — Execution guard (CE)

Guards are added at every method that actually runs component logic, immediately before the
perform/trigger function executes — covering all execution funnels, not just the workflow-node
path, so a disabled component cannot be reached via scripts or AI-agent tools either:

- `ActionDefinitionServiceImpl.executePerform(componentName, componentVersion, actionName, …)` — standard workflow action nodes (and "test action").
- `ActionDefinitionServiceImpl.executePerformForPolyglot(componentName, …)` — actions invoked from a `script` component (JS/Python/Ruby).
- `TriggerDefinitionServiceImpl.executeTrigger(componentName, componentVersion, triggerName, …)` — triggers.
- `ClusterElementDefinitionServiceImpl.executeTool(componentName, …)` (both the single- and multiple-connection overloads) — components run as AI-agent cluster-element tools.

Each consults `List<ComponentVisibilityProvider>`; if not visible, throw before execution.
(`ActionDefinitionServiceImpl` and `ClusterElementDefinitionServiceImpl` each get a `COMPONENT_DISABLED`
constant in their respective `*ErrorType`.)

**Why here and not in `ComponentDefinitionRegistry.getComponentDefinition()`:** that method is
the common resolution chokepoint but is also called for metadata, property/option resolution,
and by the admin page itself — guarding there would break the admin UI and editing of existing
workflows. These execution methods are the precise "run it" funnels and cover all authoring and
invocation paths (UI, direct API edit, YAML import, "test action", script components, and
AI-agent cluster-element tools).

**Error semantics:** throw with a clear, user-facing message, e.g.
*"Component 'slack' is disabled by an administrator and cannot be executed."* Proposed: a
`ConfigurationException` carrying a new `COMPONENT_DISABLED` error type (exact exception type /
error enum confirmed during implementation to match the surrounding convention —
`ExecutionException` vs `ConfigurationException`). It propagates through the existing
`AbstractTaskHandler` / `AbstractTriggerHandler` wrapping, so the run is marked failed with a
meaningful message rather than silently skipped.

**Tenant context:** guaranteed live at this seam — `executePerform` already resolves
tenant-scoped connections, so the DB-backed EE provider can read `TenantContext` safely. (To
re-verify during implementation given the worker-thread `ThreadLocal` caveat.)

## Data model (EE, tenant schema)

Table `component_policy`:

| column | type | notes |
|---|---|---|
| `component_name` | VARCHAR PK | registry component name, e.g. `slack` |
| `enabled` | BOOLEAN NOT NULL | the override |
| `created_by` | VARCHAR | audit |
| `created_date` | TIMESTAMP | audit |
| `last_modified_by` | VARCHAR | audit |
| `last_modified_date` | TIMESTAMP | audit |

- Only **explicitly-set** components get a row. **No row ⇒ enabled** (default-on).
- The EE provider materializes the disabled set = rows where `enabled = false`.
- Spring Data JDBC `ComponentPolicy` entity + `ComponentPolicyRepository` in the `*-service`
  module.
- Liquibase changelog placement: confirm the EE tenant-table changelog location during
  implementation (CE changelogs live in `liquibase-config`; EE tenant tables register through
  the EE changelog include).

## Backend modules (EE)

New module group `server/ee/libs/platform/platform-component-policy/`:

- `platform-component-policy-api` — `ComponentPolicyService` interface + `ComponentPolicy`
  domain model.
- `platform-component-policy-service` — JDBC entity, repository, `ComponentPolicyServiceImpl`,
  and `ComponentPolicyVisibilityProvider implements ComponentVisibilityProvider`
  (`@ConditionalOnEEVersion`).
- `platform-component-policy-graphql` — GraphQL resolver.

`ComponentPolicyService` (api) — proposed surface:

```java
public interface ComponentPolicyService {

    List<ComponentPolicy> getComponentPolicies();      // explicit overrides only

    boolean isEnabled(String componentName);           // default true when no row

    ComponentPolicy updateComponentPolicy(String componentName, boolean enabled); // upsert
}
```

Admin authorization enforced via `@PreAuthorize` on the service impl (matching how
audit-events / identity-providers settings are gated; exact authority token confirmed in
implementation).

### Optional caching

The EE provider may wrap the disabled-set lookup in a per-tenant Caffeine cache invalidated on
`updateComponentPolicy`. Kept minimal for MVP — include only if the per-listing/per-execution
query proves hot. The set is small, so a simple cache (or none) is acceptable.

## GraphQL contract (EE)

```graphql
type ComponentPolicy {
    name: String!
    title: String
    icon: String
    version: Int!
    enabled: Boolean!
}

type Query {
    componentPolicies: [ComponentPolicy!]!
}

type Mutation {
    updateComponentPolicy(name: String!, enabled: Boolean!): ComponentPolicy!
}
```

- `componentPolicies` merges the **unfiltered** registry component list (name, title, icon,
  version, from `ComponentDefinitionService`) with `component_policy` rows → each component plus
  its effective `enabled` flag (default `true`). Deliberately uses the unfiltered list so
  disabled components still appear with their toggle off.
- `updateComponentPolicy` upserts a row and returns the updated entry.

## Client (EE)

- **Route**: add to `platformSettingsRoutes` in `client/src/routes.tsx` — child route
  `href: 'component-policies'` + nav item **"Component Policies"**, EE-gated consistent with
  other `ee` settings entries.
- **Page**: `client/src/ee/pages/settings/platform/component-policies/ComponentPolicies.tsx`.
  A `<Tabs>` with a single trigger **"Component Visibility"**, structured so future
  Rules / Restrictions / Claims tabs slot in.
- **Tab body**: search box + scrollable list. Each row = component icon + title + name +
  a `<Switch>`. Toggling fires the `updateComponentPolicy` mutation with an **optimistic**
  React-Query cache update (no Save button). Global fetch interceptor handles error toasts;
  on error, revert the optimistic state.
- **GraphQL ops**: `client/src/graphql/platform/component-policies/*.graphql`; regenerate
  `client/src/shared/middleware/graphql.ts` via `npx graphql-codegen` (and add the schema path
  to `client/codegen.ts` if needed).
- Naming/style conventions: interfaces end in `I`/`Props`, sorted import destructures, `Icon`
  suffix on lucide imports, `twMerge` (not `cn`), hook ordering, etc.

## Testing

**Server**
- `ComponentPolicyServiceImpl` unit test (default-on, upsert, disabled-set).
- `ComponentDefinitionServiceImpl` test: a disabled component is absent from the listing/search
  paths but the no-arg `getComponentDefinitions()` still returns it.
- Execution-guard tests: `executePerform` / `executeTrigger` throw the expected error when the
  component is disabled, and run normally when enabled (mock provider).
- Repository `IntTest` (Testcontainers, `*IntTest` suffix).
- GraphQL resolver test (merge logic + mutation).
- All EE files carry the EE license header + `@version ee` (Spotless selects header by content).

**Client**
- Toggle-list component test incl. optimistic update + revert on error.
- `npm run check` (lint + typecheck + tests) before commit.

## Open items to resolve during implementation

- Exact admin authority constant for `@PreAuthorize`.
- EE Liquibase changelog location for the `component_policy` tenant table.
- Final exception type / error enum for the execution guard (`ConfigurationException` +
  `COMPONENT_DISABLED` vs `ExecutionException`).
- Whether to include the per-tenant cache in MVP.

## Future (explicitly out of scope)

- **Rules**, **Restrictions**, **Claims** tabs.
- Per-workspace / per-environment scoping.
- Editor warnings for workflows referencing a disabled component.
