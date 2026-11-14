# Embedded Automations as MCP Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An admin offers an embedded automation configuration's workflows as tools on an embedded MCP server; each
connected user who activated the automation gets the tool, controlled by the automation's own on/off switch, and the
connected user sheet's MCP Servers tab lists and provisions them. Integration attach stops enabling configuration
workflows as a side effect.

**Architecture:** Three new tables in `embedded-ai-mcp-service` mirror the integration ones (server link, link
workflow keyed by `workflow_uuid`, per-user row on the user's `connected_user_project_workflow`). Rows are created
from stable facts only (link, assignment, non-dangling reference); on/off state is read at listing time through a new
resolver in `embedded-configuration-api`. `EmbeddedMcpToolFacade` gains a third tool source wired into the existing
`toolFilter`.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Liquibase, Spring GraphQL, Spring AI MCP; React 19,
TanStack Query, graphql-codegen, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-14-embedded-automation-mcp-tools-design.md`

## Global Constraints

- All new server files are under `server/ee/`: ByteChef Enterprise license header and `@version ee` Javadoc tag.
- Facades never use repositories; services never call services; admin facade methods carry `@PreAuthorize("isTenantAdmin()")`.
- New Liquibase changelog in `embedded-ai-mcp-service/src/main/resources/config/liquibase/changelog/embedded/mcp/`
  (picked up by `includeAll` in `master.xml`), ids starting at `startWith="1050"` like
  `20261113000002_embedded_mcp_add_mcp_integration_instance_workflow_table.xml`; index and constraint names at most
  63 characters; own commit `<ticket> Upgrade - ...`. Never edit a merged changelog.
- Every FK added here is `ON DELETE CASCADE` (load-bearing: template project delete cascades in the DB, and
  `deleteUnassignedConfiguration` treats `DataIntegrityViolationException` as "in use").
- Only a workflow whose enabled trigger is `workflow/v1/newWorkflowCall` can be a tool (`getMcpToolCallableTrigger`).
- New interfaces called across EE apps get a `@Component @ConditionalOnEEVersion` remote stub throwing
  `UnsupportedOperationException`, like `RemoteMcpIntegrationInstanceRowsClient`.
- GraphQL enums SCREAMING_SNAKE_CASE; `.graphql` operations and regenerated `client/src/shared/middleware/graphql.ts`
  in separate commits; client and server never share a commit; specs/plans never share a commit with code.
- Commit subjects `<ticket> <Description>` / `<ticket> client - <Description>`, subject only, no AI attribution.
  The ticket number comes from the user before the first commit.
- Java style per CLAUDE.md (blank line before control statements and after a variable modification, no chaining on
  results, descriptive names); client style per CLAUDE.md (sort-keys, `Icon` suffix, `twMerge`, hook order).
- Testcontainers: `export DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`.
- Copy: disabled configuration workflows show "Turned off in the configuration".

## Review Focus

1. A user switches the MCP switch off (public endpoint or sheet): the tool disappears from `tools/list` and
   `tools/call` at once; switching only the automation's own on/off leaves the tool callable (Task 6
   `testAutomationToolHiddenWhenMcpRowDisabled`, `testAutomationToolStaysWhenAutomationSwitchedOff`).
2. The admin turns the configuration or its workflow off while the async rollout has not yet flipped
   `ProjectDeploymentWorkflow.enabled`: the tool is hidden anyway (Task 5 resolver reads the configuration directly;
   `testResolveEmptyWhenConfigurationWorkflowDisabledButDeploymentStillEnabled`).
3. Activation through `provisionReference` throws `MissingConnectionException` after the reference is saved: the MCP
   row still exists, and the tool appears once the connection is added (Task 4
   `testRowCreatedWhenProvisionThrowsMissingConnection`).
4. Two connected users on one link: each call runs only the caller's own deployment (Task 5
   `testResolveOnlyCallersOwnDeployment`, Task 6 `testAutomationToolRunsCallersDeployment`).
5. A user moved to another configuration of the same template: rows follow to the new configuration's link with the
   same `workflow_uuid` and keep `enabled`; with no such link on the server the tool is gone (Task 4
   `testRemapOnMoveKeepsEnabled`, `testRemapOnMoveWithoutTargetLinkRemovesRows`).

---

### Task 1: Integration attach no longer enables configuration workflows

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/src/main/java/com/bytechef/ee/embedded/ai/mcp/facade/McpIntegrationInstanceConfigurationFacadeImpl.java:362-402` (`saveIntegrationInstanceConfigurationWorkflow`), callers at lines 145, 269, 274
- Modify: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationWorkflowsList.tsx` (and `hooks/useMcpIntegrationWorkflowsForm.ts` if selection lives there)
- Test: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/src/test/java/com/bytechef/ee/embedded/ai/mcp/facade/McpIntegrationInstanceConfigurationFacadeIntTest.java`
- Test: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog.test.tsx`

**Interfaces:**
- Produces: `IllegalArgumentException("Workflow <workflowId> is turned off in integration instance configuration <id>")`
  when a selected workflow has no configuration row or its row is disabled. No method signature changes.

- [ ] **Step 1: Write failing server tests**
  - `testCreateRejectsWorkflowDisabledInConfiguration`: configuration workflow `enabled=false`; `createMcpIntegrationInstanceConfiguration(..., List.of(workflowId), List.of())` throws `IllegalArgumentException`; the configuration workflow is still disabled; no MCP link row exists.
  - `testCreateRejectsWorkflowMissingFromConfiguration`: no configuration workflow row; same assertions, and no configuration workflow row was created.
  - `testUpdateWithConnectionsDoesNotEnableConfigurationWorkflow`: link exists, admin then disables the configuration workflow, `updateMcpIntegrationInstanceConfiguration` with a `WorkflowConfigurationDTO` for it; afterwards `isEnabled()` is still `false`.
- [ ] **Step 2: Run them, expect FAIL** (today the workflow is created/enabled)
  Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-service:testIntegration --tests '*McpIntegrationInstanceConfigurationFacadeIntTest*'`
- [ ] **Step 3: Implement.** Validate every newly selected workflow before creating the link (create) or the link workflow (update): the configuration row must exist and be enabled. Remove both `setEnabled(true)` calls and the `updateEnabled(..., true)` branch; keep writing connections and inputs for an existing row. The "create a configuration workflow row" branch goes away.
- [ ] **Step 4: Run module `check testIntegration`, expect PASS** (existing tests that relied on the side effect get their fixture changed to start enabled, not their assertion dropped).
- [ ] **Step 5: Client.** In the workflow picker, a workflow whose configuration row is missing or disabled renders disabled with "Turned off in the configuration". Test `rendersDisabledWorkflowAsNotSelectable` (checkbox disabled, text present). Run `cd client && npx vitest run src/ee/pages/embedded/mcp-servers`.
- [ ] **Step 6: Commit** server, then client, separately.

---

### Task 2: Liquibase, domain, repositories and services for the three tables

**Files:**
- Create: `.../embedded-ai-mcp-service/src/main/resources/config/liquibase/changelog/embedded/mcp/20261114000001_embedded_mcp_add_mcp_automation_tables.xml`
- Create (api, `com.bytechef.ee.embedded.ai.mcp.domain`): `McpAutomationProjectConfiguration`, `McpAutomationProjectConfigurationWorkflow`, `McpConnectedUserProjectWorkflow`
- Create (api, `...mcp.service`): `McpAutomationProjectConfigurationService`, `McpAutomationProjectConfigurationWorkflowService`, `McpConnectedUserProjectWorkflowService`
- Create (service): three `*Repository` and three `*ServiceImpl`
- Create (remote-client, `...remote.client.service`): three `Remote*ServiceClient` stubs
- Modify: `embedded-ai-mcp-service/build.gradle.kts` (move `automation-configuration-api` from `testImplementation` to `implementation`)
- Test: `.../src/test/java/.../service/McpAutomationServicesIntTest.java`

**Interfaces:**
- Produces:
  - `McpAutomationProjectConfiguration(long automationProjectConfigurationId, long mcpServerId)`; getters `getId()`, `getMcpServerId()`, `getAutomationProjectConfigurationId()`.
  - `McpAutomationProjectConfigurationWorkflow`: `getId()`, `getMcpAutomationProjectConfigurationId()`, `getWorkflowUuid(): String`, `getParameters(): Map<String, ?>`, `isEnabled()`.
  - `McpConnectedUserProjectWorkflow`: `getId()`, `getConnectedUserProjectWorkflowId()`, `getMcpAutomationProjectConfigurationWorkflowId()`, `isEnabled()`.
  - `McpAutomationProjectConfigurationService`: `create(McpAutomationProjectConfiguration)`, `delete(long id)`, `fetch(long id): Optional<...>`, `getMcpServerMcpAutomationProjectConfigurations(long mcpServerId): List<...>`, `getAutomationProjectConfigurationMcpAutomationProjectConfigurations(long automationProjectConfigurationId): List<...>`.
  - `McpAutomationProjectConfigurationWorkflowService`: `create(long mcpAutomationProjectConfigurationId, String workflowUuid)` (enabled, empty parameters), `delete(long id)`, `fetch(long id)`, `getMcpAutomationProjectConfigurationWorkflows(long mcpAutomationProjectConfigurationId)`, `updateParameters(long id, Map<String, ?>)`, `updateEnabled(long id, boolean)`.
  - `McpConnectedUserProjectWorkflowService`: `create(long connectedUserProjectWorkflowId, long linkWorkflowId, boolean enabled)`, `delete(long id)`, `fetch(long id)`, `fetch(long connectedUserProjectWorkflowId, long linkWorkflowId)`, `getConnectedUserMcpConnectedUserProjectWorkflows(long connectedUserId)`, `getMcpServerMcpConnectedUserProjectWorkflows(long mcpServerId)`, `getConnectedUserIdsByMcpServerId(long mcpServerId): Set<Long>`, `updateEnabled(long id, boolean)`.
  - The two connected-user queries are `@Query` joins through `connected_user_project_workflow` → `connected_user_project.connected_user_id`, and through the link workflow → link → `mcp_server_id`.

- [ ] **Step 1: Write failing IntTest** `McpAutomationServicesIntTest`:
  - `testCreateAndReadLinkWorkflowRow`
  - `testDuplicateLinkRejected` (unique (`mcp_server_id`, `automation_project_configuration_id`) → `DbActionExecutionException`/`DataIntegrityViolationException`)
  - `testDeletingAutomationProjectConfigurationCascades` (link, link workflow and user row gone)
  - `testDeletingReferenceCascades` (user row gone)
  - `testGetConnectedUserIdsByMcpServerId`
- [ ] **Step 2: Run, expect FAIL** (classes missing). Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-service:testIntegration --tests '*McpAutomationServicesIntTest*'`
- [ ] **Step 3: Write the changelog** (tables per spec "Data model", audit columns and `version` like the 20261113000002 file, FKs to `mcp_server`, `automation_project_configuration`, `connected_user_project_workflow`, cascades, unique constraints, an index on each FK column).
- [ ] **Step 4: Implement domain, repositories, services, remote stubs, Gradle change.**
- [ ] **Step 5: Run, expect PASS**, then module `check`.
- [ ] **Step 6: Commit** the changelog alone (`<ticket> Upgrade - Add embedded MCP automation tables`), then the rest.

---

### Task 3: Shared server membership

**Files:**
- Create: `.../embedded-ai-mcp-service/src/main/java/com/bytechef/ee/embedded/ai/mcp/facade/McpServerMembership.java` (package-private `@Component`)
- Modify: `McpIntegrationInstanceRowsImpl.java` (`getConnectedUserIds` line 146, `getMcpServerIds` line 170, `hasEnabledRow` line 511)
- Test: `.../src/test/java/.../facade/McpIntegrationInstanceRowsIntTest.java` (extend `McpRowsIntTestFixture` with an automation link + reference helper)

**Interfaces:**
- Consumes: Task 2 services.
- Produces: `McpServerMembership`: `Set<Long> getConnectedUserIds(long mcpServerId)`, `Set<Long> getMcpServerIds(long connectedUserId)`, `Optional<Boolean> getConnectedUserState(long connectedUserId, long mcpServerId)` (empty = not on the server; `true` = any row of any kind is enabled). `McpIntegrationInstanceRowsImpl`'s public `getConnectedUserIds`/`getMcpServerIds` delegate to it; `hasEnabledRow` becomes `getConnectedUserState(...).orElse(...)`.

- [ ] **Step 1: Failing tests**
  - `testAutomationOnlyUserGetsNewIntegrationRowsEnabled`: user has only an enabled automation row on the server, then gets an integration instance covered by a link → new integration row `enabled=true`.
  - `testAutomationOnlyUserWithDisabledRowGetsNewIntegrationRowsDisabled`.
  - `testGetConnectedUserIdsIncludesAutomationOnlyUser`.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** `McpServerMembership` over the three row services and switch the integration class to it.
- [ ] **Step 4: Run `McpIntegrationInstanceRowsIntTest` and the module's full `testIntegration`, expect PASS.**
- [ ] **Step 5: Commit.**

---

### Task 4: Automation rows lifecycle and hooks

**Files:**
- Create (api, `...mcp.facade`): `McpAutomationRows.java`
- Create (service): `McpAutomationRowsImpl.java`
- Create (remote-client): `RemoteMcpAutomationRowsClient.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserWorkflowReferenceFacadeImpl.java:211` (after `connectedUserProjectWorkflowRepository.save(reference)`, before `applyWorkflow`)
- Modify: `.../AutomationProjectConfigurationAdminFacadeImpl.java:1394` (in `moveConnectedUser`, right after `automationProjectConfigurationResolver.assign(...)`)
- Modify: `.../embedded-ai-mcp-service/.../event/EmbeddedMcpServerBeforeDeleteEventListener.java` (delete the server's automation links)
- Test: `.../embedded-ai-mcp-service/src/test/java/.../facade/McpAutomationRowsIntTest.java`
- Test: `.../embedded-configuration-service/src/test/java/.../facade/ConnectedUserWorkflowReferenceFacadeIntTest.java` (hook), `AutomationProjectConfigurationAdminFacadeIntTest.java` (move hook)

**Interfaces:**
- Consumes: Task 2 services; Task 3 `McpServerMembership`; `ConnectedUserProjectConfigurationService.fetchAssignment(connectedUserId, projectId)` and `getAssignments(configurationId)`; `ConnectedUserProjectWorkflowService.fetchConnectedUserProjectWorkflow(...)`; `AutomationProjectConfigurationService.getConfiguration(id)`.
- Produces `McpAutomationRows`:
  - `void addMissingRowsForLink(long mcpAutomationProjectConfigurationId)`: every assigned user with a non-dangling reference whose `automationUuid` matches a link workflow.
  - `void addMissingRowsForReference(long connectedUserProjectWorkflowId)`: every link workflow on any server whose configuration the reference's user is assigned to and whose `workflow_uuid` equals the reference's `automationUuid`.
  - `void remapConnectedUser(long connectedUserId, long previousAutomationProjectConfigurationId, long automationProjectConfigurationId)`.
  - `void provisionMcpServer(long connectedUserId, long mcpServerId)`: create missing rows enabled and enable existing ones.
  - New row `enabled` = `McpServerMembership.getConnectedUserState(user, server).orElse(true)`.

- [ ] **Step 1: Failing tests** in `McpAutomationRowsIntTest`:
  - `testLinkCreatedAddsRowsForActivatedUsersOnly` (assigned-but-not-activated user gets none; dangling reference gets none)
  - `testReferenceCreatedAddsEnabledRowOnServerUserWasNotOn`
  - `testReferenceCreatedFollowsUserStateOnServer` (user's rows there all disabled → new row disabled)
  - `testProvisionCreatesAndEnablesRows`
  - `testRemapOnMoveKeepsEnabled`
  - `testRemapOnMoveWithoutTargetLinkRemovesRows`
  - `testServerDeleteRemovesLinksAndRows`
  - and in the embedded-configuration IntTests: `testRowCreatedWhenProvisionThrowsMissingConnection`, `testMoveConnectedUserRemapsRows`.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** `McpAutomationRowsImpl` and the remote stub; call `addMissingRowsForReference(savedReference.getId())` and `remapConnectedUser(connectedUserId, sourceConfiguration.getId(), targetConfigurationId)` at the two hook points; extend the server delete listener.
- [ ] **Step 4: Run both modules' `testIntegration`, expect PASS.** Also run `WorkflowTestConfigurationTest` and the embedded-configuration unit tests (`ConnectedUserReferenceRolloutManagerIntTest` must be unaffected).
- [ ] **Step 5: Commit** (embedded-ai-mcp and embedded-configuration changes may share the commit; they are one change).

---

### Task 5: Resolver for a user's callable automation

**Files:**
- Create (embedded-configuration-api, `com.bytechef.ee.embedded.configuration.facade`): `ConnectedUserMcpAutomationResolver.java`, `dto/CallableAutomationDTO.java`
- Create (embedded-configuration-service, `...configuration.facade`): `ConnectedUserMcpAutomationResolverImpl.java`. It lives in the facade layer because it uses `ConnectedUserReferenceDeploymentManager` and several services. The existing `ConnectedUserProjectDeploymentResolver` (automation-configuration-api) serves a different purpose and stays unchanged.
- Create (embedded-configuration-remote-client, `...remote.client.facade`): `RemoteConnectedUserMcpAutomationResolverClient.java`
- Test: `.../embedded-configuration-service/src/test/java/.../facade/ConnectedUserMcpAutomationResolverIntTest.java`

**Interfaces:**
- Consumes: `ConnectedUserReferenceDeploymentManager.fetchRow(projectDeploymentId, automationUuid)` (deployment workflow at the deployment's current version) and `getInputs`; `ConnectedUserProjectConfigurationService.fetchAssignment(connectedUserId, projectId)`; `AutomationProjectConfigurationService.getConfiguration(id)`/`isWorkflowEnabled(configuration, UUID)`; `ConnectedUserProjectWorkflowService.fetchConnectedUserProjectWorkflow(...)`. Do not use `isAdministrativelyEnabled`: it checks the user's assigned configuration, while the resolver must check the given one and that the user is assigned to it.
- Produces:
  - `Optional<CallableAutomationDTO> fetchCallableAutomation(long connectedUserId, long automationProjectConfigurationId, String workflowUuid)`
  - `record CallableAutomationDTO(long connectedUserProjectWorkflowId, long projectDeploymentId, String workflowId, Map<String, ?> inputs, @Nullable String missingConnectionComponentName)`
  - The deployment workflow's `enabled` is NOT checked (user decision: the automation's own switch controls only its event triggers); the row must exist.
  - `Optional<Long> fetchConnectedUserProjectWorkflowId(long connectedUserId, long automationProjectConfigurationId, String workflowUuid)` (reference id regardless of on/off, for the sheet).
  - The impl runs automation reads under `AutomationAuthorizationContext.callSkippingChecks`.

- [ ] **Step 1: Failing tests:** one per condition returning empty: `testResolveEmptyWhenConfigurationDisabled`, `testResolveEmptyWhenConfigurationWorkflowDisabledButDeploymentStillEnabled`, `testResolveEmptyWhenUserAssignedElsewhere`, `testResolveEmptyWhenReferenceDangling`, `testResolveEmptyWhenNoDeploymentWorkflow`; `testResolveReturnsAutomationWhenDeploymentWorkflowDisabled`; `testResolveReportsMissingConnection`; plus `testResolveReturnsDeploymentWorkflow` (id at the deployment's version, inputs) and `testResolveOnlyCallersOwnDeployment` (two users, one configuration).
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run, expect PASS.**
- [ ] **Step 5: Commit.**

---

### Task 6: Automation tools in the embedded MCP server

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/facade/EmbeddedMcpToolFacade.java`
- Modify: `.../server/config/EmbeddedMcpServerConfiguration.java:147-184` (`toolFilter`)
- Modify: `embedded-ai-mcp-server/build.gradle.kts` if `automation-configuration-api` / `embedded-configuration-api` are missing
- Test: `.../embedded-ai-mcp-server/src/test/java/.../facade/EmbeddedMcpToolFacadeAutomationIntTest.java`

**Interfaces:**
- Consumes: Task 2 services, Task 5 resolver, `WorkflowService.getWorkflow(workflowId)`, `PrincipalJobFacade.createJob(JobParametersDTO, long, PlatformType)`, existing private helpers `getWorkflowRunResult`, `awaitJob`, `getMcpToolCallableTrigger`.
- Produces: `public List<ToolCallback> getAutomationToolCallbacks(McpAutomationProjectConfiguration mcpAutomationProjectConfiguration, @Nullable String externalUserId, Environment environment, String tenantId)`, called in `toolFilter` after the integration block for `mcpAutomationProjectConfigurationService.getMcpServerMcpAutomationProjectConfigurations(mcpServer.getId())`.

- [ ] **Step 1: Failing tests:**
  - `testAutomationToolListedForActivatedUser` (name/description from link parameters, fallback to workflow label/description)
  - `testAutomationToolHiddenForAnonymousCaller`
  - `testAutomationToolHiddenWhenRowDisabled`, `...WhenLinkWorkflowDisabled`, `...WhenConfigurationDisabled`
  - `testAutomationToolStaysWhenAutomationSwitchedOff` (deployment workflow disabled by the user's own switch; tool listed and the call creates a job)
  - `testAutomationToolAnswersConnectionRequired` (resolver reports a missing connection: listed, call returns the integration-style connection-required response, no job)
  - `testAutomationToolHiddenWithoutWorkflowCallTrigger`
  - `testAutomationToolSchemaFromDeploymentVersion` (deployment pinned to an older version than the configuration; schema uses the older workflow's `fromAi` parameters)
  - `testAutomationToolRunsCallersDeployment` (job has `PlatformType.AUTOMATION`, caller's `projectDeploymentId`, inputs include deployment inputs + trigger inputs + `TRIGGER_NAME_INPUT`)
  - `testAutomationToolReturnsApprovalAndSuspendedResults`
  - `testAutomationToolRecordsExternalInvocationEvent` (`EMBEDDED`, `MCP_SERVER`, `WORKFLOW`, connected user, environment, server, job id)
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** per spec "Tool listing and calling"; reuse the integration path's parameter and schema code by extracting a shared private method rather than copying it.
- [ ] **Step 4: Run module `check testIntegration` (including the existing `EmbeddedMcpToolFacadeIntTest` and `EmbeddedMcpServerConfigurationIntTest`), expect PASS.**
- [ ] **Step 5: Commit.**

---

### Task 7: Admin facade and GraphQL for automation links

**Files:**
- Create (api): `facade/McpAutomationProjectConfigurationFacade.java`
- Create (service): `facade/McpAutomationProjectConfigurationFacadeImpl.java`; `audit/McpAutomationProjectConfigurationAuditEvents.java`
- Create (graphql): `graphql/mcp-automation-project-configuration.graphqls`, `web/graphql/McpAutomationProjectConfigurationGraphQlController.java`
- Test: `McpAutomationProjectConfigurationFacadeIntTest.java`, `McpAutomationProjectConfigurationGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: Task 2 services, Task 4 `addMissingRowsForLink`; `AutomationProjectConfigurationService.getConfiguration`/`isWorkflowEnabled`; `ProjectWorkflowService.fetchProjectWorkflow(projectId, projectVersion, workflowUuid)` and `WorkflowService.getWorkflow(workflowId)` for the trigger check at the pinned version, under `AutomationAuthorizationContext.callSkippingChecks`; `McpServerService` for the server's environment.
- Produces (facade, all `@PreAuthorize("isTenantAdmin()")`, mutations `@Audited`):
  - `McpAutomationProjectConfiguration createMcpAutomationProjectConfiguration(long mcpServerId, long automationProjectConfigurationId, List<String> workflowUuids, List<McpAutomationWorkflowConfigurationDTO> workflowConfigurations)`
  - `McpAutomationProjectConfiguration updateMcpAutomationProjectConfiguration(long id, List<String> workflowUuids, List<McpAutomationWorkflowConfigurationDTO> workflowConfigurations)`
  - `void deleteMcpAutomationProjectConfiguration(long id)`
  - `List<McpAutomationProjectConfigurationDTO> getMcpServerMcpAutomationProjectConfigurations(long mcpServerId)` with per workflow `available` (enabled in the configuration and has the workflow-call trigger at the pinned version)
  - `updateMcpAutomationProjectConfigurationWorkflowParameters(long id, Map<String, ?>)`, `updateMcpAutomationProjectConfigurationWorkflowEnabled(long id, boolean)`
  - `record McpAutomationWorkflowConfigurationDTO(String workflowUuid, Map<String, ?> parameters)`
  - GraphQL: `mcpAutomationProjectConfigurationsByServerId(mcpServerId: ID!)`, `createMcpAutomationProjectConfiguration`, `updateMcpAutomationProjectConfiguration`, `deleteMcpAutomationProjectConfiguration`, `updateMcpAutomationProjectConfigurationWorkflowParameters`, `updateMcpAutomationProjectConfigurationWorkflowEnabled`, mirroring `mcp-integration-instance-configuration(-workflow).graphqls`.

- [ ] **Step 1: Failing tests:** `testCreateRejectsOtherEnvironment`, `testCreateRejectsWorkflowWithoutWorkflowCallTrigger`, `testCreateRejectsWorkflowDisabledInConfiguration`, `testCreateDoesNotChangeConfiguration` (configuration `version` and workflows unchanged; no rollout event published), `testCreateAddsRowsForActivatedUsers`, `testListMarksTurnedOffWorkflowUnavailable`, `testNonAdminDenied`; GraphQL controller IntTest for each operation's happy path.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run module `check testIntegration`, expect PASS.**
- [ ] **Step 5: Commit.**

---

### Task 8: Connected user sheet facade and GraphQL

**Files:**
- Modify (api): `dto/ConnectedUserMcpServerDTO.java` (add `List<ConnectedUserMcpServerAutomationDTO> automations`), `dto/ProvisionableConnectedUserMcpServerDTO.java` (add `List<String> coveredAutomationNames`, `List<String> missingAutomationNames`), `facade/ConnectedUserMcpServerFacade.java`
- Create (api): `dto/ConnectedUserMcpServerAutomationDTO.java`
- Modify (service): `facade/ConnectedUserMcpServerFacadeImpl.java` (incl. line 384 provision), `audit/ConnectedUserMcpServerAuditEvents.java`
- Modify (graphql): `connected-user-mcp-server.graphqls`, `ConnectedUserMcpServerGraphQlController.java`
- Test: `ConnectedUserMcpServerFacadeIntTest.java`, `ConnectedUserMcpServerGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: Task 3 membership, Task 4 `provisionMcpServer`, Task 5 `fetchCallableAutomation`/`fetchConnectedUserProjectWorkflowId`, job lookups by deployment id with `PlatformType.AUTOMATION`.
- Produces:
  - `record ConnectedUserMcpServerAutomationDTO(long id, long connectedUserProjectWorkflowId, String automationName, String workflowUuid, String name, @Nullable String description, boolean enabled, boolean available, @Nullable Instant lastExecutionDate)`; `enabled` is the per-user MCP row's flag.
  - `ConnectedUserMcpServerAutomationDTO enableMcpAutomation(long connectedUserId, long mcpConnectedUserProjectWorkflowId, boolean enable)` (checks the row belongs to the user; audit `CONNECTED_USER_MCP_AUTOMATION_ENABLED_UPDATED`). Independent of the Automations tab's `enableConnectedUserProjectWorkflow` (user decision: two switches).
  - The sheet's Automations tab data (the connected user project workflow GraphQL type) gains `toolOnly: Boolean!` (only enabled trigger is `workflow/v1/newWorkflowCall`).
  - GraphQL: `ConnectedUserMcpServer.automations: [ConnectedUserMcpServerAutomation!]!`, `ProvisionableConnectedUserMcpServer.coveredAutomationNames/missingAutomationNames: [String!]!`, mutation `enableConnectedUserMcpAutomation(connectedUserId: ID!, id: ID!, enable: Boolean!): ConnectedUserMcpServerAutomation`.
  - `enableConnectedUserMcpServer` and `deleteConnectedUserMcpServer` also cover automation rows.

- [ ] **Step 1: Failing tests:** `testAutomationOnlyServerListed`, `testAutomationRowAvailableFalseWhenConfigurationWorkflowOff`, `testEnableMcpAutomationRejectsOtherUsersRow`, `testEnableMcpAutomationLeavesAutomationSwitch` (the reference's own enabled is unchanged), `testProjectWorkflowToolOnlyFlag`, `testEnableServerTogglesAutomationRows`, `testDeleteServerRemovesAutomationRows`, `testProvisionableCoverage` (activated → covered; assigned but not activated → missing; unassigned configuration → absent), `testProvisionAllowedWithOnlyAutomationsCovered`, `testLastExecutionDateAcrossVersions`.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run module `check testIntegration`, expect PASS.**
- [ ] **Step 5: Commit.**

---

### Task 9: Admin client, Automations section on the MCP server page

**Files:**
- Create: `client/src/graphql/embedded/mcp-servers/*McpAutomationProjectConfiguration*.graphql` (one per operation in Task 7)
- Regenerate: `client/src/shared/middleware/graphql.ts` (`cd client && npx graphql-codegen`)
- Create: `client/src/ee/pages/embedded/mcp-servers/components/mcp-automation-project-configuration-list/` (`McpAutomationProjectConfigurationList.tsx`, `...ListItem.tsx`, `McpAutomationProjectConfigurationDialog.tsx`, `tests/`)
- Modify: `client/src/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerToolsContent.tsx` (render the section beside integration configurations)

**Interfaces:**
- Consumes: Task 7 GraphQL; reuse `useMcpIntegrationWorkflowsForm` and `McpIntegrationInstanceConfigurationWorkflowDialog` for tool name, description and `fromAi` mapping where the shapes fit.

- [ ] **Step 1: Failing tests:** `rendersAutomationsSection`, `dialogListsOnlyConfigurationsOfServerEnvironment`, `dialogRendersTurnedOffWorkflowDisabled` ("Turned off in the configuration"), `dialogSubmitsSelectedWorkflowUuids`, `listItemMarksUnavailableWorkflow`.
- [ ] **Step 2: Run** `cd client && npx vitest run src/ee/pages/embedded/mcp-servers`, **expect FAIL.**
- [ ] **Step 3: Implement**, then `npm run format`, `npx eslint <changed files>` and `npm run typecheck`.
- [ ] **Step 4: Run tests, expect PASS.**
- [ ] **Step 5: Commit** `.graphql` operations, then `graphql.ts`, then components (three client commits).

---

### Task 10: Connected user sheet client

**Files:**
- Modify: `client/src/graphql/embedded/connected-users/*McpServer*.graphql` (add `automations`, the coverage fields, the new mutation) and the connected user project workflow query (`toolOnly`)
- Modify: `connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem.tsx` (tool-only: no own switch, "Tool only" hint)
- Regenerate: `client/src/shared/middleware/graphql.ts`
- Create: `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItemAutomationRow.tsx`
- Modify: `ConnectedUserMcpServerListItem.tsx`, `ConnectedUserSheetPanelMcpServerList.tsx`, `provision-mcp-server-dialog/ProvisionMcpServerDialog.tsx`
- Test: the folders' existing `tests/` (or co-located `*.test.tsx`)

**Interfaces:**
- Consumes: Task 8 GraphQL.

- [ ] **Step 1: Failing tests:** `rendersAutomationGroupWithToggle`, `automationRowGreyedOutWhenUnavailable`, `togglingAutomationCallsEnableConnectedUserMcpAutomation`, `provisionDialogListsCoveredAndMissingAutomations`, and in the Automations tab `toolOnlyAutomationShowsNoOwnSwitch`.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** like `ConnectedUserMcpServerListItemWorkflowRow`.
- [ ] **Step 4: Run tests and `npm run typecheck`, expect PASS.**
- [ ] **Step 5: Commit** operations, generated file, components separately.

---

### Task 11: Public MCP switch for the connected user

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml` (own commit), then `./gradlew -p server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest generateOpenAPI` (own commit `7537 Generated by gw generateOpenAPI`)
- Create: `.../public_/web/rest/McpAutomationWorkflowApiController.java` (mirror `McpIntegrationInstanceWorkflowApiController`)
- Modify (api + service): `McpAutomationRows` or a new `McpConnectedUserProjectWorkflowFacade` with `enableMcpAutomationWorkflow(long connectedUserId, String workflowUuid, boolean enable)`; the public automation workflow DTO/mapper (gains `mcpEnabled`, `toolOnly`)
- Test: `McpAutomationWorkflowApiControllerIntTest.java`, facade IntTest

**Interfaces:**
- Consumes: Task 2 services, Task 4 rows, the caller's connected user from the external user id (as the integration controller does).
- Produces: `POST`/`DELETE /automation/workflows/{workflowUuid}/mcp/enable` and `/{externalUserId}/automation/workflows/{workflowUuid}/mcp/enable` (204; 404 when the caller has no reference for that uuid); `mcpEnabled: Boolean` (null when no server offers it, else any row on) and `toolOnly: Boolean` on the public automation workflow.

- [ ] **Step 1: Failing tests:** `testEnableSetsEveryServersRow`, `testDisableSetsEveryServersRow`, `testOtherUsersRowsUntouched`, `testNotFoundWithoutReference`, `testMcpEnabledNullWhenNotOffered`, `testToolOnlyFlag`, `testAutomationSwitchUnchanged`.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** (openapi first, generate, then controller and facade).
- [ ] **Step 4: Run module `check testIntegration`, expect PASS.**
- [ ] **Step 5: Commit** openapi.yaml, generated output, then the rest.

---

### Task 12: SDK "Available to AI agents" switch

**Files:**
- Modify: `sdks/frontend/embedded/library/src/` where the automation's own switch is rendered (Automation Hub list item and ActivationDialog; find both) and its API client
- Test: co-located tests in the library

**Interfaces:**
- Consumes: Task 11 endpoints and fields.

- [ ] **Step 1: Failing tests:** `showsAiAgentsSwitchWhenOffered` (hidden when `mcpEnabled` is null), `togglingAiAgentsSwitchCallsMcpEnable` (POST on, DELETE off), `toolOnlyShowsOnlyAiAgentsSwitch`.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.** Label "Available to AI agents".
- [ ] **Step 4: Run the library's tests, lint and typecheck, expect PASS.**
- [ ] **Step 5: Commit** (`7537 sdk - ...`, separate from client and server).

---

### Task 15: Integration MCP links no longer edit connections and inputs

Runs right after Task 5 (out of numeric order; it is independent of Tasks 6-14).

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-graphql/src/main/resources/graphql/mcp-integration-instance-configuration.graphqls` (drop `workflowConfigurations` from the create/update inputs, lines ~32 and ~37)
- Modify: `.../embedded-ai-mcp-graphql/.../McpIntegrationInstanceConfigurationGraphQlController.java`
- Modify: `.../embedded-ai-mcp-api/.../facade/McpIntegrationInstanceConfigurationFacade.java` and `.../embedded-ai-mcp-service/.../facade/McpIntegrationInstanceConfigurationFacadeImpl.java` (remove the `List<WorkflowConfigurationDTO>` parameters, `toWorkflowConfigurationMap`, `saveIntegrationInstanceConfigurationWorkflow`'s connection/input writing, `toIntegrationInstanceConfigurationWorkflowConnections`, `isConnectionUsable`, and anything left unused)
- Modify: callers (`AuditDataContractTest` names, remote stubs, tests)
- Modify: `client/src/graphql/...` operation(s) for these mutations; regenerate `client/src/shared/middleware/graphql.ts`
- Modify: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog.tsx`, `hooks/useMcpIntegrationWorkflowsForm.ts` (remove the connections/inputs sections; keep tool name, description, parameter mapping)
- Test: `McpIntegrationInstanceConfigurationFacadeIntTest.java`, the GraphQL controller IntTest, `McpIntegrationInstanceConfigurationWorkflowDialog.test.tsx`, `useMcpIntegrationWorkflowsForm.test.ts`

**Interfaces:**
- Produces: `createMcpIntegrationInstanceConfiguration(long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds)` and `updateMcpIntegrationInstanceConfiguration(long id, List<String> selectedWorkflowIds)` (keep any other existing overloads' non-configuration parameters). The shared `WorkflowConfigurationDTO` (platform-configuration) is NOT deleted: A2A uses it.

- [ ] **Step 1: Failing tests:** `testCreateLeavesConfigurationWorkflowConnectionsAndInputs` (configuration workflow with connections and inputs set; attach; they are unchanged), and a client test `dialogHasNoConnectionsOrInputsSection`. Remove or rewrite tests that exercised connection/input writing through MCP (keep their configuration-side equivalents if any).
- [ ] **Step 2: Run, expect FAIL** (compile failure on the removed parameter counts).
- [ ] **Step 3: Implement** server, then client.
- [ ] **Step 4: Run** the module `check testIntegration`, `AuditDataContractTest`, and client vitest on `src/ee/pages/embedded/mcp-servers` plus `npm run typecheck`, **expect PASS.**
- [ ] **Step 5: Commit** server, `.graphql` operations, generated `graphql.ts`, client components separately.

---

### Task 13: Docs

**Files:**
- Modify: `docs/content/docs/platform/embedded/configure/mcp-servers.md`

- [ ] **Step 1:** Add "Expose automations": attach a configuration, pick workflows (must be on in the configuration and have a workflow-call trigger; attaching does not turn them on), the user must have activated the automation; the MCP switch (sheet, public endpoint, SDK "Available to AI agents") controls the tool, the automation's own switch only its event triggers; turning the workflow off in the configuration removes the tool for everyone. Update the integration section: only workflows on in the configuration can be attached, and their connections and inputs are set in the configuration, not on the MCP server. Add the connected user tab's automation rows and provisioning coverage. No em dashes.
- [ ] **Step 2: Commit** (`<ticket> docs - ...`).

---

### Task 14: Live test

- [ ] **Step 1:** Run the dev stack from this worktree (`reference_second_dev_stack_from_worktree`), create an automation configuration with a workflow-call workflow, attach it to an embedded MCP server, activate it for a connected user through the ActivationDialog.
- [ ] **Step 2:** With a connected-user JWT against the embedded MCP endpoint: `tools/list` shows the tool; `tools/call` runs and returns the result; a job exists on the user's deployment.
- [ ] **Step 3:** Switch the automation's own switch off (`/automation/workflows/{workflowUuid}/enable` DELETE): the tool is still listed and callable. Switch the MCP switch off (`.../mcp/enable` DELETE): `tools/list` no longer shows it and `tools/call` fails. Turn the workflow off in the configuration: hidden for every user.
- [ ] **Step 4:** In the connected user sheet, the MCP Servers tab shows the automation row and toggle; the Provision dialog shows covered and missing automations.
- [ ] **Step 5:** Integration attach: a workflow off in its configuration is greyed out in the attach dialog and stays off after saving a link.
- [ ] **Step 6:** Report results with evidence; stop only the processes this task started, by PID.
