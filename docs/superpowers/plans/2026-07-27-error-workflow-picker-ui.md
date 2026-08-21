# Error-Workflow Picker UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the shipped error-workflow feature (project-level default handler, dispatched by
`ErrorWorkflowJobStatusApplicationEventListener` on `FAILED` runs) the config surface it shipped without: a
per-workflow override API, an eligible-handlers query, and pickers on the project-settings and workflow-settings
menus.

**Architecture:** `project_workflow.error_project_workflow_id` / `error_workflow_disabled` already exist on the
domain and are already read by `ErrorWorkflowResolver` — only the write path and a read path for "which workflows
in this project are eligible handlers" are missing. The write path is a new single-field service method
(`ProjectWorkflowService.updateErrorWorkflow`, the exact shape of the existing `updatePermissionExpression`
precedent) behind a new facade method that reuses `ErrorWorkflowConfigurationValidator.validate` — never the
whitelist `ProjectWorkflowService.update(ProjectWorkflow)` path, which would silently accept fields it wasn't
asked to change. The read path (`eligibleErrorWorkflows`) mirrors an exact existing precedent,
`McpProjectWorkflowGraphQlController.toolEligibleProjectVersionWorkflows`, which resolves and filters project
workflows by trigger type directly in the GraphQL controller (bypassing the facade layer entirely for the
read side, exactly as that precedent does) — `automation-configuration-graphql` only depends on
`automation-configuration-api`, so it cannot reach `ErrorWorkflowConfigurationValidator` (a `-service` class);
the small trigger-match check is duplicated in the controller, the same intentional duplication already
established between `ErrorWorkflowConfigurationValidator` and `platform-coordinator`'s `ErrorWorkflowResolver`.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Spring GraphQL, JUnit 5, Mockito, React 19.2,
TypeScript 5.9, react-hook-form, zod, TanStack Query, GraphQL Code Generator.

## Global Constraints

- Specs: `docs/superpowers/specs/2026-07-27-error-workflow-picker-ui-design.md` (read before Task 1) and
  `docs/superpowers/specs/2026-07-26-workflow-error-handler-design.md` (the feature this plan completes). Plan
  this completes: `docs/superpowers/plans/2026-07-26-workflow-error-handler.md` — its final "Not in this plan"
  section lists exactly the two gaps this plan fills: the per-workflow override API and the client UI.
- Automation only; embedded is out of scope, matching the base feature.
- The write path validates through `ErrorWorkflowConfigurationValidator.validate(projectId,
  targetProjectWorkflowId, configuredOnProjectWorkflowId)` — same project, target carries a
  `workflow/newWorkflowError` trigger, target is not the workflow being configured. Clearing (setting
  `errorProjectWorkflowId` to `null`) skips validation — there is no reference left to be invalid, matching
  `ProjectFacadeImpl.updateProjectErrorWorkflow`'s existing behavior exactly.
- Persistence for the per-workflow override is a DEDICATED single-field service method
  (`ProjectWorkflowService.updateErrorWorkflow`), never `ProjectWorkflowService.update(ProjectWorkflow)` — that
  method whitelists `projectVersion`/`workflowId`/`uuid` only and silently drops any other field set on the
  passed-in object, so routing this write through it would look correct in a smoke test and then silently no-op
  in production.
- **Resolved ambiguity (deviation from the spec's literal signature):** the spec text describes
  `updateProjectWorkflowErrorWorkflow(projectWorkflowId, errorProjectWorkflowId, disabled)` — three arguments.
  The codebase's `@PreAuthorize` authorization for this kind of write is always keyed by `projectId`
  (`ProjectFacadeImpl.updateProjectErrorWorkflow` uses `hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')`),
  and there is no registered `PermissionEvaluator` resource type for a bare numeric `project_workflow` id (only
  `Project` and `Workflow`, the latter keyed by the *string* workflow id via
  `WorkflowOwnershipResolver#resolveOwner(Serializable)` — see Task 2). Deriving `projectId` inside a `@PreAuthorize`
  SpEL expression via a bean-method call (`@projectWorkflowService.getProjectWorkflow(#id).getProjectId()`) would
  work but has no precedent anywhere in this codebase, so this plan takes the same fully-precedented shape as the
  project-level sibling instead: the mutation and facade method both take `projectId` explicitly, alongside
  `projectWorkflowId`. The client already has `project.id` in scope everywhere this dialog renders (see Task 7).
- **Resolved gap found during grounding, fixed in Task 5:** `ProjectFacadeImpl.updateProjectErrorWorkflow` (already
  shipped) throws a bare `IllegalArgumentException` on a rejected reference. `GlobalDataFetcherExceptionResolver`
  only forwards a caught throwable's message for `AbstractException` subtypes and `GraphQlBadRequestException` — a
  bare `IllegalArgumentException` at the top level falls through to the default `INTERNAL_ERROR` handling with no
  message forwarded, meaning CLAUDE.md's "validation failures surface via the global toast" claim does not
  actually hold today for this mutation. Task 5 fixes this for both the existing and the new mutation.
- Client conventions from CLAUDE.md: ESLint `sort-keys` (alphabetical object keys), interface names ending
  `I`/`Props`, `twMerge` (never `cn()`), import-destructure alphabetical sort, hook ordering (`useState` →
  `useRef` → custom store hooks → other hooks → derived values → `useEffect` → `return`), `||` over `??` for JSX
  fallbacks. Run `npm run check` (from `client/`) before committing any client change.
- GraphQL codegen flow: add operation `.graphql` files under `client/src/graphql/automation/configuration/`, then
  `cd client && npx graphql-codegen` to regenerate `src/shared/middleware/graphql.ts` — `codegen.ts`'s `schema`
  array already globs `automation-configuration-graphql/src/main/resources/graphql/*.graphqls`, so no codegen
  config change is needed, only new `.graphqls`/`.graphql` files.
- Run `./gradlew spotlessApply` before every server-side commit. Check Gradle results by redirecting to a file and
  testing `$?` — a run piped into `tail` reports the filter's exit code, not Gradle's:
  `./gradlew <task> > /tmp/out.log 2>&1; echo $?`.
- Mockito gotcha: unstubbed wrapper-returning methods (`Long`, `Integer`) return `0`, not `null`. This plan's
  tests branch on a nullable `errorProjectWorkflowId` — stub `thenReturn(null)` explicitly wherever a null must
  be observed.
- Checkstyle: test method names are camelCase, no underscores, applies to every method in test sources including
  private helpers. `TodoComment` and `EmptyBlock` rules apply as usual.

---

## File Structure

**Created:**
- `.../automation-configuration-service/.../service/ProjectWorkflowServiceUpdateErrorWorkflowTest.java`
- `.../automation-configuration-service/.../facade/ProjectWorkflowFacadeUpdateErrorWorkflowTest.java`
- `.../automation-configuration-graphql/.../ProjectWorkflowGraphQlControllerErrorWorkflowTest.java`
- `client/src/graphql/automation/configuration/errorWorkflow.graphql`
- `client/src/pages/automation/project/components/ErrorWorkflowDialog.tsx`
- `client/src/pages/automation/project/components/ErrorWorkflowDialog.test.tsx`
- `client/src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog.tsx`
- `client/src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog.test.tsx`

**Modified:**
- `.../automation-configuration-api/.../service/ProjectWorkflowService.java`
- `.../automation-configuration-service/.../service/ProjectWorkflowServiceImpl.java`
- `.../automation-configuration-api/.../facade/ProjectWorkflowFacade.java`
- `.../automation-configuration-service/.../facade/ProjectWorkflowFacadeImpl.java` (constructor gains
  `ErrorWorkflowConfigurationValidator`)
- `.../automation-configuration-service/.../facade/ErrorWorkflowConfigurationValidator.java` (private trigger
  check stays private — no cross-module reuse needed, see Task 4)
- `.../automation-configuration-graphql/.../graphql/project.graphqls`
- `.../automation-configuration-graphql/.../graphql/project-workflow.graphqls`
- `.../automation-configuration-graphql/.../web/graphql/ProjectGraphQlController.java`
- `.../automation-configuration-graphql/.../web/graphql/ProjectWorkflowGraphQlController.java`
- `server/libs/core/graphql/graphql-impl/src/main/java/com/bytechef/graphql/error/GlobalDataFetcherExceptionResolver.java` (no code change — Task 5 changes the two controllers, not the resolver)
- `client/src/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons.tsx`
- `client/src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowTabButtons.tsx`
- `client/src/pages/automation/project/components/project-header/components/settings-menu/hooks/useSettingsMenu.ts`
- `client/src/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu.tsx`
- `docs/content/docs/automation/error-workflows.mdx`

---

### Task 1: `ProjectWorkflowService.updateErrorWorkflow` — persistence primitive

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectWorkflowService.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectWorkflowServiceImpl.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/ProjectWorkflowServiceUpdateErrorWorkflowTest.java`

**Interfaces:**
- Consumes: `ProjectWorkflow.setErrorProjectWorkflowId(Long)` / `.setErrorWorkflowDisabled(boolean)` (already
  shipped by the base feature).
- Produces: `ProjectWorkflowService.updateErrorWorkflow(long id, @Nullable Long errorProjectWorkflowId, boolean
  errorWorkflowDisabled): ProjectWorkflow`. Task 2's facade method calls this and nothing else on the service for
  persistence.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.audit.ProjectWorkflowAuditPublisher;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectWorkflowServiceUpdateErrorWorkflowTest {

    @Mock
    private ProjectWorkflowAuditPublisher projectWorkflowAuditPublisher;

    @Mock
    private ProjectWorkflowRepository projectWorkflowRepository;

    @InjectMocks
    private ProjectWorkflowServiceImpl projectWorkflowService;

    @Test
    void testUpdateErrorWorkflowSetsBothFieldsAndSaves() {
        ProjectWorkflow existing = new ProjectWorkflow();

        existing.setId(10L);

        when(projectWorkflowRepository.findById(10L))
            .thenReturn(Optional.of(existing));
        when(projectWorkflowRepository.save(existing))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectWorkflow result = projectWorkflowService.updateErrorWorkflow(10L, 99L, true);

        Assertions.assertEquals(99L, result.getErrorProjectWorkflowId());
        Assertions.assertTrue(result.isErrorWorkflowDisabled());
    }

    @Test
    void testUpdateErrorWorkflowAcceptsNullReference() {
        ProjectWorkflow existing = new ProjectWorkflow();

        existing.setId(10L);
        existing.setErrorProjectWorkflowId(5L);

        when(projectWorkflowRepository.findById(10L))
            .thenReturn(Optional.of(existing));
        when(projectWorkflowRepository.save(existing))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectWorkflow result = projectWorkflowService.updateErrorWorkflow(10L, null, false);

        Assertions.assertNull(result.getErrorProjectWorkflowId());
        Assertions.assertFalse(result.isErrorWorkflowDisabled());
    }

    @Test
    void testUpdateErrorWorkflowThrowsWhenNotFound() {
        when(projectWorkflowRepository.findById(10L))
            .thenReturn(Optional.empty());

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> projectWorkflowService.updateErrorWorkflow(10L, 99L, false));

        Mockito.verify(projectWorkflowRepository, Mockito.never())
            .save(ArgumentCaptor.forClass(ProjectWorkflow.class)
                .capture());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectWorkflowServiceUpdateErrorWorkflowTest*" > /tmp/t1.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: method updateErrorWorkflow`.

- [ ] **Step 3: Add the interface method**

In `ProjectWorkflowService.java`, beside `updatePermissionExpression`:

```java
    ProjectWorkflow updateErrorWorkflow(long id, @Nullable Long errorProjectWorkflowId, boolean errorWorkflowDisabled);
```

- [ ] **Step 4: Implement it, mirroring `updatePermissionExpression` exactly**

In `ProjectWorkflowServiceImpl.java`:

```java
    @Override
    public ProjectWorkflow updateErrorWorkflow(
        long id, @Nullable Long errorProjectWorkflowId, boolean errorWorkflowDisabled) {

        ProjectWorkflow projectWorkflow = projectWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ProjectWorkflow not found"));

        projectWorkflow.setErrorProjectWorkflowId(errorProjectWorkflowId);
        projectWorkflow.setErrorWorkflowDisabled(errorWorkflowDisabled);

        return projectWorkflowRepository.save(projectWorkflow);
    }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectWorkflowServiceUpdateErrorWorkflowTest*" > /tmp/t1.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Add ProjectWorkflowService.updateErrorWorkflow

Dedicated single-field persistence, the exact shape of the existing
updatePermissionExpression precedent -- never the whitelist update(ProjectWorkflow)
path, which silently drops any field it wasn't written to carry."
```

---

### Task 2: `ProjectWorkflowFacade.updateWorkflowErrorWorkflow` — validation + authorization

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectWorkflowFacade.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectWorkflowFacadeImpl.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectWorkflowFacadeUpdateErrorWorkflowTest.java`

**Interfaces:**
- Consumes: `ProjectWorkflowService.updateErrorWorkflow` (Task 1),
  `ErrorWorkflowConfigurationValidator.validate(long, long, Long)` (already shipped).
- Produces: `ProjectWorkflowFacade.updateWorkflowErrorWorkflow(long projectId, long projectWorkflowId, @Nullable
  Long errorProjectWorkflowId, boolean errorWorkflowDisabled)`. Task 3's GraphQL mutation calls this and nothing
  else.

`ProjectWorkflowFacadeImpl` does not currently depend on `ErrorWorkflowConfigurationValidator` — add it to the
constructor, the same way `ProjectFacadeImpl` already does.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.automation.configuration.facade;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectWorkflowFacadeUpdateErrorWorkflowTest {

    @Mock
    private ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @InjectMocks
    private ProjectWorkflowFacadeImpl projectWorkflowFacade;

    @Test
    void testValidatesBeforeSaving() {
        projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, 99L, false);

        verify(errorWorkflowConfigurationValidator).validate(1L, 99L, 10L);
        verify(projectWorkflowService).updateErrorWorkflow(10L, 99L, false);
    }

    @Test
    void testClearingSkipsValidation() {
        projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, null, false);

        verifyNoInteractions(errorWorkflowConfigurationValidator);
        verify(projectWorkflowService).updateErrorWorkflow(10L, null, false);
    }

    @Test
    void testDisablingWithNoReferenceSkipsValidation() {
        projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, null, true);

        verifyNoInteractions(errorWorkflowConfigurationValidator);
        verify(projectWorkflowService).updateErrorWorkflow(10L, null, true);
    }

    @Test
    void testRejectedReferenceIsNotSaved() {
        doThrow(new IllegalArgumentException("nope"))
            .when(errorWorkflowConfigurationValidator)
            .validate(1L, 99L, 10L);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, 99L, false));

        verify(projectWorkflowService, never())
            .updateErrorWorkflow(anyLong(), ArgumentMatchers.any(), Mockito.anyBoolean());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectWorkflowFacadeUpdateErrorWorkflowTest*" > /tmp/t2.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: method updateWorkflowErrorWorkflow`.

- [ ] **Step 3: Add the interface method**

In `ProjectWorkflowFacade.java`:

```java
    void updateWorkflowErrorWorkflow(
        long projectId, long projectWorkflowId, @Nullable Long errorProjectWorkflowId, boolean errorWorkflowDisabled);
```

- [ ] **Step 4: Inject the validator and implement**

In `ProjectWorkflowFacadeImpl.java`, add the field and constructor parameter beside the existing dependencies:

```java
    private final ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;
```

(add to the constructor parameter list and assignment, alongside the other `this.x = x;` lines)

```java
    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')")
    public void updateWorkflowErrorWorkflow(
        long projectId, long projectWorkflowId, @Nullable Long errorProjectWorkflowId,
        boolean errorWorkflowDisabled) {

        // Clearing needs no validation: there is no reference left to be invalid -- mirrors
        // ProjectFacadeImpl.updateProjectErrorWorkflow exactly.
        if (errorProjectWorkflowId != null) {
            errorWorkflowConfigurationValidator.validate(projectId, errorProjectWorkflowId, projectWorkflowId);
        }

        projectWorkflowService.updateErrorWorkflow(projectWorkflowId, errorProjectWorkflowId, errorWorkflowDisabled);
    }
```

`@PreAuthorize` is keyed by `#projectId`, not `#projectWorkflowId` -- there is no registered permission-evaluator
resource type for a bare `project_workflow` row (`WorkflowOwnershipResolver` is keyed by the *string* workflow id,
not this numeric id), so `projectId` is threaded in explicitly by the caller, the same shape
`ProjectFacadeImpl.updateProjectErrorWorkflow` already uses.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectWorkflowFacadeUpdateErrorWorkflowTest*" > /tmp/t2.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Add ProjectWorkflowFacade.updateWorkflowErrorWorkflow

Reuses ErrorWorkflowConfigurationValidator.validate for any non-null reference;
clearing or disabling with no reference skips validation, matching
ProjectFacadeImpl.updateProjectErrorWorkflow's shape exactly. Authorization is
keyed by projectId (explicitly passed in) rather than the target
project_workflow row, since no permission-evaluator resource type is registered
for a bare numeric project_workflow id."
```

---

### Task 3: GraphQL write path — mutation and field exposure

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project.graphqls`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-workflow.graphqls`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectWorkflowGraphQlController.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/web/graphql/ProjectWorkflowGraphQlControllerErrorWorkflowTest.java`

**Interfaces:**
- Consumes: `ProjectWorkflowFacade.updateWorkflowErrorWorkflow` (Task 2).
- Produces: GraphQL mutation `updateProjectWorkflowErrorWorkflow(projectId: ID!, projectWorkflowId: ID!,
  errorProjectWorkflowId: ID, errorWorkflowDisabled: Boolean!): Boolean`; `Project.errorProjectWorkflowId: ID` and
  `ProjectWorkflow.errorProjectWorkflowId: ID` / `ProjectWorkflow.errorWorkflowDisabled: Boolean!` fields, resolved
  with zero extra Java code since both GraphQL types are backed directly by the domain objects (`Project` /
  `ProjectWorkflow`, not DTOs) via GraphQL-Java's default `PropertyDataFetcher` — exactly like every other
  unmapped field already on these two types.

- [ ] **Step 1: Add the field exposure (no controller code)**

In `project.graphqls`, add to `type Project`:

```graphql
type Project {
    id: ID!
    name: String!
    category: Category
    tags: [Tag]
    errorProjectWorkflowId: ID
}
```

In `project-workflow.graphqls`, add to `type ProjectWorkflow`:

```graphql
type ProjectWorkflow {
    id: ID!
    projectId: ID!
    workflowId: String!
    projectVersion: Int!
    uuid: String
    sseStreamResponse: Boolean!
    workflow: Workflow!
    createdBy: String
    createdDate: String
    lastModifiedBy: String
    lastModifiedDate: String
    version: Int!
    errorProjectWorkflowId: ID
    errorWorkflowDisabled: Boolean!
}
```

- [ ] **Step 2: Write the failing controller test**

```java
package com.bytechef.automation.configuration.web.graphql;

import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectWorkflowGraphQlControllerErrorWorkflowTest {

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @InjectMocks
    private ProjectWorkflowGraphQlController projectWorkflowGraphQlController;

    @Test
    void testUpdateProjectWorkflowErrorWorkflowDelegatesToTheFacade() {
        Boolean result = projectWorkflowGraphQlController.updateProjectWorkflowErrorWorkflow(1L, 10L, 99L, false);

        org.junit.jupiter.api.Assertions.assertTrue(result);

        verify(projectWorkflowFacade).updateWorkflowErrorWorkflow(1L, 10L, 99L, false);
    }
}
```

Note: `ProjectWorkflowGraphQlController`'s existing constructor also takes `ProjectWorkflowService` and
`WorkflowFacade`/`WorkflowService`; `@InjectMocks` leaves unstubbed collaborators null, which is fine here since
this test only exercises the new method.

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests "*ProjectWorkflowGraphQlControllerErrorWorkflowTest*" > /tmp/t3.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: method updateProjectWorkflowErrorWorkflow`.

- [ ] **Step 4: Add the schema mutation and controller method**

In `project-workflow.graphqls`, add to `extend type Mutation`:

```graphql
    updateProjectWorkflowErrorWorkflow(projectId: ID!, projectWorkflowId: ID!, errorProjectWorkflowId: ID, errorWorkflowDisabled: Boolean!): Boolean
```

In `ProjectWorkflowGraphQlController.java`:

```java
    @MutationMapping
    public Boolean updateProjectWorkflowErrorWorkflow(
        @Argument long projectId, @Argument long projectWorkflowId,
        @Argument @Nullable Long errorProjectWorkflowId, @Argument boolean errorWorkflowDisabled) {

        projectWorkflowFacade.updateWorkflowErrorWorkflow(
            projectId, projectWorkflowId, errorProjectWorkflowId, errorWorkflowDisabled);

        return true;
    }
```

(add import `org.jspecify.annotations.Nullable`)

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests "*ProjectWorkflowGraphQlControllerErrorWorkflowTest*" > /tmp/t3.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Expose the per-workflow error-workflow override over GraphQL

errorProjectWorkflowId on Project and errorProjectWorkflowId/errorWorkflowDisabled
on ProjectWorkflow need no resolver code -- both GraphQL types are backed
directly by the domain objects, so the default PropertyDataFetcher resolves them
like every other unmapped field already on these types."
```

---

### Task 4: GraphQL read path — eligible handlers

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-workflow.graphqls`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectWorkflowGraphQlController.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/web/graphql/ProjectWorkflowGraphQlControllerErrorWorkflowTest.java` (extend)

**Interfaces:**
- Consumes: `ProjectWorkflowService.getProjectWorkflows(long, int)`, `WorkflowService.getWorkflow(String)` — both
  already injected into this controller.
- Produces: GraphQL query `eligibleErrorWorkflows(projectId: ID!, projectVersion: Int!): [ProjectWorkflow!]!`.

Mirrors `McpProjectWorkflowGraphQlController.toolEligibleProjectVersionWorkflows` exactly: filters
`projectWorkflowService.getProjectWorkflows(projectId, projectVersion)` by trigger type, resolved directly in the
controller rather than through the facade — the facade layer is bypassed for reads here the same way it is there.
The trigger check is a small private static method, deliberately duplicated from
`ErrorWorkflowConfigurationValidator#isErrorTrigger` rather than shared cross-module: `automation-configuration-graphql`
depends only on `automation-configuration-api`, so it cannot reach that `-service` class, the same reason
`platform-coordinator`'s `ErrorWorkflowResolver` already carries its own copy of this exact check.

- [ ] **Step 1: Write the failing test**

Add to `ProjectWorkflowGraphQlControllerErrorWorkflowTest`:

```java
    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    @Test
    void testEligibleErrorWorkflowsFiltersByTrigger() {
        ProjectWorkflow eligible = new ProjectWorkflow();

        eligible.setId(1L);
        eligible.setWorkflowId("wf-1");

        ProjectWorkflow ineligible = new ProjectWorkflow();

        ineligible.setId(2L);
        ineligible.setWorkflowId("wf-2");

        Mockito.when(projectWorkflowService.getProjectWorkflows(1L, 3))
            .thenReturn(List.of(eligible, ineligible));

        Workflow errorHandlerWorkflow = new Workflow();

        errorHandlerWorkflow.setDefinition(
            "{\"triggers\":[{\"name\":\"t1\",\"type\":\"workflow/v1/newWorkflowError\"}],\"tasks\":[]}");

        Workflow plainWorkflow = new Workflow();

        plainWorkflow.setDefinition("{\"triggers\":[],\"tasks\":[]}");

        Mockito.when(workflowService.getWorkflow("wf-1"))
            .thenReturn(errorHandlerWorkflow);
        Mockito.when(workflowService.getWorkflow("wf-2"))
            .thenReturn(plainWorkflow);

        List<ProjectWorkflow> result = projectWorkflowGraphQlController.eligibleErrorWorkflows(1L, 3);

        Assertions.assertEquals(List.of(eligible), result);
    }
```

(add imports `com.bytechef.automation.configuration.domain.ProjectWorkflow`, `com.bytechef.atlas.configuration.domain.Workflow`, `java.util.List`, `org.junit.jupiter.api.Assertions`)

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests "*ProjectWorkflowGraphQlControllerErrorWorkflowTest*" > /tmp/t4.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: method eligibleErrorWorkflows`.

- [ ] **Step 3: Add the schema query**

In `project-workflow.graphqls`, add to `extend type Query`:

```graphql
    eligibleErrorWorkflows(projectId: ID!, projectVersion: Int!): [ProjectWorkflow!]!
```

- [ ] **Step 4: Add the controller method**

```java
    @QueryMapping
    public List<ProjectWorkflow> eligibleErrorWorkflows(@Argument long projectId, @Argument int projectVersion) {
        return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
            .stream()
            .filter(projectWorkflow -> hasErrorTrigger(workflowService.getWorkflow(projectWorkflow.getWorkflowId())))
            .toList();
    }

    private static boolean hasErrorTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), "workflow") &&
                Objects.equals(workflowNodeType.operation(), "newWorkflowError")) {

                return true;
            }
        }

        return false;
    }
```

(add imports `com.bytechef.automation.configuration.domain.ProjectWorkflow`,
`com.bytechef.platform.configuration.domain.WorkflowTrigger`, `com.bytechef.platform.definition.WorkflowNodeType`,
`java.util.Objects`)

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests "*ProjectWorkflowGraphQlControllerErrorWorkflowTest*" > /tmp/t4.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Add eligibleErrorWorkflows GraphQL query

Mirrors McpProjectWorkflowGraphQlController.toolEligibleProjectVersionWorkflows
exactly: filters by trigger type directly in the controller, bypassing the
facade for this read. The trigger check duplicates
ErrorWorkflowConfigurationValidator's, the same intentional duplication already
established for ErrorWorkflowResolver, since automation-configuration-graphql
cannot reach -service classes."
```

---

### Task 5: Fix validation error surfacing (GraphQlBadRequestException)

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectGraphQlController.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectWorkflowGraphQlController.java`
- Test: extend the two controller tests from Tasks 3-4 (or a new focused test) plus a GraphQL error-shape
  integration test.

**Interfaces:**
- Consumes: `GraphQlBadRequestException` (`server/libs/core/graphql/graphql-api`, already exists, currently
  unused anywhere in the codebase — `grep` confirms zero call sites).
- Produces: both `updateProjectErrorWorkflow` and `updateProjectWorkflowErrorWorkflow` now surface the
  validator's actual message through the global GraphQL error resolver as `ErrorType.BAD_REQUEST`, instead of a
  generic `INTERNAL_ERROR` with no message — this is what makes the client's global toast (Task 7/8) show the real
  reason a picker selection was rejected.

`GlobalDataFetcherExceptionResolver` already special-cases `GraphQlBadRequestException` (`ErrorType.BAD_REQUEST`,
message forwarded verbatim) — no resolver change is needed, only wrapping at the two controller call sites that
let a validator's bare `IllegalArgumentException` propagate.

- [ ] **Step 1: Write the failing test**

Add to `ProjectWorkflowGraphQlControllerErrorWorkflowTest`:

```java
    @Test
    void testUpdateProjectWorkflowErrorWorkflowWrapsValidationFailureAsGraphQlBadRequest() {
        Mockito.doThrow(new IllegalArgumentException("A workflow cannot be its own error workflow"))
            .when(projectWorkflowFacade)
            .updateWorkflowErrorWorkflow(1L, 10L, 10L, false);

        GraphQlBadRequestException exception = Assertions.assertThrows(
            GraphQlBadRequestException.class,
            () -> projectWorkflowGraphQlController.updateProjectWorkflowErrorWorkflow(1L, 10L, 10L, false));

        Assertions.assertEquals("A workflow cannot be its own error workflow", exception.getMessage());
    }
```

Write the matching test on `ProjectGraphQlController` for `updateProjectErrorWorkflow` (a new small
`ProjectGraphQlControllerErrorWorkflowTest`, following the same shape, mocking `ProjectFacade` instead).

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests "*ErrorWorkflowTest*" > /tmp/t5.log 2>&1; echo $?`
Expected: non-zero — both mutations currently let `IllegalArgumentException` propagate unwrapped.

- [ ] **Step 3: Wrap at both call sites**

In `ProjectWorkflowGraphQlController.updateProjectWorkflowErrorWorkflow`:

```java
    @MutationMapping
    public Boolean updateProjectWorkflowErrorWorkflow(
        @Argument long projectId, @Argument long projectWorkflowId,
        @Argument @Nullable Long errorProjectWorkflowId, @Argument boolean errorWorkflowDisabled) {

        try {
            projectWorkflowFacade.updateWorkflowErrorWorkflow(
                projectId, projectWorkflowId, errorProjectWorkflowId, errorWorkflowDisabled);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new GraphQlBadRequestException(illegalArgumentException.getMessage(), illegalArgumentException);
        }

        return true;
    }
```

In `ProjectGraphQlController.updateProjectErrorWorkflow`:

```java
    @MutationMapping(name = "updateProjectErrorWorkflow")
    public Boolean updateProjectErrorWorkflow(
        @Argument long projectId, @Argument @Nullable Long errorProjectWorkflowId) {

        try {
            projectFacade.updateProjectErrorWorkflow(projectId, errorProjectWorkflowId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new GraphQlBadRequestException(illegalArgumentException.getMessage(), illegalArgumentException);
        }

        return true;
    }
```

(add import `com.bytechef.graphql.error.GraphQlBadRequestException` to both controllers; confirm
`automation-configuration-graphql`'s `build.gradle.kts` already depends on `graphql-api` — it does, transitively
through the shared GraphQL controller base already used across the module.)

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests "*ErrorWorkflowTest*" > /tmp/t5.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Wrap error-workflow validation failures as GraphQlBadRequestException

GlobalDataFetcherExceptionResolver only forwards a caught throwable's message
for AbstractException subtypes and GraphQlBadRequestException -- a bare
IllegalArgumentException at the top level (which is exactly what
ErrorWorkflowConfigurationValidator throws) fell through to the default
INTERNAL_ERROR handling with no message forwarded, so CLAUDE.md's 'validation
failures surface via the global toast' did not actually hold for either
mutation until now."
```

---

### Task 6: Client GraphQL operations and codegen

**Files:**
- Create: `client/src/graphql/automation/configuration/errorWorkflow.graphql`

**Interfaces:**
- Consumes: the schema additions from Tasks 3-5.
- Produces: generated hooks `useProjectErrorWorkflowQuery`, `useUpdateProjectErrorWorkflowMutation`,
  `useEligibleErrorWorkflowsQuery`, `useProjectWorkflowErrorConfigQuery`,
  `useUpdateProjectWorkflowErrorWorkflowMutation` in `client/src/shared/middleware/graphql.ts`. Tasks 7-8 import
  these directly.

- [ ] **Step 1: Write the operations file**

```graphql
query projectErrorWorkflow($id: ID!) {
    project(id: $id) {
        errorProjectWorkflowId
    }
}

mutation updateProjectErrorWorkflow($projectId: ID!, $errorProjectWorkflowId: ID) {
    updateProjectErrorWorkflow(projectId: $projectId, errorProjectWorkflowId: $errorProjectWorkflowId)
}

query eligibleErrorWorkflows($projectId: ID!, $projectVersion: Int!) {
    eligibleErrorWorkflows(projectId: $projectId, projectVersion: $projectVersion) {
        id
        workflowId
        workflow {
            label
        }
    }
}

query projectWorkflowErrorConfig($id: ID!) {
    projectWorkflow(id: $id) {
        errorProjectWorkflowId
        errorWorkflowDisabled
    }
}

mutation updateProjectWorkflowErrorWorkflow(
    $projectId: ID!
    $projectWorkflowId: ID!
    $errorProjectWorkflowId: ID
    $errorWorkflowDisabled: Boolean!
) {
    updateProjectWorkflowErrorWorkflow(
        projectId: $projectId
        projectWorkflowId: $projectWorkflowId
        errorProjectWorkflowId: $errorProjectWorkflowId
        errorWorkflowDisabled: $errorWorkflowDisabled
    )
}
```

`projectWorkflowErrorConfig` needs a small `projectWorkflow(id: ID!): ProjectWorkflow` query added alongside
`eligibleErrorWorkflows` in Task 4's schema/controller work (`ProjectWorkflowService.getProjectWorkflow(long)`
already exists and is already injected into `ProjectWorkflowGraphQlController`) — add it here as a one-line
follow-up to Task 4 rather than a full separate task:

```graphql
    projectWorkflow(id: ID!): ProjectWorkflow
```

```java
    @QueryMapping
    public ProjectWorkflow projectWorkflow(@Argument long id) {
        return projectWorkflowService.getProjectWorkflow(id);
    }
```

- [ ] **Step 2: Run codegen**

Run: `cd client && npx graphql-codegen > /tmp/t6.log 2>&1; echo $?`
Expected: `0`. Confirm the hooks landed:

```bash
grep -c "useUpdateProjectWorkflowErrorWorkflowMutation" client/src/shared/middleware/graphql.ts
```
Expected: at least `1`.

- [ ] **Step 3: Commit**

```bash
git add client/src/graphql client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "Add error-workflow GraphQL operations and regenerate client hooks

projectWorkflow(id) is a small companion query added alongside
eligibleErrorWorkflows so the per-workflow picker can read the current
override/disabled state without a bespoke facade method."
```

---

### Task 7: Project settings — Error Workflow picker

**Files:**
- Create: `client/src/pages/automation/project/components/ErrorWorkflowDialog.tsx`
- Create: `client/src/pages/automation/project/components/ErrorWorkflowDialog.test.tsx`
- Modify: `client/src/pages/automation/project/components/project-header/components/settings-menu/components/ProjectTabButtons/ProjectTabButtons.tsx`
- Modify: `client/src/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu.tsx`

**Interfaces:**
- Consumes: `useProjectErrorWorkflowQuery`, `useEligibleErrorWorkflowsQuery`,
  `useUpdateProjectErrorWorkflowMutation` (Task 6).
- Produces: an "Error Workflow" item on the Project settings tab opening a picker dialog; empty-state hint when
  the project has no eligible handlers.

Modeled directly on `ProjectGitConfigurationDialog.tsx` (react-hook-form + zod + the styled `Select`), the closest
existing precedent for "a settings dialog with a `Select` bound to a query-fetched option list, opened from
`ProjectTabButtons`/`SettingsMenu`'s existing `show*Dialog` boolean-state pattern."

- [ ] **Step 1: Write the failing test**

```tsx
import ErrorWorkflowDialog from '@/pages/automation/project/components/ErrorWorkflowDialog';
import * as graphql from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, test, vi} from 'vitest';

describe('ErrorWorkflowDialog', () => {
    test('shows the empty-state hint when the project has no eligible handlers', () => {
        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {eligibleErrorWorkflows: []},
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectErrorWorkflowQuery').mockReturnValue({
            data: {project: {errorProjectWorkflowId: null}},
        } as ReturnType<typeof graphql.useProjectErrorWorkflowQuery>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <ErrorWorkflowDialog onClose={vi.fn()} projectId="1" projectVersion={1} />
            </QueryClientProvider>
        );

        expect(screen.getByText(/add a new workflow error trigger/i)).toBeInTheDocument();
    });

    test('submits the selected handler', async () => {
        const mutateMock = vi.fn();

        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {
                eligibleErrorWorkflows: [{id: '99', workflow: {label: 'Handle Failures'}, workflowId: 'wf-99'}],
            },
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectErrorWorkflowQuery').mockReturnValue({
            data: {project: {errorProjectWorkflowId: null}},
        } as ReturnType<typeof graphql.useProjectErrorWorkflowQuery>);
        vi.spyOn(graphql, 'useUpdateProjectErrorWorkflowMutation').mockReturnValue({
            mutate: mutateMock,
        } as unknown as ReturnType<typeof graphql.useUpdateProjectErrorWorkflowMutation>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <ErrorWorkflowDialog onClose={vi.fn()} projectId="1" projectVersion={1} />
            </QueryClientProvider>
        );

        fireEvent.click(screen.getByRole('combobox'));
        fireEvent.click(await screen.findByText('Handle Failures'));
        fireEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({errorProjectWorkflowId: '99', projectId: '1'})
            )
        );
    });
});
```

- [ ] **Step 2: Run it to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/project/components/ErrorWorkflowDialog.test.tsx > /tmp/t7.log 2>&1; echo $?`
Expected: non-zero — the component does not exist yet.

- [ ] **Step 3: Write the dialog**

```tsx
import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {
    useEligibleErrorWorkflowsQuery,
    useProjectErrorWorkflowQuery,
    useUpdateProjectErrorWorkflowMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const NONE_VALUE = 'none';

const formSchema = z.object({
    errorProjectWorkflowId: z.string(),
});

interface ErrorWorkflowDialogProps {
    onClose: () => void;
    projectId: string;
    projectVersion: number;
}

const ErrorWorkflowDialog = ({onClose, projectId, projectVersion}: ErrorWorkflowDialogProps) => {
    const queryClient = useQueryClient();

    const {data: eligibleData} = useEligibleErrorWorkflowsQuery({projectId, projectVersion});
    const {data: projectData} = useProjectErrorWorkflowQuery({id: projectId});

    const eligibleWorkflows = eligibleData?.eligibleErrorWorkflows || [];
    const currentErrorProjectWorkflowId = projectData?.project?.errorProjectWorkflowId;

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            errorProjectWorkflowId: currentErrorProjectWorkflowId ? String(currentErrorProjectWorkflowId) : NONE_VALUE,
        },
        resolver: zodResolver(formSchema),
    });

    const updateProjectErrorWorkflowMutation = useUpdateProjectErrorWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['projectErrorWorkflow', {id: projectId}]});

            onClose();
        },
    });

    const handleSubmit = (values: z.infer<typeof formSchema>) => {
        updateProjectErrorWorkflowMutation.mutate({
            errorProjectWorkflowId: values.errorProjectWorkflowId === NONE_VALUE ? undefined : values.errorProjectWorkflowId,
            projectId,
        });
    };

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Error Workflow</DialogTitle>

                        <DialogDescription>
                            Runs when any workflow in this project fails, unless a workflow overrides or disables it.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                {eligibleWorkflows.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                        No eligible handlers yet — add a New Workflow Error trigger to a workflow in this project
                        first.
                    </p>
                ) : (
                    <Form {...form}>
                        <form className="space-y-8" onSubmit={form.handleSubmit(handleSubmit)}>
                            <FormField
                                control={form.control}
                                name="errorProjectWorkflowId"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Handler workflow</FormLabel>

                                        <FormControl>
                                            <Select onValueChange={field.onChange} value={field.value}>
                                                <SelectTrigger>
                                                    <SelectValue placeholder="None" />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    <SelectItem value={NONE_VALUE}>None</SelectItem>

                                                    {eligibleWorkflows.map((eligibleWorkflow) => (
                                                        <SelectItem key={eligibleWorkflow.id} value={eligibleWorkflow.id}>
                                                            {eligibleWorkflow.workflow.label}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <DialogFooter>
                                <DialogClose asChild>
                                    <Button label="Cancel" type="button" variant="outline" />
                                </DialogClose>

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ErrorWorkflowDialog;
```

- [ ] **Step 4: Wire the Project tab button**

In `ProjectTabButtons.tsx`, add an item (alongside `Project History`, before the destructive `Delete` separator):

```tsx
            <Button
                aria-label="Error Workflow"
                className="dropdown-menu-item"
                icon={<AlertTriangleIcon />}
                label="Error Workflow"
                onClick={onShowErrorWorkflowDialog}
                variant="ghost"
            />
```

(add `onShowErrorWorkflowDialog: () => void;` to the props type and destructure; add `AlertTriangleIcon` to the
existing `lucide-react` import, alphabetically sorted)

In `SettingsMenu.tsx`, add the `showErrorWorkflowDialog` state, pass `onShowErrorWorkflowDialog={() =>
setShowErrorWorkflowDialog(true)}` to `ProjectTabButtons`, and render:

```tsx
            {showErrorWorkflowDialog && (
                <ErrorWorkflowDialog
                    onClose={() => setShowErrorWorkflowDialog(false)}
                    projectId={String(project.id!)}
                    projectVersion={project.lastProjectVersion!}
                />
            )}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/project/components/ErrorWorkflowDialog.test.tsx > /tmp/t7.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Run the full client check and commit**

```bash
cd client && npm run check
git add client
git commit -m "Add the project-level Error Workflow picker dialog

Modeled on ProjectGitConfigurationDialog's react-hook-form + zod + styled Select
shape. Radix Select disallows an empty-string item value, so a 'none' sentinel
is translated to/from null at the mutate boundary. Empty-state hint when the
project has no eligible handlers, per spec."
```

---

### Task 8: Workflow settings — three-state per-workflow control

**Files:**
- Create: `client/src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog.tsx`
- Create: `client/src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog.test.tsx`
- Modify: `client/src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowTabButtons.tsx`
- Modify: `client/src/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu.tsx`

**Interfaces:**
- Consumes: `useProjectWorkflowErrorConfigQuery`, `useEligibleErrorWorkflowsQuery`,
  `useUpdateProjectWorkflowErrorWorkflowMutation` (Task 6).
- Produces: a "Error Handling" item on the Workflow settings tab opening a three-state picker: Inherit project
  default (`errorProjectWorkflowId = null`, `errorWorkflowDisabled = false`) / Override (`errorProjectWorkflowId =
  <picked>`, `errorWorkflowDisabled = false`) / Disabled (`errorWorkflowDisabled = true`, `errorProjectWorkflowId =
  null`) — mirroring `ErrorWorkflowResolver`'s exact resolution semantics: explicit disable beats inherited
  default, and eligible handlers exclude the workflow being configured itself.

- [ ] **Step 1: Write the failing test**

```tsx
import WorkflowErrorHandlingDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog';
import * as graphql from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, test, vi} from 'vitest';

describe('WorkflowErrorHandlingDialog', () => {
    test('disabling clears the override and sets errorWorkflowDisabled', async () => {
        const mutateMock = vi.fn();

        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {eligibleErrorWorkflows: []},
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectWorkflowErrorConfigQuery').mockReturnValue({
            data: {projectWorkflow: {errorProjectWorkflowId: null, errorWorkflowDisabled: false}},
        } as ReturnType<typeof graphql.useProjectWorkflowErrorConfigQuery>);
        vi.spyOn(graphql, 'useUpdateProjectWorkflowErrorWorkflowMutation').mockReturnValue({
            mutate: mutateMock,
        } as unknown as ReturnType<typeof graphql.useUpdateProjectWorkflowErrorWorkflowMutation>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <WorkflowErrorHandlingDialog
                    onClose={vi.fn()}
                    projectId="1"
                    projectVersion={1}
                    projectWorkflowId="10"
                />
            </QueryClientProvider>
        );

        fireEvent.click(screen.getByLabelText(/disabled/i));
        fireEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({
                    errorProjectWorkflowId: undefined,
                    errorWorkflowDisabled: true,
                    projectWorkflowId: '10',
                })
            )
        );
    });
});
```

- [ ] **Step 2: Run it to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog.test.tsx > /tmp/t8.log 2>&1; echo $?`
Expected: non-zero — the component does not exist yet.

- [ ] **Step 3: Write the dialog**

```tsx
import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {
    useEligibleErrorWorkflowsQuery,
    useProjectWorkflowErrorConfigQuery,
    useUpdateProjectWorkflowErrorWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

type ModeType = 'disabled' | 'inherit' | 'override';

interface WorkflowErrorHandlingDialogProps {
    onClose: () => void;
    projectId: string;
    projectVersion: number;
    projectWorkflowId: string;
}

const WorkflowErrorHandlingDialog = ({
    onClose,
    projectId,
    projectVersion,
    projectWorkflowId,
}: WorkflowErrorHandlingDialogProps) => {
    const queryClient = useQueryClient();

    const {data: eligibleData} = useEligibleErrorWorkflowsQuery({projectId, projectVersion});
    const {data: configData} = useProjectWorkflowErrorConfigQuery({id: projectWorkflowId});

    const currentConfig = configData?.projectWorkflow;

    const eligibleWorkflows = useMemo(
        () =>
            (eligibleData?.eligibleErrorWorkflows || []).filter(
                (eligibleWorkflow) => eligibleWorkflow.id !== projectWorkflowId
            ),
        [eligibleData, projectWorkflowId]
    );

    const [mode, setMode] = useState<ModeType>(
        currentConfig?.errorWorkflowDisabled
            ? 'disabled'
            : currentConfig?.errorProjectWorkflowId
              ? 'override'
              : 'inherit'
    );
    const [selectedWorkflowId, setSelectedWorkflowId] = useState(
        currentConfig?.errorProjectWorkflowId ? String(currentConfig.errorProjectWorkflowId) : ''
    );

    const updateMutation = useUpdateProjectWorkflowErrorWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['projectWorkflowErrorConfig', {id: projectWorkflowId}]});

            onClose();
        },
    });

    const handleSave = () => {
        updateMutation.mutate({
            errorProjectWorkflowId: mode === 'override' ? selectedWorkflowId : undefined,
            errorWorkflowDisabled: mode === 'disabled',
            projectId,
            projectWorkflowId,
        });
    };

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Error Handling</DialogTitle>

                        <DialogDescription>
                            Explicit disable beats the project's inherited default; an override runs instead of the
                            project default.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <RadioGroup className="space-y-3" onValueChange={(value) => setMode(value as ModeType)} value={mode}>
                    <div className="flex items-center gap-2">
                        <RadioGroupItem id="error-handling-inherit" value="inherit" />

                        <label htmlFor="error-handling-inherit">Inherit project default</label>
                    </div>

                    <div className="flex items-center gap-2">
                        <RadioGroupItem disabled={eligibleWorkflows.length === 0} id="error-handling-override" value="override" />

                        <label htmlFor="error-handling-override">Override</label>

                        {mode === 'override' && (
                            <Select onValueChange={setSelectedWorkflowId} value={selectedWorkflowId}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a workflow" />
                                </SelectTrigger>

                                <SelectContent>
                                    {eligibleWorkflows.map((eligibleWorkflow) => (
                                        <SelectItem key={eligibleWorkflow.id} value={eligibleWorkflow.id}>
                                            {eligibleWorkflow.workflow.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        )}
                    </div>

                    <div className="flex items-center gap-2">
                        <RadioGroupItem id="error-handling-disabled" value="disabled" />

                        <label htmlFor="error-handling-disabled">Disabled</label>
                    </div>
                </RadioGroup>

                <DialogFooter>
                    <DialogClose asChild>
                        <Button label="Cancel" type="button" variant="outline" />
                    </DialogClose>

                    <Button
                        disabled={mode === 'override' && !selectedWorkflowId}
                        label="Save"
                        onClick={handleSave}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowErrorHandlingDialog;
```

- [ ] **Step 4: Wire the Workflow tab button**

In `WorkflowTabButtons.tsx`, add an item (before `Export`):

```tsx
            <Button
                className="dropdown-menu-item"
                icon={<AlertTriangleIcon />}
                label="Error Handling"
                onClick={onShowErrorHandlingDialog}
                variant="ghost"
            />
```

(add `onShowErrorHandlingDialog: () => void;` to the props type and destructure)

In `SettingsMenu.tsx`, add `showWorkflowErrorHandlingDialog` state, pass it through to `WorkflowTabButtons`, and
render:

```tsx
            {showWorkflowErrorHandlingDialog && (
                <WorkflowErrorHandlingDialog
                    onClose={() => setShowWorkflowErrorHandlingDialog(false)}
                    projectId={String(project.id!)}
                    projectVersion={project.lastProjectVersion!}
                    projectWorkflowId={String((workflow as Workflow).projectWorkflowId)}
                />
            )}
```

(`workflow.projectWorkflowId` is already read this way elsewhere in `useSettingsMenu.ts`'s
`handleDeleteWorkflowAlertDialogClick`.)

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog.test.tsx > /tmp/t8.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 6: Run the full client check and commit**

```bash
cd client && npm run check
git add client
git commit -m "Add the per-workflow three-state Error Handling control

Inherit / Override / Disabled mirrors ErrorWorkflowResolver's resolution
semantics exactly: explicit disable beats an inherited project default, and the
eligible-handlers list excludes the workflow being configured, matching
ErrorWorkflowConfigurationValidator's self-reference rejection."
```

---

### Task 9: Documentation

**Files:**
- Modify: `docs/content/docs/automation/error-workflows.mdx`

**Interfaces:**
- Consumes: nothing new.
- Produces: nothing.

- [ ] **Step 1: Update the "how to configure it" section**

`error-workflows.mdx` (shipped by the base feature's Task 8) currently documents configuration as "GraphQL-only".
Update it to describe the project-settings and workflow-settings pickers from Tasks 7-8, keep the payload table
and limits sections unchanged, and drop the "no editor surface" caveat from the base plan's "Not in this plan"
section.

- [ ] **Step 2: Verify the docs build**

Run: `cd docs && npm run types:check > /tmp/t9.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 3: Commit**

```bash
git add docs
git commit -m "Document the error-workflow project/workflow settings pickers

Replaces the GraphQL-only configuration note now that Tasks 7-8 ship the editor
surface."
```

---

## Not in this plan

- **Cross-project handlers** — out of scope per the spec; a handler must live in the same project as the workflow
  it handles, enforced by `ErrorWorkflowConfigurationValidator`.
- **Editor-canvas affordances beyond settings** — no node-level or canvas-level error-workflow UI; both pickers
  live in the existing settings menus only.
- **Embedded** — the feature is automation-only, matching the base plan.
- **Bulk override management** (e.g. "set this handler on every workflow in the project at once") — the
  per-workflow dialog is one workflow at a time, matching the spec's stated three-state control shape.
