---
name: ByteChef App Builder
description: This skill should be used when the user asks to "create a ByteChef app", "set up my app with ByteChef", "bind a connection to my app", "give my app a workflow", "put the ByteChef app key in .env.local", "call a ByteChef API endpoint", "run something on a schedule", "run a background job", or works in an app made from the create-bytechef-app template and wants Claude to create the app, bind its connections, and build, deploy and bind the workflows it calls.
transports:
  required: [cli]
  optional: [mcp]
---

# ByteChef App Builder

An app made from the `create-bytechef-app` template reaches third-party systems only through ByteChef, with one app
key. This skill runs the loop that sets it up: create the app and put its key in `.env.local`, build the workflows
behind the API endpoints the app calls, and bind what the app needs. Everything runs in ByteChef's DEVELOPMENT
environment; staging and production stay a person's decision. The template's own `AGENTS.md` carries the same loop
for agents without this plugin.

## How the app reaches ByteChef

For each thing the app needs, take the first option that fits:

1. **API Platform endpoint** — the default, for logic that answers within a request.
2. **Call-workflow binding** — for long-running runs, approval-waiting runs, and background one-off runs the app
   starts and does not wait for (`waitSeconds: 0`, then poll the run).
3. **Direct action or tool** — a single operation where a workflow would be overkill, or tools for an app that runs
   its own LLM.
4. **Proxy** — last resort, for a raw API call no action covers.

Going past option 1 states why, in the reply to the user. An endpoint keeps the logic in a ByteChef workflow an admin
can see, change, disable and reuse; an action called from app code hard-wires it into the app.

| The app needs… | Build | App uses |
|---|---|---|
| Logic answering within a request | `apiPlatform/v1/newApiRequest` workflow → collection endpoint, bind the collection | `api.call` |
| A long or approval-waiting run | Call-workflow workflow, bind it | `workflows.execute` + poll |
| A one-off job it starts and doesn't wait for | Call-workflow workflow, bind it | `workflows.execute` with `waitSeconds: 0` + poll |
| Periodic work | Schedule-triggered workflow writing to a data table + an API endpoint reading it | `api.call` on the read endpoint |
| One operation, nothing else | — | bound action |
| Its own local LLM calling tools | — | bound tools |
| A raw API call no action covers | — | proxy |

Scheduled work runs on its own; the app never starts it. A background run is started by the app, once, on demand.

## One-time setup, by a person

- The CLI manages the app: it needs an automation API key created in DEVELOPMENT.
- The management MCP server builds and deploys workflows: an admin API key created in DEVELOPMENT and the header
  `X-ENVIRONMENT: DEVELOPMENT` (see the **ByteChef Management MCP Setup** skill).
- Sign-in (OAuth) connections are created by a person in the ByteChef UI; Claude cannot create them.

## The app

<!-- transport: cli uses: configure, automation app create, automation app link, automation app list, automation app status, automation connection list, automation app bind connection, automation app unbind connection, automation app bind api-collection, automation app unbind api-collection, automation app key rotate -->
Configure the CLI once with the DEVELOPMENT automation API key, then run the app commands in the app's directory.
Every `automation app` command and `automation connection list` refuses any environment but DEVELOPMENT.

```bash
bytechef configure --profile dev --host https://bytechef.example.com --token <AUTOMATION_API_KEY> \
  --environment DEVELOPMENT --workspace-id 1
bytechef automation app create --name my-app --profile dev
bytechef automation connection list --component-name hubspot --profile dev
bytechef automation app bind api-collection --api-collection-id 9 --profile dev
bytechef automation app bind connection --binding-key crm --component-name hubspot \
  --actions createContact --tools searchContacts --profile dev
bytechef automation app status --profile dev
```

- `create` writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `.env.local` (owner-only, other lines kept) and
  `.bytechef/app.json` (no secret; commit it). It never prints the key; never print or echo it yourself. If a file
  write fails after the app was created, the error names the app id: `bytechef automation app link --app-id <id>`
  restores `.bytechef/app.json` and `bytechef automation app key rotate` writes a new key.
- Later commands find the app through `.bytechef/app.json`, walking up from the current directory, or take
  `--app-id`. `bytechef automation app list` lists the workspace's apps.
- `bind api-collection` lets the app's key call every endpoint of that collection; the `bindCommand` that
  `createAppApiCollection` returns is this command.
  `bytechef automation app unbind api-collection --api-collection-id 9` removes it. `status` lists the bound collections with `available`, not their endpoint paths.
- `bind connection` makes the binding exactly what it names: an omitted `--actions`, `--tools` or `--proxy` means
  none, so repeat everything you still want. Actions are for the app's code, tools for the app's own AI. With exactly
  one usable connection of the component the CLI picks it; otherwise it lists the candidates and you pass
  `--connection-id`. Unknown action or tool names fail with the allowed ones listed.
  `bytechef automation app unbind connection --binding-key crm` removes a binding.
- Then `npm run -s bytechef:status` in the app shows what it may call.
<!-- /transport -->

## API endpoints — the default

<!-- transport: mcp uses: createAppApiCollection, createAppApiEndpoint, updateAppApiCollectionVersion, listAppApiCollections, listConnections edition: ee -->
An app's code calls ByteChef through API Platform endpoints first; a Call-workflow binding, a direct action or tool,
or the proxy only when an endpoint does not fit, and the reason is stated. An endpoint's workflow is triggered by
`apiPlatform/v1/newApiRequest` and ends in `apiPlatform/v1/responseToAPIRequest`. The app calls it with
`api.call({method, path: 'v{collectionVersion}/{contextPath}/{endpointPath}'})`. These tools run in DEVELOPMENT only.

1. Create the project with `createProject` (pass `workspaceId`), or reuse the app's project.
2. Build each endpoint workflow with `buildWorkflow` and `kind: API_ENDPOINT`. It reports each saved workflow with its
   uuid, or names what is missing; then call it again naming that `workflowId`.
3. Publish with `publishProject`.
4. Create the collection with `createAppApiCollection` (`projectId`, `projectVersion` = the published version, `name`,
   `contextPath`), once per app area.
   - `deployed`: the collection is enabled; the result carries `bindCommand`.
   - `arming failed`: the collection is saved but disabled; the result still carries `bindCommand`. Do not create it
     again: keep creating its endpoints and run `bindCommand`, and tell the user that a person holding
     `DEPLOYMENT_EDIT` must enable it. Calls to its endpoints answer 409 until then.
5. Add each endpoint with `createAppApiEndpoint` (`apiCollectionId`, `name`, `httpMethod`, `path`, `workflowUuid`).
   - `deployed`: the endpoint is live. Its `url` is `/api/app/v1/api/v{n}/{contextPath}/{path}`; the app passes the
     part after `/api/app/v1/api/` as `api.call`'s `path`.
   - `unresolved`: nothing was created. Pick from the listed candidates (`listConnections` shows every connection
     with `usable`) and call again with `connections: [{workflowNodeName, key, connectionId}]`.
   - `arming failed`: the endpoint is saved but disabled. Do not call `createAppApiEndpoint` again for it; tell the
     user that a person holding `API_PLATFORM_EDIT` on the endpoint and `DEPLOYMENT_EDIT` on the collection's
     deployment must enable it.
6. Run the returned `bindCommand` in the app's directory.

To change endpoint logic: `buildWorkflow` naming the DRAFT workflow → `publishProject` →
`updateAppApiCollectionVersion` (`apiCollectionId`, the new `projectVersion`). It keeps the collection version and
the endpoints, refuses when an endpoint's workflow is missing from the new version unless
`removeMissingEndpoints: true`, and reports `removedEndpointNames`, `disabledEndpointNames`, `missingConnections` and
`missingInputs`; resolve what disabled an endpoint before relying on it. The app's binding stays valid. New
endpoints in the new version are added with `createAppApiEndpoint`. `listAppApiCollections` lists the collections
with their endpoints (method, path, `workflowUuid`, `enabled`, `url`) and `bindCommand`.

If `buildWorkflow` is not registered, a ByteChef admin must enable Copilot (`bytechef.ai.copilot.enabled: true`).
<!-- /transport -->

## Scheduled work

<!-- transport: mcp uses: createAppDataTable, deployProject, createAppApiCollection, createAppApiEndpoint edition: ee -->
Periodic work runs on a schedule trigger and writes its results to a ByteChef data table; the app reads them through
an API endpoint whose workflow queries that table. The app never starts a scheduled workflow.

1. Create the table with `createAppDataTable` (`baseName` in snake_case, `columns: [{name, type}]`; types `STRING`,
   `NUMBER`, `INTEGER`, `DATE`, `DATE_TIME`, `BOOLEAN`). It refuses a `baseName` that already exists; on a re-run,
   reuse that table.
2. Build the scheduled workflow with `buildWorkflow` and `kind: SCHEDULED`: a schedule trigger, then
   `dataTable/v1/createRecords` or `dataTable/v1/updateRecord` with `table` = the `baseName`.
3. Build the read workflow with `buildWorkflow` and `kind: API_ENDPOINT`, reading with `dataTable/v1/findRecords`.
4. Publish with `publishProject`.
5. Deploy with `deployProject`, passing the scheduled workflow's uuid in `enabledWorkflows`, so only the schedule you
   meant starts. `deployProject` does not touch API endpoint workflows; they run on the collection's own deployment.
6. Create the collection with `createAppApiCollection` (or reuse the app's collection) and the read endpoint with
   `createAppApiEndpoint`, then run the collection's `bindCommand` in the app's directory. On `arming failed` do not
   create it again; a person must enable it, as in the API endpoint steps.

To change a scheduled workflow: `buildWorkflow` naming its DRAFT workflow → `publishProject` → `deployProject` with
the scheduled workflow's uuid still in `enabledWorkflows`. A changed read workflow follows the endpoint change loop
(`updateAppApiCollectionVersion`).

Schedules are enabled only in DEVELOPMENT by this loop; staging and production stay a person's decision.
<!-- /transport -->

## Long-running, approval and background runs

<!-- transport: mcp uses: deployProject, listConnections, listAppCallableWorkflows edition: ee -->
Use a Call-workflow binding when an API endpoint does not fit: a run that takes long, waits for an approval, or runs in
the background, started by the app, which does not wait (`workflows.execute(key, input, {waitSeconds: 0})` answers at
once with a `runId`; the app polls `workflows.getRun`). A result that is large or must outlive run retention is also
written to a data table and read through an API endpoint. An app calls a ByteChef workflow only when the workflow is
app-callable: its trigger is the Call-workflow trigger
(`workflow/v1/newWorkflowCall`) with an `inputSchema` (a string holding a JSON Schema) describing every value the app
sends. A final `workflow/v1/responseToWorkflowCall` task with an `outputSchema` (also a string holding a JSON Schema)
describes what the app gets back; without it the app receives the job outputs. The app loop runs in DEVELOPMENT only:
the MCP client sends `X-ENVIRONMENT: DEVELOPMENT` with an admin API key created in DEVELOPMENT, and these tools refuse
any other `environment`.

1. Create the project with `createProject`, always passing `workspaceId`.
2. Build each workflow with `buildWorkflow` and `kind: APP_CALLABLE`. It reports each saved `workflowId` as app-callable
   or names what is missing; then call it again naming that `workflowId`.
3. Publish with `publishProject`. `deployProject` refuses a project with unpublished changes.
4. Deploy with `deployProject`. It keeps one DEVELOPMENT deployment per project, picks a connection for each slot
   when exactly one usable candidate exists and otherwise lists the candidates (pass them in `connections`;
   `listConnections` shows every connection with `usable` and `aiProvider`), and enables the app-callable workflows.
   Its result gives, per app-callable workflow, the `bytechef automation app bind workflow` command to run in the
   app's directory, and the `draftWorkflowId` for the next change.
5. To change a workflow: `buildWorkflow` naming the `draftWorkflowId` → `publishProject` → `deployProject`. The
   app's binding stays valid. `listAppCallableWorkflows` lists what is deployed and callable.

If `buildWorkflow` is not registered, a ByteChef admin must set `bytechef.ai.copilot.enabled: true` with a chat model
provider (optionally chosen with `bytechef.ai.copilot.provider`).
<!-- /transport -->

## Binding a deployed workflow

<!-- transport: cli uses: automation app bind workflow, automation app unbind workflow, automation app status -->
Bind the deployed workflow under the key the app's code passes to `workflows.execute`:

```bash
bytechef automation app bind workflow --workflow-key onboard-customer --deployment-id 7 \
  --workflow-uuid 3f1c2a9e-0d4b-4c55-9a43-6a1f0e2b7c11 --profile dev
```

`bytechef automation app status` shows each workflow binding with `available`;
`bytechef automation app unbind workflow --workflow-key onboard-customer` removes one.
<!-- /transport -->
