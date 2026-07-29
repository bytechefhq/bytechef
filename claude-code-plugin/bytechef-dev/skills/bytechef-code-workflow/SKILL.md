---
name: ByteChef Code Workflow Builder
description: This skill should be used when the user asks to "create a code workflow", "write a workflow as code", "deploy a code-native project", "build a code workflow integration", "author a ByteChef project in JavaScript/Python/Ruby/Java", or wants whole ByteChef projects (automation) or integrations (embedded) defined in code and deployed to a running instance.
---

# ByteChef Code Workflow Builder

A **code workflow** is a whole ByteChef *project* (automation) or *integration* (embedded) authored as code and deployed as one artifact. JavaScript/Python/Ruby use a **single source file**; Java uses a **JAR** built against the ByteChef SDK. Deploying creates (or updates) the project/integration and registers its workflows.

## Connecting to ByteChef

1. `BYTECHEF_BASE_URL` + `BYTECHEF_API_KEY` (an **admin** API key) environment variables; otherwise ask the user.
2. Deployment is an **EE, admin-only** operation on both surfaces.

## Two surfaces, two contracts — pick the right one

| | Automation (project) | Embedded (integration) |
|---|---|---|
| Identity member | `name` (String, REQUIRED, **locked after first deploy**) | `componentName` (String, REQUIRED, **locked after first deploy**) |
| Other members | `version` (String), `description`, `workflows` | `componentVersion` (int), `version` (String, default "0.0.1"), `description`, `workflows` |
| Java interface | `com.bytechef.automation.project.ProjectHandler` | `com.bytechef.embedded.integration.IntegrationHandler` |
| Java SDK module | `sdks/backend/automation/project-api` (+ `workflow-api`) | `sdks/backend/embedded/integration-api` (+ `workflow-api`) |
| ServiceLoader file | `META-INF/services/com.bytechef.automation.project.ProjectHandler` | `META-INF/services/com.bytechef.embedded.integration.IntegrationHandler` |
| Deploy endpoint | `POST /api/automation/v1/projects/deploy` | `POST /api/embedded/internal/integrations/deploy` |
| Multipart fields | `projectFile` (binary) + optional `workspaceId` (int64 form field) | `integrationFile` (binary) |

Deploy resolves the target by the identity member (case-insensitive): an existing project/integration of that name is **updated** (a new version of its artifact), otherwise one is **created**. The identity member cannot change on later deploys — a renamed source is treated as a different project/integration.

## The single-file contract (JavaScript / Python / Ruby)

Plain-script evaluation; the **completion value** must expose **members** (same rules as custom components: JS bare `({...})`; Python `types.SimpleNamespace` — NOT a raw dict; Ruby core `Struct` — NOT a hash, NOT OpenStruct). `workflows` is a list of raw map entries: `{name, label, description, tasks: [{name, label, description, perform}]}` — **workflow/task entries stay raw dicts/hashes**, and `perform` is a zero-arg-callable returning the task output.

### Automation project (JavaScript)

```js
({
    name: "my-code-project",
    version: "1",
    description: "A code workflow.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            tasks: [
                { name: "my-task", label: "My Task", perform: function () { return "hello"; } }
            ]
        }
    ]
})
```

Python: `types.SimpleNamespace(name="my-code-project", version="1", description="...", workflows=[{...raw dicts, "perform": lambda *args: "hello"...}])`.
Ruby: `Struct.new(:name, :version, :description, :workflows).new("my-code-project", "1", "...", [ { "name" => ..., "perform" => lambda { |*args| "hello" } } ])`.

### Embedded integration (JavaScript)

```js
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
                { name: "my-task", label: "My Task", perform: function () { return "hello"; } }
            ]
        }
    ]
})
```

(Python/Ruby analogous with `componentName`/`componentVersion` members on the top-level namespace/Struct.)

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
                    .tasks(WorkflowDsl.task("my-task").label("My Task").perform(() -> "hello")));
    }
}
```

Integrations implement `IntegrationHandler` with `IntegrationDsl.integration(...)` analogously (componentName-based). Register the ServiceLoader file for the matching interface (table above). The SDK jars are **not on a public Maven repository** — `publishToMavenLocal` from a ByteChef checkout or build in-repo. Java deploy availability can be restricted by server configuration.

## Deploying

```bash
# Automation project (workspaceId optional; defaults server-side)
curl -sf -X POST "$BYTECHEF_BASE_URL/api/automation/v1/projects/deploy" \
  -H "Authorization: Bearer $BYTECHEF_API_KEY" \
  -F "workspaceId=1049" \
  -F "projectFile=@my-code-project.js"

# Embedded integration — NOTE: Authorization headers on the internal surface are
# routed to the embedded connected-user authenticator, which carries no admin
# authorities — a Bearer token will typically be rejected (401/403). Use an admin
# browser session (cookie + X-XSRF-TOKEN) or deploy from the instance's admin UI.
curl -sf -X POST "$BYTECHEF_BASE_URL/api/embedded/internal/integrations/deploy" \
  -H "Authorization: Bearer $BYTECHEF_ADMIN_TOKEN" \
  -F "integrationFile=@my-integration.js"
```

- File extension selects the language: `.jar`/`.js`/`.py`/`.rb`. Success: `204 No Content`.
- The automation `/api/automation/v1/**` surface accepts admin API keys (Bearer) and is CSRF-exempt.

## Editing after deploy

Code-backed projects/integrations open a **source editor** (Monaco) instead of the visual canvas in the ByteChef UI, with compile-gated saves: an edit that fails to load, or that changes the identity member, is rejected. Iterate locally, redeploy, or edit in the UI — both paths re-register the workflows and publish.
