# Connected User Provisioning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an administrator provision automation workflow references and MCP servers for one connected user from the connected user sheet, reusing the public provisioning paths.

**Architecture:** Two admin facades gain read ("what is provisionable for this user") and write ("provision") methods: `ConnectedUserProjectAdminFacade` delegates to `AutomationWorkflowProjectFacade.getPublishedProjects(externalId, environment)` and `ConnectedUserWorkflowReferenceFacade.getOrCreateReference(...)`; `ConnectedUserMcpServerFacade` computes server coverage from the user's integration instances and creates/enables the per-user tool and workflow rows. Each gets a GraphQL query and mutation, and the sheet's MCP Servers and Automation Workflows tabs get a header button opening a dialog modelled on `ProvisionIntegrationDialog`.

**Tech Stack:** Spring Boot 4 (Spring Data JDBC, Spring GraphQL, `@PreAuthorize`), JUnit 5 + Testcontainers IntTests, React 19 + TanStack Query + react-hook-form + Vitest, GraphQL codegen.

**Spec:** `docs/superpowers/specs/2026-10-29-connected-user-provisioning-design.md`

## Global Constraints

- Every file under `server/ee/` carries the ByteChef Enterprise license header and an `@version ee` Javadoc tag.
- Facade methods are guarded with `@PreAuthorize("isTenantAdmin()")` (class-level on `ConnectedUserProjectAdminFacadeImpl`, per method on `ConnectedUserMcpServerFacadeImpl`, matching each file's existing style); facades call services and facades only, never repositories.
- No new tables, no Liquibase changes, no new SPI interfaces (so no remote client stubs).
- GraphQL: enum values SCREAMING_SNAKE_CASE; maps use the existing `Map` scalar; queries/mutations added to the existing `connected-user-project.graphqls` and `connected-user-mcp-server.graphqls`.
- Client: object keys alphabetical (`sort-keys`), interfaces end with `I` or `Props`, lucide icons imported with the `Icon` suffix, `twMerge` for class merging, `void queryClient.invalidateQueries(...)`, hook ordering per CLAUDE.md. Regenerated `client/src/shared/middleware/graphql.ts` and `graphql-types.ts` go in their own commit.
- Commits: subject only, prefix `6770 ` for server, `6770 client - ` for client, no trailers. Format with `./gradlew spotlessApply` (server) and `npm run format` (client) before each commit.
- Test naming: integration test classes end with `IntTest`; test method names camelCase without underscores.
- Copy (verbatim): dialog titles "New Workflow" / "New MCP Server"; subtitles "Provision automation workflows from a released configuration for this connected user." / "Expose an MCP server through the integrations this connected user already has."; empty states "No project has a configuration released to this user's environment." / "No MCP servers are defined."; badges "Provisioned" / "Needs attention"; row lines "Uses {covered}" / "Uses {covered}. Missing {missing}: add those integrations first to expose them." / "Add {missing} on the Integrations tab first."; buttons "+ New Workflow" / "+ New MCP Server"; tab headings "Automation Workflows" / "MCP Servers".

## Review Focus

1. A workflow whose required input is left empty is still provisioned by the server (public-path behaviour); only the dialog blocks it. Pinned in Task 8 (Save disabled test).
2. A configuration or permission change between opening the dialog and saving: the save must reject the whole call before writing anything. Pinned in Task 2 (`testProvisionProjectWorkflowsRejectsUnprovisionableUuidBeforeWriting`).
3. A workflow with a missing connection must still end up provisioned, and the following workflows in the same call must still be processed. Pinned in Task 2 (`testProvisionProjectWorkflowsKeepsReferenceWhenConnectionIsMissing`).
4. An MCP server with zero coverage must never produce rows, even if the mutation is called directly. Pinned in Task 5 (`testProvisionMcpServerRejectsServerWithoutCoveredIntegration`).
5. Tool rows must only be created for the user's own instances whose integration component matches the MCP component. Pinned in Task 5 (`testProvisionMcpServerCreatesToolRowsOnlyForMatchingInstances`).

---

### Task 1: Provisionable projects read (server)

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/ProvisionableAutomationWorkflowProjectDTO.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectAdminFacade.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectAdminFacadeImpl.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectAdminFacadeIntTest.java` (new; copy the `@SpringBootTest`/`@Import(PostgreSQLContainerConfiguration.class)`/profile annotations and the project + connected user helpers from `ConnectedUserWorkflowReferenceAdminFacadeIntTest` (`createConnectedUserWithConnection`, `publishAndPinProject` via `AutomationWorkflowProjectConfigurationTestSupport`))

**Interfaces:**
- Produces:
  ```java
  public record ProvisionableAutomationWorkflowProjectDTO(
      long projectId, String name, int projectVersion, List<Workflow> workflows) {
      public record Workflow(
          String workflowUuid, String label, List<ConnectedUserWorkflowTemplateDTO.Input> inputs,
          boolean provisioned, boolean dangling) {}
  }
  List<ProvisionableAutomationWorkflowProjectDTO> getProvisionableProjects(long connectedUserId);
  ```
- Consumes: `ConnectedUserService.getConnectedUser(long id)`, `AutomationWorkflowProjectFacade.getPublishedProjects(String externalUserId, Environment environment)` (returns `AutomationWorkflowProjectDTO` with `lastPublishedVersion()` and `workflowTemplates()`), `ConnectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(long connectedUserId)` (returns `ConnectedUserProjectWorkflow` with `getAutomationWorkflowUuid()`, `isDangling()`).

- [ ] **Step 1: Write the failing tests**

```java
@Test
void testGetProvisionableProjectsListsReleasedProjectsWithTheirEnabledWorkflows()
// publishAndPinProject(projectId) for the user's environment; expect one DTO whose projectVersion is the pinned
// version, with the workflow uuids of that version, provisioned=false, dangling=false, inputs equal to the template's.

@Test
void testGetProvisionableProjectsSkipsProjectsWithoutConfigurationInTheUserEnvironment()
// a published project with no configuration in the user's environment is absent.

@Test
void testGetProvisionableProjectsSkipsWorkflowsTurnedOffInTheConfiguration()
// automationWorkflowProjectConfigurationAdminFacade.enableConfigurationWorkflow(id, uuid, false); that uuid is absent.

@Test
void testGetProvisionableProjectsMarksProvisionedAndDanglingWorkflows()
// connectedUserWorkflowReferenceFacade.getOrCreateReference(externalId, uuid, env) then: provisioned=true, dangling=false;
// mark the row dangling through the repository helper the reference IntTest already uses: dangling=true.
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests '*ConnectedUserProjectAdminFacadeIntTest*'`
Expected: compilation failure on `getProvisionableProjects`.

- [ ] **Step 3: Implement**

Add the DTO; add the interface method; in `ConnectedUserProjectAdminFacadeImpl` inject `AutomationWorkflowProjectFacade`, `ConnectedUserService`, `ConnectedUserWorkflowReferenceFacade` (keep `ConnectedUserProjectFacade`). Map each published project to the DTO (`projectVersion = lastPublishedVersion()`), each template to `Workflow` with `provisioned`/`dangling` looked up in a `Map<String, ConnectedUserProjectWorkflow>` keyed by automation workflow uuid. Sort projects by name, case-insensitive.

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: all four pass.

- [ ] **Step 5: Commit**

`6770 Add the provisionable automation workflow projects read for a connected user`

### Task 2: Provision project workflows (server)

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/ProvisionConnectedUserProjectWorkflowDTO.java`
- Modify: `.../facade/ConnectedUserProjectAdminFacade.java`, `.../facade/ConnectedUserProjectAdminFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/exception/ConnectedUserProjectWorkflowErrorType.java` (add `WORKFLOW_NOT_PROVISIONABLE = new ConnectedUserProjectWorkflowErrorType(102)`)
- Test: `ConnectedUserProjectAdminFacadeIntTest`

**Interfaces:**
- Produces:
  ```java
  public record ProvisionConnectedUserProjectWorkflowDTO(String workflowUuid, @Nullable Map<String, ?> inputs) {}
  void provisionProjectWorkflows(long connectedUserId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows);
  ```
- Consumes: `ConnectedUserWorkflowReferenceFacade.getOrCreateReference(String externalUserId, String automationWorkflowUuid, Environment environment, Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs)`; `MissingConnectionException` (same package as the reference facade's exceptions); Task 1's `getProvisionableProjects`.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void testProvisionProjectWorkflowsCreatesReferencesLikeThePublicPath()
// provision two uuids; connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(userId) has both, enabled,
// not dangling, same projectDeploymentId shape as a reference created through getOrCreateReference directly.

@Test
void testProvisionProjectWorkflowsRejectsUnprovisionableUuidBeforeWriting()
// one valid uuid + one uuid turned off in the configuration: assertThatThrownBy(...).isInstanceOf(ConfigurationException.class)
// with errorType WORKFLOW_NOT_PROVISIONABLE; afterwards getConnectedUserWorkflows(userId) is empty.

@Test
void testProvisionProjectWorkflowsKeepsReferenceWhenConnectionIsMissing()
// project whose workflow needs a connection the user does not have, followed by a workflow that does not:
// call returns normally; both references exist; the first one's attention reason (via ConnectedUserProjectFacade
// read the sheet uses) names the missing connection.

@Test
void testProvisionProjectWorkflowsPassesInputsThroughAndLeavesThemAloneWhenOmitted()
// provision with inputs Map.of("name", "x"): deployment workflow inputs contain it; provision again with inputs null:
// still "x".
```

- [ ] **Step 2: Run the tests to verify they fail**

Same command as Task 1. Expected: compilation failure on `provisionProjectWorkflows`.

- [ ] **Step 3: Implement**

`@Transactional` on the method. Load the user; compute the set of provisionable uuids from `getProvisionableProjects(connectedUserId)` (every workflow, provisioned or not); if any requested uuid is outside it, `throw new ConfigurationException("Workflow %s cannot be provisioned for connected user %s", WORKFLOW_NOT_PROVISIONABLE)` before any write. Then for each request call `getOrCreateReference(externalId, uuid, environment, Map.of(), inputs == null || inputs.isEmpty() ? null : inputs)` inside `try { ... } catch (MissingConnectionException exception) { log at debug, continue }`.

- [ ] **Step 4: Run the tests to verify they pass**

Same command. Expected: all pass (Task 1 tests included).

- [ ] **Step 5: Commit**

`6770 Provision automation workflow references for a connected user from the admin facade`

### Task 3: GraphQL for provisionable projects and provisioning (server)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/connected-user-project.graphqls`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/ConnectedUserProjectGraphQlController.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/test/java/com/bytechef/ee/embedded/configuration/web/graphql/ConnectedUserProjectGraphQlControllerIntTest.java` (existing; `@GraphQlTest` with `@MockitoBean` facades)

**Interfaces:**
- Produces (schema):
  ```graphql
  extend type Query {
      provisionableConnectedUserProjects(connectedUserId: ID!): [ProvisionableConnectedUserProject!]!
  }
  extend type Mutation {
      provisionConnectedUserProjectWorkflows(connectedUserId: ID!, workflows: [ProvisionConnectedUserProjectWorkflowInput!]!): Boolean
  }
  type ProvisionableConnectedUserProject { projectId: ID!, name: String!, projectVersion: Int!, workflows: [ProvisionableConnectedUserProjectWorkflow!]! }
  type ProvisionableConnectedUserProjectWorkflow { workflowUuid: ID!, label: String, inputs: [ProvisionableConnectedUserProjectWorkflowInputDefinition!]!, provisioned: Boolean!, dangling: Boolean! }
  type ProvisionableConnectedUserProjectWorkflowInputDefinition { name: String!, label: String, type: String, required: Boolean! }
  input ProvisionConnectedUserProjectWorkflowInput { workflowUuid: ID!, inputs: Map }
  ```
- Consumes: Task 1 and Task 2 facade methods. The controller returns the DTOs directly (records map field by field); the mutation maps the input list to `ProvisionConnectedUserProjectWorkflowDTO` and returns `true`.

- [ ] **Step 1: Write the failing tests** (`testProvisionableConnectedUserProjects` asserting `projectId`, `projectVersion`, `workflows[0].provisioned`; `testProvisionConnectedUserProjectWorkflows` verifying the facade is called with `connectedUserId` and a list containing `("uuid-1", Map.of("name", "x"))` and `("uuid-2", null)`).
- [ ] **Step 2: Run to verify they fail**: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration --tests '*ConnectedUserProjectGraphQlControllerIntTest*'`
- [ ] **Step 3: Implement** schema and controller methods (`@QueryMapping provisionableConnectedUserProjects(@Argument long connectedUserId)`, `@MutationMapping provisionConnectedUserProjectWorkflows(@Argument long connectedUserId, @Argument List<ProvisionConnectedUserProjectWorkflowInput> workflows)` with a small `record ProvisionConnectedUserProjectWorkflowInput(String workflowUuid, Map<String, Object> inputs)` nested in the controller).
- [ ] **Step 4: Run to verify they pass.**
- [ ] **Step 5: Commit** `6770 Expose connected user workflow provisioning over GraphQL`

### Task 4: Provisionable MCP servers read (server)

**Files:**
- Create: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-api/src/main/java/com/bytechef/ee/embedded/ai/mcp/dto/ProvisionableConnectedUserMcpServerDTO.java`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-api/src/main/java/com/bytechef/ee/embedded/ai/mcp/facade/ConnectedUserMcpServerFacade.java`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/src/main/java/com/bytechef/ee/embedded/ai/mcp/facade/ConnectedUserMcpServerFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/build.gradle.kts` (add `implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))`; today only the service module is a test dependency)
- Test: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/src/test/java/com/bytechef/ee/embedded/ai/mcp/facade/ConnectedUserMcpServerFacadeIntTest.java` (existing; it already builds a tools server and a workflows server in `@BeforeEach` with `insert`/`insertMcpWorkflow` helpers)

**Interfaces:**
- Produces:
  ```java
  public record ProvisionableConnectedUserMcpServerDTO(
      long id, String name, boolean provisioned, List<String> coveredComponentNames, List<String> missingComponentNames) {}
  List<ProvisionableConnectedUserMcpServerDTO> getProvisionableMcpServers(long connectedUserId);
  ```
- Consumes: `ConnectedUserService.getConnectedUser(long)`, `McpServerService.getMcpServers(PlatformType.EMBEDDED)` filtered by `isEnabled()` and `getEnvironment() == connectedUser.getEnvironment()`, `McpComponentService.getMcpServerMcpComponents(long)`, `McpIntegrationInstanceConfigurationService.getMcpServerMcpIntegrationInstanceConfigurations(long)`, `IntegrationInstanceService.getConnectedUserIntegrationInstances(long)`, `IntegrationInstanceConfigurationService.getIntegrationInstanceConfiguration(long)`, `IntegrationService.getIntegration(long)`; `getConnectedUserMcpServers(connectedUserId)` for `provisioned`.

- [ ] **Step 1: Write the failing tests**

```java
@Test void testGetProvisionableMcpServersReportsCoverageFromComponentsAndConfigurations()
// tools server: covered = [componentName of the user's instance], missing = []; workflows server likewise via configuration id.
@Test void testGetProvisionableMcpServersListsMissingComponentsForUncoveredServer()
// a third server with an McpComponent "slack" the user has no instance for: covered=[], missing=["slack"].
@Test void testGetProvisionableMcpServersSkipsDisabledServersAndOtherEnvironments()
@Test void testGetProvisionableMcpServersMarksProvisionedServers()
// after the @BeforeEach rows exist, provisioned=true for both servers; after deleteConnectedUserMcpServer, false.
```

- [ ] **Step 2: Run to verify they fail**: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-service:testIntegration --tests '*ConnectedUserMcpServerFacadeIntTest*'`
- [ ] **Step 3: Implement** (private `Map<Long, Integration>`-style caches as the facade already does; coverage computed once per call from the user's instances: a set of component names and a set of configuration ids). Sort servers by name, case-insensitive; `@PreAuthorize("isTenantAdmin()")` like the other methods.
- [ ] **Step 4: Run to verify they pass.**
- [ ] **Step 5: Commit** `6770 Add the provisionable MCP servers read for a connected user`

### Task 5: Provision an MCP server (server)

**Files:**
- Create: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/src/main/java/com/bytechef/ee/embedded/ai/mcp/exception/ConnectedUserMcpServerErrorType.java` (`extends AbstractErrorType`, domain class `McpIntegrationInstanceTool.class`, `NO_COVERED_INTEGRATION = new ConnectedUserMcpServerErrorType(100)`; model on `ConnectedUserProjectWorkflowErrorType`)
- Modify: `ConnectedUserMcpServerFacade.java`, `ConnectedUserMcpServerFacadeImpl.java`
- Test: `ConnectedUserMcpServerFacadeIntTest`

**Interfaces:**
- Produces: `void provisionMcpServer(long connectedUserId, long mcpServerId);`
- Consumes: Task 4 coverage logic; `McpToolService.getMcpComponentMcpTools(long)`, `McpIntegrationInstanceToolService.fetchMcpIntegrationInstanceTool(long, long)` / `createMcpIntegrationInstanceTool(long, long, boolean)` / `updateEnabled(Long, boolean)`; `McpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(long)`, `IntegrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(long)` (`isEnabled()`, `getWorkflowId()`), `IntegrationInstanceFacade.enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable)` (already used by `enableConnectedUserMcpServer`).

- [ ] **Step 1: Write the failing tests**

```java
@Test void testProvisionMcpServerCreatesToolRowsOnlyForMatchingInstances()
// delete the user's rows, add a second instance of another component; provision the tools server:
// rows exist (enabled) for the matching instance and every enabled McpTool, none for the other instance.
@Test void testProvisionMcpServerReEnablesExistingRows()
// enableConnectedUserMcpServer(user, server, false); provision; every row enabled again.
@Test void testProvisionMcpServerSkipsGloballyDisabledTools()
@Test void testProvisionMcpServerEnablesConfigurationWorkflows()
// workflows server: after provisioning, getConnectedUserMcpServers reports its workflow enabled.
@Test void testProvisionMcpServerRejectsServerWithoutCoveredIntegration()
// the uncovered "slack" server: ConfigurationException with NO_COVERED_INTEGRATION; no rows created.
@Test void testProvisionMcpServerMakesTheServerAppearForTheUser()
// after deleteConnectedUserMcpServer, getConnectedUserMcpServers is empty; after provisionMcpServer it lists the server.
```

- [ ] **Step 2: Run to verify they fail** (same command as Task 4).
- [ ] **Step 3: Implement** (`@Transactional`, `@PreAuthorize("isTenantAdmin()")`; step order from the spec: reject, tool rows, configuration workflows).
- [ ] **Step 4: Run to verify they pass.**
- [ ] **Step 5: Commit** `6770 Provision an MCP server for a connected user from the admin facade`

### Task 6: GraphQL for MCP server provisioning (server)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-graphql/src/main/resources/graphql/connected-user-mcp-server.graphqls`
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-graphql/src/main/java/com/bytechef/ee/embedded/ai/mcp/web/graphql/ConnectedUserMcpServerGraphQlController.java`
- Test: the existing `ConnectedUserMcpServerGraphQlController` IntTest next to it (create one modelled on `ConnectedUserProjectGraphQlControllerIntTest` if none exists)

**Interfaces:**
- Produces (schema):
  ```graphql
  provisionableConnectedUserMcpServers(connectedUserId: ID!): [ProvisionableConnectedUserMcpServer!]!
  provisionConnectedUserMcpServer(connectedUserId: ID!, mcpServerId: ID!): Boolean
  type ProvisionableConnectedUserMcpServer { id: ID!, name: String!, provisioned: Boolean!, coveredComponentNames: [String!]!, missingComponentNames: [String!]! }
  ```
- [ ] **Step 1: Write the failing tests** (query returns the mocked DTO fields; mutation calls `provisionMcpServer(7L, 3L)` and returns `true`).
- [ ] **Step 2: Run to verify they fail**: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-graphql:testIntegration --tests '*ConnectedUserMcpServerGraphQlController*'`
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run to verify they pass.**
- [ ] **Step 5: Commit** `6770 Expose connected user MCP server provisioning over GraphQL`

### Task 7: Client GraphQL operations and generated types

**Files:**
- Create: `client/src/graphql/embedded/configuration/provisionableConnectedUserProjects.graphql`, `provisionConnectedUserProjectWorkflows.graphql`, `provisionableConnectedUserMcpServers.graphql`, `provisionConnectedUserMcpServer.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`, `client/src/shared/middleware/graphql-types.ts`

**Interfaces:**
- Produces hooks: `useProvisionableConnectedUserProjectsQuery({connectedUserId})`, `useProvisionConnectedUserProjectWorkflowsMutation()`, `useProvisionableConnectedUserMcpServersQuery({connectedUserId})`, `useProvisionConnectedUserMcpServerMutation()`; query keys `['provisionableConnectedUserProjects', variables]` and `['provisionableConnectedUserMcpServers', variables]` as codegen produces them.

- [ ] **Step 1: Write the four operations** (select every field of the new types; the mutations take `$connectedUserId: ID!` plus `$workflows: [ProvisionConnectedUserProjectWorkflowInput!]!` / `$mcpServerId: ID!`).
- [ ] **Step 2: Run** `cd client && npx graphql-codegen`; expected: `graphql.ts` exports the four hooks.
- [ ] **Step 3: Commit the operations** `6770 client - Add the connected user provisioning GraphQL operations`, then the generated files `6770 client - Regenerate GraphQL types for connected user provisioning`.

### Task 8: Workflow provisioning dialog and tab header (client)

**Files:**
- Create: `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-project-workflow-dialog/ProvisionProjectWorkflowDialog.tsx`
- Create: `.../provision-project-workflow-dialog/tests/ProvisionProjectWorkflowDialog.test.tsx`
- Modify: `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList.tsx` (header row `flex items-center justify-between pb-2`, `h3` "Automation Workflows", `Button` `icon={<PlusIcon />} label="New Workflow" size="sm" variant="secondary"`, dialog mounted on state like `ConnectedUserSheetPanelIntegrationList`)
- Test: `.../connected-user-sheet/tests/ConnectedUserSheetPanelWorkflowList.test.tsx` (existing; add the button + dialog-open case)

**Interfaces:**
- Props: `{connectedUserId: number; onClose: () => void}`.
- Form (react-hook-form): `{projectId: string; workflows: Array<{inputs: Record<string, string>; selected: boolean; workflowUuid: string}>}`; the workflows array is reset when the project changes.
- Consumes Task 7 hooks. On success: `void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']})`, `void queryClient.invalidateQueries({queryKey: ['connectedUsers']})`, then `onClose()`.

- [ ] **Step 1: Write the failing tests** (mock `@/shared/middleware/graphql` like `ProvisionIntegrationDialog.test.tsx` mocks its queries):
  - `shows the empty state when no project is provisionable` (text + only "Close").
  - `lists the chosen project's workflows and disables provisioned ones` ("Provisioned" badge, checkbox disabled; "Needs attention" for dangling).
  - `keeps Save disabled until a workflow is selected and its required inputs are filled`.
  - `submits the selected workflows with their inputs and closes` (mutation variables `{connectedUserId: '7', workflows: [{inputs: {name: 'x'}, workflowUuid: 'wf-1'}]}`, `onClose` called, both query keys invalidated).
- [ ] **Step 2: Run to verify they fail**: `cd client && npx vitest run src/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-project-workflow-dialog`
- [ ] **Step 3: Implement** the dialog (native `<select>` like `ProvisionIntegrationDialog`; inputs rendered as labelled text inputs under each checked workflow; required marker `*`) and the panel header.
- [ ] **Step 4: Run** the dialog tests, the panel tests and `npm run lint && npm run typecheck`; expected green.
- [ ] **Step 5: Commit** `6770 client - Provision automation workflows from the connected user sheet`

### Task 9: MCP server provisioning dialog and tab header (client)

**Files:**
- Create: `.../connected-user-sheet/provision-mcp-server-dialog/ProvisionMcpServerDialog.tsx` and `tests/ProvisionMcpServerDialog.test.tsx`
- Modify: `.../connected-user-sheet/ConnectedUserSheetPanelMcpServerList.tsx` (header row, `h3` "MCP Servers", button `label="New MCP Server"`)
- Test: `.../connected-user-sheet/tests/ConnectedUserSheetPanelMcpServerList.test.tsx` (existing; add the button + dialog-open case)

**Interfaces:**
- Props: `{connectedUserId: number; onClose: () => void}`; one radio group named `mcpServerId`; rows ordered as the query returns them, provisioned rows after unprovisioned ones.
- On success: invalidate `['connectedUserMcpServers']` and `['connectedUsers']`, then `onClose()`.

- [ ] **Step 1: Write the failing tests**: empty state; row lines for covered / partially covered / uncovered (disabled radio) servers; "Provisioned" badge keeps the radio enabled; Save disabled until a selectable server is chosen; submit calls the mutation with `{connectedUserId: '7', mcpServerId: '3'}` and closes.
- [ ] **Step 2: Run to verify they fail**: `npx vitest run src/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-mcp-server-dialog`
- [ ] **Step 3: Implement** the dialog and the panel header.
- [ ] **Step 4: Run** dialog + panel tests, `npm run lint && npm run typecheck`.
- [ ] **Step 5: Commit** `6770 client - Provision MCP servers from the connected user sheet`

### Task 10: Whole-branch verification

- [ ] `./gradlew -p server/ee/libs/embedded/embedded-configuration spotlessApply check` and `./gradlew -p server/ee/libs/embedded/embedded-ai spotlessApply check` (unit, integration and static checks) green.
- [ ] `cd client && npm run format && npm run check` green.
- [ ] No commit here unless formatting changed files; if it did, fold with `git commit --fixup` + `git rebase -i --autosquash master`.

Docs: no page under `docs/src/content/docs` describes the connected user sheet today, so the spec's docs paragraph has nowhere to go; none is added (ruling recorded here).
