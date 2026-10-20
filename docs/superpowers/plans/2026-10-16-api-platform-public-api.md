# API Platform — Public API, Endpoint Connections and In-Place Version Update — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make API collections and endpoints fully manageable outside the UI — a public API-key REST API and CLI, endpoint connections/inputs through that API, and an in-place move of a collection to a newer project version — so the Apps build loop (plans 2 and 3) can drive API Platform.

**Architecture:** Every operation goes through `ApiCollectionFacade` (authorization lives there). Endpoint connections reuse `ProjectDeploymentFacade.updateProjectDeploymentWorkflow`, the path the UI's `ProjectDeploymentEditWorkflowDialog` uses. The version move reuses `ProjectDeploymentFacade.updateProjectDeployment(ProjectDeployment, List<ProjectDeploymentWorkflow>, List<Tag>)`, whose `checkProjectDeploymentWorkflows` already carries connections, inputs and enabled state by workflow uuid. The public API is a second OpenAPI spec in `automation-api-platform-configuration-rest`, mirroring the Apps public API (`openapi-management.yaml`).

**Tech Stack:** Java 25, Spring Boot 4 (WebMVC, method security), OpenAPI generator (spring, interfaceOnly), Spring Shell CLI, React 19 + TanStack Query, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-16-apps-api-platform-design.md` (sections "API Platform: endpoint connections…", "…moving a collection to a new project version", "…public API for collections and endpoints").

**Plan series:** 1 of 3. Plan 2 (`2026-10-16-apps-api-platform-bindings.md`) and plan 3 (`2026-10-16-apps-build-loop-api-platform.md`) consume the facade methods this plan adds.

## Global Constraints

- Work happens on a branch cut from the `0_732` tip **after** the user's pending environment-promotion commit lands; re-read `ApiCollectionPromotionHandler` and `ProjectDeploymentPromoter` from that tip before Task 4.
- EE files: ByteChef Enterprise license header and `@version ee` Javadoc tag on every new class under `server/ee/`.
- Facades own authorization; controllers own none; facades never use repositories (services only).
- Every public `ApiCollectionFacade` method carries `@PreAuthorize`, pinned in `ApiCollectionFacadeAuthorizationTest` (its reflection check at :131 fails otherwise).
- "Public API" is the name in code comments, OpenAPI titles and docs — never "management API". The spec file name `openapi-public.yaml` follows that.
- Public API: base path `/api/automation/v1`, `bearerAuth`, optional `X-Environment` header (default PRODUCTION), `Error{code,message}`; a collection or endpoint outside the request's environment answers 404.
- No Liquibase changes (endpoint connections live in `project_deployment_workflow_connection`).
- Java style from `CLAUDE.md`: blank line before control statements and after variable modification, no chained calls outside the allowed DSLs, descriptive names, camelCase test names, no `TODO:`.
- Commits: subject only (no body, no trailers), run `./gradlew spotlessApply` first, stage by path. Subjects listed per task; they form a new group placed before the Apps group (Plan 3, Task 6 reorders).

## Review Focus

1. **Endpoint delete/get bypassing authorization** — today `ApiCollectionEndpointApiController:66` calls `apiCollectionEndpointService.delete(id)` directly; after Task 1 a user without `API_PLATFORM_EDIT` on the collection gets 403. Test in Task 1.
2. **A connection from another environment wired to an endpoint** — `updateProjectDeploymentWorkflow` skips `validateDeploymentConnectionEnvironments`; a PRODUCTION connection on a DEVELOPMENT collection must be refused with 400. Test in Task 2.
3. **Version move to a version that dropped an endpoint's workflow** — without `removeMissingEndpoints=true` the move refuses and nothing changes (deployment version, endpoint rows). Test in Task 4.
4. **Version move to the same or an older/unpublished version** — refused with 400, nothing changes. Test in Task 4.
5. **Public API call with `X-Environment: DEVELOPMENT` on a PRODUCTION collection id** — 404, not 403 and not the collection. Test in Task 5.

---

### Task 1: Gate endpoint get, delete and enable on the facade

**Files:**
- Modify: `server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-api/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/facade/ApiCollectionFacade.java`
- Modify: `.../automation-api-platform-configuration-service/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/facade/ApiCollectionFacadeImpl.java`
- Modify: `.../automation-api-platform-configuration-rest/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/web/rest/ApiCollectionEndpointApiController.java` (get :~50, delete :66)
- Test: `.../automation-api-platform-configuration-service/src/test/java/com/bytechef/ee/automation/apiplatform/configuration/facade/ApiCollectionFacadeAuthorizationTest.java`, `ApiCollectionFacadeImplTest.java` (create if absent; Mockito)

**Interfaces:**
- Produces:
  - `ApiCollectionEndpointDTO getApiCollectionEndpoint(long id)` — `hasPermission(#id, 'ApiCollectionEndpoint', 'API_PLATFORM_VIEW')`, resolved through the new `ApiCollectionEndpoint` resource-type resolvers below.
  - `void deleteApiCollectionEndpoint(long id)` — `hasPermission(#id, 'ApiCollectionEndpoint', 'API_PLATFORM_EDIT')`.
  - `ApiCollectionEndpointDTO enableApiCollectionEndpoint(long id, boolean enable)` — same gate; calls `projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enable)`.
  - New `@Component`s in `.../configuration-service/.../permission/`: `ApiCollectionEndpointOwnershipResolver implements ResourceOwnershipResolver` (`resourceType()` = `"ApiCollectionEndpoint"`, owner = the collection's owner via `ApiCollectionOwnershipResolver` logic) and `ApiCollectionEndpointEnvironmentResolver implements ResourceEnvironmentResolver` (environment of the collection's deployment). Copy the shape of `ApiCollectionOwnershipResolver` / `ApiCollectionEnvironmentResolver`.

- [ ] **Step 1: Write failing authorization tests** in `ApiCollectionFacadeAuthorizationTest`: `testGetApiCollectionEndpointRequiresView`, `testDeleteApiCollectionEndpointRequiresEdit`, `testEnableApiCollectionEndpointRequiresEdit` — each asserts `AccessDeniedException` for a user with no scope on the collection and success for one holding the scope, using the test's existing fixtures. Add the three methods to the expression map the reflection test checks.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-service:test --tests '*ApiCollectionFacadeAuthorizationTest'` — Expected: FAIL (methods do not exist).
- [ ] **Step 3: Implement** the three facade methods and two resolvers; `deleteApiCollectionEndpoint` publishes `ApiCollectionAuditEvent.API_COLLECTION_ENDPOINT_DELETED` (add the constant) with `{name, path, httpMethod}`.
- [ ] **Step 4: Switch `ApiCollectionEndpointApiController`** get and delete to the facade; the controller no longer injects `ApiCollectionEndpointService`.
- [ ] **Step 5: Run** the module's `test` task — Expected: PASS.
- [ ] **Step 6: Commit** — `--- Gate API collection endpoint reads, deletes and enabling on the facade`

### Task 2: Endpoint connections and inputs through the facade

**Files:**
- Modify: `ApiCollectionFacade.java`, `ApiCollectionFacadeImpl.java`
- Modify: `.../configuration-api/.../exception/ApiCollectionErrorType.java` (add `CONNECTION_ENVIRONMENT_MISMATCH`)
- Test: `ApiCollectionFacadeImplTest.java`, `ApiCollectionFacadeAuthorizationTest.java`

**Interfaces:**
- Consumes: `ProjectDeploymentFacade.updateProjectDeploymentWorkflow(ProjectDeploymentWorkflow)` (`ProjectDeploymentFacadeImpl:961`, gated `DEPLOYMENT_EDIT` on the deployment), `ProjectDeploymentWorkflowService.getProjectDeploymentWorkflow(long id)`, `ConnectionService.getConnections(List<Long>)` (or the existing lookup used by `validateDeploymentConnectionEnvironments` at `ProjectDeploymentFacadeImpl:2085`).
- Produces:
  - `ApiCollectionEndpointDTO updateApiCollectionEndpointWorkflow(long id, List<ProjectDeploymentWorkflowConnection> connections, Map<String, ?> inputs)` — `hasPermission(#id, 'ApiCollectionEndpoint', 'API_PLATFORM_EDIT')`.
  - `ApiCollectionEndpointDTO` gains `List<ProjectDeploymentWorkflowConnection> connections` and `Map<String, ?> inputs` (read from the endpoint's `ProjectDeploymentWorkflow`); existing constructors default them to empty.

- [ ] **Step 1: Write failing tests**: `testUpdateApiCollectionEndpointWorkflowKeepsEnabledAndVersion` (captures the `ProjectDeploymentWorkflow` passed to `updateProjectDeploymentWorkflow`; asserts connections/inputs replaced, `enabled` and `version` equal the persisted row's); `testUpdateApiCollectionEndpointWorkflowRefusesForeignEnvironmentConnection` (connection environment PRODUCTION, collection DEVELOPMENT → `ConfigurationException` with `CONNECTION_ENVIRONMENT_MISMATCH`, no update call); `testGetApiCollectionEndpointReturnsConnectionsAndInputs`. Authorization test `testUpdateApiCollectionEndpointWorkflowRequiresEdit`.
- [ ] **Step 2: Run** the service module tests — Expected: FAIL.
- [ ] **Step 3: Implement** — load the endpoint's `ProjectDeploymentWorkflow`, check each connection's environment equals the collection's, copy the row, set connections and inputs, call `projectDeploymentFacade.updateProjectDeploymentWorkflow`. Map `CONNECTION_ENVIRONMENT_MISMATCH` to 400 wherever `ApiCollectionErrorType` is translated.
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Commit** — `- Set API collection endpoint connections and inputs through the facade`

### Task 3: Endpoint list per collection on the facade

**Files:** `ApiCollectionFacade.java`, `ApiCollectionFacadeImpl.java`, tests as above.

**Interfaces:**
- Produces: `List<ApiCollectionEndpointDTO> getApiCollectionEndpoints(long apiCollectionId)` — `hasPermission(#apiCollectionId, 'ApiCollection', 'API_PLATFORM_VIEW')`; ordered by `path`, then `httpMethod`.

- [ ] **Step 1:** Tests `testGetApiCollectionEndpointsOrdersByPathThenMethod` and the authorization entry — run, FAIL.
- [ ] **Step 2:** Implement from `ApiCollectionEndpointService.getApiCollectionEndpoints(apiCollectionId)` plus the PDW lookup used by `getApiCollection`. Run, PASS.
- [ ] **Step 3: Commit** — `- List an API collection's endpoints through the facade`

### Task 4: In-place move of a collection to a newer project version

**Files:**
- Modify: `ApiCollectionFacade.java`, `ApiCollectionFacadeImpl.java`, `ApiCollectionErrorType.java` (add `PROJECT_VERSION_INVALID`, `ENDPOINT_WORKFLOW_REMOVED`)
- Create: `.../configuration-api/.../dto/ApiCollectionVersionUpdateDTO.java`
- Modify (internal REST + UI): `automation-api-platform-configuration-rest/openapi.yaml` (`PUT /api-collections/{id}/project-version`), `ApiCollectionApiController.java`, regenerate (`./gradlew :…:automation-api-platform-configuration-rest:generateOpenAPISpring generateOpenAPITypeScriptFetch`), `client/src/ee/shared/mutations/automation/apiCollections.mutations.ts`, `client/src/ee/pages/automation/api-platform/api-collections/components/ApiCollectionListItemDropDownMenu.tsx`, new `ApiCollectionProjectVersionDialog.tsx` + `components/tests/ApiCollectionProjectVersionDialog.test.tsx`
- Test: `ApiCollectionFacadeImplTest.java`, authorization test

**Interfaces:**
- Consumes: `ProjectDeploymentFacade.updateProjectDeployment(ProjectDeployment, List<ProjectDeploymentWorkflow>, List<Tag>)` (`ProjectDeploymentFacadeImpl:909`, gated `DEPLOYMENT_PUSH` on the project); `ProjectWorkflowService.getProjectWorkflows(projectId, projectVersion)`; `componentConnectionFacade.getWorkflowComponentConnections(workflowId)` for slots.
- Produces:
  - `ApiCollectionVersionUpdateDTO updateApiCollectionProjectVersion(long id, int projectVersion, boolean removeMissingEndpoints)` — `hasPermission(#id, 'ApiCollection', 'API_PLATFORM_EDIT')`.
  - `record ApiCollectionVersionUpdateDTO(ApiCollectionDTO apiCollection, List<String> removedEndpointNames, List<MissingConnection> missingConnections)` with nested `record MissingConnection(String endpointName, String workflowNodeName, String key, String componentName)`.

- [ ] **Step 0:** Re-read `ApiCollectionPromotionHandler.reconcileEndpoints` and `ProjectDeploymentPromoter.sync` on the `0_732` tip; if the pending promotion commit added a reusable in-place helper, use it and adjust Step 3.
- [ ] **Step 1: Write failing tests**:
  - `testUpdateProjectVersionRepointsEndpointsByWorkflowUuid` — collection on v2 with endpoints for uuids A and B; v3 has both; after the call the deployment's `projectVersion` is 3, each endpoint's `projectDeploymentWorkflowId` is the v3 row for its uuid, `collectionVersion` unchanged, connections/inputs/enabled carried (assert on the list passed to `updateProjectDeployment`).
  - `testUpdateProjectVersionRefusesWhenWorkflowRemoved` — v3 lacks uuid B, `removeMissingEndpoints=false` → `ENDPOINT_WORKFLOW_REMOVED` naming the endpoint; `updateProjectDeployment` never called.
  - `testUpdateProjectVersionRemovesEndpointWhenConfirmed` — same with `true` → endpoint B deleted, `removedEndpointNames=[B's name]`.
  - `testUpdateProjectVersionRefusesSameOrOlderVersion` and `testUpdateProjectVersionRefusesUnpublishedVersion` → `PROJECT_VERSION_INVALID`.
  - `testUpdateProjectVersionReportsMissingConnections` — v3 adds a slot on workflow A → reported in `missingConnections`.
  - Authorization: `testUpdateApiCollectionProjectVersionRequiresEdit`.
- [ ] **Step 2: Run** — FAIL.
- [ ] **Step 3: Implement** — validate version (> current, published), compute removed uuids, refuse or delete those endpoints, build the new `ProjectDeploymentWorkflow` list (one per v3 workflow with the persisted row's connections/inputs/enabled for the same uuid), set the deployment's `projectVersion`, call `updateProjectDeployment`, then re-point each remaining endpoint through `updateApiCollectionEndpoint` with its `workflowUuid` (which re-resolves the PDW id), compute missing slots, publish `API_COLLECTION_PROJECT_VERSION_UPDATED` `{from, to}`. One transaction.
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Internal REST + UI** — add the path (body `{projectVersion, removeMissingEndpoints}`, response `ApiCollectionVersionUpdate`), controller method, regenerate both clients; menu item "Update project version" opens `ApiCollectionProjectVersionDialog` (published versions newer than the current, from the existing project versions query; on `ENDPOINT_WORKFLOW_REMOVED` shows the names and a "Remove these endpoints" confirm that resends with `true`; after success lists `missingConnections`). Test `ApiCollectionProjectVersionDialog.test.tsx`: renders newer versions only; resend with `removeMissingEndpoints: true` after confirm.
- [ ] **Step 6: Run** `cd client && npm run check` and the rest module build — PASS.
- [ ] **Step 7: Commit** — `- Move an API collection to a newer project version in place` (server) and `- client - Update an API collection's project version` (client + regenerated TS).

### Task 5: Public API — collections

**Files:**
- Create: `.../automation-api-platform-configuration-rest/openapi-public.yaml`
- Modify: `.../automation-api-platform-configuration-rest/build.gradle.kts` — task `generateOpenAPISpringPublic` (package `com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest`, models `.model`, `modelNameSuffix "Model"`, `apiFilesConstrainedTo("")`/`modelFilesConstrainedTo("")` as in `automation-app-public-rest/build.gradle.kts`), wired into an aggregate `generateOpenAPI`
- Create: `.../src/main/java/com/bytechef/ee/automation/apiplatform/configuration/public_/web/rest/ApiCollectionPublicApiController.java`, `ApiCollectionEnvironmentScope.java`, `EnvironmentModelConverter.java` (copy Apps')
- Modify: `ApiCollectionFacade` — `void requireApiCollectionInEnvironment(long id, Environment environment)` (ungated like `AppFacadeImpl:170`; throws `ConfigurationException(…, API_COLLECTION_NOT_FOUND)` → 404)
- Modify: `server/ee/.../OpenApiConfiguration.java:45` — add the public paths to the automation public group
- Test: `.../src/test/java/.../public_/web/rest/ApiCollectionPublicApiControllerIntTest.java`, `config/ApiCollectionPublicRestTestConfiguration.java`

**Interfaces:**
- Consumes: Task 1–4 facade methods; existing `createApiCollection`, `getApiCollection`, `getApiCollections`, `updateApiCollection`, `deleteApiCollection`.
- Produces (OpenAPI, tag `api-collection`):
  - `GET /workspaces/{workspaceId}/api-collections` (query `projectId?`) → `ApiCollection[]`
  - `POST /workspaces/{workspaceId}/api-collections` body `ApiCollectionInput{projectId, projectVersion, name, contextPath, collectionVersion?=1, description?}` → 201 `ApiCollection`
  - `GET|PUT|DELETE /api-collections/{id}` (PUT body `ApiCollectionUpdate{name?, description?}`)
  - `PUT /api-collections/{id}/project-version` body `{projectVersion, removeMissingEndpoints?=false}` → `ApiCollectionVersionUpdate`
  - Schema `ApiCollection{id, name, description, contextPath, collectionVersion, projectId, projectVersion, environment, enabled, createdDate, lastModifiedDate}`.

- [ ] **Step 1: Write the IntTest** (`@WebMvcTest(ApiCollectionPublicApiController.class)`, `@ContextConfiguration(ApiCollectionPublicRestTestConfiguration.class)`, `@TestPropertySource("bytechef.edition=ee")`, `@WithMockUser`, `@MockitoBean ApiCollectionFacade`, `@MockitoBean(answers = CALLS_REAL_METHODS) EnvironmentService`): `testCreateApiCollectionReturns201`, `testGetApiCollectionOutsideEnvironmentReturns404` (facade `requireApiCollectionInEnvironment` throws → 404 `API_COLLECTION_NOT_FOUND`), `testUpdateProjectVersionReturnsRemovedAndMissing`, `testDeleteApiCollectionReturns204`.
- [ ] **Step 2: Run** — FAIL (no spec/controller).
- [ ] **Step 3: Write the spec, generate** (`./gradlew :…:automation-api-platform-configuration-rest:generateOpenAPI`), **implement the controller** (`@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")`, `@ConditionalOnEEVersion @ConditionalOnCoordinator`, every id-taking method first calls `ApiCollectionEnvironmentScope.requireInEnvironment`), error handlers mapping `ApiCollectionErrorType` to 400/404/409 like `AbstractAppApiController.translate`.
- [ ] **Step 4: Run** — PASS. Confirm `AutomationApiKeySecurityConfigurer` (`^/api/automation/v[0-9]+/.+`) already covers the paths (no change expected).
- [ ] **Step 5: Commit** — `- Expose API collections through the public API`

### Task 6: Public API — endpoints

**Files:** `openapi-public.yaml`, new `ApiCollectionEndpointPublicApiController.java`, IntTest `ApiCollectionEndpointPublicApiControllerIntTest.java`.

**Interfaces:**
- Produces (tag `api-collection-endpoint`):
  - `GET|POST /api-collections/{id}/endpoints` (POST body `ApiCollectionEndpointInput{name, httpMethod, path, workflowUuid, connections?: EndpointConnection[], inputs?: object}`)
  - `GET|PUT|DELETE /api-collection-endpoints/{id}` (PUT body `ApiCollectionEndpointUpdate{name?, httpMethod?, path?, workflowUuid?, connections?, inputs?}`)
  - `PUT /api-collection-endpoints/{id}/enabled` body `{enabled}`
  - `EndpointConnection{workflowNodeName, key, connectionId}`; `ApiCollectionEndpoint{id, apiCollectionId, name, httpMethod, path, workflowUuid, enabled, connections, inputs}`.
- Create with `connections`/`inputs` = `createApiCollectionEndpoint` then `updateApiCollectionEndpointWorkflow`, in one facade call: add `ApiCollectionEndpointDTO createApiCollectionEndpoint(ApiCollectionEndpointDTO, List<ProjectDeploymentWorkflowConnection>, Map<String, ?>)` overload (gate as the existing create).

- [ ] **Step 1: IntTest**: `testCreateEndpointWithConnectionsPassesSlots`, `testUpdateEndpointConnections`, `testEnableEndpoint`, `testEndpointOfCollectionOutsideEnvironmentReturns404`, `testDeleteEndpointReturns204` — run, FAIL.
- [ ] **Step 2: Implement** spec, generate, controller, facade overload (+ authorization test entry, + `ApiCollectionFacadeImplTest.testCreateEndpointWithConnectionsAppliesThem`). Run, PASS.
- [ ] **Step 3: Commit** — `- Expose API collection endpoints, their connections and inputs through the public API`

### Task 7: CLI — `automation api-collection` commands

**Files:**
- Create: `cli/clients/automation-api-platform/build.gradle.kts` (copy `cli/clients/automation-app/build.gradle.kts`; packages `com.bytechef.cli.client.automationapiplatform.{api,model}`), `cli/clients/automation-api-platform/openapi.yaml` (copy of `openapi-public.yaml` with `Environment` inlined, per `cli/README.md:142-145`), generated sources (committed)
- Modify: `settings.gradle.kts` (:31-32 area), `cli/commands/automation/build.gradle.kts` (:5-6 area), `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationClientFactory.java` (add `apiCollectionApi(CliConfig)`, `apiCollectionEndpointApi(CliConfig)`), `cli/README.md`
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationApiCollectionCommand.java`
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationApiCollectionCommandTest.java`

**Interfaces:**
- Produces commands (environment via `ProfileResolver` like `AutomationDataTableCommand.java:277`; usable in any environment; `--output json|table`):
  - `automation api-collection list [--project-id]`, `create --project-id --project-version --name --context-path [--collection-version] [--description]`, `get --id`, `update --id [--name] [--description]`, `delete --id`, `update-version --id --project-version [--remove-missing-endpoints]`
  - `automation api-collection endpoint list --collection-id`, `create --collection-id --name --method --path --workflow-uuid [--connection node:key=connectionId]… [--inputs-json]`, `update --id …same optional…`, `delete --id`, `enable --id`, `disable --id`
- Plan 3's template/skill text and `AppTemplateCliCommandDriftTest` / `PluginSkillTagDriftIntTest` rely on these exact command and option names.

- [ ] **Step 1: Test** with `RoutedStubApi` (pattern of `AutomationAppCommandTest`): `testCreateSendsBody`, `testEndpointCreateParsesConnectionOptions` (`--connection http_1:connection=7` → `{workflowNodeName:"http_1", key:"connection", connectionId:7}`), `testUpdateVersionPrintsRemovedAndMissing`, `testMalformedConnectionOptionFails` — run, FAIL.
- [ ] **Step 2: Generate** `./gradlew :cli:clients:automation-api-platform:generateClient`; **implement** commands. Run tests, PASS; `./gradlew :cli:commands:automation:check`.
- [ ] **Step 3: Commit** — `- cli - Add the automation api-collection commands`

### Task 8: Verify

- [ ] `./gradlew spotlessApply` then `./gradlew :server:ee:libs:automation:automation-api-platform:check :cli:commands:automation:check :cli:clients:automation-api-platform:check` — PASS.
- [ ] `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test` (ProjectDeploymentFacade unchanged but exercised) — PASS.
- [ ] `cd client && npm run check` — PASS; grep the vitest output for `Unhandled` (exit 1 even when tests pass).
- [ ] Boot `server-app` once (`./gradlew -p server/apps/server-app bootRun` against the dev infra) and `curl -H "Authorization: Bearer <dev automation key>" -H "X-Environment: DEVELOPMENT" localhost:9555/api/automation/v1/workspaces/1/api-collections` — 200 JSON.
