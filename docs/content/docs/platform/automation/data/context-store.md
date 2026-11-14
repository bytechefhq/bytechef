---
title: Context Store
description: Connect external systems as queryable record stores for AI agents and workflows
ee: true
comingSoon: true
---

The Context Store keeps a synchronized copy of records from external systems (CRMs, ticket systems, knowledge bases) inside ByteChef. AI agents and workflows search and read these records without calling the source system each time.

Each **Context Store source** binds an external connector (for example HubSpot, Salesforce, or a JSON file) to a sync cadence and a set of **entities**. An entity is a named record shape with declared indexed fields. The sync engine runs on the configured cadence, writes new and changed records into the store, and marks records that no longer exist upstream as deleted (tombstones).

Open **Context Store** from the automation sidebar (in the **Deployments** group) to see your sources. Click **Add Source** to bind a connector and define an entity.

<!-- TODO screenshot: Context Store page - list of sources with their sync status and the per-row actions menu (Refresh now / Disable / Delete) -->
<!-- TODO screenshot: Add Context Source dialog on step 1 (Connection) - the Context Store select, Source Name field, and the component grid picker -->

## Add a source

**Add Source** opens a four-step wizard: **Connection**, **Record shape**, **Cadence**, and **Review**. Use **Next** and **Back** to move between steps. **Create Source** on the last step creates the source and starts its first sync.

1. **Connection** - Select the target **Context Store**, name the source, then select a component from the grid and one of its data-stream-compatible connections. Only components with such a connection appear. If the grid is empty, create a connection on the **Connections** page first.
2. **Record shape** - Name the **Entity**, add an optional description, and fill in any source-specific configuration. Select the **ID Field** (the upstream record's unique key) and add **Indexed Fields**: the typed columns to filter and search on.
3. **Cadence** - Set the **Sync Cadence** (how often new records are pulled), an optional less frequent **Full Re-sync Cadence** that detects upstream deletions, and the **Tombstone Strategy** that decides how deleted records are retired.
4. **Review** - Confirm the store, name, connection, entity, cadence, and tombstone strategy, then click **Create Source**.

<!-- TODO screenshot: source detail dialog - the entity's indexed fields, cadence, and last-sync details -->

Each source row on the Context Store page has an actions menu with **Refresh now** (start a sync immediately), **Disable** or **Enable**, and **Delete**.

---

## Records backend

The records backend is a deployment-wide setting chosen by the operator. The Add Source wizard has no backend selector. Every source stores its records in the active backend:

- **Postgres** (default) - Records are stored in the ByteChef Postgres database. Supports filter expressions and semantic search. Recommended for most workloads.
- **ClickHouse** (optional) - Records are stored in a separate ClickHouse server, in one typed table per entity. Intended for very large record counts. Semantic search is not supported.

### Enabling ClickHouse

Enabling ClickHouse switches the entire deployment to it.

1. Provision a ClickHouse instance reachable from the ByteChef server.
2. Set the three environment variables (see [Configuration → Environment Variables](/platform/use-bytechef/self-hosted/configuration/environment-variables)):
   - `BYTECHEF_CONTEXT_STORE_CLICKHOUSE_URL`
   - `BYTECHEF_CONTEXT_STORE_CLICKHOUSE_USERNAME`
   - `BYTECHEF_CONTEXT_STORE_CLICKHOUSE_PASSWORD`
3. Restart ByteChef.

When `BYTECHEF_CONTEXT_STORE_CLICKHOUSE_URL` is set, all sources read and write records in ClickHouse. When it is unset, Postgres is the only records backend. Switching backends does not migrate existing records; run each source's sync again to fill the new backend.

### What ClickHouse does differently

| Capability | Postgres | ClickHouse |
|---|---|---|
| Sync writes (insert, update, tombstone) | ✓ | ✓ |
| Composite-key lookups | ✓ | ✓ |
| Filter-expression search (typed column dispatch) | ✓ | Not supported |
| Semantic search | ✓ | Not supported |
| Chunker pipeline and admin record delete | ✓ | Not supported |
| In-place `indexedFields` schema changes | JSONB whitelist (no DDL) | `ALTER TABLE` migration |

A search against a ClickHouse-backed source returns an error instead of empty results. The UI does not offer search for these sources.

### `indexedFields` schema migrations

On a Postgres-backed source, editing an entity's `indexedFields` is a metadata update. The next sync rebuilds the affected index rows.

On a ClickHouse-backed source, the same edit is a schema migration: `ALTER TABLE ADD COLUMN` for new fields, `ALTER TABLE DROP COLUMN` for removed fields, and `ALTER TABLE MODIFY COLUMN` for type changes that ClickHouse accepts. When ClickHouse rejects a type conversion (for example `TEXT` to `TIMESTAMP`), the column is dropped and added again. The typed column is `NULL` for existing rows until the next sync writes it; the JSON `_payload` keeps the value.

The migration runs synchronously inside the entity update transaction. If ClickHouse fails, the update is aborted and Postgres is not changed.

---

## Per-entity ClickHouse tables

For each entity on a ClickHouse-backed source, ByteChef creates a table named `cs_w{workspaceId}_c{contextStoreId}_s{sourceId}_{entityName}` (sanitised) using the `ReplacingMergeTree(_last_seen_at)` engine. The schema is:

```sql
CREATE TABLE cs_w1_c5_s42_contacts (
    _id String,
    _payload_hash String,
    _payload String,
    _last_seen_at DateTime64(3),
    _deleted_at Nullable(DateTime64(3)),
    -- one typed column per indexed field:
    email Nullable(String),
    age Nullable(Float64),
    updated_at Nullable(DateTime64(3))
) ENGINE = ReplacingMergeTree(_last_seen_at) ORDER BY (_id)
```

The `c{contextStoreId}` segment separates environments: each Context Store belongs to one
environment, so the same entity has separate tables in Development, Staging, and Production.

`_id` is the upstream `sourceRecordId`. The `ReplacingMergeTree` engine deduplicates on it during background merges. Tombstones use `ALTER TABLE ... UPDATE _deleted_at = ?`, which ClickHouse applies asynchronously; the next sync sees the tombstone state either way.

---

## Backup, replication, and operational concerns

ByteChef does not manage ClickHouse backup, replication, retention, or table tuning. Operate the ClickHouse server as an external dependency.

The Postgres `context_store_source` and `context_store_entity` rows define which records should exist. If ClickHouse data is lost, run the source sync again to rebuild it.
