# Automation Workflow Configurations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give each embedded automation workflow project a per-environment configuration (pinned published version, enabled switch, per-workflow switches, hub visibility) that is the only thing deciding what the Automation Hub, the API, references and copies see.

**Architecture:** Two new tables mirror `integration_instance_configuration` / `_workflow`, keyed by `project_id` + `environment_id`. `AutomationWorkflowProjectFacadeImpl.getPublishedProjects` and every reader of "last published version" in the reference path switch to the configuration's pinned version. Admin state is derived: a reference's deployment row is `user enabled AND configuration enabled AND workflow enabled`, re-applied on configuration events; the reference row itself is never written by admin actions.

**Tech Stack:** Spring Boot 4 / Spring Data JDBC / Liquibase (EE module `server/ee/libs/embedded/embedded-configuration`), Spring GraphQL (`embedded-configuration-graphql`), React 19 + TanStack Query + GraphQL codegen (`client/src/ee/pages/embedded/automation-workflows`), Vitest, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-10-28-automation-workflow-configurations-design.md`

## Global Constraints

- Every new class under `server/ee/` carries the ByteChef Enterprise license header and `@version ee` Javadoc tag (copy from `AutomationWorkflowProject.java`).
- No data migration, no compatibility default: the feature is unreleased. Drop `automation_workflow_project.automation_hub_visible` outright.
- Liquibase changelog goes in its own commit, before any code, as the first commit of the branch. No XML comments.
- Facades use services only (never repositories); `@PreAuthorize` guards live on facades, mirroring `AutomationWorkflowProjectAdminFacadeImpl`.
- Java: one blank line before control statements and after a variable modification that the next statement uses; no `_` prefixes; no `TODO` comments; test methods camelCase without underscores; unit tests `<Class>Test`, integration tests `<Class>IntTest`; one test class per source class; `@GraphQlTest` for GraphQL controllers.
- Client: ESLint `sort-keys` (alphabetical object keys), interfaces end in `I` or `Props`, lucide icons imported with `Icon` suffix, `twMerge` not `cn`, hook order `useState → useRef → stores → custom hooks → memo/callback → useEffect`, `useRef` names end in `Ref`.
- Commits: subject only, `6756 <description>` for server, `6756 client - <description>` for client, `6756 docs - <description>` for docs; no `Co-Authored-By` trailer. Generated files (`client/src/shared/middleware/graphql.ts`, `graphql-types.ts`) go in their own `6756 client - Regenerate GraphQL types` commit right after the operation change.
- Before each push: `./gradlew spotlessApply` then `./gradlew check` on every touched server module; `npm run format` then `npm run check` in `client/`.
- Branch `6756` in the worktree `/private/tmp/claude-502/-Volumes-Data-bytechef-bytechef-master/9203f3e6-70e0-4eff-a036-2ed3eaceff5c/scratchpad/wt6756`. Do not push; the user pushes.
- "Automation workflow project id" throughout this codebase means the `project.id` of the marked `Project` (see `AutomationWorkflowProjectRepository.findByProjectId`, `ConnectedUserReferenceDeploymentManager.getLastPublishedVersion`). The new tables key by `project_id` for the same reason (this is the implementer's call where the spec says `automation_workflow_project.id`; the `automation_workflow_project` row cascades from `project` already).

## Review Focus

1. A connected user who turned a reference **off** themselves, when the admin turns the configuration off and back on, must still be off. Pinned by `testApplyConfigurationStateKeepsTheUsersOwnOffSwitch` (Task 6).
2. A configuration pointed back at an **older** version must roll references back, not be ignored as "already ahead". Pinned by `testRollOutMovesReferencesToAnOlderPinnedVersion` (Task 5).
3. A template that exists in the **last published** version but not in the configuration's **pinned** version must not appear in the hub, the API, or be activatable. Pinned by `testGetPublishedProjectsUsesThePinnedVersion` (Task 4) and `testProvisionReferenceThrowsWhenTemplateIsNotInThePinnedVersion` (Task 5).
4. Deleting a configuration must leave every user's reference deployment, connections and inputs in place so recreating it brings them back. Pinned by `testDeleteConfigurationKeepsReferenceDeployments` (Task 6).
5. The hub endpoint must hide `automationHubVisible=false` projects on the server while the provisioning endpoint still returns them. Pinned by `testGetFrontendProjectsDropsHiddenProjects` / `testGetProjectsKeepsHiddenProjects` (Task 4).

---

### Task 1: Liquibase changelog

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20261029120000_embedded_configuration_added_automation_workflow_project_configuration.xml`

**Interfaces:**
- Produces: tables `automation_workflow_project_configuration` and `automation_workflow_project_configuration_workflow`; column `automation_workflow_project.automation_hub_visible` dropped.

- [ ] **Step 1: Write the changelog** (mirror `20261008120000_embedded_configuration_added_automation_workflow_project_settings.xml`: `${uuid_type}` for uuids, `startWith="1050"`, audit columns, `<rollback>`). Three change sets, author `Ivica Cardic`:
  - `20261029120000-1`: create `automation_workflow_project_configuration` (`id`, `project_id` bigint not null FK→`project.id` `onDelete="CASCADE"`, `environment_id` bigint not null, `project_version` integer not null, `enabled` boolean not null default `false`, `automation_hub_visible` boolean not null default `true`, audit + `version`), unique `uk_automation_workflow_project_configuration_project_environment` on (`project_id`, `environment_id`). Precondition: table does not exist.
  - `20261029120000-2`: create `automation_workflow_project_configuration_workflow` (`id`, `automation_workflow_project_configuration_id` bigint not null FK→`automation_workflow_project_configuration.id` cascade, `workflow_uuid` `${uuid_type}` not null, `enabled` boolean not null default `true`, audit + `version`), unique `uk_automation_workflow_project_configuration_workflow_uuid` on (`automation_workflow_project_configuration_id`, `workflow_uuid`).
  - `20261029120000-3`: `dropColumn` `automation_hub_visible` from `automation_workflow_project`, precondition column exists.

- [ ] **Step 2: Verify the schema loads**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "*ConnectedUserProjectWorkflowRepositoryIntTest"`
Expected: PASS (Liquibase applies the new change sets; a bad XML fails the context).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase
git commit -m "6756 Add automation workflow project configuration tables"
```

---

### Task 2: Domain, repositories, service

**Files:**
- Create: `embedded-configuration-api/.../domain/AutomationWorkflowProjectConfiguration.java`, `.../domain/AutomationWorkflowProjectConfigurationWorkflow.java`, `.../service/AutomationWorkflowProjectConfigurationService.java`
- Create: `embedded-configuration-service/.../repository/AutomationWorkflowProjectConfigurationRepository.java`, `.../repository/AutomationWorkflowProjectConfigurationWorkflowRepository.java`, `.../service/AutomationWorkflowProjectConfigurationServiceImpl.java`
- Create: `embedded-configuration-remote-client/.../remote/client/service/RemoteAutomationWorkflowProjectConfigurationServiceClient.java` (`@Component @ConditionalOnEEVersion`, every method throws `UnsupportedOperationException`, mirror `RemoteAutomationWorkflowProjectServiceClient`)
- Modify: `embedded-configuration-api/.../domain/AutomationWorkflowProject.java` (remove `automationHubVisible`), `.../service/AutomationWorkflowProjectService.java` and `...ServiceImpl.java` (`create(long projectId, @Nullable String permissionExpression)`; delete `updateAutomationHubVisible`), `RemoteAutomationWorkflowProjectServiceClient` accordingly
- Test: `embedded-configuration-service/src/test/java/.../service/AutomationWorkflowProjectConfigurationServiceIntTest.java` (mirror `ConnectedUserReferenceDeploymentServiceIntTest` for `@SpringBootTest` + `PostgreSQLContainerConfiguration` setup)

**Interfaces:**
- Produces (domain): `AutomationWorkflowProjectConfiguration` — `@Table("automation_workflow_project_configuration")`; fields `Long id`, `AggregateReference<Project, Long> projectId`, `long environmentId`, `int projectVersion`, `boolean enabled`, `boolean automationHubVisible`, audit, `int version`; `@MappedCollection(idColumn = "automation_workflow_project_configuration_id") Set<AutomationWorkflowProjectConfigurationWorkflow> workflows`. `AutomationWorkflowProjectConfigurationWorkflow` — `Long id`, `UUID workflowUuid`, `boolean enabled`, audit, `int version`.
- Produces (service):
  ```java
  AutomationWorkflowProjectConfiguration create(long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible);
  void delete(long id);
  Optional<AutomationWorkflowProjectConfiguration> fetchConfiguration(long projectId, long environmentId);
  AutomationWorkflowProjectConfiguration getConfiguration(long id);
  List<AutomationWorkflowProjectConfiguration> getConfigurations(long projectId);
  boolean isWorkflowEnabled(AutomationWorkflowProjectConfiguration configuration, UUID workflowUuid); // true when no row
  AutomationWorkflowProjectConfiguration update(long id, int projectVersion, boolean enabled, boolean automationHubVisible);
  AutomationWorkflowProjectConfiguration updateEnabled(long id, boolean enabled);
  AutomationWorkflowProjectConfiguration updateWorkflowEnabled(long id, UUID workflowUuid, boolean enabled); // upserts the row
  ```
- Produces (repository): `AutomationWorkflowProjectConfigurationRepository extends ListCrudRepository<…, Long>` with `Optional<…> findByProjectIdAndEnvironmentId(long projectId, long environmentId)`, `List<…> findAllByProjectId(long projectId)`.

- [ ] **Step 1: Write the failing IntTest** with these cases:
  - `testCreateAndFetchByProjectAndEnvironment`: create(projectId, env 0, version 2, enabled false, hub true) → `fetchConfiguration` returns it with those values.
  - `testCreateTwiceForTheSameEnvironmentThrows`: second `create` same project+env → `DataIntegrityViolationException`.
  - `testIsWorkflowEnabledDefaultsToTrueWithoutARow`.
  - `testUpdateWorkflowEnabledUpsertsAndReads`: `updateWorkflowEnabled(id, uuid, false)` then `isWorkflowEnabled` false; again `true` → true, still one row.
  - `testDeleteRemovesWorkflowRows`.
- [ ] **Step 2: Run it** — Expected: compilation failure (classes missing).
- [ ] **Step 3: Implement domain, repositories, service, remote stub; remove `automationHubVisible` from `AutomationWorkflowProject` and its service.** Fix every compile error this causes in `AutomationWorkflowProjectFacadeImpl` / `AdminFacadeImpl` / GraphQL controller by dropping the argument (Task 3 reshapes those APIs; here just keep the build green).
- [ ] **Step 4: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "*AutomationWorkflowProjectConfigurationServiceIntTest"` — Expected: PASS.
- [ ] **Step 5: Commit** `6756 Add the automation workflow project configuration domain and service`.

---

### Task 3: Admin facade, events, GraphQL

**Files:**
- Create: `embedded-configuration-api/.../dto/AutomationWorkflowProjectConfigurationDTO.java`, `.../event/AutomationWorkflowProjectConfigurationUpdatedEvent.java`, `.../event/AutomationWorkflowProjectConfigurationDeletedEvent.java`, `.../facade/AutomationWorkflowProjectConfigurationAdminFacade.java`
- Create: `embedded-configuration-service/.../facade/AutomationWorkflowProjectConfigurationAdminFacadeImpl.java`
- Create: `embedded-configuration-graphql/src/main/resources/graphql/automation-workflow-project-configuration.graphqls`, `.../web/graphql/AutomationWorkflowProjectConfigurationGraphQlController.java`
- Modify: `automation-workflow-project.graphqls` (drop `automationHubVisible` from `AutomationWorkflowProject` type and from both mutations), `AutomationWorkflowProjectGraphQlController.java`, `AutomationWorkflowProjectAdminFacade(Impl)`, `AutomationWorkflowProjectFacade(Impl)` (drop the `@Nullable Boolean automationHubVisible` parameters), `AutomationWorkflowProjectDTO` (replace `boolean automationHubVisible` with nothing; the hub flag travels on the configuration DTO and, for the public DTO, see Task 4), `EmbeddedConfigurationGraphQlConfigurationSharedMocks` (add the new admin facade)
- Test: `embedded-configuration-graphql/src/test/.../AutomationWorkflowProjectConfigurationGraphQlControllerIntTest.java` (`@GraphQlTest`, copy the annotations from `ConnectedUserProjectGraphQlControllerIntTest`), `embedded-configuration-service/src/test/.../facade/AutomationWorkflowProjectConfigurationAdminFacadeIntTest.java` (reuse `AutomationWorkflowProjectFacadeIntTestConfiguration`)

**Interfaces:**
- Produces:
  ```java
  public record AutomationWorkflowProjectConfigurationDTO(
      long id, long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible,
      List<Workflow> workflows, Instant lastModifiedDate) {
      public record Workflow(String workflowUuid, String label, boolean enabled) {}
  }
  public record AutomationWorkflowProjectConfigurationUpdatedEvent(long projectId, long environmentId) {}
  public record AutomationWorkflowProjectConfigurationDeletedEvent(long projectId, long environmentId) {}

  public interface AutomationWorkflowProjectConfigurationAdminFacade {
      long createConfiguration(long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible);
      void deleteConfiguration(long id);
      void enableConfiguration(long id, boolean enable);
      void enableConfigurationWorkflow(long id, String workflowUuid, boolean enable);
      List<AutomationWorkflowProjectConfigurationDTO> getConfigurations(long projectId);
      void updateConfiguration(long id, int projectVersion, boolean enabled, boolean automationHubVisible);
  }
  ```
  `workflows` in the DTO = the `ProjectWorkflow`s of `projectVersion` (via `projectWorkflowService.getProjectWorkflows(projectId, projectVersion)`), labelled from `workflowService.getWorkflow(workflowId).getLabel()`, `enabled` from `isWorkflowEnabled`.
  `createConfiguration`/`updateConfiguration` throw `ConfigurationException` when `projectVersion` is not a published version of the project (use `projectService.getProject(projectId).getProjectVersions()` and `ProjectVersion.Status.PUBLISHED`). Every mutating method publishes `…UpdatedEvent` (delete publishes `…DeletedEvent`) through `ApplicationEventPublisher` after the write.
- GraphQL (`automation-workflow-project-configuration.graphqls`):
  ```graphql
  type AutomationWorkflowProjectConfiguration { id: ID! projectId: ID! environmentId: ID! projectVersion: Int! enabled: Boolean! automationHubVisible: Boolean! lastModifiedDate: String workflows: [AutomationWorkflowProjectConfigurationWorkflow!]! }
  type AutomationWorkflowProjectConfigurationWorkflow { workflowUuid: ID! label: String enabled: Boolean! }
  extend type Query { automationWorkflowProjectConfigurations(projectId: ID!): [AutomationWorkflowProjectConfiguration!]! }
  extend type Mutation {
      createAutomationWorkflowProjectConfiguration(projectId: ID!, environmentId: ID!, projectVersion: Int!, enabled: Boolean!, automationHubVisible: Boolean!): ID!
      updateAutomationWorkflowProjectConfiguration(id: ID!, projectVersion: Int!, enabled: Boolean!, automationHubVisible: Boolean!): Boolean!
      deleteAutomationWorkflowProjectConfiguration(id: ID!): Boolean!
      enableAutomationWorkflowProjectConfiguration(id: ID!, enable: Boolean!): Boolean!
      enableAutomationWorkflowProjectConfigurationWorkflow(id: ID!, workflowUuid: ID!, enable: Boolean!): Boolean!
  }
  ```

- [ ] **Step 1: Write the failing GraphQL IntTest**: `testAutomationWorkflowProjectConfigurationsReturnsWorkflowsWithTheirSwitches` (mock facade returns one DTO with two workflows, one disabled; assert `[*].workflows[*].enabled` is `[true, false]`) and `testEnableAutomationWorkflowProjectConfigurationWorkflowDelegates` (`verify(facade).enableConfigurationWorkflow(5L, "uuid", false)`).
- [ ] **Step 2: Write the failing facade IntTest**: `testCreateConfigurationWithUnpublishedVersionThrows` (project published once as v1; `createConfiguration(…, projectVersion 2, …)` → `ConfigurationException`), `testGetConfigurationsListsPinnedVersionWorkflows` (publish v1 with one template, add a template, publish v2; configuration pinned to v1 lists one workflow), `testUpdateConfigurationPublishesUpdatedEvent` (capture with `@RecordApplicationEvents`).
- [ ] **Step 3: Run both** — Expected: compilation failures.
- [ ] **Step 4: Implement** the DTO, events, facade (`@PreAuthorize` as on `AutomationWorkflowProjectAdminFacadeImpl`), schema, controller; drop `automationHubVisible` from the project facades, DTO, schema, controller and shared mocks.
- [ ] **Step 5: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests "*AutomationWorkflowProjectConfiguration*"` — Expected: PASS.
- [ ] **Step 6: Commit** `6756 Add the automation workflow project configuration admin facade and GraphQL API`.

---

### Task 4: Exposure through the configuration (hub, API, copies)

**Files:**
- Modify: `embedded-configuration-service/.../facade/AutomationWorkflowProjectFacadeImpl.java` (`getPublishedProjects(String, Environment)`, `toPublishedDTO`), `ConnectedUserProjectFacadeImpl.java` (copy path at `getLastPublishedWorkflowId` ~line 217 and `getPublishedWorkflows` ~line 744), `embedded-configuration-api/.../dto/AutomationWorkflowProjectDTO.java` (keep `automationHubVisible`, now sourced from the configuration; `lastPublishedVersion` carries the pinned version in the per-environment variant)
- Modify: `embedded-configuration-public-rest/.../AutomationWorkflowProjectApiController.java` (`getFrontendProjects` filters `automationHubVisible`), `embedded-configuration-public-rest/openapi.yaml` (no shape change)
- Modify (client): `client/src/ee/pages/embedded/automation-hub/queries/automationHub.queries.ts` (remove the `select` filter; the server does it) and its test
- Test: `AutomationWorkflowProjectFacadeIntTest`, `AutomationWorkflowProjectApiControllerIntTest`

**Interfaces:**
- Consumes: `AutomationWorkflowProjectConfigurationService.fetchConfiguration`, `isWorkflowEnabled` (Task 2).
- Produces: `getPublishedProjects(externalUserId, environment)` semantics per spec; `ConfigurationException` with error type `ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED` (add the enum constant) from `copyWorkflowTemplate` when the project has no enabled configuration in the user's environment, or the template is disabled or absent from the pinned version.

- [ ] **Step 1: Write the failing facade tests** (same fixtures style as `testCopyWorkflowTemplateUnpublishedThrows`):
  - `testGetPublishedProjectsSkipsProjectsWithoutAnEnabledConfiguration`
  - `testGetPublishedProjectsUsesThePinnedVersion` (publish v1 with template A; add B, publish v2; configuration pinned to v1 → only A)
  - `testGetPublishedProjectsDropsDisabledWorkflows`
  - `testGetPublishedProjectsCarriesTheConfigurationsHubVisibility`
  - `testCopyWorkflowTemplateNotReleasedThrows` and `testCopyWorkflowTemplateCopiesThePinnedVersion` (definition of v1, not v2)
- [ ] **Step 2: Write the failing controller tests** in `AutomationWorkflowProjectApiControllerIntTest`: `testGetFrontendProjectsDropsHiddenProjects`, `testGetProjectsKeepsHiddenProjects` (facade mock returns one visible, one hidden).
- [ ] **Step 3: Run** both test classes — Expected: FAIL on the new tests.
- [ ] **Step 4: Implement.** In `getPublishedProjects(externalUserId, environment)`: for each marked project, `fetchConfiguration(project.id, environment.id)`; skip when absent or disabled; build templates from `projectWorkflowService.getProjectWorkflows(projectId, configuration.getProjectVersion())`; drop `!isWorkflowEnabled`; then the existing permission filter. `getPublishedProjects()` (no environment, admin listing) stays on the last published version. Copy path: resolve the user's environment configuration the same way and read the workflow id from the pinned version's `ProjectWorkflow`.
- [ ] **Step 5: Remove the client hub `select` filter** and adjust `automationHub.queries.test.tsx`.
- [ ] **Step 6: Run** `./gradlew …embedded-configuration-service:testIntegration --tests "*AutomationWorkflowProjectFacadeIntTest" …embedded-configuration-public-rest:testIntegration --tests "*AutomationWorkflowProjectApiControllerIntTest"` and `cd client && npx vitest run src/ee/pages/embedded/automation-hub` — Expected: PASS.
- [ ] **Step 7: Commit** server `6756 Expose automation workflow projects through their environment configuration`, client `6756 client - Let the server decide which automation workflow projects the hub lists`.

---

### Task 5: References read the pinned version

**Files:**
- Modify: `ConnectedUserReferenceDeploymentManager.java` (replace `getLastPublishedVersion(long)` with `getReleasedVersion(long projectId, Environment environment)`; `createDeployment` uses the connected user's environment), `ConnectedUserReferenceRolloutManager.java` (`rollOut(long projectId, long environmentId)`; `rollOutDeploymentIfBehind` compares `!=` so rollbacks apply), `ConnectedUserReferenceAttentionResolver.java` (`UPDATE_PENDING` when `projectDeployment.getProjectVersion() != releasedVersion`), `ConnectedUserWorkflowReferenceFacadeImpl.provisionReference` (throw when not released / template disabled)
- Delete: `AutomationWorkflowProjectPublishedEvent.java`, `AutomationWorkflowProjectPublishedEventListener.java`; remove the `publishEvent` call in `AutomationWorkflowProjectFacadeImpl.publishProject`
- Create: `embedded-configuration-service/.../listener/AutomationWorkflowProjectConfigurationUpdatedEventListener.java` (`@Async("workerExecutor") @TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` → `rollOut(projectId, environmentId)`; Task 6 adds the `applyConfigurationState` call after it)
- Test: `ConnectedUserReferenceRolloutManagerIntTest`, `ConnectedUserReferenceAttentionResolverTest`, `ConnectedUserWorkflowReferenceFacadeIntTest`

**Interfaces:**
- Produces: `int getReleasedVersion(long projectId, Environment environment)` throws `ConfigurationException` (`WORKFLOW_TEMPLATE_NOT_RELEASED`) when no enabled configuration exists; `void rollOut(long projectId, long environmentId)`.

- [ ] **Step 1: Write the failing tests**:
  - Rollout: `testPublishingDoesNotMoveReferences` (publish v2, references stay on v1), `testUpdatingTheConfigurationVersionRollsReferencesForward`, `testRollOutMovesReferencesToAnOlderPinnedVersion`, `testRollOutOnlyTouchesTheConfiguredEnvironment` (two users in different environments; only one moves).
  - Attention: `testUpdatePendingWhenDeploymentVersionDiffersFromThePinnedVersion`, `testNoUpdatePendingWhenLastPublishedIsNewerButNotPinned`.
  - Reference facade: `testProvisionReferenceThrowsWhenTheProjectIsNotReleasedInTheUsersEnvironment`, `testProvisionReferenceThrowsWhenTemplateIsNotInThePinnedVersion`, `testProvisionReferencePinsTheDeploymentToTheConfiguredVersion`.
- [ ] **Step 2: Run** the three classes — Expected: FAIL on the new tests (and `testEnableReferenceAfterRepublishDoesNotFail` must be updated to create a configuration first).
- [ ] **Step 3: Implement** as listed in Files; update existing tests in these classes that relied on publish-triggered rollout to create/update a configuration instead.
- [ ] **Step 4: Run** `./gradlew …embedded-configuration-service:testIntegration --tests "*ConnectedUserReference*" --tests "*ConnectedUserWorkflowReferenceFacadeIntTest"` and `…:test --tests "*ConnectedUserReferenceAttentionResolverTest"` — Expected: PASS.
- [ ] **Step 5: Commit** `6756 Roll connected user references to the environment's pinned version instead of the last published one`.

---

### Task 6: Derived administrative state

**Files:**
- Modify: `ConnectedUserReferenceRolloutManager.java` (add `applyConfigurationState`), `ConnectedUserReferenceAttentionResolver.java` (new reason), `ConnectedUserWorkflowReferenceFacadeImpl.applyWorkflow` (effective enable), `AutomationWorkflowProjectConfigurationAdminFacadeImpl.deleteConfiguration` (must not call `deleteReferenceDeployments`)
- Create: `embedded-configuration-service/.../listener/AutomationWorkflowProjectConfigurationDeletedEventListener.java` → `applyConfigurationState(projectId, environmentId)`
- Test: `ConnectedUserReferenceRolloutManagerIntTest`, `ConnectedUserReferenceAttentionResolverTest`, `ConnectedUserWorkflowReferenceFacadeIntTest`, `AutomationWorkflowProjectConfigurationAdminFacadeIntTest`

**Interfaces:**
- Produces:
  ```java
  // ConnectedUserReferenceRolloutManager
  void applyConfigurationState(long projectId, long environmentId);
  // ConnectedUserReferenceAttentionResolver
  static final String TURNED_OFF_BY_ADMINISTRATOR = "TURNED_OFF_BY_ADMINISTRATOR";
  // ConnectedUserReferenceDeploymentManager
  boolean isAdministrativelyEnabled(long projectId, Environment environment, String automationWorkflowUuid); // configuration present && enabled && isWorkflowEnabled
  ```
  `applyConfigurationState`: for every reference deployment of `projectId` whose environment is `environmentId`, for every reference row, `projectDeploymentWorkflowService.updateEnabled(rowId, reference.isEnabled() && isAdministrativelyEnabled(…))`. `applyWorkflow` sets the deployment row to `enable && isAdministrativelyEnabled(…)` while still saving `enable` to the reference row. The resolver returns `TURNED_OFF_BY_ADMINISTRATOR` when `!isAdministrativelyEnabled` (checked before `UPDATE_PENDING`).

- [ ] **Step 1: Write the failing tests**:
  - `testApplyConfigurationStateKeepsTheUsersOwnOffSwitch` (user off; admin off then on; row stays off, reference `enabled` stays false).
  - `testDisablingAConfigurationStopsEveryReferenceInThatEnvironment` (rows false; reference rows untouched).
  - `testDisablingOneWorkflowStopsOnlyThatTemplatesReferences`.
  - `testDeleteConfigurationKeepsReferenceDeployments` (after delete: `ConnectedUserReferenceDeployment` row and `ProjectDeployment` still exist, rows disabled; recreate → `applyConfigurationState` re-enables rows for users with `enabled=true`).
  - Resolver: `testTurnedOffByAdministratorWhenConfigurationIsDisabled`, `testTurnedOffByAdministratorWinsOverUpdatePending`.
  - Reference facade: `testEnableReferenceWhileAdministrativelyOffSavesTheIntentButKeepsTheRowOff`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the same Gradle targets as Task 5 Step 4 plus `--tests "*AutomationWorkflowProjectConfigurationAdminFacadeIntTest"` — Expected: PASS.
- [ ] **Step 5: Run module checks**: `./gradlew spotlessApply` then `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-api:check :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:check :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:check :server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client:check` — Expected: BUILD SUCCESSFUL. Fix Checkstyle/PMD/SpotBugs findings in a follow-up commit `6756 Fix static analysis findings` only if any.
- [ ] **Step 6: Commit** `6756 Derive a reference's administrative state from its environment configuration`.

---

### Task 7: Client GraphQL operations and generated types

**Files:**
- Create: `client/src/graphql/embedded/configuration/automationWorkflowProjectConfigurations.graphql`, `createAutomationWorkflowProjectConfiguration.graphql`, `updateAutomationWorkflowProjectConfiguration.graphql`, `deleteAutomationWorkflowProjectConfiguration.graphql`, `enableAutomationWorkflowProjectConfiguration.graphql`, `enableAutomationWorkflowProjectConfigurationWorkflow.graphql`
- Modify: `automationWorkflowProjects.graphql` (drop `automationHubVisible`), create/update project operations (drop the variable)
- Regenerate: `client/src/shared/middleware/graphql.ts`, `graphql-types.ts`

- [ ] **Step 1: Write the operations** (query selects every field of the type plus `workflows { workflowUuid label enabled }`; mutations select nothing beyond the scalar result).
- [ ] **Step 2: Run** `cd client && npx graphql-codegen` — Expected: both generated files change; `npx tsc --noEmit -p tsconfig.json` reports errors only in the project dialog / page (fixed in Task 8).
- [ ] **Step 3: Commit twice**: `6756 client - Add automation workflow project configuration GraphQL operations` (operations) then `6756 client - Regenerate GraphQL types` (generated files).

---

### Task 8: Admin UI — configurations tab

**Files:**
- Create under `client/src/ee/pages/embedded/automation-workflows/components/`:
  - `automation-workflow-project-list/AutomationWorkflowProjectListItemTabs.tsx` (copy `integrations/components/integration-list/IntegrationListItemTabs.tsx`; values `'workflows' | 'configurations'`)
  - `automation-workflow-project-configuration-list/AutomationWorkflowProjectConfigurationList.tsx`, `AutomationWorkflowProjectConfigurationListItem.tsx`, `AutomationWorkflowProjectConfigurationWorkflowList.tsx`, `AutomationWorkflowProjectConfigurationWorkflowListItem.tsx`, `tests/*.test.tsx`
  - `automation-workflow-project-configuration-dialog/AutomationWorkflowProjectConfigurationDialog.tsx`, `tests/AutomationWorkflowProjectConfigurationDialog.test.tsx`
- Modify: `AutomationWorkflowProjectList.tsx` (wrap `CollapsibleContent` body in the tabs; `actions` = **New Workflow** group on the workflows tab, **New Configuration** button on the configurations tab, disabled with tooltip "Publish the project first" when `!project.published`), `AutomationWorkflowProjectWorkflowList.tsx` (drop the creation button group it holds today; it moves to the tab actions), `AutomationWorkflowProjectWorkflowListItem.tsx` (remove the **Executions** menu item and the executions store import), `AutomationWorkflowProjectListItem.tsx` (remove the "Hidden from hub" badge), `automation-workflow-project-dialog/AutomationWorkflowProjectDialog.tsx` + test (remove `automationHubVisible`), `AutomationWorkflows.tsx` (remove `automationHubVisible` from create/update calls), `stores/useAutomationWorkflowExecutionsSheetStore.ts` (add `environmentId: number` to `openAutomationWorkflowExecutionsSheet` props and state), `automation-workflow-executions-sheet/AutomationWorkflowExecutionsSheet.tsx` (use the store's `environmentId` instead of `useEnvironmentStore`), existing tests in `automation-workflows/tests` and `components/*/tests` that click **Executions** or pass `automationHubVisible`

**Interfaces:**
- Consumes: hooks from Task 7 (`useAutomationWorkflowProjectConfigurationsQuery`, `useCreate…Mutation`, `useUpdate…Mutation`, `useDelete…Mutation`, `useEnable…Mutation`, `useEnable…WorkflowMutation`), `useGetEnvironmentsQuery` (as used by `IntegrationInstanceConfigurationDialog`).
- Produces: `AutomationWorkflowProjectConfigurationListItem` props `{configuration, environments, project, onEdit, onDelete}`; row shows environment name, `V{projectVersion}` badge, a `Badge` "Newer version available" when `project.lastPublishedVersion > configuration.projectVersion`, "Hidden from hub" badge when `!automationHubVisible`, `Switch` bound to `enableAutomationWorkflowProjectConfiguration`, ⋮ menu **Edit**/**Delete** (`AlertDialog` confirm). Expanded content lists `configuration.workflows`: label as a `button` that calls `openAutomationWorkflowExecutionsSheet({environmentId: configuration.environmentId, projectName: project.name, workflow: {label, workflowId, workflowUuid}})` (`workflowId` from the matching `project.workflowTemplates` entry), and a `Switch` bound to `enableAutomationWorkflowProjectConfigurationWorkflow`. Dialog fields: Environment `Select` (environments without a configuration; fixed when editing), Version `Select` of published versions from `useAutomationWorkflowProjectVersionsQuery` filtered to status `PUBLISHED`, `Switch` Enabled, `Switch` Show in Automation Hub.

- [ ] **Step 1: Write the failing Vitest tests**: dialog (`creates a configuration with the chosen environment and version`, `offers only published versions`), list item (`opens the workflow's executions for the configuration's environment on click` asserting the store's `environmentId`, `shows the newer version hint when the project published a later version`, `toggles the configuration`), project list (`shows the New Configuration button disabled until the project is published`), workflow list item (`does not offer an Executions action`).
- [ ] **Step 2: Run** `npx vitest run src/ee/pages/embedded/automation-workflows` — Expected: FAIL / compile errors.
- [ ] **Step 3: Implement** the components and modifications; update the existing tests that break.
- [ ] **Step 4: Run** `npx vitest run src/ee/pages/embedded/automation-workflows src/ee/pages/embedded/connected-users` then `npm run check` in `client/` — Expected: PASS, lint and typecheck clean.
- [ ] **Step 5: Commit** `6756 client - Manage automation workflow project configurations per environment and open executions from them`.

---

### Task 9: Docs

**Files:**
- Modify: `docs/content/docs/platform/embedded/build/automations/index.md` (project row loses Show in Automation Hub; new **Configurations** tab section: fields, enable, per-workflow switches, click-to-executions, "Newer version available"; **Publishing** no longer releases; template ⋮ menu loses Executions), `docs/content/docs/platform/embedded/build/automation-hub.mdx` ("Controlling what appears in the hub": enabled configuration in the environment + hub visibility per configuration; **Copies and references**: references follow the environment's configuration, not the latest publish; admin off = reference needs attention, copies unaffected), `docs/content/docs/platform/embedded/build/automations/automation-code-workflows.mdx` (lines 34-36: a redeploy publishes a version; it reaches an environment when that environment's configuration is moved to it, or at once when the deploy call passes `release`, which never turns a disabled environment back on; Task 10 ships the flag)

- [ ] **Step 1: Edit the three pages.** No em dashes in prose; keep the existing voice.
- [ ] **Step 2: Build check**: `cd docs && npm run build` (or the project's docs lint) — Expected: no broken links.
- [ ] **Step 3: Commit** `6756 docs - Document automation workflow project configurations`.

---

### Task 10: Code-workflow deploy release flag on `0_p_code_workflows`, and the sample app

The embedded code-workflow deploy endpoint is **not on `master`**; it lives on the `0_p_code_workflows` port branch (`AutomationProjectCodeWorkflowApiController` in `embedded-configuration-public-rest`, operation `deployAutomationProjectCodeWorkflow`, and its internal twin in `embedded-configuration-rest-impl`; both call `AutomationWorkflowProjectCodeWorkflowFacade.save(byte[] bytes, Language language)`). This task is done **on `0_p_code_workflows` after rebasing it onto `6756`** (Tasks 1-9), as part of this work. The branch also passes `automationHubVisible` into `createEmptyCodeWorkflow`; drop that parameter there as Task 3 did for the project facades.

**Files:**
- Modify (on `0_p_code_workflows`): `embedded-configuration-public-rest/openapi.yaml` and `embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml` (add optional multipart field `release` boolean, default `false`, to the deploy operation; regenerate), both `AutomationProjectCodeWorkflowApiController` classes (pass it through), `AutomationWorkflowProjectCodeWorkflowFacade` (`List<String> save(byte[] bytes, Language language, Environment environment, boolean release)`) and `…FacadeImpl` (after publishing, when `release`: `fetchConfiguration(projectId, environment.id)` present → `updateConfiguration(id, newVersion, existing.enabled, existing.automationHubVisible)`; absent → `createConfiguration(projectId, environmentId, newVersion, true, true)`), `AutomationWorkflowProjectCodeWorkflowFacade.createEmptyCodeWorkflow` (drop `automationHubVisible`), `AutomationWorkflowProjectCodeWorkflowGraphQlController` accordingly.
- Modify (sample app `bytechef-samples/bytechef-embedded-sample-app`): `front-end/src/app/api/code-playground/automation/deploy/route.ts` (send `release=true`), `front-end/src/app/automation-code-playground/automation-code-playground.tsx` lines 91, 236, 320-323, 369, 408 copy ("deploys and releases it to this app's environment"), `README.md` setup step "create a configuration for `BYTECHEF_ENVIRONMENT`" for the hub and templates pages.
- Test: the branch's `AutomationWorkflowProjectCodeWorkflowFacadeIntTest` (or create it beside `AutomationWorkflowProjectFacadeIntTest`), both `AutomationProjectCodeWorkflowApiControllerIntTest`s.

- [ ] **Step 1: Rebase** `0_p_code_workflows` onto `6756` (user's call when; see memory notes on that stack) and fix compile errors from the dropped `automationHubVisible`.
- [ ] **Step 2: Write the failing tests** `testSaveWithReleaseUpdatesAnExistingConfigurationWithoutEnablingIt` (disabled configuration stays disabled, `projectVersion` moves to the new version) and `testSaveWithReleaseCreatesAnEnabledConfigurationWhenNoneExists`, plus `testSaveWithoutReleaseLeavesTheConfigurationAlone`; controller tests asserting `release` reaches the facade.
- [ ] **Step 3: Run** — Expected: FAIL.
- [ ] **Step 4: Implement** server and sample app changes.
- [ ] **Step 5: Run** the IntTests and the sample app's `npm run lint`/`npm run build` — Expected: PASS.
- [ ] **Step 6: Commit** server `6756 Release a deployed code project to the request's environment on demand` (on `0_p_code_workflows`); sample app in its own repo as `Release deployed catalog projects to the app's environment and document configurations`.
