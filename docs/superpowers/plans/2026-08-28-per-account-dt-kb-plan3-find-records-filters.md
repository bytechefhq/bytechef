# Data Table `findRecords` Filter Grammar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the data table `findRecords` action a real filter grammar, so a workflow can retrieve the rows it wants rather than paging the whole table.

**Architecture:** A `RowFilter` value type in `platform-data-table-api` mirrors the operator set already established by `ContextStoreQueryFilter`. A package-private `RowFilterSqlBuilder` turns a list of them into one SQL fragment plus its bound parameters, validating every field against the table's real, non-reserved columns — that validation is both the type lookup and the injection guard. `DataTableRowServiceImpl.listRows` gains one overload that splices the fragment in after the existing owner predicate. The component action exposes the grammar as an array of `{field, operator, value}` objects.

**Tech Stack:** Java 25, Spring JDBC (`JdbcTemplate` with a `PreparedStatementSetter`), JUnit 5, Testcontainers (PostgreSQL 15), ByteChef component DSL.

**Spec:** `docs/superpowers/specs/2026-08-27-per-account-data-tables-knowledge-bases-design.md`

**Predecessors:** `docs/superpowers/plans/2026-08-27-per-account-dt-kb-plan1-owner-model.md` (the owner columns) and `docs/superpowers/plans/2026-08-28-per-account-dt-kb-plan2-runtime-scoping.md` (the owner predicate this plan composes with). Both are landed on this branch.

## Global Constraints

- **Filters compose with the owner predicate; they never replace it.** Every filtered query is still `AND`-ed with `ownerPredicate(rowOwnerFilter)`. A filter must never widen a result set beyond what the owner filter allows.
- **Reserved columns are not filterable.** `ReservedColumns.isReserved(field)` ⇒ reject. `owner_id` and `owner_type` stay invisible to workflow authors, exactly as they are invisible to the grid, the row schema, and CSV.
- **Every user value is a bound parameter.** Only identifiers are interpolated, and only after passing `[a-z_][a-z0-9_]*`. This is the rule the rest of `DataTableRowServiceImpl` follows, and it is what makes its `@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")` honest.
- **Operator set, copied verbatim from `ContextStoreQueryFilter.FilterOp`:** `EQ, NEQ, IN, CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN`. No additions in this plan.
- **Java style:** blank line before control statements; no method chaining outside the sanctioned list; descriptive names; no `TODO:` comments (Checkstyle rejects them); test methods camelCase with no underscores; no trailing blank line before a class's closing brace.
- **Commit prefix convention:** `---` opens a group, `-` continues it. This plan opens one group.

## File Structure

| File | Responsibility |
|---|---|
| `platform-data-table-api/.../domain/RowFilter.java` (create) | The value type: field, operator, value. Dumb record plus enum. |
| `platform-data-table-service/.../execution/service/RowFilterSqlBuilder.java` (create) | All grammar knowledge: validation, SQL fragment, typed parameter bindings. The only file that knows what `CONTAINS` means. |
| `platform-data-table-api/.../execution/service/DataTableRowService.java` (modify) | One new `listRows` overload. |
| `platform-data-table-service/.../execution/service/DataTableRowServiceImpl.java` (modify) | Splice the fragment in; bind its parameters between the owner param and limit/offset. Make `coerceValue` static so the builder can reuse it. |
| `platform-data-table-remote-client/.../RemoteDataTableRowServiceClient.java` (modify) | Stub the new overload — it *implements* the interface, so a new method breaks its compile. |
| `data-table/.../action/DataTableFindRecordsAction.java` (modify) | Expose the grammar; parse the array into `List<RowFilter>`. |
| `data-table/.../constant/DataTableConstants.java` (modify) | `FILTERS`, `FIELD`, `OPERATOR`, `VALUE`. |

---

### Task 1: The filter value type and its SQL builder

All the risk lives here: this is the file that decides what SQL a user's filter becomes.

**Files:**
- Create: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/RowFilter.java`
- Create: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/RowFilterSqlBuilder.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java` (make `coerceValue` static)
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/RowFilterSqlBuilderTest.java`

**Interfaces:**
- Consumes: `ColumnType` (`com.bytechef.platform.data.table.domain.ColumnType`, values `STRING, BOOLEAN, INTEGER, NUMBER, DATE, DATE_TIME`), `ReservedColumns.isReserved(String)`.
- Produces:
  - `RowFilter(String field, RowFilter.Operator operator, @Nullable Object value)` with `Operator { EQ, NEQ, IN, CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN }`
  - `RowFilterSqlBuilder.Binding(ColumnType type, @Nullable Object value)`
  - `RowFilterSqlBuilder.Fragment(String sql, List<Binding> bindings)`
  - `static Fragment RowFilterSqlBuilder.build(List<RowFilter> rowFilters, Map<String, ColumnType> columnTypes)` — `columnTypes` keyed by lower-case column name, which is exactly what `DataTableRowServiceImpl.columnTypeMap` already returns.
  - `static Object DataTableRowServiceImpl.coerceValue(ColumnType, Object)` — visibility change only, `private` → package-private `static`.

- [ ] **Step 1: Write `RowFilter`**

```java
package com.bytechef.platform.data.table.domain;

import org.jspecify.annotations.Nullable;

/**
 * One condition on a data table query. Deliberately the same operator set as {@code ContextStoreQueryFilter}, so the
 * two query surfaces read alike.
 *
 * <p>
 * {@code value} is whatever the caller supplied -- a scalar for most operators, a {@code List} for {@code IN} and
 * {@code BETWEEN}, and {@code null} for {@code EQ}/{@code NEQ} to mean {@code IS NULL}/{@code IS NOT NULL}. It is
 * coerced to the column's type when the SQL is built, never before.
 *
 * @author Ivica Cardic
 */
public record RowFilter(String field, Operator operator, @Nullable Object value) {

    public enum Operator {
        EQ,
        NEQ,
        IN,
        CONTAINS,
        STARTS_WITH,
        GT,
        GTE,
        LT,
        LTE,
        BETWEEN
    }
}
```

- [ ] **Step 2: Write the failing builder test**

Create `RowFilterSqlBuilderTest.java`. Each test names the behaviour, not the method.

```java
package com.bytechef.platform.data.table.execution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.RowFilter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RowFilterSqlBuilderTest {

    private static final Map<String, ColumnType> COLUMN_TYPES = Map.of(
        "title", ColumnType.STRING,
        "score", ColumnType.INTEGER,
        "created", ColumnType.DATE_TIME);

    @Test
    void testNoFiltersProduceNoSql() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(List.of(), COLUMN_TYPES);

        assertEquals("", fragment.sql());
        assertTrue(fragment.bindings()
            .isEmpty());
    }

    @Test
    void testEqualsBindsOneParameter() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("title", RowFilter.Operator.EQ, "hello")), COLUMN_TYPES);

        assertEquals(" AND \"title\" = ?", fragment.sql());
        assertEquals(1, fragment.bindings()
            .size());
    }

    @Test
    void testEveryComparisonOperatorRendersItsSymbol() {
        assertEquals(" AND \"score\" <> ?", sqlFor(RowFilter.Operator.NEQ));
        assertEquals(" AND \"score\" > ?", sqlFor(RowFilter.Operator.GT));
        assertEquals(" AND \"score\" >= ?", sqlFor(RowFilter.Operator.GTE));
        assertEquals(" AND \"score\" < ?", sqlFor(RowFilter.Operator.LT));
        assertEquals(" AND \"score\" <= ?", sqlFor(RowFilter.Operator.LTE));
    }

    @Test
    void testValuesAreCoercedToTheColumnType() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("score", RowFilter.Operator.EQ, "42")), COLUMN_TYPES);

        assertEquals(42L, fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testANullValueBecomesAnIsNullTestWithNoParameter() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("title", RowFilter.Operator.EQ, null)), COLUMN_TYPES);

        assertEquals(" AND \"title\" IS NULL", fragment.sql());
        assertTrue(fragment.bindings()
            .isEmpty());
    }

    @Test
    void testANullValueWithNotEqualsBecomesIsNotNull() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("title", RowFilter.Operator.NEQ, null)), COLUMN_TYPES);

        assertEquals(" AND \"title\" IS NOT NULL", fragment.sql());
    }

    @Test
    void testInRendersOnePlaceholderPerValue() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("score", RowFilter.Operator.IN, List.of("1", "2", "3"))), COLUMN_TYPES);

        assertEquals(" AND \"score\" IN (?, ?, ?)", fragment.sql());
        assertEquals(3, fragment.bindings()
            .size());
    }

    @Test
    void testBetweenRendersTwoPlaceholders() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("score", RowFilter.Operator.BETWEEN, List.of("1", "9"))), COLUMN_TYPES);

        assertEquals(" AND \"score\" BETWEEN ? AND ?", fragment.sql());
        assertEquals(2, fragment.bindings()
            .size());
    }

    @Test
    void testContainsWrapsTheValueInWildcards() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "ell")), COLUMN_TYPES);

        assertEquals(" AND \"title\" LIKE ? ESCAPE '\\'", fragment.sql());
        assertEquals("%ell%", fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testStartsWithAnchorsTheWildcardAtTheEnd() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("title", RowFilter.Operator.STARTS_WITH, "he")), COLUMN_TYPES);

        assertEquals("he%", fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testLikeMetacharactersInTheValueAreEscaped() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("title", RowFilter.Operator.CONTAINS, "100%_of\\it")), COLUMN_TYPES);

        assertEquals("%100\\%\\_of\\\\it%", fragment.bindings()
            .getFirst()
            .value());
    }

    @Test
    void testFiltersAreAndedTogether() {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(
                new RowFilter("title", RowFilter.Operator.EQ, "a"),
                new RowFilter("score", RowFilter.Operator.GT, "1")),
            COLUMN_TYPES);

        assertEquals(" AND \"title\" = ? AND \"score\" > ?", fragment.sql());
    }

    @Test
    void testAnUnknownColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowFilterSqlBuilder.build(
                List.of(new RowFilter("nope", RowFilter.Operator.EQ, "x")), COLUMN_TYPES));
    }

    @Test
    void testAReservedColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowFilterSqlBuilder.build(
                List.of(new RowFilter("owner_id", RowFilter.Operator.EQ, 1L)), COLUMN_TYPES));
    }

    @Test
    void testContainsOnANonStringColumnIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowFilterSqlBuilder.build(
                List.of(new RowFilter("score", RowFilter.Operator.CONTAINS, "1")), COLUMN_TYPES));
    }

    @Test
    void testInWithAnEmptyListIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowFilterSqlBuilder.build(
                List.of(new RowFilter("score", RowFilter.Operator.IN, List.of())), COLUMN_TYPES));
    }

    @Test
    void testBetweenWithTheWrongNumberOfBoundsIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowFilterSqlBuilder.build(
                List.of(new RowFilter("score", RowFilter.Operator.BETWEEN, List.of("1"))), COLUMN_TYPES));
    }

    @Test
    void testAComparisonAgainstAListIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RowFilterSqlBuilder.build(
                List.of(new RowFilter("score", RowFilter.Operator.GT, List.of("1", "2"))), COLUMN_TYPES));
    }

    private static String sqlFor(RowFilter.Operator operator) {
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            List.of(new RowFilter("score", operator, "1")), COLUMN_TYPES);

        return fragment.sql();
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests '*RowFilterSqlBuilderTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E 'error:|^> Task .* FAILED' /tmp/t.log | head
```

Expected: compile failure — `RowFilterSqlBuilder` does not exist.

- [ ] **Step 4: Make `coerceValue` static**

In `DataTableRowServiceImpl`, change `private Object coerceValue(` to `static Object coerceValue(`. It reads no instance state, so this is a visibility change only and the existing call sites keep compiling. Package-private rather than private so `RowFilterSqlBuilder` — same package — reuses the one coercion table instead of growing a second.

- [ ] **Step 5: Write `RowFilterSqlBuilder`**

```java
package com.bytechef.platform.data.table.execution.service;

import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.domain.RowFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Turns a list of {@link RowFilter}s into one SQL fragment and the parameters it binds.
 *
 * <p>
 * The fragment always opens with {@code " AND "} so it can be appended to the {@code WHERE TRUE} that
 * {@code DataTableRowServiceImpl#listRows} already emits, after the owner predicate and before {@code ORDER BY}. It
 * narrows and never widens: filters are conjunctions, so no filter can reach a row the owner predicate excluded.
 *
 * <p>
 * A field must name a real, non-reserved column of the table. That check is what makes interpolating the column name
 * safe, and it is also what keeps {@code owner_id} and {@code owner_type} unaddressable from a workflow.
 *
 * @author Ivica Cardic
 */
final class RowFilterSqlBuilder {

    private static final String LIKE_ESCAPE = " ESCAPE '\\'";

    private RowFilterSqlBuilder() {
    }

    record Binding(ColumnType type, @Nullable Object value) {
    }

    record Fragment(String sql, List<Binding> bindings) {
    }

    static Fragment build(List<RowFilter> rowFilters, Map<String, ColumnType> columnTypes) {
        if (rowFilters.isEmpty()) {
            return new Fragment("", List.of());
        }

        StringBuilder sql = new StringBuilder();
        List<Binding> bindings = new ArrayList<>();

        for (RowFilter rowFilter : rowFilters) {
            String column = column(rowFilter, columnTypes);

            sql.append(" AND ")
                .append(quote(column));

            append(sql, bindings, rowFilter, columnTypes.get(column));
        }

        return new Fragment(sql.toString(), List.copyOf(bindings));
    }

    private static void append(
        StringBuilder sql, List<Binding> bindings, RowFilter rowFilter, ColumnType columnType) {

        Object value = rowFilter.value();

        switch (rowFilter.operator()) {
            case EQ -> appendComparison(sql, bindings, "=", "IS NULL", value, columnType);
            case NEQ -> appendComparison(sql, bindings, "<>", "IS NOT NULL", value, columnType);
            case GT -> appendComparison(sql, bindings, ">", null, value, columnType);
            case GTE -> appendComparison(sql, bindings, ">=", null, value, columnType);
            case LT -> appendComparison(sql, bindings, "<", null, value, columnType);
            case LTE -> appendComparison(sql, bindings, "<=", null, value, columnType);
            case CONTAINS -> appendLike(sql, bindings, rowFilter, columnType, "%", "%");
            case STARTS_WITH -> appendLike(sql, bindings, rowFilter, columnType, "", "%");
            case IN -> appendIn(sql, bindings, rowFilter, columnType);
            case BETWEEN -> appendBetween(sql, bindings, rowFilter, columnType);
        }
    }

    private static void appendComparison(
        StringBuilder sql, List<Binding> bindings, String operator, @Nullable String nullForm,
        @Nullable Object value, ColumnType columnType) {

        if (value == null) {
            Assert.notNull(nullForm, "A null value is only meaningful with Equals or Not Equals");

            sql.append(' ')
                .append(nullForm);

            return;
        }

        Assert.isTrue(!(value instanceof Collection<?>), "Operator " + operator + " takes a single value, not a list");

        sql.append(' ')
            .append(operator)
            .append(" ?");

        bindings.add(new Binding(columnType, DataTableRowServiceImpl.coerceValue(columnType, value)));
    }

    private static void appendLike(
        StringBuilder sql, List<Binding> bindings, RowFilter rowFilter, ColumnType columnType, String prefix,
        String suffix) {

        Assert.isTrue(
            columnType == ColumnType.STRING,
            "Operator " + rowFilter.operator() + " applies to text columns only, and '" + rowFilter.field() + "' is " +
                columnType);

        Object value = rowFilter.value();

        Assert.notNull(value, "Operator " + rowFilter.operator() + " requires a value");

        sql.append(" LIKE ?")
            .append(LIKE_ESCAPE);

        bindings.add(new Binding(ColumnType.STRING, prefix + escapeLike(String.valueOf(value)) + suffix));
    }

    private static void appendIn(
        StringBuilder sql, List<Binding> bindings, RowFilter rowFilter, ColumnType columnType) {

        List<?> values = listValue(rowFilter);

        Assert.isTrue(!values.isEmpty(), "Operator In requires at least one value");

        sql.append(" IN (");

        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                sql.append(", ");
            }

            sql.append('?');

            bindings.add(new Binding(columnType, DataTableRowServiceImpl.coerceValue(columnType, values.get(index))));
        }

        sql.append(')');
    }

    private static void appendBetween(
        StringBuilder sql, List<Binding> bindings, RowFilter rowFilter, ColumnType columnType) {

        List<?> values = listValue(rowFilter);

        Assert.isTrue(values.size() == 2, "Operator Between requires exactly two values");

        sql.append(" BETWEEN ? AND ?");

        for (Object value : values) {
            bindings.add(new Binding(columnType, DataTableRowServiceImpl.coerceValue(columnType, value)));
        }
    }

    private static List<?> listValue(RowFilter rowFilter) {
        Object value = rowFilter.value();

        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }

        throw new IllegalArgumentException(
            "Operator " + rowFilter.operator() + " requires a list of values");
    }

    private static String column(RowFilter rowFilter, Map<String, ColumnType> columnTypes) {
        String field = rowFilter.field();

        Assert.hasText(field, "A filter must name a column");

        String column = field.toLowerCase(Locale.ROOT);

        Assert.isTrue(!ReservedColumns.isReserved(column), "Column '" + field + "' cannot be filtered on");
        Assert.isTrue(columnTypes.containsKey(column), "Unknown column: " + field);

        return column;
    }

    private static String quote(String column) {
        Assert.isTrue(column.matches("[a-z_][a-z0-9_]*"), "Invalid identifier: " + column);

        return '"' + column + '"';
    }

    /**
     * Escapes the wildcards LIKE would otherwise honour, so a search for a literal {@code %} finds one. The backslash
     * goes first, or it would escape the escapes added after it.
     */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
```

`org.springframework.util.Assert` throws `IllegalArgumentException`, which is what the tests assert.

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests '*RowFilterSqlBuilderTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t.log | head
```

Expected: exit=0, no FAILED tasks.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply -q
git add server/libs/platform/platform-data-table
git commit -m "--- Give data table queries a filter grammar"
```

---

### Task 2: Apply filters in the row service

**Files:**
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowService.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java`
- Modify: `server/ee/libs/platform/platform-data-table/platform-data-table-remote-client/src/main/java/com/bytechef/ee/platform/data/table/remote/client/service/RemoteDataTableRowServiceClient.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableRowFilterIntTest.java`

**Interfaces:**
- Consumes: `RowFilterSqlBuilder.build`, `.Fragment`, `.Binding` from Task 1.
- Produces: `List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId, RowOwnerFilter rowOwnerFilter, List<RowFilter> rowFilters)`.

- [ ] **Step 1: Write the failing integration test**

Model it on `DataTableRowOwnerScopingIntTest` in the same package — read that file first for the exact `@SpringBootTest` annotations, the `DataTableIntTestConfiguration` import, and how it creates a table and rows. Mirror its setup rather than inventing one.

The test must prove three things: that a filter narrows, that filters `AND` together, and — the one that matters — that a filter cannot escape the owner predicate.

```java
    @Test
    void testAFilterNarrowsTheResult() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "alpha"), ENVIRONMENT_ID);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "beta"), ENVIRONMENT_ID);

        List<DataTableRow> rows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(new RowFilter("title", RowFilter.Operator.EQ, "alpha")));

        assertEquals(1, rows.size());
    }

    @Test
    void testFiltersAreAndedTogether() {
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "alpha", "score", 1), ENVIRONMENT_ID);
        dataTableRowService.insertRow(BASE_NAME, Map.of("title", "alpha", "score", 9), ENVIRONMENT_ID);

        List<DataTableRow> rows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted(),
            List.of(
                new RowFilter("title", RowFilter.Operator.EQ, "alpha"),
                new RowFilter("score", RowFilter.Operator.GT, "5")));

        assertEquals(1, rows.size());
    }

    @Test
    void testAFilterCannotReachAnotherOwnersRow() {
        dataTableRowService.insertRow(
            BASE_NAME, Map.of("title", "secret"), ENVIRONMENT_ID,
            RowOwnerFilter.ownedBy(Owner.connectedUser(2L)));

        List<DataTableRow> rows = dataTableRowService.listRows(
            BASE_NAME, 100, 0, ENVIRONMENT_ID, RowOwnerFilter.ownedBy(Owner.connectedUser(1L)),
            List.of(new RowFilter("title", RowFilter.Operator.EQ, "secret")));

        assertTrue(rows.isEmpty());
    }

    @Test
    void testTheParameterOrderSurvivesAnOwnerFilterAndPagination() {
        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.ownedBy(Owner.connectedUser(1L));

        for (int index = 0; index < 5; index++) {
            dataTableRowService.insertRow(
                BASE_NAME, Map.of("title", "row", "score", index), ENVIRONMENT_ID, rowOwnerFilter);
        }

        List<DataTableRow> rows = dataTableRowService.listRows(
            BASE_NAME, 2, 1, ENVIRONMENT_ID, rowOwnerFilter,
            List.of(new RowFilter("score", RowFilter.Operator.GTE, "1")));

        assertEquals(2, rows.size());
    }
```

That last test is the one that catches an off-by-one in the parameter index: owner param, then filter params, then limit and offset. A mis-ordered index throws or silently returns the wrong page, and only a real database shows it.

- [ ] **Step 2: Run it to verify it fails**

```bash
export DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableRowFilterIntTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E 'error:|^> Task .* FAILED' /tmp/t.log | head
```

Expected: compile failure — the six-argument `listRows` does not exist.

- [ ] **Step 3: Add the interface overload**

In `DataTableRowService`, directly below the existing owner-scoped `listRows`:

```java
    /**
     * Owner-scoped and filtered form. Filters narrow within what {@code rowOwnerFilter} already allows; they can never
     * widen it. See {@link RowFilter}.
     */
    List<DataTableRow> listRows(
        String baseName, int limit, int offset, long environmentId, RowOwnerFilter rowOwnerFilter,
        List<RowFilter> rowFilters);
```

Add `import com.bytechef.platform.data.table.domain.RowFilter;`.

- [ ] **Step 4: Implement it**

In `DataTableRowServiceImpl`, make the existing five-argument `listRows` delegate with `List.of()`, and move its body into the new six-argument method with two changes — build the fragment, and splice it in:

```java
        RowFilterSqlBuilder.Fragment fragment = RowFilterSqlBuilder.build(
            rowFilters, columnTypeMap(buildPhysicalName));

        String sql =
            "SELECT " + selectColumns + " FROM " + escapeIdentifier(buildPhysicalName) +
                " WHERE TRUE" + ownerPredicate(rowOwnerFilter) + fragment.sql() +
                " ORDER BY \"id\" LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, ps -> {
            int index = setOwnerParam(ps, 1, rowOwnerFilter);

            for (RowFilterSqlBuilder.Binding binding : fragment.bindings()) {
                setParam(ps, index++, binding.type(), binding.value());
            }

            ps.setInt(index++, Math.max(0, limit));
            ps.setInt(index, Math.max(0, offset));
        }, (rs, rowNum) -> {
            long id = rs.getLong("id");
            Map<String, Object> values = new HashMap<>();

            for (String column : columnNames) {
                values.put(column, rs.getObject(column));
            }

            return new DataTableRow(id, values);
        });
```

The rule to keep in mind: the SQL fragments concatenate as owner, filters, pagination, so the bindings must be set in that same order.

- [ ] **Step 5: Stub the remote client**

`RemoteDataTableRowServiceClient` implements `DataTableRowService`, so it breaks. Follow the shape of the stubs the previous plan added there:

```java
    @Override
    public List<DataTableRow> listRows(
        String baseName, int limit, int offset, long environmentId, RowOwnerFilter rowOwnerFilter,
        List<RowFilter> rowFilters) {

        throw new UnsupportedOperationException();
    }
```

- [ ] **Step 6: Run the integration test**

```bash
export DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests '*DataTableRowFilterIntTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t.log | head
```

Expected: exit=0.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply -q
git add server/libs/platform/platform-data-table server/ee/libs/platform/platform-data-table
git commit -m "- Filter data table rows in the row service"
```

---

### Task 3: Expose the grammar on the `findRecords` action

**Files:**
- Modify: `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/constant/DataTableConstants.java`
- Modify: `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/action/DataTableFindRecordsAction.java`
- Test: `server/libs/modules/components/data-table/src/test/java/com/bytechef/component/datatable/action/DataTableFindRecordsActionTest.java`

**Interfaces:**
- Consumes: `RowFilter` and the six-argument `listRows` from Tasks 1 and 2.
- Produces: `static List<RowFilter> DataTableFindRecordsAction.toRowFilters(List<Map<String, ?>>)`. Nothing later depends on it; this plan ends here.

- [ ] **Step 1: Add the constants**

In `DataTableConstants`, keeping that file's existing insertion-ordered style:

```java
    public static final String FILTERS = "filters";
    public static final String FIELD = "field";
    public static final String OPERATOR = "operator";
    public static final String VALUE = "value";
```

- [ ] **Step 2: Write the failing action test**

```java
package com.bytechef.component.datatable.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.data.table.domain.RowFilter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableFindRecordsActionTest {

    @Test
    void testNoFilterEntriesProduceNoFilters() {
        assertTrue(DataTableFindRecordsAction.toRowFilters(List.of())
            .isEmpty());
    }

    @Test
    void testAnEntryBecomesARowFilter() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "title", "operator", "EQ", "value", "alpha")));

        RowFilter rowFilter = rowFilters.getFirst();

        assertEquals("title", rowFilter.field());
        assertEquals(RowFilter.Operator.EQ, rowFilter.operator());
        assertEquals("alpha", rowFilter.value());
    }

    @Test
    void testInSplitsItsValueOnCommas() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "score", "operator", "IN", "value", "1, 2,3")));

        assertEquals(List.of("1", "2", "3"), rowFilters.getFirst()
            .value());
    }

    @Test
    void testBetweenSplitsItsValueOnCommas() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "score", "operator", "BETWEEN", "value", "1,9")));

        assertEquals(List.of("1", "9"), rowFilters.getFirst()
            .value());
    }

    @Test
    void testAnEntryWithNoFieldIsSkipped() {
        assertTrue(
            DataTableFindRecordsAction.toRowFilters(List.of(Map.of("operator", "EQ", "value", "alpha")))
                .isEmpty());
    }

    @Test
    void testAnEntryWithNoOperatorDefaultsToEquals() {
        List<RowFilter> rowFilters = DataTableFindRecordsAction.toRowFilters(
            List.of(Map.of("field", "title", "value", "alpha")));

        assertEquals(RowFilter.Operator.EQ, rowFilters.getFirst()
            .operator());
    }
}
```

- [ ] **Step 3: Run it to verify it fails**

```bash
./gradlew :server:libs:modules:components:data-table:test --tests '*DataTableFindRecordsActionTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E 'error:|^> Task .* FAILED' /tmp/t.log | head
```

Expected: compile failure — `toRowFilters` does not exist.

- [ ] **Step 4: Add the property and the parser**

In `build()`, insert the filters property between `TABLE` and `LIMIT`:

```java
                array(FILTERS)
                    .label("Filters")
                    .description(
                        "Conditions a record must meet. All conditions must match. Leave empty to return every " +
                            "record.")
                    .items(
                        object()
                            .properties(
                                string(FIELD)
                                    .label("Column")
                                    .description("The column to test.")
                                    .required(true),
                                string(OPERATOR)
                                    .label("Operator")
                                    .defaultValue("EQ")
                                    .options(
                                        option("Equals", "EQ"),
                                        option("Not Equals", "NEQ"),
                                        option("Contains", "CONTAINS"),
                                        option("Starts With", "STARTS_WITH"),
                                        option("Greater Than", "GT"),
                                        option("Greater Than Or Equal", "GTE"),
                                        option("Less Than", "LT"),
                                        option("Less Than Or Equal", "LTE"),
                                        option("In", "IN"),
                                        option("Between", "BETWEEN"))
                                    .required(true),
                                string(VALUE)
                                    .label("Value")
                                    .description(
                                        "The value to compare against. For In and Between, a comma-separated list.")
                                    .required(false)))
                    .required(false),
```

Add the `array`, `object`, `option` DSL static imports and the four new constants.

Then the parser:

```java
    static List<RowFilter> toRowFilters(List<Map<String, ?>> filterEntries) {
        List<RowFilter> rowFilters = new ArrayList<>();

        for (Map<String, ?> filterEntry : filterEntries) {
            Object field = filterEntry.get(FIELD);

            if (field == null || String.valueOf(field)
                .isBlank()) {
                continue;
            }

            Object operator = filterEntry.get(OPERATOR);
            RowFilter.Operator rowFilterOperator = operator == null
                ? RowFilter.Operator.EQ
                : RowFilter.Operator.valueOf(String.valueOf(operator));

            rowFilters.add(
                new RowFilter(String.valueOf(field), rowFilterOperator, value(rowFilterOperator, filterEntry)));
        }

        return rowFilters;
    }

    private static @Nullable Object value(RowFilter.Operator operator, Map<String, ?> filterEntry) {
        Object value = filterEntry.get(VALUE);

        if (operator != RowFilter.Operator.IN && operator != RowFilter.Operator.BETWEEN) {
            return value;
        }

        return Arrays.stream(String.valueOf(value)
            .split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
    }
```

and in `perform`, after the existing `rowOwnerFilter` line, read the list and pass it to the six-argument `listRows`. Check which `Parameters#getList` overload exists on this branch before writing that call. Leave `output()` unfiltered: it samples the row shape, and a filter matching nothing would leave the editor with no schema.

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:libs:modules:components:data-table:test --tests '*DataTableFindRecordsActionTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t.log | head
```

- [ ] **Step 6: Regenerate the component definition snapshot**

Adding a property changes the generated JSON definition. Delete both copies and rerun, per the repo convention:

```bash
rm -f server/libs/modules/components/data-table/src/test/resources/definition/*.json
rm -rf server/libs/modules/components/data-table/build/resources/test/definition
./gradlew :server:libs:modules:components:data-table:test > /tmp/t.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t.log | head
```

If that directory holds no snapshot for this component, confirm with `ls` and move on rather than inventing one.

- [ ] **Step 7: Verify the architecture guard still holds**

`DataTableComponentUsesScopedRowServiceTest` scans the component's source for `dataTableRowService.<method>(` calls whose argument list omits `rowOwnerFilter`. The new call still passes it, but it now spans lines differently — confirm the scanner still sees it:

```bash
./gradlew :server:libs:modules:components:data-table:test --tests '*DataTableComponentUsesScopedRowServiceTest' > /tmp/t.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/t.log | head
```

- [ ] **Step 8: Format, check the touched modules, commit**

```bash
./gradlew spotlessApply -q
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test :server:libs:modules:components:data-table:test compileJava compileTestJava --continue > /tmp/t.log 2>&1
echo "exit=$?"; grep -cE '^> Task .* FAILED' /tmp/t.log
git add server/libs/modules/components/data-table
git commit -m "- Filter records from the Find Records action"
```

---

## Deliberately not in this plan

- **No sort order.** `listRows` orders by `id` ascending, which is insertion order, and pagination can walk to the end — so retrieving "the newest N" still costs a full walk. A `sort` parameter mirroring `ContextStoreQuerySort` is the obvious next step and a separate feature from filtering.
- **No `OR` between filters.** Every condition is `AND`-ed, exactly as `ContextStoreQueryFilter` is. A grouping grammar is a much larger design.
- **No column-name dropdown.** `field` is free text rather than an options lookup, because an options function nested inside an array item's object would need `optionsLookupDependsOn` to resolve across the array boundary, which is unverified on this branch. The service rejects an unknown column with a clear message, so the failure mode is a legible error rather than a silent empty result. Verify the nested lookup before promoting this to a dropdown.
- **`findRecords` is the only filtered surface.** `deleteRecords`, the GraphQL row listing, and CSV export keep their existing signatures.
