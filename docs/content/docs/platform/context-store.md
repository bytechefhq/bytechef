---
title: Context Store
description: Connect external systems as queryable record stores for AI agents and workflows
---

# Context Store

<EEBadge />

> **Coming soon.** The Context Store is on the upcoming release track and is not yet available in the latest released version of ByteChef.

The Context Store keeps a synced replica of records from external systems (CRMs, ticket systems, knowledge bases) inside ByteChef so AI agents and workflows can search and read them without round-tripping to the source on every call.

Each **Context Store source** binds an external connector (HubSpot, Salesforce, a JSON file, etc.) to a sync cadence and a set of **entities** — named record shapes with declared indexed fields. The sync engine runs on the configured cadence, writes new and changed records into the store, and tombstones rows that have disappeared from upstream.

---

## Records backend

A Context Store source picks a records backend at create time:

- **Postgres** (default): records live in the same Postgres database as the rest of ByteChef. Supports the full query path including filter expressions and semantic search. Recommended for most workloads.
- **ClickHouse** (optional): records live in a separate ClickHouse server with per-entity typed-projection tables. Designed for sources with very large record counts where Postgres scans would be expensive. Semantic search is not supported on ClickHouse-backed sources in this release.

### Enabling ClickHouse

The ClickHouse backend is opt-in. To make it available to operators creating sources:

1. Provision a ClickHouse instance reachable from the ByteChef server.
2. Set the three environment variables (see [Configuration → Environment Variables](../self-hosting/configuration/environment-variables.md#context-store-configuration)):
   - `BYTECHEF_CONTEXT_STORE_CLICKHOUSE_URL`
   - `BYTECHEF_CONTEXT_STORE_CLICKHOUSE_USERNAME`
   - `BYTECHEF_CONTEXT_STORE_CLICKHOUSE_PASSWORD`
3. Restart ByteChef. The "Add Source" dialog will surface a **Records Backend** selector with both options.

When the URL is unset, the dialog quietly omits the selector — Postgres-only deployments see no change.

### What ClickHouse does differently

| Capability | Postgres | ClickHouse |
|---|---|---|
| Sync writes (insert, update, tombstone) | ✓ | ✓ |
| Composite-key lookups | ✓ | ✓ |
| Filter-expression search (typed column dispatch) | ✓ | Not in this release |
| Semantic search | ✓ | Not in this release |
| Chunker pipeline + admin record delete | ✓ | Not in this release |
| In-place `indexedFields` schema changes | JSONB whitelist (no DDL) | `ALTER TABLE` migration |

Calling search against a ClickHouse-backed source returns an explicit error rather than silent empty results — the UI doesn't expose search for these sources.

### `indexedFields` schema migrations

Editing an entity's `indexedFields` on a Postgres-backed source is a metadata update — the next sync rebuilds the affected index rows.

On a ClickHouse-backed source the same edit is a real schema migration: `ALTER TABLE ADD COLUMN` for new fields, `ALTER TABLE DROP COLUMN` for removed ones, and `ALTER TABLE MODIFY COLUMN` for type changes when ClickHouse accepts the conversion. When a type conversion is rejected (e.g. `TEXT` → `TIMESTAMP`), the migrator falls back to drop-and-add — the typed projection resets to `NULL` for existing rows, though the JSON `_payload` preserves the field's value. Subsequent sync writes repopulate the column.

The migration runs synchronously inside the entity-update transaction; a ClickHouse failure aborts the update with no Postgres state change.

---

## Per-entity ClickHouse tables

For each entity on a ClickHouse-backed source, the facade provisions a table named `cs_w{workspaceId}_s{sourceId}_{entityName}` (sanitised) using the `ReplacingMergeTree(_last_seen_at)` engine. The schema is:

```sql
CREATE TABLE cs_w1_s42_contacts (
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

`_id` is the `sourceRecordId` from upstream; the RMT engine deduplicates on it at background-merge time. Tombstones use `ALTER TABLE ... UPDATE _deleted_at = ?`, which is asynchronous in ClickHouse — the next sync run sees the tombstone state regardless, so eventual visibility is acceptable for the sync use case.

---

## Backup, replication, and operational concerns

ClickHouse operational tasks (backup, replication, retention policies, per-table tuning) are intentionally out of scope for ByteChef's Context Store wiring. Treat the ClickHouse server as an external dependency that the deployment team manages on its own terms — ByteChef writes to it and reads from it, but doesn't take responsibility for its lifecycle.

The Postgres `context_store_source` and `context_store_entity` rows remain the source of truth for which records *should* exist; if ClickHouse data is lost, re-running the sync from the source connector rebuilds it.
