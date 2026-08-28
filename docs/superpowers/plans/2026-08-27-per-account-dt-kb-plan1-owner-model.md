# Per-account DT/KB — Plan 1: Reserved columns and the owner model

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give `data_table` and `knowledge_base` a nullable owner, give every `dt_*` physical table reserved `owner_id` / `owner_type` columns, and migrate existing tables — without changing any read or write behaviour yet.

**Architecture:** Three moves in order. First a pure refactor collapsing sixteen open-coded `"id".equalsIgnoreCase` checks into one `ReservedColumns` helper, so the reserved set is a single edit afterwards. Then the owner columns on the two entity tables via new Liquibase changesets. Then the reserved columns on the dynamically-named `dt_*` tables — added by `createTable` for new tables and by a Java migration for existing ones, since the table set is discovered at runtime and cannot be a static changelog.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, raw `JdbcTemplate` for `dt_*` access, Liquibase, JUnit 5, Mockito, Testcontainers (PostgreSQL 15).

**Spec:** `docs/superpowers/specs/2026-08-27-per-account-data-tables-knowledge-bases-design.md`

## Global Constraints

- Reserved column names are **unprefixed**, matching the existing `id` convention: `owner_id`, `owner_type`.
- `OwnerType` is persisted as an **INT ordinal** — append new values at the end, never reorder.
- Owner columns are **nullable**. NULL means vendor-owned. Every pre-existing row and resource stays NULL.
- Both init changelogs ship in **`v0.31.4`**. Add new changesets; never edit an init file.
- The changelog directory is wired with `includeAll` in `master.xml:134`, so a new file is picked up automatically — **no `master.xml` edit**.
- Run `./gradlew spotlessApply` before every commit. Test method names are camelCase with no underscores (Checkstyle). `TODO:` comments are forbidden.
- Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep for `^> Task .* FAILED`.
- This plan changes **no** read or write behaviour. Every existing test must still pass unmodified.

---

### Task 1: Extract a ReservedColumns helper

Pure refactor, no behaviour change. `DataTableServiceImpl` and `DataTableRowServiceImpl` between them contain sixteen literal `"id"` reserved-column checks. Collapsing them now means Task 3 adds two names in one place instead of sixteen.

**Files:**
- Create: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/ReservedColumns.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/configuration/service/DataTableServiceImpl.java` (6 sites: lines 113, 115, 187, 252, 308, 309)
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java` (10 sites: lines 130, 164, 242, 313, 331, 363, 404, 457, 485, 519)
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/domain/ReservedColumnsTest.java`

The test lives in `-service` rather than `-api` deliberately: `-api` has no test source set today, and adding one is a `build.gradle.kts` change unrelated to this task. `-service` depends on `-api`, so the class is on its test classpath.

**Interfaces:**
- Consumes: nothing.
- Produces: `com.bytechef.platform.data.table.domain.ReservedColumns` with `public static final String ID`, `public static boolean isReserved(String columnName)` (null-safe, case-insensitive), and `public static Set<String> all()`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.data.table.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ReservedColumnsTest {

    @Test
    void testIsReservedMatchesIdCaseInsensitively() {
        assertTrue(ReservedColumns.isReserved("id"));
        assertTrue(ReservedColumns.isReserved("ID"));
        assertTrue(ReservedColumns.isReserved("Id"));
    }

    @Test
    void testIsReservedRejectsOrdinaryColumnNames() {
        assertFalse(ReservedColumns.isReserved("identifier"));
        assertFalse(ReservedColumns.isReserved("name"));
    }

    @Test
    void testIsReservedTreatsNullAsNotReserved() {
        assertFalse(ReservedColumns.isReserved(null));
    }

    @Test
    void testAllReturnsTheReservedSet() {
        assertEquals(Set.of("id"), ReservedColumns.all());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.data.table.domain.ReservedColumnsTest" > /tmp/t1.log 2>&1; echo $?; grep -E "^> Task .* FAILED|error:" /tmp/t1.log | head
```

Expected: compilation failure — `package com.bytechef.platform.data.table.domain does not exist` / `cannot find symbol: class ReservedColumns`.

- [ ] **Step 3: Write the minimal implementation**

```java
package com.bytechef.platform.data.table.domain;

import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Column names the platform owns on every {@code dt_*} physical table. A reserved name cannot be created, cannot be
 * renamed, cannot be renamed to, and is filtered out of every column listing so it never reaches the grid, the
 * generated row schema, or a CSV round trip.
 *
 * @author Ivica Cardic
 */
public final class ReservedColumns {

    public static final String ID = "id";

    private static final Set<String> ALL = Set.of(ID);

    private ReservedColumns() {
    }

    public static Set<String> all() {
        return ALL;
    }

    public static boolean isReserved(@Nullable String columnName) {
        if (columnName == null) {
            return false;
        }

        return ALL.contains(columnName.toLowerCase(Locale.ROOT));
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.data.table.domain.ReservedColumnsTest" > /tmp/t1.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t1.log | head
```

Expected: exit 0, no FAILED tasks.

- [ ] **Step 5: Replace all sixteen call sites**

In both impls, replace every `!"id".equalsIgnoreCase(x)` with `!ReservedColumns.isReserved(x)` and every `"id".equalsIgnoreCase(x)` with `ReservedColumns.isReserved(x)`, adding the import. The three assertion sites in `DataTableServiceImpl` keep their existing messages for now:

```java
// DataTableServiceImpl:113-115
boolean hasReserved = columnSpecs.stream()
    .anyMatch(columnSpec -> ReservedColumns.isReserved(columnSpec.name()));

Assert.isTrue(!hasReserved, "Column name 'id' is reserved for primary key");

// DataTableServiceImpl:308-309
Assert.isTrue(!ReservedColumns.isReserved(fromColumnName), "Column 'id' cannot be renamed");
Assert.isTrue(!ReservedColumns.isReserved(toColumnName), "Cannot rename to reserved name 'id'");
```

Note `DataTableRowServiceImpl:242` and `:363` and `:519` are positive checks (`if (header.equalsIgnoreCase("id"))`), so they become `if (ReservedColumns.isReserved(header))` with no negation.

- [ ] **Step 6: Verify no literal reserved-column checks remain**

```bash
grep -rn '"id"\.equalsIgnoreCase\|equalsIgnoreCase("id")' server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/
```

Expected: no output.

- [ ] **Step 7: Run the full module test suite to prove no behaviour changed**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test > /tmp/t1full.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t1full.log | head
```

Expected: exit 0. `DataTableServiceTest`, `DataTableRowServiceEnforcementTest` and `DataTableStorageServiceTest` all still pass unmodified.

- [ ] **Step 8: Format and commit**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-api:spotlessApply :server:libs:platform:platform-data-table:platform-data-table-service:spotlessApply
git add server/libs/platform/platform-data-table
git commit -m "--- Collapse the data table reserved-column checks into one helper"
```

---

### Task 2: Add the OwnerType enum and the resource-level owner columns

**Files:**
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/constant/OwnerType.java`
- Create: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/resources/config/liquibase/changelog/platform/data_table/20260827000001_platform_data_table_add_owner.xml`
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/resources/config/liquibase/changelog/platform/knowledge_base/20260827000001_platform_knowledge_base_add_owner.xml`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/configuration/domain/DataTable.java`
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/domain/KnowledgeBase.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/OwnerTypeOrdinalStabilityTest.java`

`platform-api` is the right home: both `platform-data-table` and `platform-knowledge-base` need the enum and neither may depend on the other. It sits beside the existing `PlatformType`.

**Interfaces:**
- Consumes: nothing.
- Produces: `com.bytechef.platform.constant.OwnerType` (one value, `CONNECTED_USER`); `DataTable.getOwnerId()/setOwnerId(Long)`, `DataTable.getOwnerType()/setOwnerType(OwnerType)`, and the same four on `KnowledgeBase`. Both getters return `@Nullable`.

- [ ] **Step 1: Write the failing ordinal-stability test**

The codebase persists enums as INT ordinals, so a reorder is a silent data corruption. `platform-knowledge-base` already guards this with an `EnumOrdinalStabilityTest`; this mirrors it.

```java
package com.bytechef.platform.data.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.constant.OwnerType;
import org.junit.jupiter.api.Test;

class OwnerTypeOrdinalStabilityTest {

    @Test
    void testConnectedUserKeepsOrdinalZero() {
        assertEquals(0, OwnerType.CONNECTED_USER.ordinal());
    }

    @Test
    void testNoValueWasInsertedBeforeConnectedUser() {
        assertEquals("CONNECTED_USER", OwnerType.values()[0].name());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.data.table.OwnerTypeOrdinalStabilityTest" > /tmp/t2.log 2>&1; echo $?; grep -E "error:" /tmp/t2.log | head
```

Expected: `cannot find symbol: class OwnerType`.

- [ ] **Step 3: Write the enum**

```java
package com.bytechef.platform.constant;

/**
 * Identifies what kind of principal owns a resource or a row. Persisted as an INT ordinal, so new values are appended
 * at the end and existing values are never reordered. A null owner type means the resource belongs to the tenant
 * itself rather than to any principal within it.
 *
 * @author Ivica Cardic
 */
public enum OwnerType {

    CONNECTED_USER
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.data.table.OwnerTypeOrdinalStabilityTest" > /tmp/t2.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t2.log | head
```

Expected: exit 0.

- [ ] **Step 5: Write the data_table changeset**

`logicalFilePath` must match the file's own path — the knowledge-base example in this repo has a copy-paste mismatch there; do not reproduce it.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd"
                   logicalFilePath="config/liquibase/changelog/platform/data_table/20260827000001_platform_data_table_add_owner.xml">

    <changeSet id="20260827000001-1" author="Ivica Cardic">
        <addColumn tableName="data_table">
            <column name="owner_id" type="BIGINT"/>
            <column name="owner_type" type="INT"/>
        </addColumn>

        <createIndex tableName="data_table" indexName="idx_data_table_owner">
            <column name="owner_type"/>
            <column name="owner_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

No `addNotNullConstraint` and no backfill `update`: NULL is a real state meaning vendor-owned, and every existing row correctly stays vendor-owned.

- [ ] **Step 6: Write the knowledge_base changeset**

Identical body with `tableName="knowledge_base"`, `indexName="idx_knowledge_base_owner"`, and `logicalFilePath="config/liquibase/changelog/platform/knowledge_base/20260827000001_platform_knowledge_base_add_owner.xml"`.

- [ ] **Step 7: Add the Gradle dependency to both -api modules**

Neither `platform-data-table-api` nor `platform-knowledge-base-api` depends on `platform-api` today. Add to both
`build.gradle.kts` files, in the existing alphabetical position among the `implementation(project(...))` lines:

```kotlin
    implementation(project(":server:libs:platform:platform-api"))
```

- [ ] **Step 8: Add the fields to both domain classes**

**This codebase never maps an enum field directly.** Spring Data JDBC would persist an `OwnerType` field as its
String name, against an `INT` column. Every ordinal-persisted enum here is stored as an `int` with converting
accessors — `Property.scope` (`private int scope`, `Scope.values()[scope]` / `scope.ordinal()`) and
`KnowledgeBase.environment` are both this shape. Follow it.

In `DataTable.java`, after the `description` field:

```java
    @Column("owner_id")
    private @Nullable Long ownerId;

    @Column("owner_type")
    private @Nullable Integer ownerType;
```

`Integer`, not `int`: null is a real state meaning vendor-owned, and a primitive would silently read as ordinal 0.

Accessors, placed beside the existing ones and converting at the boundary:

```java
    public @Nullable Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(@Nullable Long ownerId) {
        this.ownerId = ownerId;
    }

    public @Nullable OwnerType getOwnerType() {
        return ownerType == null ? null : OwnerType.values()[ownerType];
    }

    public void setOwnerType(@Nullable OwnerType ownerType) {
        this.ownerType = ownerType == null ? null : ownerType.ordinal();
    }
```

Imports: `com.bytechef.platform.constant.OwnerType`, `org.jspecify.annotations.Nullable`.

Add the same two fields and four accessors to `KnowledgeBase.java`, after its existing `environment` field.

- [ ] **Step 9: Add a round-trip test for the ordinal mapping**

The accessor conversion is the part that breaks silently, so test it directly. Add to
`server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/OwnerTypeOrdinalStabilityTest.java`:

```java
    @Test
    void testDataTableOwnerTypeRoundTrips() {
        DataTable dataTable = new DataTable();

        dataTable.setOwnerType(OwnerType.CONNECTED_USER);

        assertEquals(OwnerType.CONNECTED_USER, dataTable.getOwnerType());
    }

    @Test
    void testDataTableOwnerTypeDefaultsToNull() {
        DataTable dataTable = new DataTable();

        assertNull(dataTable.getOwnerType());
        assertNull(dataTable.getOwnerId());
    }
```

Run: `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.data.table.OwnerTypeOrdinalStabilityTest" > /tmp/t2rt.log 2>&1; echo $?`

Expected: exit 0, four tests passing.

- [ ] **Step 10: Prove the schema applies against a real database**

Liquibase changelog edits are verified through an existing `IntTest`, because Testcontainers builds the schema from scratch — stronger evidence than `bootRun`, which under the `liquibase` profile exits 0 having created nothing.

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:testIntegration --tests "*KnowledgeBaseServiceIntTest" > /tmp/t2int.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t2int.log | head
```

Expected: exit 0. Docker must be running; this host reaches it through the OrbStack socket rather than `/var/run/docker.sock`.

- [ ] **Step 11: Format and commit**

```bash
./gradlew spotlessApply > /tmp/t2sp.log 2>&1; echo $?
git add server/libs/platform/platform-api server/libs/platform/platform-data-table server/libs/platform/platform-knowledge-base
git commit -m "- Add a nullable owner to data tables and knowledge bases"
```

---

### Task 3: Reserve owner_id and owner_type on dt_* tables

Extends the reserved set and creates the two columns on every newly created table. Existing tables are handled in Task 4.

**Files:**
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/ReservedColumns.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/configuration/service/DataTableServiceImpl.java` (`createTable`, and the three `Assert.isTrue` messages)
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/domain/ReservedColumnsTest.java`

**Interfaces:**
- Consumes: `ReservedColumns` from Task 1; `OwnerType` from Task 2.
- Produces: `ReservedColumns.OWNER_ID` (`"owner_id"`), `ReservedColumns.OWNER_TYPE` (`"owner_type"`); every table created after this task has both columns.

- [ ] **Step 1: Extend the failing test**

Add to `ReservedColumnsTest`:

```java
    @Test
    void testIsReservedMatchesOwnerColumns() {
        assertTrue(ReservedColumns.isReserved("owner_id"));
        assertTrue(ReservedColumns.isReserved("OWNER_TYPE"));
    }

    @Test
    void testAllReturnsEveryReservedName() {
        assertEquals(Set.of("id", "owner_id", "owner_type"), ReservedColumns.all());
    }
```

and delete the now-superseded `testAllReturnsTheReservedSet`.

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.data.table.domain.ReservedColumnsTest" > /tmp/t3.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t3.log | head
```

Expected: FAILED — `testIsReservedMatchesOwnerColumns` and `testAllReturnsEveryReservedName` both fail.

- [ ] **Step 3: Extend the reserved set**

```java
    public static final String ID = "id";
    public static final String OWNER_ID = "owner_id";
    public static final String OWNER_TYPE = "owner_type";

    private static final Set<String> ALL = Set.of(ID, OWNER_ID, OWNER_TYPE);
```

- [ ] **Step 4: Run it to verify it passes**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.data.table.domain.ReservedColumnsTest" > /tmp/t3.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t3.log | head
```

Expected: exit 0.

- [ ] **Step 5: Correct the three assertion messages**

They now name a set, not one column:

```java
Assert.isTrue(!hasReserved, "Column names " + ReservedColumns.all() + " are reserved");
...
Assert.isTrue(!ReservedColumns.isReserved(fromColumnName), "Reserved columns cannot be renamed");
Assert.isTrue(!ReservedColumns.isReserved(toColumnName), "Cannot rename to a reserved name");
```

- [ ] **Step 6: Add the columns in createTable**

`createTable` builds one concatenated statement at `DataTableServiceImpl.java:123-126`. Replace:

```java
String sql = "CREATE TABLE " + escapeIdentifier(physicalName) + " (\"id\" BIGSERIAL PRIMARY KEY" +
    (userColsSql.isEmpty() ? "" : ", " + userColsSql) + ")";

jdbcTemplate.execute(sql);
```

with:

```java
String sql = "CREATE TABLE " + escapeIdentifier(physicalName) +
    " (\"id\" BIGSERIAL PRIMARY KEY, \"owner_id\" BIGINT, \"owner_type\" INT" +
    (userColsSql.isEmpty() ? "" : ", " + userColsSql) + ")";

jdbcTemplate.execute(sql);

jdbcTemplate.execute(
    "CREATE INDEX " + escapeIdentifier("idx_" + physicalName + "_owner") +
        " ON " + escapeIdentifier(physicalName) + " (\"owner_type\", \"owner_id\")");
```

The index gives the Plan 2 filter support. Both identifiers go through `escapeIdentifier`, so the existing
`@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")` on the method stays justified.

- [ ] **Step 7: Add a unit test asserting the emitted CREATE TABLE statement**

`platform-data-table` has **no `*IntTest` classes and no `application-testint.yml`** — its three test classes are
Mockito unit tests. (Task 4 adds that scaffolding, for the one deliverable that genuinely needs a live database.)
Assert on the SQL string, matching how `DataTableServiceTest` already exercises this class:

```java
@Test
void testCreateTableEmitsReservedOwnerColumns() {
    dataTableService.createTable(
        "conversations", null, List.of(new ColumnSpec("title", ColumnType.STRING)), 0);

    ArgumentCaptor<String> sqlArgumentCaptor = ArgumentCaptor.forClass(String.class);

    verify(jdbcTemplate, atLeastOnce()).execute(sqlArgumentCaptor.capture());

    String createTableSql = sqlArgumentCaptor.getAllValues()
        .stream()
        .filter(sql -> sql.startsWith("CREATE TABLE"))
        .findFirst()
        .orElseThrow();

    assertTrue(createTableSql.contains("\"owner_id\" BIGINT"));
    assertTrue(createTableSql.contains("\"owner_type\" INT"));
}

@Test
void testCreateTableRejectsAReservedColumnName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> dataTableService.createTable(
            "conversations", null, List.of(new ColumnSpec("owner_id", ColumnType.STRING)), 0));
}
```

Add both to the existing `DataTableServiceTest`, reusing its mock setup rather than building a new one.

- [ ] **Step 8: Run the module's tests**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test > /tmp/t3full.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t3full.log | head
```

Expected: exit 0.

- [ ] **Step 9: Format and commit**

```bash
./gradlew spotlessApply > /tmp/t3sp.log 2>&1; echo $?
git add server/libs/platform/platform-data-table
git commit -m "- Reserve owner_id and owner_type on every new data table"
```

---

### Task 4: Backfill the owner columns onto existing dt_* tables

The `dt_*` set is discovered at runtime from `information_schema`, so this cannot be a static changelog. It is a Java migration that enumerates the prefix and issues idempotent DDL, and it must run per tenant schema.

**Files:**
- Create: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/configuration/migration/DataTableOwnerColumnMigrator.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/configuration/migration/DataTableOwnerColumnMigratorIntTest.java`

**Interfaces:**
- Consumes: `ReservedColumns.OWNER_ID`, `ReservedColumns.OWNER_TYPE` from Task 3.
- Produces: `DataTableOwnerColumnMigrator.migrate()` returning `int` — the number of tables altered. Idempotent: a second call returns 0.

- [ ] **Step 1: Add the integration-test scaffolding**

This module has never had an `*IntTest`. The `testIntegration` task collects `*IntTest` classes from `src/test/java`,
and `build.gradle.kts` already carries every dependency it needs (`test-int-support`, `liquibase-config`,
`spring-boot-starter-test`, `spring-boot-starter-data-jdbc`). Only the profile config is missing.

Create `server/libs/platform/platform-data-table/platform-data-table-service/src/test/resources/config/application-testint.yml`:

```yaml
spring:
  liquibase:
    contexts: configuration,connection,execution
```

`execution` is required: `master.xml:134` includes the data-table changelog directory under
`contextFilter="mono or execution or multitenant"`, so omitting it leaves `data_table` absent and every test fails
with a missing-relation error rather than an assertion failure.

Also create the standard int-test configuration class
`src/test/java/com/bytechef/platform/data/table/config/DataTableIntTestConfiguration.java`, modelled on
`platform-knowledge-base-service`'s `KnowledgeBaseIntTestConfiguration`, annotated
`@EnableAutoConfiguration`, `@Configuration` and `@ComponentScan(basePackages = "com.bytechef.platform.data.table")`.

- [ ] **Step 2: Write the failing integration test**

```java
@Test
void testMigrateAddsOwnerColumnsToPreexistingTable() {
    jdbcTemplate.execute("CREATE TABLE \"dt_0_legacy\" (\"id\" BIGSERIAL PRIMARY KEY, \"title\" TEXT)");

    int altered = dataTableOwnerColumnMigrator.migrate();

    assertTrue(altered >= 1);
    assertTrue(hasColumn("dt_0_legacy", "owner_id"));
    assertTrue(hasColumn("dt_0_legacy", "owner_type"));
}

@Test
void testMigrateIsIdempotent() {
    jdbcTemplate.execute("CREATE TABLE \"dt_0_legacy_two\" (\"id\" BIGSERIAL PRIMARY KEY)");

    dataTableOwnerColumnMigrator.migrate();

    assertEquals(0, dataTableOwnerColumnMigrator.migrate());
}

@Test
void testMigrateIgnoresNonDataTables() {
    jdbcTemplate.execute("CREATE TABLE \"not_a_data_table\" (\"id\" BIGSERIAL PRIMARY KEY)");

    dataTableOwnerColumnMigrator.migrate();

    assertFalse(hasColumn("not_a_data_table", "owner_id"));
}

private boolean hasColumn(String tableName, String columnName) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM information_schema.columns "
            + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
        Integer.class, tableName, columnName);

    return count != null && count > 0;
}
```

- [ ] **Step 3: Run it to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests "*DataTableOwnerColumnMigratorIntTest" > /tmp/t4.log 2>&1; echo $?; grep -E "error:" /tmp/t4.log | head
```

Expected: `cannot find symbol: class DataTableOwnerColumnMigrator`.

- [ ] **Step 4: Write the migrator**

`ADD COLUMN IF NOT EXISTS` carries the idempotency, so the count is derived from a prior existence check rather than from the DDL.

```java
package com.bytechef.platform.data.table.configuration.migration;

import com.bytechef.platform.data.table.domain.ReservedColumns;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds the reserved owner columns to {@code dt_*} tables created before those columns existed. The table set is
 * discovered from {@code information_schema} rather than declared, which is why this is a Java migration and not a
 * Liquibase changeset. Idempotent, and scoped to the connection's current schema so it migrates one tenant per call.
 *
 * @author Ivica Cardic
 */
@Component
public class DataTableOwnerColumnMigrator {

    private static final Logger log = LoggerFactory.getLogger(DataTableOwnerColumnMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public DataTableOwnerColumnMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int migrate() {
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE 'dt\\_%'",
            String.class);

        int altered = 0;

        for (String tableName : tableNames) {
            if (hasOwnerColumns(tableName)) {
                continue;
            }

            jdbcTemplate.execute(
                "ALTER TABLE \"" + tableName + "\" ADD COLUMN IF NOT EXISTS \"" + ReservedColumns.OWNER_ID
                    + "\" BIGINT, ADD COLUMN IF NOT EXISTS \"" + ReservedColumns.OWNER_TYPE + "\" INT");

            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS \"idx_" + tableName + "_owner\" ON \"" + tableName
                    + "\" (\"" + ReservedColumns.OWNER_TYPE + "\", \"" + ReservedColumns.OWNER_ID + "\")");

            altered++;
        }

        if (altered > 0) {
            log.info("Added owner columns to {} data tables", altered);
        }

        return altered;
    }

    private boolean hasOwnerColumns(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = current_schema() AND table_name = ? AND column_name IN (?, ?)",
            Integer.class, tableName, ReservedColumns.OWNER_ID, ReservedColumns.OWNER_TYPE);

        return count != null && count == 2;
    }
}
```

The table names come from `information_schema` rather than from user input, and the `dt\_%` LIKE pattern escapes the underscore so `dt_` is a literal prefix rather than a single-character wildcard.

- [ ] **Step 5: Run the integration test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests "*DataTableOwnerColumnMigratorIntTest" > /tmp/t4.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t4.log | head
```

Expected: exit 0, three tests passing.

- [ ] **Step 6: Run the whole module and the app's context test**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --continue > /tmp/t4full.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t4full.log | head
```

Expected: exit 0. Adding a scanned `@Component` can break other modules' hand-assembled `@SpringBootTest(classes = ...)` contexts — if any fail with a missing-bean error, add a mock `@Bean` to that module's `*IntTestConfiguration`.

- [ ] **Step 7: Format, run static analysis, and commit**

```bash
./gradlew spotlessApply > /tmp/t4sp.log 2>&1; echo $?
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:check > /tmp/t4chk.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t4chk.log | head
git add server/libs/platform/platform-data-table
git commit -m "- Backfill the owner columns onto pre-existing data tables"
```

For any SpotBugs finding, read `build/reports/spotbugs/test.html` — the XML report is disabled and never rewritten.

---

## Who calls the migrator

Task 4 delivers the migrator and its tests but deliberately does not wire it to a startup hook. Multitenant deployments need it run per tenant schema, which is a tenant-lifecycle concern rather than a data-table one, and choosing that hook belongs with the deployment work in Plan 3. Until then it is invoked explicitly.

## Follow-on plans

- **Plan 2 — Runtime scoping.** `RowOwnerFilter`, `resolveRowOwner(ActionContextAware)`, enforcement in `DataTableRowServiceImpl`, the `findRecords` filter, the missing `KnowledgeBaseSearchAction` ownership check, and editor-environment fail-closed.
- **Plan 3 — Embedded admin API.** `'DataTable'` / `'KnowledgeBase'` cases in `ConnectedUserResourceMembershipResolver`, the two embedded `-graphql` modules, `isTenantAdmin()` on the API facades, and the migrator's tenant hook.
- **Plan 4 — Admin UI.** Shared components under `shared/components/`, the scope union, and the embedded console pages. Includes the one-line `IntegrationComponentDefinitionFilter` palette exclusion.
