# Context Store ClickHouse mutable schema — implementation plan

**Status**: Draft
**Date**: 2026-05-12
**Spec**: [2026-05-12-context-store-clickhouse-mutable-schema-design.md](../specs/2026-05-12-context-store-clickhouse-mutable-schema-design.md)

Three-commit breakdown for the spec. Each commit ships green on its own.

## Commit 1 — SPI surface + diff helper

**Goal**: introduce the new types and the pure diff helper. No wiring; no behavior change yet.

**Changes**:

1. `platform-context-store-api/.../clickhouse/IndexedFieldChange.java` — sealed interface with three records (Add, Drop, TypeChange). Each carries the minimum data the migrator needs to produce SQL.

2. `platform-context-store-api/.../clickhouse/ClickHouseTableMigrator.java` — SPI:
   ```java
   public interface ClickHouseTableMigrator {
       MigrationResult applyChanges(ContextStoreEntity entity, List<IndexedFieldChange> changes);
   }
   public record MigrationResult(int adds, int drops, int compatibleTypeChanges, int lossyTypeChanges) {}
   ```

3. `platform-context-store-clickhouse-service/.../schema/IndexedFieldDiffer.java` — static helper:
   ```java
   public static List<IndexedFieldChange> diff(Map<String, ?> oldFields, Map<String, ?> newFields)
   ```
   Returns a deterministic list sorted by field name. Adds come first (alphabetical), then Drops (alphabetical), then TypeChanges (alphabetical). This ordering choice doesn't matter functionally but keeps migration diffs reviewable.

4. Unit tests for the differ: empty-to-non-empty, non-empty-to-empty, additions only, drops only, type changes only, mixed, no-change identity.

**Done when**: differ unit tests pass; no production code references the new types yet.

**Files**:
- `platform-context-store-api/.../clickhouse/IndexedFieldChange.java` (new)
- `platform-context-store-api/.../clickhouse/ClickHouseTableMigrator.java` (new — SPI)
- `platform-context-store-api/.../clickhouse/MigrationResult.java` (new — record)
- `platform-context-store-clickhouse-service/.../schema/IndexedFieldDiffer.java` (new)
- `platform-context-store-clickhouse-service/.../schema/IndexedFieldDifferTest.java` (new)

## Commit 2 — ClickHouseTableMigratorImpl

**Goal**: ClickHouse-side implementation of the SPI. Reads from `IndexedFieldChange`, emits `ALTER TABLE` per change.

**Changes**:

1. `ClickHouseTableMigratorImpl` `@Component @ConditionalOnBean(name = "clickHouseDataSource")`. Mirrors the provisioner's wiring shape.

2. SQL per change:
   - **Add**: `ALTER TABLE <table> ADD COLUMN IF NOT EXISTS <field> Nullable(<chType>)`. Type mapping pulled from a shared helper that today lives inline in `ClickHouseTableDdlGenerator.resolveTypedFields()` — extract to a public `ClickHouseFieldTypeMapper.toClickHouseType(String fieldType)` so both the create-time generator and the migrator use the same mapping.
   - **Drop**: `ALTER TABLE <table> DROP COLUMN IF EXISTS <field>`.
   - **TypeChange**: try `ALTER TABLE <table> MODIFY COLUMN <field> Nullable(<newChType>)`. On `DataAccessException` (likely a type-incompatibility), catch, log warn, then `DROP COLUMN IF EXISTS` + `ADD COLUMN IF NOT EXISTS`. Track this as `lossyTypeChanges++` in the result.

3. Same `SQL_INJECTION_SPRING_JDBC` suppression posture as the provisioner — field names are validated by the field-name regex from `ClickHouseTableDdlGenerator.VALID_FIELD_NAME_PATTERN` (extract this constant too if needed); table name comes from `entity.getClickhouseTableName()` re-validated at use-site.

4. Metrics: emit `bytechef_context_store_clickhouse_schema_migration{outcome}` and `bytechef_context_store_clickhouse_schema_change{change}` via injected `ObjectProvider<MeterRegistry>`. Both no-op when the registry isn't wired (parallel to how connection-visibility metrics work).

**Done when**: unit tests for the migrator pass — mocked `JdbcTemplate`, ArgumentCaptor on SQL string per branch, including the lossy-fallback path.

**Files**:
- `platform-context-store-clickhouse-service/.../schema/ClickHouseTableMigratorImpl.java` (new)
- `platform-context-store-clickhouse-service/.../schema/ClickHouseFieldTypeMapper.java` (new — extracted from DDL generator)
- `platform-context-store-clickhouse-service/.../schema/ClickHouseTableDdlGenerator.java` (modified — use the extracted mapper)
- `platform-context-store-clickhouse-service/.../schema/ClickHouseTableMigratorImplTest.java` (new)

## Commit 3 — Service + facade integration

**Goal**: route `updateContextStoreEntity` through the migrator for CLICKHOUSE-backed entities. End-to-end.

**Changes**:

1. `ContextStoreEntityService` gains:
   ```java
   ContextStoreEntity updateWithSchemaMigration(Long entityId, Map<String, ?> newIndexedFields, ...);
   ```
   Existing `update()` stays as-is — used internally by the migration-aware path and by simple metadata-only edits (description, etc.).

2. `ContextStoreEntityServiceImpl.updateWithSchemaMigration()`:
   - Load the entity.
   - Compute `IndexedFieldChange` list via the differ.
   - Determine the backend via the parent source.
   - If CLICKHOUSE: call `ClickHouseTableMigrator.applyChanges()` (resolved via the injected `ObjectProvider`; throw `IllegalStateException` if absent — same posture as the router).
   - If POSTGRES: no migration step (the Postgres index table self-heals on next sync).
   - Apply the entity field changes and persist.

3. `ContextStoreSourceGraphQlController.updateContextStoreEntity()` is modified to call the new method when `input.indexedFields() != null`. Other fields (description, semanticIndexFields, parameters) still go through the existing path.

4. `WorkspaceContextStoreSourceFacadeImpl` doesn't directly need to know about the migrator — it doesn't currently handle entity-level updates (just source-level cadence and enable/disable). Confirm this by reading the facade's update flow before changing anything.

5. Smoke test: extend `ClickHouseServiceSmokeIntTest` with a check that `ClickHouseTableMigrator` bean is absent when URL is unset.

**Done when**: existing 35 facade tests + 24 repo tests + 9 router tests + 1 smoke test still pass; new migrator + service tests cover the integration path.

**Files**:
- `platform-context-store-api/.../service/ContextStoreEntityService.java` (modified — new method)
- `platform-context-store-service/.../service/ContextStoreEntityServiceImpl.java` (modified)
- `automation-context-store-graphql/.../web/graphql/ContextStoreSourceGraphQlController.java` (modified — route through new service method)
- `platform-context-store-clickhouse-service/.../ClickHouseServiceSmokeIntTest.java` (modified — extra bean-absence check)

## Estimated effort

- Commit 1: ~1h (types + diff helper + 8-ish unit tests)
- Commit 2: ~2h (migrator impl, fallback path, mocked JdbcTemplate tests)
- Commit 3: ~2h (service + controller integration, end-to-end run-through, smoke update)

Total: ~5h. One focused session.

## What's deliberately out of scope

Listed in the spec under "Non-goals" but worth re-flagging here:

- Eager existing-row backfill of newly-added columns from `_payload` JSON.
- Schema rollback / "undo last migration".
- Online validation at sync time (records carrying payloads for dropped fields silently keep them in `_payload`).
- Per-user "confirm lossy change" UX — the migrator just performs the drop-and-readd and surfaces it via the `lossyTypeChanges` count in the result; the GraphQL response shape isn't extended yet.

If any of those become real friction post-shipping, each is a small follow-up.

## Open questions resurfaced from the spec

1. Whether `ALTER TABLE` schema changes are sync (committed to "yes; tests confirm" — IntTest needs a check that adds a column then immediately reads the new column shape).
2. Whether to require explicit confirmation for lossy type changes (committed to "no, just log + metric").
3. JSON `_payload` preservation on `DROP COLUMN` (confirmed in spec).

None block implementation — pick them at the latest reasonable moment, document in commit messages.
