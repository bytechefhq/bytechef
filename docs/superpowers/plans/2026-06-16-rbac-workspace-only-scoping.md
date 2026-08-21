# RBAC Workspace-Only Scoping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse the RBAC permission model (ticket 1051) from two scope levels (PROJECT + WORKSPACE) down to **workspace-only**: remove project membership/roles entirely, and re-anchor the fine-grained `PermissionScope` model and custom roles to workspace membership.

**Architecture:** The current design evaluates fine-grained scopes per-project (`hasProjectScope(projectId, scope)`), resolving them from a per-project `ProjectUser` role (built-in `ProjectRole` via `BuiltInRoleScopes`, or a custom role) through a `(userId, projectId)`-keyed cache. We move the *primitive* to `hasWorkspaceScope(workspaceId, scope)`, re-key `BuiltInRoleScopes` to `WorkspaceRole`, move custom-role assignment from `ProjectUser` to `WorkspaceUser`, key the scope cache on `(userId, workspaceId)`, and delete the entire project-membership surface (domain, repo, service, GraphQL, events, audit, client UI, Liquibase). Resources are project-bound, so the handful of project-keyed call sites resolve the owning `workspaceId` from the project before checking.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Spring Security `@PreAuthorize` + `PermissionEvaluator`, Liquibase (EE changelogs), Caffeine cache, GraphQL (DGS/Spring-GraphQL), React 19 + TypeScript client, JUnit 5 / Mockito / Testcontainers.

---

## Decisions (locked with user)

1. **Scope boundary = RBAC (ticket 1051) only.** Connection-visibility PROJECT level (ticket 4750) was already removed by another session (HEAD `58ee5b10e09`); `ConnectionVisibility.PROJECT` / `ProjectConnection` are gone and `ProjectMembershipAccessor.filterByMembership` now has **zero production callers** — it is removed here as dead project-membership plumbing.
2. **Custom roles are KEPT and re-anchored to workspace.** `WorkspaceUser` gains a `customRoleId` (XOR with `workspaceRole`, mirroring today's `ProjectUser`). The `custom_role` / `custom_role_scope` tables, `CustomRole`, `CustomRoleService`, and `CustomRoleScopeResolver` (already keyed on `customRoleId`) survive.
3. **`WorkspaceRole` has no `OPERATOR` tier** (only `ADMIN`/`EDITOR`/`VIEWER`). The current `ProjectRole.OPERATOR` scope delta (`WORKFLOW_TOGGLE`, `EXECUTION_DATA`, `EXECUTION_RETRY`, `CONNECTION_USE`, `AGENT_EXECUTE`) folds into **`EDITOR`**. `VIEWER` stays read-only; `ADMIN` keeps `EnumSet.allOf`.
4. **`PermissionScope` enum:** remove the project-membership scopes `PROJECT_VIEW_USERS` and `PROJECT_MANAGE_USERS` (project membership no longer exists; workspace-user management is gated by `WorkspaceRole=ADMIN`). Keep `PROJECT_SETTINGS` and `PROJECT_DELETE` (projects remain as containers; these become workspace-evaluated, ADMIN-only via `allOf`). `PermissionScope` is persisted **by name** in `custom_role_scope` (its Javadoc states reorders/additions are non-breaking), so removing two constants is safe — any orphaned `custom_role_scope` row simply fails-closed at resolution. This is a dev branch; no prod rows exist.
5. **Liquibase = edit-in-place** (this is an unreleased April-2026 changelog batch on feature branch `0_732`, the branch's established refactor pattern). Delete the `project_user` create / custom-role-column / backfill changelogs, add a `workspace_user.custom_role_id` changelog, trim the backfill to the workspace-role promote only. **Dev DBs must be reset** (`docker compose -f server/docker-compose.dev.infra.yml down -v`) after pulling — document in the final commit message. Remember to delete stale copies from `build/resources/` per CLAUDE.md.
6. **Naming:** `ProjectScopeCacheService` → `WorkspaceScopeCacheService`; `ProjectWorkspacePermissionEvaluator` keeps its class name (it still evaluates workspace + user + tenant targets) but drops the `PROJECT_SCOPE`/`PROJECT_ROLE` branches and gains a `WORKSPACE_SCOPE` branch. `PermissionService.UserProjectPair` → `UserWorkspacePair`.

## Constraints / ground rules

- **Branch `0_732` churns and the user commits in parallel.** Per memory: NEVER `git commit --amend`; always fresh commits; only stage files this task touched. Unrelated dirty files currently in the tree (AI-gateway controllers, a webhook test) must NOT be staged.
- Server commit messages: `1051 <description>`. Client: `1051 client - <description>`.
- EE files (`server/ee/**`) keep the ByteChef Enterprise license header + `@version ee` Javadoc; CE files keep Apache header. Spotless picks header by content (`@version ee`), so preserve the tag on every EE file you touch/create.
- Run `./gradlew spotlessApply` before each server commit; `npm run check` (in `client/`) before each client commit.
- Blank-line-before-control-statement and other Java style rules from CLAUDE.md apply.

## Build-order note (compilation)

This is a coordinated removal+re-anchor; the source tree does **not** compile cleanly at every intermediate sub-step. Phases are ordered so the **server tree compiles at the end of Phase 6**, the **client at the end of Phase 8**. Commit per phase for reviewable history; run the full `./gradlew :…:compileJava` gate at the Phase 6 boundary, not after each file. If you prefer guaranteed-green commits, squash Phases 1–6 into one server commit.

---

## File Structure

### Modified — `automation-configuration-api` (CE, `server/libs/.../automation-configuration-api`)
- `service/PermissionService.java` — drop project methods, add workspace-scope methods, rename pair record.
- Delete `security/constant/ProjectRoleType.java` (marker interface; no longer referenced).
- Delete `service/ProjectMembershipAccessor.java` (dead).
- Delete `event/ProjectCreatedEvent.java` (only consumer was EE project-membership seeding).

### Modified — `automation-configuration-service` (CE)
- `service/PermissionServiceImpl.java` — CE no-op: drop project methods, add workspace-scope no-ops.
- `security/ProjectWorkspacePermissionEvaluator.java` — drop `PROJECT_SCOPE`/`PROJECT_ROLE`, add `WORKSPACE_SCOPE`.
- `security/AutomationMethodSecurityConfiguration.java` — verify bean wiring still compiles (no signature change expected; read & confirm).
- Delete `service/DefaultProjectMembershipAccessor.java` (dead).
- `service/ProjectServiceImpl.java`, `facade/ProjectFacadeImpl.java`, `facade/WorkspaceFacadeImpl.java`, `facade/ProjectDeploymentFacadeImpl.java`, `service/ProjectDeploymentServiceImpl.java`, `service/ProjectWorkflowServiceImpl.java`, `facade/WorkspaceConnectionFacadeImpl.java` — migrate any `'ProjectScope'`/`hasProjectScope`/`hasProjectRole` `@PreAuthorize` or inline checks to workspace-scope (resolve `workspaceId` from `projectId`); stop publishing `ProjectCreatedEvent` if applicable.

### Modified — `automation-configuration-api` (EE, `server/ee/.../automation-configuration-api`)
- `domain/WorkspaceUser.java` — add `customRoleId`, adopt XOR invariant + `forCustomRole` factory (mirror `ProjectUser`).
- `security/constant/BuiltInRoleScopes.java` — re-key map to `WorkspaceRole`; fold OPERATOR into EDITOR.
- `security/constant/PermissionScope.java` — remove `PROJECT_VIEW_USERS`, `PROJECT_MANAGE_USERS`; fix Javadoc example to `'WorkspaceScope'`.
- `service/WorkspaceUserService.java` — add assign-custom-role / update-role / member-scopes methods (mirror `ProjectUserService`).
- Delete `domain/ProjectUser.java`, `service/ProjectUserService.java`, `security/constant/ProjectRole.java`.

### Modified/Deleted — `automation-configuration-service` (EE)
- `service/PermissionServiceImpl.java` — drop project methods/fields, add `hasWorkspaceScope`/`getMyWorkspaceScopes` backed by `WorkspaceScopeCacheService`; remove `parseProjectRole`/`toProjectRole`/`ProjectUserRepository`.
- Rename `service/ProjectScopeCacheService.java` → `service/WorkspaceScopeCacheService.java` — key on `(userId, workspaceId)`, dispatch on `WorkspaceUser`.
- `service/WorkspaceUserServiceImpl.java` — implement custom-role assignment + role update + scopes; evict workspace-scope cache; remove the `evictProjectScopeCache` loop.
- `service/CustomRoleServiceImpl.java` — `evictAllProjectScopeCache()` → `evictAllWorkspaceScopeCache()`; change the "still referenced by a project_user" guard to reference `workspace_user`.
- `service/CustomRoleScopeResolverImpl.java` — unchanged logic (keyed on `customRoleId`); only fix any `ProjectScopeCacheService`/comment references.
- `service/WorkspaceServiceImpl.java` — `evictProjectScopeCaches` over `(member × project)` → `evictWorkspaceScopeCaches` over workspace members; update comment.
- `repository/WorkspaceUserRepository.java` — add the queries the new `WorkspaceUserServiceImpl` needs (mirror `ProjectUserRepository`: count-by-role, find-by-workspace, etc.).
- Delete: `repository/ProjectUserRepository.java`, `service/ProjectUserServiceImpl.java`, `service/ProjectMembershipAccessorImpl.java`, `event/ProjectCreatedEventListener.java`, `audit/ProjectUserAuditEvent.java`, `audit/ProjectUserAuditPublisher.java`, `exception/ProjectUserErrorType.java`.

### Modified/Deleted — `automation-configuration-graphql` (EE)
- Delete `web/graphql/ProjectUserGraphQlController.java`.
- `web/graphql/WorkspaceUserGraphQlController.java` — add the migrated mutations/queries (assign custom role, update workspace role, `myWorkspaceScopes`), gated by `hasPermission(#workspaceId, 'WorkspaceRole', 'ADMIN')`.
- `.graphqls` schema file(s) — remove project-user types/operations; add workspace equivalents.

### Modified/Deleted — `automation-configuration-remote-client` (EE)
- Delete `remote/client/service/RemoteProjectUserServiceClient.java`.
- `remote/client/service/RemotePermissionServiceClient.java` — drop project methods, add workspace-scope stubs.

### Modified — `ai-copilot` (EE)
- `ai-copilot-rest/.../CopilotApiController.java`, `ai-copilot-graphql/.../{WorkflowDescription,Property,SampleOutput}CopilotGraphQlController.java` — `hasProjectScope(projectId, …)` → resolve `workspaceId` from project, `hasWorkspaceScope(workspaceId, …)`.

### Liquibase (EE `automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/`)
- Delete `202604061200020_…_added_table_project_user.xml`.
- Delete `202604061200040_…_added_column_project_user_custom_role.xml`.
- Delete `202604061200050_…_backfill_project_user.xml` (or trim to workspace-only — see Task 16).
- Create `202604061200040_…_added_column_workspace_user_custom_role.xml`.
- Keep `…010` (workspace_role col), `…030` (custom_role tables), `…060` (workspace_user unique constraint).

### Client (`client/src`)
- Delete: `shared/hooks/useLoadProjectPermissions.ts` (+ test), `shared/hooks/useHasProjectScope.ts` (+ test), `graphql/automation/configuration/{updateProjectUserRole,projectUsers,addProjectUser,removeProjectUser}.graphql`, `pages/automation/project/components/project-header/components/settings-menu/components/ProjectUsersDialog.tsx`.
- Modify: `shared/stores/usePermissionStore.ts` (+ test) — drop project-scope state; `pages/automation/project/.../settings-menu/SettingsMenu.tsx` — remove "Project Users" entry; add/extend workspace-user management + `useHasWorkspaceScope` if the UI needs fine-grained gating.
- Regenerate `src/shared/middleware/graphql.ts` via `npx graphql-codegen`.

### Tests
- Delete EE: `ProjectUserServiceTest`, `ProjectUserRepositoryIntTest`, `ProjectScopeCacheServiceTest` (→ rewrite as `WorkspaceScopeCacheServiceTest`), `ProjectMembershipAccessorImplTest`, `ProjectCreatedEventListenerTest`, `RbacBackfillMigrationIntTest` (→ trim), and the project parts of `PermissionServiceTest`/`CustomRoleServiceTest`/`WorkspaceUserServiceTest`.
- Update `EnumOrdinalPinTest` / `EnumOrdinalStabilityTest`: drop `ProjectRole` pins; keep `WorkspaceRole`/`PermissionScope` pins (PermissionScope is name-persisted, so its pin test, if ordinal-based, must be reconciled to the new constant list).
- Update CE `PermissionServiceTest`, `ProjectWorkspacePermissionEvaluatorTest`, `PermissionEvaluatorWiringIntTest`, `AutomationMethodSecurityConfigurationTest`, `PreAuthorizeAnnotationTest`, `PreAuthorizeProxyEnforcementIntTest`, `RealImplProxyEnforcementIntTest`.

---

## Phase 1 — Re-anchor the scope model to workspace (EE core)

### Task 1: `BuiltInRoleScopes` keyed on `WorkspaceRole`

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/security/constant/BuiltInRoleScopes.java`

- [ ] **Step 1:** Replace the class body so the map is keyed on `WorkspaceRole` and OPERATOR's delta folds into EDITOR. Remove the `PROJECT_VIEW_USERS` import (that scope is being deleted in Task 3). Complete replacement of the `import`s for `ProjectRole`/`PROJECT_VIEW_USERS` and the `static` block + `getScopesForRole` signature:

```java
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_CREATE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_EDIT;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_EXECUTE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_VIEW;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_CREATE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_DELETE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_EDIT;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_USE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_VIEW;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.DEPLOYMENT_PULL;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.DEPLOYMENT_PUSH;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.EXECUTION_DATA;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.EXECUTION_RETRY;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.EXECUTION_VIEW;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_CREATE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_DELETE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_EDIT;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_TOGGLE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_VIEW;
```

```java
public final class BuiltInRoleScopes {

    private static final Map<WorkspaceRole, Set<PermissionScope>> ROLE_SCOPES;

    static {
        // VIEWER: read-only across all artifact types.
        EnumSet<PermissionScope> viewer = EnumSet.of(
            WORKFLOW_VIEW,
            EXECUTION_VIEW,
            CONNECTION_VIEW,
            AGENT_VIEW);

        // EDITOR absorbs the former project OPERATOR tier (toggle/run/use/execute) PLUS create/edit/delete and
        // deployment push/pull. WorkspaceRole has no OPERATOR, so "operate but not edit" is no longer a distinct
        // tier — operate capabilities live with EDITOR.
        EnumSet<PermissionScope> editor = EnumSet.copyOf(viewer);

        editor.addAll(EnumSet.of(
            WORKFLOW_TOGGLE,
            EXECUTION_DATA, EXECUTION_RETRY,
            CONNECTION_USE,
            AGENT_EXECUTE,
            WORKFLOW_CREATE, WORKFLOW_EDIT, WORKFLOW_DELETE,
            CONNECTION_CREATE, CONNECTION_EDIT, CONNECTION_DELETE,
            AGENT_CREATE, AGENT_EDIT,
            DEPLOYMENT_PUSH, DEPLOYMENT_PULL));

        EnumMap<WorkspaceRole, Set<PermissionScope>> map = new EnumMap<>(WorkspaceRole.class);

        map.put(WorkspaceRole.VIEWER, Collections.unmodifiableSet(viewer));
        map.put(WorkspaceRole.EDITOR, Collections.unmodifiableSet(editor));
        map.put(WorkspaceRole.ADMIN, Collections.unmodifiableSet(EnumSet.allOf(PermissionScope.class)));

        ROLE_SCOPES = Collections.unmodifiableMap(map);
    }

    public static Set<PermissionScope> getScopesForRole(WorkspaceRole role) {
        Objects.requireNonNull(role, "role must not be null");

        Set<PermissionScope> scopes = ROLE_SCOPES.get(role);

        if (scopes == null) {
            throw new IllegalStateException(
                "No scope mapping defined for WorkspaceRole." + role.name()
                    + " — update BuiltInRoleScopes.ROLE_SCOPES");
        }

        return Collections.unmodifiableSet(scopes);
    }

    private BuiltInRoleScopes() {
    }
}
```

- [ ] **Step 2:** Update the class Javadoc to describe `WorkspaceRole` (VIEWER ⊂ EDITOR ⊂ ADMIN) and note the OPERATOR fold.

### Task 2: `PermissionScope` — remove project-membership scopes

**Files:**
- Modify: `server/ee/.../security/constant/PermissionScope.java`

- [ ] **Step 1:** Delete the two enum constants `PROJECT_VIEW_USERS` and `PROJECT_MANAGE_USERS` from the `// Project management` group (keep the group's remaining constant `PROJECT_SETTINGS`). Keep `PROJECT_DELETE`. Persistence is by name, so this is safe; reordering is explicitly allowed by the class Javadoc.
- [ ] **Step 2:** In the class Javadoc, change the example `@PreAuthorize("hasPermission(#projectId, 'ProjectScope', 'SCOPE_NAME')")` to `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceScope', 'SCOPE_NAME')")`.

### Task 3: `WorkspaceUser` — add `customRoleId` with XOR invariant

**Files:**
- Modify: `server/ee/.../domain/WorkspaceUser.java`
- Reference (mirror): `server/ee/.../domain/ProjectUser.java` (before deletion in Phase 5)

- [ ] **Step 1:** Add the field and factories, mirroring `ProjectUser`'s XOR design. Add after the `workspaceRole` field:

```java
    @Column("custom_role_id")
    private Long customRoleId;
```

- [ ] **Step 2:** Replace the two existing public constructors + `forRole` with an XOR-aware set (built-in role XOR custom role). Keep the no-arg constructor for Spring Data JDBC. Mirror `ProjectUser.forBuiltInRole`/`forCustomRole`/the package-private validating constructor exactly, swapping `project`→`workspace` and `ProjectRole`→`WorkspaceRole`:

```java
    /** Creates a workspace membership with a built-in {@link WorkspaceRole}. */
    public static WorkspaceUser forRole(Long userId, Long workspaceId, WorkspaceRole workspaceRole) {
        Assert.notNull(workspaceRole, "'workspaceRole' must not be null");

        return new WorkspaceUser(userId, workspaceId, workspaceRole.ordinal(), null);
    }

    /** Creates a workspace membership with a custom role (no built-in {@code workspaceRole}). */
    public static WorkspaceUser forCustomRole(Long userId, Long workspaceId, long customRoleId) {
        Assert.isTrue(customRoleId > 0, "'customRoleId' must be positive");

        return new WorkspaceUser(userId, workspaceId, null, customRoleId);
    }

    WorkspaceUser(Long userId, Long workspaceId, Integer workspaceRole, Long customRoleId) {
        Assert.notNull(userId, "'userId' must not be null");
        Assert.notNull(workspaceId, "'workspaceId' must not be null");

        boolean hasBuiltInRole = workspaceRole != null;
        boolean hasCustomRole = customRoleId != null;

        Assert.isTrue(
            hasBuiltInRole ^ hasCustomRole,
            "Exactly one of 'workspaceRole' or 'customRoleId' must be set (XOR invariant)");

        if (hasBuiltInRole) {
            WorkspaceRole[] values = WorkspaceRole.values();

            Assert.isTrue(
                workspaceRole >= 0 && workspaceRole < values.length,
                "'workspaceRole' ordinal " + workspaceRole + " is out of range [0," + values.length + ")");
        }

        this.userId = userId;
        this.workspaceId = workspaceId;
        this.workspaceRole = workspaceRole;
        this.customRoleId = customRoleId;
    }
```

- [ ] **Step 3:** Keep the existing raw-ordinal public `WorkspaceUser(Long, Long, int)` constructor **only if** other callers rely on it (grep `new WorkspaceUser(`). Otherwise route it through the new XOR constructor: `this(userId, workspaceId, workspaceRole, null);`. Add `getCustomRoleId()`, `assignBuiltInRole(WorkspaceRole)` (sets role, nulls customRoleId), and `assignCustomRole(long)` (sets customRoleId, nulls role) — mirror `ProjectUser`. Update `equals` is unaffected; extend `toString` with `customRoleId`.

- [ ] **Step 4:** Run `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-api:compileJava` — expect FAIL only on downstream not-yet-updated EE service refs (acceptable until Phase 5); the api module itself should compile.

### Task 4: `PermissionService` interface — workspace-scope methods

**Files:**
- Modify: `server/libs/.../automation-configuration-api/.../service/PermissionService.java`

- [ ] **Step 1:** Remove the `ProjectRoleType` import and the project-oriented members: `hasProjectScope`, `hasProjectRole`, `getMyProjectScopes`, `evictProjectScopeCache`, `evictProjectScopeCaches`, `evictAllProjectScopeCache`, `UserProjectPair`, `hasProjectScopeTyped`, `hasProjectRoleTyped`.
- [ ] **Step 2:** Add the workspace-scope primitives + typed variant + workspace cache eviction. Insert after `hasWorkspaceRole`:

```java
    /**
     * Returns {@code true} if the current user has the given permission scope in the workspace. Scopes are resolved
     * from the user's workspace role via {@code BuiltInRoleScopes}, or from a custom role (EE). Results are cached per
     * (userId, workspaceId). Tenant admins always return {@code true}.
     *
     * @param scope a {@code com.bytechef.ee.automation.configuration.security.constant.PermissionScope} name
     */
    boolean hasWorkspaceScope(long workspaceId, String scope);

    /** Returns the set of permission scope names the current user has in the given workspace. */
    Set<String> getMyWorkspaceScopes(long workspaceId);

    /** Evicts the cached permission scopes for a specific (userId, workspaceId) pair. */
    void evictWorkspaceScopeCache(long userId, long workspaceId);

    /** Batch variant of {@link #evictWorkspaceScopeCache(long, long)}. CE is a no-op. */
    default void evictWorkspaceScopeCaches(Collection<UserWorkspacePair> userWorkspacePairs) {
        for (UserWorkspacePair pair : userWorkspacePairs) {
            evictWorkspaceScopeCache(pair.userId(), pair.workspaceId());
        }
    }

    /** Evicts the entire workspace scopes cache for all users. */
    void evictAllWorkspaceScopeCache();

    /** Identifies a single {@code (userId, workspaceId)} entry in the workspace scopes cache. */
    record UserWorkspacePair(long userId, long workspaceId) {
    }
```

- [ ] **Step 3:** Replace the typed `hasProjectScopeTyped` block with a workspace variant:

```java
    /**
     * Typed variant of {@link #hasWorkspaceScope(long, String)}. See {@link #hasWorkspaceRoleTyped(long, Enum)} for the
     * rationale behind both the generic {@code Enum<E> & ...} bound and the distinct method name.
     */
    default <E extends Enum<E> & PermissionScopeType> boolean hasWorkspaceScopeTyped(long workspaceId, E scope) {
        Objects.requireNonNull(scope, "'scope' must not be null");

        return hasWorkspaceScope(workspaceId, scope.name());
    }
```

- [ ] **Step 4:** Update the class Javadoc example from `'ProjectScope', 'WORKFLOW_EDIT'` to `'WorkspaceScope', 'WORKFLOW_EDIT'` and drop the "Workspace and project roles form separate hierarchies" sentence.

### Task 5: Delete CE `ProjectRoleType`, `ProjectMembershipAccessor`, `ProjectCreatedEvent`

**Files:**
- Delete: `server/libs/.../automation-configuration-api/.../security/constant/ProjectRoleType.java`
- Delete: `server/libs/.../automation-configuration-api/.../service/ProjectMembershipAccessor.java`
- Delete: `server/libs/.../automation-configuration-api/.../event/ProjectCreatedEvent.java`

- [ ] **Step 1:** Confirm no remaining production references: `grep -rn "ProjectRoleType\|ProjectMembershipAccessor\|ProjectCreatedEvent" server/libs server/ee --include=*.java | grep -v /build/`. The only hits should be files this plan deletes/edits.
- [ ] **Step 2:** Delete the three files.

### Task 6: Commit Phase 1 core API

- [ ] **Step 1:** `./gradlew spotlessApply`
- [ ] **Step 2:**
```bash
git add server/ee/.../BuiltInRoleScopes.java server/ee/.../PermissionScope.java \
        server/ee/.../domain/WorkspaceUser.java server/libs/.../service/PermissionService.java
git rm server/libs/.../ProjectRoleType.java server/libs/.../ProjectMembershipAccessor.java \
       server/libs/.../event/ProjectCreatedEvent.java
git commit -m "1051 Re-anchor RBAC permission model to workspace scope"
```
(Use exact paths from the File Structure section.)

---

## Phase 2 — Workspace scope cache + EE PermissionService

### Task 7: Rename `ProjectScopeCacheService` → `WorkspaceScopeCacheService`

**Files:**
- Create (from rename): `server/ee/.../service/WorkspaceScopeCacheService.java`
- Delete: `server/ee/.../service/ProjectScopeCacheService.java`

- [ ] **Step 1:** Copy `ProjectScopeCacheService.java` to `WorkspaceScopeCacheService.java`. Apply these transforms throughout:
  - Class name, cache constant `PROJECT_SCOPES_CACHE`→`WORKSPACE_SCOPES_CACHE` value `"workspaceScopes"`, self-type, all method names `*ProjectScope*`→`*WorkspaceScope*`.
  - Constructor dep `ProjectUserRepository projectUserRepository` → `WorkspaceUserRepository workspaceUserRepository`.
  - `getProjectScopes(long userId, long projectId)` → `getWorkspaceScopes(long userId, long workspaceId)` using `workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)`.
  - `dispatchScopes(WorkspaceUser …)`: branch on `getWorkspaceRole()` (built-in) vs `getCustomRoleId()` (custom); the corruption message references `chk_workspace_user_role_xor`.
  - `resolveBuiltInScopes` uses `WorkspaceRole.values()` and `BuiltInRoleScopes.getScopesForRole(values[ordinal])`.
  - `import ...PermissionService.UserProjectPair` → `UserWorkspacePair`; `evictProjectScopeCaches(Collection<UserProjectPair>)` → `evictWorkspaceScopeCaches(Collection<UserWorkspacePair>)`; pair accessors `projectId()`→`workspaceId()`.
  - Keep the `@version ee` tag and EE header.
- [ ] **Step 2:** Delete `ProjectScopeCacheService.java`.

### Task 8: EE `PermissionServiceImpl` — workspace scope, drop project

**Files:**
- Modify: `server/ee/.../service/PermissionServiceImpl.java`

- [ ] **Step 1:** Remove imports/fields for `ProjectUser`, `ProjectUserRepository`, `ProjectRole`. Replace the `projectScopeCacheService` field with `workspaceScopeCacheService` (type `WorkspaceScopeCacheService`); drop `projectUserRepository`. Update the constructor signature accordingly.
- [ ] **Step 2:** In `checkErrorCounters`, replace the `hasProjectScope`/`hasProjectRole`/`getMyProjectScopes` counter entries with `hasWorkspaceScope`/`getMyWorkspaceScopes` (keep `hasWorkspaceRole`/`getMyWorkspaceRole`).
- [ ] **Step 3:** Delete `hasProjectScope`, `hasProjectRole`, `getMyProjectScopes`, `evictProjectScopeCache`, `evictProjectScopeCaches`, `evictAllProjectScopeCache`, `parseProjectRole`, `toProjectRole`. Add:

```java
    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        if (isTenantAdmin()) {
            return true;
        }

        Optional<PermissionScope> required = parsePermissionScope(scope);

        if (required.isEmpty()) {
            return false;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Set<PermissionScope> scopes = withCheckErrorCounter("hasWorkspaceScope",
            () -> workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId));

        return scopes.contains(required.get());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        if (isTenantAdmin()) {
            return EnumSet.allOf(PermissionScope.class)
                .stream()
                .map(PermissionScope::name)
                .collect(Collectors.toSet());
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return Collections.emptySet();
        }

        return withCheckErrorCounter("getMyWorkspaceScopes",
            () -> workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId)
                .stream()
                .map(PermissionScope::name)
                .collect(Collectors.toSet()));
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        workspaceScopeCacheService.evictWorkspaceScopeCache(userId, workspaceId);
    }

    @Override
    public void evictWorkspaceScopeCaches(Collection<UserWorkspacePair> userWorkspacePairs) {
        workspaceScopeCacheService.evictWorkspaceScopeCaches(userWorkspacePairs);
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
        workspaceScopeCacheService.evictAllWorkspaceScopeCache();
    }
```

- [ ] **Step 4:** `hasWorkspaceRole`/`getMyWorkspaceRole`/`isTenantAdmin`/`isCurrentUser`/`parseWorkspaceRole`/`toWorkspaceRole`/`parsePermissionScope`/`withCheckErrorCounter` stay unchanged.

### Task 9: `WorkspaceScopeCacheService` cache registration

**Files:**
- Inspect: `grep -rln "projectScopes\|CacheConfiguration\|@Cacheable" server/ee server/libs --include=*.java | grep -v /build/` — find any `CacheConfiguration` that enumerates cache names.

- [ ] **Step 1:** If a cache-names list registers `"projectScopes"`, rename it to `"workspaceScopes"`. If caches are created on demand (Caffeine default), no change — note that in the commit.

### Task 10: Commit Phase 2

- [ ] **Step 1:** `./gradlew spotlessApply`
- [ ] **Step 2:** `git add`/`git rm` the renamed cache service + EE `PermissionServiceImpl` (+ any cache config); `git commit -m "1051 Key permission scope cache and EE checks on workspace"`.

---

## Phase 3 — Migrate call sites (project-scope → workspace-scope)

For each site below, the check currently passes a `projectId`. Resolve the owning workspace with the existing project read path. Determine the exact accessor by reading the file: a `Project` carries `workspaceId` (e.g. `projectService.getProject(projectId).getWorkspaceId()`); use whichever project service/facade the class already injects. If none is injected, inject `ProjectService` (CE api interface `com.bytechef.automation.configuration.service.ProjectService`).

### Task 11: AI Copilot scope checks

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/.../CopilotApiController.java:166`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/.../WorkflowDescriptionCopilotGraphQlController.java:65`
- Modify: `.../PropertyCopilotGraphQlController.java:64`
- Modify: `.../SampleOutputCopilotGraphQlController.java:63`

- [ ] **Step 1:** In each, read the method, obtain `workspaceId` from the `projectId` already in scope (inject `ProjectService` if needed), and replace `permissionService.hasProjectScope(projectId, X)` with `permissionService.hasWorkspaceScope(workspaceId, X)`. The `WORKFLOW_VIEW_SCOPE` / `WORKFLOW_EDIT` literals are unchanged.
- [ ] **Step 2:** Add the one-line blank-before-control-statement style and keep imports tidy.

### Task 12: automation-configuration call sites

**Files (read each; migrate only the project-scope checks):**
- `server/libs/.../service/ProjectServiceImpl.java`
- `server/libs/.../facade/ProjectFacadeImpl.java`
- `server/libs/.../facade/WorkspaceFacadeImpl.java`
- `server/libs/.../facade/ProjectDeploymentFacadeImpl.java`
- `server/libs/.../service/ProjectDeploymentServiceImpl.java`
- `server/libs/.../service/ProjectWorkflowServiceImpl.java`
- `server/libs/.../facade/WorkspaceConnectionFacadeImpl.java`
- `server/libs/.../web/graphql/ConnectionGraphQlController.java`

- [ ] **Step 1:** `grep -n "ProjectScope\|hasProjectScope\|hasProjectRole\|'ProjectRole'\|ProjectCreatedEvent" <file>` in each. For `@PreAuthorize("hasPermission(#projectId, 'ProjectScope', 'X')")`: if the method already has a `workspaceId` param, switch to `hasPermission(#workspaceId, 'WorkspaceScope', 'X')`. If only `projectId` is available and SpEL property navigation would be fragile, convert to an inline Java check at the top of the method: resolve `workspaceId`, then `if (!permissionService.hasWorkspaceScope(workspaceId, "X")) { throw <the module's standard access-denied> ; }` matching the surrounding code's denial idiom.
- [ ] **Step 2:** Remove any `ProjectCreatedEvent` publication (the listener that consumed it is deleted in Phase 5; project creation no longer seeds a project-admin membership). Confirm via `grep -rn "ProjectCreatedEvent" server --include=*.java | grep -v /build/` returns nothing after edits.
- [ ] **Step 3:** Note: many `@PreAuthorize` hits in the inventory are already workspace-scoped or unrelated (`CONNECTION_*` via `WorkspaceScope` does not exist yet — those use `hasWorkspaceRole`); only touch lines that literally reference `ProjectScope`/`ProjectRole`/`hasProjectScope`/`hasProjectRole`.

### Task 13: Remote clients

**Files:**
- Delete: `server/ee/.../automation-configuration-remote-client/.../service/RemoteProjectUserServiceClient.java`
- Modify: `server/ee/.../automation-configuration-remote-client/.../service/RemotePermissionServiceClient.java`

- [ ] **Step 1:** In `RemotePermissionServiceClient`, delete the `hasProjectScope`/`hasProjectRole`/`getMyProjectScopes`/`evictProjectScopeCache*`/`evictAllProjectScopeCache` overrides and add workspace equivalents (`hasWorkspaceScope`, `getMyWorkspaceScopes`, `evictWorkspaceScopeCache`, `evictAllWorkspaceScopeCache`) following the existing stub pattern (`logError("…"); return …;`). Match the throw/return convention already in the file.
- [ ] **Step 2:** Delete `RemoteProjectUserServiceClient.java`; confirm nothing references it (`grep -rn RemoteProjectUserServiceClient server --include=*.java | grep -v /build/`).

### Task 14: Commit Phase 3

- [ ] `./gradlew spotlessApply`; stage the edited call sites + remote-client changes; `git commit -m "1051 Migrate permission checks to workspace scope"`.

---

## Phase 4 — Workspace user service + GraphQL (custom-role assignment moves here)

### Task 15: `WorkspaceUserService` / `WorkspaceUserServiceImpl` / repository

**Files:**
- Modify: `server/ee/.../automation-configuration-api/.../service/WorkspaceUserService.java`
- Modify: `server/ee/.../automation-configuration-service/.../service/WorkspaceUserServiceImpl.java`
- Modify: `server/ee/.../automation-configuration-service/.../repository/WorkspaceUserRepository.java`
- Reference (mirror, then delete in Phase 5): `ProjectUserServiceImpl.java`, `ProjectUserRepository.java`

- [ ] **Step 1:** Read `ProjectUserServiceImpl` and `ProjectUserService` end-to-end. Port the membership-management surface from project to workspace: assign built-in role, assign custom role, update role, list members, remove member, last-admin / orphan-recovery guard (`validateNotLastEffectiveAdmin` analog using `WorkspaceRole.ADMIN` + `custom_role` admins), audit events (reuse the workspace audit path — see Step 4). Replace `@PreAuthorize("hasPermission(#projectId, 'ProjectScope', 'PROJECT_MANAGE_USERS')")` with `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'ADMIN')")` (workspace-user management is admin-gated; there is no workspace-scope equivalent of `PROJECT_MANAGE_USERS`).
- [ ] **Step 2:** Every mutation calls `permissionService.evictWorkspaceScopeCache(userId, workspaceId)` (replacing the project-keyed eviction loop currently at `WorkspaceUserServiceImpl.java:137`).
- [ ] **Step 3:** Add to `WorkspaceUserRepository` the queries the impl needs, mirroring `ProjectUserRepository` (e.g. `countByWorkspaceIdAndWorkspaceRole`, `findByWorkspaceId`, the custom-role-reference count used by `CustomRoleServiceImpl` — see Task 16). `findByUserIdAndWorkspaceId` already exists.
- [ ] **Step 4:** Audit: if `ProjectUserAuditEvent`/`ProjectUserAuditPublisher` were the audit path, add a `WorkspaceUserAuditEvent`/publisher (mirror) OR route through the existing workspace audit aspect — check how `WorkspaceUserServiceImpl` currently audits and stay consistent. (The Project* audit classes are deleted in Phase 5.)

### Task 16: `CustomRoleServiceImpl` + `WorkspaceServiceImpl`

**Files:**
- Modify: `server/ee/.../service/CustomRoleServiceImpl.java`
- Modify: `server/ee/.../service/WorkspaceServiceImpl.java`

- [ ] **Step 1:** `CustomRoleServiceImpl.java:135` `permissionService.evictAllProjectScopeCache()` → `evictAllWorkspaceScopeCache()`. The delete-guard that refuses deletion while a `project_user` still references the role must now query `workspace_user` (use the new `WorkspaceUserRepository` count-by-custom-role method); update wording.
- [ ] **Step 2:** `WorkspaceServiceImpl` (~lines 133–136): the `(member × project)` cache-eviction target set becomes per-workspace-member eviction. Replace the `UserProjectPair` collection build + `evictProjectScopeCaches` with a `UserWorkspacePair` collection (one per workspace member) + `evictWorkspaceScopeCaches`. Update the comment.

### Task 17: GraphQL — drop project-user controller, extend workspace-user controller

**Files:**
- Delete: `server/ee/.../web/graphql/ProjectUserGraphQlController.java`
- Modify: `server/ee/.../web/graphql/WorkspaceUserGraphQlController.java`
- Modify: the EE `.graphqls` schema (find via `grep -rln "myProjectScopes\|addProjectUser\|updateProjectUserRole\|removeProjectUser\|projectUsers" server/ee --include=*.graphqls`)

- [ ] **Step 1:** Read `ProjectUserGraphQlController` and `WorkspaceUserGraphQlController`. Move the still-relevant operations to the workspace controller: `myWorkspaceScopes(workspaceId)` → `permissionService.getMyWorkspaceScopes(workspaceId)`; add/assign workspace user (built-in or custom role), update workspace role, remove workspace user — each `@PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'ADMIN')")` except the self-scopes query which uses `isAuthenticated()` (mirror how `myProjectScopes` was gated).
- [ ] **Step 2:** Delete `ProjectUserGraphQlController.java`.
- [ ] **Step 3:** Edit the `.graphqls`: remove `projectUsers`/`addProjectUser`/`updateProjectUserRole`/`removeProjectUser`/`myProjectScopes` and the `ProjectRole` enum / project-user types; add the workspace equivalents and `myWorkspaceScopes`. Enum values stay SCREAMING_SNAKE_CASE.

### Task 18: Commit Phase 4

- [ ] `./gradlew spotlessApply`; stage workspace service/repo/graphql/custom-role/workspace-service changes + schema; `git rm` ProjectUserGraphQlController; `git commit -m "1051 Move user/role management and custom-role assignment to workspace"`.

---

## Phase 5 — Delete the project-membership surface

### Task 19: Delete EE project-membership classes

**Files (delete):**
- `server/ee/.../automation-configuration-api/.../domain/ProjectUser.java`
- `server/ee/.../automation-configuration-api/.../service/ProjectUserService.java`
- `server/ee/.../automation-configuration-api/.../security/constant/ProjectRole.java`
- `server/ee/.../automation-configuration-service/.../repository/ProjectUserRepository.java`
- `server/ee/.../automation-configuration-service/.../service/ProjectUserServiceImpl.java`
- `server/ee/.../automation-configuration-service/.../service/ProjectMembershipAccessorImpl.java`
- `server/libs/.../automation-configuration-service/.../service/DefaultProjectMembershipAccessor.java`
- `server/ee/.../automation-configuration-service/.../event/ProjectCreatedEventListener.java`
- `server/ee/.../automation-configuration-service/.../audit/ProjectUserAuditEvent.java`
- `server/ee/.../automation-configuration-service/.../audit/ProjectUserAuditPublisher.java`
- `server/ee/.../automation-configuration-service/.../exception/ProjectUserErrorType.java`

- [ ] **Step 1:** `git rm` all of the above.
- [ ] **Step 2:** `grep -rn "ProjectUser\|ProjectRole\|ProjectMembershipAccessor\|ProjectCreatedEventListener\|ProjectUserAuditEvent\|ProjectUserErrorType" server/libs server/ee --include=*.java | grep -v /build/ | grep -v /test/` — must return **zero** production hits. Fix any stragglers (e.g. `ConnectionVisibilityResolverImpl` if it still imports `ProjectMembershipAccessor` — it should have been decoupled by the 4750 cleanup; if not, remove the now-no-op narrowing call).
- [ ] **Step 3:** Compile the whole automation-configuration tree:
```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava \
          :server:ee:libs:automation:automation-configuration:automation-configuration-service:compileJava \
          :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:compileJava \
          :server:ee:libs:automation:automation-configuration:automation-configuration-remote-client:compileJava
```
Expected: PASS (test sources may still fail — handled in Phase 7).

### Task 20: Commit Phase 5

- [ ] `./gradlew spotlessApply`; `git commit -m "1051 Remove project-level membership, roles, and scoping"`.

---

## Phase 6 — Liquibase

### Task 21: Add `workspace_user.custom_role_id` migration

**Files:**
- Create: `server/ee/.../config/liquibase/changelog/automation/configuration/202604061200040_automation_configuration_added_column_workspace_user_custom_role.xml`

- [ ] **Step 1:** Mirror the deleted `project_user_custom_role` migration onto `workspace_user`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="202604061200040" author="Ivica Cardic">
        <addColumn tableName="workspace_user">
            <column name="custom_role_id" type="BIGINT"/>
        </addColumn>

        <addForeignKeyConstraint
            baseTableName="workspace_user"
            baseColumnNames="custom_role_id"
            constraintName="fk_workspace_user_custom_role"
            referencedTableName="custom_role"
            referencedColumnNames="id"
            onDelete="RESTRICT"/>

        <dropNotNullConstraint tableName="workspace_user" columnName="workspace_role" columnDataType="INT"/>

        <sql>
            ALTER TABLE workspace_user ADD CONSTRAINT chk_workspace_user_role_xor
            CHECK ((workspace_role IS NOT NULL AND custom_role_id IS NULL) OR
                   (workspace_role IS NULL AND custom_role_id IS NOT NULL));
        </sql>
    </changeSet>
</databaseChangeLog>
```

Note: the `workspace_role` column was created with `defaultValueNumeric="1"` NOT NULL in `…010`; dropping NOT NULL is required for the XOR (custom-role rows have null `workspace_role`). The default remains, which is fine — explicit factories always set exactly one side.

### Task 22: Delete project_user changelogs; trim backfill

**Files:**
- Delete: `202604061200020_automation_configuration_added_table_project_user.xml`
- Delete: `202604061200040_automation_configuration_added_column_project_user_custom_role.xml` (the OLD project one — replaced by Task 21's file of the same id; ensure the new file is the only `…040`)
- Modify or delete: `202604061200050_automation_configuration_backfill_project_user.xml`

- [ ] **Step 1:** `git rm` the project_user table + project custom-role column changelogs.
- [ ] **Step 2:** Backfill: it currently (a) promotes default-EDITOR `workspace_user` rows to ADMIN and (b) seeds `project_user` ADMIN rows + an `rbac_backfill_report`. Remove part (b) (project_user no longer exists). Either delete the whole changelog (a dev branch has no pre-RBAC deployments needing the promote) or trim to the workspace-role promote + report. **Recommended: delete it** — simplest, and dev DBs are reset anyway. Rename the file out of existence with `git rm`.
- [ ] **Step 3:** Delete stale build copies so Liquibase doesn't see both old and new on the classpath:
```bash
find server -path '*build/resources*' \( -name '*project_user*' -o -name '*backfill_project_user*' \) -delete
```
- [ ] **Step 4:** Verify changelog set is internally consistent: only `…010` (workspace_role col), `…030` (custom_role/custom_role_scope), `…040` (workspace_user custom_role col — the new file), `…060` (workspace_user unique constraint) remain. The `…060` comment references "Mirrors the constraint already present on project_user" — update that comment since project_user is gone.

### Task 23: Liquibase integration smoke + commit

**Files:**
- Run the EE configuration Liquibase/migration IntTest if present (e.g. `RbacMigrationsIntTest`), trimmed in Phase 7. For now just validate XML parses by booting the changelog in a Testcontainers context if a fast path exists; otherwise defer validation to Phase 7's IntTest run.

- [ ] **Step 1:** `git add` the new changelog; `git rm` the deleted ones; `git commit -m "1051 Drop project_user schema; add workspace_user custom role column"`.

---

## Phase 7 — Server tests

### Task 24: Delete/rewrite EE project tests

**Files:**
- Delete: `ProjectUserServiceTest.java`, `ProjectUserRepositoryIntTest.java`, `ProjectMembershipAccessorImplTest.java`, `ProjectCreatedEventListenerTest.java`.
- Rewrite: `ProjectScopeCacheServiceTest.java` → `WorkspaceScopeCacheServiceTest.java` (mirror, keyed on workspaceId / `WorkspaceUser`).
- Trim: `RbacBackfillMigrationIntTest.java` (drop project_user assertions; keep workspace-role promote if backfill kept, else delete), `RbacMigrationsIntTest.java` (drop project_user table assertions; add `workspace_user.custom_role_id` + `chk_workspace_user_role_xor` assertions).
- Update: `PermissionServiceTest.java` (EE), `CustomRoleServiceTest.java`, `WorkspaceUserServiceTest.java` — replace project-scope expectations with workspace-scope; add custom-role-on-workspace coverage.

- [ ] **Step 1:** Delete the obsolete tests with `git rm`.
- [ ] **Step 2:** Rewrite `WorkspaceScopeCacheServiceTest` mirroring the old project test: built-in-role scopes, custom-role scopes, XOR-corruption fail-closed, empty-result caching, eviction ordering.
- [ ] **Step 3:** Update the EE service tests; ensure custom-role-assigned-to-workspace resolves scopes through `getWorkspaceScopes`.
- [ ] **Step 4:** Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test` → PASS.

### Task 25: Update CE tests + enum pins

**Files:**
- Update: CE `PermissionServiceTest.java`, `ProjectWorkspacePermissionEvaluatorTest.java`, `PermissionEvaluatorWiringIntTest.java`, `AutomationMethodSecurityConfigurationTest.java`, `PreAuthorizeAnnotationTest.java`, `PreAuthorizeProxyEnforcementIntTest.java`, `RealImplProxyEnforcementIntTest.java`.
- Update: the enum-ordinal pin test (`EnumOrdinalPinTest` / `EnumOrdinalStabilityTest`) — find with `grep -rln "ProjectRole\|WorkspaceRole\|PermissionScope" server/ee --include=*OrdinalPinTest.java --include=*OrdinalStability*.java | grep -v /build/`.

- [ ] **Step 1:** `ProjectWorkspacePermissionEvaluatorTest`: replace `PROJECT_SCOPE`/`PROJECT_ROLE` cases with `WORKSPACE_SCOPE`; keep `WORKSPACE_ROLE`/`USER`/`TENANT`/skip-checks cases.
- [ ] **Step 2:** `PreAuthorizeAnnotationTest`: the inventory of expected `@PreAuthorize` expressions changes (no more `'ProjectScope'`/`'ProjectRole'`); regenerate the expected set from the new annotations.
- [ ] **Step 3:** Enum pins: remove the `ProjectRole` ordinal/privilege-rank pins entirely. For `PermissionScope`: it is name-persisted, so if the pin test asserts an ordinal list, update it to the new constant list (minus `PROJECT_VIEW_USERS`/`PROJECT_MANAGE_USERS`); if it asserts names only, just drop those two names. Keep `WorkspaceRole` pins.
- [ ] **Step 4:** Run the CE config test + the security IntTests:
```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration
```
Expected: PASS (Docker required for Testcontainers).

### Task 26: Full server check + commit

- [ ] **Step 1:** `./gradlew spotlessApply && ./gradlew compileJava`
- [ ] **Step 2:** `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:check :server:libs:automation:automation-configuration:automation-configuration-service:check`
- [ ] **Step 3:** `git commit -m "1051 Update RBAC tests for workspace-only scoping"`.

---

## Phase 8 — Client

### Task 27: Remove project-permission client surface

**Files:**
- Delete: `client/src/shared/hooks/useLoadProjectPermissions.ts` + `client/src/shared/hooks/tests/useLoadProjectPermissions.test.ts`
- Delete: `client/src/shared/hooks/useHasProjectScope.ts` + `client/src/shared/hooks/tests/useHasProjectScope.test.ts`
- Delete: `client/src/graphql/automation/configuration/updateProjectUserRole.graphql`, `projectUsers.graphql`, `addProjectUser.graphql`, `removeProjectUser.graphql`
- Delete: `client/src/pages/automation/project/components/project-header/components/settings-menu/components/ProjectUsersDialog.tsx`
- Modify: `client/src/shared/stores/usePermissionStore.ts` (+ test) — remove project-scope slice; keep workspace role/scope slice.
- Modify: `client/src/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu.tsx` — remove the "Project Users" / project-permission menu entry.

- [ ] **Step 1:** Delete the files above. `grep -rn "useHasProjectScope\|useLoadProjectPermissions\|ProjectUsersDialog\|myProjectScopes\|projectUsers\b" client/src` — fix every remaining import/usage.
- [ ] **Step 2:** Trim `usePermissionStore` to workspace-only state; update its test (reset state in `beforeEach`, keys in ascending order per the sort-keys rule).
- [ ] **Step 3:** If the project settings UI needs fine-grained gating, add a `useHasWorkspaceScope.ts` hook backed by `myWorkspaceScopes` (mirror the deleted `useHasProjectScope`), and a workspace-user management dialog if product requires it — otherwise rely on existing workspace-user UI (the `WorkspaceUserGraphQlController` already backs it).

### Task 28: Regenerate GraphQL + client check

**Files:**
- `client/codegen.ts` (verify schema path includes the EE configuration `.graphqls`)
- Regenerated: `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1:** Update/confirm `.graphql` operation files for workspace user/role management exist (mirror the removed project ones) if the UI uses them.
- [ ] **Step 2:** `cd client && npx graphql-codegen` to regenerate `graphql.ts`.
- [ ] **Step 3:** `cd client && npm run format && npm run check` (lint + typecheck + tests) → PASS. Honor sort-keys, interface `I`/`Props` suffix, import-destructure sort, `twMerge`, Lucide `Icon` suffix.
- [ ] **Step 4:** Commit operations + generated file separately:
```bash
git add client/src/graphql/... client/src/shared/... client/src/pages/...
git commit -m "1051 client - Remove project-level RBAC permission UI"
git add client/src/shared/middleware/graphql.ts
git commit -m "1051 client - Regenerate GraphQL types after removing project RBAC"
```

---

## Phase 9 — Final verification

### Task 29: Whole-repo gates

- [ ] **Step 1:** Server: `./gradlew spotlessApply && ./gradlew check` (and `./gradlew testIntegration` for the touched modules, Docker up).
- [ ] **Step 2:** Client: `cd client && npm run check`.
- [ ] **Step 3:** Sanity grep — no project-RBAC residue in production code:
```bash
grep -rn "hasProjectScope\|hasProjectRole\|getMyProjectScopes\|ProjectRole\|ProjectUser\|ProjectScopeCacheService\|ProjectMembershipAccessor\|evictProjectScopeCache\|PROJECT_MANAGE_USERS\|PROJECT_VIEW_USERS" server client --include=*.java --include=*.ts --include=*.tsx --include=*.graphql --include=*.graphqls | grep -v /build/ | grep -v node_modules
```
Expected: empty (or only historical comments you intend to keep).
- [ ] **Step 4:** Confirm only task-related files staged across all commits (`git log --stat` for the 1051 commits); the pre-existing unrelated dirty files (AI-gateway controllers, webhook test) were never staged.

### Task 30: Document dev-DB reset

- [ ] **Step 1:** Because Liquibase changelogs were edited in place, add a line to the final commit body (or a short note in the PR description): "Liquibase RBAC changelogs reshaped in place — reset dev DBs: `docker compose -f server/docker-compose.dev.infra.yml down -v`."

---

## Self-review notes

- **Coverage:** every project-RBAC artifact in the inventory maps to a deletion or migration task (domain, repo, service, evaluator, cache, events, audit, exception, graphql, remote-client, liquibase, client, tests, enum pins). The fine-grained scope model is preserved but re-anchored (Tasks 1, 4, 7, 8); custom roles preserved + moved (Tasks 3, 15, 16, 17, 21).
- **Type consistency:** `hasWorkspaceScope`/`getMyWorkspaceScopes`/`evictWorkspaceScopeCache`/`evictWorkspaceScopeCaches`/`evictAllWorkspaceScopeCache`/`UserWorkspacePair`/`WorkspaceScopeCacheService.getWorkspaceScopes` names are used identically across interface (Task 4), EE impl (Task 8), cache service (Task 7), CE no-op (in Task 8's companion — **add CE overrides too**, see below), evaluator (Task 11/12 via `WORKSPACE_SCOPE`), and remote client (Task 13).
- **Gap caught in review — CE `PermissionServiceImpl`:** Task list must also update the CE no-op `server/libs/.../service/PermissionServiceImpl.java`: remove `hasProjectScope`/`hasProjectRole`/`getMyProjectScopes`/`evictProjectScopeCache`/`evictAllProjectScopeCache`; add `hasWorkspaceScope`→`return true;`, `getMyWorkspaceScopes`→return all `PermissionScope` names (permissive CE; import the EE enum is not possible from CE — instead return `Collections.emptySet()` to match the old `getMyProjectScopes` no-op, OR keep a CE-local notion; **decision: return `Collections.emptySet()`** to avoid a CE→EE enum dependency, consistent with the prior `getMyProjectScopes` no-op), `evictWorkspaceScopeCache`/`evictAllWorkspaceScopeCache`→empty. Fold this into **Task 8** as its CE companion step and stage it in the Phase 2 commit.
- **Evaluator `WORKSPACE_SCOPE` branch:** Task 11/12 assume the evaluator exposes a `WORKSPACE_SCOPE` target. Add to **Task 8/Task 7 boundary** (Phase 2): in `ProjectWorkspacePermissionEvaluator`, add `static final String WORKSPACE_SCOPE = "WorkspaceScope";`, remove `PROJECT_ROLE`/`PROJECT_SCOPE`, and in the `switch` replace the two project cases with `case WORKSPACE_SCOPE -> permissionService.hasWorkspaceScope(id, value);`. Make this an explicit step in Task 8.
```
