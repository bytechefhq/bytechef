# Configure MCP Tools While Adding Them Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users configure MCP component and workflow tools (Tool Name, Tool Description, inputs) in the same dialog where they add them, with AI-filled defaults and a "Let AI fill all" action, in automation and embedded; fix two settings-wipe bugs on the way.

**Architecture:** Two bug fixes land first. Then the server gains a "tool properties for a not-yet-added workflow" query and a `toolParameters` list on the MCP project / MCP integration instance configuration save inputs (MCP-specific, so A2A and deployments are untouched). On the client, the gear-popover form bodies become shared form components; pure helpers compute `=fromAi(...)` values (with options, including dynamically loaded ones); the component dialog renders the form inline under each ticked tool and the MCP workflow dialogs render it in a new optional **Tool** tab.

**Tech Stack:** React 19 + TypeScript, react-hook-form, TanStack Query, GraphQL codegen, Vitest; Spring Boot 4 / Spring GraphQL, JUnit 5 IntTests (Testcontainers).

**Spec:** `docs/superpowers/specs/2026-11-11-mcp-inline-tool-configuration-design.md` (issue #7460)

**Worktree:** `/Volumes/Data/bytechef/bytechef-7460`, branch `7460_mcp_inline_tool_configuration`. Never work in `/Volumes/Data/bytechef/bytechef-master`.

## Global Constraints

- Commit subjects: server `7460 <Description>`, client `7460 client - <Description>`; subject only, no trailers, no `Co-Authored-By`, no "Generated with" line. Client and server changes in separate commits; generated `graphql.ts` in its own commit.
- Configuration is never required to save: Save/Update/Add must never be blocked by tool forms (only briefly while AI-fill seeding is pending, see Task 11).
- "Let AI fill all" fills **only empty fields** (undefined, null, `''`); a field showing its untouched built-in default counts as filled.
- Every `=fromAi(...)` for a field with options carries the full options list; if options cannot be loaded, the field stays empty.
- Untouched built-in defaults are not saved.
- Newly added tools start AI-filled and are saved that way even if the form is never opened; existing tools are never re-seeded.
- Workflow **Inputs** tab is never AI-settable and is unchanged; only New Workflow Call schema fields are.
- The A2A, project deployment, integration instance configuration and provided-values dialogs look exactly as today.
- Client: sorted object keys, interfaces end in `I`/`Props`, sorted import destructures, `Icon`-suffixed lucide imports, `twMerge`, `fieldset border-0`, hook order (state → ref → store → custom → memo/callback → effects), no IIFEs in JSX, descriptive names.
- Server: `@PreAuthorize` on facades; services never call services; EE files keep the EE header and `@version ee`; blank line before control statements and after variable modification; IntTest suffix for integration tests.
- Run `./gradlew spotlessApply` (server) and `npm run format` + targeted `npx eslint <files>` + targeted `npx vitest run <paths>` (client) before each commit. No full client lint/typecheck per task; one `npm run check` at the end.

## Review Focus

1. **Edit Tools on a component whose tools were configured via the gear** → Update keeps every kept tool's saved values (Task 1 test `keeps saved parameters of kept tools on update`).
2. **Dynamic-options field with no connection selected** → field is left empty, never `fromAi` without options (Task 8 test `leaves dynamic options field empty when options cannot be loaded`).
3. **User ticks a tool and clicks Save immediately** (form never rendered, options still loading) → Save waits for seeding and persists AI-filled values (Task 11 test `saves seeded ai values when save is clicked before the form renders`).
4. **User clears an AI-filled field, then saves** → the cleared value is saved as empty, not re-seeded (Task 11 test `does not re-seed a field the user cleared`).
5. **Re-toggling a workflow off and on in Add Workflows** → its Tool tab values reset to fresh seeds and the untoggled workflow sends no `toolParameters` (Task 13 test `drops tool parameters of workflows toggled off`).

---

## Phase A: Bug fixes (ship first, own PR)

### Task 1: Edit Tools keeps saved tool parameters (automation + embedded)

**Files:**
- Modify: `client/src/pages/automation/mcp-servers/components/mcp-component-dialog/hooks/useMcpComponentDialogToolSelectionStep.ts` (`SelectedToolType`, pre-selection effect)
- Modify: `client/src/pages/automation/mcp-servers/components/mcp-component-dialog/hooks/useMcpComponentDialog.ts:131-160` (both mutations)
- Modify: the same two files under `client/src/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/hooks/` (`SelectedToolI`)
- Test: `.../mcp-component-dialog/hooks/tests/useMcpComponentDialog.test.ts` in both trees (create if missing)

**Interfaces:**
- Produces: `SelectedToolType` / `SelectedToolI` gain `parameters?: Record<string, unknown> | null`. Pre-selected tools copy `existingTool.parameters`. Save sends `parameters: tool.parameters ?? {}`.

- [ ] **Step 1: Write the failing tests** (both trees): `keeps saved parameters of kept tools on update` - render the hook with `mcpComponent` and `existingTools` returning `[{name: 'createContact', parameters: {firstName: "=fromAi('firstName', 'STRING', {'required': true})"}}]`, call `handleSave`, assert the update mutation received `tools: [{name: 'createContact', parameters: {firstName: "=fromAi('firstName', 'STRING', {'required': true})"}}]`. And `sends empty parameters for newly ticked tools` - a tool added via `setSelectedTools` without `parameters` is sent with `parameters: {}`.
- [ ] **Step 2: Run** `cd client && npx vitest run src/pages/automation/mcp-servers/components/mcp-component-dialog src/ee/pages/embedded/mcp-servers/components/mcp-component-dialog` - Expected: FAIL (parameters `{}`).
- [ ] **Step 3: Implement** the field on the selected-tool types, copy it in the pre-selection effect, use it in both mutation payloads.
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 client - Keep saved tool settings when updating the tools of an MCP component`.

### Task 2: EE version update carries workflow tool parameters and enabled state

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/src/main/java/com/bytechef/ee/embedded/ai/mcp/facade/McpIntegrationInstanceConfigurationFacadeImpl.java:327-401` (`updateMcpIntegrationInstanceConfigurationVersion`)
- Test: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-service/src/test/java/com/bytechef/ee/embedded/ai/mcp/facade/McpIntegrationInstanceConfigurationFacadeIntTest.java`

**Interfaces:**
- Consumes: `IntegrationWorkflowService.getWorkflowIntegrationWorkflow(String workflowId)` (gives the stable `getUuidAsString()`), `McpIntegrationInstanceConfigurationWorkflowService.updateParameters(long, Map<String, ?>)` and `updateEnabled(long, boolean)`.
- Produces: no signature change.

- [ ] **Step 1: Write the failing test** `testUpdateVersionKeepsWorkflowToolParametersAndEnabled`: set up an MCP integration instance configuration on version 1 with workflow W (uuid U) whose MCP workflow row has `parameters = {message: "=fromAi('message', 'STRING', {'required': true}), obsolete: "x"}` and `enabled = false`; version 2 contains the workflow with uuid U whose trigger schema only defines `message`. Call `updateMcpIntegrationInstanceConfigurationVersion(id, 2, List.of(U))`. Assert the recreated row has `parameters == {message: "=fromAi('message', 'STRING', {'required': true})}` (obsolete key dropped) and `enabled == false`. Second test `testUpdateVersionDropsWorkflowsMissingInNewVersion` keeps today's behaviour.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-service:testIntegration --tests '*McpIntegrationInstanceConfigurationFacadeIntTest*'` - Expected: FAIL (parameters empty, enabled true).
- [ ] **Step 3: Implement**: before the delete loop, build `Map<String, McpIntegrationInstanceConfigurationWorkflow> previousByUuid` keyed by the uuid of the integration workflow behind each row (row → `IntegrationInstanceConfigurationWorkflow.getWorkflowId()` → `getWorkflowIntegrationWorkflow(workflowId).getUuidAsString()`). After each `create(...)` in the recreate loop, if `previousByUuid` has the uuid: call `updateParameters` with the previous parameters filtered to the keys present in the new workflow's callable trigger input schema plus the two tool keys (use the same schema-to-property path the properties query uses: `McpWorkflowUtils.getCallableTrigger` + `SchemaUtils.getJsonSchemaProperty`; tool keys from `ToolPropertyUtils`), and `updateEnabled` with the previous value.
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 Keep MCP workflow tool settings when the integration instance configuration version changes`.

---

## Phase B: Server support for workflow tools

### Task 3: Automation query for tool properties of a not-yet-added workflow

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-graphql/src/main/java/com/bytechef/automation/ai/mcp/web/graphql/McpProjectWorkflowGraphQlController.java:92-142`
- Modify: `.../automation-ai-mcp-graphql/src/main/resources/graphql/mcp-project-workflow.graphqls`
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-api/src/main/java/com/bytechef/automation/ai/mcp/facade/McpProjectWorkflowFacade.java` + `.../automation-ai-mcp-service/.../facade/McpProjectWorkflowFacadeImpl.java`
- Test: `.../automation-ai-mcp-graphql/src/test/java/com/bytechef/automation/ai/mcp/web/graphql/McpProjectWorkflowGraphQlControllerIntTest.java`, `.../automation-ai-mcp-service/src/test/java/com/bytechef/automation/ai/mcp/facade/McpProjectWorkflowFacadeIntTest.java`

**Interfaces:**
- Produces (GraphQL): `toolEligibleWorkflowProperties(projectId: ID!, workflowId: String!): [Property]`.
- Produces (Java): `Workflow McpProjectWorkflowFacade.getToolEligibleWorkflow(long projectId, String workflowId)` with `@PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')")`, `@Transactional(readOnly = true)`; throws `IllegalArgumentException` if the workflow does not belong to the project.
- Produces (controller private): `List<Property> getWorkflowToolProperties(Workflow workflow)` - the body of today's `mcpProjectWorkflowProperties` from `getCallableTrigger` on; the existing query now calls it.

- [ ] **Step 1: Write the failing tests**: controller `testToolEligibleWorkflowProperties` returns `[toolName, toolDescription, message]` names for a project workflow with a New Workflow Call trigger whose schema defines `message`; existing `mcpProjectWorkflowProperties` tests still pass. Facade `testGetToolEligibleWorkflowRejectsWorkflowOfAnotherProject`.
- [ ] **Step 2: Run** `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-graphql:testIntegration :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:testIntegration --tests '*McpProjectWorkflow*'` - Expected: FAIL.
- [ ] **Step 3: Implement** the facade method, the shared private builder, and the query mapping.
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 Add a query for the tool properties of a project workflow before it becomes an MCP tool`.

### Task 4: Automation `toolParameters` on MCP project create/update

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-mcp/automation-ai-mcp-api/src/main/java/com/bytechef/automation/ai/mcp/dto/WorkflowToolParametersDTO.java`
- Modify: `McpProjectFacade` / `McpProjectFacadeImpl` (5-arg `createMcpProject`, 3-arg `updateMcpProject`), `McpProjectGraphQlController` records `CreateMcpProjectInput` / `UpdateMcpProjectInput`, `mcp-project.graphqls`
- Test: `McpProjectFacadeIntTest.java`, `McpProjectGraphQlControllerIntTest.java`

**Interfaces:**
- Produces (Java): `record WorkflowToolParametersDTO(String workflowId, Map<String, ?> parameters)`; facade signatures become `createMcpProject(long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds, List<WorkflowConfigurationDTO> workflowConfigurations, List<WorkflowToolParametersDTO> toolParameters)` and `updateMcpProject(long mcpProjectId, List<String> selectedWorkflowIds, List<WorkflowConfigurationDTO> workflowConfigurations, List<WorkflowToolParametersDTO> toolParameters)`; the old overloads delegate with `List.of()`.
- Produces (GraphQL): `input WorkflowToolParametersInput { workflowId: String!, parameters: Map }`; `toolParameters: [WorkflowToolParametersInput!]` on `CreateMcpProjectInput` and `UpdateMcpProjectInput` (null → empty list in the records' compact constructors).

- [ ] **Step 1: Write the failing tests**: `testCreateMcpProjectStoresToolParameters` (new MCP workflow row has the given parameters); `testUpdateMcpProjectStoresToolParametersForNewAndExistingWorkflows`; `testUpdateMcpProjectLeavesParametersOfWorkflowsWithoutToolParameters` (existing row's parameters unchanged when no entry is sent).
- [ ] **Step 2: Run** `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service:testIntegration --tests '*McpProjectFacadeIntTest*'` - Expected: FAIL (compile).
- [ ] **Step 3: Implement**: index `toolParameters` by `workflowId`; after `mcpProjectWorkflowService.create(...)` (create and the update's add branch) and for kept workflows in update, call `mcpProjectWorkflowService.updateParameters(id, parameters)` when an entry exists.
- [ ] **Step 4: Run** the service and graphql `testIntegration` tasks - Expected: PASS.
- [ ] **Step 5: Commit** `7460 Accept MCP workflow tool parameters when creating and updating an MCP project`.

### Task 5: Embedded query + `toolParameters` (EE)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-graphql/src/main/java/com/bytechef/ee/embedded/ai/mcp/web/graphql/McpIntegrationInstanceConfigurationWorkflowGraphQlController.java` (extract builder like Task 3), `McpIntegrationInstanceConfigurationGraphQlController.java` (inputs), `mcp-integration-instance-configuration-workflow.graphqls`, `mcp-integration-instance-configuration.graphqls`
- Modify: `McpIntegrationInstanceConfigurationWorkflowFacade` / `Impl`, `McpIntegrationInstanceConfigurationFacade` / `Impl`
- Create: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-api/src/main/java/com/bytechef/ee/embedded/ai/mcp/dto/WorkflowToolParametersDTO.java` (EE header, `@version ee`)
- Test: `McpIntegrationInstanceConfigurationWorkflowFacadeIntTest.java`, `McpIntegrationInstanceConfigurationFacadeIntTest.java`, the EE graphql controller IntTests

**Interfaces:**
- Produces (GraphQL): `toolEligibleIntegrationInstanceConfigurationWorkflowProperties(integrationInstanceConfigurationId: ID!, workflowId: String!): [Property]`; `toolParameters: [WorkflowToolParametersInput!]` on `CreateMcpIntegrationInstanceConfigurationInput` / `UpdateMcpIntegrationInstanceConfigurationInput`. `WorkflowToolParametersInput` is declared once per schema module (EE module declares its own if the automation schema is not on its classpath; check `embedded-ai-mcp-graphql` test `test.graphqls`).
- Produces (Java): `Workflow getToolEligibleIntegrationInstanceConfigurationWorkflow(long integrationInstanceConfigurationId, String workflowId)` with `@PreAuthorize("isTenantAdmin()")`; facade create/update overloads gain `List<WorkflowToolParametersDTO> toolParameters`.

- [ ] **Step 1: Write the failing tests** mirroring Task 3 and Task 4 (same names with `IntegrationInstanceConfiguration` in place of `Project`).
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-service:testIntegration :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-graphql:testIntegration` - Expected: FAIL.
- [ ] **Step 3: Implement** as in Tasks 3–4.
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 Accept MCP workflow tool parameters and expose tool properties for embedded MCP workflows`.

### Task 6: Client GraphQL operations and codegen

**Files:**
- Create: `client/src/graphql/automation/.../toolEligibleWorkflowProperties.graphql`, `client/src/graphql/embedded/.../toolEligibleIntegrationInstanceConfigurationWorkflowProperties.graphql` (place beside the existing `mcpProjectWorkflowProperties` / embedded equivalent operation files; select the same `Property` fields they select)
- Modify: `client/src/shared/middleware/graphql.ts` (generated)

- [ ] **Step 1:** Add the two operations; ensure the create/update MCP project and MCP integration instance configuration mutations' variables accept `toolParameters` (they pass `input`, so no operation change is expected).
- [ ] **Step 2: Run** `cd client && npx graphql-codegen` - Expected: `useToolEligibleWorkflowPropertiesQuery`, `useToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery` and `WorkflowToolParametersInput` exist in `graphql.ts`.
- [ ] **Step 3: Commit** operations: `7460 client - Add GraphQL operations for MCP workflow tool properties`; then generated file alone: `7460 client - Regenerate GraphQL types`.

---

## Phase C: Client

### Task 7: Extract the `fromAi` expression builder

**Files:**
- Create: `client/src/pages/platform/workflow-editor/components/properties/utils/buildFromAiExpression.ts`
- Modify: `client/src/pages/platform/workflow-editor/components/properties/hooks/useProperty.ts:407-442` (use the helper)
- Test: `client/src/pages/platform/workflow-editor/components/properties/utils/tests/buildFromAiExpression.test.ts`

**Interfaces:**
- Produces: `buildFromAiExpression(params: FromAiExpressionParamsI): string` with `interface FromAiExpressionParamsI {arrayName?: string; defaultValue?: unknown; description?: string; name: string; options?: Array<{value?: unknown}> | null; required?: boolean; type: string}`. Output identical to today's memo (same escaping, same entry order: description, defaultValue unless it starts with `=`, options, required; `arrayName_name` qualification).

- [ ] **Step 1: Write the failing tests**: `builds expression with description options and required` → `buildFromAiExpression({description: "It's", name: 'format', options: [{value: 'JSON'}, {value: 'XML'}], required: true, type: 'STRING'})` equals `"=fromAi('format', 'STRING', {'description': 'It''s', 'options': {'JSON', 'XML'}, 'required': true})"`; `skips expression shaped default values`; `qualifies array item names`; `defaults required to false`.
- [ ] **Step 2: Run** `npx vitest run src/pages/platform/workflow-editor/components/properties/utils` - Expected: FAIL.
- [ ] **Step 3: Implement** and replace the memo body in `useProperty` with a call.
- [ ] **Step 4: Run** `npx vitest run src/pages/platform/workflow-editor/components/properties` - Expected: PASS (existing property tests unchanged).
- [ ] **Step 5: Commit** `7460 client - Extract the fromAi expression builder from useProperty`.

### Task 8: AI-fill computation helpers

**Files:**
- Create: `client/src/pages/platform/mcp-servers/utils/mcpToolAiFill.ts`
- Test: `client/src/pages/platform/mcp-servers/utils/tests/mcpToolAiFill.test.ts`

**Interfaces:**
- Consumes: `buildFromAiExpression` (Task 7), `PropertyAllType`.
- Produces:
  - `isFromAiEligibleProperty(property: PropertyAllType): boolean` - mirrors the controlled branches that render a `FromAiToggleButton` in `Property.tsx` (~lines 690–1080): `expressionEnabled !== false` and (`type === 'STRING'` with a text-like control, or `controlType` `SELECT` / `MULTI_SELECT`, or `optionsDataSource` present). False for `OBJECT`/`ARRAY` containers, `hidden`, and the tool name/description properties (names from the server's `ToolPropertyUtils`; read the exact names from the property list returned for any tool).
  - `isEmptyToolValue(value: unknown): boolean` - `undefined`, `null`, `''`.
  - `type LoadPropertyOptionsType = (property: PropertyAllType, path: string, values: Record<string, unknown>) => Promise<Array<{value?: unknown}> | null>`
  - `computeAiFillValues(params: {hiddenPaths?: Set<string>; includeAdvanced: boolean; includeConditional: boolean; loadOptions: LoadPropertyOptionsType; properties: PropertyAllType[]; values: Record<string, unknown>}): Promise<Record<string, string>>` - returns `{path: expression}` for each eligible, visible, empty field; recurses into `OBJECT` properties with dotted paths; static options from the property, dynamic via `loadOptions` (null → field skipped); a field with `defaultValue` present in `values` is not empty.

- [ ] **Step 1: Write the failing tests**: `fills empty eligible string field`; `skips field holding its default value`; `skips field the user typed into`; `skips object and array containers`; `skips tool name and description`; `skips hidden and advanced fields when not included`; `skips conditional fields when includeConditional is false`; `includes static options`; `includes dynamically loaded options`; `leaves dynamic options field empty when options cannot be loaded`; `fills nested eligible field with dotted path`.
- [ ] **Step 2: Run** `npx vitest run src/pages/platform/mcp-servers/utils` - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 client - Add helpers that compute AI-filled values for MCP tool properties`.

### Task 9: Dynamic options loader and payload builder

**Files:**
- Create: `client/src/pages/platform/mcp-servers/hooks/useMcpToolOptionsLoader.ts`
- Create: `client/src/pages/platform/mcp-servers/utils/buildToolParametersPayload.ts`
- Move: `toFormParameters` / `toStoredParameters` from `useMcpComponentToolPropertiesPopover.ts` into `client/src/pages/platform/mcp-servers/utils/toolParameterConversion.ts` (re-imported by the popover hook)
- Test: `client/src/pages/platform/mcp-servers/hooks/tests/useMcpToolOptionsLoader.test.ts`, `client/src/pages/platform/mcp-servers/utils/tests/buildToolParametersPayload.test.ts`

**Interfaces:**
- Produces: `useMcpToolOptionsLoader(params: {componentName: string; componentVersion: number; connectionId?: string | null; toolName: string}): LoadPropertyOptionsType` - uses `queryClient.fetchQuery` with key `['clusterElementOptions', variables]` and `useClusterElementOptionsQuery.fetcher` (variables: `clusterElementName: toolName`, `propertyName: path`, `connectionId`, `inputParameters: values`, `lookupDependsOnPaths` from the property). Resolves `null` on error, on empty result, or when the property needs a connection (`connectionRequired`) and `connectionId` is empty.
- Produces: `buildToolParametersPayload(params: {aiValues: Record<string, string>; dirtyValues: Record<string, unknown>; savedParameters?: Record<string, unknown> | null}): Record<string, unknown>` = `toStoredParameters({...savedParameters, ...aiValues, ...dirtyValues})`; untouched defaults are never included.
- Workflow tools pass a loader that always resolves `null` for dynamic options (schema fields have only static options).

- [ ] **Step 1: Write the failing tests**: loader `resolves options from the cluster element options query`, `resolves null without a connection when the property needs one`, `resolves null on error`; payload `merges saved ai and dirty values with dirty winning`, `omits untouched defaults`, `stores cleared field as null`.
- [ ] **Step 2: Run** `npx vitest run src/pages/platform/mcp-servers` - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 client - Add an options loader and a parameters payload builder for MCP tool forms`.

### Task 10: Shared tool forms with "Let AI fill all"; popovers use them

**Files:**
- Create: `client/src/pages/platform/mcp-servers/components/LetAiFillAllButton.tsx`
- Create: `client/src/pages/platform/mcp-servers/components/McpComponentToolForm.tsx`
- Create: `client/src/pages/platform/mcp-servers/components/McpWorkflowToolForm.tsx`
- Modify: `client/src/pages/platform/mcp-servers/components/McpComponentToolPropertiesPopover.tsx`, `client/src/pages/automation/mcp-servers/components/mcp-project-workflow-list/McpProjectWorkflowPropertiesPopover.tsx`, `client/src/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationWorkflowPropertiesPopover.tsx`
- Test: `client/src/pages/platform/mcp-servers/components/tests/McpComponentToolForm.test.tsx`, `.../tests/McpWorkflowToolForm.test.tsx`, existing popover tests

**Interfaces:**
- Produces: `LetAiFillAllButton({disabled, onClick}: LetAiFillAllButtonProps)` - `SparklesIcon` + label `Let AI fill all`, right-aligned, `variant="ghost"`, `size="sm"`.
- Produces: `McpComponentToolForm({componentName, componentVersion, connectionId, connectionRequired, form, properties, toolName}: McpComponentToolFormProps)` where `form: UseFormReturn<Record<string, unknown>>` - renders `Properties` (`toolsMode`, `controlPath=""`, live display conditions via `useFormDisplayConditions` with `operationName: toolName`) inside `ClusterElementProvider`; the Tool Name and Tool Description properties render first, then `LetAiFillAllButton`, then the rest. Clicking the button runs `computeAiFillValues({hiddenPaths: <from display conditions>, includeAdvanced: <advanced section expanded>, includeConditional: true, loadOptions: useMcpToolOptionsLoader(...), properties, values: form.getValues()})` and `form.setValue(path, expression, {shouldDirty: true})` per entry; disabled when the computation would return nothing (compute on value change, debounced) or while running.
- Produces: `McpWorkflowToolForm({form, properties}: McpWorkflowToolFormProps)` - same layout without `ClusterElementProvider`, loader resolving `null` for dynamic options.
- Popovers keep their header, Cancel/Save and save mutation; their body becomes the form component. Popover save now sends `buildToolParametersPayload({aiValues: {}, dirtyValues: <dirty fields>, savedParameters: mcpTool.parameters})`.

- [ ] **Step 1: Write the failing tests**: `renders let ai fill all below tool description`; `fills only empty eligible fields on click`; `does not overwrite typed values`; `disables let ai fill all when nothing is eligible`; popover test `saves the ai filled values` (component and workflow).
- [ ] **Step 2: Run** `npx vitest run src/pages/platform/mcp-servers src/pages/automation/mcp-servers/components/mcp-project-workflow-list src/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list` - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 client - Share the MCP tool forms between popovers and add Let AI fill all`.

### Task 11: Inline tool forms in the component dialog (automation + embedded)

**Files:**
- Create: `client/src/pages/platform/mcp-servers/hooks/useMcpToolSeeding.ts`
- Modify: both `McpComponentDialogToolSelectionStep.tsx`, both `useMcpComponentDialogToolSelectionStep.ts`, both `useMcpComponentDialog.ts`, both `McpComponentDialog.tsx` (Save spinner), `client/src/shared/components/mcp-server/McpComponentDialogShell.tsx` (`isSavePending` prop on Save/Update button)
- Test: both trees' `McpComponentDialog.test.tsx` and `McpComponentDialogToolSelectionStep` tests; `client/src/pages/platform/mcp-servers/hooks/tests/useMcpToolSeeding.test.ts`

**Interfaces:**
- Consumes: Tasks 8–10.
- Produces: `useMcpToolSeeding(params: {componentName: string; componentVersion: number; connectionId?: string | null; newToolNames: string[]}): {aiValuesByTool: Record<string, Record<string, string>>; isSeeding: boolean; waitForSeeding: () => Promise<Record<string, Record<string, string>>>}` - for each newly ticked tool, fetches its definition (`useClusterElementDefinitionQuery.fetcher` via `queryClient.fetchQuery`, key as the popover uses) and runs `computeAiFillValues({includeAdvanced: false, includeConditional: false, loadOptions, properties, values: <property defaults>})`. Recomputes for a tool when `connectionId` changes, keeping fields the user touched.
- Produces: selected-tool state per tool: `{name, parameters?, ...}` plus one `useForm` per ticked tool held in the step (keyed by tool name; created on tick, discarded on untick). Form default values: edit-mode saved values (`toFormParameters`) for existing tools; property defaults + `aiValuesByTool[name]` for new tools.
- Save: `await waitForSeeding()`, then per tool `buildToolParametersPayload({aiValues: isNew ? aiValuesByTool[name] : {}, dirtyValues: <dirty values of that tool's form, if rendered>, savedParameters: isNew ? undefined : existingParameters})`.

- [ ] **Step 1: Write the failing tests**: `expands the tool form when a tool is ticked`; `collapses and discards the form when a tool is unticked`; `prefills existing tools from saved parameters in edit mode`; `seeds new tools with ai values`; `does not re-seed existing tools`; `saves seeded ai values when save is clicked before the form renders`; `does not re-seed a field the user cleared`; `save is not blocked by empty required fields`; embedded equivalents of the payload tests.
- [ ] **Step 2: Run** `npx vitest run src/pages/automation/mcp-servers/components/mcp-component-dialog src/ee/pages/embedded/mcp-servers/components/mcp-component-dialog src/pages/platform/mcp-servers src/shared/components/mcp-server` - Expected: FAIL.
- [ ] **Step 3: Implement.** Each ticked row renders `McpComponentToolForm` below the row inside a bordered `fieldset` indented under the checkbox column.
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 client - Configure MCP component tools inline while selecting them`.

### Task 12: Optional Tool tab on workflow cards

**Files:**
- Modify: `client/src/shared/components/workflow-configuration/WorkflowConfigurationCard.tsx` (optional `toolContent?: ReactNode`; when set, adds a third `TabsTrigger value="tool"` labelled `Tool` with `WrenchIcon`)
- Modify: `client/src/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem.tsx` (pass-through `toolContent?: ReactNode`)
- Modify: `client/src/pages/automation/server-workflows/ServerProjectWorkflowsList.tsx` (`renderToolContent?: (workflow: Workflow, enabled: boolean) => ReactNode`)
- Modify: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationWorkflowsList.tsx` (same `renderToolContent` prop)
- Test: `client/src/shared/components/workflow-configuration/tests/WorkflowConfigurationCard.test.tsx`

- [ ] **Step 1: Write the failing tests**: `does not render a tool tab by default`; `renders the tool tab when tool content is provided`.
- [ ] **Step 2: Run** `npx vitest run src/shared/components/workflow-configuration` - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `npx vitest run src/shared/components/workflow-configuration src/pages/automation/server-workflows src/pages/automation/a2a-servers src/pages/automation/project-deployments` - Expected: PASS (A2A and deployments unchanged).
- [ ] **Step 5: Commit** `7460 client - Allow an optional Tool tab on workflow configuration cards`.

### Task 13: Tool tab in the automation Add/Edit Workflows dialog

**Files:**
- Create: `client/src/pages/automation/mcp-servers/components/McpProjectWorkflowToolTab.tsx` (+ hook `useMcpProjectWorkflowToolTab.ts` beside it)
- Modify: `client/src/pages/automation/mcp-servers/components/McpProjectWorkflowDialog.tsx` (pass `renderToolContent`, keep `toolFormsByWorkflowId`, send `toolParameters`)
- Test: `client/src/pages/automation/mcp-servers/components/McpProjectWorkflowDialog.test.tsx`

**Interfaces:**
- Consumes: `useToolEligibleWorkflowPropertiesQuery({projectId, workflowId})` (Task 6), `McpWorkflowToolForm`, `computeAiFillValues`, `buildToolParametersPayload`.
- Produces: on save, `toolParameters: WorkflowToolParametersInput[]` for selected workflows only: new workflows → seeded AI values + dirty values; existing workflows (edit mode, matched via `mcpProject.mcpProjectWorkflows[].projectDeploymentWorkflow.workflowId` or the field the list hook already uses) → saved parameters + dirty values, and only when the form is dirty.
- Seeding for new workflows uses `includeAdvanced: false, includeConditional: false` and a loader resolving `null` for dynamic options; Save awaits pending property loads.

- [ ] **Step 1: Write the failing tests**: `shows a tool tab with tool name description and schema fields`; `does not show ai controls on the inputs tab`; `sends seeded tool parameters for new workflows`; `prefills the tool tab from saved parameters in edit mode`; `drops tool parameters of workflows toggled off`.
- [ ] **Step 2: Run** `npx vitest run src/pages/automation/mcp-servers/components/McpProjectWorkflowDialog.test.tsx` - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the same command plus `npx vitest run src/pages/automation/a2a-servers` - Expected: PASS.
- [ ] **Step 5: Commit** `7460 client - Configure MCP workflow tools in a Tool tab while adding them`.

### Task 14: Tool tab in the embedded workflow dialog

**Files:**
- Create: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationWorkflowToolTab.tsx` (+ hook)
- Modify: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog.tsx`, `client/src/ee/pages/embedded/mcp-servers/components/hooks/useMcpIntegrationWorkflowsForm.ts`
- Test: `client/src/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog.test.tsx`

**Interfaces:**
- Consumes: `useToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery({integrationInstanceConfigurationId, workflowId})`; everything else as Task 13.

- [ ] **Step 1: Write the failing tests**: same five names as Task 13.
- [ ] **Step 2: Run** `npx vitest run src/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog.test.tsx` - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `7460 client - Configure embedded MCP workflow tools in a Tool tab while adding them`.

### Task 15: Final verification

- [ ] **Step 1:** `./gradlew spotlessApply && ./gradlew :server:libs:automation:automation-ai:automation-ai-mcp:check :server:ee:libs:embedded:embedded-ai:check :server:libs:platform:platform-mcp:check` - Expected: BUILD SUCCESSFUL.
- [ ] **Step 2:** `cd client && npm run check` - Expected: lint, typecheck and tests pass.
- [ ] **Step 3:** Boot the app from the worktree and check in the browser: tick a component tool (form expands, AI-filled), Save without opening anything, reopen the gear (values present); Edit Tools → Update keeps values; Add Workflows → Tool tab → Add; embedded equivalents.
