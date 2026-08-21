# Context Store Phase 16 — Optional ClickHouse record store

**Status**: Draft
**Date**: 2026-05-12
**Pattern source**: existing `PgVectorJdbcConfiguration` (provider-switch + bean cascade) and `AiHubPgVectorConfiguration` (per-feature gate).

## Goal

Swap in an alternative record-repository backend per-workspace: Postgres (default, MVP) or ClickHouse (analytics-class throughput, replaces Postgres entirely for that workspace's CS replica data). Both impls satisfy the same `ContextStoreQueryService` contract — agents and the synthetic `contextStore` component don't see the backend choice. The CS control plane (sources / entities / cadence / status / workspace relation) stays in Postgres regardless.

## Why this is its own phase

CS MVP picked Postgres for simplicity (already deployed, Liquibase-friendly, transactional, fits ≤10M-row replicas). Beyond that volume — telemetry-style sources, event streams, large CRM exports — Postgres pays a vacuum / IO tax that ClickHouse's `ReplacingMergeTree` engine cleanly avoids. A separable backend lets workspaces opt in without changing the agent surface.

## The pgvector pattern as reference

The pattern to mirror lives in two layers:

### Layer 1 — top-level provider switch (`PgVectorJdbcConfiguration:52`)

```java
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore", name = "provider", havingValue = "pgvector")
class PgVectorJdbcConfiguration {
```

The **entire config class** only loads when `bytechef.ai.vectorstore.provider=pgvector`. A different provider value (or none) means the class is invisible to the Spring context — no beans, no DataSource attempt, no init-time crash if the URL is unset.

### Layer 2 — bean-level gate within the class (`PgVectorJdbcConfiguration:69-79`)

```java
@Bean
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore.pgvector", name = "url")
@ConditionalOnProperty(prefix = "bytechef.tenant", name = "mode", havingValue = "single", matchIfMissing = true)
DataSource pgVectorDataSource() { ... }
```

Even within `provider=pgvector` mode, the DataSource bean only materializes when a URL is configured. Multi-tenant mode skips this single-tenant DataSource entirely.

### Layer 3 — downstream bean cascade

Every downstream bean (`pgVectorJdbcTemplate`, `pgVectorTransactionManager`, `pgVectorDataAccessStrategy`, `JdbcAggregateTemplate`) uses `@ConditionalOnBean(name = "pgVectorDataSource")` so they evaporate together if the DataSource doesn't materialize. **One missing property turns off the whole stack** without leaving half-wired beans that crash on first use.

### Layer 4 — per-feature gate (`AiHubPgVectorConfiguration:56`)

```java
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.ai-hub", name = "enabled", havingValue = "true")
public class AiHubPgVectorConfiguration { ... }
```

Even if pgvector itself is up, AI Hub's tool-search vector store is gated by AI Hub being on. A deployment can enable pgvector for the KB without bringing up Tool Search, and vice versa.

### Applying it to ClickHouse

The four layers translate cleanly:

| Layer | pgvector | Phase 16 ClickHouse |
|---|---|---|
| 1. Top-level switch | `bytechef.ai.vectorstore.provider=pgvector` | `bytechef.context-store.backend=clickhouse` (workspace-default; per-workspace override below) |
| 2. DataSource gate | URL must be set | `bytechef.context-store.clickhouse.url` must be set |
| 3. Cascade | `@ConditionalOnBean(name = "pgVectorDataSource")` | `@ConditionalOnBean(name = "clickHouseDataSource")` |
| 4. Per-feature | `bytechef.ai.ai-hub.enabled` | n/a (no sub-feature scope needed — CS is the only consumer) |

**Critical addition**: per-workspace backend selection. The system-wide `bytechef.context-store.backend` is the *default* for new sources, but each `ContextStoreSource` row carries its own `backend` column (`POSTGRES` | `CLICKHOUSE`). Repository selection happens at the service layer, not via Spring profile, so two workspaces in the same deployment can use different backends.

## Architecture

### Repository abstraction (Task 40)

Today's `ContextStoreRecordRepository` is a concrete Spring Data JDBC repository bound to Postgres. Refactor:

- Extract the interface as `ContextStoreRecordRepository` with the public methods (`findByKey`, `upsert`, `tombstoneUnseen`, `findByFilter`, `vectorSearch` for the semantic index, etc.).
- Rename today's class to `ContextStoreRecordPostgresRepository`. No method-body changes — pure rename + extract.
- A service-layer `ContextStoreRecordRepositoryRouter` (or `Map<ContextStoreBackend, ContextStoreRecordRepository>` injection) picks the right impl per request based on `source.getBackend()`.

### ClickHouse module (Task 41)

New module: `server/ee/libs/platform/platform-context-store/platform-context-store-clickhouse-service/`.

- `build.gradle.kts` adds `com.clickhouse:clickhouse-jdbc:0.7.x` (binary driver, not the shaded slim variant — we want the standard JDBC API).
- `ClickHouseDataSourceConfiguration` follows the pgvector pattern:
  - Class-level: `@ConditionalOnProperty(prefix = "bytechef.context-store", name = "backend-providers", havingValue = "clickhouse")` — note `backend-providers` (plural / CSV) since a deployment can run both backends side-by-side.
  - `@Bean clickHouseDataSource()` gated by `@ConditionalOnProperty(prefix = "bytechef.context-store.clickhouse", name = "url")`.
  - Cascade: `clickHouseJdbcTemplate`, `clickHouseTransactionManager` (note: ClickHouse's JDBC driver has weak transaction semantics — see "Open questions"), and the repository bean — all gated via `@ConditionalOnBean(name = "clickHouseDataSource")`.

### Per-entity dynamic-table generator (Task 42)

ClickHouse doesn't fit the "one shared `context_store_record` table for all sources" Postgres model — its strength is per-entity wide tables with typed columns. At "Add Context Source" time (when `backend=CLICKHOUSE`), the platform issues a `CREATE TABLE` against the ClickHouse connection:

```sql
CREATE TABLE context_store_{workspace}_{source}_{entity} (
    _id           String,
    _payload_hash String,
    _last_seen_at DateTime64(3),
    _deleted_at   Nullable(DateTime64(3)),
    _payload      String,                                  -- raw JSON for full-doc reads
    {column_for_each_indexed_field}                        -- typed column per ContextStoreEntity.indexedFields
) ENGINE = ReplacingMergeTree(_last_seen_at)
ORDER BY (_id);
```

`ReplacingMergeTree(_last_seen_at)` makes upserts free (last-write-wins on `_id` collision; the merge engine deduplicates in the background).

Per-source bookkeeping: a `clickhouse_table_name` column on `context_store_source` records the actual table name, since `_` substitution rules and identifier sanitization don't round-trip cleanly.

### ClickHouseRepository (Task 43)

`ContextStoreRecordClickHouseRepository` implements the `ContextStoreRecordRepository` contract:

- **`upsert`** → `INSERT INTO {table} (...)` — ClickHouse's RMT engine handles dedup at merge time. Reads see eventual consistency until the next merge; for CS this is acceptable because the writer's hash-skip fast path is the load-bearing dedup mechanism, not the storage engine.
- **`tombstoneUnseen`** → `ALTER TABLE {table} UPDATE _deleted_at = now() WHERE _last_seen_at < :runStart AND _deleted_at IS NULL` (mutations in ClickHouse are async — confirm via `system.mutations` for tests).
- **Filter translation**: `WHERE field = 'value'` translates directly for typed columns; JSONB ops (`->`, `->>`) become `JSONExtractString(_payload, 'field') = 'value'`. The query builder normalizes on the same `ContextStoreFilter` AST the Postgres impl consumes.
- **Vector search** → ClickHouse's `cosineDistance(vector, [...])` with an `ORDER BY ... LIMIT` and an `ANN INDEX` (if version supports it, otherwise sequential — measure first, optimize later). The vector column lives in the same per-entity table.

### Migration runner (Task 44)

Liquibase doesn't handle ClickHouse cleanly (the driver doesn't expose the DDL surface Liquibase needs). Two options:

- **Option A (chosen)**: at "Add/Update Context Source" time, the facade issues the `CREATE TABLE` / `ALTER TABLE` directly via JDBC. Idempotent (`CREATE TABLE IF NOT EXISTS`, `ALTER ... ADD COLUMN IF NOT EXISTS`). Schema lives in code, version-controlled with the source-creation flow.
- **Option B (rejected)**: ship a separate `clickhouse-migrator` tool. Adds operational burden disproportionate to the schema's complexity (one table per entity, columns set at create-time).

Idempotency is the safety property — replays after a partial failure don't corrupt state. We don't drop columns automatically (that's a manual op).

### Per-workspace routing (new concern not in original spec)

The original spec said "ClickHouse replaces Postgres entirely on a per-workspace basis" but didn't pick a mechanism. Proposal: `context_store_source.backend` enum column (`POSTGRES` default, `CLICKHOUSE`). The router selects the impl based on the source row's value. This allows:

- Deployments running only Postgres (the property `backend-providers` list excludes `clickhouse`) — ClickHouse beans never load; a `backend=CLICKHOUSE` source row would fail loudly at first read (acceptable: this is a config error).
- Deployments running both — each source picks its backend at create-time; cannot switch later without a migration script.
- Single-backend deployments work today's way with zero config change.

### IntTest (Task 45)

`ContextStoreSyncClickHouseIntTest` mirrors the existing Postgres `ContextStoreSyncIntTest` via Testcontainers' `ClickHouseContainer`:
- Initial sync writes N records.
- Re-sync with one changed payload updates the hash via RMT.
- Re-sync missing two records tombstones them on the next mutation flush.
- A typed-column filter and a JSONB-extract filter both return the expected rows.

Mark `@Disabled("Requires Docker")` if the EE test profile already does that for Postgres; otherwise gate behind a `clickhouse` Gradle test category.

## Schema additions (Postgres control plane)

Edit `context_store_source`'s existing init Liquibase in place (branch unmerged per established rule):

```xml
<column name="backend" type="INT" defaultValueNumeric="0">
    <constraints nullable="false"/>
</column>
<column name="clickhouse_table_name" type="VARCHAR(255)"/>
```

Append `ContextStoreBackend { POSTGRES, CLICKHOUSE }` to the enum-ordinal-stability test.

## Configuration shape

```yaml
bytechef:
  context-store:
    backend-providers: postgres,clickhouse        # csv; default 'postgres'
    clickhouse:
      url: jdbc:ch://localhost:8123/default
      username: default
      password:
```

The `provider` is plural here because both can coexist; only one is the system default for new sources (`backend-providers[0]`).

## Open questions

- **Transaction semantics**: ClickHouse's JDBC driver has weak/no transactions. The Postgres path uses `@Transactional` extensively; the ClickHouse impl needs equivalent durability stories. The MVP answer is: rely on idempotent writes (RMT dedup) and skip transactions. The IntTest must prove the worst-case retry behavior is benign.
- **Schema evolution after source create**: today the source's `indexedFields` is editable. On Postgres that's a JSONB whitelist update (no DDL). On ClickHouse it's a real `ALTER TABLE ADD COLUMN`. The facade learns to issue the DDL on update, ALTER-skipping columns that already exist.
- **Cross-backend semantic search**: when a workspace has multiple sources on different backends and the agent's `searchContextStore` tool spans them, the implementation either (a) federates the query, (b) requires single-backend per query, or (c) defers semantic-only routing. MVP picks (b) and surfaces a clear error if the agent's query spans backends; (a) is a follow-up phase.
- **Backup / restore**: Postgres has a deployment-standard answer (pg_dump). ClickHouse needs its own. Out of scope for Phase 16 but noted as a deployment requirement.

## Non-goals (Phase 16)

- **Per-table tuning** (RMT index granularity, projections) — defaults are fine for MVP.
- **Live migration from Postgres to ClickHouse on an existing source** — must drop + recreate the source.
- **Read-replica fanout** for ClickHouse — single-node deployment is the bar.
- **MaterializedView ergonomics** that ClickHouse is famous for — out of scope; CS uses ClickHouse purely as a typed record store.

## Task sequence (~6 commits)

1. `CC16 Refactor ContextStoreRecordRepository into interface + Postgres impl rename (no behavior change)`.
2. `CC16 Scaffold platform-context-store-clickhouse-service module + ConditionalOnProperty config (pgvector pattern)`.
3. `CC16 Per-entity CREATE TABLE generator + ClickHouseTableNameSanitizer`.
4. `CC16 ContextStoreRecordClickHouseRepository (CRUD + filter translation + ANN search)`.
5. `CC16 backend column + clickhouse_table_name on context_store_source (edit init liquibase) + facade routing`.
6. `CC16 ContextStoreSyncClickHouseIntTest`.

Each commit is independently shippable: 1 lands without touching any new dependencies; 2-3 land the module skeleton; 4-5 wire the runtime; 6 proves it.

## Commit convention

`CC16 …`. EE module — adds the Enterprise license header per project convention.
