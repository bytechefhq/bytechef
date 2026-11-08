# Disable individual workflow triggers

## Intent

A workflow can have several triggers (for example a schedule and a webhook). Users want to switch one of them
off temporarily, keeping its configuration, without deleting it. Today a deployment enables every trigger in the
workflow, so deleting the trigger is the only way to stop it from firing.

#6243 added Disable / Enable for tasks and explicitly left triggers out of scope. This design extends the same
behaviour to triggers.

## Goals

- The trigger node menu on the canvas gets the same **Disable / Enable** toggle tasks have.
- A disabled trigger is stored in the workflow definition as `"disabled": true` on the trigger entry. The key is
  omitted when the trigger is enabled, so existing workflows stay byte-identical. No database migration.
- A disabled trigger never starts a run of a deployed workflow, by any path: activation on deploy, incoming
  webhook / websocket / API Platform / request / app event calls, MCP, A2A, and subflow calls.
- A disabled trigger renders muted with the existing `DisabledNodeBadge`, like a disabled task.
- The last enabled trigger of a workflow cannot be disabled (user's decision).

## Non-goals

- Changing a live deployment immediately. Toggling a trigger only edits the workflow definition; it takes effect
  on the next publish and deploy (user's decision). A running deployment keeps the triggers it was deployed with.
- A per-deployment trigger on/off switch (outside the workflow definition).
- Disabling cluster elements (AI agent tools). Still out of scope, as in #6243.
- A `SKIPPED` status or any execution-history record for disabled triggers.
- Relaxing connection requirements for disabled triggers (see "Connections").

## Anti-goals

- A workflow that deploys successfully but can never start because every trigger is disabled.
- A disabled trigger that still fires in any edition (CE project deployments or EE embedded integration
  instances).
- A deployment page or chat list that offers a URL or chat for a disabled trigger.
- Existing workflows changing on save when nobody touched a trigger's disabled state.

## Model

- `WorkflowTrigger` gets an explicit `disabled` field with `isDisabled()`, parsed from `WorkflowConstants.DISABLED`
  so it no longer lands in `extensions` (otherwise `toMap()` would write it twice). `toMap()` writes it only when
  true. It does not join `equals`/`hashCode`, following `WorkflowTask`.
- Add a shared helper (for example `WorkflowTrigger.ofEnabled(workflow)`) and use it in every path below, rather
  than repeating `!isDisabled()` filters across the copy-pasted trigger lookups.
- Expose `disabled` on `WorkflowTriggerDTO` and the platform configuration OpenAPI `WorkflowTrigger` model (as #6243
  did for tasks), regenerate, and carry it through the client trigger types and saves.

## Behaviour

### Editor

- The trigger node menu shows **Disable** / **Enable**. Following the Delete precedent
  (`WorkflowNode.tsx:991` `showDeleteAction={!data.trigger || triggerCount > 1}`), **Disable is hidden when the trigger
  is the only enabled trigger** (my call; no new disabled-with-tooltip menu infrastructure). Enable is always shown.
  A workflow with a single trigger therefore never offers Disable.
- **Manual triggers cannot be disabled** (user's decision). Disable is never shown on a `manual` trigger. The
  deployment Run button ignores triggers, so disabling one would change nothing; a workflow with a manual trigger
  can always be run by hand. A `"disabled": true` written onto a manual trigger by other means is ignored by the
  enabled-trigger helper (treated as enabled) and reported as a validator warning.
- The toggle mirrors `toggleNodeDisabled.ts` (optimistic update, rollback on error, the mutation guard queue), but
  walks the definition's `triggers` array, and `applyDefinitionWithDisabledFlags` must also update
  `state.workflow.triggers` in `useWorkflowDataStore`; otherwise the test-run trigger pickers that read
  `workflow.triggers` go stale until refetch.
- Disabled triggers render muted (`opacity-50 grayscale`) with the "Disabled" badge.
- No "references a disabled node" warning for `${trigger_x...}` references (my call): with several triggers only the
  firing trigger's output exists anyway, so the warning would be noise.

### Test runs (builds on #7284)

- Default pick: the open trigger panel's trigger if it is enabled, otherwise the first **enabled** trigger.
  `getTestRunTriggerName.ts` replaces its literal `'trigger_1'` fallback with this rule.
- The server mirrors it: `TestWorkflowExecutorImpl.getTestWorkflowTrigger` with no trigger name picks the first
  enabled trigger.
- **An explicit test of a disabled trigger is still allowed** (my call, matching #6243, where disabled tasks can
  still be tested): an explicitly named trigger runs even if disabled, and the editor's webhook test listener
  (`WebhookTriggerTestFacadeImpl`) is not blocked. It runs under the test principal, not a deployment, so it does
  not violate the anti-goal, and it lets users check a trigger before re-enabling it.

### Deployment and undeployment

- Activation skips disabled triggers: CE `ProjectDeploymentFacadeImpl.enableWorkflowTriggers` and EE
  `IntegrationInstanceFacadeImpl.enableWorkflowTriggers`.
- Teardown skips triggers that are disabled in the version being torn down (CE `disableWorkflowTriggers`, EE
  `IntegrationInstanceFacadeImpl.disableWorkflowTriggers`). Running `executeTriggerDisable` on a never-enabled trigger
  is not harmless: DYNAMIC_WEBHOOK and HYBRID triggers call the component's `executeWebhookDisable` with empty state
  and log an error. The version swap (`checkProjectDeploymentWorkflows`) already disables with the old version and
  enables with the new one, so a trigger flipped between versions transitions correctly.

### Request-time check

- Extend `WebhookWorkflowExecutorImpl.isWorkflowDisabled` to also return true when the trigger named in the
  `WorkflowExecutionId` is disabled in the deployed workflow version. `WebhookTriggerController`, `stream()`,
  `RequestTriggerApiController` and `AppEventTriggerApiController` inherit it and answer through their existing
  disabled-workflow branch (410, SSE error, 404, silent skip respectively). The `workflowSyncOnEnableValidation`
  handshake keeps behaving as it does for a disabled workflow.
- Add the check where it is missing today:
  - `WebhookWebSocketHandler` (around `:325`) has no disabled check at all, not even workflow level. Add the full
    check.
  - `ApiPlatformHandlerController` resolves the trigger by name and calls `doProcessTrigger`; check there (or in
    `AbstractWebhookTriggerController.doProcessTrigger`).
- `TriggerFormApiController.getTriggerForm` returns not-found for a disabled form trigger instead of serving a form
  whose submission will be refused.

### Lookups that resolve a workflow's trigger

These do not go through the webhook executor, so they must use the enabled-trigger helper:

- MCP / A2A job creation and exposure: `McpWorkflowUtils`, `A2aWorkflowTriggerUtils`, `AutomationMcpToolFacade`,
  `A2AAgentExecutorImpl`, `EmbeddedMcpToolFacade`, `McpProjectWorkflowMappingValidator`,
  `ListMcpProjectWorkflowsToolCallback`, `McpIntegrationInstanceConfigurationWorkflowFacadeImpl`,
  `McpIntegrationInstanceConfigurationWorkflowGraphQlController`. A workflow whose matching trigger is disabled is
  not exposed and cannot be called.
- API Platform: `ApiCollectionFacadeImpl` (`:238-248`) and `ApiPlatformResponseToApiRequestAction` (`:121`) take
  `findFirst()` and reject when it is not `newApiRequest`. They must pick the first **enabled** trigger, or a
  disabled non-API trigger placed first breaks API collection creation.
- Webhook URL and chat listings: `ProjectDeploymentFacadeImpl.getStaticWebhookUrl`,
  `ProjectDeploymentWorkflowGraphQlController.workflowExecutionId`, `ConnectedUserWorkflowWebhookUrlResolver`, and
  the hosted chat listing `hasHostedChatTrigger` (which uses `getFirst()` for `mode`) consider only enabled
  triggers, so the deployment page never shows a URL that answers 410 and the chat list never shows a chat that
  cannot start.
- Subflows (user's decision: block): `SubflowResolverImpl.getCallableTriggerName` and `SubflowDataSourceImpl` skip
  disabled triggers, so a parent's subflow step that calls a child whose callable trigger is disabled fails with the
  existing "No callable trigger found" error. The subflow picker no longer offers that child.
- Validator: `WorkflowValidator.hasChatCapableTrigger` ignores disabled chat triggers.

### Connections (reversed from the first draft)

- Disabled triggers **still require their connections** to deploy, exactly as disabled tasks do today
  (`doEnableProjectDeploymentWorkflow` uses `workflow.getTasks(true)`, which keeps disabled tasks). No change to the
  deploy dialog, `WorkflowFacadeImpl` trigger connections, `IntegrationInstanceConfigurationFacadeImpl`, or the
  MCP / A2A connection collectors.
- Why: relaxing it in embedded would provision connected users without that credential. When a later version
  re-enables the trigger, `IntegrationInstanceFacadeImpl.enableWorkflowTriggers` would fail for every existing
  instance, with no UI to collect the missing credential.

### Save-time guard

- A hard check on save, alongside `validateNoDuplicateNodeNames` / `validateInputNames`: saving a definition that
  has triggers but no enabled trigger fails. Wire it in every path that runs those validators:
  `WorkflowFacadeImpl.update`, `ProjectWorkflowFacadeImpl.addWorkflow`, and EE `IntegrationWorkflowFacadeImpl.addWorkflow`.
  Check the direct `workflowService.create/update` writers (`AutomationWorkflowProjectFacadeImpl`,
  `ConnectedUserProjectWorkflowManager`, EE `CodeWorkflowContainerFacadeImpl`, `DemoProjectConfiguration`) and add the
  check where they accept user-supplied definitions.
- The same rule appears as an **error** in `WorkflowValidatorFacade.WorkflowValidationResult` (via
  `WorkflowValidator.validateWorkflow`), so the Copilot can correct itself before saving. The builder picks the
  matching `WorkflowIssueKind`.
- A `manual` trigger always counts as enabled (it can never be disabled, see Editor).

### Docs

- Update "Multiple triggers" in `docs/content/docs/platform/automation/build/workflows/workflows.mdx` (the only
  copy; `enable-chat.mdx` links to it): every **enabled** trigger is enabled on deployment, disabled triggers never
  start a run, at least one trigger must stay enabled, and disabled triggers still need their connections. Update
  the #7284 test-run paragraph.

## Constraints

- Builds on #7284 (test run starts from the selected trigger), branch `7284` / PR #7285, not yet on master:
  `getSelectedTestRunTriggerName.ts`, `getTestRunTriggerName.ts`, `useTestRunChatTrigger.ts`,
  `startChatTestSession.ts`, `useWorkflowTestStream.ts` and the `TestWorkflowExecutorImpl` trigger selection come
  from it.
- Follows the #6243 conventions: flag key `disabled`, omitted when false.
- Server and client changes go in separate commits (`<ticket> ...` and `<ticket> client - ...`); generated files in
  their own commits.

## Left to the builder

- Test coverage shape, following the #6243 tests (`toggleNodeDisabled.test.ts`, facade tests for the deploy and
  teardown skip, request-time tests per entry point, validator tests).
- Whether `collectWorkflowIssues` also flags "no enabled trigger" in the editor for definitions edited in the raw
  JSON editor (the save-time guard is the real protection).
- The CE/EE asymmetry that CE `enableWorkflowTriggers` skips `manual` and EE does not; not a regression, leave as is
  unless it gets in the way.
