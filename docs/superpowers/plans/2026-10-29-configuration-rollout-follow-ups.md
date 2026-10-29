# Configuration Rollout Follow-ups Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Block destructive configuration rollbacks, serialize rollouts, report the turned-off state on enable, filter hidden projects in the listing API, resume rollout-paused workflows when the user completes them, warn the administrator about workflows a version change will pause, and let a configuration provide locked connections and inputs.

**Architecture:** All server work is in `server/ee/libs/embedded/embedded-configuration` (api, service, graphql, public-rest, remote-client). The configuration admin facade gains the rollback guard and a version-impact read; the rollout manager gains a per-(project, environment) row lock and stops overwriting the user's enabled intent; the public REST layer changes the enable response and filters listings. Client changes are the configuration dialog and the Automation Hub query; docs and the embedded sample app get matching copy.

**Tech Stack:** Spring Boot 4, Spring Data JDBC, Spring GraphQL, OpenAPI-generated public REST, JUnit 5 + Testcontainers, React 19 + TanStack Query + Vitest, GraphQL codegen, Next.js (sample app).

**Spec:** `docs/superpowers/specs/2026-10-29-configuration-rollout-follow-ups-design.md`

## Global Constraints

- Every file under `server/ee/` carries the ByteChef Enterprise license header and an `@version ee` Javadoc tag.
- Facades call services and facades only, never repositories; admin facades stay `@PreAuthorize("isTenantAdmin()")`.
- Liquibase: one new changeset for Task 11 only (two columns); existing changesets and identifiers are never edited (user decision).
- When a facade interface changes, its `embedded-configuration-remote-client` stub changes with it (throws `UnsupportedOperationException`).
- Java style: descriptive names, one blank line before control statements and between an assignment and its first use, no trailing blank line before `}`, no chaining on method-call results except streams/Optional/builders, test method names camelCase.
- Client: sorted object keys, `I`/`Props` interface suffixes, sorted named imports, lucide `Icon` suffix, `twMerge`, hook order, no fixed sleeps in tests, `void queryClient.invalidateQueries(...)`; regenerated `client/src/shared/middleware/graphql.ts` / `graphql-types.ts` in their own commit.
- Commits: subject only, no body, no `Co-Authored-By` or other trailer; prefixes `6786 ` (server), `6786 client - `, `6786 docs - `. Never push.
- Copy (verbatim): rollback error `Version %d lacks workflows connected users still use: %s. Turn them off in this configuration or remove them from those users first.`; dialog older-version note `Versions that lack workflows connected users still use cannot be selected.`; impact lines `Version %d adds required input %s to %s: %d connected users will pause until they fill it.` and `Version %d adds a required %s connection to %s: connected users without a %s integration will pause.`

## Review Focus

1. A rollback to an older version that still contains every referenced workflow must keep working. Pinned in Task 1.
2. Moving forward to a version that removes a referenced workflow must still succeed and dangle it as today. Pinned in Task 1.
3. Two rapid configuration saves must end with deployments in the second save's state. Pinned in Task 3.
4. A user who disabled a workflow themselves must stay disabled after a rollout and after filling an input. Pinned in Task 6.
5. A hidden project must still be provisionable by UUID. Pinned in Task 5.

---

### Task 1: Block moving a configuration to an older version that lacks referenced workflows

**Files:**
- Modify: `.../embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/exception/AutomationWorkflowProjectConfigurationErrorType.java` (add `VERSION_LACKS_REFERENCED_WORKFLOWS = new AutomationWorkflowProjectConfigurationErrorType(103)`)
- Modify: `.../facade/AutomationWorkflowProjectConfigurationAdminFacadeImpl.java` (`updateConfiguration`)
- Test: `.../facade/AutomationWorkflowProjectConfigurationAdminFacadeIntTest.java`

**Interfaces:**
- Consumes: `ConnectedUserWorkflowReferenceAdminFacade.getReferences(Set<String> automationWorkflowUuids)` (returns DTOs with `automationWorkflowUuid()`, `environment()` as the `Environment` name, `dangling()`), `ProjectWorkflowService.getProjectWorkflows(long projectId, int projectVersion)`, `WorkflowService.getWorkflow(String)` for labels, `Environment.values()[(int) environmentId]`.

- [ ] **Step 1: Failing tests** in the IntTest:
  - `testUpdateConfigurationRejectsOlderVersionLackingReferencedWorkflow` — V1 has A, V2 has A and B; configuration on V2; a connected user references B; `updateConfiguration(id, 1, ...)` throws `ConfigurationException` with `VERSION_LACKS_REFERENCED_WORKFLOWS` and a message containing B's label; the configuration still reads version 2.
  - `testUpdateConfigurationAllowsOlderVersionContainingReferencedWorkflows` — the user references only A: moving to V1 succeeds.
  - `testUpdateConfigurationIgnoresDanglingAndOtherEnvironmentReferences` — a dangling reference to B and a reference to B in another environment do not block.
  - `testUpdateConfigurationStillBlocksWhenWorkflowIsTurnedOff` — B turned off via `enableConfigurationWorkflow`: moving to V1 still throws.
  - `testUpdateConfigurationAllowsNewerVersionRemovingReferencedWorkflow` — configuration on V2, V3 lacks B, user references B: moving to V3 succeeds.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests '*AutomationWorkflowProjectConfigurationAdminFacadeIntTest*'` — new tests fail.
- [ ] **Step 3: Implement** the guard in `updateConfiguration` before the service update, only when the requested version is lower than the stored one; the message uses the copy from Global Constraints with labels joined by `", "`.
- [ ] **Step 4: Run** the same command — green.
- [ ] **Step 5: Commit** `6786 Reject moving a configuration to an older version that lacks workflows connected users use`

### Task 2: Configuration dialog note for older versions

**Files:**
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-dialog/AutomationWorkflowProjectConfigurationDialog.tsx` (the `movesToOlderVersion` paragraph, lines ~183-188)
- Test: `.../automation-workflow-project-configuration-dialog/tests/AutomationWorkflowProjectConfigurationDialog.test.tsx`

- [ ] **Step 1: Failing test** `shows the older-version note when an older version is chosen` asserting the exact note from Global Constraints (replace the existing assertion of the old "will be disconnected" text).
- [ ] **Step 2: Run** `cd client && npx vitest run src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-dialog` — fails.
- [ ] **Step 3: Implement** the copy change.
- [ ] **Step 4: Run** — green; `npx eslint` on the two files.
- [ ] **Step 5: Commit** `6786 client - Tell administrators that older versions lacking used workflows cannot be selected`

### Task 3: Serialize rollouts per project and environment

**Files:**
- Modify: `.../repository/AutomationWorkflowProjectConfigurationRepository.java` (add `@Query("SELECT * FROM automation_workflow_project_configuration WHERE project_id = :projectId AND environment_id = :environmentId FOR UPDATE") Optional<AutomationWorkflowProjectConfiguration> findByProjectIdAndEnvironmentIdForUpdate(long projectId, long environmentId)`, following `ConnectedUserProjectRepository.findByIdForUpdate`)
- Modify: `.../embedded-configuration-api/.../service/AutomationWorkflowProjectConfigurationService.java` + `ServiceImpl` + `embedded-configuration-remote-client/.../RemoteAutomationWorkflowProjectConfigurationServiceClient.java` (add `Optional<AutomationWorkflowProjectConfiguration> fetchConfigurationForUpdate(long projectId, long environmentId)`)
- Modify: `.../facade/ConnectedUserReferenceRolloutManager.java` (add `@Transactional public void rollOutAndApply(long automationWorkflowProjectId, long environmentId)`: lock via `fetchConfigurationForUpdate`, then `rollOut` and `applyConfigurationState`; keep the per-deployment `REQUIRES_NEW` templates and failure logging)
- Modify: `.../listener/AutomationWorkflowProjectConfigurationUpdatedEventListener.java` (call `rollOutAndApply`)
- Test: a rollout IntTest next to the existing rollout tests (find the class covering `ConnectedUserReferenceRolloutManager`; add there)

- [ ] **Step 1: Failing test** `testConcurrentRolloutsForTheSamePairEndInTheNewerState` — configuration on V1 with references; update the configuration to V2 then V3 (commit both), start `rollOutAndApply` twice on two threads released by one `CountDownLatch` (check the `await` boolean); assert every deployment of that pair is on V3 and no deployment is on V2. Plus `testRolloutsForDifferentPairsDoNotWaitOnEachOther` if cheap (two pairs both complete).
- [ ] **Step 2: Run** the IntTest — the first fails (or is flaky without the lock; run it 3 times and record).
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — green 3 runs in a row.
- [ ] **Step 5: Commit** `6786 Serialize configuration rollouts per project and environment`

### Task 4: Enable endpoints report why a workflow stays paused

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml` (both `.../automation/workflows/{workflowUuid}/enable` operations: response `200` with schema `EnableConnectedUserWorkflowResponse {enabled: boolean, attentionReason: string, nullable}`; keep the 409 responses; disable operations stay `204`)
- Regenerate: the module's generated API/models (the module's existing openapi generation task; commit generated files in their own commit)
- Modify: `.../embedded-configuration-api/.../facade/ConnectedUserProjectFacade.java` (`enableProjectWorkflow(String externalUserId, String workflowUuid, boolean enable, ...)` returns `@Nullable String` attention reason), its impl, and `RemoteConnectedUserProjectFacadeClient`
- Modify: `.../public_/web/rest/ConnectedUserProjectWorkflowApiController.java` (both enable methods return `ResponseEntity.ok(new EnableConnectedUserWorkflowResponseModel(...))`)
- Test: the public REST controller IntTest for `ConnectedUserProjectWorkflowApiController` (existing; extend)

- [ ] **Step 1: Failing tests**: enable while the configuration turns the workflow off → 200, `attentionReason == "TURNED_OFF_BY_ADMINISTRATOR"`, `enabled == true`; enable while on → 200, `attentionReason == null`; disable → 204; a missing connection still → 409.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:testIntegration --tests '*ConnectedUserProjectWorkflowApiController*'` — fails.
- [ ] **Step 3: Implement**: the impl returns what `ConnectedUserReferenceAttentionResolver` reports for the reference after enabling (existing precedence).
- [ ] **Step 4: Run** — green; also compile `embedded-configuration-remote-client`.
- [ ] **Step 5: Commit** generated files `6786 Regenerate the public REST models for the enable response`, then `6786 Return the attention reason from the public enable endpoints`

### Task 5: Listing endpoints leave out hidden projects; the hub stops filtering

**Files:**
- Modify: `.../public_/web/rest/AutomationWorkflowProjectApiController.java` (`getFrontendProjects` and `getProjects` drop projects whose DTO `automationHubVisible()` is false; `getPublishedProjects` itself unchanged)
- Test: the controller IntTest for `AutomationWorkflowProjectApiController` (existing)
- Modify: `client/src/ee/pages/embedded/automation-hub/queries/automationHub.queries.ts` (remove the `select` filter) and `.../queries/tests/automationHub.queries.test.tsx`
- Modify: `docs/content/docs/platform/embedded/build/activation-dialog.mdx`, `docs/content/docs/platform/embedded/build/automation-hub.mdx` (where they describe "Show in Automation Hub": it controls listing through the API, not provisioning; a hidden project's workflows are still provisionable by UUID)

- [ ] **Step 1: Failing tests**: server `testHiddenProjectIsNotListedButStillProvisionable` (configuration `automationHubVisible=false`: absent from both listing endpoints; `POST .../workflow-templates/{uuid}/provision` succeeds); client test that the hub query returns what the API returns (no filtering).
- [ ] **Step 2: Run** both — fail.
- [ ] **Step 3: Implement** server filter, client removal, docs.
- [ ] **Step 4: Run** — green; client lint/typecheck on touched files.
- [ ] **Step 5: Commit** `6786 Leave projects hidden from the hub out of the listing endpoints`, `6786 client - Stop filtering hidden projects in the Automation Hub`, `6786 docs - Describe Show in Automation Hub as listing through the API`

### Task 6: Rollouts keep the user's intent; completing a paused workflow resumes it

**Files:**
- Modify: `.../facade/ConnectedUserReferenceRolloutManager.java` (`rollOutDeployment`: stop `reference.setEnabled(rowSpec.enabled())`; the row's enabled = intent && resolved && administratively enabled)
- Modify: `.../facade/ConnectedUserWorkflowReferenceFacadeImpl.java` (`updateReferenceInputs`: when the reference intent is on, re-resolve via `applyWorkflow`/`resolveReference` and enable the row if complete) and `ConnectedUserReferenceDeploymentManager.updateInputs` as needed
- Connection creation hook: find where a connected user's connection is created (embedded connection facade / service). If a post-create hook or event exists, re-resolve that user's references whose attention reason is `MISSING_CONNECTION:<component>`; if none exists without a new event, do not add one — ledger the ruling and rely on the next enable/rollout.
- Test: rollout IntTest + `ConnectedUserWorkflowReferenceFacadeIntTest` (existing)

- [ ] **Step 1: Failing tests**:
  - `testRolloutAddingRequiredInputPausesButKeepsIntent` — V2 adds required input; after rollout the row is disabled, the reference's `enabled` stays true, attention `INPUT_REQUIRED:<name>`.
  - `testFillingTheMissingInputResumesTheWorkflow` — then `updateReferenceInputs` with the value → row enabled, attention null.
  - `testUserDisabledWorkflowStaysDisabledAfterRolloutAndInputs` — user disabled first: after rollout and filling, row stays disabled.
  - `testAddingTheMissingConnectionResumesTheWorkflow` — only if the hook exists (else skip with the ledgered ruling).
- [ ] **Step 2: Run** the IntTests — fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — green.
- [ ] **Step 5: Commit** `6786 Keep the user's intent across rollouts and resume paused workflows once completed`

### Task 7: Version impact read

**Files:**
- Create: `.../embedded-configuration-api/.../dto/AutomationWorkflowProjectConfigurationVersionImpactDTO.java` (record `(List<Workflow> workflows)`, nested `Workflow(String workflowUuid, String label, List<String> newRequiredInputs, List<String> newRequiredConnections, int affectedConnectedUserCount)`)
- Modify: `.../facade/AutomationWorkflowProjectConfigurationAdminFacade.java` + impl (`AutomationWorkflowProjectConfigurationVersionImpactDTO getVersionImpact(long configurationId, int projectVersion)`, `@Transactional(readOnly = true)`) + remote client stub if the facade has one
- Modify: `.../embedded-configuration-graphql/.../graphql/automation-workflow-project-configuration.graphqls` and its controller (query `automationWorkflowProjectConfigurationVersionImpact(id: ID!, projectVersion: Int!): AutomationWorkflowProjectConfigurationVersionImpact!`)
- Test: the admin facade IntTest and the GraphQL controller IntTest

- [ ] **Step 1: Failing tests**: new required input counted only for references lacking a value; optional→required counted; required→optional and unchanged inputs not reported; new required connection reported with all references counted; workflows without changes omitted; dangling and other-environment references not counted.
- [ ] **Step 2: Run** — fail.
- [ ] **Step 3: Implement**: inputs from `Workflow.getInputs()` (`name()`, `required()`); connection slots from the existing `WorkflowConnectionSlot.getSlots(workflowId)` (`componentName()`, `required()`); stored inputs from `ConnectedUserReferenceDeploymentManager.getInputs(projectDeploymentId, uuid)` via the references.
- [ ] **Step 4: Run** — green.
- [ ] **Step 5: Commit** `6786 Report which workflows a configuration version change would pause`

### Task 8: Impact warning in the configuration dialog

**Files:**
- Create: `client/src/graphql/embedded/configuration/automationWorkflowProjectConfigurationVersionImpact.graphql`; regenerate `graphql.ts`/`graphql-types.ts` (own commit)
- Modify: `AutomationWorkflowProjectConfigurationDialog.tsx` (when editing and the chosen version differs from the current one, query the impact and render one warning line per workflow with the exact copy from Global Constraints; saving is never blocked)
- Test: the dialog test (mock the query; assert both line formats; no lines when the list is empty or when creating)

- [ ] Steps: failing test → run → implement → run + lint/typecheck → commits `6786 client - Add the configuration version impact query` (operation), `6786 client - Regenerate GraphQL types for the version impact query`, `6786 client - Warn administrators which workflows a version change will pause`

### Task 9: Embedded sample app copy (separate repository)

**Files:** `/Volumes/Data/bytechef/bytechef-samples/bytechef-embedded-sample-app/front-end/src/app/automation-workflow/page.tsx` (near the "Find catalog workflow UUID" card / "Catalog workflow UUID" field), plus the playground's enable handling if it reads the enable response.

- [ ] Add the line: projects with "Show in Automation Hub" off are not listed here; to provision one of their workflows, paste its UUID (from the administrator's Automation Workflows page) into "Catalog workflow UUID". Where the sample calls an enable endpoint, show `attentionReason` from the new 200 body (it already maps `TURNED_OFF_BY_ADMINISTRATOR` to a message). Run the sample's `npm run lint` and `npm run build` in `front-end`. Commit in that repository (subject only): `Explain provisioning hidden projects by UUID and show why an enabled workflow stays paused`. Leave the unrelated uncommitted `front-end/package.json` change alone. Never push.

### Task 11: Store provided connections and inputs on configuration workflows

**Files:** new changeset `.../resources/config/liquibase/changelog/embedded/configuration/<timestamp>_embedded_configuration_added_provided_values.xml` (own commit first, `6786 Add provided connection and input columns to configuration workflows`); `AutomationWorkflowProjectConfigurationWorkflow` (two `MapWrapper` fields); `AutomationWorkflowProjectConfigurationService` + impl + remote stub (`updateWorkflowProvidedValues(long id, UUID workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs)`, creating the row on demand); `AutomationWorkflowProjectConfigurationAdminFacade` + impl (`updateConfigurationWorkflow(...)` with the validation and `INVALID_PROVIDED_VALUE = 104`, publishes the updated event); configuration DTO `Workflow` gains `providedConnections`, `providedInputs`; GraphQL schema + controller (mutation and fields).
**Tests:** admin facade IntTest (round-trip, each validation error, event published) and GraphQL controller IntTest.
**Commits:** changeset commit, then `6786 Let a configuration provide connections and inputs for its workflows`.

### Task 12: Resolve references with provided values

**Files:** `ConnectedUserWorkflowConnectionResolver` (provided map bypasses entitlement for its components), `ConnectedUserReferenceDeploymentManager.resolveReference` (+ `findMissingRequiredInput` sees merged inputs) and every caller (`ConnectedUserWorkflowReferenceFacadeImpl` provisioning/enable/inputs, `ConnectedUserReferenceRolloutManager.rollOutDeployment` and `applyConfigurationState` — which now always re-resolves rows), reading provided values via the configuration service for the reference's project and environment.
**Tests:** IntTests listed under the design's section 7 "Resolution".
**Commit:** `6786 Resolve connected user workflows with the connections and inputs their configuration provides`

### Task 13: Lock provided values in the public API and mark them in reads

**Files:** `openapi.yaml` (`ProvidedValueLockedError` 409 on provisioning and inputs operations; `provided` on input and connection schemas) + regenerated models (own commit); exception + handler mapping to 409; `ConnectedUserWorkflowTemplateDTO.Input` / components and the connected user workflow connection read gain `provided`; controllers map them.
**Tests:** public REST IntTests from the design's section 7.
**Commits:** generated models commit, then `6786 Reject user changes to provided connections and inputs and mark them in reads`

### Task 14: Admin dialog for provided values (client)

**Files:** GraphQL operation for the mutation and the new fields in `automationWorkflowProjectConfigurations.graphql`; regenerated types (own commit); new `AutomationWorkflowProjectConfigurationWorkflowDialog.tsx` opened from the configuration workflow row's actions menu ("Edit"); connection pickers list the administrator's connections of the component in the configuration's environment (reuse the existing connections query the integration configuration dialog uses), with "Let each user connect"; input fields with empty meaning "Let each user fill it".
**Tests:** dialog test (renders slots and inputs, saves the mutation variables, invalidates `['automationWorkflowProjectConfigurations']`).
**Commits:** operation, regenerate, `6786 client - Provide connections and inputs from a configuration workflow`

### Task 15: Automation Hub shows provided values read-only (client)

**Files:** the hub's connection step and inputs dialog components (find the components rendering a workflow's connections and inputs under `client/src/ee/pages/embedded/automation-hub/`); regenerate the embedded REST client if its models changed.
**Tests:** a provided connection renders "Provided by your administrator" without a picker; a provided input is not editable.
**Commit:** `6786 client - Show connections and inputs provided by the administrator as read-only in the Automation Hub`

### Task 16: Whole-branch verification

- [ ] `./gradlew -p server/ee/libs/embedded/embedded-configuration spotlessApply check` green.
- [ ] `cd client && npm run format && npm run check` green.
- [ ] Fold any formatting fixes with `--fixup` + autosquash.
