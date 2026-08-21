# Project Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the automation `Project` into the resource-visibility model (PRIVATE | WORKSPACE, named-user grants in EE), with workflows, project workflows, deployments and jobs inheriting the project's reach.

**Architecture:** A `visibility` INT column on `project` + `ProjectVisibilityPolicy` + five `ResourceVisibilityProvider`s (Project, Workflow, ProjectWorkflow, ProjectDeployment, Job) close every by-id gate through the existing `PermissionService.hasResourceScope` precondition; a shared `ProjectVisibilityFilter` closes every list surface through the existing `ResourceVisibilityResolver`. Inheritance is expressed by two new **default** methods on `ResourceVisibilityProvider` (`fetchVisibility(Serializable)` and `visibilityResourceType()`), so a child provider returns the *project's* record under resource type `"Project"` and grants resolve against the project. EE adds a separate `ProjectSharingFacade` (+ GraphQL) over the existing `resource_grant` table; the client generalizes the connection picker/badge and mounts them on `ProjectDialog` and `ProjectListItem`.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Liquibase / Spring GraphQL / MapStruct / JUnit 5 + Mockito + AssertJ; React 19 / TypeScript / TanStack Query / Vitest; OpenAPI generator (typescript-fetch + spring); graphql-codegen.

**Spec:** `docs/superpowers/specs/2026-08-17-project-visibility-design.md` (builds on `docs/superpowers/specs/2026-08-10-resource-visibility-design.md`).

## Global Constraints

- Rungs for `Project`: `PRIVATE`, `WORKSPACE` only; default `WORKSPACE`; CE force-writes `WORKSPACE`; `ORGANIZATION` rejected.
- Resource-type tokens are the existing strings: `"Project"`, `"Workflow"`, `"ProjectWorkflow"`, `"ProjectDeployment"`, `"Job"`. Grants for every inheriting type are stored under `("Project", projectId)`.
- **The inheritance invariant:** a child is exactly as visible as its project — never more, never less. EVERY deployment,
  project workflow, workflow and job of a project inherits; there is no per-deployment opt-out, no "published is public"
  carve-out and no per-environment exception. No child gets a `visibility` column (spec §4.1).
- **Runtime is out of bounds.** Inheritance governs management surfaces only. Do NOT add visibility or permission checks to
  `AbstractWebhookTriggerController#doProcessTrigger`, `RequestTriggerApiController`, `AppEventTriggerApiController`, the
  scheduler, or any other trigger-execution path — they are anonymous by design (verified: no `@PreAuthorize`, no
  `PermissionService` reference) and a `PRIVATE` project's deployments must keep serving traffic. Making a project private
  is not a way to undeploy it (spec §4.1.1).
- Column: `project.visibility INT NOT NULL DEFAULT 1` (ordinal; `1` = `WORKSPACE`), changeset id `20260817000001-01`.
- The `ProjectOwnershipResolver` `ownerUserId` change and the `ProjectVisibilityProvider` registration land in the **same commit** (Task 4) — see spec §8.
- Owner-or-admin SpEL for every sharing method, verbatim:
  `@permissionService.isResourceOwner('Project', #projectId) || @permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')`.
- Audit events: `PROJECT_VISIBILITY_CHANGED`, `PROJECT_ACCESS_GRANTED`, `PROJECT_ACCESS_REVOKED`; payload keys `toVisibility`, `targetUserId`.
- GraphQL (EE): `projectGrants(workspaceId: ID!, projectId: ID!): [Long!]!`, `setProjectVisibility(...): Boolean!`, `grantProjectAccess(...): Boolean!`, `revokeProjectAccess(...): Boolean!`.
- Client component names after the move: `ResourceVisibilityPicker`, `ResourceVisibilityBadge` in `client/src/shared/components/visibility/`; hook `useVisibilityFeatureEnabled` in `client/src/shared/hooks/`.
- Java conventions (CLAUDE.md): one blank line before control statements and after a variable modification that the next statement uses; no `_` prefixes; descriptive names; EE files carry the ByteChef Enterprise header + `@version ee`; test method names camelCase without underscores; test class names end in `Test` (`IntTest` for Testcontainers).
- Client conventions: sort-keys, `Icon`-suffixed lucide imports, `twMerge`, hook ordering, `I`/`Props` interface suffix, `Ref` suffix.
- Commit messages: server `4750 <description>`, client `4750 client - <description>`; never amend on `0_732`.
- Before each server commit: `./gradlew spotlessApply` on the touched modules. Before each client commit: `cd client && npm run format && npm run check`.
- Never judge a Gradle run through a pipe — redirect to a file, check `$?`, then grep `^> Task .* FAILED`.

---

## File map

**Modified (CE server)**
- `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceVisibilityProvider.java` — two default methods
- `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java` — `isResourceVisible`, `hasWorkspaceScopeForProject`
- `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java` — same two
- `.../automation-configuration-api/.../domain/Project.java` — `visibility` field
- `.../automation-configuration-api/.../dto/ProjectDTO.java` — `visibility` component
- `.../automation-configuration-api/.../service/ProjectService.java` + `.../automation-configuration-service/.../service/ProjectServiceImpl.java` — `updateVisibility`
- `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/.../RemoteProjectServiceClient.java` — stub
- `.../automation-configuration-service/.../security/ProjectOwnershipResolver.java` — `ownerUserId`
- `.../automation-configuration-service/.../facade/ProjectFacadeImpl.java` — create force/validate, list filters
- `.../automation-configuration-service/.../facade/ProjectDeploymentFacadeImpl.java` — deployment list filter
- `.../automation-configuration-service/.../search/{Project,Workflow,ProjectDeployment}SearchAssetProvider.java` — filters
- `.../automation-configuration-graphql/.../web/graphql/ProjectGraphQlController.java` — `project`, `projects`
- `.../automation-configuration-graphql/src/main/resources/graphql/project.graphqls` — `Project.visibility`
- `.../automation-workflow-execution-service/.../facade/ProjectWorkflowExecutionFacadeImpl.java` — explicit-filter check
- `.../automation-configuration-rest-impl/openapi.yaml` + regenerated `automation-configuration-rest-api/generated/**` + `client/src/shared/middleware/automation/configuration/**`
- `.../automation-configuration-service/.../audit/ProjectAuditEvent.java` — three constants
- `server/ee/.../automation-configuration-service/.../event/ProjectBeforeDeleteEventListener.java` — grant purge

**Created (CE server)**
- `.../automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20260817000001_automation_configuration_project_visibility.xml`
- `.../automation-configuration-service/.../security/ProjectVisibilityPolicy.java`
- `.../automation-configuration-api/.../security/ProjectVisibilityFilter.java` (component in `-api` so `-graphql` can use it — the `ResourceVisibilityConfiguration`-in-`-api` precedent)
- `.../automation-configuration-service/.../security/{Project,Workflow,ProjectWorkflow,ProjectDeployment}VisibilityProvider.java`
- `.../automation-workflow-execution-service/.../security/JobVisibilityProvider.java`
- `.../automation-configuration-service/.../exception/ProjectErrorType.java`

**Created (EE server)**
- `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacade.java`
- `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacadeImpl.java`
- `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-sharing.graphqls`
- `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/ProjectSharingGraphQlController.java`

**Client**
- Move: `client/src/shared/components/connection/ConnectionVisibilityPicker{,.test}.tsx` → `client/src/shared/components/visibility/ResourceVisibilityPicker{,.test}.tsx`
- Move: `client/src/pages/automation/connections/components/ConnectionScopeBadge{,.test}.tsx` → `client/src/shared/components/visibility/ResourceVisibilityBadge{,.test}.tsx`
- Move: `client/src/pages/automation/connections/hooks/useVisibilityFeatureEnabled.ts` (+ `hooks/tests/useVisibilityFeatureEnabled.test.ts`) → `client/src/shared/hooks/useVisibilityFeatureEnabled.ts` (+ `client/src/shared/hooks/tests/useVisibilityFeatureEnabled.test.ts`)
- Create: `client/src/graphql/automation/configuration/{projectGrants,setProjectVisibility,grantProjectAccess,revokeProjectAccess}.graphql`; regenerate `client/src/shared/middleware/graphql.ts`
- Modify: `client/src/pages/automation/projects/components/ProjectDialog.tsx`, `client/src/pages/automation/projects/components/project-list/ProjectListItem.tsx` (+ new `ProjectListItem.visibility.test.tsx`)

**Docs**
- `CLAUDE.md` "Resource Visibility & Sharing"; parent spec §11; `openapi.yaml` connection `visibility` description.

---

### Task 1: `ResourceVisibilityProvider` inheritance hook + `isResourceVisible` in both `PermissionServiceImpl`s

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceVisibilityProvider.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java:152-174`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java:206-228`
- Test: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/PermissionServiceVisibilityTest.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/PermissionServiceResourceTest.java`

**Interfaces:**
- Produces: `ResourceVisibilityProvider.fetchVisibility(Serializable id)` (default) and `ResourceVisibilityProvider.visibilityResourceType()` (default, returns `resourceType()`). Every provider in Tasks 4–5 relies on both.

- [ ] **Step 1: Write the failing EE test — an inheriting type is granted through its parent**

Add to `PermissionServiceVisibilityTest` (below the existing tests; the class already has `authenticate`, `permissionService(Set<Long>)`, `visibilityResolver(...)`, `ownershipResolver(...)`):

```java
    private static final String WORKFLOW = "Workflow";
    private static final String PRIVATE_WORKFLOW_ID = "wf-private";
    private static final long PRIVATE_PROJECT_ID = 20L;

    @Test
    void testInheritingResourceIsGrantedThroughItsParent() {
        authenticate("ana");

        assertThat(
            permissionServiceWithWorkflow(Set.of(PRIVATE_PROJECT_ID))
                .hasResourceScope(PRIVATE_WORKFLOW_ID, WORKFLOW, "WORKFLOW_VIEW"))
                    .as("the grant sits on (Project, 20); the workflow provider must resolve against it")
                    .isTrue();
    }

    @Test
    void testInheritingResourceIsDeniedWithoutParentGrant() {
        authenticate("ana");

        assertThat(
            permissionServiceWithWorkflow(Set.of()).hasResourceScope(PRIVATE_WORKFLOW_ID, WORKFLOW, "WORKFLOW_VIEW"))
                .isFalse();
    }

    @Test
    void testStringIdOnNumericOnlyProviderFailsClosed() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope("not-a-number", CONNECTION, "CONNECTION_EDIT"))
            .as("the default Serializable overload rejects non-numeric ids for a numeric-keyed provider")
            .isFalse();
    }

    private static PermissionServiceImpl permissionServiceWithWorkflow(Set<Long> grantedProjectIds) {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong()))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        return new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), mock(ProjectRepository.class),
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(ownershipResolver(WORKFLOW)), List.of(workflowVisibilityProvider()),
            visibilityResolver(grantedProjectIds));
    }

    /**
     * A workflow inherits its project's reach: the record it returns is the PROJECT's (id, visibility, owner) and it
     * declares "Project" as the type its grants live under.
     */
    private static ResourceVisibilityProvider workflowVisibilityProvider() {
        return new ResourceVisibilityProvider() {
            @Override
            public String resourceType() {
                return WORKFLOW;
            }

            @Override
            public String visibilityResourceType() {
                return "Project";
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                return Optional.empty();
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(Serializable id) {
                if (PRIVATE_WORKFLOW_ID.equals(id)) {
                    return Optional.of(new VisibilityRecord(PRIVATE_PROJECT_ID, ResourceVisibility.PRIVATE, "ivica"));
                }

                return Optional.empty();
            }
        };
    }
```

The test's `visibilityResolver` lambda already ignores `resourceType` and honours `grantedIds` by record id, so `grantedProjectIds = {20}` with a record whose id is `20` models a grant on the project. Also add an assertion inside a new test that the resolver is called with `"Project"` — replace `visibilityResolver(...)` usage in `permissionServiceWithWorkflow` with a capturing wrapper:

```java
    private static final java.util.concurrent.atomic.AtomicReference<String> LAST_RESOLVED_TYPE =
        new java.util.concurrent.atomic.AtomicReference<>();

    private static ResourceVisibilityResolver capturingResolver(Set<Long> grantedIds) {
        ResourceVisibilityResolver delegate = visibilityResolver(grantedIds);

        return (resourceType, workspaceId, candidates) -> {
            LAST_RESOLVED_TYPE.set(resourceType);

            return delegate.filterVisibleIds(resourceType, workspaceId, candidates);
        };
    }

    @Test
    void testInheritingResourceResolvesUnderParentType() {
        authenticate("ana");

        permissionServiceWithWorkflow(Set.of(PRIVATE_PROJECT_ID))
            .hasResourceScope(PRIVATE_WORKFLOW_ID, WORKFLOW, "WORKFLOW_VIEW");

        assertThat(LAST_RESOLVED_TYPE.get())
            .as("grants for a workflow live under its project, so the resolver must be asked about Project")
            .isEqualTo("Project");
    }
```

(Use `capturingResolver(grantedProjectIds)` as the last constructor argument in `permissionServiceWithWorkflow`; import `java.util.concurrent.atomic.AtomicReference` properly instead of the inline FQN.)

- [ ] **Step 2: Run the EE test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*PermissionServiceVisibilityTest' > /tmp/t1.log 2>&1; echo $?; grep -E "^> Task .* FAILED|visibilityResourceType|cannot find symbol" /tmp/t1.log | head
```
Expected: compile failure — `visibilityResourceType()` / `fetchVisibility(Serializable)` do not exist on the interface.

- [ ] **Step 3: Add the two default methods to `ResourceVisibilityProvider`**

Replace the interface body with:

```java
public interface ResourceVisibilityProvider {

    /**
     * Discriminator matching {@code ResourceOwnershipResolver.resourceType()} — e.g. {@code "Connection"},
     * {@code "Project"}, {@code "Workflow"}. Must be unique across providers.
     */
    String resourceType();

    /**
     * The resource's visibility and creator, or empty when it does not exist.
     */
    Optional<VisibilityRecord> fetchVisibility(long id);

    /**
     * Id-shape-agnostic entry point used by {@code PermissionService}. The default handles numeric ids; providers for
     * string-keyed resources (workflows) override it. A non-numeric id on the default fails closed.
     */
    default Optional<VisibilityRecord> fetchVisibility(Serializable id) {
        if (id instanceof Number number) {
            return fetchVisibility(number.longValue());
        }

        return Optional.empty();
    }

    /**
     * The resource type under which the returned record's visibility and grants are stored. A resource that inherits
     * its reach returns its parent's type ({@code "Project"}) and its record's id is the parent's id, so grant lookups
     * resolve against the parent. Defaults to {@link #resourceType()}.
     */
    default String visibilityResourceType() {
        return resourceType();
    }
}
```

Add `import java.io.Serializable;`.

- [ ] **Step 4: Rewrite `isResourceVisible` in the CE `PermissionServiceImpl`**

Replace the method body (lines ~152-174) with:

```java
    private boolean isResourceVisible(Serializable id, String resourceType) {
        ResourceVisibilityProvider resourceVisibilityProvider = resourceVisibilityProviders.get(resourceType);

        if (resourceVisibilityProvider == null) {
            return true;
        }

        return resourceVisibilityProvider.fetchVisibility(id)
            .map(visibilityRecord -> {
                // workspaceId is unused by both resolver implementations — they resolve against the current
                // principal, not the argument — so 0 is safe. The parameter exists for a future SQL-predicate
                // implementation. The resource type handed to the resolver is the one the record and its grants
                // are stored under, which for an inheriting resource is its parent's.
                Set<Long> visibleIds = resourceVisibilityResolver.filterVisibleIds(
                    resourceVisibilityProvider.visibilityResourceType(), 0L, List.of(visibilityRecord));

                return !visibleIds.isEmpty();
            })
            .orElse(false);
    }
```

- [ ] **Step 5: Rewrite `isResourceVisible` in the EE `PermissionServiceImpl` identically**

Same body as Step 4 (the two classes keep the method verbatim-identical, as they are today).

- [ ] **Step 6: Add the CE regression test**

In `PermissionServiceResourceTest` add:

```java
    @Test
    void testStringIdOnNumericProviderFailsClosedInCe() {
        PermissionService service = new PermissionServiceImpl(
            userService, List.of(resolver("Connection", ResourceOwner.ofUser(7L))),
            List.of(visibilityProvider("Connection")), permissiveResolver());

        assertThat(service.hasResourceScope("abc", "Connection", "CONNECTION_DELETE")).isFalse();
    }
```

- [ ] **Step 7: Run both test classes**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*PermissionServiceVisibilityTest' :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*PermissionServiceResourceTest' > /tmp/t1.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t1.log
```
Expected: exit 0, no FAILED lines.

- [ ] **Step 8: Commit**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:spotlessApply :server:libs:automation:automation-configuration:automation-configuration-service:spotlessApply :server:ee:libs:automation:automation-configuration:automation-configuration-service:spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceVisibilityProvider.java server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/PermissionServiceVisibilityTest.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/PermissionServiceResourceTest.java
git commit -m "4750 Let a visibility provider inherit its record and grants from a parent resource type"
```

---

### Task 2: `hasWorkspaceScopeForProject` goes through the visibility precondition

**Files:**
- Modify: CE `PermissionServiceImpl.hasWorkspaceScopeForProject` (line ~95)
- Modify: EE `PermissionServiceImpl.hasWorkspaceScopeForProject` (line ~148)
- Test: `PermissionServiceVisibilityTest` (EE), `PermissionServiceResourceTest` (CE)

**Interfaces:**
- Consumes: `hasResourceScope(Serializable, String, String)` unchanged.
- Produces: `hasWorkspaceScopeForProject(projectId, scope) == hasResourceScope(projectId, "Project", scope)` in both editions.

> **Scope note (added 2026-08-19).** A THIRD entry point, `PermissionService.hasWorkflowScope(String
> workflowId, String scope)`, has the same bypass — EE resolves workflow → project → `hasWorkspaceScope`
> with no visibility precondition ([EE `PermissionServiceImpl`](../../../server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java) `hasWorkflowScope`), and it is live: `WorkflowEditorSpringAIAgent`
> calls it with `WORKFLOW_VIEW`. It is deliberately NOT fixed here: the fix routes it through
> `hasResourceScope(workflowId, "Workflow", scope)`, which needs the `WorkflowVisibilityProvider` that
> Task 4 creates. It is Task 4 Step 12. Do not attempt it in this task, and do not treat its absence
> here as a gap.

- [ ] **Step 1: Write the failing EE test**

```java
    private static final String PROJECT = "Project";
    private static final long PRIVATE_PROJECT_FOR_SCOPE_ID = 30L;

    @Test
    void testHasWorkspaceScopeForProjectDeniesHiddenProject() {
        authenticate("ana");

        PermissionServiceImpl permissionService = permissionServiceWithProject(Set.of());

        assertThat(permissionService.hasWorkspaceScopeForProject(PRIVATE_PROJECT_FOR_SCOPE_ID, "WORKFLOW_VIEW"))
            .as("the project-keyed entry point must not bypass visibility")
            .isFalse();
        assertThat(permissionServiceWithProject(Set.of(PRIVATE_PROJECT_FOR_SCOPE_ID))
            .hasWorkspaceScopeForProject(PRIVATE_PROJECT_FOR_SCOPE_ID, "WORKFLOW_VIEW"))
                .isTrue();
    }

    private static PermissionServiceImpl permissionServiceWithProject(Set<Long> grantedProjectIds) {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong()))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        return new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), mock(ProjectRepository.class),
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(ownershipResolver(PROJECT)), List.of(projectVisibilityProvider()),
            visibilityResolver(grantedProjectIds));
    }

    private static ResourceVisibilityProvider projectVisibilityProvider() {
        return new ResourceVisibilityProvider() {
            @Override
            public String resourceType() {
                return PROJECT;
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                if (id == PRIVATE_PROJECT_FOR_SCOPE_ID) {
                    return Optional.of(new VisibilityRecord(id, ResourceVisibility.PRIVATE, "ivica"));
                }

                return Optional.empty();
            }
        };
    }
```

- [ ] **Step 2: Run to verify it fails**

Same Gradle invocation as Task 1 Step 2. Expected: `testHasWorkspaceScopeForProjectDeniesHiddenProject` FAILS (the mocked `ProjectRepository.findById` returns empty → current EE impl returns `false` for BOTH calls, so the second assertion fails; the CE version returns `true` for both).

- [ ] **Step 3: Implement in EE**

Replace the EE method with:

```java
    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        // A project-keyed check is a resource-scope check on the project: routing through hasResourceScope gives it
        // the same visibility precondition every hasPermission(#id, 'Project', ...) gate has.
        return hasResourceScope(projectId, "Project", scope);
    }
```

- [ ] **Step 4: Implement in CE**

Replace the CE method (`return SecurityUtils.isAuthenticated();`) with the identical body from Step 3.

- [ ] **Step 5: Add the CE test**

In `PermissionServiceResourceTest`:

```java
    @Test
    void testHasWorkspaceScopeForProjectHonoursVisibilityInCe() {
        User user = new User();

        user.setId(99L);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        ResourceVisibilityProvider hiddenProject = new ResourceVisibilityProvider() {
            @Override
            public String resourceType() {
                return "Project";
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                return Optional.of(new VisibilityRecord(id, ResourceVisibility.PRIVATE, "someone-else"));
            }
        };

        ResourceVisibilityResolver denyAll = (resourceType, workspaceId, candidates) -> Set.of();

        PermissionService service = new PermissionServiceImpl(
            userService, List.of(resolver("Project", ResourceOwner.ofWorkspace(1L))), List.of(hiddenProject), denyAll);

        assertThat(service.hasWorkspaceScopeForProject(1L, "WORKFLOW_VIEW")).isFalse();
    }
```

- [ ] **Step 6: Run both test classes** (Task 1 Step 7 command). Expected: pass.

- [ ] **Step 7: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/PermissionServiceImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/PermissionServiceVisibilityTest.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/PermissionServiceResourceTest.java
git commit -m "4750 Route hasWorkspaceScopeForProject through the resource-scope visibility precondition"
```

---

### Task 3: Column, entity, DTO, service, policy, error type

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20260817000001_automation_configuration_project_visibility.xml`
- Modify: `.../automation-configuration-api/.../domain/Project.java`
- Modify: `.../automation-configuration-api/.../dto/ProjectDTO.java`
- Modify: `.../automation-configuration-api/.../service/ProjectService.java`, `.../automation-configuration-service/.../service/ProjectServiceImpl.java`
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/service/RemoteProjectServiceClient.java`
- Create: `.../automation-configuration-service/.../security/ProjectVisibilityPolicy.java`
- Create: `.../automation-configuration-service/.../exception/ProjectErrorType.java`
- Test: `.../automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ProjectVisibilityPolicyTest.java` (new)
- Verify schema: `.../automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectFacadeIntTest.java` (existing, Testcontainers)

**Interfaces:**
- Produces: `Project.getVisibility(): ResourceVisibility`, `Project.setVisibility(ResourceVisibility)`; `ProjectDTO.visibility()`; `ProjectService.updateVisibility(long id, ResourceVisibility visibility): Project`; `ProjectVisibilityPolicy` (`"Project"`, default `WORKSPACE`, supports `{PRIVATE, WORKSPACE}`); `ProjectErrorType.INVALID_PROJECT`, `ProjectErrorType.UNSUPPORTED_VISIBILITY`.

- [ ] **Step 1: Write the failing policy test**

`ProjectVisibilityPolicyTest`:

```java
package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectVisibilityPolicyTest {

    private final ResourceVisibilityPolicyRegistry registry =
        new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy()));

    @Test
    void testDefaultIsWorkspace() {
        assertThat(registry.defaultVisibility("Project")).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testOrganizationIsNotSupported() {
        assertThat(registry.supports("Project", ResourceVisibility.PRIVATE)).isTrue();
        assertThat(registry.supports("Project", ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(registry.supports("Project", ResourceVisibility.ORGANIZATION))
            .as("a project belongs to one workspace; there is no representation for it outside that workspace")
            .isFalse();
    }
}
```

- [ ] **Step 2: Run to verify it fails to compile**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectVisibilityPolicyTest' > /tmp/t3.log 2>&1; echo $?; grep -E "cannot find symbol|^> Task .* FAILED" /tmp/t3.log | head -3
```

- [ ] **Step 3: Create the changeset**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="20260817000001-01" author="Ivica Cardic">
        <addColumn tableName="project">
            <column name="visibility" type="INT" defaultValueNumeric="1">
                <constraints nullable="false"/>
            </column>
        </addColumn>
        <comment>
            Projects default to WORKSPACE (1) — shared with the owning workspace, which is the pre-visibility behaviour
            for every existing row. PRIVATE (0) is opt-in via setProjectVisibility (EE). Ordinal of ResourceVisibility.
        </comment>
        <rollback>
            <dropColumn tableName="project" columnName="visibility"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 4: Add the field to `Project`**

After the `uuid` field (keep fields alphabetical-ish as the class does):

```java
    @Column
    private int visibility = ResourceVisibility.WORKSPACE.ordinal();
```

Import `com.bytechef.platform.security.domain.ResourceVisibility` and `java.util.Objects` (already imported). Add getter/setter beside `getUuid()`/`setUuid()`:

```java
    public ResourceVisibility getVisibility() {
        ResourceVisibility[] values = ResourceVisibility.values();

        if (visibility < 0 || visibility >= values.length) {
            throw new IllegalStateException(
                "Project id=%s has invalid visibility ordinal %d (valid range: 0-%d)".formatted(
                    id, visibility, values.length - 1));
        }

        return values[visibility];
    }

    /**
     * Plain assignment. Which rungs a project supports is declared by {@code ProjectVisibilityPolicy} and enforced by
     * the facades that write it, not here.
     */
    public void setVisibility(ResourceVisibility visibility) {
        Objects.requireNonNull(visibility, "visibility");

        this.visibility = visibility.ordinal();
    }
```

Add `", visibility=" + visibility +` to `toString()`. Do **not** add it to the `@PersistenceCreator` constructor (Spring Data JDBC populates it by field access, like `errorProjectWorkflowId`). Add to the `Builder`: field `private ResourceVisibility visibility;`, method `visibility(ResourceVisibility)`, and in `build()`: `if (visibility != null) { project.setVisibility(visibility); }`.

- [ ] **Step 5: Add the component to `ProjectDTO`**

Record header becomes (insert `ResourceVisibility visibility` between `version` and `workspaceId`):

```java
public record ProjectDTO(
    Category category, boolean codeWorkflow, String codeWorkflowLanguage, String createdBy, Instant createdDate,
    String description, Long id, String name, String lastModifiedBy, Instant lastModifiedDate,
    Instant lastPublishedDate, Status lastStatus, int lastProjectVersion, List<ProjectVersion> projectVersions,
    List<Long> projectWorkflowIds, List<Tag> tags, String uuid, int version, ResourceVisibility visibility,
    Long workspaceId) {
```

Update the two `Project`-based constructors to pass `project.getVisibility()` before `project.getWorkspaceId()`; `toProject()` adds `project.setVisibility(visibility == null ? ResourceVisibility.WORKSPACE : visibility);` after `setVersion`; the `Builder` gains `private ResourceVisibility visibility = ResourceVisibility.WORKSPACE;` + `visibility(ResourceVisibility)` and passes it in `build()`. Import `com.bytechef.platform.security.domain.ResourceVisibility`.

- [ ] **Step 6: `ProjectService.updateVisibility`**

Interface (after `updatePermissionExpression`):

```java
    /**
     * Sets the project's reach. Deliberately un-guarded: authorization (owner-or-admin) and policy validation live on
     * the sharing facade so every caller of that facade shares them.
     */
    Project updateVisibility(long id, ResourceVisibility visibility);
```

Impl (`ProjectServiceImpl`, after `updatePermissionExpression`):

```java
    @Override
    public Project updateVisibility(long id, ResourceVisibility visibility) {
        Project project = getProject(id);

        project.setVisibility(visibility);

        Project savedProject = projectRepository.save(project);

        projectAuditPublisher.publish(
            ProjectAuditEvent.PROJECT_UPDATED, savedProject.getId(), Map.of("toVisibility", visibility.name()));

        return savedProject;
    }
```

(Import `java.util.Map` and `ResourceVisibility`.) Remote stub in `RemoteProjectServiceClient` (beside `updatePermissionExpression`):

```java
    @Override
    public Project updateVisibility(long id, ResourceVisibility visibility) {
        throw new UnsupportedOperationException();
    }
```

- [ ] **Step 7: `ProjectVisibilityPolicy`**

```java
package com.bytechef.automation.configuration.security;

import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Declares the rungs a project supports. {@code ORGANIZATION} is deliberately absent: a project belongs to one
 * workspace and promoting it organization-wide would surface it in other workspaces' lists, which the project model
 * has no other way to express (resource-visibility spec §3.2).
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectVisibilityPolicy implements ResourceVisibilityPolicy {

    public static final String PROJECT = "Project";

    @Override
    public String resourceType() {
        return PROJECT;
    }

    @Override
    public ResourceVisibility defaultVisibility() {
        return ResourceVisibility.WORKSPACE;
    }

    @Override
    public Set<ResourceVisibility> supportedVisibilities() {
        return Set.of(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE);
    }
}
```

- [ ] **Step 8: `ProjectErrorType`**

```java
package com.bytechef.automation.configuration.exception;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ProjectErrorType extends AbstractErrorType {

    /** Unknown project, project outside the caller's workspace, or grantee outside the project's workspace. */
    public static final ProjectErrorType INVALID_PROJECT = new ProjectErrorType(100);

    public static final ProjectErrorType UNSUPPORTED_VISIBILITY = new ProjectErrorType(101);

    private ProjectErrorType(int errorKey) {
        super(Project.class, errorKey);
    }
}
```

- [ ] **Step 9: Run the policy test and the schema-building IntTest**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectVisibilityPolicyTest' > /tmp/t3.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t3.log
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*ProjectFacadeIntTest' > /tmp/t3i.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t3i.log
```
Expected: both exit 0 (the IntTest proves Liquibase applies the new changeset and the entity round-trips).

- [ ] **Step 10: Commit**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:spotlessApply :server:libs:automation:automation-configuration:automation-configuration-service:spotlessApply :server:ee:libs:automation:automation-configuration:automation-configuration-remote-client:spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/20260817000001_automation_configuration_project_visibility.xml server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/Project.java server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/dto/ProjectDTO.java server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectService.java server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectServiceImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/service/RemoteProjectServiceClient.java server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ProjectVisibilityPolicy.java server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/exception/ProjectErrorType.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ProjectVisibilityPolicyTest.java
git commit -m "4750 Add project visibility column, policy and service update path"
```

---

### Task 4: Visibility providers (Project, Workflow, ProjectWorkflow, ProjectDeployment, Job) + `ProjectOwnershipResolver` owner + `ProjectVisibilityFilter`

**Files:**
- Create: `.../automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ProjectVisibilityFilter.java`
- Create: `.../automation-configuration-service/.../security/ProjectVisibilityProvider.java`, `WorkflowVisibilityProvider.java`, `ProjectWorkflowVisibilityProvider.java`, `ProjectDeploymentVisibilityProvider.java`
- Create: `server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-service/src/main/java/com/bytechef/automation/workflow/execution/security/JobVisibilityProvider.java`
- Modify: `.../automation-configuration-service/.../security/ProjectOwnershipResolver.java`
- Test: `.../automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ProjectVisibilityProvidersTest.java` (new), `.../ProjectVisibilityFilterTest.java` (new, in `-api` test dir or `-service` test dir — put it in `-service` beside the providers test), `JobVisibilityProviderTest.java` (new, execution-service test dir), and extend the existing `ProjectOwnershipResolver` coverage (there is none — add `ProjectOwnershipResolverTest.java`).

**Interfaces:**
- Consumes: `ResourceVisibilityProvider` defaults (Task 1); `Project.getVisibility()` (Task 3); `ProjectRepository.findById/findByWorkflowId`, `ProjectWorkflowRepository.findById`, `ProjectDeploymentRepository.findById`, `JobService.fetchJob`, `ProjectService.getWorkflowProject`.
- Produces: `ProjectVisibilityFilter.filterVisible(Collection<Project>): List<Project>`, `ProjectVisibilityFilter.visibleProjectIds(Collection<Project>): Set<Long>`, `ProjectVisibilityFilter.toVisibilityRecord(Project): VisibilityRecord`, constant `ProjectVisibilityFilter.PROJECT = "Project"`. Every provider's `visibilityResourceType()` returns `"Project"` and its record id is the project id.

- [ ] **Step 1: Write the failing providers test**

`ProjectVisibilityProvidersTest` (`-service` test dir, package `com.bytechef.automation.configuration.security`):

```java
package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectVisibilityProvidersTest {

    private static final long PROJECT_ID = 5L;
    private static final long PROJECT_WORKFLOW_ID = 8L;
    private static final long PROJECT_DEPLOYMENT_ID = 9L;
    private static final String WORKFLOW_ID = "wf-1";

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectWorkflowRepository projectWorkflowRepository = mock(ProjectWorkflowRepository.class);
    private final ProjectDeploymentRepository projectDeploymentRepository = mock(ProjectDeploymentRepository.class);

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();

        project.setId(PROJECT_ID);
        project.setVisibility(ResourceVisibility.PRIVATE);
        // created_by is @CreatedBy-managed; the test seeds it the way the persistence layer would
        org.springframework.test.util.ReflectionTestUtils.setField(project, "createdBy", "ivica");

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectRepository.findByWorkflowId(WORKFLOW_ID)).thenReturn(Optional.of(project));

        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setId(PROJECT_WORKFLOW_ID);
        projectWorkflow.setProjectId(PROJECT_ID);

        when(projectWorkflowRepository.findById(PROJECT_WORKFLOW_ID)).thenReturn(Optional.of(projectWorkflow));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(PROJECT_DEPLOYMENT_ID);
        projectDeployment.setProjectId(PROJECT_ID);

        when(projectDeploymentRepository.findById(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.of(projectDeployment));
    }

    @Test
    void testProjectProviderReturnsOwnRecord() {
        ProjectVisibilityProvider provider = new ProjectVisibilityProvider(projectRepository);

        assertThat(provider.resourceType()).isEqualTo("Project");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(PROJECT_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(999L)).isEmpty();
    }

    @Test
    void testWorkflowProviderInheritsProjectRecordUnderStringId() {
        WorkflowVisibilityProvider provider = new WorkflowVisibilityProvider(projectRepository);

        assertThat(provider.resourceType()).isEqualTo("Workflow");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility((java.io.Serializable) WORKFLOW_ID)).contains(projectRecord());
        assertThat(provider.fetchVisibility(1L)).as("workflow ids are strings; a long is not a workflow").isEmpty();
        assertThat(provider.fetchVisibility((java.io.Serializable) "unknown")).isEmpty();
    }

    @Test
    void testProjectWorkflowProviderInheritsProjectRecord() {
        ProjectWorkflowVisibilityProvider provider =
            new ProjectWorkflowVisibilityProvider(projectRepository, projectWorkflowRepository);

        assertThat(provider.resourceType()).isEqualTo("ProjectWorkflow");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(PROJECT_WORKFLOW_ID)).contains(projectRecord());
    }

    @Test
    void testProjectDeploymentProviderInheritsProjectRecord() {
        ProjectDeploymentVisibilityProvider provider =
            new ProjectDeploymentVisibilityProvider(projectDeploymentRepository, projectRepository);

        assertThat(provider.resourceType()).isEqualTo("ProjectDeployment");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(PROJECT_DEPLOYMENT_ID)).contains(projectRecord());
    }

    private static VisibilityRecord projectRecord() {
        return new VisibilityRecord(PROJECT_ID, ResourceVisibility.PRIVATE, "ivica");
    }
}
```

(Import `org.springframework.test.util.ReflectionTestUtils` and `java.io.Serializable` at the top rather than inline; check that `spring-test` is on the module's test classpath — it is used by other tests in this module; if `ProjectDeployment`/`ProjectWorkflow` lack a no-arg constructor + `setProjectId`, use their builders/constructors as the neighbouring `ProjectDeploymentOwnershipResolverTest` does.)

- [ ] **Step 2: Run to verify it fails to compile**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectVisibilityProvidersTest' > /tmp/t4.log 2>&1; echo $?; grep -E "cannot find symbol" /tmp/t4.log | head -3
```

- [ ] **Step 3: `ProjectVisibilityFilter` (in `-api`)**

```java
package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The one place a {@link Project} is mapped to a {@link VisibilityRecord} and filtered through the
 * {@link ResourceVisibilityResolver}. Every project list surface (projects, workflows, deployments, executions,
 * search, GraphQL) goes through here so the mapping cannot drift between them.
 *
 * <p>
 * Lives in {@code -api} rather than {@code -service} because {@code automation-configuration-graphql} depends only on
 * {@code -api} — the same reason {@code ResourceVisibilityConfiguration} declares its bean in an api module.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectVisibilityFilter {

    public static final String PROJECT = "Project";

    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings("EI")
    public ProjectVisibilityFilter(ResourceVisibilityResolver resourceVisibilityResolver) {
        this.resourceVisibilityResolver = resourceVisibilityResolver;
    }

    public static VisibilityRecord toVisibilityRecord(Project project) {
        return new VisibilityRecord(
            Objects.requireNonNull(project.getId(), "id"), project.getVisibility(), project.getCreatedBy());
    }

    /**
     * Ids of the given projects the current principal may see. The resolver ignores {@code workspaceId} (it resolves
     * against the principal), so callers without a workspace in hand pass the projects as-is.
     */
    public Set<Long> visibleProjectIds(Collection<Project> projects) {
        if (projects.isEmpty()) {
            return Set.of();
        }

        List<VisibilityRecord> visibilityRecords = projects.stream()
            .map(ProjectVisibilityFilter::toVisibilityRecord)
            .toList();

        return resourceVisibilityResolver.filterVisibleIds(PROJECT, 0L, visibilityRecords);
    }

    public List<Project> filterVisible(Collection<Project> projects) {
        Set<Long> visibleProjectIds = visibleProjectIds(projects);

        return projects.stream()
            .filter(project -> visibleProjectIds.contains(project.getId()))
            .toList();
    }
}
```

- [ ] **Step 4: `ProjectVisibilityProvider`**

```java
package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Registers projects with the visibility precondition of {@code PermissionService.hasResourceScope}. Reads the
 * repository directly (not the guarded facade) to avoid recursion.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public ProjectVisibilityProvider(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return projectRepository.findById(id)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
```

- [ ] **Step 5: `WorkflowVisibilityProvider`**

```java
package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * A workflow has no visibility of its own: its reach IS its project's, resolved here at check time (resource-visibility
 * spec §11). Workflow ids are strings, so the {@link Serializable} overload is the real entry point and the numeric one
 * fails closed.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public WorkflowVisibilityProvider(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "Workflow";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(Serializable id) {
        if (!(id instanceof String workflowId)) {
            return Optional.empty();
        }

        return projectRepository.findByWorkflowId(workflowId)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
```

- [ ] **Step 6: `ProjectWorkflowVisibilityProvider` and `ProjectDeploymentVisibilityProvider`**

```java
@Component
public class ProjectWorkflowVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectRepository projectRepository;
    private final ProjectWorkflowRepository projectWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowVisibilityProvider(
        ProjectRepository projectRepository, ProjectWorkflowRepository projectWorkflowRepository) {

        this.projectRepository = projectRepository;
        this.projectWorkflowRepository = projectWorkflowRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectWorkflow";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return projectWorkflowRepository.findById(id)
            .map(ProjectWorkflow::getProjectId)
            .flatMap(projectRepository::findById)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
```

```java
@Component
public class ProjectDeploymentVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectDeploymentRepository projectDeploymentRepository;
    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentVisibilityProvider(
        ProjectDeploymentRepository projectDeploymentRepository, ProjectRepository projectRepository) {

        this.projectDeploymentRepository = projectDeploymentRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectDeployment";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return projectDeploymentRepository.findById(id)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectRepository::findById)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
```

Both carry the standard Apache header + `@author Ivica Cardic` javadoc, package `com.bytechef.automation.configuration.security`, imports for the domain classes/repositories/`VisibilityRecord`/`Optional`/`SuppressFBWarnings`/`Component`.

- [ ] **Step 7: `JobVisibilityProvider` (execution-service)**

```java
package com.bytechef.automation.workflow.execution.security;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * A job (workflow execution) inherits the visibility of the project its workflow belongs to — traversing
 * job → workflowId → project exactly as {@link JobOwnershipResolver} does for ownership.
 *
 * @author Ivica Cardic
 */
@Component
public class JobVisibilityProvider implements ResourceVisibilityProvider {

    private final JobService jobService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public JobVisibilityProvider(JobService jobService, ProjectService projectService) {
        this.jobService = jobService;
        this.projectService = projectService;
    }

    @Override
    public String resourceType() {
        return "Job";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return jobService.fetchJob(id)
            .map(Job::getWorkflowId)
            .flatMap(this::fetchProject)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }

    private Optional<Project> fetchProject(String workflowId) {
        try {
            return Optional.ofNullable(projectService.getWorkflowProject(workflowId));
        } catch (RuntimeException exception) {
            // Not a project workflow (platform/embedded job) — fail closed.
            return Optional.empty();
        }
    }
}
```

Match `JobOwnershipResolver`'s exact `JobService`/`Job` imports (copy them from that file). Add `JobVisibilityProviderTest` in the same test package as the existing `JobOwnershipResolverTest` (if there is one; otherwise create beside it):

```java
class JobVisibilityProviderTest {

    @Test
    void testJobInheritsProjectRecord() {
        JobService jobService = mock(JobService.class);
        ProjectService projectService = mock(ProjectService.class);
        Job job = mock(Job.class);
        Project project = new Project();

        project.setId(3L);
        project.setVisibility(ResourceVisibility.PRIVATE);
        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        when(job.getWorkflowId()).thenReturn("wf");
        when(jobService.fetchJob(11L)).thenReturn(Optional.of(job));
        when(projectService.getWorkflowProject("wf")).thenReturn(project);

        JobVisibilityProvider provider = new JobVisibilityProvider(jobService, projectService);

        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(11L))
            .contains(new VisibilityRecord(3L, ResourceVisibility.PRIVATE, "ivica"));
    }

    @Test
    void testNonProjectJobFailsClosed() {
        JobService jobService = mock(JobService.class);
        ProjectService projectService = mock(ProjectService.class);
        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("platform-wf");
        when(jobService.fetchJob(12L)).thenReturn(Optional.of(job));
        when(projectService.getWorkflowProject("platform-wf")).thenThrow(new IllegalArgumentException("no project"));

        assertThat(new JobVisibilityProvider(jobService, projectService).fetchVisibility(12L)).isEmpty();
    }
}
```

- [ ] **Step 8: `ProjectOwnershipResolver` returns the owner user id**

Replace the class body:

```java
@Component
public class ProjectOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ProjectOwnershipResolver(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "Project";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return projectRepository.findById(id)
            .map(project -> ResourceOwner.of(
                OptionalLong.of(project.getWorkspaceId()), resolveOwnerUserId(project.getCreatedBy())))
            .orElseGet(ResourceOwner::unknown);
    }

    private OptionalLong resolveOwnerUserId(String createdBy) {
        if (createdBy == null) {
            return OptionalLong.empty();
        }

        return userService.fetchUserByLogin(createdBy)
            .map(user -> OptionalLong.of(user.getId()))
            .orElse(OptionalLong.empty());
    }
}
```

Update the class Javadoc: *"Maps a project id to its owning workspace and, via `created_by`, its owner user id (needed by `isResourceOwner('Project', …)` on the sharing facade). CE `hasResourceScope` does not owner-isolate projects because `ProjectVisibilityProvider` is registered — visibility decides instead; the two must be registered together."* Imports: `com.bytechef.platform.user.service.UserService`, `java.util.OptionalLong`. Add `ProjectOwnershipResolverTest`:

```java
class ProjectOwnershipResolverTest {

    @Test
    void testResolvesWorkspaceAndOwnerFromCreatedBy() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        UserService userService = mock(UserService.class);
        Project project = new Project();

        project.setId(1L);
        project.setWorkspaceId(4L);
        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        User user = new User();

        user.setId(77L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userService.fetchUserByLogin("ivica")).thenReturn(Optional.of(user));

        ResourceOwner owner = new ProjectOwnershipResolver(projectRepository, userService).resolveOwner(1L);

        assertThat(owner.workspaceId()).hasValue(4L);
        assertThat(owner.ownerUserId()).hasValue(77L);
    }

    @Test
    void testUnknownProjectIsUnknownOwner() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(projectRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(new ProjectOwnershipResolver(projectRepository, mock(UserService.class)).resolveOwner(9L))
            .isEqualTo(ResourceOwner.unknown());
    }
}
```

- [ ] **Step 9: `ProjectVisibilityFilterTest`**

```java
class ProjectVisibilityFilterTest {

    @Test
    void testFilterKeepsOnlyResolvedIds() {
        ResourceVisibilityResolver resolver = (resourceType, workspaceId, candidates) -> {
            assertThat(resourceType).isEqualTo("Project");

            return Set.of(1L);
        };
        Project visible = project(1L);
        Project hidden = project(2L);

        ProjectVisibilityFilter filter = new ProjectVisibilityFilter(resolver);

        assertThat(filter.filterVisible(List.of(visible, hidden))).containsExactly(visible);
        assertThat(filter.visibleProjectIds(List.of())).isEmpty();
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setVisibility(ResourceVisibility.WORKSPACE);

        return project;
    }
}
```

- [ ] **Step 10: Run the four new test classes + the CE/EE permission tests**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectVisibilityProvidersTest' --tests '*ProjectVisibilityFilterTest' --tests '*ProjectOwnershipResolverTest' --tests '*PermissionService*Test' :server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service:test --tests '*JobVisibilityProviderTest' > /tmp/t4.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t4.log
```
Expected: exit 0. Then compile the whole server to be sure no hand-assembled `@SpringBootTest(classes=...)` context lost a bean:

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t4c.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t4c.log
```
If an `*IntTestConfiguration` assembles `ProjectOwnershipResolver` or `PermissionServiceImpl` by hand and now lacks a `UserService`/provider bean, add a mock `@Bean` there (CLAUDE.md "Adding a constructor collaborator…").

- [ ] **Step 11: Commit (providers + resolver together — spec §8)**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:spotlessApply :server:libs:automation:automation-configuration:automation-configuration-service:spotlessApply :server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service:spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ProjectVisibilityFilter.java server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/ server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-service/src/main/java/com/bytechef/automation/workflow/execution/security/JobVisibilityProvider.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/security/ server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-service/src/test/
git commit -m "4750 Register project, workflow, deployment and job visibility providers and resolve project owners"
```

---


- [ ] **Step 12: Close the third bypass — `hasWorkflowScope`**

Added 2026-08-19 after the Task 1 review surfaced it; neither the spec nor this plan originally covered it.
`PermissionService.hasWorkflowScope(String workflowId, String scope)` is a third project-keyed entry point
that bypasses visibility exactly as `hasWorkspaceScopeForProject` did. EE resolves workflow → project →
`hasWorkspaceScope(workspaceId, scope)` with no precondition; CE returns `SecurityUtils.isAuthenticated()`.
It is reachable in production: `WorkflowEditorSpringAIAgent` (ai-copilot-service) calls it with
`WORKFLOW_VIEW` to gate the copilot's workflow editor, so without this a non-grantee opens a workflow
inside a `PRIVATE` project.

This lands here, and not in Task 2, because the fix depends on the `WorkflowVisibilityProvider` from
Step 5 of this task. `WorkflowOwnershipResolver` (`resourceType() == "Workflow"`) already exists, so both
halves `hasResourceScope` needs are in place once Step 5 is committed.

Replace the body in BOTH editions with:

```java
    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        // A workflow-keyed check is a resource-scope check on the workflow: routing through hasResourceScope gives it
        // the visibility precondition, and WorkflowVisibilityProvider redirects the lookup to the owning project.
        return hasResourceScope(workflowId, "Workflow", scope);
    }
```

Tests — one per edition, added to the classes those editions already use
(`PermissionServiceVisibilityTest` EE, `PermissionServiceResourceTest` CE):

- a workflow whose project is `PRIVATE` and NOT granted → `hasWorkflowScope(workflowId, "WORKFLOW_VIEW")`
  is `false`, even though the caller holds `WORKFLOW_VIEW` in the workspace. Assert the scope is genuinely
  held, so the test fails if the precondition is removed rather than passing for the wrong reason.
- the same workflow with the project granted → `true`.

Confirm the EE test fails against the pre-Step-12 body before you change it.

Watch for callers that relied on the looser behaviour: run `WorkflowEditorSpringAIAgentTest` in
`server/libs/ai/ai-copilot/ai-copilot-service` as well — it stubs `hasWorkflowScope` directly, so it
should be unaffected, but confirm rather than assume.

- [ ] **Step 13: Commit the bypass fix**

```bash
./gradlew spotlessApply
git commit -am "4750 Route hasWorkflowScope through the resource-scope visibility precondition"
```

### Task 5: List surfaces filter through `ProjectVisibilityFilter`

**Files:**
- Modify: `.../automation-configuration-service/.../facade/ProjectFacadeImpl.java` (constructor; `getVisibleWorkspaceProjectIds` ~710; private `getProjects` ~718; `getWorkspaceLatestProjectWorkflows` ~453)
- Modify: `.../automation-configuration-service/.../facade/ProjectDeploymentFacadeImpl.java` (constructor; `filterOutSystemProjectDeployments` ~833)
- Modify: `.../automation-configuration-graphql/.../web/graphql/ProjectGraphQlController.java` (`project`, `projects`)
- Modify: `.../automation-configuration-service/.../search/ProjectSearchAssetProvider.java`, `WorkflowSearchAssetProvider.java`, `ProjectDeploymentSearchAssetProvider.java`
- Modify: `.../automation-workflow-execution-service/.../facade/ProjectWorkflowExecutionFacadeImpl.java` (constructor; `getWorkflowExecutions` ~193)
- Test: `.../automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectFacadeVisibilityTest.java` (new), `ProjectDeploymentFacadeTest.java` (extend), search provider tests (new `ProjectSearchAssetProviderTest.java`), `ProjectWorkflowExecutionFacadeVisibilityTest.java` (new)

**Interfaces:**
- Consumes: `ProjectVisibilityFilter.filterVisible / visibleProjectIds` (Task 4); `PermissionService.hasResourceScope`.

- [ ] **Step 1: Write the failing `ProjectFacadeVisibilityTest`**

The existing `ProjectFacadeImplCodeWorkflowFlagTest` (`ProjectFacadeImplCodeWorkflowFlagTest.java:100-107`) already builds a `ProjectFacadeImpl` with mocks; the new test copies that constructor call and adds the `ProjectVisibilityFilter` argument (Task 6 later prepends the `edition` string and appends the `ResourceVisibilityPolicyRegistry` — update BOTH test classes' constructor calls when those land):

```java
package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.validator.ErrorWorkflowConfigurationValidator;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
// + the same imports ProjectFacadeImplCodeWorkflowFlagTest uses for the remaining collaborators

class ProjectFacadeVisibilityTest {

    private static final long WORKSPACE_ID = 1L;

    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final TagService tagService = mock(TagService.class);

    private ProjectFacadeImpl projectFacade;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setPublicUrl("http://localhost");

        @SuppressWarnings("unchecked")
        ObjectProvider<ProjectCodeWorkflowInfoSupplier> supplierProvider = mock(ObjectProvider.class);

        when(supplierProvider.getIfAvailable()).thenReturn(null);

        // Only project 1 is visible; the resolver is the real seam every list surface shares.
        ResourceVisibilityResolver resolver = (resourceType, workspaceId, candidates) -> Set.of(1L);

        projectFacade = new ProjectFacadeImpl(
            applicationProperties, mock(CategoryService.class), mock(ComponentDefinitionHelper.class),
            mock(ErrorWorkflowConfigurationValidator.class), mock(PreBuiltTemplateService.class), supplierProvider,
            projectWorkflowService, mock(ProjectDeploymentService.class), projectService,
            new ProjectVisibilityFilter(resolver), mock(ProjectDeploymentFacade.class),
            mock(ProjectWorkflowFacade.class), mock(SharedTemplateFileStorage.class),
            mock(SharedTemplateService.class), tagService, mock(WorkflowService.class),
            mock(WorkflowTestConfigurationService.class), mock(WorkflowNodeTestOutputService.class));

        when(projectService.getProjects(null, null, null, null, null, WORKSPACE_ID))
            .thenReturn(List.of(project(1L), project(2L)));
        when(projectService.getProjects(null, null, false, null, null, WORKSPACE_ID))
            .thenReturn(List.of(project(1L), project(2L)));
        when(projectWorkflowService.getProjectWorkflows(any())).thenReturn(List.of());
        when(tagService.getTags(any())).thenReturn(List.of());
    }

    @Test
    void testWorkspaceProjectsHideWhatTheFilterHides() {
        List<ProjectDTO> projectDTOs =
            projectFacade.getWorkspaceProjects(null, null, false, null, null, null, WORKSPACE_ID);

        assertThat(projectDTOs).extracting(ProjectDTO::id).containsExactly(1L);
    }

    @Test
    void testWorkspaceProjectWorkflowsOnlySpanVisibleProjects() {
        projectFacade.getWorkspaceProjectWorkflows(WORKSPACE_ID);

        verify(projectWorkflowService).getProjectWorkflows(List.of(1L));
    }

    @Test
    void testWorkspaceLatestProjectWorkflowsOnlySpanVisibleProjects() {
        projectFacade.getWorkspaceLatestProjectWorkflows(WORKSPACE_ID);

        verify(projectWorkflowService).getProjectWorkflows(List.of(1L));
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setName("project-" + id);
        project.setWorkspaceId(WORKSPACE_ID);
        project.setVisibility(ResourceVisibility.WORKSPACE);

        return project;
    }
}
```

(Match `getWorkspaceProjects`'s real parameter order from `ProjectFacade` — `(apiCollections, categoryId, includeAllFields, projectDeployments, status, tagId, workspaceId)` per the REST controller call in the exploration notes; adjust the `when(...)` argument lists to whatever the private `getProjects` overload actually passes to `projectService.getProjects`, which the implementation step below preserves unchanged.)

- [ ] **Step 2: Run to verify it fails** (constructor arity mismatch → compile error).

- [ ] **Step 3: `ProjectFacadeImpl` — inject and apply**

Add `private final ProjectVisibilityFilter projectVisibilityFilter;` + constructor parameter (keep the parameter list alphabetical-ish: place `ProjectVisibilityFilter projectVisibilityFilter` right after `ProjectService projectService`) + assignment. Then:

`getVisibleWorkspaceProjectIds`:

```java
    /**
     * Workspace project ids minus the feature-owned system projects (see {@link SystemProjects}) and minus the projects
     * the current principal may not see.
     */
    private List<Long> getVisibleWorkspaceProjectIds(long workspaceId) {
        List<Project> projects = projectService.getProjects(null, null, null, null, null, workspaceId)
            .stream()
            .filter(project -> !SystemProjects.isSystemProject(project))
            .toList();

        return projectVisibilityFilter.filterVisible(projects)
            .stream()
            .map(Project::getId)
            .toList();
    }
```

Private `getProjects(...)` — replace the first statement's tail:

```java
        List<Project> projects = projectVisibilityFilter.filterVisible(
            projectService.getProjects(apiCollections, categoryId, projectDeployments, tagId, status, workspaceId)
                .stream()
                .filter(project -> !SystemProjects.isSystemProject(project))
                .toList());
```

`getWorkspaceLatestProjectWorkflows` — same wrap:

```java
        List<Project> projects = projectVisibilityFilter.filterVisible(
            projectService.getProjects(null, null, false, null, null, workspaceId)
                .stream()
                .filter(project -> !SystemProjects.isSystemProject(project))
                .toList());
```

- [ ] **Step 4: `ProjectDeploymentFacadeImpl`**

Inject `ProjectVisibilityFilter projectVisibilityFilter` (constructor + field). Rename `filterOutSystemProjectDeployments` → keep the name but extend the body so it drops hidden projects too:

```java
    /**
     * Drops the deployments whose project is feature-owned rather than user-created (see {@link SystemProjects}) or
     * hidden from the current principal — a deployment is only as visible as the project it deploys.
     */
    private List<ProjectDeployment> filterOutSystemProjectDeployments(List<ProjectDeployment> projectDeployments) {
        if (projectDeployments.isEmpty()) {
            return projectDeployments;
        }

        List<Project> projects = getProjects(projectDeployments);

        Set<Long> systemProjectIds = projects.stream()
            .filter(SystemProjects::isSystemProject)
            .map(Project::getId)
            .collect(Collectors.toSet());

        Set<Long> visibleProjectIds = projectVisibilityFilter.visibleProjectIds(projects);

        return projectDeployments.stream()
            .filter(projectDeployment -> !systemProjectIds.contains(projectDeployment.getProjectId()))
            .filter(projectDeployment -> visibleProjectIds.contains(projectDeployment.getProjectId()))
            .toList();
    }
```

Extend `ProjectDeploymentFacadeTest` (it already constructs the facade; add the new arg) with two tests, which together pin the invariant that a deployment is exactly as visible as its project:

```java
    @Test
    void testWorkspaceDeploymentsHideDeploymentsOfHiddenProjects() {
        // deployments over projects 1 (visible) and 2 (hidden); resolver returns Set.of(1L)
        List<ProjectDeploymentDTO> projectDeploymentDTOs =
            projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, null, null, null, false);

        assertThat(projectDeploymentDTOs).extracting(ProjectDeploymentDTO::projectId).containsExactly(1L);
    }

    @Test
    void testEveryDeploymentOfAHiddenProjectIsHidden() {
        // Three deployments of project 2 across DEV/STAGING/PRODUCTION; resolver returns Set.of() for it.
        // There is no per-deployment or per-environment opt-out: a PRODUCTION deployment of a private project
        // is as hidden as its DEV sibling.
        assertThat(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, null, null, null, false))
            .isEmpty();
    }
```

(Match `getWorkspaceProjectDeployments`'s real parameter order — `(id, environmentId, projectId, tagId, includeAllFields)` — and the DTO's accessor for the project id.) The by-id half is covered by `ProjectDeploymentVisibilityProvider` (Task 4) feeding the existing `hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_*')` gates; no deployment controller changes.

**Do not** touch the trigger-execution path while you are in here: a hidden project's deployments must keep receiving webhooks and firing schedules (see Global Constraints).

- [ ] **Step 5: `ProjectGraphQlController`**

Inject `PermissionService permissionService` and `ProjectVisibilityFilter projectVisibilityFilter` (both from `-api`). Replace the two queries:

```java
    @QueryMapping(name = "project")
    public Project project(@Argument long id) {
        // The projects listing goes through ProjectFacade's @PreAuthorize gates; this by-id lookup returns the entity
        // and so cannot, hence the explicit check — same visibility-preconditioned scope check as the facade's.
        if (!permissionService.hasResourceScope(id, ProjectVisibilityFilter.PROJECT, "WORKFLOW_VIEW")) {
            throw new AccessDeniedException("Project id=%s".formatted(id));
        }

        return projectService.getProject(id);
    }

    @QueryMapping(name = "projects")
    public List<Project> projects() {
        return projectVisibilityFilter.filterVisible(
            projectService.getProjects()
                .stream()
                .filter(project -> !SystemProjects.isSystemProject(project))
                .toList());
    }
```

Import `org.springframework.security.access.AccessDeniedException` and `PermissionService`/`ProjectVisibilityFilter`. Add `visibility: ResourceVisibility!` to `type Project` in `project.graphqls` (the enum is already declared in `connection.graphqls`; Spring GraphQL maps the entity's `getVisibility()` automatically).

- [ ] **Step 6: Search providers**

Each of the three gets `ProjectVisibilityFilter` injected. `ProjectSearchAssetProvider.search`:

```java
        List<Project> projects = projectVisibilityFilter.filterVisible(
            projectService.getProjects(false, null, null, null, null, null)
                .stream()
                .filter(project -> !SystemProjects.isSystemProject(project))
                .toList());

        return projects.stream()
            .filter(
                project -> containsIgnoreCase(project.getName(), queryLower) ||
                    containsIgnoreCase(project.getDescription(), queryLower))
            .limit(limit)
            .map(project -> new ProjectSearchResult(
                project.getId(), project.getName(), project.getDescription(), project.getWorkspaceId()))
            .toList();
```

`WorkflowSearchAssetProvider` — after building `projectIds`, load projects once and derive both maps from the *visible* subset:

```java
        List<Project> visibleProjects = projectVisibilityFilter.filterVisible(projectService.getProjects(projectIds));

        Map<Long, Long> projectIdToWorkspaceId = visibleProjects.stream()
            .collect(Collectors.toMap(Project::getId, Project::getWorkspaceId));

        return projectWorkflows.stream()
            .filter(projectWorkflow -> projectIdToWorkspaceId.containsKey(projectWorkflow.getProjectId()))
            .filter(projectWorkflow -> { ...existing label/description match... })
            ...
```

`ProjectDeploymentSearchAssetProvider` — build `projectMap` from `projectVisibilityFilter.filterVisible(projectService.getProjects(projectIds))`; the existing `project != null` guard then also drops hidden ones.

Add `ProjectSearchAssetProviderTest`: two projects, resolver `{1}`, query matching both → only project 1's result.

- [ ] **Step 7: Executions with an explicit filter id**

In `ProjectWorkflowExecutionFacadeImpl` inject `PermissionService permissionService` and change the head of `getWorkflowExecutions`:

```java
        List<String> workflowIds = new ArrayList<>();

        if (workflowId != null) {
            requireResourceScope(workflowId, "Workflow", "EXECUTION_VIEW");

            workflowIds.add(workflowId);
        } else if (projectId != null) {
            requireResourceScope(projectId, "Project", "EXECUTION_VIEW");

            workflowIds.addAll(projectWorkflowService.getProjectWorkflowIds(projectId));
        } else {
            workflowIds.addAll(
                CollectionUtils.map(
                    projectFacade.getWorkspaceProjectWorkflows(workspaceId), ProjectWorkflowDTO::getId));
        }

        if (projectDeploymentId != null) {
            requireResourceScope(projectDeploymentId, "ProjectDeployment", "EXECUTION_VIEW");
        }
```

with

```java
    /**
     * An explicit filter id names a resource by id, so it gets the by-id semantics: a hidden project/deployment/workflow
     * is denied rather than silently emptied — the client never sends an id it cannot see.
     */
    private void requireResourceScope(Serializable id, String resourceType, String scope) {
        if (!permissionService.hasResourceScope(id, resourceType, scope)) {
            throw new AccessDeniedException("%s id=%s".formatted(resourceType, id));
        }
    }
```

Add `ProjectWorkflowExecutionFacadeVisibilityTest` with a mocked `PermissionService` returning `false` for `(7L, "Project", "EXECUTION_VIEW")` → `getWorkflowExecutions(null, null, null, null, null, 7L, null, null, 1L, 0)` throws `AccessDeniedException`; and returning `true` → no throw (rest mocked to return empty).

- [ ] **Step 8: Run affected tests + full compile**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test :server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service:test :server:libs:automation:automation-configuration:automation-configuration-graphql:test > /tmp/t5.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t5.log
./gradlew compileJava compileTestJava --continue > /tmp/t5c.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t5c.log
```
Expected: exit 0 both. Fix any hand-assembled test contexts that construct these facades (grep `new ProjectFacadeImpl(`, `new ProjectDeploymentFacadeImpl(`, `new ProjectWorkflowExecutionFacadeImpl(` under `src/test`).

- [ ] **Step 9: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectFacadeImpl.java server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeImpl.java server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectGraphQlController.java server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project.graphqls server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/search/ server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-service/src/main/java/com/bytechef/automation/workflow/execution/facade/ProjectWorkflowExecutionFacadeImpl.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/ server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-service/src/test/
git commit -m "4750 Filter project, workflow, deployment, execution and search listings by project visibility"
```

---

### Task 6: Create path (CE force / EE validate) + REST `visibility` on `Project`

> **Hazard (added 2026-08-19, from the Task 3 review).** `ProjectVisibilityPolicy` is a `@Component` in
> `automation-configuration-service` (matching the `ConnectionVisibilityPolicy` precedent). Distributed-EE
> apps that carry `automation-configuration-api` + `-remote-client` WITHOUT `-service` therefore have no
> `"Project"` entry in the registry — where `supports(...)` returns `false` and `defaultVisibility("Project")`
> **throws** `IllegalArgumentException`. Before wiring the create path to the registry, confirm its call
> sites either do not run in those apps or tolerate the fail-closed path; do not let a project create throw
> there. This is the same shape as the repo's standing rule that beans consumed across deployments belong in
> `-api`, not `-service` (CLAUDE.md, EE Microservice Remote Client Pattern — it bit once already with
> `ResourceVisibilityPolicyRegistry`).

**Files:**
- Modify: `ProjectFacadeImpl.createProject` (+ constructor: `@Value("${bytechef.edition:CE}") String edition`, `ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry`)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/openapi.yaml` (`ProjectBasic.visibility`) — then regenerate
- Regenerated: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-api/generated/**`, `client/src/shared/middleware/automation/configuration/**`
- Test: `ProjectFacadeVisibilityTest` (extend)

**Interfaces:**
- Consumes: `ResourceVisibilityPolicyRegistry.supports/defaultVisibility` (existing), `ProjectDTO.visibility()` (Task 3).
- Produces: REST `Project.visibility` (`PRIVATE` | `WORKSPACE`), read on every project, accepted on create; MapStruct maps `ProjectModel.VisibilityEnum` ↔ `ResourceVisibility` by constant name.

- [ ] **Step 1: Write the failing tests**

In `ProjectFacadeVisibilityTest`:

```java
    @Test
    void testCeForcesWorkspaceVisibilityOnCreate() {
        ProjectFacadeImpl facade = facade("CE");
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        when(projectService.create(captor.capture())).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        facade.createProject(ProjectDTO.builder().name("p").workspaceId(1L).visibility(ResourceVisibility.PRIVATE).build());

        assertThat(captor.getValue().getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testEeHonoursRequestedVisibilityOnCreate() {
        ProjectFacadeImpl facade = facade("EE");
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

        when(projectService.create(captor.capture())).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        facade.createProject(ProjectDTO.builder().name("p").workspaceId(1L).visibility(ResourceVisibility.PRIVATE).build());

        assertThat(captor.getValue().getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testEeRejectsUnsupportedVisibilityOnCreate() {
        ProjectFacadeImpl facade = facade("EE");

        assertThatThrownBy(() -> facade.createProject(
            ProjectDTO.builder().name("p").workspaceId(1L).visibility(ResourceVisibility.ORGANIZATION).build()))
                .isInstanceOf(ConfigurationException.class);
    }
```

`facade(edition)` builds the impl with a real `new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy()))`; `withId` sets the id on the captured project and returns it.

- [ ] **Step 2: Run to verify failure** (constructor arity / no forcing yet).

- [ ] **Step 3: Implement in `ProjectFacadeImpl`**

Fields:

```java
    private final boolean eeEdition;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
```

Constructor: add `@Value("${bytechef.edition:CE}") String edition` as the FIRST parameter and `ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry` after `projectVisibilityFilter`; then

```java
        validateEdition(edition);

        this.eeEdition = "EE".equalsIgnoreCase(edition);
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
```

`createProject` — after `Project project = projectDTO.toProject();`:

```java
        applyCreateVisibility(project, projectDTO.visibility());
```

with

```java
    /**
     * CE force-writes WORKSPACE (there is no authorization boundary between members to withhold from); EE takes the
     * request, defaulting to the policy default and rejecting rungs the project type does not support.
     */
    private void applyCreateVisibility(Project project, ResourceVisibility requestedVisibility) {
        if (!eeEdition) {
            if (requestedVisibility != null && requestedVisibility != ResourceVisibility.WORKSPACE
                && logger.isInfoEnabled()) {

                logger.info("Forcing WORKSPACE visibility for project (requested={}, eeEdition=false)",
                    requestedVisibility);
            }

            project.setVisibility(ResourceVisibility.WORKSPACE);

            return;
        }

        ResourceVisibility visibility = requestedVisibility == null
            ? resourceVisibilityPolicyRegistry.defaultVisibility(ProjectVisibilityFilter.PROJECT)
            : requestedVisibility;

        if (!resourceVisibilityPolicyRegistry.supports(ProjectVisibilityFilter.PROJECT, visibility)) {
            throw new ConfigurationException(
                "Project does not support %s visibility".formatted(visibility), ProjectErrorType.UNSUPPORTED_VISIBILITY);
        }

        project.setVisibility(visibility);
    }

    private static void validateEdition(String edition) {
        if (edition == null || !("CE".equalsIgnoreCase(edition) || "EE".equalsIgnoreCase(edition))) {
            throw new IllegalStateException(
                "bytechef.edition must be CE or EE (case-insensitive); got '" + edition + "'");
        }
    }
```

(`ProjectFacadeImpl` already has a `logger`? If not, add `private static final Logger logger = LoggerFactory.getLogger(ProjectFacadeImpl.class);`.) `duplicateProject`, `importProjectTemplate` use `new Project()` / `new ProjectDTO(project)` whose visibility is the field default `WORKSPACE`; the import path also flows through `createProject`, so both write the default (spec §9.1).

- [ ] **Step 4: OpenAPI**

In `ProjectBasic.properties` (after `uuid`):

```yaml
        visibility:
          description: "The visibility scope of the project: WORKSPACE (default, shared with every member of the owning workspace) or PRIVATE (owner plus named grantees, EE). Accepted on create; changed afterwards via the setProjectVisibility GraphQL mutation. CE always persists WORKSPACE."
          type: "string"
          enum:
            - "PRIVATE"
            - "WORKSPACE"
```

Also fix the stale connection description at line ~1253 to: `"The visibility scope of the connection. Accepted on create; defaults to WORKSPACE (shared with the workspace). PRIVATE withholds it; ORGANIZATION is not user-settable here."`

Regenerate:

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl:generateOpenAPI > /tmp/gen.log 2>&1; echo $?
```

Then `git status` — expect changes under `automation-configuration-rest-api/generated/` (`ProjectBasicModel.java`, `ProjectModel.java`) and `client/src/shared/middleware/automation/configuration/models/{Project,ProjectBasic}.ts`. MapStruct's `ProjectDTOToProjectModelMapper` maps `visibility` by name (enum constant names match) — verify by compiling `automation-configuration-rest-impl`.

- [ ] **Step 5: Run tests + compile**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectFacadeVisibilityTest' :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl:compileJava > /tmp/t6.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t6.log
```

- [ ] **Step 6: Commit server + generated separately**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:spotlessApply
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectFacadeImpl.java server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectFacadeVisibilityTest.java server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/openapi.yaml server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-api/generated
git commit -m "4750 Force WORKSPACE project visibility in CE, validate the requested rung in EE, expose visibility on the project REST model"
git add client/src/shared/middleware/automation/configuration
git commit -m "4750 client - Regenerate the automation configuration client for project visibility"
```

---

### Task 7: EE `ProjectSharingFacade`, audit events, delete cleanup, GraphQL

> **Validate the rung before persisting it (added 2026-08-19, from the Task 6 review).**
> `ProjectServiceImpl.updateVisibility(id, visibility)` writes whatever it is handed, with NO policy check —
> it is a plain setter-and-save. Today that is harmless because it has no production caller. This task gives
> it one, and that makes it **the only path in the system that can persist a rung the project model does not
> support**. Guard it here: validate against `ResourceVisibilityPolicyRegistry.supports("Project", visibility)`
> and reject with the task's error type before calling the service.
>
> Concretely, an unguarded `ORGANIZATION` would not fail quietly — Task 6 added
> `@ValueMapping(source = "ORGANIZATION", target = THROW_EXCEPTION)` to `ProjectMapper` and EE
> `ApiCollectionMapper`, deliberately, so that a corrupt row is loud rather than plausible. A row written
> here would therefore make every subsequent READ of that project throw. The spec already requires rejecting
> `ORGANIZATION` at this mutation (§9); this note is about where the check has to live for that to be true.

> **Do not double-audit (added 2026-08-19, from the Task 3 review).** Task 3's
> `ProjectServiceImpl.updateVisibility` already emits a generic `ProjectAuditEvent.PROJECT_UPDATED` carrying
> a `toVisibility` key. This task adds the dedicated `PROJECT_VISIBILITY_CHANGED` event at the facade. If
> both stay, one visibility change is audited twice, under two different event types — which is worse than
> either alone, because a log reader cannot tell whether two entries mean two changes. Resolve it: the
> dedicated facade-level event is the one to keep (it mirrors the connection precedent,
> `CONNECTION_VISIBILITY_CHANGED` emitted from `WorkspaceConnectionFacadeImpl`, and it is `strictAudit`), so
> remove the service-level emission as part of this task. Note the CE consequence and state it in your
> report: the sharing facade is EE-only, so removing the service-level event means a CE-side
> `updateVisibility` (if any caller reaches one) is no longer audited — confirm whether any CE caller
> exists before removing, and if one does, say so rather than silently dropping its audit trail.

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacade.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacadeImpl.java`
- Modify: `.../automation-configuration-service/.../audit/ProjectAuditEvent.java` (CE)
- Modify: `server/ee/.../automation-configuration-service/.../event/ProjectBeforeDeleteEventListener.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-sharing.graphqls`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/ProjectSharingGraphQlController.java`
- Test: `server/ee/.../automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacadeTest.java`, `ProjectSharingFacadeAuthorizationTest.java` (both new)

**Interfaces:**
- Consumes: `ProjectService.getProject/updateVisibility` (Task 3), `ResourceGrantService.grant/revoke/getGrantedUserIds/deleteGrants`, `WorkspaceUserService.fetchWorkspaceUser(userId, workspaceId)`, `ResourceVisibilityPolicyRegistry`, `ProjectAuditPublisher.publish(event, projectId, Map)`, `ProjectErrorType` (Task 3).
- Produces: `ProjectSharingFacade` with `setProjectVisibility(long workspaceId, long projectId, ResourceVisibility)`, `grantProjectAccess(long workspaceId, long projectId, long userId)`, `revokeProjectAccess(long workspaceId, long projectId, long userId)`, `getProjectGrants(long workspaceId, long projectId): List<Long>`; GraphQL query `projectGrants`, mutations `setProjectVisibility`, `grantProjectAccess`, `revokeProjectAccess`.

- [ ] **Step 1: Write the failing authorization test** (`@version ee`, EE header)

```java
class ProjectSharingFacadeAuthorizationTest {

    private static final String OWNER_OR_ADMIN_EXPRESSION =
        "@permissionService.isResourceOwner('Project', #projectId) || " +
            "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')";

    @ParameterizedTest
    @ValueSource(strings = {"setProjectVisibility", "grantProjectAccess", "revokeProjectAccess", "getProjectGrants"})
    void testSharingMethodsRequireOwnerOrAdmin(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must carry @PreAuthorize", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(OWNER_OR_ADMIN_EXPRESSION);
    }

    @Test
    void testEverySharingMethodIsCovered() {
        List<String> annotatedMethods = Arrays.stream(ProjectSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .distinct()
            .sorted()
            .toList();

        assertThat(annotatedMethods)
            .containsExactly("getProjectGrants", "grantProjectAccess", "revokeProjectAccess", "setProjectVisibility");
    }

    private static Method findMethod(String methodName) {
        return Arrays.stream(ProjectSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(methodName))
            .filter(method -> !method.isSynthetic())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected '" + methodName + "' on ProjectSharingFacadeImpl"));
    }
}
```

- [ ] **Step 2: Write the failing behaviour test** `ProjectSharingFacadeTest`

```java
class ProjectSharingFacadeTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long OTHER_WORKSPACE_ID = 2L;
    private static final long PROJECT_ID = 10L;
    private static final long USER_ID = 7L;

    private final ProjectService projectService = mock(ProjectService.class);
    private final ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);
    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);
    private final ProjectAuditPublisher projectAuditPublisher = mock(ProjectAuditPublisher.class);

    private ProjectSharingFacadeImpl facade;

    @BeforeEach
    void setUp() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(WORKSPACE_ID);

        when(projectService.getProject(PROJECT_ID)).thenReturn(project);
        when(workspaceUserService.fetchWorkspaceUser(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(mock(WorkspaceUser.class)));

        facade = new ProjectSharingFacadeImpl(
            projectAuditPublisher, projectService, resourceGrantService,
            new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())), workspaceUserService);
    }

    @Test
    void testSetVisibilityUpdatesAndAudits() {
        facade.setProjectVisibility(WORKSPACE_ID, PROJECT_ID, ResourceVisibility.PRIVATE);

        verify(projectService).updateVisibility(PROJECT_ID, ResourceVisibility.PRIVATE);
        verify(projectAuditPublisher).publish(
            ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, PROJECT_ID, Map.of("toVisibility", "PRIVATE"));
    }

    @Test
    void testSetVisibilityRejectsOrganization() {
        assertThatThrownBy(() -> facade.setProjectVisibility(WORKSPACE_ID, PROJECT_ID, ResourceVisibility.ORGANIZATION))
            .isInstanceOf(ConfigurationException.class);

        verify(projectService, never()).updateVisibility(anyLong(), any());
    }

    @Test
    void testSetVisibilityRejectsProjectOutsideWorkspace() {
        assertThatThrownBy(() -> facade.setProjectVisibility(OTHER_WORKSPACE_ID, PROJECT_ID, ResourceVisibility.PRIVATE))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testGrantValidatesMembershipThenGrantsAndAudits() {
        facade.grantProjectAccess(WORKSPACE_ID, PROJECT_ID, USER_ID);

        verify(resourceGrantService).grant("Project", PROJECT_ID, USER_ID);
        verify(projectAuditPublisher).publish(
            ProjectAuditEvent.PROJECT_ACCESS_GRANTED, PROJECT_ID, Map.of("targetUserId", USER_ID));
    }

    @Test
    void testGrantToNonMemberFailsWithTheUnknownProjectError() {
        when(workspaceUserService.fetchWorkspaceUser(99L, WORKSPACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.grantProjectAccess(WORKSPACE_ID, PROJECT_ID, 99L))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorType())
            .isEqualTo(ProjectErrorType.INVALID_PROJECT);

        verify(resourceGrantService, never()).grant(any(), anyLong(), anyLong());
    }

    @Test
    void testRevokeSkipsMembershipCheck() {
        facade.revokeProjectAccess(WORKSPACE_ID, PROJECT_ID, 99L);

        verify(resourceGrantService).revoke("Project", PROJECT_ID, 99L);
        verify(workspaceUserService, never()).fetchWorkspaceUser(anyLong(), anyLong());
    }

    @Test
    void testGetGrantsReturnsUserIds() {
        when(resourceGrantService.getGrantedUserIds("Project", PROJECT_ID)).thenReturn(List.of(USER_ID));

        assertThat(facade.getProjectGrants(WORKSPACE_ID, PROJECT_ID)).containsExactly(USER_ID);
    }
}
```

(Check `ConfigurationException`'s accessor name for the error type — grep `class ConfigurationException` in `server/libs/core/exception`; adjust the `extracting` accordingly.)

- [ ] **Step 3: Run both tests to verify they fail to compile**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectSharingFacade*' > /tmp/t7.log 2>&1; echo $?; grep -E "cannot find symbol" /tmp/t7.log | head -3
```

- [ ] **Step 4: Audit events (CE enum)**

Append to `ProjectAuditEvent`:

```java
    PROJECT_DELETED,

    /**
     * The project's reach changed. Payload: {@code projectId}, {@code toVisibility} (the new
     * {@code ResourceVisibility} name).
     */
    PROJECT_VISIBILITY_CHANGED,

    /**
     * A named workspace member was granted access to a withheld project. Payload: {@code projectId},
     * {@code targetUserId}.
     */
    PROJECT_ACCESS_GRANTED,

    /**
     * A grant was revoked. Payload: {@code projectId}, {@code targetUserId}.
     */
    PROJECT_ACCESS_REVOKED
```

- [ ] **Step 5: `ProjectSharingFacade` (EE api)**

```java
package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;

/**
 * Visibility and named-user grants for automation projects (resource-visibility phase 2). Authorization is
 * owner-or-admin, annotated on the implementation so it protects every caller; the GraphQL controller only maps
 * arguments. Validation order is authorize → project-in-workspace → grantee-in-workspace, and every validation failure
 * collapses to {@code ProjectErrorType.INVALID_PROJECT} so a caller cannot enumerate user ids.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectSharingFacade {

    List<Long> getProjectGrants(long workspaceId, long projectId);

    void grantProjectAccess(long workspaceId, long projectId, long userId);

    void revokeProjectAccess(long workspaceId, long projectId, long userId);

    void setProjectVisibility(long workspaceId, long projectId, ResourceVisibility visibility);
}
```

- [ ] **Step 6: `ProjectSharingFacadeImpl` (EE service)**

```java
package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.audit.ProjectAuditEvent;
import com.bytechef.automation.configuration.audit.ProjectAuditPublisher;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ProjectSharingFacadeImpl implements ProjectSharingFacade {

    private static final String PROJECT = ProjectVisibilityFilter.PROJECT;

    private final ProjectAuditPublisher projectAuditPublisher;
    private final ProjectService projectService;
    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public ProjectSharingFacadeImpl(
        ProjectAuditPublisher projectAuditPublisher, ProjectService projectService,
        ResourceGrantService resourceGrantService, ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry,
        WorkspaceUserService workspaceUserService) {

        this.projectAuditPublisher = projectAuditPublisher;
        this.projectService = projectService;
        this.resourceGrantService = resourceGrantService;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public List<Long> getProjectGrants(long workspaceId, long projectId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);

        return resourceGrantService.getGrantedUserIds(PROJECT, projectId);
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void grantProjectAccess(long workspaceId, long projectId, long userId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);
        validateGranteeIsWorkspaceMember(workspaceId, userId);

        resourceGrantService.grant(PROJECT, projectId, userId);

        projectAuditPublisher.publish(ProjectAuditEvent.PROJECT_ACCESS_GRANTED, projectId, Map.of("targetUserId", userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void revokeProjectAccess(long workspaceId, long projectId, long userId) {
        validateProjectBelongsToWorkspace(workspaceId, projectId);

        // No membership check on the way out: someone removed from the workspace must still be revocable, and
        // revoking a grant that is not there is already a no-op.
        resourceGrantService.revoke(PROJECT, projectId, userId);

        projectAuditPublisher.publish(ProjectAuditEvent.PROJECT_ACCESS_REVOKED, projectId, Map.of("targetUserId", userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) || " +
        "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")
    public void setProjectVisibility(long workspaceId, long projectId, ResourceVisibility visibility) {
        if (!resourceVisibilityPolicyRegistry.supports(PROJECT, visibility)) {
            throw new ConfigurationException(
                "Project does not support %s visibility".formatted(visibility), ProjectErrorType.UNSUPPORTED_VISIBILITY);
        }

        validateProjectBelongsToWorkspace(workspaceId, projectId);

        // Unlike connections there is no "not while deployed" rule: a project's deployments inherit its visibility, so
        // narrowing leaves nothing dangling.
        projectService.updateVisibility(projectId, visibility);

        projectAuditPublisher.publish(
            ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, projectId, Map.of("toVisibility", visibility.name()));
    }

    private void validateProjectBelongsToWorkspace(long workspaceId, long projectId) {
        Project project = projectService.getProject(projectId);

        if (!Objects.equals(project.getWorkspaceId(), workspaceId)) {
            throw new ConfigurationException(
                "Project id=%s does not belong to workspace id=%s".formatted(projectId, workspaceId),
                ProjectErrorType.INVALID_PROJECT);
        }
    }

    private void validateGranteeIsWorkspaceMember(long workspaceId, long userId) {
        if (workspaceUserService.fetchWorkspaceUser(userId, workspaceId)
            .isEmpty()) {

            // Deliberately the same error as an unknown project: a grantor must not be able to enumerate user ids by
            // distinguishing "no such user" from "not a member of this workspace".
            throw new ConfigurationException(
                "User id=%s is not a member of workspace id=%s".formatted(userId, workspaceId),
                ProjectErrorType.INVALID_PROJECT);
        }
    }
}
```

`ProjectServiceImpl.updateVisibility` (Task 3) already publishes a `PROJECT_UPDATED` audit; that is the generic row-changed trail — the facade's `PROJECT_VISIBILITY_CHANGED` is the sharing-specific one. Keep both (the connection precedent also emits both a service-level update and the sharing audit).

- [ ] **Step 7: Delete cleanup**

In `ProjectBeforeDeleteEventListener` add `ResourceGrantService resourceGrantService` (constructor + field) and as the FIRST statement of `onBeforeDelete` after computing `projectId`:

```java
        // Grants first: resource_grant.resource_id is polymorphic and carries no foreign key, so a grant left behind
        // would attach to whatever later recycles this id.
        resourceGrantService.deleteGrants(ProjectVisibilityFilter.PROJECT, projectId);
```

- [ ] **Step 8: GraphQL**

`project-sharing.graphqls`:

```graphql
extend type Query {
    """The users a private project has been granted to. Owner or admin only — an ordinary viewer of a shared project must not learn who else it was handed to. (EE only)"""
    projectGrants(workspaceId: ID!, projectId: ID!): [Long!]!
}

extend type Mutation {
    """Set a project's reach. PRIVATE withholds it (and its workflows, deployments and executions) from the workspace; WORKSPACE shares it. ORGANIZATION is not supported for projects. (owner or admin, EE only)"""
    setProjectVisibility(workspaceId: ID!, projectId: ID!, visibility: ResourceVisibility!): Boolean!
    """Grant a named workspace member access to a project its owner has withheld. Idempotent. (owner or admin, EE only)"""
    grantProjectAccess(workspaceId: ID!, projectId: ID!, userId: ID!): Boolean!
    """Revoke a grant. Silent when no grant exists. (owner or admin, EE only)"""
    revokeProjectAccess(workspaceId: ID!, projectId: ID!, userId: ID!): Boolean!
}
```

`ProjectSharingGraphQlController` — a verbatim analogue of `ConnectionSharingGraphQlController` (`@Controller @ConditionalOnEEVersion @ConditionalOnCoordinator`, EE header, `@version ee`), four methods mapping to `ProjectSharingFacade` and returning `true`/the list.

- [ ] **Step 9: Run EE tests + full compile**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test :server:ee:libs:automation:automation-configuration:automation-configuration-graphql:compileJava > /tmp/t7.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t7.log
./gradlew compileJava compileTestJava --continue > /tmp/t7c.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/t7c.log
```
If an EE `*IntTestConfiguration` builds `ProjectBeforeDeleteEventListener` by hand, add a mock `ResourceGrantService` bean there.

- [ ] **Step 10: Commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/audit/ProjectAuditEvent.java server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacade.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacadeImpl.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/event/ProjectBeforeDeleteEventListener.java server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-sharing.graphqls server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/ProjectSharingGraphQlController.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacadeTest.java server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/facade/ProjectSharingFacadeAuthorizationTest.java
git commit -m "4750 Add the EE project sharing facade, audit events and GraphQL surface"
```

---

### Task 8: Client — generalize picker, badge and hook

**Files:**
- Move (git mv): `client/src/shared/components/connection/ConnectionVisibilityPicker.tsx` → `client/src/shared/components/visibility/ResourceVisibilityPicker.tsx` (+ `.test.tsx`)
- Move: `client/src/pages/automation/connections/components/ConnectionScopeBadge.tsx` → `client/src/shared/components/visibility/ResourceVisibilityBadge.tsx` (+ `.test.tsx`)
- Move: `client/src/pages/automation/connections/hooks/useVisibilityFeatureEnabled.ts` → `client/src/shared/hooks/useVisibilityFeatureEnabled.ts`; `client/src/pages/automation/connections/hooks/tests/useVisibilityFeatureEnabled.test.ts` → `client/src/shared/hooks/tests/useVisibilityFeatureEnabled.test.ts`
- Modify importers: `ConnectionListItem.tsx`, `ConnectionDialog.tsx`, `ConnectionReassignmentDialog.tsx`, `ConnectionTabConnectionSelect.tsx`, `SelectConnectionMessage.tsx` (+ its test), and any test that mocks the old paths (`grep -rn "useVisibilityFeatureEnabled\|ConnectionScopeBadge\|ConnectionVisibilityPicker" client/src`).

**Interfaces:**
- Produces: `export default ResourceVisibilityPicker` (props unchanged, interface renamed `ResourceVisibilityPickerPropsI`), `export type ResourceVisibilityStateType`, `export type ResourceVisibilityValueType = 'ORGANIZATION' | 'PRIVATE' | 'WORKSPACE'`, `export const deriveVisibilityState`, `export interface WorkspaceMemberI` from `@/shared/components/visibility/ResourceVisibilityPicker`; `export default ResourceVisibilityBadge` (`{grantedUserCount?: number; visibility: ResourceVisibilityValueType}`) from `@/shared/components/visibility/ResourceVisibilityBadge`; `useVisibilityFeatureEnabled`, `useIsVisibilityEditionEnabled`, `VisibilityFeatureType` from `@/shared/hooks/useVisibilityFeatureEnabled`.

- [ ] **Step 1: Move the files**

```bash
cd client
mkdir -p src/shared/components/visibility src/shared/hooks/tests
git mv src/shared/components/connection/ConnectionVisibilityPicker.tsx src/shared/components/visibility/ResourceVisibilityPicker.tsx
git mv src/shared/components/connection/ConnectionVisibilityPicker.test.tsx src/shared/components/visibility/ResourceVisibilityPicker.test.tsx
git mv src/pages/automation/connections/components/ConnectionScopeBadge.tsx src/shared/components/visibility/ResourceVisibilityBadge.tsx
git mv src/pages/automation/connections/components/ConnectionScopeBadge.test.tsx src/shared/components/visibility/ResourceVisibilityBadge.test.tsx
git mv src/pages/automation/connections/hooks/useVisibilityFeatureEnabled.ts src/shared/hooks/useVisibilityFeatureEnabled.ts
git mv src/pages/automation/connections/hooks/tests/useVisibilityFeatureEnabled.test.ts src/shared/hooks/tests/useVisibilityFeatureEnabled.test.ts
```

- [ ] **Step 2: Rename symbols inside the moved files**

In `ResourceVisibilityPicker.tsx`: `ConnectionVisibilityStateType` → `ResourceVisibilityStateType`; `ConnectionVisibilityPickerPropsI` → `ResourceVisibilityPickerPropsI`; component const + default export → `ResourceVisibilityPicker`; add and use:

```ts
export type ResourceVisibilityValueType = 'ORGANIZATION' | 'PRIVATE' | 'WORKSPACE';
```

for the `visibility` prop and `onVisibilityChange` parameter (they were inline unions). In `ResourceVisibilityBadge.tsx`: interface → `ResourceVisibilityBadgePropsI`, `visibility: ResourceVisibilityValueType` (import from the picker; drop the `ConnectionVisibilityEnum` import), `VISIBILITY_CONFIG: Record<ResourceVisibilityValueType, …>`, component/default export → `ResourceVisibilityBadge`; update the comment to say the union mirrors GraphQL `ResourceVisibility` and that a new server value falls back to PRIVATE. Update `data-testid`/aria strings only if they contain "connection" generically (keep any that tests assert on, and update those tests in lockstep). Update the two moved test files' imports and `describe` names.

- [ ] **Step 3: Update every importer**

```bash
grep -rln "ConnectionVisibilityPicker\|ConnectionScopeBadge\|connections/hooks/useVisibilityFeatureEnabled" src | xargs sed -i '' \
  -e "s#@/shared/components/connection/ConnectionVisibilityPicker#@/shared/components/visibility/ResourceVisibilityPicker#g" \
  -e "s#@/pages/automation/connections/components/ConnectionScopeBadge#@/shared/components/visibility/ResourceVisibilityBadge#g" \
  -e "s#'../ConnectionScopeBadge'#'@/shared/components/visibility/ResourceVisibilityBadge'#g" \
  -e "s#@/pages/automation/connections/hooks/useVisibilityFeatureEnabled#@/shared/hooks/useVisibilityFeatureEnabled#g" \
  -e "s#\bConnectionVisibilityPicker\b#ResourceVisibilityPicker#g" \
  -e "s#\bConnectionScopeBadge\b#ResourceVisibilityBadge#g" \
  -e "s#\bConnectionVisibilityStateType\b#ResourceVisibilityStateType#g"
```

Then hand-check each touched file for `vi.mock('…useVisibilityFeatureEnabled')` paths and relative imports (`ConnectionListItem.tsx` imported the badge relatively). Fix import sort order (`bytechef/sort-import-destructures`, path grouping) — run `npm run lint` and fix reports.

- [ ] **Step 4: Run the client checks**

```bash
cd client && npm run format && npm run check > /tmp/c8.log 2>&1; echo $?; tail -30 /tmp/c8.log
```
Expected: exit 0; the moved tests pass under their new names.

- [ ] **Step 5: Commit**

```bash
git add -A src/shared/components/visibility src/shared/hooks src/shared/components/connection src/pages/automation/connections src/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTabConnectionSelect.tsx src/shared/components/ai-chat/messages
git commit -m "4750 client - Generalize the connection visibility picker, badge and edition hook for reuse by projects"
```

---

### Task 9: Client — project GraphQL documents, create-dialog picker, list-item badge dropdown

**Files:**
- Create: `client/src/graphql/automation/configuration/projectGrants.graphql`, `setProjectVisibility.graphql`, `grantProjectAccess.graphql`, `revokeProjectAccess.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts` (+ `graphql-types.ts`)
- Modify: `client/src/pages/automation/projects/components/ProjectDialog.tsx`
- Modify: `client/src/pages/automation/projects/components/project-list/ProjectListItem.tsx`
- Create: `client/src/shared/hooks/useProjectVisibility.ts` (shared by both edit surfaces)
- Create: `client/src/pages/automation/project/components/ProjectVisibilityDialog.tsx`
- Modify: `client/src/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu.tsx`, `.../settings-menu/components/ProjectTabButtons/ProjectTabButtons.tsx` (+ its existing test)
- Test: `client/src/pages/automation/projects/components/project-list/ProjectListItem.visibility.test.tsx` (new), `client/src/pages/automation/projects/components/ProjectDialog.test.tsx` (new)

**Interfaces:**
- Consumes: `ResourceVisibilityPicker`, `ResourceVisibilityBadge`, `useVisibilityFeatureEnabled`, `useIsVisibilityEditionEnabled` (Task 8); generated `Project.visibility?: ProjectVisibilityEnum` (Task 6); `useWorkspaceUsersQuery` (existing).
- Produces: generated hooks `useProjectGrantsQuery`, `useSetProjectVisibilityMutation`, `useGrantProjectAccessMutation`, `useRevokeProjectAccessMutation`.

- [ ] **Step 1: GraphQL documents**

```graphql
# projectGrants.graphql
query ProjectGrants($workspaceId: ID!, $projectId: ID!) {
    projectGrants(workspaceId: $workspaceId, projectId: $projectId)
}
```
```graphql
# setProjectVisibility.graphql
mutation SetProjectVisibility($workspaceId: ID!, $projectId: ID!, $visibility: ResourceVisibility!) {
    setProjectVisibility(workspaceId: $workspaceId, projectId: $projectId, visibility: $visibility)
}
```
```graphql
# grantProjectAccess.graphql
mutation GrantProjectAccess($workspaceId: ID!, $projectId: ID!, $userId: ID!) {
    grantProjectAccess(workspaceId: $workspaceId, projectId: $projectId, userId: $userId)
}
```
```graphql
# revokeProjectAccess.graphql
mutation RevokeProjectAccess($workspaceId: ID!, $projectId: ID!, $userId: ID!) {
    revokeProjectAccess(workspaceId: $workspaceId, projectId: $projectId, userId: $userId)
}
```

```bash
cd client && npx graphql-codegen > /tmp/cg.log 2>&1; echo $?; grep -n "useProjectGrantsQuery\|useSetProjectVisibilityMutation" src/shared/middleware/graphql.ts | head -4
```

Commit the documents and the generated file separately (convention):

```bash
git add src/graphql/automation/configuration/projectGrants.graphql src/graphql/automation/configuration/setProjectVisibility.graphql src/graphql/automation/configuration/grantProjectAccess.graphql src/graphql/automation/configuration/revokeProjectAccess.graphql
git commit -m "4750 client - Add project visibility and sharing GraphQL operations"
git add src/shared/middleware/graphql.ts src/shared/middleware/graphql-types.ts
git commit -m "4750 client - Regenerate GraphQL types for project sharing"
```

- [ ] **Step 2: Write the failing `ProjectListItem.visibility.test.tsx`**

```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

const {mockUseVisibilityFeatureEnabled} = vi.hoisted(() => ({
    mockUseVisibilityFeatureEnabled: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: mockUseVisibilityFeatureEnabled,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useGrantProjectAccessMutation: () => ({mutate: vi.fn()}),
    useProjectGrantsQuery: () => ({data: {projectGrants: []}}),
    useRevokeProjectAccessMutation: () => ({mutate: vi.fn()}),
    useSetProjectVisibilityMutation: () => ({mutate: vi.fn()}),
    useWorkspaceUsersQuery: () => ({data: {workspaceUsers: []}}),
}));

// …plus whatever the existing ProjectList tests already mock for router / stores / react-query
// (copy the provider wrapper from Projects.test.tsx).

const project = {
    id: 1,
    lastProjectVersion: 1,
    name: 'Alpha',
    projectWorkflowIds: [],
    visibility: 'PRIVATE',
    workspaceId: 1,
} as never;

describe('ProjectListItem visibility', () => {
    it('renders the visibility badge in EE', () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 1});

        render(<ProjectListItem project={project} remainingTags={[]} />, {wrapper: Wrapper});

        expect(screen.getByLabelText('Change visibility')).toBeInTheDocument();
    });

    it('renders no badge in CE', () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});

        render(<ProjectListItem project={project} remainingTags={[]} />, {wrapper: Wrapper});

        expect(screen.queryByLabelText('Change visibility')).not.toBeInTheDocument();
    });
});
```

(`Wrapper` = the QueryClient + MemoryRouter wrapper used by `Projects.test.tsx`; import `ProjectListItem` as a default import.)

- [ ] **Step 3: Run to verify it fails**

```bash
cd client && npx vitest run src/pages/automation/projects/components/project-list/ProjectListItem.visibility.test.tsx > /tmp/c9.log 2>&1; echo $?; tail -20 /tmp/c9.log
```

- [ ] **Step 4: `useProjectVisibility` — one hook behind both edit surfaces**

Create `client/src/shared/hooks/useProjectVisibility.ts`. Decision 8 was revised (spec §16) to add the project
header settings menu as a second edit surface; this hook is what keeps the two from drifting — the grant/revoke
diff and the three mutations exist once.

```ts
import {
    ResourceVisibility,
    useGrantProjectAccessMutation,
    useProjectGrantsQuery,
    useRevokeProjectAccessMutation,
    useSetProjectVisibilityMutation,
    useWorkspaceUsersQuery,
} from '@/shared/middleware/graphql';
import {useVisibilityFeatureEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';

interface UseProjectVisibilityPropsI {
    projectId?: number;
    visibility?: string;
}

export const useProjectVisibility = ({projectId, visibility}: UseProjectVisibilityPropsI) => {
    const {enabled, workspaceId} = useVisibilityFeatureEnabled();

    const queryClient = useQueryClient();

    const invalidateProjects = () => {
        queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

        if (projectId) {
            queryClient.invalidateQueries({queryKey: ProjectKeys.project(projectId)});
        }
    };

    const grantProjectAccessMutation = useGrantProjectAccessMutation({onSuccess: invalidateProjects});
    const revokeProjectAccessMutation = useRevokeProjectAccessMutation({onSuccess: invalidateProjects});
    const setProjectVisibilityMutation = useSetProjectVisibilityMutation({onSuccess: invalidateProjects});

    // Only a withheld project can have a meaningful audience, so the two lookups the picker needs are skipped
    // for the workspace-visible majority.
    const isWithheld = visibility === 'PRIVATE';
    const lookupsEnabled = enabled && isWithheld && !!projectId && !!workspaceId;

    const projectGrantsQuery = useProjectGrantsQuery(
        {projectId: String(projectId), workspaceId: String(workspaceId)},
        {enabled: lookupsEnabled}
    );

    const workspaceUsersQuery = useWorkspaceUsersQuery(
        {workspaceId: String(workspaceId)},
        {enabled: lookupsEnabled}
    );

    const grantedUserIds = (projectGrantsQuery.data?.projectGrants ?? []).map(Number);

    const workspaceMembers = (workspaceUsersQuery.data?.workspaceUsers ?? []).map((workspaceUser) => ({
        label: workspaceUser.user?.email ?? `User ${workspaceUser.userId}`,
        userId: Number(workspaceUser.userId),
    }));

    // Diff rather than replace: the server has no set-grants operation.
    const handleGrantedUserIdsChange = (nextUserIds: number[]) => {
        const identifiers = {projectId: String(projectId), workspaceId: String(workspaceId)};

        grantedUserIds
            .filter((userId) => !nextUserIds.includes(userId))
            .forEach((userId) => revokeProjectAccessMutation.mutate({...identifiers, userId: String(userId)}));

        nextUserIds
            .filter((userId) => !grantedUserIds.includes(userId))
            .forEach((userId) => grantProjectAccessMutation.mutate({...identifiers, userId: String(userId)}));
    };

    const handleVisibilityChange = (nextVisibility: string) =>
        setProjectVisibilityMutation.mutate({
            projectId: String(projectId),
            // The picker speaks plain strings; the generated enum's values are exactly those strings.
            visibility: nextVisibility as ResourceVisibility,
            workspaceId: String(workspaceId),
        });

    return {
        enabled: enabled && !!projectId && !!workspaceId,
        grantedUserIds,
        onGrantedUserIdsChange: handleGrantedUserIdsChange,
        onVisibilityChange: handleVisibilityChange,
        workspaceMembers,
    };
};
```

- [ ] **Step 5: `ProjectListItem` — badge dropdown over the shared hook**

Imports to add:

```tsx
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import ResourceVisibilityPicker from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useProjectVisibility} from '@/shared/hooks/useProjectVisibility';
```

(Merge into the existing sorted import block; `DropdownMenu`, `DropdownMenuContent`, `DropdownMenuTrigger` are
already imported. The GraphQL hooks are no longer imported here — Step 4's hook owns them.) One hook call, in
the custom-hooks section after `currentWorkspaceId`:

```tsx
    const projectVisibility = useProjectVisibility({projectId: project.id, visibility: project.visibility});
```

(Respect the hook-ordering rule: `useState` → `useRef` → store hooks → other custom hooks → derived values.)
Render helper (a plain function above `return`):

```tsx
    const renderVisibilityPicker = () => {
        if (!projectVisibility.enabled) {
            return null;
        }

        return (
            <div className="min-w-64 p-3">
                <ResourceVisibilityPicker
                    grantedUserIds={projectVisibility.grantedUserIds}
                    onGrantedUserIdsChange={projectVisibility.onGrantedUserIdsChange}
                    onVisibilityChange={projectVisibility.onVisibilityChange}
                    visibility={project.visibility || 'WORKSPACE'}
                    workspaceMembers={projectVisibility.workspaceMembers}
                />
            </div>
        );
    };
```

Badge — directly after the name-rendering ternary (before the `project.codeWorkflow` badge):

```tsx
                            {projectVisibility.enabled && (
                                <div onClick={(event) => event.stopPropagation()}>
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <button
                                                aria-label="Change visibility"
                                                className="cursor-pointer rounded-sm hover:bg-surface-neutral-primary-hover"
                                                type="button"
                                            >
                                                <ResourceVisibilityBadge
                                                    grantedUserCount={projectVisibility.grantedUserIds.length}
                                                    visibility={project.visibility || 'WORKSPACE'}
                                                />
                                            </button>
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="start" className="p-0">
                                            {renderVisibilityPicker()}
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </div>
                            )}
```

The `stopPropagation` wrapper keeps the click from toggling the workflows collapsible (the list item's row `onClick`).

- [ ] **Step 6: `ProjectDialog` — create-only picker**

Imports: `ResourceVisibilityPicker`, `useIsVisibilityEditionEnabled`. Hook (after `captureProjectCreated`): `const visibilityFeatureEnabled = useIsVisibilityEditionEnabled();`. `defaultValues` gains `visibility: project?.visibility || 'WORKSPACE',` (sort-keys: between `tags` and `workspaceId`). Field — after the tags `FormField`, before `DialogFooter`:

```tsx
                        {!project?.id && visibilityFeatureEnabled && (
                            <FormField
                                control={control}
                                name="visibility"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Visibility</FormLabel>

                                        <FormControl>
                                            {/* Grants need a project id, so creation offers reach only; the list
                                                item's picker adds people afterwards. */}

                                            <ResourceVisibilityPicker
                                                grantedUserIds={[]}
                                                onGrantedUserIdsChange={() => undefined}
                                                onVisibilityChange={field.onChange}
                                                visibility={field.value || 'WORKSPACE'}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}
```

`createProjectMutation.mutate({...formData, …})` already spreads `visibility` through; the generated `Project.visibility` accepts `'PRIVATE' | 'WORKSPACE'`. In edit mode nothing renders (the list item is the edit surface — spec §12). The update mutation also spreads `formData` (so it carries the unchanged `project.visibility` default) — leave it: `ProjectServiceImpl.update(Project)` copies only category/description/name/tags/version and never touches visibility, and the REST update path goes through it.

`ProjectDialog.test.tsx` (new, minimal): with `useIsVisibilityEditionEnabled` mocked `true` and no `project` prop, `screen.getByText('Visibility')` is present; with a `project` prop (edit) it is absent; with the hook `false` it is absent.

- [ ] **Step 7: Project header settings menu — a `Visibility` item and its dialog**

Spec §16 decision 8 (revised): the project header's Settings menu is the third surface. **Label it
`Visibility`, not `Share`.** That menu already carries two outward-publishing items — `Share`
(`ProjectShareDialog`, which mints a public template URL via `exportSharedProject`) and `Share with
Community`. A third item named "Share" that instead *restricts* reach would read as a fourth publishing
option. `Visibility` + a lock icon states the direction.

Create `client/src/pages/automation/project/components/ProjectVisibilityDialog.tsx`:

```tsx
import {Dialog, DialogCloseButton, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import ResourceVisibilityPicker from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useProjectVisibility} from '@/shared/hooks/useProjectVisibility';

interface ProjectVisibilityDialogPropsI {
    onClose: () => void;
    projectId: number;
    visibility?: string;
}

const ProjectVisibilityDialog = ({onClose, projectId, visibility}: ProjectVisibilityDialogPropsI) => {
    const projectVisibility = useProjectVisibility({projectId, visibility});

    return (
        <Dialog onOpenChange={onClose} open>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Project Visibility</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                {projectVisibility.enabled && (
                    <ResourceVisibilityPicker
                        grantedUserIds={projectVisibility.grantedUserIds}
                        onGrantedUserIdsChange={projectVisibility.onGrantedUserIdsChange}
                        onVisibilityChange={projectVisibility.onVisibilityChange}
                        visibility={visibility || 'WORKSPACE'}
                        workspaceMembers={projectVisibility.workspaceMembers}
                    />
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ProjectVisibilityDialog;
```

Match the surrounding dialogs' actual import surface — copy the `Dialog*` imports from `ErrorWorkflowDialog.tsx`
in the same directory rather than assuming the names above, and follow whichever of default/named export that
file uses.

In `ProjectTabButtons.tsx`: add an `onShowVisibilityDialog: () => void` prop (alphabetical, between
`onShowProjectVersionHistorySheet` and `projectGitConfigurationEnabled` in both the destructure and the type),
import `LockIcon` from `lucide-react` (sorted), and render the item immediately **after** the existing `Share`
button and before `Share with Community`, gated on the edition hook:

```tsx
            {visibilityFeatureEnabled && (
                <Button
                    aria-label="Project Visibility Button"
                    className="dropdown-menu-item"
                    icon={<LockIcon />}
                    label="Visibility"
                    onClick={onShowVisibilityDialog}
                    variant="ghost"
                />
            )}
```

with `const visibilityFeatureEnabled = useIsVisibilityEditionEnabled();` beside the existing
`useFeatureFlagsStore()` calls (import from `@/shared/hooks/useVisibilityFeatureEnabled`). The existing
`ProjectTabButtons.test.tsx` passes every handler as a `vi.fn()` — add `onShowVisibilityDialog: vi.fn()` to
that object or the type stops compiling.

In `SettingsMenu.tsx`: add `const [showProjectVisibilityDialog, setShowProjectVisibilityDialog] = useState(false);`
(alphabetical among the sibling `useState`s — after `showProjectVersionHistorySheet`), pass
`onShowVisibilityDialog={() => setShowProjectVisibilityDialog(true)}` to `ProjectTabButtons` (alphabetical among
its props), import `ProjectVisibilityDialog`, and render beside the other conditional dialogs:

```tsx
            {showProjectVisibilityDialog && (
                <ProjectVisibilityDialog
                    onClose={() => setShowProjectVisibilityDialog(false)}
                    projectId={project.id!}
                    visibility={project.visibility}
                />
            )}
```

Test — `client/src/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons.test.tsx`,
extending the existing file rather than adding a new one: with `useIsVisibilityEditionEnabled` mocked `true`,
`screen.getByLabelText('Project Visibility Button')` is present and clicking it calls the handler; mocked
`false`, it is absent. Assert in the same test that `getByLabelText('Share ProjectButton')` still renders —
the two items are distinct affordances and a future edit must not collapse them. (Note that existing label's
missing space; do not "fix" it here, it is asserted elsewhere.)

- [ ] **Step 8: Run the client checks**

```bash
cd client && npm run format && npm run check > /tmp/c9.log 2>&1; echo $?; tail -30 /tmp/c9.log
```
Expected: exit 0.

- [ ] **Step 9: Commit**

```bash
git add src/pages/automation/projects/components/ProjectDialog.tsx src/pages/automation/projects/components/ProjectDialog.test.tsx src/pages/automation/projects/components/project-list/ProjectListItem.tsx src/pages/automation/projects/components/project-list/ProjectListItem.visibility.test.tsx src/shared/hooks/useProjectVisibility.ts src/pages/automation/project/components/ProjectVisibilityDialog.tsx src/pages/automation/project/components/project-header/components/settings-menu
git commit -m "4750 client - Add the project visibility picker to the create dialog and the list item badge dropdown"
```

---

### Task 10: Docs, full verification

**Files:**
- Modify: `CLAUDE.md` ("Resource Visibility & Sharing" section)
- Modify: `docs/superpowers/specs/2026-08-10-resource-visibility-design.md` (§11)
- (openapi connection description was fixed in Task 6)

- [ ] **Step 1: `CLAUDE.md`**

In "Resource Visibility & Sharing", replace *"The model is resource-agnostic; connections are the only resource wired to it so far."* with:

> The model is resource-agnostic. Wired so far: **connections** (`PRIVATE`/`WORKSPACE`/`ORGANIZATION`) and
> **projects** (`PRIVATE`/`WORKSPACE`; `ProjectVisibilityPolicy`, column `project.visibility`). Workflows, project
> workflows, project deployments and jobs have **no column** — they inherit the project's reach at check time via
> `ResourceVisibilityProvider`s whose `visibilityResourceType()` returns `"Project"` and whose record id is the
> project id, so grants resolve against `("Project", projectId)`. `ProjectVisibilityFilter` (`-api`) is the single
> `Project → VisibilityRecord` mapping every list surface uses (projects, workflows, deployments, executions,
> search, GraphQL). `hasWorkspaceScopeForProject` delegates to `hasResourceScope(projectId, "Project", scope)`.
> `ProjectOwnershipResolver` returns `ownerUserId` (from `created_by`) ONLY because `ProjectVisibilityProvider` is
> registered — CE `hasResourceScope` takes the visibility branch; the two must never be split across commits.
> EE sharing lives on the separate `ProjectSharingFacade` (owner-or-admin), GraphQL `project-sharing.graphqls`.

- [ ] **Step 2: Parent spec §11**

Change the first bullet to: *"**Resources:** `Project` — implemented by `2026-08-17-project-visibility-design.md` (with `Workflow`, `ProjectWorkflow`, `ProjectDeployment`, `Job` inheriting via the `visibilityResourceType()` provider hook). `DataTable`, `KnowledgeBase`, `File`, `Skill` — pending. Each adds a `visibility` column, a `ResourceVisibilityPolicy` entry, and a `VisibilityRecord` mapping in its list facade."*

- [ ] **Step 2a: Pin the `ResourceVisibility` ordinals**

Added 2026-08-19, from the Task 3 review. TWO liquibase migrations now hard-code `ResourceVisibility`
ordinal `1` as a column default — `connection.visibility` (phase 1) and `project.visibility` (Task 3) — and
the enum has no ordinal-pinning test, even though that is a well-worn idiom in this repo
(`BlockingModeStabilityTest`, `EnumOrdinalPinTest`, `ConnectionStatusTest`, `OtelSpanStatusTest`,
`ComponentOperationPolicyOperationTypeTest`). Reordering the enum would silently reinterpret every existing
project AND connection row — a data-corruption failure with no error and no test to catch it.

Add a test beside `ResourceVisibility` (`platform-api`) asserting each constant's ordinal explicitly —
`PRIVATE` 0, `WORKSPACE` 1, `ORGANIZATION` 2 — with a comment naming both migrations that depend on it, and
follow whichever of the listed pinning tests is closest in shape. Three lines of assertion; it closes the
gap for phase 1 as well as phase 2.

- [ ] **Step 2b: Entry-point audit — enumerate the whole `PermissionService` surface**

Added 2026-08-19. THREE of the four authorization entry points this plan closes were discovered by review
rather than by the spec: the spec named `hasWorkspaceScopeForProject` (2-arg); the Task 1 review found
`hasWorkflowScope`; the Task 2 implementer found the 3-arg `hasWorkspaceScopeForProject(long, String,
Environment)`. That is a discovery process, not a design, and the next one should not be found by a
customer.

Enumerate EVERY public method on `PermissionService` (the interface in `automation-configuration-api`) and
classify each in a table, checked into the spec as a new section:

| Method | By-id? | Goes through `isResourceVisible`? | If not, why that is correct |

A method belongs in the "correct to skip" column only for a stated reason — it is not resource-keyed
(`isTenantAdmin`, `getMyWorkspaceScopes`), the caller is the resource's own owner (`isResourceOwner` — an
owner is always visible to themselves), or it is a deliberate management posture. Exactly one is known to
be in that last category today and MUST appear in the table with its reasoning:

- `hasResourceRole(long id, String resourceType, String minimumRole)` — no precondition, and live for
  `'Connection'` (a type that already has a registered provider) through `WorkspaceConnectionFacadeImpl`'s
  owner-or-admin gates on `setConnectionVisibility` / `grantConnectionAccess` / `revokeConnectionAccess` /
  `getConnectionGrants`. This is the documented owner-or-admin sharing-management posture, not an
  oversight: a workspace admin must be able to repair the sharing of a resource they cannot themselves
  see. Record it as a decision, with a one-line comment on the method pointing at the table.

Anything the table cannot justify is a finding — report it rather than fixing it silently, since a fix at
this point lands outside every task's review.

- [ ] **Step 3: Full server + client verification**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo $?
./gradlew check --continue > /tmp/check.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/check.log
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration > /tmp/int.log 2>&1; echo $?; grep -E "^> Task .* FAILED" /tmp/int.log
cd client && npm run check > /tmp/cc.log 2>&1; echo $?; tail -5 /tmp/cc.log
```
Expected: all exit 0. If `check` surfaces Checkstyle/PMD/SpotBugs findings in new files, fix them (typical: missing `@SuppressFBWarnings("EI")` on stored mocks/collaborators, unused imports).

- [ ] **Step 4: Manual smoke (optional but recommended)**

Run the server (`./gradlew -p server/apps/server-app bootRun`, EE edition) + client; as `admin@localhost.com` create a project, set it Private from the list badge, log in as `user@localhost.com` in another browser — the project, its workflows, deployments and executions are absent; grant `user` from the picker — they appear.

- [ ] **Step 5: Commit docs**

```bash
git add CLAUDE.md docs/superpowers/specs/2026-08-10-resource-visibility-design.md
git commit -m "4750 Document project visibility and the provider inheritance hook"
```

---

## Self-review against the spec

- §3 model/policy → Task 3. §3.4 editions (CE force, EE validate) → Task 6.
- §4.1 inheriting types (Workflow, ProjectWorkflow, ProjectDeployment, Job) → Task 4; §4.2 SPI hook → Task 1; §4.3 `hasWorkspaceScopeForProject` → Task 2.
- §5 schema → Task 3. §6 placement — `ProjectVisibilityFilter` in `-api` (deviation from the spec's `-service` listing, forced by `-graphql`'s dependency graph; noted in the file's Javadoc). `ProjectVisibilityPolicy`, providers, error type in `-service`; `JobVisibilityProvider` in execution-service; EE facade/graphql/listener as listed.
- §7 read path: projects list, workflow lists, deployments, executions explicit filter, GraphQL `project`/`projects`, three search providers → Task 5. Project tags deliberately untouched (spec ⚑10).
- §8 by-id: providers + `ProjectOwnershipResolver` in one commit → Task 4.
- §9 write path: create → Task 6; sharing facade, validations, no-deploy-rule, delete cleanup → Task 7.
- §10 audit events → Task 7 (`ProjectAuditEvent` + publisher calls); no metric (spec ⚑9).
- §11 REST + GraphQL → Tasks 6, 7, 9.
- §12 client: rename/move → Task 8; dialog + list item → Task 9; no header entry (spec ⚑8).
- §13 tests: policy (T3), providers (T4), `isResourceVisible` inheritance both editions (T1/T2), list/by-id agreement (T5), sharing authorization + behaviour (T7), create CE/EE (T6), delete purge (T7 listener — covered by review; add a `ProjectBeforeDeleteEventListenerTest` verifying `deleteGrants("Project", id)` if the module has a precedent test for that listener), client (T8/T9).
- §14 docs → Task 10 (+ openapi text in T6).
- Type consistency: `ProjectVisibilityFilter.PROJECT` / `"Project"` everywhere; `updateVisibility(long, ResourceVisibility)`; `ProjectSharingFacade` method names match the GraphQL controller and the authorization test list; `useVisibilityFeatureEnabled` returns `{enabled, isAdmin, workspaceId}` as today.
