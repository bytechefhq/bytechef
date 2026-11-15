# Data Table Link Columns: Design

Status: draft, 2026-11-15. Builds on branch `5746` (PR #5811): id-addressed, workspace-scoped data tables.

## Intent

Data table users need control over their data. Today a row can refer to a row in another table only by storing
that row's `id` or `external_id` in an `INTEGER` or `STRING` column. Nothing stops the referenced row from being
deleted, and nothing checks that the number points anywhere. The database cannot help because it does not know the
column is a reference.

A **link column** makes the reference explicit. Each row of the table that owns the column (the *source* table)
points to at most one row of another table in the same workspace (the *target* table). The database enforces that
the link is valid. The user decides, per column, what happens when someone deletes a linked row.

## Goals

- A new column type, `LINK`. Its value is the id of one row of the target table, or empty.
- Enforcement in the database. No API, workflow, CSV import or direct REST call can store a link to a missing row.
- A per-column delete rule, chosen when the column is created:
  - `BLOCK` (default): deleting a target row that any source row links to fails with a clear error.
  - `CLEAR`: the delete succeeds and the links to that row become empty.
- When writing rows, a link can be set by target row `id` or by target row `external_id`.
- Rows can be filtered by a link column's value (the target row id), using the existing conditions API.
- An opt-in **expand** when reading rows: Get Record, Find Records, and the REST row endpoints can return the
  linked row inline, one level deep.
- A row picker in the client that shows the target row's display column (for example the customer's name, not `42`).

## Non-goals (first version)

- Many-to-many links. They need hidden junction tables and two-sided editing.
- A `CASCADE` delete rule.
- Joins or filtering on the target row's columns (for example "orders whose customer's country is HR").
- Expanding more than one level.
- Links across workspaces or across environments.
- Other column rules such as required values or uniqueness.
- A general registry for all column types.
- Converting an existing `INTEGER` or `STRING` column into a link column. Users add a new link column and fill it
  (for example with an Update Records workflow). Conversion is a candidate follow-up.

## Anti-goals

These would make the feature a failure even if it technically works:

- **Silent mass data loss.** One delete must never remove or change many rows without a trace. This is why there is
  no `CASCADE`, and why `CLEAR` is not the default.
- **Enforcement that can be bypassed.** A rule enforced only in the Java service, which a bulk insert, CSV import or
  migration can get around, does not give control.
- **Links that break on rename.** Renaming the target table or the display column must not break a link column.

## Constraints

- A table's physical instance in an environment is `dt_<environmentId>_<dataTableId>`, built by
  `DataTablePhysicalNameUtils`. All of a workspace's tables in one environment live in the same database schema, so
  a real foreign key between two physical tables is possible.
- Column types are not stored anywhere. `DataTableServiceImpl.listColumns` reads them back from
  `information_schema.columns` and maps SQL types to `ColumnType`. `bigint` is currently read as `INTEGER`.
- Both PostgreSQL and H2 must be supported. The existing code already branches on `DatabaseDialect.isH2`.
- The `data_table` registry row is shared by every environment. A table can exist physically in some environments
  and not in others.
- Columns are per environment. `addColumn`, `renameColumn` and `removeColumn` take an `environmentId` and change one
  physical table, and `createTable` in a second environment takes its own column list, so the same table can have
  different columns in DEV and PROD.
- Webhook triggers (`DataTableRecordCreated/Updated/DeletedTrigger`) fire from `DataTableWebhookEvent`s that the row
  service publishes. Changes the database makes on its own, such as `ON DELETE SET NULL`, publish no event.
- Liquibase changelogs go in their own commit, before the code that needs them.
- Released changelogs must not be edited.

## Approaches considered

1. **The foreign key is the only record.** The target table and delete rule are read back from the database catalog
   (`information_schema.referential_constraints` / `key_column_usage`). There is no new table. The catalog queries
   differ between PostgreSQL and H2, each environment's copy of the constraint can drift, and there is nowhere to
   keep the display column.
2. **A registry for link columns only, with the foreign key enforcing.** *(chosen)* A new table holds one row per
   link column per environment, matching how columns already work per environment. The physical table carries a
   real foreign key built from that row. The settings are explicit and identical on both databases. There is room for the display column. The
   cost is that column create, rename and drop must keep the registry row and the physical tables in step.
3. **A registry for every column of every type.** This would stop reading types from `information_schema` and open
   the door to descriptions, ordering and required flags. Every existing table would need migrating, and the scope
   goes well beyond links.

The user picked approach 2. It can grow into approach 3 later if column metadata becomes a broader need.

## Design

### Data model

New registry table `data_table_link_column`, added by a new Liquibase changelog in `platform/data_table`:

| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL | primary key |
| `data_table_id` | BIGINT, not null | source table, FK to `data_table(id)` |
| `environment_id` | BIGINT, not null | the environment whose physical table holds the column |
| `column_name` | VARCHAR(255), not null | the physical column name in the source table |
| `target_data_table_id` | BIGINT, not null | FK to `data_table(id)` |
| `on_delete` | VARCHAR(16), not null | `BLOCK` or `CLEAR` |
| `display_column_name` | VARCHAR(255), null | a user column of the target table; null means show the id |
| audit columns + `version` | | same as `data_table` |

Unique key: `(data_table_id, environment_id, column_name)`. *(Builder's call, confirmed in review: per environment,
because columns already are. A table-wide registry would fight `addColumn`/`renameColumn`/`removeColumn`, which
change one environment, and would need rules for display columns renamed in only one environment.)*

The target table must be in the same workspace as the source table. The service checks this when the column is
created, because two nullable `workspace_id` values cannot be compared by a constraint.

In the environment's physical source table:

- The link column is `BIGINT`, nullable.
- It has a foreign key `fk_dt_link_<registryRowId>` to `dt_<env>_<targetId>("id")` (named by the registry row
  id because PostgreSQL identifiers are capped at 63 characters), with
  `ON DELETE RESTRICT` for `BLOCK` and `ON DELETE SET NULL` for `CLEAR`.
- It has an index on the link column. PostgreSQL does not index foreign key columns on its own, and a delete on the
  target table would otherwise scan the source table.

`ColumnSpec` gets an optional `LinkSpec link` part: `(long targetDataTableId, LinkDeleteRule onDelete,
@Nullable String displayColumnName)`. It is required when `type == LINK` and must be null otherwise. A new
`LinkDeleteRule` enum holds `BLOCK` and `CLEAR`.

When the columns of a table are read, a column that has a `data_table_link_column` row is reported as `LINK` with
its `LinkSpec`. Every other `bigint` column keeps being reported as `INTEGER`.

### Environments

A link always points into the same environment: the column in `dt_2_5` references `dt_2_7`. Every link operation
works on one environment, like every other column operation.

- **Adding a link column** in an environment: the target table must exist physically in that environment, or the
  request fails with `LINK_TARGET_INVALID`.
- **Creating the source table in a new environment** (`createTable` reusing the registry row): link columns in the
  caller's column list are validated and created like an added link column. Nothing is copied from other
  environments.
- **Dropping the target table in an environment** while source tables in that environment link to it: blocked with
  `ROW_STILL_LINKED`, naming the linking tables and columns. The user removes those link columns first.
- **Links in both directions** (A links to B and B links to A) are allowed. Because columns are per environment,
  there is no ordering deadlock: create both tables, then add the link columns. Dropping either table requires
  removing the link columns that point at it first. *(Builder's call.)*

### Operations on tables and columns

| Operation | Behavior |
|---|---|
| Add link column | Validate the target (same workspace, exists in this environment, not the source itself in the first version) and the display column (a user column of the target in this environment). Insert the registry row, then add the column, foreign key and index. |
| Rename link column | Rename the physical column (the foreign key is named after the registry row, `fk_dt_link_<registryRowId>`, so it keeps its name), and update `column_name` in this environment's registry row. |
| Remove link column | Drop the foreign key, index and column, then delete this environment's registry row. |
| Rename target table | Nothing to do: links use ids. |
| Rename the target's display column | Update `display_column_name` in the registry rows of the same environment that use it. |
| Remove the target's display column | Set `display_column_name` to null in the registry rows of the same environment that use it. |
| Drop target table in an environment | Blocked while a link column in that environment targets it (see Environments). |
| Drop source table in an environment | That environment's registry rows are deleted along with the physical table. |
| Duplicate source table | The copy gets the same link columns pointing to the same target table, with new registry rows and foreign keys. |
| Duplicate target table | Links are not copied: existing source rows keep pointing at the original. |

A link column must not target its own table in the first version. That avoids ordering problems for
self-references in import and duplicate.

### Writing rows

`insertRow`, `insertRows`, `updateRow`, `updateRows` and `upsertRow` accept, for a link column, either:

- a number: the target row `id`, or
- an object `{"externalId": "..."}`: resolved to the target row with that `external_id` in the same environment.
  If there is none, the write fails with `LINKED_ROW_NOT_FOUND`.

The foreign key makes any write with a missing target fail. The service turns that database error into a
`DataTableException` with error type `LINKED_ROW_NOT_FOUND`, naming the column and the value.

### Deleting rows

- `BLOCK`: when a delete of target rows is refused, the service turns the constraint violation into a
  `DataTableException` with a new error type `ROW_STILL_LINKED`. The message names the source table, the column and
  the number of linking rows, for example "Row 42 is linked from 3 rows in Orders.customer".
- `CLEAR`: the database clears the links. Before deleting, the row service finds the source rows that will be
  cleared and, after the delete commits, publishes `RECORD_UPDATED` webhook events for them. Without this, triggers
  on the source table would miss the change. This happens in the row service, not in the database.

### Reading rows

- By default a link column's value is the target row id, as a number.
- With `expand` (a list of link column names, or all of them), each listed link value becomes an object:
  `{"id": 42, "externalId": "...", "values": {...}}`. Expanding is one level deep. The linked rows are fetched in one
  query per link column per page (`WHERE id IN (...)`), not one query per row.
- Filtering uses the existing `DataTableRowConditions`. A link column takes the operations of a number column
  (`EQUALS`, `NOT_EQUALS`, `IN`, `EMPTY`, `NOT_EMPTY`). Ordering operations and text operations are rejected.
- Sorting by a link column sorts by the target row id.

### CSV import and export

Row ids are per physical table: row 42 in DEV is not row 42 in PROD, and import assigns new ids. An id-based CSV
moved to another environment or re-imported would link to the wrong rows without any error, which defeats the
integrity goal. So *(builder's call)*:

- Export writes two columns per link column: `<column>` with the target row id and `<column>.externalId` with the
  target row's external id (empty when it has none).
- Import resolves a link through `<column>.externalId` when that header is present, and ignores `<column>` then.
  An empty external id in a row means an empty link.
- Import by `<column>` ids alone is accepted only into the environment and table the file was exported from. In
  practice that means only when no `.externalId` header exists, and the dialog warns that ids are not portable.
- An import row whose link target does not exist fails the whole import, the same way other invalid values do today.

### Storage limit (5809)

A `BIGINT` link column counts like any other column.

### Migrators

`DataTableWorkspaceScopedNamingMigrator.sqlType` throws on `bigint` today. It runs before any link column can exist,
so nothing needs to change. Any later migrator that copies physical tables must recreate link foreign keys from
the registry.

### APIs

**Platform service** (`DataTableService`): `addColumn(dataTableId, ColumnSpec, environmentId)` accepts the `LINK`
spec; `fetchDataTableInfo` reports `LinkSpec`s. New method `List<DataTableLinkColumn> getLinksTo(long targetDataTableId, long environmentId)` for
the blocking checks and display-column updates. `DataTableRowService.getRow/findRows` gain an `expand` parameter
(an overload, so existing callers are unchanged).

**GraphQL** (`data-table.graphqls`):
- `ColumnType` gains `LINK`.
- The column type gets an optional `link { targetDataTableId, targetDataTableName, onDelete, displayColumnName }`.
- The add-column input gets an optional `link` input.
- A new enum `LinkDeleteRule { BLOCK CLEAR }`.
- A new query `dataTableLinkOptions(dataTableId, environmentId, search, limit)` returns `{id, label}` pairs for the
  row picker.

**Public REST** (`openapi.yaml`, under `/workspaces/{workspaceId}/data-tables/{name}`):
- `ColumnType` gains `LINK`.
- The column model and the create-column request get an optional `link` object.
- The row read endpoints get an `expand` query parameter.
- New error keys: `LINKED_ROW_NOT_FOUND` and `ROW_STILL_LINKED`.

**Component** (`data-table`):
- Create/Update Record(s) show a link column as an integer input. Their description says an
  `{"externalId": "..."}` object also works.
- Get Record and Find Records get an `expand` multi-select of the table's link columns.
- Output schemas describe an expanded link as an object.

### Client

- **Add column dialog**: choosing `LINK` shows a target table picker (tables of the same workspace), a display
  column picker (user columns of the target), and the delete rule as two choices, "Prevent deleting linked rows"
  (default) and "Clear the link".
- **Grid cell**: shows the display column value of the linked row, or `#<id>` without one. Editing opens a
  searchable picker backed by `dataTableLinkOptions`. A clear action sets the value to empty.
- **Column header**: a link icon and the target table name.
- **Delete rows dialog**: a `ROW_STILL_LINKED` error is shown through the existing toast, which already displays
  GraphQL errors.

### Errors

New `DataTableErrorType` values:

| Type | When |
|---|---|
| `LINK_TARGET_INVALID` | The target table is missing, in another workspace, the source table itself, or missing in an environment where the source exists |
| `LINK_DISPLAY_COLUMN_INVALID` | The display column is not a user column of the target |
| `LINKED_ROW_NOT_FOUND` | A written link value (id or external id) matches no target row |
| `ROW_STILL_LINKED` | A `BLOCK` link prevents deleting a row, or a link column targets a table being dropped |

### Testing

- PostgreSQL and H2 IntTests in `platform-data-table-service`. Cover:
  - each table and column operation in the table above
  - both delete rules
  - writes by id and by external id
  - `expand` with one query per link column
  - filtering
  - CSV round-trip
  - environment rules
  - `CLEAR` publishing `RECORD_UPDATED` events for the cleared rows
- WebMvc IntTests for the new REST shapes and error keys, and GraphQL IntTests for the new fields.
- Component tests for `expand` and the link inputs. Regenerate the definition JSON.
- Client tests for the add-column dialog, the cell and the picker.

## Decided by the user

- Purpose: control over data (integrity), not convenience.
- Delete behavior: a per-column choice, `BLOCK` by default and `CLEAR` as the option. No `CASCADE`.
- No other rules (required, unique) in the first version.
- Converting existing columns into link columns is left out of the first version.
- Approach 2: a registry for link columns only, with database foreign keys doing the enforcement.

## Calls made while writing (check these)

- Link columns are per environment, like all columns (registry keyed by environment).
- Links in both directions are allowed. Only self-links are rejected.
- CSV export writes ids and external ids. Import prefers external ids. Id-only import comes with a portability
  warning.
- `CLEAR` publishes `RECORD_UPDATED` webhook events for the cleared source rows.
- One level of `expand`, fetched with one query per link column per page.

## Left to the builder

- Class and package names beyond those given here.
- The exact wording of error messages.
- How the client picker paginates and debounces.
- Whether `getLinksTo` lives on `DataTableService` or on a small new link service in the same module.
