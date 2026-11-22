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
environment; staging and production stay a person's decision, and outside DEVELOPMENT the CLI can only read, create a
version, deploy, roll back, cancel and record a deployment. The template's own `AGENTS.md` carries the same loop
for agents without this plugin.

Once an administrator activates the app and turns on "Show in App Portal" in Studio (there is no CLI command for
that), workspace members who may use an instance find the app in the App Portal and launch it from there.

## How the app reaches ByteChef

For each thing the app needs, take the first option that fits:

1. **API Platform endpoint** — the default, for logic that answers within a request.
2. **Call-workflow binding** — for long-running runs, approval-waiting runs, and background one-off runs the app
   starts and does not wait for (`waitSeconds: 0`, then poll the run).
3. **Direct action or tool** — a single operation where a workflow would be overkill, or tools for an app that runs
   its own LLM.
4. **Proxy** — last resort, for a raw API call no action covers. Inside a workflow, an API no component covers is a
   question for the person; see [An API no component covers](#an-api-no-component-covers).

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

A [code workflow](#code-workflows) is another way to build the same endpoint, scheduled or Call-workflow workflow: a
file in the app's repository, deployed with the CLI, instead of `buildWorkflow`. Use it when `buildWorkflow` cannot
express the logic, or when the person asks for code.

## One-time setup, by a person

- The CLI manages the app: it needs an automation API key created in DEVELOPMENT.
- The management MCP server builds and deploys workflows: an admin API key created in DEVELOPMENT and the header
  `X-ENVIRONMENT: DEVELOPMENT` (see the **ByteChef Management MCP Setup** skill).
- Sign-in (OAuth) connections are created by a person in the ByteChef UI; Claude cannot create them.

## The app

<!-- transport: cli uses: configure, automation app create, automation app link, automation app list, automation app instance create, automation app instance list, automation app instance update, automation app status, automation connection list, automation app bind connection, automation app unbind connection, automation app bind api-collection, automation app unbind api-collection, automation app key rotate -->
Configure the CLI once with the DEVELOPMENT automation API key, then run the app commands in the app's directory.
The commands that change an app (`create`, `link`, `instance create`, `instance update`, `key rotate`, `bind`,
`unbind`) and
`automation connection list` refuse any environment but DEVELOPMENT before sending a request; the server refuses the
same changes there too. `list`, `instance list` and `status` read in every environment.

```bash
bytechef configure --profile dev --host https://bytechef.example.com --token <AUTOMATION_API_KEY> \
  --environment DEVELOPMENT --workspace-id 1
bytechef automation app create --name my-app --profile dev
bytechef automation app instance update --url http://localhost:3000 --profile dev
bytechef automation connection list --component-name hubspot --profile dev
bytechef automation app bind api-collection --api-collection-id 9 --profile dev
bytechef automation app bind connection --binding-key crm --component-name hubspot \
  --actions createContact --tools searchContacts --profile dev
bytechef automation app status --profile dev
```

- `create` writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `.env.local` (owner-only, other lines kept) and
  `.bytechef/app.json` (`id`, `instanceId`, no secret; commit it). It never prints the key; never print or echo it yourself. If a file
  write fails after the app was created, the error names the app id: `bytechef automation app link --app-id <id>`
  restores `.bytechef/app.json` and `bytechef automation app key rotate` writes a new key.
- Right after `create`, set where the app runs: `bytechef automation app instance update --url http://localhost:3000`
  for `npm run dev`. An instance that requires sign-in gets its sign-in client only once its URL is set; until then
  the app's `/auth/login` answers 503 saying the URL is missing. `instance update` also takes `--name`, and
  `--app-id` with `--instance-id` for another instance; it is DEVELOPMENT only.
- Later commands find the app through `.bytechef/app.json`, walking up from the current directory, or take
  `--app-id`. `bytechef automation app list` lists the workspace's apps.
- An app has instances: one per environment, possibly several. The key, the URL, the bindings and the sign-in mode
  belong to an instance, not to the app; `create` makes the first one, in DEVELOPMENT, and `.bytechef/app.json`
  records it. `bytechef automation app instance list --app-id 42` lists the instances in the profile's environment
  with id, workspace, status, sign-in mode and URL; `status` shows them too, then the bindings of the one it
  addresses.
- When an app has several instances in the environment, the commands refuse to guess: pass `--instance-id` (`link`,
  `status`, `key rotate`, `bind`, `unbind`), or in DEVELOPMENT link the directory to one with
  `bytechef automation app link --app-id 42 --instance-id 8`. Outside DEVELOPMENT only `--app-id` with
  `--instance-id` works, because linking is DEVELOPMENT only. `key rotate --instance-id` for an instance other than
  the linked one needs `--env-file`.
- `bytechef automation app instance create --app-id 42 --name Partner --env-file .env.partner` adds an instance to an
  existing app, in DEVELOPMENT only. The new key goes to the named file and is never printed; `.bytechef/app.json`
  is left alone. `--workspace-id` puts the instance in another workspace than the profile's; the server decides
  whether that is allowed. Staging and production instances are created by a person in ByteChef.
- New apps and instances require sign-in when the authorization server is on, and get `NONE` when it is off (the
  output's `userAuthMode` says which). `--user-auth-mode NONE` on `create` or `instance create` opts out
  (`OPTIONAL` serves signed-in and anonymous visitors); pass `NONE` only when the person asked for an app without
  sign-in. When the authorization server is off, an explicit `OPTIONAL` or `REQUIRED` is answered with 409
  `APP_USER_AUTH_UNAVAILABLE` naming the property to turn on; tell the person, or create the instance with `NONE`.
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

## Versions and deployments

<!-- transport: cli uses: automation app version create, automation app version list, automation app deployment record, automation app deployment list, automation app deployment deploy, automation app deployment rollback, automation app deployment cancel, automation app instance deployment-settings set, automation app instance deployment-settings get, automation app instance deployment-settings clear -->
The version and deployment commands work in every environment and write no file; CI runs `record` after it deployed the app.

- `bytechef automation app version create --name 1.4.0 --commit <sha>` names a release of the app's code.
- `bytechef automation app version list [--limit <n>]` lists the app's versions, newest first (1 to 200, default 50).
- `bytechef automation app deployment record --version 1.4.0 --url https://my-app.example.com --commit <sha>`
  records that the version was deployed to an instance and makes it the current one; a new version name is created.
- `bytechef automation app deployment list [--limit <n>]` shows an instance's deployment history, newest first (1 to 200,
  default 50).

In CI there is no `.bytechef/app.json`: pass `--app-id` and, for the deployment commands, `--instance-id`. A version
name that exists with another commit is refused (409 `APP_VERSION_COMMIT_MISMATCH`); pick a new name.

CI records a deployment with `bytechef automation app deployment record --version <name> --commit <sha> --url <url>`,
adding `--app-id` and `--instance-id` when there is no linked directory. It works in every environment and needs
`APP_DEPLOY` there. A version name is only a label, and recording an existing name with a different commit is refused.

**Deploying through ByteChef.**

ByteChef can run the deployment itself through the instance's hosting target: `VERCEL` deploys through Vercel,
`SELF_HOSTED` through a deploy hook on your own server. Set the instance's deployment configuration once, in
DEVELOPMENT (`instance deployment-settings get` works everywhere, `clear` removes it):

- Vercel: `bytechef automation app instance deployment-settings set --connection-id <vercel connection> --setting
  projectId=<id or name> --setting repository=<owner/name> [--setting teamId=<id>] [--setting target=preview]`. The
  repository is on GitHub. A PRODUCTION instance deploys to production unless `target=preview`; any other environment
  always deploys a preview.
- Self-hosted: `--connection-id <HTTP Client connection with the agent's token> --setting
  deployUrl=https://deploy.example.com/ [--setting statusUrl=<url with {id}>] [--setting cancelUrl=<url with {id}>]`.
  `https` is required outside DEVELOPMENT, and the status and cancel URLs must share the deploy URL's origin. The app
  template carries a reference deploy agent for one server (`npm run deploy-agent`, described in its `AGENTS.md`).

Settings are plain values, not secrets: the token lives in the connection. They cannot be changed while a deployment
is in progress.

- `bytechef automation app deployment deploy --version 1.4.0 --wait` deploys a version. A version that does not exist
  is created only when `--commit` or `--git-ref` says which code it is. `--wait` follows the deployment until it is
  `READY`, `FAILED` or `CANCELED` (`--timeout <seconds>`, default 600) and exits non-zero unless it is `READY`.
- `bytechef automation app deployment rollback --version 1.3.0 --wait` deploys a version that was `READY` on that
  instance before, as a new deployment.
- `bytechef automation app deployment cancel [--deployment-id <id>]` cancels the one deployment in progress.
- An instance has at most one deployment in progress. Use `record` instead when the deployment was made outside
  ByteChef, for example by CI.
<!-- /transport -->

## Signing users in

An app made from the template already signs users in; there is no sign-in code to write. Who may open the app is the
instance's sign-in mode (`userAuthMode` in `bytechef automation app status` and `npm run -s bytechef:status`).

- Sign-in needs the OAuth2 authorization server on (`bytechef.oauth2.authorization-server.enabled`) and the
  instance's URL set, with `bytechef automation app instance update --url <url>` in DEVELOPMENT. The callback is
  `<instance url>/auth/callback`, and a DEVELOPMENT instance also accepts `http://localhost:3000/auth/callback`, the
  address of `npm run dev`.
- `src/start.ts` registers `bytechefRequestMiddleware` (through `requestMiddleware` in `src/server/bytechef.ts`), and
  `src/routes/auth/` holds the login, callback and logout routes. Do not remove or replace that registration, do not
  read the session cookie yourself, and never pass a token or the whole user to the browser.
- The signed-in user is `context.user` on the server, undefined when no one is signed in. Read it in a server function
  and return only what the page shows, such as a display name.
- Sign-out is a POST to `/auth/logout`. It clears the app's own session only; the user's tokens in ByteChef stay valid
  until they expire.
- Two server-only environment variables belong to sign-in. `BYTECHEF_SESSION_SECRET` seals the session cookie: the
  scaffolder generates it into `.env.local`, and every deployment needs its own value. `APP_URL` is the app's public
  URL, needed behind a reverse proxy. The CLI writes neither.
- An instance's configuration is visible to everyone who can view the instance. It must not hold secrets.

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

## Code workflows

A code workflow is an automation project written as a file and deployed as a whole. Write one when `buildWorkflow`
cannot express the logic or the person asks for code: in TypeScript, the app's language, or in Python when the person
asks. Before writing it, read the **ByteChef Code Workflow Builder** skill: its file contract, its "Component tasks"
section and its "Reading the trigger and the call input" section.

**The file.** `bytechef/workflows/<project>.ts` (or `.py`), committed with the app. One project per file.

- TypeScript: the file is a bare `({ ... })` expression. Never `import` or `export` anything: either one makes the
  file a module, and an `export` in the deployed file makes deploy answer HTTP 500.
- Compile it before every deploy with `npm run build:workflows` (`tsc -p bytechef/workflows`), which writes
  `bytechef/workflows/dist/<project>.js`. Deploy that `.js`, never the `.ts`: the server runs JavaScript. An app
  without the script gets the template's `bytechef/workflows/tsconfig.json` and the script. The config doesn't
  require types on `input` and `context`, because `@bytechef/workflow-api` is not published; once it is installed
  from a ByteChef checkout, add `/// <reference types="@bytechef/workflow-api" />` and end the object in
  `satisfies ByteChef.ProjectDefinition`.
- Python: the definition is a `dict`, with the variable alone on the last line.
- Every workflow has a `label`, unique in the project; the lookup below finds workflows by it.

**Per kind.**

- An endpoint workflow has the `apiPlatform/v1/newApiRequest` trigger and ends in an
  `apiPlatform/v1/responseToAPIRequest` component task. Nothing checks for that task: without it the endpoint
  answers 200 with an empty body, so never leave it out.
- A scheduled workflow has a schedule trigger. Read its parameters with `getTriggerDefinition`: deploy does not check
  trigger or action parameters. `dataTable/v1/createRecords` takes `records: {values: [...]}`, a list of column
  maps, although its definition shows `records` as an open object.
- A Call-workflow workflow has the `workflow/v1/newWorkflowCall` trigger, with `inputSchema` as a string. The run's
  result is the workflow's `outputs` (`[{name, value: "${task}"}]`); without them it is `{}`.

<!-- transport: mcp uses: createAppApiCollection, deployProject edition: ee -->
A code project is a project deployed from a file with `bytechef automation project deploy`, and it is a project of its
own. Before its first deploy, run `listProjects` and pick a project `name` no other project in the workspace uses.
Deploy matches that name case-insensitively and writes into any project with it, a visual one included. It adds the
file's workflows to that project and publishes it, together with the visual project's unpublished edits. On a later
deploy of the same file, `listProjects` shows the project under that name: that match is this file's own project, so
keep the name. Afterwards that `name`, and each workflow's `name`, stay fixed. A renamed workflow gets a new uuid, which
breaks the endpoint or binding that points at it.
<!-- /transport -->

<!-- transport: cli uses: automation project deploy -->
Deploy the file in DEVELOPMENT. Each deploy publishes a new version of the project:

```bash
npm run build:workflows
bytechef automation project deploy --project-file bytechef/workflows/dist/orders.js --profile dev
```

- A deploy error that names a task: fix the file and deploy again.
- `401`: the profile's key is not an automation API key created in DEVELOPMENT. `403`: its user may not create or
  publish projects in the workspace. Stop and ask a person in both cases.
<!-- /transport -->

<!-- transport: mcp uses: createAppApiCollection, createAppApiEndpoint, updateAppApiCollectionVersion, deployProject edition: ee -->
After a deploy, find the ids:

1. `listProjects` (pass `workspaceId`): the project `id`, matching its `name`.
2. `getProject`: the highest entry of `versions` whose `status` is `PUBLISHED`. That is the project version.
3. `listWorkflows`: each workflow's `workflow_uuid`, matching `name`, which carries the workflow's label. Its `version`
   field is not the project version.

Then bind with these ids, in place of `buildWorkflow` and `publishProject`:

- **An endpoint**: create a collection for this project with `createAppApiCollection` and its own `contextPath`; a
  collection serves only one project, so never add a code workflow to the visual project's collection. Then add the
  endpoint with `createAppApiEndpoint` and run the collection's `bindCommand`. An `unresolved` or `arming failed`
  result is handled as in the API endpoint steps.
- **A schedule**: `deployProject` with the scheduled workflow's uuid in `enabledWorkflows`.
- **A Call-workflow run**: `deployProject`, then run the `bytechef automation app bind workflow` command it prints.

To change a code workflow: edit the file, deploy again, and take the new `PUBLISHED` version from `getProject`. Then run
`updateAppApiCollectionVersion` for an endpoint, or `deployProject` for a schedule or a Call-workflow run. If
`updateAppApiCollectionVersion` refuses because an endpoint's workflow is missing, a workflow `name` changed: restore it
in the file and deploy again. Never pass `removeMissingEndpoints` to get past that refusal, since it deletes the
endpoint the app calls. Never change a code project with a workflow tool (`buildWorkflow`, `updateWorkflow`,
`createProjectWorkflow`, `deleteWorkflow`, `updateScriptComponentCode` or `publishProject`), even when a `deployProject`
result suggests `draftWorkflowId`. The next deploy overwrites or publishes that edit, so the file is the only source.
<!-- /transport -->

## An API no component covers

When a workflow needs an API no ByteChef component covers, never pick the way alone, even if the person said earlier
to just proceed. Name the API and ask which they want:

- **The HTTP Client component** inside this workflow. The call stays in this one workflow.
- **A custom component**, written in JavaScript (Python on request). Every workflow on the instance can see and reuse
  it. Publishing it needs an admin API key, which the app's automation profile does not hold: a person creates a
  second CLI profile with one.

Wait for the answer. On "custom component", build it with the **ByteChef Custom Component Builder** skill, then call
its action from a component task or from `context.component` in a `perform`.

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
