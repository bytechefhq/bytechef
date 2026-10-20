# Apps — Agent Loop, Server (D1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A coding agent on the management MCP can build an app-callable workflow, deploy its project to the single
DEVELOPMENT deployment with every workflow row, connection and input in place, and list what an app can call; the CLI
(Plan D2) gets the four read-only management REST routes it needs.

**Architecture:** A CE SPI, `CallableWorkflowChecker`, carries the EE `AppCallableWorkflowResolver`'s definition of
"callable" into CE code (the `buildWorkflow` wrapper). `ProjectDeploymentFacade.deployProject` owns every deployment
rule (uuid keying, unpublished changes, a row per workflow, connection choice, all-or-nothing) behind one guard; the
EE `AppLoopTools` only resolve scope, compute the default enabled set, arm the deployment and shape the answer. The
REST routes delegate to `AppFacade`/`AppWorkflowFacade`, which own authorization; an end-to-end IntTest in `server-app`
drives build → publish → deploy → bind → call twice.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI 2.0.1 (`@Tool`, `ToolCallback`, `ToolContext`), Spring Data JDBC,
openapi-generator 7.25 (`spring`, `interfaceOnly`, `useTags`), Jackson 3 (`tools.jackson`), JUnit 5, Mockito, AssertJ,
Testcontainers (PostgreSQL), `@WebMvcTest`.

**Spec:** `docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md` (sections "Management MCP (workflow half)",
"Server REST (for the CLI)", "Testing — Plan D1", and the `.agents/apps.md` line of "Changes to the parent spec").
Plan D2 (CLI, template, plugin) builds against the REST contract this plan merges.

## Rulings on the spec

Read with the spec; each is binding for this plan.

1. **The generated-instructions test** ("on a CE tool set `buildInstructions` omits the app-loop section") moves to
   Plan D2. `buildInstructions` reads the generated `bytechef/mcp-instructions.md`, and the app-loop section only exists
   once D2 adds the `bytechef-app-builder` skill and regenerates the file; in D1 the test would pass vacuously. Plan D2
   (Task 8) owns it.
7. **REST contract.** The four routes use exactly the operationIds, schema names and field names Plan D2
   (`docs/superpowers/plans/2026-10-08-apps-agent-loop-cli.md`, Task 1) copies into the CLI client:
   `getAppBindableConnections`, `getAppBindableWorkflows`, `getAppComponentOperations`, `getWorkspaceConnections` (tag
   `workspace-connection`); schemas `AppBindableConnection`, `AppBindableWorkflow`, `AppComponentOperations`,
   `AppWorkspaceConnection`.
2. **System deployments.** `getWorkspaceProjectDeployments` hides system *projects* but not system *deployments*
   (the `__API_COLLECTION__`/`__MCP_SERVER__`/`__A2A_SERVER__` name markers), so a project that also backs an MCP server
   has a second DEVELOPMENT deployment. `SystemProjects` gains `isSystemDeploymentName(String)` (the resolver's private
   check, made shared); `deployProject` (facade and tool) and the resolver's listing ignore such deployments. Without
   it, `deployProject` would update an MCP server's deployment in place.
3. **Unpublished changes** are decided inside `ProjectDeploymentFacade.deployProject`, so the four cases the spec lists
   under the `AppLoopTools` tests (never published, changed definition, added workflow, removed workflow) are pinned in
   `ProjectDeploymentFacadeDeployProjectIntTest` against real rows; `AppLoopToolsTest` pins that a facade refusal is
   returned to the agent as `refused` with its message.
4. **AI-provider connections are never `usable`.** `AppFacade.getConnections` reports them with `aiProvider: true` and
   `usable: false` without running a `CONNECTION_USE` check on their virtual id.
5. **New collaborators of `ProjectDeploymentFacadeImpl`** (`ConnectionDefinitionService`, `PermissionService`,
   `WorkspaceConnectionFacade`) are injected `@Lazy`. Nine hand-assembled IntTest contexts in seven modules build this
   class without those beans; `@Lazy` keeps them booting, and the beans are resolved only when `deployProject` runs.
6. **Wrapped tools cannot throw messages to the agent.** `WorkspaceScopedFlatToolCallback` turns any exception into
   "`<tool>` failed (`<ExceptionClass>`)", dropping the message. `AppLoopTools` therefore return refusals as values
   (`status: "refused"`, `error`), catching the facade's `ConfigurationException`/`IllegalArgumentException`.

## Global Constraints

- **DEVELOPMENT only.** An `environment` argument other than `DEVELOPMENT` is refused with
  `"The app loop runs in DEVELOPMENT only"` (verbatim) by `deployProject`, `listConnections` and
  `listAppCallableWorkflows`. `deployProject` only ever creates or updates a DEVELOPMENT deployment.
- **Uuid keying.** "Every new tool input and result keys workflows by uuid; a `workflowId` appears only as the DRAFT id
  to pass to `buildWorkflow`." (`draftWorkflowId`). The facade's `DeployedWorkflow.workflowId` (the deployed version's
  id) is internal: the tool uses it for the callable check and never returns it.
- **Guards, verbatim.**
  - `ProjectDeploymentFacade.deployProject`:
    `hasResourceScopeInEnvironment(#projectId, 'Project', 'WORKFLOW_EDIT', T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT) and (#projectDeploymentId == null or hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT'))`
  - `AppWorkflowFacade.getCallableWorkflows`:
    `hasWorkspaceScopeInEnvironment(#workspaceId, 'DEPLOYMENT_VIEW', T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)`
  - `AppFacade.getConnections`: `hasWorkspaceScopeInEnvironment(#workspaceId, 'CONNECTION_VIEW', #environment)`
  - `AppFacade.getBindableConnections` keeps `hasPermission(#appId, 'App', 'APP_MANAGE')`.
- **Usable connection:** `credentialStatus` is `VALID` and the caller holds `CONNECTION_USE`
  (`PermissionService.hasResourceScope(id, "Connection", "CONNECTION_USE")`). AI-provider connections are never a
  candidate for a deployment slot.
- **Rows:** every workflow of the deployed version gets a deployment-workflow row, never a subset. Listed uuids are
  enabled; an unlisted workflow keeps its current row's enabled state; a new row starts disabled.
- **Choice per slot:** explicit `connections` entry (must be a candidate, else refused) → the existing row's connection
  for the same slot while still a candidate → the only candidate. An optional slot without a choice stays empty.
- **All or nothing:** an enabled workflow with an unresolved required slot or a missing required input means nothing
  is written. A disabled workflow's slots are resolved where possible and never block.
- **Write:** one transaction; a new deployment is saved disabled; an update disarms its enabled workflows and is marked
  disabled before rows are written through `checkProjectDeploymentWorkflows`. Arming is the tool's separate
  `enableProjectDeployment(id, true)`; its failure leaves the written deployment disabled and is reported as
  `arming failed`.
- **`buildWorkflow` wrapper** forwards `{"request": …}` only; `appCallable` exists in its schema only when a
  `CallableWorkflowChecker` bean is present, and only on the management MCP wrapper. No new runtime check for Copilot
  being enabled.
- **Facades own authorization.** Controllers carry no `@PreAuthorize`; every new REST controller extends
  `AbstractAppApiController` (403, not 500, on `AccessDeniedException`). Facades use services, never repositories.
  Tools go through facades only.
- **No secret leaves the server through D1.** No tool or route added here returns an app key or connection
  parameters/credentials.
- **Headers.** New files under `server/ee/` carry the ByteChef Enterprise header and a `@version ee` Javadoc tag
  (Spotless picks the header from that tag):
  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */
  ```
  Files under `server/libs/` and `server/apps/` carry the Apache 2.0 header of their neighbours.
- **Java style (CLAUDE.md):** one blank line before control statements and after a variable modification that the next
  statement uses; no trailing blank line in a class body; descriptive names (no `o`, `e` for non-exceptions, `u`);
  no method chaining outside builders/streams/Optional/AssertJ/MockMvc/RestClient DSLs; no rationale `//` comments;
  no `TODO:`; test methods camelCase (new tests follow the prefix style of the class they join); IntTests end in
  `IntTest`.
- **Commits.** Commit by explicit path only, never `git add -A`; never amend; never `git stash`. No `Co-Authored-By`
  and no "Generated with" line. The first commit of this plan opens a group: `--- <description>`; every later commit:
  `- <description>`; the docs commit: `- docs - <description>`.
- **Gradle.** Every Gradle call: redirect to a log in your scratchpad, then `echo "exit=$?"` on its own line, then
  `grep '^> Task .* FAILED' <log>`. Never pipe Gradle into `tail`/`grep`. Foreground with a 600000 ms timeout; never
  background or `sleep`. Use `--continue` on `check`. `<scratchpad>` below means your session scratchpad directory.
- **Module abbreviations** used in commands (Gradle paths):

  | Abbrev | Gradle path |
  |--------|-------------|
  | `ACA` | `:server:libs:automation:automation-configuration:automation-configuration-api` |
  | `ACS` | `:server:libs:automation:automation-configuration:automation-configuration-service` |
  | `ACG` | `:server:libs:automation:automation-configuration:automation-configuration-graphql` |
  | `EACS` | `:server:ee:libs:automation:automation-configuration:automation-configuration-service` |
  | `PCA` | `:server:libs:platform:platform-configuration:platform-configuration-api` |
  | `PCS` | `:server:libs:platform:platform-configuration:platform-configuration-service` |
  | `PCRC` | `:server:ee:libs:platform:platform-configuration:platform-configuration-remote-client` |
  | `ECS` | `:server:ee:libs:embedded:embedded-configuration:embedded-configuration-service` |
  | `AAA` | `:server:ee:libs:automation:automation-app:automation-app-api` |
  | `AAS` | `:server:ee:libs:automation:automation-app:automation-app-service` |
  | `AAG` | `:server:ee:libs:automation:automation-app:automation-app-graphql` |
  | `AAR` | `:server:ee:libs:automation:automation-app:automation-app-public-rest` |
  | `AAT` | `:server:libs:automation:automation-ai:automation-ai-tool` |
  | `EAAT` | `:server:ee:libs:automation:automation-ai:automation-ai-tool` |
  | `ACT` | `:server:libs:ai:ai-copilot:ai-copilot-tool` |
  | `ACOS` | `:server:libs:ai:ai-copilot:ai-copilot-service` |
  | `SA` | `:server:apps:server-app` |

  Write the full path in the command (the shell does not know the abbreviation): e.g. `ACA:check` means
  `./gradlew --continue :server:libs:automation:automation-configuration:automation-configuration-api:check`.

## Review Focus

Failure modes the spec implies but its test list does not exercise, most likely first. Each has a test in the task
named.

1. **A project that also backs an MCP server (or API collection, A2A server)** has a `__MCP_SERVER__…` DEVELOPMENT
   deployment. `deployProject` must neither count it as "several deployments" nor update it in place; the callable
   listing must not offer it. — Task 1 (`AppCallableWorkflowResolverTest.testBindableWorkflowsSkipSystemDeploymentsAndDeploymentsOfSystemProjects`),
   Task 3 (`ProjectDeploymentFacadeDeployProjectIntTest.testSystemDeploymentOfTheProjectIsIgnored`), Task 5
   (`AppLoopToolsTest.testSystemDeploymentsDoNotCountAsTheProjectsDeployment`).
2. **Redeploying the same published version** (no publish in between, e.g. to change one connection) must update the
   rows in place, not duplicate them, and keep the existing connection when it is still a candidate. — Task 3
   (`testRedeployOfTheSameVersionUpdatesRowsInPlace`).
3. **An agent passing a `workflowId` where a uuid belongs**, or a connection for a slot the workflow does not have, is
   refused with the version's uuids named and nothing written. — Task 3
   (`testUnknownWorkflowUuidIsRefusedNamingTheVersionsUuids`, `testConnectionForAnUnknownSlotIsRefused`).
4. **A disabled workflow whose required slot has several candidates** must not block the deploy; its row is written,
   disabled, without a connection. — Task 3 (`testDisabledWorkflowWithAnAmbiguousSlotDoesNotBlock`).
5. **A `buildWorkflow` run that persists the same workflow twice and a second workflow** checks each distinct id once,
   and an error names only the failing id. — Task 6
   (`WorkspaceScopedSubAgentToolCallbackAppCallableTest.testEachDistinctCapturedWorkflowIsCheckedOnceAndOnlyFailuresAreNamed`).

## File map

| File | Responsibility | Task |
|------|----------------|------|
| `ACA …/configuration/workflow/CallableWorkflowChecker.java` (new) | CE SPI: is a workflow app-callable | 1 |
| `ACA …/configuration/workflow/CallableWorkflowCheck.java` (new) | its answer: problems + inputSchema | 1 |
| `ACA …/configuration/domain/SystemProjects.java` | `isSystemDeploymentName` | 1 |
| `AAS …/app/workflow/AppCallableWorkflowResolver.java` | implements the SPI; keyed by `(workspaceId, environment)`; listing drops system deployments | 1 |
| `AAS` `AppWorkflowFacadeImpl`, `AppWorkflowExecutionFacadeImpl`, `AppWorkflowServiceImpl` | pass the app's workspace and environment | 1 |
| `PCA …/facade/ComponentConnectionFacade.java`, `PCS …/ComponentConnectionFacadeImpl.java` | `getWorkflowComponentConnections` (the old `WorkflowConnectionSlots` body) | 2 |
| `PCRC …/RemoteComponentConnectionFacadeClient.java` | stub for the new method | 2 |
| `ECS` `ConnectedUserWorkflowConnectionResolver`, `ConnectedUserReferenceAttentionResolver`; `WorkflowConnectionSlots` (deleted) | switch to the facade | 2 |
| `ACA …/dto/DeployProjectResultDTO.java`, `DeploymentConnectionChoiceDTO.java` (new) | facade contract | 3 |
| `ACA …/exception/ProjectDeploymentErrorType.java` | 103–105 | 3 |
| `ACA …/facade/ProjectDeploymentFacade.java`, `ACS …/ProjectDeploymentFacadeImpl.java` | `deployProject` | 3 |
| `ACS` test `ProjectDeploymentFacadeDeployProjectIntTest.java` (new) | the deploy rules against Postgres | 3 |
| `AAA …/dto/AppWorkspaceConnectionDTO.java`, `AppCallableWorkflowDTO.java` (new); `AppBindableConnectionDTO.java` (+`usable`) | lookup DTOs | 4 |
| `AAA` `AppFacade`, `AppWorkflowFacade`; `AAS` impls | `getConnections`, `getBindableConnections(appId, componentName)`, `getCallableWorkflows` | 4 |
| `EAAT …/ai/tool/AppLoopTools.java`, `AppLoopMcpContributorConfiguration.java` (new) | the three MCP tools and their registration | 5 |
| `AAT` `CreateProjectDeploymentToolCallback`, `PromoteWorkflowToolCallback` | descriptions point to `deployProject` | 5 |
| `ACT …/util/WorkflowPersistCaptureUtils.java` | keep an existing holder; `getCapturedWorkflowIds` | 6 |
| `AAT …/AppCallableDraftGuard.java` (new); `ProjectWorkflowTools`, `ScriptTools`, `ProjectWorkflowLifecycleTools`, `WorkspaceScopedSubAgentToolCallback` | DRAFT-only refusals; app-callable mode | 6 |
| `ACOS …/ToolCallbackContributorConfiguration.java` | passes the checker to `buildWorkflow`'s wrapper | 6 |
| `AAR` `openapi-management.yaml`, `generated/**`, `AppApiController`, `AppWorkflowBindingApiController`, `WorkspaceConnectionApiController` (new) | four routes | 7 |
| `SA` test `AppAgentLoopIntTest.java` (new) | end to end | 8 |
| `.agents/apps.md`, `.agents/embedded-bridge.md` | docs | 9 |

Paths: `ACA …` = `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation`,
`AAS …` = `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation`,
and so on; every task below spells its paths out in full.

---

### Task 1: `CallableWorkflowChecker` SPI and the resolver keyed by workspace and environment

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/workflow/CallableWorkflowChecker.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/workflow/CallableWorkflowCheck.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/SystemProjects.java`
- Test: `server/libs/automation/automation-configuration/automation-configuration-api/src/test/java/com/bytechef/automation/configuration/domain/SystemProjectsTest.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolver.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeImpl.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacadeImpl.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/service/AppWorkflowServiceImpl.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolverTest.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeTest.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacadeTest.java`
- Test: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/service/AppWorkflowServiceTest.java`

**Interfaces:**
- Produces (CE, `com.bytechef.automation.configuration.workflow`):
  `public interface CallableWorkflowChecker { CallableWorkflowCheck check(String workflowId); }` and
  `public record CallableWorkflowCheck(List<String> problems, @Nullable String inputSchema) { public boolean callable(); }`
- Produces (CE): `public static boolean SystemProjects.isSystemDeploymentName(@Nullable String name)`
- Produces (EE): `AppCallableWorkflowResolver implements CallableWorkflowChecker`,
  `List<BindableWorkflow> getBindableWorkflows(long workspaceId, Environment environment)`,
  `Optional<CallableWorkflow> resolve(long workspaceId, Environment environment, long projectDeploymentId, String workflowUuid)`.
  The old `App`-taking overloads are removed.
- Problem strings returned by `check` (later tasks and tests match on them):
  `"workflow <id> does not exist"`, `"no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall"`,
  `"unreadable inputSchema: <reason>"`.

- [ ] **Step 1: Write the failing `SystemProjects` test.** Append to `SystemProjectsTest` (before its closing brace):

  ```java
      @Test
      void testDeploymentMarkersAreRecognized() {
          assertTrue(
              SystemProjects.isSystemDeploymentName(SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX + "orders"));
          assertTrue(SystemProjects.isSystemDeploymentName(SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX + "orders"));
          assertTrue(SystemProjects.isSystemDeploymentName(SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX + "orders"));
      }

      @Test
      void testUserDeploymentNamesAreNotSystemDeployments() {
          assertFalse(SystemProjects.isSystemDeploymentName("Orders"));
          assertFalse(SystemProjects.isSystemDeploymentName("orders " + SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX));
          assertFalse(SystemProjects.isSystemDeploymentName(null));
      }
  ```

- [ ] **Step 2: Run it to see it fail**

  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:test --tests "com.bytechef.automation.configuration.domain.SystemProjectsTest" > <scratchpad>/d1-t1-red.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; the log names `cannot find symbol` / `isSystemDeploymentName`.

- [ ] **Step 3: Add `isSystemDeploymentName` and the SPI.**

  In `SystemProjects.java` add `import org.jspecify.annotations.Nullable;` and, directly after `isSystemProjectName`,
  this method:

  ```java
      /**
       * Whether a project deployment name carries one of the deployment markers
       * ({@link #API_COLLECTION_DEPLOYMENT_NAME_PREFIX}, {@link #MCP_SERVER_DEPLOYMENT_NAME_PREFIX},
       * {@link #A2A_SERVER_DEPLOYMENT_NAME_PREFIX}): a deployment a feature created for itself, which no deployment
       * picker or app may treat as the user's own.
       */
      public static boolean isSystemDeploymentName(@Nullable String name) {
          return name != null && (name.startsWith(API_COLLECTION_DEPLOYMENT_NAME_PREFIX) ||
              name.startsWith(MCP_SERVER_DEPLOYMENT_NAME_PREFIX) || name.startsWith(A2A_SERVER_DEPLOYMENT_NAME_PREFIX));
      }
  ```

  Create `CallableWorkflowChecker.java` (copy the Apache header of `SystemProjects.java`, lines 1–15, verbatim):

  ```java
  package com.bytechef.automation.configuration.workflow;

  /**
   * Answers whether a workflow can be called by an app: triggered by {@code workflow/v1/newWorkflowCall} with an
   * {@code inputSchema} that is absent or readable JSON Schema. The implementation is EE
   * ({@code AppCallableWorkflowResolver}), so binding, listing, execution and this check share one definition; CE code
   * receives it through an {@code ObjectProvider} and treats its absence as "no app-callable checks on this edition".
   *
   * @author Ivica Cardic
   */
  public interface CallableWorkflowChecker {

      CallableWorkflowCheck check(String workflowId);
  }
  ```

  Create `CallableWorkflowCheck.java` (same header):

  ```java
  package com.bytechef.automation.configuration.workflow;

  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.List;
  import org.jspecify.annotations.Nullable;

  /**
   * The answer of {@link CallableWorkflowChecker#check(String)}: what keeps the workflow from being app-callable (empty
   * when it is) and the trigger's input schema when one was found.
   *
   * @author Ivica Cardic
   */
  @SuppressFBWarnings("EI")
  public record CallableWorkflowCheck(List<String> problems, @Nullable String inputSchema) {

      public boolean callable() {
          return problems.isEmpty();
      }
  }
  ```

  `automation-configuration-api` already has `spotbugs-annotations` on its classpath through `SystemProjects`'
  neighbours (`ProjectDeploymentDTO` uses `@SuppressFBWarnings`); no build change.

- [ ] **Step 4: Run the `SystemProjects` test to see it pass**

  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:test --tests "com.bytechef.automation.configuration.domain.SystemProjectsTest" > <scratchpad>/d1-t1-green.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.

- [ ] **Step 5: Write the failing resolver tests.** In `AppCallableWorkflowResolverTest`:

  1. Remove the `private App app;` field, the four `app` lines in `beforeEach` (keep the `resolver = new …` statement)
     and the `import com.bytechef.ee.automation.app.domain.App;` line.
  2. Rewrite the call sites:
     ```bash
     cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416/server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow
     sed -i '' -e 's/resolver\.resolve(app, /resolver.resolve(WORKSPACE_ID, Environment.PRODUCTION, /' \
       -e 's/resolver\.getBindableWorkflows(app)/resolver.getBindableWorkflows(WORKSPACE_ID, Environment.PRODUCTION)/' \
       AppCallableWorkflowResolverTest.java
     ```
  3. Add `import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;` and these tests before
     `private ProjectDeployment deployment(`:

  ```java
      @Test
      void testBindableWorkflowsSkipSystemDeploymentsAndDeploymentsOfSystemProjects() {
          ProjectDeployment userDeployment = deployment(DEPLOYMENT_ID, Environment.PRODUCTION, true);
          ProjectDeployment mcpServerDeployment = deployment(13L, Environment.PRODUCTION, true);
          ProjectDeployment systemProjectDeployment = deployment(14L, Environment.PRODUCTION, true);
          Project systemProject = project(WORKSPACE_ID);

          mcpServerDeployment.setName(SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX + "orders");
          systemProjectDeployment.setProjectId(4L);
          systemProject.setId(4L);
          systemProject.setName(SystemProjects.KNOWLEDGE_BASE_NAME_PREFIX + "7");

          when(projectDeploymentService.getProjectDeployments(false, Environment.PRODUCTION, null, null, WORKSPACE_ID))
              .thenReturn(List.of(userDeployment, mcpServerDeployment, systemProjectDeployment));
          when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project(WORKSPACE_ID)));
          when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(DEPLOYMENT_ID))
              .thenReturn(List.of(deploymentWorkflow("wf-2", true, DEPLOYMENT_ID)));
          when(workflowService.fetchWorkflow("wf-2"))
              .thenReturn(Optional.of(workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
          when(projectWorkflowService.getProjectWorkflowUuid(DEPLOYMENT_ID, "wf-2")).thenReturn(WORKFLOW_UUID);

          lenient().when(projectService.fetchProject(4L))
              .thenReturn(Optional.of(systemProject));
          lenient().when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(13L))
              .thenReturn(List.of(deploymentWorkflow("wf-13", true, 13L)));
          lenient().when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(14L))
              .thenReturn(List.of(deploymentWorkflow("wf-14", true, 14L)));
          lenient().when(workflowService.fetchWorkflow("wf-13"))
              .thenReturn(Optional.of(workflow("wf-13", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
          lenient().when(workflowService.fetchWorkflow("wf-14"))
              .thenReturn(Optional.of(workflow("wf-14", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));
          lenient().when(projectWorkflowService.getProjectWorkflowUuid(13L, "wf-13"))
              .thenReturn("mcp-uuid");
          lenient().when(projectWorkflowService.getProjectWorkflowUuid(14L, "wf-14"))
              .thenReturn("system-project-uuid");

          assertThat(resolver.getBindableWorkflows(WORKSPACE_ID, Environment.PRODUCTION))
              .extracting(BindableWorkflow::workflowUuid)
              .containsExactly(WORKFLOW_UUID);
      }

      @Test
      void testCheckAcceptsACallWorkflowWorkflowAndReturnsItsInputSchema() {
          when(workflowService.fetchWorkflow("wf-2"))
              .thenReturn(Optional.of(workflow("wf-2", INPUT_SCHEMA, "workflow/v1/newWorkflowCall")));

          CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

          assertThat(callableWorkflowCheck.callable()).isTrue();
          assertThat(callableWorkflowCheck.problems()).isEmpty();
          assertThat(callableWorkflowCheck.inputSchema()).isEqualTo(INPUT_SCHEMA);
      }

      @Test
      void testCheckReportsAWorkflowWithoutACallWorkflowTrigger() {
          when(workflowService.fetchWorkflow("wf-2"))
              .thenReturn(Optional.of(workflow("wf-2", null, "manual/v1/manual")));

          CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

          assertThat(callableWorkflowCheck.callable()).isFalse();
          assertThat(callableWorkflowCheck.problems())
              .containsExactly("no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall");
      }

      @Test
      void testCheckReportsAnUnreadableInputSchema() {
          when(workflowService.fetchWorkflow("wf-2"))
              .thenReturn(Optional.of(workflow("wf-2", "{not json", "workflow/v1/newWorkflowCall")));

          CallableWorkflowCheck callableWorkflowCheck = resolver.check("wf-2");

          assertThat(callableWorkflowCheck.callable()).isFalse();
          assertThat(callableWorkflowCheck.problems())
              .singleElement()
              .asString()
              .startsWith("unreadable inputSchema: ");
      }

      @Test
      void testCheckReportsAMissingWorkflow() {
          when(workflowService.fetchWorkflow("wf-9")).thenReturn(Optional.empty());

          assertThat(resolver.check("wf-9")
              .problems()).containsExactly("workflow wf-9 does not exist");
      }
  ```

- [ ] **Step 6: Run them to see them fail**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:compileTestJava > <scratchpad>/d1-t1-red2.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; `cannot find symbol` for `check`, and `resolve`/`getBindableWorkflows` signature mismatches.

- [ ] **Step 7: Replace `AppCallableWorkflowResolver.java`** with:

  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */

  package com.bytechef.ee.automation.app.workflow;

  import com.bytechef.atlas.configuration.domain.Workflow;
  import com.bytechef.atlas.configuration.domain.WorkflowTask;
  import com.bytechef.atlas.configuration.service.WorkflowService;
  import com.bytechef.automation.configuration.domain.Project;
  import com.bytechef.automation.configuration.domain.ProjectDeployment;
  import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
  import com.bytechef.automation.configuration.domain.SystemProjects;
  import com.bytechef.automation.configuration.service.ProjectDeploymentService;
  import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
  import com.bytechef.automation.configuration.service.ProjectService;
  import com.bytechef.automation.configuration.service.ProjectWorkflowService;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
  import com.bytechef.commons.util.MapUtils;
  import com.bytechef.platform.annotation.ConditionalOnEEVersion;
  import com.bytechef.platform.component.constant.WorkflowConstants;
  import com.bytechef.platform.configuration.domain.Environment;
  import com.bytechef.platform.configuration.domain.WorkflowTrigger;
  import com.bytechef.platform.definition.WorkflowNodeType;
  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;
  import java.util.Objects;
  import java.util.Optional;
  import org.jspecify.annotations.Nullable;
  import org.springframework.stereotype.Component;

  /**
   * Resolves a workflow binding to the workflow it runs now. A binding names a project deployment and a workflow uuid;
   * the workflow id is looked up on every call, so a redeploy changes what the app runs. A binding resolves only while
   * the deployment exists, is enabled, belongs to the given workspace and environment, is not a system deployment (an API
   * collection, MCP server or A2A server deployment, or a deployment of a system project), its deployment workflow exists
   * and is enabled, the workflow is triggered by {@code workflow/newWorkflowCall} and that trigger's input schema is
   * readable JSON. This is the one definition of a callable workflow: binding, listing, execution and
   * {@link CallableWorkflowChecker#check(String)} all use it. Deployments are looked up by workspace and environment,
   * never through an app, so the app bindings and the agent loop's listing share it.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  @Component
  @ConditionalOnEEVersion
  public class AppCallableWorkflowResolver implements CallableWorkflowChecker {

      static final String NO_CALL_WORKFLOW_TRIGGER =
          "no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall";
      static final String UNREADABLE_INPUT_SCHEMA = "unreadable inputSchema: ";

      private final ProjectDeploymentService projectDeploymentService;
      private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
      private final ProjectService projectService;
      private final ProjectWorkflowService projectWorkflowService;
      private final WorkflowService workflowService;

      @SuppressFBWarnings("EI")
      public AppCallableWorkflowResolver(
          ProjectDeploymentService projectDeploymentService,
          ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
          ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

          this.projectDeploymentService = projectDeploymentService;
          this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
          this.projectService = projectService;
          this.projectWorkflowService = projectWorkflowService;
          this.workflowService = workflowService;
      }

      @Override
      public CallableWorkflowCheck check(String workflowId) {
          Optional<Workflow> workflow = workflowService.fetchWorkflow(workflowId);

          if (workflow.isEmpty()) {
              return new CallableWorkflowCheck(List.of("workflow %s does not exist".formatted(workflowId)), null);
          }

          Optional<WorkflowTrigger> callableTrigger = fetchCallableTrigger(workflow.get());

          if (callableTrigger.isEmpty()) {
              return new CallableWorkflowCheck(List.of(NO_CALL_WORKFLOW_TRIGGER), null);
          }

          WorkflowTrigger workflowTrigger = callableTrigger.get();

          String inputSchema = blankToNull(
              MapUtils.getString(workflowTrigger.getParameters(), WorkflowConstants.INPUT_SCHEMA));

          try {
              AppWorkflowSchemas.checkReadable(inputSchema);
          } catch (IllegalArgumentException illegalArgumentException) {
              return new CallableWorkflowCheck(
                  List.of(UNREADABLE_INPUT_SCHEMA + illegalArgumentException.getMessage()), inputSchema);
          }

          return new CallableWorkflowCheck(List.of(), inputSchema);
      }

      public List<BindableWorkflow> getBindableWorkflows(long workspaceId, Environment environment) {
          List<BindableWorkflow> bindableWorkflows = new ArrayList<>();

          List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
              false, environment, null, null, workspaceId);

          for (ProjectDeployment projectDeployment : projectDeployments) {
              if (!projectDeployment.isEnabled() || SystemProjects.isSystemDeploymentName(projectDeployment.getName())) {
                  continue;
              }

              Optional<Project> project = fetchProjectInWorkspace(workspaceId, projectDeployment)
                  .filter(curProject -> !isSystemProject(curProject));

              if (project.isEmpty()) {
                  continue;
              }

              long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId());

              for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
                  .getProjectDeploymentWorkflows(projectDeploymentId)) {

                  if (!projectDeploymentWorkflow.isEnabled()) {
                      continue;
                  }

                  toCallableWorkflow(projectDeploymentWorkflow).ifPresent(callableWorkflow -> bindableWorkflows.add(
                      new BindableWorkflow(
                          projectDeployment, project.get(),
                          projectWorkflowService.getProjectWorkflowUuid(
                              projectDeploymentId, projectDeploymentWorkflow.getWorkflowId()),
                          callableWorkflow)));
              }
          }

          return bindableWorkflows;
      }

      public Optional<CallableWorkflow> resolve(
          long workspaceId, Environment environment, long projectDeploymentId, String workflowUuid) {

          Optional<ProjectDeployment> fetchedProjectDeployment = projectDeploymentService.fetchProjectDeployment(
              projectDeploymentId);

          if (fetchedProjectDeployment.isEmpty()) {
              return Optional.empty();
          }

          ProjectDeployment projectDeployment = fetchedProjectDeployment.get();

          if (!projectDeployment.isEnabled() || projectDeployment.getEnvironment() != environment ||
              SystemProjects.isSystemDeploymentName(projectDeployment.getName())) {

              return Optional.empty();
          }

          Optional<Project> project = fetchProjectInWorkspace(workspaceId, projectDeployment);

          if (project.isEmpty() || isSystemProject(project.get())) {
              return Optional.empty();
          }

          Optional<String> workflowId;

          try {
              workflowId = projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid);
          } catch (IllegalArgumentException illegalArgumentException) {
              return Optional.empty();
          }

          return workflowId
              .flatMap(id -> projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeploymentId, id))
              .filter(ProjectDeploymentWorkflow::isEnabled)
              .flatMap(this::toCallableWorkflow);
      }

      private Optional<Project> fetchProjectInWorkspace(long workspaceId, ProjectDeployment projectDeployment) {
          return projectService.fetchProject(projectDeployment.getProjectId())
              .filter(project -> Objects.equals(project.getWorkspaceId(), workspaceId));
      }

      /**
       * Projects a feature provisions for itself, including the embedded deployment marker the binding picker excludes
       * through {@code getProjectDeployments(false, ...)}.
       */
      private static boolean isSystemProject(Project project) {
          String name = project.getName();

          return SystemProjects.isSystemProjectName(name)
              || (name != null && name.startsWith(SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX));
      }

      private Optional<CallableWorkflow> toCallableWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
          String workflowId = projectDeploymentWorkflow.getWorkflowId();

          return workflowService.fetchWorkflow(workflowId)
              .flatMap(workflow -> fetchCallableTrigger(workflow).flatMap(trigger -> {
                  String inputSchema = blankToNull(
                      MapUtils.getString(trigger.getParameters(), WorkflowConstants.INPUT_SCHEMA));

                  if (!isReadable(inputSchema)) {
                      return Optional.empty();
                  }

                  String label = workflow.getLabel() != null ? workflow.getLabel() : workflowId;

                  return Optional.of(
                      new CallableWorkflow(
                          workflowId, trigger.getName(), label, workflow.getDescription(), inputSchema,
                          fetchOutputSchema(workflow), projectDeploymentWorkflow.getInputs()));
              }));
      }

      private static boolean isReadable(@Nullable String inputSchema) {
          try {
              AppWorkflowSchemas.checkReadable(inputSchema);

              return true;
          } catch (IllegalArgumentException illegalArgumentException) {
              return false;
          }
      }

      private static @Nullable String blankToNull(@Nullable String value) {
          return value == null || value.isBlank() ? null : value;
      }

      private static Optional<WorkflowTrigger> fetchCallableTrigger(Workflow workflow) {
          for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
              if (isWorkflowOperation(workflowTrigger.getType(), WorkflowConstants.NEW_WORKFLOW_CALL)) {
                  return Optional.of(workflowTrigger);
              }
          }

          return Optional.empty();
      }

      private static @Nullable String fetchOutputSchema(Workflow workflow) {
          for (WorkflowTask workflowTask : workflow.getTasks(true)) {
              if (isWorkflowOperation(workflowTask.getType(), WorkflowConstants.RESPONSE_TO_WORKFLOW_CALL)) {
                  return blankToNull(MapUtils.getString(workflowTask.getParameters(), WorkflowConstants.OUTPUT_SCHEMA));
              }
          }

          return null;
      }

      private static boolean isWorkflowOperation(String type, String operation) {
          WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

          return Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
              Objects.equals(workflowNodeType.operation(), operation);
      }

      /**
       * A project deployment workflow resolved to the workflow it runs, its Call-workflow trigger and its schemas.
       */
      @SuppressFBWarnings("EI")
      public record CallableWorkflow(
          String workflowId, String triggerName, String label, @Nullable String description,
          @Nullable String inputSchema, @Nullable String outputSchema, Map<String, ?> deploymentInputs) {
      }

      @SuppressFBWarnings("EI")
      public record BindableWorkflow(
          ProjectDeployment projectDeployment, Project project, String workflowUuid,
          CallableWorkflow callableWorkflow) {
      }
  }
  ```

  `automation-app-service` already depends on `automation-configuration-api` (the SPI's module); no build change.

- [ ] **Step 8: Pass the app's workspace and environment at the three call sites.**

  In `AppWorkflowFacadeImpl.java`:
  - `getBindableWorkflows`: replace `appCallableWorkflowResolver.getBindableWorkflows(app)` with
    `appCallableWorkflowResolver.getBindableWorkflows(getWorkspaceId(app), app.getEnvironment())`, and the
    `getVisibleProjectIds(\n            app, bindableWorkflows…` call with
    `getVisibleProjectIds(\n            getWorkspaceId(app), bindableWorkflows…`.
  - `checkProjectVisible`: replace `getVisibleProjectIds(app, List.of(project.get()))` with
    `getVisibleProjectIds(getWorkspaceId(app), List.of(project.get()))`.
  - Replace the whole `getVisibleProjectIds` method with:
    ```java
        private Set<Long> getVisibleProjectIds(long workspaceId, List<Project> projects) {
            if (projects.isEmpty()) {
                return Set.of();
            }

            return resourceVisibilityResolver.filterVisibleIds(
                PROJECT, workspaceId,
                projects.stream()
                    .map(project -> new VisibilityRecord(project.getId(), project.getVisibility(), project.getCreatedBy()))
                    .distinct()
                    .toList());
        }

        private static long getWorkspaceId(App app) {
            return Objects.requireNonNull(app.getWorkspaceId(), "workspaceId");
        }
    ```
  - `toBindingDTO`: replace `appCallableWorkflowResolver.resolve(\n            app, binding.getProjectDeploymentId(), …`
    with `appCallableWorkflowResolver.resolve(\n            getWorkspaceId(app), app.getEnvironment(), binding.getProjectDeploymentId(), …`.

  In `AppWorkflowExecutionFacadeImpl.java`, at both `appCallableWorkflowResolver.resolve(` call sites (in
  `executeWorkflow` and `getWorkflows`) replace the first argument `app,` with
  `Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"), app.getEnvironment(),`.

  In `AppWorkflowServiceImpl.java` add `import java.util.Objects;` and replace
  `appCallableWorkflowResolver.resolve(app, projectDeploymentId, workflowUuid)` with
  `appCallableWorkflowResolver.resolve(\n            Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"), app.getEnvironment(), projectDeploymentId,\n            workflowUuid)`.

  Update the three test classes, whose apps are all workspace `7`, `PRODUCTION`:

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416/server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app
  sed -i '' -e 's/appCallableWorkflowResolver\.resolve(app, /appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, /' \
    facade/AppWorkflowFacadeTest.java facade/AppWorkflowExecutionFacadeTest.java service/AppWorkflowServiceTest.java
  sed -i '' -e 's/appCallableWorkflowResolver\.getBindableWorkflows(app)/appCallableWorkflowResolver.getBindableWorkflows(7L, Environment.PRODUCTION)/' \
    facade/AppWorkflowFacadeTest.java
  grep -n "resolve(app\|getBindableWorkflows(app)" facade/AppWorkflowFacadeTest.java facade/AppWorkflowExecutionFacadeTest.java service/AppWorkflowServiceTest.java
  ```
  Expected: the final `grep` prints nothing. All three files already import `Environment`.

- [ ] **Step 9: Run the module's tests and integration tests**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:test > <scratchpad>/d1-t1-test.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.
  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:testIntegration > <scratchpad>/d1-t1-int.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0` (needs Docker; `AppWorkflowExecutionFacadeIntTest` builds the real resolver).

- [ ] **Step 10: Format and check both modules**

  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:spotlessApply :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply > <scratchpad>/d1-t1-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:libs:automation:automation-configuration:automation-configuration-api:check :server:ee:libs:automation:automation-app:automation-app-service:check > <scratchpad>/d1-t1-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t1-check.log`.
  Expected: `exit=0`, no FAILED line. `git status --short` must list only this task's files.

- [ ] **Step 11: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/workflow/CallableWorkflowChecker.java \
    server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/workflow/CallableWorkflowCheck.java \
    server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/SystemProjects.java \
    server/libs/automation/automation-configuration/automation-configuration-api/src/test/java/com/bytechef/automation/configuration/domain/SystemProjectsTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolver.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacadeImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/service/AppWorkflowServiceImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/workflow/AppCallableWorkflowResolverTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowExecutionFacadeTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/service/AppWorkflowServiceTest.java
  git commit -m "--- Add the callable workflow check SPI and key the app workflow resolver by workspace and environment"
  ```

---
### Task 2: Move the slot walk onto `ComponentConnectionFacade.getWorkflowComponentConnections`

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/facade/ComponentConnectionFacade.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/facade/ComponentConnectionFacadeImpl.java`
- Create: `server/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/platform/configuration/facade/ComponentConnectionFacadeWorkflowComponentConnectionsTest.java`
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-remote-client/src/main/java/com/bytechef/ee/platform/configuration/remote/client/facade/RemoteComponentConnectionFacadeClient.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowConnectionResolver.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserReferenceAttentionResolver.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowConnectionResolverTest.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserReferenceAttentionResolverTest.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/AutomationCodeWorkflowBridgeIntTest.java` (Javadoc mention only)
- Delete: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/WorkflowConnectionSlots.java`
- Delete: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/WorkflowConnectionSlotsTest.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `List<ComponentConnection> ComponentConnectionFacade.getWorkflowComponentConnections(String workflowId)` —
  every connection slot of a workflow (triggers, every task including nested ones, cluster elements through the
  cluster-root connection factory). Ungated, like `getComponentConnections(WorkflowTask)`: it is an internal
  composition read, called with an already-authorized workflow id. The remote client throws
  `UnsupportedOperationException`.

- [ ] **Step 1: Write the failing test** (the moved `WorkflowConnectionSlotsTest`). Create
  `ComponentConnectionFacadeWorkflowComponentConnectionsTest.java` with the Apache header of
  `ComponentConnectionFacadeTest.java` (lines 1–15) and:

  ```java
  package com.bytechef.platform.configuration.facade;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.tuple;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.doAnswer;
  import static org.mockito.Mockito.spy;
  import static org.mockito.Mockito.when;

  import com.bytechef.atlas.configuration.domain.Workflow;
  import com.bytechef.atlas.configuration.domain.WorkflowTask;
  import com.bytechef.atlas.configuration.service.WorkflowService;
  import com.bytechef.platform.component.service.ClusterElementDefinitionService;
  import com.bytechef.platform.component.service.ComponentDefinitionService;
  import com.bytechef.platform.configuration.domain.ComponentConnection;
  import com.bytechef.platform.configuration.domain.WorkflowTrigger;
  import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
  import com.bytechef.test.extension.ObjectMapperSetupExtension;
  import java.util.List;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;

  /**
   * The slot walk moved here from the embedded {@code WorkflowConnectionSlots}: triggers first, then every task
   * including those nested inside task dispatchers.
   *
   * @author Ivica Cardic
   */
  @ExtendWith({
      MockitoExtension.class, ObjectMapperSetupExtension.class
  })
  class ComponentConnectionFacadeWorkflowComponentConnectionsTest {

      private static final String DEFINITION = """
          {
            "label": "Nested",
            "triggers": [
              {"name": "newMessage1", "type": "slack/v1/newMessage"}
            ],
            "tasks": [
              {
                "name": "condition1",
                "type": "condition/v1",
                "parameters": {
                  "rawExpression": true,
                  "caseTrue": [
                    {"name": "createIssue1", "type": "jira/v1/createIssue", "parameters": {"summary": "s"}}
                  ],
                  "caseFalse": []
                }
              }
            ]
          }
          """;

      @Mock
      private ClusterElementDefinitionService clusterElementDefinitionService;

      @Mock
      private ComponentDefinitionService componentDefinitionService;

      @Mock
      private ComponentConnectionFactoryResolver componentConnectionFactoryResolver;

      @Mock
      private WorkflowService workflowService;

      @Test
      void testGetWorkflowComponentConnectionsCoversTriggersAndTasksNestedInsideDispatchers() {
          ComponentConnectionFacadeImpl componentConnectionFacade = spy(
              new ComponentConnectionFacadeImpl(
                  clusterElementDefinitionService, List.of(), componentDefinitionService,
                  componentConnectionFactoryResolver, workflowService));

          when(workflowService.getWorkflow("wf-1")).thenReturn(new Workflow("wf-1", DEFINITION, Workflow.Format.JSON));

          doAnswer(invocation -> {
              WorkflowTrigger workflowTrigger = invocation.getArgument(0);

              return List.of(new ComponentConnection("slack", 1, workflowTrigger.getName(), "slack", true));
          }).when(componentConnectionFacade)
              .getComponentConnections(any(WorkflowTrigger.class));

          doAnswer(invocation -> {
              WorkflowTask workflowTask = invocation.getArgument(0);

              return "createIssue1".equals(workflowTask.getName())
                  ? List.of(new ComponentConnection("jira", 1, "createIssue1", "jira", true))
                  : List.of();
          }).when(componentConnectionFacade)
              .getComponentConnections(any(WorkflowTask.class));

          assertThat(componentConnectionFacade.getWorkflowComponentConnections("wf-1"))
              .extracting(ComponentConnection::componentName, ComponentConnection::workflowNodeName)
              .containsExactly(tuple("slack", "newMessage1"), tuple("jira", "createIssue1"));
      }
  }
  ```

- [ ] **Step 2: Run it to see it fail**

  Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:compileTestJava > <scratchpad>/d1-t2-red.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; `cannot find symbol` for `getWorkflowComponentConnections`.

- [ ] **Step 3: Add the method.**

  In `ComponentConnectionFacade.java`, after `getComponentConnections(WorkflowTrigger workflowTrigger);`:

  ```java
      /**
       * Every connection slot of a workflow: its triggers, every task including those nested inside task dispatchers,
       * and the cluster elements the cluster-root connection factory adds. The one enumeration deployments, the embedded
       * connected-user resolvers and the app loop's {@code deployProject} share.
       */
      List<ComponentConnection> getWorkflowComponentConnections(String workflowId);
  ```

  In `ComponentConnectionFacadeImpl.java` add `import com.bytechef.commons.util.CollectionUtils;` and, after
  `getComponentConnections(WorkflowTrigger workflowTrigger)`:

  ```java
      @Override
      public List<ComponentConnection> getWorkflowComponentConnections(String workflowId) {
          Workflow workflow = workflowService.getWorkflow(workflowId);

          return CollectionUtils.concat(
              WorkflowTrigger.of(workflow)
                  .stream()
                  .flatMap(workflowTrigger -> CollectionUtils.stream(getComponentConnections(workflowTrigger)))
                  .toList(),
              workflow.getTasks(true)
                  .stream()
                  .flatMap(workflowTask -> CollectionUtils.stream(getComponentConnections(workflowTask)))
                  .toList());
      }
  ```

  `platform-configuration-service` already uses `commons-util` (check with
  `grep -n commons-util server/libs/platform/platform-configuration/platform-configuration-service/build.gradle.kts`;
  it prints the `implementation(project(":server:libs:core:commons:commons-util"))` line).

  In `RemoteComponentConnectionFacadeClient.java`, after `getComponentConnections(WorkflowTrigger workflowTrigger)`:

  ```java
      @Override
      public List<ComponentConnection> getWorkflowComponentConnections(String workflowId) {
          throw new UnsupportedOperationException();
      }
  ```

- [ ] **Step 4: Run the test to see it pass**

  Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests "com.bytechef.platform.configuration.facade.ComponentConnectionFacadeWorkflowComponentConnectionsTest" > <scratchpad>/d1-t2-green.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.

- [ ] **Step 5: Switch the embedded resolvers and delete `WorkflowConnectionSlots`.**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416/server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src
  sed -i '' -e 's/WorkflowConnectionSlots workflowConnectionSlots/ComponentConnectionFacade componentConnectionFacade/g' \
    -e 's/this\.workflowConnectionSlots = workflowConnectionSlots;/this.componentConnectionFacade = componentConnectionFacade;/' \
    -e 's/workflowConnectionSlots\.getSlots(/componentConnectionFacade.getWorkflowComponentConnections(/g' \
    -e 's/, workflowConnectionSlots)/, componentConnectionFacade)/' \
    -e 's/Slots are enumerated by {@link WorkflowConnectionSlots}/Slots are enumerated by {@link ComponentConnectionFacade#getWorkflowComponentConnections(String)}/' \
    main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowConnectionResolver.java \
    main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserReferenceAttentionResolver.java \
    test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowConnectionResolverTest.java \
    test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserReferenceAttentionResolverTest.java
  sed -i '' -e 's/{@code WorkflowConnectionSlots}/{@code ComponentConnectionFacade.getWorkflowComponentConnections}/' \
    test/java/com/bytechef/ee/embedded/configuration/AutomationCodeWorkflowBridgeIntTest.java
  git rm -q main/java/com/bytechef/ee/embedded/configuration/facade/WorkflowConnectionSlots.java \
    test/java/com/bytechef/ee/embedded/configuration/facade/WorkflowConnectionSlotsTest.java
  grep -rn "WorkflowConnectionSlots\|workflowConnectionSlots" main test
  ```
  Expected: the final `grep` prints nothing.

  Add `import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;` to the four files the first `sed`
  touched (the two resolvers and their two tests); `WorkflowConnectionSlots` lived in the same package, so there was no
  import to remove. Spotless orders imports in Step 7.

- [ ] **Step 6: Run the affected tests**

  Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.facade.ConnectedUser*" > <scratchpad>/d1-t2-ecs.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.
  Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "com.bytechef.ee.embedded.configuration.AutomationCodeWorkflowBridgeIntTest" > <scratchpad>/d1-t2-ecs-int.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0` (the context now wires the resolvers to the facade bean the slots class used to wrap).

- [ ] **Step 7: Format and check every touched module**

  Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:spotlessApply :server:libs:platform:platform-configuration:platform-configuration-service:spotlessApply :server:ee:libs:platform:platform-configuration:platform-configuration-remote-client:spotlessApply :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:spotlessApply > <scratchpad>/d1-t2-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:libs:platform:platform-configuration:platform-configuration-api:check :server:libs:platform:platform-configuration:platform-configuration-service:check :server:ee:libs:platform:platform-configuration:platform-configuration-remote-client:check :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check > <scratchpad>/d1-t2-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t2-check.log`.
  Expected: `exit=0`, no FAILED line. If the check runs past 10 minutes, run it once per module.

- [ ] **Step 8: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/facade/ComponentConnectionFacade.java \
    server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/facade/ComponentConnectionFacadeImpl.java \
    server/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/platform/configuration/facade/ComponentConnectionFacadeWorkflowComponentConnectionsTest.java \
    server/ee/libs/platform/platform-configuration/platform-configuration-remote-client/src/main/java/com/bytechef/ee/platform/configuration/remote/client/facade/RemoteComponentConnectionFacadeClient.java \
    server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowConnectionResolver.java \
    server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserReferenceAttentionResolver.java \
    server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowConnectionResolverTest.java \
    server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserReferenceAttentionResolverTest.java \
    server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/AutomationCodeWorkflowBridgeIntTest.java \
    server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/WorkflowConnectionSlots.java \
    server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/WorkflowConnectionSlotsTest.java
  git commit -m "- Move the workflow connection slot walk onto ComponentConnectionFacade"
  ```

---

### Task 3: `ProjectDeploymentFacade.deployProject`

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/dto/DeployProjectResultDTO.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/dto/DeploymentConnectionChoiceDTO.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/exception/ProjectDeploymentErrorType.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacade.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeImpl.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeDeployProjectIntTest.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeAuthorizationTest.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeWorkflowBindingTest.java` (constructor call)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeChatWorkflowTest.java` (constructor call)
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ProjectDeploymentCrossEnvironmentReadReproductionIntTest.java` (constructor call)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/security/VisibilityBearingSurfaceAuditTest.java` (classify the two DTOs)

**Interfaces:**
- Consumes: `SystemProjects.isSystemDeploymentName` (Task 1), `ComponentConnectionFacade.getWorkflowComponentConnections`
  (Task 2).
- Produces (CE `com.bytechef.automation.configuration`):
  ```java
  DeployProjectResultDTO ProjectDeploymentFacade.deployProject(
      long projectId, @Nullable Long projectDeploymentId, List<String> enabledWorkflowUuids,
      List<DeploymentConnectionChoiceDTO> connections, Map<String, Map<String, ?>> inputs);

  record DeploymentConnectionChoiceDTO(String workflowUuid, String workflowNodeName, String key, long connectionId)

  record DeployProjectResultDTO(
      @Nullable Long projectDeploymentId, int projectVersion, List<DeployedWorkflow> workflows,
      List<RemovedWorkflow> removedWorkflows, List<UnresolvedConnection> unresolvedConnections,
      List<MissingInput> missingInputs) {
      boolean written();   // unresolvedConnections and missingInputs both empty
      record DeployedWorkflow(String workflowUuid, String workflowId, String name, boolean enabled,
          @Nullable String draftWorkflowId, List<DeployedConnection> connections)
      record DeployedConnection(String workflowNodeName, String key, long connectionId, String connectionName)
      record RemovedWorkflow(String workflowUuid, String name)
      record UnresolvedConnection(String workflowUuid, String workflowName, String workflowNodeName, String key,
          String componentName, boolean required, List<Candidate> candidates)
      record Candidate(long id, String name)
      record MissingInput(String workflowUuid, String name)
  }
  ```
  `DeployedWorkflow.workflowId` is the deployed (published) version's id, for the callable check only.
- Refusals throw `ConfigurationException` with `ProjectDeploymentErrorType.UNPUBLISHED_CHANGES` (103),
  `INVALID_DEPLOYMENT` (104) or `INVALID_CONNECTION_CHOICE` (105); the unpublished-changes message ends with
  `call publishProject first`.

- [ ] **Step 1: Write the failing guard test.** In `ProjectDeploymentFacadeAuthorizationTest` add `import java.util.Map;`
  and:

  ```java
      @Test
      void testDeployProjectRequiresWorkflowEditInDevelopmentAndDeploymentEditWhenUpdating() {
          assertExpression(
              "hasResourceScopeInEnvironment(#projectId, 'Project', 'WORKFLOW_EDIT', " +
                  "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT) and " +
                  "(#projectDeploymentId == null or hasPermission(#projectDeploymentId, 'ProjectDeployment', " +
                  "'DEPLOYMENT_EDIT'))",
              "deployProject", long.class, Long.class, List.class, List.class, Map.class);
      }
  ```

- [ ] **Step 2: Write the failing IntTest.** Create `ProjectDeploymentFacadeDeployProjectIntTest.java` (Apache header of
  `ProjectDeploymentFacadeIntTest.java`, lines 1–15):

  ```java
  package com.bytechef.automation.configuration.facade;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.assertj.core.api.Assertions.tuple;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.anyLong;
  import static org.mockito.ArgumentMatchers.anyString;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.ArgumentMatchers.isNull;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.when;

  import com.bytechef.atlas.configuration.service.WorkflowService;
  import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
  import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
  import com.bytechef.automation.configuration.domain.Project;
  import com.bytechef.automation.configuration.domain.ProjectDeployment;
  import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
  import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
  import com.bytechef.automation.configuration.domain.ProjectVersion;
  import com.bytechef.automation.configuration.domain.ProjectWorkflow;
  import com.bytechef.automation.configuration.domain.SystemProjects;
  import com.bytechef.automation.configuration.domain.Workspace;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedConnection;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedWorkflow;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.MissingInput;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.RemovedWorkflow;
  import com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO;
  import com.bytechef.automation.configuration.dto.ProjectDTO;
  import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
  import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
  import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
  import com.bytechef.automation.configuration.repository.ProjectRepository;
  import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
  import com.bytechef.automation.configuration.repository.WorkspaceRepository;
  import com.bytechef.automation.configuration.service.PermissionService;
  import com.bytechef.automation.configuration.service.ProjectService;
  import com.bytechef.automation.configuration.service.ProjectWorkflowService;
  import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
  import com.bytechef.exception.ConfigurationException;
  import com.bytechef.platform.category.repository.CategoryRepository;
  import com.bytechef.platform.component.domain.ConnectionDefinition;
  import com.bytechef.platform.component.service.ConnectionDefinitionService;
  import com.bytechef.platform.configuration.domain.ComponentConnection;
  import com.bytechef.platform.configuration.domain.Environment;
  import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
  import com.bytechef.platform.connection.domain.Connection;
  import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
  import com.bytechef.platform.connection.domain.ConnectionStatus;
  import com.bytechef.platform.connection.dto.ConnectionDTO;
  import com.bytechef.platform.connection.service.ConnectionService;
  import com.bytechef.platform.security.domain.ResourceVisibility;
  import com.bytechef.platform.tag.repository.TagRepository;
  import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
  import java.util.List;
  import java.util.Map;
  import java.util.Objects;
  import java.util.UUID;
  import org.junit.jupiter.api.AfterEach;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.context.annotation.Import;
  import org.springframework.test.context.bean.override.mockito.MockitoBean;

  /**
   * The deploy rules of the Apps agent loop against real project, version and deployment rows. Connection candidates
   * come from a mocked {@link WorkspaceConnectionFacade}: what it returns stands for what the caller can see, so a
   * PRIVATE connection of someone else is simply absent from its answer. Method security is not active in this
   * context; the guard is pinned by {@code ProjectDeploymentFacadeAuthorizationTest}.
   *
   * @author Ivica Cardic
   */
  @SpringBootTest(
      classes = ProjectIntTestConfiguration.class,
      properties = {
          "bytechef.workflow.repository.jdbc.enabled=true"
      })
  @Import(PostgreSQLContainerConfiguration.class)
  @ProjectIntTestConfigurationSharedMocks
  public class ProjectDeploymentFacadeDeployProjectIntTest {

      private static final String SLACK = "slack";
      private static final String SLACK_NODE = "sendMessage_1";
      private static final ComponentConnection SLACK_SLOT = new ComponentConnection(SLACK, 1, SLACK_NODE, SLACK, true);
      private static final String WORKFLOW_A = """
          {"label": "Sync A", "triggers": [], "tasks": []}""";
      private static final String WORKFLOW_B = """
          {"label": "Sync B", "triggers": [], "tasks": []}""";
      private static final String WORKFLOW_WITH_INPUT = """
          {"label": "Sync A", "inputs": [{"name": "region", "label": "Region", "type": "string", "required": true}],
           "triggers": [], "tasks": []}""";

      @Autowired
      private CategoryRepository categoryRepository;

      @Autowired
      private ComponentConnectionFacade componentConnectionFacade;

      @MockitoBean
      private ConnectionDefinitionService connectionDefinitionService;

      @Autowired
      private ConnectionService connectionService;

      @MockitoBean
      private PermissionService permissionService;

      @Autowired
      private ProjectDeploymentFacade projectDeploymentFacade;

      @Autowired
      private ProjectDeploymentRepository projectDeploymentRepository;

      @Autowired
      private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

      @Autowired
      private ProjectFacade projectFacade;

      @Autowired
      private ProjectRepository projectRepository;

      @Autowired
      private ProjectService projectService;

      @Autowired
      private ProjectWorkflowFacade projectWorkflowFacade;

      @Autowired
      private ProjectWorkflowRepository projectWorkflowRepository;

      @Autowired
      private ProjectWorkflowService projectWorkflowService;

      @Autowired
      private TagRepository tagRepository;

      @MockitoBean
      private WorkspaceConnectionFacade workspaceConnectionFacade;

      @Autowired
      private WorkflowService workflowService;

      @Autowired
      private WorkspaceRepository workspaceRepository;

      private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;
      private Workspace workspace;

      @AfterEach
      public void afterEach() {
          projectDeploymentWorkflowRepository.deleteAll();
          projectWorkflowRepository.deleteAll();
          projectDeploymentRepository.deleteAll();
          projectRepository.deleteAll();
          workspaceRepository.deleteAll();

          categoryRepository.deleteAll();
          tagRepository.deleteAll();
      }

      @BeforeEach
      void beforeEach() {
          workspace = workspaceRepository.save(new Workspace("test"));

          projectDeploymentFacadeHelper = new ProjectDeploymentFacadeHelper(
              categoryRepository, projectFacade, projectRepository, projectDeploymentFacade, projectWorkflowFacade,
              projectWorkflowRepository);

          ConnectionDefinition connectionDefinition = mock(ConnectionDefinition.class);

          when(connectionDefinition.getVersion()).thenReturn(1);
          when(connectionDefinitionService.getConnectionDefinition(SLACK, 1)).thenReturn(connectionDefinition);
          when(componentConnectionFacade.getWorkflowComponentConnections(anyString())).thenReturn(List.of(SLACK_SLOT));
          when(componentConnectionFacade.getComponentConnection(anyString(), eq(SLACK_NODE), eq(SLACK)))
              .thenReturn(SLACK_SLOT);
          when(connectionService.getConnection(anyLong()))
              .thenAnswer(invocation -> connection(invocation.getArgument(0)));
          when(permissionService.hasResourceScope(any(), eq("Connection"), eq("CONNECTION_USE"))).thenReturn(true);

          stubCandidates(connectionDTO(101L, "Slack A"));
      }

      @Test
      public void testCreatesTheDeploymentWithARowForEveryWorkflowAndPicksTheOnlyCandidate() {
          long projectId = createPublishedProject(WORKFLOW_A, WORKFLOW_B);
          String workflowAUuid = uuidOf(projectId, "Sync A");
          String workflowBUuid = uuidOf(projectId, "Sync B");

          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowAUuid), List.of(), Map.of());

          assertThat(result.written()).isTrue();

          long projectDeploymentId = Objects.requireNonNull(result.projectDeploymentId());

          ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
              .orElseThrow();

          assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
          assertThat(projectDeployment.isEnabled()).isFalse();
          assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
              .extracting(ProjectDeploymentWorkflow::isEnabled, ProjectDeploymentWorkflow::getConnectionsCount)
              .containsExactlyInAnyOrder(tuple(true, 1), tuple(false, 1));
          assertThat(result.workflows())
              .extracting(DeployedWorkflow::workflowUuid, DeployedWorkflow::enabled)
              .containsExactlyInAnyOrder(tuple(workflowAUuid, true), tuple(workflowBUuid, false));
          assertThat(result.workflows())
              .allSatisfy(deployedWorkflow -> {
                  assertThat(deployedWorkflow.draftWorkflowId()).isNotEqualTo(deployedWorkflow.workflowId());
                  assertThat(deployedWorkflow.connections())
                      .extracting(DeployedConnection::connectionId, DeployedConnection::connectionName)
                      .containsExactly(tuple(101L, "Slack A"));
              });
      }

      @Test
      public void testRedeployAfterPublishUpdatesTheSameDeploymentSoItsBindingsStayValid() {
          long projectId = createPublishedProject(WORKFLOW_A);
          String workflowUuid = uuidOf(projectId, "Sync A");

          long projectDeploymentId = deploy(projectId, null, List.of(workflowUuid));

          updateDraft(workflowUuid, """
              {"label": "Sync A", "description": "changed", "triggers": [], "tasks": []}""");

          projectFacade.publishProject(projectId, "Second", false);

          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, projectDeploymentId, List.of(), List.of(), Map.of());

          String publishedWorkflowId = publishedWorkflowId(projectId, workflowUuid);

          assertThat(result.projectDeploymentId()).isEqualTo(projectDeploymentId);
          assertThat(result.removedWorkflows()).isEmpty();
          assertThat(projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid))
              .contains(publishedWorkflowId);
          assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
              .singleElement()
              .satisfies(projectDeploymentWorkflow -> {
                  assertThat(projectDeploymentWorkflow.getWorkflowId()).isEqualTo(publishedWorkflowId);
                  assertThat(projectDeploymentWorkflow.isEnabled()).isTrue();
              });
      }

      @Test
      public void testSubsetOfEnabledWorkflowsKeepsEveryOtherRowAndItsEnabledState() {
          long projectId = createPublishedProject(WORKFLOW_A, WORKFLOW_B);
          String workflowAUuid = uuidOf(projectId, "Sync A");
          String workflowBUuid = uuidOf(projectId, "Sync B");

          long projectDeploymentId = deploy(projectId, null, List.of(workflowAUuid, workflowBUuid));

          deploy(projectId, projectDeploymentId, List.of(workflowAUuid));

          assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
              .extracting(ProjectDeploymentWorkflow::isEnabled)
              .containsExactly(true, true);
      }

      @Test
      public void testExplicitConnectionWithoutConnectionUseIsRefused() {
          long projectId = createPublishedProject(WORKFLOW_A);
          String workflowUuid = uuidOf(projectId, "Sync A");

          stubCandidates(connectionDTO(101L, "Slack A"), connectionDTO(102L, "Slack B"));

          when(permissionService.hasResourceScope(eq(102L), eq("Connection"), eq("CONNECTION_USE"))).thenReturn(false);

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowUuid),
              List.of(new DeploymentConnectionChoiceDTO(workflowUuid, SLACK_NODE, SLACK, 102L)), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("Connection 102 cannot be used");

          assertThat(projectDeploymentRepository.count()).isZero();
      }

      @Test
      public void testPrivateConnectionInvisibleToTheCallerIsNeitherACandidateNorAccepted() {
          long projectId = createPublishedProject(WORKFLOW_A);
          String workflowUuid = uuidOf(projectId, "Sync A");

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowUuid),
              List.of(new DeploymentConnectionChoiceDTO(workflowUuid, SLACK_NODE, SLACK, 103L)), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("Connection 103 cannot be used");

          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowUuid), List.of(), Map.of());

          assertThat(result.workflows()
              .getFirst()
              .connections())
                  .extracting(DeployedConnection::connectionId)
                  .containsExactly(101L);
      }

      @Test
      public void testDeploymentOfProductionIsRefused() {
          long projectId = createPublishedProject(WORKFLOW_A);
          long productionDeploymentId = saveDeployment(projectId, "Orders", Environment.PRODUCTION);

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, productionDeploymentId, List.of(), List.of(), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("is not a DEVELOPMENT deployment of project");
      }

      @Test
      public void testDeploymentOfAnotherProjectIsRefused() {
          long projectId = createPublishedProject(WORKFLOW_A);
          long otherProjectId = createPublishedProject(WORKFLOW_B);
          long otherDeploymentId = saveDeployment(otherProjectId, "Other", Environment.DEVELOPMENT);

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, otherDeploymentId, List.of(), List.of(), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("is not a DEVELOPMENT deployment of project");
      }

      @Test
      public void testCreateWhenTheProjectAlreadyHasADevelopmentDeploymentIsRefused() {
          long projectId = createPublishedProject(WORKFLOW_A);

          saveDeployment(projectId, "Orders", Environment.DEVELOPMENT);

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, null, List.of(), List.of(), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("pass its projectDeploymentId");
      }

      @Test
      public void testSystemDeploymentOfTheProjectIsIgnored() {
          long projectId = createPublishedProject(WORKFLOW_A);
          long mcpServerDeploymentId = saveDeployment(
              projectId, SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX + "orders", Environment.DEVELOPMENT);

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, mcpServerDeploymentId, List.of(), List.of(), Map.of()))
                  .isInstanceOf(ConfigurationException.class);

          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, null, List.of(), List.of(), Map.of());

          assertThat(result.projectDeploymentId()).isNotEqualTo(mcpServerDeploymentId);
          assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(mcpServerDeploymentId)).isEmpty();
      }

      @Test
      public void testRemovedWorkflowsAreReported() {
          long projectId = createPublishedProject(WORKFLOW_A, WORKFLOW_B);
          String workflowAUuid = uuidOf(projectId, "Sync A");
          String workflowBUuid = uuidOf(projectId, "Sync B");

          long projectDeploymentId = deploy(projectId, null, List.of(workflowAUuid, workflowBUuid));

          projectWorkflowFacade.deleteWorkflow(projectWorkflowService.getLastWorkflowId(workflowBUuid));
          projectFacade.publishProject(projectId, "Without B", false);

          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, projectDeploymentId, List.of(), List.of(), Map.of());

          assertThat(result.removedWorkflows()).containsExactly(new RemovedWorkflow(workflowBUuid, "Sync B"));
          assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId)).hasSize(1);
      }

      @Test
      public void testAmbiguousRequiredSlotOfAnEnabledWorkflowWritesNothing() {
          long projectId = createPublishedProject(WORKFLOW_A);
          String workflowUuid = uuidOf(projectId, "Sync A");

          stubCandidates(connectionDTO(101L, "Slack A"), connectionDTO(102L, "Slack B"));

          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowUuid), List.of(), Map.of());

          assertThat(result.written()).isFalse();
          assertThat(result.projectDeploymentId()).isNull();
          assertThat(result.unresolvedConnections())
              .singleElement()
              .satisfies(unresolvedConnection -> {
                  assertThat(unresolvedConnection.workflowUuid()).isEqualTo(workflowUuid);
                  assertThat(unresolvedConnection.workflowNodeName()).isEqualTo(SLACK_NODE);
                  assertThat(unresolvedConnection.required()).isTrue();
                  assertThat(unresolvedConnection.candidates())
                      .extracting(DeployProjectResultDTO.Candidate::id)
                      .containsExactly(101L, 102L);
              });
          assertThat(projectDeploymentRepository.count()).isZero();
      }

      @Test
      public void testDisabledWorkflowWithAnAmbiguousSlotDoesNotBlock() {
          long projectId = createPublishedProject(WORKFLOW_A);

          stubCandidates(connectionDTO(101L, "Slack A"), connectionDTO(102L, "Slack B"));

          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, null, List.of(), List.of(), Map.of());

          assertThat(result.written()).isTrue();
          assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
              Objects.requireNonNull(result.projectDeploymentId())))
                  .singleElement()
                  .satisfies(projectDeploymentWorkflow -> {
                      assertThat(projectDeploymentWorkflow.isEnabled()).isFalse();
                      assertThat(projectDeploymentWorkflow.getConnectionsCount()).isZero();
                  });
      }

      @Test
      public void testMissingRequiredInputWritesNothingAndAGivenOneIsStored() {
          long projectId = createPublishedProject(WORKFLOW_WITH_INPUT);
          String workflowUuid = uuidOf(projectId, "Sync A");

          DeployProjectResultDTO missing = projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowUuid), List.of(), Map.of());

          assertThat(missing.written()).isFalse();
          assertThat(missing.missingInputs()).containsExactly(new MissingInput(workflowUuid, "region"));
          assertThat(projectDeploymentRepository.count()).isZero();

          DeployProjectResultDTO written = projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowUuid), List.of(), Map.of(workflowUuid, Map.of("region", "eu")));

          assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
              Objects.requireNonNull(written.projectDeploymentId())))
                  .singleElement()
                  .satisfies(projectDeploymentWorkflow -> assertThat(projectDeploymentWorkflow.getInputs())
                      .containsEntry("region", "eu"));
      }

      @Test
      public void testNeverPublishedProjectIsRefused() {
          ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(Objects.requireNonNull(workspace.getId()));

          projectDeploymentFacadeHelper.addTestWorkflow(projectDTO, WORKFLOW_A);

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              Objects.requireNonNull(projectDTO.id()), null, List.of(), List.of(), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("call publishProject first");
      }

      @Test
      public void testChangedDefinitionIsAnUnpublishedChange() {
          long projectId = createPublishedProject(WORKFLOW_A);

          updateDraft(uuidOf(projectId, "Sync A"), """
              {"label": "Sync A", "description": "changed", "triggers": [], "tasks": []}""");

          assertUnpublishedChanges(projectId);
      }

      @Test
      public void testAddedWorkflowIsAnUnpublishedChange() {
          long projectId = createPublishedProject(WORKFLOW_A);

          projectWorkflowFacade.addWorkflow(projectId, WORKFLOW_B);

          assertUnpublishedChanges(projectId);
      }

      @Test
      public void testRemovedWorkflowIsAnUnpublishedChange() {
          long projectId = createPublishedProject(WORKFLOW_A, WORKFLOW_B);

          projectWorkflowFacade.deleteWorkflow(projectWorkflowService.getLastWorkflowId(uuidOf(projectId, "Sync B")));

          assertUnpublishedChanges(projectId);
      }

      @Test
      public void testRedeployOfTheSameVersionUpdatesRowsInPlace() {
          long projectId = createPublishedProject(WORKFLOW_A);
          String workflowUuid = uuidOf(projectId, "Sync A");

          long projectDeploymentId = deploy(projectId, null, List.of(workflowUuid));

          stubCandidates(connectionDTO(101L, "Slack A"), connectionDTO(102L, "Slack B"));

          deploy(projectId, projectDeploymentId, List.of());

          assertThat(connectionIdsOf(projectDeploymentId)).containsExactly(101L);

          projectDeploymentFacade.deployProject(
              projectId, projectDeploymentId, List.of(),
              List.of(new DeploymentConnectionChoiceDTO(workflowUuid, SLACK_NODE, SLACK, 102L)), Map.of());

          assertThat(connectionIdsOf(projectDeploymentId)).containsExactly(102L);
      }

      @Test
      public void testUnknownWorkflowUuidIsRefusedNamingTheVersionsUuids() {
          long projectId = createPublishedProject(WORKFLOW_A);
          String workflowUuid = uuidOf(projectId, "Sync A");
          String publishedWorkflowId = publishedWorkflowId(projectId, workflowUuid);

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, null, List.of(publishedWorkflowId), List.of(), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining(workflowUuid);

          assertThat(projectDeploymentRepository.count()).isZero();
      }

      @Test
      public void testConnectionForAnUnknownSlotIsRefused() {
          long projectId = createPublishedProject(WORKFLOW_A);
          String workflowUuid = uuidOf(projectId, "Sync A");

          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, null, List.of(workflowUuid),
              List.of(new DeploymentConnectionChoiceDTO(workflowUuid, "missing_1", SLACK, 101L)), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("has no connection slot missing_1/slack");

          assertThat(projectDeploymentRepository.count()).isZero();
      }

      private void assertUnpublishedChanges(long projectId) {
          assertThatThrownBy(() -> projectDeploymentFacade.deployProject(
              projectId, null, List.of(), List.of(), Map.of()))
                  .isInstanceOf(ConfigurationException.class)
                  .hasMessageContaining("has unpublished changes; call publishProject first");

          assertThat(projectDeploymentRepository.count()).isZero();
      }

      private List<Long> connectionIdsOf(long projectDeploymentId) {
          return projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId)
              .stream()
              .flatMap(projectDeploymentWorkflow -> projectDeploymentWorkflow.getConnections()
                  .stream())
              .map(ProjectDeploymentWorkflowConnection::getConnectionId)
              .toList();
      }

      private long createPublishedProject(String... workflowDefinitions) {
          ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(Objects.requireNonNull(workspace.getId()));

          for (String workflowDefinition : workflowDefinitions) {
              projectDeploymentFacadeHelper.addTestWorkflow(projectDTO, workflowDefinition);
          }

          long projectId = Objects.requireNonNull(projectDTO.id());

          projectFacade.publishProject(projectId, "Published for test", false);

          return projectId;
      }

      private long deploy(long projectId, Long projectDeploymentId, List<String> enabledWorkflowUuids) {
          DeployProjectResultDTO result = projectDeploymentFacade.deployProject(
              projectId, projectDeploymentId, enabledWorkflowUuids, List.of(), Map.of());

          assertThat(result.written()).isTrue();

          return Objects.requireNonNull(result.projectDeploymentId());
      }

      private List<ProjectWorkflow> publishedProjectWorkflows(long projectId) {
          Project project = projectService.getProject(projectId);

          ProjectVersion projectVersion = Objects.requireNonNull(project.getLastPublishedProjectVersion());

          return projectWorkflowService.getProjectWorkflows(projectId, projectVersion.getVersion());
      }

      private String publishedWorkflowId(long projectId, String workflowUuid) {
          return publishedProjectWorkflows(projectId).stream()
              .filter(projectWorkflow -> Objects.equals(projectWorkflow.getUuidAsString(), workflowUuid))
              .map(ProjectWorkflow::getWorkflowId)
              .findFirst()
              .orElseThrow();
      }

      private long saveDeployment(long projectId, String name, Environment environment) {
          ProjectDeployment projectDeployment = new ProjectDeployment();

          projectDeployment.setEnabled(false);
          projectDeployment.setEnvironment(environment);
          projectDeployment.setName(name);
          projectDeployment.setProjectId(projectId);
          projectDeployment.setProjectVersion(
              Objects.requireNonNull(projectService.getProject(projectId)
                  .getLastPublishedProjectVersion())
                  .getVersion());
          projectDeployment.setUuid(UUID.randomUUID());

          ProjectDeployment savedProjectDeployment = projectDeploymentRepository.save(projectDeployment);

          return Objects.requireNonNull(savedProjectDeployment.getId());
      }

      private void stubCandidates(ConnectionDTO... connectionDTOs) {
          when(
              workspaceConnectionFacade.getConnections(
                  eq(Objects.requireNonNull(workspace.getId())), eq(SLACK), eq(1), eq(0L), isNull()))
                      .thenReturn(List.of(connectionDTOs));
      }

      private void updateDraft(String workflowUuid, String definition) {
          String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

          ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.getProjectWorkflow(draftWorkflowId);

          projectWorkflowFacade.updateWorkflow(draftWorkflowId, definition, projectWorkflowDTO.getVersion());
      }

      private String uuidOf(long projectId, String label) {
          return publishedProjectWorkflows(projectId).stream()
              .filter(projectWorkflow -> Objects.equals(
                  workflowService.getWorkflow(projectWorkflow.getWorkflowId())
                      .getLabel(),
                  label))
              .map(ProjectWorkflow::getUuidAsString)
              .findFirst()
              .orElseThrow();
      }

      private static Connection connection(long id) {
          Connection connection = new Connection();

          connection.setComponentName(SLACK);
          connection.setEnvironmentId(Environment.DEVELOPMENT.ordinal());
          connection.setId(id);
          connection.setName("Slack " + id);

          return connection;
      }

      private static ConnectionDTO connectionDTO(long id, String name) {
          return ConnectionDTO.builder()
              .componentName(SLACK)
              .connectionVersion(1)
              .credentialStatus(CredentialStatus.VALID)
              .environmentId(Environment.DEVELOPMENT.ordinal())
              .id(id)
              .name(name)
              .status(ConnectionStatus.ACTIVE)
              .visibility(ResourceVisibility.WORKSPACE)
              .build();
      }
  }
  ```

  `ProjectDeploymentFacadeHelper.addTestWorkflow(ProjectDTO, String)`, `createProject(long)`,
  `ProjectWorkflowFacade.updateWorkflow(String, String, int)`, `deleteWorkflow(String)`, `addWorkflow(long, String)` and
  `ProjectWorkflowService.getLastWorkflowId(String)` exist today.

- [ ] **Step 3: Run the tests to see them fail**

  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileTestJava > <scratchpad>/d1-t3-red.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; `cannot find symbol` for `DeployProjectResultDTO`, `DeploymentConnectionChoiceDTO`,
  `deployProject`.

- [ ] **Step 4: Add the DTOs and error types.**

  `ProjectDeploymentErrorType.java` — after `INVALID_PROJECT_VERSION`:

  ```java
      public static final ProjectDeploymentErrorType UNPUBLISHED_CHANGES = new ProjectDeploymentErrorType(103);
      public static final ProjectDeploymentErrorType INVALID_DEPLOYMENT = new ProjectDeploymentErrorType(104);
      public static final ProjectDeploymentErrorType INVALID_CONNECTION_CHOICE = new ProjectDeploymentErrorType(105);
  ```

  `DeploymentConnectionChoiceDTO.java` (Apache header of `ProjectDeploymentDTO.java`):

  ```java
  package com.bytechef.automation.configuration.dto;

  /**
   * The connection a caller chose for one connection slot of one workflow of a deployment, the workflow named by its
   * uuid.
   *
   * @author Ivica Cardic
   */
  public record DeploymentConnectionChoiceDTO(
      String workflowUuid, String workflowNodeName, String key, long connectionId) {
  }
  ```

  `DeployProjectResultDTO.java` (same header):

  ```java
  package com.bytechef.automation.configuration.dto;

  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.List;
  import org.jspecify.annotations.Nullable;

  /**
   * What {@code ProjectDeploymentFacade.deployProject} did, or why it wrote nothing. {@link #written()} is false when an
   * enabled workflow has an unresolved required connection slot or a missing required input; then
   * {@link #projectDeploymentId()} is the deployment that would have been updated, or null for a create.
   *
   * @author Ivica Cardic
   */
  @SuppressFBWarnings("EI")
  public record DeployProjectResultDTO(
      @Nullable Long projectDeploymentId, int projectVersion, List<DeployedWorkflow> workflows,
      List<RemovedWorkflow> removedWorkflows, List<UnresolvedConnection> unresolvedConnections,
      List<MissingInput> missingInputs) {

      public boolean written() {
          return unresolvedConnections.isEmpty() && missingInputs.isEmpty();
      }

      /**
       * One workflow of the deployed version. {@code workflowId} is that version's workflow id; {@code draftWorkflowId}
       * is the id of the same workflow in the project's DRAFT version, the one to edit next.
       */
      @SuppressFBWarnings("EI")
      public record DeployedWorkflow(
          String workflowUuid, String workflowId, String name, boolean enabled, @Nullable String draftWorkflowId,
          List<DeployedConnection> connections) {
      }

      public record DeployedConnection(String workflowNodeName, String key, long connectionId, String connectionName) {
      }

      public record RemovedWorkflow(String workflowUuid, String name) {
      }

      @SuppressFBWarnings("EI")
      public record UnresolvedConnection(
          String workflowUuid, String workflowName, String workflowNodeName, String key, String componentName,
          boolean required, List<Candidate> candidates) {
      }

      public record Candidate(long id, String name) {
      }

      public record MissingInput(String workflowUuid, String name) {
      }
  }
  ```

  `ProjectDeploymentFacade.java` — add imports `com.bytechef.automation.configuration.dto.DeployProjectResultDTO`,
  `com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO`, `java.util.Map`,
  `org.jspecify.annotations.Nullable`, and after `deleteProjectDeployment(long id);`:

  ```java
      /**
       * Creates or updates the project's single DEVELOPMENT deployment at its latest published version, leaving it
       * disabled; the caller arms it with {@link #enableProjectDeployment(long, boolean)}. Workflows are named by uuid.
       * Every workflow of the version gets a row: listed uuids are enabled, the others keep their current state and a new
       * row starts disabled. Connections and inputs not given keep the current row's value; a slot with exactly one
       * usable candidate gets it. Nothing is written when an enabled workflow has an unresolved required slot or a
       * missing required input; the result lists them.
       */
      DeployProjectResultDTO deployProject(
          long projectId, @Nullable Long projectDeploymentId, List<String> enabledWorkflowUuids,
          List<DeploymentConnectionChoiceDTO> connections, Map<String, Map<String, ?>> inputs);
  ```

- [ ] **Step 5: Implement it in `ProjectDeploymentFacadeImpl.java`.**

  Constructor: append three parameters and fields (keep every existing one):

  ```java
          ComponentConnectionFacade componentConnectionFacade, WorkflowService workflowService,
          @Lazy ConnectionDefinitionService connectionDefinitionService, @Lazy PermissionService permissionService,
          @Lazy WorkspaceConnectionFacade workspaceConnectionFacade) {
  ```
  with fields `private final ConnectionDefinitionService connectionDefinitionService;`,
  `private final PermissionService permissionService;`,
  `private final WorkspaceConnectionFacade workspaceConnectionFacade;` and the three assignments at the end of the
  constructor body.

  Imports to add: `com.bytechef.automation.configuration.domain.ProjectVersion`,
  `com.bytechef.automation.configuration.dto.DeployProjectResultDTO`,
  `com.bytechef.automation.configuration.dto.DeployProjectResultDTO.Candidate`,
  `com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedConnection`,
  `com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedWorkflow`,
  `com.bytechef.automation.configuration.dto.DeployProjectResultDTO.MissingInput`,
  `com.bytechef.automation.configuration.dto.DeployProjectResultDTO.RemovedWorkflow`,
  `com.bytechef.automation.configuration.dto.DeployProjectResultDTO.UnresolvedConnection`,
  `com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO`,
  `com.bytechef.automation.configuration.service.PermissionService`,
  `com.bytechef.platform.component.domain.ConnectionDefinition`,
  `com.bytechef.platform.component.service.ConnectionDefinitionService`,
  `com.bytechef.platform.connection.domain.AiProviderConnectionId`,
  `com.bytechef.platform.connection.domain.Connection.CredentialStatus`,
  `com.bytechef.platform.connection.dto.ConnectionDTO`, `java.util.stream.Stream`,
  `org.jspecify.annotations.Nullable`, `org.springframework.context.annotation.Lazy`.
  (`WorkspaceConnectionFacade` is in the same package.)

  Constants (next to `MANUAL_TRIGGER_NAME`):

  ```java
      private static final String CONNECTION = "Connection";
      private static final String CONNECTION_USE = "CONNECTION_USE";
  ```

  The method, after `deleteProjectDeployment`:

  ```java
      @Override
      @PreAuthorize(
          "hasResourceScopeInEnvironment(#projectId, 'Project', 'WORKFLOW_EDIT', " +
              "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT) and " +
              "(#projectDeploymentId == null or hasPermission(#projectDeploymentId, 'ProjectDeployment', " +
              "'DEPLOYMENT_EDIT'))")
      public DeployProjectResultDTO deployProject(
          long projectId, @Nullable Long projectDeploymentId, List<String> enabledWorkflowUuids,
          List<DeploymentConnectionChoiceDTO> connections, Map<String, Map<String, ?>> inputs) {

          Project project = projectService.getProject(projectId);

          ProjectDeployment projectDeployment = fetchDevelopmentProjectDeployment(projectId, projectDeploymentId);

          int projectVersion = getDeployableProjectVersion(project);

          List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

          checkWorkflowUuids(projectId, projectVersion, projectWorkflows, enabledWorkflowUuids, connections, inputs);

          long workspaceId = Objects.requireNonNull(project.getWorkspaceId(), "workspaceId");
          Map<String, ProjectDeploymentWorkflow> currentProjectDeploymentWorkflows = projectDeployment == null
              ? Map.of() : getProjectDeploymentWorkflowsByUuid(projectDeployment);
          Map<String, String> draftWorkflowIds = getWorkflowIdsByUuid(projectId, project.getLastProjectVersion());
          Map<String, List<ConnectionDTO>> candidateConnections = new HashMap<>();

          List<DeployedWorkflow> deployedWorkflows = new ArrayList<>();
          List<MissingInput> missingInputs = new ArrayList<>();
          List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = new ArrayList<>();
          List<UnresolvedConnection> unresolvedConnections = new ArrayList<>();

          for (ProjectWorkflow projectWorkflow : projectWorkflows) {
              String workflowUuid = projectWorkflow.getUuidAsString();
              Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());
              ProjectDeploymentWorkflow currentProjectDeploymentWorkflow = currentProjectDeploymentWorkflows.get(
                  workflowUuid);

              boolean enabled = enabledWorkflowUuids.contains(workflowUuid) ||
                  (currentProjectDeploymentWorkflow != null && currentProjectDeploymentWorkflow.isEnabled());

              List<DeployedConnection> deployedConnections = new ArrayList<>();
              List<ProjectDeploymentWorkflowConnection> workflowConnections = new ArrayList<>();

              for (ComponentConnection componentConnection : componentConnectionFacade
                  .getWorkflowComponentConnections(workflow.getId())) {

                  List<ConnectionDTO> candidates = candidateConnections.computeIfAbsent(
                      componentConnection.componentName() + ":" + componentConnection.componentVersion(),
                      candidateKey -> getCandidateConnections(workspaceId, componentConnection));

                  ConnectionDTO connection = chooseConnection(
                      workflowUuid, componentConnection, candidates, connections, currentProjectDeploymentWorkflow);

                  if (connection == null) {
                      if (enabled && componentConnection.required()) {
                          unresolvedConnections.add(
                              toUnresolvedConnection(workflowUuid, workflow, componentConnection, candidates));
                      }

                      continue;
                  }

                  long connectionId = Objects.requireNonNull(connection.id(), "id");

                  workflowConnections.add(
                      new ProjectDeploymentWorkflowConnection(
                          connectionId, componentConnection.key(), componentConnection.workflowNodeName()));
                  deployedConnections.add(
                      new DeployedConnection(
                          componentConnection.workflowNodeName(), componentConnection.key(), connectionId,
                          connection.name()));
              }

              Map<String, ?> workflowInputs = resolveInputs(workflowUuid, inputs, currentProjectDeploymentWorkflow);

              if (enabled) {
                  missingInputs.addAll(getMissingInputs(workflowUuid, workflow, workflowInputs));
              }

              projectDeploymentWorkflows.add(
                  toProjectDeploymentWorkflow(workflow.getId(), enabled, workflowInputs, workflowConnections));
              deployedWorkflows.add(
                  new DeployedWorkflow(
                      workflowUuid, workflow.getId(), getWorkflowName(workflow), enabled,
                      draftWorkflowIds.get(workflowUuid), deployedConnections));
          }

          checkConnectionChoicesMatchSlots(connections, deployedWorkflows);

          List<RemovedWorkflow> removedWorkflows = projectDeployment == null
              ? List.of() : getRemovedWorkflows(projectDeployment, projectWorkflows);

          if (!unresolvedConnections.isEmpty() || !missingInputs.isEmpty()) {
              return new DeployProjectResultDTO(
                  projectDeployment == null ? null : projectDeployment.getId(), projectVersion, deployedWorkflows,
                  removedWorkflows, unresolvedConnections, missingInputs);
          }

          long savedProjectDeploymentId = projectDeployment == null
              ? createDevelopmentProjectDeployment(project, projectVersion, projectDeploymentWorkflows)
              : redeployDevelopmentProjectDeployment(projectDeployment, projectVersion, projectDeploymentWorkflows);

          return new DeployProjectResultDTO(
              savedProjectDeploymentId, projectVersion, deployedWorkflows, removedWorkflows, List.of(), List.of());
      }
  ```

  Private helpers (place them with the other private methods, alphabetically is fine):

  ```java
      private void checkConnectionChoicesMatchSlots(
          List<DeploymentConnectionChoiceDTO> connectionChoices, List<DeployedWorkflow> deployedWorkflows) {

          for (DeploymentConnectionChoiceDTO connectionChoice : connectionChoices) {
              boolean matched = deployedWorkflows.stream()
                  .filter(deployedWorkflow -> Objects.equals(
                      deployedWorkflow.workflowUuid(), connectionChoice.workflowUuid()))
                  .flatMap(deployedWorkflow -> deployedWorkflow.connections()
                      .stream())
                  .anyMatch(deployedConnection -> Objects.equals(
                      deployedConnection.workflowNodeName(), connectionChoice.workflowNodeName()) &&
                      Objects.equals(deployedConnection.key(), connectionChoice.key()));

              if (!matched) {
                  throw new ConfigurationException(
                      "Workflow %s has no connection slot %s/%s".formatted(
                          connectionChoice.workflowUuid(), connectionChoice.workflowNodeName(), connectionChoice.key()),
                      ProjectDeploymentErrorType.INVALID_CONNECTION_CHOICE);
              }
          }
      }

      private static void checkWorkflowUuids(
          long projectId, int projectVersion, List<ProjectWorkflow> projectWorkflows, List<String> enabledWorkflowUuids,
          List<DeploymentConnectionChoiceDTO> connectionChoices, Map<String, Map<String, ?>> inputs) {

          Set<String> workflowUuids = projectWorkflows.stream()
              .map(ProjectWorkflow::getUuidAsString)
              .collect(Collectors.toCollection(TreeSet::new));

          List<String> unknownWorkflowUuids = Stream.of(
              enabledWorkflowUuids.stream(),
              connectionChoices.stream()
                  .map(DeploymentConnectionChoiceDTO::workflowUuid),
              inputs.keySet()
                  .stream())
              .flatMap(Function.identity())
              .filter(workflowUuid -> !workflowUuids.contains(workflowUuid))
              .distinct()
              .toList();

          if (!unknownWorkflowUuids.isEmpty()) {
              throw new ConfigurationException(
                  "Workflows %s are not in version %d of project %d; its workflow uuids are %s".formatted(
                      unknownWorkflowUuids, projectVersion, projectId, workflowUuids),
                  ProjectDeploymentErrorType.INVALID_DEPLOYMENT);
          }
      }

      private static @Nullable ConnectionDTO chooseConnection(
          String workflowUuid, ComponentConnection componentConnection, List<ConnectionDTO> candidates,
          List<DeploymentConnectionChoiceDTO> connectionChoices,
          @Nullable ProjectDeploymentWorkflow currentProjectDeploymentWorkflow) {

          Optional<DeploymentConnectionChoiceDTO> connectionChoice = connectionChoices.stream()
              .filter(choice -> Objects.equals(choice.workflowUuid(), workflowUuid) &&
                  Objects.equals(choice.workflowNodeName(), componentConnection.workflowNodeName()) &&
                  Objects.equals(choice.key(), componentConnection.key()))
              .findFirst();

          if (connectionChoice.isPresent()) {
              DeploymentConnectionChoiceDTO deploymentConnectionChoiceDTO = connectionChoice.get();

              long connectionId = deploymentConnectionChoiceDTO.connectionId();

              return findCandidate(candidates, connectionId)
                  .orElseThrow(() -> new ConfigurationException(
                      "Connection %d cannot be used for slot %s/%s of workflow %s; usable candidates: %s".formatted(
                          connectionId, componentConnection.workflowNodeName(), componentConnection.key(), workflowUuid,
                          describeCandidates(candidates)),
                      ProjectDeploymentErrorType.INVALID_CONNECTION_CHOICE));
          }

          if (currentProjectDeploymentWorkflow != null) {
              Optional<ConnectionDTO> currentConnection = currentProjectDeploymentWorkflow.getConnections()
                  .stream()
                  .filter(workflowConnection -> Objects.equals(
                      workflowConnection.getWorkflowNodeName(), componentConnection.workflowNodeName()) &&
                      Objects.equals(workflowConnection.getWorkflowConnectionKey(), componentConnection.key()))
                  .map(ProjectDeploymentWorkflowConnection::getConnectionId)
                  .filter(Objects::nonNull)
                  .flatMap(connectionId -> findCandidate(candidates, connectionId).stream())
                  .findFirst();

              if (currentConnection.isPresent()) {
                  return currentConnection.get();
              }
          }

          return candidates.size() == 1 ? candidates.getFirst() : null;
      }

      private long createDevelopmentProjectDeployment(
          Project project, int projectVersion, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {

          ProjectDeployment projectDeployment = new ProjectDeployment();

          projectDeployment.setEnvironment(Environment.DEVELOPMENT);
          projectDeployment.setName(project.getName());
          projectDeployment.setProjectId(project.getId());
          projectDeployment.setProjectVersion(projectVersion);

          projectDeployment = projectDeploymentService.create(projectDeployment);

          checkProjectDeploymentWorkflows(projectDeployment, -1, projectDeploymentWorkflows, List.of());

          return Objects.requireNonNull(projectDeployment.getId(), "id");
      }

      private static String describeCandidates(List<ConnectionDTO> candidates) {
          if (candidates.isEmpty()) {
              return "none";
          }

          return candidates.stream()
              .map(candidate -> "%d (%s)".formatted(candidate.id(), candidate.name()))
              .collect(Collectors.joining(", "));
      }

      private @Nullable ProjectDeployment fetchDevelopmentProjectDeployment(
          long projectId, @Nullable Long projectDeploymentId) {

          if (projectDeploymentId == null) {
              List<Long> developmentProjectDeploymentIds = projectDeploymentService
                  .getProjectDeployments(false, Environment.DEVELOPMENT, projectId, null, null)
                  .stream()
                  .filter(projectDeployment -> !SystemProjects.isSystemDeploymentName(projectDeployment.getName()))
                  .map(ProjectDeployment::getId)
                  .toList();

              if (!developmentProjectDeploymentIds.isEmpty()) {
                  throw new ConfigurationException(
                      "Project %d already has a DEVELOPMENT deployment %s; pass its projectDeploymentId".formatted(
                          projectId, developmentProjectDeploymentIds),
                      ProjectDeploymentErrorType.INVALID_DEPLOYMENT);
              }

              return null;
          }

          return projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
              .filter(projectDeployment -> Objects.equals(projectDeployment.getProjectId(), projectId))
              .filter(projectDeployment -> projectDeployment.getEnvironment() == Environment.DEVELOPMENT)
              .filter(projectDeployment -> !SystemProjects.isSystemDeploymentName(projectDeployment.getName()))
              .orElseThrow(() -> new ConfigurationException(
                  "Project deployment %d is not a DEVELOPMENT deployment of project %d".formatted(
                      projectDeploymentId, projectId),
                  ProjectDeploymentErrorType.INVALID_DEPLOYMENT));
      }

      private static Optional<ConnectionDTO> findCandidate(List<ConnectionDTO> candidates, long connectionId) {
          return candidates.stream()
              .filter(candidate -> Objects.equals(candidate.id(), connectionId))
              .findFirst();
      }

      private List<ConnectionDTO> getCandidateConnections(long workspaceId, ComponentConnection componentConnection) {
          ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionDefinition(
              componentConnection.componentName(), componentConnection.componentVersion());

          return workspaceConnectionFacade
              .getConnections(
                  workspaceId, componentConnection.componentName(), connectionDefinition.getVersion(),
                  (long) Environment.DEVELOPMENT.ordinal(), null)
              .stream()
              .filter(this::isUsableCandidate)
              .toList();
      }

      private int getDeployableProjectVersion(Project project) {
          long projectId = Objects.requireNonNull(project.getId(), "id");
          ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

          if (lastPublishedProjectVersion == null) {
              throw new ConfigurationException(
                  "Project %d has never been published; call publishProject first".formatted(projectId),
                  ProjectDeploymentErrorType.UNPUBLISHED_CHANGES);
          }

          int publishedProjectVersion = lastPublishedProjectVersion.getVersion();

          Map<String, String> draftWorkflowIds = getWorkflowIdsByUuid(projectId, project.getLastProjectVersion());
          Map<String, String> publishedWorkflowIds = getWorkflowIdsByUuid(projectId, publishedProjectVersion);

          boolean unpublishedChanges = !Objects.equals(draftWorkflowIds.keySet(), publishedWorkflowIds.keySet()) ||
              draftWorkflowIds.entrySet()
                  .stream()
                  .anyMatch(entry -> !hasSameDefinition(entry.getValue(), publishedWorkflowIds.get(entry.getKey())));

          if (unpublishedChanges) {
              throw new ConfigurationException(
                  "Project %d has unpublished changes; call publishProject first".formatted(projectId),
                  ProjectDeploymentErrorType.UNPUBLISHED_CHANGES);
          }

          return publishedProjectVersion;
      }

      private static List<MissingInput> getMissingInputs(
          String workflowUuid, Workflow workflow, Map<String, ?> workflowInputs) {

          List<MissingInput> missingInputs = new ArrayList<>();

          for (Workflow.Input input : workflow.getInputs()) {
              Object value = workflowInputs.get(input.name());

              if (input.required() && (value == null || (value instanceof String string && string.isBlank()))) {
                  missingInputs.add(new MissingInput(workflowUuid, input.name()));
              }
          }

          return missingInputs;
      }

      private Map<String, ProjectDeploymentWorkflow> getProjectDeploymentWorkflowsByUuid(
          ProjectDeployment projectDeployment) {

          long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId(), "id");

          Map<String, ProjectDeploymentWorkflow> projectDeploymentWorkflows = new HashMap<>();

          for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
              .getProjectDeploymentWorkflows(projectDeploymentId)) {

              projectDeploymentWorkflows.put(
                  projectWorkflowService.getProjectWorkflowUuid(
                      projectDeploymentId, projectDeploymentWorkflow.getWorkflowId()),
                  projectDeploymentWorkflow);
          }

          return projectDeploymentWorkflows;
      }

      private List<RemovedWorkflow> getRemovedWorkflows(
          ProjectDeployment projectDeployment, List<ProjectWorkflow> projectWorkflows) {

          Set<String> workflowUuids = projectWorkflows.stream()
              .map(ProjectWorkflow::getUuidAsString)
              .collect(Collectors.toSet());

          return projectWorkflowService
              .getProjectWorkflows(projectDeployment.getProjectId(), projectDeployment.getProjectVersion())
              .stream()
              .filter(projectWorkflow -> !workflowUuids.contains(projectWorkflow.getUuidAsString()))
              .map(projectWorkflow -> new RemovedWorkflow(
                  projectWorkflow.getUuidAsString(),
                  getWorkflowName(workflowService.getWorkflow(projectWorkflow.getWorkflowId()))))
              .toList();
      }

      private Map<String, String> getWorkflowIdsByUuid(long projectId, int projectVersion) {
          return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
              .stream()
              .collect(Collectors.toMap(ProjectWorkflow::getUuidAsString, ProjectWorkflow::getWorkflowId));
      }

      private static String getWorkflowName(Workflow workflow) {
          String label = workflow.getLabel();

          return label == null ? workflow.getId() : label;
      }

      private boolean hasSameDefinition(String workflowId, String otherWorkflowId) {
          Workflow workflow = workflowService.getWorkflow(workflowId);
          Workflow otherWorkflow = workflowService.getWorkflow(otherWorkflowId);

          return Objects.equals(workflow.getDefinition(), otherWorkflow.getDefinition());
      }

      private boolean isUsableCandidate(ConnectionDTO connection) {
          Long connectionId = connection.id();

          return connectionId != null && !AiProviderConnectionId.isAiProviderConnectionId(connectionId) &&
              connection.credentialStatus() == CredentialStatus.VALID &&
              permissionService.hasResourceScope(connectionId, CONNECTION, CONNECTION_USE);
      }

      private long redeployDevelopmentProjectDeployment(
          ProjectDeployment projectDeployment, int projectVersion,
          List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {

          long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId(), "id");
          int oldProjectVersion = projectDeployment.getProjectVersion();

          if (projectDeployment.isEnabled()) {
              enableProjectDeployment(projectDeploymentId, false);
          }

          ProjectDeployment disabledProjectDeployment = projectDeploymentService.getProjectDeployment(
              projectDeploymentId);

          disabledProjectDeployment.setEnabled(false);
          disabledProjectDeployment.setProjectVersion(projectVersion);

          disabledProjectDeployment = projectDeploymentService.update(disabledProjectDeployment);

          checkProjectDeploymentWorkflows(
              disabledProjectDeployment, oldProjectVersion, projectDeploymentWorkflows,
              projectWorkflowService.getProjectWorkflows(disabledProjectDeployment.getProjectId()));

          return projectDeploymentId;
      }

      private static Map<String, ?> resolveInputs(
          String workflowUuid, Map<String, Map<String, ?>> inputs,
          @Nullable ProjectDeploymentWorkflow currentProjectDeploymentWorkflow) {

          Map<String, ?> workflowInputs = inputs.get(workflowUuid);

          if (workflowInputs != null) {
              return workflowInputs;
          }

          if (currentProjectDeploymentWorkflow != null) {
              return currentProjectDeploymentWorkflow.getInputs();
          }

          return Map.of();
      }

      private static ProjectDeploymentWorkflow toProjectDeploymentWorkflow(
          String workflowId, boolean enabled, Map<String, ?> workflowInputs,
          List<ProjectDeploymentWorkflowConnection> workflowConnections) {

          ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

          projectDeploymentWorkflow.setConnections(workflowConnections);
          projectDeploymentWorkflow.setEnabled(enabled);
          projectDeploymentWorkflow.setInputs(workflowInputs);
          projectDeploymentWorkflow.setWorkflowId(workflowId);

          return projectDeploymentWorkflow;
      }

      private static UnresolvedConnection toUnresolvedConnection(
          String workflowUuid, Workflow workflow, ComponentConnection componentConnection,
          List<ConnectionDTO> candidates) {

          return new UnresolvedConnection(
              workflowUuid, getWorkflowName(workflow), componentConnection.workflowNodeName(),
              componentConnection.key(), componentConnection.componentName(), componentConnection.required(),
              candidates.stream()
                  .map(candidate -> new Candidate(Objects.requireNonNull(candidate.id(), "id"), candidate.name()))
                  .toList());
      }
  ```

  Add `import java.util.TreeSet;` (the uuid set is sorted so the refusal names the uuids in a stable order). `Function`, `Collectors`, `Set`, `HashMap`, `ArrayList`, `Optional`,
  `Objects` and `SystemProjects` are already imported.

  Update the three hand-built instances to the new constructor arity by appending `, null, null, null` before the
  closing `)` of each `new ProjectDeploymentFacadeImpl(`:
  - `ProjectDeploymentFacadeWorkflowBindingTest.java` line ~170: `… Mockito.mock(ApplicationProperties.class), null, null, null, null, null);`
    (the original ended `…, null, null);`; it now ends with five `null`s).
  - `ProjectDeploymentFacadeChatWorkflowTest.java` line ~275: `workflowService);` → `workflowService, null, null, null);`.
  - `ProjectDeploymentCrossEnvironmentReadReproductionIntTest.java` line ~289:
    `triggerLifecycleFacade, applicationProperties, componentConnectionFacade, workflowService);` →
    `triggerLifecycleFacade, applicationProperties, componentConnectionFacade, workflowService, null, null, null);`.

  Classify the two DTOs in `VisibilityBearingSurfaceAuditTest.java`: add
  `"com.bytechef.automation.configuration.dto.DeployProjectResultDTO",` to `VISIBILITY_BEARING_TYPE_NAMES` (it carries
  a project's workflow names, so `deployProject` must stay gated — the audit then checks it is), and add to
  `NON_VISIBILITY_BEARING_DTOS`:

  ```java
          "DeploymentConnectionChoiceDTO",
          "An input only: the workflow uuid, node name, key and connection id the caller chose; returns nothing.",
  ```

- [ ] **Step 6: Run the tests to see them pass**

  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "com.bytechef.automation.configuration.facade.ProjectDeployment*" > <scratchpad>/d1-t3-test.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.
  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests "com.bytechef.automation.configuration.facade.ProjectDeploymentFacade*IntTest" > <scratchpad>/d1-t3-int.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`; both `ProjectDeploymentFacadeDeployProjectIntTest` (20 tests) and the existing
  `ProjectDeploymentFacadeIntTest` pass.
  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-graphql:test --tests "com.bytechef.automation.configuration.security.VisibilityBearingSurfaceAuditTest" > <scratchpad>/d1-t3-audit.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.
  Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests "*ProjectDeploymentCrossEnvironmentReadReproductionIntTest" > <scratchpad>/d1-t3-ee-int.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.

  Then prove the `@Lazy` collaborators keep the other hand-assembled contexts booting:
  Run: `./gradlew --continue :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:testIntegration > <scratchpad>/d1-t3-ctx.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t3-ctx.log`.
  Expected: `exit=0`, no FAILED line. A `NoSuchBeanDefinitionException` naming `ConnectionDefinitionService`,
  `PermissionService` or `WorkspaceConnectionFacade` means a `@Lazy` was dropped from the constructor.

- [ ] **Step 7: Format and check**

  Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:spotlessApply :server:libs:automation:automation-configuration:automation-configuration-service:spotlessApply :server:libs:automation:automation-configuration:automation-configuration-graphql:spotlessApply :server:ee:libs:automation:automation-configuration:automation-configuration-service:spotlessApply > <scratchpad>/d1-t3-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:libs:automation:automation-configuration:automation-configuration-api:check :server:libs:automation:automation-configuration:automation-configuration-service:check :server:libs:automation:automation-configuration:automation-configuration-graphql:check :server:ee:libs:automation:automation-configuration:automation-configuration-service:check > <scratchpad>/d1-t3-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t3-check.log`.
  Expected: `exit=0`, no FAILED line.

- [ ] **Step 8: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/dto/DeployProjectResultDTO.java \
    server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/dto/DeploymentConnectionChoiceDTO.java \
    server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/exception/ProjectDeploymentErrorType.java \
    server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacade.java \
    server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeImpl.java \
    server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeDeployProjectIntTest.java \
    server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeAuthorizationTest.java \
    server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeWorkflowBindingTest.java \
    server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeChatWorkflowTest.java \
    server/libs/automation/automation-configuration/automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/security/VisibilityBearingSurfaceAuditTest.java \
    server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/ee/automation/configuration/service/ProjectDeploymentCrossEnvironmentReadReproductionIntTest.java
  git commit -m "- Add ProjectDeploymentFacade.deployProject for the app loop's DEVELOPMENT deployments"
  ```

---
### Task 4: The app facades' lookups — `getConnections`, filtered `getBindableConnections`, `getCallableWorkflows`

**Files:**
- Create: `server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppWorkspaceConnectionDTO.java`
- Create: `server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppCallableWorkflowDTO.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppBindableConnectionDTO.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppFacade.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacade.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppFacadeImpl.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeImpl.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppFacadeTest.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeTest.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-graphql/src/main/java/com/bytechef/ee/automation/app/web/graphql/AppGraphQlController.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-graphql/src/test/java/com/bytechef/ee/automation/app/web/graphql/AppGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `AppCallableWorkflowResolver.getBindableWorkflows(long, Environment)` (Task 1).
- Produces (EE `com.bytechef.ee.automation.app`):
  ```java
  record AppWorkspaceConnectionDTO(long id, String name, String componentName, int connectionVersion,
      @Nullable String authorizationType, String credentialStatus, boolean aiProvider, boolean usable)
  record AppCallableWorkflowDTO(long projectDeploymentId, String workflowUuid, String workflowName, long projectId,
      String projectName, @Nullable String inputSchema)
  record AppBindableConnectionDTO(long id, String name, String componentName, int connectionVersion,
      int componentVersion, boolean usable)                       // + usable
  List<AppWorkspaceConnectionDTO> AppFacade.getConnections(long workspaceId, Environment environment,
      @Nullable String componentName)
  List<AppBindableConnectionDTO> AppFacade.getBindableConnections(long appId, @Nullable String componentName)
      // replaces getBindableConnections(long appId)
  List<AppCallableWorkflowDTO> AppWorkflowFacade.getCallableWorkflows(long workspaceId, @Nullable Long projectId)
  ```
  Field names match Plan D2's `AppWorkspaceConnection`/`AppBindableConnection` schemas.

- [ ] **Step 1: Write the failing facade tests.**

  `AppFacadeTest.java`:
  - add `@Mock private PermissionService permissionService;` and `@Mock private WorkspaceConnectionFacade workspaceConnectionFacade;`
    (imports `com.bytechef.automation.configuration.service.PermissionService`,
    `com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade`,
    `com.bytechef.platform.connection.domain.AiProviderConnectionId`,
    `com.bytechef.platform.connection.domain.Connection.CredentialStatus`,
    `com.bytechef.platform.connection.dto.ConnectionDTO`,
    `com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO`);
  - replace the constructor call in `beforeEach` with
    ```java
            appFacade = new AppFacadeImpl(
                actionDefinitionService, appAuditPublisher, appConnectionService, appService,
                clusterElementDefinitionService, componentDefinitionService, connectionService, permissionService,
                resourceGrantService, new ResourceVisibilityPolicyRegistry(List.of(new AppVisibilityPolicy())),
                resourceVisibilityResolver, userService, workspaceConnectionFacade);
    ```
  - in `preAuthorizeExpressionsArePinned` replace the `getBindableConnections` assertion with
    ```java
            assertThat(preAuthorize("getBindableConnections", long.class, String.class))
                .isEqualTo("hasPermission(#appId, 'App', 'APP_MANAGE')");
            assertThat(preAuthorize("getConnections", long.class, Environment.class, String.class))
                .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'CONNECTION_VIEW', #environment)");
    ```
  - in the two existing `getBindableConnections…` tests change `appFacade.getBindableConnections(1L)` to
    `appFacade.getBindableConnections(1L, null)` and append `, false` to each expected
    `new AppBindableConnectionDTO(5L, …, 1, 3)` (their connections carry no credential status, so not usable);
  - add these tests before `private static Connection connection(`:

  ```java
      @Test
      void getBindableConnectionsMarksOnlyValidConnectionsTheCallerMayUseAsUsable() {
          Connection usable = connection(5L, "Usable");
          Connection notUsable = connection(6L, "Not mine to use");
          Connection invalid = connection(8L, "Expired");

          usable.setCredentialStatus(CredentialStatus.VALID);
          notUsable.setCredentialStatus(CredentialStatus.VALID);
          invalid.setCredentialStatus(CredentialStatus.INVALID);

          when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
          when(appConnectionService.getBindableConnections(1L)).thenReturn(List.of(usable, notUsable, invalid));
          when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(7L), anyCollection()))
              .thenReturn(Set.of(5L, 6L, 8L));
          when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_USE")).thenReturn(true);
          when(permissionService.hasResourceScope(6L, "Connection", "CONNECTION_USE")).thenReturn(false);
          stubLatestComponentVersion("salesforce", 3);

          assertThat(appFacade.getBindableConnections(1L, null))
              .extracting(AppBindableConnectionDTO::id, AppBindableConnectionDTO::usable)
              .containsExactly(tuple(5L, true), tuple(6L, false), tuple(8L, false));
      }

      @Test
      void getBindableConnectionsFiltersByComponentName() {
          Connection salesforce = connection(5L, "Salesforce");
          Connection slack = connection(6L, "Slack");

          slack.setComponentName("slack");

          when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
          when(appConnectionService.getBindableConnections(1L)).thenReturn(List.of(salesforce, slack));
          when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(7L), anyCollection()))
              .thenReturn(Set.of(5L, 6L));
          stubLatestComponentVersion("slack", 2);

          assertThat(appFacade.getBindableConnections(1L, "slack"))
              .extracting(AppBindableConnectionDTO::id)
              .containsExactly(6L);
      }

      @Test
      void getConnectionsMarksAiProviderAndUsableConnectionsAndNeverCarriesParameters() {
          long aiProviderConnectionId = AiProviderConnectionId.encode(3, Environment.DEVELOPMENT.ordinal());

          when(workspaceConnectionFacade.getConnections(7L, "slack", null, 0L, null)).thenReturn(List.of(
              connectionDTO(101L, "Slack A", CredentialStatus.VALID),
              connectionDTO(102L, "Slack B", CredentialStatus.INVALID),
              connectionDTO(aiProviderConnectionId, "OpenAI", CredentialStatus.VALID)));
          when(permissionService.hasResourceScope(101L, "Connection", "CONNECTION_USE")).thenReturn(true);

          List<AppWorkspaceConnectionDTO> connections = appFacade.getConnections(7L, Environment.DEVELOPMENT, "slack");

          assertThat(connections)
              .extracting(
                  AppWorkspaceConnectionDTO::id, AppWorkspaceConnectionDTO::credentialStatus,
                  AppWorkspaceConnectionDTO::aiProvider, AppWorkspaceConnectionDTO::usable)
              .containsExactly(
                  tuple(101L, "VALID", false, true),
                  tuple(102L, "INVALID", false, false),
                  tuple(aiProviderConnectionId, "VALID", true, false));

          verify(permissionService, never()).hasResourceScope(aiProviderConnectionId, "Connection", "CONNECTION_USE");
      }
  ```

  and, next to `connection(`, the helper:

  ```java
      private static ConnectionDTO connectionDTO(long id, String name, CredentialStatus credentialStatus) {
          return ConnectionDTO.builder()
              .componentName("slack")
              .connectionVersion(1)
              .credentialStatus(credentialStatus)
              .id(id)
              .name(name)
              .parameters(Map.of("token", "secret"))
              .visibility(ResourceVisibility.WORKSPACE)
              .build();
      }
  ```
  plus `import static org.assertj.core.api.Assertions.tuple;`. `AppWorkspaceConnectionDTO` has no field that could
  carry `parameters`, which the record's component list itself pins.

  `AppWorkflowFacadeTest.java`: add the guard to `testEveryFacadeMethodIsGated`'s map (it becomes five entries):
  ```java
              "getCallableWorkflows",
              "hasWorkspaceScopeInEnvironment(#workspaceId, 'DEPLOYMENT_VIEW', " +
                  "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)",
  ```
  and add (imports `com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO`):

  ```java
      @Test
      void testGetCallableWorkflowsListsDevelopmentWorkflowsOfVisibleProjects() {
          Project visibleProject = project(3L, "Refund project", ResourceVisibility.WORKSPACE, "alice");
          Project hiddenProject = project(4L, "Secret project", ResourceVisibility.PRIVATE, "bob");

          when(appCallableWorkflowResolver.getBindableWorkflows(7L, Environment.DEVELOPMENT)).thenReturn(List.of(
              new BindableWorkflow(
                  projectDeployment(11L, 3L, "Refunds"), visibleProject, FIRST_UUID,
                  new CallableWorkflow("wf-2", "trigger_1", "Approve refund", null, "{}", null, Map.of())),
              new BindableWorkflow(
                  projectDeployment(12L, 4L, "Secrets"), hiddenProject, SECOND_UUID, callableWorkflow("Leak", null))));
          when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(7L), any())).thenReturn(Set.of(3L));

          assertThat(appWorkflowFacade.getCallableWorkflows(7L, null)).containsExactly(
              new AppCallableWorkflowDTO(11L, FIRST_UUID, "Approve refund", 3L, "Refund project", "{}"));
      }

      @Test
      void testGetCallableWorkflowsKeepsOnlyTheGivenProject() {
          Project refunds = project(3L, "Refund project", ResourceVisibility.WORKSPACE, "alice");
          Project orders = project(4L, "Order project", ResourceVisibility.WORKSPACE, "alice");

          when(appCallableWorkflowResolver.getBindableWorkflows(7L, Environment.DEVELOPMENT)).thenReturn(List.of(
              new BindableWorkflow(
                  projectDeployment(11L, 3L, "Refunds"), refunds, FIRST_UUID, callableWorkflow("Refund", null)),
              new BindableWorkflow(
                  projectDeployment(12L, 4L, "Orders"), orders, SECOND_UUID, callableWorkflow("Order", null))));
          when(resourceVisibilityResolver.filterVisibleIds(eq("Project"), eq(7L), any())).thenReturn(Set.of(4L));

          assertThat(appWorkflowFacade.getCallableWorkflows(7L, 4L))
              .extracting(AppCallableWorkflowDTO::workflowUuid)
              .containsExactly(SECOND_UUID);
      }
  ```

- [ ] **Step 2: Run them to see them fail**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:compileTestJava > <scratchpad>/d1-t4-red.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; `cannot find symbol` for `AppWorkspaceConnectionDTO`, `getConnections`,
  `getCallableWorkflows`, and a constructor arity error.

- [ ] **Step 3: Add the DTOs and interface methods.**

  `AppWorkspaceConnectionDTO.java`:

  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */

  package com.bytechef.ee.automation.app.dto;

  import org.jspecify.annotations.Nullable;

  /**
   * A connection of a workspace as the app loop lists it. It never carries parameters or credentials. {@code usable}
   * means valid credentials and {@code CONNECTION_USE} for the caller; an AI-provider connection is never usable.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  public record AppWorkspaceConnectionDTO(
      long id, String name, String componentName, int connectionVersion, @Nullable String authorizationType,
      String credentialStatus, boolean aiProvider, boolean usable) {
  }
  ```

  `AppCallableWorkflowDTO.java` (same header):

  ```java
  package com.bytechef.ee.automation.app.dto;

  import org.jspecify.annotations.Nullable;

  /**
   * A deployed workflow an app can call in DEVELOPMENT, keyed for binding by {@code (projectDeploymentId,
   * workflowUuid)}.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  public record AppCallableWorkflowDTO(
      long projectDeploymentId, String workflowUuid, String workflowName, long projectId, String projectName,
      @Nullable String inputSchema) {
  }
  ```

  `AppBindableConnectionDTO.java`: the record becomes
  `public record AppBindableConnectionDTO(long id, String name, String componentName, int connectionVersion, int componentVersion, boolean usable) {`.

  `AppFacade.java`: replace `List<AppBindableConnectionDTO> getBindableConnections(long appId);` and its Javadoc with

  ```java
      /**
       * The connections {@link #bindConnection} would accept for the app, optionally of one component. {@code usable}
       * marks those with valid credentials the caller may use.
       */
      List<AppBindableConnectionDTO> getBindableConnections(long appId, @Nullable String componentName);

      /**
       * The connections of a workspace in an environment, for the app loop: no parameters, no credentials.
       */
      List<AppWorkspaceConnectionDTO> getConnections(
          long workspaceId, Environment environment, @Nullable String componentName);
  ```
  (import `com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO`).

  `AppWorkflowFacade.java`: add (imports `AppCallableWorkflowDTO`, `org.jspecify.annotations.Nullable`)

  ```java
      /**
       * The workflows an app can call in DEVELOPMENT in a workspace, optionally of one project, limited to projects the
       * caller can see.
       */
      List<AppCallableWorkflowDTO> getCallableWorkflows(long workspaceId, @Nullable Long projectId);
  ```

- [ ] **Step 4: Implement them.**

  `AppFacadeImpl.java`: add fields/constructor parameters `PermissionService permissionService` (after
  `connectionService`) and `WorkspaceConnectionFacade workspaceConnectionFacade` (last), both assigned in the
  constructor; imports `com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade`,
  `com.bytechef.automation.configuration.service.PermissionService`,
  `com.bytechef.component.definition.Authorization.AuthorizationType`,
  `com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO`,
  `com.bytechef.platform.connection.domain.AiProviderConnectionId`,
  `com.bytechef.platform.connection.domain.Connection.CredentialStatus`,
  `com.bytechef.platform.connection.dto.ConnectionDTO`. Constants:
  ```java
      private static final String CONNECTION = "Connection";
      private static final String CONNECTION_USE = "CONNECTION_USE";
  ```
  Replace `getBindableConnections` with:

  ```java
      @Override
      @PreAuthorize("hasPermission(#appId, 'App', 'APP_MANAGE')")
      @Transactional(readOnly = true)
      public List<AppBindableConnectionDTO> getBindableConnections(long appId, @Nullable String componentName) {
          App app = appService.getApp(appId);

          List<Connection> bindableConnections = appConnectionService.getBindableConnections(appId)
              .stream()
              .filter(connection -> componentName == null || Objects.equals(connection.getComponentName(), componentName))
              .toList();

          Set<Long> visibleConnectionIds = resourceVisibilityResolver.filterVisibleIds(
              CONNECTION, Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"),
              bindableConnections.stream()
                  .map(connection -> new VisibilityRecord(
                      connection.getId(), connection.getVisibility(), connection.getCreatedBy()))
                  .toList());

          Map<String, Optional<Integer>> latestComponentVersions = new HashMap<>();

          return bindableConnections.stream()
              .filter(connection -> visibleConnectionIds.contains(connection.getId()))
              .flatMap(connection -> latestComponentVersions
                  .computeIfAbsent(connection.getComponentName(), this::fetchLatestVersion)
                  .map(componentVersion -> new AppBindableConnectionDTO(
                      connection.getId(), connection.getName(), connection.getComponentName(),
                      connection.getConnectionVersion(), componentVersion,
                      isUsable(connection.getId(), connection.getCredentialStatus())))
                  .stream())
              .toList();
      }

      @Override
      @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'CONNECTION_VIEW', #environment)")
      @Transactional(readOnly = true)
      public List<AppWorkspaceConnectionDTO> getConnections(
          long workspaceId, Environment environment, @Nullable String componentName) {

          return workspaceConnectionFacade
              .getConnections(workspaceId, componentName, null, (long) environment.ordinal(), null)
              .stream()
              .map(this::toWorkspaceConnectionDTO)
              .toList();
      }
  ```

  and the helpers (with the other private methods):

  ```java
      private boolean isUsable(long connectionId, @Nullable CredentialStatus credentialStatus) {
          return credentialStatus == CredentialStatus.VALID &&
              permissionService.hasResourceScope(connectionId, CONNECTION, CONNECTION_USE);
      }

      private AppWorkspaceConnectionDTO toWorkspaceConnectionDTO(ConnectionDTO connectionDTO) {
          long connectionId = Objects.requireNonNull(connectionDTO.id(), "id");
          boolean aiProvider = AiProviderConnectionId.isAiProviderConnectionId(connectionId);
          AuthorizationType authorizationType = connectionDTO.authorizationType();

          return new AppWorkspaceConnectionDTO(
              connectionId, connectionDTO.name(), connectionDTO.componentName(), connectionDTO.connectionVersion(),
              authorizationType == null ? null : authorizationType.name(),
              String.valueOf(connectionDTO.credentialStatus()), aiProvider,
              !aiProvider && isUsable(connectionId, connectionDTO.credentialStatus()));
      }
  ```

  The existing `getBindableConnections` used the literal `"Connection"`; it now uses `CONNECTION`.

  `AppWorkflowFacadeImpl.java`: imports `com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO`,
  `com.bytechef.platform.configuration.domain.Environment`, `org.jspecify.annotations.Nullable`; add after
  `getBindableWorkflows`:

  ```java
      @Override
      @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'DEPLOYMENT_VIEW', " +
          "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)")
      @Transactional(readOnly = true)
      public List<AppCallableWorkflowDTO> getCallableWorkflows(long workspaceId, @Nullable Long projectId) {
          List<BindableWorkflow> bindableWorkflows = appCallableWorkflowResolver
              .getBindableWorkflows(workspaceId, Environment.DEVELOPMENT)
              .stream()
              .filter(bindableWorkflow -> projectId == null || Objects.equals(getProjectId(bindableWorkflow), projectId))
              .toList();

          Set<Long> visibleProjectIds = getVisibleProjectIds(
              workspaceId, bindableWorkflows.stream()
                  .map(BindableWorkflow::project)
                  .toList());

          return bindableWorkflows.stream()
              .filter(bindableWorkflow -> visibleProjectIds.contains(getProjectId(bindableWorkflow)))
              .map(AppWorkflowFacadeImpl::toCallableWorkflowDTO)
              .toList();
      }
  ```

  and the helpers:

  ```java
      private static Long getProjectId(BindableWorkflow bindableWorkflow) {
          Project project = bindableWorkflow.project();

          return project.getId();
      }

      private static AppCallableWorkflowDTO toCallableWorkflowDTO(BindableWorkflow bindableWorkflow) {
          ProjectDeployment projectDeployment = bindableWorkflow.projectDeployment();
          Project project = bindableWorkflow.project();
          CallableWorkflow callableWorkflow = bindableWorkflow.callableWorkflow();

          return new AppCallableWorkflowDTO(
              Objects.requireNonNull(projectDeployment.getId(), "id"), bindableWorkflow.workflowUuid(),
              callableWorkflow.label(), Objects.requireNonNull(project.getId(), "id"), project.getName(),
              callableWorkflow.inputSchema());
      }
  ```

  `AppIntTestConfiguration.java`: add two beans (imports
  `com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade`,
  `com.bytechef.automation.configuration.service.PermissionService`):
  ```java
      @Bean
      PermissionService permissionService() {
          return Mockito.mock(PermissionService.class);
      }

      @Bean
      WorkspaceConnectionFacade workspaceConnectionFacade() {
          return Mockito.mock(WorkspaceConnectionFacade.class);
      }
  ```

  `AppGraphQlController.java`: `appFacade.getBindableConnections(appId)` → `appFacade.getBindableConnections(appId, null)`.
  `AppGraphQlControllerTest.java`: `when(appFacade.getBindableConnections(5L))` → `when(appFacade.getBindableConnections(5L, null))`
  and `new AppBindableConnectionDTO(9L, "Slack", "slack", 1, 2)` → `new AppBindableConnectionDTO(9L, "Slack", "slack", 1, 2, true)`.
  The GraphQL schema and `AppBindableConnectionResponse` stay unchanged.

- [ ] **Step 5: Run the tests to see them pass**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:test :server:ee:libs:automation:automation-app:automation-app-graphql:test > <scratchpad>/d1-t4-test.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.
  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:testIntegration > <scratchpad>/d1-t4-int.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0` (the hand-assembled `AppIntTestConfiguration` context boots with the two new collaborators).

- [ ] **Step 6: Format and check**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-api:spotlessApply :server:ee:libs:automation:automation-app:automation-app-service:spotlessApply :server:ee:libs:automation:automation-app:automation-app-graphql:spotlessApply > <scratchpad>/d1-t4-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-api:check :server:ee:libs:automation:automation-app:automation-app-service:check :server:ee:libs:automation:automation-app:automation-app-graphql:check > <scratchpad>/d1-t4-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t4-check.log`.
  Expected: `exit=0`, no FAILED line. (`automation-app-public-rest` does not compile against
  `getBindableConnections(long)`; nothing there calls it yet — Task 7 adds the call.)

- [ ] **Step 7: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppWorkspaceConnectionDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppCallableWorkflowDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/dto/AppBindableConnectionDTO.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppFacade.java \
    server/ee/libs/automation/automation-app/automation-app-api/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacade.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppFacadeImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/main/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeImpl.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppFacadeTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/facade/AppWorkflowFacadeTest.java \
    server/ee/libs/automation/automation-app/automation-app-service/src/test/java/com/bytechef/ee/automation/app/config/AppIntTestConfiguration.java \
    server/ee/libs/automation/automation-app/automation-app-graphql/src/main/java/com/bytechef/ee/automation/app/web/graphql/AppGraphQlController.java \
    server/ee/libs/automation/automation-app/automation-app-graphql/src/test/java/com/bytechef/ee/automation/app/web/graphql/AppGraphQlControllerTest.java
  git commit -m "- Add the connection and callable workflow lookups the app loop needs to the app facades"
  ```

---

### Task 5: The `deployProject`, `listConnections` and `listAppCallableWorkflows` MCP tools

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/AppLoopTools.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/AppLoopMcpContributorConfiguration.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/ee/automation/ai/tool/AppLoopToolsTest.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/ee/automation/ai/tool/AppLoopMcpContributorConfigurationTest.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-tool/build.gradle.kts`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/CreateProjectDeploymentToolCallback.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/PromoteWorkflowToolCallback.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/CreateProjectDeploymentToolCallbackTest.java`
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/PromoteWorkflowToolCallbackTest.java`

**Interfaces:**
- Consumes: `CallableWorkflowChecker`, `SystemProjects.isSystemDeploymentName` (Task 1);
  `ProjectDeploymentFacade.deployProject`, `DeployProjectResultDTO`, `DeploymentConnectionChoiceDTO` (Task 3);
  `AppFacade.getConnections`, `AppWorkflowFacade.getCallableWorkflows` (Task 4);
  `ProjectDeploymentFacade.getWorkspaceProjectDeployments(long, long, Long, Long)`,
  `ProjectDeploymentFacade.enableProjectDeployment(long, boolean)`, `ProjectFacade.getProject(long)`,
  `ProjectWorkflowFacade.getProjectVersionWorkflows(long, int, boolean)` (existing).
- Produces (EE `com.bytechef.ee.automation.ai.tool`):
  ```java
  public class AppLoopTools {
      AppLoopTools(AppFacade, AppWorkflowFacade, CallableWorkflowChecker, ProjectDeploymentFacade, ProjectFacade,
          ProjectWorkflowFacade)
      @Tool DeployProjectResult deployProject(long projectId, @Nullable Long projectDeploymentId,
          @Nullable List<String> enabledWorkflows, @Nullable List<ConnectionChoice> connections,
          @Nullable Map<String, Map<String, Object>> inputs, ToolContext toolContext)
      @Tool ConnectionListResult listConnections(@Nullable String componentName, ToolContext toolContext)
      @Tool CallableWorkflowListResult listAppCallableWorkflows(@Nullable Long projectId, ToolContext toolContext)
      static String suggestWorkflowKey(String workflowName)
      record ConnectionChoice(String workflowUuid, String workflowNodeName, String key, long connectionId)
      record DeployProjectResult(String status, @Nullable String message, @Nullable Long projectDeploymentId,
          @Nullable Integer projectVersion, boolean enabled, List<DeployedWorkflow> workflows,
          List<RemovedWorkflow> removedWorkflows, List<UnresolvedConnection> unresolvedConnections,
          List<MissingInput> missingInputs, List<DeploymentChoice> deployments)
      record DeployedWorkflow(String workflowUuid, String name, boolean enabled, boolean appCallable,
          @Nullable String inputSchema, @Nullable String draftWorkflowId, List<DeployedConnection> connections,
          @Nullable String bindCommand)
      record DeploymentChoice(long projectDeploymentId, String name)
      record ConnectionListResult(@Nullable String error, List<AppWorkspaceConnectionDTO> connections)
      record CallableWorkflowListResult(@Nullable String error, List<AppCallableWorkflowDTO> workflows)
  }
  ```
  Statuses: `"deployed"`, `"arming failed"`, `"unresolved"`, `"refused"`. `bindCommand` is
  `bytechef automation app bind workflow --workflow-key <KEY> --deployment-id <id> --workflow-uuid <uuid>` (Plan D2's
  command), present only on a `deployed` result for an enabled app-callable workflow.
- Produces: bean `appLoopMcpContributor` (`McpServerToolCallbackContributor`) wrapping each tool in
  `WorkspaceScopedFlatToolCallback`; it contributes nothing unless `AppFacade`, `AppWorkflowFacade`,
  `CallableWorkflowChecker`, `ProjectDeploymentFacade`, `ProjectFacade` and `ProjectWorkflowFacade` are all present
  (`ai-copilot-app` carries this module without `automation-app-service`).

- [ ] **Step 1: Add the dependency.** In `server/ee/libs/automation/automation-ai/automation-ai-tool/build.gradle.kts`,
  after the `automation-api-platform-configuration-api` line, add
  `    implementation(project(":server:ee:libs:automation:automation-app:automation-app-api"))`.

- [ ] **Step 2: Write the failing tests.** `AppLoopToolsTest.java`:

  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */

  package com.bytechef.ee.automation.ai.tool;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.tuple;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.anyBoolean;
  import static org.mockito.ArgumentMatchers.anyList;
  import static org.mockito.ArgumentMatchers.anyLong;
  import static org.mockito.ArgumentMatchers.anyMap;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.ArgumentMatchers.isNull;
  import static org.mockito.Mockito.doThrow;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.never;
  import static org.mockito.Mockito.verify;
  import static org.mockito.Mockito.verifyNoInteractions;
  import static org.mockito.Mockito.when;

  import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
  import com.bytechef.automation.configuration.domain.ProjectDeployment;
  import com.bytechef.automation.configuration.domain.ProjectVersion;
  import com.bytechef.automation.configuration.domain.SystemProjects;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.Candidate;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedConnection;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.RemovedWorkflow;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.UnresolvedConnection;
  import com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO;
  import com.bytechef.automation.configuration.dto.ProjectDTO;
  import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
  import com.bytechef.automation.configuration.exception.ProjectDeploymentErrorType;
  import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
  import com.bytechef.automation.configuration.facade.ProjectFacade;
  import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
  import com.bytechef.ee.automation.ai.tool.AppLoopTools.ConnectionChoice;
  import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployProjectResult;
  import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployedWorkflow;
  import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
  import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
  import com.bytechef.ee.automation.app.facade.AppFacade;
  import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
  import com.bytechef.exception.ConfigurationException;
  import com.bytechef.platform.configuration.domain.Environment;
  import java.time.Instant;
  import java.util.List;
  import java.util.Map;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.params.ParameterizedTest;
  import org.junit.jupiter.params.provider.CsvSource;
  import org.springframework.ai.chat.model.ToolContext;

  /**
   * @version ee
   *
   * @author Ivica Cardic
   */
  class AppLoopToolsTest {

      private static final String CALLABLE_UUID = "11111111-1111-4111-8111-111111111111";
      private static final long PROJECT_ID = 5L;
      private static final String SCHEDULED_UUID = "22222222-2222-4222-8222-222222222222";
      private static final long WORKSPACE_ID = 1049L;

      private AppFacade appFacade;
      private AppLoopTools appLoopTools;
      private AppWorkflowFacade appWorkflowFacade;
      private CallableWorkflowChecker callableWorkflowChecker;
      private ProjectDeploymentFacade projectDeploymentFacade;
      private ProjectFacade projectFacade;
      private ProjectWorkflowFacade projectWorkflowFacade;

      @BeforeEach
      void beforeEach() {
          appFacade = mock(AppFacade.class);
          appWorkflowFacade = mock(AppWorkflowFacade.class);
          callableWorkflowChecker = mock(CallableWorkflowChecker.class);
          projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
          projectFacade = mock(ProjectFacade.class);
          projectWorkflowFacade = mock(ProjectWorkflowFacade.class);

          appLoopTools = new AppLoopTools(
              appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
              projectWorkflowFacade);

          when(callableWorkflowChecker.check("wf-callable")).thenReturn(new CallableWorkflowCheck(List.of(), "{}"));
          when(callableWorkflowChecker.check("wf-scheduled"))
              .thenReturn(new CallableWorkflowCheck(List.of("no Call-workflow trigger"), null));
      }

      @Test
      void testNonDevelopmentEnvironmentIsRefused() {
          DeployProjectResult deployProjectResult = appLoopTools.deployProject(
              PROJECT_ID, null, null, null, null, toolContext(Environment.PRODUCTION));

          assertThat(deployProjectResult.status()).isEqualTo("refused");
          assertThat(deployProjectResult.message()).isEqualTo("The app loop runs in DEVELOPMENT only");
          assertThat(appLoopTools.listConnections(null, toolContext(Environment.STAGING))
              .error()).isEqualTo("The app loop runs in DEVELOPMENT only");
          assertThat(appLoopTools.listAppCallableWorkflows(null, toolContext(Environment.PRODUCTION))
              .error()).isEqualTo("The app loop runs in DEVELOPMENT only");

          verifyNoInteractions(appFacade, appWorkflowFacade, projectDeploymentFacade, projectFacade);
      }

      @Test
      void testProjectOfAnotherWorkspaceIsRefused() {
          when(projectFacade.getProject(PROJECT_ID)).thenReturn(project(7L));

          DeployProjectResult deployProjectResult = deploy(null, null, null);

          assertThat(deployProjectResult.status()).isEqualTo("refused");
          assertThat(deployProjectResult.message()).contains("does not belong to workspace 1049");

          verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
      }

      @Test
      void testSeveralDevelopmentDeploymentsRequireAnId() {
          when(projectFacade.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
          when(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, 0L, PROJECT_ID, null))
              .thenReturn(List.of(projectDeployment(11L, "Orders"), projectDeployment(12L, "Orders copy")));

          DeployProjectResult deployProjectResult = deploy(null, null, null);

          assertThat(deployProjectResult.status()).isEqualTo("refused");
          assertThat(deployProjectResult.deployments())
              .extracting(AppLoopTools.DeploymentChoice::projectDeploymentId)
              .containsExactly(11L, 12L);

          verify(projectDeploymentFacade, never()).deployProject(anyLong(), any(), anyList(), anyList(), anyMap());
      }

      @Test
      void testSystemDeploymentsDoNotCountAsTheProjectsDeployment() {
          stubPublishedProject();

          when(projectDeploymentFacade.getWorkspaceProjectDeployments(WORKSPACE_ID, 0L, PROJECT_ID, null))
              .thenReturn(List.of(
                  projectDeployment(13L, SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX + "orders"),
                  projectDeployment(11L, "Orders")));
          when(projectDeploymentFacade.deployProject(eq(PROJECT_ID), eq(11L), anyList(), anyList(), anyMap()))
              .thenReturn(written(11L));

          assertThat(deploy(null, null, null).status()).isEqualTo("deployed");
      }

      @Test
      void testDefaultEnabledWorkflowsAreTheAppCallableOnesAndConnectionsArePickedByTheFacade() {
          stubPublishedProject();

          when(projectDeploymentFacade.deployProject(
              PROJECT_ID, null, List.of(CALLABLE_UUID), List.of(), Map.of())).thenReturn(written(11L));

          deploy(null, null, null);

          verify(projectDeploymentFacade).deployProject(PROJECT_ID, null, List.of(CALLABLE_UUID), List.of(), Map.of());
          verify(projectDeploymentFacade).enableProjectDeployment(11L, true);
      }

      @Test
      void testExplicitWorkflowsConnectionsAndInputsArePassedThrough() {
          stubPublishedProject();

          List<DeploymentConnectionChoiceDTO> connectionChoices = List.of(
              new DeploymentConnectionChoiceDTO(SCHEDULED_UUID, "sendMessage_1", "slack", 101L));
          Map<String, Map<String, ?>> inputs = Map.of(SCHEDULED_UUID, Map.of("region", "eu"));

          when(projectDeploymentFacade.deployProject(
              PROJECT_ID, null, List.of(SCHEDULED_UUID), connectionChoices, inputs)).thenReturn(written(11L));

          deploy(
              List.of(SCHEDULED_UUID), List.of(new ConnectionChoice(SCHEDULED_UUID, "sendMessage_1", "slack", 101L)),
              Map.of(SCHEDULED_UUID, Map.of("region", "eu")));

          verify(projectDeploymentFacade).deployProject(
              PROJECT_ID, null, List.of(SCHEDULED_UUID), connectionChoices, inputs);
      }

      @Test
      void testUnresolvedConnectionsWriteNothingAndAreReported() {
          stubPublishedProject();

          UnresolvedConnection unresolvedConnection = new UnresolvedConnection(
              CALLABLE_UUID, "Greet", "sendMessage_1", "slack", "slack", true,
              List.of(new Candidate(101L, "Slack A"), new Candidate(102L, "Slack B")));

          when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
              .thenReturn(new DeployProjectResultDTO(
                  null, 1, List.of(), List.of(), List.of(unresolvedConnection), List.of()));

          DeployProjectResult deployProjectResult = deploy(null, null, null);

          assertThat(deployProjectResult.status()).isEqualTo("unresolved");
          assertThat(deployProjectResult.unresolvedConnections()).containsExactly(unresolvedConnection);
          assertThat(deployProjectResult.message()).contains("Nothing was written");

          verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
      }

      @Test
      void testUnpublishedChangesAreReturnedAsARefusalWithTheFacadesMessage() {
          stubPublishedProject();

          when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
              .thenThrow(new ConfigurationException(
                  "Project 5 has unpublished changes; call publishProject first",
                  ProjectDeploymentErrorType.UNPUBLISHED_CHANGES));

          DeployProjectResult deployProjectResult = deploy(null, null, null);

          assertThat(deployProjectResult.status()).isEqualTo("refused");
          assertThat(deployProjectResult.message()).contains("call publishProject first");

          verify(projectDeploymentFacade, never()).enableProjectDeployment(anyLong(), anyBoolean());
      }

      @Test
      void testArmingFailureLeavesTheDeploymentDisabledAndIsReported() {
          stubPublishedProject();

          when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
              .thenReturn(written(11L));
          doThrow(new IllegalStateException("trigger rejected"))
              .when(projectDeploymentFacade)
              .enableProjectDeployment(11L, true);

          DeployProjectResult deployProjectResult = deploy(null, null, null);

          assertThat(deployProjectResult.status()).isEqualTo("arming failed");
          assertThat(deployProjectResult.enabled()).isFalse();
          assertThat(deployProjectResult.projectDeploymentId()).isEqualTo(11L);
          assertThat(deployProjectResult.message())
              .contains("trigger rejected")
              .contains("call deployProject again");
          assertThat(deployProjectResult.workflows())
              .extracting(DeployedWorkflow::bindCommand)
              .containsOnlyNulls();
      }

      @Test
      void testDeployedResultMarksAppCallableWorkflowsWithTheirBindCommand() {
          stubPublishedProject();

          when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
              .thenReturn(written(11L));

          DeployProjectResult deployProjectResult = deploy(null, null, null);

          assertThat(deployProjectResult.status()).isEqualTo("deployed");
          assertThat(deployProjectResult.enabled()).isTrue();
          assertThat(deployProjectResult.workflows())
              .extracting(
                  DeployedWorkflow::workflowUuid, DeployedWorkflow::appCallable, DeployedWorkflow::inputSchema,
                  DeployedWorkflow::draftWorkflowId, DeployedWorkflow::bindCommand)
              .containsExactly(
                  tuple(
                      CALLABLE_UUID, true, "{}", "wf-callable-draft",
                      "bytechef automation app bind workflow --workflow-key greet_customers --deployment-id 11 " +
                          "--workflow-uuid " + CALLABLE_UUID),
                  tuple(
                      SCHEDULED_UUID, false, null, "wf-scheduled-draft", null));
      }

      @Test
      void testRemovedWorkflowsPointToTheAppStatusCommand() {
          stubPublishedProject();

          DeployProjectResultDTO writtenWithRemoval = new DeployProjectResultDTO(
              11L, 1, List.of(), List.of(new RemovedWorkflow("33333333-3333-4333-8333-333333333333", "Old")),
              List.of(), List.of());

          when(projectDeploymentFacade.deployProject(anyLong(), isNull(), anyList(), anyList(), anyMap()))
              .thenReturn(writtenWithRemoval);

          DeployProjectResult deployProjectResult = deploy(null, null, null);

          assertThat(deployProjectResult.removedWorkflows()).hasSize(1);
          assertThat(deployProjectResult.message()).contains("bytechef automation app status");
      }

      @Test
      void testListConnectionsPassesTheFacadeAnswerThrough() {
          List<AppWorkspaceConnectionDTO> connections = List.of(
              new AppWorkspaceConnectionDTO(101L, "Slack A", "slack", 1, "OAUTH2_AUTHORIZATION_CODE", "VALID", false,
                  true),
              new AppWorkspaceConnectionDTO(-300L, "OpenAI", "openai", 1, null, "VALID", true, false));

          when(appFacade.getConnections(WORKSPACE_ID, Environment.DEVELOPMENT, "slack")).thenReturn(connections);

          AppLoopTools.ConnectionListResult connectionListResult = appLoopTools.listConnections(
              "slack", toolContext(Environment.DEVELOPMENT));

          assertThat(connectionListResult.error()).isNull();
          assertThat(connectionListResult.connections()).isEqualTo(connections);
      }

      @Test
      void testListAppCallableWorkflowsUsesTheWorkspaceAndProject() {
          List<AppCallableWorkflowDTO> workflows = List.of(
              new AppCallableWorkflowDTO(11L, CALLABLE_UUID, "Greet", PROJECT_ID, "Orders", "{}"));

          when(appWorkflowFacade.getCallableWorkflows(WORKSPACE_ID, PROJECT_ID)).thenReturn(workflows);

          assertThat(appLoopTools.listAppCallableWorkflows(PROJECT_ID, toolContext(Environment.DEVELOPMENT))
              .workflows()).isEqualTo(workflows);
      }

      @ParameterizedTest
      @CsvSource({
          "Greet customers,greet_customers",
          "2024 Report!,workflow_2024_report",
          "!!!,workflow",
          "Überweisung prüfen,berweisung_pr_fen"
      })
      void testSuggestedWorkflowKeysAreValidBindingKeys(String workflowName, String expectedWorkflowKey) {
          assertThat(AppLoopTools.suggestWorkflowKey(workflowName))
              .isEqualTo(expectedWorkflowKey)
              .matches("^[a-z][a-z0-9_-]{0,63}$");
      }

      @Test
      void testSuggestedWorkflowKeyIsCutToSixtyFourCharacters() {
          assertThat(AppLoopTools.suggestWorkflowKey("a".repeat(80))).hasSize(64);
      }

      private DeployProjectResult deploy(
          List<String> enabledWorkflows, List<ConnectionChoice> connections, Map<String, Map<String, Object>> inputs) {

          return appLoopTools.deployProject(
              PROJECT_ID, null, enabledWorkflows, connections, inputs, toolContext(Environment.DEVELOPMENT));
      }

      private void stubPublishedProject() {
          ProjectWorkflowDTO callableWorkflow = mock(ProjectWorkflowDTO.class);
          ProjectWorkflowDTO scheduledWorkflow = mock(ProjectWorkflowDTO.class);

          when(callableWorkflow.getId()).thenReturn("wf-callable");
          when(callableWorkflow.getWorkflowUuid()).thenReturn(CALLABLE_UUID);
          when(scheduledWorkflow.getId()).thenReturn("wf-scheduled");
          when(scheduledWorkflow.getWorkflowUuid()).thenReturn(SCHEDULED_UUID);
          when(projectFacade.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
          when(projectWorkflowFacade.getProjectVersionWorkflows(PROJECT_ID, 1, false))
              .thenReturn(List.of(callableWorkflow, scheduledWorkflow));
      }

      private static ProjectDTO project(long workspaceId) {
          return ProjectDTO.builder()
              .id(PROJECT_ID)
              .name("Orders")
              .projectVersions(
                  List.of(
                      new ProjectVersion(1, ProjectVersion.Status.PUBLISHED.ordinal(), Instant.now(), "v1"),
                      new ProjectVersion(2)))
              .workspaceId(workspaceId)
              .build();
      }

      private static ProjectDeployment projectDeployment(long id, String name) {
          ProjectDeployment projectDeployment = new ProjectDeployment();

          projectDeployment.setId(id);
          projectDeployment.setName(name);

          return projectDeployment;
      }

      private static ToolContext toolContext(Environment environment) {
          return new ToolContext(
              Map.of(
                  AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID,
                  AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, (long) environment.ordinal()));
      }

      private static DeployProjectResultDTO written(long projectDeploymentId) {
          return new DeployProjectResultDTO(
              projectDeploymentId, 1,
              List.of(
                  new DeployProjectResultDTO.DeployedWorkflow(
                      CALLABLE_UUID, "wf-callable", "Greet customers", true, "wf-callable-draft",
                      List.of(new DeployedConnection("sendMessage_1", "slack", 101L, "Slack A"))),
                  new DeployProjectResultDTO.DeployedWorkflow(
                      SCHEDULED_UUID, "wf-scheduled", "Nightly sync", true, "wf-scheduled-draft", List.of())),
              List.of(), List.of(), List.of());
      }
  }
  ```

  `AppLoopMcpContributorConfigurationTest.java` (EE header, `@version ee`):

  ```java
  package com.bytechef.ee.automation.ai.tool;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.when;

  import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
  import com.bytechef.automation.ai.tool.AccessibleWorkspaceResolver;
  import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
  import com.bytechef.automation.configuration.facade.ProjectFacade;
  import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
  import com.bytechef.ee.automation.app.facade.AppFacade;
  import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.ObjectProvider;

  /**
   * @version ee
   *
   * @author Ivica Cardic
   */
  class AppLoopMcpContributorConfigurationTest {

      private final AppLoopMcpContributorConfiguration configuration = new AppLoopMcpContributorConfiguration();

      @Test
      void testContributesTheThreeToolsWrappedForWorkspaceScope() {
          McpServerToolCallbackContributor contributor = configuration.appLoopMcpContributor(
              present(mock(AppFacade.class)), present(mock(AppWorkflowFacade.class)),
              present(mock(CallableWorkflowChecker.class)), present(mock(ProjectDeploymentFacade.class)),
              present(mock(ProjectFacade.class)), present(mock(ProjectWorkflowFacade.class)),
              mock(AccessibleWorkspaceResolver.class));

          assertThat(contributor.getToolCallbacks())
              .extracting(toolCallback -> toolCallback.getToolDefinition()
                  .name())
              .containsExactlyInAnyOrder("deployProject", "listConnections", "listAppCallableWorkflows");
          assertThat(contributor.getToolCallbacks())
              .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                  .inputSchema()).contains("workspaceId"));
      }

      @Test
      void testContributesNothingWithoutTheAppModule() {
          McpServerToolCallbackContributor contributor = configuration.appLoopMcpContributor(
              absent(), absent(), absent(), present(mock(ProjectDeploymentFacade.class)),
              present(mock(ProjectFacade.class)), present(mock(ProjectWorkflowFacade.class)),
              mock(AccessibleWorkspaceResolver.class));

          assertThat(contributor.getToolCallbacks()).isEmpty();
      }

      @SuppressWarnings("unchecked")
      private static <T> ObjectProvider<T> present(T value) {
          ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

          when(objectProvider.getIfAvailable()).thenReturn(value);

          return objectProvider;
      }

      @SuppressWarnings("unchecked")
      private static <T> ObjectProvider<T> absent() {
          return mock(ObjectProvider.class);
      }
  }
  ```

  In `CreateProjectDeploymentToolCallbackTest.testToolDefinitionExposesName` append
  `assertThat(definition.description()).contains("deployProject");`, and create `PromoteWorkflowToolCallbackTest.java`
  (Apache header of its neighbours):

  ```java
  package com.bytechef.automation.ai.tool;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.Mockito.mock;

  import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
  import org.junit.jupiter.api.Test;

  /**
   * @author Ivica Cardic
   */
  class PromoteWorkflowToolCallbackTest {

      @Test
      void testDescriptionPointsToDeployProjectForAppDeployments() {
          PromoteWorkflowToolCallback promoteWorkflowToolCallback = new PromoteWorkflowToolCallback(
              mock(ProjectDeploymentFacade.class));

          assertThat(promoteWorkflowToolCallback.getToolDefinition()
              .description()).contains("deployProject");
      }
  }
  ```

- [ ] **Step 3: Run them to see them fail**

  Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:compileTestJava > <scratchpad>/d1-t5-red.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; `cannot find symbol` `AppLoopTools`, `AppLoopMcpContributorConfiguration`.
  Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.automation.ai.tool.*DeploymentToolCallbackTest" --tests "com.bytechef.automation.ai.tool.PromoteWorkflowToolCallbackTest" > <scratchpad>/d1-t5-red2.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; the two description assertions fail.

- [ ] **Step 4: Write `AppLoopTools.java`.**

  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */

  package com.bytechef.ee.automation.ai.tool;

  import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
  import com.bytechef.automation.configuration.domain.ProjectVersion;
  import com.bytechef.automation.configuration.domain.SystemProjects;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.DeployedConnection;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.MissingInput;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.RemovedWorkflow;
  import com.bytechef.automation.configuration.dto.DeployProjectResultDTO.UnresolvedConnection;
  import com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO;
  import com.bytechef.automation.configuration.dto.ProjectDTO;
  import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
  import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
  import com.bytechef.automation.configuration.facade.ProjectFacade;
  import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
  import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
  import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
  import com.bytechef.ee.automation.app.facade.AppFacade;
  import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
  import com.bytechef.exception.ConfigurationException;
  import com.bytechef.platform.configuration.domain.Environment;
  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.ArrayList;
  import java.util.Comparator;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Locale;
  import java.util.Map;
  import java.util.Objects;
  import java.util.Optional;
  import java.util.regex.Matcher;
  import java.util.regex.Pattern;
  import java.util.stream.Collectors;
  import org.jspecify.annotations.Nullable;
  import org.springframework.ai.chat.model.ToolContext;
  import org.springframework.ai.tool.annotation.Tool;
  import org.springframework.ai.tool.annotation.ToolParam;

  /**
   * The management MCP tools of the Apps agent loop: deploy a project for an app, list the connections to choose from,
   * list what an app can call. DEVELOPMENT only; the workspace comes from {@code WorkspaceScopedFlatToolCallback}. They
   * run as the MCP key's user and go through facades only. Refusals are returned as values, never thrown: the wrapper
   * reduces a thrown exception to its class name, and the agent needs the message to correct itself.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  public class AppLoopTools {

      static final String DEVELOPMENT_ONLY = "The app loop runs in DEVELOPMENT only";
      static final String STATUS_ARMING_FAILED = "arming failed";
      static final String STATUS_DEPLOYED = "deployed";
      static final String STATUS_REFUSED = "refused";
      static final String STATUS_UNRESOLVED = "unresolved";

      private static final String BIND_COMMAND =
          "bytechef automation app bind workflow --workflow-key %s --deployment-id %d --workflow-uuid %s";
      private static final Pattern EDGE_UNDERSCORES = Pattern.compile("^_+|_+$");
      private static final Pattern NON_KEY_CHARACTERS = Pattern.compile("[^a-z0-9]+");
      private static final String UNRESOLVED_MESSAGE =
          "Nothing was written. Pass connections (one {workflowUuid, workflowNodeName, key, connectionId} per " +
              "unresolved slot, chosen from its candidates) and inputs for the missing ones, then call deployProject " +
              "again.";
      private static final int WORKFLOW_KEY_MAX_LENGTH = 64;

      private final AppFacade appFacade;
      private final AppWorkflowFacade appWorkflowFacade;
      private final CallableWorkflowChecker callableWorkflowChecker;
      private final ProjectDeploymentFacade projectDeploymentFacade;
      private final ProjectFacade projectFacade;
      private final ProjectWorkflowFacade projectWorkflowFacade;

      @SuppressFBWarnings("EI")
      public AppLoopTools(
          AppFacade appFacade, AppWorkflowFacade appWorkflowFacade, CallableWorkflowChecker callableWorkflowChecker,
          ProjectDeploymentFacade projectDeploymentFacade, ProjectFacade projectFacade,
          ProjectWorkflowFacade projectWorkflowFacade) {

          this.appFacade = appFacade;
          this.appWorkflowFacade = appWorkflowFacade;
          this.callableWorkflowChecker = callableWorkflowChecker;
          this.projectDeploymentFacade = projectDeploymentFacade;
          this.projectFacade = projectFacade;
          this.projectWorkflowFacade = projectWorkflowFacade;
      }

      @Tool(
          description = "Deploy a project's latest published version to its single DEVELOPMENT deployment (created " +
              "the first time, updated in place after that, so app bindings stay valid) and enable it. Every workflow " +
              "of the version gets a deployment row: enabledWorkflows (workflow uuids) are enabled, the others keep " +
              "their state, and when enabledWorkflows is omitted the app-callable workflows are enabled. A connection " +
              "slot gets the only usable connection that fits; otherwise pass connections " +
              "[{workflowUuid, workflowNodeName, key, connectionId}] from the candidates the result lists. inputs " +
              "maps a workflow uuid to its deployment inputs. Call publishProject first. Each enabled app-callable " +
              "workflow in the result carries bindCommand, the bytechef CLI command that binds it to an app, and " +
              "every workflow carries draftWorkflowId, the id to pass to buildWorkflow for the next change.")
      public DeployProjectResult deployProject(
          @ToolParam(description = "The id of the project to deploy") long projectId,
          @ToolParam(
              required = false,
              description = "The DEVELOPMENT deployment to update; needed only when the project has several") @Nullable Long projectDeploymentId,
          @ToolParam(
              required = false,
              description = "Workflow uuids to enable; omitted, the app-callable workflows are enabled") @Nullable List<String> enabledWorkflows,
          @ToolParam(
              required = false,
              description = "An explicit connection per slot: workflowUuid, workflowNodeName, key, connectionId") @Nullable List<ConnectionChoice> connections,
          @ToolParam(
              required = false,
              description = "Deployment inputs per workflow uuid") @Nullable Map<String, Map<String, Object>> inputs,
          ToolContext toolContext) {

          ToolScope toolScope = ToolScope.of(toolContext);

          if (toolScope.refusal() != null) {
              return DeployProjectResult.refused(toolScope.refusal(), List.of());
          }

          long workspaceId = toolScope.workspaceId();

          ProjectDTO project = projectFacade.getProject(projectId);

          if (!Objects.equals(project.workspaceId(), workspaceId)) {
              return DeployProjectResult.refused(
                  "Project %d does not belong to workspace %d".formatted(projectId, workspaceId), List.of());
          }

          Long targetProjectDeploymentId = projectDeploymentId;

          if (targetProjectDeploymentId == null) {
              List<DeploymentChoice> deployments = getDevelopmentDeployments(workspaceId, projectId);

              if (deployments.size() > 1) {
                  return DeployProjectResult.refused(
                      "Project %d has several DEVELOPMENT deployments; pass projectDeploymentId".formatted(projectId),
                      deployments);
              }

              if (deployments.size() == 1) {
                  DeploymentChoice deploymentChoice = deployments.getFirst();

                  targetProjectDeploymentId = deploymentChoice.projectDeploymentId();
              }
          }

          DeployProjectResultDTO deployProjectResultDTO;

          try {
              deployProjectResultDTO = projectDeploymentFacade.deployProject(
                  projectId, targetProjectDeploymentId,
                  enabledWorkflows == null ? getAppCallableWorkflowUuids(project) : enabledWorkflows,
                  toConnectionChoiceDTOs(connections), toInputs(inputs));
          } catch (ConfigurationException | IllegalArgumentException exception) {
              return DeployProjectResult.refused(exception.getMessage(), List.of());
          }

          if (!deployProjectResultDTO.written()) {
              return toResult(STATUS_UNRESOLVED, UNRESOLVED_MESSAGE, deployProjectResultDTO, false);
          }

          long savedProjectDeploymentId = Objects.requireNonNull(deployProjectResultDTO.projectDeploymentId());

          try {
              projectDeploymentFacade.enableProjectDeployment(savedProjectDeploymentId, true);
          } catch (RuntimeException exception) {
              return toResult(
                  STATUS_ARMING_FAILED,
                  ("arming failed: %s. The configuration is saved and deployment %d is disabled; call deployProject " +
                      "again to retry.").formatted(exception.getMessage(), savedProjectDeploymentId),
                  deployProjectResultDTO, false);
          }

          return toResult(STATUS_DEPLOYED, getDeployedMessage(deployProjectResultDTO), deployProjectResultDTO, true);
      }

      @Tool(
          description = "List the DEVELOPMENT connections of the workspace: id, name, componentName, " +
              "connectionVersion, authorizationType, credentialStatus, aiProvider and usable (valid credentials and " +
              "the caller may use it). Never parameters or credentials. Use it to choose connectionId values for " +
              "deployProject and for app connection bindings; AI-provider connections never fill a deployment slot.")
      public ConnectionListResult listConnections(
          @ToolParam(
              required = false, description = "Only connections of this component, for example slack") @Nullable String componentName,
          ToolContext toolContext) {

          ToolScope toolScope = ToolScope.of(toolContext);

          if (toolScope.refusal() != null) {
              return new ConnectionListResult(toolScope.refusal(), List.of());
          }

          return new ConnectionListResult(
              null, appFacade.getConnections(toolScope.workspaceId(), Environment.DEVELOPMENT, componentName));
      }

      @Tool(
          description = "List the workflows an app can call in DEVELOPMENT: enabled Call-workflow workflows of " +
              "enabled deployments of the workspace, optionally of one project. Each: projectDeploymentId, " +
              "workflowUuid, workflowName, projectId, projectName, inputSchema. Bind one with " +
              "bytechef automation app bind workflow --workflow-key <KEY> --deployment-id <projectDeploymentId> " +
              "--workflow-uuid <workflowUuid>.")
      public CallableWorkflowListResult listAppCallableWorkflows(
          @ToolParam(required = false, description = "Only workflows of this project") @Nullable Long projectId,
          ToolContext toolContext) {

          ToolScope toolScope = ToolScope.of(toolContext);

          if (toolScope.refusal() != null) {
              return new CallableWorkflowListResult(toolScope.refusal(), List.of());
          }

          return new CallableWorkflowListResult(
              null, appWorkflowFacade.getCallableWorkflows(toolScope.workspaceId(), projectId));
      }

      static String suggestWorkflowKey(String workflowName) {
          String lowerCaseName = workflowName.toLowerCase(Locale.ROOT);

          Matcher nonKeyCharactersMatcher = NON_KEY_CHARACTERS.matcher(lowerCaseName);

          String underscored = nonKeyCharactersMatcher.replaceAll("_");

          Matcher edgeUnderscoresMatcher = EDGE_UNDERSCORES.matcher(underscored);

          String workflowKey = edgeUnderscoresMatcher.replaceAll("");

          if (workflowKey.isEmpty()) {
              return "workflow";
          }

          if (!Character.isLetter(workflowKey.charAt(0))) {
              workflowKey = "workflow_" + workflowKey;
          }

          return workflowKey.length() > WORKFLOW_KEY_MAX_LENGTH
              ? workflowKey.substring(0, WORKFLOW_KEY_MAX_LENGTH) : workflowKey;
      }

      private List<String> getAppCallableWorkflowUuids(ProjectDTO project) {
          Optional<ProjectVersion> lastPublishedProjectVersion = project.projectVersions()
              .stream()
              .filter(projectVersion -> projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED)
              .max(Comparator.comparingInt(ProjectVersion::getVersion));

          if (lastPublishedProjectVersion.isEmpty()) {
              return List.of();
          }

          ProjectVersion projectVersion = lastPublishedProjectVersion.get();

          return projectWorkflowFacade
              .getProjectVersionWorkflows(Objects.requireNonNull(project.id()), projectVersion.getVersion(), false)
              .stream()
              .filter(projectWorkflowDTO -> isAppCallable(projectWorkflowDTO.getId()))
              .map(ProjectWorkflowDTO::getWorkflowUuid)
              .toList();
      }

      private static String getDeployedMessage(DeployProjectResultDTO deployProjectResultDTO) {
          String message = "Deployed and enabled. Bind each app-callable workflow with its bindCommand; pass " +
              "draftWorkflowId to buildWorkflow for the next change.";

          if (deployProjectResultDTO.removedWorkflows()
              .isEmpty()) {

              return message;
          }

          String removedWorkflows = deployProjectResultDTO.removedWorkflows()
              .stream()
              .map(removedWorkflow -> "%s (%s)".formatted(removedWorkflow.name(), removedWorkflow.workflowUuid()))
              .collect(Collectors.joining(", "));

          return message + " Workflows removed since the previous deployment: " + removedWorkflows +
              ". App bindings to them are now unavailable; check `bytechef automation app status`.";
      }

      private List<DeploymentChoice> getDevelopmentDeployments(long workspaceId, long projectId) {
          return projectDeploymentFacade
              .getWorkspaceProjectDeployments(workspaceId, Environment.DEVELOPMENT.ordinal(), projectId, null)
              .stream()
              .filter(projectDeployment -> !SystemProjects.isSystemDeploymentName(projectDeployment.getName()))
              .map(projectDeployment -> new DeploymentChoice(
                  Objects.requireNonNull(projectDeployment.getId()), projectDeployment.getName()))
              .toList();
      }

      private boolean isAppCallable(String workflowId) {
          CallableWorkflowCheck callableWorkflowCheck = callableWorkflowChecker.check(workflowId);

          return callableWorkflowCheck.callable();
      }

      private static List<DeploymentConnectionChoiceDTO> toConnectionChoiceDTOs(
          @Nullable List<ConnectionChoice> connections) {

          if (connections == null) {
              return List.of();
          }

          return connections.stream()
              .map(connectionChoice -> new DeploymentConnectionChoiceDTO(
                  connectionChoice.workflowUuid(), connectionChoice.workflowNodeName(), connectionChoice.key(),
                  connectionChoice.connectionId()))
              .toList();
      }

      private static Map<String, Map<String, ?>> toInputs(@Nullable Map<String, Map<String, Object>> inputs) {
          if (inputs == null) {
              return Map.of();
          }

          return new HashMap<>(inputs);
      }

      private DeployProjectResult toResult(
          String status, String message, DeployProjectResultDTO deployProjectResultDTO, boolean enabled) {

          List<DeployedWorkflow> deployedWorkflows = new ArrayList<>();

          for (DeployProjectResultDTO.DeployedWorkflow deployedWorkflow : deployProjectResultDTO.workflows()) {
              CallableWorkflowCheck callableWorkflowCheck = callableWorkflowChecker.check(deployedWorkflow.workflowId());

              String bindCommand = null;

              if (enabled && deployedWorkflow.enabled() && callableWorkflowCheck.callable()) {
                  bindCommand = BIND_COMMAND.formatted(
                      suggestWorkflowKey(deployedWorkflow.name()), deployProjectResultDTO.projectDeploymentId(),
                      deployedWorkflow.workflowUuid());
              }

              deployedWorkflows.add(
                  new DeployedWorkflow(
                      deployedWorkflow.workflowUuid(), deployedWorkflow.name(), deployedWorkflow.enabled(),
                      callableWorkflowCheck.callable(), callableWorkflowCheck.inputSchema(),
                      deployedWorkflow.draftWorkflowId(), deployedWorkflow.connections(), bindCommand));
          }

          return new DeployProjectResult(
              status, message, deployProjectResultDTO.projectDeploymentId(), deployProjectResultDTO.projectVersion(),
              enabled, deployedWorkflows, deployProjectResultDTO.removedWorkflows(),
              deployProjectResultDTO.unresolvedConnections(), deployProjectResultDTO.missingInputs(), List.of());
      }

      public record ConnectionChoice(String workflowUuid, String workflowNodeName, String key, long connectionId) {
      }

      @SuppressFBWarnings("EI")
      public record CallableWorkflowListResult(@Nullable String error, List<AppCallableWorkflowDTO> workflows) {
      }

      @SuppressFBWarnings("EI")
      public record ConnectionListResult(@Nullable String error, List<AppWorkspaceConnectionDTO> connections) {
      }

      @SuppressFBWarnings("EI")
      public record DeployedWorkflow(
          String workflowUuid, String name, boolean enabled, boolean appCallable, @Nullable String inputSchema,
          @Nullable String draftWorkflowId, List<DeployedConnection> connections, @Nullable String bindCommand) {
      }

      public record DeploymentChoice(long projectDeploymentId, String name) {
      }

      @SuppressFBWarnings("EI")
      public record DeployProjectResult(
          String status, @Nullable String message, @Nullable Long projectDeploymentId,
          @Nullable Integer projectVersion, boolean enabled, List<DeployedWorkflow> workflows,
          List<RemovedWorkflow> removedWorkflows, List<UnresolvedConnection> unresolvedConnections,
          List<MissingInput> missingInputs, List<DeploymentChoice> deployments) {

          static DeployProjectResult refused(@Nullable String message, List<DeploymentChoice> deployments) {
              return new DeployProjectResult(
                  STATUS_REFUSED, message, null, null, false, List.of(), List.of(), List.of(), List.of(), deployments);
          }
      }

      private record ToolScope(long workspaceId, @Nullable String refusal) {

          static ToolScope of(@Nullable ToolContext toolContext) {
              AutomationToolInvocationContext automationToolInvocationContext =
                  AutomationToolInvocationContext.fromToolContext(toolContext);

              Long workspaceId = automationToolInvocationContext == null
                  ? null : automationToolInvocationContext.workspaceId();

              if (workspaceId == null) {
                  return new ToolScope(0, "Workspace context unavailable; pass workspaceId");
              }

              int environmentOrdinal = AutomationToolInvocationContext.resolveEnvironmentOrDefault(
                  automationToolInvocationContext);

              if (environmentOrdinal != Environment.DEVELOPMENT.ordinal()) {
                  return new ToolScope(workspaceId, DEVELOPMENT_ONLY);
              }

              return new ToolScope(workspaceId, null);
          }
      }
  }
  ```

  `AppLoopMcpContributorConfiguration.java` (EE header):

  ```java
  package com.bytechef.ee.automation.ai.tool;

  import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
  import com.bytechef.automation.ai.tool.AccessibleWorkspaceResolver;
  import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
  import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
  import com.bytechef.automation.configuration.facade.ProjectFacade;
  import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
  import com.bytechef.ee.automation.app.facade.AppFacade;
  import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
  import java.util.ArrayList;
  import java.util.List;
  import org.springframework.ai.support.ToolCallbacks;
  import org.springframework.ai.tool.ToolCallback;
  import org.springframework.beans.factory.ObjectProvider;
  import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;

  /**
   * Registers the Apps agent loop's {@code deployProject}, {@code listConnections} and {@code listAppCallableWorkflows}
   * on the management MCP server, each wrapped in {@link WorkspaceScopedFlatToolCallback}. Every collaborator is optional:
   * a deployment without the app module (the distributed {@code ai-copilot-app}) or on CE contributes nothing.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  @Configuration
  @ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
  public class AppLoopMcpContributorConfiguration {

      @Bean
      McpServerToolCallbackContributor appLoopMcpContributor(
          ObjectProvider<AppFacade> appFacadeProvider, ObjectProvider<AppWorkflowFacade> appWorkflowFacadeProvider,
          ObjectProvider<CallableWorkflowChecker> callableWorkflowCheckerProvider,
          ObjectProvider<ProjectDeploymentFacade> projectDeploymentFacadeProvider,
          ObjectProvider<ProjectFacade> projectFacadeProvider,
          ObjectProvider<ProjectWorkflowFacade> projectWorkflowFacadeProvider,
          AccessibleWorkspaceResolver accessibleWorkspaceResolver) {

          return () -> {
              AppFacade appFacade = appFacadeProvider.getIfAvailable();
              AppWorkflowFacade appWorkflowFacade = appWorkflowFacadeProvider.getIfAvailable();
              CallableWorkflowChecker callableWorkflowChecker = callableWorkflowCheckerProvider.getIfAvailable();
              ProjectDeploymentFacade projectDeploymentFacade = projectDeploymentFacadeProvider.getIfAvailable();
              ProjectFacade projectFacade = projectFacadeProvider.getIfAvailable();
              ProjectWorkflowFacade projectWorkflowFacade = projectWorkflowFacadeProvider.getIfAvailable();

              if (appFacade == null || appWorkflowFacade == null || callableWorkflowChecker == null ||
                  projectDeploymentFacade == null || projectFacade == null || projectWorkflowFacade == null) {

                  return List.of();
              }

              List<ToolCallback> toolCallbacks = new ArrayList<>();

              for (ToolCallback toolCallback : ToolCallbacks.from(
                  new AppLoopTools(
                      appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
                      projectWorkflowFacade))) {

                  toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, accessibleWorkspaceResolver));
              }

              return toolCallbacks;
          };
      }
  }
  ```

- [ ] **Step 5: Point the old deployment tools at `deployProject`.**

  `CreateProjectDeploymentToolCallback.DESCRIPTION` — replace its last sentence
  `The deployment\n        is created disabled by default — call toggleProjectDeployment to enable it after configuring\n        per-workflow connections in the UI.`
  with
  ```
  The deployment
          is created disabled by default — call toggleProjectDeployment to enable it after configuring
          per-workflow connections in the UI. For a DEVELOPMENT deployment an app will call, use deployProject
          instead: it writes a row, connections and inputs for every workflow.
  ```
  `PromoteWorkflowToolCallback.DESCRIPTION` — after `Returns {projectDeploymentId, projectId, projectVersion, environment, enabled}.`
  (before the closing `"""`), add the line
  `        For a DEVELOPMENT deployment an app will call, use deployProject instead.` (keep the `.formatted(` call).

- [ ] **Step 6: Run the tests to see them pass**

  Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.ee.automation.ai.tool.AppLoop*" > <scratchpad>/d1-t5-green.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.
  Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.automation.ai.tool.CreateProjectDeploymentToolCallbackTest" --tests "com.bytechef.automation.ai.tool.PromoteWorkflowToolCallbackTest" > <scratchpad>/d1-t5-green2.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.

- [ ] **Step 7: Format and check**

  Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:spotlessApply :server:libs:automation:automation-ai:automation-ai-tool:spotlessApply > <scratchpad>/d1-t5-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:ee:libs:automation:automation-ai:automation-ai-tool:check :server:libs:automation:automation-ai:automation-ai-tool:check > <scratchpad>/d1-t5-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t5-check.log`.
  Expected: `exit=0`, no FAILED line.

- [ ] **Step 8: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add server/ee/libs/automation/automation-ai/automation-ai-tool/build.gradle.kts \
    server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/AppLoopTools.java \
    server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/AppLoopMcpContributorConfiguration.java \
    server/ee/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/ee/automation/ai/tool/AppLoopToolsTest.java \
    server/ee/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/ee/automation/ai/tool/AppLoopMcpContributorConfigurationTest.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/CreateProjectDeploymentToolCallback.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/PromoteWorkflowToolCallback.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/CreateProjectDeploymentToolCallbackTest.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/PromoteWorkflowToolCallbackTest.java
  git commit -m "- Add the deployProject, listConnections and listAppCallableWorkflows management MCP tools"
  ```

---
### Task 6: `buildWorkflow` app-callable mode

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/util/WorkflowPersistCaptureUtils.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/WorkflowPersistCaptureUtilsTest.java`
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AppCallableDraftGuard.java`
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/AppCallableDraftGuardTest.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowTools.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowLifecycleTools.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ScriptTools.java`
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ScriptToolsTest.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowToolsTest.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallback.java`
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallbackAppCallableTest.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/McpServerToolCallbackContributorConfigurationTest.java`

**Interfaces:**
- Consumes: `CallableWorkflowChecker`, `CallableWorkflowCheck` (Task 1).
- Produces:
  - `WorkflowPersistCaptureUtils.withCaptureHolder(Map)` keeps an existing holder;
    `static List<String> WorkflowPersistCaptureUtils.getCapturedWorkflowIds(Map<String, Object> context)` — distinct
    captured `workflowId`s in capture order.
  - `AppCallableDraftGuard` (CE `@Component`): `static final String APP_CALLABLE_KEY = "bytechef.workflowEditor.appCallable"`,
    `void checkDraft(@Nullable ToolContext toolContext, String workflowId)` — no-op unless the flag is `true` in the
    context; then throws `IllegalStateException` naming the DRAFT `workflowId` when the workflow is not in its
    project's last version.
  - `ProjectWorkflowTools(AppCallableDraftGuard, ProjectService, ProjectWorkflowFacade, ProjectWorkflowService, UserService, WorkspaceFacade, ObjectProvider<WorkflowArtifactRecorder>, ObjectProvider<WorkflowTestConfigurationFacade>)`;
    `String deleteWorkflow(String workflowId, @Nullable ToolContext toolContext)`.
  - `ScriptTools(AppCallableDraftGuard, ProjectWorkflowFacade)`;
    `WorkflowInfo updateScriptComponentCode(String workflowId, String code, String scriptName, ToolContext toolContext)`.
  - `WorkspaceScopedSubAgentToolCallback(ToolCallback, AccessibleWorkspaceResolver, @Nullable CallableWorkflowChecker)`
    (the two-argument constructor stays and passes `null`). Success text appended to the reply:
    `"\n\nApp-callable: <id>, <id>. Publish the project, then call deployProject."`; failure: a tool error
    `{"error": "The workflow was saved but an app cannot call it. workflowId <id>: <problem>[; …][ | …]. Call buildWorkflow again with appCallable true, naming the workflowId, to fix it."}`.

- [ ] **Step 1: Write the failing capture-utils tests.** Append to `WorkflowPersistCaptureUtilsTest`:

  ```java
      @Test
      void testWithCaptureHolderKeepsAHolderThatIsAlreadyPresent() {
          Map<String, Object> outer = WorkflowPersistCaptureUtils.withCaptureHolder(Map.of());

          Map<String, Object> inner = WorkflowPersistCaptureUtils.withCaptureHolder(outer);

          capture(inner, "wf-uuid-1", 7L, 55L, "Inner");

          assertThat(inner.get(CAPTURE_KEY)).isSameAs(outer.get(CAPTURE_KEY));
          assertThat(WorkflowPersistCaptureUtils.getCapturedWorkflowIds(outer)).containsExactly("wf-uuid-1");
      }

      @Test
      void testGetCapturedWorkflowIdsIsDistinctInCaptureOrder() {
          Map<String, Object> context = WorkflowPersistCaptureUtils.withCaptureHolder(Map.of());

          capture(context, "wf-2", 7L, 56L, "Second");
          capture(context, "wf-1", 7L, 55L, "First");
          capture(context, "wf-2", 7L, 56L, "Second again");

          assertThat(WorkflowPersistCaptureUtils.getCapturedWorkflowIds(context)).containsExactly("wf-2", "wf-1");
      }

      @Test
      void testGetCapturedWorkflowIdsWithoutAHolderIsEmpty() {
          assertThat(WorkflowPersistCaptureUtils.getCapturedWorkflowIds(new HashMap<>())).isEmpty();
      }
  ```

- [ ] **Step 2: Run them to see them fail**

  Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests "com.bytechef.ai.copilot.tool.WorkflowPersistCaptureUtilsTest" > <scratchpad>/d1-t6-red1.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit (`getCapturedWorkflowIds` missing).

- [ ] **Step 3: Change `WorkflowPersistCaptureUtils`.** Add `import java.util.LinkedHashSet;` and `import java.util.Set;`.
  Replace `withCaptureHolder` with:

  ```java
      /**
       * Returns a copy of {@code forwardedContext} carrying a thread-safe capture holder so the subagent's
       * workflow-persist tools can record the ids they create. A holder already present is kept, so a wrapper that
       * installed one before delegating reads what the subagent recorded. The original map is left untouched.
       */
      public static Map<String, Object> withCaptureHolder(Map<String, Object> forwardedContext) {
          Map<String, Object> context = new HashMap<>(forwardedContext);

          context.putIfAbsent(
              PERSISTED_WORKFLOW_CAPTURE_KEY, Collections.synchronizedList(new ArrayList<Map<String, Object>>()));

          return context;
      }

      /**
       * The distinct {@code workflowId}s recorded in the context's capture holder, in the order they were first
       * recorded; empty when there is no holder.
       */
      public static List<String> getCapturedWorkflowIds(Map<String, Object> context) {
          Object holder = context.get(PERSISTED_WORKFLOW_CAPTURE_KEY);

          if (!(holder instanceof List<?> captures)) {
              return List.of();
          }

          Set<String> workflowIds = new LinkedHashSet<>();

          synchronized (captures) {
              for (Object capture : captures) {
                  if (capture instanceof Map<?, ?> entry && entry.get("workflowId") instanceof String workflowId) {
                      workflowIds.add(workflowId);
                  }
              }
          }

          return List.copyOf(workflowIds);
      }
  ```

  Run the Step 2 command again. Expected: `exit=0`.

- [ ] **Step 4: Write the failing guard and tool tests.**

  `AppCallableDraftGuardTest.java` (Apache header of its neighbours):

  ```java
  package com.bytechef.automation.ai.tool;

  import static org.assertj.core.api.Assertions.assertThatCode;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.Mockito.lenient;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.verifyNoInteractions;

  import com.bytechef.automation.configuration.domain.Project;
  import com.bytechef.automation.configuration.domain.ProjectVersion;
  import com.bytechef.automation.configuration.domain.ProjectWorkflow;
  import com.bytechef.automation.configuration.service.ProjectService;
  import com.bytechef.automation.configuration.service.ProjectWorkflowService;
  import java.time.Instant;
  import java.util.List;
  import java.util.Map;
  import java.util.UUID;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.springframework.ai.chat.model.ToolContext;

  /**
   * @author Ivica Cardic
   */
  class AppCallableDraftGuardTest {

      static final String WORKFLOW_UUID = "11111111-1111-4111-8111-111111111111";

      private AppCallableDraftGuard appCallableDraftGuard;
      private ProjectService projectService;
      private ProjectWorkflowService projectWorkflowService;

      @BeforeEach
      void beforeEach() {
          projectService = mock(ProjectService.class);
          projectWorkflowService = mock(ProjectWorkflowService.class);

          appCallableDraftGuard = new AppCallableDraftGuard(projectService, projectWorkflowService);
      }

      @Test
      void testWithoutTheFlagNothingIsLookedUp() {
          appCallableDraftGuard.checkDraft(new ToolContext(Map.of("other", true)), "wf-published");
          appCallableDraftGuard.checkDraft(null, "wf-published");

          verifyNoInteractions(projectService, projectWorkflowService);
      }

      @Test
      void testADraftWorkflowPasses() {
          stubPublishedAndDraft(projectService, projectWorkflowService);

          assertThatCode(() -> appCallableDraftGuard.checkDraft(appCallableContext(), "wf-draft"))
              .doesNotThrowAnyException();
      }

      @Test
      void testAPublishedWorkflowIsRefusedNamingTheDraft() {
          stubPublishedAndDraft(projectService, projectWorkflowService);

          assertThatThrownBy(() -> appCallableDraftGuard.checkDraft(appCallableContext(), "wf-published"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("wf-published")
              .hasMessageContaining("edit the DRAFT workflow wf-draft");
      }

      static ToolContext appCallableContext() {
          return new ToolContext(Map.of(AppCallableDraftGuard.APP_CALLABLE_KEY, true));
      }

      static void stubPublishedAndDraft(ProjectService projectService, ProjectWorkflowService projectWorkflowService) {
          Project project = new Project();

          project.setId(7L);
          project.setProjectVersions(
              List.of(
                  new ProjectVersion(1, ProjectVersion.Status.PUBLISHED.ordinal(), Instant.now(), "v1"),
                  new ProjectVersion(2)));

          lenient().when(projectService.getProject(7L))
              .thenReturn(project);
          lenient().when(projectWorkflowService.getWorkflowProjectWorkflow("wf-published"))
              .thenReturn(new ProjectWorkflow(7L, 1, "wf-published", UUID.fromString(WORKFLOW_UUID)));
          lenient().when(projectWorkflowService.getWorkflowProjectWorkflow("wf-draft"))
              .thenReturn(new ProjectWorkflow(7L, 2, "wf-draft", UUID.fromString(WORKFLOW_UUID)));
          lenient().when(projectWorkflowService.getLastWorkflowId(WORKFLOW_UUID))
              .thenReturn("wf-draft");
      }
  }
  ```

  The helper stubs leniently because `ProjectWorkflowToolsTest` (strict `MockitoExtension`) reuses it and each test
  reads only some of the stubs.

  `ScriptToolsTest.java` (Apache header):

  ```java
  package com.bytechef.automation.ai.tool;

  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.ArgumentMatchers.anyInt;
  import static org.mockito.ArgumentMatchers.anyString;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.never;
  import static org.mockito.Mockito.verify;

  import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
  import com.bytechef.automation.configuration.service.ProjectService;
  import com.bytechef.automation.configuration.service.ProjectWorkflowService;
  import com.bytechef.exception.ExecutionException;
  import org.junit.jupiter.api.Test;

  /**
   * @author Ivica Cardic
   */
  class ScriptToolsTest {

      @Test
      void testUpdateScriptComponentCodeRefusesAPublishedWorkflowInAppCallableMode() {
          ProjectService projectService = mock(ProjectService.class);
          ProjectWorkflowFacade projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
          ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

          AppCallableDraftGuardTest.stubPublishedAndDraft(projectService, projectWorkflowService);

          ScriptTools scriptTools = new ScriptTools(
              new AppCallableDraftGuard(projectService, projectWorkflowService), projectWorkflowFacade);

          assertThatThrownBy(() -> scriptTools.updateScriptComponentCode(
              "wf-published", "return 1;", "script_1", AppCallableDraftGuardTest.appCallableContext()))
                  .isInstanceOf(ExecutionException.class)
                  .hasMessageContaining("edit the DRAFT workflow wf-draft");

          verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
      }
  }
  ```

  `ProjectWorkflowToolsTest.java`: change `newTools()` to

  ```java
      private ProjectWorkflowTools newTools() {
          return new ProjectWorkflowTools(
              new AppCallableDraftGuard(projectService, projectWorkflowService), projectService, projectWorkflowFacade,
              projectWorkflowService, userService, workspaceFacade, recorderProvider, testConfigurationFacadeProvider);
      }
  ```
  and add (imports `static org.assertj.core.api.Assertions.assertThatThrownBy`,
  `static org.mockito.ArgumentMatchers.anyInt`, `static org.mockito.ArgumentMatchers.anyString`):

  ```java
      @Test
      void testUpdateWorkflowRefusesAPublishedWorkflowInAppCallableMode() {
          AppCallableDraftGuardTest.stubPublishedAndDraft(projectService, projectWorkflowService);

          ProjectWorkflowTools tools = newTools();

          assertThatThrownBy(
              () -> tools.updateWorkflow("wf-published", DEFINITION, AppCallableDraftGuardTest.appCallableContext()))
                  .isInstanceOf(ExecutionException.class)
                  .hasMessageContaining("edit the DRAFT workflow wf-draft");

          verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
      }

      @Test
      void testDeleteWorkflowRefusesAPublishedWorkflowInAppCallableMode() {
          AppCallableDraftGuardTest.stubPublishedAndDraft(projectService, projectWorkflowService);

          ProjectWorkflowTools tools = newTools();

          assertThatThrownBy(() -> tools.deleteWorkflow("wf-published", AppCallableDraftGuardTest.appCallableContext()))
              .isInstanceOf(ExecutionException.class)
              .hasMessageContaining("edit the DRAFT workflow wf-draft");

          verify(projectWorkflowFacade, never()).deleteWorkflow(anyString());
      }
  ```

  `WorkspaceScopedSubAgentToolCallbackAppCallableTest.java` (Apache header):

  ```java
  package com.bytechef.automation.ai.tool;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.ArgumentMatchers.anyLong;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.times;
  import static org.mockito.Mockito.verify;
  import static org.mockito.Mockito.when;

  import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.springframework.ai.chat.model.ToolContext;
  import org.springframework.ai.tool.ToolCallback;
  import org.springframework.ai.tool.definition.ToolDefinition;

  /**
   * The app-callable mode of the management MCP {@code buildWorkflow} wrapper, with the specialist replaced by a fake
   * that records what a real {@code ProjectWorkflowTools} persist would record in the capture holder.
   *
   * @author Ivica Cardic
   */
  class WorkspaceScopedSubAgentToolCallbackAppCallableTest {

      private static final String CAPTURE_KEY = "bytechef.workflowEditor.persistedWorkflows";

      private AccessibleWorkspaceResolver accessibleWorkspaceResolver;
      private CallableWorkflowChecker callableWorkflowChecker;
      private FakeSpecialist fakeSpecialist;

      @BeforeEach
      void beforeEach() {
          accessibleWorkspaceResolver = mock(AccessibleWorkspaceResolver.class);
          callableWorkflowChecker = mock(CallableWorkflowChecker.class);
          fakeSpecialist = new FakeSpecialist();

          when(accessibleWorkspaceResolver.isAccessible(anyLong())).thenReturn(true);
      }

      @Test
      void testWithoutACheckerTheSchemaHasNoAppCallable() {
          WorkspaceScopedSubAgentToolCallback toolCallback = new WorkspaceScopedSubAgentToolCallback(
              fakeSpecialist, accessibleWorkspaceResolver);

          ToolDefinition toolDefinition = toolCallback.getToolDefinition();

          assertThat(toolDefinition.inputSchema()).doesNotContain("appCallable");
      }

      @Test
      void testWithACheckerTheSchemaOffersAppCallable() {
          ToolDefinition toolDefinition = appCallableToolCallback().getToolDefinition();

          assertThat(toolDefinition.inputSchema()).contains("appCallable");
      }

      @Test
      void testAppCallableAppendsTheInstructionAndFlagsTheForwardedContextButForwardsOnlyTheRequest() {
          appCallableToolCallback().call(input(true));

          assertThat(fakeSpecialist.capturedInput)
              .startsWith("{\"request\":\"Build a greeting workflow")
              .contains("workflow/v1/newWorkflowCall")
              .contains("workflow/v1/responseToWorkflowCall")
              .doesNotContain("appCallable")
              .doesNotContain("workspaceId");
          assertThat(fakeSpecialist.capturedContext)
              .containsEntry(AppCallableDraftGuard.APP_CALLABLE_KEY, true)
              .containsKey(CAPTURE_KEY);
      }

      @Test
      void testWithoutAppCallableTheRequestAndContextAreUnchanged() {
          String result = appCallableToolCallback().call(input(false));

          assertThat(result).isEqualTo("Built it.");
          assertThat(fakeSpecialist.capturedInput).isEqualTo("{\"request\":\"Build a greeting workflow\"}");
          assertThat(fakeSpecialist.capturedContext).doesNotContainKey(AppCallableDraftGuard.APP_CALLABLE_KEY);
      }

      @Test
      void testACallableWorkflowPassesAndIsMarked() {
          fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

          when(callableWorkflowChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), "{}"));

          String result = appCallableToolCallback().call(input(true));

          assertThat(result)
              .startsWith("Built it.")
              .contains("App-callable: wf-1.")
              .contains("call deployProject");
      }

      @Test
      void testAManualTriggerFailsNamingTheWorkflowWhichStaysSaved() {
          fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

          when(callableWorkflowChecker.check("wf-1")).thenReturn(
              new CallableWorkflowCheck(
                  List.of("no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall"), null));

          String result = appCallableToolCallback().call(input(true));

          assertThat(result)
              .contains("\"error\"")
              .contains("The workflow was saved but an app cannot call it")
              .contains("workflowId wf-1: no Call-workflow trigger")
              .contains("Call buildWorkflow again");
      }

      @Test
      void testAnUnreadableSchemaFailsNamingTheWorkflow() {
          fakeSpecialist.persistedWorkflowIds = List.of("wf-1");

          when(callableWorkflowChecker.check("wf-1")).thenReturn(
              new CallableWorkflowCheck(List.of("unreadable inputSchema: The schema is not valid JSON"), "{not json"));

          assertThat(appCallableToolCallback().call(input(true)))
              .contains("workflowId wf-1: unreadable inputSchema");
      }

      @Test
      void testNothingPersistedPassesTheReplyThrough() {
          String result = appCallableToolCallback().call(input(true));

          assertThat(result).isEqualTo("Built it.");
      }

      @Test
      void testEachDistinctCapturedWorkflowIsCheckedOnceAndOnlyFailuresAreNamed() {
          fakeSpecialist.persistedWorkflowIds = List.of("wf-1", "wf-1", "wf-2");

          when(callableWorkflowChecker.check("wf-1")).thenReturn(new CallableWorkflowCheck(List.of(), "{}"));
          when(callableWorkflowChecker.check("wf-2")).thenReturn(
              new CallableWorkflowCheck(
                  List.of("no Call-workflow trigger: the trigger must be workflow/v1/newWorkflowCall"), null));

          String result = appCallableToolCallback().call(input(true));

          assertThat(result)
              .contains("workflowId wf-2")
              .doesNotContain("workflowId wf-1");

          verify(callableWorkflowChecker, times(1)).check("wf-1");
          verify(callableWorkflowChecker, times(1)).check("wf-2");
      }

      private WorkspaceScopedSubAgentToolCallback appCallableToolCallback() {
          return new WorkspaceScopedSubAgentToolCallback(
              fakeSpecialist, accessibleWorkspaceResolver, callableWorkflowChecker);
      }

      private static String input(boolean appCallable) {
          return "{\"request\": \"Build a greeting workflow\", \"workspaceId\": 42, \"appCallable\": " + appCallable +
              "}";
      }

      private static final class FakeSpecialist implements ToolCallback {

          private Map<String, Object> capturedContext;
          private String capturedInput;
          private List<String> persistedWorkflowIds = List.of();

          @Override
          public ToolDefinition getToolDefinition() {
              return ToolDefinition.builder()
                  .name("buildWorkflow")
                  .description("Builds workflows.")
                  .inputSchema(
                      "{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}, " +
                          "\"required\": [\"request\"]}")
                  .build();
          }

          @Override
          public String call(String toolInput) {
              return call(toolInput, null);
          }

          @Override
          @SuppressWarnings("unchecked")
          public String call(String toolInput, ToolContext toolContext) {
              capturedInput = toolInput;
              capturedContext = toolContext.getContext();

              Object holder = capturedContext.get(CAPTURE_KEY);

              if (holder instanceof List<?> captures) {
                  for (String workflowId : persistedWorkflowIds) {
                      Map<String, Object> entry = new HashMap<>();

                      entry.put("workflowId", workflowId);

                      ((List<Map<String, Object>>) captures).add(entry);
                  }
              }

              return "Built it.";
          }
      }
  }
  ```

- [ ] **Step 5: Run them to see them fail**

  Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:compileTestJava > <scratchpad>/d1-t6-red2.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit; `AppCallableDraftGuard` missing, constructor arity errors, the three-argument wrapper
  constructor missing.

- [ ] **Step 6: Add `AppCallableDraftGuard.java`** (Apache header):

  ```java
  package com.bytechef.automation.ai.tool;

  import com.bytechef.automation.configuration.domain.Project;
  import com.bytechef.automation.configuration.domain.ProjectWorkflow;
  import com.bytechef.automation.configuration.service.ProjectService;
  import com.bytechef.automation.configuration.service.ProjectWorkflowService;
  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.Map;
  import org.jspecify.annotations.Nullable;
  import org.springframework.ai.chat.model.ToolContext;
  import org.springframework.stereotype.Component;

  /**
   * Keeps {@code buildWorkflow}'s app-callable mode on DRAFT workflows. The management MCP wrapper sets
   * {@link #APP_CALLABLE_KEY} in the context it forwards; the workflow-writing tools the specialist calls then refuse a
   * workflow of a published version and name the DRAFT workflow with the same uuid, so an agent holding an id from
   * before {@code publishProject} cannot change what is deployed.
   *
   * @author Ivica Cardic
   */
  @Component
  public class AppCallableDraftGuard {

      public static final String APP_CALLABLE_KEY = "bytechef.workflowEditor.appCallable";

      private final ProjectService projectService;
      private final ProjectWorkflowService projectWorkflowService;

      @SuppressFBWarnings("EI")
      public AppCallableDraftGuard(ProjectService projectService, ProjectWorkflowService projectWorkflowService) {
          this.projectService = projectService;
          this.projectWorkflowService = projectWorkflowService;
      }

      public void checkDraft(@Nullable ToolContext toolContext, String workflowId) {
          if (!isAppCallable(toolContext)) {
              return;
          }

          ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);
          Project project = projectService.getProject(projectWorkflow.getProjectId());

          if (projectWorkflow.getProjectVersion() == project.getLastProjectVersion()) {
              return;
          }

          String draftWorkflowId = projectWorkflowService.getLastWorkflowId(projectWorkflow.getUuidAsString());

          throw new IllegalStateException(
              ("Workflow %s belongs to published version %d of project %d and cannot be edited; edit the DRAFT " +
                  "workflow %s instead").formatted(
                      workflowId, projectWorkflow.getProjectVersion(), projectWorkflow.getProjectId(),
                      draftWorkflowId));
      }

      static boolean isAppCallable(@Nullable ToolContext toolContext) {
          if (toolContext == null) {
              return false;
          }

          Map<String, Object> context = toolContext.getContext();

          return context != null && Boolean.TRUE.equals(context.get(APP_CALLABLE_KEY));
      }
  }
  ```

- [ ] **Step 7: Call the guard from the three writers.**

  `ProjectWorkflowTools.java`: add field `private final AppCallableDraftGuard appCallableDraftGuard;`, make it the first
  constructor parameter (`AppCallableDraftGuard appCallableDraftGuard, ProjectService projectService, …`) and assign it.
  - `updateWorkflow`: first statement inside `try {` becomes `appCallableDraftGuard.checkDraft(toolContext, workflowId);`
    followed by a blank line.
  - `deleteWorkflow`: the signature becomes
    ```java
        public String deleteWorkflow(
            @ToolParam(description = "The ID of the workflow to delete") String workflowId,
            @Nullable ToolContext toolContext) {
    ```
    and its first statement inside `try {` becomes `appCallableDraftGuard.checkDraft(toolContext, workflowId);`
    followed by a blank line.

  `ProjectWorkflowLifecycleTools.java`: `return delegate.deleteWorkflow(workflowId);` →
  `return delegate.deleteWorkflow(workflowId, null);` (the MCP-facing tool never runs in app-callable mode).

  `ScriptTools.java`: add field `private final AppCallableDraftGuard appCallableDraftGuard;`; the constructor becomes
  `public ScriptTools(AppCallableDraftGuard appCallableDraftGuard, ProjectWorkflowFacade projectWorkflowFacade)` with
  both assignments; `updateScriptComponentCode` gains a last parameter `ToolContext toolContext` (import
  `org.springframework.ai.chat.model.ToolContext`) and its first statement inside `try {` becomes
  `appCallableDraftGuard.checkDraft(toolContext, workflowId);` followed by a blank line.

  Both thrown `IllegalStateException`s are caught by the methods' existing `catch (Exception e)` and rethrown as
  `ExecutionException("Failed to update workflow: " + message, …)` / `("Failed to delete workflow: …")`, so the
  specialist model reads the refusal and the DRAFT id.

- [ ] **Step 8: Add the app-callable mode to `WorkspaceScopedSubAgentToolCallback.java`.**

  Imports to add: `com.bytechef.ai.copilot.tool.util.WorkflowPersistCaptureUtils`,
  `com.bytechef.automation.configuration.workflow.CallableWorkflowCheck`,
  `com.bytechef.automation.configuration.workflow.CallableWorkflowChecker`, `java.util.ArrayList`.

  In the class Javadoc replace the sentence beginning `Delegates must declare an input schema of exactly` through
  `Every delegate on this surface satisfies that today.` with:

  ```
   * schema of exactly {@code {"request": string}}: this wrapper re-serializes the delegate input as
   * {@code {"request": ...}} and forwards no other field. The wrapper may own inputs it never forwards: given a
   * {@link CallableWorkflowChecker} (only {@code buildWorkflow}'s wrapper on the management MCP, and only where the EE
   * checker exists), it adds an optional {@code appCallable} it consumes itself — it appends the Call-workflow
   * requirements to the request, flags the forwarded context for {@link AppCallableDraftGuard}, and checks every
   * workflow the specialist persisted, answering an error naming each one an app cannot call.
  ```
  (keep the `* Applies to the copilot-domain delegates …` and `{@code SubAgentToolCallback} was removed …` text before
  it).

  Add, after `INPUT_SCHEMA`:

  ```java
      private static final String APP_CALLABLE_INPUT_SCHEMA =
          """
              {
                  "type": "object",
                  "properties": {
                      "request": {
                          "type": "string",
                          "description": "The task for the specialist, plus any ids or decisions already resolved."
                      },
                      "workspaceId": {
                          "type": "integer",
                          "description": "Target workspace id. Optional when the account has exactly one workspace; otherwise required — an error response lists the candidates."
                      },
                      "environment": {
                          "type": "string",
                          "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional — defaults to DEVELOPMENT when omitted."
                      },
                      "appCallable": {
                          "type": "boolean",
                          "description": "Build or change the workflow so an app can call it: a workflow/v1/newWorkflowCall trigger with an inputSchema and a closing workflow/v1/responseToWorkflowCall task. Only DRAFT workflows are edited, and the result says whether every saved workflow is app-callable."
                      }
                  },
                  "required": ["request"]
              }""";

      private static final String APP_CALLABLE_INSTRUCTION =
          "\n\nApp-callable requirements: the workflow's trigger is workflow/v1/newWorkflowCall with an inputSchema " +
              "parameter holding a JSON Schema that describes every value the caller supplies. The workflow ends " +
              "with a workflow/v1/responseToWorkflowCall task carrying an outputSchema parameter. Edit only workflows " +
              "of the project's DRAFT version.";
  ```

  Fields and constructors:

  ```java
      private final ToolCallback delegate;
      private final AccessibleWorkspaceResolver accessibleWorkspaceResolver;
      private final @Nullable CallableWorkflowChecker callableWorkflowChecker;
      private final JsonMapper jsonMapper = new JsonMapper();

      public WorkspaceScopedSubAgentToolCallback(
          ToolCallback delegate, AccessibleWorkspaceResolver accessibleWorkspaceResolver) {

          this(delegate, accessibleWorkspaceResolver, null);
      }

      @SuppressFBWarnings("EI_EXPOSE_REP2")
      public WorkspaceScopedSubAgentToolCallback(
          ToolCallback delegate, AccessibleWorkspaceResolver accessibleWorkspaceResolver,
          @Nullable CallableWorkflowChecker callableWorkflowChecker) {

          this.delegate = delegate;
          this.accessibleWorkspaceResolver = accessibleWorkspaceResolver;
          this.callableWorkflowChecker = callableWorkflowChecker;
      }
  ```

  `getToolDefinition()` — choose the schema and extend the description:

  ```java
      @Override
      public ToolDefinition getToolDefinition() {
          ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

          String description = delegateToolDefinition.description() +
              " Supply workspaceId when the account has more than one workspace. Supply environment" +
              " (DEVELOPMENT, STAGING, or PRODUCTION) to target something other than DEVELOPMENT.";

          if (callableWorkflowChecker != null) {
              description += " Set appCallable to true to build a workflow an app can call.";
          }

          return ToolDefinition.builder()
              .name(delegateToolDefinition.name())
              .description(description)
              .inputSchema(callableWorkflowChecker == null ? INPUT_SCHEMA : APP_CALLABLE_INPUT_SCHEMA)
              .build();
      }
  ```

  In `call(String, ToolContext)`:
  - after `String request = input.request();` and its blank-check, add
    ```java
                CallableWorkflowChecker appCallableWorkflowChecker = Boolean.TRUE.equals(input.appCallable())
                    ? callableWorkflowChecker : null;
    ```
  - after the existing `forwardedContext.putAll(AgentToolInvocationContext.builder()…toToolContext());` statement add
    ```java
                if (appCallableWorkflowChecker != null) {
                    forwardedContext = WorkflowPersistCaptureUtils.withCaptureHolder(forwardedContext);

                    forwardedContext.put(AppCallableDraftGuard.APP_CALLABLE_KEY, true);
                }
    ```
  - replace the last two statements of the `try` block with
    ```java
                String delegateInput = jsonMapper.writeValueAsString(
                    Map.of("request", appCallableWorkflowChecker == null ? request : request + APP_CALLABLE_INSTRUCTION));

                String result = delegate.call(delegateInput, new ToolContext(forwardedContext));

                if (appCallableWorkflowChecker == null) {
                    return result;
                }

                return checkAppCallable(
                    result, WorkflowPersistCaptureUtils.getCapturedWorkflowIds(forwardedContext),
                    appCallableWorkflowChecker);
    ```
  - add the method:
    ```java
        private String checkAppCallable(
            String result, List<String> workflowIds, CallableWorkflowChecker appCallableWorkflowChecker) {

            if (workflowIds.isEmpty()) {
                return result;
            }

            List<String> failures = new ArrayList<>();

            for (String workflowId : workflowIds) {
                CallableWorkflowCheck callableWorkflowCheck = appCallableWorkflowChecker.check(workflowId);

                if (!callableWorkflowCheck.callable()) {
                    failures.add("workflowId " + workflowId + ": " + String.join("; ", callableWorkflowCheck.problems()));
                }
            }

            if (failures.isEmpty()) {
                return result + "\n\nApp-callable: " + String.join(", ", workflowIds) +
                    ". Publish the project, then call deployProject.";
            }

            return ToolErrors.toolError(
                jsonMapper,
                "The workflow was saved but an app cannot call it. " + String.join(" | ", failures) +
                    ". Call buildWorkflow again with appCallable true, naming the workflowId, to fix it.");
        }
    ```
  - the input record becomes
    `public record WorkspaceScopedInput(@Nullable String request, @Nullable Long workspaceId, @Nullable String environment, @Nullable Boolean appCallable) {`.

  `forwardedContext` is declared `Map<String, Object> forwardedContext = new HashMap<>();` today; it is reassigned
  now, so it must not be `final` (it is not).

- [ ] **Step 9: Wire the checker in `ToolCallbackContributorConfiguration.java`.** Add imports
  `com.bytechef.automation.configuration.workflow.CallableWorkflowChecker`; add a constant
  `private static final String BUILD_WORKFLOW_TOOL_NAME = "buildWorkflow";`; change the bean to

  ```java
      @Bean
      McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
          ObjectProvider<SkillsTools> skillsToolsProvider,
          IntelligentToolCatalog intelligentToolCatalog, AccessibleWorkspaceResolver accessibleWorkspaceResolver,
          ObjectProvider<CallableWorkflowChecker> callableWorkflowCheckerProvider) {

          return () -> {
              List<ToolCallback> toolCallbacks = new ArrayList<>();

              skillsToolsProvider.ifAvailable(
                  skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

              CallableWorkflowChecker callableWorkflowChecker = callableWorkflowCheckerProvider.getIfAvailable();

              toolCallbacks.addAll(
                  intelligentToolCatalog.getByNames(
                      INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                      (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(
                          toolCallback, accessibleWorkspaceResolver,
                          BUILD_WORKFLOW_TOOL_NAME.equals(definition.name()) ? callableWorkflowChecker : null),
                      SubAgentQuestionRenderer.PLAIN_TEXT));

              return toolCallbacks;
          };
      }
  ```

  In `McpServerToolCallbackContributorConfigurationTest` pass `emptyProvider()` as the new last argument at the three
  existing `copilotAgentToolCallbackContributor(` calls, and add (imports
  `com.bytechef.automation.configuration.workflow.CallableWorkflowChecker`):

  ```java
      @Test
      void contributesAppCallableOnBuildWorkflowOnlyWhenACheckerIsPresent() {
          IntelligentToolCatalog intelligentToolCatalog = catalogOf(
              intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"));

          McpServerToolCallbackContributor withChecker = configuration.copilotAgentToolCallbackContributor(
              emptyProvider(), intelligentToolCatalog, mock(AccessibleWorkspaceResolver.class),
              presentChecker(mock(CallableWorkflowChecker.class)));

          assertThat(withChecker.getToolCallbacks())
              .extracting(
                  toolCallback -> toolCallback.getToolDefinition()
                      .name(),
                  toolCallback -> toolCallback.getToolDefinition()
                      .inputSchema()
                      .contains("appCallable"))
              .containsExactlyInAnyOrder(
                  tuple("buildWorkflow", true),
                  tuple("writeScript", false));

          McpServerToolCallbackContributor withoutChecker = configuration.copilotAgentToolCallbackContributor(
              emptyProvider(), intelligentToolCatalog, mock(AccessibleWorkspaceResolver.class), emptyProvider());

          assertThat(withoutChecker.getToolCallbacks())
              .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                  .inputSchema()).doesNotContain("appCallable"));
      }

      @SuppressWarnings("unchecked")
      private static ObjectProvider<CallableWorkflowChecker> presentChecker(
          CallableWorkflowChecker callableWorkflowChecker) {

          ObjectProvider<CallableWorkflowChecker> provider = mock(ObjectProvider.class);

          when(provider.getIfAvailable()).thenReturn(callableWorkflowChecker);

          return provider;
      }
  ```
  and add `import static org.assertj.core.api.Assertions.tuple;`.

- [ ] **Step 10: Run the tests to see them pass**

  Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests "com.bytechef.automation.ai.tool.AppCallableDraftGuardTest" --tests "com.bytechef.automation.ai.tool.ScriptToolsTest" --tests "com.bytechef.automation.ai.tool.ProjectWorkflowToolsTest" --tests "com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleToolsTest" --tests "com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback*" > <scratchpad>/d1-t6-green.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.
  Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ai.copilot.config.McpServerToolCallbackContributorConfigurationTest" --tests "com.bytechef.ai.copilot.config.CopilotIntelligentToolContributorConfigurationTest" > <scratchpad>/d1-t6-green2.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`.

- [ ] **Step 11: Format and check**

  Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:spotlessApply :server:libs:automation:automation-ai:automation-ai-tool:spotlessApply :server:libs:ai:ai-copilot:ai-copilot-service:spotlessApply > <scratchpad>/d1-t6-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:automation:automation-ai:automation-ai-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check > <scratchpad>/d1-t6-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t6-check.log`.
  Expected: `exit=0`, no FAILED line. `automation-ai-tool:check` includes `toolContextWorkspaceScan`; the new flag key is
  not a workspace-id key, so it stays green.
  Also compile the modules that construct `ScriptTools`/`ProjectWorkflowTools` through Spring only:
  `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:compileTestJava :server:ee:libs:embedded:embedded-ai:embedded-ai-tool:compileJava > <scratchpad>/d1-t6-compile.log 2>&1`, then `echo "exit=$?"`. Expected: `exit=0`.

- [ ] **Step 12: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/util/WorkflowPersistCaptureUtils.java \
    server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/WorkflowPersistCaptureUtilsTest.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AppCallableDraftGuard.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/AppCallableDraftGuardTest.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowTools.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectWorkflowLifecycleTools.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ScriptTools.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ScriptToolsTest.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ProjectWorkflowToolsTest.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallback.java \
    server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallbackAppCallableTest.java \
    server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java \
    server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/McpServerToolCallbackContributorConfigurationTest.java
  git commit -m "- Add an app-callable mode to buildWorkflow on the management MCP"
  ```

---

### Task 7: The four management REST routes

**Files:**
- Modify: `server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml`
- Regenerate: `server/ee/libs/automation/automation-app/automation-app-public-rest/generated/**`
- Modify: `server/ee/libs/automation/automation-app/automation-app-public-rest/build.gradle.kts`
- Modify: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiController.java`
- Modify: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppWorkflowBindingApiController.java`
- Create: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/WorkspaceConnectionApiController.java`
- Create: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/config/AppManagementRestTestConfiguration.java`
- Create: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiControllerLookupIntTest.java`
- Create: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppWorkflowBindingApiControllerBindableWorkflowsIntTest.java`
- Create: `server/ee/libs/automation/automation-app/automation-app-public-rest/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/WorkspaceConnectionApiControllerIntTest.java`

**Interfaces:**
- Consumes: `AppFacade.getBindableConnections(long, String)`, `AppFacade.getConnections(long, Environment, String)`,
  `AppFacade.getComponentOperations(String, int)`, `AppWorkflowFacade.getBindableWorkflows(long)` (Task 4 and existing).
- Produces — **the contract Plan D2 copies**, identical to D2 Task 1's YAML (operationIds, schema names, field names):
  `GET /apps/{id}/bindable-connections?componentName=` (`getAppBindableConnections`, tag `app`, `[AppBindableConnection]`),
  `GET /apps/{id}/bindable-workflows` (`getAppBindableWorkflows`, tag `app-workflow-binding`, `[AppBindableWorkflow]`),
  `GET /apps/component-operations?componentName=&componentVersion=` (`getAppComponentOperations`, tag `app`,
  `AppComponentOperations`), `GET /connections?workspaceId=&componentName=` + `X-Environment`
  (`getWorkspaceConnections`, tag `workspace-connection`, `[AppWorkspaceConnection]`). Generated interfaces:
  `AppApi.getAppBindableConnections(Long id, String componentName)`,
  `AppApi.getAppComponentOperations(String componentName, Integer componentVersion)`,
  `AppWorkflowBindingApi.getAppBindableWorkflows(Long id)`,
  `WorkspaceConnectionApi.getWorkspaceConnections(Long workspaceId, String componentName, EnvironmentModel xEnvironment)`
  (required parameters first — the generator's `sortParamsByRequiredFlag`). Open the generated files to confirm the
  exact signatures before implementing them.

- [ ] **Step 1: Extend `openapi-management.yaml`.** Apply exactly the edits (b) through (g) of Plan D2 Task 1 Step 2
  (`docs/superpowers/plans/2026-10-08-apps-agent-loop-cli.md`), here to the server spec; do **not** apply its edit (a)
  (the server keeps its relative `Environment` `$ref`). For a reader without that file, the edits are:

  (b) after the two `app-workflow-binding` tag lines, insert
  ```yaml
    - name: "workspace-connection"
      description: "The Automation Workspace Connection Management Public API"
  ```
  (c) immediately before the line `  /apps/{id}:` insert
  ```yaml
    /apps/component-operations:
      get:
        description: "Get the names of the actions and agent tools a component version offers: the values the allow-lists of a connection binding accept."
        summary: "Get the operations of a component"
        tags:
          - "app"
        operationId: "getAppComponentOperations"
        security:
          - bearerAuth: []
        parameters:
          - name: "componentName"
            description: "The name of a component."
            in: "query"
            required: true
            schema:
              type: "string"
          - name: "componentVersion"
            description: "The version of the component."
            in: "query"
            required: true
            schema:
              type: "integer"
              format: "int32"
        responses:
          "200":
            description: "The names of the component's actions and agent tools."
            content:
              application/json:
                schema:
                  $ref: "#/components/schemas/AppComponentOperations"
          "400":
            $ref: "#/components/responses/badRequest"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "404":
            $ref: "#/components/responses/notFound"
          "409":
            $ref: "#/components/responses/conflict"
          "500":
            $ref: "#/components/responses/internalServerError"
  ```
  (d) immediately before the line `  /apps/{id}/connections:` insert
  ```yaml
    /apps/{id}/bindable-connections:
      get:
        description: "Get the connections that can be bound to an app: those of the app's workspace and environment that the caller can see. A connection is usable when its credentials are valid and the caller may use it."
        summary: "Get the bindable connections of an app"
        tags:
          - "app"
        operationId: "getAppBindableConnections"
        security:
          - bearerAuth: []
        parameters:
          - $ref: "#/components/parameters/appId"
          - name: "componentName"
            description: "Only connections of this component."
            in: "query"
            required: false
            schema:
              type: "string"
        responses:
          "200":
            description: "The bindable connections."
            content:
              application/json:
                schema:
                  type: "array"
                  items:
                    $ref: "#/components/schemas/AppBindableConnection"
          "400":
            $ref: "#/components/responses/badRequest"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "404":
            $ref: "#/components/responses/notFound"
          "409":
            $ref: "#/components/responses/conflict"
          "500":
            $ref: "#/components/responses/internalServerError"
  ```
  (e) immediately before the line `  /apps/{id}/workflows:` insert
  ```yaml
    /apps/{id}/bindable-workflows:
      get:
        description: "Get the deployed app-callable workflows that can be bound to an app."
        summary: "Get the bindable workflows of an app"
        tags:
          - "app-workflow-binding"
        operationId: "getAppBindableWorkflows"
        security:
          - bearerAuth: []
        parameters:
          - $ref: "#/components/parameters/appId"
        responses:
          "200":
            description: "The bindable workflows."
            content:
              application/json:
                schema:
                  type: "array"
                  items:
                    $ref: "#/components/schemas/AppBindableWorkflow"
          "400":
            $ref: "#/components/responses/badRequest"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "404":
            $ref: "#/components/responses/notFound"
          "409":
            $ref: "#/components/responses/conflict"
          "500":
            $ref: "#/components/responses/internalServerError"
  ```
  (f) immediately before the line `components:` insert
  ```yaml
    /connections:
      get:
        description: "Get the connections of a workspace in an environment. A connection never carries its parameters or credentials."
        summary: "Get the connections of a workspace"
        tags:
          - "workspace-connection"
        operationId: "getWorkspaceConnections"
        security:
          - bearerAuth: []
        parameters:
          - name: "workspaceId"
            description: "The id of a workspace."
            in: "query"
            required: true
            schema:
              type: "integer"
              format: "int64"
          - name: "componentName"
            description: "Only connections of this component."
            in: "query"
            required: false
            schema:
              type: "string"
          - name: "X-Environment"
            description: "The environment. When omitted, PRODUCTION is used."
            in: "header"
            required: false
            schema:
              $ref: "#/components/schemas/Environment"
        responses:
          "200":
            description: "The connections."
            content:
              application/json:
                schema:
                  type: "array"
                  items:
                    $ref: "#/components/schemas/AppWorkspaceConnection"
          "400":
            $ref: "#/components/responses/badRequest"
          "401":
            $ref: "#/components/responses/unauthorized"
          "403":
            $ref: "#/components/responses/forbidden"
          "404":
            $ref: "#/components/responses/notFound"
          "409":
            $ref: "#/components/responses/conflict"
          "500":
            $ref: "#/components/responses/internalServerError"
  ```
  (g) append after the `AppWorkflowBindingInput` schema (same indentation as the other schemas)
  ```yaml
      AppBindableConnection:
        description: "A connection that can be bound to an app."
        type: "object"
        properties:
          id:
            type: "integer"
            format: "int64"
          name:
            type: "string"
          componentName:
            type: "string"
          connectionVersion:
            type: "integer"
            format: "int32"
          componentVersion:
            type: "integer"
            format: "int32"
          usable:
            description: "Whether the connection's credentials are valid and the caller may use it."
            type: "boolean"
      AppBindableWorkflow:
        description: "A deployed app-callable workflow that can be bound to an app."
        type: "object"
        properties:
          projectDeploymentId:
            type: "integer"
            format: "int64"
          projectDeploymentName:
            type: "string"
          projectName:
            type: "string"
          workflowUuid:
            type: "string"
          workflowName:
            type: "string"
          description:
            type: "string"
      AppComponentOperations:
        description: "The names of the actions and agent tools of a component version."
        type: "object"
        properties:
          actionNames:
            type: "array"
            items:
              type: "string"
          toolNames:
            type: "array"
            items:
              type: "string"
      AppWorkspaceConnection:
        description: "A connection of a workspace. It never carries parameters or credentials."
        type: "object"
        properties:
          id:
            type: "integer"
            format: "int64"
          name:
            type: "string"
          componentName:
            type: "string"
          connectionVersion:
            type: "integer"
            format: "int32"
          authorizationType:
            type: "string"
          credentialStatus:
            type: "string"
          aiProvider:
            description: "Whether this is a virtual AI-provider connection, which never fills a deployment connection slot."
            type: "boolean"
          usable:
            description: "Whether the connection's credentials are valid and the caller may use it."
            type: "boolean"
  ```
  The block indentation above is shown shifted two spaces right inside this list item; in the file, path keys start in
  column 3 (`  /connections:`) and schema keys in column 5 (`    AppBindableConnection:`), like their neighbours.

  Confirm the server spec and D2's copy differ only in `Environment`:
  `diff server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml cli/clients/automation-app/openapi.yaml`
  (if Plan D2 has already landed); expected: one hunk, the `Environment` schema.

- [ ] **Step 2: Generate the code**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-public-rest:generateOpenAPI > <scratchpad>/d1-t7-gen.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`; new files under
  `generated/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/management/`:
  `WorkspaceConnectionApi.java`, `model/AppBindableConnectionModel.java`, `model/AppBindableWorkflowModel.java`,
  `model/AppComponentOperationsModel.java`, `model/AppWorkspaceConnectionModel.java`.
  Revert files whose only change is the regenerated `@Generated(date = …)` line:

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git diff --name-only -- server/ee/libs/automation/automation-app/automation-app-public-rest/generated | while read -r file; do
    if [ -z "$(git diff -U0 -- "$file" | grep '^[-+][^-+]' | grep -v '@Generated(')" ]; then git checkout -- "$file"; fi
  done
  git status --short server/ee/libs/automation/automation-app/automation-app-public-rest/generated
  ```
  Expected: modified `management/AppApi.java`, `management/AppWorkflowBindingApi.java`; the five new files untracked;
  nothing else. `.openapi-generator/FILES` is rewritten by the app-facing task that runs last and stays unchanged.

- [ ] **Step 3: Add the test dependencies.** In `automation-app-public-rest/build.gradle.kts`, add to the
  `testImplementation` block:
  ```kotlin
      testImplementation("org.springframework.security:spring-security-test")
      testImplementation(project(":server:libs:test:test-int-support"))
  ```

- [ ] **Step 4: Write the failing WebMvc IntTests.**

  `config/AppManagementRestTestConfiguration.java`:

  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */

  package com.bytechef.ee.automation.app.public_.web.rest.config;

  import com.bytechef.ee.automation.app.public_.web.rest.AppApiController;
  import com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowBindingApiController;
  import com.bytechef.ee.automation.app.public_.web.rest.WorkspaceConnectionApiController;
  import com.bytechef.jackson.config.JacksonConfiguration;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.context.annotation.Import;

  /**
   * The three management controllers the app loop's lookups go through, for {@code @WebMvcTest}s. Each test declares
   * {@code @MockitoBean}s for {@code AppFacade}, {@code AppWorkflowFacade} and {@code EnvironmentService}.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  @Configuration
  @Import({
      JacksonConfiguration.class, AppApiController.class, AppWorkflowBindingApiController.class,
      WorkspaceConnectionApiController.class
  })
  public class AppManagementRestTestConfiguration {
  }
  ```

  `AppApiControllerLookupIntTest.java`:

  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */

  package com.bytechef.ee.automation.app.public_.web.rest;

  import static org.mockito.Mockito.when;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
  import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
  import com.bytechef.ee.automation.app.facade.AppFacade;
  import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
  import com.bytechef.ee.automation.app.public_.web.rest.config.AppManagementRestTestConfiguration;
  import com.bytechef.platform.configuration.service.EnvironmentService;
  import java.util.List;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
  import org.springframework.security.access.AccessDeniedException;
  import org.springframework.security.test.context.support.WithMockUser;
  import org.springframework.test.context.ContextConfiguration;
  import org.springframework.test.context.TestPropertySource;
  import org.springframework.test.context.bean.override.mockito.MockitoBean;
  import org.springframework.test.web.servlet.MockMvc;

  /**
   * @version ee
   *
   * @author Ivica Cardic
   */
  @ContextConfiguration(classes = AppManagementRestTestConfiguration.class)
  @TestPropertySource(properties = "bytechef.edition=ee")
  @WebMvcTest(AppApiController.class)
  @WithMockUser
  class AppApiControllerLookupIntTest {

      @MockitoBean
      private AppFacade appFacade;

      @MockitoBean
      private AppWorkflowFacade appWorkflowFacade;

      @MockitoBean
      private EnvironmentService environmentService;

      @Autowired
      private MockMvc mockMvc;

      @Test
      void testGetAppBindableConnectionsPassesTheComponentFilterAndReturnsUsable() throws Exception {
          when(appFacade.getBindableConnections(1L, "slack")).thenReturn(
              List.of(
                  new AppBindableConnectionDTO(101L, "Slack A", "slack", 1, 2, true),
                  new AppBindableConnectionDTO(102L, "Slack B", "slack", 1, 2, false)));

          mockMvc.perform(get("/v1/apps/1/bindable-connections").param("componentName", "slack"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.length()").value(2))
              .andExpect(jsonPath("$[0].id").value(101))
              .andExpect(jsonPath("$[0].name").value("Slack A"))
              .andExpect(jsonPath("$[0].componentName").value("slack"))
              .andExpect(jsonPath("$[0].connectionVersion").value(1))
              .andExpect(jsonPath("$[0].componentVersion").value(2))
              .andExpect(jsonPath("$[0].usable").value(true))
              .andExpect(jsonPath("$[1].usable").value(false));
      }

      @Test
      void testGetAppBindableConnectionsWithoutAFilterPassesNull() throws Exception {
          when(appFacade.getBindableConnections(1L, null)).thenReturn(List.of());

          mockMvc.perform(get("/v1/apps/1/bindable-connections"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.length()").value(0));
      }

      @Test
      void testGetAppBindableConnectionsAccessDeniedIsForbiddenNotServerError() throws Exception {
          when(appFacade.getBindableConnections(1L, null)).thenThrow(new AccessDeniedException("Access Denied"));

          mockMvc.perform(get("/v1/apps/1/bindable-connections"))
              .andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      }

      @Test
      void testGetAppComponentOperationsReturnsActionAndToolNames() throws Exception {
          when(appFacade.getComponentOperations("slack", 2)).thenReturn(
              new AppComponentOperationsDTO(List.of("sendMessage"), List.of("sendMessageTool")));

          mockMvc.perform(
              get("/v1/apps/component-operations").param("componentName", "slack")
                  .param("componentVersion", "2"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.actionNames[0]").value("sendMessage"))
              .andExpect(jsonPath("$.toolNames[0]").value("sendMessageTool"));
      }

      @Test
      void testGetAppComponentOperationsAccessDeniedIsForbiddenNotServerError() throws Exception {
          when(appFacade.getComponentOperations("slack", 2)).thenThrow(new AccessDeniedException("Access Denied"));

          mockMvc.perform(
              get("/v1/apps/component-operations").param("componentName", "slack")
                  .param("componentVersion", "2"))
              .andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      }
  }
  ```

  `AppWorkflowBindingApiControllerBindableWorkflowsIntTest.java` (same header, imports and annotations, with
  `@WebMvcTest(AppWorkflowBindingApiController.class)`; DTO import `com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO`):

  ```java
  @ContextConfiguration(classes = AppManagementRestTestConfiguration.class)
  @TestPropertySource(properties = "bytechef.edition=ee")
  @WebMvcTest(AppWorkflowBindingApiController.class)
  @WithMockUser
  class AppWorkflowBindingApiControllerBindableWorkflowsIntTest {

      private static final String WORKFLOW_UUID = "5b3f6a1e-0c2d-4e8f-9a7b-1d2c3e4f5a6b";

      @MockitoBean
      private AppFacade appFacade;

      @MockitoBean
      private AppWorkflowFacade appWorkflowFacade;

      @MockitoBean
      private EnvironmentService environmentService;

      @Autowired
      private MockMvc mockMvc;

      @Test
      void testGetAppBindableWorkflowsMapsEveryField() throws Exception {
          when(appWorkflowFacade.getBindableWorkflows(1L)).thenReturn(
              List.of(
                  new AppBindableWorkflowDTO(
                      11L, "Refunds", "Refund project", WORKFLOW_UUID, "Approve refund", "Asks a manager")));

          mockMvc.perform(get("/v1/apps/1/bindable-workflows"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$[0].projectDeploymentId").value(11))
              .andExpect(jsonPath("$[0].projectDeploymentName").value("Refunds"))
              .andExpect(jsonPath("$[0].projectName").value("Refund project"))
              .andExpect(jsonPath("$[0].workflowUuid").value(WORKFLOW_UUID))
              .andExpect(jsonPath("$[0].workflowName").value("Approve refund"))
              .andExpect(jsonPath("$[0].description").value("Asks a manager"));
      }

      @Test
      void testGetAppBindableWorkflowsAccessDeniedIsForbiddenNotServerError() throws Exception {
          when(appWorkflowFacade.getBindableWorkflows(1L)).thenThrow(new AccessDeniedException("Access Denied"));

          mockMvc.perform(get("/v1/apps/1/bindable-workflows"))
              .andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      }
  }
  ```

  `WorkspaceConnectionApiControllerIntTest.java` (same header, imports and annotations, with
  `@WebMvcTest(WorkspaceConnectionApiController.class)`; extra imports
  `com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO`,
  `com.bytechef.platform.configuration.domain.Environment`):

  ```java
  @ContextConfiguration(classes = AppManagementRestTestConfiguration.class)
  @TestPropertySource(properties = "bytechef.edition=ee")
  @WebMvcTest(WorkspaceConnectionApiController.class)
  @WithMockUser
  class WorkspaceConnectionApiControllerIntTest {

      @MockitoBean
      private AppFacade appFacade;

      @MockitoBean
      private AppWorkflowFacade appWorkflowFacade;

      @MockitoBean
      private EnvironmentService environmentService;

      @Autowired
      private MockMvc mockMvc;

      @Test
      void testGetWorkspaceConnectionsPassesWorkspaceEnvironmentAndComponent() throws Exception {
          when(environmentService.getEnvironment("DEVELOPMENT")).thenReturn(Environment.DEVELOPMENT);
          when(appFacade.getConnections(1049L, Environment.DEVELOPMENT, "slack")).thenReturn(
              List.of(
                  new AppWorkspaceConnectionDTO(
                      101L, "Slack A", "slack", 1, "OAUTH2_AUTHORIZATION_CODE", "VALID", false, true),
                  new AppWorkspaceConnectionDTO(-300L, "OpenAI", "openai", 1, null, "VALID", true, false)));

          mockMvc.perform(
              get("/v1/connections").param("workspaceId", "1049")
                  .param("componentName", "slack")
                  .header("X-Environment", "DEVELOPMENT"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$[0].id").value(101))
              .andExpect(jsonPath("$[0].name").value("Slack A"))
              .andExpect(jsonPath("$[0].componentName").value("slack"))
              .andExpect(jsonPath("$[0].connectionVersion").value(1))
              .andExpect(jsonPath("$[0].authorizationType").value("OAUTH2_AUTHORIZATION_CODE"))
              .andExpect(jsonPath("$[0].credentialStatus").value("VALID"))
              .andExpect(jsonPath("$[0].aiProvider").value(false))
              .andExpect(jsonPath("$[0].usable").value(true))
              .andExpect(jsonPath("$[1].aiProvider").value(true))
              .andExpect(jsonPath("$[1].usable").value(false))
              .andExpect(jsonPath("$[0].parameters").doesNotExist());
      }

      @Test
      void testGetWorkspaceConnectionsAccessDeniedIsForbiddenNotServerError() throws Exception {
          when(environmentService.getEnvironment("DEVELOPMENT")).thenReturn(Environment.DEVELOPMENT);
          when(appFacade.getConnections(1049L, Environment.DEVELOPMENT, null))
              .thenThrow(new AccessDeniedException("Access Denied"));

          mockMvc.perform(
              get("/v1/connections").param("workspaceId", "1049")
                  .header("X-Environment", "DEVELOPMENT"))
              .andExpect(status().isForbidden())
              .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      }
  }
  ```

- [ ] **Step 5: Run them to see them fail**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-public-rest:compileTestJava > <scratchpad>/d1-t7-red.log 2>&1`, then `echo "exit=$?"`.
  Expected: non-zero exit — `WorkspaceConnectionApiController` does not exist yet. (The generated interfaces give the
  new operations default bodies answering 501, so `AppApiController` still compiles; the IntTests would fail on 501.)

- [ ] **Step 6: Implement the controllers.**

  `AppApiController.java` — imports `com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO`,
  `com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO`,
  `com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindableConnectionModel`,
  `com.bytechef.ee.automation.app.public_.web.rest.management.model.AppComponentOperationsModel`; add after
  `getAppBindings`:

  ```java
      @Override
      public ResponseEntity<List<AppBindableConnectionModel>> getAppBindableConnections(
          Long id, @Nullable String componentName) {

          return ResponseEntity.ok(
              appFacade.getBindableConnections(id, componentName)
                  .stream()
                  .map(this::toModel)
                  .toList());
      }

      @Override
      public ResponseEntity<AppComponentOperationsModel> getAppComponentOperations(
          String componentName, Integer componentVersion) {

          AppComponentOperationsDTO appComponentOperationsDTO = appFacade.getComponentOperations(
              componentName, componentVersion);

          return ResponseEntity.ok(
              new AppComponentOperationsModel()
                  .actionNames(appComponentOperationsDTO.actionNames())
                  .toolNames(appComponentOperationsDTO.toolNames()));
      }
  ```
  and, with the other `toModel` overloads:
  ```java
      private AppBindableConnectionModel toModel(AppBindableConnectionDTO appBindableConnectionDTO) {
          return new AppBindableConnectionModel()
              .id(appBindableConnectionDTO.id())
              .name(appBindableConnectionDTO.name())
              .componentName(appBindableConnectionDTO.componentName())
              .connectionVersion(appBindableConnectionDTO.connectionVersion())
              .componentVersion(appBindableConnectionDTO.componentVersion())
              .usable(appBindableConnectionDTO.usable());
      }
  ```

  `AppWorkflowBindingApiController.java` — imports `com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO`,
  `com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindableWorkflowModel`; add:

  ```java
      @Override
      public ResponseEntity<List<AppBindableWorkflowModel>> getAppBindableWorkflows(Long id) {
          return ResponseEntity.ok(
              appWorkflowFacade.getBindableWorkflows(id)
                  .stream()
                  .map(this::toModel)
                  .toList());
      }

      private AppBindableWorkflowModel toModel(AppBindableWorkflowDTO appBindableWorkflowDTO) {
          return new AppBindableWorkflowModel()
              .projectDeploymentId(appBindableWorkflowDTO.projectDeploymentId())
              .projectDeploymentName(appBindableWorkflowDTO.projectDeploymentName())
              .projectName(appBindableWorkflowDTO.projectName())
              .workflowUuid(appBindableWorkflowDTO.workflowUuid())
              .workflowName(appBindableWorkflowDTO.workflowName())
              .description(appBindableWorkflowDTO.description());
      }
  ```

  `WorkspaceConnectionApiController.java`:

  ```java
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */

  package com.bytechef.ee.automation.app.public_.web.rest;

  import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
  import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
  import com.bytechef.ee.automation.app.facade.AppFacade;
  import com.bytechef.ee.automation.app.public_.web.rest.management.WorkspaceConnectionApi;
  import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppWorkspaceConnectionModel;
  import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
  import com.bytechef.platform.annotation.ConditionalOnEEVersion;
  import com.bytechef.platform.configuration.domain.Environment;
  import com.bytechef.platform.configuration.service.EnvironmentService;
  import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
  import java.util.List;
  import org.jspecify.annotations.Nullable;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RestController;

  /**
   * The workspace connection listing of the automation public API, for the {@code bytechef automation connection list}
   * command and the app loop. Lives with the app management routes so the CLI has one client from one spec.
   *
   * <p>
   * {@link AppFacade#getConnections} owns authorization, so this controller carries no {@code @PreAuthorize}; extending
   * {@link AbstractAppApiController} answers a denial with 403 instead of the global handler's 500.
   *
   * @version ee
   *
   * @author Ivica Cardic
   */
  @RestController("com.bytechef.ee.automation.app.public_.web.rest.WorkspaceConnectionApiController")
  @RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
  @ConditionalOnCoordinator
  @ConditionalOnEEVersion
  public class WorkspaceConnectionApiController extends AbstractAppApiController implements WorkspaceConnectionApi {

      private final AppFacade appFacade;
      private final EnvironmentService environmentService;

      @SuppressFBWarnings("EI2")
      public WorkspaceConnectionApiController(AppFacade appFacade, EnvironmentService environmentService) {
          this.appFacade = appFacade;
          this.environmentService = environmentService;
      }

      @Override
      public ResponseEntity<List<AppWorkspaceConnectionModel>> getWorkspaceConnections(
          Long workspaceId, @Nullable String componentName, @Nullable EnvironmentModel xEnvironment) {

          Environment environment = environmentService.getEnvironment(
              xEnvironment == null ? null : xEnvironment.name());

          return ResponseEntity.ok(
              appFacade.getConnections(workspaceId, environment, componentName)
                  .stream()
                  .map(WorkspaceConnectionApiController::toModel)
                  .toList());
      }

      private static AppWorkspaceConnectionModel toModel(AppWorkspaceConnectionDTO appWorkspaceConnectionDTO) {
          return new AppWorkspaceConnectionModel()
              .id(appWorkspaceConnectionDTO.id())
              .name(appWorkspaceConnectionDTO.name())
              .componentName(appWorkspaceConnectionDTO.componentName())
              .connectionVersion(appWorkspaceConnectionDTO.connectionVersion())
              .authorizationType(appWorkspaceConnectionDTO.authorizationType())
              .credentialStatus(appWorkspaceConnectionDTO.credentialStatus())
              .aiProvider(appWorkspaceConnectionDTO.aiProvider())
              .usable(appWorkspaceConnectionDTO.usable());
      }
  }
  ```

  `EnvironmentModel` is generated for the `X-Environment` header of the existing `getApps` route; `AppApiController`
  already imports it from `…management.model`.

- [ ] **Step 7: Run the tests to see them pass**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-public-rest:testIntegration > <scratchpad>/d1-t7-int.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`; 9 tests in the three new IntTests. A 401 on every request means Spring Security's servlet
  configuration was pulled in without a user: the classes already carry `@WithMockUser`, so check that
  `spring-security-test` is on the test classpath (Step 3).
  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-public-rest:test > <scratchpad>/d1-t7-test.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0` (the existing standalone controller tests).

- [ ] **Step 8: Format and check**

  Run: `./gradlew :server:ee:libs:automation:automation-app:automation-app-public-rest:spotlessApply > <scratchpad>/d1-t7-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:ee:libs:automation:automation-app:automation-app-public-rest:check > <scratchpad>/d1-t7-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t7-check.log`.
  Expected: `exit=0`, no FAILED line. Spotless targets `src/*/java/**` only, so it does not touch `generated/`.

- [ ] **Step 9: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  R=server/ee/libs/automation/automation-app/automation-app-public-rest
  G=$R/generated/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/management
  git add $R/openapi-management.yaml $R/build.gradle.kts \
    $G/AppApi.java $G/AppWorkflowBindingApi.java $G/WorkspaceConnectionApi.java \
    $G/model/AppBindableConnectionModel.java $G/model/AppBindableWorkflowModel.java \
    $G/model/AppComponentOperationsModel.java $G/model/AppWorkspaceConnectionModel.java \
    $R/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiController.java \
    $R/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppWorkflowBindingApiController.java \
    $R/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/WorkspaceConnectionApiController.java \
    $R/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/config/AppManagementRestTestConfiguration.java \
    $R/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiControllerLookupIntTest.java \
    $R/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/AppWorkflowBindingApiControllerBindableWorkflowsIntTest.java \
    $R/src/test/java/com/bytechef/ee/automation/app/public_/web/rest/WorkspaceConnectionApiControllerIntTest.java
  git status --short $R
  git commit -m "- Add the bindable-connections, bindable-workflows, component-operations and connections management REST routes"
  ```
  Expected before the commit: `git status --short $R` shows nothing unstaged under `$R`.

---
### Task 8: End to end — `AppAgentLoopIntTest`

**Files:**
- Modify: `server/apps/server-app/build.gradle.kts` (two `testImplementation` lines)
- Create: `server/apps/server-app/src/test/java/com/bytechef/server/AppAgentLoopIntTest.java`

**Interfaces:**
- Consumes: everything above — `WorkspaceScopedSubAgentToolCallback(ToolCallback, AccessibleWorkspaceResolver, CallableWorkflowChecker)`
  and `ProjectWorkflowTools.updateWorkflow(String, String, ToolContext)` (Task 6), `AppLoopTools` (Task 5),
  `AppWorkflowFacade.bindWorkflow` and `AppService.create` (existing), the app lane `GET /api/app/v1/workflows` and
  `POST /api/app/v1/workflows/{workflowKey}` (existing, `Authorization: Bearer <app key>`).
- Produces: nothing for later tasks.

The specialist model is replaced by a fake `ToolCallback` that persists a definition through the real
`ProjectWorkflowTools.updateWorkflow` with the context the wrapper forwards, so the capture holder, the DRAFT guard and
the real `AppCallableWorkflowResolver` check all run. Copilot stays off in `server-app`'s `testint` profile (a
pgvector-backed store would need the `vector` extension the plain Postgres container lacks), so the test builds the
wrapper and `AppLoopTools` itself instead of going through the MCP server.

- [ ] **Step 1: Add the test dependencies.** In `server/apps/server-app/build.gradle.kts`, after
  `testImplementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))`, add:
  ```kotlin
      testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-tool"))
      testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
  ```
  (`automation-configuration-api` exports `platform-api` and `platform-configuration-api`; `automation-app-api` is
  exported by `automation-app-service`, already an `implementation` dependency.)

- [ ] **Step 2: Write the test.** `AppAgentLoopIntTest.java` (Apache header of `ServerApplicationIntTest.java`, lines
  1–15):

  ```java
  package com.bytechef.server;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.Mockito.mock;
  import static org.mockito.Mockito.when;

  import com.bytechef.automation.ai.tool.AccessibleWorkspaceResolver;
  import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
  import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
  import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
  import com.bytechef.automation.configuration.domain.Workspace;
  import com.bytechef.automation.configuration.dto.ProjectDTO;
  import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
  import com.bytechef.automation.configuration.facade.ProjectFacade;
  import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
  import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
  import com.bytechef.ee.automation.ai.tool.AppLoopTools;
  import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployProjectResult;
  import com.bytechef.ee.automation.ai.tool.AppLoopTools.DeployedWorkflow;
  import com.bytechef.ee.automation.app.domain.App;
  import com.bytechef.ee.automation.app.domain.AppHostingTarget;
  import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
  import com.bytechef.ee.automation.app.facade.AppFacade;
  import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
  import com.bytechef.ee.automation.app.service.AppService;
  import com.bytechef.platform.configuration.domain.Environment;
  import com.bytechef.platform.security.constant.AuthorityConstants;
  import com.bytechef.platform.security.domain.ResourceVisibility;
  import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
  import java.util.List;
  import java.util.Map;
  import java.util.Objects;
  import java.util.UUID;
  import org.junit.jupiter.api.AfterEach;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.springframework.ai.chat.model.ToolContext;
  import org.springframework.ai.tool.ToolCallback;
  import org.springframework.ai.tool.definition.ToolDefinition;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.boot.test.web.server.LocalServerPort;
  import org.springframework.context.annotation.Import;
  import org.springframework.http.HttpHeaders;
  import org.springframework.http.MediaType;
  import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import org.springframework.security.core.context.SecurityContextHolder;
  import org.springframework.web.client.RestClient;
  import tools.jackson.databind.JsonNode;
  import tools.jackson.databind.json.JsonMapper;

  /**
   * The Apps agent loop end to end: build an app-callable workflow, publish, deploy, bind it to an app and call it on
   * the app lane; then change it through the DRAFT id the deploy returned, publish and deploy again, and the same
   * binding runs the changed definition.
   *
   * @author Ivica Cardic
   */
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  @Import({
      PostgreSQLContainerConfiguration.class, ServerApplicationIntTest.ServerApplicationIntTestConfiguration.class
  })
  class AppAgentLoopIntTest {

      private static final String CALLABLE_DEFINITION = """
          {
            "label": "Greet",
            "inputs": [],
            "triggers": [
              {
                "name": "trigger_1",
                "type": "workflow/v1/newWorkflowCall",
                "parameters": {
                  "inputSchema": "{\\"type\\": \\"object\\", \\"properties\\": {\\"name\\": {\\"type\\": \\"string\\"}}}"
                }
              }
            ],
            "tasks": [
              {
                "name": "responseToWorkflowCall_1",
                "type": "workflow/v1/responseToWorkflowCall",
                "parameters": {
                  "outputSchema": "{\\"type\\": \\"object\\", \\"properties\\": {\\"greeting\\": {\\"type\\": \\"string\\"}}}",
                  "response": {"greeting": "%s"}
                }
              }
            ]
          }
          """;
      private static final String MANUAL_DEFINITION = """
          {"label": "Greet", "inputs": [],
           "triggers": [{"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}], "tasks": []}""";
      private static final String WORKFLOW_KEY = "greet";

      @Autowired
      private AppFacade appFacade;

      @Autowired
      private AppService appService;

      @Autowired
      private AppWorkflowFacade appWorkflowFacade;

      @Autowired
      private CallableWorkflowChecker callableWorkflowChecker;

      private final JsonMapper jsonMapper = new JsonMapper();

      @LocalServerPort
      private int port;

      @Autowired
      private ProjectDeploymentFacade projectDeploymentFacade;

      @Autowired
      private ProjectFacade projectFacade;

      @Autowired
      private ProjectWorkflowFacade projectWorkflowFacade;

      @Autowired
      private ProjectWorkflowTools projectWorkflowTools;

      @BeforeEach
      void beforeEach() {
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(
                      "admin@localhost.com", null, List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
      }

      @AfterEach
      void afterEach() {
          SecurityContextHolder.clearContext();
      }

      @Test
      void testBuildPublishDeployBindCallAndARebuildKeepsTheBinding() {
          long projectId = projectFacade.createProject(
              ProjectDTO.builder()
                  .name("App loop " + UUID.randomUUID())
                  .workspaceId(Workspace.DEFAULT_WORKSPACE_ID)
                  .build());

          String workflowId = projectWorkflowFacade.addWorkflow(projectId, MANUAL_DEFINITION)
              .getWorkflowId();

          FakeWorkflowSpecialist fakeWorkflowSpecialist = new FakeWorkflowSpecialist(projectWorkflowTools);

          WorkspaceScopedSubAgentToolCallback buildWorkflow = new WorkspaceScopedSubAgentToolCallback(
              fakeWorkflowSpecialist, accessibleWorkspaceResolver(), callableWorkflowChecker);

          fakeWorkflowSpecialist.target(workflowId, CALLABLE_DEFINITION.formatted("v1"));

          assertThat(buildWorkflow.call(buildWorkflowInput(workflowId)))
              .contains("App-callable: " + workflowId);

          projectFacade.publishProject(projectId, "v1", false);

          AppLoopTools appLoopTools = new AppLoopTools(
              appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
              projectWorkflowFacade);

          DeployProjectResult firstDeployment = appLoopTools.deployProject(
              projectId, null, null, null, null, developmentToolContext());

          assertThat(firstDeployment.status()).isEqualTo("deployed");
          assertThat(firstDeployment.enabled()).isTrue();

          DeployedWorkflow deployedWorkflow = firstDeployment.workflows()
              .getFirst();

          assertThat(deployedWorkflow.appCallable()).isTrue();
          assertThat(deployedWorkflow.bindCommand())
              .contains("--workflow-uuid " + deployedWorkflow.workflowUuid());

          long projectDeploymentId = Objects.requireNonNull(firstDeployment.projectDeploymentId());

          AppService.CreatedApp createdApp = appService.create(newApp());

          long appId = Objects.requireNonNull(createdApp.app()
              .getId());

          appWorkflowFacade.bindWorkflow(appId, WORKFLOW_KEY, projectDeploymentId, deployedWorkflow.workflowUuid());

          RestClient restClient = appRestClient(createdApp.secretKey());

          assertThat(isAvailable(restClient)).isTrue();
          assertThat(runGreeting(restClient)).isEqualTo("v1");

          String draftWorkflowId = Objects.requireNonNull(deployedWorkflow.draftWorkflowId());

          fakeWorkflowSpecialist.target(draftWorkflowId, CALLABLE_DEFINITION.formatted("v2"));

          assertThat(buildWorkflow.call(buildWorkflowInput(draftWorkflowId)))
              .contains("App-callable: " + draftWorkflowId);

          projectFacade.publishProject(projectId, "v2", false);

          DeployProjectResult secondDeployment = appLoopTools.deployProject(
              projectId, null, null, null, null, developmentToolContext());

          assertThat(secondDeployment.status()).isEqualTo("deployed");
          assertThat(secondDeployment.projectDeploymentId()).isEqualTo(projectDeploymentId);
          assertThat(appWorkflowFacade.getWorkflowBindings(appId))
              .singleElement()
              .extracting(AppWorkflowBindingDTO::available)
              .isEqualTo(true);
          assertThat(isAvailable(restClient)).isTrue();
          assertThat(runGreeting(restClient)).isEqualTo("v2");
      }

      private static AccessibleWorkspaceResolver accessibleWorkspaceResolver() {
          AccessibleWorkspaceResolver accessibleWorkspaceResolver = mock(AccessibleWorkspaceResolver.class);

          when(accessibleWorkspaceResolver.isAccessible(Workspace.DEFAULT_WORKSPACE_ID)).thenReturn(true);

          return accessibleWorkspaceResolver;
      }

      private RestClient appRestClient(String secretKey) {
          return RestClient.builder()
              .baseUrl("http://localhost:" + port)
              .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
              .build();
      }

      private static String buildWorkflowInput(String workflowId) {
          return """
              {"request": "Make workflow %s greet the caller", "workspaceId": %d, "appCallable": true}"""
              .formatted(workflowId, Workspace.DEFAULT_WORKSPACE_ID);
      }

      private static ToolContext developmentToolContext() {
          return new ToolContext(
              Map.of(
                  AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, Workspace.DEFAULT_WORKSPACE_ID,
                  AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY,
                  (long) Environment.DEVELOPMENT.ordinal()));
      }

      private boolean isAvailable(RestClient restClient) {
          String body = restClient.get()
              .uri("/api/app/v1/workflows")
              .retrieve()
              .body(String.class);

          for (JsonNode workflowNode : jsonMapper.readTree(body)) {
              if (WORKFLOW_KEY.equals(workflowNode.get("workflowKey")
                  .asString())) {

                  return workflowNode.get("available")
                      .asBoolean();
              }
          }

          return false;
      }

      private static App newApp() {
          App app = new App();

          app.setEnvironment(Environment.DEVELOPMENT);
          app.setHostingTarget(AppHostingTarget.VERCEL);
          app.setName("agent-loop-" + UUID.randomUUID());
          app.setOwnerUserId(1L);
          app.setVisibility(ResourceVisibility.WORKSPACE);
          app.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

          return app;
      }

      private String runGreeting(RestClient restClient) {
          String body = restClient.post()
              .uri("/api/app/v1/workflows/" + WORKFLOW_KEY)
              .contentType(MediaType.APPLICATION_JSON)
              .body("{\"input\": {\"name\": \"Ada\"}, \"waitSeconds\": 20}")
              .retrieve()
              .body(String.class);

          JsonNode workflowRunNode = jsonMapper.readTree(body);

          assertThat(workflowRunNode.get("status")
              .asString()).isEqualTo("COMPLETED");

          return workflowRunNode.get("result")
              .get("greeting")
              .asString();
      }

      private static final class FakeWorkflowSpecialist implements ToolCallback {

          private final ProjectWorkflowTools projectWorkflowTools;

          private String definition;
          private String workflowId;

          private FakeWorkflowSpecialist(ProjectWorkflowTools projectWorkflowTools) {
              this.projectWorkflowTools = projectWorkflowTools;
          }

          @Override
          public ToolDefinition getToolDefinition() {
              return ToolDefinition.builder()
                  .name("buildWorkflow")
                  .description("Builds workflows.")
                  .inputSchema(
                      "{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}, " +
                          "\"required\": [\"request\"]}")
                  .build();
          }

          @Override
          public String call(String toolInput) {
              return call(toolInput, null);
          }

          @Override
          public String call(String toolInput, ToolContext toolContext) {
              projectWorkflowTools.updateWorkflow(workflowId, definition, toolContext);

              return "Built the workflow.";
          }

          private void target(String workflowId, String definition) {
              this.definition = definition;
              this.workflowId = workflowId;
          }
      }
  }
  ```

  The job of a run is created by the real `PrincipalJobFacade` and executed in-process (the `server-app` test context
  runs the coordinator and worker on the in-memory broker). The `result` of a `responseToWorkflowCall` run is its
  `response` map — `WorkflowCallResults.fetchCallableResponseOutput`; if the assertion on `result.greeting` fails, read
  that method before changing the assertion: the expected shape is what the app SDK documents to apps.

- [ ] **Step 3: Run it**

  Run: `./gradlew :server:apps:server-app:testIntegration --tests "com.bytechef.server.AppAgentLoopIntTest" > <scratchpad>/d1-t8-int.log 2>&1`, then `echo "exit=$?"`.
  Expected: `exit=0`. This compiles and boots the whole server once (several minutes). A failure inside the first
  `buildWorkflow.call` naming `AccessDeniedException` means the admin authentication did not reach the facade; one in
  `runGreeting` with `409 APP_WORKFLOW_UNAVAILABLE` means the deployment was not armed or the binding does not resolve
  — read the `deployProject` result's `status`/`message` first.

- [ ] **Step 4: Prove the test can fail.** Temporarily change `CALLABLE_DEFINITION.formatted("v2")` in the second
  `fakeWorkflowSpecialist.target(...)` to `CALLABLE_DEFINITION.formatted("v1")` and run Step 3 again.
  Expected: non-zero exit; `runGreeting` returns `v1` where `v2` is expected. Restore the line and run Step 3 once more
  (expected `exit=0`).

- [ ] **Step 5: Format and check**

  Run: `./gradlew :server:apps:server-app:spotlessApply > <scratchpad>/d1-t8-spotless.log 2>&1`, then `echo "exit=$?"`.
  Run: `./gradlew --continue :server:apps:server-app:check > <scratchpad>/d1-t8-check.log 2>&1`, then `echo "exit=$?"`, then `grep '^> Task .* FAILED' <scratchpad>/d1-t8-check.log`.
  Expected: `exit=0`, no FAILED line.

- [ ] **Step 6: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add server/apps/server-app/build.gradle.kts \
    server/apps/server-app/src/test/java/com/bytechef/server/AppAgentLoopIntTest.java
  git commit -m "- Cover the app agent loop end to end"
  ```

---

### Task 9: Docs — the server half of the agent loop

**Files:**
- Modify: `.agents/apps.md` (line 13 and a new section before `## SDK`)
- Modify: `.agents/embedded-bridge.md` (line 123)

**Interfaces:** none.

- [ ] **Step 1: Replace `.agents/apps.md` line 13's sentence.** In the paragraph at lines 10–13 replace
  `The CLI/MCP deploy commands are a separate sub-project and
  do not exist yet.` with
  `Sub-project D, the coding-agent loop (design
  `docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md`), has its server half in the **Agent loop** section; the
  CLI, template and plugin half is Plan D2's.`

- [ ] **Step 2: Add the section** immediately before the line `## SDK`:

  ```markdown
  ## Agent loop (server half)

  A coding agent builds, deploys and lists app-callable workflows through the management MCP; it manages apps through
  the CLI (Plan D2), which reads the four routes below. Design: `docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md`.
  Plan: `docs/superpowers/plans/2026-10-08-apps-agent-loop-server.md`. DEVELOPMENT only: every tool refuses another
  `environment` with "The app loop runs in DEVELOPMENT only".

  - **One definition of callable.** `AppCallableWorkflowResolver` implements the CE SPI `CallableWorkflowChecker`
    (`automation-configuration-api`, `…configuration.workflow`): `check(workflowId)` returns the problems (no
    `workflow/v1/newWorkflowCall` trigger, unreadable `inputSchema`) and the schema. The resolver is keyed by
    `(workspaceId, environment)`, not an `App`, and its listing drops system deployments
    (`SystemProjects.isSystemDeploymentName`) and system projects, as `resolve` always did.
  - **`buildWorkflow` app-callable mode** lives on the management MCP wrapper, not the delegate:
    `WorkspaceScopedSubAgentToolCallback` gets the checker only for `buildWorkflow` and only where the EE bean exists
    (`ToolCallbackContributorConfiguration`), so `appCallable` is absent from CE and from every other surface. With it
    the wrapper appends the Call-workflow requirements to the request, installs the capture holder and the
    `AppCallableDraftGuard.APP_CALLABLE_KEY` flag in the forwarded context (`WorkflowPersistCaptureUtils.withCaptureHolder`
    keeps a holder already present), and checks every distinct captured `workflowId` afterwards: nothing captured
    passes the reply through; a failure is an error naming each id and leaves the workflow saved. Under the flag,
    `ProjectWorkflowTools.updateWorkflow`/`deleteWorkflow` and `ScriptTools.updateScriptComponentCode` refuse a
    workflow outside the project's DRAFT version and name the DRAFT id.
  - **`ProjectDeploymentFacade.deployProject`** owns the deploy rules behind one guard (`WORKFLOW_EDIT` on the project
    in DEVELOPMENT, plus `DEPLOYMENT_EDIT` when updating): one DEVELOPMENT deployment per project, updated in place so
    bindings `(deployment id, workflow uuid)` stay valid; refuses unpublished changes (the Data Sync rule over every
    workflow); a row for every workflow of the version (`checkProjectDeploymentWorkflows` deletes rows missing from its
    list); slots from `ComponentConnectionFacade.getWorkflowComponentConnections`; candidates from
    `WorkspaceConnectionFacade.getConnections` filtered to `VALID` + `CONNECTION_USE`, AI-provider connections never;
    all or nothing. It writes the deployment disabled; arming is the tool's separate `enableProjectDeployment`, whose
    failure leaves it disabled and is reported as `arming failed`. Its three new collaborators are `@Lazy` so the
    hand-assembled IntTest contexts that build the facade without them still boot.
  - **MCP tools** `deployProject`, `listConnections`, `listAppCallableWorkflows`: `AppLoopTools` (EE
    `automation-ai-tool`), registered by `AppLoopMcpContributorConfiguration` (`appLoopMcpContributor`) through
    `WorkspaceScopedFlatToolCallback`, contributed only when the app module is present. Refusals are returned as values
    (`status: refused`, `error`): the wrapper reduces a thrown exception to its class name.
  - **REST for the CLI** in `openapi-management.yaml`: `GET /apps/{id}/bindable-connections?componentName=`,
    `GET /apps/{id}/bindable-workflows`, `GET /apps/component-operations`, `GET /connections` (new
    `WorkspaceConnectionApiController`, tag `workspace-connection`). Facades own the guards
    (`AppFacade.getConnections`: `CONNECTION_VIEW` in the environment; `getCallableWorkflows`: `DEPLOYMENT_VIEW` in
    DEVELOPMENT); every controller extends `AbstractAppApiController` for 403 on a denial. `AppBindableConnectionDTO`
    gained `usable`; GraphQL `appBindableConnections` did not change.
  - **End to end:** `AppAgentLoopIntTest` (`server-app`) runs build → publish → deploy → bind → call, then a rebuild
    through `draftWorkflowId`, and the same binding runs the new definition.
  ```

- [ ] **Step 3: Fix `.agents/embedded-bridge.md` line 123.** Replace
  `Slots come from `WorkflowConnectionSlots` over every trigger and every` with
  `Slots come from `ComponentConnectionFacade.getWorkflowComponentConnections` over every trigger and every`.

- [ ] **Step 4: Check nothing still names the deleted class**

  Run: `grep -rn "WorkflowConnectionSlots" .agents CLAUDE.md server --include='*.md' --include='*.java'`
  Expected: no output.

- [ ] **Step 5: Commit**

  ```bash
  cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416
  git add .agents/apps.md .agents/embedded-bridge.md
  git commit -m "- docs - Describe the server half of the Apps agent loop"
  ```

---

## Self-review

- **Spec coverage (Plan D1).** CE SPI + EE implementation, resolver refactor and system-deployment filter → Task 1.
  `WorkflowConnectionSlots` moved → Task 2. `deployProject` facade (guard, deployment, version, unpublished changes,
  rows, slots, candidates, choice, inputs, all-or-nothing, write, removed workflows) → Task 3. `AppFacade.getConnections`,
  `getBindableConnections(appId, componentName)` + `usable`, `AppWorkflowFacade.getCallableWorkflows` and their guard
  tests → Task 4. MCP tools, contributor, default enabled set, several-deployments refusal, workspace mismatch,
  arming failure, bind command, `removedWorkflows` → `bytechef automation app status`, existing tools' descriptions →
  Task 5. `buildWorkflow` app-callable mode, capture holder, DRAFT-only refusals, contributor wiring, wrapper Javadoc
  amendment → Task 6. Four REST routes on D2's contract, `WorkspaceConnectionApiController`, generated code,
  `@WebMvcTest` IntTests with 403 → Task 7. `AppAgentLoopIntTest` → Task 8. `.agents/apps.md` D section → Task 9.
  Spec test list: `AppLoopTools` cases (Task 5; the four unpublished-change cases in Task 3 per Ruling 3);
  `ProjectDeploymentFacadeDeployProjectIntTest` cases incl. finding-21's (subset keeps rows, explicit without
  `CONNECTION_USE`, PRIVATE invisible, PRODUCTION / other project, `removedWorkflows`) and the pinned guard (Task 3);
  wrapper tests (Task 6); resolver + `WorkflowConnectionSlotsTest` move (Tasks 1–2); REST and facade guard tests
  (Tasks 4, 7); end to end (Task 8). The generated-instructions CE test is Plan D2's (Ruling 1).
- **Type consistency.** `DeployProjectResultDTO` nested records, `DeploymentConnectionChoiceDTO`,
  `CallableWorkflowCheck.callable()`, `AppCallableDraftGuard.APP_CALLABLE_KEY`, `AppWorkspaceConnectionDTO` and
  `AppCallableWorkflowDTO` field orders are the same in every task that uses them; REST field names equal D2's YAML.
- **Placeholders.** None; `<scratchpad>` is defined in Global Constraints.
