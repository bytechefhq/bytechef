# Disable Workflow Triggers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users disable individual triggers of a multi-trigger workflow so a disabled trigger never starts a run of a deployed workflow (issue #7293).

**Architecture:** A `disabled` flag on the trigger entry of the workflow definition, parsed into `WorkflowTrigger`, with
one shared `isEnabled()` / `ofEnabled(workflow)` rule (manual triggers always count as enabled). Every server path that
activates, resolves, lists or calls a trigger switches to that rule; a save-time guard keeps at least one enabled
trigger. The client adds the toggle to trigger nodes, reusing the #6243 task toggle machinery.

**Tech Stack:** Java 25 / Spring Boot 4, JUnit 5 + Mockito + AssertJ, React 19 / TypeScript, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-08-disable-workflow-triggers-design.md`

## Global Constraints

- Branch `7293`, based on `7284` (PR #7285). Do not rebase onto master until #7285 merges.
- Flag key `disabled` (`WorkflowConstants.DISABLED`), written only when `true`, omitted when enabled. Existing workflows stay byte-identical.
- A `manual` trigger always counts as enabled, even with `"disabled": true` in the JSON.
- Disabled triggers still require their connections to deploy. Do not touch connection-requirement code.
- Takes effect on next publish and deploy only. No live-deployment switch.
- Commit subjects: server `7293 <Description>`, client `7293 client - <Description>`; generated files (`generateOpenAPI` output) in their own `7293 Generate` / `7293 client - Generate` commits. Subject line only; no code comments in added lines.
- Run `./gradlew spotlessApply` before every server commit, `cd client && npm run format` before every client commit.
- Java: blank line before control statements, no method chaining on results, descriptive names, no trailing blank line in class body.
- Client: sort-keys, sorted import destructures, `Icon` suffix, `twMerge`, hook ordering per CLAUDE.md.

## Review Focus

1. **Manual trigger with `"disabled": true` written by Copilot/raw JSON** must still count as enabled everywhere (deploy skip, request check, save guard). Test added in Task 1 (`testIsEnabledManualTriggerIgnoresDisabledFlag`) and Task 2 (`testNoEnabledTriggerAllowsDisabledManual`).
2. **Disabled trigger placed first in the `triggers` array**: `findFirst()`/`getFirst()` consumers (API collection creation, webhook URL listing, hosted chat mode) must pick the first *enabled* one. Tests in Task 5 (`testCreateApiCollectionSkipsDisabledFirstTrigger`) and Task 6 (`testGetStaticWebhookUrlSkipsDisabledTrigger`).
3. **Version swap where the old version had the trigger enabled and the new one disabled** must disable it (old flags) and not re-enable it (new flags). Test in Task 3 (`testUpdateDeploymentDisablesTriggerTurnedOffInNewVersion`).
4. **A workflow with zero triggers** must still save (the guard applies only when triggers exist). Test in Task 2 (`testNoEnabledTriggerAllowsWorkflowWithoutTriggers`).
5. **Toggling a trigger while another save is in flight** must queue like task toggles do and keep `workflow.triggers` in the store in sync. Test in Task 9 (`toggles a trigger and updates workflow.triggers`).

---

### Task 1: Trigger model, DTO and OpenAPI

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/domain/WorkflowTrigger.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/dto/WorkflowTriggerDTO.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml` (`WorkflowTrigger` schema at ~3538; copy the `disabled` property shape from `WorkflowTask` at ~3485)
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/test/java/com/bytechef/platform/configuration/domain/WorkflowTriggerTest.java`

**Interfaces:**
- Produces:
  - `boolean WorkflowTrigger.isDisabled()` — raw flag.
  - `boolean WorkflowTrigger.isEnabled()` — `!disabled || WorkflowNodeType.ofType(type).name().equals("manual")`.
  - `static List<WorkflowTrigger> WorkflowTrigger.ofEnabled(Workflow workflow)` — `of(workflow)` filtered by `isEnabled()`, order preserved.
  - `WorkflowTriggerDTO.disabled` (boolean component) and OpenAPI `WorkflowTrigger.disabled` → client type `WorkflowTrigger.disabled?: boolean`.

- [ ] **Step 1: Write failing tests in `WorkflowTriggerTest`** (use `@ExtendWith(ObjectMapperSetupExtension.class)` if `JsonUtils` is needed)
  - `testParsesDisabledFlag`: `new WorkflowTrigger(Map.of("name","trigger_1","type","webhook/v1/autoRespondWithHTTP200","disabled",true)).isDisabled()` is true and `getExtensions()` does not contain `"disabled"`.
  - `testToMapWritesDisabledOnlyWhenTrue`: disabled trigger's `toMap()` has `"disabled" -> true`; enabled trigger's `toMap()` has no `"disabled"` key.
  - `testIsEnabledManualTriggerIgnoresDisabledFlag`: `type "manual/v1/manual"`, `disabled true` → `isEnabled()` true.
  - `testOfEnabledFiltersDisabledTriggers`: workflow with `trigger_1` disabled webhook, `trigger_2` enabled schedule → `ofEnabled` returns only `trigger_2`.
  - `testEqualsIgnoresDisabled`: two triggers differing only by `disabled` are equal.
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:test --tests '*WorkflowTriggerTest'` — expect FAIL (methods missing).
- [ ] **Step 3: Implement** the field, parsing branch for `WorkflowConstants.DISABLED` (atlas `WorkflowConstants`), `toMap()` write when true, `isDisabled`, `isEnabled`, `ofEnabled`. Not in `equals`/`hashCode`.
- [ ] **Step 4: Run tests** — expect PASS.
- [ ] **Step 5: Add `disabled` to `WorkflowTriggerDTO`** (from `workflowTrigger.isDisabled()`) and the OpenAPI schema, then run `./gradlew -p server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl generateOpenAPI`. Run `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:testIntegration --tests '*WorkflowFacadeIntTest'` — expect PASS.
- [ ] **Step 6: Commit** in three commits: `7293 Add a disabled flag to the workflow trigger model` (domain, DTO, yaml, test), `7293 Generate` (server generated), `7293 client - Generate` (client `middleware/**` generated).

### Task 2: Save-time guard and validator rules

**Files:**
- Modify: `server/libs/platform/platform-workflow/platform-workflow-validator/platform-workflow-validator-service/src/main/java/com/bytechef/platform/workflow/validator/WorkflowValidator.java` (next to `getDuplicateNodeNames` ~974; `hasChatCapableTrigger` ~863; `validateWorkflow` ~86-170)
- Modify: `.../platform-workflow-validator-api/src/main/java/com/bytechef/platform/workflow/validator/WorkflowValidatorFacade.java` (next to `validateNoDuplicateNodeNames` ~131)
- Modify: `.../platform-workflow-validator-api/.../exception/WorkflowValidatorErrorType.java` (add `NO_ENABLED_TRIGGER = new WorkflowValidatorErrorType(102)`)
- Modify: `WorkflowFacadeImpl.java:89`, `ProjectWorkflowFacadeImpl.java:162`, EE `IntegrationWorkflowFacadeImpl.java:108` — call the new guard right after `validateNoDuplicateNodeNames(definition)`.
- Test: `WorkflowValidatorTest.java`, `WorkflowValidatorFacadeIntTest.java`, `WorkflowFacadeIntTest.java`

**Interfaces:**
- Produces:
  - `static boolean WorkflowValidator.hasNoEnabledTrigger(String workflow)` — true when `triggers` is a non-empty array and no entry is enabled (enabled = `disabled` not true, or type component name `manual`). Fails open (false) on unparsable JSON, like `getDuplicateNodeNames`.
  - `default void WorkflowValidatorFacade.validateHasEnabledTrigger(String workflow)` — throws `ConfigurationException("A workflow must have at least one enabled trigger.", WorkflowValidatorErrorType.NO_ENABLED_TRIGGER)`.

- [ ] **Step 1: Write failing tests in `WorkflowValidatorTest`**
  - `testNoEnabledTriggerDetectsAllDisabled`: two disabled webhook triggers → true.
  - `testNoEnabledTriggerAllowsOneEnabled`: one disabled, one enabled → false.
  - `testNoEnabledTriggerAllowsDisabledManual`: single `manual/v1/manual` with `disabled true` → false.
  - `testNoEnabledTriggerAllowsWorkflowWithoutTriggers`: no `triggers` key, and `"triggers": []` → false.
  - `testValidateWorkflowReportsNoEnabledTriggerError`: `validateWorkflow` on an all-disabled definition yields an ERROR issue (kind `OTHER`) with message `A workflow must have at least one enabled trigger.`
  - `testValidateWorkflowWarnsOnDisabledManualTrigger`: disabled manual trigger yields a WARNING `Manual triggers cannot be disabled; the disabled flag is ignored.`
  - `testChatApprovalWarningIgnoresDisabledChatTrigger`: the existing chat-only approval channel warning fires when the only chat trigger is disabled (i.e. `hasChatCapableTrigger` returns false for it).
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-service:test --tests '*WorkflowValidatorTest'` — expect FAIL.
- [ ] **Step 3: Implement** `hasNoEnabledTrigger`, the two validation issues inside `validateWorkflow`, the `hasChatCapableTrigger` skip, the error type, and `validateHasEnabledTrigger`. Wire the guard into the three facades.
- [ ] **Step 4: Add IntTest** `testUpdateRejectsWorkflowWithoutEnabledTrigger` in `WorkflowFacadeIntTest`: updating a workflow definition whose triggers are all disabled throws `ConfigurationException` with `NO_ENABLED_TRIGGER`.
- [ ] **Step 5: Run** the unit test plus `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:testIntegration --tests '*WorkflowFacadeIntTest'` and `./gradlew :server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-service:testIntegration` — expect PASS.
- [ ] **Step 6: Check the direct writers** (`AutomationWorkflowProjectFacadeImpl`, `ConnectedUserProjectWorkflowManager`, EE `CodeWorkflowContainerFacadeImpl`, `DemoProjectConfiguration`): add `validateHasEnabledTrigger` only where the definition comes from a user or API; list the outcome for each in the commit's PR notes.
- [ ] **Step 7: Commit** `7293 Reject saving a workflow whose triggers are all disabled`.

### Task 3: Deploy and teardown skip disabled triggers (CE + EE)

**Files:**
- Modify: `ProjectDeploymentFacadeImpl.java` — `enableWorkflowTriggers` (~801) and `disableWorkflowTriggers` (~728): iterate `WorkflowTrigger.ofEnabled(workflow)` instead of `of(workflow)`. Leave the required-connection block (~759) and `getConnectionId` untouched.
- Modify: EE `IntegrationInstanceFacadeImpl.java` — `enableWorkflowTriggers` (~392) and `disableWorkflowTriggers` (~335): same change.
- Test: `ProjectDeploymentFacadeIntTest.java`, `IntegrationInstanceFacadeIntTest.java`

**Interfaces:**
- Consumes: `WorkflowTrigger.ofEnabled(Workflow)` (Task 1).

- [ ] **Step 1: Write failing IntTests** (mock/spy `TriggerLifecycleFacade` the way the existing tests in these classes do)
  - `ProjectDeploymentFacadeIntTest.testEnableSkipsDisabledTrigger`: workflow with disabled `trigger_1` (schedule) and enabled `trigger_2` (webhook); enabling the deployment calls `executeTriggerEnable` for `trigger_2` only.
  - `ProjectDeploymentFacadeIntTest.testDisableSkipsDisabledTrigger`: disabling the same deployment calls `executeTriggerDisable` for `trigger_2` only.
  - `ProjectDeploymentFacadeIntTest.testUpdateDeploymentDisablesTriggerTurnedOffInNewVersion`: old version `trigger_1` enabled, new version `trigger_1` disabled → `executeTriggerDisable(trigger_1)` called once, `executeTriggerEnable(trigger_1)` not called after the swap.
  - `ProjectDeploymentFacadeIntTest.testEnableStillRequiresDisabledTriggerConnection`: disabled trigger with a required connection and no connection supplied → enabling fails as today.
  - `IntegrationInstanceFacadeIntTest.testEnableSkipsDisabledTrigger` and `testDisableSkipsDisabledTrigger`: same as above for integration instances.
- [ ] **Step 2: Run** `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*ProjectDeploymentFacadeIntTest'` and the EE module's `testIntegration --tests '*IntegrationInstanceFacadeIntTest'` — expect FAIL.
- [ ] **Step 3: Implement** the four loop changes.
- [ ] **Step 4: Run** both — expect PASS.
- [ ] **Step 5: Commit** `7293 Skip disabled triggers when enabling and disabling deployed workflows`.

### Task 4: Refuse incoming requests for a disabled trigger

**Files:**
- Modify: `WebhookWorkflowExecutorImpl.java:274` `isWorkflowDisabled` — also return true when `WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow)` is non-null and `!isEnabled()` (resolve the workflow as `getComponentOperation` ~393 does). Update the Javadoc on `WebhookWorkflowExecutor.isWorkflowDisabled` (~123) to say "workflow or its trigger".
- Modify: `WebhookWebSocketHandler.java:325` — before `executeAsync`, if `webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)`, close the session with `CloseStatus.POLICY_VIOLATION.withReason("Workflow is disabled.")` and return.
- Modify: `AbstractWebhookTriggerController.java:83` `doProcessTrigger` — at the top, if `isWorkflowDisabled`, return `ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Workflow is disabled."))` (match the body `WebhookTriggerController` uses at ~100-116; reuse its helper if one exists).
- Modify: `TriggerFormApiController.java:62-81` `getTriggerForm` — return 404 when the named trigger is disabled.
- Test: `WebhookWorkflowExecutorTest.java`; create `WebhookWebSocketHandlerTest.java` in `platform-websocket-webhook-rest/src/test/java/com/bytechef/platform/webhook/web/websocket/` only if that module already has test infrastructure, otherwise cover via the executor test and note it.

**Interfaces:**
- Consumes: `WorkflowTrigger.isEnabled()` (Task 1).
- Produces: `WebhookWorkflowExecutor.isWorkflowDisabled(WorkflowExecutionId)` now covers trigger-level disable (all existing callers — `WebhookTriggerController`, `stream()`, `RequestTriggerApiController`, `AppEventTriggerApiController` — inherit it).

- [ ] **Step 1: Write failing tests in `WebhookWorkflowExecutorTest`**
  - `testIsWorkflowDisabledWhenTriggerDisabled`: workflow enabled, named trigger disabled → true.
  - `testIsWorkflowDisabledFalseForEnabledTrigger`: workflow enabled, trigger enabled → false.
  - `testIsWorkflowDisabledFalseForDisabledManualTrigger`: manual trigger with `disabled true` → false.
  - `testStreamRejectsDisabledTrigger`: `stream(...)` on a disabled trigger calls `sseStreamBridge.onError` with message `Workflow is disabled.` and never creates a job.
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-webhook:platform-webhook-impl:test --tests '*WebhookWorkflowExecutorTest'` — expect FAIL.
- [ ] **Step 3: Implement** the executor change, then the websocket, `doProcessTrigger` and form changes.
- [ ] **Step 4: Run** the test plus `./gradlew :server:libs:platform:platform-webhook:check` — expect PASS.
- [ ] **Step 5: Commit** `7293 Refuse webhook, websocket, API Platform and form requests for a disabled trigger`.

### Task 5: MCP, A2A and API Platform resolve only enabled triggers

**Files (switch trigger lookups from `WorkflowTrigger.of(workflow)` to `WorkflowTrigger.ofEnabled(workflow)`):**
- `McpWorkflowUtils.java`, `A2aWorkflowTriggerUtils.java`
- `AutomationMcpToolFacade.java` (~211, ~348), `A2AAgentExecutorImpl.java` (~201, ~425), EE `EmbeddedMcpToolFacade.java` (~504, ~687)
- `McpProjectWorkflowMappingValidator.java:146`, `ListMcpProjectWorkflowsToolCallback.java:180`
- EE `McpIntegrationInstanceConfigurationWorkflowFacadeImpl.java:217`, `McpIntegrationInstanceConfigurationWorkflowGraphQlController.java:242`
- EE `ApiCollectionFacadeImpl.java:238-248` and `ApiPlatformResponseToApiRequestAction.java:121` (`findFirst()` over enabled triggers)
- EE `ApiPlatformHandlerController.java:202` `getApiPlatformTriggerName` (enabled triggers only)
- Test: `ListMcpProjectWorkflowsToolCallbackTest.java`, `A2AAgentExecutorTest.java`, `ApiCollectionFacadeIntTest.java`, `ApiPlatformResponseToApiRequestActionTest.java`, `McpProjectWorkflowMappingValidatorIntTest.java`

**Interfaces:**
- Consumes: `WorkflowTrigger.ofEnabled(Workflow)` (Task 1). Where a site reads raw maps rather than `WorkflowTrigger`, filter with the same rule (`!Boolean.TRUE.equals(map.get("disabled"))` or manual); prefer converting to `WorkflowTrigger` if the site already has a `Workflow`.

- [ ] **Step 1: Write failing tests**
  - `ListMcpProjectWorkflowsToolCallbackTest.testOmitsWorkflowWhoseMcpTriggerIsDisabled`.
  - `A2AAgentExecutorTest.testRejectsWorkflowWhoseA2aTriggerIsDisabled`: no job created, error response as for a workflow without an A2A trigger.
  - `ApiCollectionFacadeIntTest.testCreateApiCollectionSkipsDisabledFirstTrigger`: `trigger_1` disabled webhook, `trigger_2` enabled `apiPlatform/v1/newApiRequest` → creation succeeds.
  - `ApiPlatformResponseToApiRequestActionTest.testUsesFirstEnabledTrigger`: same layout, resolves `trigger_2`.
  - `McpProjectWorkflowMappingValidatorIntTest.testRejectsMappingToDisabledMcpTrigger`.
- [ ] **Step 2: Run** each module's `test` / `testIntegration` for those classes — expect FAIL.
- [ ] **Step 3: Implement** the lookup switches at every listed site.
- [ ] **Step 4: Run** the same tests plus `AutomationMcpToolFacadeIntTest` and `EmbeddedMcpToolFacadeIntTest` — expect PASS.
- [ ] **Step 5: Commit** `7293 Expose MCP, A2A and API Platform workflows only through enabled triggers`.

### Task 6: Webhook URL and chat listings use enabled triggers

**Files:**
- Modify: `ProjectDeploymentFacadeImpl.getStaticWebhookUrl` (~927): iterate `ofEnabled`.
- Modify: `ProjectDeploymentWorkflowGraphQlController.java` `workflowExecutionId` (~138-160) and `hasHostedChatTrigger` (~265-281, replace `getFirst()` with the first enabled chat trigger).
- Modify: EE `ConnectedUserWorkflowWebhookUrlResolver.java:84-93`: enabled triggers only.
- Test: `ProjectDeploymentFacadeIntTest.java`, `ConnectedUserWorkflowWebhookUrlResolverTest.java`; create `ProjectDeploymentWorkflowGraphQlControllerIntTest.java` only if a GraphQL controller IntTest pattern exists in that module, otherwise test via the facade.

- [ ] **Step 1: Write failing tests**
  - `ProjectDeploymentFacadeIntTest.testGetStaticWebhookUrlSkipsDisabledTrigger`: `trigger_1` disabled webhook, `trigger_2` enabled webhook → URL contains `trigger_2`.
  - `ProjectDeploymentFacadeIntTest.testGetStaticWebhookUrlNullWhenOnlyWebhookDisabled`: only webhook disabled, other trigger schedule → null.
  - `ConnectedUserWorkflowWebhookUrlResolverTest.testResolvesFirstEnabledWebhookTrigger`.
- [ ] **Step 2: Run** — expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — expect PASS.
- [ ] **Step 5: Commit** `7293 List webhook URLs and hosted chats only for enabled triggers`.

### Task 7: Subflows refuse a disabled callable trigger

**Files:**
- Modify: `SubflowResolverImpl.java:55-69` `getCallableTriggerName`: iterate `ofEnabled`; keep the existing `No callable trigger found in workflow '%s'` message.
- Modify: `SubflowDataSourceImpl.java:~148`: do not offer workflows whose callable trigger is disabled.
- Test: `SubflowResolverTest.java`, `SubflowDataSourceTest.java`

- [ ] **Step 1: Write failing tests**
  - `SubflowResolverTest.testDisabledCallableTriggerThrows`: child with disabled `workflow/v1/newWorkflowCall` → `IllegalStateException` with message `No callable trigger found in workflow '<id>'`.
  - `SubflowDataSourceTest.testOmitsWorkflowWithDisabledCallableTrigger`.
- [ ] **Step 2: Run** `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*Subflow*Test'` — expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — expect PASS.
- [ ] **Step 5: Commit** `7293 Refuse subflow calls through a disabled callable trigger`.

### Task 8: Server test run defaults to the first enabled trigger

**Files:**
- Modify: `TestWorkflowExecutorImpl.java:485-496` `getTestWorkflowTrigger`: with a null/blank `triggerName`, return the first trigger where `isEnabled()`; an explicitly named trigger is returned even if disabled; if no trigger is enabled, fall back to the first trigger.
- Test: `TestWorkflowExecutorIntTest.java`

- [ ] **Step 1: Write failing IntTests**
  - `testDefaultTestRunSkipsDisabledTrigger`: `trigger_1` disabled, `trigger_2` enabled, no name → SSE `start` event names `trigger_2`.
  - `testExplicitTestRunOfDisabledTriggerAllowed`: name `trigger_1` (disabled) → run starts from `trigger_1`.
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-service:testIntegration --tests '*TestWorkflowExecutorIntTest'` — expect FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — expect PASS.
- [ ] **Step 5: Commit** `7293 Start a default test run from the first enabled trigger`.

### Task 9: Client toggle for triggers

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/toggleNodeDisabled.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/toggleNodeDisabled.test.ts`

**Interfaces:**
- Consumes: client `WorkflowTrigger.disabled?: boolean` (Task 1 generate).
- Produces: `toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName})` (signature unchanged) now also toggles a top-level entry of `workflowDefinition.triggers`; `applyDefinitionWithDisabledFlags` also updates `state.workflow.triggers` and trigger nodes.

- [ ] **Step 1: Write failing tests** in `toggleNodeDisabled.test.ts`
  - `toggles a trigger and updates workflow.triggers`: definition with `trigger_1`, `trigger_2`; toggling `trigger_1` sends a definition whose `triggers[0].disabled === true`, and `useWorkflowDataStore.getState().workflow.triggers[0].disabled === true` and the `trigger_1` node's `data.disabled === true`.
  - `re-enabling a trigger removes the disabled key`: `'disabled' in triggers[0]` is false after the second toggle.
  - `rolls back the trigger flag when the mutation fails`.
- [ ] **Step 2: Run** `cd client && npx vitest run src/pages/platform/workflow-editor/utils/toggleNodeDisabled.test.ts` — expect FAIL.
- [ ] **Step 3: Implement**: try `triggers` before `tasks` (`toggleInTriggers(definition.triggers, name)` using `flipDisabled`); build the flag map from triggers too and apply it to `state.workflow.triggers`.
- [ ] **Step 4: Run** — expect PASS.
- [ ] **Step 5: Commit** `7293 client - Toggle the disabled flag of workflow triggers`.

### Task 10: Client trigger menu and muted rendering

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/nodes/WorkflowNode.tsx` (~653 `isEffectivelyDisabled`, ~657 `triggerCount`, ~992 and ~1071 `showDisableAction`)
- Test: `client/src/pages/platform/workflow-editor/nodes/WorkflowNode.test.tsx`

**Interfaces:**
- Produces: `showDisableAction` for a trigger = `!isManualTrigger && (data.disabled || enabledTriggerCount > 1)` where `enabledTriggerCount` counts `workflow.triggers` entries that are not disabled or are manual, and `isManualTrigger` = the trigger's component name is `manual`. Enable (when `data.disabled`) is always shown.

- [ ] **Step 1: Write failing tests**
  - `shows Disable on a trigger when another trigger is enabled`.
  - `hides Disable on the last enabled trigger`.
  - `hides Disable on a manual trigger`.
  - `shows Enable on a disabled trigger`.
  - `renders a disabled trigger muted with the Disabled badge` (`opacity-50`, `grayscale`, badge text `Disabled`).
- [ ] **Step 2: Run** `cd client && npx vitest run src/pages/platform/workflow-editor/nodes/WorkflowNode.test.tsx` — expect FAIL.
- [ ] **Step 3: Implement** with a `useMemo` for `enabledTriggerCount`. Do not add disabled-reference warnings for `${trigger_x}` (spec: none).
- [ ] **Step 4: Run** — expect PASS.
- [ ] **Step 5: Commit** `7293 client - Offer Disable and Enable on trigger nodes`.

### Task 11: Client test-run pick skips disabled triggers

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/getTestRunTriggerName.ts` (and `getSelectedTestRunTriggerName.ts` if it applies its own fallback)
- Test: `getTestRunTriggerName.test.ts`, `getSelectedTestRunTriggerName.test.ts`

**Interfaces:**
- Produces: `getTestRunTriggerName(workflow: {triggers?: Array<Pick<WorkflowTrigger, 'disabled' | 'name' | 'type'>>}, selectedNodeName?: string): string` — the selected trigger if found and enabled; otherwise the first enabled trigger (manual counts as enabled); otherwise the first trigger; otherwise `'trigger_1'`.

- [ ] **Step 1: Write failing tests**
  - `uses the selected trigger when it is enabled`.
  - `skips a selected disabled trigger and uses the first enabled one`.
  - `skips a disabled first trigger when nothing is selected`.
  - `treats a disabled manual trigger as enabled`.
- [ ] **Step 2: Run** `cd client && npx vitest run src/pages/platform/workflow-editor/utils/getTestRunTriggerName.test.ts` — expect FAIL.
- [ ] **Step 3: Implement.** The explicit per-trigger test in the trigger's own panel is unchanged.
- [ ] **Step 4: Run** — expect PASS, then `cd client && npm run check` — expect PASS.
- [ ] **Step 5: Commit** `7293 client - Start a default test run from the first enabled trigger`.

### Task 12: Docs and full verification

**Files:**
- Modify: `docs/content/docs/platform/automation/build/workflows/workflows.mdx` (~280 "Multiple triggers", ~546 test-run paragraph)

- [ ] **Step 1: Update docs**: "On deployment every **enabled** trigger is enabled"; add that a trigger can be disabled from its node menu, takes effect on the next publish and deploy, never starts a run while disabled, still needs its connections, the last enabled trigger and manual triggers cannot be disabled, and a subflow calling a workflow whose callable trigger is disabled fails. Test-run paragraph: "from the trigger whose panel is open if it is enabled, otherwise the first enabled trigger".
- [ ] **Step 2: Commit** `7293 Document disabling workflow triggers`.
- [ ] **Step 3: Full verification**: `./gradlew spotlessApply && ./gradlew check` and `./gradlew testIntegration` for every module touched in Tasks 1-8; `cd client && npm run check`. Expected: all PASS. Report any failures with output; do not claim done without it.
