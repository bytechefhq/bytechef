---
name: ByteChef Code Workflow Builder
description: This skill should be used when the user asks to "create a code workflow", "write a workflow as code", "deploy a code-native project", "build a code workflow integration", "author a ByteChef project in JavaScript/Python/Java", or wants whole ByteChef projects (automation) or integrations (embedded) defined in code and deployed to a running instance.
transports:
  required: [local, cli]
---

# ByteChef Code Workflow Builder

A **code workflow** is a whole ByteChef *project* (automation) or *integration* (embedded) authored as code and deployed as one artifact. JavaScript and Python use a **single source file**; Java uses a **JAR** built against the ByteChef SDK. Deploying creates (or updates) the project/integration and registers its workflows.

## Connecting to ByteChef

1. `BYTECHEF_BASE_URL` + `BYTECHEF_API_KEY` environment variables; otherwise ask the user.
2. Deployment is an **EE** operation. The key must match the surface:
   - Automation (`/api/automation/v1`): an **automation API key** of the workspace (Settings → Current Workspace → API Keys), created in the environment you deploy to. Its user must be allowed to create and publish projects in the workspace. An Admin API key or an Embedded API key is rejected with `401`.
   - Embedded (`/api/embedded/v1`): an **Embedded API key** whose owning user holds the admin authority; see each deploy section below.

## Two surfaces, two contracts — pick the right one

| | Automation (project) | Embedded (integration) |
|---|---|---|
| Identity member | `name` (String, REQUIRED, **locked after first deploy**) | `componentName` (String, REQUIRED, **locked after first deploy**) |
| Other members | `version` (String), `description`, `workflows` | `componentVersion` (int), `version` (String, default "0.0.1"), `description`, `workflows` |
| Java interface | `com.bytechef.automation.project.ProjectHandler` | `com.bytechef.embedded.integration.IntegrationHandler` |
| Java SDK module | `sdks/backend/automation/project-api` (+ `workflow-api`) | `sdks/backend/embedded/integration-api` (+ `workflow-api`) |
| ServiceLoader file | `META-INF/services/com.bytechef.automation.project.ProjectHandler` | `META-INF/services/com.bytechef.embedded.integration.IntegrationHandler` |
| Deploy endpoint | `POST /api/automation/v1/projects/deploy` | `POST /api/embedded/internal/integrations/deploy` |
| Multipart fields | `projectFile` (binary) + `workspaceId` (int64, required) | `integrationFile` (binary) |

Deploy resolves the target by the identity member (case-insensitive): an existing project/integration of that name is **updated** (a new version of its artifact), otherwise one is **created**. The identity member cannot change on later deploys — a renamed source is treated as a different project/integration.

## The single-file contract (JavaScript / Python)

ByteChef evaluates the file and reads the value it evaluates to:

- **JavaScript**: the file is one parenthesized object expression, `({ ... })`. Keep it a bare expression: never `export` it or wrap it in `module.exports`, which hides the object from the server. An `export` makes the deploy answer HTTP 500, and only the server log names the `SyntaxError`.
- **Python**: define each `perform` with `def`, build the definition as a `dict`, and end the file with the variable alone on its last line. A `types.SimpleNamespace` works too. This differs from a Python **custom component**, which must be a `types.SimpleNamespace`.

`workflows` is a list of raw entries: `{name, label, description, inputs, outputs, triggers, tasks}`. Give **every workflow a `label`**, unique within the project: tools that look a workflow up by name (`listWorkflows`, for example) report its label, and two workflows without one break that lookup. ByteChef re-evaluates the file for every task run, so top-level code should only build the definition.

`perform` is called as `perform(input, context)`, or `perform(input, connection, context)` when the task reads its connections' parameters:

- `input` holds the workflow's inputs, each trigger's output and every finished task's output, each under its own name in one namespace (so an input and a task cannot share a name). Read a name that is not an identifier as `input["fetch-orders"]`. A task that has not run yet is absent.
- `connection` maps each [declared connection](#declaring-connections) by name to its parameters. It carries credentials; never log it.
- `context.component.<componentName>.<actionName>(input, connectionName)` invokes any built-in component action.
- `context.log(level, message)`: `"trace" | "debug" | "info" | "warn" | "error"`, case-insensitive; any other string throws. Java tasks pass the `TaskContext.LogLevel` enum.

The context offers nothing else: there is no `context.input()`, `context.parameters()` or `context.connection(name)`.

### Declaring connections

A task declares the connections its perform uses, so the platform can wire them and the editor can prompt for them:

```javascript
{
    name: "my_task",
    connections: [
        {componentName: "httpClient", name: "billing_api"},
        {componentName: "slack", componentVersion: 1, name: "slack_prod"}
    ],
    perform: function (input, connection, context) { /* ... */ }
}
```

A map keyed by connection name parses too. In Java: `.connections(connection("slack", "slack_prod"))`. The declared `name` is the `connectionName` a `context.component` call passes and the key in `connection`. A name that is not wired fails at run time.

### Running tasks concurrently

Tasks run in order. Group them to run at the same time — `type: "parallel"` with a `tasks` list dispatches them all at once, `type: "forkJoin"` with a `branches` list of task lists runs the branches concurrently and each branch in order:

```javascript
{name: "enrich", type: "parallel", tasks: [
    {name: "fetch_customer", perform: (input, context) => ...},
    {name: "fetch_inventory", perform: (input, context) => ...}
]}
```

In Java: `parallel("enrich").tasks(...)` and `forkJoin("notify").branches(branch(...), branch(...))`.

A group declares no `perform` of its own. Task names are flat and must be unique across the whole workflow, nesting included; a group inside a group is rejected. Both are checked at save time. **Tasks in one parallel group cannot read each other**: they start together, so a sibling is absent from `input`; put shared work before the group. Never spawn threads inside a `perform`: the sandbox forbids it, and self-spawned work is invisible to the engine.

## Component tasks

A task can run a component action directly, as the same task does in a visual workflow. Give it a `type` of the form `<component>/v<version>/<action>` and the action's inputs as `parameters`. It has no `perform`, `connections`, `tasks` or `branches`:

```javascript
{
    name: "respond",
    type: "apiPlatform/v1/responseToAPIRequest",
    parameters: {responseType: "SUCCESS", response: {success: "${fetchOrders}"}}
}
```

- **Deploy checks the type.** A malformed `type`, or a component, version or action the instance doesn't have, rejects the deploy with an error naming the task.
- **Deploy does not check the parameters**, of an action or of a trigger. Read the action with `getActionDefinition` (and a trigger with `getTriggerDefinition`) before writing them, and use the property names and types it reports.
- **`${...}` is evaluated by the engine when the task runs.** `${fetchOrders}` is the whole output of the task `fetchOrders`, `${fetchOrders.count}` one field, `${fetchOrders.orders[0].id}` a list element, and `"Found ${fetchOrders.count} orders"` interpolates into text. A workflow input is read by its name. A task you reference this way needs an identifier name: `fetchOrders`, never `fetch-orders`. Deploy accepts `${fetch-orders}`; the run then fails with `Invalid expression: ${fetch-orders}`, which an endpoint answers as a 422.
- **Anywhere a task can stand**: at the top level or inside a `parallel` group or a `forkJoin` branch. Its name counts toward the unique task names.
- **Its connection is its component's**, wired by the deployment the same way as for that task in a visual workflow.

Prefer a component task when the step must be a real workflow task the engine runs and shows, such as the response of an API endpoint. Use `context.component` inside a `perform` when the call is part of your own logic and you shape its result.

An API Platform endpoint in code, JavaScript:

```javascript
({
    name: "Orders API",
    version: "1",
    workflows: [
        {
            name: "listOrders",
            label: "List Orders",
            triggers: [{name: "request", type: "apiPlatform/v1/newApiRequest", parameters: {request: {}}}],
            tasks: [
                {
                    name: "fetchOrders",
                    perform: function (input, context) {
                        const orders = [{id: 1, total: 42}, {id: 2, total: 17}];

                        return {orders: orders, count: orders.length};
                    }
                },
                {
                    name: "respond",
                    type: "apiPlatform/v1/responseToAPIRequest",
                    parameters: {responseType: "SUCCESS", response: {success: "${fetchOrders}"}}
                }
            ]
        }
    ]
})
```

Python:

```python
def fetch_orders(input, context):
    orders = [{"id": 1, "total": 42}, {"id": 2, "total": 17}]

    return {"orders": orders, "count": len(orders)}


project = {
    "name": "Orders API",
    "version": "1",
    "workflows": [
        {
            "name": "listOrders",
            "label": "List Orders",
            "triggers": [{"name": "request", "type": "apiPlatform/v1/newApiRequest", "parameters": {"request": {}}}],
            "tasks": [
                {"name": "fetchOrders", "perform": fetch_orders},
                {
                    "name": "respond",
                    "type": "apiPlatform/v1/responseToAPIRequest",
                    "parameters": {"responseType": "SUCCESS", "response": {"success": "${fetchOrders}"}},
                },
            ],
        }
    ],
}

project
```

In Java: `componentTask("respond", "apiPlatform/v1/responseToAPIRequest").parameters(Map.of(...))`.

Nothing checks that an endpoint workflow ends in a `responseToAPIRequest` task: one without it deploys, binds and answers 200 with an empty body, dropping its tasks' output, so add it yourself.

## Reading the trigger and the call input

- A trigger's output is in `input` under the **trigger's name**. For the `apiPlatform/v1/newApiRequest` trigger above, named `request`, `input.request` holds the call's `method`, `headers`, `parameters` (the query and path parameters) and `body`.
- A Call-workflow trigger (`workflow/v1/newWorkflowCall`) declares its input as `inputSchema`, a **string** holding a JSON Schema: `JSON.stringify({type: "object", properties: {...}})` in JavaScript, `json.dumps({...})` in Python. A map there is not accepted. The run's result is the workflow's `outputs`, a list of `{name, value}` such as `outputs: [{name: "summary", value: "${summary}"}]`; a workflow without `outputs` returns `{}`.
- A schedule trigger's parameters come from `getTriggerDefinition`, like any other trigger's.

### Automation project (JavaScript)

```javascript
({
    name: "my-code-project",
    version: "1",
    description: "A code workflow.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            tasks: [
                {name: "my_task", label: "My Task", perform: function (input, context) { return "hello"; }}
            ]
        }
    ]
})
```

Python: the same members in a `dict`, with `"perform": my_task` pointing at a `def my_task(input, context)`, and the variable alone on the last line.

### Embedded integration (JavaScript)

```javascript
({
    componentName: "my-integration",
    componentVersion: 1,
    version: "0.0.1",
    description: "A code workflow integration.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            tasks: [
                {name: "my_task", label: "My Task", perform: function (input, context) { return "hello"; }}
            ]
        }
    ]
})
```

(Python is analogous, with `componentName`/`componentVersion` keys in the definition `dict`.)

## The Java contract (JAR)

```java
public class MyProjectHandler implements ProjectHandler {
    @Override
    public ProjectDefinition getDefinition() {
        return ProjectDsl.project("my-code-project")
            .version("1.0.0")
            .description("A code workflow.")
            .workflows(
                WorkflowDsl.workflow("my-workflow")
                    .label("My Workflow")
                    .tasks(WorkflowDsl.task("my_task").label("My Task").perform((input, context) -> "hello")));
    }
}
```

Integrations implement `IntegrationHandler` with `IntegrationDsl.integration(...)` analogously (componentName-based). Register the ServiceLoader file for the matching interface (table above).
<!-- transport: local -->
The SDK jars are **not on a public Maven repository** — `publishToMavenLocal` from a ByteChef checkout or build in-repo.
<!-- /transport -->
Java deploy availability can be restricted by server configuration.

## Deploying

<!-- transport: cli uses: automation project deploy -->
```bash
# Automation project, via the CLI
bytechef automation project deploy --project-file my-code-project.js --workspace-id 1049
```
<!-- /transport -->

No CLI available? Same endpoint over curl:

```bash
curl -sf -X POST "$BYTECHEF_BASE_URL/api/automation/v1/projects/deploy" \
  -H "Authorization: Bearer $BYTECHEF_API_KEY" \
  -H "X-ENVIRONMENT: DEVELOPMENT" \
  -F "workspaceId=1049" \
  -F "projectFile=@my-code-project.js"
```

Each automation deploy publishes a new version of the project. A workflow keeps its uuid across deploys as long as its `name` stays the same.

<!-- transport: cli uses: embedded integration deploy -->
```bash
# Embedded integration: POST /api/embedded/v1/integration-code-workflows/deploy
bytechef embedded integration deploy --file my-integration.js --enable --environment DEVELOPMENT
```

The profile token must be an **embedded API key** whose owning user holds the admin authority; an Admin
API Key is rejected (401), since it is reserved for `/api/platform/v1`. The profile's environment (or
`--environment`) is sent as `X-Environment`. `--enable` makes the published integration live in that
environment: it creates and enables the instance configuration when there is none, otherwise moves the
existing one to the new version and enables it.
<!-- /transport -->

The admin console's `POST /api/embedded/internal/integrations/deploy` accepts
only an admin browser session (cookie + X-XSRF-TOKEN), never a bearer token.

- File extension selects the language: `.jar`/`.js`/`.py`. Success: `204 No Content`.
- The automation `/api/automation/v1/**` surface accepts automation API keys (Bearer) and is CSRF-exempt. The key is looked up in the environment `X-ENVIRONMENT` names (`PRODUCTION` when absent).

## Bridge: serving a code workflow to embedded connected users

A third deploy target exists for the **automation** artifact only (same `ProjectHandler` /
`name`-identity contract as the automation row above — nothing in the artifact changes): the embedded
bridge. Use it when the intent is "serve this to embedded connected users" instead of, or in addition
to, a standalone automation project.

| | Plain automation deploy | Embedded bridge deploy |
|---|---|---|
| Endpoint | `POST /api/automation/v1/projects/deploy` | `POST /api/embedded/v1/automation-project-code-workflows/deploy` (embedded API key of an admin user, bearer token) or `POST /api/embedded/internal/automation/projects/deploy` (admin browser session only) |
| Artifact | `ProjectHandler`, identical contract | Same `ProjectHandler` artifact, byte-for-byte |
| Result | A plain automation project, nothing embedded-reachable | The same kind of project, but marked so embedded connected users can reach it |

Deploying the same bytes through the plain endpoint instead creates an unrelated, unmarked project —
the endpoint you deploy through, not anything in the artifact, is what makes it embedded-servable. The
two bridge endpoints reach the same facade and are interchangeable in effect; pick whichever your
caller can authenticate with — `/api/embedded/internal/**` only accepts an admin browser session
(cookie + X-XSRF-TOKEN), a bearer token there is rejected (401/403).

<!-- transport: cli uses: embedded code-workflow deploy, embedded code-workflow list, configure -->
Deploy through the CLI once it's configured (`bytechef configure ...`):

```bash
bytechef embedded code-workflow deploy --file my-project.js
bytechef embedded code-workflow deploy --file my-project.js --release
bytechef embedded code-workflow list --output table
```

The profile token must be an embedded API key whose owning user holds the admin authority; an Admin
API Key is rejected. `--release` also points the configuration of the profile's environment at the
newly published version.
<!-- /transport -->

No CLI available? The same deploy is a plain multipart POST with a bearer token:

```bash
curl -sf -X POST "$BYTECHEF_BASE_URL/api/embedded/v1/automation-project-code-workflows/deploy" \
  -H "Authorization: Bearer $BYTECHEF_API_KEY" \
  -F "projectFile=@my-project.js"
```

`list` has no admin-console equivalent to fall back to — it also goes through
`GET /api/embedded/v1/automation-project-code-workflows` with the same bearer token.

**Model: deploy once, reference per user.** Every connected user shares the one deployed workflow —
there is no per-user copy and no per-user editing. A connected user's reference is provisioned
automatically the first time they invoke the workflow (auto-wiring their connections by component),
or ahead of time via `POST /api/embedded/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision`.
Redeploying the artifact upgrades every referencing connected user at once — there is no version
pinning per user.

**Invocable-trigger requirement.** A deployed workflow is only reachable by connected users if it
declares a trigger the embedded public endpoints understand: a `request` trigger (plus an action that
writes the response) for synchronous `POST /api/embedded/v1/workflows/{workflowUuid}` calls, or the
App Event trigger for asynchronous `POST /api/embedded/v1/app-events` fan-out — the same rule
`/integrations/deploy`-based embedded integrations already follow. A workflow with neither trigger
still deploys, but no connected user can ever call it; check for a WARN in the server log at deploy
time if a workflow isn't showing up as invocable.

Full write-up, including the reference-vs-copy split, the 409 unresolvable-connection contract, and
dangling references on redeploy: [Automation Code Workflows](/platform/embedded/build/automations/automation-code-workflows) in
the docs site.

## Editing after deploy

Code-backed projects/integrations open a **source editor** (Monaco) instead of the visual canvas in the ByteChef UI, with compile-gated saves: an edit that fails to load, or that changes the identity member, is rejected. An editor save updates a **draft** — publishing stays the project/integration header's publish action — while an upload or deploy publishes immediately. The editor header carries Test Configuration for wiring the declared connections before a test run.

Duplicating, exporting and importing a code-backed project all travel the **source**, not the generated workflow definitions: an export is a zip carrying `code-workflow.<LANGUAGE>`, and an import or duplicate redeploys it. The declared project name is rewritten to the new project's name, since an upload resolves its target by that name — so declare the name as a plain string literal, or the rename is rejected.
