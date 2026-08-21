# Context Store ClickHouse mutable schema — design spec

**Status**: Draft
**Date**: 2026-05-12
**Phase 16 follow-up** — resolves the deferred "open question 3" from `2026-05-12-context-store-phase16-clickhouse-plan.md`.

## Problem

Phase 16 lands per-entity ClickHouse tables provisioned at `ContextStoreEntity` creation time via `ClickHouseTableProvisionerImpl.provisionTable()`. The Postgres side handles entity-fields edits trivially — `context_store_record_index` is a generic `(record_id, field_name, value_*)` table, so changing `ContextStoreEntity.indexedFields` is a JSONB whitelist update with no DDL. The ClickHouse side has typed columns baked into the `CREATE TABLE` DDL, so the same edit needs schema migration.

Today, `WorkspaceContextStoreSourceFacadeImpl.update()` and the `updateContextStoreEntity` GraphQL mutation accept arbitrary `indexedFields` edits without flagging that ClickHouse-backed entities can't apply them. The user-visible failure is silent: subsequent sync writes succeed, but the new field never appears in queries because the ClickHouse table doesn't have a column for it.

We need a story for this. Phase 16 explicitly deferred it; this spec picks the semantics.

## Scope of the edit problem

`ContextStoreEntity.indexedFields` is a `Map<String, ?>` where keys are field names and values are field-type identifiers (`"TEXT"`, `"NUMERIC"`, `"TIMESTAMP"`). Editing this map can do four distinct things:

1. **Add a new field**: `{name: TEXT}` → `{name: TEXT, age: NUMERIC}`. New column needed.
2. **Remove an existing field**: `{name: TEXT, age: NUMERIC}` → `{name: TEXT}`. Column becomes dead.
3. **Rename a field**: `{name: TEXT}` → `{full_name: TEXT}`. Column needs to be renamed or replaced.
4. **Change a field's type**: `{age: TEXT}` → `{age: NUMERIC}`. Column needs to be retyped or replaced.

ClickHouse has different operational profiles for each:

| Operation | ClickHouse mechanism | Cost / risk |
|---|---|---|
| Add column | `ALTER TABLE ... ADD COLUMN <name> Nullable(<type>)` | Cheap. Existing rows get NULL for the new column. Sync-going-forward populates it. |
| Drop column | `ALTER TABLE ... DROP COLUMN <name>` | Cheap on metadata; reclaims disk lazily during merges. Existing payloads still carry the old field; only the projection disappears. |
| Rename column | `ALTER TABLE ... RENAME COLUMN <old> TO <new>` | Cheap on metadata. Equivalent to drop + add for the JSON payload (still has `<old>`). |
| Type change (compatible) | `ALTER TABLE ... MODIFY COLUMN <name> <new_type>` | Cheap when types are compatible (e.g. `Int32` → `Int64`). Otherwise rejected. |
| Type change (incompatible) | Drop + add | The old column's values are lost from the typed projection; the JSON payload still has them. |

## Design

### Layer 1: Surface validation

`WorkspaceContextStoreSourceFacadeImpl.update()` and `updateContextStoreEntity` route to a new method on the existing `ContextStoreEntityService`:

```java
ContextStoreEntity updateWithSchemaMigration(Long entityId, UpdateContextStoreEntityInput input);
```

The implementation diffs the entity's current `indexedFields` against the input's new value and produces a list of `IndexedFieldChange` records:

```java
sealed interface IndexedFieldChange {
    record Add(String fieldName, String fieldType) implements IndexedFieldChange {}
    record Drop(String fieldName) implements IndexedFieldChange {}
    record TypeChange(String fieldName, String oldType, String newType) implements IndexedFieldChange {}
}
```

There's no explicit `Rename` — a rename presents as `Drop` of the old key + `Add` of the new key. We could detect "drop+add with same type and value=value heuristic" and emit a `Rename` but the simple shape preserves correctness (the new field starts empty either way; the JSON payload retains the old value under the old name).

### Layer 2: Backend-conditional execution

For POSTGRES-backed entities: `contextStoreEntityRepository.save()` of the updated row is the whole story — the JSONB whitelist updates, the `context_store_record_index` rows that exist for the dropped field become orphaned (and harmless; they just stop matching new queries since the field is no longer in the whitelist). A separate cleanup task can sweep them later, but it's not blocking.

For CLICKHOUSE-backed entities: the change list runs through a new SPI:

```java
public interface ClickHouseTableMigrator {
    void applyChanges(ContextStoreEntity entity, List<IndexedFieldChange> changes);
}
```

`ClickHouseTableMigratorImpl` (in `platform-context-store-clickhouse-service`) executes each change as a separate `ALTER TABLE` statement against the per-entity table:

- `Add` → `ALTER TABLE <name> ADD COLUMN IF NOT EXISTS <field> Nullable(<chType>)`. Idempotent.
- `Drop` → `ALTER TABLE <name> DROP COLUMN IF EXISTS <field>`. Idempotent. **The dropped field stays in `_payload`** (the JSON column), so reverting the change (re-adding with same name + type) is non-destructive going forward — old rows just have NULL for the column until the next sync overwrites them.
- `TypeChange` → try `MODIFY COLUMN` with the new ClickHouse type. If the ALTER fails (type incompatibility), fall back to `DROP COLUMN` + `ADD COLUMN` (lossy on the projection; payload preserved). Surface the lossy fallback in the response so the user knows reindexing is needed.

The `ClickHouseTableMigrator` is injected into `ContextStoreEntityService` via `ObjectProvider` — Postgres-only deployments don't see it and the CLICKHOUSE-branch never fires.

### Layer 3: Transactional consistency

The Postgres `context_store_entity` row update and the ClickHouse `ALTER TABLE` execution are on different datasources, so they can't share a transaction. The chosen order:

1. Issue ClickHouse `ALTER TABLE` statements first. If any fails, abort — Postgres entity row stays as-is.
2. Persist the updated Postgres entity row.

This is safe for retries — ClickHouse changes are `IF NOT EXISTS` / `IF EXISTS` idempotent. The worst case is a successful ClickHouse migration followed by a Postgres save failure (e.g. optimistic-lock conflict from a concurrent edit): the ClickHouse table now has a column the entity row doesn't reflect. That's a benign drift — the column is `Nullable`, doesn't fill, and a subsequent successful update reconciles. We **don't** plan an active rollback step.

### Layer 4: Existing-row reindexing

For POSTGRES, reindexing is a separate concern handled by the chunker pipeline (rebuilds `context_store_record_index` rows on next sync for changed entities).

For CLICKHOUSE, the new typed column gets populated by the **next sync run** when records pass through `ContextStoreItemWriter`. Existing rows have NULL in the new column until they're touched again. Two implications:

- Queries against a freshly-added column return only records that have been re-synced post-migration. Users who add an indexed field mid-source-lifetime should expect a delayed materialization.
- If the user wants immediate population, they can manually trigger `refreshContextStoreSource(id)` (full re-sync) after the entity update — same workflow Phase 17b uses for paired-cadence refresh.

We accept this asymmetry. A "auto-trigger sync after schema migration" toggle is an opt-in we can add later if friction is real.

### Layer 5: Status surface

`UpdateContextStoreEntityInput` doesn't gain new fields. The mutation response (the updated `ContextStoreEntity` row) is sufficient — the operator sees the new `indexedFields` shape.

Two new metrics emitted on the migrator:

- `bytechef_context_store_clickhouse_schema_migration{outcome=success|partial|failure}` — counter, per-call.
- `bytechef_context_store_clickhouse_schema_change{change=ADD|DROP|TYPE_COMPATIBLE|TYPE_LOSSY}` — counter, per individual change.

The `TYPE_LOSSY` tag flags the drop-and-readd fallback so an operator dashboard can spot users who incurred a query-projection reset.

## Failure modes

| Scenario | Behavior |
|---|---|
| `ALTER TABLE ADD COLUMN` race (column already exists) | `IF NOT EXISTS` handles it; success. |
| Concurrent two-user edit to same entity | Spring Data JDBC `@Version` on `ContextStoreEntity` blocks one with OptimisticLockingFailure; user retries with current state. |
| ClickHouse server temporarily unreachable | `ALTER TABLE` fails fast (no async wait). Update mutation throws; user retries. |
| Lossy type change | Migrator surfaces `TYPE_LOSSY` outcome; metric incremented; user notified via the mutation's eventually-returned `ContextStoreEntity` shape (since the column type DID change). |
| Operator removes `bytechef.context-store.clickhouse.url` after a CLICKHOUSE source exists | Migrator bean disappears. Any update to that entity's `indexedFields` fails with the same "Refusing silent fallback" message the router uses today. |

## Non-goals

- **Existing-row backfill**: we do not query the JSON `_payload` to retroactively populate the new column for existing rows. The next sync writes do that. Eager backfill could be a follow-up if a user case demands it.
- **Schema rollback**: no "undo last migration" command. Users edit `indexedFields` back to the previous shape if they need the column back; the migrator treats it as a fresh Add.
- **Cross-source schema sharing**: each entity owns its table. Two entities (even in the same source) with identical `indexedFields` get separate tables and migrate independently.
- **Online schema validation at sync time**: `ContextStoreItemWriter` doesn't validate that a record's payload matches the latest `indexedFields` shape. If a sync write arrives for a column that's been dropped, the JSON payload still captures the value — only the typed projection is missing. Queries that look at the typed column miss it; queries that look at JSON still find it. We accept this implicit dual-read story.

## Implementation surface (preview, will become its own plan)

- New SPI: `platform-context-store-api/.../clickhouse/ClickHouseTableMigrator.java`.
- Impl: `platform-context-store-clickhouse-service/.../ClickHouseTableMigratorImpl.java`.
- New value type: `platform-context-store-api/.../clickhouse/IndexedFieldChange.java` (sealed interface).
- Service surface: `ContextStoreEntityService.updateWithSchemaMigration(Long, UpdateContextStoreEntityInput)`.
- Facade wiring: `WorkspaceContextStoreSourceFacadeImpl` injects `ObjectProvider<ClickHouseTableMigrator>` (mirror of the provisioner pattern).
- Diff helper: small util that produces `List<IndexedFieldChange>` from old + new `Map<String, ?>` — TreeMap-based, deterministic order.
- Test: focused unit tests for the diff + migrator's SQL shape per change. Smoke check in `ClickHouseServiceSmokeIntTest` that the migrator bean is absent when URL isn't set.
- Documentation: `ContextStoreEntity.indexedFields` getter Javadoc gains a note about the migrator and the dual-read query semantics for CLICKHOUSE.

## Effort estimate

- Diff helper + value types: ~1h
- `ClickHouseTableMigrator` SPI + impl: ~1.5h
- Service hookup + facade wiring: ~1h
- Unit + smoke tests: ~1h
- Documentation: ~0.5h

Total: ~5h, one focused session.

## Open questions

1. **Should the migrator wait for ClickHouse mutations to complete?** `ALTER TABLE` for schema is sync (unlike `ALTER UPDATE` for data which is async). I think we just call and trust the round-trip — if the migrator returns, the schema is changed. If we're wrong about that for one of the change types, the IntTest will catch it.

2. **Should lossy type changes require explicit confirmation?** The mutation today silently performs the drop-and-readd. We could require `UpdateContextStoreEntityInput.confirmLossyTypeChange: Boolean = false` and reject silently-lossy edits. I'd defer this until we see a real user accidentally lose a projection — opt-in confirmation is friction we don't need before we know whether the failure mode happens in practice.

3. **Should `DROP COLUMN` actually preserve the JSON payload or also strip the key?** The Postgres side keeps the JSONB payload intact and just removes the index rows. ClickHouse `_payload` is the same JSON string and we naturally preserve it. Confirmed: no payload mutation.
