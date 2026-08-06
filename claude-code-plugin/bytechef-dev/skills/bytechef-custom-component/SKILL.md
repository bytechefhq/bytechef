---
name: ByteChef Custom Component Builder
description: This skill should be used when the user asks to "create a custom component", "build a custom ByteChef component", "write a JavaScript/Python/Ruby component", "upload a custom component", "deploy a custom component", or wants a single-file uploadable component (as opposed to an in-repo platform component) for a running ByteChef instance. Covers all four languages (JavaScript, Python, Ruby, Java) and uploading the built artifact to a configured ByteChef server.
---

# ByteChef Custom Component Builder

Custom components are **uploadable** components deployed to a *running* ByteChef instance — unlike in-repo platform components (see the ByteChef Component Builder skill), they are not part of the ByteChef codebase. JavaScript/Python/Ruby components are a **single source file**; Java components are a **JAR** built against the ByteChef SDK.

## Connecting to ByteChef

All uploads target a configured instance. Resolve the connection in this order:
1. `BYTECHEF_BASE_URL` environment variable (e.g. `https://bytechef.example.com`) and `BYTECHEF_API_KEY` (an **admin** API key).
2. Otherwise, ask the user for the base URL and an admin API key.

Custom-component deployment is an **EE, admin-only** operation (the facade is `ROLE_ADMIN`-gated).

## The single-file contract (JavaScript / Python / Ruby)

The file is evaluated by a GraalVM polyglot engine as a **plain script** (NOT a module). The script's **completion value** is the component object, and the engine reads its **members** by name:

| Member | Type | Required |
|---|---|---|
| `name` | String | yes |
| `version` | **Integer** | yes |
| `title` | String | no |
| `description` | String | no |
| `icon` | String — an inline SVG, shown in the editor | no |
| `connection` | object — see *Declaring a connection* | no |
| `actions` | list of entries with `name`, `title`, `description`, `properties`, `output`, `sampleOutput`, `tool`, `perform` | yes (to be useful) |
| `triggers` | list of **polling** triggers — see *Polling triggers* | no |

Notes that prevent every common failure:
- Action entries use **`title`, not `label`**.
- `perform` is a function `(inputParameters, connectionParameters, context)`; the engine may invoke it with no arguments, so accept-and-default liberally (`*args` in Python, `|*args|` in Ruby).
- The perform `context` carries **`http` and `log` only** — deliberately no component invocation. A custom component is a leaf: it calls APIs itself.
- The **top-level value must expose polyglot members**: a JavaScript bare parenthesized object literal `({...})` works; a raw Python dict or Ruby hash does **NOT** (dict keys are not members). Use `types.SimpleNamespace(...)` in Python and core `Struct` in Ruby (NOT `OpenStruct` — `require 'ostruct'` is blocked by the sandbox).
- **Nested action entries stay raw** dicts/hashes/objects — they are consumed as maps.
- No `export default`, no `module.exports`, no `require` of stdlib beyond what the sandbox allows.

### Declaring inputs, output and options

An action without `properties` renders no inputs in the editor, so declare them:

```js
{
    name: "myAction",
    title: "My Action",
    tool: true,                                  // also expose this action as an AI agent tool
    properties: [
        {name: "email", type: "STRING", label: "Email", required: true},
        {name: "listId", type: "STRING", label: "List",
         options: function (inputParameters, connectionParameters) {   // dynamic options
             return [{label: "Main", value: "1"}];
         }}
    ],
    output: {type: "OBJECT", properties: [{name: "id", type: "STRING"}]},
    sampleOutput: {id: "42"},
    perform: function (inputParameters, connectionParameters, context) { ... }
}
```

An `options` function is re-evaluated in a fresh sandbox each time the editor asks, so it may call
`context.http`. `output`/`sampleOutput` give downstream nodes a schema to bind against.

### Declaring a connection

```js
connection: {
    baseUri: "https://api.example.com",
    authorizations: [
        {type: "BEARER_TOKEN"},
        {type: "OAUTH2_AUTHORIZATION_CODE",
         authorizationUrl: "https://example.com/oauth/authorize",
         tokenUrl: "https://example.com/oauth/token",
         scopes: ["read", "write"]}
    ],
    properties: [{name: "region", type: "STRING", label: "Region"}]
}
```

Every authorization type works, OAuth2 included — the platform runs the flow, refresh and callback.
The URL seams and `apply` may be functions; `apply` then runs per outbound request, so declare it
only when the credential placement is not standard. A function-valued seam cannot cross the Java
(Espresso) boundary, so a Java-loaded component reports such a connection as unsupported.

### Polling triggers

```js
triggers: [
    {
        name: "newItem", title: "New Item", type: "POLLING",
        properties: [{name: "folder", type: "STRING"}],
        output: {type: "OBJECT", properties: [{name: "id", type: "STRING"}]},
        poll: function (inputParameters, connectionParameters, closureParameters) {
            const since = closureParameters.cursor ?? 0;

            return {records: [{id: "1"}], closureParameters: {cursor: since + 1}};
        }
    }
]
```

Each record starts a workflow run; `closureParameters` comes back on the next poll, so use it for a
cursor. `pollImmediately: true` polls again at once instead of waiting for the next tick. **Only
`POLLING` is supported** — a webhook or listener trigger is rejected at save time rather than
registered as one that would silently never fire.

### JavaScript (the canonical shape)

```js
({
    name: "my-component",
    title: "My Component",
    version: 1,
    description: "A custom component.",
    actions: [
        {
            name: "myAction",
            title: "My Action",
            description: "An example action.",
            perform: function (inputParameters, connectionParameters, context) {
                return {};
            }
        }
    ]
})
```

### Python

```python
import types

types.SimpleNamespace(
    name="my-component",
    title="My Component",
    version=1,
    description="A custom component.",
    actions=[
        {"name": "myAction", "title": "My Action", "description": "An example action.",
         "perform": lambda *args: {}}
    ],
)
```

### Ruby

```ruby
Struct.new(:name, :title, :version, :description, :actions).new(
  "my-component", "My Component", 1, "A custom component.",
  [
    { "name" => "myAction", "title" => "My Action", "description" => "An example action.",
      "perform" => lambda { |*args| {} } }
  ]
)
```

## The Java contract (JAR)

A Java custom component is a JAR containing an implementation of `com.bytechef.component.ComponentHandler` (from the SDK module `sdks/backend/java/component-api`, which pulls `definition-api`):

```java
public class MyComponentHandler implements ComponentHandler {
    @Override
    public ComponentDefinition getDefinition() {
        return ComponentDsl.component("my-component")
            .title("My Component")
            .version(1)
            .actions(/* ... ComponentDsl actions ... */);
    }
}
```

Requirements:
- Register via ServiceLoader: `META-INF/services/com.bytechef.component.ComponentHandler` containing the implementation's fully-qualified class name.
- The SDK jars are **not published to a public Maven repository**. Build them from the ByteChef repo (`./gradlew :sdks:backend:java:component-api:publishToMavenLocal` etc.) and depend on them from `mavenLocal()`, or build your component inside a ByteChef checkout.
- Java upload is gated by the server property `bytechef.component.custom-component.java-enabled` (default `true`); if the server disables it, only `.js`/`.py`/`.rb` upload.

## Uploading

Endpoint (EE): `POST {BYTECHEF_BASE_URL}/api/platform/v1/custom-components/deploy`
- Multipart form, field **`componentFile`**.
- The **file extension selects the language**: `.jar` → Java, `.js` → JavaScript, `.py` → Python, `.rb` → Ruby. Any other extension is rejected.
- Success: `204 No Content`.
- Auth: `Authorization: Bearer <admin API key>` (the `/api/platform/v1/**` surface accepts API keys and is CSRF-exempt). Browser sessions use cookie + `X-XSRF-TOKEN` instead.

```bash
curl -sf -X POST "$BYTECHEF_BASE_URL/api/platform/v1/custom-components/deploy" \
  -H "Authorization: Bearer $BYTECHEF_API_KEY" \
  -F "componentFile=@my-component.js"
```

## Draft vs published

Uploads publish immediately. The **editor** in the UI works on a draft instead: saving a published
component spawns a new draft row (its declared `version` must be strictly above the current maximum),
and `publishCustomComponent` flips it. Only published rows are visible to workflows. At most one
draft exists at a time.

The component's identity is **`name` + the `version` member together**: re-uploading with the same name and same `version` updates that entry in place; **bump the `version` member to create a new version** alongside the old one.

## Verify after upload

The component appears in Settings → Custom Components on the instance, and its actions become available in the workflow editor. If the upload returns 4xx, re-check: admin key, file extension, and that the script's completion value exposes the required members (`name` + Integer `version`).
