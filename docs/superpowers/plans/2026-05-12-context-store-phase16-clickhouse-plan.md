# Context Store Phase 16 — Optional ClickHouse store: implementation plan

**Status**: Active
**Date**: 2026-05-12
**Spec**: [2026-05-12-context-store-phase16-design.md](../specs/2026-05-12-context-store-phase16-design.md)

Implementation breakdown for the 6-commit task sequence the spec mapped out. Each commit lands a coherent slice that compiles + tests green on its own; the order is sequenced so each commit's surface is consumable by the next without breaking anything in between.

## Pre-flight inventory (already gathered)

`ContextStoreRecordRepository` lives in `server/ee/libs/platform/platform-context-store/platform-context-store-service/src/main/java/com/bytechef/ee/platform/contextstore/repository/ContextStoreRecordRepository.java`. Two service impls call it: `ContextStoreRecordServiceImpl` and `ContextStoreQueryServiceImpl`. The surface area used externally:

- `save(ContextStoreRecord)`
- `findBySourceIdAndEntityNameAndSourceRecordId(sourceId, entityName, sourceRecordId)` — composite-key lookup; hot path of every sync write
- `tombstoneUnseen(sourceId, entityName, seenIds, deletedAt)` — bulk soft-delete after the run
- `findAllById(Iterable<Long>)` — used by the chunker pipeline to fetch record batches
- `findTombstonedRecordIdsBySourceIdAndEntityName(sourceId, entityName)` — tombstoned-id scan for the chunker
- `deleteById(Long)` — administrative single-record delete

Six methods. The `PagingAndSortingRepository` / `ListCrudRepository` inherited members (`findAll(Pageable)`, `count()`, etc.) are not called from outside the repository today — confirmed via grep. The interface abstraction can pin these six and only these.

`ContextStoreQueryServiceImpl.search()` does much more — it builds dynamic queries against `context_store_record` + `context_store_record_index` for the agent-facing query path. That surface is the OTHER half of the Postgres repository today (~10 more methods that aren't on `ContextStoreRecordRepository`). Commit 1 below decides whether to absorb the query-path methods into the abstraction immediately or do it in a follow-up commit.

**Decision**: defer the query-path abstraction. Commit 1 abstracts only the six write/lookup methods on `ContextStoreRecordRepository`. Commit 4 (ClickHouse repository impl) covers the same six. The query-path stays Postgres-only for Phase 16 — `ContextStoreQueryServiceImpl` keeps its direct Postgres dep and the spec's "cross-backend semantic search" open question stays open. Documented in commit 4's message.

## Commit 1 — Repository interface extract + Postgres rename

**Goal**: refactor `ContextStoreRecordRepository` from a single Spring Data JDBC interface into an SPI interface + a Postgres-bound impl. **No behavior change.** Service layer keeps working unchanged via the new SPI.

**Changes**:

1. Move existing `ContextStoreRecordRepository.java` interface contents into a new `ContextStoreRecordPostgresRepository` interface (same package). The new interface extends Spring Data JDBC's `PagingAndSortingRepository<ContextStoreRecord, Long>` + `ListCrudRepository<ContextStoreRecord, Long>` and carries the three custom query methods.
2. Create a new `ContextStoreRecordRepository` SPI interface in `platform-context-store-api` (NOT `-service`). Surface:
   ```java
   public interface ContextStoreRecordRepository {
       ContextStoreRecord save(ContextStoreRecord record);
       Optional<ContextStoreRecord> findBySourceIdAndEntityNameAndSourceRecordId(Long, String, String);
       int tombstoneUnseen(Long, String, Collection<String>, Instant);
       List<ContextStoreRecord> findAllById(Iterable<Long>);
       List<Long> findTombstonedRecordIdsBySourceIdAndEntityName(Long, String);
       void deleteById(Long);
   }
   ```
3. Create `ContextStoreRecordPostgresRepositoryAdapter` (a `@Component` in `-service`) that takes `ContextStoreRecordPostgresRepository` (Spring Data interface) via constructor and implements the new SPI by delegating each method. The adapter is the bridge between Spring Data's interface and our SPI.
4. Update `ContextStoreRecordServiceImpl` and `ContextStoreQueryServiceImpl` to depend on the new SPI interface rather than the Spring Data interface. (The latter only uses `findBySourceIdAndEntityNameAndSourceRecordId` — minor edit.)
5. Existing tests against the old `ContextStoreRecordRepository` bean need their type narrowed — most should still resolve since the SPI keeps the same method signatures. Spot-check `ContextStoreEntityServiceIntTest` and similar.

**Done when**: full build + test pass for `platform-context-store-service` and `automation-context-store-service`. Existing service-level tests cover the refactor — no new tests in this commit.

**Files**:
- `platform-context-store-api/.../repository/ContextStoreRecordRepository.java` (new — the SPI)
- `platform-context-store-service/.../repository/ContextStoreRecordPostgresRepository.java` (renamed from the existing file)
- `platform-context-store-service/.../repository/ContextStoreRecordPostgresRepositoryAdapter.java` (new — bridge)
- `platform-context-store-service/.../service/ContextStoreRecordServiceImpl.java` (depend on SPI)
- `platform-context-store-service/.../service/ContextStoreQueryServiceImpl.java` (depend on SPI)

## Commit 2 — `platform-context-store-clickhouse-service` module scaffold

**Goal**: create the module with its `@ConditionalOnProperty` cascade following the pgvector pattern. Module is wired but contains no consumers yet.

**Changes**:

1. New module directory: `server/ee/libs/platform/platform-context-store/platform-context-store-clickhouse-service/`.
2. `build.gradle.kts` deps: ClickHouse JDBC driver (`com.clickhouse:clickhouse-jdbc:0.7.x`), Spring DI, `platform-context-store-api` (SPI), Spring Data commons.
3. Add the module to `settings.gradle.kts`.
4. `ClickHouseDataSourceConfiguration` — `@Configuration` class with the pgvector four-layer pattern:
   - Class-level `@ConditionalOnProperty(prefix = "bytechef.context-store", name = "backend-providers", havingValue = "clickhouse")` — config only loads when ClickHouse is in the configured backends list. (Listing as csv keeps Postgres + ClickHouse coexistence possible per spec §"Per-workspace routing".)
   - `@Bean clickHouseDataSource()` gated on `@ConditionalOnProperty(prefix = "bytechef.context-store.clickhouse", name = "url")` — DataSource only materializes when a URL is set.
   - Cascade: `clickHouseJdbcTemplate`, `clickHouseTransactionManager`, all gated `@ConditionalOnBean(name = "clickHouseDataSource")`.
5. `ApplicationProperties.ContextStore` model class gets a new nested `ClickHouse` record (url, username, password) — mirrors `ApplicationProperties.Ai.Vectorstore.PgVector`.

**Done when**: module compiles. Spring context starts with `backend-providers=postgres` (the default) without instantiating any ClickHouse beans. Spring context with `backend-providers=postgres,clickhouse` + missing `clickhouse.url` starts cleanly with the DataSource bean absent (graceful degradation, per the pgvector pattern).

**Files**:
- `platform-context-store-clickhouse-service/build.gradle.kts` (new)
- `settings.gradle.kts` (added include)
- `platform-context-store-clickhouse-service/.../ClickHouseDataSourceConfiguration.java` (new)
- `app-config/.../ApplicationProperties.java` (extend `ContextStore` model)

## Commit 3 — Per-entity `CREATE TABLE` generator

**Goal**: at "Add Context Source" time when `backend=CLICKHOUSE`, generate `CREATE TABLE context_store_{workspace}_{source}_{entity} (...)` from the entity's `indexedFields`. Schema: `_id`, `_payload_hash`, `_last_seen_at`, `_deleted_at`, `_payload JSON`, one typed column per indexed field. Engine `ReplacingMergeTree(_last_seen_at) ORDER BY (_id)`.

**Changes**:

1. `ClickHouseTableNameSanitizer` — utility class. Takes `(workspaceId, sourceId, entityName)`, produces a safe table name (no SQL injection vector — strict regex on allowed characters, length cap, suffix `_<sourceId>` to disambiguate name collisions across workspaces).
2. `ClickHouseTableDdlGenerator` — static utility that produces `CREATE TABLE` DDL from a `ContextStoreEntity`. Maps `IndexedField.type` (TEXT/NUMERIC/TIMESTAMP) to ClickHouse types (`String`, `Int64`, `DateTime64(3)`).
3. `ContextStoreSource` gets a new `clickhouse_table_name VARCHAR(255)` column (Liquibase init edited in place per the established rule). Populated by the facade on create when `backend=CLICKHOUSE`.
4. `WorkspaceContextStoreSourceFacadeImpl.create()` learns to issue the `CREATE TABLE` via the new generator at the right point in the source-creation flow (after the entity row is persisted, before triggering the initial sync). Gated by `source.backend == CLICKHOUSE`.

**Done when**: unit tests cover the sanitizer (injection cases) and the DDL generator (each indexed-field type mapping; edge cases on field names). Facade flow stays Postgres-default — Phase 16 commits don't yet route runtime writes to ClickHouse; that's commit 4.

**Files**:
- `platform-context-store-clickhouse-service/.../ClickHouseTableNameSanitizer.java` (new)
- `platform-context-store-clickhouse-service/.../ClickHouseTableDdlGenerator.java` (new)
- corresponding unit tests
- `platform-context-store-api/.../domain/ContextStoreSource.java` (add `clickhouseTableName` field)
- `platform-context-store-service/.../resources/.../00000000000001_platform_context_store_init.xml` (edit in place — add `clickhouse_table_name`)
- `automation-context-store-service/.../facade/WorkspaceContextStoreSourceFacadeImpl.java` (issue CREATE TABLE when backend=CLICKHOUSE)

## Commit 4 — `ContextStoreRecordClickHouseRepository` SPI impl

**Goal**: ClickHouse-backed implementation of the `ContextStoreRecordRepository` SPI from commit 1.

**Changes**:

1. `ContextStoreRecordClickHouseRepository implements ContextStoreRecordRepository`. Bean-discovered via `@Component`, conditional on `@ConditionalOnBean(name = "clickHouseDataSource")` from commit 2 so it only activates when the DataSource bean is up.
2. Constructor takes `@Qualifier("clickHouseJdbcTemplate") JdbcTemplate` and uses the dynamic table name resolved via `(workspaceId, sourceId, entityName)`. Since the repository's method signatures take `sourceId` + `entityName` but no `workspaceId`, the repository needs `ContextStoreSourceService.fetch(sourceId).getWorkspaceId()` to resolve the table — or, more efficiently, cache the resolved table name on the `ContextStoreSource` row (the `clickhouse_table_name` column from commit 3).
3. Method-by-method:
   - `save(record)` — `INSERT INTO {table} (_id, _payload_hash, _last_seen_at, _deleted_at, _payload, ...) VALUES (?,?,?,?,?,...)`. RMT engine handles dedup at merge time. No-op return for inserts (ClickHouse INSERT doesn't return generated ids; the SPI return type stays `ContextStoreRecord` but the impl returns the input record with the original id when present, or a new long generated client-side when absent).
   - `findBySourceIdAndEntityNameAndSourceRecordId` — `SELECT ... FROM {table} WHERE _id = ? LIMIT 1` (sourceRecordId IS the `_id` in ClickHouse, since per-source/entity tables already partition).
   - `tombstoneUnseen` — `ALTER TABLE {table} UPDATE _deleted_at = ? WHERE _id NOT IN (...) AND _deleted_at IS NULL`. Mutations are async — wait via `system.mutations` table query if synchronous semantics are needed by tests.
   - `findAllById` — `SELECT ... WHERE id IN (...)`. ClickHouse doesn't have an `id` column in the per-entity table by default; the SPI's `Long id` becomes the same as `sourceRecordId` for ClickHouse (or we track a separate auto-increment via `id UInt64 DEFAULT generateSnowflakeID()`).
   - `findTombstonedRecordIdsBySourceIdAndEntityName` — straightforward query.
   - `deleteById` — `ALTER TABLE {table} DELETE WHERE _id = ?`. Async; same caveat as tombstoneUnseen.
3. The Postgres impl from commit 1 stays unchanged.

**Done when**: unit tests for each method (mocked JdbcTemplate, asserting the right SQL is fired with the right parameters). IntTest is commit 5.

**Files**:
- `platform-context-store-clickhouse-service/.../repository/ContextStoreRecordClickHouseRepository.java` (new)
- unit tests with `@ExtendWith(MockitoExtension.class)` + JdbcTemplate mocks

## Commit 5 — `backend` column on `context_store_source` + facade routing

**Goal**: persist the per-source backend choice + route writes to the right repository impl at runtime.

**Changes**:

1. `context_store_source` gets a `backend` enum column (INT ordinal) — `ContextStoreBackend { POSTGRES, CLICKHOUSE }`. Edited in place on the init Liquibase. Existing rows default to `POSTGRES` ordinal 0.
2. New enum `ContextStoreBackend` in `platform-context-store-api`. Append to `EnumOrdinalStabilityTest` (same pattern Phase 17b followed for `TombstoneStrategy`).
3. `ContextStoreSource.backend` field + getter/setter (ordinal-stored, `TombstoneStrategy`-style accessor).
4. `Create/UpdateContextStoreSourceInput` DTO + GraphQL input + .graphqls schema gain the new `backend` field. UI dialog gets a backend selector — defaults to POSTGRES, only enables CLICKHOUSE when `backend-providers` advertises ClickHouse on the server.
5. `ContextStoreRecordRepositoryRouter` (`@Component` in `-service`) takes both Postgres + ClickHouse impl beans via `@Autowired` (the ClickHouse one is optional via `ObjectProvider`). Dispatches each repository call to the right impl based on a `Long sourceId -> ContextStoreBackend` cache populated lazily from `ContextStoreSourceService.fetch`.
6. `ContextStoreRecordServiceImpl` and `ContextStoreQueryServiceImpl` depend on the Router instead of the raw SPI.

**Done when**: a workspace with `backend=POSTGRES` source continues to work exactly as before; a workspace with `backend=CLICKHOUSE` source routes through the router → ClickHouse impl. Unit tests cover both router branches.

**Files**:
- `platform-context-store-api/.../domain/ContextStoreBackend.java` (new enum)
- `platform-context-store-api/.../domain/ContextStoreSource.java` (add backend field)
- `platform-context-store-service/.../resources/.../00000000000001_platform_context_store_init.xml` (edit in place — add `backend` INT NOT NULL DEFAULT 0)
- `platform-context-store-service/.../util/EnumOrdinalStabilityTest.java` (pin new enum ordinals)
- `automation-context-store-api/.../dto/Create/UpdateContextStoreSourceInput.java` + GraphQL DTO mirrors
- `automation-context-store-graphql/.../context-store.graphqls`
- `platform-context-store-service/.../repository/ContextStoreRecordRepositoryRouter.java` (new)
- `client/src/pages/automation/context-store/components/AddContextSourceDialog.tsx` (backend selector — small)

## Commit 6 — IntTest paired-backend coverage

**Goal**: Testcontainers `ClickHouseContainer` IntTest that exercises the same scenarios `ContextStoreSyncE2EIntTest` does (initial sync, change-detect, tombstone) but against the ClickHouse path.

**Changes**:

1. `ContextStoreSyncClickHouseIntTest` — sibling to the existing Postgres IntTest. Same `@Disabled` skeleton pattern (the full Atlas + DataStream stack is still the blocker), but with the Testcontainers `ClickHouseContainer` configured so the wiring is ready for whichever session lights up the full E2E suite.
2. Inline notes in the test cover the ClickHouse-specific assertions: RMT merge timing (use `OPTIMIZE TABLE ... FINAL` between assertions in tests), async ALTER UPDATE mutations (wait via `SELECT * FROM system.mutations WHERE is_done = 0` or `OPTIMIZE` short-circuit).

**Done when**: the IntTest file lands with the same disabled-but-spec'd pattern as the existing CS E2E tests. Not expected to actually run in CI without the full E2E wiring.

## Open questions resurfaced from the spec

These remain unresolved after the plan above. Each is a flag for the implementation session — pick at the latest reasonable moment, document the decision in the commit message:

1. **Async ClickHouse mutations vs synchronous test expectations**. ALTER UPDATE / ALTER DELETE are async. The Postgres write-through tests assume immediate visibility. Tests need either `OPTIMIZE TABLE ... FINAL` between assertions or a mutation-completion poller. Affects commit 6.
2. **`_id` vs `id` column shape in ClickHouse**. The Postgres `ContextStoreRecord` has an `id Long` autoincrement. ClickHouse's RMT prefers `_id` as the merge key. Two options:
   - Repository SPI's `Long id` is the same as `sourceRecordId` for ClickHouse (no separate id column).
   - Add an auto-generated id via `generateSnowflakeID()` and keep both.
   Affects commit 4.
3. **Index update on entity-fields change**. Postgres handles `indexedFields` edits via JSONB whitelist updates (no DDL). ClickHouse needs real `ALTER TABLE ADD COLUMN`. Affects commit 3's `WorkspaceContextStoreSourceFacadeImpl.update()` flow — not yet scoped in the plan above. Will need a small follow-up edit to the facade's update path or a separate commit.

## Estimated effort

Per-commit time estimate (with the previous Phase 17b session as a baseline calibration):

- Commit 1: ~3 hours (refactor + run existing test surface)
- Commit 2: ~2 hours (module scaffold, deps, conditional config)
- Commit 3: ~4 hours (DDL generator + sanitizer + Liquibase + facade hook + unit tests)
- Commit 4: ~5 hours (six repository methods with unit-test coverage; ALTER UPDATE async timing is the main risk)
- Commit 5: ~3 hours (enum + Liquibase + DTO/GraphQL + router + unit tests)
- Commit 6: ~1 hour (disabled IntTest skeleton)

Total: ~18 hours of focused work, roughly two full sessions. The IntTest (commit 6) is shippable in this plan even without the full E2E suite landing — same shape as the existing disabled tests, ready to enable when the suite goes hot.

## What's deliberately deferred

Per spec §"Non-goals":

- Per-table tuning (RMT index granularity, projections).
- Live migration from Postgres to ClickHouse on an existing source.
- Read-replica fanout.
- MaterializedView ergonomics.
- Cross-backend semantic search (the query-path repository methods stay Postgres-only — see commit 1's "Decision" note).
- Backup / restore for ClickHouse — operational concern, separate from this code work.
