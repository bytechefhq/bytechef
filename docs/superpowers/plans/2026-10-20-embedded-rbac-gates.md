# Embedded RBAC Gates Implementation Plan (Plan B)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every internal embedded operation is authorised by embedded scope instead of `isTenantAdmin()`. Automation Hub projects and connected users' projects leave the automation side (decision 8). Embedded API keys are shared per environment (decision 9). Embedded run details are redacted without `EXECUTION_DETAILS_VIEW`.

**Architecture:** Each embedded resource type gets an ownership resolver, and an environment resolver where the resource lives in an environment. Both chain into the existing EE `PermissionServiceImpl` and return `ResourceOwner.ofEmbedded()`. Facades then gate with the same expression shapes automation uses (`hasPermission(#id, '<Type>', '<SCOPE>')`, `hasWorkflowScopeInEnvironment(..., DEVELOPMENT)`), plus the embedded built-ins from Plan A for environment-less and list operations.

**Tech Stack:** Spring Security method security (SpEL), Spring Data JDBC, JUnit 5, Mockito, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-10-19-embedded-rbac-design.md`. Plan A (`docs/superpowers/plans/2026-10-19-embedded-rbac-engine.md`) built the engine and lists the carry-overs this plan resolves.

## Global Constraints

- EE only. CE behaviour does not change, except that CE `PermissionServiceImpl` must tolerate several resolvers for one type.
- New files under `server/ee/` carry the ByteChef Enterprise license header and `@version ee`. Files under `server/libs/` carry the Apache header.
- No explanatory inline comments. Commits are `1051 <description>`. Never add a Co-Authored-By trailer.
- Verify each task with the full `check` of every module it touches. Integration tests need `DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock`.
- Tenant admins keep full access everywhere. A non-member (no `embedded_user` row, not a tenant admin) gains nothing.
- Connected-user decisions keep precedence, and their gate parts (`isConnectedUser()`, `isCurrentConnectedUser(#connectedUserId)`, `#externalUserId == authentication.name`) stay exactly as they are.
- A null environment never widens access. Plan A already makes a null `Environment` mean every environment. Task 1 extends that rule to an environment-bearing resource whose environment cannot be resolved.
- The coverage tests are not widened for embedded: `PermissionScopeTypeTest`, `ResourceOwnershipResolverTest` and `AutomationMethodSecurityConfigurationTest`. Every scope this plan enforces is removed from `KNOWN_UNENFORCED_SCOPES`.
- Gate expressions use the automation constant pattern `private static final String DEVELOPMENT = "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT";` (as in `ProjectWorkflowFacadeImpl`).
- The catalogue has no `CONNECTION_CREATE`. Embedded connection creation uses `CONNECTION_EDIT` (Editor, the same minimum role as the spec's create).

## Review Focus

1. **Tenant admin regression.** Every converted gate must still grant a tenant admin who has no `embedded_user` row. Each gate task's int test has a `testATenantAdminWithoutARowStillPasses` case.
2. **Non-member regression.** A plain tenant user with no embedded row must be denied on every converted gate. Each gate task's int test has a `testANonMemberIsDenied` case.
3. **Connected users.** Their paths (`or isConnectedUser()`, `isCurrentConnectedUser`) must keep working. Tasks 8, 9 and 10 each keep a connected-user case green.
4. **Environment-only members.** A member with a role only in Development is denied on a Production resource, by id and in lists, and on a list call whose `environmentId` is null. This is pinned in Task 13 and in each list gate's test.
5. **Startup with chained resolvers.** A second resolver for an existing type (`Job`, `TriggerExecution`, `Connection`, `McpServer`, `McpComponent`, `McpTool`) must not crash `ConnectedUserAccessDeciderImpl`, and its `resolveProjectId` must still answer. Task 1 fixes this and Task 13 pins it.

---

## Expression shapes (used by every gate task)

| Operation kind | Expression |
|---|---|
| Resource by id | `hasPermission(#<id>, '<Type>', '<SCOPE>')` (the resource's environment comes from its environment resolver; environment-less types use the environment-unaware answer) |
| Workflow write | `hasWorkflowScopeInEnvironment(#workflowId, 'WORKFLOW_<X>', " + DEVELOPMENT + ")` |
| Workflow read | `hasWorkflowScope(#workflowId, 'WORKFLOW_VIEW')` |
| Create a workflow under an integration | `hasResourceScopeInEnvironment(#integrationId, 'Integration', 'WORKFLOW_CREATE', " + DEVELOPMENT + ")` |
| Environment-less create or list | `hasEmbeddedScope('<SCOPE>')` |
| List or create in a given environment | `hasEmbeddedScopeInEnvironmentId('<SCOPE>', #environmentId)` or `hasEmbeddedScopeInEnvironment('<SCOPE>', #environment)` |
| Connected-user alternative kept | `... or isConnectedUser()` / `... or isCurrentConnectedUser(#connectedUserId)` appended unchanged |

New resource type tokens, each with a resolver from Tasks 4–6:

- Environment-less: `Integration`, `IntegrationWorkflow` (by `integration_workflow.id`), `AutomationWorkflowProject` (Hub project id), `AutomationWorkflowProjectWorkflow` (Hub workflow uuid).
- With an environment: `IntegrationInstanceConfiguration`, `IntegrationInstance`, `ConnectedUser`, `ConnectedUserProjectWorkflow`, `SigningKey`, `McpIntegrationInstanceConfiguration`, `McpIntegrationInstanceConfigurationWorkflow`, `McpIntegrationInstanceTool`.

Embedded chain members for existing tokens: `Connection`, `Job`, `TriggerExecution`, `McpServer`, `McpComponent`, `McpTool`, and `ApiKey` (Task 11).

---

### Task 1: Engine adjustments for chained and environment-bearing embedded resources

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserAccessDeciderImpl.java` (resolver maps)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java` (CE resolver maps)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceOwnershipResolver.java` (`ResourceOwner`, Javadoc "must be unique")
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/EmbeddedResourceResolver.java`, and `EmbeddedResourceResolverImpl` in embedded-configuration-service
- Modify: EE `PermissionServiceImpl` (`hasResourceScope`, `hasResourceScopeInEnvironment`, `hasWorkflowScope*`)
- Modify: `server/ee/.../automation-configuration-service/src/test/java/.../service/PermissionScopeRegistryTest.java`
- Test: `EETEST/service/PermissionServiceResourceTest.java`, `PermissionServiceTest.java`, `ConnectedUserAccessDeciderTest` (create beside the decider if absent)

**Interfaces:**
- Produces:

```java
record ResourceOwner(OptionalLong workspaceId, OptionalLong ownerUserId, boolean embedded, boolean embeddedProject) {
    // existing 2- and 3-arg constructors keep embeddedProject = false
    public static ResourceOwner ofEmbeddedProject();   // (empty, empty, true, true)
    // compact ctor: embeddedProject requires embedded
}

public interface EmbeddedResourceResolver {
    boolean isEmbeddedProject(long projectId);
    boolean isHubProject(long projectId);                // automation_workflow_project row exists
    boolean isIntegrationWorkflow(String workflowId);
    boolean isEmbeddedProjectWorkflow(String workflowId);
    boolean isHubProjectWorkflow(String workflowId);
}
```

- The two resolver maps become `Map<String, List<...>>` (`Collectors.groupingBy`) in both the decider and CE `PermissionServiceImpl`. The first answer that is not unknown (or not empty) wins, as in EE. In the decider, `resolveProjectId` returns the first present answer in the chain.
- In EE `PermissionServiceImpl`:
  - An owner with `embeddedProject()` denies every non-tenant-admin. This replaces the Plan A special case that keys on type `'Project'`.
  - An embedded owner whose type has at least one registered environment resolver, but whose environment did not resolve, is answered with `hasEmbeddedScopeInEveryEnvironment(scope)`.
  - An embedded owner of an environment-less type keeps `hasEmbeddedScope(scope)`.
- `hasWorkflowScope`, `hasWorkflowScope(…, environment)` and `hasWorkflowScopeIfProjectWorkflow`: a Hub project workflow (`isHubProjectWorkflow`) is answered like an integration workflow. A connected-user project workflow still denies.
- `PermissionScopeRegistryTest`: programmatic `.hasEmbeddedScope*(` literals count as embedded side. The literals of `.hasResourceScope(` and `hasPermission(` must be registered on at least one side, because the side depends on the resource.

- [ ] **Step 1: Write the failing tests:**

```java
@Test void testTwoResolversForOneTypeDoNotBreakTheConnectedUserDecider()      // decider built with 2 'Job' resolvers; resolveProjectId answers from the second when the first is empty
@Test void testCePermissionServiceAcceptsTwoResolversForOneType()
@Test void testAnEmbeddedProjectOwnerDeniesAMemberAndGrantsATenantAdmin()     // ofEmbeddedProject + member holding every embedded scope -> false; tenant admin -> true
@Test void testAnEnvironmentBearingEmbeddedResourceWithoutAnEnvironmentNeedsTheScopeEverywhere() // env resolver registered, returns empty; scope only in DEVELOPMENT -> false; in all three -> true
@Test void testAnEnvironmentLessEmbeddedResourceUsesTheUnawareAnswer()
@Test void testAHubProjectWorkflowIsAnsweredByTheEmbeddedScopeInDevelopment()
@Test void testAConnectedUserProjectWorkflowStillDenies()
@Test void testAProgrammaticEmbeddedScopeLiteralMustBeAnEmbeddedScope()        // registry test
```

- [ ] **Step 2: Run them and confirm they fail.** `./gradlew EECFG:test --tests '*PermissionService*' --tests '*PermissionScopeRegistryTest' EMBCFG:test --tests '*ConnectedUserAccessDecider*' CECFG:test --tests '*PermissionService*' -q`
- [ ] **Step 3: Implement.** In `EmbeddedResourceResolverImpl`, `isHubProject` and `isHubProjectWorkflow` read `automationWorkflowProjectRepository`. Keep the guard against recursion: use only repositories and unguarded services.
- [ ] **Step 4: Run the full `check` on EECFG, CECFG, EMBCFG and the CE configuration-api.**
- [ ] **Step 5: Commit** `1051 Chain resolvers everywhere, deny embedded project owners, and require every environment when an embedded resource's environment is unknown`

---

### Task 2: Decision 8, part 1: automation resolvers hand embedded projects to the embedded side

**Files:**
- Modify, in `CESVC/security/` unless noted:
  - `ProjectWorkflowOwnershipResolver`, `ProjectDeploymentOwnershipResolver`, `ProjectDeploymentWorkflowOwnershipResolver`, `TestJobOwnershipResolver`
  - `JobOwnershipResolver`, `TriggerExecutionOwnershipResolver` and `TriggerExecutionEnvironmentResolver` (automation-workflow-execution-service `security/`)
  - `ApprovalTaskOwnershipResolver` (automation-task-service)
  - `ApiCollectionOwnershipResolver` (EE automation-api-platform-configuration-service)
- Modify: EE `PermissionServiceImpl.canUseConnectionInWorkflow`
- Test: each resolver's test class (create beside it if absent), and `PermissionServiceResourceTest`

**Interfaces:**
- Consumes `EmbeddedResourceResolver` (Task 1) through `ObjectProvider`, as `ProjectOwnershipResolver` does.
- Rules:
  - A resource whose project is a connected-user project returns `ofEmbeddedProject()`.
  - For a Hub project:
    - `ProjectWorkflow` and `TestJob` return `ofEmbedded()`, so Hub workflow editing and testing use embedded `WORKFLOW_*`.
    - `Job` and `TriggerExecution` return `ofEmbedded()`.
    - `ProjectDeployment`, `ProjectDeploymentWorkflow`, `ApprovalTask` and `ApiCollection` return `ofEmbeddedProject()`.
  - A `TestJob` or `Job` of an integration workflow returns `ofEmbedded()`.
  - Jobs and trigger executions of connected-user projects return `ofEmbedded()`. Their runs are embedded executions, checked with `EXECUTION_*` in the deployment's environment, which the existing `JobEnvironmentResolver` already resolves.
  - `resolveProjectId` keeps answering for every resolver it answers for today.
- `TriggerExecutionEnvironmentResolver` answers only for `PlatformType.AUTOMATION` principals and is empty otherwise. Task 4b adds the embedded one.
- `canUseConnectionInWorkflow`:
  - For an integration or Hub workflow: the connection must be `PlatformType.EMBEDDED`, and the caller needs `hasEmbeddedScope('CONNECTION_VIEW', <connection environment>)`.
  - For a connected-user project workflow: deny non-tenant-admins.
  - The order stays: connected-user decider, skip, tenant admin, then this branch.

- [ ] **Step 1: Write the failing tests.** One per rule:
  - `testAConnectedUserProjectWorkflowResolvesToAnEmbeddedProject`
  - `testAHubProjectWorkflowResolvesToEmbedded`
  - `testAHubDeploymentResolvesToAnEmbeddedProject`
  - `testAnIntegrationTestJobResolvesToEmbedded`
  - `testAConnectedUserProjectJobResolvesToEmbeddedAndKeepsItsProjectId`
  - `testAnEmbeddedTriggerExecutionHasNoAutomationEnvironment`
  - `testADefaultWorkspaceEditorCannotUseAConnectedUserProjectWorkflow` (`PermissionServiceResourceTest`)
  - `testAnEmbeddedMemberUsesAnEmbeddedConnectionInAnIntegrationWorkflow`
  - `testAnEmbeddedConnectionInAnotherEnvironmentIsRefused`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the full `check` on every touched module.**
- [ ] **Step 5: Commit** `1051 Resolve Hub and connected-user project resources to the embedded side`

---

### Task 3: Decision 8, part 2: automation lists skip embedded projects

**Files:**
- Modify: `SystemProjects` (automation-configuration-api), so that `isSystemProjectName` and `isSystemProject` match both `"__EMBEDDED_AUTOMATION__"` and `"__EMBEDDED__"`. Add the constant `EMBEDDED_CONNECTED_USER_NAME_PREFIX = "__EMBEDDED__"`.
- Modify: `ProjectRepository.findProjectIdsByWorkspaceId` to exclude both prefixes. Escape `_` in the `LIKE` (`ESCAPE '\'`), and fix `CustomProjectRepositoryImpl` line 63 the same way.
- Modify: `ProjectDeploymentSearchAssetProvider`, `WorkflowSearchAssetProvider`, `ProjectCategoryFacadeImpl` and the AI-tool `ScopedProjectGuard` / `ProjectWorkspaceScope`, so that each uses the excluding query or `SystemProjects`.
- Modify: `ProjectWorkflowExecutionFacadeImpl.getWorkflowExecutions`. When `embedded == true`, the gate is `hasEmbeddedScopeInEnvironmentId('EXECUTION_VIEW', #environmentId)` and not the workspace scope. Write it as one SpEL ternary on `#embedded`, and check the parameter names.
- Test: `ProjectRepositoryIntTest` (or the existing repository int test), `SystemProjectsTest`, the search-provider tests, `ProjectWorkflowExecutionFacadeTest` and `PreAuthorizeAnnotationTest`.

- [ ] **Step 1: Write the failing tests:**
  - `testWorkspaceProjectIdsExcludeHubAndConnectedUserProjects` (seeds `__EMBEDDED_AUTOMATION__x`, `__EMBEDDED__ext`, and a project literally named `aEMBEDDEDb` that must stay)
  - `testDeploymentSearchSkipsConnectedUserDeployments`
  - `testWorkflowSearchSkipsEmbeddedProjects`
  - `testEmbeddedRunsNeedTheEmbeddedExecutionScope` (pin the ternary expression in `PreAuthorizeAnnotationTest`)
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the full `check` on CECFG, the configuration API, automation-search, automation-workflow-execution-service and the AI tool module.**
- [ ] **Step 5: Commit** `1051 Keep Hub and connected-user projects out of automation lists and search`

---

### Task 4a: Embedded resolvers for integrations, instances, connected users and connections

**Files:** create, in `EMB/security/` (embedded-configuration-service) unless noted:
- `IntegrationOwnershipResolver` (`"Integration"`)
- `IntegrationWorkflowOwnershipResolver` (`"IntegrationWorkflow"`)
- `AutomationWorkflowProjectOwnershipResolver` (`"AutomationWorkflowProject"`): Hub projects only, unknown otherwise
- `AutomationWorkflowProjectWorkflowOwnershipResolver` (`"AutomationWorkflowProjectWorkflow"`): keyed by workflow uuid; read the facade to confirm what `workflowUuid` is
- `IntegrationInstanceConfigurationOwnershipResolver` and `...EnvironmentResolver`
- `IntegrationInstanceOwnershipResolver` and `...EnvironmentResolver` (instance → configuration → environment)
- `ConnectedUserProjectWorkflowOwnershipResolver` and `...EnvironmentResolver`
- `EmbeddedConnectionOwnershipResolver` (`"Connection"`, `PlatformType.EMBEDDED` rows only)
- `ConnectedUserOwnershipResolver` and `ConnectedUserEnvironmentResolver`, in embedded-connected-user-service `security/`

Test: one `*ResolverTest` per class.

**Rules:**
- Every resolver returns `ofEmbedded()` for a row it owns and `unknown()` for a missing row or a row that belongs to another side.
- No resolver calls a gated facade.
- Environment resolvers return `Optional.empty()` only when the row is missing.

- [ ] **Step 1: Write the failing tests.** For each class:
  - `testResolvesAnEmbeddedRow`
  - `testAMissingRowIsUnknown`
  - `testAnotherSidesRowIsUnknown`, where the type is shared
  - `testTheEnvironmentComesFrom<Column|Parent>`, for environment resolvers
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the full `check` on EMBCFG, embedded-connected-user-service and CECFG.** `ResourceOwnershipResolverTest` must pass unchanged.
- [ ] **Step 5: Commit** `1051 Resolve embedded integrations, instances, connected users and connections`

### Task 4b: Embedded job and trigger execution resolvers

**Files:** create `EmbeddedJobOwnershipResolver`, `EmbeddedJobEnvironmentResolver`, `EmbeddedTriggerExecutionOwnershipResolver` and `EmbeddedTriggerExecutionEnvironmentResolver` in the embedded workflow execution service `security/` package. Add tests.

**Rules:**
- A job with an `EMBEDDED` principal (`fetchJobPrincipalId(jobId, PlatformType.EMBEDDED)`) is `ofEmbedded()`.
- Its environment comes from:
  - the integration instance's configuration, when an instance with that id exists and the job's workflow belongs to that configuration's integration version;
  - otherwise, the integration instance configuration with that id, for the jobs `createIntegrationInstanceConfigurationWorkflowJob` creates.
- If neither matches, or the two would give different environments, the environment is empty. Task 1 then makes the check require the scope in every environment.
- Trigger executions follow the same rule from `WorkflowExecutionId` (type `EMBEDDED`, `jobPrincipalId`).

- [ ] **Step 1: Write the failing tests:**
  - `testARuntimeJobTakesItsInstanceConfigurationsEnvironment`
  - `testAConfigurationTestJobTakesTheConfigurationsEnvironment`
  - `testAnAmbiguousPrincipalHasNoEnvironment`
  - `testAnAutomationJobIsUnknown`
  - `testAnEmbeddedTriggerExecutionTakesItsInstanceEnvironment`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the full `check` on the module and EECFG.**
- [ ] **Step 5: Commit** `1051 Resolve embedded jobs and trigger executions with their environment`

### Task 5: Embedded resolvers for MCP and signing keys

**Files:** create in embedded-ai-mcp-service `security/`:
- `EmbeddedMcpServerOwnershipResolver` (`"McpServer"`, `PlatformType.EMBEDDED` rows)
- `EmbeddedMcpComponentOwnershipResolver` and `EmbeddedMcpToolOwnershipResolver` (child → server → `EMBEDDED`)
- `McpIntegrationInstanceConfigurationOwnershipResolver` and `...EnvironmentResolver` (environment from the MCP server)
- `McpIntegrationInstanceConfigurationWorkflowOwnershipResolver` and `...EnvironmentResolver`
- `McpIntegrationInstanceToolOwnershipResolver` and `...EnvironmentResolver`

Create in embedded-security-service `security/`: `SigningKeyOwnershipResolver` (`ofEmbedded(userId)`) and `SigningKeyEnvironmentResolver`.

The existing MCP environment resolvers have no type guard, so they also serve embedded servers. Do not duplicate them.

- [ ] **Step 1: Write the same four test shapes as Task 4a**, per class.
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.** Use repositories, not the gated `McpServerService`.
- [ ] **Step 4: Run the full `check` on embedded-ai-mcp-service, embedded-security-service and EECFG.**
- [ ] **Step 5: Commit** `1051 Resolve embedded MCP resources and signing keys`

---

### Task 6: Gates for integrations and integration workflows

**Files:** modify `IntegrationFacadeImpl`, `IntegrationCategoryFacadeImpl`, `IntegrationTagFacadeImpl`, `IntegrationWorkflowFacadeImpl` and `WebhookTriggerTestAdminFacadeImpl`. Update their int tests, which stub `isTenantAdmin()`, and `PermissionScopeTypeTest` (remove the pins `INTEGRATION_CREATE`, `INTEGRATION_PUBLISH`, `INTEGRATION_SETTINGS` and `INTEGRATION_DELETE`). Test: `EmbeddedIntegrationGatesIntTest`, beside `EmbeddedUserFacadeIntTest`, with the real `PermissionServiceImpl` and method security.

| Method | Gate |
|---|---|
| `createIntegration` | `hasEmbeddedScope('INTEGRATION_CREATE')` |
| `deleteIntegration(id)` | `hasPermission(#id, 'Integration', 'INTEGRATION_DELETE')` |
| `getIntegration(id)`, `getIntegrationVersions(id)` | `hasPermission(#id, 'Integration', 'WORKFLOW_VIEW')` |
| `getIntegrations(...)`, `getIntegrationCategories()`, `getIntegrationTags()` | `hasEmbeddedScope('WORKFLOW_VIEW')` |
| `publishIntegration(id, ...)` | `hasPermission(#id, 'Integration', 'WORKFLOW_EDIT') and hasPermission(#id, 'Integration', 'INTEGRATION_PUBLISH')` |
| `updateIntegration(dto)` | `hasPermission(#integrationDTO.id, 'Integration', 'WORKFLOW_EDIT')` (use the actual parameter name and accessor) |
| `updateIntegrationTags(id, tags)` | `hasPermission(#id, 'Integration', 'WORKFLOW_EDIT')` |
| `addWorkflow(integrationId, ...)` | `hasResourceScopeInEnvironment(#integrationId, 'Integration', 'WORKFLOW_CREATE', DEVELOPMENT)` |
| `deleteWorkflow(workflowId)` | `hasWorkflowScopeInEnvironment(#workflowId, 'WORKFLOW_DELETE', DEVELOPMENT)` |
| `updateWorkflow(workflowId, ...)` | `hasWorkflowScopeInEnvironment(#workflowId, 'WORKFLOW_EDIT', DEVELOPMENT)` |
| `getIntegrationWorkflow(String workflowId)` | `hasWorkflowScope(#workflowId, 'WORKFLOW_VIEW')` |
| `getIntegrationWorkflow(long integrationWorkflowId)` | `hasPermission(#integrationWorkflowId, 'IntegrationWorkflow', 'WORKFLOW_VIEW')` |
| `updatePermissionExpression(integrationWorkflowId, ...)` | `hasPermission(#integrationWorkflowId, 'IntegrationWorkflow', 'INTEGRATION_SETTINGS')` |
| `getIntegrationWorkflows()` | `hasEmbeddedScope('WORKFLOW_VIEW')` |
| `getIntegrationWorkflows(integrationId)`, `getIntegrationVersionWorkflows(id, ...)` | `hasPermission(#<integrationId or id>, 'Integration', 'WORKFLOW_VIEW')` |
| `startWebhookTriggerTest`, `stopWebhookTriggerTest` | `hasWorkflowScopeInEnvironment(#workflowId, 'WORKFLOW_EDIT', DEVELOPMENT)` (replaces the class-level gate) |

- [ ] **Step 1: Write the failing tests.**
  - `PreAuthorizeAnnotationTest.testEmbeddedIntegrationGates` pins every row above with `assertEveryPublicOverloadIsGated` / `assertOverloadPreAuthorize`.
  - `EmbeddedIntegrationGatesIntTest` has these cases:
    - `testAViewerReadsButCannotCreate`
    - `testAnEditorBuildsWorkflowsOnlyWithADevelopmentRole` (an Editor in Production only is denied `updateWorkflow`)
    - `testOnlyAnAdminChangesPermissionExpressions`
    - `testATenantAdminWithoutARowStillPasses`
    - `testANonMemberIsDenied`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement**, and update the existing int tests so that they stub embedded scopes instead of `isTenantAdmin()`.
- [ ] **Step 4: Run the full `check` on EMBCFG, EECFG and CECFG.**
- [ ] **Step 5: Commit** `1051 Gate integrations and integration workflows on embedded scopes`

### Task 7: Gates for Automation Hub templates

**Files:** modify `AutomationWorkflowProjectAdminFacadeImpl`: replace the class-level gate and add `@ConditionalOnEEVersion`. Test: `PreAuthorizeAnnotationTest`, and `EmbeddedHubGatesIntTest`.

Let `P` stand for `hasPermission(#projectId, 'AutomationWorkflowProject', '<S>')`, and `W` for `hasPermission(#workflowUuid, 'AutomationWorkflowProjectWorkflow', '<S>')`. Workflow-write scopes are checked in Development: `hasResourceScopeInEnvironment(#workflowUuid, 'AutomationWorkflowProjectWorkflow', '<S>', DEVELOPMENT)`. Use the same form with `#projectId` for `createProjectWorkflow`.

| Method | Gate |
|---|---|
| `createProject(..., permissionExpression, ...)` | `hasEmbeddedScope('INTEGRATION_CREATE') and (#permissionExpression == null or #permissionExpression.isBlank() or hasEmbeddedScope('INTEGRATION_SETTINGS'))` |
| `createProjectWorkflow(projectId, definition, permissionExpression)` | `WORKFLOW_CREATE` in Development on `P`, `and` the same `INTEGRATION_SETTINGS` clause on `P` |
| `deleteProject` | `P INTEGRATION_DELETE` |
| `deleteProjectWorkflow` | `WORKFLOW_DELETE` in Development on `W` |
| `duplicateProject` | `P WORKFLOW_VIEW and hasEmbeddedScope('INTEGRATION_CREATE')` |
| `duplicateProjectWorkflow` | `WORKFLOW_CREATE` in Development on `W` |
| `getCategories`, `getProjects`, `getTags` | `hasEmbeddedScope('WORKFLOW_VIEW')` |
| `getProjectVersions` | `P WORKFLOW_VIEW` |
| `publishProject` | `P WORKFLOW_EDIT and P INTEGRATION_PUBLISH` |
| `updateProject` | `P INTEGRATION_SETTINGS` |
| `updateProjectWorkflow(workflowUuid, label, description)` | `WORKFLOW_EDIT` in Development on `W` |
| `updateProjectWorkflowPermissionExpression` | `W INTEGRATION_SETTINGS` |

- [ ] **Step 1: Write the failing tests.**
  - Pin every row in `PreAuthorizeAnnotationTest`.
  - `EmbeddedHubGatesIntTest` has these cases:
    - `testAnEditorEditsHubWorkflowsButNotWhoSeesThem`
    - `testCreatingWithAPermissionExpressionNeedsAnAdmin`
    - `testATenantAdminWithoutARowStillPasses`
    - `testANonMemberIsDenied`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the full `check` on EMBCFG and EECFG.**
- [ ] **Step 5: Commit** `1051 Gate Automation Hub templates on embedded scopes`

### Task 8: Gates for instance configurations, instances and app events

**Files:** modify `IntegrationInstanceConfigurationFacadeImpl`, `IntegrationInstanceFacadeImpl`, `IntegrationInstanceAdminFacadeImpl` and `AppEventFacadeImpl` (also add `@ConditionalOnEEVersion` to `AppEventFacadeImpl`). In `PermissionScopeTypeTest`, unpin `INTEGRATION_INSTANCE_CONFIGURATION_*` and `APP_EVENT_*`.

Let `C` stand for `hasPermission(#<id>, 'IntegrationInstanceConfiguration', '<S>')`, and `I` for `hasPermission(#integrationInstanceId, 'IntegrationInstance', '<S>')`.

| Method | Gate |
|---|---|
| `createIntegrationInstanceConfiguration(dto)` | `hasEmbeddedScopeInEnvironment('INTEGRATION_INSTANCE_CONFIGURATION_CREATE', #<dto>.environment)` |
| `createIntegrationInstanceConfigurationWorkflowJob(id, ...)` | `C _EDIT` |
| `deleteIntegrationInstanceConfiguration` | `C _DELETE` |
| `enableIntegrationInstanceConfiguration`, `enableIntegrationInstanceConfigurationWorkflow` | `C _EDIT` |
| `getIntegrationInstanceConfiguration(id)` | `C _VIEW` |
| `getIntegrationInstanceConfigurationTags()` | `hasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_VIEW')` |
| `getIntegrationInstanceConfigurations(environmentId, ...)` | `hasEmbeddedScopeInEnvironmentId('INTEGRATION_INSTANCE_CONFIGURATION_VIEW', #environmentId)` |
| `updateIntegrationInstanceConfiguration`, `updateIntegrationInstanceConfigurationTags`, `updateIntegrationInstanceConfigurationWorkflow` | `C _EDIT` (for the workflow update, use the workflow's `integrationInstanceConfigurationId`) |
| `getIntegrationInstanceConfigurationIntegrations` (`isAuthenticated()`) and the ungated `getIntegrationInstanceConfigurationIntegration` | Unchanged if only connected-user or public paths reach them. Otherwise gate them with `C _VIEW` (`... or isConnectedUser()`). Report which applies. |
| `deleteIntegrationInstance` | `I CONNECTED_USER_DELETE` |
| `enableIntegrationInstance`, `enableIntegrationInstanceWorkflowTriggers` (both overloads), admin `enableIntegrationInstanceWorkflow` | `I CONNECTED_USER_MANAGE` |
| `enableIntegrationInstanceWorkflow`, `updateIntegrationInstanceWorkflow` (currently `isTenantAdmin() or isConnectedUser()`) | `I CONNECTED_USER_MANAGE or isConnectedUser()` |
| `getIntegrationInstance(id)` | `hasPermission(#id, 'IntegrationInstance', 'CONNECTED_USER_VIEW')` |
| AppEvent `create` | `hasEmbeddedScope('APP_EVENT_CREATE')` |
| AppEvent `update` | `hasEmbeddedScope('APP_EVENT_EDIT')` |
| AppEvent `delete` | `hasEmbeddedScope('APP_EVENT_DELETE')` |
| AppEvent `getAppEvent`, `getAppEvents` | `hasEmbeddedScope('APP_EVENT_VIEW')` |

- [ ] **Step 1: Write the failing tests.**
  - Pin every row in `PreAuthorizeAnnotationTest`.
  - `EmbeddedInstanceGatesIntTest` has these cases:
    - `testAProductionEditorEditsOnlyProductionConfigurations`
    - `testAListWithoutAnEnvironmentNeedsTheScopeEverywhere`
    - `testAConnectedUserStillTogglesTheirInstanceWorkflow`
    - `testATenantAdminWithoutARowStillPasses`
    - `testANonMemberIsDenied`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement**, and update the existing facade int tests.
- [ ] **Step 4: Run the full `check` on EMBCFG, EECFG and CECFG.**
- [ ] **Step 5: Commit** `1051 Gate instance configurations, instances and app events on embedded scopes`

### Task 9: Gates for connected users and embedded connections

**Files:** modify these facades:
- `ConnectedUserFacadeImpl` (embedded-connected-user-service)
- `ConnectedUserProjectAdminFacadeImpl` (also add `@ConditionalOnEEVersion`)
- `ConnectedUserWorkflowReferenceAdminFacadeImpl`
- `ConnectedUserConnectionFacadeImpl`
- `ConnectionAdminFacadeImpl`: replace its class-level gate; keep `requireEmbedded`

Also replace `ConnectedUserFacadeTest`'s `isTenantAdmin()` pin with the new expressions, and in `PermissionScopeTypeTest` unpin `CONNECTED_USER_*`.

Let `U` stand for `hasPermission(#<connectedUserId or id>, 'ConnectedUser', '<S>')`, and `K` for `hasPermission(#id, 'Connection', '<S>')`.

| Method | Gate |
|---|---|
| `deleteConnectedUser` | `U CONNECTED_USER_DELETE` |
| `enableConnectedUser` | `U CONNECTED_USER_MANAGE` |
| `getConnectedUser`, `getConnectedUserDTO` | `U CONNECTED_USER_VIEW` |
| `getConnectedUsers`, `getConnectedUserDTOs` (`environmentId`) | `hasEmbeddedScopeInEnvironmentId('CONNECTED_USER_VIEW', #environmentId)` |
| `getConnectedUserProjects(connectedUserId, environment)` | `U CONNECTED_USER_VIEW` |
| `enableProjectWorkflow(connectedUserProjectWorkflowId, ...)` | `hasPermission(#connectedUserProjectWorkflowId, 'ConnectedUserProjectWorkflow', 'CONNECTED_USER_MANAGE')` |
| `deleteProjectWorkflow(connectedUserProjectWorkflowId)` | `... 'CONNECTED_USER_DELETE'` |
| `getReferences(...)` | `hasEmbeddedScope('CONNECTED_USER_VIEW')` |
| `createConnectedUserConnection`, `deleteConnectedUserConnection`, `reauthorizeConnectedUserConnection` | `U CONNECTED_USER_MANAGE or isCurrentConnectedUser(#connectedUserId)` |
| `getConnectedUserConnections(long connectedUserId, ...)` | `U CONNECTED_USER_VIEW or isCurrentConnectedUser(#connectedUserId)` |
| the two `#externalUserId == authentication.name` methods | unchanged |
| the three ungated `ConnectedUserConnectionFacadeImpl` methods | Leave them unchanged if no controller or GraphQL resolver reaches them; report the callers. Otherwise gate them with `U CONNECTED_USER_VIEW`. |
| `createConnection(connectionDTO, shared)` | `hasEmbeddedScopeInEnvironmentId('CONNECTION_EDIT', #connectionDTO.environmentId)` (check the accessor) |
| `deleteConnection` | `K CONNECTION_DELETE` |
| `getConnection` | `K CONNECTION_VIEW` |
| `getConnectionTags`, `getSharedConnectionIds` | `hasEmbeddedScope('CONNECTION_VIEW')` |
| `getConnections(..., environmentId, ...)` | `hasEmbeddedScopeInEnvironmentId('CONNECTION_VIEW', #environmentId)` |
| `updateConnection`, `updateConnectionTags` | `K CONNECTION_EDIT` |

- [ ] **Step 1: Write the failing tests.**
  - Pin every row in `PreAuthorizeAnnotationTest`.
  - `EmbeddedConnectedUserGatesIntTest` has these cases:
    - `testAViewerListsConnectedUsersOnlyInTheirEnvironment`
    - `testOnlyAnAdminDeletesAConnectedUser`
    - `testAConnectedUserStillManagesTheirOwnConnection`
    - `testAnEmbeddedEditorEditsAnEmbeddedConnectionButNotAnAutomationOne` (`requireEmbedded` still refuses)
    - `testATenantAdminWithoutARowStillPasses`
    - `testANonMemberIsDenied`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement**, and update the existing int tests. `AutomationMethodSecurityConfigurationTest`'s `GATED_DELEGATES` must still pass, now through method-level gates.
- [ ] **Step 4: Run the full `check` on EMBCFG, embedded-connected-user-service, EECFG and CECFG.**
- [ ] **Step 5: Commit** `1051 Gate connected users and embedded connections on embedded scopes`

### Task 10: Gates for embedded MCP

**Files:** modify these facades in embedded-ai-mcp-service:
- `EmbeddedMcpServerFacadeImpl`
- `McpIntegrationInstanceConfigurationFacadeImpl`
- `McpIntegrationInstanceConfigurationWorkflowFacadeImpl`
- `McpIntegrationInstanceToolFacadeImpl`
- `McpIntegrationInstanceWorkflowFacadeImpl`
- `ConnectedUserMcpServerFacadeImpl`

**Rules:**
- **Copy automation's minimum roles.** For every MCP server, component and tool operation, use the scope that automation's `McpServerFacadeImpl` and `McpServerServiceImpl` gate the same operation with. Key it by the embedded resource: `hasPermission(#mcpServerId, 'McpServer', ...)`, `'McpComponent'`, `'McpTool'`.
- **Creating a server:** `hasEmbeddedScopeInEnvironment('MCP_CREATE', #environment)`.
- **Static lists** (`getMcpComponentDefinitions`, `getEmbeddedMcpServerTags`): `hasEmbeddedScope('MCP_VIEW')`.
- **Lists that span environments** (`getEmbeddedMcpServers`, `getMcpIntegrationInstanceConfigurations()`, `getMcpIntegrationInstanceConfigurationWorkflows()`):
  - Gate them with `hasEmbeddedScope('MCP_VIEW')`.
  - The facade then keeps only rows whose environment satisfies `permissionService.hasEmbeddedScope("MCP_VIEW", environment)`.
  - A tenant admin keeps every row.
- **MCP integration instance configurations:**
  - `hasPermission(#id, 'McpIntegrationInstanceConfiguration', 'MCP_EDIT')` for writes and `'MCP_VIEW'` for reads.
  - `create(mcpServerId, integrationInstanceConfigurationId, ...)`: `hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT') and hasPermission(#integrationInstanceConfigurationId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')`.
  - The by-server list: `hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')`.
- **MCP integration instance configuration workflows:** the same, keyed by `'McpIntegrationInstanceConfigurationWorkflow'`.
  - `create(mcpIntegrationInstanceConfigurationId, ...)`: `hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', 'MCP_EDIT')`.
  - Tool-eligible lists: `hasPermission(#integrationId, 'Integration', 'WORKFLOW_VIEW')` and `hasPermission(#integrationInstanceConfigurationId, 'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')`.
- **`isTenantAdmin() or isConnectedUser()` gates:** become `hasPermission(#integrationInstanceId, 'IntegrationInstance', 'CONNECTED_USER_MANAGE') or isConnectedUser()`.
- **`ConnectedUserMcpServerFacadeImpl`:**
  - `getConnectedUserMcpServers(connectedUserId)`: `hasPermission(#connectedUserId, 'ConnectedUser', 'MCP_VIEW')`.
  - `deleteConnectedUserMcpServer`: `hasPermission(#mcpServerId, 'McpServer', 'MCP_DELETE')`.
  - `enableConnectedUserMcpServer`: `hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')`.
  - `enableMcpTool`: `hasPermission(#mcpIntegrationInstanceToolId, 'McpIntegrationInstanceTool', 'MCP_EDIT')`.

- [ ] **Step 1: Write the failing tests.**
  - Pin every gate in `PreAuthorizeAnnotationTest`.
  - `EmbeddedMcpGatesIntTest` has these cases:
    - `testTheServerListShowsOnlyEnvironmentsTheMemberCanView`
    - `testAnEditorEditsButOnlyAnAdminDeletesAServer` (or whatever the automation counterpart's roles are)
    - `testAConnectedUserStillTogglesTheirTools`
    - `testATenantAdminWithoutARowStillPasses`
    - `testANonMemberIsDenied`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement**, and update `EmbeddedMcpServerFacadeIntTest` and `McpIntegrationInstanceConfigurationWorkflowFacadeIntTest`.
- [ ] **Step 4: Run the full `check` on embedded-ai-mcp-service and EECFG.**
- [ ] **Step 5: Commit** `1051 Gate embedded MCP on embedded scopes`

### Task 11: Signing keys and embedded API keys (decision 9)

**Files:**
- Modify: `SigningKeyFacadeImpl`. Unpin `EMBEDDED_SETTINGS_MANAGE` in `PermissionScopeTypeTest`.
- Create: `EmbeddedApiKeyFacade` (embedded-security-api `facade/`) and `EmbeddedApiKeyFacadeImpl` (embedded-security-service `facade/`, `@Service @ConditionalOnEEVersion`). The implementation works on the unguarded `ApiKeyService` and is gated with `@PreAuthorize` on every method.
- Create: `EmbeddedApiKeyOwnershipResolver` (embedded-security-service `security/`, `"ApiKey"`). It answers only for rows whose `type` is `PlatformType.EMBEDDED` and returns `ofEmbedded(userId)`. A row whose type is null is a platform key, so this resolver returns `unknown()` for it. The existing `ApiKeyEnvironmentResolver` already resolves environments for embedded rows.
- Create: `embedded-api-key.graphqls` and `EmbeddedApiKeyGraphQlController` in `embedded-configuration-graphql`, a module the client codegen already reads (`@Controller @ConditionalOnCoordinator @ConditionalOnEEVersion`, delegating to the facade). Plan C points the Embedded API Keys page at it.
- Modify: `ApiKeyFacadeImpl.revokeAll(userId)` so it skips `EMBEDDED` keys. This is a lifecycle rule, not an authorisation check. Apart from that change, the shared `ApiKeyFacadeImpl` and its GraphQL keep today's rules, including tenant-admin-only for `EMBEDDED` keys created through them.
- Test: `PreAuthorizeAnnotationTest` (pins), `EmbeddedApiKeyFacadeIntTest`, `EmbeddedApiKeyOwnershipResolverTest`, `EmbeddedApiKeyGraphQlControllerTest`, `ApiKeyFacadeTest` (`revokeAll`) and `SigningKeyFacadeIntTest`.

**Interfaces:**

```java
public interface EmbeddedApiKeyFacade {
    String createApiKey(ApiKey apiKey);            // type EMBEDDED, userId = current user; returns the secret as ApiKeyFacade.create does
    void deleteApiKey(long id);
    ApiKey getApiKey(long id);
    List<ApiKey> getApiKeys(long environmentId);   // every EMBEDDED key of that environment, no owner filter
    ApiKey updateApiKey(ApiKey apiKey);            // name only, as ApiKeyFacade.update
}
```

GraphQL:

```graphql
extend type Query {
    embeddedApiKey(id: ID!): ApiKey
    embeddedApiKeys(environmentId: ID!): [ApiKey]
}
extend type Mutation {
    createEmbeddedApiKey(input: CreateApiKeyInput!): String
    deleteEmbeddedApiKey(id: ID!): Boolean
    updateEmbeddedApiKey(input: UpdateApiKeyInput!): ApiKey
}
```

Reuse the `ApiKey`, `CreateApiKeyInput` and `UpdateApiKeyInput` types from the platform `api-key.graphqls`. Check their exact names and do not redeclare them. If the input types carry a `type` field, the embedded mutations ignore it.

**Embedded API key gates:**

| Method | Gate |
|---|---|
| `createApiKey(apiKey)` | `hasEmbeddedScopeInEnvironment('API_KEY_CREATE', #apiKey.environment)` (check the accessor) |
| `getApiKeys(environmentId)` | `hasEmbeddedScopeInEnvironmentId('API_KEY_VIEW', #environmentId)` |
| `getApiKey(id)` | `hasPermission(#id, 'ApiKey', 'API_KEY_VIEW')` |
| `updateApiKey(apiKey)` | `hasPermission(#apiKey.id, 'ApiKey', 'API_KEY_CREATE')` (there is no edit scope for keys) |
| `deleteApiKey(id)` | `hasPermission(#id, 'ApiKey', 'API_KEY_DELETE')` |

The facade must also refuse a non-`EMBEDDED` key: `getApiKey`, `updateApiKey` and `deleteApiKey` throw the same "Api key not found" as `ApiKeyFacadeImpl` when the row is not `EMBEDDED`. A tenant admin, whose gate passes for any id, must not reach automation or platform keys this way.

**Signing key gates:**

| Method | Gate |
|---|---|
| `create(signingKey, type)` | `hasEmbeddedScopeInEnvironment('EMBEDDED_SETTINGS_MANAGE', #signingKey.environment)` |
| `delete(id)`, `getSigningKey(id)`, `update(signingKey)` | `hasPermission(#<id or signingKey.id>, 'SigningKey', 'EMBEDDED_SETTINGS_MANAGE')` |
| `getSigningKeys(type, environmentId)` | `hasEmbeddedScopeInEnvironmentId('EMBEDDED_SETTINGS_MANAGE', #environmentId)` |

- [ ] **Step 1: Write the failing tests:**
  - the embedded API key and signing key gate pins in `PreAuthorizeAnnotationTest`
  - `testAnEmbeddedEditorCreatesAnEmbeddedKeyInTheirEnvironment`
  - `testAnEmbeddedViewerSeesEveryEmbeddedKeyOfTheEnvironment`
  - `testAnEmbeddedKeyIsDeletableByScopeNotOnlyByItsOwner`
  - `testAProductionOnlyMemberCannotTouchADevelopmentKey`
  - `testTheEmbeddedFacadeRefusesAnAutomationOrPlatformKey` (also for a tenant admin)
  - `testANullTypeKeyIsNotClaimedByTheEmbeddedResolver`
  - `testRemovingAUserKeepsTheirEmbeddedKeys`
  - `testATenantAdminWithoutARowStillPasses`
  - `testANonMemberIsDenied`
  - `testASigningKeyNeedsAnAdminInItsEnvironment`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the full `check` on platform-security-service, embedded-security-api, embedded-security-service, embedded-configuration-graphql and EECFG.**
- [ ] **Step 5: Commit** `1051 Share embedded API keys per environment and gate signing keys on embedded settings`

### Task 12: Embedded executions: gates and run details

**Files:**
- Modify: `IntegrationWorkflowExecutionFacadeImpl`. Inject `PermissionService`, and add the constant `EXECUTION_DETAILS_VIEW`.
- Modify: the embedded `WorkflowExecutionDTO` (embedded-workflow-execution-api). Add `withoutDetails()` and `isTriggerOnly()`, mirroring the automation DTO and composed from `JobDTO`, `TaskExecutionDTO` and `TriggerExecutionDTO.withoutDetails()`.
- Test: `IntegrationWorkflowExecutionFacadeTest` (unit), `IntegrationWorkflowExecutionFacadeIntTest`, `WorkflowExecutionDTOTest`, and `PreAuthorizeAnnotationTest`.

| Method | Gate | Redaction |
|---|---|---|
| `getWorkflowExecution(id)` | `hasPermission(#id, 'Job', 'EXECUTION_VIEW')` | `hasResourceScope(id, "Job", EXECUTION_DETAILS_VIEW) ? dto : dto.withoutDetails()` |
| `getWorkflowExecutionTaskExecution(id, taskExecutionId)` | `hasPermission(#id, 'Job', 'EXECUTION_VIEW')` | without details, it skips reading task data and returns `withoutDetails()` |
| `getWorkflowExecutions(environmentId, ...)` | `hasEmbeddedScopeInEnvironmentId('EXECUTION_VIEW', #environmentId)` | per row: `isTriggerOnly()` rows are redacted unless the job check passes; otherwise memoise `hasEmbeddedScope(EXECUTION_DETAILS_VIEW, <row's configuration environment>)` per integration instance configuration |

The trigger input, output and state that `getTriggerExecutionDTO` always fills are redacted as well.

- [ ] **Step 1: Write the failing tests:**
  - `testAViewerSeesTheRunButNotItsInputsOutputsOrTrigger`
  - `testAnEditorSeesRunDetails`
  - `testRunListsRedactPerConfigurationEnvironment`
  - `testATaskExecutionIsNotReadWithoutDetails` (verify that no file storage read happens)
  - `testWithoutDetailsClearsEveryNestedField` (DTO test)
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the full `check` on the embedded workflow execution API and service modules and EECFG.**
- [ ] **Step 5: Commit** `1051 Gate embedded runs and hide their details without EXECUTION_DETAILS_VIEW`

### Task 13: Coverage, end-to-end permission test and the ticket

**Files:**
- Test: `EETEST/service/EmbeddedGatesIntTest.java` (real `PermissionServiceImpl`, method security, seeded rows)
- Modify: none in main code. A defect found here goes back to the task that owns it.

- [ ] **Step 1: Write the test.** Seed:
  - members: an embedded Viewer (everywhere), an embedded Editor in Development only, an embedded Admin in Production only, a default-workspace automation Editor with no embedded row, a tenant admin, and a connected user;
  - resources: one integration and one integration workflow, configurations in Development and Production, a Hub project, and a connected-user project.

```java
@Test void testTheAutomationEditorReachesNoEmbeddedResourceOrEmbeddedProject()
@Test void testTheDevelopmentEditorBuildsWorkflowsButCannotTouchProduction()
@Test void testTheProductionAdminManagesOnlyProduction()
@Test void testTheViewerReadsEverywhereAndChangesNothing()
@Test void testTheTenantAdminPassesEveryConvertedGate()
@Test void testTheConnectedUserKeepsTheirOwnPaths()
@Test void testNoEmbeddedFacadeStillGatesOnTenantAdminAlone()   // source scan: no "isTenantAdmin()"-only gate remains in server/ee/libs/embedded main, except a listed set of genuinely tenant-level operations (expected empty; any entry needs a reason)
@Test void testNoEmbeddedScopeIsPinnedAsUnenforced()            // KNOWN_UNENFORCED_SCOPES contains no embedded-side scope
@Test void testTheApplicationContextStartsWithChainedResolvers() // context with the embedded and automation resolvers for Job/Connection/McpServer and ConnectedUserAccessDeciderImpl
```

- [ ] **Step 2: Run it, then run the full `check` on every module Plan B touched** (one log; grep for `FAILED` and `BUILD`).
- [ ] **Step 3: Start the server once on a fresh database** (`SERVER_PORT=9556`, a dedicated database). Confirm it starts, then stop it by its PID and drop the database.
- [ ] **Step 4: Update #1051.** Mark the embedded catalogue enforced, add the embedded gate rules and the "Results after adding the embedded gates (date)" block, write the body to the scratchpad, and send it with `gh issue edit 1051 -R bytechefhq/bytechef --body-file`.
- [ ] **Step 5: Commit** `1051 Pin that embedded roles, tenant admins and connected users each get exactly their access`

---

## Out of this plan

- **Plan C** (client, Playwright, docs) builds on the GraphQL and gates above. It also decides whether a per-environment embedded Admin may remove themselves past the every-environment gate.
- Plan A carry-overs that this plan does not take:
  - Caching the extra queries per `hasWorkflowScope` check.
  - Configuration-app returning a 500 instead of a deny for a missing connection id (remote `fetchConnection`).
  - Deleting a user audits their embedded removal under `WORKSPACE_MEMBERSHIPS_REMOVED`.
  - `CustomRoleServiceImpl` evicting the embedded scope cache, which is needed only once embedded custom roles exist.
- Plan C repoints the client's Embedded API Keys page from the shared `apiKeys` queries to the `embeddedApiKeys` queries and mutations from Task 11. The mutations take `CreateEmbeddedApiKeyInput` and `UpdateEmbeddedApiKeyInput`; the platform schema has no input types to reuse.
- Carried into Plan C from the execution of this plan:
  - `API_KEY_VIEW` is an Editor scope, so embedded Viewers see no API keys page.
  - Rotating an embedded MCP server's secret key stays tenant admin only (`updateEmbeddedMcpServerSecretKey`, like automation's `rotateSecretKey`). The docs list it as tenant-level. The secret key and URL go only to `MCP_EDIT` holders; Viewers get null.
  - Members without the edit scope get no credentials: run responses never carry `connectionParameters`; instance configuration reads need `INTEGRATION_INSTANCE_CONFIGURATION_EDIT`; embedded connection reads need `CONNECTION_EDIT` (connected-user admin reads need `CONNECTED_USER_MANAGE`). The client shows these fields as hidden instead of empty.
  - MCP instance configurations use the `INTEGRATION_INSTANCE_CONFIGURATION_*` scopes (delete is Admin), and an MCP server can only be linked to a configuration in the same environment.
  - The automation connection endpoints refuse embedded connections; the embedded admin endpoints are the only way to manage them.
  - Open question for the user: whether automation should also hide connection credentials and MCP secret keys from workspace members without the edit scope (embedded does; automation is unchanged).
  - Parked minors: connected-user project runs are gated by `EXECUTION_*` rather than `CONNECTED_USER_VIEW`; tag lists are not environment-filtered (names only); `getIntegrationInstanceConfigurationIntegration(s)` returns credential maps but is reached only internally from `ConnectedUserIntegrationFacadeImpl`; trigger output is read before redaction (wasted I/O only).
