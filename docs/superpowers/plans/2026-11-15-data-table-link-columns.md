# Data Table Link Columns Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `LINK` column type to data tables. It holds the id of one row of another table in the same workspace
and environment, is enforced by a database foreign key, and has a per-column delete rule (`BLOCK` or `CLEAR`).

**Architecture:** A new registry table `data_table_link_column` (one row per link column per environment) records
the target table, delete rule and display column. The physical source table gets a `BIGINT` column with a real
foreign key and an index. `DataTableServiceImpl` and `DataTableRowServiceImpl` both read the registry through a new
repository (services never call each other) and overlay it on the types read from `information_schema`. GraphQL,
public REST, the data-table component and the client expose the new type.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, JdbcTemplate, Liquibase, PostgreSQL + H2, Testcontainers,
Spring GraphQL, OpenAPI generator, React 19 + TypeScript, TanStack Query, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-15-data-table-link-columns-design.md`

## Global Constraints

- Branch `data-table-links`, built on `5746`. Commit subjects only, as `5746 <Description>` (server) or
  `5746 client - <Description>` (client). No trailers.
- No prose comments or Javadoc in new code. Class Javadoc holds only `@author Ivica Cardic`; files under `server/ee/`
  use the EE license header and add `@version ee`.
- Liquibase changelogs are committed on their own, before the code that uses them. Never edit a released changelog.
- Generated files (OpenAPI sources, `client/src/shared/middleware/graphql.ts`, component definition JSON) are
  committed on their own.
- Every interface change in `platform-data-table-api` updates the EE remote clients in
  `server/ee/libs/platform/platform-data-table/platform-data-table-remote-client` in the same task (stubs throw
  `UnsupportedOperationException`).
- Services never call other services. Both `DataTableServiceImpl` and `DataTableRowServiceImpl` use
  `DataTableLinkColumnRepository` directly.
- Controller tests are WebMvc IntTests. Platform behavior is covered by IntTests on PostgreSQL (Testcontainers) and
  H2 (`*H2IntTest`).
- Constraint and index names: `fk_dt_link_<registryRowId>` and `ix_dt_link_<registryRowId>`.
- Error types continue `DataTableErrorType` numbering: `LINK_TARGET_INVALID` 117, `LINK_DISPLAY_COLUMN_INVALID` 118,
  `LINKED_ROW_NOT_FOUND` 119, `ROW_STILL_LINKED` 120.
- A written link value is either a number (or a numeric string): the target row id, or a map
  `{"externalId": "<value>"}`.
- An expanded link value is the target's `DataTableRow(id, externalId, values)`; an empty link expands to `null`.
- Run `./gradlew -p <module> spotlessApply` on touched modules, and `npm run format` plus a scoped
  `npx eslint`/`npx tsc --noEmit` on touched client files (no full client lint runs).

## Review Focus

1. **Bulk deletes against `BLOCK`:** `deleteRows(conditions)` and `deleteAllRows` on a target table with linking
   rows must throw `ROW_STILL_LINKED` and leave every row in place. They must not delete the unlinked rows and stop
   at the first linked one. Test in Task 4.
2. **Batch inserts with one bad link:** `insertRows` with one missing target must fail with `LINKED_ROW_NOT_FOUND`
   naming the row position, and insert nothing, under both `CreateStrategy` values. Test in Task 4.
3. **Numeric strings:** GraphQL `Map` values from the client arrive as strings. `"42"` must link to row 42, and
   `"abc"` must fail with `ROW_VALUE_INVALID`, not a SQL error. Test in Task 4.
4. **Dialect-specific violations:** foreign-key violations must be recognized by SQLState `23503` on both
   PostgreSQL and H2, not by message text. Covered by running Task 4's tests in both `DataTableLinkRowIntTest` and
   `DataTableLinkRowH2IntTest`.
5. **Expanding cleared or empty links:** after a `CLEAR` delete, or for a row whose link was never set, `expand`
   returns `null` for that column and does not fail. Test in Task 6.

---

### Task 1: Changelog for the link column registry

**Files:**
- Create: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/resources/config/liquibase/changelog/platform/data_table/20261115000001_platform_data_table_link_column.xml`

**Interfaces:**
- Produces: table `data_table_link_column(id BIGSERIAL PK, data_table_id BIGINT NOT NULL FK data_table(id),
  environment_id BIGINT NOT NULL, column_name VARCHAR(255) NOT NULL, target_data_table_id BIGINT NOT NULL
  FK data_table(id), on_delete VARCHAR(16) NOT NULL, display_column_name VARCHAR(255) NULL, created_date,
  created_by, last_modified_date, last_modified_by, version INT NOT NULL)`, unique constraint
  `uk_data_table_link_column` on `(data_table_id, environment_id, column_name)`, index on
  `(target_data_table_id, environment_id)`.

- [ ] **Step 1:** Write the changelog with `logicalFilePath` equal to its classpath path, changeSet ids
  `20261115000001-1` (createTable + unique constraint) and `20261115000001-2` (index). Copy the column types and
  audit-column conventions from `00000000000001_platform_data_table_init.xml`. No `<!-- -->` comments.
- [ ] **Step 2:** Run `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableServiceIntTest'`. Expected: PASS (the changelog applies on PostgreSQL).
- [ ] **Step 3:** Commit: `5746 Add changelog for the data table link column registry`.

### Task 2: Link types, registry entity and repository

**Files:**
- Create: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/LinkDeleteRule.java`
- Create: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/LinkSpec.java`
- Create: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/configuration/domain/DataTableLinkColumn.java`
- Create: `.../platform-data-table-service/src/main/java/com/bytechef/platform/data/table/configuration/repository/DataTableLinkColumnRepository.java`
- Modify: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/ColumnType.java` (add `LINK`)
- Modify: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/ColumnSpec.java`
- Modify: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/configuration/exception/DataTableErrorType.java`
- Test: `.../platform-data-table-api/src/test/java/com/bytechef/platform/data/table/domain/ColumnSpecTest.java`

**Interfaces:**
- Produces:
  - `enum LinkDeleteRule { BLOCK, CLEAR }`
  - `record LinkSpec(long targetDataTableId, LinkDeleteRule onDelete, @Nullable String displayColumnName)`; the
    compact constructor rejects a non-positive id and a null `onDelete`.
  - `record ColumnSpec(String name, ColumnType type, @Nullable LinkSpec link)`. The existing
    `ColumnSpec(String name, ColumnType type)` constructor stays and passes `null`. A `link` is required exactly
    when `type == LINK`; otherwise the constructor throws `IllegalArgumentException`.
  - `DataTableLinkColumn`: a Spring Data JDBC entity on `@Table("data_table_link_column")`, modeled on `DataTable`
    (audit fields, `@Version`). Fields: `id`, `dataTableId`, `environmentId`, `columnName`, `targetDataTableId`,
    `onDelete` (stored as the enum name), `displayColumnName`. Plus `LinkSpec toLinkSpec()`.
  - `DataTableLinkColumnRepository extends CrudRepository<DataTableLinkColumn, Long>` with:
    `List<DataTableLinkColumn> findAllByDataTableIdAndEnvironmentId(long dataTableId, long environmentId)`,
    `Optional<DataTableLinkColumn> findByDataTableIdAndEnvironmentIdAndColumnName(long dataTableId, long environmentId, String columnName)`,
    `List<DataTableLinkColumn> findAllByTargetDataTableIdAndEnvironmentId(long targetDataTableId, long environmentId)`.
  - `DataTableErrorType.LINK_TARGET_INVALID` (117), `LINK_DISPLAY_COLUMN_INVALID` (118), `LINKED_ROW_NOT_FOUND`
    (119) and `ROW_STILL_LINKED` (120).

- [ ] **Step 1: Write the failing tests** in `ColumnSpecTest`: `testLinkTypeRequiresLinkSpec` (asserts
  `new ColumnSpec("customer", ColumnType.LINK, null)` throws `IllegalArgumentException`),
  `testNonLinkTypeRejectsLinkSpec` (asserts that `STRING` with a `LinkSpec` throws), and `testTwoArgumentConstructorHasNoLink`
  (`new ColumnSpec("name", ColumnType.STRING).link()` is null).
- [ ] **Step 2:** Run `./gradlew :server:libs:platform:platform-data-table:platform-data-table-api:test --tests '*ColumnSpecTest'`. Expected: FAIL (compilation).
- [ ] **Step 3:** Implement the types above. `ColumnType.LINK` makes every exhaustive `switch` on `ColumnType` fail to
  compile. Add `LINK` to each one so the project compiles: `"BIGINT"` in `DataTableServiceImpl.sqlType`; `"bigint"` and
  `((Number) value).longValue()` in the row service's array helpers; `integer(name)` in the component's `mapColumn`;
  `1L` in `getSampleValue`. Find the rest with `./gradlew compileJava`.
- [ ] **Step 4:** Run the Step 2 command. Expected: PASS. Then run `./gradlew compileTestJava`. Expected: PASS.
- [ ] **Step 5:** Commit: `5746 Add the link column type, its delete rules and the link column registry`.

### Task 3: Link columns in the table service

**Files:**
- Modify: `.../platform-data-table-service/src/main/java/com/bytechef/platform/data/table/configuration/service/DataTableServiceImpl.java`
- Modify: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/configuration/service/DataTableService.java`
- Modify: `server/ee/libs/platform/platform-data-table/platform-data-table-remote-client/.../RemoteDataTableServiceClient.java`
- Test: `.../platform-data-table-service/src/test/java/com/bytechef/platform/data/table/configuration/service/DataTableLinkColumnIntTest.java`
- Test: `.../platform-data-table-service/src/test/java/com/bytechef/platform/data/table/configuration/service/DataTableLinkColumnH2IntTest.java`

**Interfaces:**
- Consumes: Task 2 types.
- Produces:
  - The constructor becomes `DataTableServiceImpl(DataTableLinkColumnRepository dataTableLinkColumnRepository, DataTableRepository dataTableRepository, JdbcTemplate jdbcTemplate)`.
  - `List<DataTableLinkColumn> getLinksTo(long targetDataTableId, long environmentId)` on `DataTableService`.
  - `addColumn`, `createTable`, `fetchDataTableInfo`, `listTables` and `listAllTables` accept and report
    `ColumnSpec`s whose `type` is `LINK` and whose `link` is set. A `bigint` column with no registry row still
    reports as `INTEGER`.

- [ ] **Step 1: Write the failing tests** in `DataTableLinkColumnIntTest`. Use `DataTableIntTestUtils.dropAll` in
  teardown. Create `customers` (STRING `name`) and `orders` in one workspace and environment.
  - `testAddLinkColumnReportsLinkSpec`: after `addColumn(ordersId, new ColumnSpec("customer", LINK, new LinkSpec(customersId, BLOCK, "name")), env)`, `fetchDataTableInfo(ordersId, env)` has that exact spec.
  - `testAddLinkColumnRejectsTargetInAnotherWorkspace`: throws `DataTableException` with `LINK_TARGET_INVALID`.
  - `testAddLinkColumnRejectsTargetMissingInEnvironment`: same error. The source has no new column afterwards.
  - `testAddLinkColumnRejectsSelfLink`: same error.
  - `testAddLinkColumnRejectsUnknownDisplayColumn`: `LINK_DISPLAY_COLUMN_INVALID`.
  - `testLinksInBothDirectionsAreAllowed`.
  - `testRenameLinkColumnKeepsLink`: after `renameColumn`, the info reports the new name with the same `LinkSpec`.
  - `testRemoveLinkColumnDeletesRegistryRow`: `getLinksTo(customersId, env)` is empty afterwards.
  - `testRenameDisplayColumnUpdatesLinks` and `testRemoveDisplayColumnClearsDisplayColumn`: only the registry rows
    of the same environment change.
  - `testDropTargetTableIsBlockedWhileLinked`: `ROW_STILL_LINKED`. The target still exists.
  - `testDropSourceTableDeletesItsLinks`.
  - `testDuplicateSourceTableCopiesLinks`: the copy reports the same `LinkSpec`.
  - `testCreateTableInNewEnvironmentWithLinkColumn`: `createTable` with a `LINK` spec in a second environment.
  - `testRenameTargetTableKeepsLinks`.

  `DataTableLinkColumnH2IntTest` extends the same scenarios on H2, following `DataTableServiceH2IntTest`.
- [ ] **Step 2:** Run `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableLinkColumn*'`. Expected: FAIL.
- [ ] **Step 3:** Implement the behavior in `DataTableServiceImpl`.
  - Add a link column in one transaction: save the registry row, then
    `ALTER TABLE ... ADD COLUMN "<name>" BIGINT`, then
    `ALTER TABLE ... ADD CONSTRAINT "fk_dt_link_<id>" FOREIGN KEY ("<name>") REFERENCES "dt_<env>_<target>"("id") ON DELETE RESTRICT|SET NULL`,
    then `CREATE INDEX "ix_dt_link_<id>" ON ... ("<name>")`.
  - `listColumns` overlays the registry rows of `(dataTableId, environmentId)`.
  - The guards in `dropTable`, `removeColumn` and `renameColumn` use `findAllByTargetDataTableIdAndEnvironmentId`.
    A display-column rename or removal updates only the matching registry rows.
- [ ] **Step 4:** Run the Step 2 command, then the module's full `test testIntegration`. Expected: PASS.
- [ ] **Step 5:** Commit: `5746 Add, rename and remove link columns and guard their target tables`.

### Task 4: Writing and deleting rows through links

**Files:**
- Modify: `.../platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java`
- Test: `.../platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableLinkRowIntTest.java`
- Test: `.../platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableLinkRowH2IntTest.java`

**Interfaces:**
- Consumes: Task 3's link columns; `DataTableLinkColumnRepository`.
- Produces:
  - `DataTableRowServiceImpl(ApplicationEventPublisher applicationEventPublisher, DataTableLinkColumnRepository dataTableLinkColumnRepository, JdbcTemplate jdbcTemplate)`.
  - Writes accept the two link value forms (see Global Constraints).
  - Foreign-key violations (SQLState `23503`) become `DataTableException`. A write gets `LINKED_ROW_NOT_FOUND`, a
    delete gets `ROW_STILL_LINKED`. The message names the source table id, the column and, for batches, the row
    position, as the existing `withRowPosition` does.
  - Filters on a `LINK` column allow `EQUALS`, `NOT_EQUALS`, `IN`, `EMPTY` and `NOT_EMPTY`; other operations throw
    `FILTER_INVALID`.
  - Sorting by a `LINK` column sorts by id.

- [ ] **Step 1: Write the failing tests.** Fixture: `customers` with external ids `c-1` and `c-2`, and `orders` with
  BLOCK link `customer`.
  - `testInsertRowLinksById`.
  - `testInsertRowLinksByExternalId`: `values.get("customer")` equals `customers` row `c-1`'s id.
  - `testInsertRowLinksByNumericString` (`"42"`-style value). The Review Focus 3 case,
    `testInsertRowRejectsNonNumericLinkValue`, gets `ROW_VALUE_INVALID`.
  - `testInsertRowRejectsMissingTargetId` and `testInsertRowRejectsUnknownExternalId`: `LINKED_ROW_NOT_FOUND`.
  - `testInsertRowsWithOneMissingTargetInsertsNothing`, run for `CreateStrategy.INSERT` and `UPSERT`
    (Review Focus 2).
  - `testUpdateRowAndUpsertRowResolveLinks`.
  - `testDeleteLinkedRowIsBlocked`: `ROW_STILL_LINKED`, and the message contains `orders` and `customer`.
  - `testDeleteRowsByConditionsIsBlockedAndDeletesNothing` and `testDeleteAllRowsIsBlockedAndDeletesNothing`
    (Review Focus 1). `countRows` is unchanged.
  - `testClearRuleSetsLinksToNull`: a second link column with `CLEAR`.
  - `testFilterByLinkEqualsAndIn`, `testFilterByLinkRejectsGreater` (`FILTER_INVALID`), `testSortByLink`.

  The H2 class runs the same scenarios.
- [ ] **Step 2:** Run `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableLinkRow*'`. Expected: FAIL.
- [ ] **Step 3:** Implement the behavior.
  - Resolve each `{"externalId": ...}` value with one `SELECT "id", "external_id" ... WHERE "external_id" IN (...)`
    per link column per call, before building the statement.
  - Translate violations where `DataIntegrityViolationException`s are already caught. Read the SQLState from the
    root `SQLException`.
- [ ] **Step 4:** Run the Step 2 command, then the module's full `test testIntegration`. Expected: PASS.
- [ ] **Step 5:** Commit: `5746 Write rows through link columns and enforce their delete rules`.

### Task 5: Webhook events for cleared links

**Files:**
- Modify: `DataTableRowServiceImpl.java` (as in Task 4)
- Test: `.../platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableLinkClearEventIntTest.java`

**Interfaces:**
- Consumes: `publishWebhookEvent(...)` and `DataTableWebhookEvent(long dataTableId, DataTableWebhookType type, List<DataTableRow> rows, long environmentId)`.
- Produces: deleting target rows publishes a `RECORD_UPDATED` event for each source table that has a `CLEAR` link to
  them, carrying the source rows after the change. Events are published only after the delete succeeds.

- [ ] **Step 1: Write the failing tests**, capturing events with `@RecordApplicationEvents`:
  - `testDeleteTargetRowPublishesUpdatedEventForClearedRows`: one `RECORD_UPDATED` event for `orders`, whose rows
    have `customer == null`.
  - `testBlockedDeletePublishesNoEvents`.
  - `testDeleteWithoutLinkingRowsPublishesNoUpdatedEvent`.
- [ ] **Step 2:** Run `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableLinkClearEventIntTest'`. Expected: FAIL.
- [ ] **Step 3:** In `deleteMatchingRows` and `deleteRow`, select the ids of the source rows to be cleared before
  deleting (for each `CLEAR` registry row targeting this table and environment). After the delete, re-read those rows
  and publish one event per source table.
- [ ] **Step 4:** Run the Step 2 command. Expected: PASS.
- [ ] **Step 5:** Commit: `5746 Publish update events for rows whose links a delete clears`.

### Task 6: Expanding links when reading rows

**Files:**
- Modify: `.../platform-data-table-api/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowService.java`
- Modify: `DataTableRowServiceImpl.java`
- Modify: the EE `RemoteDataTableRowServiceClient.java`
- Test: `.../platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableLinkExpandIntTest.java`

**Interfaces:**
- Produces two new overloads; the existing ones delegate with `List.of()`:
  - `@Nullable DataTableRow getRow(long dataTableId, long id, List<String> expand, long environmentId)`
  - `List<DataTableRow> findRows(long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit, int offset, List<String> expand, long environmentId)`

  A listed link column's value becomes the target `DataTableRow`, or `null`. Naming a non-link or unknown column
  throws `COLUMN_NOT_FOUND`.

- [ ] **Step 1: Write the failing tests:**
  - `testFindRowsExpandsLink`: the value is a `DataTableRow` with the customer's id, external id and `name`.
  - `testExpandUsesOneQueryPerLinkColumn`: wrap the `DataSource` in a counting proxy, or assert through a
    statement-count `JdbcTemplate` spy. 50 orders and 1 expanded column give exactly 2 selects.
  - `testExpandOfEmptyLinkIsNull` and `testExpandAfterClearDeleteIsNull` (Review Focus 5).
  - `testExpandRejectsNonLinkColumn`.
  - `testGetRowExpandsLink`.
- [ ] **Step 2:** Run `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableLinkExpandIntTest'`. Expected: FAIL.
- [ ] **Step 3:** After loading the page, collect the non-null ids per expanded column, fetch them with one
  `WHERE "id" IN (...)` per column, and substitute.
- [ ] **Step 4:** Run the Step 2 command. Expected: PASS.
- [ ] **Step 5:** Commit: `5746 Expand link columns into their target rows`.

### Task 7: CSV export and import of link columns

**Files:**
- Modify: `DataTableRowServiceImpl.java` (`exportCsv`, `importCsv`)
- Test: `.../platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableLinkCsvIntTest.java`

**Interfaces:**
- Produces: for each link column `customer`, export writes the columns `customer` and `customer.externalId`. When
  the `customer.externalId` header is present, import resolves through it and ignores `customer`. An empty external
  id means an empty link. Without that header, import uses `customer` as ids.

- [ ] **Step 1: Write the failing tests:**
  - `testExportWritesIdAndExternalIdColumns`: exact header line `id,external_id,customer,customer.externalId`.
  - `testRoundTripIntoAnotherEnvironmentLinksByExternalId`: the export from env 0, imported into env 1 where the
    customers have the same external ids but other row ids, links to env 1's rows.
  - `testImportWithUnknownExternalIdFailsWholeImport`: `LINKED_ROW_NOT_FOUND`, row count unchanged.
  - `testImportWithIdsOnly`.
- [ ] **Step 2:** Run `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableLinkCsvIntTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Reuse Task 4's batch external-id resolution.
- [ ] **Step 4:** Run the Step 2 command, then the module's full `test testIntegration`. Expected: PASS.
- [ ] **Step 5:** Commit: `5746 Export and import link columns with their target external ids`.

### Task 8: GraphQL

**Files:**
- Modify: `server/libs/automation/automation-data-table/automation-data-table-graphql/src/main/resources/graphql/data-table.graphqls`
- Modify: `.../automation-data-table-graphql/src/main/resources/graphql/data-table-row.graphqls`
- Modify: `.../automation-data-table-graphql/src/main/java/com/bytechef/automation/data/table/web/graphql/DataTableGraphQlController.java`
- Modify: `.../DataTableRowGraphQlController.java`
- Modify: `server/libs/automation/automation-data-table/automation-data-table-api/.../WorkspaceDataTableFacade.java` and `automation-data-table-service/.../WorkspaceDataTableFacadeImpl.java` (pass-throughs for `expand` and link options), and the EE `RemoteWorkspaceDataTableFacadeClient`
- Test: `.../automation-data-table-graphql/src/test/java/.../DataTableGraphQlControllerIntTest.java`, `DataTableRowGraphQlControllerIntTest.java` (add cases)

**Interfaces:**
- Produces the schema:
  - `enum ColumnType { ... LINK }`
  - `enum LinkDeleteRule { BLOCK CLEAR }`
  - `type DataTableColumnLink { targetDataTableId: ID! targetDataTableName: String! onDelete: LinkDeleteRule! displayColumnName: String }`
  - `DataTableColumn.link: DataTableColumnLink`
  - `input ColumnLinkInput { targetDataTableId: ID! onDelete: LinkDeleteRule! displayColumnName: String }`
  - `ColumnInput.link: ColumnLinkInput`
  - `dataTableRows(..., expand: [String!])` and `dataTableRowsPage(..., expand: [String!])`
  - `type DataTableLinkOption { id: ID! label: String! }`
  - `dataTableLinkOptions(environmentId: ID!, tableId: ID!, columnName: String!, search: String, limit: Int): [DataTableLinkOption!]!`

  `label` is the target's display column value, or `#<id>` when there is none. `search` is a case-insensitive
  `CONTAINS` on the display column. `limit` defaults to 20.
- Produces: `List<DataTableLinkOption> getLinkOptions(long dataTableId, String columnName, @Nullable String search, int limit, long environmentId)` on `WorkspaceDataTableFacade`, plus a record `DataTableLinkOption(long id, String label)` in automation-data-table-api.

- [ ] **Step 1: Write the failing tests:**
  - `testAddDataTableColumnWithLink`: asserts on the `link` fields of the `dataTables` query.
  - `testDataTableRowsExpand`.
  - `testDataTableLinkOptionsSearchesDisplayColumn`: three customers, search `"an"`, labels in id order.
  - `testDataTableLinkOptionsWithoutDisplayColumnUseHashId`.
- [ ] **Step 2:** Run `./gradlew :server:libs:automation:automation-data-table:automation-data-table-graphql:testIntegration`. Expected: FAIL.
- [ ] **Step 3:** Implement the schema, controller mappings and facade pass-throughs. The facade checks workspace
  ownership as the existing methods do.
- [ ] **Step 4:** Run the Step 2 command and the `automation-data-table-service` `test testIntegration`. Expected: PASS.
- [ ] **Step 5:** Commit: `5746 Expose link columns, expanded rows and link options over GraphQL`.

### Task 9: Public REST

**Files:**
- Modify: `server/ee/libs/automation/automation-data-table/automation-data-table-public-rest/openapi.yaml`
- Modify: `.../src/main/java/com/bytechef/ee/automation/data/table/public_/web/rest/DataTableApiController.java`, `DataTableRowApiController.java`, `DataTableModelMapper.java`, `AbstractDataTableApiController.java`
- Test: `.../src/test/java/.../DataTableApiControllerIntTest.java`, `DataTableRowApiControllerIntTest.java`
- Generated: `.../generated/**` (own commit)

**Interfaces:**
- Produces:
  - `ColumnType` enum gains `LINK`.
  - Schema `ColumnLink { targetDataTableId: integer(int64), onDelete: enum [BLOCK, CLEAR], displayColumnName?: string }`.
  - `DataTableColumn.link` and `CreateColumnRequest.link` (optional `ColumnLink`).
  - Query parameter `expand` (array of string, `explode: false`, comma-separated) on get-row and list-rows.
  - The error handler maps `LINKED_ROW_NOT_FOUND`, `LINK_TARGET_INVALID` and `LINK_DISPLAY_COLUMN_INVALID` to 400,
    and `ROW_STILL_LINKED` to 409, with those `errorKey`s.

- [ ] **Step 1: Write the failing tests:**
  - `testCreateLinkColumn`
  - `testGetTableReportsLink`
  - `testListRowsExpand` (`?expand=customer`)
  - `testInsertRowWithUnknownLinkReturns400` (`errorKey` `LINKED_ROW_NOT_FOUND`)
  - `testDeleteLinkedRowReturns409` (`errorKey` `ROW_STILL_LINKED`)
- [ ] **Step 2:** Run `./gradlew :server:ee:libs:automation:automation-data-table:automation-data-table-public-rest:testIntegration`. Expected: FAIL.
- [ ] **Step 3:** Edit `openapi.yaml`, then immediately run `./gradlew -p server/ee/libs/automation/automation-data-table/automation-data-table-public-rest generateOpenAPI`. Revert generated files whose only change is the `@Generated` date. Implement the controllers and mapper.
- [ ] **Step 4:** Run the Step 2 command and the module's `test`. Expected: PASS.
- [ ] **Step 5:** Commit the hand-written files as `5746 Expose link columns in the public data table API`, then the
  generated files as `5746 Generate the public data table REST sources`.

### Task 10: Data-table component

**Files:**
- Modify: `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/util/DataTableUtils.java`
- Modify: `.../action/DataTableGetRecordAction.java`, `DataTableFindRecordsAction.java`
- Test: `.../src/test/java/com/bytechef/component/datatable/action/DataTableGetRecordActionTest.java`, `DataTableFindRecordsActionTest.java`, `util/DataTableUtilsTest.java`
- Generated: `src/test/resources/definition/data-table_v1.json` (own commit)

**Interfaces:**
- Consumes: Task 6's row service overloads.
- Produces:
  - Property `expand`: an array of strings. Its options come from the selected table's `LINK` columns, through a new
    `DataTableUtils.getActionLinkColumnOptions(...)` modeled on `getActionColumnOptions`. Get Record and Find Records
    pass it to the row service.
  - `mapColumn` maps `LINK` to `integer(name)`, with description `Target row id, or {"externalId": "..."}`.
  - `rowObjectSchema` maps an expanded column to an object with `id` (integer), `externalId` (string) and `values`
    (object).
  - `getSampleValue(LINK)` returns `1L`.

- [ ] **Step 1: Write the failing tests:**
  - `testFindRecordsPassesExpand` (verify on the mocked row service)
  - `testGetRecordPassesExpand`
  - `testLinkColumnOptionsListOnlyLinkColumns`
  - `testRowObjectSchemaDescribesExpandedLink`
- [ ] **Step 2:** Run `./gradlew -p server/libs/modules/components/data-table test`. Expected: FAIL.
- [ ] **Step 3:** Implement the properties and pass-throughs.
- [ ] **Step 4:** Delete `src/test/resources/definition/data-table_v1.json` and `build/resources/test/definition/`,
  then rerun the Step 2 command. Expected: PASS, and the JSON is regenerated.
- [ ] **Step 5:** Commit the code as `5746 Expand link columns in the data table component`, then the JSON as
  `5746 Generate the data table component definition`.

### Task 11: Client

**Files:**
- Create: `client/src/graphql/automation/datatable/dataTableLinkOptions.graphql`
- Modify: `client/src/graphql/automation/datatable/addDataTableColumn.graphql`, `dataTables.graphql` (select `link { ... }`), and the rows query files (pass `expand`)
- Generated: `client/src/shared/middleware/graphql.ts` (own commit, `npx graphql-codegen`)
- Modify: `client/src/pages/automation/datatable/components/AddDataTableColumnDialog.tsx` (+ its hook)
- Create: `client/src/pages/automation/datatable/components/cell-renderers/LinkCellRenderer.tsx`
- Create: `client/src/pages/automation/datatable/components/cell-renderers/hooks/useLinkCellRenderer.ts`
- Modify: `.../cell-renderers/index.ts`, `.../cells/ColumnHeaderCell.tsx`, `.../ImportDataTableCsvDialog.tsx`
- Test: `.../components/tests/AddDataTableColumnDialog.test.tsx`, `.../cell-renderers/tests/LinkCellRenderer.test.tsx`, `.../cells/tests/ColumnHeaderCell.test.tsx`, `.../components/tests/ImportDataTableCsvDialog.test.tsx`

**Interfaces:**
- Consumes: Task 8's schema.
- Produces:
  - **Add column dialog:** choosing `LINK` shows:
    - a target table select listing the workspace's tables except the current one;
    - a display column select listing the target's columns except LINK columns, plus a "None" option;
    - radio buttons labeled exactly "Prevent deleting linked rows" (`BLOCK`, the default) and "Clear the link" (`CLEAR`).
  - **Grid:** the rows query asks for `expand` with all link columns.
  - **Link cell:** shows the expanded row's display column value, or `#<id>`. Editing opens a searchable picker backed
    by `dataTableLinkOptions` (debounced search) and a clear action that sends `null`.
  - **Column header:** `LinkIcon` from `lucide-react`, with the target table name in a tooltip.
  - **CSV import dialog:** when the table has link columns, shows the warning "Row ids differ between environments.
    Include <column>.externalId columns to link rows reliably."

- [ ] **Step 1: Write the failing tests:**
  - `AddDataTableColumnDialog`:
    - `it('shows target table, display column and delete rule when LINK is chosen')`
    - `it('sends the link input with BLOCK by default')` asserts the mutation variables
      `{column: {link: {displayColumnName: 'name', onDelete: 'BLOCK', targetDataTableId: '7'}, name: 'customer', type: 'LINK'}}`.
  - `LinkCellRenderer`:
    - `it('renders the display value of the expanded row')`
    - `it('renders #id without a display column')`
    - `it('sends null when cleared')`
  - `ColumnHeaderCell`: `it('shows a link icon for link columns')`.
  - `ImportDataTableCsvDialog`: `it('warns about row ids when the table has link columns')`.
- [ ] **Step 2:** Run `cd client && npx vitest run src/pages/automation/datatable`. Expected: FAIL.
- [ ] **Step 3:** Add the `.graphql` operations, run `npx graphql-codegen`, and commit `graphql.ts` alone as
  `5746 client - Generate`. Then implement the UI. Follow the CLAUDE.md client rules: sorted keys, `twMerge`,
  `*Icon` imports, the hook order, interfaces ending in `I`/`Props`.
- [ ] **Step 4:** Run the Step 2 command, then `npx eslint` and `npx tsc --noEmit -p .` limited to the changed files.
  Expected: PASS.
- [ ] **Step 5:** Commit: `5746 client - Add link columns to data tables`.

### Task 12: Docs and full verification

**Files:**
- Modify: the data tables documentation page under `docs/` that describes column types (find it with
  `grep -rl "DATE_TIME" docs/content`), and the public API reference generated from `openapi.yaml` if the docs build
  embeds it.

- [ ] **Step 1:** Document the `LINK` type, the two delete rules, writing by external id, `expand` and the CSV
  columns. This is user-facing prose, so no code comments are involved.
- [ ] **Step 2:** Run `./gradlew compileTestJava` and, for every module touched in Tasks 1-10, `test testIntegration spotlessCheck checkstyleMain checkstyleTest pmdMain pmdTest spotbugsMain`. Expected: all green.
- [ ] **Step 3:** Commit: `5746 docs - Document data table link columns`.
