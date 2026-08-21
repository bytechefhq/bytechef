# Context Store multi-environment — parent `ContextStore` entity

**Status**: Draft
**Date**: 2026-05-13
**Predecessors**:
- [2026-05-08 Context Store baseline](2026-05-08-context-store-design.md)
- [2026-05-12 Phase 16 (ClickHouse backend)](2026-05-12-context-store-phase16-design.md)
- [2026-05-12 Phase 17b (incremental sync auto-wiring)](2026-05-12-context-store-phase17b-design.md)
- [2026-05-12 ClickHouse mutable schema](2026-05-12-context-store-clickhouse-mutable-schema-design.md)

## Goal

Let users configure a Context Store **independently per environment** (`DEVELOPMENT`, `STAGING`, `PRODUCTION`). After this work, a user can have a "Customer mirror" store in DEV that reads from a dev Postgres connection on a fast cadence, and a parallel "Customer mirror" store in PROD reading from the prod connection on a slower cadence — without any new deployment, promotion, or version-bumping infrastructure.

## Why this shape

We considered two designs in the design conversation:

- **Promotion-based** — add env-awareness to the hidden project (`__CONTEXT_STORE__<id>`), spin up three `ProjectDeployment` rows per env, introduce a `promoteContextStoreSource(fromEnv, toEnv)` mutation, dimension records by env. Mirrors how user-facing project deploys work.
- **Env-stamped parent entity (chosen)** — introduce a `ContextStore` table with an `environment` column; sources, entities, records, ClickHouse projection tables all hang off `context_store_id` and inherit env transitively. No promotion; users create N stores for N envs. Mirrors `KnowledgeBase` ([KnowledgeBase.java:50-51](server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/domain/KnowledgeBase.java:50)).

The env-stamped parent wins on three fronts:

1. **Precedent.** Knowledge Base already does exactly this. Code review will be friction-free because reviewers have seen the pattern.
2. **No `ProjectDeploymentFacade` refactor.** Today's "hidden project on DRAFT version via `projectDeploymentService` bypass" trick keeps working unchanged — the hidden project is now correctly understood as a *cron container*, not the DEV environment. (`Environment.DEVELOPMENT` becomes a load-bearing constant whose name is a misnomer; this is documented, not fixed.)
3. **No env-dimensioning of records.** Each `ContextStore` is its own root, so records, indexes, and ClickHouse tables are already naturally segregated by env via `context_store_id`. No `environment` column on `context_store_record`, no `environment` parameter through `ContextStoreQueryService`'s signatures.

The cost we accept: no diff/promote UX. A user wanting "the same store in three envs" creates three rows and configures each independently. Config drift between envs is the user's responsibility, same as Knowledge Base today.

## Non-goals

- **No migration of existing rows.** Per [decision in design conversation](memory:project_workspace_relation_table_refactor.md), tables will be recreated on this branch. The init Liquibase changesets are edited in-place; no follow-up migration files.
- **No "deploy" or "publish" concept on Context Store.** A Context Store *exists*; it is not deployed. Its sync workflow is the only thing scheduled, and it continues to ride the hidden project's `DEVELOPMENT` deployment.
- **No promotion mutation** (`promoteContextStore`, `copyToEnvironment`, etc.). Defer to a future spec if users ask for it.
- **No shared `HiddenProjectFacade` extraction.** Both KB and Context Store now have the same hidden-project pattern, but extracting the abstraction is a separate spec — out of scope here.

## Architecture

### Data model

New parent table `context_store`:

```
context_store
  id                BIGINT PK
  name              VARCHAR(256)  NOT NULL
  description       TEXT
  environment       INT           NOT NULL   -- Environment ordinal
  created_by        ...
  created_date      ...
  last_modified_by  ...
  last_modified_date ...
  version           BIGINT        NOT NULL
```

No unique constraint on `name` at table level — uniqueness within `(workspace_id, environment)` is enforced at the application layer in `WorkspaceContextStoreFacade.create`, the same way Knowledge Base handles it. (Cross-table unique with the workspace relation row can't be expressed in SQL; this is the [known limitation](memory:project_workspace_relation_table_refactor.md).)

Edits to `context_store_source` (init changeset):

- **Add** `context_store_id BIGINT NOT NULL` with FK to `context_store.id` `ON DELETE CASCADE`.
- **Drop** `uk_context_store_source_name` (name no longer globally unique).
- **Add** `uk_context_store_source_name (context_store_id, name)` (name unique within a store).

No edits to `context_store_entity`, `context_store_record`, or `context_store_record_index` — they hang off `source_id`, which already pins the env transitively via the new parent.

### Relation tables (add alongside, not replace)

KB precedent ([WorkspaceKnowledgeBase.java](server/libs/automation/automation-knowledge-base/automation-knowledge-base-api/src/main/java/com/bytechef/automation/knowledgebase/domain/WorkspaceKnowledgeBase.java), [WorkspaceKnowledgeBaseSource.java](server/libs/automation/automation-knowledge-base/automation-knowledge-base-api/src/main/java/com/bytechef/automation/knowledgebase/domain/WorkspaceKnowledgeBaseSource.java)) has **both** parent and source relation tables coexist — confirmed by reading the KB init Liquibase and domain classes. We mirror that:

- Keep existing `workspace_context_store_source` (workspace ↔ source) unchanged. Direct workspace→source lookups stay one join away.
- **Add** new `workspace_context_store` (workspace ↔ store):

```
workspace_context_store
  id                BIGINT PK
  context_store_id  BIGINT NOT NULL  -- FK to context_store.id
  workspace_id      BIGINT NOT NULL
  ...
  UNIQUE (workspace_id, context_store_id)
```

Source workspace membership transits *either* way (direct relation or via `context_store_id → workspace_context_store`); the redundancy is the KB pattern and keeps existing direct-lookup code paths working without joining through the parent.

This follows the [workspace_id placement rule](memory:feedback_workspace_id_placement_rule.md): `ContextStore` is in the automation package, but workspace ownership lives in a relation table (not a column on `context_store`).

### ClickHouse table naming

Today: `ClickHouseTableNameSanitizer.tableNameFor(workspaceId, sourceId, entityName)`.
After: `tableNameFor(workspaceId, contextStoreId, sourceId, entityName)` — adds one more disambiguator.

The `workspaceId` part stays because ClickHouse tables are workspace-namespaced for tenancy isolation, not for env separation. The new `contextStoreId` segment is what gives us env isolation.

Existing `clickhouse_table_name` column on `context_store_entity` keeps caching the resolved name; the table generator just emits a longer name now.

### Java surface

**New:**

- `com.bytechef.ee.platform.contextstore.domain.ContextStore` — record-shaped (id, name, description, environment, audit, version).
- `com.bytechef.ee.platform.contextstore.repository.ContextStoreRepository`.
- `com.bytechef.ee.platform.contextstore.service.ContextStoreService` (+ Impl in `-service` module per the [repository module placement rule](memory:feedback_repository_module_placement.md)).
- `com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreFacade` — `create(workspaceId, CreateContextStoreInput)`, `update`, `delete`, `list(workspaceId, environment)`, `get(workspaceId, id)`.
- `com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreService` + relation entity `WorkspaceContextStore`.

**Modified:**

- `ContextStoreSource` — adds `contextStoreId` field (`AggregateReference<ContextStore, Long>`). Sync state (`lastSyncRunAt`, `lastSyncJobExecutionId`, `status`, `enabled`) stays on the source row — it's per-source, not per-store, because each store has its own sources.
- `WorkspaceContextStoreSourceFacade.create(input)` now requires `contextStoreId` in the input; reads env transitively from the parent for any downstream concerns (cron container choice stays `DEVELOPMENT` regardless).
- `ContextStoreQueryService.search(...)` adds a `contextStoreId` parameter and scopes the query to that store's sources. Env is implicit.
- `ContextStoreWorkflowGenerator` — workflow metadata gains `metadata.contextStoreId` alongside the existing `metadata.contextStoreSourceId`, so any listener resolving env from a workflow can do `source → store → environment`.

**Unchanged:**

- Hidden project (`__CONTEXT_STORE__<id>`).
- `findOrCreateProjectDeployment(projectId, projectVersion)` still pins `Environment.DEVELOPMENT`.
- `ProjectDeploymentWorkflow` rows — one per source as today.
- Sync-job listener (`ContextStoreSyncJobListener`) — still writes back to `ContextStoreSource`.

### GraphQL surface

New:

```graphql
input CreateContextStoreInput {
  name: String!
  description: String
  environment: Environment!     # SCREAMING_SNAKE_CASE per CLAUDE.md
}

input UpdateContextStoreInput {
  id: ID!
  name: String
  description: String
  version: Int!
}

extend type Mutation {
  createContextStore(workspaceId: ID!, input: CreateContextStoreInput!): ContextStore!
  updateContextStore(workspaceId: ID!, input: UpdateContextStoreInput!): ContextStore!
  deleteContextStore(workspaceId: ID!, id: ID!): Boolean!
}

extend type Query {
  contextStores(workspaceId: ID!, environment: Environment): [ContextStore!]!
  contextStore(workspaceId: ID!, id: ID!): ContextStore
}
```

Modified `CreateContextStoreSourceInput` and `UpdateContextStoreSourceInput`:
- Add required `contextStoreId: ID!` on create. (Existing sources on this branch will be re-seeded.)
- List queries take `contextStoreId` instead of `workspaceId` as the primary scoping arg.

### `contextStore.search` cluster element

Today: takes a `sourceId` at workflow design time; env is implicit via `sourceId → contextStoreId → environment`.

**Deferred to a follow-up spec:** a store-level search API (`ContextStoreQueryService.search(contextStoreId, ...)`) that queries across all sources in a given store, plus a `contextStoreId`-taking variant of the cluster element. Both are non-trivial — the cluster element needs a property picker UI for stores, the query service needs a join through all sources of the store, and the cluster element runtime needs to resolve env from execution context if the user wants "this workflow's env" semantics. None of that gates multi-env support today (a workflow can already target a specific source row pinned to a specific env via the parent).

What lands now: nothing in this layer. Existing source-scoped search continues to work; env isolation is correct by construction.

## Commit plan

1. **Schema** — edit `00000000000001_platform_context_store_init.xml` in place: add `context_store` table + alter `context_store_source` to include `context_store_id` FK and the per-store name uniqueness. Add a new `20260513000001_automation_context_store_added_table_workspace_context_store.xml` for the parent relation table; **leave `workspace_context_store_source` untouched** (matches KB precedent of two parallel relations). No follow-up migration files; the [Liquibase init-edit lesson](memory:project_workspace_relation_table_refactor.md) applies — rename indexes to their final names in init.
2. **Domain + service + repo** — new `ContextStore`, `ContextStoreService`, `ContextStoreRepository`; `WorkspaceContextStore` + `WorkspaceContextStoreService` + relation repo.
3. **Source FK + cluster-element env-resolution** — `ContextStoreSource.contextStoreId` field, `ContextStoreQueryService.search(contextStoreId, ...)`, `ClickHouseTableNameSanitizer` adds `contextStoreId` segment. `EnumOrdinalStabilityTest` updated if any ordinals shift (`Environment` is in `platform-configuration`, so ordinals are already pinned — just confirm).
4. **Facade + GraphQL** — new `WorkspaceContextStoreFacade`, GraphQL mutations + queries. `WorkspaceContextStoreSourceFacade.create` requires `contextStoreId` in input; downstream wiring (workflow generator, hidden project lookup) takes `contextStoreId` from the source row.
5. **Workflow metadata + listener** — `ContextStoreWorkflowGenerator` emits `metadata.contextStoreId`; `ContextStoreSyncJobListener` does no env lookup today (it writes back to the source row directly) but the metadata is in place for any future per-env routing.
6. **UI** — workspace list page gains a Context Stores tab (or sidebar entry) with env filter dropdown, mirroring the KB list. Source list scopes under a chosen store. Out of scope for this spec to detail; covered in a follow-up frontend spec.

## Tests

- Update `EnumOrdinalStabilityTest` if any enum touched. (None expected — `Environment` lives elsewhere.)
- New `WorkspaceContextStoreFacadeImplTest` — create/update/delete, name uniqueness within `(workspace, environment)`.
- New `ContextStoreServiceIntTest` — CRUD + relation table semantics.
- Update `ContextStoreSyncE2EIntTest` — seed a `ContextStore` row first, then a source under it; verify ClickHouse table name includes `contextStoreId`.
- Update `ContextStoreQueryServiceIntTest` — search now takes `contextStoreId`; verify env scoping is implicit (two stores in same workspace different envs return disjoint results).

## Open questions

1. **Store-level cadence default?** Each source has its own cadence today. Should the parent `ContextStore` carry a workspace-default cadence that new sources inherit unless overridden? Not in v1 — defer.
2. **Store-level "enabled" toggle?** Disabling a store could fan out and pause all its sources. Not in v1 — defer; users can disable sources individually.
3. **`contextStore.search` env-auto-select?** Should the cluster element have a "use the workflow's current env" mode that resolves `contextStoreId` from a (workspace, name, env) lookup at runtime instead of a hardcoded ID? This is the templating concern called out above — defer to a follow-up if users ask.
4. **Naming the misnomer.** `findOrCreateProjectDeployment(...) → Environment.DEVELOPMENT` is now provably mislabeled. Worth a one-line code comment update calling out that the constant is a cron-container marker, not a real env. Tiny; can land with this work or its own commit.

## Frontend follow-ups

Captured here so they don't get lost — to be picked up in a separate frontend pass:

- **Tags on Context Store — done.** Backend slice (`context_store_tag` join table, `ContextStoreTag` entity, `ContextStore.tagIds`/`tags`, GraphQL type fields + Create/Update inputs, facade wiring) landed alongside all four UI sub-items: (a) tag picker in `ContextStoreFormDialog` driven by the new `contextStoreTags(workspaceId)` GraphQL query (commit `732456d42e9`); (b) tag column in `ContextStores.tsx` via shared `TagList` (commit `732456d42e9`); (c) tag filter group in the Context Sources sidebar (commit `0557e4f4d7e`); (d) `updateContextStoreTags(id, tags: [TagInput!]!)` mutation for inline editing without round-tripping the whole store (commit `a42c2581e4a`).
- **Refresh Now → INTERNAL_ERROR — done.** Refresh now surfaces the real underlying message (e.g. "Workflow input 'baseId' is required") instead of the sanitized `INTERNAL_ERROR for <uuid>` toast. Implementation wraps unexpected runtime failures from `principalJobFacade.createJob` in `ConfigurationException` so `GlobalDataFetcherExceptionResolver` forwards the message; the missing-workflow branch becomes `ConfigurationException` too. The companion refuse-to-enable validation gates the create path: every entity's required source-cluster-element parameters are checked before any DB write, with `ConfigurationException(MISSING_REQUIRED_PARAMETER)` returning a precise per-entity field list. Both shipped in commit `19282c48a73`.
- **Edit Entity dynamic field options — done.** `EditContextStoreEntityDialog` now mirrors `AddContextStoreEntityDialog`'s `useClusterElementFieldsQuery` wiring and exposes `IndexedFieldsEditor` with availableFields-driven dropdowns. Source-component info is threaded through `ContextStoreEntitiesList`.

## Post-spec extensions

Work that fell out of the original spec but shipped on the same branch:

- **Store-level `contextStore.searchByStore` action.** A new component action that takes `(contextStoreId, entityName)` and fans the structured query across every source in the store, returning a merged result. Registered as both a regular action and a TOOLS cluster element. Cursor pagination across sources is intentionally not supported in v1 — each per-source result honours filter/sort and the union is capped at the shared `limit`. Heavier sorted-pagination needs should keep using the single-source `ContextStoreSearchAction`. (Commit `2d4c734f260`.) Resolves the **Phase 4** deferral at lines 153–157.
- **Name → id lookup primitive.** New `contextStoreIdByName(workspaceId, name, environmentId)` GraphQL query backed by `WorkspaceContextStoreFacade.findContextStoreIdByName`. Lets env-aware workflows resolve a store by stable name across DEVELOPMENT / STAGING / PRODUCTION instead of hardcoding ids. (Commit `0a40f758f41`.)
- **Phase 5: runtime env auto-resolve in `searchByStore`.** Open Question #3 (line 182) shipped via the `JobParameterContributor` pattern rather than threading a new method onto `ActionContextAware` (which lives in the platform layer and would require all 48 `createActionContext` call-sites to source a workspaceId). New `ContextStoreWorkflowContextJobParameterContributor` injects `contextStore.workspaceId` into every trigger-spawned automation job's `__jobParameters` metadata; the action reads it alongside the already-wired `ActionContextAware.getEnvironmentId()` at perform-time. The `contextStoreId` parameter on `searchByStore` becomes optional and a new `contextStoreName` parameter triggers an env-aware lookup via a new platform SPI `ContextStoreNameLookupService` (impl `AutomationContextStoreNameLookupService` bridges to `WorkspaceContextStoreFacade`). Workflow promotion across envs now works without hand-edits — same workflow JSON resolves to the env-specific store id at run time. Embedded principals and ad-hoc (non-trigger) invocations error with an actionable message rather than silently picking the wrong store.
- **Master enable switch.** New `bytechef.context-store.enabled` flag in `ApplicationProperties.ContextStore.enabled`, exposed at `/actuator/info` and consumed by the client nav gate in `App.tsx`. Mirrors the `Ai.Hub` / `Ai.Gateway` pattern: a single property toggles every Context Store bean (GraphQL controllers, JDBC services, ClickHouse cascade, sync-job listeners, the contextStore component handler, and the @EnableJdbcRepositories config). When off, the module stays inert on the classpath. (Commit `a799b804dcb`.)
- **`findOrCreateProjectDeployment` misnomer comment.** `Environment.DEVELOPMENT` here is a cron-container marker, not the source's effective env — the real env is stamped on the parent ContextStore and propagates via `contextStoreId` on every synced row. Comment expanded so a future cleanup doesn't try to "fix" it by introducing per-env deployments. (Commit `0a40f758f41`.) Resolves Open Question #4 (line 183).
