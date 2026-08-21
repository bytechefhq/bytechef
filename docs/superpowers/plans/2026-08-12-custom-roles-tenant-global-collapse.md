# Custom Roles Tenant-Global Collapse Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the per-workspace custom-role tier so custom roles are tenant-global only — defined once by tenant admins, assignable in any workspace.

**Architecture:** Pure collapse of an unreleased two-tier feature. `custom_role.workspace_id` is deleted from the domain, schema, service, GraphQL contract, and client; mutations single-branch to `isTenantAdmin()`. One deliberate remnant: `getCustomRoles(Long workspaceId)` keeps its dual-audience `@PreAuthorize` (tenant admin OR `WORKSPACE_MEMBER_MANAGE`) because workspace admins need the role list to populate the invite/assign picker — the argument becomes authorization context, never a filter.

**Tech Stack:** Spring Boot 4 / Spring Data JDBC / Spring GraphQL (EE modules), Liquibase, React 19 + TanStack Query + generated GraphQL client, JUnit 5 + Mockito, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-12-custom-roles-tenant-global-collapse-design.md`

## Global Constraints

- All files under `server/ee/` carry the ByteChef Enterprise license header (NOT Apache 2.0) and an `@version ee` Javadoc tag. Every file this plan touches already has both — do not remove them.
- Java style: one blank line before control statements (`if`, `for`, `try`…) except at block start or after `} else {`-style continuations; one blank line between a variable modification and a following statement using it; no trailing blank line before a class's closing `}`.
- Test method names are camelCase without underscores; unit test classes end in `Test`, integration tests in `IntTest`.
- Client: ESLint `sort-keys` requires object keys in ascending alphabetical order (NOT auto-fixed by `--fix`); interfaces end in `I` or `Props`; named imports sorted alphabetically inside `{}`.
- Commit messages: server `1051 <description>`, client `1051 client - <description>`. Never amend; always fresh commits. Stage only files this task modified.
- Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep the file for `^> Task .* FAILED`.
- The schema is UNRELEASED (`custom_role` absent from `v0.31.2`), which is the license to edit the init changelog in place.

---

### Task 1: Server — collapse CustomRole to tenant-global

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/domain/CustomRole.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/service/CustomRoleService.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/CustomRoleRepository.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/CustomRoleServiceImpl.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/WorkspaceUserServiceImpl.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/CustomRoleGraphQlController.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/custom-role.graphqls`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/CustomRoleServiceTest.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/WorkspaceUserServiceTest.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/PreAuthorizeAnnotationTest.java`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/RealImplProxyEnforcementIntTest.java`

**Interfaces:**
- Consumes: nothing from other tasks (this is the first task).
- Produces (later tasks and the client rely on these exact shapes):
  - `CustomRole(String name, Set<String> scopeNames)` — the only public constructor; no `workspaceId` field or getter.
  - `CustomRoleService.createCustomRole(String name, String description, Set<String> scopeNames)`
  - `CustomRoleService.updateCustomRole(long roleId, String name, String description, Set<String> scopeNames)`
  - `CustomRoleService.deleteCustomRole(long roleId)`
  - `CustomRoleService.getCustomRoles(Long workspaceId)` — signature unchanged; returns every role regardless of argument.
  - GraphQL: `customRoles(workspaceId: ID): [CustomRole!]!` (arg kept), `deleteCustomRole(id: ID!): Boolean!` (arg dropped), `CustomRole` type and both inputs WITHOUT `workspaceId`.

- [ ] **Step 1: Update `CustomRoleServiceTest` to pin the tenant-global contract (RED)**

Delete these six test methods entirely (the states they pin no longer exist):
- `testDeleteCustomRoleRejectsAMismatchedTier`
- `testUpdateCustomRoleRejectsAnotherWorkspacesRole`
- `testUpdateCustomRoleAcceptsMatchingWorkspace`
- `testCreateCustomRoleStoresTheOwningWorkspace`
- `testCreateCustomRoleWithoutWorkspaceIsGlobal`
- `testGetCustomRolesForAWorkspaceExcludesOtherWorkspaces`

Change every remaining call site to the new signatures (drop the trailing `null`):
- `customRoleService.createCustomRole("Custom Editor", "Can view and edit workflows", scopeNames)` in `testCreateCustomRolePersistsCorrectScopes`
- `customRoleService.createCustomRole("Bad Role", "desc", Set.of("NOT_A_REAL_SCOPE"))` in `testCreateCustomRoleRejectsUnknownScope`
- `customRoleService.deleteCustomRole(1L)` in `testDeleteCustomRoleInUseThrowsException` and `testDeleteCustomRoleNotInUseSucceeds` — and REMOVE the `when(customRoleRepository.findById(1L))...` stubbing from both (the new `deleteCustomRole` never loads the role; `validateRoleTier` is gone)
- `customRoleService.updateCustomRole(1L, "New Name", "Updated description", newScopeNames)` in `testUpdateCustomRoleReplacesScopes`; same 4-arg shape in `testUpdateCustomRoleEvictsCache` and `testUpdateCustomRoleSkipsCacheEvictionWhenScopesUnchanged`

Add one new test pinning the read remnant:

```java
@Test
void testGetCustomRolesForAWorkspaceReturnsEveryRole() {
    CustomRole role = new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"));

    when(customRoleRepository.findAll()).thenReturn(List.of(role));

    // The workspaceId is authorization context for the assignment picker, not a filter — every role is
    // tenant-global and assignable in any workspace.
    assertThat(customRoleService.getCustomRoles(7L)).containsExactly(role);
}
```

Keep `testGetCustomRolesWithoutWorkspaceReturnsEveryRole` unchanged. Remove the now-unused `import java.util.Optional;` if no remaining test uses it (`testUpdateCustomRole*` still stub `findById`, so `Optional` stays — verify with the compiler, not by guessing).

- [ ] **Step 2: Update `WorkspaceUserServiceTest` (RED)**

- Delete `testAddWorkspaceUserRejectsAnotherWorkspacesCustomRole` and `testAssignCustomRoleRejectsAnotherWorkspacesRole` entirely.
- Delete `testAssignCustomRoleAcceptsThisWorkspacesOwnRole` (with tiers gone it duplicates the global-accept case).
- Rename `testAssignCustomRoleAcceptsAGlobalRole` to `testAssignCustomRoleAcceptsAnExistingRole` (there is no other kind anymore).
- Change every 3-arg `CustomRole` constructor call to 2-arg: `new CustomRole("Deployer", Set.of("WORKFLOW_VIEW"))` — sites are in `testAddWorkspaceUserAcceptsACustomRole` (~line 104), `testInviteWorkspaceUserAcceptsACustomRole` (~line 158), and the assign tests (~line 491).
- Add a test pinning the surviving existence check:

```java
@Test
void testAssignCustomRoleRejectsAnUnknownRoleId() {
    // The workspace-boundary check is gone, but a dangling custom_role_id would fail closed at permission-check
    // time and invisibly lock the member out — writes must still reject it loudly.
    when(customRoleRepository.findById(900L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, 900L))
        .isInstanceOf(ConfigurationException.class)
        .hasMessageContaining("does not exist");
}
```

(Match the file's existing constants `USER_ID` / `WORKSPACE_ID` and its existing imports — `Optional`, `assertThatThrownBy`, `ConfigurationException` are already imported.)

- [ ] **Step 3: Update `PreAuthorizeAnnotationTest` (RED)**

Replace the whole `testCustomRoleServiceWorkspaceScopedMethodsAreTiered` method (and its Javadoc) with these two:

```java
/**
 * Mutations are tenant-admin-only: a custom role is tenant-global and assignable everywhere, so defining one is a
 * tenant-wide act. Pinned in full so a workspace-tier branch cannot quietly reappear.
 */
@Test
void testCustomRoleMutationsRequireTenantAdmin() throws NoSuchMethodException {
    assertPreAuthorize(
        CustomRoleService.class.getMethod(
            "createCustomRole", String.class, String.class, java.util.Set.class),
        "isTenantAdmin()");

    assertPreAuthorize(
        CustomRoleService.class.getMethod(
            "updateCustomRole", long.class, String.class, String.class, java.util.Set.class),
        "isTenantAdmin()");

    assertPreAuthorize(
        CustomRoleService.class.getMethod("deleteCustomRole", long.class),
        "isTenantAdmin()");
}

/**
 * The read is the deliberate remnant of the old two-tier model: it has two audiences — tenant admins managing
 * roles, and workspace member managers populating the assignment picker. The workspaceId argument is
 * authorization context only; the body returns every role either way.
 */
@Test
void testGetCustomRolesIsTieredForAssignmentReads() throws NoSuchMethodException {
    assertPreAuthorize(
        CustomRoleService.class.getMethod("getCustomRoles", Long.class),
        "(#workspaceId == null and isTenantAdmin()) or " +
            "(#workspaceId != null and hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE'))");
}
```

- [ ] **Step 4: Update `RealImplProxyEnforcementIntTest` call sites (RED)**

Two signature fixes, nothing else changes in this file:
- `customRoleService.createCustomRole("r", "d", Set.of("WORKFLOW_VIEW"))` (~line 105)
- `customRoleService.deleteCustomRole(1L)` (~line 111)

`getCustomRoles(null)` (~line 117) stays as-is — the signature is unchanged.

- [ ] **Step 5: Verify RED — compilation fails against the old production code**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:compileTestJava --continue > /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task1-red.log 2>&1
echo $?
grep "^> Task .* FAILED" /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task1-red.log
```

Expected: exit code non-zero, `compileTestJava` FAILED — the tests call 3-arg `createCustomRole`, which does not exist yet.

- [ ] **Step 6: Collapse the domain — `CustomRole.java`**

- Delete the `workspaceId` field and its Javadoc block ("The workspace this role belongs to…").
- Delete the 3-arg constructor `CustomRole(String, Set<String>, Long)`.
- Rewrite the 2-arg constructor to hold the invariant logic directly (it currently delegates to the 3-arg):

```java
/**
 * Creates a validated custom role. Enforces the invariants that persistence alone cannot: a non-blank name and a
 * non-empty scope set (a role with no scopes grants no permissions, which is almost never what the caller means
 * and would leave affected users silently locked out). Every custom role is tenant-global — defined once and
 * assignable in any workspace.
 */
public CustomRole(String name, Set<String> scopeNames) {
    Assert.hasText(name, "'name' must not be blank");
    Assert.notEmpty(scopeNames, "'scopeNames' must not be empty");

    this.name = name;
    this.scopes = scopeNames.stream()
        .map(CustomRoleScope::new)
        .collect(Collectors.toCollection(HashSet::new));
}
```

- Delete `getWorkspaceId()` and its Javadoc.

- [ ] **Step 7: Prune the repository — `CustomRoleRepository.java`**

Delete all three declared methods — `findByName` and `findAllGlobal` have zero callers anywhere (verified by grep across `server/`), and `findAllVisibleToWorkspace`'s union predicate is meaningless once every role is global:

```java
@Repository
@ConditionalOnEEVersion
public interface CustomRoleRepository extends ListCrudRepository<CustomRole, Long> {
}
```

(Keep the license header, package, `CustomRole`/`ConditionalOnEEVersion` imports as needed — drop the now-unused `List`, `Optional`, `Query`, `Param` imports.)

- [ ] **Step 8: Collapse the service — `CustomRoleService.java` (api) and `CustomRoleServiceImpl.java`**

New interface method set (replace the five Javadoc'd methods; `getPermissionScopeNames` is untouched):

```java
/**
 * Creates a tenant-global custom role, assignable in every workspace. Requires tenant admin — a global role is
 * assignable everywhere, so creating one is a tenant-wide act.
 */
CustomRole createCustomRole(String name, String description, Set<String> scopeNames);

/**
 * Deletes a custom role. Fails while the role is still assigned to any workspace member. Requires tenant admin.
 */
void deleteCustomRole(long roleId);

/**
 * Every permission scope name a module contributed through {@code PermissionScopeProvider}.
 *
 * <p>
 * Exists so a role editor offers the scopes the server will actually accept. A hardcoded client list silently omits
 * any scope a module adds later, and the server rejects anything it does not recognise — the two must come from the
 * same place.
 */
List<String> getPermissionScopeNames();

/**
 * Every custom role in the tenant. The read has two audiences — tenant admins managing roles, and workspace member
 * managers populating the assignment picker — so {@code workspaceId} is pure authorization context: pass the
 * workspace being managed to read as its member manager, or {@code null} for the tenant-admin view. It never
 * filters; every role is tenant-global.
 */
List<CustomRole> getCustomRoles(Long workspaceId);

/**
 * Updates a custom role. Requires tenant admin.
 */
CustomRole updateCustomRole(long roleId, String name, String description, Set<String> scopeNames);
```

In `CustomRoleServiceImpl`:
- `createCustomRole`: annotation becomes `@PreAuthorize("isTenantAdmin()")`, parameter list drops `Long workspaceId`, construction becomes `new CustomRole(name, scopeNames)`. Rest of the body (audit, save) unchanged.
- `deleteCustomRole(long roleId)`: annotation `@PreAuthorize("isTenantAdmin()")`, drop the `validateRoleTier(roleId, workspaceId)` call. Rest unchanged (in-use check → delete → audit).
- `updateCustomRole(long roleId, String name, String description, Set<String> scopeNames)`: annotation `@PreAuthorize("isTenantAdmin()")`, drop the `validateRoleTier` call. The scope-diff cache-eviction logic stays byte-identical.
- `getCustomRoles(Long workspaceId)`: KEEP the existing dual-branch `@PreAuthorize` exactly as it is. Body becomes:

```java
@Override
@PreAuthorize("(#workspaceId == null and isTenantAdmin()) or " +
    "(#workspaceId != null and hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE'))")
@Transactional(readOnly = true)
public List<CustomRole> getCustomRoles(Long workspaceId) {
    // The workspaceId is authorization context, not a filter: a workspace member manager reads the list to
    // populate the assignment picker, a tenant admin reads it to manage roles. Every role is tenant-global.
    return customRoleRepository.findAll();
}
```

- Delete the whole `validateRoleTier` method and its Javadoc. Remove the now-unused imports (`java.util.Objects`, `com.bytechef.commons.util.OptionalUtils` — `OptionalUtils` is still used by `updateCustomRole`'s `findById`, so only delete what the compiler confirms is unused).

- [ ] **Step 9: Shrink assignment validation — `WorkspaceUserServiceImpl.java`**

Replace `validateCustomRoleAssignable(long customRoleId, long workspaceId)` with:

```java
/**
 * Shared by {@code assignCustomRole} and the add/invite paths. Every custom role is tenant-global and assignable
 * in any workspace, so the only thing left to validate is existence — a dangling {@code custom_role_id} would
 * fail closed at permission-check time and invisibly lock the member out, so writes must reject it loudly.
 */
private void validateCustomRoleExists(long customRoleId) {
    customRoleRepository.findById(customRoleId)
        .orElseThrow(() -> new ConfigurationException(
            "Custom role " + customRoleId + " does not exist",
            WorkspaceUserErrorType.CUSTOM_ROLE_NOT_IN_WORKSPACE));
}
```

Update both call sites (in `assignCustomRole` and `addWorkspaceUser`) from `validateCustomRoleAssignable(customRoleId, workspaceId)` to `validateCustomRoleExists(customRoleId)`. Remove the `java.util.Objects` import if it is now unused in this file (check with the compiler — other methods may use it).

- [ ] **Step 10: Collapse the GraphQL contract — `custom-role.graphqls` and `CustomRoleGraphQlController.java`**

Schema — replace the query doc, all three mutation docs, the `CustomRole` type, and both inputs:

```graphql
extend type Query {
    """
    List every custom role in the tenant. Roles are tenant-global: defined once, assignable in any
    workspace. Pass a workspaceId to read as that workspace's member manager (requires the
    WORKSPACE_MEMBER_MANAGE scope there — the id is authorization context, not a filter), or omit it
    for the tenant-admin management view.
    """
    customRoles(workspaceId: ID): [CustomRole!]!
    """
    Every permission scope name the server recognises, for composing a role. Requires authentication:
    this is static metadata about what the server was built with, identical for every tenant.
    """
    permissionScopes: [String!]!
}

extend type Mutation {
    "Create a tenant-global custom role, assignable in every workspace. Requires tenant admin."
    createCustomRole(input: CreateCustomRoleInput!): CustomRole!
    "Update a custom role. Requires tenant admin."
    updateCustomRole(id: ID!, input: UpdateCustomRoleInput!): CustomRole!
    "Delete a custom role. Fails if it is still assigned to any member. Requires tenant admin."
    deleteCustomRole(id: ID!): Boolean!
}

"A custom permission role (EE) with a user-defined set of permission scopes"
type CustomRole {
    id: ID!
    name: String!
    description: String
    "Permission scope names granted by this role (e.g., WORKFLOW_VIEW, EXECUTION_VIEW)"
    scopes: [String!]!
    createdDate: String
}

input CreateCustomRoleInput {
    name: String!
    description: String
    "Permission scope names to grant (must be names registered by the server's PermissionScopeProvider SPI)"
    scopes: [String!]!
}

input UpdateCustomRoleInput {
    name: String!
    description: String
    "Permission scope names to grant (must be names registered by the server's PermissionScopeProvider SPI)"
    scopes: [String!]!
}
```

Controller:
- `createCustomRole`: `return customRoleService.createCustomRole((String) input.get("name"), (String) input.get("description"), scopeNames);`
- `updateCustomRole`: `return customRoleService.updateCustomRole(id, (String) input.get("name"), (String) input.get("description"), scopeNames);`
- `deleteCustomRole`: signature becomes `public boolean deleteCustomRole(@Argument long id)`, body `customRoleService.deleteCustomRole(id);`
- Delete the whole `toWorkspaceId` helper and its Javadoc.

- [ ] **Step 11: Verify GREEN**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-api:compileJava :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:compileJava :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --continue > /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task1-green.log 2>&1
echo $?
grep "^> Task .* FAILED" /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task1-green.log
```

Expected: exit 0, no FAILED lines.

- [ ] **Step 12: Format and commit**

```bash
./gradlew spotlessApply > /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task1-spotless.log 2>&1
echo $?
```

```bash
git add server/ee/libs/automation/automation-configuration/
git commit -m "1051 Collapse custom roles to tenant-global only

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

(Stage only the automation-configuration tree; if spotless touched unrelated files, leave them unstaged.)

---

### Task 2: Schema — init changelog edit and dev sync script

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200030_automation_configuration_added_table_custom_role.xml`
- Modify: `scripts/dev/sync-local-schema-after-collapse.sh`

**Interfaces:**
- Consumes: Task 1's entity shape (`CustomRole` no longer maps `workspace_id`).
- Produces: a `custom_role` table without `workspace_id`, with unique constraint `uk_custom_role_name` on `name`. `RbacMigrationsIntTest` (unchanged — it asserts table/column existence that still holds) is the from-scratch schema verification.

- [ ] **Step 1: Edit the init changelog in place**

In `202604061200030_automation_configuration_added_table_custom_role.xml`:

1. Delete the `workspace_id` column and the XML comment block above it (the one beginning "NULL means the role is tenant-global…").
2. Replace the entire `<sql dbms="postgresql">…</sql>` block (both partial `CREATE UNIQUE INDEX` statements) AND the comment above it (the one beginning "Two partial indexes rather than one unique constraint…") with:

```xml
<addUniqueConstraint tableName="custom_role" columnNames="name" constraintName="uk_custom_role_name"/>
```

Everything else in the changeset (table, `custom_role_scope`, PK, FK) stays byte-identical. In-place edit is justified: the table shipped in no release (`git ls-tree -r --name-only v0.31.2 | grep custom_role` returns nothing), per the same argument already recorded in D5 of the membership spec.

- [ ] **Step 2: Extend the dev sync script**

In `scripts/dev/sync-local-schema-after-collapse.sh`, append a new guarded block AFTER the existing `ai_observability_alert_rule_channel` block and BEFORE the final md5sum-nulling section:

```bash
# custom_role.workspace_id never shipped: the per-workspace role tier was removed before release
# (see docs/superpowers/specs/2026-08-12-custom-roles-tenant-global-collapse-design.md), so the init
# changelog now creates the table without the column, with a plain unique constraint on name in place
# of the two partial indexes. Bring a local DB created before that edit forward. Guarded throughout.
SQL="$SQL
DO \$\$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'custom_role' AND column_name = 'workspace_id') THEN
        DROP INDEX IF EXISTS ux_custom_role_global_name;
        DROP INDEX IF EXISTS ux_custom_role_workspace_name;

        -- Names were unique only within their tier; collapsing the tiers can collide them. Rename the
        -- newer duplicates deterministically so the plain unique constraint can be added.
        UPDATE custom_role SET name = name || ' (' || id || ')'
        WHERE id NOT IN (SELECT MIN(id) FROM custom_role GROUP BY name);

        ALTER TABLE custom_role DROP COLUMN workspace_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'custom_role')
       AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                       WHERE table_name = 'custom_role' AND constraint_name = 'uk_custom_role_name') THEN
        ALTER TABLE custom_role ADD CONSTRAINT uk_custom_role_name UNIQUE (name);
    END IF;
END
\$\$;"
```

- [ ] **Step 3: Syntax-check the script**

```bash
bash -n scripts/dev/sync-local-schema-after-collapse.sh
echo $?
```

Expected: exit 0.

- [ ] **Step 4: Verify the schema builds from scratch (Testcontainers)**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests "com.bytechef.ee.automation.configuration.repository.RbacMigrationsIntTest" > /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task2-inttest.log 2>&1
echo $?
grep "^> Task .* FAILED" /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task2-inttest.log
```

Expected: exit 0 (requires Docker running). Also delete any stale changelog copy from build output first if present: `rm -rf server/ee/libs/automation/automation-configuration/automation-configuration-service/build/resources/main/config/liquibase` (Liquibase sees both old and new on classpath after in-place edits).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200030_automation_configuration_added_table_custom_role.xml scripts/dev/sync-local-schema-after-collapse.sh
git commit -m "1051 Drop custom_role.workspace_id from the init changelog

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: Client — single-surface custom roles

**Files:**
- Modify: `client/src/graphql/automation/configuration/customRoles.graphql`
- Modify: `client/src/graphql/automation/configuration/createCustomRole.graphql`
- Modify: `client/src/graphql/automation/configuration/updateCustomRole.graphql`
- Modify: `client/src/graphql/automation/configuration/deleteCustomRole.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts` and `client/src/shared/middleware/graphql-types.ts` (via `npx graphql-codegen`)
- Modify: `client/src/ee/shared/components/custom-roles/CustomRolesManager.tsx`
- Modify: `client/src/ee/pages/settings/platform/custom-roles/GlobalCustomRoles.tsx`
- Delete: `client/src/ee/pages/settings/automation/custom-roles/` (the whole directory: `CustomRoles.tsx` + `tests/CustomRoles.test.tsx`)
- Modify: `client/src/routes.tsx`
- Test: `client/src/ee/shared/components/custom-roles/tests/CustomRolesManager.test.tsx`
- Test: `client/src/ee/pages/settings/platform/custom-roles/tests/GlobalCustomRoles.test.tsx`
- Test: `client/src/ee/pages/settings/automation/users/tests/WorkspaceUsers.test.tsx`

**Interfaces:**
- Consumes: Task 1's GraphQL schema (codegen reads the server `.graphqls` files listed in `client/codegen.ts`).
- Produces: `CustomRolesManager` takes NO props. The `CustomRoles` GraphQL query keeps its `$workspaceId` variable (the invite picker in `WorkspaceUsers.tsx` passes it — that file needs no change). `useDeleteCustomRoleMutation` variables are `{id}` only.

- [ ] **Step 1: Update the four GraphQL operation files**

`customRoles.graphql` (keep the variable — the invite picker sends it as authorization context; drop only the response field):

```graphql
query CustomRoles($workspaceId: ID) {
    customRoles(workspaceId: $workspaceId) {
        id
        name
        description
        scopes
    }
}
```

`createCustomRole.graphql`:

```graphql
mutation CreateCustomRole($input: CreateCustomRoleInput!) {
    createCustomRole(input: $input) {
        id
        name
        description
        scopes
    }
}
```

`updateCustomRole.graphql`:

```graphql
mutation UpdateCustomRole($id: ID!, $input: UpdateCustomRoleInput!) {
    updateCustomRole(id: $id, input: $input) {
        id
        name
        description
        scopes
    }
}
```

`deleteCustomRole.graphql`:

```graphql
mutation DeleteCustomRole($id: ID!) {
    deleteCustomRole(id: $id)
}
```

- [ ] **Step 2: Regenerate the GraphQL client**

```bash
cd client && npx graphql-codegen
```

Expected: regenerates `src/shared/middleware/graphql.ts` (and `graphql-types.ts`) without errors. If codegen fails on a type mismatch, the server `.graphqls` edit from Task 1 Step 10 is not in place — fix that first, do not hand-edit generated files.

- [ ] **Step 3: Commit the operations and regenerated client (house convention: generated file separately trails its operations only when committed apart; here the intermediate state would not typecheck, so commit operations + generated output together, components in the next commit)**

```bash
git add client/src/graphql/automation/configuration/ client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "1051 client - Regenerate the GraphQL client for tenant-global custom roles

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

- [ ] **Step 4: Update `CustomRolesManager.test.tsx` to the single-tier contract (RED)**

The suite locates the pencil/trash icon buttons by their empty accessible name: `screen.getAllByRole('button', {name: ''})`, indexed positionally per row (`[0]` = edit of row 1, `[1]` = delete of row 1, `[2]` = edit of row 2, `[3]` = delete of row 2). Keep that idiom.

- The two module-scope mock role objects (~lines 26–41) are named `workspaceRole` and `globalRole` — rename them to `deployerRole` and `auditorRole` (the tier distinction is gone) and delete the `workspaceId` property from both. Keep ids `'900'`/`'901'`, names `'Deployer'`/`'Auditor'`, scopes, and the `beforeEach` reset `hoisted.customRoles = [deployerRole, auditorRole];`.
- Every `render(<CustomRolesManager workspaceId="7" />)` and `render(<CustomRolesManager workspaceId={null} />)` becomes `render(<CustomRolesManager />)`.
- Delete the test `'marks each role as workspace-owned or global'` (no Scope column anymore).
- Delete the test `'offers edit and delete only for a workspace-owned role'` and the entire `describe('on the tenant tier', …)` block (no tiers). In their place add:

```tsx
it('offers edit and delete for every role', () => {
    render(<CustomRolesManager />);

    // Every role is tenant-global and managed here — there is no read-only tier anymore. Two roles,
    // two icon buttons (edit, delete) each.
    expect(screen.getAllByRole('button', {name: ''})).toHaveLength(4);
});
```

- `'creates a workspace-owned role'` → rename to `'creates a role'`; assertion becomes:

```tsx
expect(hoisted.createMutate).toHaveBeenCalledWith({
    input: {description: '', name: 'Deployer', scopes: ['WORKFLOW_EDIT']},
});
```

- `'loads a role into the form for editing and saves it'`: the click target stays `screen.getAllByRole('button', {name: ''})[0]` (edit of the first role); assertion becomes:

```tsx
expect(hoisted.updateMutate).toHaveBeenCalledWith({
    id: '900',
    input: {description: 'Can deploy', name: 'Deployer', scopes: ['WORKFLOW_VIEW']},
});
```

- `'deletes a workspace-owned role with its workspace id'` → rename to `'deletes a role'`; the click target stays `screen.getAllByRole('button', {name: ''})[1]` (delete of the first role); drop the tier comment; assertion becomes:

```tsx
expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: '900'});
```

- [ ] **Step 5: Update `GlobalCustomRoles.test.tsx` (RED)**

The manager mock no longer receives props — drop the `managerProps` hoisted spy and the `'manages roles on the tenant tier'` test (its `workspaceId: null` assertion is meaningless now). Keep a render test:

```tsx
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import GlobalCustomRoles from '../GlobalCustomRoles';

vi.mock('@/ee/shared/components/custom-roles/CustomRolesManager', () => ({
    default: () => <div data-testid="custom-roles-manager" />,
}));

describe('GlobalCustomRoles', () => {
    it('renders the manager', () => {
        render(<GlobalCustomRoles />);

        expect(screen.getByTestId('custom-roles-manager')).toBeInTheDocument();
    });
});
```

- [ ] **Step 6: Run the two updated suites to verify RED**

```bash
cd client && npx vitest run src/ee/shared/components/custom-roles src/ee/pages/settings/platform/custom-roles
```

Expected: FAIL — `CustomRolesManager` still requires its prop and renders the Scope column.

- [ ] **Step 7: Rewrite `CustomRolesManager.tsx` prop-less**

- Delete the `CustomRolesManagerPropsI` interface and the `workspaceId` prop: `const CustomRolesManager = () => {`.
- `useCustomRolesQuery({})` — the tenant page reads the tenant-admin view, so no variable. Update the comment above it to: `// Every custom role in the tenant — roles are tenant-global and assignable in any workspace.`
- `handleSubmit`: both mutation inputs become `{description, name, scopes: selectedScopes}` (sort-keys already satisfied).
- Delete mutation call becomes `deleteCustomRoleMutation.mutate({id: String(customRole.id)})`.
- Delete the `isOwnedHere` helper and its Javadoc; render the edit/delete button group unconditionally in the actions cell.
- Delete the `Scope` `TableHead` and the `'One workspace' : 'All workspaces'` `TableCell`.
- Replace the ternary header copy with the single sentence: `Roles apply to every workspace in the tenant and can be assigned to any member.`

- [ ] **Step 8: Simplify `GlobalCustomRoles.tsx`**

```tsx
import CustomRolesManager from '@/ee/shared/components/custom-roles/CustomRolesManager';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

/**
 * Custom roles are tenant-global: defined once here, assignable in every workspace. The route's ROLE_ADMIN gate
 * matches the server's isTenantAdmin() check on every mutation — the server's check is the one that counts.
 */
const GlobalCustomRoles = () => (
    <LayoutContainer header={<Header centerTitle position="main" title="Custom Roles" />} leftSidebarOpen={false}>
        <CustomRolesManager />
    </LayoutContainer>
);

export default GlobalCustomRoles;
```

- [ ] **Step 9: Delete the workspace page and unwire it**

```bash
rm -rf client/src/ee/pages/settings/automation/custom-roles
```

In `client/src/routes.tsx`:
- Delete the lazy import line: `const CustomRoles = lazy(() => import('@/ee/pages/settings/automation/custom-roles/CustomRoles'));` (~line 135).
- Delete the whole route object with `path: 'custom-roles'` — the block beginning with the comment `// Same gate as Users, and for the same reason…` through its closing `},` (~lines 219–232).
- Delete the sidebar nav entry `{href: 'custom-roles', title: 'Custom Roles'},` under the Current Workspace group (~lines 294–297).
- In the `GlobalCustomRoles` route's comment (~line 350), delete the paragraph beginning `// Distinct from the workspace page's 'custom-roles'…` (the shadowing concern is gone); keep the first line about tenant-wide management.
- Leave `path: 'global-custom-roles'` and its nav entry `{href: 'global-custom-roles', title: 'Custom Roles'}` unchanged — renaming the URL buys nothing and would churn `Settings.test.ts`.

`client/src/shared/layout/Settings.tsx` (~line 22): in the comment above `isNavItemCurrent`, drop the parenthetical `(likewise \`custom-roles\` inside \`global-custom-roles\`)` — the `users`/`workspace-users` example carries the point alone. `Settings.test.ts` itself needs no change (it tests the pure function).

- [ ] **Step 10: Clean the `WorkspaceUsers.test.tsx` mock rows**

Remove the `workspaceId: '7'` property from the two hoisted `customRoles` mock rows (~lines 12 and 85) — the field no longer exists on the query result type.

- [ ] **Step 11: Verify GREEN, then full client check**

```bash
cd client && npx vitest run src/ee/shared/components/custom-roles src/ee/pages/settings/platform/custom-roles src/ee/pages/settings/automation/users src/shared/layout
```

Expected: PASS.

```bash
cd client && npm run format && npm run check
```

Expected: exit 0 (lint + typecheck + tests). Typecheck is the real net here: it catches any remaining `workspaceId` reads on generated types anywhere in the client.

- [ ] **Step 12: Commit**

```bash
git add client/src
git commit -m "1051 client - Collapse custom roles to the tenant settings page

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

(Verify with `git status` first that the staged set is exactly this task's files — routes, layout comment, manager, page, deleted directory, tests. `npm run format` may have reformatted only touched files; do not stage unrelated drift.)

---

### Task 4: Full verification sweep

**Files:** none created — verification only (plus a formatting commit if spotless moved anything).

**Interfaces:**
- Consumes: all previous tasks.
- Produces: evidence the tree is green — required before reporting completion.

- [ ] **Step 1: Server-wide compile**

```bash
./gradlew compileJava compileTestJava --continue > /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task4-compile.log 2>&1
echo $?
grep "^> Task .* FAILED" /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task4-compile.log
```

Expected: exit 0, no FAILED lines. This catches any caller of the old signatures outside the automation-configuration modules (none are known, but the compiler is the proof).

- [ ] **Step 2: Module tests + integration tests**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:test :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration --continue > /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task4-tests.log 2>&1
echo $?
grep "^> Task .* FAILED" /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task4-tests.log
```

Expected: exit 0 (Docker required for `testIntegration`).

- [ ] **Step 3: Spotless + client check**

```bash
./gradlew spotlessApply > /private/tmp/claude-502/-Volumes-Data-bytechef-bytechef--claude-worktrees-custom-roles-per-workspace-9fb08b/c5ac4f9f-77e3-4e0f-9c70-5d6ac0589b71/scratchpad/task4-spotless.log 2>&1
echo $?
git status --porcelain
```

```bash
cd client && npm run check
```

Expected: spotless exit 0; if `git status` shows reformatted files from this feature's tree, commit them as `1051 Apply spotless formatting`; `npm run check` exit 0.

- [ ] **Step 4: Grep for stragglers**

```bash
grep -rn "findAllVisibleToWorkspace\|validateRoleTier\|toWorkspaceId" server/ee/libs/automation/automation-configuration --include="*.java"
grep -rn "workspaceId" client/src/ee/shared/components/custom-roles client/src/graphql/automation/configuration/createCustomRole.graphql client/src/graphql/automation/configuration/updateCustomRole.graphql client/src/graphql/automation/configuration/deleteCustomRole.graphql
grep -rn "custom-roles" client/src/routes.tsx
```

Expected: first two return nothing; third returns only the `global-custom-roles` route/nav lines and the `GlobalCustomRoles` import path.
