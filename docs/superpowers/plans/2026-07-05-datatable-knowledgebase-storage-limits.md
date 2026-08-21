# DataTable & KnowledgeBase Storage Limits Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add n8n-style per-tenant storage limits (warn at 80%, hard-block at 100%) to DataTables and KnowledgeBase, with independent caps and warning banners.

**Architecture:** ByteChef is schema-per-tenant, so "per tenant" usage is naturally the sum of everything in the current tenant schema (the connection's `search_path`). Two independent platform services compute usage (`DataTableStorageService` via `pg_total_relation_size` over `dt_*` tables; `KnowledgeBaseStorageService` via `SUM(document_size)` on `knowledge_base_document`) and enforce a hard cap at write time. Automation-layer facades expose usage over GraphQL for warning banners.

**Tech Stack:** Java 25 / Spring Boot 4 (Spring Data JDBC, Spring for GraphQL), PostgreSQL + Liquibase, React 19 + TypeScript + GraphQL codegen + React Query, Vitest.

## Global Constraints

- Server files under `server/libs/**` use the **Apache 2.0** license header (existing header in each file). Do not add the EE header.
- The one EE file touched (`server/ee/.../AddKnowledgeBaseDocumentToolCallback.java`) keeps its existing ByteChef Enterprise header and `@version ee` tag — we only change a call site.
- Any new `bytechef.*` property MUST be a declared field in `ApplicationProperties` (strict binding, `ignoreUnknownFields = false`) or the context fails to boot.
- Java style: one blank line before control statements; one blank line after a variable modification that precedes its use; no `_`-prefixed private methods; descriptive variable names (no single letters); no trailing blank line before a class's closing `}`; no method chaining except the documented idiomatic exceptions.
- Enum/JDBC: not relevant here (no new enums).
- Run `./gradlew spotlessApply` before every server commit; `./gradlew check` before finishing server work.
- Client style: object keys in ascending order (`sort-keys`, not auto-fixable); interface names end in `I` or `Props`; named imports sorted alphabetically; lucide icons imported with the `Icon` suffix; use `twMerge` (not `cn()`) in NEW component code — note `alert.tsx` already uses `cn` inside its own `cva`, which we extend in place (no new `cn` call added).
- Run `cd client && npm run check` before every client commit.
- Test naming: camelCase, no underscores. Unit test classes end in `Test`; integration test classes end in `IntTest`.
- Commit messages: server `<ticket> <description>`, client `<ticket> client - <description>`. If a GitHub issue number is assigned to this work, prefix each message with it; the messages below omit the prefix — add it when known.
- `StorageUsage` is intentionally split into two records/GraphQL types (`DataTableStorageUsage`, `KnowledgeBaseStorageUsage`) rather than one shared type: the two `.graphqls` files merge into a single runtime schema, so a single `StorageUsage` type defined in both would collide. Keeping them separate also decouples the two feature modules.
- Default limits: DataTables `52_428_800` (50 MB), KnowledgeBase `1_073_741_824` (1 GB). `0` = unlimited.
- Block semantics: throw when `used + incomingBytes > limit`. DataTable `insertRow` passes `incomingBytes = 0` (blocks once already over); `importCsv` passes the estimated CSV byte length; KB upload passes the file size.

---

### Task 1: `DataTableStorageService` + config (platform)

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (add top-level `DataTable` group near the `fileStorage` field at line 129 and its getter near line 287)
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/build.gradle.kts` (add app-config dependency)
- Create: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/DataTableStorageUsage.java`
- Create: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/exception/DataTableStorageLimitExceededException.java`
- Create: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableStorageService.java`
- Create: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableStorageServiceImpl.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableStorageServiceTest.java`

**Interfaces:**
- Produces:
  - `record DataTableStorageUsage(long usedBytes, long limitBytes, double percentage, boolean unlimited)`
  - `DataTableStorageLimitExceededException extends RuntimeException`
  - `DataTableStorageService`: `DataTableStorageUsage getUsage()`, `void checkWithinLimit(long incomingBytes)`
  - `ApplicationProperties.DataTable#getMaxSizeBytes(): long` (path `bytechef.data-table.max-size-bytes`, default `52_428_800`)

- [ ] **Step 1: Add the config group to `ApplicationProperties`**

Add the field (near line 129, alphabetically among top-level fields):

```java
    private DataTable dataTable = new DataTable();
```

Add the getter/setter (near the other getters, e.g. after `getDataStorage`):

```java
    public DataTable getDataTable() {
        return dataTable;
    }

    public void setDataTable(DataTable dataTable) {
        this.dataTable = dataTable;
    }
```

Add the nested class (place it near the other top-level nested classes, e.g. just before `public static class DataStorage` or after it):

```java
    /**
     * Data table storage configuration.
     */
    public static class DataTable {

        /**
         * Maximum total size in bytes of all data tables in a tenant. 0 means unlimited. Default 50 MB.
         */
        private long maxSizeBytes = 52_428_800L;

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }
    }
```

- [ ] **Step 2: Add the app-config dependency**

In `server/libs/platform/platform-data-table/platform-data-table-service/build.gradle.kts`, add under `dependencies {`:

```kotlin
    implementation(project(":server:libs:config:app-config"))
```

- [ ] **Step 3: Create `DataTableStorageUsage`**

```java
package com.bytechef.platform.data.table.domain;

/**
 * @author Ivica Cardic
 */
public record DataTableStorageUsage(long usedBytes, long limitBytes, double percentage, boolean unlimited) {
}
```

(Include the Apache license header — copy it verbatim from any sibling file in the module.)

- [ ] **Step 4: Create `DataTableStorageLimitExceededException`**

```java
package com.bytechef.platform.data.table.exception;

/**
 * @author Ivica Cardic
 */
public class DataTableStorageLimitExceededException extends RuntimeException {

    public DataTableStorageLimitExceededException(long usedBytes, long limitBytes) {
        super(
            "Data table storage limit reached (" + (usedBytes / 1_048_576) + " MB of " + (limitBytes / 1_048_576) +
                " MB used). Delete rows or increase bytechef.data-table.max-size-bytes.");
    }
}
```

- [ ] **Step 5: Create the `DataTableStorageService` interface**

```java
package com.bytechef.platform.data.table.execution.service;

import com.bytechef.platform.data.table.domain.DataTableStorageUsage;

/**
 * @author Ivica Cardic
 */
public interface DataTableStorageService {

    DataTableStorageUsage getUsage();

    void checkWithinLimit(long incomingBytes);
}
```

- [ ] **Step 6: Write the failing test**

```java
package com.bytechef.platform.data.table.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DataTableStorageServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private DataTableStorageServiceImpl createService(long limit, long used) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getDataTable()
            .setMaxSizeBytes(limit);

        when(jdbcTemplate.queryForObject(eq(DataTableStorageServiceImpl.USAGE_SQL), eq(Long.class)))
            .thenReturn(used);

        return new DataTableStorageServiceImpl(applicationProperties, jdbcTemplate);
    }

    @Test
    void testGetUsageComputesPercentage() {
        DataTableStorageUsage usage = createService(52_428_800L, 26_214_400L).getUsage();

        assertThat(usage.usedBytes()).isEqualTo(26_214_400L);
        assertThat(usage.limitBytes()).isEqualTo(52_428_800L);
        assertThat(usage.percentage()).isEqualTo(50.0);
        assertThat(usage.unlimited()).isFalse();
    }

    @Test
    void testGetUsageUnlimitedWhenLimitZero() {
        DataTableStorageUsage usage = createService(0L, 26_214_400L).getUsage();

        assertThat(usage.unlimited()).isTrue();
        assertThat(usage.percentage()).isEqualTo(0.0);
    }

    @Test
    void testCheckWithinLimitThrowsWhenOver() {
        assertThatThrownBy(() -> createService(52_428_800L, 52_428_800L).checkWithinLimit(1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);
    }

    @Test
    void testCheckWithinLimitPassesWhenUnlimited() {
        createService(0L, 999_999_999L).checkWithinLimit(1_000_000);
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "*DataTableStorageServiceTest"`
Expected: FAIL — `DataTableStorageServiceImpl` does not exist.

- [ ] **Step 8: Create `DataTableStorageServiceImpl`**

```java
package com.bytechef.platform.data.table.execution.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableStorageServiceImpl implements DataTableStorageService {

    static final String USAGE_SQL =
        "SELECT COALESCE(SUM(pg_total_relation_size(" +
            "(quote_ident(current_schema()) || '.' || quote_ident(tablename))::regclass)), 0) " +
            "FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' ESCAPE '\\'";

    private final ApplicationProperties applicationProperties;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableStorageServiceImpl(ApplicationProperties applicationProperties, JdbcTemplate jdbcTemplate) {
        this.applicationProperties = applicationProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DataTableStorageUsage getUsage() {
        long limit = getLimit();
        long used = currentUsageBytes();

        boolean unlimited = limit <= 0;
        double percentage = unlimited ? 0.0 : used * 100.0 / limit;

        return new DataTableStorageUsage(used, limit, percentage, unlimited);
    }

    @Override
    public void checkWithinLimit(long incomingBytes) {
        long limit = getLimit();

        if (limit <= 0) {
            return;
        }

        long used = currentUsageBytes();

        if (used + incomingBytes > limit) {
            throw new DataTableStorageLimitExceededException(used, limit);
        }
    }

    private long getLimit() {
        return applicationProperties.getDataTable()
            .getMaxSizeBytes();
    }

    private long currentUsageBytes() {
        Long used = jdbcTemplate.queryForObject(USAGE_SQL, Long.class);

        return used == null ? 0 : used;
    }
}
```

- [ ] **Step 9: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "*DataTableStorageServiceTest"`
Expected: PASS (4 tests).

- [ ] **Step 10: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/config/app-config server/libs/platform/platform-data-table
git commit -m "Add DataTableStorageService and data-table.max-size-bytes config"
```

---

### Task 2: Enforce the DataTable limit on insert / CSV import

**Files:**
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java` (constructor + `insertRow` + `importCsv`)
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceEnforcementTest.java`

**Interfaces:**
- Consumes: `DataTableStorageService#checkWithinLimit(long)` (Task 1).

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.data.table.execution.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

class DataTableRowServiceEnforcementTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DataTableStorageService dataTableStorageService = mock(DataTableStorageService.class);

    private final DataTableRowServiceImpl dataTableRowService =
        new DataTableRowServiceImpl(eventPublisher, jdbcTemplate, dataTableStorageService);

    @Test
    void testInsertRowBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(0);

        assertThatThrownBy(() -> dataTableRowService.insertRow("orders", Map.of("name", "x"), 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testImportCsvBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(org.mockito.ArgumentMatchers.anyLong());

        assertThatThrownBy(() -> dataTableRowService.importCsv("orders", "name\nx\n", 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "*DataTableRowServiceEnforcementTest"`
Expected: FAIL — constructor has 2 args, not 3.

- [ ] **Step 2b: Find any other `new DataTableRowServiceImpl(` call sites**

Run: `grep -rn "new DataTableRowServiceImpl(" server --include='*.java'`
If any existing test constructs it with the old 2-arg form, update it to pass a `mock(DataTableStorageService.class)` (or a real instance) as the third argument in Step 3.

- [ ] **Step 3: Add the dependency to the constructor**

In `DataTableRowServiceImpl`, add the field and constructor param:

```java
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DataTableStorageService dataTableStorageService;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableRowServiceImpl(
        ApplicationEventPublisher applicationEventPublisher, JdbcTemplate jdbcTemplate,
        DataTableStorageService dataTableStorageService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.dataTableStorageService = dataTableStorageService;
    }
```

- [ ] **Step 4: Guard `insertRow`**

Make `checkWithinLimit(0)` the FIRST statement of `insertRow`, before `validateBaseName`:

```java
    public DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId) {
        dataTableStorageService.checkWithinLimit(0);

        validateBaseName(baseName);
        // ... rest unchanged
```

- [ ] **Step 5: Guard `importCsv`**

Make the estimate check the FIRST statement of `importCsv`, before `validateBaseName`:

```java
    public void importCsv(String baseName, String csv, long environmentId) {
        dataTableStorageService.checkWithinLimit(csv == null ? 0 : csv.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

        validateBaseName(baseName);
        // ... rest unchanged
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "*DataTableRowServiceEnforcementTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-data-table
git commit -m "Enforce data table storage limit on insert and CSV import"
```

---

### Task 3: Expose DataTable usage over GraphQL

**Files:**
- Modify: `server/libs/automation/automation-data-table/automation-data-table-api/src/main/java/com/bytechef/automation/data/table/configuration/facade/WorkspaceDataTableFacade.java`
- Modify: `server/libs/automation/automation-data-table/automation-data-table-service/src/main/java/com/bytechef/automation/data/table/configuration/facade/WorkspaceDataTableFacadeImpl.java`
- Modify: `server/libs/automation/automation-data-table/automation-data-table-graphql/src/main/resources/graphql/data-table.graphqls`
- Modify: `server/libs/automation/automation-data-table/automation-data-table-graphql/src/main/java/com/bytechef/automation/data/table/web/graphql/DataTableGraphQlController.java`
- Test: `server/libs/automation/automation-data-table/automation-data-table-graphql/src/test/java/com/bytechef/automation/data/table/web/graphql/DataTableStorageUsageGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `DataTableStorageService#getUsage()` (Task 1), `DataTableStorageUsage` (Task 1).
- Produces: GraphQL query `dataTableStorageUsage: DataTableStorageUsage!`; facade method `WorkspaceDataTableFacade#getStorageUsage(): DataTableStorageUsage`.

- [ ] **Step 1: Add the facade interface method**

In `WorkspaceDataTableFacade.java` add the import and method:

```java
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
```
```java
    DataTableStorageUsage getStorageUsage();
```

- [ ] **Step 2: Implement it in `WorkspaceDataTableFacadeImpl`**

Add the import, a `DataTableStorageService` field + constructor param (following the existing constructor pattern — add `dataTableStorageService` to the parameter list and `this.dataTableStorageService = dataTableStorageService;`), then:

```java
    @Override
    public DataTableStorageUsage getStorageUsage() {
        return dataTableStorageService.getUsage();
    }
```

- [ ] **Step 3: Extend the GraphQL schema**

In `data-table.graphqls`, add to the `extend type Query {` block:

```graphql
    dataTableStorageUsage: DataTableStorageUsage!
```

And add the type:

```graphql
type DataTableStorageUsage {
    usedBytes: Long!
    limitBytes: Long!
    percentage: Float!
    unlimited: Boolean!
}
```

- [ ] **Step 4: Add the resolver**

In `DataTableGraphQlController`, add the import `com.bytechef.platform.data.table.domain.DataTableStorageUsage` and:

```java
    @QueryMapping
    public DataTableStorageUsage dataTableStorageUsage() {
        return workspaceDataTableFacade.getStorageUsage();
    }
```

(The domain record's components — `usedBytes`, `limitBytes`, `percentage`, `unlimited` — map directly onto the GraphQL type fields, so no separate output record is needed.)

- [ ] **Step 5: Write the resolver test**

```java
package com.bytechef.automation.data.table.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import org.junit.jupiter.api.Test;

class DataTableStorageUsageGraphQlControllerTest {

    @Test
    void testDataTableStorageUsageReturnsFacadeValue() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);

        when(facade.getStorageUsage()).thenReturn(new DataTableStorageUsage(10L, 100L, 10.0, false));

        DataTableGraphQlController controller =
            new DataTableGraphQlController(mock(EnvironmentService.class), facade);

        DataTableStorageUsage usage = controller.dataTableStorageUsage();

        assertThat(usage.percentage()).isEqualTo(10.0);
        assertThat(usage.limitBytes()).isEqualTo(100L);
    }
}
```

- [ ] **Step 6: Run the test**

Run: `./gradlew :server:libs:automation:automation-data-table:automation-data-table-graphql:test --tests "*DataTableStorageUsageGraphQlControllerTest"`
Expected: PASS.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-data-table
git commit -m "Expose dataTableStorageUsage GraphQL query"
```

---

### Task 4: Add `document_size` column + entity field (KnowledgeBase)

**Files:**
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/resources/config/liquibase/changelog/platform/knowledge_base/20260705000001_platform_knowledge_base_document_size.xml`
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/domain/KnowledgeBaseDocument.java`

**Interfaces:**
- Produces: `KnowledgeBaseDocument#getDocumentSize(): Long`, `KnowledgeBaseDocument#setDocumentSize(Long)`; new nullable column `knowledge_base_document.document_size BIGINT`.

Note: `master.xml` uses `<includeAll path=".../platform/knowledge_base/">`, so a new changelog file dropped into that directory is auto-registered (ordered by filename). No master.xml edit needed.

- [ ] **Step 1: Create the changelog**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd"
                   logicalFilePath="config/liquibase/changelog/platform/knowledge_base/20260705000001_platform_knowledge_base_document_size.xml">

    <changeSet id="20260705000001-1" author="Ivica Cardic">
        <addColumn tableName="knowledge_base_document">
            <column name="document_size" type="BIGINT"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Add the entity field**

In `KnowledgeBaseDocument.java`, add the import and field (after the `status` field):

```java
import org.springframework.data.relational.core.mapping.Column;
```
```java
    @Column("document_size")
    private Long documentSize;
```

Add the getter/setter (after `setStatus`):

```java
    public Long getDocumentSize() {
        return documentSize;
    }

    public void setDocumentSize(Long documentSize) {
        this.documentSize = documentSize;
    }
```

Add `documentSize` to the `toString()` (after `status`): `", documentSize=" + documentSize +`.

- [ ] **Step 3: Delete stale build resources so Liquibase does not see the old classpath copy**

```bash
rm -rf server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/build/resources
```

- [ ] **Step 4: Compile to verify the entity mapping is valid**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-knowledge-base
git commit -m "Add document_size column and entity field to knowledge_base_document"
```

---

### Task 5: `KnowledgeBaseStorageService` + config (platform)

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (add `maxSizeBytes` to the `Ai.KnowledgeBase` class at line 1275)
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/domain/KnowledgeBaseStorageUsage.java`
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/exception/KnowledgeBaseStorageLimitExceededException.java`
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseStorageService.java`
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseStorageServiceImpl.java`
- Test: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseStorageServiceTest.java`

**Interfaces:**
- Produces:
  - `record KnowledgeBaseStorageUsage(long usedBytes, long limitBytes, double percentage, boolean unlimited)`
  - `KnowledgeBaseStorageLimitExceededException extends RuntimeException`
  - `KnowledgeBaseStorageService`: `KnowledgeBaseStorageUsage getUsage()`, `void checkWithinLimit(long incomingBytes)`
  - `ApplicationProperties.Ai.KnowledgeBase#getMaxSizeBytes(): long` (path `bytechef.ai.knowledge-base.max-size-bytes`, default `1_073_741_824`)

- [ ] **Step 1: Add config to `Ai.KnowledgeBase`**

In `ApplicationProperties.java`, inside `public static class KnowledgeBase` (line 1275), add the field after `enabled`:

```java
            /**
             * Maximum total size in bytes of all knowledge base documents in a tenant. 0 means unlimited. Default 1 GB.
             */
            private long maxSizeBytes = 1_073_741_824L;
```

Add the getter/setter after `setEnabled`:

```java
            public long getMaxSizeBytes() {
                return maxSizeBytes;
            }

            public void setMaxSizeBytes(long maxSizeBytes) {
                this.maxSizeBytes = maxSizeBytes;
            }
```

- [ ] **Step 2: Create `KnowledgeBaseStorageUsage`**

```java
package com.bytechef.platform.knowledgebase.domain;

/**
 * @author Ivica Cardic
 */
public record KnowledgeBaseStorageUsage(long usedBytes, long limitBytes, double percentage, boolean unlimited) {
}
```

- [ ] **Step 3: Create `KnowledgeBaseStorageLimitExceededException`**

```java
package com.bytechef.platform.knowledgebase.exception;

/**
 * @author Ivica Cardic
 */
public class KnowledgeBaseStorageLimitExceededException extends RuntimeException {

    public KnowledgeBaseStorageLimitExceededException(long usedBytes, long limitBytes) {
        super(
            "Knowledge base storage limit reached (" + (usedBytes / 1_048_576) + " MB of " + (limitBytes / 1_048_576) +
                " MB used). Delete documents or increase bytechef.ai.knowledge-base.max-size-bytes.");
    }
}
```

- [ ] **Step 4: Create the `KnowledgeBaseStorageService` interface**

```java
package com.bytechef.platform.knowledgebase.service;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;

/**
 * @author Ivica Cardic
 */
public interface KnowledgeBaseStorageService {

    KnowledgeBaseStorageUsage getUsage();

    void checkWithinLimit(long incomingBytes);
}
```

- [ ] **Step 5: Write the failing test**

```java
package com.bytechef.platform.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class KnowledgeBaseStorageServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private KnowledgeBaseStorageServiceImpl createService(long limit, long used) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getKnowledgeBase()
            .setMaxSizeBytes(limit);

        when(jdbcTemplate.queryForObject(eq(KnowledgeBaseStorageServiceImpl.USAGE_SQL), eq(Long.class)))
            .thenReturn(used);

        return new KnowledgeBaseStorageServiceImpl(applicationProperties, jdbcTemplate);
    }

    @Test
    void testGetUsageComputesPercentage() {
        KnowledgeBaseStorageUsage usage = createService(1_000L, 800L).getUsage();

        assertThat(usage.percentage()).isEqualTo(80.0);
        assertThat(usage.unlimited()).isFalse();
    }

    @Test
    void testCheckWithinLimitThrowsWhenIncomingExceeds() {
        assertThatThrownBy(() -> createService(1_000L, 900L).checkWithinLimit(200))
            .isInstanceOf(KnowledgeBaseStorageLimitExceededException.class);
    }

    @Test
    void testCheckWithinLimitPassesAtExactBoundary() {
        createService(1_000L, 900L).checkWithinLimit(100);
    }

    @Test
    void testCheckWithinLimitPassesWhenUnlimited() {
        createService(0L, 999_999L).checkWithinLimit(1_000_000);
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test --tests "*KnowledgeBaseStorageServiceTest"`
Expected: FAIL — `KnowledgeBaseStorageServiceImpl` does not exist.

- [ ] **Step 7: Create `KnowledgeBaseStorageServiceImpl`**

```java
package com.bytechef.platform.knowledgebase.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class KnowledgeBaseStorageServiceImpl implements KnowledgeBaseStorageService {

    static final String USAGE_SQL = "SELECT COALESCE(SUM(document_size), 0) FROM knowledge_base_document";

    private final ApplicationProperties applicationProperties;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseStorageServiceImpl(ApplicationProperties applicationProperties, JdbcTemplate jdbcTemplate) {
        this.applicationProperties = applicationProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public KnowledgeBaseStorageUsage getUsage() {
        long limit = getLimit();
        long used = currentUsageBytes();

        boolean unlimited = limit <= 0;
        double percentage = unlimited ? 0.0 : used * 100.0 / limit;

        return new KnowledgeBaseStorageUsage(used, limit, percentage, unlimited);
    }

    @Override
    public void checkWithinLimit(long incomingBytes) {
        long limit = getLimit();

        if (limit <= 0) {
            return;
        }

        long used = currentUsageBytes();

        if (used + incomingBytes > limit) {
            throw new KnowledgeBaseStorageLimitExceededException(used, limit);
        }
    }

    private long getLimit() {
        return applicationProperties.getAi()
            .getKnowledgeBase()
            .getMaxSizeBytes();
    }

    private long currentUsageBytes() {
        Long used = jdbcTemplate.queryForObject(USAGE_SQL, Long.class);

        return used == null ? 0 : used;
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test --tests "*KnowledgeBaseStorageServiceTest"`
Expected: PASS (4 tests).

- [ ] **Step 9: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/config/app-config server/libs/platform/platform-knowledge-base
git commit -m "Add KnowledgeBaseStorageService and knowledge-base.max-size-bytes config"
```

---

### Task 6: Enforce the KB limit + persist size in the document facade

**Files:**
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/facade/KnowledgeBaseDocumentFacade.java` (signature)
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/java/com/bytechef/platform/knowledgebase/facade/KnowledgeBaseDocumentFacadeImpl.java`
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-rest/src/main/java/com/bytechef/platform/knowledgebase/web/rest/KnowledgeBaseDocumentApiController.java` (caller)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/AddKnowledgeBaseDocumentToolCallback.java` (caller)
- Test: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/facade/KnowledgeBaseDocumentFacadeTest.java`

**Interfaces:**
- Consumes: `KnowledgeBaseStorageService#checkWithinLimit(long)` (Task 5), `KnowledgeBaseDocument#setDocumentSize(Long)` (Task 4).
- Produces: new facade signature `createKnowledgeBaseDocument(Long knowledgeBaseId, String filename, String contentType, long size, InputStream inputStream)`.

- [ ] **Step 1: Change the interface signature**

In `KnowledgeBaseDocumentFacade.java`, change the method to:

```java
    KnowledgeBaseDocument createKnowledgeBaseDocument(
        Long knowledgeBaseId, String filename, String contentType, long size, InputStream inputStream);
```

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.platform.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseStorageService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseVectorStoreMetadataService;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;

class KnowledgeBaseDocumentFacadeTest {

    private final KnowledgeBaseFileStorage fileStorage = mock(KnowledgeBaseFileStorage.class);
    private final KnowledgeBaseDocumentService documentService = mock(KnowledgeBaseDocumentService.class);
    private final KnowledgeBaseStorageService storageService = mock(KnowledgeBaseStorageService.class);

    private KnowledgeBaseDocumentFacadeImpl createFacade() {
        return new KnowledgeBaseDocumentFacadeImpl(
            mock(ApplicationEventPublisher.class), mock(KnowledgeBaseDocumentChunkService.class), documentService,
            mock(KnowledgeBaseDocumentTagService.class), fileStorage,
            mock(KnowledgeBaseVectorStoreMetadataService.class), mock(VectorStore.class), storageService);
    }

    @Test
    void testCreateBlockedWhenOverLimit() {
        doThrow(new KnowledgeBaseStorageLimitExceededException(2_000L, 1_000L))
            .when(storageService)
            .checkWithinLimit(500);

        assertThatThrownBy(() -> createFacade().createKnowledgeBaseDocument(
            1L, "a.txt", "text/plain", 500, new ByteArrayInputStream(new byte[0])))
            .isInstanceOf(KnowledgeBaseStorageLimitExceededException.class);

        verifyNoInteractions(fileStorage);
    }

    @Test
    void testCreatePersistsDocumentSize() {
        when(fileStorage.storeDocument(eq("a.txt"), any())).thenReturn(mock(FileEntry.class));
        when(documentService.saveKnowledgeBaseDocument(any())).thenAnswer(invocation -> invocation.getArgument(0));

        createFacade().createKnowledgeBaseDocument(
            1L, "a.txt", "text/plain", 500, new ByteArrayInputStream(new byte[0]));

        ArgumentCaptor<KnowledgeBaseDocument> captor = ArgumentCaptor.forClass(KnowledgeBaseDocument.class);

        verify(documentService).saveKnowledgeBaseDocument(captor.capture());

        org.assertj.core.api.Assertions.assertThat(captor.getValue()
            .getDocumentSize())
            .isEqualTo(500L);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test --tests "*KnowledgeBaseDocumentFacadeTest"`
Expected: FAIL — constructor arity / signature mismatch.

- [ ] **Step 4: Update `KnowledgeBaseDocumentFacadeImpl`**

Add the storage-service field + constructor param (append `KnowledgeBaseStorageService knowledgeBaseStorageService` to the constructor and assign it), then rewrite `createKnowledgeBaseDocument`:

```java
    @Override
    public KnowledgeBaseDocument createKnowledgeBaseDocument(
        Long knowledgeBaseId, String filename, String contentType, long size, InputStream inputStream) {

        knowledgeBaseStorageService.checkWithinLimit(size);

        FileEntry fileEntry = knowledgeBaseFileStorage.storeDocument(filename, inputStream);

        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setKnowledgeBaseId(knowledgeBaseId);
        knowledgeBaseDocument.setName(filename);
        knowledgeBaseDocument.setDocument(fileEntry);
        knowledgeBaseDocument.setDocumentSize(size);
        knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);

        knowledgeBaseDocument = knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

        eventPublisher.publishEvent(new KnowledgeBaseDocumentEvent(knowledgeBaseDocument.getId()));

        return knowledgeBaseDocument;
    }
```

- [ ] **Step 5: Update the REST caller**

In `KnowledgeBaseDocumentApiController.uploadDocument`:

```java
        return ResponseEntity.ok(
            knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
                id, file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream()));
```

- [ ] **Step 6: Update the AI Hub caller**

In `AddKnowledgeBaseDocumentToolCallback` (around line 161):

```java
            KnowledgeBaseDocument document = knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
                knowledgeBaseId, input.name(), input.mimeType(), contentBytes.length,
                new ByteArrayInputStream(contentBytes));
```

- [ ] **Step 7: Run the facade test**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test --tests "*KnowledgeBaseDocumentFacadeTest"`
Expected: PASS (2 tests).

- [ ] **Step 8: Compile the two caller modules**

Run: `./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-rest:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: BUILD SUCCESSFUL (confirms both call sites updated).

- [ ] **Step 9: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-knowledge-base server/ee/libs/ai/ai-hub
git commit -m "Enforce knowledge base storage limit and persist document size on upload"
```

---

### Task 7: Expose KB usage over GraphQL

**Files:**
- Modify: `server/libs/automation/automation-knowledge-base/automation-knowledge-base-api/src/main/java/com/bytechef/automation/knowledgebase/facade/WorkspaceKnowledgeBaseFacade.java`
- Modify: `server/libs/automation/automation-knowledge-base/automation-knowledge-base-service/src/main/java/com/bytechef/automation/knowledgebase/facade/WorkspaceKnowledgeBaseFacadeImpl.java`
- Modify: `server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/main/resources/graphql/automation/knowledge-base/knowledge-base.graphqls`
- Modify: `server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/main/java/com/bytechef/automation/knowledgebase/web/graphql/KnowledgeBaseGraphQlController.java`
- Test: `server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/test/java/com/bytechef/automation/knowledgebase/web/graphql/KnowledgeBaseStorageUsageGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `KnowledgeBaseStorageService#getUsage()` (Task 5).
- Produces: GraphQL query `knowledgeBaseStorageUsage: KnowledgeBaseStorageUsage!`; facade method `WorkspaceKnowledgeBaseFacade#getStorageUsage(): KnowledgeBaseStorageUsage`.

- [ ] **Step 1: Add the facade interface method**

In `WorkspaceKnowledgeBaseFacade.java` add the import and method:

```java
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;
```
```java
    KnowledgeBaseStorageUsage getStorageUsage();
```

- [ ] **Step 2: Implement it in `WorkspaceKnowledgeBaseFacadeImpl`**

Add the import, a `KnowledgeBaseStorageService` field + constructor param (following the existing constructor pattern), then:

```java
    @Override
    public KnowledgeBaseStorageUsage getStorageUsage() {
        return knowledgeBaseStorageService.getUsage();
    }
```

- [ ] **Step 3: Extend the GraphQL schema**

In `knowledge-base.graphqls`, add to the `extend type Query {` block:

```graphql
    knowledgeBaseStorageUsage: KnowledgeBaseStorageUsage!
```

And add the type:

```graphql
type KnowledgeBaseStorageUsage {
    usedBytes: Long!
    limitBytes: Long!
    percentage: Float!
    unlimited: Boolean!
}
```

- [ ] **Step 4: Add the resolver**

In `KnowledgeBaseGraphQlController`, add the import `com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage` and:

```java
    @QueryMapping
    public KnowledgeBaseStorageUsage knowledgeBaseStorageUsage() {
        return workspaceKnowledgeBaseFacade.getStorageUsage();
    }
```

- [ ] **Step 5: Write the resolver test**

```java
package com.bytechef.automation.knowledgebase.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.ai.provider.embedding.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseStorageUsage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class KnowledgeBaseStorageUsageGraphQlControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void testKnowledgeBaseStorageUsageReturnsFacadeValue() {
        WorkspaceKnowledgeBaseFacade facade = mock(WorkspaceKnowledgeBaseFacade.class);

        when(facade.getStorageUsage()).thenReturn(new KnowledgeBaseStorageUsage(800L, 1_000L, 80.0, false));

        KnowledgeBaseGraphQlController controller = new KnowledgeBaseGraphQlController(
            mock(ObjectProvider.class), mock(EnvironmentService.class),
            mock(KnowledgeBaseDocumentService.class), facade);

        KnowledgeBaseStorageUsage usage = controller.knowledgeBaseStorageUsage();

        assertThat(usage.percentage()).isEqualTo(80.0);
    }
}
```

Note: verify the `EmbeddingProviderStatusProvider` import path against the controller's actual import; adjust if the package differs.

- [ ] **Step 6: Run the test**

Run: `./gradlew :server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql:test --tests "*KnowledgeBaseStorageUsageGraphQlControllerTest"`
Expected: PASS.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-knowledge-base
git commit -m "Expose knowledgeBaseStorageUsage GraphQL query"
```

---

### Task 8: Client — `warning` alert variant + GraphQL operations & codegen

**Files:**
- Modify: `client/src/components/ui/alert.tsx`
- Create: `client/src/graphql/automation/datatable/dataTableStorageUsage.graphql`
- Create: `client/src/graphql/automation/knowledge-base/knowledgeBaseStorageUsage.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts` (+ `graphql-types.ts`) via codegen

**Interfaces:**
- Consumes: server GraphQL queries `dataTableStorageUsage` (Task 3) and `knowledgeBaseStorageUsage` (Task 7) — both must be committed first so codegen sees them.
- Produces: generated hooks `useDataTableStorageUsageQuery`, `useKnowledgeBaseStorageUsageQuery`; `Alert` `variant="warning"`.

- [ ] **Step 1: Add the `warning` variant to `alert.tsx`**

In the `alertVariants` `cva` `variant` map, add after `success`:

```tsx
        warning:
          "border-amber-500/50 text-amber-700 dark:border-amber-500 dark:text-amber-500 [&>svg]:text-amber-600",
```

- [ ] **Step 2: Create the DataTable usage operation**

`client/src/graphql/automation/datatable/dataTableStorageUsage.graphql`:

```graphql
query DataTableStorageUsage {
    dataTableStorageUsage {
        limitBytes
        percentage
        unlimited
        usedBytes
    }
}
```

- [ ] **Step 3: Create the KnowledgeBase usage operation**

`client/src/graphql/automation/knowledge-base/knowledgeBaseStorageUsage.graphql`:

```graphql
query KnowledgeBaseStorageUsage {
    knowledgeBaseStorageUsage {
        limitBytes
        percentage
        unlimited
        usedBytes
    }
}
```

- [ ] **Step 4: Regenerate GraphQL types & hooks**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` now contains `useDataTableStorageUsageQuery` and `useKnowledgeBaseStorageUsageQuery`. Verify:

```bash
grep -n "useDataTableStorageUsageQuery\|useKnowledgeBaseStorageUsageQuery" client/src/shared/middleware/graphql.ts
```

- [ ] **Step 5: Typecheck**

Run: `cd client && npm run typecheck`
Expected: no errors.

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add client/src/components/ui/alert.tsx client/src/graphql client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "client - Add storage usage GraphQL operations and warning alert variant"
```

---

### Task 9: Client — `StorageUsageBanner` component + wire into both pages

**Files:**
- Create: `client/src/shared/components/StorageUsageBanner.tsx`
- Test: `client/src/shared/components/StorageUsageBanner.test.tsx`
- Modify: `client/src/pages/automation/datatables/DataTables.tsx`
- Modify: `client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx`

**Interfaces:**
- Consumes: `Alert` `warning` variant (Task 8); generated usage hooks (Task 8).
- Produces: `StorageUsageBanner` component taking `{label, percentage, usedBytes, limitBytes, unlimited}`.

- [ ] **Step 1: Write the failing component test**

```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import StorageUsageBanner from './StorageUsageBanner';

describe('StorageUsageBanner', () => {
    it('renders when usage is at or above 80 percent', () => {
        render(
            <StorageUsageBanner
                label="Data table"
                limitBytes={52_428_800}
                percentage={87}
                unlimited={false}
                usedBytes={45_613_056}
            />
        );

        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(screen.getByText(/87%/)).toBeInTheDocument();
    });

    it('renders nothing below 80 percent', () => {
        const {container} = render(
            <StorageUsageBanner
                label="Data table"
                limitBytes={52_428_800}
                percentage={50}
                unlimited={false}
                usedBytes={26_214_400}
            />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing when unlimited', () => {
        const {container} = render(
            <StorageUsageBanner
                label="Data table"
                limitBytes={0}
                percentage={0}
                unlimited={true}
                usedBytes={99_999_999}
            />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npm run test -- StorageUsageBanner`
Expected: FAIL — module not found.

- [ ] **Step 3: Create `StorageUsageBanner.tsx`**

```tsx
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {TriangleAlertIcon} from 'lucide-react';

interface StorageUsageBannerProps {
    label: string;
    limitBytes: number;
    percentage: number;
    unlimited: boolean;
    usedBytes: number;
}

const WARNING_THRESHOLD = 80;

const formatMegabytes = (bytes: number) => `${Math.round(bytes / 1_048_576)} MB`;

const StorageUsageBanner = ({label, limitBytes, percentage, unlimited, usedBytes}: StorageUsageBannerProps) => {
    if (unlimited || percentage < WARNING_THRESHOLD) {
        return null;
    }

    return (
        <Alert className="m-4 mb-0 w-auto" variant="warning">
            <TriangleAlertIcon />

            <AlertTitle>{`${label} storage is at ${Math.round(percentage)}%`}</AlertTitle>

            <AlertDescription>
                <span>
                    {`Using ${formatMegabytes(usedBytes)} of ${formatMegabytes(limitBytes)}. New items are blocked once the limit is reached — delete items or increase the limit.`}
                </span>
            </AlertDescription>
        </Alert>
    );
};

export default StorageUsageBanner;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npm run test -- StorageUsageBanner`
Expected: PASS (3 tests).

- [ ] **Step 5: Wire into `DataTables.tsx`**

Add imports (alphabetically placed):

```tsx
import StorageUsageBanner from '@/shared/components/StorageUsageBanner';
import {useDataTableStorageUsageQuery} from '@/shared/middleware/graphql';
```

Inside the component, before `return`:

```tsx
    const {data: storageUsageData} = useDataTableStorageUsageQuery();

    const storageUsage = storageUsageData?.dataTableStorageUsage;
```

Render the banner as the first child inside `<PageLoader …>`:

```tsx
            <PageLoader errors={[error]} loading={isLoading}>
                {storageUsage && (
                    <StorageUsageBanner
                        label="Data table"
                        limitBytes={storageUsage.limitBytes}
                        percentage={storageUsage.percentage}
                        unlimited={storageUsage.unlimited}
                        usedBytes={storageUsage.usedBytes}
                    />
                )}

                {filteredTables.length > 0 ? (
```

- [ ] **Step 6: Wire into `KnowledgeBases.tsx`**

Add imports:

```tsx
import StorageUsageBanner from '@/shared/components/StorageUsageBanner';
import {useKnowledgeBaseEmbeddingActiveQuery, useKnowledgeBaseStorageUsageQuery} from '@/shared/middleware/graphql';
```

(Replace the existing single-name `useKnowledgeBaseEmbeddingActiveQuery` import with the combined sorted import above.)

Inside the component, after the `embeddingActive` derivation:

```tsx
    const {data: storageUsageData} = useKnowledgeBaseStorageUsageQuery();

    const storageUsage = storageUsageData?.knowledgeBaseStorageUsage;
```

Render the banner just above the existing embedding `Alert`, inside the `<div className="flex size-full flex-col">`:

```tsx
                <div className="flex size-full flex-col">
                    {storageUsage && (
                        <StorageUsageBanner
                            label="Knowledge base"
                            limitBytes={storageUsage.limitBytes}
                            percentage={storageUsage.percentage}
                            unlimited={storageUsage.unlimited}
                            usedBytes={storageUsage.usedBytes}
                        />
                    )}

                    {!embeddingActive && (
```

- [ ] **Step 7: Full client check**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests all pass.

- [ ] **Step 8: Commit**

```bash
cd client && npm run format
git add client/src/shared/components/StorageUsageBanner.tsx client/src/shared/components/StorageUsageBanner.test.tsx client/src/pages/automation/datatables/DataTables.tsx client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx
git commit -m "client - Show storage usage warning banners on DataTables and KnowledgeBases pages"
```

---

## Final verification

- [ ] Run the full server check for touched modules:

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:check \
  :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:check \
  :server:libs:automation:automation-data-table:automation-data-table-graphql:check \
  :server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql:check
```

- [ ] Run `cd client && npm run check` once more.
- [ ] Manual smoke (optional, needs running stack): set `bytechef.data-table.max-size-bytes` low, import a CSV that pushes over the cap → expect an error toast; observe the warning banner appear at ≥80%.
