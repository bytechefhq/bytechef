# Per-tenant storage limits for DataTables & KnowledgeBase — Design

**Date:** 2026-07-05
**Status:** Approved (design)

## Motivation

n8n caps the total size of all data tables in an instance (default 50 MB, override via
`N8N_DATA_TABLES_MAX_SIZE_BYTES`) and warns the user when storage approaches 80% of the limit.

ByteChef has two comparable storage-backed features — **DataTables** and **KnowledgeBase** — with
no size accounting or limits today. This design adds n8n-style storage limits (warn at 80%,
hard-block at 100%) to both, adapted to ByteChef's multi-tenant model.

## Scope & behavior

- **Scope: per tenant.** ByteChef is schema-per-tenant, so a limit that sums everything in the
  current tenant schema naturally covers all workspaces and environments for that tenant. In a
  single-tenant self-hosted deployment this behaves exactly like n8n's instance-wide cap; in
  multi-tenant cloud each customer/tenant gets its own budget.
- **Two independent limits.** DataTables (Postgres tables) and KnowledgeBase (object-storage files)
  are different backends and get separate, independently tunable caps. No combined budget.
- **Warn at 80%, hard-block at 100%.** A warning banner appears when tenant usage crosses 80%. New
  writes (DataTable row insert / CSV import, KnowledgeBase document upload) are rejected once the
  tenant is at or over the limit. Existing data is never modified or deleted.
- **Defaults on**, matching n8n's out-of-box posture:
  - DataTables: **50 MB** (`52_428_800` bytes)
  - KnowledgeBase: **1 GB** (`1_073_741_824` bytes)
- `0` = unlimited (enforcement disabled for that feature).

## Configuration (`ApplicationProperties`)

`ApplicationProperties` uses strict binding (`ignoreUnknownFields = false`), so every new
`bytechef.*` property must be a declared field. Both properties are therefore env-var overridable
for free (e.g. `BYTECHEF_DATA_TABLE_MAX_SIZE_BYTES`).

- **DataTables** — new top-level group:
  - `bytechef.data-table.max-size-bytes` (`long`, default `52_428_800`)
- **KnowledgeBase** — add to the existing `Ai.KnowledgeBase` group (KB config already lives under
  `bytechef.ai.knowledge-base.*`):
  - `bytechef.ai.knowledge-base.max-size-bytes` (`long`, default `1_073_741_824`)

The **80% warning threshold is a fixed shared constant** (`STORAGE_WARNING_THRESHOLD = 0.8`),
matching n8n which hardcodes it. This keeps the config surface minimal; the client simply checks
`percentage >= 80`.

## Size accounting

No running counters / accounting tables (they drift on failed deletes, cascade deletes, direct
SQL). Both features compute usage from a single cheap query at check time.

### DataTables

Each data table is a physical Postgres table named `dt_<environmentId>_<baseName>` in the tenant
schema. Usage is the sum of physical relation sizes:

```sql
SELECT COALESCE(
  SUM(pg_total_relation_size((current_schema() || '.' || tablename)::regclass)), 0)
FROM pg_tables
WHERE schemaname = current_schema()
  AND tablename LIKE 'dt\_%';
```

`current_schema()` is set to the tenant schema by `BaseDataSource.setSearchPath()` on every
connection, so this is automatically tenant-scoped with no explicit tenant id. Note: this measures
*physical* pages (8 KB minimum per table + bloat), so many small tables over-report vs. logical
row bytes. Accepted — it's the cheap, accurate-enough measure and matches "total storage used."

### KnowledgeBase

Add a nullable `document_size BIGINT` column to `knowledge_base_document`, written at upload time
from the multipart / content length. Usage:

```sql
SELECT COALESCE(SUM(document_size), 0) FROM knowledge_base_document;
```

Tenant-scoped via schema-per-tenant. **Lazy backfill:** existing rows stay `NULL` and count as 0
until re-uploaded; tenant usage under-reports for pre-existing documents until then. Accepted
tradeoff to keep the migration fast and avoid a HEAD-per-file sweep at upgrade.

No short-TTL cache in v1 — both queries are single, cheap (catalog lookup / indexed SUM). Revisit
if profiling shows a hotspot.

## Server components

### `StorageUsage` (shared shape)

Record returned to callers and GraphQL:

```
record StorageUsage(long usedBytes, long limitBytes, double percentage, boolean unlimited)
```

`percentage` is `0` when `unlimited` (limit `0`); otherwise `usedBytes * 100.0 / limitBytes`.

### `DataTableStorageService` (in `platform-data-table-service`)

- `StorageUsage getUsage()` — runs the catalog query, reads the limit from `ApplicationProperties`.
- `void checkWithinLimit(long incomingBytes)` — throws `DataTableStorageLimitExceededException`
  when the tenant is at/over the limit. No-op when limit is `0` (unlimited).

Enforcement:
- `DataTableRowServiceImpl.insertRow()` — call `checkWithinLimit(...)` before inserting; blocks
  when tenant usage is already `>= limit`. (A single row/import may cross the line before the next
  write is blocked — `pg_total_relation_size` page allocation makes exact per-row pre-checks
  unreliable, so we block on "already at/over" rather than the crossing row itself.)
- `DataTableRowServiceImpl.importCsv()` — pre-check current usage plus the estimated CSV byte
  length (the CSV string is already in hand) before importing.
- `DataTableRowServiceImpl.updateRow()` — same "already at/over" block as `insertRow` (guards
  against growing storage by updating existing rows with larger values while over the cap).

### `KnowledgeBaseStorageService` (in `platform-knowledge-base-service`)

- `StorageUsage getUsage()` — runs the `SUM(document_size)` query, reads the limit.
- `void checkWithinLimit(long incomingFileBytes)` — throws
  `KnowledgeBaseStorageLimitExceededException` when `used + incomingFileBytes > limit`. No-op when
  limit is `0`.

Enforcement:
- `KnowledgeBaseDocumentFacadeImpl.createKnowledgeBaseDocument()` — the incoming file size is known
  up front, so check `used + incoming > limit` **before** `storeDocument()`; then persist
  `document_size` on the `KnowledgeBaseDocument` entity.

## GraphQL (for the banners)

- automation-data-table schema: `dataTableStorageUsage: StorageUsage`, resolved in the data-table
  GraphQL controller, delegating through the workspace facade to `DataTableStorageService`.
- automation-knowledge-base schema: `knowledgeBaseStorageUsage: StorageUsage`, same pattern.
- New client `.graphql` operation files under `client/src/graphql/automation/{datatable,knowledge-base}/`;
  run `npx graphql-codegen` to generate `useDataTableStorageUsageQuery` /
  `useKnowledgeBaseStorageUsageQuery` hooks.

## Client

- **`alert.tsx`**: add a `warning` variant (amber) — only `default`/`destructive`/`success` exist,
  and 80% is a warning, not an error.
- **`DataTables.tsx`**: render the warning banner after `<PageLoader>` when `percentage >= 80`,
  e.g. *"Data table storage is at 87% (44 MB of 50 MB). Delete rows or increase the limit."*
- **`KnowledgeBases.tsx`**: same banner, above the existing "no embedding model" alert.
- **Blocked writes**:
  - DataTable insert / CSV import are GraphQL → the thrown exception auto-surfaces as an error
    toast via `useFetchInterceptor`.
  - KB upload is REST (`POST .../knowledge-bases/{id}/documents`) → return 4xx with the message;
    the existing `useUploadKnowledgeBaseDocumentDialog` hook already renders per-file error status.

## Testing

- **Server unit tests** for both storage services: usage math, threshold/boundary behavior,
  `0 = unlimited`, over-limit throws. Tenant-scoping verified via schema-scoped queries in an
  `IntTest` where feasible.
- **Enforcement tests**: DataTable insert/import blocked when over limit; KB upload blocked when
  `used + incoming > limit`; `document_size` persisted on upload.
- **Client tests**: banner renders at `>= 80%`, hidden below; `alert.tsx` warning variant.

## Out of scope (v1)

- Running-counter accounting / accounting table.
- Cross-tenant admin/usage dashboard.
- Per-workspace or per-environment sub-quotas.
- Retroactive eager backfill of `document_size` for existing KB documents.
- Configurable warning threshold (fixed at 80%).
- Size limits on DataTable schema growth from `ADD COLUMN`.
- Combined DataTable + KnowledgeBase budget.
