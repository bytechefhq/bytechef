# Apps: Lifecycle, Instances, End-User Access and the App Portal - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn today's single `app` row into an app with configured instances, give instances an access model on the existing RBAC, let Studio users sign in to an app through the existing authorization server, record and trigger deployments through a provider SPI with Vercel first, and add the App Portal as a Studio page.

**Architecture:** `app` keeps the logical application; a new `app_instance` takes the environment, key, URL, status and every binding. `"AppInstance"` becomes a resource type on the existing visibility, grant and scope machinery with two new scopes. The authorization server gains programmatic client registration and an access-check SPI; `/api/app/v1` accepts a signed user token beside the instance key. Deployments are immutable rows written either by a record call or by an `AppDeploymentProvider`; the Vercel provider runs actions of a new `vercel` component over a ByteChef connection. The portal is a GraphQL query plus a Studio route.

**Tech Stack:** Java 25, Spring Boot 4 (Data JDBC, WebMVC, GraphQL, method security), Spring Authorization Server, Liquibase, OpenAPI generator, Spring Shell, React 19, TypeScript SDK (vitest, openapi-typescript).

**Spec:** `docs/superpowers/specs/2026-11-16-apps-lifecycle-portal-and-access-design.md`. Section names below ("Design 1" to "Design 6") refer to it.

## Phases and how they map to the proposal's roadmap

| Phase here | Proposal phase | Why re-cut |
|---|---|---|
| 1. App and instance split | 1 (domain model), part of 2 (instances) | The registry exists; the work is the split |
| 2. Configuration and access | rest of 2 | Access needs the instance to exist as a resource type |
| 3. End-user sign-in | 3 | Unchanged |
| 4. SDK, template, CLI, skill | 4 | The SDK exists; only `auth` and `context` are new |
| 5. Versions and recorded deployments | 5 (direct registration), part of 8 (versioning) | Registration already works; what it lacks is history, which needs the version table |
| 6. Provider SPI and Vercel | 6, rest of 8 (rollback) | Rollback is a deploy of an earlier version |
| 7. App Portal | 7 | Last, as the proposal asks: registration and sign-in are proven first |
| 8. Audit, metrics, docs | cross-cutting | |
| not planned | 9, 10 | Deferred in the spec |

Phases 1 to 4 deliver the proposal's Scenario A and C. Phase 7 delivers Scenario B. Each phase leaves the branch green and shippable.

## Global Constraints

- Work on the Apps branch stack (`0_p_apps`, `0_p_apps_followups`). `A` = `server/ee/libs/automation/automation-app`. `C` = `server/ee/libs/automation/automation-configuration`. `AS` = `server/libs/platform/platform-oauth2-authorization-server`. `L` = `sdks/frontend/automation/app/library/src`. `T` = `sdks/frontend/automation/app/create/template`.
- **Schema.** The app tables are in no release tag and not on `master` (spec, "Current state"). Phase 1 edits the three existing files under `A/automation-app-service/src/main/resources/config/liquibase/changelog/automation/app/` in place. Later phases add one new file per feature in the same directory. Never edit a changelog that is on `master`. After editing or renaming a changelog, delete stale copies under `build/resources/`. Local databases that ran the old changesets must be reset.
- **Commits.** Subject only. Server: `XXXX <description>`. Client: `XXXX client - <description>`. A commit that holds a Liquibase file holds nothing else and is `XXXX Upgrade - <description>`. Generated sources go in their own commits right after their source: `XXXX Generate ...` for `*/generated/*`, `XXXX client - Regenerate ...` for `client/src/shared/middleware/**`. Stage only files of the task. No commit, push or PR without the user's yes.
- **EE.** New classes under `server/ee/` carry the ByteChef Enterprise license header and `@version ee`; new beans are `@ConditionalOnEEVersion`.
- **Layering.** Facades own authorization (`@PreAuthorize` on the implementation) and never use repositories. Services never call services. Controllers and GraphQL controllers only delegate and carry no `@PreAuthorize`. A lower module never depends on an upper one: nothing under `server/libs/` may import `com.bytechef.ee.automation.app`.
- **Tests.** Unit tests end in `Test`, integration tests in `IntTest`, controller tests are WebMvc IntTests, one test class per source class, no `Impl` in test names, camelCase test method names. Write the test first and see it fail. `test` and `testIntegration` are different tasks; run both.
- **OpenAPI.** After any edit of an `openapi*.yaml` run `./gradlew -p <module dir> generateOpenAPI` at once. After a change to `openapi-management.yaml`, re-copy it to `cli/clients/automation-app/openapi.yaml` with `Environment` inlined and run `./gradlew :cli:clients:automation-app:generateClient`. After a change to `openapi.yaml`, run `npm run generate:api` in the SDK library.
- **GraphQL.** After a change to `app.graphqls` add or update operations under `client/src/graphql/automation/app/` and run `cd client && npx graphql-codegen`.
- **Enums persisted as ordinals** (`AppStatus`, `AppInstanceStatus`, `AppDeploymentStatus`, `ExternalInvocationSource`): append only once Phase 1 has merged; inside Phase 1 `AppStatus` is redefined because nothing is released.
- **Errors.** New `AppErrorType` keys continue from 129, each added to `NAMES` and to `AbstractAppApiController.toAppStatus`; `AppErrorTypeTest` pins them.
- **Style.** Java and client rules of `CLAUDE.md` (blank lines before control statements, no chaining outside the allowed cases, descriptive names, sort-keys, `I`/`Props` suffix, `Icon`-suffixed lucide imports, `twMerge`, hook order). No prose comments in code.
- **Properties.** New `bytechef.app.*` keys bind through `ApplicationProperties.App` with defaults in `server/apps/server-app/src/main/resources/config/application-bytechef.yml`; `ApplicationPropertiesTest` covers them.
- **Management REST outside DEVELOPMENT is read-only plus deploy** (spec, Design 4). Every mutating operation of `openapi-management.yaml` except version create and the deployment operations calls `AppEnvironmentScope.requireDevelopment` and answers 403 `APP_OPERATION_DEVELOPMENT_ONLY` in STAGING and PRODUCTION. The CLI repeats the refusal locally; the server rule is the one that counts and is the one tested. GraphQL (Studio) is not restricted.
- **Instances may live in another workspace than their app.** Every instance check resolves against the instance's `workspace_id`; nothing may assume it equals the app's.
- **Docs to keep true.** `.agents/apps.md` is updated in the phase that changes what it describes, in a `XXXX docs - ...` commit.

## Review Focus

1. **A suspended app stops every instance; a suspended instance stops only itself.** Filter-chain tests in Phase 1, Task 3.
2. **A key of instance X never reaches a binding of instance Y of the same app.** Phase 1, Task 4.
3. **A workspace member with `APP_USE` is refused a PRIVATE instance without a grant**, in the portal, at sign-in and on `/api/app/v1`. Phase 2 Task 2, Phase 3 Tasks 3 and 4, Phase 7 Task 2.
4. **A user token is never accepted without the instance key, never for another instance, never from the MCP token family, and never downgraded to "no user" when invalid.** Phase 3, Task 4.
5. **Access removed between two calls takes effect on the second call.** Phase 3, Task 4.
6. **A deployment row is never updated after a terminal status.** Phase 5 Task 2, Phase 6 Task 3.
7. **No secret in the browser bundle:** the session secret and tokens join the existing key checks. Phase 4, Task 3.
8. **Creating an instance in another workspace needs `APP_MANAGE` on the app and in the target workspace; either alone is refused.** The target workspace's admin can manage the instance and cannot change the app. Phase 1, Task 4.
9. **The CLI rule holds without the CLI:** a raw management REST call with an API key cannot create an instance, rotate a key or change a binding outside DEVELOPMENT. Phase 1, Task 5.
10. **A new instance requires sign-in by default; a row not created through the facade is `NONE`.** Phase 3, Task 3.
11. **An instance has its own owner.** The target workspace's admin who created a cross-workspace instance can share it; the app's owner, not a member there, cannot. Phase 2, Task 2.

---

## Phase 1: App and instance split

### Task 1: Schema

**Files:**
- Modify: `A/automation-app-service/src/main/resources/config/liquibase/changelog/automation/app/20261006000001_automation_app_init.xml`
- Modify: `.../20261007000001_automation_app_workflow.xml`, `.../20261016000001_automation_app_api_collection.xml`
- Create: `server/ee/libs/platform/platform-external-invocation-log/platform-external-invocation-log-service/src/main/resources/config/liquibase/changelog/platform/external/invocation/log/20261116000001_external_invocation_log_added_app_instance_id_and_user_id.xml`

- [ ] **Step 1:** Rewrite the init changeset to the two tables of spec "Design 1" (`app`: drop `environment`, `hosting_target`, `url`, `last_deployed_date`, `secret_key_*`; `workspace_id` NOT NULL; add `category_id`, `icon`, `portal_discoverable`; unique `uk_app_workspace_name`. `app_instance`: all columns listed in the spec, including its own `owner_user_id BIGINT NOT NULL` (the creator of the instance, not the app's owner), `user_auth_mode` with column default `NONE` and `icon` on `app` as `TEXT` for a serialized `FileEntry`, except the Phase 5 and 6 ones, which are added now so the file is final: `deployment_connection_id`, `deployment_settings`, `current_app_deployment_id` without foreign keys). In `app_connection`, `app_workflow`, `app_workflow_run`, `app_api_collection` rename `app_id` to `app_instance_id` with FKs to `app_instance`, cascade delete, and rename the constraints and indexes to match.
- [ ] **Step 2:** New invocation-log changeset: nullable `app_instance_id BIGINT` and `user_id BIGINT`, an index on `app_instance_id`.
- [ ] **Step 3: Verify** by running an existing Apps IntTest that boots Liquibase: `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:testIntegration --tests '*AppRepositoryIntTest'`. It fails to compile until Task 2; the changelog itself must apply (run it after Task 2 if compilation blocks).
- [ ] **Step 4: Commit** `XXXX Upgrade - Split the app table into app and app_instance` and `XXXX Upgrade - Add the app instance id and user id to the external invocation log`.

### Task 2: Domain, repositories and services

**Files:**
- Modify: `A/automation-app-api/.../domain/App.java`, `AppStatus.java` (`DRAFT, ACTIVE, SUSPENDED, ARCHIVED`), `AppConnection.java`, `AppWorkflow.java`, `AppWorkflowRun.java`, `AppApiCollection.java`
- Create: `A/automation-app-api/.../domain/AppInstance.java`, `AppInstanceStatus.java`, `AppUserAuthMode.java`; `.../service/AppInstanceService.java`
- Create: `A/automation-app-service/.../repository/AppInstanceRepository.java`, `.../service/AppInstanceServiceImpl.java`
- Modify: `A/automation-app-api/.../service/AppService.java` and impl (key methods move to `AppInstanceService`: `fetchAppInstanceBySecretKey`, `rotateSecretKey`, `touchSecretKeyLastUsedDate`), `AppConnectionService`, `AppWorkflowService`, `AppApiCollectionService` and their impls and repositories (parameter `appId` becomes `appInstanceId`)
- Tests: adapt `AppRepositoryIntTest`, `AppServiceIntTest`, `AppConnectionRepositoryIntTest`, `AppWorkflowRepositoryIntTest`, `AppWorkflowRunRepositoryIntTest`, `AppApiCollectionServiceIntTest`, `AppConnectionServiceTest`, `AppWorkflowServiceTest`, `AppSecretKeysTest`; create `AppInstanceRepositoryIntTest`, `AppInstanceServiceIntTest`

- [ ] **Step 1: Tests first.** `AppInstanceServiceIntTest`: `testCreateGeneratesKeyAndReturnsItOnce`, `testTwoInstancesOfOneAppInOneEnvironment`, `testDuplicateNameInEnvironmentIsRejected`, `testRotateReplacesHash`, `testDeletingAppCascadesToInstancesAndBindings`, `testUpdateIsKeepWhenNull`, `testOwnerUserIdIsRequired`. Run, FAIL.
- [ ] **Step 2: Implement.** `AppInstanceServiceImpl` does not call `AppService`; the facade composes them.
- [ ] **Step 3: Verify** `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:test :server:ee:libs:automation:automation-app:automation-app-service:testIntegration`.
- [ ] **Step 4: Commit** `XXXX Add app instances and move the secret key and bindings to them`.

### Task 3: Principal and key authentication

**Files:**
- Modify: `A/automation-app-api/.../security/AppPrincipal.java` (add `appInstanceId`; `user` arrives in Phase 3)
- Modify: `server/ee/libs/automation/automation-security-web/automation-security-web-impl/.../security/web/app/AppApiKeyAuthenticationProvider.java`
- Tests: `.../app/AppApiKeyFilterChainTest.java` (extend), the provider's existing unit test if present

- [ ] **Step 1: Tests.** Add `testSuspendedAppRejectsEveryInstanceKey`, `testSuspendedInstanceRejectsOnlyItsOwnKey`, `testTerminatedInstanceKeyIsRejected`, `testDraftAppKeyAuthenticates`. Run, FAIL.
- [ ] **Step 2: Implement** the lookup by instance key hash and the status rule of spec "Lifecycle".
- [ ] **Step 3: Verify** `./gradlew :server:ee:libs:automation:automation-security-web:automation-security-web-impl:check`.
- [ ] **Step 4: Commit** `XXXX Authenticate app keys as an app instance`.

### Task 4: Facades

**Files:**
- Create: `A/automation-app-api/.../facade/AppInstanceFacade.java`, `.../dto/AppInstanceDTO.java`; `A/automation-app-service/.../facade/AppInstanceFacadeImpl.java`
- Modify: `AppFacade`/`AppFacadeImpl` (app fields only; `createApp` also creates the first instance and returns its key; `updateAppStatus` takes the new statuses; bindings, key rotation and bindable lookups move to `AppInstanceFacade`), `AppWorkflowFacade(Impl)`, `AppApiCollectionFacade(Impl)` (keyed by instance), `AppExecutionFacadeImpl`, `AppWorkflowExecutionFacadeImpl`, `AppApiExecutionFacadeImpl` (resolve the instance from `AppPrincipal`, record `appInstanceId` on the invocation event), `AppConnectionUsageChecker`
- Modify: `server/libs/platform/platform-external-invocation/platform-external-invocation-api/.../ExternalInvocationEvent.java` (builder `appInstanceId`, `userId`) and the EE log domain, service and mapper under `server/ee/libs/platform/platform-external-invocation-log/`
- Modify: `A/automation-app-service/.../security/` (the four `"App"` beans read the slimmer `App`; `AppEnvironmentResolver` is removed because an app no longer has an environment) and create the four `"AppInstance"` beans: `AppInstanceVisibilityPolicy`, `AppInstanceVisibilityProvider` (the instance's visibility and `created_by`), `AppInstanceOwnershipResolver` (the instance's own `owner_user_id` and `workspace_id`, never the app's owner), `AppInstanceEnvironmentResolver`. `AppInstanceFacadeImpl.createAppInstance` sets `owner_user_id` to the current user; `AppFacadeImpl.createApp` sets it on the first instance to the app's owner
- Modify: `A/automation-app-service/.../audit/AppAuditEvent.java` (lifecycle names of spec "Monitoring and audit"; remove `APP_ENABLED`, `APP_DISABLED`)
- Tests: adapt `AppFacadeTest`, `AppFacadeAuthorizationTest`, `AppWorkflowFacadeTest`, `AppWorkflowFacadeAuthorizationTest`, `AppApiCollectionFacadeTest`, `AppApiCollectionFacadeAuthorizationTest`, `AppExecutionFacadeIntTest`, `AppWorkflowExecutionFacadeIntTest`, `AppApiExecutionFacadeTest`, `AppResolversTest`, `AppConnectionUsageCheckerIntTest`; create `AppInstanceFacadeTest`, `AppInstanceFacadeAuthorizationTest`

**Guards on `AppInstanceFacadeImpl`:** `createAppInstance(appId, workspaceId, environment, ...)` requires `hasPermission(#appId, 'App', 'APP_MANAGE') and hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)`, where `workspaceId` is the target workspace and may differ from the app's; `AppInstanceDTO` carries a read-only `app` summary (id, name, description, category, icon, status) and `AppFacade.getApp` returns foreign instances without bindings, key hint or configuration; read `hasPermission(#id, 'AppInstance', 'APP_VIEW')`; everything else `hasPermission(#id, 'AppInstance', 'APP_MANAGE')`; `bindConnection` keeps the additional `CONNECTION_EDIT` check.

- [ ] **Step 1: Tests.** New cases: `testKeyOfOneInstanceCannotUseBindingOfSibling` (Review Focus 2, in `AppExecutionFacadeIntTest`), `testCreateAppCreatesFirstInstance`, `testAppWithoutEnvironmentRoleCannotCreateProductionInstance`, `testTerminateRemovesBindingsAndKeepsRow`; cross-workspace (Review Focus 8, in `AppInstanceFacadeAuthorizationTest` and `AppInstanceFacadeTest`): `testCreateInOtherWorkspaceNeedsManageOnAppAndInTargetWorkspace`, `testManageOnAppAloneIsRefused`, `testManageInTargetWorkspaceAloneIsRefused`, `testTargetWorkspaceAdminManagesInstanceButCannotUpdateApp`, `testOwningWorkspaceSeesForeignInstanceWithoutBindingsOrKeyHint`, `testForeignInstanceBindsOnlyConnectionsOfItsOwnWorkspace`, `testSuspendingAppStopsForeignInstance`, `testCreatorBecomesInstanceOwner`, `testFirstInstanceOwnerIsAppOwner`; in `AppResolversTest`: `testInstanceOwnerIsNotAppOwnerForForeignInstance`. Run, FAIL.
- [ ] **Step 2: Implement.**
- [ ] **Step 3: Verify** `./gradlew :server:ee:libs:automation:automation-app:check :server:ee:libs:automation:automation-app:automation-app-service:testIntegration :server:ee:libs:platform:platform-external-invocation-log:check`.
- [ ] **Step 4: Commit** `XXXX Move app bindings, execution and key rotation to app instances`.

### Task 5: GraphQL and management REST

**Files:**
- Modify: `A/automation-app-graphql/src/main/resources/graphql/app.graphqls` (type `AppInstance`, enum `AppInstanceStatus`, new `AppStatus` values, `App.instances`, queries `appInstance`, `appInstances(appId)`, mutations `createAppInstance`, `updateAppInstance`, `updateAppInstanceStatus`, `deleteAppInstance`, `rotateAppInstanceSecretKey`; binding operations take `appInstanceId`; `createAppInstance` takes the target `workspaceId`; `hostedAppInstances(workspaceId, environmentId)` lists instances a workspace hosts of apps owned elsewhere); `apps(workspaceId)` drops `environmentId`
- Create: `A/automation-app-graphql/.../web/graphql/AppInstanceGraphQlController.java`; modify the three existing controllers
- Modify: `A/automation-app-public-rest/openapi-management.yaml` (spec "APIs": `/apps/{id}/instances`, `/app-instances/{id}`, `/app-instances/{id}/secret-key`, bindings under `/app-instances/{id}/...`), controllers under `.../public_/web/rest/`, `AppEnvironmentScope.java` (scopes an instance, not an app, to `X-Environment`; new `requireDevelopment`, called first by every mutating operation: app create, update, delete and status, instance create, update, delete and status, key rotation, every bind and unbind), `AppErrorType.java` (`APP_OPERATION_DEVELOPMENT_ONLY`, 403)
- Tests: adapt `AppGraphQlControllerTest`, `AppWorkflowGraphQlControllerTest`, `AppApiCollectionGraphQlControllerTest`, `AppApiControllerIntTest`, `AppWorkflowBindingApiControllerIntTest`, `AppApiCollectionBindingApiControllerIntTest`, `WorkspaceConnectionApiControllerIntTest`; create `AppInstanceGraphQlControllerTest` (with the no-`@PreAuthorize` reflection check) and `AppInstanceApiControllerIntTest`

- [ ] **Step 1: Tests**, including `testInstanceOfAnotherEnvironmentIs404` and, for Review Focus 9, in `AppInstanceApiControllerIntTest`, `AppApiControllerIntTest` and the three binding controller IntTests, parameterised over STAGING and PRODUCTION: `testCreateInstanceOutsideDevelopmentIs403`, `testRotateKeyOutsideDevelopmentIs403`, `testBindOutsideDevelopmentIs403`, `testUpdateAndDeleteOutsideDevelopmentAre403`, `testReadsOutsideDevelopmentAreAllowed`, and `testRefusalHappensBeforeTheFacadeIsCalled` (the facade mock sees no interaction). Run, FAIL.
- [ ] **Step 2: Implement**, then `./gradlew -p server/ee/libs/automation/automation-app/automation-app-public-rest generateOpenAPI`.
- [ ] **Step 3: Verify** `./gradlew :server:ee:libs:automation:automation-app:check`; `AppLoopToolsIntTest` in `server-app` still passes (`./gradlew :server:apps:server-app:testIntegration --tests '*AppLoopToolsIntTest'`) after `AppLoopTools` (EE `automation-ai-tool`) is adapted to instance ids.
- [ ] **Step 4: Commit** `XXXX Expose app instances on the Apps GraphQL and public REST API`, then `XXXX Generate the App public REST sources`.

### Task 6: CLI and SDK regeneration

**Files:**
- Modify: `cli/clients/automation-app/openapi.yaml` (re-copy, inline `Environment`), regenerate
- Modify: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppLink.java`, `AppLinkFile.java` (`instanceId`), `AppCommandSupport.java`, `AutomationAppCommand.java`, `AutomationAppBindingCommand.java`
- Modify: `L/generated/openapi.ts` only if `openapi.yaml` changed (it does not in this phase)
- Tests: `AppLinkFileTest`, `AutomationAppCommandTest`, `AutomationAppBindingCommandTest`

- [ ] **Step 1: Tests.** `testListAndStatusWorkInProduction`, `testCreateAndKeyRotateAndBindAreRefusedLocallyOutsideDevelopment`, `testCreateWritesInstanceId`, `testLinkFileWithoutInstanceIdIsRejectedWithRelinkHint`, existing bind tests against `/app-instances/{id}/...`. Run, FAIL.
- [ ] **Step 2: Implement.**
- [ ] **Step 3: Verify** `./gradlew :cli:commands:automation:check`; `AppTemplateCliCommandDriftTest` passes.
- [ ] **Step 4: Commit** `XXXX cli - Address app instances in the automation app commands`, then `XXXX Generate the CLI app client`.

### Task 7: Admin UI

**Files:**
- Modify: `client/src/graphql/automation/app/*.graphql`; create `appInstance.graphql`, `appInstances.graphql`, `createAppInstance.graphql`, `updateAppInstance.graphql`, `updateAppInstanceStatus.graphql`, `deleteAppInstance.graphql`, `rotateAppInstanceSecretKey.graphql`
- Modify: `client/src/pages/automation/apps/Apps.tsx`, `App.tsx` (an Instances tab; the Connections, API collections, Workflows and Activity tabs act on the instance chosen in an instance selector), `components/AppDialog.tsx`, `AppOverviewTab.tsx`, `AppStatusSwitch.tsx`, `AppRotateKeyDialog.tsx`, `AppBindingsTab.tsx`, `AppWorkflowsTab.tsx`, `AppApiCollectionsTab.tsx`, `AppActivityTab.tsx`
- Create: `client/src/pages/automation/apps/components/AppInstancesTab.tsx`, `AppInstanceDialog.tsx` (with a target workspace picker limited to workspaces where the user holds `APP_MANAGE`), `AppInstanceSelect.tsx`, `HostedAppInstances.tsx` (the section of `Apps.tsx` for instances of apps owned elsewhere, with the parent app shown read-only)
- Tests: adapt `client/src/pages/automation/apps/tests/*.test.tsx`; create `AppInstancesTab.test.tsx`, `AppInstanceDialog.test.tsx`

- [ ] **Step 1: Tests.** Instances tab lists instances grouped by environment; creating one shows the key once; the status control offers Suspend and Activate; manage controls are hidden without `APP_MANAGE`; a foreign instance row shows no bindings and no manage controls; a hosted instance shows the parent app without edit controls. Run `npx vitest run src/pages/automation/apps`, FAIL.
- [ ] **Step 2: Implement**, `npx graphql-codegen`.
- [ ] **Step 3: Verify** `npx vitest run src/pages/automation/apps` passes; full `npm run check` once before the phase is pushed.
- [ ] **Step 4: Commit** `XXXX client - Manage app instances on the Apps pages`, then `XXXX client - Regenerate the GraphQL client`.

### Task 8: Phase verification and docs

- [ ] `./gradlew spotlessApply`, then `./gradlew :server:ee:libs:automation:automation-app:check :server:ee:libs:automation:automation-security-web:automation-security-web-impl:check :cli:commands:automation:check`, then the module `testIntegration` tasks.
- [ ] Boot `server-app` against a fresh database; create an app in the UI; create a second DEVELOPMENT instance; bind a connection to each; call `GET /api/app/v1/connections` with each key and see only that instance's binding; suspend the app and see both keys answer 401.
- [ ] Update `.agents/apps.md` (module map, the key, admin side, CLI, the stale `ToolExecution*` names). Commit `XXXX docs - Describe app instances in the Apps overview`.

---

## Phase 2: Configuration and access

### Task 1: Scopes

**Files:**
- Modify: `C/automation-configuration-service/.../security/scope/AppPermissionScope.java` (`APP_USE`, `APP_DEPLOY`), `AppPermissionScopeProvider.java` (`APP_USE` VIEWER, `APP_DEPLOY` EDITOR)
- Tests: `C/automation-configuration-service/src/test/.../service/PermissionScopeRegistryTest.java`

- [ ] Test `testAppScopesAreAssignedByRank` (VIEWER has `APP_VIEW`, `APP_USE`; EDITOR adds `APP_DEPLOY`; ADMIN adds `APP_MANAGE`), FAIL, implement, `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test`.
- [ ] Confirm in the running client that both scopes appear in the custom role editor without a client change; if the editor lists scopes from a hard-coded label map, add labels there in the client commit of Task 4.
- [ ] **Commit** `XXXX Add the APP_USE and APP_DEPLOY permission scopes`.

### Task 2: Sharing facade

**Files:**
- Create: `A/automation-app-api/.../facade/AppSharingFacade.java`; `A/automation-app-service/.../facade/AppSharingFacadeImpl.java`
- Modify: `AppFacade.updateApp` (drop `visibility`), `AppInstanceFacadeImpl.deleteAppInstance` (delete grants), `AppAuditEvent.java` (six sharing events)
- Modify: `app.graphqls` (`setAppVisibility`, `grantAppAccess`, `revokeAppAccess`, `appGrants`, and the four `AppInstance` equivalents), create `A/automation-app-graphql/.../web/graphql/AppSharingGraphQlController.java`
- Tests: create `AppSharingFacadeTest`, `AppSharingFacadeAuthorizationTest`, `AppSharingGraphQlControllerTest`; create `AppInstancePermissionIntTest` in `A/automation-app-service/src/test` modelled on `C/.../service/PermissionServiceVisibilityTest.java`

- [ ] **Step 1: Tests.** `testMemberWithAppUseIsRefusedPrivateInstanceWithoutGrant` (Review Focus 3), `testGrantMakesPrivateInstanceUsable`, `testGrantToNonMemberFailsLikeUnknownUser`, `testOwnerOrAppManageHolderMayShare`, `testWorkspaceAdminWhoIsNotOwnerSharesWorkspaceVisibleInstance`, `testEditorWhoIsNotOwnerCannotShare`, `testTargetWorkspaceAdminWhoCreatedForeignInstanceCanShareIt`, `testAppOwnerWhoIsNotMemberOfInstanceWorkspaceCannotShareIt` (both against the real `PermissionServiceImpl` in `AppInstancePermissionIntTest`, with the app's owner absent from `workspace_user` of the target workspace), `testPlainUserCannotListGrants`, `testViewerInProductionOnlyIsRefusedDevelopmentInstance`, `testGrantIsIdempotent`. Run, FAIL.
- [ ] **Step 2: Implement**, copying `C/automation-configuration-service/.../facade/ProjectSharingFacadeImpl.java` including its narrowing behaviour, but with the guard `@permissionService.isResourceOwner('AppInstance', #appInstanceId) || hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')` (and the `'App'` form) on all eight methods, as the spec's "Who may share" explains.
- [ ] **Step 3: Verify** module `check` and `testIntegration`.
- [ ] **Step 4: Commit** `XXXX Add visibility and grants for apps and app instances`.

### Task 3: Member removal

**Files:**
- Create: `A/automation-app-service/.../listener/AppWorkspaceUserRemovalListener.java`
- Tests: `AppWorkspaceUserRemovalListenerTest`

- [ ] Tests `testRemovalRevokesAppAndInstanceGrantsOfThatWorkspaceOnly`, `testOwnerLeavingPublishesOwnerLeftForPrivateAppsAndInstancesOnly`, `testOwnerLeavingDoesNotChangeVisibilityOrOwner`; in `AppInstancePermissionIntTest`: `testAfterOwnerLeftWorkspaceAdminManagesWorkspaceVisibleInstance`, `testAfterOwnerLeftPrivateInstanceIsManagedOnlyByTenantAdmin`, `testAppOwnerLeavingAppWorkspaceLeavesForeignInstanceOwnerIntact`. Add `APP_OWNER_LEFT` and `APP_INSTANCE_OWNER_LEFT` to `AppAuditEvent.java`. FAIL, implement (the OAuth2 and favorites parts are added in Phases 3 and 7), verify, **commit** `XXXX Revoke app grants when a member leaves the workspace`.

### Task 4: Configuration and the sharing UI

**Files:**
- Modify: `AppInstanceFacade(Impl)` (`updateAppInstanceConfiguration`, `APP_MANAGE`; 64 KB cap; JSON object only), `AppInstanceDTO`, `app.graphqls`, `openapi-management.yaml` (`configuration` on the instance model)
- Client: create `client/src/pages/automation/apps/components/AppInstanceShareDialog.tsx`, `AppInstanceConfigurationDialog.tsx`; modify `AppInstancesTab.tsx`, `AppDialog.tsx` (visibility through the sharing mutation); operations under `client/src/graphql/automation/app/`
- Tests: `AppInstanceFacadeTest` (`testConfigurationOverLimitIsRejected`, `testConfigurationMustBeAnObject`), `AppInstanceShareDialog.test.tsx`, `AppInstanceConfigurationDialog.test.tsx`

- [ ] Tests, FAIL, implement reusing `client/src/shared/components/visibility/ResourceVisibilityPicker.tsx`, `ResourceVisibilityBadge.tsx`, `client/src/shared/hooks/useCanManageResourceSharing.ts` and the layout of `client/src/pages/automation/project/components/ProjectShareDialog.tsx`; regenerate; verify with the module checks and `npx vitest run src/pages/automation/apps`.
- [ ] **Commits** `XXXX Add per-instance app configuration`, `XXXX Generate the App public REST sources`, `XXXX client - Share app instances and edit their configuration`, `XXXX client - Regenerate the GraphQL client`.

### Task 5: App icon in file storage

**Files:**
- Create: `A/automation-app-api/.../file/storage/AppFileStorage.java`; `A/automation-app-service/.../file/storage/AppFileStorageImpl.java` and its configuration, wrapping the core `FileStorageService` through `FileStorageServiceRegistry` as `server/libs/platform/platform-knowledge-base/platform-knowledge-base-file-storage/platform-knowledge-base-file-storage-impl/.../KnowledgeBaseFileStorageImpl.java` and its `config/KnowledgeBaseFileStorageConfiguration.java` do
- Modify: `AppFacade(Impl)` (`updateAppIcon`, `deleteAppIcon`: `APP_MANAGE` on the app; `getAppIcon`: `APP_VIEW` on the app, or the app is in the caller's portal once Phase 7 lands), `AppServiceImpl` (store the `FileEntry`; delete the old file on replace and on app delete), `AppErrorType.java` (`APP_ICON_INVALID`)
- Create: an icon upload and download controller in the app module. Pick the existing session-authenticated multipart upload controller to copy at task time (the spec did not verify which one fits) and keep authorization on the facade
- Tests: `AppFileStorageTest`, `AppFacadeTest` (`testIconOfUnsupportedTypeIsRejected`, `testSvgIsRejected`, `testIconOverLimitIsRejected`, `testReplacingIconDeletesOldFile`, `testDeletingAppDeletesIcon`), `AppFacadeAuthorizationTest`, the controller's WebMvc IntTest

- [ ] Tests, FAIL, implement, verify module `check` and `testIntegration` (with `bytechef.file-storage.provider` set as the existing Apps IntTests set it).
- [ ] **Commit** `XXXX Store app icons in file storage`.

---

## Phase 3: End-user sign-in

### Task 1: Settle what the authorization server issues

**Files:**
- Tests only: `AS/src/test/java/com/bytechef/platform/oauth2/authorizationserver/AppClientAuthorizationIntTest.java` (name it for what it proves; model on `McpOAuth2EndToEndIntTest.java` in the same directory)

- [ ] Write an IntTest that saves a public PKCE client with scopes `openid profile app:use` straight into the `RegisteredClientRepository` and runs authorize, token and refresh. Assert and record in the test: whether an `id_token` is returned, whether `/userinfo` answers, and the `aud` of the access token. These are the two "Could not verify" items of the spec.
- [ ] If no ID token is issued: enable Spring Authorization Server's OIDC support in `AS/.../config/Oauth2AuthorizationServerConfiguration.java`, add `sub`, `name` and `email` to the ID token in `jwtTokenCustomizer`, and re-run every existing test in the module (`./gradlew :server:libs:platform:platform-oauth2-authorization-server:check :server:libs:platform:platform-oauth2-authorization-server:testIntegration`) to prove MCP behaviour is unchanged. If `aud` is not the client id: add an audience customizer for clients whose scopes contain `app:use`, beside `McpResourceAudienceTokenCustomizer`.
- [ ] **Commit** `XXXX Issue OIDC tokens to first-party app clients`.

### Task 2: Client registration and the access-check SPI

**Files:**
- Modify: `AS/.../service/RegisteredClientService.java` and impl (`registerClient`, `updateClient`), `AS/.../facade/RegisteredClientFacade.java` and impl are not extended (no user calls this; the app facade does)
- Create: `AS/.../authentication/OAuth2ClientAccessChecker.java` (SPI) and the validator that applies every checker to `OAuth2AuthorizationCodeRequestAuthenticationProvider`; wire in `Oauth2AuthorizationServerConfiguration.java` next to `setAuthorizationCodeGenerator`
- Tests: `AS/src/test/.../service/RegisteredClientServiceIntTest` (extend or create), `AS/src/test/.../authentication/OAuth2ClientAccessCheckerIntTest.java`

- [ ] **Tests.** `testRegisterClientIsPublicWithProofKey`, `testUpdateClientReplacesRedirectUris`, `testDeniedCheckerAnswersAccessDenied`, `testCheckerNotSupportingClientIsIgnored`, `testDynamicRegistrationStillRejectsAppScope`. FAIL, implement, verify with the module's `check` and `testIntegration`.
- [ ] **Commit** `XXXX Add programmatic client registration and an access check to the authorization server`.

### Task 3: Instance clients

**Files:**
- Modify: `AppInstanceFacade(Impl)` (`createAppInstance` now writes `REQUIRED` unless a mode is passed, and registers the client as soon as the instance has a URL; the column default stays `NONE`; `updateAppInstanceUserAuth(id, mode)`: registers, updates or deletes the client through `ObjectProvider<RegisteredClientService>`; refuses with `APP_USER_AUTH_UNAVAILABLE`; revokes grants on suspend, terminate and app suspend or archive), `AppErrorType.java`, `AppAuditEvent.java` (`APP_USER_AUTH_CHANGED`)
- Create: `A/automation-app-service/.../security/AppInstanceClientAccessChecker.java`
- Do not modify `server/apps/server-app/src/main/resources/config/application-bytechef.yml` (`bytechef.oauth2.authorization-server.enabled` stays `false`; note its `edition: EE` default) or `application-prod.yml`
- Modify: `server/apps/server-app/src/main/resources/config/application-dev.yml` (add `bytechef.oauth2.authorization-server.enabled: true` under the existing `bytechef:` block, which already has `public-url: http://127.0.0.1:5173`). `bootRun` runs under `dev,local` (`buildSrc/src/main/kotlin/com.bytechef.java-spring-boot-conventions.gradle.kts`), so this is what a local server gets
- No change to any `application-testint.yml` or to `client/playwright.config.ts`. Because the create default becomes `REQUIRED`, adapt every Apps test that creates an instance through the facade: pass `NONE` explicitly, or enable the server for that test as `ServerApplicationIntTest` does
- Modify: `T/AGENTS.md` and `T/README.md` (one sentence: local sign-in redirects to the dev `public-url`, so the Studio client dev server must be running)
- Create: `kubernetes/helm/bytechef/values-ee.yaml` (overlay with only `env.normal.BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED: "true"`; no `BYTECHEF_EDITION`, which would repeat the application default; `kubernetes/helm/bytechef/values.yaml` unchanged pending the spec's remaining question) and root `docker-compose.cluster.ee.yml` (override of `x-bytechef-environment` in `docker-compose.cluster.yml` setting `BYTECHEF_EDITION: EE`, needed because that file defaults to CE, and `BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED: "true"`). No Java default, no `matchIfMissing`
- Tests: `server/libs/config/app-config/src/test/java/com/bytechef/config/ApplicationPropertiesTest.java` or the test beside it that reads `application-bytechef.yml` gains `testAuthorizationServerIsOffInSharedDefaults`; in `AS/src/test`, `Oauth2AuthorizationServerConfigurationIntTest` (or a sibling `ApplicationContextRunner` test) gains `testNoAuthorizationServerBeansWhenPropertyIsAbsent` and `testNoAuthorizationServerBeansWhenPropertyIsFalse`
- Modify: `A/automation-app-service/build.gradle.kts` (depend on `:server:libs:platform:platform-oauth2-authorization-server`), `AppWorkspaceUserRemovalListener.java` (revoke the user's authorizations after commit)
- Modify: `app.graphqls`, `openapi-management.yaml`, client `AppInstanceDialog.tsx` (a sign-in mode control, preselected to Required)
- Tests: `AppInstanceFacadeTest`, `AppInstanceClientAccessCheckerTest`, `AppWorkspaceUserRemovalListenerTest`; `server/apps/server-app/src/test/java/com/bytechef/server/AppSignInIntTest.java` for the end-to-end flow

- [ ] **Tests.** Review Focus 10: `testNewInstanceDefaultsToRequired`, `testExplicitNoneIsKeptOnCreate`, `testRowInsertedWithoutModeIsNone` (repository-level, in `AppInstanceRepositoryIntTest`), `testRequiredInstanceWithoutUrlAnswersUserRequiredAndRegistersClientWhenUrlIsSet`, `testDefaultCreateWithAuthorizationServerOffIsRefusedNamingTheProperty`; then `testEnablingSignInRegistersClientWithCallbackOfInstanceUrl`, `testLocalhostCallbackOnlyForDevelopment`, `testEnablingWithoutAuthorizationServerIsRefused`, `testSuspendRevokesGrants`, `testUserWithoutAppUseGetsAccessDenied`, `testUserOfPrivateInstanceWithoutGrantGetsAccessDenied` (Review Focus 3). FAIL, implement.
- [ ] **Verify** module checks, then `./gradlew :server:apps:server-app:testIntegration --tests '*AppSignInIntTest'`.
- [ ] **Verify the deployment defaults.** `helm template kubernetes/helm/bytechef` renders no `BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED` and no `BYTECHEF_EDITION` (so that install is EE by the application default, with the server off); with `-f kubernetes/helm/bytechef/values-ee.yaml` the config map carries the setting as `"true"`. `docker compose -f docker-compose.cluster.yml config` shows edition `CE` and no authorization-server variable; adding `-f docker-compose.cluster.ee.yml` shows `EE` and `true`.
- [ ] **Shared default, by test.** `testAuthorizationServerIsOffInSharedDefaults` (listed above) reads `application-bytechef.yml` alone and asserts `enabled` is `false` and `edition` is `EE`; a sibling `testDevProfileTurnsAuthorizationServerOn` reads `application-dev.yml` and asserts `true`; `testProdProfileDoesNotSetAuthorizationServer` asserts `application-prod.yml` has no such key.
- [ ] **Dev boot check.** `./gradlew -p server/apps/server-app bootRun` (profiles `dev,local`, edition EE by default): `GET /.well-known/openid-configuration` and `GET /oauth2/jwks` answer, with issuer `http://127.0.0.1:5173`. With the client dev server running, confirm `/oauth2/authorize` and `/.well-known/openid-configuration` are reachable through `http://127.0.0.1:5173`; if the Vite proxy does not forward them, add the proxy entries in a `XXXX client - ...` commit. Then run `bytechef automation app create --name demo` once, with no sign-in flag: it succeeds, the instance has `userAuthMode` `REQUIRED` and a registered client, and the template app signs a user in.
- [ ] **CE boot check.** A stock boot is EE, and `bootRun` uses the `dev` profile, which now turns the server on, so neither is a CE check. Run the built jar without the `dev` profile and with the edition set: `BYTECHEF_EDITION=CE java -jar server/apps/server-app/build/libs/<server-app jar> --spring.profiles.active=prod` against the dev infrastructure. Expect: the startup log lists no `oauth2AuthorizationServerSecurityFilterChain`, `GET /oauth2/jwks` is not served by the authorization server, and the Apps nav row and `/api/app/v1` are absent. Repeat with `BYTECHEF_EDITION=EE` and nothing else: Apps are present, the authorization server is still off, and creating an instance with the default mode answers `APP_USER_AUTH_UNAVAILABLE` naming the property.
- [ ] **Commits** `XXXX Register an OAuth2 client per app instance and check APP_USE at sign-in`, `XXXX Enable the authorization server in the EE Helm values and compose override`, `XXXX Enable the authorization server in the dev profile`, `XXXX sdk - Note the local sign-in issuer in the template`, generated commit, `XXXX client - Add the sign-in setting to app instances`, regenerate commit.

### Task 4: The user token on `/api/app/v1`

**Files:**
- Modify: `A/automation-app-api/.../security/AppPrincipal.java` (`@Nullable AppUser user`), create `AppUser.java` (`id`, `login`)
- Modify: `.../security/web/app/AppApiKeyAuthenticationProvider.java` and its converter (read `X-ByteChef-User-Token`), `AppApiKeySecurityConfigurer.java` if the header must be allowed through
- Create: `A/automation-app-service/.../security/AppUserTokenVerifier.java` (decode, claims, revocation, the run-as `APP_USE` check with `SecurityUtils.runAs`)
- Modify: `AppExecutionFacadeImpl`, `AppWorkflowExecutionFacadeImpl`, `AppApiExecutionFacadeImpl` (record `userId`)
- Modify: `A/automation-app-public-rest/openapi.yaml` (`GET /context`, `GET /me`, the header as an optional parameter), create `AppContextApiController.java`; `AppExecutionFacade` gains `getContext()`, `getCurrentUser()`
- Tests: `AppUserTokenVerifierTest`, `AppApiKeyFilterChainTest` (extend), `AppContextApiControllerIntTest`, `AppExecutionFacadeIntTest` (extend)

- [ ] **Tests** (Review Focus 4 and 5): `testUserTokenWithoutKeyIs401`, `testTokenOfAnotherInstanceIs401`, `testMcpTokenIs401`, `testExpiredTokenIs401`, `testRevokedTokenIs401`, `testOtherTenantTokenIs401`, `testInvalidTokenInOptionalModeIs401NotAnonymous`, `testRequiredModeWithoutTokenIs401`, `testGrantRevokedBetweenCallsIs403`, `testRoleChangedBetweenCallsIs403`, `testCallRecordsUserId`, `testMeReturnsUserAndScopes`, `testContextReturnsConfigurationAndClientId`. In the MCP module, add `testAppTokenIsRejectedOnMcpEndpoint` to the existing resource-server test beside `TenantAwareJwtAuthenticationFilter`. FAIL, implement, regenerate.
- [ ] **Verify** `./gradlew :server:ee:libs:automation:automation-app:check :server:ee:libs:automation:automation-security-web:automation-security-web-impl:check :server:libs:platform:platform-security-web:platform-security-web-impl:check` and the app `testIntegration`.
- [ ] **Commits** `XXXX Accept a signed user token beside the app instance key`, `XXXX Generate the App public REST sources`.

### Task 5: Phase verification

- [ ] With `bytechef.oauth2.authorization-server.enabled=true`: enable sign-in on a DEVELOPMENT instance; run the flow by hand with a browser and `curl` (authorize, token, then `GET /api/app/v1/me` with key and token); remove the user's grant and repeat the call: 403.
- [ ] Update `.agents/apps.md` ("App-facing authentication"). Commit `XXXX docs - Describe app sign-in in the Apps overview`.

---

## Phase 4: SDK, template, CLI and skill

### Task 1: SDK core

**Files:**
- Regenerate: `L/generated/openapi.ts` (`npm run generate:api`)
- Create: `L/core/auth.ts`, `L/core/auth.test.ts`, `L/core/context.ts`, `L/core/context.test.ts`, `L/core/pkce.ts`, `L/core/pkce.test.ts`
- Modify: `L/core/config.ts` (`userToken` option), `L/core/request.ts` (send `X-ByteChef-User-Token`), `L/core/client.ts`, `L/core/errors.ts`, `L/core/types.ts`, `L/index.ts`

- [ ] **Tests** (`mockFetch`): `createAuthorizationRequest builds a PKCE S256 URL from /context`, `exchangeCode posts the verifier and returns tokens`, `refresh rotates the refresh token`, `getCurrentUser throws APP_USER_REQUIRED without a token`, `requests carry the user token header only when set`, `context.get is cached per client`, `auth refuses to run in a browser`. `npm test`, FAIL, implement, `npm run check`.
- [ ] **Commit** `XXXX sdk - Add sign-in and context to @bytechef/app`.

### Task 2: TanStack Start adapter

**Files:**
- Create: `L/tanstack/session.ts` (seal and unseal the cookie with `BYTECHEF_SESSION_SECRET`), `L/tanstack/authMiddleware.ts`, `L/tanstack/authHandlers.ts` (`loginHandler`, `callbackHandler`, `logoutHandler`), tests beside each
- Modify: `L/tanstack-start.ts` (exports), `sdks/frontend/automation/app/test-app` (wire the three routes), `test-app/scripts/check-bundle.mjs`

- [ ] **Tests:** state mismatch is refused; a tampered cookie is treated as no session; an expired access token is refreshed once; `REQUIRED` mode redirects to login, `OPTIONAL` does not; cookie flags are HttpOnly, SameSite=Lax and Secure outside localhost; no `createServerFn(` in `tanstack-start.js` (the existing bundle check).
- [ ] **Verify** `npm run check` and `npm run check:test-app`. **Commit** `XXXX sdk - Add the TanStack Start session middleware and sign-in handlers`.

### Task 3: Template, scaffolder, CLI, skill

**Files:**
- Modify: `T/src/routes/__root.tsx`, create `T/src/routes/auth/login.tsx`, `callback.tsx`, `logout.tsx`; `T/src/server/bytechef.ts`; `T/AGENTS.md` (rule 8, a "Who is signed in" recipe), `T/README.md`, `T/scripts/check-bundle.mjs` (fail on `BYTECHEF_SESSION_SECRET` name or value in the client), `T/scripts/bytechef-status.mjs` (report `userAuthMode`)
- Modify: `sdks/frontend/automation/app/create/` scaffolder (generate `BYTECHEF_SESSION_SECRET` into `.env.local`), `scripts/mock-bytechef.mjs` and `scripts/check-template.mjs` (mock `/context`, `/me`, authorize and token)
- Modify: `cli/commands/automation/.../AutomationAppCommand.java` (`status` shows instances and sign-in), create `AutomationAppInstanceCommand.java` (`automation app instance create|list`; `create` is DEVELOPMENT only and refused before any request elsewhere, `list` works in every environment); `automation app create` and the scaffolder now always produce an app that signs users in, with `--user-auth-mode NONE` as the explicit opt-out; tests `AutomationAppInstanceCommandTest` (`testCreateOutsideDevelopmentIsRefusedLocally`, `testListWorksInProduction`)
- Modify: `claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md`; regenerate `mcp-instructions.md` (`./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions`)

- [ ] **Verify** `npm run check:template` (Linux or macOS, needs registry access), `./gradlew :cli:commands:automation:check`, and in `server-app` `AppTemplateCliCommandDriftTest`, `PluginSkillTagDriftIntTest`, `McpInstructionsStalenessTest`.
- [ ] **Commits** `XXXX sdk - Sign users in to apps built from the template`, `XXXX cli - Add the app instance commands`, `XXXX docs - Teach the app builder skill about sign-in and instances`.

---

## Phase 5: Versions and recorded deployments

### Task 1: Schema

- [ ] Create `.../changelog/automation/app/20261116000001_automation_app_deployment.xml`: `app_version`, `app_deployment` as in spec "Design 5"; FK from `app_instance.current_app_deployment_id` to `app_deployment` (`onDelete=SET NULL`).
- [ ] **Commit** `XXXX Upgrade - Add app versions and app deployments`.

### Task 2: Domain, services, facade

**Files:**
- Create: `A/automation-app-api/.../domain/AppVersion.java`, `AppDeployment.java`, `AppDeploymentStatus.java`, `AppDeploymentProviderType.java`; `.../service/AppVersionService.java`, `AppDeploymentService.java`; `.../facade/AppDeploymentFacade.java`; DTOs
- Create: `A/automation-app-service/.../repository/AppVersionRepository.java`, `AppDeploymentRepository.java`, service impls, `.../facade/AppDeploymentFacadeImpl.java`
- Modify: `AppAuditEvent.java`, `AppErrorType.java`
- Tests: `AppVersionServiceIntTest`, `AppDeploymentServiceIntTest`, `AppDeploymentFacadeTest`, `AppDeploymentFacadeAuthorizationTest`

**Guards:** list `hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')`; `createVersion` `hasPermission(#appId, 'App', 'APP_DEPLOY')`; `recordDeployment` `hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')`.

- [ ] **Tests.** `testRecordCreatesVersionWhenNameIsNew`, `testRecordSetsCurrentDeployment`, `testTerminalDeploymentCannotBeUpdated` (Review Focus 6), `testEditorMayRecordAndViewerMayNot`, `testRecordInProductionNeedsProductionRole`. FAIL, implement, verify module `check` and `testIntegration`.
- [ ] **Commit** `XXXX Add app versions and recorded app deployments`.

### Task 3: Surfaces

**Files:** `app.graphqls` and a new `AppDeploymentGraphQlController.java`; `openapi-management.yaml` (`/apps/{id}/versions`, `/app-instances/{id}/deployments`) and `AppDeploymentApiController.java`; CLI client regeneration; create `cli/commands/automation/.../AutomationAppDeploymentCommand.java` (`automation app deployment record|list`, `automation app version create|list`; allowed in every environment, the only CLI writes that are; `deployment deploy` is added in Phase 6); client `client/src/pages/automation/apps/components/AppDeploymentsTab.tsx` and the instance header.
**Tests:** `AppDeploymentGraphQlControllerTest`, `AppDeploymentApiControllerIntTest` (including `testRecordDeploymentAndCreateVersionAreAllowedInProduction`), `AutomationAppDeploymentCommandTest`, `AppDeploymentsTab.test.tsx`.

- [ ] Tests, FAIL, implement, regenerate all three clients, verify module checks, `./gradlew :cli:commands:automation:check`, `npx vitest run src/pages/automation/apps`.
- [ ] **Commits** `XXXX Expose app versions and deployments on GraphQL and the public REST API`, `XXXX Generate the App public REST sources`, `XXXX cli - Add the app deployment and version commands`, `XXXX Generate the CLI app client`, `XXXX client - Show app deployment history`, `XXXX client - Regenerate the GraphQL client`.

---

## Phase 6: Provider SPI and Vercel

### Task 1: SPI and deploy

**Files:**
- Create: `A/automation-app-api/.../deployment/AppDeploymentProvider.java`, `ProviderDeploymentRequest.java`, `ProviderDeployment.java`, `ProviderDeploymentRef.java`
- Modify: `AppDeploymentFacade(Impl)` (`deploy`, `rollback`, `cancel`; refuses a second non-terminal deployment), `AppInstanceFacade(Impl)` (`updateAppInstanceDeploymentSettings`: `APP_MANAGE` and `CONNECTION_EDIT` on the connection, the same rule as `bindConnection`)
- Create: `A/automation-app-service/.../deployment/AppDeploymentStatusJob.java`; modify `server/libs/config/app-config/.../ApplicationProperties.java` (`App.Deployment{pollCron, timeout}`), `application-bytechef.yml`
- Create: `A/automation-app-service/.../security/AppDeploymentConnectionUsageChecker.java`
- Tests: `AppDeploymentFacadeTest` (fake provider), `AppDeploymentStatusJobTest`, `AppDeploymentConnectionUsageCheckerIntTest`, `ApplicationPropertiesTest`

- [ ] **Tests.** `testDeployWritesQueuedRowAndCallsProvider`, `testSelfHostedInstanceAnswersProviderUnavailable`, `testSecondDeployWhileBuildingIsRefused`, `testJobMovesBuildingToReadyAndSetsCurrent`, `testJobMarksFailedAfterDeadline`, `testJobSkipsFailingTenant`, `testRollbackCreatesNewRowForEarlierVersion`, `testDeploymentConnectionCannotBeDeleted`. FAIL, implement (provider calls from the job go on `appExecutor`), verify module `check`, `testIntegration`, `./gradlew :server:libs:config:app-config:test`.
- [ ] **Commit** `XXXX Add the app deployment provider SPI and provider-driven deployments`.

### Task 2: The `vercel` component

**Files:**
- Create: `server/libs/modules/components/vercel/` (`VercelComponentHandler` with `@AutoService(ComponentHandler.class)`, connection, actions `listProjects`, `createDeployment`, `getDeployment`, `cancelDeployment`, `src/main/resources/README.md`), add to `settings.gradle.kts` and the `server-app` dependencies
- Tests: `VercelComponentHandlerTest` (generates `src/test/resources/definition/vercel_v1.json`), one unit test per action with a mocked `Context.Http`

- [ ] Check each endpoint and field against Vercel's current REST API documentation before writing the action (the spec did not verify them). Implement, run `./gradlew :server:libs:modules:components:vercel:check`, then `./gradlew generateDocumentation`.
- [ ] **Commits** `XXXX Add the Vercel component`, `XXXX docs - Add the Vercel component documentation`.

### Task 3: Vercel provider

**Files:**
- Create module `A/automation-app-deployment/automation-app-deployment-vercel/` (`build.gradle.kts` depending on `automation-app-api` and `platform-component-api`; `VercelAppDeploymentProvider.java`), register in `settings.gradle.kts` and in `server-app`'s dependencies
- Tests: `VercelAppDeploymentProviderTest` (mocked `ActionDefinitionFacade`)

- [ ] **Tests.** `testDeployRunsCreateDeploymentOnTheInstanceConnection`, `testVercelStatesMapToDeploymentStatus`, `testProductionInstanceTargetsProduction`, `testActionFailureBecomesFailedDeploymentWithMessageOnly`. FAIL, implement, verify the module's `check`.
- [ ] Verify by hand against a real Vercel project in a DEVELOPMENT instance: deploy, watch the row go `QUEUED` to `READY`, roll back.
- [ ] **Commit** `XXXX Add the Vercel app deployment provider`.

### Task 4: UI

- [ ] CLI: `automation app deployment deploy --version <name>` in `AutomationAppDeploymentCommand.java`, allowed in every environment, with `AutomationAppDeploymentCommandTest.testDeployWorksInProduction` and the REST IntTest `testDeployIsAllowedInProduction`. **Commit** `XXXX cli - Trigger app deployments`.
- [ ] `AppInstanceDialog.tsx` gains the deployment connection and Vercel settings; `AppDeploymentsTab.tsx` gains Deploy, Roll back and Cancel for holders of `APP_DEPLOY`. Tests in `tests/`. **Commits** `XXXX client - Deploy and roll back app instances`, regenerate commit.

---

## Phase 7: App Portal

### Task 1: Schema

- [ ] Create `.../changelog/automation/app/20261116000002_automation_app_portal.xml`: `app_favorite`, `app_instance_usage` (spec "Design 6"). **Commit** `XXXX Upgrade - Add app favorites and app instance usage`.

### Task 2: Portal facade

**Files:**
- Create: `A/automation-app-api/.../domain/AppFavorite.java`, `AppInstanceUsage.java`, `.../service/AppFavoriteService.java`, `AppInstanceUsageService.java`, `.../facade/AppPortalFacade.java`, `.../dto/PortalAppDTO.java`, `PortalAppInstanceDTO.java`; impls and repositories in `-service`
- Modify: `AppWorkspaceUserRemovalListener.java` (delete favorites and usage), `AppExecutionFacadeImpl.getCurrentUser` (throttled usage touch)
- Tests: `AppPortalFacadeTest`, `AppPortalFacadeIntTest`, `AppFavoriteServiceIntTest`, `AppInstanceUsageServiceIntTest`

- [ ] **Tests** (the filter matrix, Review Focus 3): not discoverable is hidden; `DRAFT`, `SUSPENDED`, `ARCHIVED` are hidden; an app with no usable instance is hidden; a PRIVATE instance appears only with a grant; an instance of another environment is hidden; a member of two workspaces sees instances of both under one card, including an instance their workspace hosts of an app owned by a workspace they are not in; a user without `APP_USE` sees nothing; favorite of an app outside the caller's portal is refused; launch count increments and `lastUsedDate` moves. FAIL, implement, verify module `check` and `testIntegration`.
- [ ] **Commit** `XXXX Add the App Portal listing, favorites and usage`.

### Task 3: GraphQL

- [ ] `app.graphqls` (`portalApps`, `setAppFavorite`, `recordAppLaunch`), `A/automation-app-graphql/.../web/graphql/AppPortalGraphQlController.java`, `AppPortalGraphQlControllerTest`. **Commit** `XXXX Expose the App Portal on GraphQL`.

### Task 4: Client

**Files:**
- Create: `client/src/pages/automation/portal/Portal.tsx`, `components/PortalAppCard.tsx`, `PortalInstanceSelect.tsx`, `PortalFilters.tsx`, `hooks/usePortal.ts`, `tests/*.test.tsx`; operations `client/src/graphql/automation/app/portalApps.graphql`, `setAppFavorite.graphql`, `recordAppLaunch.graphql`
- Modify: `client/src/routes.tsx` (route `portal`, wrapped like `apps`), `client/src/App.tsx` (nav row and the EE test), `client/src/pages/automation/apps/components/AppDialog.tsx` (category, icon upload against the Phase 2 Task 5 endpoint, "Show in App Portal")

- [ ] **Tests:** search filters by name and description; category filter; Favorites and Recently used sections; one instance launches directly, several open the selector; launch calls `recordAppLaunch` and opens the URL in a new tab; empty state; the nav row is absent in CE. `npx vitest run src/pages/automation/portal`, FAIL, implement, `npx graphql-codegen`, PASS.
- [ ] **Commits** `XXXX client - Add the App Portal`, `XXXX client - Regenerate the GraphQL client`.

### Task 5: Scenario B by hand

- [ ] As an admin: mark an app discoverable and activate it. As a VIEWER member: open `/automation/portal`, see the app, favorite it, launch it, land in the app signed in without a login prompt. Narrow the instance to PRIVATE without a grant: the card disappears and the app's next call answers 403.

---

## Phase 8: Audit, metrics and documentation

- [ ] **Audit.** One test per new `AppAuditEvent` in the facade tests that owns it; `AppAuditPublisherTest` unchanged. Confirm no event carries a key, a token or configuration values.
- [ ] **Metrics.** `bytechef_app_deployment` in `AppDeploymentFacadeImpl` and `bytechef_app_user_token_rejected` in `AppUserTokenVerifier`, both through `ObjectProvider<MeterRegistry>`; tests with a `SimpleMeterRegistry` and with no registry. **Commit** `XXXX Count app deployments and rejected app user tokens`.
- [ ] **Activity tab.** Add the user column and an instance filter (`AppActivityTab.tsx`, the `workspaceToolInvocationLogs`-backed query's `appInstanceId` argument in the external invocation log GraphQL module). **Commits** server, client, regenerate.
- [ ] **Docs.** Final pass over `.agents/apps.md`; the "Apps (EE)" paragraph of `CLAUDE.md`; the library and template READMEs. **Commit** `XXXX docs - Update the Apps overview for instances, sign-in, deployments and the portal`.
- [ ] **Full verification before any push:** `./gradlew spotlessApply`, `./gradlew check`, `./gradlew testIntegration` for the touched modules, `cd client && npm run format && npm run check`, `npm run check` and `npm run check:template` in `sdks/frontend/automation/app`. If Gradle fails in code nobody changed, `./gradlew --stop` and re-run before investigating.

## Decisions folded into this plan

The user answered the spec's nine open questions on 2026-11-16 and added decision 10 afterwards (spec, "Decisions on the open questions"). Where each lands:

| Decision | Tasks |
|---|---|
| 1. Instances in other workspaces, two `APP_MANAGE`s, parent app read-only | Phase 1 Tasks 4, 5, 7; Phase 7 Task 2 |
| 2. `DRAFT` restricts the portal only | Phase 1 Task 3; Phase 7 Task 2 |
| 3. Organization-wide use deferred | No task. `AppInstanceVisibilityPolicy` supports PRIVATE and WORKSPACE only |
| 4. In-app roles deferred | No task. `/me` returns the workspace role name (Phase 3 Task 4) |
| 5. CLI outside DEVELOPMENT is read-only plus deploy, enforced on the server | Global Constraints; Phase 1 Tasks 5 and 6; Phase 4 Task 3; Phase 5 Task 3; Phase 6 Task 4 |
| 6. External employee-portal API deferred | No task. The portal is GraphQL only (Phase 7 Task 3) |
| 7. `user_auth_mode` defaults to `REQUIRED`, existing rows `NONE` | Phase 1 Task 1 (column default `NONE`); Phase 3 Task 3 |
| 8. Authorization server on by default in EE deployments; shared yml stays `false` | Phase 3 Task 3 (`values-ee.yaml`, `docker-compose.cluster.ee.yml`, shared-default test, CE boot check) |
| 9. Icon in file storage | Phase 1 Task 1 (column); Phase 2 Task 5; Phase 7 Task 4 |
| 10. The dev profile turns the authorization server on | Phase 3 Task 3 (`application-dev.yml`, dev boot check with one-step `automation app create`) |

No task in the earlier draft served only a deferred item, so none was removed; the draft's closing "build the conservative side until answered" note is gone because nothing waits on an answer any more.

One question remains open in the spec ("Remaining question"): whether the chart's own `kubernetes/helm/bytechef/values.yaml` should carry the authorization-server setting. Until answered, Phase 3 Task 3 leaves `values.yaml` unchanged.
