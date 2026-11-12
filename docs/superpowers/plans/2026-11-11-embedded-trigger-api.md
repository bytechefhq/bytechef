# Embedded Trigger API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an embedded developer subscribe a connected user to any component trigger over the public API and receive every fired event as a signed POST, backed entirely by the existing trigger lifecycle and workflow engine.

**Architecture:** Each subscription owns one hidden automation project (new `ProjectOwner.EMBEDDED_TRIGGER_SUBSCRIPTION`) holding a two-node workflow: the requested trigger, then an internal `triggerSubscription/v1/deliver` task. The existing publish -> deploy -> enable path registers/schedules the trigger; the deliver task hands the trigger output to `WebhookDeliveryService` without waiting and records the execution in the external invocation log from the outcome callback. All subscription logic lives in `embedded-configuration` (where project/deployment orchestration, lifecycle hooks, ownership and Liquibase already are); only the public controller lives in `embedded-execution-public-rest` next to the Action API.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Liquibase, Spring GraphQL, OpenAPI generator, React 19 + TypeScript, TanStack Query + graphql-codegen, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-11-embedded-trigger-api-design.md` (read it first; this plan argues from it).

**Worktree:** `/Volumes/Data/bytechef/bytechef-1732`, branch `1732`. All paths below are relative to it. Run every command from the worktree root.

**Implementation notes:** this plan is kept as the design record. Where review changed the implementation, the task carries an "Implementation note" line. The main changes: the webhook settings class is `TriggerSubscriptionWebhookSettingsAccessor`; a deleted connection deletes its subscriptions; bulk disables run after commit; each delivery attempt is signed with its own send time; `INTERNAL_ERROR` (500) and `USER_INPUT_ERROR` (400) are separate from the 499 remote errors.

## Global Constraints

- All new server files are under `server/ee/` or touch EE modules: ByteChef Enterprise license header (copy from any file in `server/ee/libs/embedded/`) and `@version ee` Javadoc tag on every new class. Exception: files added to CE modules (`server/libs/...`) keep the Apache 2.0 header and no `@version ee`.
- `ExternalInvocationSource.API_TRIGGER` is appended **last** (ordinal-persisted).
- Enum columns in the new table are stored as ordinal `int` like `ConnectedUser.environment`; JSON columns as `TEXT` via `com.bytechef.commons.data.jdbc.wrapper.MapWrapper`.
- Event envelope `type` values are exactly `trigger.event`, `subscription.disabled`, `subscription.deleted`.
- Disable `reason` values are exactly `CONNECTION_DELETED`, `CONNECTION_INVALID`, `INTEGRATION_INSTANCE_DISABLED`, `INTEGRATION_INSTANCE_DELETED`, `CONNECTED_USER_DISABLED`, `CONNECTED_USER_DELETED`.
- Error codes are exactly `USER_INPUT_ERROR`, `API_ERROR`, `TIMEOUT_ERROR`, `OAUTH_REFRESH`, `NO_TRIGGER`, `INTERNAL_ERROR`.
- Statuses are exactly `ENABLED`, `DISABLED`, `CONNECTION_ERROR`, `FAILED`.
- Delivery uses `WebhookDeliveryService.submit(...)` and never blocks on the returned future inside the task (CLAUDE.md "Five executors" rules).
- Subscribing/updating/deleting one subscription must never touch another project, deployment or trigger.
- Java style per CLAUDE.md: blank line before control statements and after variable modification, no chaining except allowed DSLs, descriptive names, no `TODO:`; tests: unit `*Test`, integration `*IntTest`, one test class per source class; `@ExtendWith(ObjectMapperSetupExtension.class)` when `JsonUtils`/`MapUtils` are used.
- Client style per CLAUDE.md: sorted keys, `I`/`Props` interface suffixes, `Icon`-suffixed lucide imports, `twMerge`, hook ordering, `Ref` suffix.
- Commits: subject only, no trailers. Server `1732 <Description>`; client `1732 client - <Description>`; docs `1732 docs - <Description>`; Liquibase changelogs in their own commit `1732 Upgrade - <Description>`; generated files (OpenAPI `generated/`, `client/src/shared/middleware/graphql.ts`, client REST middleware) in their own commit. Stage by path only - the worktree may contain unrelated changes.
- Before each commit: `./gradlew spotlessApply` (server) / `cd client && npm run format` (client), scoped to touched modules where possible.
- EE microservice apps (`server/ee/apps/*`) are out of scope; monolith (`server/apps/server-app`) must boot.

## Review Focus

1. **Static-webhook URL stability across `PUT`.** A user pastes the URL once; updating params must keep `projectDeploymentId` and workflow UUID, so the URL is unchanged. Test in Task 3 (`testUpdateKeepsStaticWebhookUrl`).
2. **Neither environment URL nor override configured when an event fires.** Expected: no exception escapes the worker, an `ERROR` invocation row with message `No webhook URL configured for environment <ENV>`, subscription `lastDeliveryStatus = FAILED`. Test in Task 5 (`testHandleWithoutDestinationRecordsError`).
3. **Another user's / environment's subscription id.** `GET/PUT/DELETE/enable/disable` with a valid id that belongs to a different `externalUserId` or environment answers 404, never 403 or 200. Test in Task 7 (`testGetSubscriptionOfOtherUserReturns404`).
4. **Delivery outcome callback runs on a delivery-lane thread with no tenant context.** Expected: status/log writes succeed under the tenant captured at submit time. Test in Task 5 (`testOutcomeCallbackRunsUnderCapturedTenant`).
5. **Connection INVALID while the subscription is enabled.** Expected: trigger actually disabled through `TriggerLifecycleFacade` (not only the `enabled` flag), status `CONNECTION_ERROR`, one `subscription.disabled` with `CONNECTION_INVALID`. Test in Task 6 (`testConnectionInvalidatedDisablesTriggerAndNotifies`).

---

## File Structure

Server, `server/ee/libs/embedded/embedded-configuration/`:

| File | Responsibility |
|---|---|
| `embedded-configuration-api/.../domain/TriggerSubscription.java` | Entity for `connected_user_trigger_subscription` |
| `embedded-configuration-api/.../domain/TriggerSubscriptionStatus.java` | `ENABLED, DISABLED, CONNECTION_ERROR, FAILED` |
| `embedded-configuration-api/.../domain/TriggerSubscriptionErrorCode.java` | the six error codes |
| `embedded-configuration-api/.../domain/TriggerSubscriptionDisableReason.java` | the six reasons |
| `embedded-configuration-api/.../dto/TriggerSubscriptionDTO.java` | read model incl. `staticWebhookUrl` |
| `embedded-configuration-api/.../dto/TriggerSubscriptionWebhookSettingsDTO.java` | env URL + secret (+ masked) |
| `embedded-configuration-api/.../service/TriggerSubscriptionService.java` | CRUD + finders |
| `embedded-configuration-api/.../facade/TriggerSubscriptionFacade.java` | public API (externalUserId-scoped) |
| `embedded-configuration-api/.../facade/TriggerSubscriptionAdminFacade.java` | admin UI + connected-user lifecycle (`isTenantAdmin()`) |
| `embedded-configuration-service/.../repository/TriggerSubscriptionRepository.java` | Spring Data JDBC |
| `embedded-configuration-service/.../service/TriggerSubscriptionServiceImpl.java` | |
| `embedded-configuration-service/.../facade/TriggerSubscriptionManager.java` | hidden project/deployment orchestration, `@SkipAutomationAuthorization` |
| `embedded-configuration-service/.../facade/TriggerSubscriptionWorkflowDefinitionFactory.java` | builds the two-node workflow JSON |
| `embedded-configuration-service/.../facade/TriggerSubscriptionFacadeImpl.java`, `TriggerSubscriptionAdminFacadeImpl.java` | |
| `embedded-configuration-service/.../trigger/subscription/TriggerSubscriptionWebhookSettingsAccessor.java` | per-environment URL/secret in `property` table |
| `embedded-configuration-service/.../trigger/subscription/TriggerSubscriptionEventSender.java` | builds envelopes, submits to `WebhookDeliveryService`, handles outcome + invocation log |
| `embedded-configuration-service/.../trigger/subscription/TriggerSubscriptionDeliveryTaskHandler.java` | `TaskHandler` bean `triggerSubscription/v1/deliver` |
| `embedded-configuration-service/.../facade/TriggerSubscriptionConnectionEventListener.java` | reacts to `ConnectionCredentialsInvalidatedEvent` (moved from `trigger/subscription/` so that package no longer depends on `facade`) |
| `embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20261111120000_embedded_configuration_added_connected_user_trigger_subscription.xml` | table |
| `embedded-configuration-graphql/src/main/resources/graphql/trigger-subscription.graphqls` + `.../web/graphql/TriggerSubscriptionGraphQlController.java` | admin GraphQL |

(`...` = `src/main/java/com/bytechef/ee/embedded/configuration`.)

Other server files: `ProjectOwner.java`, `ProjectDeploymentOwner.java` (automation-configuration-api), `EmbeddedOwnerContributorConfiguration.java`, `ExternalInvocationSource.java`, new CE `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/event/ConnectionCredentialsInvalidatedEvent.java`, `ConnectionAfterSaveEventListener.java`, `IntegrationInstanceFacadeImpl.java`, `ConnectedUserConnectionFacadeImpl.java`, `ConnectedUserFacadeImpl.java`, `embedded-execution-public-rest/openapi.yaml` + new `TriggerSubscriptionApiController.java`, `application-bytechef.yml` (retry defaults).

Client: `client/src/graphql/embedded/configuration/*TriggerSubscription*.graphql`, `client/src/ee/pages/settings/embedded/trigger-events/`, `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelTriggerSubscriptionList.tsx`, `client/src/pages/automation/external-invocations/components/ExternalInvocationsTable.tsx`, `client/src/routes.tsx`.

Docs: `docs/content/docs/platform/embedded/build/trigger-subscriptions.mdx`, `component-kit.mdx`, `monitor/external-invocations.mdx`, `build/meta.json`.

---

### Task 1: Subscription table, entity, repository, service, and project ownership

**Files:**
- Create: Liquibase changelog (path in File Structure), `TriggerSubscription*.java` domain/enums, `TriggerSubscriptionService(.java|Impl.java)`, `TriggerSubscriptionRepository.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/ownership/ProjectOwner.java`, `ProjectDeploymentOwner.java` (same package), `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/ownership/EmbeddedOwnerContributorConfiguration.java`, `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/config/AutomationConfigurationJdbcRepositoryConfigurationIntTest.java` (expected owner set), `TestOwnerContributors.java` (same dir) if it enumerates owners
- Test: `embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/repository/TriggerSubscriptionRepositoryIntTest.java`, `.../service/TriggerSubscriptionServiceIntTest.java`

**Interfaces:**
- Produces:
  - Table `connected_user_trigger_subscription`: `id bigint PK autoIncrement startWith 1050`, `connected_user_id bigint not null` (FK `connected_user` ON DELETE CASCADE in a separate `contextFilter="mono"` changeSet, as in `20261015120000_...`), `environment int not null`, `component_name varchar(256) not null`, `component_version int not null`, `trigger_name varchar(256) not null`, `name varchar(256)`, `external_id varchar(256)`, `parameters text`, `webhook_url varchar(2048)`, `status int not null`, `error_code varchar(64)`, `error_message varchar(1024)`, `integration_instance_id bigint`, `connection_id bigint`, `project_id bigint` (FK `project` ON DELETE CASCADE, unique), `project_deployment_id bigint` (unique), `workflow_uuid varchar(36)`, `last_event_date timestamp`, `last_delivery_status varchar(16)`, `last_delivery_error varchar(1024)`, audit columns, `version int default 0`. Index on `(connected_user_id, environment)`.
  - `TriggerSubscription` getters/setters for every column; `getEnvironment(): Environment`, `getStatus(): TriggerSubscriptionStatus`, `getParameters(): Map<String, ?>`, `getErrorCode(): @Nullable TriggerSubscriptionErrorCode`.
  - `TriggerSubscriptionRepository extends ListCrudRepository<TriggerSubscription, Long>`: `List<TriggerSubscription> findAllByConnectedUserIdAndEnvironment(long connectedUserId, int environment)`, `List<TriggerSubscription> findAllByConnectedUserId(long connectedUserId)`, `List<TriggerSubscription> findAllByConnectionId(long connectionId)`, `List<TriggerSubscription> findAllByIntegrationInstanceId(long integrationInstanceId)`.
  - `TriggerSubscriptionService`: `TriggerSubscription create(TriggerSubscription)`, `TriggerSubscription update(TriggerSubscription)`, `void delete(long id)`, `TriggerSubscription getTriggerSubscription(long id)`, `Optional<TriggerSubscription> fetchTriggerSubscription(long id)`, `List<TriggerSubscription> getConnectedUserTriggerSubscriptions(long connectedUserId, Environment environment)`, `List<TriggerSubscription> getConnectedUserTriggerSubscriptions(long connectedUserId)`, `List<TriggerSubscription> getConnectionTriggerSubscriptions(long connectionId)`, `List<TriggerSubscription> getIntegrationInstanceTriggerSubscriptions(long integrationInstanceId)`, `void updateDelivery(long id, Instant lastEventDate, String lastDeliveryStatus, @Nullable String lastDeliveryError)` (uses a `@Modifying @Query` so concurrent deliveries do not bump `version` against facade writes).
  - `ProjectOwner.EMBEDDED_TRIGGER_SUBSCRIPTION("connected_user_trigger_subscription")`; `ProjectDeploymentOwner.EMBEDDED("connected_user_project", "connected_user_reference_deployment", "connected_user_trigger_subscription")`.
  - Beans in `EmbeddedOwnerContributorConfiguration`: `embeddedTriggerSubscriptionProjectOwnerContributor` (`EXISTS (SELECT 1 FROM connected_user_trigger_subscription WHERE connected_user_trigger_subscription.project_id = %1$s)`) and `embeddedTriggerSubscriptionProjectDeploymentOwnerContributor` (`... .project_deployment_id = %1$s`).

- [ ] **Step 1: Write the Liquibase changelog** (columns above), then commit it alone:

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20261111120000_embedded_configuration_added_connected_user_trigger_subscription.xml
git commit -m "1732 Upgrade - Add connected_user_trigger_subscription table"
```

- [ ] **Step 2: Write failing tests**

`TriggerSubscriptionRepositoryIntTest` (copy setup from `ConnectedUserReferenceDeploymentServiceIntTest`):
  - `testSaveAndFindAllByConnectedUserIdAndEnvironment`: save two rows for the same user in `DEVELOPMENT` and one in `PRODUCTION`; `findAllByConnectedUserIdAndEnvironment(userId, Environment.DEVELOPMENT.ordinal())` has size 2; parameters map round-trips (`{"channel": "C1"}`).
  - `testFindAllByConnectionId` and `testFindAllByIntegrationInstanceId`: only matching rows returned.

`TriggerSubscriptionServiceIntTest`:
  - `testUpdateDeliveryDoesNotChangeVersion`: after `updateDelivery(id, now, "SENT", null)`, `getTriggerSubscription(id).getVersion()` is unchanged and `getLastDeliveryStatus()` is `"SENT"`.

Update `AutomationConfigurationJdbcRepositoryConfigurationIntTest.assertEmbeddedOwnersMissing` to expect `EMBEDDED_AUTOMATION_PROJECT, EMBEDDED_CONNECTED_USER_PROJECT, EMBEDDED_TRIGGER_SUBSCRIPTION`.

- [ ] **Step 3: Run to verify failure**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests '*TriggerSubscription*'`
Expected: compilation failure (types missing).

- [ ] **Step 4: Implement** the entity (pattern: `ConnectedUserReferenceDeployment` + `MapWrapper` like `ProjectDeploymentWorkflow.inputs`), enums, repository, service, owner enum values and contributor beans.

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests '*TriggerSubscription*' && ./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*AutomationConfigurationJdbcRepositoryConfigurationIntTest' --tests '*ProjectOwnershipServiceIntTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/ownership server/libs/automation/automation-configuration/automation-configuration-service/src/test
git commit -m "1732 Add trigger subscription entity, service and hidden project ownership"
```

---

### Task 2: Per-environment webhook settings

**Files:**
- Create: `embedded-configuration-service/.../trigger/subscription/TriggerSubscriptionWebhookSettingsAccessor.java`, `embedded-configuration-api/.../dto/TriggerSubscriptionWebhookSettingsDTO.java`
- Test: `embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/trigger/subscription/TriggerSubscriptionWebhookSettingsAccessorTest.java`

**Interfaces:**
- Consumes: `PropertyService.fetchProperty(String key, Property.Scope scope, @Nullable Long scopeId, @Nullable Long environmentId)`, `PropertyService.save(String key, Map<String, ?> value, Scope scope, @Nullable Long scopeId, @Nullable Long environmentId)`; `EncodingUtils` for base64.
- Produces:
  - `record TriggerSubscriptionWebhookSettingsDTO(@Nullable String webhookUrl, @Nullable String signingSecret)`.
  - `TriggerSubscriptionWebhookSettingsAccessor` (`@Component`, `@ConditionalOnEEVersion`): `TriggerSubscriptionWebhookSettingsDTO getSettings(Environment environment)` (both null when absent), `TriggerSubscriptionWebhookSettingsDTO saveWebhookUrl(Environment environment, String webhookUrl)` (generates a secret if none exists), `TriggerSubscriptionWebhookSettingsDTO rotateSigningSecret(Environment environment)`. Implementation note: the class was first named `...Service` in this plan and was renamed to `...Accessor`. Rotation applies to new events at once; deliveries already queued or retrying keep the secret they were submitted with.
  - Storage: key `triggerSubscriptionWebhook`, `Property.Scope.EMBEDDED`, `scopeId = null`, `environmentId = (long) environment.ordinal()`, value `{"webhookUrl": ..., "signingSecret": ...}` (the `property` value column is already encrypted).
  - Secret format: `"whsec_" + base64url(32 SecureRandom bytes)`.

- [ ] **Step 1: Write failing tests** (mock `PropertyService`):
  - `testGetSettingsWhenAbsentReturnsNulls`
  - `testSaveWebhookUrlGeneratesSecretOnce`: first save -> secret starts with `whsec_` and length > 40; second save with a new URL keeps the same secret.
  - `testRotateSigningSecretChangesSecretAndKeepsUrl`
  - `testSaveWebhookUrlRejectsNonHttpUrl`: `ftp://x` -> `IllegalArgumentException`.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*TriggerSubscriptionWebhookSettingsAccessorTest'` - Expected: FAIL (missing class).
- [ ] **Step 3: Implement** the service.
- [ ] **Step 4: Run** the same command - Expected: PASS.
- [ ] **Step 5: Commit** `1732 Add per-environment trigger subscription webhook settings`

---

### Task 3: Workflow definition factory and the subscription manager

**Files:**
- Create: `.../facade/TriggerSubscriptionWorkflowDefinitionFactory.java`, `.../facade/TriggerSubscriptionManager.java`
- Test: `.../facade/TriggerSubscriptionWorkflowDefinitionFactoryTest.java`, `.../facade/TriggerSubscriptionManagerIntTest.java` (setup: copy `ConnectedUserReferenceDeploymentManagerIntTest`)

**Interfaces:**
- Consumes (exact): `ProjectService.create(Project)`; `ProjectWorkflowFacade.addWorkflow(long projectId, String definition)`; `ProjectFacade.publishProject(long id, @Nullable String description, boolean syncWithGit): int`; `ProjectWorkflowService` (resolve workflowId for `(projectId, version, workflowUuid)`, see `ConnectedUserReferenceDeploymentManager.getWorkflowId`); `WorkflowService.update(String id, String definition, int version)`; `ProjectDeploymentFacade.createProjectDeployment(ProjectDeployment, String workflowId, List<ProjectDeploymentWorkflowConnection>)`, `.enableProjectDeployment(long, boolean)`, `.enableProjectDeploymentWorkflow(long projectDeploymentId, String workflowId, boolean enable)`, `.updateProjectDeployment(ProjectDeployment, List<ProjectDeploymentWorkflow>, List<Tag>)`, `.deleteProjectDeployment(long)`, `.getProjectDeployment(long): ProjectDeploymentDTO` (`projectDeploymentWorkflows().getFirst().staticWebhookUrl()`); `ProjectFacade.deleteProject(long)`; `TriggerDefinitionService.getTriggerDefinition(String, int, String)`; Task 1 service.
- Produces:
  - `TriggerSubscriptionWorkflowDefinitionFactory.create(String componentName, int componentVersion, String triggerName, Map<String, ?> parameters, long subscriptionId): String` - JSON with `"label"`, one trigger `{"name": "trigger_1", "type": "<component>/v<version>/<trigger>", "parameters": {...}}` and one task `{"name": "deliver_1", "type": "triggerSubscription/v1/deliver", "parameters": {"subscriptionId": <id>, "output": "${trigger_1}"}}`. Constants `TRIGGER_NAME = "trigger_1"`, `DELIVER_TASK_TYPE = "triggerSubscription/v1/deliver"` are public.
  - `TriggerSubscriptionManager` (`@Component @ConditionalOnEEVersion @SkipAutomationAuthorization`, package `facade`, public methods):
    - `TriggerSubscription create(ConnectedUser connectedUser, @Nullable Long integrationInstanceId, @Nullable Long connectionId, String componentName, int componentVersion, String triggerName, Map<String, ?> parameters, @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl)` - validates trigger exists (else `ConfigurationException` with `NO_TRIGGER`), persists the row (status `FAILED` until enabled), creates project named `"<externalUserId> - <component>/<trigger>"` in `Workspace.DEFAULT_WORKSPACE_ID`, adds the workflow, publishes, creates the deployment with connection `{workflowNodeName: "trigger_1", key: componentName, connectionId}` set explicitly (no connection row when the component needs none, e.g. `webhook`), enables the deployment and the workflow, sets `ENABLED`. On enable failure: status `FAILED`, `errorCode = API_ERROR` (or `OAUTH_REFRESH`/`TIMEOUT_ERROR` when the cause says so), `errorMessage`, deployment left disabled, row kept; the exception is rethrown. Implementation note: `API_ERROR` is used only for component or provider failures. A missing required connection is `USER_INPUT_ERROR` and any other local failure is `INTERNAL_ERROR` with the fixed message `Internal error while enabling the trigger subscription`. The facade deletes the row of a failed create (the error then carries no subscription id, unless that delete fails).
    - `TriggerSubscription update(long id, Map<String, ?> parameters, @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl)` - disable workflow if enabled, update draft workflow definition, publish new version, `updateProjectDeployment` with the same deployment id and the new workflowId, re-enable if it was `ENABLED`. Implementation note: for a subscription in any other status the update only redeploys and keeps the status; a failure then marks nothing `FAILED` and carries no subscription id.
    - `void enable(long id)`, `void disable(long id, @Nullable TriggerSubscriptionDisableReason reason)` - reason `null` = developer request (`DISABLED`); `CONNECTION_*` -> `CONNECTION_ERROR`; other reasons -> `DISABLED`. Every disable calls `enableProjectDeploymentWorkflow(..., false)` even if the flag is already false. Implementation note: the disabled status is written before the trigger is unregistered, in the same transaction. `CONNECTION_DELETED` still maps to `CONNECTION_ERROR`, but a deleted connection now deletes its subscriptions, so the manager no longer disables with it.
    - `void delete(long id)` - disable, delete deployment, delete project, delete row. Implementation note: the trigger is unregistered first, because that needs the deployment the local delete removes; when the local delete fails afterwards the subscription stays `ENABLED` with no trigger registered, and the failure is logged at ERROR.
    - `@Nullable String getStaticWebhookUrl(TriggerSubscription subscription)`.

- [ ] **Step 1: Write failing tests**
  - Factory: `testCreateBuildsTriggerAndDeliverTask` (asserts exact JSON nodes above, parameters copied verbatim, `subscriptionId` numeric).
  - Manager IntTest (real automation services + Testcontainers; trigger lifecycle facade as a `@MockitoBean` so no real app is called; a test component trigger via the `webhook` component `webhook/v1/autoRespondWithHTTP200` (static webhook) is acceptable):
    - `testCreateEnablesTriggerOnHiddenProject`: status `ENABLED`; `TriggerLifecycleFacade.executeTriggerEnable` called once; the project is excluded from `ProjectService`/`ProjectFacade` workspace listing; `getStaticWebhookUrl` contains `/webhooks/`.
    - `testUpdateKeepsStaticWebhookUrl` (Review Focus 1): URL before == URL after `update`; `executeTriggerDisable` then `executeTriggerEnable` called.
    - `testCreateDoesNotTouchOtherSubscriptions`: create A, then B for same user; `executeTriggerDisable` never called.
    - `testEnableFailureMarksFailed`: `executeTriggerEnable` throws -> status `FAILED`, `errorCode = API_ERROR`, exception propagates, row exists.
    - `testDisableWithConnectionReasonSetsConnectionError` and `testDeleteRemovesProjectAndRow`.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*TriggerSubscriptionWorkflowDefinitionFactoryTest' && ./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:testIntegration --tests '*TriggerSubscriptionManagerIntTest'` - Expected: FAIL.
- [ ] **Step 3: Implement** factory and manager. Resolve the AUTOMATION pre-send processor ambiguity: confirm which of `ConnecteduserProjectTriggerDispatcherPreSendProcessor` / `ProjectTriggerDispatcherPreSendProcessor` runs for these deployments; if `ConnecteduserProject...` can win, make its `canProcess` return false for deployments owned by `EMBEDDED_TRIGGER_SUBSCRIPTION` (via `ProjectDeploymentFacade.getOwnedProjectDeploymentIds`) or give the plain project processor precedence, and add a unit test to that processor's existing test class.
- [ ] **Step 4: Run** the Step 2 commands - Expected: PASS.
- [ ] **Step 5: Commit** `1732 Add trigger subscription manager backed by a hidden single-workflow project`

---

### Task 4: `API_TRIGGER` invocation source

**Files:**
- Modify: `server/libs/platform/platform-external-invocation/platform-external-invocation-api/src/main/java/com/bytechef/platform/external/invocation/ExternalInvocationSource.java`, `server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest/openapi.yaml` (`ExternalInvocationSource` enum: add `A2A_SERVER`, `API_TRIGGER`; `getExternalInvocationsPage` description mentions trigger executions), `client/src/pages/automation/external-invocations/components/ExternalInvocationsTable.tsx` (`SOURCE_LABELS.API_TRIGGER = 'API (Trigger)'`, `SOURCE_TYPES.API_TRIGGER = {icon: RadioTowerIcon, label: 'Trigger'}`), the source filter options on `client/src/ee/pages/embedded/external-invocations/EmbeddedExternalInvocations.tsx` if it enumerates sources
- Test: existing `ExternalInvocationsTable.test.tsx` (add case); an enum ordinal guard in `platform-external-invocation-api` tests if one exists, else `ExternalInvocationSourceTest`

- [ ] **Step 1: Write failing tests**: `ExternalInvocationSourceTest.testOrdinalsAreStable` asserting `MCP_SERVER=0, API_ACTION=1, API_TOOL=2, A2A_SERVER=3, API_TRIGGER=4`; Vitest case `renders API (Trigger) label for API_TRIGGER source`.
- [ ] **Step 2: Run** `./gradlew :server:libs:platform:platform-external-invocation:platform-external-invocation-api:test` and `cd client && npx vitest run src/pages/automation/external-invocations/components/ExternalInvocationsTable.test.tsx` - Expected: FAIL.
- [ ] **Step 3: Implement**; then `./gradlew -p server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest generateOpenAPI`.
- [ ] **Step 4: Run** Step 2 commands - Expected: PASS.
- [ ] **Step 5: Commit** three commits: server (`1732 Add API_TRIGGER external invocation source`), generated OpenAPI sources (`1732 Regenerate embedded execution public API`), client (`1732 client - Label API_TRIGGER external invocations`).

---

### Task 5: Delivery task handler and event sender

**Files:**
- Create: `.../trigger/subscription/TriggerSubscriptionEventSender.java`, `.../trigger/subscription/TriggerSubscriptionDeliveryTaskHandler.java`
- Modify: `embedded-configuration-service/build.gradle.kts` (add `platform-notification-delivery`, `platform-external-invocation-api`, `atlas-worker-api`, `atlas-execution-api` as needed), `server/libs/config/app-config/src/main/resources/config/application-bytechef.yml` (or wherever `bytechef.webhook-delivery` lives): `bytechef.embedded.trigger-subscription.delivery.retry: {max-retries: 3, initial-interval: 1s, multiplier: 2.0}` bound via `ApplicationProperties` or a small `@ConfigurationProperties` record in the same package (defaults in yml, not Java)
- Test: `.../trigger/subscription/TriggerSubscriptionEventSenderTest.java`, `.../trigger/subscription/TriggerSubscriptionDeliveryTaskHandlerTest.java`

**Interfaces:**
- Consumes: Task 1 service, Task 2 `getSettings`, `WebhookDeliveryService.submit(WebhookDelivery): CompletableFuture<WebhookDeliveryOutcome>`, `WebhookDelivery.builder(source, url)...eventType(...).secret(...).deliveryId(...).timestamp(...).retry(...).payloads(...)`, `ExternalInvocationRecorder.record(ExternalInvocationEvent)`, `ExternalInvocationEvent.builder(PlatformType.EMBEDDED, ExternalInvocationSource.API_TRIGGER, ExternalInvocationKind.COMPONENT, triggerName)`, `TenantContext`.
- Produces:
  - `TriggerSubscriptionEventSender` (`@Component @ConditionalOnEEVersion`):
    - `void sendTriggerEvent(TriggerSubscription subscription, Object data, long jobId)` - envelope `{type: "trigger.event", id, subscriptionId, externalId, externalUserId, environment, componentName, componentVersion, triggerName, jobId, timestamp, data}`; `WebhookDelivery` `source = "triggerSubscription"`, `eventType = "trigger.event"`, `deliveryId = id` (UUID). Does not wait. Implementation note: the delivery sets no `timestamp`, so every attempt is signed with its own send time (`X-ByteChef-Timestamp` and `t=` change on a retry) while the body `timestamp` stays the event time. On completion (under the tenant captured at call time): `updateDelivery(...)` with `SENT`/`FAILED`/`DROPPED` and record one invocation row: `SUCCESS` iff `outcome.anySent()`, else `ERROR` with `errorType = "DELIVERY_" + status` and `errorMessage = outcome.lastFailure()`; `durationMs` = submit-to-completion (implementation note: `0` when nothing was submitted, and the row error type is `DELIVERY_ERROR` when building or submitting the delivery failed); `jobId`, `connectionId`, `connectedUserId`, `integrationInstanceId`, `externalUserId`, `environment` ordinal, `componentName/Version`, `operationName = triggerName`.
    - `void sendLifecycleEvent(TriggerSubscription subscription, String type, TriggerSubscriptionDisableReason reason)` - same identifying fields, `reason`, no `data`/`jobId`; outcome is logged only (no invocation row, no status update).
    - Destination: `subscription.getWebhookUrl()` else environment URL; neither -> no submit, record `ERROR` row `errorType = "NO_DESTINATION"`, message `No webhook URL configured for environment <ENV>`, and `updateDelivery(..., "FAILED", message)`.
  - `TriggerSubscriptionDeliveryTaskHandler implements TaskHandler<Void>`, `@Component("triggerSubscription/v1/deliver") @ConditionalOnEEVersion` (bean name = task type; it is picked up by the `Map<String, TaskHandler<?>> taskHandlerMap` in `TaskHandlerRegistryConfiguration`): `handle(TaskExecution)` reads `subscriptionId` and `output` from `taskExecution.getParameters()`, loads the subscription, skips (returns null, logs debug) if it is missing or not `ENABLED`, else `sendTriggerEvent(subscription, output, taskExecution.getJobId())`. Implementation note: `handle` never throws, and a missing URL or signing secret records a `NO_DESTINATION` / `NO_SIGNING_SECRET` row.

- [ ] **Step 1: Write failing tests** (mock `WebhookDeliveryService`, service, settings, recorder; complete futures manually):
  - Sender: `testSendTriggerEventBuildsEnvelopeAndDoesNotWait` (future left incomplete; method returns; captured `WebhookDelivery` has `eventType "trigger.event"`, the env secret, JSON payload containing `"subscriptionId"`, `"jobId":42`, `"data":{"id":"c1"}`), `testOverrideUrlWinsOverEnvironmentUrl`, `testSuccessfulOutcomeRecordsSuccessRow`, `testFailedOutcomeRecordsErrorRowAndFailedStatus`, `testDroppedOutcomeRecordsErrorRow`, `testHandleWithoutDestinationRecordsError` (Review Focus 2), `testOutcomeCallbackRunsUnderCapturedTenant` (Review Focus 4: complete the future on another thread with no tenant set; assert service called inside `TenantContext.getCurrentTenantId() == "tenant1"`), `testLifecycleEventHasReasonAndNoData`.
  - Handler: `testHandleSendsOutputWithJobId`, `testHandleSkipsDisabledSubscription`.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*TriggerSubscriptionEventSenderTest' --tests '*TriggerSubscriptionDeliveryTaskHandlerTest'` - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** same - Expected: PASS.
- [ ] **Step 5: Commit** `1732 Deliver trigger subscription events through the webhook delivery lane`

---

### Task 6: Connection, integration instance and connected-user lifecycle

**Files:**
- Create: CE `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/event/ConnectionCredentialsInvalidatedEvent.java` (`public record ConnectionCredentialsInvalidatedEvent(long connectionId)`), `.../facade/TriggerSubscriptionConnectionEventListener.java` (implementation note: first created in `trigger/subscription/`, then moved to `facade/`)
- Modify: `server/libs/automation/automation-connection/automation-connection-service/src/main/java/com/bytechef/automation/connection/event/ConnectionAfterSaveEventListener.java` (inject `ApplicationEventPublisher`; in the `INVALID` branch publish the event **before** `disableProjectDeploymentWorkflowByConnectionId`), `IntegrationInstanceFacadeImpl.java` (`enableIntegrationInstance(id, false)` -> manager disable all with `INTEGRATION_INSTANCE_DISABLED`; `deleteIntegrationInstance` -> `INTEGRATION_INSTANCE_DELETED` before deletion), `ConnectedUserConnectionFacadeImpl.deleteConnectedUserConnection` (`CONNECTION_DELETED` before the connection is removed; implementation note: it calls `ConnectionFacade.checkDeletable` first, then deletes every subscription of the connection and sends `subscription.deleted` with `CONNECTION_DELETED`; `ConnectionAdminFacadeImpl.deleteConnection` does the same, and `deleteConnectedUser` checks all the user's connections first), `ConnectedUserFacadeImpl.enableConnectedUser(id, false)` (-> `TriggerSubscriptionAdminFacade.disableConnectedUserTriggerSubscriptions(id, CONNECTED_USER_DISABLED)`) and `deleteConnectedUser` (first statement: `deleteConnectedUserTriggerSubscriptions(id)`)
- Create (API used above): `TriggerSubscriptionAdminFacade` interface methods `void disableConnectedUserTriggerSubscriptions(long connectedUserId, TriggerSubscriptionDisableReason reason)`, `void deleteConnectedUserTriggerSubscriptions(long connectedUserId)`; impl in `TriggerSubscriptionAdminFacadeImpl` with `@PreAuthorize("isTenantAdmin()")` (Task 8 adds the rest of this facade).
- Test: `ConnectionAfterSaveEventListenerTest` (exists? else create), `TriggerSubscriptionConnectionEventListenerTest`, extend `IntegrationInstanceFacadeImplIntTest`/`ConnectedUserFacadeIntTest` (whichever exist; one class per source class) with the new behavior, `TriggerSubscriptionManagerIntTest` gains `testConnectionInvalidatedDisablesTriggerAndNotifies` (Review Focus 5)

**Interfaces:**
- Consumes: Task 3 `TriggerSubscriptionManager.disable(long, reason)` / `delete(long)`, Task 5 `sendLifecycleEvent`.
- Produces: manager methods `void disableByConnectionId(long connectionId, TriggerSubscriptionDisableReason reason)`, `void disableByIntegrationInstanceId(long integrationInstanceId, TriggerSubscriptionDisableReason reason)`, `void disableByConnectedUserId(long connectedUserId, TriggerSubscriptionDisableReason reason)`, `void deleteByConnectedUserId(long connectedUserId)`; each iterates matching `ENABLED` subscriptions, calls `disable`/`delete`, then `sendLifecycleEvent(subscription, "subscription.disabled" | "subscription.deleted", reason)`. A failure on one subscription is logged and does not stop the others. Implementation note: the bulk disables caused by another change (integration instance, connected user, credentials) run after that change commits, each subscription in its own transaction, so a rolled-back change disables nothing; a subscription whose disable fails becomes `FAILED` with the error recorded and gets no `subscription.disabled` event.

- [ ] **Step 1: Write failing tests**:
  - `ConnectionAfterSaveEventListenerTest.testInvalidStatusPublishesEventBeforeDisablingWorkflows` (InOrder: publisher then `disableProjectDeploymentWorkflowByConnectionId`).
  - `TriggerSubscriptionConnectionEventListenerTest.testEventDisablesSubscriptionsWithConnectionInvalid`.
  - `TriggerSubscriptionManagerIntTest.testConnectionInvalidatedDisablesTriggerAndNotifies`: `executeTriggerDisable` called, status `CONNECTION_ERROR`, sender `sendLifecycleEvent(..., "subscription.disabled", CONNECTION_INVALID)` called once.
  - Integration instance: disabling -> `INTEGRATION_INSTANCE_DISABLED`; deleting -> `INTEGRATION_INSTANCE_DELETED`.
  - Connected user: disabling -> `CONNECTED_USER_DISABLED`; deleting -> `deleteConnectedUserTriggerSubscriptions` called before any integration instance deletion (InOrder) and `subscription.deleted` sent.
- [ ] **Step 2: Run** the affected modules' `test`/`testIntegration` with `--tests '*TriggerSubscription*' --tests '*ConnectionAfterSaveEventListener*' --tests '*IntegrationInstanceFacade*' --tests '*ConnectedUserFacade*'` - Expected: FAIL.
- [ ] **Step 3: Implement.** The listener uses `@EventListener` (synchronous, runs inside the token-refresh thread's tenant). Implementation note: the listener is a `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`, because a synchronous listener that opens its own transactions can deadlock against the transaction that marked the credentials invalid. Keep `server/libs/automation` free of embedded imports.
- [ ] **Step 4: Run** same - Expected: PASS.
- [ ] **Step 5: Commit** two commits: CE (`1732 Publish an event when connection credentials become invalid`), EE (`1732 Disable trigger subscriptions when their connection, integration instance or connected user goes away`).

---

### Task 7: Public REST API

**Files:**
- Create: `embedded-configuration-api/.../facade/TriggerSubscriptionFacade.java`, `embedded-configuration-api/.../dto/TriggerSubscriptionDTO.java`, `embedded-configuration-service/.../facade/TriggerSubscriptionFacadeImpl.java`, `server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest/src/main/java/com/bytechef/ee/embedded/execution/public_/web/rest/TriggerSubscriptionApiController.java`
- Modify: `embedded-execution-public-rest/openapi.yaml` (replace the commented `getComponentTriggers`, `/subscriptions*`, `Trigger`, `TriggerBasic`, `TriggerSubscription`, `TriggerSubscriptionError` sketches), `embedded-execution-public-rest/build.gradle.kts` (uncomment `embedded-configuration-api`, add `embedded-connected-user-api` if needed)
- Test: `TriggerSubscriptionFacadeImplTest` (unit, mocks), `embedded-execution-public-rest/src/test/java/.../TriggerSubscriptionApiControllerIntTest.java` (WebMvc IntTest; copy setup from an existing controller IntTest in that module)

**Interfaces:**
- Consumes: Task 3 manager, Task 1 service, `ConnectedUserService.getConnectedUser(String externalUserId, Environment)`, `IntegrationInstanceService.fetchIntegrationInstance(long connectedUserId, String componentName, Environment)` / `getIntegrationInstance(long)` (same ownership rule as `ConnectionIdHelper`: foreign instance -> `AccessDeniedException`), `ComponentDefinitionService.getComponentDefinition(String, Integer).isConnectionRequired()`, `TriggerDefinitionService` (for listing).
- Produces:
  - `record TriggerSubscriptionDTO(long id, String componentName, int componentVersion, String triggerName, @Nullable String name, @Nullable String externalId, Map<String, ?> input, @Nullable String webhookUrl, TriggerSubscriptionStatus status, @Nullable TriggerSubscriptionErrorCode errorCode, @Nullable String errorMessage, @Nullable String staticWebhookUrl, @Nullable Instant lastEventDate, @Nullable String lastDeliveryStatus, @Nullable String lastDeliveryError, Instant createdDate, Instant lastModifiedDate)`.
  - `TriggerSubscriptionFacade`: `TriggerSubscriptionDTO createTriggerSubscription(String externalUserId, String componentName, int componentVersion, String triggerName, Map<String, ?> input, @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl, @Nullable Long instanceId, Environment environment)`, `List<TriggerSubscriptionDTO> getTriggerSubscriptions(String externalUserId, Environment environment)`, `TriggerSubscriptionDTO getTriggerSubscription(String externalUserId, long id, Environment environment)`, `TriggerSubscriptionDTO updateTriggerSubscription(String externalUserId, long id, Map<String, ?> input, @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl, Environment environment)`, `void enableTriggerSubscription(String externalUserId, long id, boolean enable, Environment environment)`, `void deleteTriggerSubscription(String externalUserId, long id, Environment environment)`. Any id not owned by `(externalUserId, environment)` -> `ConfigurationException` mapped to 404 (follow how other public controllers raise not-found). Missing required connection -> 400 with `USER_INPUT_ERROR`. Implementation note: error mapping in the controller is `USER_INPUT_ERROR` and `NO_TRIGGER` -> 400 (origin `bytechef`), `INTERNAL_ERROR` -> 500 (origin `bytechef`), `API_ERROR` / `TIMEOUT_ERROR` / `OAUTH_REFRESH` -> 499 (origin `remote-provider`), subscription not found -> 404. An `X-Instance-Id` of another connected user answers 403.
  - OpenAPI (server `/api/embedded/v1`, auth/headers as `executeAction`, tag `trigger-subscription`):
    - `GET /components/{componentName}/versions/{componentVersion}/triggers` -> `getComponentTriggers` -> `Trigger[]` (`name, title, description, type, inputSchema, outputSchema, hasDynamicOutput`). Implementation note: the implemented path is `/{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers` and the facade restricts the call to the connected user of the path (403 otherwise).
    - `POST /{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers/{triggerName}/subscriptions` -> `createTriggerSubscription`, body `{input (required), name, externalId, webhookUrl}` -> 200 `TriggerSubscription`.
    - `GET /{externalUserId}/trigger-subscriptions` -> `getTriggerSubscriptions`; `GET|PUT|DELETE /{externalUserId}/trigger-subscriptions/{id}`; `POST /{externalUserId}/trigger-subscriptions/{id}/enable` and `/disable`.
    - `TriggerSubscription` model mirrors `TriggerSubscriptionDTO` (field `staticWebhookUrl` = "The ByteChef URL to register in the app; only for static webhook triggers"); `error: {code, message}`; `status` enum as in Global Constraints.

- [ ] **Step 1: Write the OpenAPI changes** and run `./gradlew -p server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest generateOpenAPI`; commit generated sources alone: `1732 Generate embedded trigger subscription public API`.
- [ ] **Step 2: Write failing tests**:
  - Facade unit: `testCreateResolvesInstanceConnectionAndDelegates`, `testCreateWithoutRequiredConnectionFails`, `testCreateWithForeignInstanceIdIsDenied`, `testGetSubscriptionOfOtherEnvironmentIsNotFound`.
  - Controller IntTest: `testCreateTriggerSubscriptionReturnsSubscription` (JSON field names as in the model), `testGetSubscriptionOfOtherUserReturns404` (Review Focus 3), `testDeleteReturns204`, `testEnableDisableReturn204`, `testGetComponentTriggers`.
- [ ] **Step 3: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests '*TriggerSubscriptionFacadeImplTest' && ./gradlew :server:ee:libs:embedded:embedded-execution:embedded-execution-public-rest:testIntegration --tests '*TriggerSubscriptionApiControllerIntTest'` - Expected: FAIL.
- [ ] **Step 4: Implement** facade and controller (controller only maps models; `@InitBinder` for `EnvironmentModel` like `ActionApiController`).
- [ ] **Step 5: Run** Step 3 - Expected: PASS.
- [ ] **Step 6: Commit** `1732 Add embedded trigger subscription public API`

---

### Task 8: Admin GraphQL

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/trigger-subscription.graphqls`, `.../web/graphql/TriggerSubscriptionGraphQlController.java`
- Modify: `TriggerSubscriptionAdminFacade(.java|Impl.java)` (extend Task 6 interface)
- Test: `embedded-configuration-graphql/src/test/java/.../web/graphql/TriggerSubscriptionGraphQlControllerIntTest.java` (copy setup from an existing GraphQL controller test there; add types to `src/test/resources/graphql/test.graphqls` if that file stubs base types)

**Interfaces:**
- Produces (schema, exact):
```graphql
extend type Query {
    connectedUserTriggerSubscriptions(connectedUserId: ID!): [TriggerSubscription!]!
    triggerSubscriptionWebhookSettings(environmentId: ID!): TriggerSubscriptionWebhookSettings!
}

extend type Mutation {
    deleteTriggerSubscription(id: ID!): Boolean
    enableTriggerSubscription(id: ID!, enable: Boolean!): Boolean
    rotateTriggerSubscriptionWebhookSecret(environmentId: ID!): TriggerSubscriptionWebhookSettings!
    sendTriggerSubscriptionTestEvent(environmentId: ID!): TriggerSubscriptionTestEventResult!
    updateTriggerSubscriptionWebhookUrl(environmentId: ID!, webhookUrl: String!): TriggerSubscriptionWebhookSettings!
}

type TriggerSubscription {
    componentName: String!
    componentVersion: Int!
    createdDate: Long
    errorCode: String
    errorMessage: String
    externalId: String
    id: ID!
    lastDeliveryError: String
    lastDeliveryStatus: String
    lastEventDate: Long
    name: String
    status: TriggerSubscriptionStatus!
    staticWebhookUrl: String
    triggerName: String!
    webhookUrl: String
}

enum TriggerSubscriptionStatus { CONNECTION_ERROR DISABLED ENABLED FAILED }

type TriggerSubscriptionWebhookSettings {
    signingSecret: String
    webhookUrl: String
}

type TriggerSubscriptionTestEventResult {
    error: String
    statusCode: Int
    success: Boolean!
}
```
  - `TriggerSubscriptionAdminFacade` (all `@PreAuthorize("isTenantAdmin()")`): `List<TriggerSubscriptionDTO> getConnectedUserTriggerSubscriptions(long connectedUserId)`, `void enableTriggerSubscription(long id, boolean enable)`, `void deleteTriggerSubscription(long id)`, `TriggerSubscriptionWebhookSettingsDTO getWebhookSettings(long environmentId)`, `TriggerSubscriptionWebhookSettingsDTO updateWebhookUrl(long environmentId, String webhookUrl)`, `TriggerSubscriptionWebhookSettingsDTO rotateSigningSecret(long environmentId)`, `WebhookTestResult sendTestEvent(long environmentId)` (uses `WebhookDeliveryService.sendTest` with a `trigger.event` envelope whose `subscriptionId` is `"test"` and `data` is `{"test": true}`), plus the two Task 6 methods.

- [ ] **Step 1: Write failing tests**: `testConnectedUserTriggerSubscriptions`, `testEnableTriggerSubscription`, `testUpdateWebhookUrlReturnsSecret`, `testSendTestEvent` (facade mocked; assert GraphQL field values).
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:testIntegration --tests '*TriggerSubscriptionGraphQlController*'` (or `test`, matching how that module names its controller tests) - Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** - Expected: PASS.
- [ ] **Step 5: Commit** `1732 Add trigger subscription admin GraphQL API`

---

### Task 9: Client - settings page, connected-user Triggers tab

**Files:**
- Create: `client/src/graphql/embedded/configuration/connectedUserTriggerSubscriptions.graphql`, `deleteTriggerSubscription.graphql`, `enableTriggerSubscription.graphql`, `triggerSubscriptionWebhookSettings.graphql`, `updateTriggerSubscriptionWebhookUrl.graphql`, `rotateTriggerSubscriptionWebhookSecret.graphql`, `sendTriggerSubscriptionTestEvent.graphql`; `client/src/ee/pages/settings/embedded/trigger-events/TriggerEvents.tsx` (+ `components/` as needed); `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelTriggerSubscriptionList.tsx`
- Modify: `client/src/routes.tsx` (route `trigger-events` under embedded settings, nav item `{href: '/embedded/settings/trigger-events', title: 'Trigger Events'}` after API Keys, same `PrivateRoute ADMIN` + `EEVersion` wrapping as Signing Keys), `ConnectedUserSheetPanel.tsx` (new tab `triggers` labelled "Triggers" between "Workflows" and "External Invocations")
- Test: `client/src/ee/pages/settings/embedded/trigger-events/tests/TriggerEvents.test.tsx`, `client/src/ee/pages/embedded/connected-users/components/connected-user-sheet/tests/ConnectedUserSheetPanelTriggerSubscriptionList.test.tsx`

**Interfaces:**
- Consumes: Task 8 schema; `useEnvironmentStore((state) => state.currentEnvironmentId)` for the settings page.
- Produces: after `cd client && npx graphql-codegen`, hooks `useConnectedUserTriggerSubscriptionsQuery`, `useEnableTriggerSubscriptionMutation`, `useDeleteTriggerSubscriptionMutation`, `useTriggerSubscriptionWebhookSettingsQuery`, `useUpdateTriggerSubscriptionWebhookUrlMutation`, `useRotateTriggerSubscriptionWebhookSecretMutation`, `useSendTriggerSubscriptionTestEventMutation`.

Behavior:
- Trigger Events page: URL input + Save; signing secret masked with Reveal and Copy; Rotate (confirm dialog; implementation note: the text says that new events are signed with the new secret right away and queued events keep the old one); "Send test event" shows `Delivered (HTTP <code>)` or the error. Empty state when no URL: "Set a webhook URL to receive trigger events for this environment."
- Triggers tab: rows with component icon, trigger name (and `name` if set), status badge (`ENABLED` success, `DISABLED` secondary, `CONNECTION_ERROR`/`FAILED` destructive, error message as tooltip), last event time, last delivery status/error; row dropdown Enable/Disable and Delete (confirm). Empty state: "No trigger subscriptions. Create them with the Trigger API."

- [ ] **Step 1: Add the `.graphql` operations, run `cd client && npx graphql-codegen`, commit the operations then the generated `src/shared/middleware/graphql.ts` separately** (`1732 client - Add trigger subscription GraphQL operations`, `1732 client - Regenerate GraphQL types`).
- [ ] **Step 2: Write failing tests**: settings - `renders empty state without webhook url`, `saves webhook url`, `reveals and copies secret`, `shows test event result`; list - `renders subscriptions with status badges`, `disables a subscription from the row menu`, `deletes after confirmation`, `renders empty state`.
- [ ] **Step 3: Run** `cd client && npx vitest run src/ee/pages/settings/embedded/trigger-events src/ee/pages/embedded/connected-users/components/connected-user-sheet/tests/ConnectedUserSheetPanelTriggerSubscriptionList.test.tsx` - Expected: FAIL.
- [ ] **Step 4: Implement** (follow `SigningKeys.tsx` and `ConnectedUserSheetPanelWorkflowList.tsx` patterns).
- [ ] **Step 5: Run** Step 3, then `npx eslint` and `npx tsc --noEmit -p .` scoped to changed files if practical - Expected: PASS.
- [ ] **Step 6: Commit** `1732 client - Add Trigger Events settings and connected user Triggers tab`

---

### Task 10: Documentation

**Files:**
- Create: `docs/content/docs/platform/embedded/build/trigger-subscriptions.mdx`
- Modify: `docs/content/docs/platform/embedded/build/meta.json` (add page after `component-kit`), `component-kit.mdx` (short "Triggers" section linking to the new page), `docs/content/docs/platform/embedded/monitor/external-invocations.mdx` (API (Trigger) rows = one per trigger execution, SUCCESS when the developer endpoint accepted the event)

Content (only what the code does): configuring Trigger Events per environment; listing triggers; subscribing (curl for `POST .../subscriptions`); static webhook `staticWebhookUrl`; envelope examples for `trigger.event`, `subscription.disabled`, `subscription.deleted`; verifying `X-ByteChef-Signature: t=<ms>,v1=<hex>` = HMAC-SHA256(secret, `t + "." + body`) with a Node and a Java snippet (check the exact encoding in `WebhookNotificationClient`); statuses, error codes, reasons; delivery semantics (one at a time per URL, retries, not durable, failed deliveries visible in External Invocations, not job failures).

- [ ] **Step 1: Write the pages.**
- [ ] **Step 2: Verify** `cd docs && npm run build` (or the docs site's lint script) - Expected: success, no broken links.
- [ ] **Step 3: Commit** `1732 docs - Document the embedded Trigger API`

---

### Task 11: End-to-end verification on a running server

- [ ] **Step 1:** `./gradlew spotlessApply && ./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check :server:ee:libs:embedded:embedded-execution:embedded-execution-public-rest:check :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:check :server:libs:automation:automation-connection:automation-connection-service:check` - Expected: BUILD SUCCESSFUL.
- [ ] **Step 2:** Start infra and the server from the worktree (`server/docker-compose.dev.infra.yml`; `./gradlew -p server/apps/server-app bootRun`, EE edition). Confirm it boots.
- [ ] **Step 3:** Start a receiver that prints headers and bodies (a 15-line Python `http.server` handler in the scratchpad, port 9911); set it as the Development Trigger Events URL in the UI; "Send test event" shows `Delivered (HTTP 200)` and the receiver prints a signed body.
- [ ] **Step 4:** With an embedded API key/JWT, create a connected user, subscribe to `webhook/v1/autoRespondWithHTTP200` (static webhook), `curl -X POST <staticWebhookUrl> -d '{"hello":"world"}'`; Expected: receiver prints `trigger.event` with `"data"` containing `hello`, a valid signature, the connected user's Triggers tab shows `ENABLED` with last delivery `SENT`, External Invocations shows one `API (Trigger)` SUCCESS row linked to the job.
- [ ] **Step 5:** Stop the receiver, fire again; Expected: `ERROR` invocation row and `lastDeliveryStatus FAILED`; the job itself `COMPLETED`.
- [ ] **Step 6:** Disable the connected user; Expected: subscription `DISABLED`, receiver (restarted) gets `subscription.disabled` with `CONNECTED_USER_DISABLED`; POST to the static URL starts no job.
- [ ] **Step 7:** Stop only the processes you started (by PID).
