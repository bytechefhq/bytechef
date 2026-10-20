# Apps — Workflow Bindings and the SDK — Design

## Summary

This spec extends Apps (`2026-10-06-apps-governed-ai-built-apps-design.md`, sub-project A, landed) in two ways:

| # | Sub-project | Delivers |
|---|-------------|----------|
| A4 | Workflow bindings (server + admin UI) | An app can bind deployed **Call workflow** workflows and run them through `/api/app/v1`, governed like its connections: allow-listed by an admin, logged per app, stopped by disable or key rotation. Long runs and approvals return a run handle the app polls. |
| B | SDK | `@bytechef/app`: a framework-agnostic, server-only client with subpath adapters, `@bytechef/app/tanstack-start` (plus its ready-made server functions in `@bytechef/app/tanstack-start/server-fns`) and `@bytechef/app/ai-sdk`. |

It replaces the Apps spec's "Design — B. SDK" section. A4 is built first: B's workflow API is generated from the
`openapi.yaml` A4 changes. Each has its own implementation plan.

## Decisions

Made with the user on 2026-10-06:

1. **Workflows are governed bindings, not webhooks.** An app could already `fetch` a workflow's webhook, but
   `/webhooks/**` is permit-all (the Webhook trigger's only check is an optional `X-Csrf-Token` header), the URL is
   the credential, and the call is neither tied to the app nor stopped by disabling it. That contradicts the Apps
   premise that ByteChef is the app's only path to data. Apps bind **Call workflow** (`workflow/newWorkflowCall`)
   workflows, the same rule MCP servers use for workflows-as-tools.
2. **Long runs and approvals return a run handle.** Execute waits a bounded time; a finished run answers 200, an
   unfinished or approval-waiting run answers 202 with a `runId` the app polls.
3. **One npm package with subpath exports**, not the two packages the Apps spec named: one install and one version
   for coding agents and the template; a later Next.js adapter is another subpath.
4. **A Vercel AI SDK adapter** (`@bytechef/app/ai-sdk`), because coding agents building AI features in TanStack
   Start reach for the AI SDK by default, and hand-written glue (parsing schemas, mapping flat names, turning
   approvals into model messages) is easy to get subtly wrong.
5. **Generic ready-made server functions ship**, as the Apps spec says, with the trade-off stated in the package
   README and the template's `AGENTS.md` (see the TanStack Start adapter).

## Non-goals

- **Governing the model call.** The app brings its own LLM key; the AI gateway stays out of scope. ByteChef sees and
  controls every action, tool, proxy and workflow call, but not prompts, model spend or guardrails. Routing the model
  call through the AI gateway is the natural phase-2 follow-up. The template's `AGENTS.md` tells agents not to send
  connection data to a model the company has not approved.
- **An approve operation in the app API.** A run waiting on an approval is resolved on the approval channels (chat,
  Slack, email, the hosted form). The API offers no approve call, but the `approvalFormUrl` it returns is the hosted
  form's anonymous capability link (`/job/resume/**` is permit-all, as it is for MCP clients and e-mail recipients):
  whoever opens it may decide, and the app is expected to show it to a person. An app that opens the link itself
  can therefore resolve its own run; a per-binding switch to withhold the link is a follow-up if a customer needs it.
- **Cancelling runs** from the app, and cancelling runs on unbind.
- **Webhook-triggered workflows.** They stay public endpoints for external systems.
- **Adapters for other frameworks or LLM libraries** (Next.js, Hono, Mastra, LangChain.js, OpenAI Agents SDK). Apps
  on those use the core client; an adapter is a later subpath if demand shows.

## Design — A4. Workflow bindings

### What an app can bind

A workflow from a **project deployment in the app's own workspace and environment** whose trigger is
`workflow/newWorkflowCall`. That trigger is `TriggerType.CALLABLE` with `workflowSyncExecution(true)`; its
`inputSchema` property is the JSON Schema the workflow author builds in the editor.

### Model

New tables in `automation-app-service`'s changelog:

**`app_workflow`**: `id`, `app_id` (FK to `app`, cascade delete), `workflow_key`, `project_deployment_id`,
`workflow_uuid`, `version`. Unique `(app_id, workflow_key)`. No audit columns, like `app_connection`: binding changes
are audited through `AppAuditPublisher` events. No foreign key on `project_deployment_id`: deleting a deployment is
not blocked (see Availability).

- `workflow_key` follows the connection binding-key rule (`AppBindingKeys`) and is unique per app, including after
  normalisation to its flat upper-snake form, so it can name an AI SDK tool (see the AI SDK adapter).
- The binding stores `(project_deployment_id, workflow_uuid)`, never a version-specific workflow id. Every call
  resolves the current workflow through `ProjectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId,
  workflowUuid)`, the lookup webhooks use, so a project redeploy changes what the app runs with no rebinding. This
  deliberately differs from MCP, which pins a dedicated deployment to one project version.

**`app_workflow_run`**: `id`, `run_id` (a UUID string, the public `runId`, unique), `app_id` (FK to `app`, cascade
delete), `workflow_key`, `job_id`, `created_date`; indexes on `app_id` and `created_date`. This is the authorisation
record for polling: an app sees only runs it started. It is a table of its own because the tool invocation log is
written asynchronously, and authorisation cannot depend on it.

Rows are insert-only and one is written per run, so they are purged like the tool invocation log: an
`AppWorkflowRunRetentionJob` deletes rows older than `bytechef.app.workflow.run-retention-days` (90) on a daily cron
(`bytechef.app.workflow.run-retention-cron`). A run whose row or job is gone polls as 404.

### Availability

A binding is **unavailable** when the deployment or workflow no longer exists, the deployment or the project
deployment workflow is disabled, the deployment is in another workspace or environment than the app, the workflow's
trigger is no longer `workflow/newWorkflowCall`, or the trigger's `inputSchema` cannot be read (a workflow whose
declared input contract is unreadable must not accept arbitrary input). `AppCallableWorkflowResolver` is the one
definition of "callable": binding, listing and execution all use it, so an unreadable schema also keeps a workflow out
of the bindable list and fails a bind with 400 `APP_WORKFLOW_NOT_BINDABLE`. Listings return an unavailable binding
with `available: false` (the admin UI marks it "Unavailable", the same rule as connection bindings whose component
cannot be resolved); executing it answers 409 `APP_WORKFLOW_UNAVAILABLE`. The app-facing listing shows an unavailable
binding with its key as `name` and no description or schemas. The facades also catch an unreadable schema themselves
(execute answers 409, the listing reports `available: false` with label and description kept), as defence in depth
for a schema that was readable when the resolver ran.

Deleting a project deployment is **not** blocked by app workflow bindings, unlike connections and
`ConnectionUsageChecker`: losing a workflow cannot leak a credential, so the guard is not worth its coupling.

### Admin

- App detail gains a **Workflows** tab: bind (deployment, then Call-workflow workflow, then key), list (key,
  workflow name, deployment, Unavailable marker), unbind.
- GraphQL (`app.graphqls`): `appWorkflows(appId)`, `appBindableWorkflows(appId)`, `bindAppWorkflow`,
  `unbindAppWorkflow`.
- Management REST (`/api/automation/v1/apps`): `GET /apps/{id}/workflows`, `PUT /apps/{id}/workflows/{workflowKey}`
  (body `{projectDeploymentId, workflowUuid}`), `DELETE /apps/{id}/workflows/{workflowKey}`. The deploy CLI (D) uses
  these.
- Everything is gated through a facade of its own, `AppWorkflowFacade` (`APP_VIEW` to list, `APP_MANAGE` for the
  rest), beside `AppFacade`; the GraphQL and REST controllers own no authorization. The bindable list only offers
  deployments of projects the caller can see, and binding a deployment of an invisible project is refused. Audit
  events `APP_WORKFLOW_BOUND` (with the previous deployment and uuid when a key is re-pointed) and
  `APP_WORKFLOW_UNBOUND` (only when a row was deleted).

### App-facing endpoints

Added to `automation-app-public-rest/openapi.yaml`, under `/api/app/v1`, authenticated with the app key:

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/workflows` | The app's workflow bindings: `workflowKey`, `name` (workflow label), `description`, `inputSchema` (from the trigger), `outputSchema` (from `responseToWorkflowCall`, when present), `available`. Schemas are JSON objects, not strings, and both are `nullable` in the OpenAPI: they are null for an unavailable binding and for an unparseable `inputSchema`. |
| POST | `/workflows/{workflowKey}` | Body `{input, waitSeconds?}`. Starts a run and waits up to `waitSeconds` (default 30, capped by `bytechef.app.workflow.max-wait`, 60 s in yml). |
| GET | `/workflow-runs/{runId}` | A run this app started. |

Both run endpoints answer the same `WorkflowRun` body:

```json
{"runId": "…", "workflowKey": "approve_refund", "status": "COMPLETED", "result": {}}
```

- `status` is `RUNNING`, `APPROVAL_REQUIRED`, `COMPLETED`, `FAILED` or `CANCELLED`, mapped from the job status:
  `CREATED` or `STARTED` gives `RUNNING`; `STOPPED` with a pending approval gives `APPROVAL_REQUIRED`; `STOPPED`
  otherwise gives `CANCELLED`; `COMPLETED` and `FAILED` map to themselves.
- On `COMPLETED`, `result` is the `responseToWorkflowCall` output when the workflow responds, otherwise the job
  outputs (MCP's rule). On `FAILED`, `error: {message}`. On `APPROVAL_REQUIRED`, `approvalFormUrl`: the same hosted
  form MCP returns, so the app can show it to its user.
- Execute answers **200** when the run reached `COMPLETED`, `FAILED` or `CANCELLED` within the wait, and **202** when
  it is `RUNNING` or `APPROVAL_REQUIRED`. A workflow failure is run state, not an HTTP error; HTTP errors mean no run
  started.
- `GET /workflow-runs/{runId}` answers 404 `APP_WORKFLOW_RUN_NOT_FOUND` for an unknown id **and** for another app's
  run, so ids reveal nothing.

### Execution

`AppWorkflowExecutionFacade.executeWorkflow(workflowKey, input, waitSeconds)`, a facade of its own beside
`AppExecutionFacade`:

1. Resolve the app (it must be enabled) and the binding by `(appId, workflowKey)`; 404 `APP_WORKFLOW_NOT_FOUND` if
   absent.
2. Resolve the current workflow id and check availability; 409 `APP_WORKFLOW_UNAVAILABLE`.
3. Validate `waitSeconds` (0 to `max-wait`; omitted means `default-wait`), then validate `input` against the trigger's
   `inputSchema` with the networknt `json-schema-validator` already on the classpath (the LLM component's
   `JsonSchemaValidationAdvisor` uses it). Either failure answers 400 `APP_WORKFLOW_INPUT_INVALID` with the
   validator's messages, and no run starts.
4. Create the job exactly as MCP and webhooks do: `principalJobFacade.createJob(new JobParametersDTO(workflowId,
   inputs), projectDeploymentId, PlatformType.AUTOMATION)`, where `inputs` is the deployment workflow's inputs plus
   `{<triggerName>: input, __triggerName: <triggerName>}`. The job principal is the project deployment, so licence
   usage, variables and the Executions list behave as for any deployment run. No `runAsSystem`: `createJob` has no
   authorization check, and the job's `createdBy` stays honest.
5. Insert the `app_workflow_run` row.
6. Wait with `JobCompletionAwaiter.await(jobId, wait)`; `waitSeconds: 0` reads the job's state without waiting.
   Reaching the timeout is not an error: the job keeps running and execute answers 202. Once the job is
   created, a failure to record the run or an awaiter failure other than the timeout propagates to the caller as an
   error with no run id, and the job keeps running (the `executeWorkflow` Javadoc states this contract); the caller
   never learns the `runId` of such a run.

The workflow runs on the normal workers. The request thread only waits on a future, so this path does **not** use
`syncWorkerExecutor`, the actions and tools lane. It does hold a request thread for up to `max-wait`, as sync webhooks
(300 s) and MCP do, with no cap on concurrent waits — a known gap in the same family as A's shared lane.

MCP's result and approval helpers (`fetchCallableResponseOutput`, `readResult`, `isPendingApproval`,
`fetchApprovalFormUrl` and `fetchResumeToken`) move into `WorkflowCallResults` in `platform-workflow-execution-api`,
which both `AutomationMcpToolFacade`
and `AppWorkflowExecutionFacadeImpl` call, so the two paths cannot drift.

`getWorkflowRun(runId)` looks up `app_workflow_run` by `(runId, appId)`, loads the job and maps it as above.

### Governance and logging

- A disabled app gets 403 `APP_DISABLED` on execute and poll (401 at the filter, as today). A rotated key gets 401.
- Unbinding does not cancel running runs; their run rows stay pollable, so the app can learn how they ended.
- Each execute is recorded through `ToolExecutionRecorder` with a new `ToolExecutionSurface.AUTOMATION_APP_WORKFLOW`
  (appended, because ordinals are persisted), kind `WORKFLOW`, `appId`, `workspaceId`, `environment`, `jobId`, and
  the workflow key as `operationName`. The outcome is `ERROR` for a `FAILED` run (with its message),
  `APPROVAL_REQUIRED` for a run waiting on an approval and `SUCCESS` for every other status, `RUNNING` included, since
  the call itself succeeded. Polls are not recorded. The run also appears in the normal Executions list
  under its project deployment.

### Errors added

| Situation | Status | Code |
|-----------|--------|------|
| Unknown workflow key | 404 | `APP_WORKFLOW_NOT_FOUND` |
| Deployment or workflow gone or disabled, trigger not Call workflow, or unreadable `inputSchema` | 409 | `APP_WORKFLOW_UNAVAILABLE` |
| Input fails the trigger's `inputSchema`, or `waitSeconds` out of range | 400 | `APP_WORKFLOW_INPUT_INVALID` |
| Unknown run, another app's run, or a run whose row or job was purged | 404 | `APP_WORKFLOW_RUN_NOT_FOUND` |
| Bind-time: deployment not callable by the app, project not visible to the caller, malformed uuid | 400 | `APP_WORKFLOW_NOT_BINDABLE` |
| Workflow key breaks the key rule | 400 | `APP_BINDING_KEY_INVALID` |
| Workflow key whose flat form collides with another workflow binding | 409 | `APP_BINDING_KEY_ALREADY_EXISTS` |

New `AppErrorType` values are appended after the existing ones.

## Design — B. SDK

### Package

`sdks/frontend/automation/app/` is an npm workspace like `sdks/frontend/automation/chat`. `library/` is the
published package `@bytechef/app` (Vite library mode, `vite-plugin-dts`, vitest, ESM only); `test-app/` is a
minimal TanStack Start app that uses the adapter entry points; Verdaccio serves local publishing.

```json
"exports": {
  ".": "./dist/index.js",
  "./tanstack-start": "./dist/tanstack-start.js",
  "./tanstack-start/server-fns": "./dist/server-fns.js",
  "./ai-sdk": "./dist/ai-sdk.js"
}
```

Four entry points (each with its `types`), plus `./package.json`. The ready-made server functions have an entry of
their own because TanStack Start registers every server function declared in a module the app imports: had they
stayed in `./tanstack-start`, importing only `bytechefMiddleware` would publish all nine, `proxy` included, as RPC
endpoints. The test app's bundle check fails when anything reachable from the built `./tanstack-start` calls
`createServerFn(`.

The core has **no runtime dependencies** and needs Node 20.19+ (the workspace and test app need 22.12+, as the peers
do). `@tanstack/react-start` (`^1.168.0`) and `ai` (`^7.0.0`) are peer dependencies marked optional in
`peerDependenciesMeta`; importing a subpath without its peer fails only in that app. The `@tanstack/react-start` peer
entry is load-bearing: Start's Vite plugin treats a dependency that names it as a peer as a framework package and
compiles it for the client and the server, which is what lets server functions shipped in the package work
(`package.test.ts` pins the entry). The library build is unminified so `dist` keeps the module-level
`createServerFn` declarations the app's compiler reads.

Request and response types are generated from the app-facing `openapi.yaml` with `openapi-typescript` into
`src/generated/` and committed. A `check:api` script regenerates them and fails on a diff, so a server contract
change that is not mirrored in the SDK fails CI. `AppBinding.componentVersion` is nullable in `openapi.yaml`: it is null
when the binding's component can no longer be resolved.

The package is published to npm under `@bytechef`, as `@bytechef/embedded` is, starting at `0.1.0`.

### Core client — `@bytechef/app`

```ts
const bytechef = createBytechefClient(); // or createBytechefClient({url, appKey, fetch, timeoutMs})

await bytechef.connections.list();                                        // AppBinding[]
await bytechef.actions.list();                                            // AppAction[], inputSchema as a JSON Schema
await bytechef.actions.execute('crm', 'createRecord', input, {componentVersion});
await bytechef.tools.list();                                              // AppTool[], parameters parsed
await bytechef.tools.execute('crm', 'searchRecords', input);
await bytechef.tools.executeByName('CRM__SEARCH_RECORDS', input);
await bytechef.proxy('crm', {method: 'GET', path: '/v3/objects/contacts', query: {limit: ['10']}});
await bytechef.workflows.list();
await bytechef.workflows.execute('approve_refund', input, {wait: true, signal});  // WorkflowRun
await bytechef.workflows.getRun(runId);
```

**Configuration and safety**

- `BYTECHEF_URL` and `BYTECHEF_APP_KEY` are read lazily, on the first request, and never under a `VITE_` prefix
  (Vite inlines `VITE_*` variables into the client bundle). Explicit options override the environment, for runtimes
  without `process.env` and for tests.
- Construction throws when `typeof window !== 'undefined'`.
- A missing key fails with a message naming the variable and where a key comes from (the app's Rotate key in
  ByteChef). `timeoutMs` must be positive.
- `workflows.execute` throws a `RangeError`, sending nothing, when `waitSeconds * 1000 >= timeoutMs`: the starting
  request would time out with no `runId` while the run had already started, and a retry would start it twice.
- Every request sends `Authorization: Bearer <key>`; nothing is exchanged, cached or refreshed.

**Conveniences**, the only places the SDK goes beyond the raw API:

- Actions and tools return the unwrapped `result`. `tools.list()` returns `parameters` parsed from the server's JSON
  string into an object. `actions.list()` returns `AppAction[]` (`bindingKey`, `componentName`, `actionName`,
  `description`, `inputSchema`): the generated schema with `inputSchema` typed as the SDK's `JsonSchema`, exported
  from `@bytechef/app`. A server without `GET /actions` answers 404 with no `{code, message}` body, which is raised as
  `BytechefAppError` code `HTTP_ERROR` with status 404.
- `executeByName` maps a flat name to `(bindingKey, toolName)` through the tool listing, cached for 60 s and
  re-fetched once on a miss, so a tool an admin just allowed works without a restart. It never parses names.
- `proxy`: `body` may be a string (sent `UTF8`), a `Uint8Array` (sent `BASE64`) or a plain object (sent as JSON,
  with `contentType: application/json` unless one is given). The result has `status`, `headers`, `text()`, `json()`
  and `bytes()`; a response body that is not text is carried as bytes (`BASE64` on the wire). An upstream 4xx or 5xx
  is the upstream's answer, not a thrown error.
- `workflows.execute` returns the run as the server reports it. `{wait: true}` polls `getRun`, backing off from 1 s to
  5 s (never faster than 100 ms), until the run is `COMPLETED`, `FAILED` or `CANCELLED`, or the caller's
  `AbortSignal` fires; the wait has no limit of its own, so a caller bounds it with the signal. It stops at
  `APPROVAL_REQUIRED` so a page never blocks on a person; `{wait: 'approval'}` waits through approvals too. An aborted
  wait and a failed poll both throw a `BytechefAppError` that keeps the run's `runId`.

**Errors**

- Every ByteChef refusal throws `BytechefAppError {status, code, message}`: `APP_OPERATION_NOT_ALLOWED` (whose
  message lists the allowed operations, so a coding agent can correct itself), `APP_DISABLED`,
  `APP_WORKFLOW_UNAVAILABLE`, `COMPONENT_ERROR`, `PROVIDER_ERROR` and the rest.
- Transport failures, the client timeout (default 70 s, above the server's 60 s execution timeout, and covering the
  body read as well as the headers) and an aborted call throw `BytechefAppError` with `code` `NETWORK_ERROR`,
  `TIMEOUT` or `ABORTED` and no `status`. An error answer without a `{code, message}` body is `HTTP_ERROR`, and a 2xx
  answer whose body is not JSON is `INVALID_RESPONSE`; both carry the status.
- A failed workflow run is data (`status: 'FAILED'`), not an exception.
- **No retries.** Actions are not idempotent, and a silent retry can create a record twice.

**Runtimes**: the client uses only `fetch`, `AbortSignal`, `TextEncoder`/`TextDecoder` and `atob`/`btoa`, so it runs
on Node 20.19+, Vercel's Node and Edge runtimes, Bun and Deno.

### TanStack Start adapter — `@bytechef/app/tanstack-start`

- `bytechefMiddleware(options?)` is function middleware (`createMiddleware({type: 'function'}).server(({next}) =>
  next({context: {bytechef}}))`) that puts a client on `context.bytechef` for the app's own server functions:
  `createServerFn().middleware([bytechefMiddleware()]).validator(…).handler(…)`. The adapter and its docs use
  `.validator`, not the deprecated `.inputValidator`. One middleware, and so one client and one tool cache, serves
  all the ready-made server functions.
- Nine ready-made server functions in `@bytechef/app/tanstack-start/server-fns`, configured from the environment:
  `listConnections`, `executeAction`, `listTools`, `executeTool`, `executeToolByName`, `proxy`, `listWorkflows`,
  `executeWorkflow` and `getWorkflowRun`. Each validates its input shape (plain objects only; a bad shape throws `TypeError`) and delegates
  to the client, so a page can call ByteChef without writing server code. Their results are concrete
  JSON-serialisable types, because Start rejects `unknown`: the package exports `JsonValue` and `JsonSerializable`,
  and `proxy` returns the wire form `{body, bodyEncoding, headers, status}` rather than a `ProxyResult`. The package
  also exports each handler and validator from `@bytechef/app/tanstack-start`, so an app can build its own server
  function from the same parts. Importing any one ready-made function registers all nine; importing only
  `@bytechef/app/tanstack-start` registers none.
- **`executeWorkflow` does not wait.** Start gives a handler no abort signal, so a server-side wait could not be
  stopped. The ready-made function returns the run after the server's `waitSeconds` hold (`RUNNING` or
  `APPROVAL_REQUIRED` for a long run) and the browser polls `getWorkflowRun`. An app that wants a server-side wait
  writes its own server function on `bytechefMiddleware()` and passes a signal to `workflows.execute`.
- **Trade-off, stated in the README and the template's `AGENTS.md`:** anyone who can open the deployed app can call
  the ready-made server functions, with any input the app's allow-list permits. ByteChef still enforces the
  allow-list, but the app adds no constraint of its own. Until app sign-in lands (Apps phase 2), the host must
  restrict who can open the app, for example with Vercel Deployment Protection. App-specific server functions built
  on `bytechefMiddleware()` are the narrower pattern, and the one the template's example page uses: an app that
  imports only `@bytechef/app/tanstack-start` exposes exactly the server functions it declares. An app that needs
  explicit client options (`createBytechefClient({url, appKey, …})`) writes its own server functions on
  `bytechefMiddleware(options)`; there is no factory.
- **Spike verdict (2026-10-07): the package ships its server functions.** A server function defined in the package
  and imported by a fresh TanStack Start 1.168 app compiles for the client (an RPC stub) and the server, over SSR and
  RPC, also when the package is installed from a packed tarball; the peer entry above is what makes it so. Start
  compiles only module-level `x = createServerFn(…)` declarations: an object-literal form fails the build, and a
  declaration inside a function is silently left uncompiled. That rules out a `createBytechefServerFns()` factory,
  hence nine module-level functions. The template (C) therefore writes its own server functions on
  `bytechefMiddleware()` and does not import the ready-made ones: each is a public endpoint, so they stay opt-in in
  `@bytechef/app/tanstack-start/server-fns`.

### AI SDK adapter — `@bytechef/app/ai-sdk`

```ts
const tools = await toAiSdkTools(bytechef, {include: ['crm'], workflows: true});
const result = await generateText({model, tools, prompt});
```

- Returns `Record<name, Tool>`, each built with the AI SDK's `dynamicTool({description,
  inputSchema: jsonSchema(parameters), execute})`, which suits tools discovered at runtime: no Zod and no
  compile-time types. `jsonSchema` validates nothing on the client; ByteChef validates the input. A schema whose root
  is not an object is replaced by an open object, because model providers require an object root. `dynamicTool` and
  `jsonSchema` exist since AI SDK 5; the peer range is `^7.0.0`, the current major, and `ai` needs `zod` in the app.
- Component tools are named by their flat name, and `execute` calls `tools.executeByName`. A flat name is the
  binding key upper-cased, `__`, and the tool name in UPPER_SNAKE. Binding and workflow keys are lower-case
  (`^[a-z][a-z0-9_-]{0,63}$`).
- With `workflows: true`, every available workflow becomes a tool named after its flat upper-snake key, described by
  the workflow's description and typed by its `inputSchema`. `execute` runs `workflows.execute(key, input,
  {wait: true})`. `COMPLETED` returns `result` to the model; `APPROVAL_REQUIRED` returns a short message saying the
  request is waiting on a person, with the form URL; `FAILED` returns the error message so the model can explain it.
- Workflow tool names cannot collide with component tool names: a component tool's flat name always contains `__`
  (`<BINDING_KEY>__<TOOL_NAME>`), while `AppBindingKeys` collapses every run of non-alphanumerics in a key to a single
  `_`, so a normalised workflow key never does. Workflow keys are unique among themselves after normalisation (bind
  enforces it).
- `include` filters by flat name or by binding or workflow key, so an app can expose a subset to the model.
- A `BytechefAppError` thrown inside a tool's `execute` (for example `APP_OPERATION_NOT_ALLOWED`) is returned to the
  model as the tool's error text rather than aborting the generation. A client-side failure (`NETWORK_ERROR`,
  `TIMEOUT`, `HTTP_ERROR`, `INVALID_RESPONSE`) reaches the model as `Error <CODE>: ByteChef is unavailable`, without
  transport details. `ABORTED` and any other error are rethrown, and the AI SDK's abort signal is forwarded to the
  call.

## Testing

**A4 server**

- `AppWorkflowServiceTest`: key rules, flat-name collisions, malformed uuid, workflows that are not callable
  rejected; run records get fresh ids.
- `AppCallableWorkflowResolverTest`: each unavailable cause (other workspace, other environment, disabled deployment,
  disabled deployment workflow, missing deployment, non-Call-workflow trigger, malformed uuid) resolves to empty; the
  workflow id is read on every call, so a redeploy changes what runs.
- `AppWorkflowExecutionFacadeTest`:
  - invalid input answers 400 and creates no job; an unreadable schema answers 409;
  - `waitSeconds` bounds, and `0` reads the job without waiting;
  - status mapping for every job state, including `APPROVAL_REQUIRED` with its form URL and `CANCELLED`;
  - result selection (callable response or job outputs);
  - the job is created as MCP creates it (inputs, principal, platform type);
  - the invocation is recorded with the job id and the outcome.
- Run ownership: another app's `runId`, and a run whose job was purged, answer 404, not 403.
- `AppWorkflowExecutionFacadeIntTest` (Postgres): a run is pollable by the app that started it and not by another;
  deleting the app removes its bindings and runs. `automation-app-service` has no workflow engine on its test
  classpath, so the real engine (a Call-workflow run end to end, and one that pauses on an approval) is exercised by
  the manual end-to-end run per release, not by this module.
- MCP's existing tests stay green after its helpers move to `WorkflowCallResults`; `AutomationMcpToolFacadeTest`
  changes only in wiring.
- A test pins `AUTOMATION_APP_WORKFLOW` as the last `ToolExecutionSurface` value, the `AppErrorTypeTest` count is
  updated, and `AppApiKeyFilterChainTest` covers the new paths (app key only).
- Client: the Workflows tab (bind dialog, Unavailable marker, unbind) and codegen.

**B SDK**, with vitest and a mocked `fetch` (`library/src/**/*.test.ts`) and the test app's bundle check:

- `core/config.test.ts`, `core/config.browser.test.ts`, `core/client.test.ts` and `core/client.browser.test.ts`: the
  key is sent on every request; environment variables are read lazily and explicit options win; construction in a
  browser-like environment throws; a missing key gives the clear message; `timeoutMs` must be positive;
- `core/request.test.ts` and `core/errors.test.ts`: error typing (`BytechefAppError` codes, `NETWORK_ERROR`,
  `TIMEOUT` covering the body read, `ABORTED`, `HTTP_ERROR`, `INVALID_RESPONSE`) and abort;
- `core/connections.test.ts` and `core/tools.test.ts`: result unwrapping, `componentVersion`, `parameters` parsing,
  `executeByName` mapping, its single re-fetch on a miss and `APP_TOOL_NOT_FOUND`;
- `core/proxy.test.ts`: proxy body encoding (string, bytes, JSON) and the response helpers;
- `core/workflows.test.ts`: the `workflows.execute` waiting policy: it stops at `APPROVAL_REQUIRED` by default, waits
  through it with `'approval'`, backs off with delays of at least 100 ms, honours the signal, and keeps `runId` on an
  aborted wait and a failed poll;
- `ai/names.test.ts` and `ai/tools.test.ts`: names, schemas (including the open-object replacement), `include`,
  workflow outcome messages, errors returned to the model, `ABORTED` rethrown and the abort signal forwarded;
- `tanstack/validators.test.ts`, `tanstack/handlers.test.ts` and `tanstack/middleware.test.ts`: validators accept
  plain objects only and throw `TypeError`; handlers delegate and return JSON-serialisable results (`proxy` in wire
  form); the middleware puts the client on context;
- `package.test.ts`: the optional `@tanstack/react-start` and `ai` peers and the four entry points;
- `generated/openapi.test.ts` and `check:api`: the generated types match `openapi.yaml`;
- `test-app/scripts/check-bundle.mjs` (`check:test-app`): `test-app` builds with `vite build`, its client bundle,
  source maps included, contains the server-function RPC stubs, and neither `BYTECHEF_APP_KEY`, `/api/app/v1` nor the
  key's value (CI runs it with a canary key); and in the library's `dist`, the `./tanstack-start` entry and every chunk
  it imports contain no `createServerFn` call while the `./tanstack-start/server-fns` entry declares all nine
  ready-made server functions.

## Plans and order

1. **A4**: server (model, availability, execution, run polling, logging, shared MCP helpers), management REST,
   GraphQL and the admin Workflows tab. One plan.
2. **B**: the SDK. One plan, opening with the TanStack server-function spike. Its workflow API is generated from A4's
   `openapi.yaml`, so it starts once A4's API has landed.

Both run as multi-task work in a worktree branched from `0_732` and land as a regrouped group.
