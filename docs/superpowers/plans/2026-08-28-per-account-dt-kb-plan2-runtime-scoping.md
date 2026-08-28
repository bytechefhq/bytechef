# Per-account DT/KB — Plan 2: Runtime scoping

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a workflow running for a connected user read and write only that account's data table rows, and refuse a knowledge base it does not own — while every automation and admin path keeps seeing everything.

**Architecture:** Additive, not a signature change. `DataTableRowService` keeps its seven existing methods with today's unrestricted semantics and gains seven owner-scoped variants; the unrestricted ones delegate to the scoped ones with `RowOwnerFilter.unrestricted()`, so enforcement has exactly one home in the impl. Only the `dataTable` component calls the scoped variants, and an architecture test keeps it that way. Identity is resolved through one CE SPI implemented in EE embedded, because a platform module may not know what a connected user is.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, raw `JdbcTemplate` for `dt_*` access, JUnit 5, Mockito, Testcontainers (PostgreSQL 15).

**Spec:** `docs/superpowers/specs/2026-08-27-per-account-data-tables-knowledge-bases-design.md`

**Depends on:** Plan 1 (`docs/superpowers/plans/2026-08-27-per-account-dt-kb-plan1-owner-model.md`), commits `6661a848a5e..c6f526afb41` on branch `worktree-dt-kb-owner-model`. This plan stacks on that branch; `ReservedColumns.OWNER_ID` / `OWNER_TYPE` and the `dt_*` columns do not exist anywhere else yet.

## Global Constraints

- **`owner_id` is NEVER `jobPrincipalId`.** For `PlatformType.EMBEDDED` the job principal is the integration-instance id; for the automation bridge it is the project-deployment id. Both differ per integration and per project *for the same account*. The owner is always the resolved `connected_user.id`. Storing the principal instead shards one account's rows across its own integrations, which reads as data loss rather than a leak and so survives testing.
- **Enforcement lives in `DataTableRowServiceImpl`.** The component has nine actions and triggers; nine hand-written filters is where one gets forgotten.
- **Do not touch `ai-hub`, `ai-copilot`, `automation-ai-tool`, `DataTableRowGraphQlController` or `WorkspaceDataTableFacadeImpl`.** All five are admin-authenticated automation paths that must keep seeing every row. Verified: the data-table copilot agent registers under `Source.DATA_TABLE`, while the connected-user entry point `ConnectedUserCopilotApiController` hardcodes `Source.WORKFLOW_EDITOR` + `Mode.BUILD`, so no connected user reaches it.
- Visibility rule is `owner_id = :owner OR owner_id IS NULL`. Unowned rows are vendor-shared and visible to everyone.
- Run `./gradlew spotlessApply` before every commit. Test method names are camelCase with no underscores. `TODO:` comments are forbidden.
- Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep `^> Task .* FAILED`. Note `grep error:` also matches the `:server:libs:core:error:` module path.
- Testcontainers on this machine needs `DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` exported before any `testIntegration` run. Confirm the endpoint with `docker context inspect --format '{{.Endpoints.docker.Host}}'` rather than assuming.

---

### Task 1: The Owner identity and its resolver SPI

**Files:**
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/owner/Owner.java`
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/owner/OwnerResolver.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/owner/OwnerTest.java`

The SPI lives in `platform-api`, not `platform-data-table-api`, because Task 7 needs it from `platform-knowledge-base` and the two stores may not depend on each other. The test lives in `platform-data-table-service` because `platform-api` has no test source set and adding one is unrelated scaffolding.

**Interfaces:**
- Consumes: `OwnerType` from Plan 1.
- Produces: `com.bytechef.platform.owner.Owner`, a record `Owner(OwnerType type, long id)` with a static `Owner connectedUser(long id)`; and `com.bytechef.platform.owner.OwnerResolver` with `Optional<Owner> resolveJobPrincipal(long jobPrincipalId, PlatformType platformType)` and `Optional<Owner> resolveCurrentPrincipal()`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.owner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.constant.OwnerType;
import org.junit.jupiter.api.Test;

class OwnerTest {

    @Test
    void testConnectedUserBuildsAConnectedUserOwner() {
        Owner owner = Owner.connectedUser(1055L);

        assertEquals(OwnerType.CONNECTED_USER, owner.type());
        assertEquals(1055L, owner.id());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.owner.OwnerTest" > /tmp/p2t1.log 2>&1; echo $?; grep -E "symbol:" /tmp/p2t1.log | head -3
```

Expected: `cannot find symbol: class Owner`.

- [ ] **Step 3: Write Owner**

```java
package com.bytechef.platform.owner;

import com.bytechef.platform.constant.OwnerType;

/**
 * The principal a resource or a row belongs to. Never the job principal: for {@code PlatformType.EMBEDDED} that is the
 * integration-instance id and for the automation bridge the project-deployment id, both of which differ per
 * integration and per project for the same account.
 *
 * @author Ivica Cardic
 */
public record Owner(OwnerType type, long id) {

    public static Owner connectedUser(long id) {
        return new Owner(OwnerType.CONNECTED_USER, id);
    }
}
```

- [ ] **Step 4: Write the SPI**

```java
package com.bytechef.platform.owner;

import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;

/**
 * Resolves the {@link Owner} a running workflow or an inbound request belongs to. Implemented in Enterprise embedded,
 * which is the only edition where a principal below the tenant exists; Community ships no implementation, and an empty
 * result there correctly means "no owner, see everything".
 *
 * @author Ivica Cardic
 */
public interface OwnerResolver {

    /**
     * @param jobPrincipalId a project-deployment id when {@code platformType} is {@code AUTOMATION}, an
     *                       integration-instance id when it is {@code EMBEDDED}
     * @return the owner behind that job principal, or empty when the principal belongs to no connected user
     */
    Optional<Owner> resolveJobPrincipal(long jobPrincipalId, PlatformType platformType);

    /**
     * Resolves the owner from the current security context rather than from a job. Used by editor test runs, which
     * have no persisted job and therefore no job principal. Empty means the caller is not a connected user.
     */
    Optional<Owner> resolveCurrentPrincipal();
}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "com.bytechef.platform.owner.OwnerTest" > /tmp/p2t1.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t1.log | head
```

Expected: exit 0.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply > /tmp/p2t1sp.log 2>&1; echo $?
git add server/libs/platform/platform-api server/libs/platform/platform-data-table
git commit -m "--- Add the Owner identity and its resolver SPI"
```

---

### Task 2: The Enterprise resolver

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserOwnerResolver.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserOwnerResolverTest.java`

Both lookups already exist as service methods — no new service surface. Files under `server/ee/` use the ByteChef Enterprise licence header and carry a `@version ee` Javadoc tag; Spotless selects the header by that tag's presence, not by path.

**Interfaces:**
- Consumes: `Owner`, `OwnerResolver` from Task 1; `IntegrationInstanceService.getIntegrationInstance(long)`; `ConnectedUserProjectService.fetchConnectedUserId(long)`.
- Produces: a Spring bean implementing `OwnerResolver`.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class ConnectedUserOwnerResolverTest {

    @Mock
    private ConnectedUserProjectService connectedUserProjectService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    private ConnectedUserOwnerResolver connectedUserOwnerResolver;

    @BeforeEach
    void setUp() {
        connectedUserOwnerResolver = new ConnectedUserOwnerResolver(
            connectedUserProjectService, integrationInstanceService);
    }

    @Test
    void testEmbeddedResolvesThroughTheIntegrationInstance() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(1055L);

        when(integrationInstanceService.getIntegrationInstance(77L)).thenReturn(integrationInstance);

        Optional<Owner> owner = connectedUserOwnerResolver.resolveJobPrincipal(77L, PlatformType.EMBEDDED);

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);
    }

    @Test
    void testAutomationResolvesThroughTheConnectedUserProject() {
        when(connectedUserProjectService.fetchConnectedUserId(88L)).thenReturn(Optional.of(1055L));

        Optional<Owner> owner = connectedUserOwnerResolver.resolveJobPrincipal(88L, PlatformType.AUTOMATION);

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);
    }

    @Test
    void testAutomationWithNoConnectedUserProjectResolvesToNoOwner() {
        when(connectedUserProjectService.fetchConnectedUserId(88L)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), connectedUserOwnerResolver.resolveJobPrincipal(88L, PlatformType.AUTOMATION));
    }
}
```

That third case is the vendor's own automation workflow — the common path — and it must resolve to no owner rather than to a denial.

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserOwnerResolverTest" > /tmp/p2t2.log 2>&1; echo $?; grep -E "symbol:" /tmp/p2t2.log | head -3
```

Expected: `cannot find symbol: class ConnectedUserOwnerResolver`.

- [ ] **Step 3: Write the resolver**

```java
@Component
@ConditionalOnEEVersion
public class ConnectedUserOwnerResolver implements OwnerResolver {

    private final ConnectedUserProjectService connectedUserProjectService;
    private final IntegrationInstanceService integrationInstanceService;

    public ConnectedUserOwnerResolver(
        ConnectedUserProjectService connectedUserProjectService,
        IntegrationInstanceService integrationInstanceService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public Optional<Owner> resolveJobPrincipal(long jobPrincipalId, PlatformType platformType) {
        if (platformType == PlatformType.EMBEDDED) {
            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                jobPrincipalId);

            Long connectedUserId = integrationInstance.getConnectedUserId();

            return connectedUserId == null ? Optional.empty() : Optional.of(Owner.connectedUser(connectedUserId));
        }

        return connectedUserProjectService.fetchConnectedUserId(jobPrincipalId)
            .map(Owner::connectedUser);
    }

    @Override
    public Optional<Owner> resolveCurrentPrincipal() {
        return Optional.empty();
    }
}
```

`resolveCurrentPrincipal` is deliberately left empty here and filled in Task 5, where the editor case is designed. An empty result means "not a connected user", which is the correct answer for every caller until that task wires the security-context read.

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserOwnerResolverTest" > /tmp/p2t2.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t2.log | head
```

Expected: exit 0, three tests passing.

- [ ] **Step 5: Verify the module's own suite still passes**

```bash
./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test > /tmp/p2t2full.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t2full.log | head
```

Adding a scanned `@Component` can break other modules' hand-assembled `@SpringBootTest(classes = ...)` contexts with a missing-bean error. If one fails, add a mock `@Bean` to that module's `*IntTestConfiguration`.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply > /tmp/p2t2sp.log 2>&1; echo $?
git add server/ee/libs/embedded/embedded-configuration
git commit -m "- Resolve the connected user behind a job principal"
```

---

### Task 3: Owner-scoped overloads that change nothing yet

Adds the seven scoped variants and routes the existing seven through them with `unrestricted()`. Behaviour is identical; this task exists so the enforcement in Task 4 lands in code that is already wired.

**Files:**
- Create: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/domain/RowOwnerFilter.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-api/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowService.java`
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/domain/RowOwnerFilterTest.java`

`RowOwnerFilter` lives in `platform-data-table-api`, not `platform-api`, because only data tables filter rows — a knowledge base checks ownership of a whole resource instead.

**Interfaces:**
- Consumes: `Owner` from Task 1.
- Produces: `RowOwnerFilter` with `RowOwnerFilter.unrestricted()`, `RowOwnerFilter.ownedBy(Owner)`, `RowOwnerFilter.from(Optional<Owner>)`, and `boolean isUnrestricted()` / `Optional<Owner> owner()`. Seven new `DataTableRowService` methods, each the existing signature plus a trailing `RowOwnerFilter rowOwnerFilter`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.data.table.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.owner.Owner;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RowOwnerFilterTest {

    @Test
    void testUnrestrictedCarriesNoOwner() {
        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.unrestricted();

        assertTrue(rowOwnerFilter.isUnrestricted());
        assertEquals(Optional.empty(), rowOwnerFilter.owner());
    }

    @Test
    void testOwnedByCarriesTheOwner() {
        Owner owner = Owner.connectedUser(1055L);

        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.ownedBy(owner);

        assertFalse(rowOwnerFilter.isUnrestricted());
        assertEquals(Optional.of(owner), rowOwnerFilter.owner());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test --tests "*RowOwnerFilterTest" > /tmp/p2t3.log 2>&1; echo $?; grep -E "symbol:" /tmp/p2t3.log | head -3
```

Expected: `cannot find symbol: class RowOwnerFilter`.

- [ ] **Step 3: Write RowOwnerFilter**

```java
package com.bytechef.platform.data.table.domain;

import com.bytechef.platform.owner.Owner;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Which rows of a data table a caller may see. {@link #unrestricted()} is every row and is correct for admin and
 * automation callers; {@link #ownedBy(Owner)} narrows to that owner's rows plus the unowned, vendor-shared ones.
 *
 * <p>
 * The unrestricted case is a named factory rather than a null argument so that bypasses are greppable.
 *
 * @author Ivica Cardic
 */
public final class RowOwnerFilter {

    private static final RowOwnerFilter UNRESTRICTED = new RowOwnerFilter(null);

    private final @Nullable Owner owner;

    private RowOwnerFilter(@Nullable Owner owner) {
        this.owner = owner;
    }

    public static RowOwnerFilter unrestricted() {
        return UNRESTRICTED;
    }

    public static RowOwnerFilter ownedBy(Owner owner) {
        return new RowOwnerFilter(owner);
    }

    public static RowOwnerFilter from(Optional<Owner> owner) {
        return owner.map(RowOwnerFilter::ownedBy)
            .orElse(UNRESTRICTED);
    }

    public boolean isUnrestricted() {
        return owner == null;
    }

    public Optional<Owner> owner() {
        return Optional.ofNullable(owner);
    }
}
```

- [ ] **Step 4: Add the seven scoped methods to the interface**

Each existing method keeps its signature and gains a sibling with a trailing `RowOwnerFilter rowOwnerFilter`. Document on the interface that the un-suffixed forms mean unrestricted:

```java
    boolean deleteRow(String baseName, long id, long environmentId, RowOwnerFilter rowOwnerFilter);

    DataTableRow getRow(String baseName, long id, long environmentId, RowOwnerFilter rowOwnerFilter);

    DataTableRow insertRow(
        String baseName, Map<String, Object> values, long environmentId, RowOwnerFilter rowOwnerFilter);

    List<DataTableRow> listRows(
        String baseName, int limit, int offset, long environmentId, RowOwnerFilter rowOwnerFilter);

    String exportCsv(String baseName, long environmentId, RowOwnerFilter rowOwnerFilter);

    void importCsv(String baseName, String csv, long environmentId, RowOwnerFilter rowOwnerFilter);

    DataTableRow updateRow(
        String baseName, long id, Map<String, Object> values, long environmentId, RowOwnerFilter rowOwnerFilter);
```

- [ ] **Step 5: Rename the impl bodies and add delegating originals**

In `DataTableRowServiceImpl`, give each existing method the new trailing parameter — the body is unchanged for now — and add the original signature as a one-line delegate. For example:

```java
    @Override
    public List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId) {
        return listRows(baseName, limit, offset, environmentId, RowOwnerFilter.unrestricted());
    }

    @Override
    public List<DataTableRow> listRows(
        String baseName, int limit, int offset, long environmentId, RowOwnerFilter rowOwnerFilter) {

        // existing body, unchanged in this task
    }
```

Repeat for all seven. Keep the `@SuppressFBWarnings` annotations on the methods that carry them.

- [ ] **Step 6: Run the module's whole suite to prove nothing changed**

```bash
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test > /tmp/p2t3full.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t3full.log | head
```

Expected: exit 0, every existing test unmodified.

- [ ] **Step 7: Compile the whole repo — the interface grew, and 22 files implement or call it**

```bash
./gradlew compileJava compileTestJava --continue > /tmp/p2t3c.log 2>&1; echo $?; grep -cE "^> Task .* FAILED" /tmp/p2t3c.log
```

Expected: exit 0 and a count of 0 — **after** you handle the one legitimate break.

`RemoteDataTableRowServiceClient` (`server/ee/libs/platform/platform-data-table/platform-data-table-remote-client`)
*implements* the interface rather than calling it, so it must grow the seven new methods. It is a pure stub —
every existing method throws `UnsupportedOperationException` — so the new ones do the same. Throwing is right
rather than delegating to the unscoped REST call, which would silently drop the filter.

Any break that is not that one means a *caller* was relying on something this task was supposed to leave
alone; stop and investigate.

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply > /tmp/p2t3sp.log 2>&1; echo $?
git add server/libs/platform/platform-data-table
git commit -m "- Add owner-scoped variants of every data table row operation"
```

---

### Task 4: Enforce the filter

**Files:**
- Modify: `server/libs/platform/platform-data-table/platform-data-table-service/src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java`
- Test: `server/libs/platform/platform-data-table/platform-data-table-service/src/test/java/com/bytechef/platform/data/table/execution/service/DataTableRowOwnerScopingIntTest.java`

An integration test, not a unit test: the whole point is the SQL predicate, and a mocked `JdbcTemplate` would assert the string rather than the behaviour. Plan 1 left the scaffolding in place — `DataTableIntTestConfiguration` and `application-testint.yml` already exist, but the configuration scans only the migration package, so widen its `basePackages` to `com.bytechef.platform.data.table`.

**Interfaces:**
- Consumes: `RowOwnerFilter` from Task 3; `ReservedColumns.OWNER_ID` / `OWNER_TYPE` from Plan 1.
- Produces: no new API. `insertRow` stamps the owner; `getRow`, `listRows`, `updateRow`, `deleteRow` and `exportCsv` apply `owner_id = ? OR owner_id IS NULL`.

- [ ] **Step 1: Write the failing integration test**

```java
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableRowOwnerScopingIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final RowOwnerFilter ACCOUNT_A = RowOwnerFilter.ownedBy(Owner.connectedUser(1L));
    private static final RowOwnerFilter ACCOUNT_B = RowOwnerFilter.ownedBy(Owner.connectedUser(2L));

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @BeforeEach
    void setUp() {
        dataTableService.createTable(
            "conversations", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);
    }

    @Test
    void testAnAccountSeesOnlyItsOwnRows() {
        dataTableRowService.insertRow("conversations", Map.of("title", "a"), ENVIRONMENT_ID, ACCOUNT_A);
        dataTableRowService.insertRow("conversations", Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        List<DataTableRow> rows = dataTableRowService.listRows(
            "conversations", 100, 0, ENVIRONMENT_ID, ACCOUNT_A);

        assertEquals(1, rows.size());

        DataTableRow dataTableRow = rows.getFirst();

        assertEquals("a", dataTableRow.values().get("title"));
    }

    @Test
    void testAnAccountAlsoSeesUnownedVendorRows() {
        dataTableRowService.insertRow(
            "conversations", Map.of("title", "shared"), ENVIRONMENT_ID, RowOwnerFilter.unrestricted());

        List<DataTableRow> rows = dataTableRowService.listRows(
            "conversations", 100, 0, ENVIRONMENT_ID, ACCOUNT_A);

        assertEquals(1, rows.size());
    }

    @Test
    void testUnrestrictedSeesEveryRow() {
        dataTableRowService.insertRow("conversations", Map.of("title", "a"), ENVIRONMENT_ID, ACCOUNT_A);
        dataTableRowService.insertRow("conversations", Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        List<DataTableRow> rows = dataTableRowService.listRows(
            "conversations", 100, 0, ENVIRONMENT_ID, RowOwnerFilter.unrestricted());

        assertEquals(2, rows.size());
    }

    @Test
    void testAnAccountCannotReadAnotherAccountsRowById() {
        DataTableRow inserted = dataTableRowService.insertRow(
            "conversations", Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        assertNull(dataTableRowService.getRow("conversations", inserted.id(), ENVIRONMENT_ID, ACCOUNT_A));
    }

    @Test
    void testAnAccountCannotDeleteAnotherAccountsRow() {
        DataTableRow inserted = dataTableRowService.insertRow(
            "conversations", Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        assertFalse(dataTableRowService.deleteRow("conversations", inserted.id(), ENVIRONMENT_ID, ACCOUNT_A));
    }

    @Test
    void testAnAccountCannotUpdateAnotherAccountsRow() {
        DataTableRow inserted = dataTableRowService.insertRow(
            "conversations", Map.of("title", "b"), ENVIRONMENT_ID, ACCOUNT_B);

        assertNull(
            dataTableRowService.updateRow(
                "conversations", inserted.id(), Map.of("title", "hacked"), ENVIRONMENT_ID, ACCOUNT_A));
    }
}
```

- [ ] **Step 2: Widen the int-test configuration**

In `DataTableIntTestConfiguration`, change `basePackages` from `com.bytechef.platform.data.table.configuration.migration` to `com.bytechef.platform.data.table`, and update its Javadoc — the narrow scan was a Plan 1 decision that this task supersedes. `DataTableStorageService` and the audit publisher come along; if either needs a collaborator the context lacks, add a mock `@Bean` here rather than widening further.

- [ ] **Step 3: Run it to verify it fails**

```bash
export DOCKER_HOST="unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests "*DataTableRowOwnerScopingIntTest" > /tmp/p2t4.log 2>&1; echo $?
```

Expected: failures on every scoping assertion — rows are currently visible to everyone.

- [ ] **Step 4: Stamp the owner on insert**

In the scoped `insertRow`, after `insertableColumnNames` is built, append the owner columns when the filter carries one. The reserved-column filter already keeps a caller from setting them directly, so this is the only writer:

```java
        List<Object> ownerValues = new ArrayList<>();
        List<String> allInsertColumnNames = new ArrayList<>(insertableColumnNames);

        Optional<Owner> ownerOptional = rowOwnerFilter.owner();

        if (ownerOptional.isPresent()) {
            Owner owner = ownerOptional.get();

            allInsertColumnNames.add(ReservedColumns.OWNER_ID);
            allInsertColumnNames.add(ReservedColumns.OWNER_TYPE);

            ownerValues.add(owner.id());

            OwnerType ownerType = owner.type();

            ownerValues.add(ownerType.ordinal());
        }
```

Build the columns clause, placeholders and bound parameters from `allInsertColumnNames` and the existing values followed by `ownerValues`. Read the current method before editing — it assembles `columnsClause`, `placeholders`, `valuesClause` and the `RETURNING` list separately, and all four must stay consistent.

- [ ] **Step 5: Filter every read and mutation**

Add one private helper and use it in `getRow`, `listRows`, `updateRow`, `deleteRow` and `exportCsv`:

```java
    private String ownerPredicate(RowOwnerFilter rowOwnerFilter) {
        if (rowOwnerFilter.isUnrestricted()) {
            return "";
        }

        return " AND (\"" + ReservedColumns.OWNER_ID + "\" = ? OR \"" + ReservedColumns.OWNER_ID + "\" IS NULL)";
    }
```

The predicate is appended to an existing `WHERE`, so `listRows` — which has none — needs `WHERE TRUE` before it. The owner id is a bound parameter, never interpolated. `exportCsv` already delegates to `listRows`, so passing the filter through is enough.

`importCsv` inserts through the same path as `insertRow`; route it through the scoped insert so imported rows are stamped.

- [ ] **Step 6: Run the integration test to verify it passes**

```bash
export DOCKER_HOST="unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --tests "*DataTableRowOwnerScopingIntTest" > /tmp/p2t4.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t4.log | head
```

Expected: exit 0, six tests passing. Then confirm they ran rather than being filtered away:

```bash
grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' server/libs/platform/platform-data-table/platform-data-table-service/build/test-results/testIntegration/TEST-com.bytechef.platform.data.table.execution.service.DataTableRowOwnerScopingIntTest.xml
```

- [ ] **Step 7: Run the module's whole suite and the repo compile**

```bash
export DOCKER_HOST="unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:test :server:libs:platform:platform-data-table:platform-data-table-service:testIntegration --continue > /tmp/p2t4full.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t4full.log | head
```

Expected: exit 0. `DataTableRowServiceEnforcementTest` is the one to watch — it exercises the unrestricted path, which must be unchanged.

- [ ] **Step 8: Format, check, commit**

```bash
./gradlew spotlessApply > /tmp/p2t4sp.log 2>&1; echo $?
./gradlew :server:libs:platform:platform-data-table:platform-data-table-service:check -x test -x testIntegration > /tmp/p2t4chk.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t4chk.log | head
git add server/libs/platform/platform-data-table
git commit -m "- Scope data table rows to their owner"
```

For any SpotBugs finding read `build/reports/spotbugs/test.html`; the XML report is disabled and never rewritten.

---

### Task 5: Resolve the owner in the component, and fail closed in the editor

**Files:**
- Create: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/owner/OwnerResolution.java`
- Modify: all six actions and three triggers under `server/libs/modules/components/data-table/src/main/java/com/bytechef/component/datatable/`
- Modify: `server/libs/modules/components/data-table/build.gradle.kts` if needed (`platform-component-api` declares `api(platform-api)`, so `Owner` should already resolve — verify before adding anything)
- Modify: `server/ee/.../security/ConnectedUserOwnerResolver.java` (fill in `resolveCurrentPrincipal`)
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/owner/OwnerResolutionTest.java` (create the test source set if the module has none; otherwise place it in `platform-component-service`)

**Interfaces:**
- Consumes: `OwnerResolver`, `Owner`, `RowOwnerFilter`, `ActionContextAware`.
- Produces: `OwnerResolution.resolve(ActionContextAware, ObjectProvider<OwnerResolver>)` returning `Optional<Owner>`. Shared by both components: the data table wraps it with `RowOwnerFilter.from(...)`, the knowledge base uses it directly in Task 7.

- [ ] **Step 1: Write the failing test**

```java
class OwnerResolutionTest {

    @Test
    void testNoResolverMeansUnrestricted() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, emptyProvider());

        assertEquals(Optional.empty(), owner);
    }

    @Test
    void testJobRunResolvesThroughTheJobPrincipal() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.isEditorEnvironment()).thenReturn(false);
        when(actionContextAware.getJobPrincipalId()).thenReturn(77L);
        when(actionContextAware.getPlatformType()).thenReturn(PlatformType.EMBEDDED);

        OwnerResolver ownerResolver = mock(OwnerResolver.class);

        when(ownerResolver.resolveJobPrincipal(77L, PlatformType.EMBEDDED))
            .thenReturn(Optional.of(Owner.connectedUser(1055L)));

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, providerOf(ownerResolver));

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);
    }

    @Test
    void testEditorRunResolvesFromTheSecurityPrincipal() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.isEditorEnvironment()).thenReturn(true);

        OwnerResolver ownerResolver = mock(OwnerResolver.class);

        when(ownerResolver.resolveCurrentPrincipal()).thenReturn(Optional.of(Owner.connectedUser(1055L)));

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, providerOf(ownerResolver));

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);
        verify(actionContextAware, never()).getJobPrincipalId();
    }

    @Test
    void testEditorRunNeverFallsBackToTheJobPrincipal() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.isEditorEnvironment()).thenReturn(true);

        OwnerResolver ownerResolver = mock(OwnerResolver.class);

        when(ownerResolver.resolveCurrentPrincipal()).thenReturn(Optional.empty());

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, providerOf(ownerResolver));

        assertEquals(Optional.empty(), owner);
        verify(actionContextAware, never()).getJobPrincipalId();
    }
}
```

Write `emptyProvider()` and `providerOf(...)` as small private helpers returning `ObjectProvider<OwnerResolver>` — `ObjectProvider` is an interface, so a Mockito mock stubbing `getIfAvailable()` is enough.

The fourth test is the important one. An editor run has no persisted job, so `getJobPrincipalId()` is null; a naive implementation that falls through to the job arm would call `resolveJobPrincipal(null, ...)` or skip resolution entirely and return unrestricted **while a connected user is driving the Test button**. The editor branch must be exclusive.

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:modules:components:data-table:test --tests "*OwnerResolutionTest" > /tmp/p2t5.log 2>&1; echo $?; grep -E "symbol:" /tmp/p2t5.log | head -3
```

Expected: `cannot find symbol: class OwnerResolution`.

- [ ] **Step 3: Write the resolution helper**

```java
public final class OwnerResolution {

    private OwnerResolution() {
    }

    public static Optional<Owner> resolve(
        ActionContextAware actionContextAware, ObjectProvider<OwnerResolver> ownerResolverProvider) {

        OwnerResolver ownerResolver = ownerResolverProvider.getIfAvailable();

        if (ownerResolver == null) {
            return Optional.empty();
        }

        if (actionContextAware.isEditorEnvironment()) {
            return ownerResolver.resolveCurrentPrincipal();
        }

        Long jobPrincipalId = actionContextAware.getJobPrincipalId();
        PlatformType platformType = actionContextAware.getPlatformType();

        if (jobPrincipalId == null || platformType == null) {
            return Optional.empty();
        }

        return ownerResolver.resolveJobPrincipal(jobPrincipalId, platformType);
    }
}
```

- [ ] **Step 4: Fill in resolveCurrentPrincipal in the EE resolver**

`ConnectedUserResourceMembershipResolver` in the same package already reads exactly this pair — `SecurityUtils.fetchCurrentUserLogin()` for the external id and `PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId()` for the environment — and both are SecurityContext reads with no database access. Reuse that shape, then map (externalId, environment) to the connected user through `ConnectedUserService`, returning empty when either is absent:

```java
    @Override
    public Optional<Owner> resolveCurrentPrincipal() {
        Optional<String> externalUserIdOptional = SecurityUtils.fetchCurrentUserLogin();

        if (externalUserIdOptional.isEmpty()) {
            return Optional.empty();
        }

        Optional<Long> environmentIdOptional = PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId();

        if (environmentIdOptional.isEmpty()) {
            return Optional.empty();
        }

        return connectedUserService
            .fetchConnectedUser(externalUserIdOptional.get(), toEnvironment(environmentIdOptional.get()))
            .map(connectedUser -> Owner.connectedUser(connectedUser.getId()));
    }
```

Check `ConnectedUserService`'s actual fetch signature before writing this and match it; add a unit test for the empty-login and empty-environment branches alongside Task 2's tests.

- [ ] **Step 5: Wire the nine component classes**

Each already casts `(ActionContextAware) actionContext`. Add the resolution and pass the filter to the scoped service method. `DataTableFindRecordsAction.perform` becomes:

```java
        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);
        int limit = inputParameters.getInteger(LIMIT, 100);
        int offset = inputParameters.getInteger(OFFSET, 0);

        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.from(
            OwnerResolution.resolve(actionContextAware, ownerResolverProvider));

        return dataTableRowService.listRows(
            baseName, limit, offset, Objects.requireNonNull(actionContextAware.getEnvironmentId()), rowOwnerFilter);
```

The `ObjectProvider<OwnerResolver>` is constructor-injected into `DataTableComponentHandler` and threaded to each action and trigger the same way `DataTableRowService` already is.

The three triggers matter as much as the actions: a record-created trigger that fires on another account's row leaks its contents into a workflow run.

- [ ] **Step 6: Run the component tests and regenerate the definition snapshot**

Component tests write `.json` definition files. Nothing in this task changes the definition, so the snapshot must be unchanged — if it differs, a property leaked into the public definition and that is a bug in this task.

```bash
./gradlew :server:libs:modules:components:data-table:test > /tmp/p2t5t.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t5t.log | head
git status --porcelain server/libs/modules/components/data-table/src/test/resources
```

Expected: exit 0, and no modification under `src/test/resources`.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply > /tmp/p2t5sp.log 2>&1; echo $?
git add server/libs/modules/components/data-table server/ee/libs/embedded/embedded-configuration
git commit -m "- Scope the data table component to the calling connected user"
```

---

### Task 6: Keep the component honest

The one weakness of an explicit filter is that a future action can call an unrestricted method and nothing complains. This converts that from convention into a check.

**Files:**
- Test: `server/libs/modules/components/data-table/src/test/java/com/bytechef/component/datatable/DataTableComponentUsesScopedRowServiceTest.java`

**Interfaces:**
- Consumes: nothing at compile time — it reads the component's own compiled classes.
- Produces: nothing.

- [ ] **Step 1: Write the test**

The module has no ArchUnit dependency and adding one for a single assertion is not worth it. Read the sources instead — it is a small, fixed set of files, and a source scan states the rule in terms a reviewer can check by eye:

```java
class DataTableComponentUsesScopedRowServiceTest {

    private static final List<String> UNSCOPED_CALLS = List.of(
        "dataTableRowService.listRows(", "dataTableRowService.getRow(", "dataTableRowService.insertRow(",
        "dataTableRowService.updateRow(", "dataTableRowService.deleteRow(", "dataTableRowService.exportCsv(",
        "dataTableRowService.importCsv(");

    @Test
    void testEveryRowServiceCallPassesARowOwnerFilter() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/bytechef/component/datatable");

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<String> offenders = paths.filter(path -> path.toString().endsWith(".java"))
                .filter(DataTableComponentUsesScopedRowServiceTest::callsRowServiceWithoutFilter)
                .map(Path::toString)
                .toList();

            assertTrue(offenders.isEmpty(), "These call DataTableRowService without a RowOwnerFilter: " + offenders);
        }
    }
}
```

Implement `callsRowServiceWithoutFilter` by reading the file, finding each occurrence of an entry in `UNSCOPED_CALLS`, taking the text through its matching close parenthesis, and reporting true when that argument list does not mention `rowOwnerFilter` or `RowOwnerFilter`. A brace-counting scan is enough; the calls are all single statements.

- [ ] **Step 2: Run it — it must pass, because Task 5 already fixed every call**

```bash
./gradlew :server:libs:modules:components:data-table:test --tests "*DataTableComponentUsesScopedRowServiceTest" > /tmp/p2t6.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t6.log | head
```

Expected: exit 0.

- [ ] **Step 3: Prove the test can fail**

Temporarily drop the filter argument from one call in `DataTableGetRecordAction`, re-run, confirm the test fails and names that file, then revert. A guard that has never been seen to fail is not known to guard anything.

- [ ] **Step 4: Format and commit**

```bash
./gradlew spotlessApply > /tmp/p2t6sp.log 2>&1; echo $?
git add server/libs/modules/components/data-table
git commit -m "- Fail the build when a data table action forgets its owner filter"
```

---

### Task 7: Reject a knowledge base the caller does not own

Closes a hole that predates this work: `KnowledgeBaseSearchAction.perform` reads `inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID)` and wraps the vector store with it, with no ownership check at all. An account can name another account's knowledge base id today.

**Files:**
- Modify: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-api/src/main/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseService.java`
- Modify: the corresponding `KnowledgeBaseServiceImpl`
- Modify: `server/libs/modules/components/ai/vectorstore/knowledgebase/src/main/java/com/bytechef/component/ai/vectorstore/knowledgebase/action/KnowledgeBaseSearchAction.java` and the sibling load/update/delete actions plus the two cluster elements
- Modify: `.../util/KnowledgeBaseOptionsUtils.java` — the picker calls the no-arg `getKnowledgeBases()` and so lists the whole tenant
- Test: `server/libs/platform/platform-knowledge-base/platform-knowledge-base-service/src/test/java/com/bytechef/platform/knowledgebase/service/KnowledgeBaseOwnershipTest.java`

**Interfaces:**
- Consumes: `Owner` from Task 1.
- Produces: `KnowledgeBaseService.getKnowledgeBase(Long id, Optional<Owner> owner)` which throws when the knowledge base is owned by someone else, and `List<KnowledgeBase> getKnowledgeBases(int environment, Optional<Owner> owner)`.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void testAnOwnedKnowledgeBaseIsRefusedToAnotherAccount() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setOwnerId(2L);
        knowledgeBase.setOwnerType(OwnerType.CONNECTED_USER);

        when(knowledgeBaseRepository.findById(7L)).thenReturn(Optional.of(knowledgeBase));

        assertThrows(
            ExecutionException.class,
            () -> knowledgeBaseService.getKnowledgeBase(7L, Optional.of(Owner.connectedUser(1L))));
    }

    @Test
    void testAnUnownedKnowledgeBaseIsReadableByAnyAccount() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        when(knowledgeBaseRepository.findById(7L)).thenReturn(Optional.of(knowledgeBase));

        assertNotNull(knowledgeBaseService.getKnowledgeBase(7L, Optional.of(Owner.connectedUser(1L))));
    }

    @Test
    void testAnAdminWithNoOwnerReadsAnything() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setOwnerId(2L);
        knowledgeBase.setOwnerType(OwnerType.CONNECTED_USER);

        when(knowledgeBaseRepository.findById(7L)).thenReturn(Optional.of(knowledgeBase));

        assertNotNull(knowledgeBaseService.getKnowledgeBase(7L, Optional.empty()));
    }
```

Match the module's existing error type rather than inventing one — check what `KnowledgeBaseServiceImpl` already throws for a missing knowledge base and use the same family.

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test --tests "*KnowledgeBaseOwnershipTest" > /tmp/p2t7.log 2>&1; echo $?
```

- [ ] **Step 3: Add the owner-aware methods and implement the check**

`KnowledgeBaseServiceImpl.getKnowledgeBase` currently reads:

```java
    public KnowledgeBase getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("KnowledgeBase not found: " + id));
    }
```

Keep that signature as the unrestricted form and add the owner-aware one beside it. **The refusal reuses the
not-found message verbatim** — a caller must not be able to tell "exists but is someone else's" from "does not
exist", or the id space becomes an enumeration oracle:

```java
    @Override
    public KnowledgeBase getKnowledgeBase(Long id) {
        return getKnowledgeBase(id, Optional.empty());
    }

    @Override
    public KnowledgeBase getKnowledgeBase(Long id, Optional<Owner> owner) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("KnowledgeBase not found: " + id));

        if (!isReadableBy(knowledgeBase, owner)) {
            throw new RuntimeException("KnowledgeBase not found: " + id);
        }

        return knowledgeBase;
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBases(int environment, Optional<Owner> owner) {
        return knowledgeBaseRepository.findAllByEnvironment(environment)
            .stream()
            .filter(knowledgeBase -> isReadableBy(knowledgeBase, owner))
            .toList();
    }

    private static boolean isReadableBy(KnowledgeBase knowledgeBase, Optional<Owner> owner) {
        Long ownerId = knowledgeBase.getOwnerId();

        if (ownerId == null) {
            return true;
        }

        return owner.filter(curOwner -> curOwner.id() == ownerId && curOwner.type() == knowledgeBase.getOwnerType())
            .isPresent();
    }
```

Three rules, matching the data-table ones: an unowned knowledge base is readable by everyone, an empty owner
(admin, automation) reads everything, and an owned one is readable only by that exact owner. Confirm the
repository's existing by-environment finder name before using `findAllByEnvironment`.

- [ ] **Step 4: Wire the component actions and the picker**

Six call sites in the knowledge-base component, each already holding an `ActionContext`:
`KnowledgeBaseSearchAction`, `KnowledgeBaseLoadAction`, `KnowledgeBaseUpdateAction`, `KnowledgeBaseDeleteAction`
and the two cluster elements `KnowledgeBaseSearchTool` / `KnowledgeBaseUpdateTool`.

Each resolves the owner with the Task 5 helper and passes it before touching the vector store:

```java
        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);

        Optional<Owner> owner = OwnerResolution.resolve(actionContextAware, ownerResolverProvider);

        knowledgeBaseService.getKnowledgeBase(knowledgeBaseId, owner);
```

The `getKnowledgeBase` call is the gate — it throws before `KnowledgeBaseVectorStoreWrapper` is ever constructed,
so no unauthorized id reaches the vector store. Thread `ObjectProvider<OwnerResolver>` in through
`KnowledgeBaseComponentHandler`'s constructor the same way the services already are.

Then fix the picker in `KnowledgeBaseOptionsUtils.buildKnowledgeBaseOptions`, which calls the **no-arg**
`getKnowledgeBases()` and so lists every knowledge base in the tenant. Switch it to the owner-aware
`getKnowledgeBases(environment, owner)`; leaving it would keep leaking the names the runtime now protects.

Note the cluster elements take a `ClusterElementContext` rather than an `ActionContext`. Check whether it also
implements `ActionContextAware` — if it does not, resolve through whichever context-aware interface it exposes
rather than casting blindly.

- [ ] **Step 5: Run both modules' tests and the definition snapshots**

```bash
./gradlew :server:libs:platform:platform-knowledge-base:platform-knowledge-base-service:test :server:libs:modules:components:ai:vectorstore:knowledgebase:test --continue > /tmp/p2t7full.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/p2t7full.log | head
git status --porcelain server/libs/modules/components/ai/vectorstore/knowledgebase/src/test/resources
```

Expected: exit 0 and no snapshot modification.

- [ ] **Step 6: Format, whole-repo compile, commit**

```bash
./gradlew spotlessApply > /tmp/p2t7sp.log 2>&1; echo $?
./gradlew compileJava compileTestJava --continue > /tmp/p2t7c.log 2>&1; echo $?; grep -cE "^> Task .* FAILED" /tmp/p2t7c.log
git add server/libs/platform/platform-knowledge-base server/libs/modules/components/ai/vectorstore/knowledgebase
git commit -m "- Refuse a knowledge base the calling account does not own"
```

---

## Deliberately not in this plan

- **`findRecords` gets no filter grammar.** It is a distinct feature with its own query DSL and is not what isolation rests on — isolation is the service-level owner predicate. It becomes Plan 3, and `ContextStoreQueryFilter`'s operator set (`EQ, NEQ, IN, CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN`) is the grammar to follow.
- **No startup guard for a missing `OwnerResolver` in a distributed EE app.** `OwnerResolution` returns an empty owner when the resolver is absent, which both components read as unrestricted, which is correct for Community and wrong for an EE app that carries connected users but not `embedded-configuration-service`. This is the same shape as the gap already recorded for `WorkflowVariablesResolver`. Only `server-app` runs the affected paths today; the guard belongs with the deployment work that also chooses the Plan 1 migrator's tenant hook.
- **The admin API and UI**, which remain Plans 4 and 5.
