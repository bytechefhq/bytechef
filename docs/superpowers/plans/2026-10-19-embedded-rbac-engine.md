# Embedded RBAC, Plan A: engine and membership

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the RBAC engine an embedded side: an embedded scope catalogue, embedded membership (one role or one per environment), scope resolution with a cache, SpEL built-ins, resolver chains that let `hasPermission` answer for embedded resources, and the membership facade with its GraphQL API and audit events.

**Architecture:** Everything lives beside the automation RBAC pieces in EE `automation-configuration` (the RBAC module): `EmbeddedUser` next to `WorkspaceUser`, `EmbeddedScopeCacheService` next to `WorkspaceScopeCacheService`, embedded scope providers next to the automation ones, and new methods on `PermissionService`. `PermissionScopeRegistry` learns which side a scope belongs to. `ResourceOwner` learns to say "embedded", resolvers chain per resource type, and a small SPI (`EmbeddedResourceResolver`) tells the engine which workflows and projects are embedded. No facade gate moves in this plan; that is Plan B. The client is Plan C.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Spring Security method security (SpEL), Liquibase, GraphQL (Spring for GraphQL), JUnit 5 + Mockito + Testcontainers (PostgreSQL).

**Spec:** `docs/superpowers/specs/2026-10-19-embedded-rbac-design.md`

## Global Constraints

- EE only. Every new main-source file under `server/ee/` carries the ByteChef Enterprise license header and `@version ee` in its class Javadoc; copy both from `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/WorkspaceUserServiceImpl.java`.
- No explanatory inline comments in code. The why goes in the commit message.
- Commit messages: `1051 <desc>` for server changes, `1051 docs - <desc>` for docs. No `Co-Authored-By` trailer.
- Verification runs the module's `check` task, never `test` alone, prefixed with `DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock`. A run is green only when `grep -E "^> Task .* FAILED" <log>` prints nothing and the log ends in `BUILD SUCCESSFUL`.
- `./gradlew spotlessApply` before every commit.
- Built-in roles reuse `WorkspaceRole` (`ADMIN(0) > EDITOR(1) > VIEWER(2)`, stored as the ordinal). Shared scope names (`WORKFLOW_*`, `EXECUTION_*`, `CONNECTION_*`, `MCP_*`, `API_KEY_*`) keep the automation minimum roles exactly.
- Tenant admins (`ROLE_ADMIN`) hold every embedded scope in every environment and are never subject to per-environment rows. A connected user (governed principal) never holds an embedded scope.
- Embedded membership is independent of workspace membership: no code path reads `workspace_user` to answer an embedded question or `embedded_user` to answer a workspace question.
- No last-Admin rule for embedded: the last embedded Admin may be removed or demoted.
- Spec decision 4 is not in this plan: integration workflow gates stay admin-only until Plan B. This plan only makes `hasWorkflowScope*` able to answer for them.

## Review Focus

1. A member in explicit per-environment mode with rows for Development only, asked about Production: denied, never a fallback to another environment's role. Pinned in Task 4.
2. A connected user (embedded API key principal) calling an embedded built-in: `false`, even when a tenant user with the same numeric id is an embedded Admin. Pinned in Task 5.
3. An automation workspace Editor with no `embedded_user` row holds no embedded scope, and an embedded Admin with no `workspace_user` row holds no workspace scope. Pinned in Task 11.
4. A job whose principal is neither an automation deployment nor an embedded instance: every resolver in the chain answers unknown and `hasResourceScope` denies without throwing. Pinned in Task 6.
5. An embedded Admin removing their own last environment role: it widens to an embedded-wide role; an embedded Admin removing themselves entirely succeeds (no last-Admin rule). Pinned in Task 8.

## File structure

Prefixes used below:
- `EEAPI` = `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration`
- `EESVC` = `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration`
- `EETEST` = `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration`
- `EEGQL` = `server/ee/libs/automation/automation-configuration/automation-configuration-graphql`
- `CEAPI` = `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration`
- `CESVC` = `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration`
- `CETEST` = `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration`
- `EMB` = `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration`
- Gradle: `EECFG` = `:server:ee:libs:automation:automation-configuration:automation-configuration-service`, `CECFG` = `:server:libs:automation:automation-configuration:automation-configuration-service`, `EMBCFG` = `:server:ee:libs:embedded:embedded-configuration:embedded-configuration-service`

| File | Responsibility |
|---|---|
| `EEAPI/security/constant/PermissionScopeSide.java` (new) | `enum PermissionScopeSide { AUTOMATION, EMBEDDED }` |
| `EEAPI/security/PermissionScopeProvider.java` | gains `default PermissionScopeSide side()` |
| `EESVC/service/PermissionScopeRegistry.java` | per-side scope sets |
| `EESVC/security/scope/embedded/*` (new) | embedded scope enums and providers |
| `EEAPI/domain/EmbeddedUser.java` (new) | membership row |
| `EESVC/repository/EmbeddedUserRepository.java` (new) | membership queries |
| `.../changelog/automation/configuration/20261020100000_automation_configuration_added_table_embedded_user.xml` (new) | table |
| `EESVC/service/EmbeddedScopeCacheService.java` (new) | cached role → scopes per user and environment |
| `CEAPI/service/PermissionService.java` + 3 impls | embedded methods |
| `CESVC/security/AutomationMethodSecurityExpressionRoot.java` | embedded built-ins |
| `CEAPI/security/ResourceOwnershipResolver.java` | `ResourceOwner.embedded` |
| `CEAPI/security/EmbeddedResourceResolver.java` (new) | SPI: is this workflow / project embedded |
| `EMB/security/EmbeddedResourceResolverImpl.java` (new) | SPI implementation |
| `EEAPI/service/EmbeddedUserService.java`, `EESVC/service/EmbeddedUserServiceImpl.java` (new) | membership rules |
| `EEAPI/facade/EmbeddedUserFacade.java`, `EESVC/facade/EmbeddedUserFacadeImpl.java` (new) | gates and audit |
| `EESVC/audit/EmbeddedUserAuditEvents.java`, `EESVC/audit/EmbeddedUserAuditMapper.java` (new) | audit |
| `EEGQL/src/main/resources/graphql/embedded-user.graphqls`, `EEGQL/src/main/java/.../web/graphql/EmbeddedUserGraphQlController.java` (new) | API |

---

### Task 1: Scope sides in the registry

**Files:**
- Create: `EEAPI/security/constant/PermissionScopeSide.java`
- Modify: `EEAPI/security/PermissionScopeProvider.java`
- Modify: `EESVC/service/PermissionScopeRegistry.java`
- Test: `EETEST/service/PermissionScopeRegistryTest.java`

**Interfaces:**
- Produces: `enum PermissionScopeSide { AUTOMATION, EMBEDDED }`; `default PermissionScopeSide side() { return PermissionScopeSide.AUTOMATION; }` on `PermissionScopeProvider`; on the registry `Set<String> getAllScopeNames(PermissionScopeSide side)` and `Set<String> getScopeNames(WorkspaceRole role, PermissionScopeSide side)`. The existing `getAllScopeNames()`, `getScopeNames(WorkspaceRole)`, `getBuiltInRoles()` and `getScopeGroups()` keep answering for `AUTOMATION` only.

- [ ] **Step 1: Write the failing tests** in `PermissionScopeRegistryTest` (follow the existing fixture style that builds a registry from provider instances):

```java
@Test
void testAScopeDeclaredOnBothSidesWithTheSameMinimumRoleBelongsToBoth()
// providers: automation {WORKFLOW_VIEW -> VIEWER}, embedded {WORKFLOW_VIEW -> VIEWER}
// assert getAllScopeNames(AUTOMATION) contains WORKFLOW_VIEW, getAllScopeNames(EMBEDDED) contains WORKFLOW_VIEW

@Test
void testAnEmbeddedOnlyScopeIsAbsentFromTheAutomationSide()
// embedded {INTEGRATION_CREATE -> EDITOR}
// assert getAllScopeNames() (automation) does not contain it; getScopeNames(VIEWER, EMBEDDED) does not; getScopeNames(EDITOR, EMBEDDED) does

@Test
void testConflictingMinimumRolesAcrossSidesStillFailStartup()
// automation {MCP_DELETE -> ADMIN}, embedded {MCP_DELETE -> EDITOR} -> IllegalStateException with the existing message
```

Use an anonymous `PermissionScopeProvider` overriding `side()` for the embedded ones; scope types can be the existing enums (`WorkflowPermissionScope`, `McpPermissionScope`) plus a test-local `enum TestScope implements PermissionScopeType { INTEGRATION_CREATE }`.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew EECFG:test --tests '*PermissionScopeRegistryTest' -q 2>&1 | tail -20`
Expected: compilation failure on `side()` / `getAllScopeNames(PermissionScopeSide)`.

- [ ] **Step 3: Implement**

`PermissionScopeSide` enum in `EEAPI/security/constant/`. In `PermissionScopeProvider`, add `default PermissionScopeSide side() { return PermissionScopeSide.AUTOMATION; }`. In `PermissionScopeRegistry`, keep `minimumRoleByScope` as the single conflict detector; add `Map<PermissionScopeSide, Set<String>> scopeNamesBySide` (insertion-ordered `LinkedHashSet`, one per side, both sides always present) filled while iterating providers; implement the two new methods; make the existing automation-only methods delegate with `AUTOMATION`.

- [ ] **Step 4: Run the module tests**

Run: `./gradlew EECFG:test --tests '*PermissionScopeRegistryTest' -q 2>&1 | tail -20`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/automation/automation-configuration
git commit -m "1051 Record which side a permission scope belongs to so embedded and automation catalogues can share names"
```

---

### Task 2: Embedded scope catalogue

**Files:**
- Create under `EESVC/security/scope/embedded/`: `IntegrationPermissionScope.java`, `IntegrationPermissionScopeProvider.java`, `IntegrationInstanceConfigurationPermissionScope.java`, `IntegrationInstanceConfigurationPermissionScopeProvider.java`, `ConnectedUserPermissionScope.java`, `ConnectedUserPermissionScopeProvider.java`, `AppEventPermissionScope.java`, `AppEventPermissionScopeProvider.java`, `EmbeddedPermissionScope.java`, `EmbeddedPermissionScopeProvider.java`, `EmbeddedSharedPermissionScopeProvider.java`
- Modify: `CETEST/security/PermissionScopeTypeTest.java` (`KNOWN_UNENFORCED_SCOPES`)
- Test: `EETEST/service/PermissionScopeRegistryTest.java`

**Interfaces:**
- Produces the catalogue. Every provider is `@Component @ConditionalOnEEVersion` and overrides `side()` to `EMBEDDED`.

| Enum | Constants → minimum role |
|---|---|
| `IntegrationPermissionScope` | `INTEGRATION_CREATE` EDITOR, `INTEGRATION_PUBLISH` EDITOR, `INTEGRATION_SETTINGS` ADMIN, `INTEGRATION_DELETE` ADMIN |
| `IntegrationInstanceConfigurationPermissionScope` | `INTEGRATION_INSTANCE_CONFIGURATION_VIEW` VIEWER, `_CREATE` EDITOR, `_EDIT` EDITOR, `_DELETE` ADMIN |
| `ConnectedUserPermissionScope` | `CONNECTED_USER_VIEW` VIEWER, `CONNECTED_USER_MANAGE` EDITOR, `CONNECTED_USER_DELETE` ADMIN |
| `AppEventPermissionScope` | `APP_EVENT_VIEW` VIEWER, `APP_EVENT_CREATE` EDITOR, `APP_EVENT_EDIT` EDITOR, `APP_EVENT_DELETE` ADMIN |
| `EmbeddedPermissionScope` | `EMBEDDED_VIEW` VIEWER, `EMBEDDED_MEMBER_MANAGE` ADMIN, `EMBEDDED_SETTINGS_MANAGE` ADMIN |

`EmbeddedSharedPermissionScopeProvider` returns, with side `EMBEDDED`, the same `ScopeDefinition`s that `WorkflowPermissionScopeProvider`, `ExecutionPermissionScopeProvider`, `ConnectionPermissionScopeProvider`, `McpPermissionScopeProvider` and `ApiKeyPermissionScopeProvider` return (same enum constants, same minimum roles).

- [ ] **Step 1: Write the failing test** in `PermissionScopeRegistryTest`:

```java
@Test
void testTheEmbeddedCatalogueHoldsEveryEmbeddedScopeWithItsMinimumRole()
// registry built from every provider class above plus the five automation providers it shares
// assert getScopeNames(VIEWER, EMBEDDED) equals exactly:
//   WORKFLOW_VIEW, EXECUTION_VIEW, CONNECTION_VIEW, MCP_VIEW,
//   INTEGRATION_INSTANCE_CONFIGURATION_VIEW, CONNECTED_USER_VIEW, APP_EVENT_VIEW, EMBEDDED_VIEW
// assert getScopeNames(ADMIN, EMBEDDED) contains INTEGRATION_SETTINGS, INTEGRATION_DELETE, EMBEDDED_MEMBER_MANAGE, EMBEDDED_SETTINGS_MANAGE
// assert getScopeNames(EDITOR, EMBEDDED) contains API_KEY_VIEW, API_KEY_CREATE, API_KEY_DELETE, EXECUTION_DETAILS_VIEW and not INTEGRATION_DELETE
// assert getAllScopeNames() (automation) contains none of INTEGRATION_CREATE, CONNECTED_USER_VIEW, APP_EVENT_VIEW, EMBEDDED_VIEW
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew EECFG:test --tests '*PermissionScopeRegistryTest' -q 2>&1 | tail -20`
Expected: compilation failure (classes missing).

- [ ] **Step 3: Implement the enums and providers** as listed. The shared provider builds its set by calling `new WorkflowPermissionScopeProvider().scopeDefinitions()` and so on and unioning them, so the minimum roles cannot drift.

- [ ] **Step 4: Pin the not-yet-enforced scopes** in `PermissionScopeTypeTest.KNOWN_UNENFORCED_SCOPES`: `INTEGRATION_CREATE`, `INTEGRATION_PUBLISH`, `INTEGRATION_SETTINGS`, `INTEGRATION_DELETE`, `INTEGRATION_INSTANCE_CONFIGURATION_VIEW`, `INTEGRATION_INSTANCE_CONFIGURATION_CREATE`, `INTEGRATION_INSTANCE_CONFIGURATION_EDIT`, `INTEGRATION_INSTANCE_CONFIGURATION_DELETE`, `CONNECTED_USER_VIEW`, `CONNECTED_USER_MANAGE`, `CONNECTED_USER_DELETE`, `APP_EVENT_VIEW`, `APP_EVENT_CREATE`, `APP_EVENT_EDIT`, `APP_EVENT_DELETE`, `EMBEDDED_SETTINGS_MANAGE`. Plan B removes each pin as it gates the scope. `EMBEDDED_VIEW` and `EMBEDDED_MEMBER_MANAGE` are not pinned: Task 9 gates them.

- [ ] **Step 5: Run both modules' tests**

Run: `./gradlew EECFG:test --tests '*PermissionScopeRegistryTest' CECFG:test --tests '*PermissionScopeTypeTest' -q 2>&1 | tail -20`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/automation server/libs/automation/automation-configuration
git commit -m "1051 Add the embedded permission scope catalogue"
```

---

### Task 3: `embedded_user` table, domain and repository

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20261020100000_automation_configuration_added_table_embedded_user.xml`
- Create: `EEAPI/domain/EmbeddedUser.java`
- Create: `EESVC/repository/EmbeddedUserRepository.java`
- Test: `EETEST/service/EmbeddedUserRepositoryIntTest.java`

**Interfaces:**
- Produces:

```java
@Table("embedded_user")
public final class EmbeddedUser {
    // fields: createdBy, createdDate, customRoleId (Long), environment (Integer), @Id id (Long),
    //         lastModifiedBy, lastModifiedDate, userId (Long), @Version version (int), embeddedRole (Integer)
    public static EmbeddedUser forRole(long userId, WorkspaceRole embeddedRole);
    public static EmbeddedUser forRole(long userId, WorkspaceRole embeddedRole, Environment environment);
    public void assignRole(WorkspaceRole embeddedRole);
    public @Nullable Environment getEnvironment();
    public @Nullable WorkspaceRole getEmbeddedRole();
    public @Nullable Long getCustomRoleId();
    // plus getters for id, userId, createdBy, createdDate, lastModifiedBy, lastModifiedDate, version
}

@Repository @ConditionalOnEEVersion
public interface EmbeddedUserRepository extends ListCrudRepository<EmbeddedUser, Long> {
    long countByCustomRoleId(long customRoleId);
    void deleteByUserId(long userId);
    boolean existsByUserId(long userId);
    boolean existsByUserIdAndEnvironmentIsNotNull(long userId);
    List<EmbeddedUser> findAllByUserId(long userId);
    Optional<EmbeddedUser> findByUserIdAndEnvironmentIsNull(long userId);
    Optional<EmbeddedUser> findByUserIdAndEnvironment(long userId, int environment);
}
```

- [ ] **Step 1: Write the failing integration test** `EmbeddedUserRepositoryIntTest` (`@SpringBootTest(classes = EeAutomationConfigurationIntTestConfiguration.class) @ActiveProfiles("testint") @Import(PostgreSQLContainerConfiguration.class)`), seeding users through `JdbcTemplate` as `PermissionServiceIntTest` does:

```java
@Test void testAnImplicitRowAndAnExplicitRowCoexistAndAreFoundSeparately()
// save forRole(u, EDITOR) and forRole(u, VIEWER, PRODUCTION)
// findByUserIdAndEnvironmentIsNull(u) -> EDITOR; findByUserIdAndEnvironment(u, 2) -> VIEWER; findAllByUserId(u).size() == 2
@Test void testASecondImplicitRowForTheSameUserIsRejected()   // DuplicateKeyException (or DbActionExecutionException wrapping it)
@Test void testASecondRowForTheSameUserAndEnvironmentIsRejected()
@Test void testDeleteByUserIdRemovesEveryRow()
```

- [ ] **Step 2: Run to verify it fails**

Run: `DOCKER_HOST=... ./gradlew EECFG:testIntegration --tests '*EmbeddedUserRepositoryIntTest' -q 2>&1 | tail -20`
Expected: compilation failure.

- [ ] **Step 3: Write the changelog.** Model it on `20240604153040_automation_configuration_added_table_workspace_user.xml` plus the two EE alterations (`202604061200010_*_added_column_workspace_user_role.xml`, `20260819120000_*_added_column_workspace_user_environment.xml`): one `createTable embedded_user` with `id` (bigint autoIncrement pk), `user_id` bigint not null, `embedded_role` int nullable, `custom_role_id` bigint nullable, `environment` int nullable, `created_by` varchar(255), `created_date` timestamp, `last_modified_by`, `last_modified_date`, `version` int; the foreign key to `user` in its own changeSet with `contextFilter="mono"` (as `workspace_user` does); a foreign key to `custom_role`; uniqueness exactly as the workspace_user environment changelog does it (a partial unique index on `(user_id) WHERE environment IS NULL` for PostgreSQL, the H2 generated-column variant with `-h2` ids, and a unique constraint on `(user_id, environment)`). Author `Ivica Cardic`.

- [ ] **Step 4: Implement `EmbeddedUser` and `EmbeddedUserRepository`.** Mirror `WorkspaceUser`'s constructor validation (exactly one of role or custom role; ordinal range). The repository package is already covered by `EeAutomationConfigurationJdbcRepositoryConfiguration`; verify with `grep -n basePackages EESVC/config/*.java`.

- [ ] **Step 5: Run the integration test**

Run: `DOCKER_HOST=... ./gradlew EECFG:testIntegration --tests '*EmbeddedUserRepositoryIntTest' -q 2>&1 | tail -20`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/automation
git commit -m "1051 Add the embedded_user membership table"
```

---

### Task 4: Embedded scope cache

**Files:**
- Create: `EESVC/service/EmbeddedScopeCacheService.java`
- Test: `EETEST/service/EmbeddedScopeCacheServiceTest.java`, `EETEST/service/EmbeddedScopeCacheKeyConsistencyTest.java`

**Interfaces:**
- Consumes: `EmbeddedUserRepository` (Task 3), `PermissionScopeRegistry.getScopeNames(WorkspaceRole, PermissionScopeSide)` (Task 1), `CustomRoleScopeResolver` (nullable, as in `WorkspaceScopeCacheService`).
- Produces:

```java
@Service @Transactional(readOnly = true)
public class EmbeddedScopeCacheService {
    public EmbeddedScopeCacheService(CacheManager cacheManager, @Nullable CustomRoleScopeResolver customRoleScopeResolver, PermissionScopeRegistry permissionScopeRegistry, EmbeddedUserRepository embeddedUserRepository);
    public void evictAllEmbeddedScopeCache();
    public void evictEmbeddedScopeCache(long userId);
    @Cacheable(value = "embeddedScopes") public Set<String> getEmbeddedScopes(long userId);
    @Cacheable(value = "embeddedScopes") public Set<String> getEmbeddedScopes(long userId, Environment environment);
}
```

Semantics copied from `WorkspaceScopeCacheService` with the workspace dimension removed: the environment-unaware answer is the implicit row's scopes, or the union of the explicit rows; the environment-aware answer is that environment's row, empty when the member is in explicit mode and has no row there, else the implicit row. A custom-role row resolves through `customRoleScopeResolver.resolveScopes(id)` intersected with `getAllScopeNames(EMBEDDED)`. Evictions after commit when a transaction is active.

- [ ] **Step 1: Write the failing unit test** `EmbeddedScopeCacheServiceTest` (Mockito, `ConcurrentMapCacheManager`):

```java
@Test void testImplicitRowAnswersEveryEnvironment()        // forRole(u, EDITOR): getEmbeddedScopes(u, PRODUCTION) contains WORKFLOW_EDIT
@Test void testExplicitModeDeniesAnEnvironmentWithoutARow() // rows: DEVELOPMENT EDITOR only -> getEmbeddedScopes(u, PRODUCTION) is empty
@Test void testExplicitModeUnionsForTheUnawareAnswer()      // DEVELOPMENT EDITOR + PRODUCTION VIEWER -> getEmbeddedScopes(u) contains WORKFLOW_EDIT and EMBEDDED_VIEW
@Test void testNoRowsMeansNoScopes()
@Test void testEvictionClearsEveryEnvironmentKey()          // warm all four keys, evict, repository is queried again
```

and `EmbeddedScopeCacheKeyConsistencyTest` mirroring `WorkspaceScopeCacheKeyConsistencyTest` for keys `"{tenant}_{userId}"` and `"{tenant}_{userId}_{ENV}"`.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew EECFG:test --tests '*EmbeddedScopeCache*' -q 2>&1 | tail -20`
Expected: compilation failure.

- [ ] **Step 3: Implement** `EmbeddedScopeCacheService`. Confirm the cache name needs no registration: `grep -rn workspaceScopes server/libs/config server/apps/server-app/src/main/resources` prints nothing, so caches are created on demand.

- [ ] **Step 4: Run the tests**

Run: `./gradlew EECFG:test --tests '*EmbeddedScopeCache*' -q 2>&1 | tail -20`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/automation
git commit -m "1051 Cache a member's embedded scopes per environment"
```

---

### Task 5: `PermissionService` embedded methods and SpEL built-ins

**Files:**
- Modify: `CEAPI/service/PermissionService.java`
- Modify: `CESVC/service/PermissionServiceImpl.java` (CE)
- Modify: `EESVC/service/PermissionServiceImpl.java` (EE)
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/service/RemotePermissionServiceClient.java`
- Modify: `CESVC/security/AutomationMethodSecurityExpressionRoot.java`
- Modify: `CETEST/security/PermissionScopeTypeTest.java` (`KNOWN_GATE_FUNCTIONS`, `SCOPE_PATTERNS`)
- Test: `EETEST/service/PermissionServiceTest.java` (existing, add a nested class), `CETEST/security/AutomationMethodSecurityExpressionRootTest.java`

**Interfaces:**
- Consumes: `EmbeddedScopeCacheService` (Task 4), `PermissionScopeRegistry.getAllScopeNames(EMBEDDED)`.
- Produces on `PermissionService`:

```java
boolean hasEmbeddedScope(String scope);
boolean hasEmbeddedScope(String scope, Environment environment);
boolean hasEmbeddedScopeInEveryEnvironment(String scope);
boolean isEmbeddedMember();
Set<String> getMyEmbeddedScopes(@Nullable Environment environment);   // @PreAuthorize("isAuthenticated()") on the EE impl, like getMyWorkspaceScopes
void evictEmbeddedScopeCache(long userId);
void evictAllEmbeddedScopeCache();
```

and on `AutomationMethodSecurityExpressionRoot`:

```java
public boolean hasEmbeddedScope(String scope);
public boolean hasEmbeddedScopeInEnvironment(String scope, Environment environment);
public boolean hasEmbeddedScopeInEnvironmentId(String scope, @Nullable Long environmentId);   // null -> every environment
public boolean hasEmbeddedScopeInEveryEnvironment(String scope);
```

Decision order in the EE impl, same as `hasWorkspaceScope`: governed principal (connected user) → `false`; `isAutomationAuthorizationSkipped()` → `true`; `isTenantAdmin()` → `true`; no current user → `false`; otherwise `embeddedScopeCacheService.getEmbeddedScopes(userId[, environment]).contains(scope)`. `getMyEmbeddedScopes` returns `getAllScopeNames(EMBEDDED)` for a tenant admin, empty for a governed principal. `isEmbeddedMember` is `embeddedUserRepository.existsByUserId(userId)` (tenant admin → `true`). CE impl: `hasEmbeddedScope*` return `false`, `getMyEmbeddedScopes` returns `Set.of()`, `isEmbeddedMember` returns `false`, evictions no-op. Remote stub: throw `UnsupportedOperationException` like its siblings. The root built-ins follow the existing pattern (`isSkippedAndGranted` guard, delegate to the service).

- [ ] **Step 1: Write the failing tests.** In `PermissionServiceTest` add `@Nested class EmbeddedScopes` constructing `PermissionServiceImpl` with a mocked `EmbeddedScopeCacheService`:

```java
@Test void testTenantAdminHoldsEveryEmbeddedScope()
@Test void testMemberHoldsTheScopesTheCacheAnswers()           // cache answers {EMBEDDED_VIEW}: hasEmbeddedScope("EMBEDDED_VIEW") true, ("INTEGRATION_CREATE") false
@Test void testEnvironmentOverloadAsksTheCacheForThatEnvironment()
@Test void testEveryEnvironmentRequiresAllThree()               // DEVELOPMENT and STAGING true, PRODUCTION false -> false
@Test void testConnectedUserNeverHoldsAnEmbeddedScope()        // decider.decideWorkspace returns DENY (governed) -> false, cache never called
@Test void testAnonymousHoldsNothing()
```

In `AutomationMethodSecurityExpressionRootTest`: `testHasEmbeddedScopeInEnvironmentIdWithNullRequiresEveryEnvironment` and `testHasEmbeddedScopeDelegatesToThePermissionService`.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew EECFG:test --tests '*PermissionServiceTest' CECFG:test --tests '*AutomationMethodSecurityExpressionRootTest' -q 2>&1 | tail -20`
Expected: compilation failure.

- [ ] **Step 3: Implement** the interface, the three impls and the root. EE `PermissionServiceImpl` gains constructor parameters `EmbeddedScopeCacheService embeddedScopeCacheService, EmbeddedUserRepository embeddedUserRepository`; update every test that constructs it by hand (`grep -rln "new PermissionServiceImpl(" server/ee`).

- [ ] **Step 4: Teach the scan the new built-ins.** In `PermissionScopeTypeTest` add `hasEmbeddedScope`, `hasEmbeddedScopeInEnvironment`, `hasEmbeddedScopeInEnvironmentId`, `hasEmbeddedScopeInEveryEnvironment` to `KNOWN_GATE_FUNCTIONS` and one `SCOPE_PATTERNS` regex per function with the scope in the first argument (model on the `hasWorkspaceScopeInEveryEnvironment` entry, minus the leading id argument).

- [ ] **Step 5: Run the tests**

Run: `./gradlew EECFG:test --tests '*PermissionServiceTest' CECFG:test --tests '*AutomationMethodSecurityExpressionRootTest' --tests '*PermissionScopeTypeTest' -q 2>&1 | tail -20`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply -q
git add server/libs/automation/automation-configuration server/ee/libs/automation/automation-configuration
git commit -m "1051 Answer embedded scope questions from the permission service and the security expression root"
```

---

### Task 6: Embedded resource owners and resolver chains

**Files:**
- Modify: `CEAPI/security/ResourceOwnershipResolver.java` (`ResourceOwner`)
- Modify: `EESVC/service/PermissionServiceImpl.java` (resolver maps, `hasResourceScope`, `hasResourceScopeInEnvironment`, `isResourceOwner`)
- Modify: `CESVC/security/ConnectionOwnershipResolver.java`, `CESVC/security/ApiKeyOwnershipResolver.java`, `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-service/src/main/java/com/bytechef/automation/ai/mcp/security/McpServerOwnershipResolver.java`
- Verify only: `JobOwnershipResolver`, `TriggerExecutionOwnershipResolver` (automation-workflow-execution-service) return `unknown()` when no `AUTOMATION` principal exists
- Test: `EETEST/service/PermissionServiceResourceTest.java`, `CETEST/security/ResourceOwnershipResolverTest.java` (should still pass unchanged)

**Interfaces:**
- Produces:

```java
record ResourceOwner(OptionalLong workspaceId, OptionalLong ownerUserId, boolean embedded) {
    public ResourceOwner(OptionalLong workspaceId, OptionalLong ownerUserId) { this(workspaceId, ownerUserId, false); }
    public static ResourceOwner ofEmbedded();                  // (empty, empty, true)
    public static ResourceOwner ofEmbedded(long ownerUserId);  // (empty, of(ownerUserId), true)
    public boolean isUnknown();                                 // no workspace, no owner, not embedded
    // existing factories unchanged
}
```

`PermissionServiceImpl` keys resolvers as `Map<String, List<ResourceOwnershipResolver>>` and `Map<String, List<ResourceEnvironmentResolver>>` (`Collectors.groupingBy`). Ownership: the first resolver whose answer is not `isUnknown()` wins; all unknown → unknown. Environment: the first non-empty `Optional` wins; a resolver that throws still denies. In `hasResourceScope`, after the owner is resolved: if `owner.embedded()`, answer `hasEmbeddedScope(scope, env)` when an environment was resolved, else `hasEmbeddedScope(scope)`; the tenant-admin and connected-user short-circuits above it are unchanged. `hasResourceScopeInEnvironment`: `owner.embedded()` → `hasEmbeddedScope(scope, environment)`. `isResourceOwner` compares `ownerUserId` as today (embedded or not).

The three platform-typed automation resolvers (`Connection`, `ApiKey`, `McpServer`) return `ResourceOwner.unknown()` when the row's `PlatformType` is not `AUTOMATION`, so an embedded resolver registered later in Plan B can answer for `EMBEDDED` rows. Their environment resolvers stay as they are (the environment is the same fact on both sides).

- [ ] **Step 1: Write the failing tests** in `PermissionServiceResourceTest`:

```java
@Test void testTwoResolversForOneTypeChainUntilOneKnowsTheOwner()   // first answers unknown(), second ofEmbedded(); member with EMBEDDED_VIEW -> true
@Test void testAnEmbeddedOwnerWithAnEnvironmentIsCheckedInThatEnvironment()  // env resolver -> PRODUCTION; cache has scope only in DEVELOPMENT -> false
@Test void testAnEmbeddedOwnerWithoutAnEnvironmentUsesTheUnawareAnswer()
@Test void testEveryResolverUnknownDeniesWithoutThrowing()
@Test void testAnEnvironmentResolverThatThrowsDenies()                  // unchanged behaviour, now across a chain
@Test void testAutomationConnectionResolverIgnoresEmbeddedRows()         // ConnectionOwnershipResolver with a connection of type EMBEDDED -> unknown()
```

and `ApiKeyOwnershipResolverTest` / `McpServerOwnershipResolverTest` cases of the same shape (create them beside the resolvers if absent).

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew EECFG:test --tests '*PermissionServiceResourceTest' -q 2>&1 | tail -20`
Expected: compilation failure on `ofEmbedded`.

- [ ] **Step 3: Implement** the record change, the chains and the three resolver guards. Confirm `JobOwnershipResolver` and `TriggerExecutionOwnershipResolver` already return `unknown()` for a job without an `AUTOMATION` principal (`grep -n "fetchJobPrincipalId\|unknown" <file>`); if either throws instead, make it return `unknown()`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew EECFG:test --tests '*PermissionService*' CECFG:test --tests '*ResolverTest' -q 2>&1 | tail -20`
Expected: PASS, including the unchanged `ResourceOwnershipResolverTest`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply -q
git add server/libs/automation server/ee/libs/automation
git commit -m "1051 Let resource owners be embedded and chain ownership resolvers per resource type"
```

---

### Task 7: Embedded workflow and project SPI

**Files:**
- Create: `CEAPI/security/EmbeddedResourceResolver.java`
- Create: `EMB/security/EmbeddedResourceResolverImpl.java`
- Modify: `EESVC/service/PermissionServiceImpl.java` (`hasWorkflowScope` both overloads, `hasWorkflowScopeIfProjectWorkflow`, `hasWorkspaceScopeForProject` both overloads)
- Modify: `CESVC/security/ProjectOwnershipResolver.java`
- Test: `EETEST/service/PermissionServiceTest.java`, `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/security/EmbeddedResourceResolverIntTest.java`

**Interfaces:**
- Produces:

```java
public interface EmbeddedResourceResolver {
    boolean isEmbeddedProject(long projectId);
    boolean isEmbeddedWorkflow(String workflowId);
}
```

`EmbeddedResourceResolverImpl` (`@Component @ConditionalOnEEVersion`, constructor `(AutomationWorkflowProjectRepository, ConnectUserProjectRepository, IntegrationWorkflowRepository, ProjectWorkflowService)`): a workflow is embedded when `integrationWorkflowRepository.findByWorkflowId(workflowId)` is present, or when its project (via `ProjectWorkflowService`) is embedded; a project is embedded when `automationWorkflowProjectRepository.findByProjectId(projectId)` is present or a `connected_user_project` row references it (add `boolean existsByProjectId(long projectId)` to `ConnectUserProjectRepository` if missing).

`PermissionServiceImpl` takes `ObjectProvider<EmbeddedResourceResolver>`. In `hasWorkflowScope(workflowId, scope)` and `hasWorkflowScope(workflowId, scope, environment)`, after the connected-user, skip and tenant-admin short-circuits and before the project lookup: if the resolver is present and `isEmbeddedWorkflow(workflowId)`, return `hasEmbeddedScope(scope)` / `hasEmbeddedScope(scope, environment)`. Same branch in `hasWorkflowScopeIfProjectWorkflow`. In `hasWorkspaceScopeForProject` both overloads: `isEmbeddedProject(projectId)` → embedded answer. `ProjectOwnershipResolver` takes `ObjectProvider<EmbeddedResourceResolver>` and returns `ResourceOwner.ofEmbedded()` for an embedded project.

- [ ] **Step 1: Write the failing tests.** In `PermissionServiceTest` (nested `EmbeddedWorkflows`):

```java
@Test void testAnIntegrationWorkflowIsAnsweredByTheEmbeddedScopeInTheGivenEnvironment()
@Test void testAnIntegrationWorkflowWithoutTheScopeIsDeniedEvenForAWorkspaceEditor()
@Test void testAProjectWorkflowStillResolvesThroughItsWorkspace()        // resolver answers false
@Test void testHasWorkflowScopeIfProjectWorkflowGrantsAnEmbeddedWorkflowByEmbeddedScope()
@Test void testWithoutAResolverBeanNothingChanges()                      // ObjectProvider empty -> existing behaviour
```

`EmbeddedResourceResolverIntTest` (use `IntegrationIntTestConfiguration` and `@IntegrationIntTestConfigurationSharedMocks` as `AppEventFacadeIntTest` does): seed an integration with one workflow and a Hub project row; assert `isEmbeddedWorkflow` true for it, false for a random id; `isEmbeddedProject` true for the Hub project, false otherwise.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew EECFG:test --tests '*PermissionServiceTest' -q 2>&1 | tail -20`
Expected: compilation failure.

- [ ] **Step 3: Implement.** Check `EMBCFG`'s `build.gradle.kts` already depends on the CE `automation-configuration-api` (it does for `ConnectedUserAccessDeciderImpl`).

- [ ] **Step 4: Run the tests**

Run: `./gradlew EECFG:test --tests '*PermissionServiceTest' -q 2>&1 | tail -5 && DOCKER_HOST=... ./gradlew EMBCFG:testIntegration --tests '*EmbeddedResourceResolverIntTest' -q 2>&1 | tail -5`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply -q
git add server/libs/automation server/ee/libs/automation server/ee/libs/embedded
git commit -m "1051 Resolve integration workflows and Hub projects to the embedded side in workflow and project scope checks"
```

---

### Task 8: Embedded membership service

**Files:**
- Create: `EEAPI/service/EmbeddedUserService.java`, `EESVC/service/EmbeddedUserServiceImpl.java`
- Modify: `EESVC/service/WorkspaceMembershipAssignerImpl.java` (`removeMemberships(long userId)` also deletes embedded rows and evicts)
- Test: `EETEST/service/EmbeddedUserServiceIntTest.java`

**Interfaces:**
- Consumes: `EmbeddedUserRepository`, `PermissionService.getMyEmbeddedScopes(Environment)`, `PermissionService.evictEmbeddedScopeCache(long)`, `CurrentUserResolver`.
- Produces:

```java
public interface EmbeddedUserService {
    EmbeddedUser addEmbeddedUser(long userId, WorkspaceRole embeddedRole);
    List<EmbeddedUser> getEmbeddedUsers();
    List<EmbeddedUser> getUserEmbeddedUsers(long userId);
    boolean isEmbeddedMember(long userId);
    void removeEnvironmentRole(long userId, Environment environment);
    boolean removeEmbeddedUser(long userId);
    EmbeddedUser setEnvironmentRole(long userId, Environment environment, WorkspaceRole embeddedRole);
    EmbeddedUser updateEmbeddedUserRole(long userId, WorkspaceRole embeddedRole);
}
```

Rules, copied from `WorkspaceUserServiceImpl` with the workspace dimension removed and no last-Admin rule:
- `addEmbeddedUser`: the user must exist and not already be a member (`existsByUserId`); saves an implicit row.
- `updateEmbeddedUserRole`: replaces the implicit row's role; a member in explicit mode is rejected with `IllegalStateException` (switch modes by setting environment roles).
- `setEnvironmentRole`: converts an implicit member to explicit mode by first copying the implicit role to the other two environments (as `WorkspaceUserServiceImpl.setEnvironmentRole` does), then upserts the row for `environment`.
- `removeEnvironmentRole`: deletes the row; when it was the last explicit row, saves `forRole(userId, thatRole)` as the implicit row (the widening rule restored in `efb7dc5390f`).
- Every grant is validated with `validateGrantWithinCallerScopes(List<Environment> environments, WorkspaceRole embeddedRole)`: for each environment the granted role's scopes (`permissionScopeRegistry.getScopeNames(role, EMBEDDED)`) must be a subset of `permissionService.getMyEmbeddedScopes(environment)`; mirror the message and exception type of the workspace version.
- `removeEmbeddedUser`: deletes every row; returns whether any existed.
- Every write ends with `permissionService.evictEmbeddedScopeCache(userId)`.

- [ ] **Step 1: Write the failing integration test** `EmbeddedUserServiceIntTest` (same harness as `PermissionServiceIntTest`, real `PermissionServiceImpl`, `EmbeddedScopeCacheService` and `EmbeddedUserServiceImpl`; authenticate with the `authenticate(login)` helper pattern from that test; a tenant admin for grants unless stated):

```java
@Test void testAddingAMemberGivesThemTheRoleInEveryEnvironment()
@Test void testSettingAnEnvironmentRoleTurnsAnImplicitMemberExplicit()        // after setEnvironmentRole(u, PRODUCTION, VIEWER) from EDITOR: DEVELOPMENT and STAGING rows are EDITOR, PRODUCTION is VIEWER, no implicit row
@Test void testRemovingTheLastEnvironmentRoleWidensItToAnEmbeddedWideRole()
@Test void testAnEmbeddedEditorCannotGrantAdmin()                              // authenticate as an embedded EDITOR member -> AccessDeniedException (or the workspace version's exception)
@Test void testAnEmbeddedAdminMayRemoveThemselves()                            // no last-admin rule
@Test void testRemovingATenantUserRemovesTheirEmbeddedRows()                   // WorkspaceMembershipAssignerImpl.removeMemberships(u)
@Test void testARoleChangeIsVisibleImmediately()                               // hasEmbeddedScope before/after updateEmbeddedUserRole, cache evicted
```

- [ ] **Step 2: Run to verify it fails**

Run: `DOCKER_HOST=... ./gradlew EECFG:testIntegration --tests '*EmbeddedUserServiceIntTest' -q 2>&1 | tail -20`
Expected: compilation failure.

- [ ] **Step 3: Implement** the service and the `removeMemberships` extension.

- [ ] **Step 4: Run the test**

Run: `DOCKER_HOST=... ./gradlew EECFG:testIntegration --tests '*EmbeddedUserServiceIntTest' -q 2>&1 | tail -20`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/automation
git commit -m "1051 Manage embedded membership with embedded-wide and per-environment roles"
```

---

### Task 9: Membership facade, gates and audit

**Files:**
- Create: `EEAPI/facade/EmbeddedUserFacade.java`, `EESVC/facade/EmbeddedUserFacadeImpl.java`
- Create: `EESVC/audit/EmbeddedUserAuditEvents.java`, `EESVC/audit/EmbeddedUserAuditMapper.java`
- Modify: `EETEST/service/PreAuthorizeAnnotationTest.java`
- Test: `EETEST/audit/EmbeddedUserAuditMapperTest.java`, `EETEST/facade/EmbeddedUserFacadeIntTest.java`

**Interfaces:**
- Consumes: `EmbeddedUserService` (Task 8).
- Produces `EmbeddedUserFacade` with the same method set as the service minus `isEmbeddedMember`/`getUserEmbeddedUsers`, gated on the impl:

| Method | `@PreAuthorize` | `@Audited` event |
|---|---|---|
| `addEmbeddedUser(long userId, WorkspaceRole embeddedRole)` | `hasEmbeddedScopeInEveryEnvironment('EMBEDDED_MEMBER_MANAGE')` | `EMBEDDED_USER_ADDED` |
| `getEmbeddedUsers()` | `hasEmbeddedScope('EMBEDDED_VIEW')` | none |
| `updateEmbeddedUserRole(long userId, WorkspaceRole embeddedRole)` | every-environment | `EMBEDDED_USER_ROLE_UPDATED` |
| `removeEmbeddedUser(long userId)` | every-environment | `EMBEDDED_USER_REMOVED` |
| `setEnvironmentRole(long userId, Environment environment, WorkspaceRole embeddedRole)` | `hasEmbeddedScopeInEnvironment('EMBEDDED_MEMBER_MANAGE', #environment)` | `EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED` |
| `removeEnvironmentRole(long userId, Environment environment)` | in-environment | `EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED` |

Event names follow the `WORKSPACE_USER_*` convention from #746 rather than the spec's `EMBEDDED_MEMBER_*` wording (my call, for consistency in the audit filter). `EmbeddedUserAuditMapper` (`@Component @ConditionalOnEEVersion`, constructor `(EmbeddedUserRepository)`) mirrors `WorkspaceUserAuditMapper`: `events()` is the five names; `capture` reads the previous role; `map` writes `userId`, `environment`, `role`, `previousRole`, `widenedToEmbeddedWide`, reading arguments `userId`, `environment`, `embeddedRole`.

- [ ] **Step 1: Write the failing tests.**
  - `EmbeddedUserAuditMapperTest`, modelled on `WorkspaceUserAuditMapperTest`: `testAddedRecordsUserAndRole`, `testEnvironmentRoleRemovedRecordsWideningOnlyWhenItHappened`, `testRoleUpdatedRecordsThePreviousRole`.
  - In `PreAuthorizeAnnotationTest`: `testEmbeddedUserFacadeMembershipChangesAreProtected` asserting each expression above with `assertEveryPublicOverloadIsGated(Class.forName("com.bytechef.ee.automation.configuration.facade.EmbeddedUserFacadeImpl"), "<method>", "<expression>")`, and add `EmbeddedUserServiceImpl` to `testServiceImplementationsCarryNoAuthorizationChecks`.
  - `EmbeddedUserFacadeIntTest` (harness of `PermissionServiceIntTest` plus `@EnableMethodSecurity`, as `PreAuthorizeProxyEnforcementIntTest` does): `testANonMemberIsRefusedTheMemberList`, `testAnEmbeddedViewerListsMembersButCannotAdd`, `testAnEmbeddedAdminAddsAMember`, `testATenantAdminWithNoRowAddsAMember`.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew EECFG:test --tests '*EmbeddedUserAuditMapperTest' --tests '*PreAuthorizeAnnotationTest' -q 2>&1 | tail -20`
Expected: compilation failure / `ClassNotFoundException`.

- [ ] **Step 3: Implement** the facade, events and mapper.

- [ ] **Step 4: Run the tests, then `PermissionScopeTypeTest`** (which now finds `EMBEDDED_VIEW` and `EMBEDDED_MEMBER_MANAGE` enforced):

Run: `./gradlew EECFG:test --tests '*EmbeddedUser*' --tests '*PreAuthorizeAnnotationTest' CECFG:test --tests '*PermissionScopeTypeTest' -q 2>&1 | tail -20 && DOCKER_HOST=... ./gradlew EECFG:testIntegration --tests '*EmbeddedUserFacadeIntTest' -q 2>&1 | tail -5`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/automation server/libs/automation/automation-configuration
git commit -m "1051 Gate and audit embedded membership changes"
```

---

### Task 10: GraphQL API for embedded membership and scopes

**Files:**
- Create: `EEGQL/src/main/resources/graphql/embedded-user.graphqls`
- Create: `EEGQL/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/EmbeddedUserGraphQlController.java`
- Test: `EEGQL/src/test/java/com/bytechef/ee/automation/configuration/web/graphql/EmbeddedUserGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `EmbeddedUserFacade` (Task 9), `PermissionService.getMyEmbeddedScopes(Environment)`, `UserService` for the `user` field and inherited tenant admins (as `WorkspaceUserGraphQlController` does).
- Produces the schema (reusing `WorkspaceRole`, `EnvironmentEnum` and `WorkspaceUserInfo` from `workspace-user.graphqls`):

```graphql
extend type Query {
    embeddedUsers: [EmbeddedUser!]!
    myEmbeddedScopes(environment: EnvironmentEnum): [String!]!
}

extend type Mutation {
    addEmbeddedUser(userId: ID!, role: WorkspaceRole!): EmbeddedUser!
    updateEmbeddedUserRole(userId: ID!, role: WorkspaceRole!): EmbeddedUser!
    removeEmbeddedUser(userId: ID!): Boolean!
    setEmbeddedUserEnvironmentRole(userId: ID!, environment: EnvironmentEnum!, role: WorkspaceRole!): EmbeddedUser!
    removeEmbeddedUserEnvironmentRole(userId: ID!, environment: EnvironmentEnum!): Boolean!
}

type EmbeddedUser {
    id: ID
    userId: ID!
    embeddedRole: WorkspaceRole
    customRoleId: ID
    inherited: Boolean!
    user: WorkspaceUserInfo
    createdDate: String
    environment: EnvironmentEnum
}
```

Controller: `@Controller @ConditionalOnCoordinator @ConditionalOnEEVersion`, records `EmbeddedUserView(Long id, long userId, String embeddedRole, Long customRoleId, boolean inherited, String createdDate, String environment)`; `embeddedUsers` appends tenant admins as `inherited` rows exactly as `workspaceUsers` does; `myEmbeddedScopes` delegates to `permissionService.getMyEmbeddedScopes(environment)`.

- [ ] **Step 1: Write the failing test** `EmbeddedUserGraphQlControllerTest`, modelled on `CustomRoleGraphQlControllerTest`: `testEmbeddedUsersListsMembersAndInheritedTenantAdmins`, `testMyEmbeddedScopesPassesTheEnvironmentThrough`, `testAddEmbeddedUserMapsTheRole`.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:test --tests '*EmbeddedUserGraphQlControllerTest' -q 2>&1 | tail -20`
Expected: compilation failure.

- [ ] **Step 3: Implement** the schema and controller.

- [ ] **Step 4: Run the test**

Run: same command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply -q
git add server/ee/libs/automation
git commit -m "1051 Expose embedded membership and my embedded scopes over GraphQL"
```

---

### Task 11: Independence and tenant-admin integration test

**Files:**
- Test: `EETEST/service/EmbeddedPermissionServiceIntTest.java`

**Interfaces:**
- Consumes: everything above.

- [ ] **Step 1: Write the integration test** (harness of `PermissionServiceIntTest`; seed one workspace with a `WorkspaceUser` EDITOR who has no embedded row, one `EmbeddedUser` ADMIN who has no workspace row, one embedded member in explicit mode with DEVELOPMENT EDITOR only, and use the test's tenant admin):

```java
@Test void testAWorkspaceEditorHoldsNoEmbeddedScope()                 // hasEmbeddedScope("WORKFLOW_VIEW") false; getMyEmbeddedScopes(null) empty
@Test void testAnEmbeddedAdminHoldsNoWorkspaceScope()                 // hasWorkspaceScope(ws, "WORKSPACE_VIEW") false
@Test void testAnExplicitMemberIsDeniedOutsideTheirEnvironments()     // hasEmbeddedScope("WORKFLOW_EDIT", DEVELOPMENT) true, (…, PRODUCTION) false, hasEmbeddedScopeInEveryEnvironment false
@Test void testATenantAdminHoldsEveryEmbeddedScopeInEveryEnvironment()
@Test void testAnUnauthenticatedCallerGetsNoEmbeddedScopes()         // getMyEmbeddedScopes -> AuthenticationCredentialsNotFoundException, like the workspace variant
```

- [ ] **Step 2: Run it**

Run: `DOCKER_HOST=... ./gradlew EECFG:testIntegration --tests '*EmbeddedPermissionServiceIntTest' -q 2>&1 | tail -20`
Expected: PASS. If a case fails, the fix belongs in the task that owns the code (5, 6 or 8), not in the test.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation
git commit -m "1051 Pin that embedded and workspace roles stay independent"
```

---

### Task 12: Module checks and the ticket

**Files:**
- Modify: none in the repo; the #1051 ticket body (through `gh issue edit`).

- [ ] **Step 1: Run the full checks of every touched module**

Run (one log, in the scratchpad):
```bash
DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock ./gradlew --continue \
  :server:libs:automation:automation-configuration:automation-configuration-api:check \
  CECFG:check \
  :server:ee:libs:automation:automation-configuration:automation-configuration-api:check \
  EECFG:check \
  :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:check \
  :server:ee:libs:automation:automation-configuration:automation-configuration-remote-client:check \
  EMBCFG:check \
  :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:check \
  :server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service:check \
  > <scratchpad>/check-plan-a.log 2>&1; grep -E "^> Task .* FAILED|BUILD (SUCCESSFUL|FAILED)" <scratchpad>/check-plan-a.log
```
Expected: no `FAILED` task lines, `BUILD SUCCESSFUL`. `AutomationMethodSecurityConfigurationTest` must still pass unchanged: the new facade is gated, so no allowlist edit.

- [ ] **Step 2: Start the server once against a fresh database** (`createdb bytechef_plan_a`, `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bytechef_plan_a SERVER_PORT=9556 ./gradlew -p server/apps/server-app bootRun`) and confirm `embedded_user` exists with its two unique indexes: `psql bytechef_plan_a -c '\d embedded_user'`. Stop the server by its PID.

- [ ] **Step 3: Update #1051.** Add to the scope catalogue table an "Embedded" block listing the Task 2 scopes with their roles, and a "Results after adding the embedded RBAC engine (date)" block with the commit list and check results. Write the body to the scratchpad and send it with `gh issue edit 1051 -R bytechefhq/bytechef --body-file`.

---

## Follow-up plans (not in this document)

- **Plan B, gates:** move the 107 `isTenantAdmin()` gates in the 23 embedded facades to the catalogue (unpinning `KNOWN_UNENFORCED_SCOPES` as each scope is enforced); embedded ownership and environment resolvers for `Integration`, `IntegrationInstanceConfiguration`, `IntegrationInstance`, `ConnectedUser`, `SigningKey`, `McpIntegrationInstanceConfiguration`, `Job`, `TriggerExecution`, `Connection`, `McpServer`, `ApiKey`; the `ApiKeyFacadeImpl` embedded branch (spec decision 9); moving Hub and connected-user projects out of the default workspace (decision 8); `withoutDetails()` redaction in `IntegrationWorkflowExecutionFacadeImpl`; embedded list filtering per environment.
  - Carried over from Plan A's reviews:
    - Hub and connected-user projects, and workflows embedded only through such a project, deny on every automation gate for anyone but a tenant admin (`4b8d009d7e8`); replace that with their embedded gates (`CONNECTED_USER_*` in the resource's environment, `INTEGRATION_SETTINGS` for Hub templates).
    - Finish decision 8 on the remaining automation paths, which still resolve these projects to the default workspace: `ProjectWorkflowOwnershipResolver`, `TestJobOwnershipResolver`, the `ProjectDeployment` and `ProjectDeploymentWorkflow` resolvers, `canUseConnectionInWorkflow`, and the project lists. `0_p_1051` does not ship without this.
    - The embedded `ApiKey` resolver must not claim rows whose `type` is null; those are platform admin keys.
    - `PermissionScopeRegistryTest` treats the literals of `.hasResourceScope` and other programmatic checks as automation-side; embedded resource gates and programmatic `hasEmbeddedScope` calls need side-aware handling there.
    - Worth fixing alongside:
      - A non-embedded `hasWorkflowScope` check runs about five uncached queries; cache them or use one `EXISTS`.
      - On configuration-app, the remote `fetchConnection` answers 500 for a missing id instead of a deny.
      - Deleting a user audits their embedded removal under `WORKSPACE_MEMBERSHIPS_REMOVED`.
      - `CustomRoleServiceImpl` evicts only `workspaceScopes`; it must also evict `embeddedScopes` once embedded custom roles exist.
      - A per-environment embedded Admin cannot remove themselves, because removal needs `EMBEDDED_MEMBER_MANAGE` in every environment (decide in Plan C).
- **Plan C, client, e2e and docs:** `useHasEmbeddedScope`, embedded routes and nav off `AUTHORITIES.ADMIN`, control gating on every embedded page, the read-only integration editor, `detailsHidden` in the embedded run sheet, the Embedded > Settings > Members page, codegen, `routes.test.tsx`, `tests/embedded-rbac` Playwright specs with fixtures, and the embedded users and roles doc page.
