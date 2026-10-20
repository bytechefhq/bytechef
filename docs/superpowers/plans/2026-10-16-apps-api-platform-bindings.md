# Apps — API Collection Bindings, the App API Path and the SDK — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an app call API Platform endpoints of collections an admin bound to it, with its app key, under `/api/app/v1/api/v{n}/{contextPath}/{path}`, and give `@bytechef/app` a typed way to do it.

**Architecture:** A new `app_api_collection` binding mirrors `app_workflow` (table, service, resolver, facade, GraphQL, public REST, CLI, admin tab). The resolve-and-execute logic of `ApiPlatformHandlerController` moves into an abstract base in the handler module; `/api/o/**` and the new app controller both extend it and differ only in which collections they search and how the environment is chosen. The SDK gets an `api` namespace on the core client plus TanStack server functions.

**Tech Stack:** Java 25, Spring Boot 4 (Data JDBC, WebMVC, GraphQL, method security), Liquibase, OpenAPI generator, Spring Shell, React 19, TypeScript SDK (vitest, openapi-typescript).

**Spec:** `docs/superpowers/specs/2026-10-16-apps-api-platform-design.md` (sections "Binding model", "Admin surface and public API", "App-facing calls", "Design — SDK and template").

**Plan series:** 2 of 3. Needs plan 1 (`2026-10-16-api-platform-public-api.md`) merged into the working branch. Plan 3 consumes `AppApiCollectionFacade` and the CLI commands from here.

## Global Constraints

- Branch: same working branch as plan 1 (cut from the `0_732` tip). Commits here are folded into the Apps group by plan 3, Task 6; give each the subject of the Apps commit it folds into (listed per task) prefixed `fixup! ` so `--autosquash` places it.
- Apps commit subjects to fold into (on `0_732`): schema `- Add the automation app schema and the app id on the tool invocation log`; server `- Add Apps with secret-key authentication, connection bindings and a connection proxy, callable actions, tools and workflows, and an environment-scoped management API`; CLI `- cli - Add the automation app commands`; client `- client - Add the Apps pages and app workflow bindings, and filter tool invocations by app`; SDK `- sdk - Add the @bytechef/app client library, the app template and the create-bytechef-app scaffolder, and check them in CI`; regenerated `- Regenerate the App REST sources, the CLI app client, the GraphQL client and the TypeSafe definition`. Autosquash matches subjects, not SHAs.
- EE header + `@version ee` on new `server/ee/` classes. Facades own authorization; controllers and GraphQL controllers carry no `@PreAuthorize` (`AppWorkflowGraphQlControllerTest` reflection pattern).
- Liquibase: changelog-only change, its own `fixup!` commit, no XML comments; new file in `…/changelog/automation/app/` (picked up by `includeAll` at `master.xml:59`).
- App-facing environment is always `AppPrincipal.environment()`; `X-ENVIRONMENT` is ignored on `/api/app/**`.
- New `AppErrorType` keys continue from 123: `APP_API_COLLECTION_NOT_FOUND`(124, 404), `APP_API_COLLECTION_NOT_BINDABLE`(125, 400), `APP_API_COLLECTION_UNAVAILABLE`(126, 409), `APP_API_ENDPOINT_NOT_FOUND`(127, 404). Add each to `NAMES` and `toAppStatus`; `AppErrorTypeTest` pins them.
- New `ToolExecutionSurface` constant `AUTOMATION_APP_API` is appended last (ordinals pinned by `ToolExecutionEnumOrdinalStabilityTest`; update that test's expected list).
- "Public API" naming in comments and docs (the existing file name `openapi-management.yaml` stays).
- Java and client style rules from `CLAUDE.md` (sort-keys, `I`/`Props` interface suffix, `Icon`-suffixed lucide imports, `twMerge`, hook order).

## Review Focus

1. **App key against a collection bound to a different app of the same workspace** — 404 `APP_API_ENDPOINT_NOT_FOUND`, and the endpoint workflow never runs. Test in Task 6.
2. **Two bound collections whose endpoint patterns overlap** (`/v1/crm/contacts/{id}` and `/v1/crm/contacts/search`) — the same first-match rule `/api/o/**` uses today must apply; the shared base guarantees it. Test in Task 5 (extraction keeps order) and Task 6.
3. **Workflow-chosen 4xx/5xx and headers from `responseToApiRequest`** pass through unchanged on the app path (not turned into an `Error` body). Test in Task 6.
4. **App disabled or key rotated between two calls** — the second call is 401/403 even though the collection is still bound. Test in Task 6 (filter-chain level, existing `AppApiKeyFilterChainTest` style).
5. **Unbinding while the app is mid-request** — no partial state; the next call 404s. Covered by Task 6's unbound test; no extra code.

---

### Task 1: Schema — `app_api_collection`

**Files:**
- Create: `server/ee/libs/automation/automation-app/automation-app-service/src/main/resources/config/liquibase/changelog/automation/app/20261016000001_automation_app_api_collection.xml`

- [ ] **Step 1:** Changeset `id="20261016000001-1" author="Ivica Cardic"` creating `app_api_collection`: `id` BIGINT autoIncrement startWith=1050 PK; `app_id` BIGINT NOT NULL; `api_collection_id` BIGINT NOT NULL; `version` INT NOT NULL default 0; unique `uk_app_api_collection_app_api_collection(app_id, api_collection_id)`; FK `fk_app_api_collection_app` → `app.id` `onDelete=CASCADE`; index `idx_app_api_collection_api_collection(api_collection_id)`. No FK to `api_collection` (spec: unavailable, not blocked).
- [ ] **Step 2:** `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:testIntegration --tests '*AppWorkflowServiceIntTest'` (any existing Apps IntTest that boots Liquibase) — PASS (changelog applies).
- [ ] **Step 3: Commit** — `fixup! - Add the automation app schema and the app id on the tool invocation log`

### Task 2: Domain, repository and service

**Files:**
- Create: `A/automation-app-api/src/main/java/com/bytechef/ee/automation/app/domain/AppApiCollection.java` (A = `server/ee/libs/automation/automation-app`)
- Create: `A/automation-app-api/.../service/AppApiCollectionService.java`
- Create: `A/automation-app-service/.../repository/AppApiCollectionRepository.java`, `.../service/AppApiCollectionServiceImpl.java`
- Test: `A/automation-app-service/src/test/.../service/AppApiCollectionServiceIntTest.java` (copy the setup of the existing `AppWorkflowService` IntTest)

**Interfaces:**
- Produces:
  - `AppApiCollection` — `@Table("app_api_collection")`, `@Column("app_id") AggregateReference<App, Long> appId`, `@Id Long id`, `@Column("api_collection_id") Long apiCollectionId`, `@Version int version`; `getAppId()`/`setAppId(Long)` like `AppWorkflow`.
  - `AppApiCollectionRepository extends ListCrudRepository<AppApiCollection, Long>`: `List<AppApiCollection> findAllByAppIdOrderByApiCollectionId(long appId)`, `Optional<AppApiCollection> findByAppIdAndApiCollectionId(long appId, long apiCollectionId)`, `List<AppApiCollection> findAllByApiCollectionId(long apiCollectionId)`.
  - `AppApiCollectionService`: `AppApiCollection bind(long appId, long apiCollectionId)` (idempotent: returns the existing row), `boolean unbind(long appId, long apiCollectionId)`, `List<AppApiCollection> getBindings(long appId)`, `boolean isBound(long appId, long apiCollectionId)`.

- [ ] **Step 1: IntTest**: `testBindIsIdempotent`, `testUnbindReturnsFalseWhenAbsent`, `testGetBindingsOrdersByCollectionId`, `testDeletingAppCascades` — run, FAIL.
- [ ] **Step 2: Implement** (`@Service @Transactional @ConditionalOnEEVersion`, reads `readOnly=true`). Run, PASS.
- [ ] **Step 3: Commit** — `fixup! - Add Apps with secret-key authentication, …` (full server subject)

### Task 3: Availability resolver

**Files:**
- Create: `A/automation-app-service/.../workflow/AppApiCollectionResolver.java`
- Test: `A/automation-app-service/src/test/.../workflow/AppApiCollectionResolverTest.java`

**Interfaces:**
- Consumes: `ApiCollectionService.fetchApiCollection(long)` / `getApiCollections(...)`, `ProjectDeploymentService.fetchProjectDeployment(long)`, `ProjectService.getProject(long)` (services, not facades).
- Produces:
  - `Optional<ApiCollection> resolve(long workspaceId, Environment environment, long apiCollectionId)` — empty when the collection is missing, its deployment is missing or disabled, in another environment, or its project is in another workspace.
  - `List<ApiCollection> getBindableApiCollections(long workspaceId, Environment environment)` — collections of the workspace and environment whose deployment exists (disabled ones included, so an admin can bind before enabling; `available` reported separately).

- [ ] **Step 1: Tests**: `testResolveReturnsEnabledCollectionInWorkspaceAndEnvironment`, `testResolveEmptyForDisabledDeployment`, `testResolveEmptyForOtherEnvironment`, `testResolveEmptyForOtherWorkspace`, `testResolveEmptyForMissingCollection`, `testBindableIncludesDisabledDeployment` — run, FAIL.
- [ ] **Step 2: Implement.** Run, PASS.
- [ ] **Step 3: Commit** — `fixup!` server subject.

### Task 4: Facade, GraphQL, public REST and admin tab

**Files:**
- Create: `A/automation-app-api/.../facade/AppApiCollectionFacade.java`, `.../dto/AppApiCollectionBindingDTO.java`, `.../dto/AppBindableApiCollectionDTO.java`
- Create: `A/automation-app-service/.../facade/AppApiCollectionFacadeImpl.java`
- Modify: `A/automation-app-service/.../audit/AppAuditEvent.java` (add `APP_API_COLLECTION_BOUND`, `APP_API_COLLECTION_UNBOUND`)
- Modify: `A/automation-app-api/.../exception/AppErrorType.java` (keys 124–127, see Global Constraints)
- Modify: `A/automation-app-graphql/src/main/resources/graphql/app.graphqls`; create `A/automation-app-graphql/.../web/graphql/AppApiCollectionGraphQlController.java`
- Modify: `A/automation-app-public-rest/openapi-management.yaml`; create `.../public_/web/rest/AppApiCollectionBindingApiController.java`; regenerate
- Modify: `A/automation-app-graphql/src/test/.../facade/AppAuthorizationTestConfiguration.java` (`@Import` the new facade impl + resolver)
- Client: `client/src/graphql/automation/app/{appApiCollections,appBindableApiCollections,bindAppApiCollection,unbindAppApiCollection}.graphql`; `client/src/pages/automation/apps/components/{AppApiCollectionsTab,AppApiCollectionBindingDialog}.tsx`; `client/src/pages/automation/apps/App.tsx` (tab `api-collections` between `connections` and `workflows`); tests `client/src/pages/automation/apps/tests/{AppApiCollectionsTab,AppApiCollectionBindingDialog}.test.tsx`; regenerate `npm run codegen`
- Test: `A/automation-app-service/src/test/.../facade/AppApiCollectionFacadeTest.java`, `A/automation-app-graphql/src/test/.../facade/AppApiCollectionFacadeAuthorizationTest.java`, `.../web/graphql/AppApiCollectionGraphQlControllerTest.java`, `A/automation-app-public-rest/src/test/.../AppApiCollectionBindingApiControllerIntTest.java`

**Interfaces:**
- Produces:
  - `AppApiCollectionFacade`:
    - `List<AppApiCollectionBindingDTO> getApiCollectionBindings(long appId)` — `hasPermission(#appId, 'App', 'APP_VIEW')`
    - `List<AppBindableApiCollectionDTO> getBindableApiCollections(long appId)` — `APP_MANAGE`; only collections of projects visible to the caller (`resourceVisibilityResolver.filterVisibleIds("Project", …)` as `AppWorkflowFacadeImpl:200`)
    - `AppApiCollectionBindingDTO bindApiCollection(long appId, long apiCollectionId)` — `APP_MANAGE`; refuses (`APP_API_COLLECTION_NOT_BINDABLE`) a collection outside the app's workspace/environment or of an invisible project; publishes `APP_API_COLLECTION_BOUND {apiCollectionId, name}`
    - `void unbindApiCollection(long appId, long apiCollectionId)` — `APP_MANAGE`; publishes `APP_API_COLLECTION_UNBOUND` only when a row was deleted
  - `record AppApiCollectionBindingDTO(long id, long appId, long apiCollectionId, String name, String contextPath, int collectionVersion, long projectId, String projectName, boolean available)` (unavailable + missing collection → `name` = `"#" + apiCollectionId`, others null/0).
  - `record AppBindableApiCollectionDTO(long apiCollectionId, String name, String contextPath, int collectionVersion, long projectId, String projectName, boolean available)`.
  - GraphQL: `appApiCollections(appId: ID!): [AppApiCollectionBinding!]!`, `appBindableApiCollections(appId: ID!): [AppBindableApiCollection!]!`, `bindAppApiCollection(appId: ID!, apiCollectionId: ID!): AppApiCollectionBinding!`, `unbindAppApiCollection(appId: ID!, apiCollectionId: ID!): Boolean!`.
  - Public REST (tag `app-api-collection-binding`): `GET /apps/{id}/bindable-api-collections`, `GET /apps/{id}/api-collections`, `PUT|DELETE /apps/{id}/api-collections/{apiCollectionId}`; schemas `AppApiCollectionBinding`, `AppBindableApiCollection`; `X-Environment` via `AppEnvironmentScope.requireInEnvironment` first, like `AppWorkflowBindingApiController`.

- [ ] **Step 1: Facade unit tests**: `testBindPublishesAudit`, `testBindRefusesOtherEnvironment`, `testBindRefusesInvisibleProject`, `testUnbindPublishesOnlyWhenDeleted`, `testBindingsReportUnavailable`. **Authorization tests**: each method's gate (copy `AppWorkflowFacadeAuthorizationTest`). Run, FAIL.
- [ ] **Step 2: Implement facade, audit constants, error types** (+ `AppErrorTypeTest` expectations). Run, PASS.
- [ ] **Step 3: GraphQL + public REST tests** (`AppApiCollectionGraphQlControllerTest` incl. no-`@PreAuthorize` reflection check; `AppApiCollectionBindingApiControllerIntTest`: list, bind 200, unbind 204, app outside environment 404). Run, FAIL. Implement + regenerate. Run, PASS.
- [ ] **Step 4: Client tab tests**: lists bindings with an "Unavailable" badge; bind dialog lists bindable collections and calls `bindAppApiCollection`; unbind invalidates `['appApiCollections']`; manage controls hidden when `canManage` is false. Run `npx vitest run src/pages/automation/apps` — FAIL. Implement, codegen, run — PASS.
- [ ] **Step 5: Commit** — server files `fixup!` server subject; client files `fixup! - client - Add the Apps pages …`; regenerated sources (`generated/`, `client/src/shared/middleware/graphql.ts`) `fixup! - Regenerate the App REST sources, …`.

### Task 5: Extract the shared API Platform resolve-and-execute base

**Files:**
- Create: `server/ee/libs/automation/automation-api-platform/automation-api-platform-handler/automation-api-platform-handler-rest/src/main/java/com/bytechef/ee/automation/apiplatform/handler/web/rest/AbstractApiPlatformEndpointController.java`
- Modify: `.../handler/web/rest/ApiPlatformHandlerController.java`
- Test: `.../automation-api-platform-handler-rest/src/test/java/.../web/rest/ApiPlatformHandlerControllerTest.java` (create; Mockito, `MockHttpServletRequest`)

**Interfaces:**
- Produces: `public abstract class AbstractApiPlatformEndpointController extends AbstractWebhookTriggerController` with
  - constructor = today's `ApiPlatformHandlerController` constructor minus nothing (same dependencies)
  - `protected abstract List<ApiCollection> getSearchableApiCollections(Environment environment)`
  - `protected abstract Environment getEnvironment(HttpServletRequest request)`
  - `protected abstract String getBasePath()` (`/api/o` here; `/api/app/v1/api` in Task 6)
  - `protected ResponseEntity<?> doHandle(HttpMethod httpMethod, HttpServletRequest request, HttpServletResponse response)` — today's `doHandle` body (tenant wrap, strip base path, match among `getSearchableApiCollections`, enabled checks, path variables, flags, `doProcessTrigger(...).join()`), with two changes: no match → `ResponseEntity.status(404)` via `protected ResponseEntity<?> notFound()` (overridable), disabled deployment/workflow → `protected ResponseEntity<?> unavailable()` (default 404, preserving `/api/o` behaviour).
- `ApiPlatformHandlerController` keeps its mapping and annotations; `getSearchableApiCollections` = all collections of the environment (today's call), `getEnvironment` = `x-environment` header default PRODUCTION.

- [ ] **Step 1: Tests (characterise today's behaviour first)**: `testMatchesFirstEndpointInCollectionOrder`, `testPathVariablesMergedIntoParameters`, `testDisabledWorkflowReturns404`, `testNoMatchReturns404` (today throws `IllegalArgumentException` — this test pins the new 404), `testEnvironmentDefaultsToProduction`. Run against the current class — all but `testNoMatchReturns404` PASS (prove the characterisation), that one FAILs.
- [ ] **Step 2: Extract** the base; `ApiPlatformHandlerController` becomes the thin subclass. Run, all PASS.
- [ ] **Step 3: Commit** — `--- Share API Platform endpoint resolution and answer an unmatched path with 404` (a standalone API Platform commit, ordered with plan 1's group by plan 3, Task 6).

### Task 6: The app API path

**Files:**
- Create: `A/automation-app-public-rest/src/main/java/com/bytechef/ee/automation/app/public_/web/rest/AppApiEndpointController.java`
- Create: `A/automation-app-api/.../facade/AppApiExecutionFacade.java`, `A/automation-app-service/.../facade/AppApiExecutionFacadeImpl.java`
- Modify: `A/automation-app-public-rest/build.gradle.kts` (add `implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-handler:automation-api-platform-handler-rest"))` and what it needs on the compile path: platform-webhook-rest-api, file-storage-api, app-config, tenant-api)
- Modify: `A/automation-app-public-rest/openapi.yaml` (`GET /api-collections`, schema `AppApiCollection{apiCollectionId, name, contextPath, collectionVersion, available, endpoints: AppApiEndpoint[]}`, `AppApiEndpoint{name, httpMethod, path, requestSchema?, responseSchema?}`; document `/{any}` under `/api/v{n}/…` as a free-form operation `callApiEndpoint` for each method so the SDK types exist)
- Create: `A/automation-app-public-rest/.../AppApiCollectionApiController.java` (the `GET /api-collections` listing)
- Modify: `server/libs/platform/platform-tool-execution/platform-tool-execution-api/.../ToolExecutionSurface.java` (append `AUTOMATION_APP_API`) and `ToolExecutionEnumOrdinalStabilityTest`
- Modify: `A/automation-app-public-rest/src/test/.../config/AppExecutionRestTestConfiguration.java` (`@Import` both new controllers)
- Test: `A/automation-app-public-rest/src/test/.../AppApiEndpointControllerIntTest.java`, `AppApiCollectionApiControllerIntTest.java`; `A/automation-app-service/src/test/.../facade/AppApiExecutionFacadeImplTest.java`; `server/ee/libs/automation/automation-security-web/automation-security-web-impl/src/test/.../app/AppApiKeyFilterChainTest.java` (extend)

**Interfaces:**
- Consumes: Task 3 resolver, Task 2 service, Task 5 base. `AppApiExecutionFacadeImpl` reads collections and endpoints through API Platform services, not `ApiCollectionFacade`: an app-key caller holds no user permissions, so the binding is the authorization.
- Produces:
  - `AppApiExecutionFacade` (app-key callers; reads `AppPrincipal.getCurrent()`, throws `APP_DISABLED` unless enabled, like `AppExecutionFacadeImpl.resolveApp()` :466):
    - `List<ApiCollection> getCallableApiCollections()` — bound collections that resolve as available, in binding order
    - `boolean hasUnavailableBinding()` — any bound collection failing `resolve`
    - `List<AppApiCollectionDTO> getApiCollections()` — listing with endpoints and schemas read from each endpoint workflow's `apiPlatform/v1/newApiRequest` trigger `request`/`response` properties (null when unreadable)
    - `<T> T recordApiCall(String collectionName, String endpointName, Supplier<T> call)` — wraps `toolExecutionRecorder.record` with `ToolExecutionEvent.builder(AUTOMATION_APP_API, …, endpointName).appId(…).workspaceId(…).environment(…)`
  - `AppApiEndpointController extends AbstractApiPlatformEndpointController`, `@RestController @RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1/api/**") @ConditionalOnEEVersion @ConditionalOnCoordinator`; `getSearchableApiCollections` → `appApiExecutionFacade.getCallableApiCollections()`; `getEnvironment` → `AppPrincipal.getCurrent().environment()`; `getBasePath` → `/api/app/v1/api`; `notFound()` → 409 `APP_API_COLLECTION_UNAVAILABLE` when `hasUnavailableBinding()` and the path's `v{n}/{contextPath}` matches an unavailable bound collection, else 404 `APP_API_ENDPOINT_NOT_FOUND` (both as `ErrorBody`); `unavailable()` → 409 `APP_API_COLLECTION_UNAVAILABLE`; the handler call is wrapped in `recordApiCall`.

- [ ] **Step 1: IntTests** (`@WebMvcTest(AppApiEndpointController.class)` with `AppExecutionRestTestConfiguration`, `@MockitoBean AppApiExecutionFacade`, `WebhookWorkflowExecutor` stubbed to return outputs carrying a `WEBHOOK_RESPONSE`):
  - `testBoundCollectionEndpointRuns` — `POST /api/app/v1/api/v1/crm/contacts` → 201 + body from the workflow's response
  - `testWorkflowChosenStatusAndHeadersPassThrough` — response `INVALID_INPUT` → 400 with the workflow's JSON body, header `X-Foo` kept
  - `testUnboundCollectionReturns404` — collection exists in the environment but `getCallableApiCollections()` excludes it → 404 `APP_API_ENDPOINT_NOT_FOUND`
  - `testOtherAppsCollectionReturns404` (Review Focus 1)
  - `testDisabledEndpointWorkflowReturns409`
  - `testUnavailableBoundCollectionReturns409`
  - `testOverlappingPatternsUseFirstMatch` (Review Focus 2)
  - `testXEnvironmentHeaderIsIgnored` — header `X-ENVIRONMENT: PRODUCTION` on a DEVELOPMENT app still searches DEVELOPMENT
  - `testCallIsRecordedWithAppId` (verify `recordApiCall` invoked with collection and endpoint names)
  - Filter chain (extend `AppApiKeyFilterChainTest`): `testDisabledAppOnApiPathIsRejected`, `testRotatedKeyOnApiPathIsRejected` (Review Focus 4)
  - `AppApiCollectionApiControllerIntTest.testListsBoundCollectionsWithEndpointsAndSchemas`
  - `AppApiExecutionFacadeImplTest`: `testCallableCollectionsExcludeUnavailable`, `testDisabledAppThrows`, `testSchemasNullWhenTriggerUnreadable`
  Run, FAIL.
- [ ] **Step 2: Implement** facade, controllers, surface constant, spec + regenerate. Run, PASS.
- [ ] **Step 3: Commit** — server `fixup!` server subject; `ToolExecutionSurface` change in the same fixup; regenerated `generated/` `fixup!` regenerated subject.

### Task 7: CLI — bind, unbind and status

**Files:**
- Modify: `cli/clients/automation-app/openapi.yaml` (copy the Task 4 public REST additions, `Environment` inlined) and regenerate `./gradlew :cli:clients:automation-app:generateClient`
- Modify: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppBindingCommand.java` (new `bindApiCollection`, `unbindApiCollection`), `AutomationAppCommand.java` (`status` :205-238 and table :443 add an "API collections" section: name, `v{n}/{contextPath}`, available, endpoints `METHOD path`), `AutomationClientFactory.java` (`appApiCollectionBindingApi(CliConfig)`)
- Test: `cli/commands/automation/src/test/.../AutomationAppBindingCommandTest.java`, `AutomationAppCommandTest.java`

**Interfaces:**
- Produces: `automation app bind api-collection --api-collection-id <id> [--app-id] [--profile …]`, `automation app unbind api-collection --api-collection-id <id>`; both DEVELOPMENT-only via `AppCommandSupport.resolveDevelopment(...)`. When `--api-collection-id` is omitted, `bind` lists bindable collections (like `describeBindableWorkflows` :366) and exits non-zero.

- [ ] **Step 1: Tests**: `testBindApiCollectionPutsBinding`, `testBindApiCollectionWithoutIdListsBindable`, `testUnbindApiCollection`, `testStatusShowsApiCollectionsAndEndpoints`, `testBindApiCollectionRefusesNonDevelopment` — run, FAIL.
- [ ] **Step 2: Implement.** Run, PASS; `./gradlew :cli:commands:automation:check`.
- [ ] **Step 3: Commit** — `fixup! - cli - Add the automation app commands` (generated client in the same fixup, matching how the original commit carried it; if the original put it in the regenerated commit, use that subject).

### Task 8: SDK — `client.api` and server functions

**Files (L = `sdks/frontend/automation/app/library/src`):**
- Modify: `L/core/request.ts` — `RequestOptions.method` becomes `'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT'`; add `headers?: Record<string, string>`, `query?: Record<string, string | string[]>`; add `rawResponse?: boolean` which returns `{data, headers, status}` without throwing on non-2xx
- Create: `L/core/api.ts`, `L/core/api.test.ts`
- Modify: `L/core/client.ts` (`api: ApiApi`), `L/core/types.ts` (re-export `AppApiCollection`, `AppApiEndpoint`), `L/index.ts`
- Modify: `L/tanstack/validators.ts` (`callApiInput`), `L/tanstack/handlers.ts` (`callApiHandler`, `listApiCollectionsHandler`), `L/tanstack/serverFns.ts` (`callApi`, `listApiCollections`), `L/server-fns.ts`; tests beside each
- Regenerate: `npm run generate:api` (updates `L/generated/openapi.ts`)
- Template: `sdks/frontend/automation/app/create/template/src/server/overview.ts` (`AppOverview` gains `apiCollections: AppApiCollection[]`), `template/test/overview.test.ts`; README of the library (`api` first in the usage section)

**Interfaces:**
- Produces:
  - `export interface ApiCallOptionsI extends CallOptions { body?: unknown; headers?: Record<string, string>; method: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT'; path: string; query?: Record<string, string | string[]> }` — `path` is `v{n}/{contextPath}/{endpointPath}` (leading slash tolerated, stripped).
  - `export interface ApiResponseI<T = unknown> { body: T; headers: Record<string, string>; status: number }`
  - `export interface ApiApi { call<T = unknown>(options: ApiCallOptionsI): Promise<ApiResponseI<T>>; list(options?: CallOptions): Promise<AppApiCollection[]> }`
  - `export function createApiApi(request: Requester): ApiApi` — `call` requests `/api/app/v1/api/${path}`; it does **not** throw for a workflow-chosen status; it throws `BytechefAppError` only when the body is an app `Error` with `code` in (`APP_API_ENDPOINT_NOT_FOUND`, `APP_API_COLLECTION_UNAVAILABLE`, `APP_DISABLED`, `UNAUTHORIZED`) or on transport failure/timeout.

- [ ] **Step 1: Tests** (`mockFetch`/`jsonResponse`, pattern of `core/workflows.test.ts`): `call sends method, path, query and body`, `call returns a workflow-chosen 400 without throwing`, `call throws BytechefAppError on APP_API_ENDPOINT_NOT_FOUND`, `call strips a leading slash`, `list returns collections`, request tests for `PUT`/`DELETE`/`PATCH` and `rawResponse`; validator tests for `callApiInput` (rejects unknown method, non-string path); handler test for `callApiHandler`. Run `npm test` in the library — FAIL.
- [ ] **Step 2: Implement**, regenerate types, run `npm run check` (includes `check:api`) — PASS.
- [ ] **Step 3: Template** — overview loads `apiCollections`; update its test; run the template's test — PASS.
- [ ] **Step 4: Commit** — `fixup! - sdk - Add the @bytechef/app client library, …`

### Task 9: Verify

- [ ] `./gradlew spotlessApply`, then `./gradlew :server:ee:libs:automation:automation-app:check :server:ee:libs:automation:automation-api-platform:check :server:ee:libs:automation:automation-security-web:automation-security-web-impl:check :server:libs:platform:platform-tool-execution:platform-tool-execution-api:check :cli:commands:automation:check` — PASS.
- [ ] `./gradlew :server:ee:libs:automation:automation-app:automation-app-service:testIntegration` — PASS (test ≠ testIntegration).
- [ ] `cd client && npm run check` — PASS, no `Unhandled` in output. `cd sdks/frontend/automation/app/library && npm run check` — PASS.
- [ ] Boot `server-app`, bind a DEVELOPMENT collection to a DEVELOPMENT app, `curl -H "Authorization: Bearer <app key>" localhost:9555/api/app/v1/api/v1/<ctx>/<path>` — the endpoint workflow's response; the same call with a key of an unbound app → 404.
