# Per-account data tables and knowledge bases

**Status:** design only — nothing implemented
**Edition:** Enterprise (embedded); the owner model itself is Community (platform modules)
**Date:** 2026-08-27

## Problem

Neither store has any notion of who owns what below the tenant.

`data_table` carries **no owner column at all** — automation ownership lives in a
`workspace_data_table` relation that the runtime never consults. `knowledge_base` carries only
`environment` (`platform-knowledge-base-api/.../domain/KnowledgeBase.java:51`).

At runtime both enumerate the whole tenant. `DataTableServiceImpl.listTables`
(`platform-data-table-service/.../configuration/service/DataTableServiceImpl.java:233`) scans
`information_schema` for the `dt_<environmentId>_` prefix; `KnowledgeBaseOptionsUtils` calls the
**no-arg** `getKnowledgeBases()`. So a connected user offered either component gets a dropdown
listing every table and every knowledge base the vendor owns, and can read any of them.

The driving use case is an embedded AI assistant where **each customer account is a connected user**,
conversations are stored in data tables and each account has its own knowledge base. That is
unbuildable today: two accounts would share one `conversations` table with no way to separate them —
`DataTableFindRecordsAction` exposes only `TABLE`, `LIMIT` and `OFFSET` despite being described as
"Find records in a table with filters", and `DataTableRowService`'s seven methods carry no predicate
parameter at any layer.

**End state:** both stores carry an owner, the runtime scopes reads and writes to the calling
connected user, and the vendor manages the whole thing from the embedded admin console.

## What already exists (verified)

**The connected-user spine.** `connected_user_project` links `(external_id, environment)` to a real
automation `Project` via `ConnectedUserProjectWorkflowManager.getOrCreateConnectedUserProject`
(`.../facade/ConnectedUserProjectWorkflowManager.java:151`), with per-account workflows
(`connected_user_project_workflow`) and connections
(`connected_user_project_workflow_connection`).

**Two owner lookups, both written.** `IntegrationInstance.getConnectedUserId()`
(`embedded-configuration-api/.../domain/IntegrationInstance.java:33`) for integration workflows, and
`ConnectUserProjectRepository.findConnectedUserIdByProjectDeploymentId` for the automation bridge.

**Fail-closed authorization.** `ConnectedUserResourceMembershipResolver.resolve` switches on
resource type with `default -> Decision.NOT_APPLICABLE`, which `ResourceMembershipDecider` maps to
`Outcome.DENY` for a governed principal. `'DataTable'` and `'KnowledgeBase'` are therefore already
denied to connected users; adding cases **grants** a subset rather than closing a hole.

**A reserved-column mechanism.** `DataTableServiceImpl` already reserves `id`: rejected on create
(`:115`), rejected on rename in both directions (`:308-309`), filtered out of `listColumns` at two
sites (`:187`, `:252`). Reserved names here are unprefixed by convention.

**A mandatory-filter shim for vectors.** `KnowledgeBaseVectorStoreWrapper` stamps
`knowledge_base_id` into metadata on `add()` and AND-combines it onto every `similaritySearch` and
`delete(Filter.Expression)` — taking the caller's filter as a floor it can narrow but never widen.
It already carries a second optional dimension (`tagNames`) in exactly that shape.

**A shared-component precedent.** `ee/shared/components/variables/VariablesContent.tsx` is consumed
by both `ee/pages/settings/automation/variables` and `ee/pages/settings/embedded/variables`, with the
difference expressed as `VariableScopeType = {type: 'WORKSPACE'; workspaceId: number} | {type:
'EMBEDDED'}` (`client/src/shared/edition/variables/variablesApi.ts`).

**Chrome-stripped viewers.** `EmbeddableKnowledgeBase.tsx` and `EmbeddableDataTable.tsx` already
exist for the AI Hub resource panel — but both call automation GraphQL directly, so only the
presentation half of the extraction is done.

## Decisions

| | Resource owner | Row / document owner | Created by |
|---|---|---|---|
| `data_table` | nullable `owner_id` / `owner_type` | reserved `owner_id` / `owner_type` on every `dt_*` table | vendor only |
| `knowledge_base` | nullable `owner_id` / `owner_type` | documents inherit via `knowledge_base_id` | vendor only |

- NULL owner means vendor-owned and shared; a set owner means assigned to one account.
- **Accounts never create resources.** The vendor creates every table and knowledge base from the
  embedded admin console and assigns ownership. This is what keeps the physical table count bounded —
  a data table is real DDL (`dt_<environmentId>_<baseName>`), so self-service creation would mean one
  Postgres table per account per table-kind, with base-name mangling leaking into every UI surface
  and `listTables` slowing for everyone.
- **UI audience is the vendor admin**, in ByteChef's `/embedded` section. No connected-user-facing
  UI, no SDK surface, no new authentication path.
- **Enforcement is an explicit parameter, not ambient context** (see Runtime scoping).
- **Automation ownership is unchanged.** `workspace_data_table` and `workspace_knowledge_base`
  keep owning the workspace relation; the new `owner_id` / `owner_type` pair is a second,
  independent axis that is NULL for every automation-created resource.

## Data model

A shared `OwnerType` enum in `com.bytechef.platform.constant`, beside `PlatformType`, since both
platform stores need it and neither may depend on the other. One value to start: `CONNECTED_USER`.
Persisted as INT ordinal — append new values at the end, never reorder.

`data_table` and `knowledge_base` each gain nullable `owner_id BIGINT` and `owner_type INT`.

Both init changelogs ship in **`v0.31.4`**, so these are **new changesets**. Do not edit the init
files; customers have run them.

Every `dt_*` table gains reserved `owner_id` / `owner_type` columns. The four existing `id` guards
become reserved-name *set* membership, and the same `listColumns` filtering keeps the new columns out
of the grid, the generated row schema, CSV export and CSV import.

Knowledge base documents get **no** owner column — `KnowledgeBaseVectorStoreWrapper` already
partitions by `knowledge_base_id`, and a knowledge base has exactly one owner.

## Runtime scoping

One resolution function, explicit passing.

`resolveRowOwner(ActionContextAware)` is the single place identity is derived. Every
`DataTableRowService` method takes a non-optional `RowOwnerFilter` beside the `environmentId` it
already threads. The bypass is a named factory, `RowOwnerFilter.unrestricted()`, so it is greppable
rather than a nullable argument.

| Caller | `getJobPrincipalId()` is | Owner resolved via |
|---|---|---|
| Integration workflow | `integration_instance.id` | `IntegrationInstance.getConnectedUserId()` |
| Bridged automation workflow | `project_deployment.id` | `findConnectedUserIdByProjectDeploymentId` |
| Vendor automation workflow | a project deployment, no connected user | `RowOwnerFilter.unrestricted()` |

**Enforcement lives in `DataTableRowServiceImpl`, never in the actions.** MCP tools, AI Hub tools and
cluster elements reach the service without passing through a component; a filter in
`DataTableFindRecordsAction` would miss all of them.

**Visibility rule:** `owner_id = me OR owner_id IS NULL`. One rule covers both cases without
per-table configuration — a `conversations` table where the vendor writes no unowned rows gives each
account only its own, while a shared lookup table whose rows are all unowned is readable by everyone.

**Knowledge base gets the check it never had.** `KnowledgeBaseSearchAction.perform` reads
`inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID)` and wraps the vector store with it, with no
ownership validation — an account can name another account's knowledge base id today. The check goes
in `KnowledgeBaseService`, against the same resolved owner, before the store is ever wrapped.

`findRecords` gains a real filter, since it is the mechanism the row scoping rests on rather than a
convenience. Its operator grammar can follow `ContextStoreQueryFilter` (`EQ, NEQ, IN, CONTAINS,
STARTS_WITH, GT, GTE, LT, LTE, BETWEEN`).

## Authorization and surfaces

Three callers, three gates:

1. **Connected user at runtime** — covered entirely by the service-layer filter. The filter is the
   authorization; the resolver is not involved.
2. **Connected user via the API** — `'DataTable'` and `'KnowledgeBase'` cases in
   `ConnectedUserResourceMembershipResolver`. Denied by default until added, so this can land
   incrementally with no window of exposure.
3. **Vendor admin in the console** — a tenant-admin session, gated `isTenantAdmin()` on the embedded
   **API facade**, not the controller and not the shared facade.

**Module shape is thinner than automation's.** `automation-data-table` needs `-api` + `-service` +
`-graphql` because ownership lives in a relation table that must be maintained. Embedded ownership is
a column on the platform entity, so the embedded side needs only a `-graphql` module per store: a
controller over the existing platform service exposing `dataTables(environmentId, ownerId)` and
`knowledgeBases(environmentId, ownerId)`. No relation table, no service layer, no remote clients.

## Component palette

`IntegrationComponentDefinitionFilter.COMPONENT_NAMES` gains `dataTable` and `knowledgeBase`, keeping
per-account data work in bridged automation projects while integration workflows stay the vendor's
connector surface.

A **second** `ComponentDefinitionFilter` bean for the same `PlatformType` will not work —
`ComponentDefinitionServiceImpl` selects with `.filter(f -> f.supports(platformType)).findFirst()`,
so an added bean is silently ignored or wins by bean ordering. Modify the existing filter in place.

The filter is listing-only: `getComponentDefinition(name, version)` applies
`componentVisibilityProviders` but **not** the platform filter, so already-placed nodes still render
and still execute. Enforcement, if ever wanted, is a `ComponentVisibilityProvider`.

## Admin UI and shared extraction

The embedded console is the **same page** as automation, plus an owner filter and an
owner-assignment control. Vendor-creates-everything means the operation set is identical.

**Shared components go in `shared/components/`, not `ee/shared/components/`.** The variables
precedent sits under `ee/shared` because both its consumers are EE; the automation DT/KB pages are CE
(`client/src/pages/automation/datatables`), and no CE page imports from `@/ee/` anywhere in the client.

**No registry.** `shared/edition/variables/variablesApi.ts` uses register/get because CE needs a
no-op fallback for a feature it lacks. That constraint is absent here — the component itself is CE
and the generated hooks all land in the shared `shared/middleware/graphql.ts`. A scope prop suffices:

```ts
export type DataTableScopeType =
    | {type: 'WORKSPACE'; workspaceId: number}
    | {type: 'EMBEDDED'; ownerId?: number};
```

`ownerId` absent means "all owners" — the vendor's console-wide view.

Work: move the presentation components under `shared/components/{data-tables,knowledge-bases}`,
thread the scope union through `useDataTable` / `useKnowledgeBases`, and let the two page shells
supply chrome and scope. `Embeddable*` collapses into the same components once they are
scope-parameterized.

## Migration

Backfilling existing `dt_*` tables is the sharpest implementation risk. The table set is discovered
at runtime from `information_schema`, so it cannot be a static changelog — it needs a Java migration
that enumerates the `dt_<environmentId>_` prefix and issues `ALTER TABLE` per table, idempotently,
per tenant schema.

Existing rows and existing resources get NULL owners, i.e. vendor-owned. That is the correct default:
nothing becomes invisible to the vendor, and nothing becomes visible to an account that could not see
it before.

The demo's `listConversations` / `getConversation` / `ingestWhatsApp` integration workflows must
migrate to the bridge before the palette exclusion lands, since the filter would prevent rebuilding
them in place.

## Testing

- `DataTableRowServiceImpl` unit tests per method: owner stamped on insert; read/update/delete
  filtered; `unrestricted()` sees everything.
- An `IntTest` proving two connected users cannot see each other's rows in one shared table, driven
  through the service rather than the component, so the MCP/agent path is covered by construction.
- A test that a connected user naming another account's `knowledgeBaseId` is rejected.
- A test that an editor-environment run fails closed rather than falling through to `unrestricted()`.
- Reserved-column tests: create, rename-to, rename-from, `listColumns`, CSV round-trip.
- Client tests for both page shells against one shared component, per scope variant.

## Traps

- **`owner_id` is never `jobPrincipalId`.** That id varies per integration and per project for the
  same account. Using it would shard one account's rows across its own integrations — which reads as
  data loss rather than a leak, and so survives testing longer.
- **`isEditorEnvironment()` must fail closed.** A bridged builder's Test button runs with no
  persisted job and no job principal. A naive `resolveRowOwner` returns nothing and falls through to
  `unrestricted()`, turning Test into a full table read. Editor runs resolve the owner from the
  security principal, or refuse.
- **`KnowledgeBaseVectorStoreWrapper.delete(List<String> idList)` is unfiltered.** Harmless while
  `knowledge_base_id` is the only dimension; a hole the moment ownership matters.
- **One NULL-owner row in an account-partitioned table is visible to every account.** The cost of the
  single visibility rule. The stricter alternative is `= me` plus a per-table shared flag.
- **`DataTableUtils.getTableOptions` hardcodes `DEVELOPMENT`** and the KB picker filters by nothing.
  Both must become owner-aware, or the dropdown keeps leaking what the runtime now protects.

## Out of scope

- Connected-user-facing UI and any SDK surface.
- Self-service creation of tables or knowledge bases by accounts.
- Execution-time component blocking (`ComponentVisibilityProvider`).
- Per-document ownership within a knowledge base.
- Distributed EE: knowledge base has no remote client and is `server-app`-only today.
