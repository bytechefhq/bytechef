# Apps (EE, server side)

An **App** is a web application built outside ByteChef (with an AI coding tool, hosted on Vercel or the customer's
own infrastructure) that reaches third-party systems only through ByteChef. ByteChef holds the credentials, decides
what the app may call, records every call, and can switch the app off. The app holds one secret key that is useless
anywhere but ByteChef. ByteChef does not host app code.

Read this before touching anything named `App*`, `/api/app/v1`, or the connection-delete guard.

Design rationale: `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`. Plan:
`docs/superpowers/plans/2026-10-06-apps-server.md`. This file covers sub-project A (server and admin UI), the SDK (its
**SDK** section) and the app template (its **Template** section; design
`docs/superpowers/specs/2026-10-07-apps-template-design.md`). Sub-project D, the coding-agent loop (design
`docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md`), has its server half in the **Agent loop** section; the
CLI, template and plugin half is Plan D2's.

## Module map

`server/ee/libs/automation/automation-app/`, wired into `server-app` only (not a distributed-EE app):

| Module | Holds |
|--------|-------|
| `automation-app-api` | `App`, `AppConnection`, `AppConnectionOperation` entities, enums (`AppStatus`, `AppHostingTarget`, `AppOperationType`), `AppErrorType`, DTOs, `AppFacade` (admin) and `AppExecutionFacade` (app-facing) interfaces, `AppService`/`AppConnectionService`, `AppSecretKeys`, `AppBindingKeys`, `AppPrincipal`, `ProxyRequest`/`ProxyResponse` |
| `automation-app-service` | the service and facade impls, repositories, the visibility/ownership/environment resolvers, `AppConnectionUsageChecker`, the audit events (`audit/AppAuditEvent`, `audit/AppAuditPublisher`), and the proxy (`AppProxyExecutor`, `ProxyPathValidator`, `ProxyHeaders`). Liquibase: `changelog/automation/app/20261006000001_automation_app_init.xml` |
| `automation-app-graphql` | admin UI surface (`app.graphqls`, `AppGraphQlController`, which only delegates to `AppFacade`) |
| `automation-app-public-rest` | two OpenAPI specs and generators: the app-facing `/api/app/v1` (`AppConnectionApiController`, `AppToolApiController`, `AppProxyApiController`) and the management `/api/automation/v1/apps` (`AppApiController`); both extend `AbstractAppApiController`. Both generator tasks set `supportingFilesConstrainedTo=[ApiUtil.java]` so they do not collide on `org.openapitools.configuration.*` |

Touched outside the module:

- `automation-security-web-impl`, package `...security.web.app`: `AppApiKeySecurityConfigurer` (+ its
  `Contributor`), `AppApiKeyAuthenticationProvider`, `AppAuthenticationToken`.
- `platform-security-web-api`: `AbstractApiKeyAuthenticationToken` gained one protected constructor taking an
  arbitrary principal and authorities (the existing authenticated constructors require a Spring `User`).
- `automation-configuration-service` (EE): `AppPermissionScope { APP_VIEW -> VIEWER, APP_MANAGE -> ADMIN }` and its
  provider (`@ConditionalOnEEVersion`, checked by `PermissionScopeProviderEditionGatingTest`).
- `automation-configuration-service` (CE): `WorkspaceConnectionFacadeImpl.delete` now runs the connection delete
  (which the guard can refuse) before cancelling the scheduled OAuth refresh, since the cancel reaches an external
  scheduler that no rollback undoes; `platform-connection-api`/`-service`: `ConnectionUsageChecker` SPI and
  `ConnectionFacadeImpl` wiring.
- `platform-tool-execution-api`: `ToolExecutionSurface.AUTOMATION_APP_ACTION|TOOL|PROXY`,
  `ToolExecutionKind.HTTP_PROXY`, `ToolExecutionEvent.appId`. `platform-tool-invocation-log-*`: `app_id` column
  (`20261105000001_tool_invocation_log_added_app_id.xml`), an `appId` filter on `toolInvocationLogs`. Reads go
  through `ToolInvocationLogFacade`: `workspaceToolInvocationLogs(workspaceId, environmentId, ...)` needs
  `EXECUTION_VIEW` in that workspace and environment and is what the automation Tool Invocations page and the app
  Activity tab call; the tenant-wide `toolInvocationLogs` is `isTenantAdmin()` and serves the embedded Tool
  Invocations page and Connected User sheet. Its optional `environmentId` narrows it to one environment (null means
  all): the embedded Tool Invocations page (`EmbeddedToolInvocations`) passes the current embedded environment and
  remounts on a switch, the Connected User sheet filters by `connectedUserId` alone (a connected user lives in one
  environment), and the embedded public REST `/{externalUserId}/tool-invocations` passes the environment it resolves
  the connected user in. A row with a null environment (an editor test-run `AI_AGENT` row, an API row written without
  one) matches no environment, so it never appears on the environment-filtered embedded page. Rows without a workspace
  (the embedded surfaces) are visible only through the tenant-admin `toolInvocationLogs` query. The three embedded
  writers (`EmbeddedMcpToolFacade`, embedded `ActionFacadeImpl`, `ToolFacadeImpl`) record the row's `connectedUserId`
  (the MCP one leaves it null when no connected user exists yet for the external user), which is what the Connected
  User sheet and the public REST endpoint filter on. The AI agent component's `AI_AGENT` rule-block, approval-gate
  and gate-resolution rows carry the run's environment (`ActionContextAware.getEnvironmentId()`) and, for automation
  runs, its workspace (the CE
  `ToolInvocationWorkspaceResolver` seam, EE-implemented over `JobPrincipalWorkspaceResolver`). A connected user's
  project run is typed `PlatformType.AUTOMATION` (`ConnectedUserProjectTaskDispatcherPreSendProcessor`), so its
  `AI_AGENT` rows get the workspace holding that project as well; only integration-instance (`EMBEDDED`) runs get the
  environment alone, and editor test runs neither.
  Enum ordinals are persisted: append only.
- `platform-file-storage-*`: `TempFileStorage.deleteFile(FileEntry)`.
- `app-config`: `ApplicationProperties.App` (below). `server-app` and `config-server-app` yml: the
  `openapi.openAPIDefinition.base-path.app` entry and `bytechef.app.*` defaults.
- Client: `client/src/pages/automation/apps`, the Apps row in the automation Deploy group, an App filter on the Tool
  Invocations page (`?appId=`).

## The key

`AppSecretKeys.generate()` returns a `TenantKey.of()` (so the tenant is readable from the key before any lookup), its
hex SHA-256 (`app.secret_key_hash`, 64 chars, unique index) and a hint (last 8 characters, `=` padding stripped; not
the first characters, which encode the tenant id and are identical for every app of a tenant). The hash is
**unsalted on purpose**: the key has 24 bytes of CSPRNG entropy and the request-path lookup is an equality `SELECT` on
the hash, which a salt would defeat. This is the first hashed secret in the codebase; `api_key` and
`mcp_server.secret_key` are plain. The key is returned only by create and rotate. Rotation replaces hash and hint; the
old key is dead on its next request (no overlap window). `secret_key_last_used_date` is written by a targeted
`UPDATE` throttled by its own `secret_key_last_used_date < :cutoff` predicate, so the whole cluster writes at most once
per app per minute, best-effort (a failing `UPDATE` is logged at WARN and does not change the request outcome; the method is
`REQUIRES_NEW`, so a failure to begin or commit that transaction escapes the catch, see the open items); it bumps
neither `version` nor the audit columns.

**The lookup is not cached.** Caches here are per-replica Caffeine with no eviction broadcast, so a cache would make
disable and rotate take effect on one node only. Do not add one.

## App-facing authentication

`/api/app/v1/**` is a root of its own, not under `/api/automation/v1`: `AutomationApiKeySecurityConfigurer` claims
every `^/api/automation/v[0-9]+/.+` path for user API keys, and the two credential types must never be accepted on
each other's endpoints. The base path is `${openapi.openAPIDefinition.base-path.app:/api/app}/v1`; the default is
deliberately non-empty (the sibling `${...:}` pattern would mount at `/v1/**`, outside the `/api/**` chain, if the
property went missing).

- `AppApiKeySecurityConfigurer` matches `^/api/app/v[0-9]+/.+`, ignores CSRF on that pattern (without it every POST
  is rejected), and ignores `X-ENVIRONMENT`: an app's environment belongs to its row.
- The converter turns every malformed-key failure (a `TenantKey.parse` `RuntimeException`, an invalid tenant id) into
  `BadCredentialsException`. The filter switches to the key's tenant before the provider runs, outside its
  authentication-failure handling, so a tenant `TenantContext` would reject must be refused in the converter or it
  surfaces as a 500.
- The provider hashes the key, looks the app up, answers unknown key as 401 and a `DISABLED` app with
  `DisabledException`, which the API key filter maps to **401**. `AppExecutionFacadeImpl` re-checks the status and
  answers **403 `APP_DISABLED`** for an app disabled between authentication and execution. Both are intentional; the
  401 is the common case.
- The principal is `AppPrincipal(tenantId, appId, workspaceId, environment)`; `AppAuthenticationToken` carries
  `ROLE_APP`. The app is not a user: `SecurityUtils.extractPrincipal` returns null for it, so writes made during an
  app call are audited as `SYSTEM`, and `SecurityUtils.getCurrentUserLogin()` throws. Attribution is the invocation
  log's job (`app_id`), not the audit columns.
- `AppApiKeyFilterChainTest` runs both key configurers in one real filter chain with CSRF on: each key type is 401 on
  the other's endpoints, an app POST needs no CSRF token, and disabled, unknown, rotated-out and malformed keys are
  401.

## Execution lane

`AppExecutionFacadeImpl` does the identity, allow-list and connection checks on the request thread and runs only the
component call on **`appExecutor`** (a bulkhead of its own, limit `bytechef.app.execution.concurrency-limit`, default
10, so a burst from apps takes no permit from scheduled runs on `workerExecutor` nor from the API Platform sync path
and the editor's Test button on `syncWorkerExecutor`), bounded by `bytechef.app.execution.timeout` (read and validated
before submit: once a component has started, its side effects cannot be undone, so a missing timeout must fail the
request, not the component). The facade is deliberately not transactional. Waiting for a lane permit counts against
that timeout: a fair `Semaphore` sized to the same concurrency limit sits in front of `appExecutor` (taken with
`tryAcquire(timeout)`, released when the lane task ends, including a task cancelled before it ran), so a saturated lane
answers 504 `APP_EXECUTION_TIMEOUT` ("No app execution slot became free within …") instead of parking the request
thread in `SimpleAsyncTaskExecutor`'s throttle. The component gets whatever remains of the timeout. A timeout cancels
the future with interrupt and answers 504 `APP_EXECUTION_TIMEOUT`.

The call runs under `SecurityUtils.runAsSystem`, like components in workflow workers. Why: `ContextPropagatingTaskDecorator`
carries the `AppPrincipal` onto the lane, and OAuth2 token refresh (`TokenRefreshHandler` ->
`ConnectionServiceImpl.validateOwnerOrAdmin`) throws for a non-user principal. Consequence: component code on the lane
sees the system principal with admin authority; none branch on caller identity for automation execution today.

The only connection check on the path is `checkAvailable`: the connection exists, is in the app's workspace
(`workspace_connection`), is in the app's environment, and is `ACTIVE`; otherwise 409 `APP_CONNECTION_UNAVAILABLE`.
`executePerform` and `executeTool` trust whatever connection id they are given.

An unknown operation answers 403 `APP_OPERATION_NOT_ALLOWED` with a message that lists the binding's allowed
operations of that type (`'x' is not allowed on binding 'crm'; allowed actions: a, b`) so a coding agent can
self-correct. Typed bindings and an instructions endpoint were deliberately not built.

## Tool names

Tools are addressed by `(bindingKey, toolName)` in the URL. The listing returns a flat `name` for LLM function
calling, `<BINDING_KEY>__<TOOL_NAME>`: the binding key through `AppBindingKeys.toFlatKey` (non-alphanumeric runs ->
`_`, upper case) and the tool name camel-to-UPPER_SNAKE (`SALES_SLACK__SEND_MESSAGE`). Clients never parse a flat
name; they map it back through the listing.

**Collision rule:** the binding key regex (`^[a-z][a-z0-9_-]{0,63}$`) lets `support-crm` and `support_crm` both
exist, and both flatten to `SUPPORT_CRM`. `AppConnectionServiceImpl.bind` therefore rejects a key whose flat form
collides with another binding on the same app (`APP_BINDING_KEY_ALREADY_EXISTS`). The check is check-then-save
with no DB constraint, so two concurrent first binds can still race.

## Action listing

`GET /api/app/v1/actions` (tag `app-connection`, so it lands on `AppConnectionApiController` beside `executeAction`;
model `AppAction`: `bindingKey`, `componentName`, `actionName`, nullable `description`, `inputSchema`) returns every
allowed action of the app's bindings with its input JSON Schema. `AppExecutionFacadeImpl.getActions()` resolves each
binding's component at its **latest version** (what `actions.execute` uses without a `componentVersion`) and keeps the
allowed names; a component that cannot be resolved, or an allowed name missing from the latest version, is skipped,
as `getTools()` skips an unresolvable component. The listing is not written to the invocation log, and it is
authenticated as the app, like `GET /tools`.

- **`ActionInputSchemaGenerator`** (`platform-component-api`, `component/util`) builds the schema recursively from the
  platform `Property` tree: nested objects and array items, static options as `enum` (DATE/DATE_TIME/TIME options as
  ISO strings), `default`, `format` for the date types, `DYNAMIC_PROPERTIES` as an open object, and a property with a
  display condition never in `required`. **Hidden properties are excluded everywhere in the tree**, as are options that
  come from a data source. Tools keep the flat `JsonSchemaGeneratorUtils.generateInputSchema`; it flattens objects and
  arrays and must not be used for actions. A follow-up may move tools over.
- **No component visibility filtering on this line.** There is no `ComponentVisibilityProvider` SPI here, so the
  listings and execution offer every allowed operation of a resolvable component, and no component- or
  action-disabled 403 exists (see "Not on this line yet").
- **Listings do not gate on connection state.** `getBindings`, `getTools` and `getActions` list a binding's allowed
  operations whatever the connection's status; only execution answers 409 `APP_CONNECTION_UNAVAILABLE`.
- **One bad definition does not fail a listing.** `getActions()` and `getTools()` catch a `RuntimeException` from
  schema generation per definition, list that action or tool with `{"type": "object"}`, and log a WARN naming the
  component and the action or tool.

## The proxy

`POST /connections/{bindingKey}/proxy` sends a raw HTTP request through a bound connection with the connection's
credentials attached. The credentials must only ever reach the connection's own base URI.

- **Pinning.** `ProxyPathValidator.validate` accepts only a plain absolute path; `pin(baseUri, path)` joins it to the
  connection's `executeBaseUri` and proves scheme, host and port equal the base's and the path stays under the base
  path (a base with userinfo, query or fragment is rejected). The executor sends the **pinned absolute URI**, not the
  relative path: `HttpClientExecutor` passes any URL containing `://` through unchanged while still attaching
  credentials, so the string validated is exactly the string sent. Redirects are never followed; a 3xx goes back to
  the app.
- **Path rules.** ASCII punctuation allow-list; `? # @ \` whitespace and control characters rejected; percent-encoded
  `/ \ ? # @ %` rejected (no second decoding layer); empty inner segments (`//`) rejected. Each decoded segment is cut
  at the first `;` (decoded `%3b` included: upstreams drop path parameters before resolving dot segments), trimmed,
  NFKC-normalised, and re-checked against `.`, `..`, `/`, `\`, `@`, `;`. Beyond ASCII only letters, digits and
  combining marks are allowed (U+3002 and similar look-alikes are not folded by NFKC). Cost: legitimate non-ASCII
  punctuation or symbols (euro sign, em dash) in upstream paths are unreachable through the proxy.
- **Not covered:** combining marks on bare dots, and IIS-style trailing dot/space trimming. Decide whether IIS
  upstreams are in scope before relying on the proxy for them.
- **Headers.** Request side drops `authorization`, `proxy-authorization`, `cookie`, `host`, `content-length`,
  `expect`, hop-by-hop headers, `forwarded`, `x-real-ip`, `x-original-url` and anything starting `x-forwarded-`,
  case-insensitively. Response side drops `set-cookie`, `set-cookie2` and hop-by-hop headers. Names must be RFC 9110
  tokens and values free of control characters, else 400 `APP_PROXY_REQUEST_INVALID`; the content type must parse as
  `type/subtype`.
- **Credentials.** The proxy sets `disableAuthorization(true)` and applies credentials itself
  (`ConnectionDefinitionService.executeAuthorizationApply`), removing app headers and query keys that collide
  case-insensitively with the applied ones. The client's own authorization path installs an error interceptor that
  turns every 4xx/5xx into an exception before the body is read, which would hide upstream errors from the app. A
  connection with no base URI or no `authorizationType` answers 409 `APP_PROXY_UNSUPPORTED`.
- **Token refresh.** Refresh lives in `TokenRefreshAspect` around `@WithTokenRefresh` beans, which the proxy does not
  pass through, so the proxy refreshes itself: exactly once, when the connection `canCredentialsBeRefreshed()` and
  the upstream status matches the authorization's refresh-on rules; it then rebuilds the context, re-resolves and
  re-pins the base URI and resends. A failed refresh answers 409 `APP_CONNECTION_UNAVAILABLE` with a fixed message.
  The refresh itself still has a pre-existing race (the identity-provider call is unlocked).
- **Spool files.** The response is requested BINARY so the client spools it to temp file storage; the client
  aborts the exchange as soon as the body passes `bytechef.app.proxy.max-response-bytes` (the SDK's
  `Http.Configuration` `maxResponseBytes`, counted while spooling; `HttpResponseTooLargeException` in the cause chain
  becomes 502 `APP_PROXY_RESPONSE_TOO_LARGE`), the read-back keeps the same cap as a backstop, and the request body
  cap is checked up front (413 `APP_PROXY_REQUEST_TOO_LARGE`). Request spool `proxy-request-<uuid>`, response spool
  `proxy-response-<uuid>[.<ext>]` (the upstream's Content-Disposition name is never used). Unique names matter
  because some storage providers (AWS) delete temp files by name alone. Both spools are deleted in `finally`.
- **In-flight cap.** `HttpClientExecutor` wraps a BINARY body in a counting stream when the configuration carries
  `maxResponseBytes`; past the limit the stream closes the upstream (cancelling the exchange) and reports end of
  stream, the client deletes the partial spool (a delete failure is logged and never masks the size error) and throws
  `HttpResponseTooLargeException`. No spool survives an oversized response on any storage provider.
- **Errors.** An `IOException` or `HttpTimeoutException` anywhere in the cause chain is an upstream transport
  failure: 502 `APP_PROXY_UPSTREAM_UNAVAILABLE` with a fixed message (the original can carry upstream body text).
  Message-text matching is not used. A local spool
  read/store failure is `ProxyStorageException`, rethrown by the facade as 500 `APP_PROXY_STORAGE_FAILED` (123). Upstream
  status codes, including 4xx/5xx, come back as a 200 proxy response with `status` set.
- The invocation log records `"<METHOD> <path without query>"` only; never the query, headers or body.

## Error mapping

Controller-local `@ExceptionHandler`s in `AbstractAppApiController`, not an advice: `GlobalResponseEntityExceptionHandler`
is `HIGHEST_PRECEDENCE` and would answer `AccessDeniedException` with 500 and every `AbstractException` with 400, and
a standalone MockMvc test hides that. Spring consults the controller's own handlers first. Every new app-facing
controller must extend it. Body is `{"code", "message"}`. A component failure is an `ExecutionException` (not a
`ConfigurationException`), so the controller handles `AbstractException` too: any non-App `AbstractException` is 422
`COMPONENT_ERROR` with the message only, never the input parameters the global handler would echo.
`ActionDefinitionServiceImpl` wraps every perform failure, `ProviderException` included, in `ExecutionException`, so
`translate` walks the cause chain: a `ProviderException` anywhere in it answers 422 `PROVIDER_ERROR` with that
exception's message only. App errors are decided before that walk. A
bean-validation failure on a management body (`maxLength` on name and url) is 400 `INVALID_REQUEST`.

Not exhaustive; `AbstractAppApiController.toAppStatus` is the source. 401 unknown/malformed/disabled key (filter); 403 `APP_DISABLED` (facade), `APP_OPERATION_NOT_ALLOWED`; 404 `APP_NOT_FOUND`,
`APP_CONNECTION_NOT_FOUND`, `APP_WORKFLOW_NOT_FOUND`, `APP_WORKFLOW_RUN_NOT_FOUND`; 409 `APP_CONNECTION_UNAVAILABLE`,
`APP_PROXY_UNSUPPORTED`, `APP_BINDING_KEY_ALREADY_EXISTS`, `APP_WORKFLOW_UNAVAILABLE`; 413 `APP_PROXY_REQUEST_TOO_LARGE`; 502 `APP_PROXY_RESPONSE_TOO_LARGE`,
`APP_PROXY_UPSTREAM_UNAVAILABLE`; 500 `APP_PROXY_STORAGE_FAILED`; 504 `APP_EXECUTION_TIMEOUT`; 422 `COMPONENT_ERROR` (a component's own error,
message only) and `PROVIDER_ERROR` (a `ProviderException` itself or in the cause chain, message only); other app errors
(including `APP_FIELD_TOO_LONG`, `APP_PROXY_PATH_REJECTED`, `APP_PROXY_REQUEST_INVALID` and
`APP_WORKFLOW_INPUT_INVALID`) and `IllegalArgumentException` 400.

## Admin side

- Gates: `APP_VIEW` (any workspace member) and `APP_MANAGE` (workspace admin; creating, binding and rotating hand the
  app credentials, so EDITOR is not enough). `AppFacade` owns authorization; `AppGraphQlController` and the REST
  controllers only delegate. The bindable-connections and component-operations lookups live on `AppFacade`, not in
  the controller.
- Visibility: an app belongs to one workspace, so `AppVisibilityPolicy` supports `PRIVATE` and `WORKSPACE` only (no
  `ORGANIZATION`) and the app is created `WORKSPACE`. `AppFacade.getApps` filters the list through
  `ResourceVisibilityResolver.filterVisibleIds("App", ...)`, so a PRIVATE app is listed only for those who can see it.
  `AppVisibilityPolicy`, `AppVisibilityProvider`, `AppOwnershipResolver` and `AppEnvironmentResolver` are
  `@ConditionalOnEEVersion`, like every bean that needs `AppService`. `AppFacade.getBindableConnections` filters through
  `ResourceVisibilityResolver.filterVisibleIds`, so a PRIVATE connection the caller cannot see is not offered; binding
  additionally requires `CONNECTION_EDIT` on the connection (this line has no `CONNECTION_USE` scope), and a bindable
  connection's `usable` flag means valid credentials plus `CONNECTION_EDIT`. The picker does not apply the per-environment
  `environmentScopeFilter` that `WorkspaceConnectionFacadeImpl` does (APP_MANAGE on an env-resolved App already implies
  workspace ADMIN in that environment).
- **Update semantics are keep-when-null** for every field except `id` (`AppService.update(long id, @Nullable ...)`);
  an empty string clears `description`/`url`. The invariant lives in the service so each transport does not
  re-implement it. Consequences: GraphQL cannot clear `lastDeployedDate` or tell absent from explicit null, and the
  management PATCH with fields plus a status change runs two facade calls (non-atomic).
- **Component version.** Execution always runs the component's latest version, so `AppBindingDTO`,
  `AppBindableConnectionDTO`, the GraphQL `AppBinding`/`AppBindableConnection` types and the app-facing
  `GET /connections` carry `componentVersion` (latest, resolved server-side through `ComponentDefinitionService`) next
  to `connectionVersion`. The binding dialog loads operations for `componentVersion`; `connectionVersion` is
  informational only. Listings resolve it with `fetchComponentDefinition`, never `getComponentDefinition` (which
  throws), so one unresolvable component (a deleted custom component, one dropped in an upgrade, one missing from the
  component index) cannot fail a whole listing: `getBindableConnections` leaves such connections out (so
  `AppBindableConnectionDTO.componentVersion` stays `int`), `AppExecutionFacade.getTools` leaves out their tools, and
  `AppBindingDTO.componentVersion` is a nullable `Integer`, null for them (GraphQL `AppBinding.componentVersion: Int`,
  not required in the app-facing OpenAPI). The Bindings tab marks such a binding "Unavailable" and never loads its
  operations. `executeAction`/`executeTool` still resolve with `getComponentDefinition` and fail that one call.
- Rebinding a key to a different connection publishes `APP_CONNECTION_BOUND` with `previousConnectionId`; the same
  connection publishes `APP_OPERATIONS_CHANGED`. `AppConnectionService.unbind` returns whether a row was deleted and
  `APP_CONNECTION_UNBOUND` is published only then. `updateApp` publishes `APP_UPDATED` when at least one editable
  field changed: `changedFields` lists them, and the new `name`, `visibility` and `hostingTarget` ride along when they
  changed.
- Name (256) and url (2048) lengths are checked in `AppServiceImpl` before any write (400 `APP_FIELD_TOO_LONG`). A
  `DataIntegrityViolationException` on save becomes `APP_NAME_ALREADY_EXISTS` only when its cause chain names
  `uk_app_workspace_environment_name`; any other violation is rethrown.
- **Connection-delete guard SPI:** `ConnectionUsageChecker` (`platform-connection-api`) lets a module outside
  platform-connection say a connection is in use; `ConnectionFacadeImpl` collects every checker through
  `ObjectProvider#orderedStream` and refuses the delete naming the consumers. `AppConnectionUsageChecker` reports
  every app binding the connection, whatever the app's status. `app_connection.connection_id` has no cascade, so
  without the guard the user would get a bare FK violation.
- Client-side owner name is resolved with `useWorkspaceUsersQuery`, falling back to `User <id>`; there is no
  server-side `ownerName`.

## Workflow bindings

An app can run workflows through `/api/app/v1/workflows`. Design: `docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md`.
Plan: `docs/superpowers/plans/2026-10-07-apps-workflow-bindings.md`. Code: `AppWorkflowExecutionFacade` (app-facing),
`AppWorkflowFacade` (admin, beside `AppFacade`), `AppWorkflowService`, `AppCallableWorkflowResolver`, tables
`app_workflow` and `app_workflow_run` (`20261007000001_automation_app_workflow.xml`).

- **What can be bound.** A **Call workflow** workflow (`workflow/newWorkflowCall` trigger) of an **enabled** project
  deployment in the app's own workspace and environment, whose project deployment workflow is enabled. Webhook-triggered
  workflows are never bindable; they stay public endpoints. `AppCallableWorkflowResolver` is the single definition of
  "callable" for binding, listing and execution: the three paths cannot disagree about what a binding means.
- **Identity of a binding.** `(project_deployment_id, workflow_uuid)`, never a workflow id. Every call resolves the id
  again through `ProjectWorkflowService.fetchProjectWorkflowWorkflowId`, so a redeploy changes what runs with no
  rebinding. Do not cache the id or store it in the row: MCP pins a version, apps deliberately do not. The key follows
  `AppBindingKeys` and is unique per app also after `toFlatKey` normalisation (same check-then-save race as connection
  bindings).
- **Availability causes.** Deployment or workflow gone, deployment disabled, deployment workflow disabled, deployment in
  another workspace or environment, trigger no longer Call workflow, trigger `inputSchema` not readable JSON or not a
  JSON Schema networknt can compile (`AppWorkflowSchemas.checkReadable`, references resolved eagerly), malformed uuid: all make `AppCallableWorkflowResolver.resolve` return empty, so bind (400 `APP_WORKFLOW_NOT_BINDABLE`), the
  bindable list, the admin list (`available: false`), the app-facing list and execute (409 `APP_WORKFLOW_UNAVAILABLE`)
  agree. Execute's `checkInput` and `getWorkflows`' unreadable-schema branch (`available: false`, label and description
  kept, schemas null) remain as defence in depth; do not remove them. Deleting a deployment is not blocked by bindings
  (no FK, no `ConnectionUsageChecker`-style guard); the binding just becomes unavailable.
- **200/202 contract.** `POST /workflows/{key}` waits up to `waitSeconds` (omitted: `bytechef.app.workflow.default-wait`,
  30 s; allowed `0..max-wait`, 60 s, else 400 `APP_WORKFLOW_INPUT_INVALID`; `0` reads the job's state without waiting).
  200 when the run is `COMPLETED`/`FAILED`/`CANCELLED`, 202 for `RUNNING`/`APPROVAL_REQUIRED`. A failed workflow is run
  state, not an HTTP error: HTTP errors mean no run started. A wait that times out is not an error. Any other awaiter
  failure, or a failure recording the run, propagates without a run id while the job keeps running. The request thread
  waits (not on `appExecutor`, no cap on concurrent waits).
- **Run ownership.** `app_workflow_run` (`run_id` UUID string, `app_id`, `job_id`) is the authorisation record for
  polling, written synchronously because the invocation log is asynchronous. `GET /workflow-runs/{runId}` looks up by
  `(appId, runId)`: another app's run, an unknown run and a run whose job is gone all answer 404
  `APP_WORKFLOW_RUN_NOT_FOUND`, never 403. Rows cascade with the app. `AppWorkflowRunRetentionJob` purges rows older than
  `bytechef.app.workflow.run-retention-days` (90) daily (`run-retention-cron`, default 02:40); a purged run polls 404.
  A retention of zero or fewer days disables the sweep rather than deleting every row. The sweep runs once per
  tenant under that tenant's context (a failing tenant is logged and skipped), as does
  `ToolInvocationLogRetentionJob`'s.
  Unbinding does not cancel runs and does not remove their rows. `AppWorkflowExecutionFacadeIntTest` pins ownership and
  the cascade against Postgres.
- **Who the job runs as.** `principalJobFacade.createJob(..., projectDeploymentId, AUTOMATION)`, as MCP and webhooks do:
  the principal is the project deployment, so licence usage, variables and the Executions list behave as for any
  deployment run. No `runAsSystem` (unlike the action lane): `createJob` has no authorization check and `createdBy`
  stays honest. Inputs are the deployment workflow's inputs plus `{<triggerName>: input, __triggerName: <triggerName>}`.
- **Shared result mapping.** `WorkflowCallResults` (`platform-workflow-execution-api`) holds the callable-response
  output, pending-approval and approval-form-URL helpers used by both `AutomationMcpToolFacade` and
  `AppWorkflowExecutionFacadeImpl`. Change them for both or neither. Status mapping: `CREATED`/`STARTED` -> `RUNNING`,
  `STOPPED` with a resume id -> `APPROVAL_REQUIRED`, any other `STOPPED` -> `CANCELLED` (`Job.Status` has no
  `CANCELLED` of its own on this line).
- **Recording.** Each execute records one event through `ToolExecutionRecorder` under
  `ToolExecutionSurface.AUTOMATION_APP_WORKFLOW` (kind `WORKFLOW`, key as `operationName`, `jobId`): outcome `ERROR` for
  `FAILED`, `APPROVAL_REQUIRED` for a waiting run, `SUCCESS` otherwise (a still-`RUNNING` run included). Polls and
  refused attempts are not recorded.
- **The approval link is a capability.** `approvalFormUrl` is the hosted form link (`/job/resume/**` is permit-all), so
  whoever holds it, including the app, may decide the approval. The API has no approve call; the app is expected to show
  the link to a person. Withholding it per binding is an open follow-up.
- **Errors.** `AppErrorType` 118 `APP_WORKFLOW_NOT_FOUND` (404), 119 `APP_WORKFLOW_UNAVAILABLE` (409), 120
  `APP_WORKFLOW_INPUT_INVALID` (400), 121 `APP_WORKFLOW_RUN_NOT_FOUND` (404), 122 `APP_WORKFLOW_NOT_BINDABLE` (400, bind
  time, including a project the caller cannot see). `AppWorkflowFacade` gates `APP_VIEW`/`APP_MANAGE`; GraphQL and REST
  only delegate. `AppWorkflowSchemas` validates input with networknt `json-schema-validator`.
- **`resolve` refuses system deployments.** A deployment named with an API collection, MCP server or A2A server marker,
  or one whose project is a system project (`SystemProjects.isSystemProjectName` or the `__EMBEDDED__` marker), never
  resolves — the same set `getBindableWorkflows` hides. Bind validates through `resolve`, so such a deployment cannot
  be bound through the REST `PUT /apps/{id}/workflows/{workflowKey}` either, and an existing binding to one stops
  executing.

## API collection bindings and the app API path

An app calls API Platform endpoints through `/api/app/v1/api/v{collectionVersion}/{contextPath}/{endpointPath}`; this
is the default path for the app's code (design `docs/superpowers/specs/2026-10-16-apps-api-platform-design.md`, plan
`docs/superpowers/plans/2026-10-16-apps-api-platform-bindings.md`). Code: `AppApiCollectionFacade` (admin),
`AppApiExecutionFacade` (app-facing), `AppApiCollectionService`, `AppApiCollectionResolver`, `AppApiEndpointController`.

- **Table.** `app_api_collection` (`app_id`, `api_collection_id`, unique together;
  `20261016000001_automation_app_api_collection.xml`). Rows cascade with the app; there is no FK to `api_collection`, so deleting a collection leaves a binding that
  resolves to nothing. A whole collection is bound: the app's key may call every endpoint of it, no per-endpoint list.
- **Resolver.** `AppApiCollectionResolver` is the single definition of "callable": a binding resolves only while the
  collection exists and its hidden project deployment (`"__API_COLLECTION__" + projectId`) exists, is enabled, is in
  the app's environment and belongs to a project of the app's workspace. Bindable collections are those of the
  workspace and environment whose deployment exists, disabled ones included, so an admin can bind before enabling.
- **Facade gates.** `AppApiCollectionFacade`: `getApiCollectionBindings` `APP_VIEW`; `getBindableApiCollections`,
  `bindApiCollection`, `unbindApiCollection` `APP_MANAGE`. The bindable list keeps only collections whose
  project the caller can see (`ResourceVisibilityResolver.filterVisibleIds`). Bind publishes `APP_API_COLLECTION_BOUND` only when a row was created, unbind
  `APP_API_COLLECTION_UNBOUND` only when one was deleted. GraphQL (`appApiCollections`, `appBindableApiCollections`,
  `bindAppApiCollection`, `unbindAppApiCollection`), management REST (`/apps/{id}/bindable-api-collections`,
  `/apps/{id}/api-collections[/{apiCollectionId}]`) and the CLI (`automation app bind|unbind api-collection`) only
  delegate. The client's Apps page has an API collections tab.
- **Errors.** `AppErrorType` 124 `APP_API_COLLECTION_NOT_FOUND` (404; reserved: mapped, not thrown today), 125 `APP_API_COLLECTION_NOT_BINDABLE` (400, bind
  time), 126 `APP_API_COLLECTION_UNAVAILABLE` (409: a path under a bound collection that does not resolve, or a
  matched endpoint whose workflow or deployment is disabled), 127 `APP_API_ENDPOINT_NOT_FOUND` (404: no endpoint of
  an available bound collection matches; never says whether an unbound collection would have). 128
  `APP_EXECUTION_FAILED` (500) is a failed endpoint workflow; a timeout is 504 `APP_EXECUTION_TIMEOUT`.
- **Shared endpoint resolution.** `AbstractApiPlatformEndpointController` (`automation-api-platform-handler-rest`) does
  matching (`/v{collectionVersion}/{contextPath}/{endpointPath}`, collection order then endpoint order, first match
  wins) and runs the endpoint's workflow through its `apiPlatform/newApiRequest` trigger. Subclasses choose the
  searchable collections, the environment, and what an unmatched or unavailable path answers:
  `ApiPlatformHandlerController` (`/api/o/**`, platform API keys) and `AppApiEndpointController` (the app's bound,
  resolvable collections, the app's own environment; `X-ENVIRONMENT` is ignored). Change matching there, for both.
- **Pass-through and the error marker.** The status, headers and body the workflow chooses pass through unchanged.
  ByteChef's own errors carry `X-ByteChef-App-Error: true`; the controller strips that header from anything a workflow
  writes, so the SDK can trust it (`api.call` throws only on marked responses).
- **Recording.** Each matched call records one event under `ToolExecutionSurface.AUTOMATION_APP_API` (kind
  `WORKFLOW`, endpoint name as tool, collection name as `operationName`, the app id); a status of 400 or above is
  recorded as `HTTP <status>`. Unmatched and unavailable paths are not recorded.
- **App-facing listing.** `GET /api-collections` lists every binding with `available`; an unavailable one has empty
  `endpoints`. Endpoints carry method, path and the trigger's request and response JSON Schemas.

## Agent loop (server half)

A coding agent builds API endpoint, scheduled and app-callable workflows, creates API collections, endpoints and data
tables, and deploys through the management MCP; it manages apps through the CLI (Plan D2), which reads the four routes
below. Design: `docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md`, and for API endpoints and scheduled work
`docs/superpowers/specs/2026-10-16-apps-api-platform-design.md`.
Plan: `docs/superpowers/plans/2026-10-08-apps-agent-loop-server.md`. DEVELOPMENT only: every tool refuses another
`environment` with "The app loop runs in DEVELOPMENT only".

- **One definition of callable.** `AppCallableWorkflowResolver` implements the CE SPI `CallableWorkflowChecker`
  (`automation-configuration-api`, `…configuration.workflow`): `check(workflowId)` returns the problems (no
  `workflow/v1/newWorkflowCall` trigger, unreadable `inputSchema`) and the schema. The resolver is keyed by
  `(workspaceId, environment)`, not an `App`, and its listing drops system deployments
  (`SystemProjects.isSystemDeploymentName`) and system projects, as `resolve` always did.
- **`buildWorkflow` kinds** live on the management MCP wrapper, not the delegate: `WorkspaceScopedSubAgentToolCallback`
  takes every `WorkflowKindChecker` bean (`automation-configuration-api`) for `buildWorkflow` only
  (`ToolCallbackContributorConfiguration`), and offers `kind` only when at least one exists: `APP_CALLABLE`
  (`AppCallableWorkflowResolver`, EE), `API_ENDPOINT` (`ApiEndpointWorkflowChecker`, EE) and `SCHEDULED`
  (`ScheduledWorkflowChecker`, CE). An unknown kind is refused with the supported ones. The result names, per saved
  workflow, its uuid and the next step: `deployProject` for `APP_CALLABLE`, `createAppApiCollection` /
  `createAppApiEndpoint` for `API_ENDPOINT`, and for `SCHEDULED` `deployProject` with the uuid in `enabledWorkflows`
  when the app loop is registered (decided by an `APP_CALLABLE` checker being present: the EE app module that
  registers `deployProject` also supplies it), otherwise a deploy in the ByteChef UI with the workflows enabled.
  With a kind the wrapper appends that kind's requirements to the request, installs the capture holder and the
  `AppCallableDraftGuard.APP_CALLABLE_KEY` flag in the forwarded context (`WorkflowPersistCaptureUtils.withCaptureHolder`
  keeps a holder already present), and checks every distinct captured `workflowId` afterwards: nothing captured
  passes the reply through; a failure is an error naming each id and leaves the workflow saved. A check that throws
  reports `workflowId X: could not be checked` (never the exception text), and a workflow deleted in the same run is
  dropped from the capture holder. Under the flag, `ProjectWorkflowTools.updateWorkflow`/`deleteWorkflow` and
  `ScriptTools.updateScriptComponentCode` refuse a workflow outside the project's DRAFT version and name the DRAFT
  id, checked after their facade read.
- **`ProjectDeploymentFacade.deployProject`** owns the deploy rules behind one guard (`DEPLOYMENT_CREATE` on the
  project in DEVELOPMENT, plus `DEPLOYMENT_EDIT` on the deployment when updating one): one DEVELOPMENT deployment per
  project, updated in place so bindings `(deployment id, workflow uuid)` stay valid; refuses unpublished changes (the Data Sync rule over every
  workflow); a row for every workflow of the version (`checkProjectDeploymentWorkflows` deletes rows missing from its
  list); slots from `ComponentConnectionFacade.getWorkflowComponentConnections`; candidates from
  `WorkspaceConnectionFacade.getConnections` filtered to `VALID` + `CONNECTION_VIEW`, AI-provider connections never;
  all or nothing. It writes the deployment disabled; arming is the tool's separate `enableProjectDeployment`, whose
  failure leaves it disabled and is reported as `arming failed`. Redeploying an enabled deployment reads the
  registrations of its armed triggers before the write and disarms them only after the transaction commits (a
  failure is logged at WARN); a write that fails or rolls back leaves the deployment enabled and its triggers armed.
  Its three new collaborators are `@Lazy` so the hand-assembled IntTest contexts that build the facade without them
  still boot.
- **Why deploy and bind ask for different connection scopes.** `deployProject` offers any valid connection the caller
  can see (`CONNECTION_VIEW`): a deployment only runs the connection inside the workspace's own workflows. Binding a
  connection to an app (`AppFacade.bindConnection`) hands it to code ByteChef does not run, through the app's actions,
  tools and the credentialed proxy, so it needs `CONNECTION_EDIT`.
- **API Platform and data-table tools** `createAppApiCollection`, `createAppApiEndpoint`,
  `updateAppApiCollectionVersion`, `listAppApiCollections`, `createAppDataTable`: `AppApiPlatformLoopTools` (EE
  `automation-ai-tool`), registered beside `AppLoopTools`, DEVELOPMENT only and Copilot-independent, named apart from
  the Copilot `createApiCollection`/`listApiCollections`/`createDataTable` to avoid a registry collision. The two
  create tools enable what they create (the collection's hidden deployment, the endpoint's workflow); the admin UI and
  REST keep creating disabled. `createAppApiCollection` returns `bindCommand` (the CLI bind command), also on
  `arming failed`: a disabled collection is bindable, and its calls answer 409 until a `DEPLOYMENT_EDIT` holder enables
  it. `createAppApiEndpoint` resolves each connection slot like `deployProject`, auto-filling only required slots (an
  optional slot is filled only when chosen): `unresolved` creates nothing and lists candidates, `arming failed` leaves
  the endpoint saved but disabled (enabling needs `API_PLATFORM_EDIT` on the endpoint and `DEPLOYMENT_EDIT` on the
  collection's deployment). `updateAppApiCollectionVersion` moves the
  collection to a newer published version in place and reports removed, disabled endpoints and the missing
  connections and inputs that disabled them.
- **MCP tools** `deployProject`, `listConnections`, `listAppCallableWorkflows`: `AppLoopTools` (EE
  `automation-ai-tool`), registered by `AppLoopMcpContributorConfiguration` (`appLoopMcpContributor`) through
  `WorkspaceScopedFlatToolCallback`, contributed only when the app module is present. Refusals are returned as values
  (`status: refused`, `error`): the wrapper reduces a thrown exception to its class name.
- **REST for the CLI** in `openapi-management.yaml`: `GET /apps/{id}/bindable-connections?componentName=`,
  `GET /apps/{id}/bindable-workflows`, `GET /apps/component-operations`, `GET /connections` (new
  `WorkspaceConnectionApiController`, tag `workspace-connection`). Facades own the guards
  (`AppFacade.getConnections`: `CONNECTION_VIEW` in the environment; `getCallableWorkflows`: `DEPLOYMENT_VIEW` in
  DEVELOPMENT); every controller extends `AbstractAppApiController` for 403 on a denial. `AppBindableConnectionDTO`
  gained `usable`; GraphQL `appBindableConnections` did not change. Every `/apps/{id}/…` route is scoped to the
  request's environment (`X-Environment`, absent meaning PRODUCTION, the one the API key authenticated in):
  `AppEnvironmentScope` first calls the unguarded `AppFacade.requireAppInEnvironment`, before any permission check, so
  an app of another environment is `APP_NOT_FOUND` (404), indistinguishable from a missing one; only `getApp` and
  `updateApp` then read the app through the guarded `AppFacade.getApp`.
- **End to end:** `AppLoopToolsIntTest` (`server-app`) runs build → publish → deploy → bind → call, then a rebuild
  through `draftWorkflowId`, and the same binding runs the new definition.

### CLI, template and plugin (Plan D2)

- **Commands** (`cli/commands/automation`, all DEVELOPMENT only, refused before any request otherwise):
  `AutomationAppCommand` (`automation app create|link|list|status|key rotate`), `AutomationAppBindingCommand`
  (`automation app bind connection|bind workflow|unbind connection|unbind workflow`), `AutomationConnectionCommand`
  (`automation connection list`). They share `AppCommandSupport`, `AppLinkFile` and `EnvFile`.
- **The client** `cli/clients/automation-app` is generated from a committed copy of `openapi-management.yaml` with
  `Environment` inlined (the server file's relative `$ref` does not resolve from there). Re-copy, re-inline and run
  `generateClient` whenever the server spec changes; nothing fails if you forget.
- **`.bytechef/app.json`** (`id`, `uuid`, `workspaceId`, `environment`, `host`; no secret) is found by walking up like
  git, keyed on the file, never on a `.bytechef` directory alone: `~/.bytechef/` holds the CLI's `config`. A file
  written for another host, environment or workspace is an error. `create` and `link` refuse where one exists, so a
  directory never gets a second app.
- **The key** goes only into the env file: only the `BYTECHEF_URL`/`BYTECHEF_APP_KEY` lines change, the write goes
  through an owner-only temporary file renamed over the target (a symlinked env file is refused), and
  `git check-ignore` decides the not-ignored warning (silent outside a repository). A failed write after create names
  the app id and the two recovery commands, never the key.
- **`bind connection` is a full replace** (empty lists, `proxyEnabled: false`), picks the only usable bindable
  connection when `--connection-id` is absent, and checks names against `component-operations` before its single PUT.
- **`status`** takes each binding's component version from `bindable-connections` (the management `AppBinding` has
  none); a 403 there (an `APP_VIEW`-only caller) or a connection that can no longer be bound shows `none` in table output and `null` in
  JSON.
- **Template:** `AGENTS.md`'s "Setting up the app with ByteChef" section carries the loop; `AppTemplateCliCommandDriftTest`
  (server-app, next to `PluginSkillTagDriftIntTest`) fails when it names a `bytechef …` command with no
  `@Command(name = …)` under `cli/commands`. `check:template` cannot do this: its job has no CLI binary.
- **Plugin:** the `bytechef-app-builder` skill. It states the order of preference (API Platform endpoint → Call-workflow
  binding → direct action or tool → proxy, saying why past the first) and the decision table. Its `mcp` fences are
  `edition: ee` and keyed only on the EE app-loop tools (`createAppApiCollection`, `createAppApiEndpoint`,
  `updateAppApiCollectionVersion`, `listAppApiCollections`, `createAppDataTable`, `deployProject`, `listConnections`,
  `listAppCallableWorkflows`), so a CE server never receives the app-loop instructions; its prose names the CE tools
  (`buildWorkflow`, `publishProject`, `createProject`), the one documented exception to the "`uses:` lists every
  tool" rule. The template's `AGENTS.md` carries the same order and loop.

### Not on this line yet

These are described in the Apps designs but arrive with other port branches:

- **Component visibility** (`ComponentVisibilityProvider` filtering of the listings, and the `COMPONENT_DISABLED` /
  `ACTION_DISABLED` 403s on execution): `0_p_component_policies`.
- **Code-artifact plugin skills** (`bytechef-code-workflow`, `bytechef-custom-component`): the code_workflows and
  custom_components ports.

## SDK

`@bytechef/app` lives in `sdks/frontend/automation/app/` (npm workspace: `library/` is the package, `test-app/` a
minimal TanStack Start app). Design: `docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md`, "Design —
B. SDK". Four entry points: `.` (core client, no runtime dependencies), `./tanstack-start` (middleware, handlers,
validators, types), `./tanstack-start/server-fns` (the eleven ready-made server functions, nothing else) and `./ai-sdk`.

- **`./tanstack-start` must never reach a `createServerFn` call.** Start registers every server function declared in
  a module the app imports, used or not: when the nine lived in `./tanstack-start`, importing only
  `bytechefMiddleware` published all nine, `proxy` included, as RPC endpoints at predictable ids. Importing any one
  ready-made function registers all eleven; importing only `./tanstack-start` registers none.
  `test-app/scripts/check-bundle.mjs` follows the built `tanstack-start.js` and its `./*.js` chunks and fails on any
  `createServerFn(`, and fails unless `server-fns.js` declares all eleven at module level.

- **Types come from the server contract.** `library/src/generated/openapi.ts` is generated from
  `automation-app-public-rest/openapi.yaml` (`npm run generate:api`) and committed; `npm run check:api` regenerates and
  fails on drift, so change the spec and the generated file together. The package never declares its own wire types.
- **The `@tanstack/react-start` peer entry is load-bearing.** Start's Vite plugin treats a dependency that names it as
  a peer as a framework package and compiles it, which is what makes server functions shipped in the package work;
  `library/src/package.test.ts` pins it. Start compiles only module-level `x = createServerFn(…)` declarations, so
  there is no factory: eleven ready-made server functions configured from the environment, and an app that wants
  explicit options writes its own on `bytechefMiddleware(options)`. The library build stays unminified (`build.minify:
  false`) so `dist` keeps those declarations. The server functions ship in the package, opt-in. The template does **not** import them: each is a public endpoint, so it writes its own server functions on
  `bytechefMiddleware()`; only the middleware, validators and handlers come from the package.
- **Core surface.** `api.list()` and `api.call({method, path, body?, query?, headers?})` (`path`
  `v{n}/{contextPath}/{endpointPath}`, `.`/`..` segments refused) → `{status, headers, body}`: a workflow-chosen 4xx or
  5xx is data; only a response marked `X-ByteChef-App-Error: true` throws `BytechefAppError`. TanStack: `callApi`,
  `listApiCollections` ready-made server functions with `callApiHandler`/`callApiInput`. `connections.list()`, `actions.list()` (`AppAction[]`, `inputSchema` typed as `JsonSchema`) and
  `actions.execute`, `tools.list/execute/executeByName`, `proxy`, `workflows.list/execute/getRun`. `AppAction` is the
  generated schema with `inputSchema` narrowed to the SDK's `JsonSchema`, exported from `@bytechef/app`. A server that
  predates `/actions` answers 404 without a `{code, message}` body, which surfaces as `BytechefAppError` code
  `HTTP_ERROR` with status 404; callers treat that as "no action schemas on this server".
- **Environment variables.** `BYTECHEF_URL` and `BYTECHEF_APP_KEY`, read lazily on the first request, never under a
  `VITE_` prefix (Vite inlines those into the client bundle); client construction throws in a browser. Explicit
  `createBytechefClient` options win. `test-app/scripts/check-bundle.mjs` (`npm run check:test-app`) fails the build if
  the client bundle contains the variable name, `/api/app/v1` or the key's value.
- **No retries anywhere.** Actions are not idempotent; a silent retry can create a record twice. Failures surface as
  `BytechefAppError` with the server's `code`, or `NETWORK_ERROR`, `TIMEOUT`, `ABORTED`, `HTTP_ERROR`,
  `INVALID_RESPONSE` client-side. A failed workflow run is data, not an exception.
- **Adapter seams.** TanStack: `handlers.ts` (plain `({data, context}) => …` functions returning JSON-serialisable
  results, since Start rejects `unknown`), `validators.ts` (plain functions that throw `TypeError`; plain objects only),
  `middleware.ts` (puts the client on `context.bytechef`) and `serverFns.ts` (the only file that calls
  `createServerFn`, re-exported only by `src/server-fns.ts`). The ready-made `executeWorkflow` does not wait (Start gives handlers no abort signal): it returns the
  run after `waitSeconds` and the browser polls `getWorkflowRun`; the core's `wait` is unbounded except by `signal`.
  `workflows.execute` rejects `waitSeconds * 1000 >= timeoutMs` with a `RangeError` before sending (a starting POST
  cut off by the client timeout leaves a started run with no `runId`). With `waitSeconds` omitted and a `timeoutMs`
  under 35 s, it sends `floor(timeoutMs / 1000) - 5` (minimum 0) so the server's 30 s default wait cannot outlast
  the client.
  AI SDK: `ai/tools.ts` builds every tool with `dynamicTool` + `jsonSchema` (validates nothing client-side; `ai` needs
  `zod` in the app), returns `BytechefAppError`s to the model as text except `ABORTED`, a 401, and a 403 that is
  `APP_DISABLED` or carries no code (rethrown to the developer; `APP_OPERATION_NOT_ALLOWED` stays model-visible since
  it lists the allowed operations), logs unavailable codes with `console.error`, tells the model a workflow `TIMEOUT`
  may have started a run, and flat names come from the
  server listing (`AppBindingKeys.toFlatKey` of the binding key, `__`, UPPER_SNAKE tool name; see Tool names) or the
  SDK's `toFlatKey` for workflows.
  Binding and workflow keys are lower-case (`^[a-z][a-z0-9_-]{0,63}$`).

## Template

`create-bytechef-app` (`sdks/frontend/automation/app/create/`, npm workspace member, same version as `@bytechef/app`)
scaffolds a TanStack Start app from the vendored `create/template/`. Design:
`docs/superpowers/specs/2026-10-07-apps-template-design.md`. The point of the template is that it is safe by default
and extensible by an AI coding agent: `AGENTS.md` says to discover with `npm run -s bytechef:status` and to reach
third-party systems only through `@bytechef/app`.

- **Two server modules, development only.** `src/server/bytechef.ts` holds only the module-level
  `bytechefMiddleware()` and no `createServerFn`. `src/server/dev.ts` holds the ByteChef-calling server functions
  (`getAppOverview`, `tryAction`); `src/server/overview.ts` is the pure `buildOverview` mapping. Every client path to
  `dev.ts` is gated on `import.meta.env.DEV`: the route's loader imports it dynamically behind that check and the route
  picks the overview component on DEV, so neither the server functions nor the overview UI is in a production build,
  which renders `#placeholder`. **Why two modules and no inline gate:** Start's compiler registers only module-level
  `x = createServerFn(…)` declarations, and it registers every server function of any module the app imports, so a
  `createServerFn` cannot be declared conditionally in place. A dynamic import whose condition is
  `import.meta.env.DEV` is replaced by `false` and eliminated at build time. `NODE_ENV=development vite build` is the
  one way to ship the dev code, and `npm run check` catches it.
- **The leak check** (`scripts/check-bundle.mjs`, run by `npm run check` after `tsc` and `vite build`, and by the
  Dockerfile build) derives the dev-only server function names from `export const x = createServerFn` in `dev.ts`
  (it fails if that file is missing or declares none, so keep it) and fails when any server build file
  contains `<name>_createServerFn_handler` or imports a chunk whose base name is exactly `dev` (`./dev-<8-char
  hash>.mjs`, so a user `dev-utils.ts` does not match), or when no server build exists. It scans every build output
  that exists — client `.output/public`, `.vercel/output/static`, `dist/client`; server `.output/server`,
  `.vercel/output/functions` (Nitro writes the `.vercel/output` pair when `VERCEL=1`) — so a stale directory can only
  add failures, and the success line names the directories it scanned. It also fails when the server
  build registers any of the eleven public ready-made server functions from `@bytechef/app/tanstack-start/server-fns`
  (`<name>_createServerFn_handler`; importing one registers all eleven) unless `package.json` has
  `"bytechef": {"allowReadyMadeServerFns": true}`, which turns the failure into a warning; the template does not set
  it. The key values are those of every variable whose name ends with `BYTECHEF_APP_KEY` (so `VITE_BYTECHEF_APP_KEY`
  too), taken from `process.env` **and** from `.env`, `.env.local`, `.env.production`, `.env.production.local` parsed
  with `util.parseEnv`, and must appear in no client file; the client must also contain no `BYTECHEF_APP_KEY` and no
  `/api/app/v1`, and must contain `/_serverFn/` (a sanity check that it is a Start client build). Client code therefore
  never names the variable; setup help lives in the README and in server-side errors.
- **`bytechef:status` contract** (`node scripts/bytechef-status.mjs`; the script loads `.env.local` itself with
  `util.parseEnv`, as the bundle check reads env files, and when the file is absent writes one stderr line instead of
  Node's `--env-file-if-exists` notice): JSON on stdout,
  diagnostics on stderr, exit 1 on a missing URL or key (the SDK's configuration message) or a ByteChef error
  (`BytechefAppError` code and message), with stdout empty. It is built from `/connections` first, so every allowed
  action and tool is listed even when the component is unavailable (`componentVersion: null`, `inputSchema`/
  `parameters` null; tools carry the server's flat name). `actionSchemas` is false when `/actions` failed in any way (a
  404 from an older server, a 500, a network error), and actions are then listed by name with `inputSchema: null`;
  the script adds one stderr line `Action schemas unavailable: <code or message>` and still exits 0. The development
  overview degrades the same way. A variable already in the
  environment, even an empty one, wins over `.env.local`, as it did under Node's `--env-file`.
- **Dependencies.** Every dependency is an exact version (an upstream minor release must not break every new app); the
  scaffolder rewrites only `@bytechef/app`, to its own version. `create/template/package-lock.json` locks the
  transitive dependencies; `npm run template:lock` (workspace root, `scripts/template-lock.mjs`) regenerates it from
  the template's `package.json` **without** `@bytechef/app` (published in lockstep with the scaffolder, so it cannot
  be locked ahead), and the app's first `npm install` adds that one entry and changes no locked version. The
  scaffolder keeps the lockfile for npm and deletes it for pnpm, yarn and bun with a one-line notice; with
  `--no-install` the next steps say to install before deploying, because the Dockerfile's `npm ci` needs the lockfile
  to list `@bytechef/app`. `template:lock` runs a real `npm install` plus `npm prune` (a `--package-lock-only` run left
  an `extraneous` entry) against `https://registry.npmjs.org/` with `npm_config_registry` stripped, and the
  `check:template` lock step fails on any `resolved` host other than `registry.npmjs.org`. The shadcn components import `cn` from the `cn` package
  and `Slot` from the unified `radix-ui` package, not `@/lib/utils` or `@radix-ui/react-slot`, so both stay
  dependencies; `components.json` still lists the `@/lib/utils` alias, which does not exist.
- **The generated app needs Node.js >= 22.18.0** (template `engines`; the scaffolder warns on older Node). Nitro's
  development server fails on Node 22.17 and earlier with `Vite environment "ssr" is unavailable`; 22.18.0 onward and
  Node 24 pass `check:template`. CI's `node-version: 22` resolves to the newest 22.
- **Hosting is Nitro v3 (beta).** `nitro/vite` produces `.output`; `start` is `node .output/server/index.mjs` (honours
  `PORT`/`HOST`); `vercel.json` sets `"framework": "tanstack-start"` and `"buildCommand": "npm run check"`, so every
  Vercel deploy typechecks, builds and runs the leak check against `.vercel/output`, and a failing check fails the
  deploy. The Dockerfile uses `node:24-alpine` in both stages; with a lockfile it runs `npm ci` and, if that fails,
  prints `package-lock.json does not match this npm version; falling back to npm install (locked versions are kept)`
  and runs `npm install`; without one it runs `npm install`. It runs the bundle check in its build stage, and the
  runtime stage runs as `USER node`. There are no Google Fonts. `AGENTS.md` rule 9 still has
  the agent run `npm run check` for local work.
- **The scaffolder.** No secret on the command line: there is no key flag, and `BYTECHEF_APP_KEY` is copied only from
  the environment, with a notice. `.env.local` is written with mode `0600`. `_gitignore` and `_dockerignore` are renamed
  on copy because npm strips such dotfiles from published packages; for the same reason the package's `files` list
  excludes local build output and `.env*` secrets from `template/`, and the `cpSync` filter skips the same top-level
  names. Under `npm create` the flags must follow `--`; the scaffolder refuses a second positional argument and says
  so, and it blames npm for a swallowed flag only when that flag did not reach it: `npm_config_url` non-empty, or `npm_config_install` set at all (npm writes a swallowed `--no-install` as the empty string). `--url` must be http(s); a line break in the URL or the key is refused before anything is written; `@scope/name` creates `name/` with the scoped package name. `--help`/`-h` prints the usage. Next
  steps print `npm run -s bytechef:status` for npm only (`-s` is npm's) and `<manager> run bytechef:status`
  otherwise. The workspace `.gitignore` (`sdks/frontend/automation/app/.gitignore`) carries `!create/bin/` because
  the repo-root `.gitignore` ignores `bin/`.
- **Proof in CI.** `npm run check:template` (`scripts/check-template.mjs`, mock server `scripts/mock-bytechef.mjs`,
  fixture `scripts/fixtures/app.json`) in the `sdk-app` job: it checks the formatting of the template's own files (the workspace `.prettierignore` lists the generated and starter-only ones; `npm run format` checks the same set), packs both packages, scaffolds with
  `npm exec --package=<tgz>`, checks the scaffolded lockfile against the pins and the public registry
  (`scripts/template-lockfile.mjs`; a pin bumped without `template:lock` fails), installs from the tarball and checks
  the install changed no locked version and added only `@bytechef/app` and optional peer entries (nested removals are
  reported, not failed), checks `npm ci --dry-run` passes, runs the app's `npm run check`,
  compiles every ```ts/```tsx
  block of the scaffolded `AGENTS.md` (written to `src/agents-recipes/`, `@ai-sdk/anthropic` stubbed, `ai`/`zod`
  installed `--no-save`, `tsc --noEmit`, then deleted), proves a route importing a ready-made server function and a
  `NODE_ENV=development` build each fail that check, and drives `bytechef:status` (including `/actions` answering
  404 and 500), the production server and the dev server against the mock (which fails the run on any POST). Linux and macOS only; needs registry access.
- **Known gaps.** Lockfiles written by npm 11 drop an optional nested peer entry (`nitro/node_modules/lru-cache`,
  an optional peer of `unstorage`) that npm 10's `npm ci` requires (`Missing: lru-cache@… from lock file`), and npm
  10's `npm install` adds it back; no single lockfile satisfies both. The template lockfile is written by npm 11, the
  Dockerfile uses Node 24 and falls back to `npm install` when `npm ci` refuses the lockfile, and CI's `sdk-app` job
  (Node 22) tolerates the re-added optional peer. pnpm, yarn and bun scaffolds resolve their transitive dependencies fresh (the npm lockfile is
  removed for them), and their Docker builds have no npm lockfile, so the Dockerfile's `npm install` lets those float
  too. The devtools plugin's event bus listens on `localhost:4206`, only in development, and falls back to an OS-assigned
  port when 4206 is taken, so two dev servers run side by side (each opens one more local port). There
  is no sign-in yet, so anything a page shows is visible to whoever can open the app.

## Configuration

`bytechef.app.*` binds through **`ApplicationProperties`** (prefix `bytechef`, `ignoreUnknownFields=false`), as a
nested `App` class: `Execution{concurrencyLimit, timeout}`, `Proxy{maxRequestBytes, maxResponseBytes, timeout}` and
`WorkflowRun workflow{defaultWait, maxWait, runRetentionDays, runRetentionCron}`. A standalone
`@ConfigurationProperties` for `bytechef.app` would fail startup. No Java field initializers; defaults live in
server-app's `application-bytechef.yml` (`ApplicationPropertiesTest` covers them): execution concurrency limit 10 and timeout 60 s; proxy
10485760 request bytes, 10485760 response bytes, 30 s timeout; workflow default wait 30 s, max wait 60 s, run retention
90 days, retention sweep cron `0 40 2 * * *` (the `@Scheduled` placeholder repeats
that cron as its default so contexts without the yml still boot).

## Open items an engineer must not trip over

- Response-spool write failures inside `http.execute` still classify as 502, and header values with chars above
  0xFF pass validation and the JDK rejects them (500, not 400). Both need a platform change.
- A non-App `ConfigurationException` from `executeAuthorizationApply` reaches the app as 422 `COMPONENT_ERROR` with
  the cause's `toString()` (same channel as the action path).
- Lane permits: the permit wait and the run share one deadline, so the timeout bounds end-to-end latency; a call that
  ignores interrupts keeps its permit until it returns. A per-app cap is a follow-up. `InterruptedException` is reported as a timeout. Refused attempts (allow-list or availability) are not
  recorded in the invocation log.
- `getTools`/`getBindings`/`getActions` read credential-store state per binding and list the operations of
  unavailable connections (see Action listing).
- `uk_app_workspace_environment_name` does not bite when `workspace_id` is null; apps always have a workspace, but
  nothing enforces it in the schema.
- `AppConnection.getAppId()` and `setProxyAllowedMethodList(null)` are NPE hazards (brief-mandated shape); `bind()`
  with null proxy methods NPEs.
- A bearer whose tenant id contains characters outside `[A-Za-z0-9_]` (for example `base64("!!!:abc")`) is answered
  **403**, not 401, on every `/api/**` path of every key type: `TenantAwareApiKeyAuthenticationFilter`
  (`platform-security-web-api`, the MCP filter, untouched by this work) wraps the request in
  `TenantContext.runWithTenantId` before any key-type filter runs, and its `IllegalArgumentException` surfaces as an
  error dispatch. `AppApiKeyAuthenticationConverter`'s own tenant validation is correct but is not reached for that
  input on the monolith. The fix belongs in that filter (fall through on an invalid tenant id).
- The touch write's `REQUIRES_NEW` begin/commit failures escape `AppServiceImpl`'s catch (500); `AppPrincipal.current()`
  ignores `isAuthenticated()`; `AppAuthenticationToken.getName()` returns the principal's record `toString`, which
  matters if auditing ever reads it.
- `AppSecretKeyDTO`, `AppService.CreatedApp`/`RotatedKey`, `AppSecretKeys.GeneratedKey` and
  `AppGraphQlController.AppSecretKeyResponse` redact the key in `toString`. The generated `AppSecretKeyModel` (REST)
  cannot be edited and still prints it; do not log it.
- The connection-delete DTO's used-flag builds description strings and fetches the app per binding on list pages.
- Pre-existing and out of scope: `TrialFilter` (`billing.enabled`) re-runs the chain under a session tenant when a
  session cookie accompanies an API key; AWS temp storage keys by name without a UUID; the OAuth2 refresh race.
- Push to the app's `url` when scheduled results are ready (spec Decision 5) is a later follow-up: today the app reads
  them by calling the read endpoint.
- Client: the Activity tab accumulates pages through an effect (a focus refetch can misorder rows; `useInfiniteQuery`
  is the follow-up); `?appId=` is read on mount only.
