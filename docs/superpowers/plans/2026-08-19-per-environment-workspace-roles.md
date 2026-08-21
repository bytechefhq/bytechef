# Per-Environment Workspace Roles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a workspace member hold a different role per environment — editor in Development, viewer in Production, or no access to Production at all — and make promotion check the role in the environment being deployed *into*.

**Architecture:** A nullable `environment` column on `workspace_user`, where `NULL` means "every environment" and preserves today's behaviour with no migration. Per (workspace, user) the membership is either *implicit* (one NULL row) or *explicit* (one row per environment, absence meaning denial); exactly one mode exists at a time, so the resolver needs no precedence rule. Permission checks gain environment-aware overloads that take the environment as an explicit argument, and the promotion guard passes the deployment DTO so the evaluator reads the target environment from it.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Liquibase, PostgreSQL 15+, JUnit 5, Mockito, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-08-19-per-environment-workspace-roles-design.md`

## Global Constraints

- **Enterprise Edition only.** Every production file in this plan lives under `server/ee/`. CE's `PermissionServiceImpl.hasWorkspaceScope` returns `SecurityUtils.isAuthenticated()` and must keep doing so — CE has no authorization boundary between workspace members.
- **EE file conventions:** use the ByteChef Enterprise license header (not Apache 2.0) and add a `@version ee` Javadoc tag to every class under `server/ee/`. Copy the header verbatim from a neighbouring file in the same module.
- **No data migration.** Every existing `workspace_user` row keeps `environment = NULL`, which is implicit mode, which is exactly today's behaviour. A task that requires backfilling rows has misread the design.
- **`Environment` is an enum**, `com.bytechef.platform.configuration.domain.Environment`, with values `DEVELOPMENT, STAGING, PRODUCTION`. It is persisted as an **INT ordinal**, matching the repo-wide convention. Do not add an `environment` table.
- **Enum ordinals are pinned.** `Environment` values are persisted by ordinal; never reorder them, and append any new value at the end.
- **Never read the environment from `EnvironmentContext`.** It is a thread-local holding the *source* environment during a promotion and is already known to be lost on worker threads and in agent tool calls. Every check in this plan takes the environment as an explicit parameter.
- **Blank line before control statements** (`if`, `for`, `while`, `try`, `switch`) and **after a variable modification that a following statement uses**, per the repo Java style. Very short top-of-method guard clauses may omit it.
- **No trailing blank line** between a class's last member and its closing brace.
- **Descriptive variable names.** No single letters, no `_` prefix on private methods.
- **Test naming:** unit test classes end in `Test`, integration test classes end in `IntTest`. Test method names are camelCase with no underscores — `testResolvesEnvironmentRow`, never `testResolves_EnvironmentRow`. Checkstyle enforces this on **all** methods in test sources, including private helpers.
- **Never judge a Gradle run through a pipe.** `./gradlew ... | tail` reports `tail`'s exit code. Redirect to a file, check `$?` on its own line, then grep the file for `^> Task .* FAILED`. Do not grep for `error:` — it matches module paths like `:server:libs:core:error:`.
- **Run `./gradlew spotlessApply`** before every commit that touches Java.
- **Commit convention:** `732 <description>` for server-side changes. Never amend; always fresh commits. Stage only the files the task changed.

---

## File Structure

**Modified — domain and persistence (EE):**
- `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/domain/WorkspaceUser.java` — gains an `environment` field and environment-aware factories.
- `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/WorkspaceUserRepository.java` — gains environment-aware finders; the existing `findByUserIdAndWorkspaceId` is narrowed to the NULL row.
- New Liquibase changelog under `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/`.

**Modified — resolution and checks (EE):**
- `.../service/WorkspaceUserServiceImpl.java` and its API interface `.../service/WorkspaceUserService.java` — gain `fetchRole` and the mode-switching writes.
- `.../service/WorkspaceScopeCacheService.java` — the environment joins the cache key.
- `.../service/PermissionServiceImpl.java` (EE) — environment-aware overloads.
- `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/PermissionService.java` — the interface the overloads are declared on.
- `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java` (CE) — the new overloads return `isAuthenticated()`.

**Modified — promotion:**
- `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/AutomationPermissionEvaluator.java` — recognises `ProjectDeploymentDTO`.
- `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeImpl.java:148` — the guard passes the DTO.

**New tests:**
- `.../service/WorkspaceUserRoleResolutionTest.java` — the resolution matrix.
- `.../service/WorkspaceUserModeSwitchingTest.java` — implicit ⇄ explicit transitions.
- `.../PerEnvironmentRoleIntTest.java` — the partial unique indexes against a real PostgreSQL.
- `.../security/PromotionTargetEnvironmentTest.java` — the target environment decides.

**Existing tests that must keep passing untouched** (they are the regression guard that today's behaviour is preserved): `WorkspaceUserServiceTest`, `PermissionServiceTest`, `PermissionServiceResourceTest`, `WorkspaceScopeCacheServiceTest`, `WorkspaceScopeCacheKeyConsistencyTest`.

---

## Task 1: Add the environment column and its partial unique indexes

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20260819120000_automation_configuration_added_column_workspace_user_environment.xml`
- Modify: the module's `master.xml` (find it with `find server/ee/libs/automation/automation-configuration -name 'master.xml'`) — add the `<include>` at the end of the existing list
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/PerEnvironmentRoleIntTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: a nullable `workspace_user.environment` INT column, plus the indexes `uk_workspace_user_implicit` and `uk_workspace_user_explicit`.

- [ ] **Step 1: Write the failing integration test**

`@IntTest` classes here use Testcontainers, which builds the schema from the changelogs — so this test proves the changelog, not a mock.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
class PerEnvironmentRoleIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testAllowsOneImplicitRowPerMember() {
        insertMember(1L, 1L, null);

        assertThatThrownBy(() -> insertMember(1L, 1L, null))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testAllowsOneRowPerEnvironment() {
        insertMember(2L, 2L, 0);
        insertMember(2L, 2L, 1);

        assertThatThrownBy(() -> insertMember(2L, 2L, 0))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testAllowsImplicitAndExplicitRowsToCoexistInTheDatabase() {
        insertMember(3L, 3L, null);

        assertThatCode(() -> insertMember(3L, 3L, 2)).doesNotThrowAnyException();
    }

    private void insertMember(long userId, long workspaceId, Integer environment) {
        jdbcTemplate.update(
            "INSERT INTO workspace_user (user_id, workspace_id, workspace_role, environment, version) "
                + "VALUES (?, ?, 1, ?, 0)",
            userId, workspaceId, environment);
    }
}
```

The third test is deliberate: the **database** permits the combination, because a partial index cannot express "not both modes". The one-mode-at-a-time rule is a service-layer invariant, enforced in Task 4 and tested there. Writing it here records that the constraint is intentionally weaker than the rule.

- [ ] **Step 2: Run it and watch it fail**

```bash
cd /Volumes/Data/bytechef/bytechef
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*PerEnvironmentRoleIntTest*' --console=plain > /tmp/t1.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t1.txt
```

Expected: FAILED, with an error naming the unknown `environment` column.

- [ ] **Step 3: Write the changelog**

Copy the license header and `<databaseChangeLog>` preamble from the neighbouring `202604061200040_automation_configuration_added_column_workspace_user_custom_role.xml`, then:

```xml
<changeSet id="20260819120000" author="ivicac">
    <addColumn tableName="workspace_user">
        <column name="environment" type="INT"/>
    </addColumn>
</changeSet>

<changeSet id="20260819120001" author="ivicac">
    <sql>
        CREATE UNIQUE INDEX uk_workspace_user_implicit
            ON workspace_user (workspace_id, user_id) WHERE environment IS NULL;
    </sql>
    <sql>
        CREATE UNIQUE INDEX uk_workspace_user_explicit
            ON workspace_user (workspace_id, user_id, environment) WHERE environment IS NOT NULL;
    </sql>
    <rollback>
        <sql>DROP INDEX IF EXISTS uk_workspace_user_implicit;</sql>
        <sql>DROP INDEX IF EXISTS uk_workspace_user_explicit;</sql>
    </rollback>
</changeSet>
```

Partial indexes need raw `<sql>` — Liquibase's `<createIndex>` has no `WHERE` clause. The explicit `<rollback>` is required because Liquibase cannot auto-generate a rollback for raw SQL.

- [ ] **Step 4: Register it in master.xml**

Add `<include file="config/liquibase/changelog/automation/configuration/20260819120000_automation_configuration_added_column_workspace_user_environment.xml"/>` as the **last** include, matching the surrounding style exactly.

- [ ] **Step 5: Run the test and watch it pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*PerEnvironmentRoleIntTest*' --console=plain > /tmp/t1.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t1.txt
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test
git commit -m "732 Add the workspace_user environment column and its partial unique indexes"
```

---

## Task 2: Carry environment on the WorkspaceUser entity

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/domain/WorkspaceUser.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/test/java/com/bytechef/ee/automation/configuration/domain/WorkspaceUserTest.java`

**Interfaces:**
- Consumes: the `environment` column from Task 1.
- Produces:
  - `Environment getEnvironment()` — returns `null` for an implicit row
  - `static WorkspaceUser forRole(Long userId, Long workspaceId, WorkspaceRole workspaceRole, Environment environment)`
  - `static WorkspaceUser forCustomRole(Long userId, Long workspaceId, long customRoleId, Environment environment)`
  - The existing 3-arg `forRole` and `forCustomRole` remain and produce implicit rows.

- [ ] **Step 1: Write the failing test**

```java
class WorkspaceUserTest {

    @Test
    void testThreeArgFactoryProducesAnImplicitRow() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(1L, 2L, WorkspaceRole.EDITOR);

        assertThat(workspaceUser.getEnvironment()).isNull();
    }

    @Test
    void testFourArgFactoryCarriesTheEnvironment() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(1L, 2L, WorkspaceRole.VIEWER, Environment.PRODUCTION);

        assertThat(workspaceUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testCustomRoleFactoryCarriesTheEnvironment() {
        WorkspaceUser workspaceUser = WorkspaceUser.forCustomRole(1L, 2L, 9L, Environment.DEVELOPMENT);

        assertThat(workspaceUser.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(workspaceUser.getCustomRoleId()).isEqualTo(9L);
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-api:test --tests '*WorkspaceUserTest*' --console=plain > /tmp/t2.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t2.txt
```

Expected: FAILED — `getEnvironment()` does not exist.

- [ ] **Step 3: Add the field and factories**

The entity stores the ordinal, matching how `workspaceRole` is stored as `Integer`:

```java
@Column("environment")
private Integer environment;
```

```java
public Environment getEnvironment() {
    return environment == null ? null : Environment.values()[environment];
}
```

```java
public static WorkspaceUser forRole(
    Long userId, Long workspaceId, WorkspaceRole workspaceRole, Environment environment) {

    WorkspaceUser workspaceUser = forRole(userId, workspaceId, workspaceRole);

    workspaceUser.environment = environment == null ? null : environment.ordinal();

    return workspaceUser;
}

public static WorkspaceUser forCustomRole(
    Long userId, Long workspaceId, long customRoleId, Environment environment) {

    WorkspaceUser workspaceUser = forCustomRole(userId, workspaceId, customRoleId);

    workspaceUser.environment = environment == null ? null : environment.ordinal();

    return workspaceUser;
}
```

`Environment.values()[ordinal]` is safe only because the ordinals are pinned — see the Global Constraints.

- [ ] **Step 4: Run the test and watch it pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-api:test --tests '*WorkspaceUserTest*' --console=plain > /tmp/t2.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t2.txt
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/ee/libs/automation/automation-configuration/automation-configuration-api
git commit -m "732 Carry the environment on the WorkspaceUser entity"
```

---

## Task 3: Narrow the repository finders

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/WorkspaceUserRepository.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/PerEnvironmentRoleIntTest.java` (extend Task 1's class)

**Interfaces:**
- Consumes: `WorkspaceUser.getEnvironment()` from Task 2.
- Produces:
  - `Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironmentIsNull(long userId, long workspaceId)`
  - `Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironment(long userId, long workspaceId, int environment)`
  - `List<WorkspaceUser> findAllByUserIdAndWorkspaceId(long userId, long workspaceId)`

**This is the task that makes the existing `Optional<WorkspaceUser> findByUserIdAndWorkspaceId` unsafe.** It assumes one row per (user, workspace), which stops being true. It is **kept** — deleting it would touch roughly 25 stubs in `WorkspaceUserServiceTest` alone — but every production caller moves to an explicit finder in Tasks 4–6. Do not change its signature.

- [ ] **Step 1: Write the failing test**

Add to `PerEnvironmentRoleIntTest`:

```java
@Autowired
private WorkspaceUserRepository workspaceUserRepository;

@Test
void testImplicitFinderIgnoresEnvironmentRows() {
    workspaceUserRepository.save(WorkspaceUser.forRole(10L, 10L, WorkspaceRole.EDITOR, null));
    workspaceUserRepository.save(WorkspaceUser.forRole(10L, 10L, WorkspaceRole.VIEWER, Environment.PRODUCTION));

    Optional<WorkspaceUser> implicitRow =
        workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(10L, 10L);

    assertThat(implicitRow).isPresent();
    assertThat(implicitRow.get().getEnvironment()).isNull();
}

@Test
void testEnvironmentFinderReturnsOnlyThatEnvironment() {
    workspaceUserRepository.save(WorkspaceUser.forRole(11L, 11L, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));

    assertThat(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
        11L, 11L, Environment.DEVELOPMENT.ordinal())).isPresent();
    assertThat(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
        11L, 11L, Environment.PRODUCTION.ordinal())).isEmpty();
}

@Test
void testListFinderReturnsEveryRowForTheMember() {
    workspaceUserRepository.save(WorkspaceUser.forRole(12L, 12L, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
    workspaceUserRepository.save(WorkspaceUser.forRole(12L, 12L, WorkspaceRole.VIEWER, Environment.STAGING));

    assertThat(workspaceUserRepository.findAllByUserIdAndWorkspaceId(12L, 12L)).hasSize(2);
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*PerEnvironmentRoleIntTest*' --console=plain > /tmp/t3.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t3.txt
```

Expected: FAILED — the finders do not exist.

- [ ] **Step 3: Declare the finders**

Spring Data derives all three from their names; no `@Query` is needed.

```java
Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironmentIsNull(long userId, long workspaceId);

Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironment(long userId, long workspaceId, int environment);

List<WorkspaceUser> findAllByUserIdAndWorkspaceId(long userId, long workspaceId);
```

Add a Javadoc line on the existing `findByUserIdAndWorkspaceId` recording that it is ambiguous once a member has environment rows, and that callers wanting the implicit row should use `findByUserIdAndWorkspaceIdAndEnvironmentIsNull`.

- [ ] **Step 4: Run the test and watch it pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*PerEnvironmentRoleIntTest*' --console=plain > /tmp/t3.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t3.txt
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/ee/libs/automation/automation-configuration/automation-configuration-service
git commit -m "732 Add environment-aware WorkspaceUser finders"
```

---

## Task 4: Resolve a role for an environment, and enforce one mode

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/service/WorkspaceUserService.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/WorkspaceUserServiceImpl.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/domain/ResolvedRole.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/WorkspaceUserRoleResolutionTest.java`

**Interfaces:**
- Consumes: the three finders from Task 3.
- Produces:
  - `record ResolvedRole(WorkspaceRole workspaceRole, Long customRoleId)`
  - `Optional<ResolvedRole> fetchRole(long userId, long workspaceId, Environment environment)` on `WorkspaceUserService`

- [ ] **Step 1: Write the failing resolution test**

```java
@ExtendWith(MockitoExtension.class)
class WorkspaceUserRoleResolutionTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 2L;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceUserServiceImpl workspaceUserService;

    @Test
    void testImplicitRowAppliesToEveryEnvironment() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR)));

        Optional<ResolvedRole> resolved =
            workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(resolved).map(ResolvedRole::workspaceRole).hasValue(WorkspaceRole.EDITOR);
    }

    @Test
    void testEnvironmentRowWins() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(
                    WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION)));

        Optional<ResolvedRole> resolved =
            workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(resolved).map(ResolvedRole::workspaceRole).hasValue(WorkspaceRole.VIEWER);
        verify(workspaceUserRepository, never()).findByUserIdAndWorkspaceIdAndEnvironmentIsNull(anyLong(), anyLong());
    }

    @Test
    void testNoRowForTheEnvironmentAndNoImplicitRowDenies() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testResolvesACustomRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.STAGING.ordinal()))
                .thenReturn(Optional.of(
                    WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 7L, Environment.STAGING)));

        Optional<ResolvedRole> resolved =
            workspaceUserService.fetchRole(USER_ID, WORKSPACE_ID, Environment.STAGING);

        assertThat(resolved).map(ResolvedRole::customRoleId).hasValue(7L);
    }
}
```

The `verify(..., never())` in the second test is the point of that test: an environment row must **short-circuit**, not merge with the implicit row.

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceUserRoleResolutionTest*' --console=plain > /tmp/t4.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t4.txt
```

Expected: FAILED — `fetchRole` and `ResolvedRole` do not exist.

- [ ] **Step 3: Add `ResolvedRole`**

```java
public record ResolvedRole(WorkspaceRole workspaceRole, Long customRoleId) {
}
```

Exactly one of the two is non-null. It exists so callers do not re-implement the built-in-versus-custom branch that `WorkspaceUser` already encodes.

- [ ] **Step 4: Implement `fetchRole`**

```java
@Override
public Optional<ResolvedRole> fetchRole(long userId, long workspaceId, Environment environment) {
    Optional<WorkspaceUser> environmentRow = workspaceUserRepository
        .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal());

    if (environmentRow.isPresent()) {
        return environmentRow.map(WorkspaceUserServiceImpl::toResolvedRole);
    }

    return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
        .map(WorkspaceUserServiceImpl::toResolvedRole);
}

private static ResolvedRole toResolvedRole(WorkspaceUser workspaceUser) {
    Long customRoleId = workspaceUser.getCustomRoleId();

    if (customRoleId != null) {
        return new ResolvedRole(null, customRoleId);
    }

    return new ResolvedRole(WorkspaceRole.values()[workspaceUser.getWorkspaceRole()], null);
}
```

Note what this does **not** do: it never consults the implicit row once an environment row exists. That is what makes "an environment with no row is denied" hold — a member in explicit mode has no implicit row at all, so the fallback finds nothing.

- [ ] **Step 5: Run the test and watch it pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceUserRoleResolutionTest*' --console=plain > /tmp/t4.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t4.txt
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Write the failing mode-switching test**

Create `WorkspaceUserModeSwitchingTest` in the same package:

```java
@ExtendWith(MockitoExtension.class)
class WorkspaceUserModeSwitchingTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 2L;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceUserServiceImpl workspaceUserService;

    @Test
    void testGrantingAnEnvironmentRoleDeletesTheImplicitRow() {
        WorkspaceUser implicitRow = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(implicitRow));
        when(workspaceUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        workspaceUserService.setEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT, WorkspaceRole.EDITOR);

        verify(workspaceUserRepository).delete(implicitRow);
    }

    @Test
    void testRemovingTheLastEnvironmentRowRemovesMembership() {
        WorkspaceUser onlyRow =
            WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal())).thenReturn(Optional.of(onlyRow));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of());

        workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT);

        verify(workspaceUserRepository).delete(onlyRow);
        verify(workspaceUserRepository, never()).save(any());
    }
}
```

The second test's `never()).save(any())` is the security assertion: removing a member's last environment must **not** silently write an implicit row, which would turn "revoke their last environment" into "grant them every environment".

- [ ] **Step 7: Run it and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceUserModeSwitchingTest*' --console=plain > /tmp/t4b.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t4b.txt
```

Expected: FAILED — the two methods do not exist.

- [ ] **Step 8: Implement the mode-switching writes**

```java
@Override
@Transactional
public WorkspaceUser setEnvironmentRole(
    long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole) {

    workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
        .ifPresent(workspaceUserRepository::delete);

    Optional<WorkspaceUser> existing = workspaceUserRepository
        .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal());

    if (existing.isPresent()) {
        WorkspaceUser workspaceUser = existing.get();

        workspaceUser.setWorkspaceRole(workspaceRole.ordinal());

        return workspaceUserRepository.save(workspaceUser);
    }

    return workspaceUserRepository.save(
        WorkspaceUser.forRole(userId, workspaceId, workspaceRole, environment));
}

@Override
@Transactional
public void removeEnvironmentRole(long userId, long workspaceId, Environment environment) {
    workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal())
        .ifPresent(workspaceUserRepository::delete);
}
```

`removeEnvironmentRole` deliberately does nothing else. When the deleted row was the member's last, they end with no rows, which is the same state as not being a member — per the spec. Returning someone to implicit mode is `addWorkspaceUser`, an explicit call that names the role.

Declare both on `WorkspaceUserService` with Javadoc stating the mode-switch behaviour.

- [ ] **Step 9: Run both tests and watch them pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceUserRoleResolutionTest*' --tests '*WorkspaceUserModeSwitchingTest*' --console=plain > /tmp/t4.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t4.txt
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Confirm the existing service tests still pass untouched**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceUserServiceTest*' --console=plain > /tmp/t4c.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t4c.txt
```

Expected: BUILD SUCCESSFUL, with no edits to that file. It stubs `findByUserIdAndWorkspaceId` roughly 25 times; if it needs changing, a signature was altered that should not have been.

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/ee/libs/automation/automation-configuration
git commit -m "732 Resolve a workspace role per environment and enforce one membership mode"
```

---

## Task 5: Put the environment in the scope cache key

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/WorkspaceScopeCacheService.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/WorkspaceScopeCacheKeyConsistencyTest.java` (extend the existing class)

**Interfaces:**
- Consumes: `fetchRole` from Task 4.
- Produces: `Set<String> getWorkspaceScopes(long userId, long workspaceId, Environment environment)`.

**Read this before writing code.** `getWorkspaceScopes` is annotated `@Cacheable(value = WORKSPACE_SCOPES_CACHE)` with no explicit key, so Spring derives the key from the method arguments — today `(userId, workspaceId)`. Adding an environment parameter without it reaching the key means the first environment checked warms the cache and every later check on a different environment is served that same scope set. A member who is viewer in Production would be answered with their Development scopes. Adding the parameter to the method signature is what fixes it, because Spring's default key generator includes all arguments — but the test below is what proves it, and it must be written first.

- [ ] **Step 1: Write the failing cache-separation test**

Add to `WorkspaceScopeCacheKeyConsistencyTest`:

```java
@Test
void testTwoEnvironmentsDoNotShareACacheEntry() {
    when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
        USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal()))
            .thenReturn(Optional.of(
                WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT)));
    when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
        USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(
                WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION)));

    Set<String> developmentScopes =
        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT);
    Set<String> productionScopes =
        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID, Environment.PRODUCTION);

    assertThat(developmentScopes).isNotEqualTo(productionScopes);
    verify(workspaceUserRepository).findByUserIdAndWorkspaceIdAndEnvironment(
        USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT.ordinal());
    verify(workspaceUserRepository).findByUserIdAndWorkspaceIdAndEnvironment(
        USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal());
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceScopeCacheKeyConsistencyTest*' --console=plain > /tmp/t5.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t5.txt
```

Expected: FAILED — the 3-argument overload does not exist.

- [ ] **Step 3: Add the environment to the cached method and the eviction**

```java
@Cacheable(value = WORKSPACE_SCOPES_CACHE)
public Set<String> getWorkspaceScopes(long userId, long workspaceId, Environment environment) {
    return workspaceUserService.fetchRole(userId, workspaceId, environment)
        .map(resolvedRole -> dispatchScopes(resolvedRole, userId, workspaceId))
        .orElse(Collections.emptySet());
}
```

Keep the existing 2-argument method, delegating with the implicit row's semantics, so no current caller changes:

```java
public Set<String> getWorkspaceScopes(long userId, long workspaceId) {
    return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
        .map(workspaceUser -> dispatchScopes(workspaceUser, userId, workspaceId))
        .orElse(Collections.emptySet());
}
```

Then widen `evictSingleEntry` so a single eviction clears every environment for that member — the cache holds up to one entry per environment and the enum is closed, so the loop is bounded:

```java
private void evictSingleEntry(long userId, long workspaceId) {
    Cache cache = cacheManager.getCache(WORKSPACE_SCOPES_CACHE);

    if (cache == null) {
        return;
    }

    cache.evict(new SimpleKey(userId, workspaceId));

    for (Environment environment : Environment.values()) {
        cache.evict(new SimpleKey(userId, workspaceId, environment));
    }
}
```

Both key shapes are evicted because both method arities are cached.

- [ ] **Step 4: Run the test and watch it pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*WorkspaceScopeCache*' --console=plain > /tmp/t5.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t5.txt
```

Expected: BUILD SUCCESSFUL — including the pre-existing `WorkspaceScopeCacheServiceTest`, unedited.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/ee/libs/automation/automation-configuration/automation-configuration-service
git commit -m "732 Include the environment in the workspace scope cache key"
```

---

## Task 6: Environment-aware permission checks

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/PermissionService.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java` (CE)
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java` (EE)
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/PermissionServiceEnvironmentTest.java`

**Interfaces:**
- Consumes: `getWorkspaceScopes(userId, workspaceId, environment)` from Task 5.
- Produces:
  - `boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment)`
  - `boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment)`

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class PermissionServiceEnvironmentTest {

    private static final long WORKSPACE_ID = 2L;

    @Mock
    private WorkspaceScopeCacheService workspaceScopeCacheService;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void testAllowsWhenTheEnvironmentRoleGrantsTheScope() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(1L));
        when(workspaceScopeCacheService.getWorkspaceScopes(1L, WORKSPACE_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isTrue();
    }

    @Test
    void testDeniesWhenTheEnvironmentRoleDoesNotGrantTheScope() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(1L));
        when(workspaceScopeCacheService.getWorkspaceScopes(1L, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testDeniesWhenTheMemberHasNoRoleInThatEnvironment() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(1L));
        when(workspaceScopeCacheService.getWorkspaceScopes(1L, WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());

        assertThat(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*PermissionServiceEnvironmentTest*' --console=plain > /tmp/t6.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t6.txt
```

Expected: FAILED — the 3-argument overloads do not exist.

- [ ] **Step 3: Declare the overloads on the interface**

Add to `PermissionService`, with Javadoc stating that the environment is the one being acted **on** and must be passed explicitly rather than read from `EnvironmentContext`:

```java
boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment);

boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment);
```

- [ ] **Step 4: Implement them in CE as pass-throughs**

CE has no boundary between workspace members, so these match the existing 2-argument behaviour exactly:

```java
@Override
public boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment) {
    return SecurityUtils.isAuthenticated();
}

@Override
public boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment) {
    return SecurityUtils.isAuthenticated();
}
```

- [ ] **Step 5: Implement them in EE**

```java
@Override
public boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment) {
    if (!SecurityUtils.isAuthenticated()) {
        return false;
    }

    if (isTenantAdmin()) {
        return true;
    }

    OptionalLong userId = currentUserResolver.fetchCurrentUserId();

    if (userId.isEmpty()) {
        return false;
    }

    Set<String> scopes = workspaceScopeCacheService.getWorkspaceScopes(
        userId.getAsLong(), workspaceId, environment);

    return scopes.contains(scope);
}
```

`hasWorkspaceScopeForProject(projectId, scope, environment)` resolves the workspace from the project exactly as its 2-argument sibling does, then delegates to the method above.

Leave both existing 2-argument methods untouched. They resolve against the implicit row and every current call site keeps its behaviour.

- [ ] **Step 6: Run the test and watch it pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*PermissionService*' --console=plain > /tmp/t6.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t6.txt
```

Expected: BUILD SUCCESSFUL — including the pre-existing `PermissionServiceTest` and `PermissionServiceResourceTest`, unedited.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/libs/automation/automation-configuration server/ee/libs/automation/automation-configuration
git commit -m "732 Add environment-aware workspace scope checks"
```

---

## Task 7: Make promotion check the target environment

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/AutomationPermissionEvaluator.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeImpl.java:148`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/PromotionTargetEnvironmentTest.java`

**Interfaces:**
- Consumes: `hasWorkspaceScopeForProject(projectId, scope, environment)` from Task 6.
- Produces: nothing later tasks depend on. This is the last task.

**This is the task the feature exists for.** The guard today is:

```java
@PreAuthorize("hasPermission(#projectDeploymentDTO.projectId, 'Project', 'WORKFLOW_EDIT')")
public long createProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO)
```

`ProjectDeploymentDTO` already carries `Environment environment`; the guard passes the project id and ignores it. If the check used the caller's *current* environment instead of the DTO's, the whole feature would be decorative — anyone who could edit in Development could put code into Production.

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class PromotionTargetEnvironmentTest {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AutomationPermissionEvaluator automationPermissionEvaluator;

    @Test
    void testChecksTheEnvironmentCarriedByTheDeployment() {
        ProjectDeploymentDTO projectDeploymentDTO = projectDeployment(42L, Environment.PRODUCTION);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .thenReturn(false);

        boolean allowed = automationPermissionEvaluator.hasPermission(
            authentication(), projectDeploymentDTO, "WORKFLOW_EDIT");

        assertThat(allowed).isFalse();
        verify(permissionService).hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT", Environment.PRODUCTION);
    }

    @Test
    void testAllowsWhenTheTargetEnvironmentGrantsTheScope() {
        ProjectDeploymentDTO projectDeploymentDTO = projectDeployment(42L, Environment.DEVELOPMENT);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .thenReturn(true);

        assertThat(automationPermissionEvaluator.hasPermission(
            authentication(), projectDeploymentDTO, "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testNeverConsultsTheAmbientEnvironment() {
        EnvironmentContext.setCurrentEnvironment(Environment.DEVELOPMENT);

        try {
            ProjectDeploymentDTO projectDeploymentDTO = projectDeployment(42L, Environment.PRODUCTION);

            when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT", Environment.PRODUCTION))
                .thenReturn(false);

            assertThat(automationPermissionEvaluator.hasPermission(
                authentication(), projectDeploymentDTO, "WORKFLOW_EDIT")).isFalse();
            verify(permissionService, never())
                .hasWorkspaceScopeForProject(anyLong(), anyString(), eq(Environment.DEVELOPMENT));
        } finally {
            EnvironmentContext.clear();
        }
    }
}
```

Write `projectDeployment(long projectId, Environment environment)` and `authentication()` as private camelCase helpers in the test class — Checkstyle's no-underscore rule applies to helpers too. If `EnvironmentContext` exposes different setter/clear names, adjust the third test to match; its point is that a *different* ambient environment does not change the outcome.

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*PromotionTargetEnvironmentTest*' --console=plain > /tmp/t7.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t7.txt
```

Expected: FAILED — the evaluator does not recognise a `ProjectDeploymentDTO`.

- [ ] **Step 3: Teach the evaluator to read the DTO**

In `AutomationPermissionEvaluator.hasPermission(Authentication, Object targetDomainObject, Object permission)`, add a branch before the existing handling:

```java
if (targetDomainObject instanceof ProjectDeploymentDTO projectDeploymentDTO) {
    return permissionService.hasWorkspaceScopeForProject(
        projectDeploymentDTO.projectId(), (String) permission, projectDeploymentDTO.environment());
}
```

Spring's `PermissionEvaluator` types `permission` as `Object`, so a compound string such as `'WORKFLOW_EDIT@PRODUCTION'` would also compile. It is rejected deliberately: it is stringly-typed and would need parsing in the one place that must not be wrong.

- [ ] **Step 4: Change the guard to pass the DTO**

```java
@Override
@PreAuthorize("hasPermission(#projectDeploymentDTO, 'ProjectDeployment', 'WORKFLOW_EDIT')")
public long createProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO) {
```

Leave the other two `createProjectDeployment` overloads unguarded. They are internal callers, and that matches the existing convention: the API facade owns authorization, while the shared facade is deliberately unguarded because runtime agent tools call it with no security context.

- [ ] **Step 5: Run the test and watch it pass**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*PromotionTargetEnvironmentTest*' --console=plain > /tmp/t7.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t7.txt
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run the whole affected surface**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --continue --console=plain > /tmp/t7all.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t7all.txt
```

Expected: BUILD SUCCESSFUL. `--continue` matters: without it one failing module hides the rest.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/libs/automation/automation-configuration
git commit -m "732 Check the target environment's role when creating a project deployment"
```

---

## Task 8: Expose environment roles over GraphQL

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/workspace-user.graphqls`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/WorkspaceUserGraphQlController.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/ee/automation/configuration/web/graphql/WorkspaceUserEnvironmentGraphQlTest.java`

**Interfaces:**
- Consumes: `setEnvironmentRole(userId, workspaceId, environment, workspaceRole)` and `removeEnvironmentRole(userId, workspaceId, environment)` from Task 4.
- Produces: the `environment` field on the `WorkspaceUser` GraphQL type, and two mutations the client task consumes.

`WorkspaceUserView` is a record nested inside the controller with two factories: `stored(WorkspaceUser)` and `inherited(workspaceId, userId)`. **The inherited factory synthesizes tenant admins, who have no row** — their `environment` is always null, and a tenant admin is not subject to per-environment roles at all because `hasWorkspaceScope` short-circuits on `isTenantAdmin()`. Do not try to give an inherited entry an environment.

- [ ] **Step 1: Write the failing controller test**

```java
@ExtendWith(MockitoExtension.class)
class WorkspaceUserEnvironmentGraphQlTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 2L;

    @Mock
    private WorkspaceUserService workspaceUserService;

    @InjectMocks
    private WorkspaceUserGraphQlController workspaceUserGraphQlController;

    @Test
    void testSetEnvironmentRoleDelegatesToTheService() {
        when(workspaceUserService.setEnvironmentRole(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER))
                .thenReturn(WorkspaceUser.forRole(
                    USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        WorkspaceUserView view = workspaceUserGraphQlController.setWorkspaceUserEnvironmentRole(
            WORKSPACE_ID, USER_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER);

        assertThat(view.environment()).isEqualTo("PRODUCTION");
        assertThat(view.workspaceRole()).isEqualTo("VIEWER");
    }

    @Test
    void testRemoveEnvironmentRoleDelegatesToTheService() {
        boolean removed = workspaceUserGraphQlController.removeWorkspaceUserEnvironmentRole(
            WORKSPACE_ID, USER_ID, Environment.DEVELOPMENT);

        assertThat(removed).isTrue();
        verify(workspaceUserService).removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.DEVELOPMENT);
    }

    @Test
    void testAnInheritedEntryHasNoEnvironment() {
        WorkspaceUserView view = WorkspaceUserView.inherited(WORKSPACE_ID, USER_ID);

        assertThat(view.environment()).isNull();
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:test --tests '*WorkspaceUserEnvironmentGraphQlTest*' --console=plain > /tmp/t8.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t8.txt
```

Expected: FAILED — the mutations and the `environment` component do not exist.

- [ ] **Step 3: Extend the schema**

Add the enum, the field, and the two mutations to `workspace-user.graphqls`:

```graphql
enum Environment {
    DEVELOPMENT
    STAGING
    PRODUCTION
}
```

Add to `type WorkspaceUser`:

```graphql
    "The environment this role applies to, or null when it applies to every environment"
    environment: Environment
```

Add to `extend type Mutation`:

```graphql
    """
    Give a member a role in one environment. The first such call switches the member from a
    single workspace-wide role to per-environment roles, deleting their workspace-wide row.
    Environments the member has no row for are then denied. Requires the WORKSPACE_MEMBER_MANAGE scope.
    """
    setWorkspaceUserEnvironmentRole(
        workspaceId: ID!
        userId: ID!
        environment: Environment!
        role: WorkspaceRole!
    ): WorkspaceUser!
    """
    Remove a member's role in one environment, denying them there. Removing their last
    environment role removes them from the workspace entirely — it does not restore a
    workspace-wide role. Requires the WORKSPACE_MEMBER_MANAGE scope.
    """
    removeWorkspaceUserEnvironmentRole(workspaceId: ID!, userId: ID!, environment: Environment!): Boolean!
```

The second description states the consequence explicitly because it is the one an admin can get wrong: this is a revoke, not a demote.

- [ ] **Step 4: Add the field to `WorkspaceUserView` and wire the mutations**

Add `String environment` as the **last** component of the record, so existing positional constructions stay readable, and set it in both factories:

```java
public record WorkspaceUserView(
    Long id, long workspaceId, long userId, String workspaceRole, Long customRoleId, boolean inherited,
    String createdDate, String environment) {

    static WorkspaceUserView stored(WorkspaceUser workspaceUser) {
        Integer roleOrdinal = workspaceUser.getWorkspaceRole();
        Environment environment = workspaceUser.getEnvironment();

        return new WorkspaceUserView(
            workspaceUser.getId(), workspaceUser.getWorkspaceId(), workspaceUser.getUserId(),
            roleOrdinal == null ? null : WorkspaceRole.values()[roleOrdinal].name(),
            workspaceUser.getCustomRoleId(), false,
            workspaceUser.getCreatedDate() == null ? null : String.valueOf(workspaceUser.getCreatedDate()),
            environment == null ? null : environment.name());
    }

    static WorkspaceUserView inherited(long workspaceId, long userId) {
        return new WorkspaceUserView(
            null, workspaceId, userId, WorkspaceRole.ADMIN.name(), null, true, null, null);
    }
}
```

Then the two mutations, matching the `@PreAuthorize` style already on the neighbouring mutations in this controller — copy it verbatim from `addWorkspaceUser` rather than inventing one:

```java
@MutationMapping
public WorkspaceUserView setWorkspaceUserEnvironmentRole(
    @Argument long workspaceId, @Argument long userId, @Argument Environment environment,
    @Argument WorkspaceRole role) {

    return WorkspaceUserView.stored(
        workspaceUserService.setEnvironmentRole(userId, workspaceId, environment, role));
}

@MutationMapping
public boolean removeWorkspaceUserEnvironmentRole(
    @Argument long workspaceId, @Argument long userId, @Argument Environment environment) {

    workspaceUserService.removeEnvironmentRole(userId, workspaceId, environment);

    return true;
}
```

- [ ] **Step 5: Run the test and watch it pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:test --console=plain > /tmp/t8.txt 2>&1
echo "exit: $?"
grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" /tmp/t8.txt
```

Expected: BUILD SUCCESSFUL, including any pre-existing controller tests unedited.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.txt 2>&1
echo "exit: $?"
git add server/ee/libs/automation/automation-configuration/automation-configuration-graphql
git commit -m "732 Expose per-environment workspace roles over GraphQL"
```

---

## Task 9: Per-environment roles in the members UI

**Files:**
- Modify: `client/src/graphql/automation/configuration/workspaceUsers.graphql`
- Create: `client/src/graphql/automation/configuration/setWorkspaceUserEnvironmentRole.graphql`
- Create: `client/src/graphql/automation/configuration/removeWorkspaceUserEnvironmentRole.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`
- Modify: `client/src/ee/pages/settings/automation/workspaces/components/WorkspaceUsersDialog.tsx`
- Test: `client/src/ee/pages/settings/automation/users/tests/WorkspaceUsers.test.tsx`

**Interfaces:**
- Consumes: the `environment` field and the two mutations from Task 8.
- Produces: nothing later tasks depend on. This is the last task.

Client conventions that Checkstyle's equivalents enforce here, all from `CLAUDE.md`:
- **Object keys must be in alphabetical order** (ESLint `sort-keys`). `--fix` does **not** repair this; fix it by hand.
- **Named imports sorted alphabetically** within `{}`, with `type` imports sorted by name rather than grouped.
- **Interface names end in `I` or `Props`** — `EnvironmentRoleRowProps`, not `EnvironmentRoleRow`.
- **Lucide icons import with the `Icon` suffix** — `TrashIcon`, not `Trash`.
- **Use `twMerge` from `tailwind-merge`**, never a `cn()` helper.
- **Hook order:** `useState` → `useRef` → store hooks → other custom hooks → `useMemo`/`useCallback` → `useEffect` last, immediately before `return`.

- [ ] **Step 1: Write the GraphQL operations**

`setWorkspaceUserEnvironmentRole.graphql`:

```graphql
mutation SetWorkspaceUserEnvironmentRole(
    $workspaceId: ID!
    $userId: ID!
    $environment: Environment!
    $role: WorkspaceRole!
) {
    setWorkspaceUserEnvironmentRole(
        workspaceId: $workspaceId
        userId: $userId
        environment: $environment
        role: $role
    ) {
        id
        userId
        workspaceRole
        environment
    }
}
```

`removeWorkspaceUserEnvironmentRole.graphql`:

```graphql
mutation RemoveWorkspaceUserEnvironmentRole($workspaceId: ID!, $userId: ID!, $environment: Environment!) {
    removeWorkspaceUserEnvironmentRole(workspaceId: $workspaceId, userId: $userId, environment: $environment)
}
```

Add `environment` to the selection set in the existing `workspaceUsers.graphql`, so the dialog can tell which mode a member is in.

- [ ] **Step 2: Regenerate the typed client**

```bash
cd client
npx graphql-codegen
```

Commit the operations and the regenerated `graphql.ts` **separately**, per the repo's GraphQL workflow.

- [ ] **Step 3: Write the failing test**

`vi.mock(...)` hoists above module-scope `const`s, so mock refs must be declared with `vi.hoisted` or the test crashes with `Cannot access X before initialization`:

```ts
const {removeEnvironmentRoleMock, setEnvironmentRoleMock} = vi.hoisted(() => ({
    removeEnvironmentRoleMock: vi.fn(),
    setEnvironmentRoleMock: vi.fn(),
}));

it('shows one role row per environment once a member has environment roles', async () => {
    renderDialog({
        workspaceUsers: [
            {environment: 'DEVELOPMENT', id: '1', userId: '7', workspaceRole: 'EDITOR'},
            {environment: 'PRODUCTION', id: '2', userId: '7', workspaceRole: 'VIEWER'},
        ],
    });

    expect(await screen.findByText('Development')).toBeInTheDocument();
    expect(screen.getByText('Production')).toBeInTheDocument();
});

it('warns that removing the last environment role removes the member', async () => {
    renderDialog({
        workspaceUsers: [{environment: 'DEVELOPMENT', id: '1', userId: '7', workspaceRole: 'EDITOR'}],
    });

    await userEvent.click(await screen.findByRole('button', {name: /remove development/i}));

    expect(screen.getByText(/removes them from the workspace/i)).toBeInTheDocument();
});
```

Object keys in those fixtures are alphabetical because `sort-keys` requires it.

The second test is the one that matters. Removing a member's last environment removes them from the workspace, and an admin clicking a per-row delete will not expect that — the dialog must say so before it happens.

- [ ] **Step 4: Run it and watch it fail**

```bash
cd client
npm run test -- WorkspaceUsers > /tmp/t9.txt 2>&1
echo "exit: $?"
tail -20 /tmp/t9.txt
```

Expected: failing assertions — the environment rows do not render.

- [ ] **Step 5: Build the UI**

In `WorkspaceUsersDialog.tsx`, derive each member's mode from the rows returned by `workspaceUsers`:

- **Every row has `environment === null`** → implicit mode. Render exactly what is rendered today: one role select. Add a control that switches the member to per-environment roles, which calls `setWorkspaceUserEnvironmentRole` for the environment the admin picks.
- **Any row has an `environment`** → explicit mode. Render one row per environment that has a role, each with its own role select and a remove control, plus a control to add a role for an environment that has none.

Use `useMemo` for the grouping rather than an IIFE in JSX.

The remove control opens a confirmation. When it is the member's **last** environment row, the confirmation says the member will be removed from the workspace; otherwise it says they will lose access to that environment. That difference is the whole point of the control.

- [ ] **Step 6: Run the test and watch it pass**

```bash
cd client
npm run test -- WorkspaceUsers > /tmp/t9.txt 2>&1
echo "exit: $?"
tail -20 /tmp/t9.txt
```

Expected: passing.

- [ ] **Step 7: Run the full client check**

```bash
cd client
npm run check > /tmp/t9check.txt 2>&1
echo "exit: $?"
tail -30 /tmp/t9check.txt
```

Expected: exit 0. This runs lint, typecheck and tests together, and `sort-keys` violations surface here rather than in the test run.

- [ ] **Step 8: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add client/src/graphql client/src/shared/middleware/graphql.ts
git commit -m "732 client - Add the per-environment workspace role GraphQL operations"
git add client/src/ee/pages/settings/automation/workspaces client/src/ee/pages/settings/automation/users
git commit -m "732 client - Manage per-environment workspace roles in the members dialog"
```

---

## Not in this plan

**Other call sites.** Only the promotion guard moves to an environment-aware check. Every other `@PreAuthorize` keeps its 2-argument form and its current behaviour. Widening them is a follow-up, and each one needs its own decision about which environment it is acting on — a question with a different answer for a workflow edit than for a connection read.

**Custom roles per environment.** `custom_role_id` rides in the same row and so is already per-environment once Task 1 lands. Task 9 renders built-in roles only; exposing custom roles per environment in the dialog is a follow-up on a settled model.
