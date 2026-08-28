# Embedded/Automation Resource Pool Split — Implementation Plan (sub-project 1, server)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A data table or knowledge base belongs to exactly one pool — AUTOMATION or EMBEDDED — and a workflow run acting for a connected user can never reach the AUTOMATION pool.

**Architecture:** A `platform_type INT` discriminator on both registries, a distinct physical table prefix for embedded data tables, required `PlatformType` parameters replacing the unscoped service overloads, and runtime scoping derived from the **owner** of a run rather than its `PlatformType`.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Liquibase, PostgreSQL 16, JUnit 5, Mockito, Testcontainers, AssertJ.

**Spec:** `docs/superpowers/specs/2026-08-29-embedded-automation-resource-pools-design.md`

## Global Constraints

- `PlatformType` is `com.bytechef.platform.constant.PlatformType` — `AUTOMATION` ordinal 0, `EMBEDDED` ordinal 1.
- Enum fields are **never** mapped directly. Persist the ordinal as `int` with converting accessors, following `Property.scope` and `KnowledgeBase.environment`.
- Physical prefixes: AUTOMATION `dt_<envId>_<baseName>` (unchanged), EMBEDDED `edt_<envId>_<baseName>`.
- Registry keys: `data_table` → `(name, platform_type)`; `knowledge_base` → `(name, platform_type, environment)`.
- **Delete unscoped overloads. Never add a `PlatformType`-defaulting convenience overload.** The compiler enumerating call sites is the mechanism this design relies on.
- Runtime rule: **owner present → EMBEDDED pool only; owner absent → both pools.** Never key on `PlatformType`.
- Fail closed: an unresolvable pool yields nothing, never everything.
- Integration test classes end in `IntTest`; unit test classes end in `Test`. Test method names are camelCase with no underscores.
- Run `./gradlew spotlessApply` on touched modules before every commit.
- Testcontainers needs `DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`.
- Never judge a Gradle run through a pipe. Redirect to a file, check `$?` on its own line, then grep for `^> Task .* FAILED`.
- EE files (`server/ee/`) use the ByteChef Enterprise licence header and carry `@version ee`.

---

## File Structure

**Data table discriminator (Task 1–2)**
- Create: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/resources/config/liquibase/changelog/platform/data_table/20260829000001_platform_data_table_add_platform_type.xml`
- Modify: `.../platform-data-table-api/.../configuration/domain/DataTable.java` — the `platformType` field
- Modify: `.../platform-data-table-service/.../configuration/repository/DataTableRepository.java` — pool-aware finders
- Modify: `.../platform-data-table-api/.../configuration/service/DataTableService.java` — signatures
- Modify: `.../platform-data-table-service/.../configuration/service/DataTableServiceImpl.java` — naming, keying, listing
- Modify: `server/ee/libs/platform/platform-data-table/platform-data-table-remote-client/.../RemoteDataTableServiceClient.java` — stub parity

**Automation callers (Task 3)**
- Modify: `server/libs/automation/automation-data-table/automation-data-table-service/.../facade/WorkspaceDataTableFacadeImpl.java`
- Modify: `.../automation-data-table-service/.../search/DataTableSearchAssetProvider.java`

**Runtime scoping (Task 4–5)**
- Modify: `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/util/DataTableUtils.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/.../domain/RowOwnerFilter.java`
- Modify: `.../platform-data-table-api/.../execution/service/DataTableRowService.java` and its impl — the pool on all 14 methods

**Knowledge base (Task 6)**
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/resources/config/liquibase/changelog/platform/knowledge_base/20260829000001_platform_knowledge_base_add_platform_type.xml`
- Modify: `.../platform-knowledge-base-api/.../domain/KnowledgeBase.java`, `.../service/KnowledgeBaseService.java`, `.../KnowledgeBaseServiceImpl.java`

**Embedded create (Task 7)**
- Modify: `server/ee/libs/embedded/embedded-data-table-graphql/**`, `server/ee/libs/embedded/embedded-knowledge-base-graphql/**`

**Palette (Task 8)**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/.../filter/IntegrationComponentDefinitionFilter.java`

---

## Task 1: The `platform_type` column on `data_table`

**Files:**
- Create: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/resources/config/liquibase/changelog/platform/data_table/20260829000001_platform_data_table_add_platform_type.xml`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/configuration/domain/DataTable.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/configuration/repository/DataTableRepository.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/configuration/service/DataTablePlatformTypeIntTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `DataTable#getPlatformType(): PlatformType`, `DataTable#setPlatformType(PlatformType)`, `DataTableRepository#findByNameAndPlatformType(String, int): Optional<DataTable>`.

- [ ] **Step 1: Write the failing test**

Create `DataTablePlatformTypeIntTest.java`:

```java
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTablePlatformTypeIntTest {

    @Autowired
    private DataTableRepository dataTableRepository;

    @Test
    void testTheSameNameIsAllowedInBothPools() {
        dataTableRepository.save(dataTable("shared", PlatformType.AUTOMATION));
        dataTableRepository.save(dataTable("shared", PlatformType.EMBEDDED));

        assertThat(dataTableRepository.findByNameAndPlatformType("shared", PlatformType.AUTOMATION.ordinal()))
            .isPresent();
        assertThat(dataTableRepository.findByNameAndPlatformType("shared", PlatformType.EMBEDDED.ordinal()))
            .isPresent();
    }

    @Test
    void testTheSameNameTwiceInOnePoolIsRejected() {
        dataTableRepository.save(dataTable("dupe", PlatformType.AUTOMATION));

        assertThatThrownBy(() -> dataTableRepository.save(dataTable("dupe", PlatformType.AUTOMATION)))
            .isInstanceOf(DbActionExecutionException.class);
    }

    @Test
    void testAnExistingRowDefaultsToAutomation() {
        DataTable dataTable = new DataTable();

        dataTable.setName("defaulted");

        DataTable saved = dataTableRepository.save(dataTable);

        assertThat(saved.getPlatformType()).isEqualTo(PlatformType.AUTOMATION);
    }

    private static DataTable dataTable(String name, PlatformType platformType) {
        DataTable dataTable = new DataTable();

        dataTable.setName(name);
        dataTable.setPlatformType(platformType);

        return dataTable;
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests "*DataTablePlatformTypeIntTest*" > /tmp/t1.log 2>&1; echo $?; grep -E "Results:|error:" /tmp/t1.log
```

Expected: compilation failure — `setPlatformType` and `findByNameAndPlatformType` do not exist.

- [ ] **Step 3: Add the changelog**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260829000001-1" author="Ivica Cardic">
        <addColumn tableName="data_table">
            <column name="platform_type" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

    <changeSet id="20260829000001-2" author="Ivica Cardic">
        <dropUniqueConstraint tableName="data_table" constraintName="uk_data_table_name"/>

        <addUniqueConstraint
            tableName="data_table"
            columnNames="name, platform_type"
            constraintName="uk_data_table_name_platform_type"/>
    </changeSet>
</databaseChangeLog>
```

The `defaultValueNumeric="0"` **is** the migration — every existing row lands on AUTOMATION. Leave the default on the column so a writer that forgets the field gets automation rather than a constraint violation.

- [ ] **Step 4: Add the domain field**

In `DataTable.java`, alongside the existing `ownerType` handling:

```java
    @Column("platform_type")
    private int platformType;

    public PlatformType getPlatformType() {
        return PlatformType.values()[platformType];
    }

    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType.ordinal();
    }
```

`int`, not `Integer` — unlike `owner_type` there is no meaningful null, and a primitive reads the DEFAULT 0 as AUTOMATION.

- [ ] **Step 5: Add the repository finder**

In `DataTableRepository.java`:

```java
    Optional<DataTable> findByNameAndPlatformType(String name, int platformType);
```

- [ ] **Step 6: Run the test and confirm it passes**

Same command as Step 2. Expected: `Results: SUCCESS (3 tests, 3 successes)`.

- [ ] **Step 7: Commit**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:spotlessApply
git add server/libs/platform/platform-data-table
git commit -m "--- Add the platform type discriminator to data_table"
```

---

## Task 2: Pool-aware naming, keying and listing

**Files:**
- Modify: `.../platform-data-table-api/.../configuration/service/DataTableService.java`
- Modify: `.../platform-data-table-service/.../configuration/service/DataTableServiceImpl.java`
- Modify: `server/ee/libs/platform/platform-data-table/platform-data-table-remote-client/.../RemoteDataTableServiceClient.java`
- Test: `.../platform-data-table-service/src/test/java/.../DataTablePoolIsolationIntTest.java`

**Interfaces:**
- Consumes: `DataTable#getPlatformType()`, `DataTableRepository#findByNameAndPlatformType`.
- Produces:
  - `createTable(String baseName, String description, List<ColumnSpec> columnSpecs, long environmentId, PlatformType platformType)`
  - `listTables(long environmentId, PlatformType platformType, Optional<Owner> owner)`
  - `getIdByBaseName(String baseName, PlatformType platformType)`
  - `getBaseNameById(long id)` — unchanged, the id already identifies the pool
  - `duplicateTable(String fromBaseName, String toBaseName, long environmentId, PlatformType platformType)`
  - `assignOwner(long dataTableId, @Nullable Owner owner)` — unchanged

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTablePoolIsolationIntTest {

    private static final long ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testEachPoolGetsItsOwnPhysicalPrefix() {
        dataTableService.createTable(
            "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.AUTOMATION);
        dataTableService.createTable(
            "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        assertThat(physicalTableExists("dt_0_orders")).isTrue();
        assertThat(physicalTableExists("edt_0_orders")).isTrue();
    }

    @Test
    void testNeitherPoolListsTheOther() {
        dataTableService.createTable(
            "automationOnly".toLowerCase(), null, List.of(new ColumnSpec("a", ColumnType.STRING)),
            ENVIRONMENT_ID, PlatformType.AUTOMATION);
        dataTableService.createTable(
            "embeddedonly", null, List.of(new ColumnSpec("a", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        assertThat(baseNames(PlatformType.AUTOMATION))
            .contains("automationonly")
            .doesNotContain("embeddedonly");
        assertThat(baseNames(PlatformType.EMBEDDED))
            .contains("embeddedonly")
            .doesNotContain("automationonly");
    }

    @Test
    void testTheColumnAndThePrefixAgree() {
        dataTableService.createTable(
            "agreeing", null, List.of(new ColumnSpec("a", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        long id = dataTableService.getIdByBaseName("agreeing", PlatformType.EMBEDDED);

        Integer storedOrdinal = jdbcTemplate.queryForObject(
            "SELECT platform_type FROM data_table WHERE id = ?", Integer.class, id);

        assertThat(storedOrdinal).isEqualTo(PlatformType.EMBEDDED.ordinal());
        assertThat(physicalTableExists("edt_0_agreeing")).isTrue();
        assertThat(physicalTableExists("dt_0_agreeing")).isFalse();
    }

    private List<String> baseNames(PlatformType platformType) {
        return dataTableService.listTables(ENVIRONMENT_ID, platformType, Optional.empty())
            .stream()
            .map(DataTableInfo::baseName)
            .toList();
    }

    private boolean physicalTableExists(String physicalName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() "
                + "AND table_name = ?",
            Integer.class, physicalName);

        return count != null && count > 0;
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests "*DataTablePoolIsolationIntTest*" > /tmp/t2.log 2>&1; echo $?; grep -E "Results:|error:" /tmp/t2.log
```

Expected: compilation failure — the five-argument `createTable` does not exist.

- [ ] **Step 3: Make the physical name pool-aware**

In `DataTableServiceImpl`, replace `buildPhysicalName(long, String)`:

```java
    private static String buildPhysicalName(PlatformType platformType, long environmentId, String baseName) {
        return prefix(platformType, environmentId) + baseName;
    }

    /**
     * The pool lives in the physical name as well as in the registry column, so a name may repeat across pools and
     * the separation survives a query that forgets to filter. The two must never disagree.
     */
    private static String prefix(PlatformType platformType, long environmentId) {
        return (platformType == PlatformType.EMBEDDED ? "edt_" : "dt_") + environmentId + "_";
    }
```

All eight existing `buildPhysicalName(environmentId, baseName)` call sites take the pool their enclosing method now receives.

- [ ] **Step 4: Thread `PlatformType` through the service**

In `DataTableService`, **delete** `createTable(String, List<ColumnSpec>, long)`, `createTable(String, String, List<ColumnSpec>, long)`, `listTables(long)`, `listTables(long, Optional<Owner>)`, `getIdByBaseName(String)` and `duplicateTable(String, String, long)`, replacing them with the signatures in **Interfaces** above. Do not keep defaulting overloads — the compile errors are the audit.

In `DataTableServiceImpl.listTables`, take the prefix from the pool and filter the registry row on it:

```java
    @Override
    public List<DataTableInfo> listTables(long environmentId, PlatformType platformType, Optional<Owner> owner) {
        String prefix = prefix(platformType, environmentId);

        // LIKE treats _ as a wildcard, so the startsWith below is the real guard; the pattern only narrows the scan.
        String sqlTables = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE ?";

        List<String> tableNames = jdbcTemplate.query(
            sqlTables, ps -> ps.setString(1, prefix + "%"), (rs, rowNum) -> rs.getString("table_name"));

        List<DataTableInfo> dataTableInfos = new ArrayList<>();

        for (String tableName : tableNames) {
            if (!tableName.startsWith(prefix)) {
                continue;
            }

            String baseName = tableName.substring(prefix.length());

            DataTable dataTable = dataTableRepository
                .findByNameAndPlatformType(baseName, platformType.ordinal())
                .orElse(null);

            if (dataTable == null) {
                log.warn("Skipping unregistered physical data table '{}' in environment {}", baseName, environmentId);

                continue;
            }

            if (!isReadableBy(dataTable, owner)) {
                continue;
            }

            List<ColumnSpec> columnSpecs = listColumns(tableName)
                .stream()
                .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
                .toList();

            dataTableInfos.add(
                new DataTableInfo(
                    dataTable.getId(), baseName, dataTable.getDescription(), columnSpecs,
                    dataTable.getLastModifiedDate(), dataTable.getOwnerId()));
        }

        return dataTableInfos;
    }
```

**`dt_` is a prefix of nothing else, but `edt_0_x` must not match the `dt_0_%` scan.** It does not: `LIKE 'dt_0_%'` is anchored at the start, and `startsWith` re-checks. Add the reverse guard too — an AUTOMATION scan must reject a name beginning `edt_` only if a future prefix makes one a prefix of the other; today it cannot, and the `startsWith` check is what keeps that true.

`register` becomes keyed on the pair, preserving the find-or-create restored in `e8ad0e6b676`:

```java
    private long register(String baseName, @Nullable String description, PlatformType platformType) {
        Assert.hasText(baseName, "baseName required");

        return dataTableRepository.findByNameAndPlatformType(baseName, platformType.ordinal())
            .map(DataTable::getId)
            .orElseGet(() -> {
                DataTable dataTable = new DataTable();

                dataTable.setName(baseName);
                dataTable.setDescription(description);
                dataTable.setPlatformType(platformType);

                DataTable savedDataTable = dataTableRepository.save(dataTable);

                return savedDataTable.getId();
            });
    }
```

`hasPhysicalTablesForBaseName(String baseName)` gains the pool and scans only that pool's prefix. Without it, dropping the embedded `orders` finds `dt_0_orders`, concludes an instance remains, and leaves the embedded registry row behind as an orphan.

- [ ] **Step 5: Update the remote client stub**

`RemoteDataTableServiceClient` implements `DataTableService`, so widening the interface obliges it. Replace the two `listTables` overrides and the `createTable`/`getIdByBaseName`/`duplicateTable` overrides with the new signatures, each still `throw new UnsupportedOperationException();`.

- [ ] **Step 6: Thread the pool through the ROW service too**

`DataTableRowServiceImpl` builds physical names as well — 8 call sites — and its interface has 14 public
methods. Without the pool every row operation on an embedded table targets `dt_…`, so the embedded pool
is not merely unfiltered but unusable. Same mechanical change as Step 4, same commit.

Every `DataTableRowService` method that names a table takes a `PlatformType` immediately after
`environmentId`: `insertRow`, `updateRow`, `deleteRow`, `getRow`, the four `listRows` overloads,
`importCsv`, and the count/aggregate forms. Extract `prefix(platformType, environmentId)` to a shared
package-private utility rather than copying it into this class, so the two cannot drift.

`RemoteDataTableRowServiceClient` implements the interface, so all 14 stubs take the new parameter and
keep throwing `UnsupportedOperationException`.

- [ ] **Step 7: Prove a row round-trips in the embedded pool**

Add to `DataTablePoolIsolationIntTest`:

```java
    @Test
    void testRowsGoIntoTheRightPhysicalTable() {
        dataTableService.createTable(
            "rows", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        dataTableRowService.insertRow(
            "rows", Map.of("title", "x"), ENVIRONMENT_ID, PlatformType.EMBEDDED, RowOwnerFilter.unownedOnly());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edt_0_rows", Integer.class);

        assertThat(count).as("the row must land in the embedded physical table, not dt_0_rows").isEqualTo(1);
    }
```

- [ ] **Step 8: Run the test and confirm it passes**

Same command as Step 2. Expected: `Results: SUCCESS (4 tests, 4 successes)`.

- [ ] **Step 9: Commit**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:spotlessApply
git add server/libs/platform/platform-data-table server/ee/libs/platform/platform-data-table
git commit -m "- Give every data table lookup an explicit pool"
```

---

## Task 3: Point the automation callers at the AUTOMATION pool

**Files:**
- Modify: `server/libs/automation/automation-data-table/automation-data-table-service/src/main/java/com/bytechef/automation/data/table/configuration/facade/WorkspaceDataTableFacadeImpl.java`
- Modify: `.../automation-data-table-service/src/main/java/com/bytechef/automation/data/table/search/DataTableSearchAssetProvider.java`
- Test: `.../automation-data-table-service/src/test/java/.../facade/WorkspaceDataTableFacadePoolTest.java`

**Interfaces:**
- Consumes: the Task 2 signatures.
- Produces: nothing new; `WorkspaceDataTableFacade`'s own signatures are unchanged.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class WorkspaceDataTableFacadePoolTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final long WORKSPACE_ID = 1049L;

    @Mock
    private DataTableRowService dataTableRowService;

    @Mock
    private DataTableService dataTableService;

    @Mock
    private DataTableStorageService dataTableStorageService;

    @Mock
    private DataTableTagService dataTableTagService;

    @Mock
    private DataTableWebhookService dataTableWebhookService;

    @Mock
    private WorkspaceDataTableService workspaceDataTableService;

    @InjectMocks
    private WorkspaceDataTableFacadeImpl workspaceDataTableFacade;

    @Test
    void testTheWorkspaceFacadeOnlyEverAsksForAutomation() {
        when(dataTableService.listTables(ENVIRONMENT_ID, PlatformType.AUTOMATION, Optional.empty()))
            .thenReturn(List.of());
        when(workspaceDataTableService.getWorkspaceDataTables(WORKSPACE_ID)).thenReturn(List.of());

        workspaceDataTableFacade.listTables(WORKSPACE_ID, ENVIRONMENT_ID);

        verify(dataTableService).listTables(ENVIRONMENT_ID, PlatformType.AUTOMATION, Optional.empty());
    }

    @Test
    void testCreateGoesIntoTheAutomationPool() {
        List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("title", ColumnType.STRING));

        when(dataTableService.getIdByBaseName("t", PlatformType.AUTOMATION)).thenReturn(7L);

        workspaceDataTableFacade.createTable("t", "d", columnSpecs, WORKSPACE_ID, ENVIRONMENT_ID);

        verify(dataTableService).createTable("t", "d", columnSpecs, ENVIRONMENT_ID, PlatformType.AUTOMATION);
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
./gradlew :server:libs:automation:automation-data-table:automation-data-table-service:test --tests "*WorkspaceDataTableFacadePoolTest*" > /tmp/t3.log 2>&1; echo $?; grep -E "Results:|error:" /tmp/t3.log
```

Expected: compilation failure in the *production* file — the old unscoped calls no longer exist.

- [ ] **Step 3: Pass AUTOMATION at every call site**

In `WorkspaceDataTableFacadeImpl`, every `dataTableService` call gains `PlatformType.AUTOMATION` — `createTable`, `listTables`, `getIdByBaseName`, `duplicateTable`, `getBaseNameById` (unchanged, id-keyed), `addColumn`, `removeColumn`, `renameColumn`, `renameTable`, `dropTable`.

In `DataTableSearchAssetProvider`:

```java
        return dataTableService.listTables(1L, PlatformType.AUTOMATION, Optional.empty())
```

Note the pre-existing hardcoded environment `1L`; leave it, it is out of scope.

- [ ] **Step 4: Run the test and confirm it passes**

Same command as Step 2. Expected: `Results: SUCCESS (2 tests, 2 successes)`.

- [ ] **Step 5: Compile the whole tree to find the remaining call sites**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t3c.log 2>&1; echo $?; grep -E "error:" /tmp/t3c.log | head -40
```

Fix each by passing the pool the enclosing caller means. Every remaining one is an automation path.

- [ ] **Step 6: Commit**

```bash
./gradlew :server:libs:automation:automation-data-table:automation-data-table-service:spotlessApply
git add server/libs/automation/automation-data-table
git commit -m "- Ask for the automation pool from the automation callers"
```

---

## Task 4: Owner-keyed runtime scoping

**Files:**
- Modify: `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/util/DataTableUtils.java`
- Test: `server/libs/modules/components/data-table/src/test/java/com/bytechef/component/datatable/util/DataTableUtilsPoolTest.java`

**Interfaces:**
- Consumes: `listTables(long, PlatformType, Optional<Owner>)`.
- Produces: `DataTableUtils#poolFor(Optional<Owner>): List<PlatformType>` and a pool-aware `getDataTableInfo(DataTableService, String, long, Optional<Owner>)`.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class DataTableUtilsPoolTest {

    private static final long ENVIRONMENT_ID = 0L;

    @Mock
    private DataTableService dataTableService;

    @Test
    void testAnOwnedRunSeesOnlyTheEmbeddedPool() {
        Optional<Owner> owner = Optional.of(Owner.connectedUser(1055L));

        when(dataTableService.listTables(anyLong(), eq(PlatformType.EMBEDDED), eq(owner))).thenReturn(List.of());

        DataTableUtils.getTableOptions(null, dataTableService, owner);

        verify(dataTableService).listTables(anyLong(), eq(PlatformType.EMBEDDED), eq(owner));
        verify(dataTableService, never()).listTables(anyLong(), eq(PlatformType.AUTOMATION), any());
    }

    @Test
    void testAnUnownedRunSeesBothPools() {
        when(dataTableService.listTables(anyLong(), any(PlatformType.class), eq(Optional.empty())))
            .thenReturn(List.of());

        DataTableUtils.getTableOptions(null, dataTableService, Optional.empty());

        verify(dataTableService).listTables(anyLong(), eq(PlatformType.EMBEDDED), eq(Optional.empty()));
        verify(dataTableService).listTables(anyLong(), eq(PlatformType.AUTOMATION), eq(Optional.empty()));
    }

    @Test
    void testPoolForIsOwnerKeyed() {
        assertThat(DataTableUtils.poolFor(Optional.of(Owner.connectedUser(1L))))
            .containsExactly(PlatformType.EMBEDDED);
        assertThat(DataTableUtils.poolFor(Optional.empty()))
            .containsExactlyInAnyOrder(PlatformType.AUTOMATION, PlatformType.EMBEDDED);
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
./gradlew :server:libs:modules:components:data-table:test --tests "*DataTableUtilsPoolTest*" > /tmp/t4.log 2>&1; echo $?; grep -E "Results:|error:" /tmp/t4.log
```

Expected: compilation failure — `poolFor` does not exist.

- [ ] **Step 3: Implement owner-keyed pool selection**

```java
    /**
     * The pool a run may see, derived from WHO the run is for rather than from where the workflow was authored. The
     * embedded bridge dispatches an automation workflow under a connected user, so PlatformType answers a different
     * question than this one and must not be used here.
     */
    public static List<PlatformType> poolFor(Optional<Owner> owner) {
        return owner.isPresent()
            ? List.of(PlatformType.EMBEDDED)
            : List.of(PlatformType.AUTOMATION, PlatformType.EMBEDDED);
    }
```

and in `getTableOptions`:

```java
    public static List<Option<String>> getTableOptions(
        String searchText, DataTableService dataTableService, Optional<Owner> owner) {

        List<DataTableInfo> dataTableInfos = poolFor(owner).stream()
            .flatMap(platformType -> dataTableService.listTables(DEVELOPMENT.ordinal(), platformType, owner).stream())
            .toList();

        return dataTableInfos.stream()
            .filter(
                dataTableInfo -> searchText == null || dataTableInfo.baseName()
                    .toLowerCase()
                    .contains(searchText.toLowerCase()))
            .<Option<String>>map(
                dataTableInfo -> option(dataTableInfo.baseName(), dataTableInfo.baseName(),
                    dataTableInfo.description()))
            .toList();
    }
```

Nothing has to be threaded into the options request: `getActionTableOptions` and `getTriggerTableOptions` already hand this method `OwnerResolution.resolve(...)`, and that resolution works in the editor too, falling through to `resolveCurrentPrincipal()`.

- [ ] **Step 4: Close the metadata leak in `getDataTableInfo`**

It currently resolves a table **by name with no scoping**, so an account naming `invoices` learns its column structure, and after the split two same-named tables resolve to whichever the scan reaches first. Give it the owner and select the pool the same way:

```java
    @Nullable
    public static DataTableInfo getDataTableInfo(
        DataTableService dataTableService, String baseName, long environmentId, Optional<Owner> owner) {

        return poolFor(owner).stream()
            .flatMap(platformType -> dataTableService.listTables(environmentId, platformType, owner).stream())
            .filter(dataTableInfo -> {
                String curBaseName = dataTableInfo.baseName();

                return curBaseName.equalsIgnoreCase(baseName);
            })
            .findFirst()
            .orElse(null);
    }
```

Its three callers — `rowObjectSchema`, `createSampleOutput` and the column-properties options function at line ~242 — each take an `Optional<Owner>` and pass it down. Their own callers already resolve one.

- [ ] **Step 5: Run the test and confirm it passes**

Same command as Step 2. Expected: `Results: SUCCESS (3 tests, 3 successes)`.

- [ ] **Step 6: Commit**

```bash
./gradlew :server:libs:modules:components:data-table:spotlessApply
git add server/libs/modules/components/data-table
git commit -m "- Scope the data table runtime on the owner of the run"
```

---

## Task 5: Unowned runs see unowned rows, and the account parameter

**Files:**
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/RowOwnerFilter.java`
- Test: `.../platform-data-table-service/src/test/java/.../DataTableRowPoolScopingIntTest.java`

**Interfaces:**
- Consumes: Task 2 and Task 4.
- Produces: `RowOwnerFilter#unownedOnly()`.

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRowPoolScopingIntTest {

    private static final long ENVIRONMENT_ID = 0;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Test
    void testAnUnownedRunSeesOnlyUnownedRowsOfAnEmbeddedTable() {
        dataTableService.createTable(
            "scoped", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        dataTableRowService.createRow(
            "scoped", Map.of("title", "alice"), ENVIRONMENT_ID, Optional.of(Owner.connectedUser(1055L)));
        dataTableRowService.createRow("scoped", Map.of("title", "shared"), ENVIRONMENT_ID, Optional.empty());

        List<DataTableRow> rows = dataTableRowService.listRows(
            "scoped", ENVIRONMENT_ID, RowOwnerFilter.unownedOnly());

        assertThat(rows)
            .as("a vendor run that named no account must not read an account's rows")
            .hasSize(1);
        assertThat(rows.get(0).values()).containsEntry("title", "shared");
    }

    @Test
    void testNamingAnAccountReachesThatAccountsRows() {
        dataTableService.createTable(
            "named", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        dataTableRowService.createRow(
            "named", Map.of("title", "alice"), ENVIRONMENT_ID, Optional.of(Owner.connectedUser(1055L)));

        List<DataTableRow> rows = dataTableRowService.listRows(
            "named", ENVIRONMENT_ID, RowOwnerFilter.from(Optional.of(Owner.connectedUser(1055L))));

        assertThat(rows).hasSize(1);
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests "*DataTableRowPoolScopingIntTest*" > /tmp/t5.log 2>&1; echo $?; grep -E "Results:|error:" /tmp/t5.log
```

Expected: compilation failure — `unownedOnly()` does not exist.

- [ ] **Step 3: Pin the table-level rule, which is deliberately different**

Ownership has two independent axes and they do not follow the same rule. Add to the same test class:

```java
    @Test
    void testAVendorRunStillSeesATableAssignedToAnAccount() {
        dataTableService.createTable(
            "assigned", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        long id = dataTableService.getIdByBaseName("assigned", PlatformType.EMBEDDED);

        dataTableService.assignOwner(id, Owner.connectedUser(1055L));

        // Table-level ownership is metadata the vendor manages and must be able to see; only the ROWS are the
        // account's. Filtering both levels alike would make the console's assignment view unusable.
        assertThat(dataTableService.listTables(ENVIRONMENT_ID, PlatformType.EMBEDDED, Optional.empty()))
            .extracting(DataTableInfo::baseName)
            .contains("assigned");
    }
```

This must pass without changing `isReadableBy` — it pins existing behaviour so a later "make the two
levels symmetric" tidy-up fails loudly instead of silently hiding assigned tables from the console.

- [ ] **Step 4: Add `unownedOnly` to `RowOwnerFilter`**

`from(Optional.empty())` stays `unrestricted()` — that is correct for the AUTOMATION pool, whose rows have no owners, and for Community Edition. The new factory expresses the narrower thing an embedded-pool read needs:

```java
    /**
     * Rows belonging to nobody. What a vendor run sees in an EMBEDDED table when it has not named an account --
     * unrestricted() would hand it every account's rows.
     */
    public static RowOwnerFilter unownedOnly() {
        return new RowOwnerFilter(null, null, true);
    }
```

(Match the existing record/constructor shape; the third component is the "restrict to NULL owner" flag the SQL builder reads.)

- [ ] **Step 5: Make an unowned write into an EMBEDDED table deliberate**

An unowned row is visible to every account, so it must never be produced by omission. `insertRow` stamps
the owner columns only when `rowOwnerFilter.owner()` is present, which means an unresolved owner writes a
row nobody can attribute and everybody can read — permanently, since nothing corrects it afterwards.

The three filters now say three different things on a write, and `unownedOnly()` doubles as the explicit
"this row is shared" marker, so no new type is needed:

| filter | write to an EMBEDDED table |
|---|---|
| `from(owner)` with an owner | stamp that owner |
| `unownedOnly()` | write a shared row — deliberate |
| `unrestricted()` | **reject** |

```java
    private static void checkWritableOwner(PlatformType platformType, RowOwnerFilter rowOwnerFilter) {
        if (platformType != PlatformType.EMBEDDED) {
            return;
        }

        if (rowOwnerFilter.owner().isEmpty() && !rowOwnerFilter.unownedOnly()) {
            throw new IllegalArgumentException(
                "A write to an embedded data table must either carry an owner or declare the row shared; an "
                    + "unowned row is visible to every account and must not be produced by omission");
        }
    }
```

Call it from `insertRow`, `updateRow` and `importCsv` — all three can create or relabel rows. AUTOMATION
tables are untouched: no row there has an owner and none is expected to.

- [ ] **Step 6: Test both directions of the write rule**

```java
    @Test
    void testAnUnresolvedOwnerCannotWriteAnEmbeddedRow() {
        dataTableService.createTable(
            "guarded", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        assertThatThrownBy(() -> dataTableRowService.insertRow(
            "guarded", Map.of("title", "x"), ENVIRONMENT_ID, PlatformType.EMBEDDED,
            RowOwnerFilter.unrestricted()))
            .as("a row nobody can attribute and everybody can read must not be written by omission")
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testADeliberateSharedWriteIsAllowed() {
        dataTableService.createTable(
            "shareable", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.EMBEDDED);

        DataTableRow row = dataTableRowService.insertRow(
            "shareable", Map.of("title", "reference"), ENVIRONMENT_ID, PlatformType.EMBEDDED,
            RowOwnerFilter.unownedOnly());

        assertThat(row).isNotNull();
    }

    @Test
    void testAnAutomationWriteIsUnaffected() {
        dataTableService.createTable(
            "plain", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID,
            PlatformType.AUTOMATION);

        DataTableRow row = dataTableRowService.insertRow(
            "plain", Map.of("title", "x"), ENVIRONMENT_ID, PlatformType.AUTOMATION,
            RowOwnerFilter.unrestricted());

        assertThat(row).isNotNull();
    }
```

- [ ] **Step 7: Emit the predicate**

In the SQL builder that turns a `RowOwnerFilter` into a predicate, the unowned-only case emits `owner_id IS NULL`. The existing owner case already emits `(owner_id = ? OR owner_id IS NULL)`, so the new case is that expression minus its first disjunct.

- [ ] **Step 8: Add the account parameter to the actions**

Data table actions gain an optional account property. **Honour it only when the run has no owner:**

```java
    /**
     * A vendor run may act for a named account. A run that already belongs to one may not name a different account --
     * without this, an account's own workflow names another and reads its rows.
     */
    private static Optional<Owner> effectiveOwner(Optional<Owner> resolvedOwner, @Nullable Long namedAccountId) {
        if (resolvedOwner.isPresent()) {
            return resolvedOwner;
        }

        return namedAccountId == null ? Optional.empty() : Optional.of(Owner.connectedUser(namedAccountId));
    }
```

- [ ] **Step 9: Write the escalation test**

```java
    @Test
    void testAnOwnedRunCannotNameAnotherAccount() {
        Optional<Owner> alice = Optional.of(Owner.connectedUser(1055L));

        assertThat(DataTableUtils.effectiveOwner(alice, 9999L))
            .as("a run that already belongs to an account must ignore a named account")
            .isEqualTo(alice);
    }
```

- [ ] **Step 10: Run every test in this task and confirm they pass**

Expected: `Results: SUCCESS`.

- [ ] **Step 11: Commit**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:spotlessApply :server:libs:modules:components:data-table:spotlessApply
git add server/libs/platform/platform-data-table server/libs/modules/components/data-table
git commit -m "- Keep an unowned run out of an account's rows unless it names the account"
```

---

## Task 6: The same split for knowledge bases

**Files:**
- Create: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/main/resources/config/liquibase/changelog/platform/knowledge_base/20260829000001_platform_knowledge_base_add_platform_type.xml`
- Modify: `.../platform-knowledge-base-api/.../domain/KnowledgeBase.java`, `.../service/KnowledgeBaseService.java`
- Modify: `.../platform-knowledge-base-service/.../service/KnowledgeBaseServiceImpl.java`
- Test: `.../platform-knowledge-base-service/src/test/java/.../KnowledgeBasePoolIntTest.java`

**Interfaces:**
- Consumes: `PlatformType`.
- Produces: `KnowledgeBase#getPlatformType()/setPlatformType(PlatformType)`, `getKnowledgeBases(int environment, PlatformType platformType, Optional<Owner> owner)`.

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class KnowledgeBasePoolIntTest {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    void testNeitherPoolListsTheOther() {
        knowledgeBaseService.createKnowledgeBase(knowledgeBase("kbauto", 0, PlatformType.AUTOMATION));
        knowledgeBaseService.createKnowledgeBase(knowledgeBase("kbembedded", 0, PlatformType.EMBEDDED));

        assertThat(names(0, PlatformType.AUTOMATION)).contains("kbauto").doesNotContain("kbembedded");
        assertThat(names(0, PlatformType.EMBEDDED)).contains("kbembedded").doesNotContain("kbauto");
    }

    @Test
    void testTheSameNameIsAllowedInASecondEnvironment() {
        knowledgeBaseService.createKnowledgeBase(knowledgeBase("perenv", 0, PlatformType.AUTOMATION));
        knowledgeBaseService.createKnowledgeBase(knowledgeBase("perenv", 1, PlatformType.AUTOMATION));

        assertThat(names(0, PlatformType.AUTOMATION)).contains("perenv");
        assertThat(names(1, PlatformType.AUTOMATION)).contains("perenv");
    }

    private List<String> names(int environment, PlatformType platformType) {
        return knowledgeBaseService.getKnowledgeBases(environment, platformType, Optional.empty())
            .stream()
            .map(KnowledgeBase::getName)
            .toList();
    }

    private static KnowledgeBase knowledgeBase(String name, int environment, PlatformType platformType) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName(name);
        knowledgeBase.setEnvironment(Environment.values()[environment]);
        knowledgeBase.setPlatformType(platformType);

        return knowledgeBase;
    }
}
```

The second test also closes [#5591](https://github.com/bytechefhq/bytechef/issues/5591) — a knowledge base name is currently usable in exactly one environment.

- [ ] **Step 2: Run it and confirm it fails**

```bash
DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:testIntegration --tests "*KnowledgeBasePoolIntTest*" > /tmp/t6.log 2>&1; echo $?; grep -E "Results:|error:" /tmp/t6.log
```

Expected: compilation failure — `setPlatformType` does not exist.

- [ ] **Step 3: Add the changelog**

```xml
    <changeSet id="20260829000001-1" author="Ivica Cardic">
        <addColumn tableName="knowledge_base">
            <column name="platform_type" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

    <changeSet id="20260829000001-2" author="Ivica Cardic">
        <dropUniqueConstraint tableName="knowledge_base" constraintName="uk_knowledge_base_name"/>

        <addUniqueConstraint
            tableName="knowledge_base"
            columnNames="name, platform_type, environment"
            constraintName="uk_knowledge_base_name_platform_type_environment"/>
    </changeSet>
```

`environment` belongs in this key and **not** in `data_table`'s: a `knowledge_base` row *is* the per-environment resource, whereas a `data_table` row is one logical table with a physical instance per environment, and `dropTable` depends on that.

- [ ] **Step 4: Add the field and thread the pool**

`KnowledgeBase` gets the same `private int platformType` plus converting accessors as `DataTable`. `KnowledgeBaseService` replaces `getKnowledgeBases(int)` and `getKnowledgeBases(int, Optional<Owner>)` with `getKnowledgeBases(int, PlatformType, Optional<Owner>)`, deleting the unscoped forms. `WorkspaceKnowledgeBaseFacadeImpl` passes AUTOMATION; `EmbeddedKnowledgeBaseApiFacadeImpl` passes EMBEDDED.

- [ ] **Step 5: Run the test and confirm it passes**

Same command as Step 2. Expected: `Results: SUCCESS (2 tests, 2 successes)`.

- [ ] **Step 6: Commit**

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:spotlessApply
git add server/libs/platform/platform-knowledge-base server/libs/automation/automation-knowledge-base server/ee/libs/embedded/embedded-knowledge-base-graphql
git commit -m "- Split the knowledge base pools and key the name per environment"
```

---

## Task 7: Embedded create paths

**Files:**
- Modify: `server/ee/libs/embedded/embedded-data-table-graphql/src/main/java/com/bytechef/ee/embedded/data/table/facade/EmbeddedDataTableApiFacade.java` and `Impl`
- Modify: `.../embedded-data-table-graphql/src/main/resources/graphql/embedded-data-table.graphqls`
- Modify: `.../embedded-data-table-graphql/.../web/graphql/EmbeddedDataTableGraphQlController.java`
- Same four shapes under `embedded-knowledge-base-graphql`
- Test: `.../embedded-data-table-graphql/src/test/java/.../EmbeddedDataTableApiFacadeCreateTest.java`

**Interfaces:**
- Consumes: `createTable(String, String, List<ColumnSpec>, long, PlatformType)`.
- Produces: `EmbeddedDataTableApiFacade#createDataTable(long environmentId, String name, String description, List<ColumnSpec> columnSpecs)`.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class EmbeddedDataTableApiFacadeCreateTest {

    @Mock
    private DataTableService dataTableService;

    @InjectMocks
    private EmbeddedDataTableApiFacadeImpl embeddedDataTableApiFacade;

    @Test
    void testCreateGoesIntoTheEmbeddedPool() {
        List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("title", ColumnType.STRING));

        embeddedDataTableApiFacade.createDataTable(0L, "conversations", "d", columnSpecs);

        verify(dataTableService).createTable("conversations", "d", columnSpecs, 0L, PlatformType.EMBEDDED);
    }

    @Test
    void testCreateIsTenantAdminGated() throws Exception {
        Method method = EmbeddedDataTableApiFacadeImpl.class.getMethod(
            "createDataTable", long.class, String.class, String.class, List.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("isTenantAdmin()");
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
./gradlew :server:ee:libs:embedded:embedded-data-table-graphql:test > /tmp/t7.log 2>&1; echo $?; grep -E "Results:|error:" /tmp/t7.log
```

Expected: compilation failure — `createDataTable` does not exist.

- [ ] **Step 3: Add the facade method**

```java
    @Override
    @PreAuthorize("isTenantAdmin()")
    public void createDataTable(
        long environmentId, String name, String description, List<ColumnSpec> columnSpecs) {

        dataTableService.createTable(name, description, columnSpecs, environmentId, PlatformType.EMBEDDED);
    }
```

- [ ] **Step 4: Add the GraphQL mutation**

```graphql
extend type Mutation {
    createEmbeddedDataTable(input: CreateEmbeddedDataTableInput!): Boolean
}

input CreateEmbeddedDataTableInput {
    environmentId: Long!
    name: String!
    description: String
    columns: [EmbeddedDataTableColumnInput!]!
}

input EmbeddedDataTableColumnInput {
    name: String!
    type: EmbeddedDataTableColumnType!
}
```

Enum values are SCREAMING_SNAKE_CASE, matching the rest of the schema. Mirror the same four changes for knowledge bases.

- [ ] **Step 5: Run the test and confirm it passes**

Expected: `Results: SUCCESS`.

- [ ] **Step 6: Regenerate the GraphQL client**

```bash
cd client && npx graphql-codegen
```

Commit the operations and the generated file separately, per the repo convention.

- [ ] **Step 7: Commit**

```bash
./gradlew :server:ee:libs:embedded:embedded-data-table-graphql:spotlessApply :server:ee:libs:embedded:embedded-knowledge-base-graphql:spotlessApply
git add server/ee/libs/embedded
git commit -m "- Let the embedded console create into its own pool"
```

---

## Task 8: Return the components to the integration palette

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/filter/IntegrationComponentDefinitionFilter.java`
- Test: the module's existing filter test

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void testDataTableAndKnowledgeBaseAreOfferedToIntegrationWorkflows() {
        assertThat(integrationComponentDefinitionFilter.filter(componentDefinition("dataTable")))
            .as("the embedded pool is unusable from the embedded editor if its components are hidden")
            .isTrue();
        assertThat(integrationComponentDefinitionFilter.filter(componentDefinition("knowledgeBase"))).isTrue();
    }

    @Test
    void testApiPlatformAndWebhookStayHidden() {
        assertThat(integrationComponentDefinitionFilter.filter(componentDefinition("apiPlatform"))).isFalse();
        assertThat(integrationComponentDefinitionFilter.filter(componentDefinition("webhook"))).isFalse();
    }
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test > /tmp/t8.log 2>&1; echo $?; grep -E "Results:" /tmp/t8.log
```

Expected: FAIL — `dataTable` is currently filtered out.

- [ ] **Step 3: Shorten the list**

```java
    private static final List<String> COMPONENT_NAMES = List.of("apiPlatform", "webhook");
```

Those two were hidden because data tables and knowledge bases were automation-only and hiding them was the only way to stop embedded authors reaching automation's resources. The pool split does that by a stronger mechanism, so the filter stops being protection and becomes an obstacle.

- [ ] **Step 4: Run the test and confirm it passes**

Expected: `Results: SUCCESS`.

- [ ] **Step 5: Full verification**

```bash
./gradlew check -x testIntegration --continue > /tmp/final.log 2>&1; echo $?
grep -E "^> Task .* FAILED" /tmp/final.log
DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew testIntegration --continue > /tmp/finalint.log 2>&1; echo $?
grep -E "^> Task .* FAILED" /tmp/finalint.log
```

Both greps must print nothing.

- [ ] **Step 6: Commit**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "- Offer data tables and knowledge bases to integration workflows again"
```

---

## Self-review notes

**Spec coverage.** Discriminator → Task 1/6. Physical naming → Task 2. Registry keying → Task 1/2/6. Service signatures → Task 2/3/6. Runtime scoping → Task 4. Account parameter and the row tightening → Task 5. Palette → Task 8. Migration → the changelogs in Task 1 and 6. Testing → the test in every task, with the negative assertions the spec asks for.

**Not covered here, by design.** The `/embedded` console UI is sub-project 2. Per-environment ownership of a data table stays out. The demo's tables are the user's to re-create.

**One thing the executor must not shortcut.** When Task 2 deletes the unscoped overloads, the compile errors in Task 3 Step 5 *are* the audit. Re-adding a defaulting overload to make them go away would leave the design intact on paper and leaking in practice.
