# Apps — Agent Loop, Plan D2 (CLI, template, plugin) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give a coding agent working in a `create-bytechef-app` app a CLI that creates the app, writes its key into
`.env.local` without ever printing it, binds connections and workflows, and lists connections, plus the template's
`AGENTS.md` loop (checked against the real commands) and the `bytechef-app-builder` plugin skill.

**Architecture:** A new generated client `cli/clients/automation-app` (openapi-generator `java`/`native`, sources
committed) is built from a committed copy of the server's `openapi-management.yaml`, so the CLI can be built and
tested against an in-process stub server before Plan D1 merges. Three command classes in `cli/commands/automation`
(`AutomationAppCommand`, `AutomationAppBindingCommand`, `AutomationConnectionCommand`) share a static
`AppCommandSupport` (DEVELOPMENT guard, app lookup, env file location) and two file helpers (`AppLinkFile` for
`.bytechef/app.json`, `EnvFile` for the owner-only atomic env file write and the git-ignore check). The template's
`AGENTS.md` gains the loop, guarded by a plain JUnit drift test in `server-app`; the plugin skill and the regenerated
MCP instructions come last, after D1's MCP tools exist.

**Tech Stack:** Java 25, Spring Shell 4.0.3 (`org.springframework.shell.core.command.annotation.Command`/`Option`),
openapi-generator 7.25.0 (`java`, library `native`, `modelNameSuffix` `Model`), Jackson 2 (`com.fasterxml.jackson`),
JUnit 5, the JDK `com.sun.net.httpserver.HttpServer` as the stub server, `git` on the PATH for the git-ignore check.

**Spec:** `docs/superpowers/specs/2026-10-08-apps-agent-loop-design.md` (sections "CLI (app half)", "Template
(agent-facing docs)", "Plugin", "Testing → Plan D2", "Changes to the parent spec"). Background: `.agents/apps.md`,
`cli/README.md`, `claude-code-plugin/README.md`.

## Preconditions and ordering

- Tasks 1–6 need nothing from Plan D1: the client is generated from this plan's contract copy and every CLI test runs
  against an in-process stub server.
- Tasks 7–9 need Plan D1 merged (its four REST routes in `openapi-management.yaml` and its MCP tools
  `deployProject`, `listConnections`, `listAppCallableWorkflows`). Each of those tasks starts with a check; if the
  check fails, stop and report — do not continue with a partial D1.

## Deviations from the spec, settled while writing this plan

1. **The contract names.** The spec names the four routes but not their operationIds or schemas. This plan fixes
   them (Task 1 table below). Plan D1 should use the same names; if it merged others, Task 7 re-syncs the copy and
   renames the listed call sites.
2. **`status` derives each binding's component version from `GET /apps/{id}/bindable-connections`.** The management
   `AppBinding` schema has no `componentVersion` (only the GraphQL type does), and the spec adds none. The CLI joins
   bindings to bindable connections by `connectionId`; a binding whose connection is no longer bindable, or a caller
   the bindable lookup refuses (403, `APP_VIEW` only), shows `componentVersion: null` ("none when it cannot be
   resolved").
3. **The bind/unbind commands live in a sibling class `AutomationAppBindingCommand`**, as the data-table rows live in
   `AutomationDataTableRowCommand`. Command names are exactly the spec's.
4. **A multi-route stub.** The existing `StubApi` answers every request with one fixed response; the bind flow makes
   three calls. A new test helper `RoutedStubApi` answers per `METHOD /path` and records every request. `StubApi` is
   unchanged.
5. **"Idempotent `create`"** means: a second `create` in the same directory (or below it) finds `.bytechef/app.json`
   and refuses before any request, so a directory never gets a second app.

## Global Constraints

- **DEVELOPMENT only:** every `automation app` command and `automation connection list` refuses (exit 1) when the
  resolved environment (option, then `BYTECHEF_ENVIRONMENT`, then profile) is not DEVELOPMENT, telling the user to use
  a DEVELOPMENT profile with an automation API key created in DEVELOPMENT. The refusal happens before any request.
- **Never print the key.** No command prints `BYTECHEF_APP_KEY` or the `secretKey` of a create/rotate response — not
  in JSON output, not in table output, not in any error message. The CLI writes it only to the env file.
- `.bytechef/app.json` holds `id`, `uuid`, `workspaceId`, `environment`, `host` — no secret, safe to commit.
- **Finding the app:** `--app-id`, else `.bytechef/app.json` found by walking up from the current directory (like
  git). The directory holding `.bytechef/` is the app directory; `--env-file` is resolved relative to it. A file whose
  host, environment or workspace differs from the resolved profile is an error. The lookup keys on the `app.json`
  file, never on the `.bytechef` directory alone (`~/.bytechef/` holds the CLI's own `config`).
- **`create` and `link`** refuse when walking up finds an existing `.bytechef/app.json`. If writing a file fails after
  the server created the app, `create` prints the app id and what is missing: `automation app link --app-id <id>`
  restores `.bytechef/app.json`, and `automation app key rotate` puts a new key in the env file. It never prints the
  key.
- **Env file writing:** replaces only the `BYTECHEF_URL` / `BYTECHEF_APP_KEY` lines and keeps every other line; writes
  a temporary file with owner-only permissions (0600) in the same directory and renames it over the env file; warns
  when the env file is not git-ignored.
- **`bind connection`:** the binding becomes exactly what the command names: the PUT body replaces the connection and
  every allow-list, and an omitted `--actions`, `--tools` or `--proxy` means none. `--component-name` is required
  unless `--connection-id` is given (the component is then the connection's). Without `--connection-id`, the CLI picks
  among the usable connections of `GET /apps/{id}/bindable-connections?componentName=`: exactly one is used; otherwise
  it fails listing the candidates (id, name, usable). Action and tool names are checked against
  `GET /apps/component-operations` before the PUT; an unknown name fails listing the allowed ones. Nothing is written
  on failure.
- Errors say what to do next and list allowed values where the server returns them.
- Options on every command: `--profile`, `--host`, `--token`, `--environment`, `--workspace-id`, `--output json|table`
  (default `json`). `create`: `--name` (required), `--description`, `--hosting-target VERCEL|SELF_HOSTED` (default
  `VERCEL`), `--env-file` (default `.env.local`). `key rotate`: `--env-file` (default `.env.local`).
- `bind workflow` options are exactly `--workflow-key`, `--deployment-id`, `--workflow-uuid` (D1's `deployProject`
  prints `bytechef automation app bind workflow --workflow-key <KEY> --deployment-id <id> --workflow-uuid <uuid>`).
- Exit codes (existing): 0 success, 1 generic, 2 HTTP 401/403, 3 HTTP 404, 4 missing configuration.
- `check:template` is unchanged: it runs in the Node SDK job, which has no CLI binary.
- Java: Apache 2.0 header `Copyright 2025 ByteChef` on every new file (cli and server-app are CE), `@author Ivica
  Cardic`, a blank line before every control statement and between a variable's modification and its use, no blank
  line before a class's closing brace, descriptive names (no single letters, also in lambdas), no `//` comments, no
  `TODO`, camelCase test names without underscores, no chaining outside builders/streams/Optional/AssertJ-style DSLs.
- Gradle: logs go to `$SCRATCH` (export it once to your session scratchpad directory). Redirect each run to a file,
  then `echo "exit=$?"` on its own line, then grep `^> Task .* FAILED`. Never pipe Gradle into `tail`/`grep`. Use
  foreground timeout 600000, no `&`, no `sleep`.
- Commits: by explicit path only, never amend, never `git stash`, no `Co-Authored-By`, no "Generated with" line.
  Subjects: `- cli - …` for `cli/`, `- sdk - …` for the template, `- docs - …` for specs and `.agents/`, a plain
  `- <imperative sentence>` for `server/` and `claude-code-plugin/`.

### Contract names (Task 1 defines them; D1 should match)

| Route | Tag → generated API | operationId → method | Response schema |
|---|---|---|---|
| `GET /apps/{id}/bindable-connections?componentName=` | `app` → `AppApi` | `getAppBindableConnections(Long id, String componentName)` | array of `AppBindableConnection` (`id, name, componentName, connectionVersion, componentVersion, usable`) |
| `GET /apps/{id}/bindable-workflows` | `app-workflow-binding` → `AppWorkflowBindingApi` | `getAppBindableWorkflows(Long id)` | array of `AppBindableWorkflow` (`projectDeploymentId, projectDeploymentName, projectName, workflowUuid, workflowName, description`) |
| `GET /apps/component-operations?componentName=&componentVersion=` | `app` → `AppApi` | `getAppComponentOperations(String componentName, Integer componentVersion)` | `AppComponentOperations` (`actionNames, toolNames`) |
| `GET /connections?workspaceId=&componentName=` (+ `X-Environment`) | `workspace-connection` → `WorkspaceConnectionApi` | `getWorkspaceConnections(Long workspaceId, String componentName, EnvironmentModel xEnvironment)` | array of `AppWorkspaceConnection` (`id, name, componentName, connectionVersion, authorizationType, credentialStatus, aiProvider, usable`) |

## Review Focus

1. **`~/.bytechef/` is an ancestor of the app directory** (an app under the home directory, where the CLI keeps
   `config`). `create` must not treat that directory as an existing app, and `status` must not look for the app there.
   Pinned by `AppLinkFileTest.testFindSkipsABytechefDirectoryWithoutAppJson` (Task 2) and
   `AutomationAppCommandTest.testCreateIgnoresTheCliConfigDirectoryAbove` (Task 3).
2. **An existing `.env.local` holding other secrets**, `export` prefixes, comments, blank lines and a 0644 mode. Only
   the two ByteChef lines may change, everything else must survive byte for byte, and the result must be 0600. Pinned
   by `EnvFileTest.testMergeReplacesOnlyTheByteChefLinesAndKeepsEveryOtherLine` and
   `EnvFileTest.testWriteTightensAnExistingFileAndKeepsItsOtherLines` (Task 2).
3. **`--env-file` pointing into a directory that does not exist.** The CLI must refuse before the server creates the
   app, or the user is left with an orphan app and a key nobody saw. Pinned by
   `AutomationAppCommandTest.testCreateRefusesAnEnvFileInAMissingDirectoryBeforeCreatingTheApp` (Task 3).
4. **`status` run by a workspace member who is not an admin** (`APP_VIEW` but not `APP_MANAGE`): the
   bindable-connections lookup answers 403. `status` must still show the app and its bindings, with
   `componentVersion: null`. Pinned by `AutomationAppCommandTest.testStatusShowsNoComponentVersionWhenTheLookupIsForbidden`
   (Task 3).
5. **Server error bodies.** A 409 `APP_BINDING_KEY_ALREADY_EXISTS`, a 403 `ACCESS_DENIED` from a facade guard and an
   unreachable host must each reach the user with the server's code and message (or "Cannot reach"), the next step and
   the right exit code, not a bare "HTTP 409". Pinned by `AutomationAppClientTest` (Task 1) and
   `AutomationAppBindingCommandTest.testBindConnectionSurfacesTheServerErrorCode` (Task 4).

---

## File structure

| File | Responsibility |
|---|---|
| `settings.gradle.kts` | include `cli:clients:automation-app` |
| `cli/clients/automation-app/{build.gradle.kts,.gitignore,openapi.yaml,generated/src/**}` | the generated management client and its contract copy |
| `cli/commands/automation/build.gradle.kts` | depend on the new client |
| `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationClientFactory.java` | app client builders, app error mapping |
| `…/automation/AppLink.java` | the `.bytechef/app.json` record |
| `…/automation/AppLinkFile.java` | walk-up lookup, read, write of `.bytechef/app.json` |
| `…/automation/EnvFile.java` | merge, owner-only atomic write, git-ignore status |
| `…/automation/AppCommandSupport.java` | DEVELOPMENT guard, workspace requirement, app location, env file location, warnings |
| `…/automation/AutomationAppCommand.java` | `create`, `link`, `list`, `status`, `key rotate` |
| `…/automation/AutomationAppBindingCommand.java` | `bind connection`, `bind workflow`, `unbind connection`, `unbind workflow` |
| `…/automation/AutomationConnectionCommand.java` | `automation connection list` |
| `cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java` | register the three classes |
| `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/{RoutedStubApi,GitTestRepository}.java` | test helpers |
| `…/test/…/automation/{AutomationAppClientTest,AppLinkFileTest,EnvFileTest,AutomationAppCommandTest,AutomationAppBindingCommandTest,AutomationConnectionCommandTest}.java` | tests |
| `cli/README.md` | the app commands |
| `sdks/frontend/automation/app/create/template/{AGENTS.md,README.md}` | the loop |
| `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/{CliCommandInventory,AppTemplateCliCommandDriftTest}.java` | the drift test |
| `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/PluginSkillTagDriftIntTest.java` | use `CliCommandInventory` |
| `claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md` | the plugin skill |
| `claude-code-plugin/{README.md,bytechef-dev/README.md,bytechef-dev/skills/bytechef-mcp-setup/SKILL.md}` | the README exception, the skill table, the setup line |
| `server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md` | regenerated |
| `server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/config/McpInstructionAssemblyTest.java` | the app-loop section is EE-keyed |
| `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`, `docs/superpowers/specs/2026-10-07-apps-template-design.md`, `.agents/apps.md` | the D2 edits |

---

### Task 1: The `automation-app` client, its contract copy and the app error mapping

**Files:**
- Modify: `settings.gradle.kts:31` (insert one line before it)
- Create: `cli/clients/automation-app/build.gradle.kts`
- Create: `cli/clients/automation-app/.gitignore`
- Create: `cli/clients/automation-app/openapi.yaml`
- Create (generated, committed): `cli/clients/automation-app/generated/src/**`
- Modify: `cli/commands/automation/build.gradle.kts`
- Modify: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationClientFactory.java`
- Create: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/RoutedStubApi.java`
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppClientTest.java`

**Interfaces:**
- Consumes: `com.bytechef.cli.core.config.CliConfig(String host, String token, Environment environment, Long workspaceId)`,
  `AuthInterceptor`, `CliException(int exitCode, String message)`.
- Produces (package `com.bytechef.cli.command.automation`, all `static`, package-private):
  - `com.bytechef.cli.client.automationapp.ApiClient appApiClient(CliConfig config)`
  - `AppApi appApi(CliConfig config)`, `AppWorkflowBindingApi appWorkflowBindingApi(CliConfig config)`,
    `WorkspaceConnectionApi workspaceConnectionApi(CliConfig config)` (`com.bytechef.cli.client.automationapp.api.*`)
  - `CliException toCliException(com.bytechef.cli.client.automationapp.ApiException exception)` — 0 → exit 1
    "Cannot reach ByteChef: …"; 401 → exit 2 with the DEVELOPMENT-key hint; 403 → exit 2 with the admin-role hint;
    404 → exit 3; else exit 1; each carries `": CODE: message"` from the `{code, message}` body when present.
  - Generated models (package `com.bytechef.cli.client.automationapp.model`): `AppModel`, `AppSecretKeyModel`,
    `AppInputModel`, `AppHostingTargetModel`, `AppBindingModel`, `AppBindingInputModel`, `AppWorkflowBindingModel`,
    `AppWorkflowBindingInputModel`, `AppBindableConnectionModel`, `AppBindableWorkflowModel`,
    `AppComponentOperationsModel`, `AppWorkspaceConnectionModel`, `EnvironmentModel`.
  - Test helper `RoutedStubApi` (package-private): `static RoutedStubApi start(Map<String, Response> routes)` keyed
    `"METHOD /path"`; `static Response response(int status, String body)`; `String host()`;
    `List<Request> requests()`; `Optional<Request> lastRequest(String method, String path)`;
    `record Request(String method, String path, String query, String body, String authorization, String environment)`.

- [ ] **Step 1: Register the module**

In `settings.gradle.kts`, insert before line 31 (`include("cli:clients:automation-configuration")`):

```kotlin
include("cli:clients:automation-app")
```

Create `cli/clients/automation-app/build.gradle.kts`:

```kotlin
plugins {
    id("com.bytechef.java-library-conventions")
    alias(libs.plugins.org.openapi.generator)
}

val generateClient by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("java")
    library.set("native")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$projectDir/generated")
    apiPackage.set("com.bytechef.cli.client.automationapp.api")
    modelPackage.set("com.bytechef.cli.client.automationapp.model")
    invokerPackage.set("com.bytechef.cli.client.automationapp")
    modelNameSuffix.set("Model")
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "useTags" to "true",
            "hideGenerationTimestamp" to "true",
            "openApiNullable" to "false"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

listOf("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest").forEach { taskName ->
    tasks.matching { it.name == taskName }
        .configureEach { enabled = false }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.apache.httpcomponents:httpmime:4.5.14")
}
```

Create `cli/clients/automation-app/.gitignore` (same as `cli/clients/automation-configuration/.gitignore`):

```
# Only the generated Java client sources are committed; the openapi-generator
# scaffolding (build files, CI, docs, its own .gitignore) is regenerated and ignored.
/generated/*
!/generated/src/
```

In `cli/commands/automation/build.gradle.kts`, add after `implementation(project(":cli:cli-core"))`:

```kotlin
    implementation(project(":cli:clients:automation-app"))
```

- [ ] **Step 2: Write the contract copy**

```bash
cp server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml \
  cli/clients/automation-app/openapi.yaml
```

Then edit `cli/clients/automation-app/openapi.yaml` exactly as follows. The server's `Environment` is a relative
`$ref` that does not resolve from `cli/clients/automation-app`, so the copy inlines it (without the server file's
`readOnly`). **This copy must be re-synced from the server spec once D1 lands (Task 7).**

(a) Replace

```yaml
    Environment:
      $ref: "../../../../../libs/platform/platform-rest/openapi/components/schemas/objects/environment.yaml"
```

with

```yaml
    Environment:
      description: "The environment."
      type: "string"
      enum:
        - "DEVELOPMENT"
        - "STAGING"
        - "PRODUCTION"
```

(b) After the two lines

```yaml
  - name: "app-workflow-binding"
    description: "The Automation App Workflow Binding Management Public API"
```

insert

```yaml
  - name: "workspace-connection"
    description: "The Automation Workspace Connection Management Public API"
```

(c) Insert immediately before the line `  /apps/{id}:`:

```yaml
  /apps/component-operations:
    get:
      description: "Get the names of the actions and agent tools a component version offers: the values the allow-lists of a connection binding accept."
      summary: "Get the operations of a component"
      tags:
        - "app"
      operationId: "getAppComponentOperations"
      security:
        - bearerAuth: []
      parameters:
        - name: "componentName"
          description: "The name of a component."
          in: "query"
          required: true
          schema:
            type: "string"
        - name: "componentVersion"
          description: "The version of the component."
          in: "query"
          required: true
          schema:
            type: "integer"
            format: "int32"
      responses:
        "200":
          description: "The names of the component's actions and agent tools."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AppComponentOperations"
        "400":
          $ref: "#/components/responses/badRequest"
        "401":
          $ref: "#/components/responses/unauthorized"
        "403":
          $ref: "#/components/responses/forbidden"
        "404":
          $ref: "#/components/responses/notFound"
        "409":
          $ref: "#/components/responses/conflict"
        "500":
          $ref: "#/components/responses/internalServerError"
```

(d) Insert immediately before the line `  /apps/{id}/connections:`:

```yaml
  /apps/{id}/bindable-connections:
    get:
      description: "Get the connections that can be bound to an app: those of the app's workspace and environment that the caller can see. A connection is usable when its credentials are valid and the caller may use it."
      summary: "Get the bindable connections of an app"
      tags:
        - "app"
      operationId: "getAppBindableConnections"
      security:
        - bearerAuth: []
      parameters:
        - $ref: "#/components/parameters/appId"
        - name: "componentName"
          description: "Only connections of this component."
          in: "query"
          required: false
          schema:
            type: "string"
      responses:
        "200":
          description: "The bindable connections."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/AppBindableConnection"
        "400":
          $ref: "#/components/responses/badRequest"
        "401":
          $ref: "#/components/responses/unauthorized"
        "403":
          $ref: "#/components/responses/forbidden"
        "404":
          $ref: "#/components/responses/notFound"
        "409":
          $ref: "#/components/responses/conflict"
        "500":
          $ref: "#/components/responses/internalServerError"
```

(e) Insert immediately before the line `  /apps/{id}/workflows:`:

```yaml
  /apps/{id}/bindable-workflows:
    get:
      description: "Get the deployed app-callable workflows that can be bound to an app."
      summary: "Get the bindable workflows of an app"
      tags:
        - "app-workflow-binding"
      operationId: "getAppBindableWorkflows"
      security:
        - bearerAuth: []
      parameters:
        - $ref: "#/components/parameters/appId"
      responses:
        "200":
          description: "The bindable workflows."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/AppBindableWorkflow"
        "400":
          $ref: "#/components/responses/badRequest"
        "401":
          $ref: "#/components/responses/unauthorized"
        "403":
          $ref: "#/components/responses/forbidden"
        "404":
          $ref: "#/components/responses/notFound"
        "409":
          $ref: "#/components/responses/conflict"
        "500":
          $ref: "#/components/responses/internalServerError"
```

(f) Insert immediately before the line `components:`:

```yaml
  /connections:
    get:
      description: "Get the connections of a workspace in an environment. A connection never carries its parameters or credentials."
      summary: "Get the connections of a workspace"
      tags:
        - "workspace-connection"
      operationId: "getWorkspaceConnections"
      security:
        - bearerAuth: []
      parameters:
        - name: "workspaceId"
          description: "The id of a workspace."
          in: "query"
          required: true
          schema:
            type: "integer"
            format: "int64"
        - name: "componentName"
          description: "Only connections of this component."
          in: "query"
          required: false
          schema:
            type: "string"
        - name: "X-Environment"
          description: "The environment. When omitted, PRODUCTION is used."
          in: "header"
          required: false
          schema:
            $ref: "#/components/schemas/Environment"
      responses:
        "200":
          description: "The connections."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/AppWorkspaceConnection"
        "400":
          $ref: "#/components/responses/badRequest"
        "401":
          $ref: "#/components/responses/unauthorized"
        "403":
          $ref: "#/components/responses/forbidden"
        "404":
          $ref: "#/components/responses/notFound"
        "409":
          $ref: "#/components/responses/conflict"
        "500":
          $ref: "#/components/responses/internalServerError"
```

(g) Append at the end of the file (after the `AppWorkflowBindingInput` schema, same indentation as the other
schemas):

```yaml
    AppBindableConnection:
      description: "A connection that can be bound to an app."
      type: "object"
      properties:
        id:
          type: "integer"
          format: "int64"
        name:
          type: "string"
        componentName:
          type: "string"
        connectionVersion:
          type: "integer"
          format: "int32"
        componentVersion:
          type: "integer"
          format: "int32"
        usable:
          description: "Whether the connection's credentials are valid and the caller may use it."
          type: "boolean"
    AppBindableWorkflow:
      description: "A deployed app-callable workflow that can be bound to an app."
      type: "object"
      properties:
        projectDeploymentId:
          type: "integer"
          format: "int64"
        projectDeploymentName:
          type: "string"
        projectName:
          type: "string"
        workflowUuid:
          type: "string"
        workflowName:
          type: "string"
        description:
          type: "string"
    AppComponentOperations:
      description: "The names of the actions and agent tools of a component version."
      type: "object"
      properties:
        actionNames:
          type: "array"
          items:
            type: "string"
        toolNames:
          type: "array"
          items:
            type: "string"
    AppWorkspaceConnection:
      description: "A connection of a workspace. It never carries parameters or credentials."
      type: "object"
      properties:
        id:
          type: "integer"
          format: "int64"
        name:
          type: "string"
        componentName:
          type: "string"
        connectionVersion:
          type: "integer"
          format: "int32"
        authorizationType:
          type: "string"
        credentialStatus:
          type: "string"
        aiProvider:
          description: "Whether this is a virtual AI-provider connection, which never fills a deployment connection slot."
          type: "boolean"
        usable:
          description: "Whether the connection's credentials are valid and the caller may use it."
          type: "boolean"
```

- [ ] **Step 3: Generate the client**

```bash
./gradlew :cli:clients:automation-app:generateClient > "$SCRATCH/d2-t1-gen.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|ERROR' "$SCRATCH/d2-t1-gen.log"
git status --short cli/clients/automation-app
ls cli/clients/automation-app/generated/src/main/java/com/bytechef/cli/client/automationapp/api
```

Expected: `exit=0`; `git status` lists only `.gitignore`, `build.gradle.kts`, `openapi.yaml` and `generated/src/`;
the `api` directory holds `AppApi.java`, `AppWorkflowBindingApi.java`, `WorkspaceConnectionApi.java`.

Verify the generated signatures the later tasks call:

```bash
grep -hE 'public (List<[A-Za-z]+Model>|[A-Za-z]+Model|void) (getApps|createApp|getApp|rotateAppSecretKey|getAppBindings|bindAppConnection|unbindAppConnection|getAppBindableConnections|getAppComponentOperations|getAppWorkflowBindings|bindAppWorkflow|unbindAppWorkflow|getAppBindableWorkflows|getWorkspaceConnections)\(' \
  cli/clients/automation-app/generated/src/main/java/com/bytechef/cli/client/automationapp/api/*.java
```

Expected (one line each, ignoring the `@jakarta.annotation` annotations and the `Map<String, String> headers`
overloads): `getApps(Long workspaceId, EnvironmentModel xEnvironment)`, `createApp(Long workspaceId, AppInputModel
appInputModel, EnvironmentModel xEnvironment)`, `getApp(Long id)`, `rotateAppSecretKey(Long id)`,
`getAppBindings(Long id)`, `bindAppConnection(Long id, String bindingKey, AppBindingInputModel appBindingInputModel)`,
`unbindAppConnection(Long id, String bindingKey)`, `getAppBindableConnections(Long id, String componentName)`,
`getAppComponentOperations(String componentName, Integer componentVersion)`, `getAppWorkflowBindings(Long id)`,
`bindAppWorkflow(Long id, String workflowKey, AppWorkflowBindingInputModel appWorkflowBindingInputModel)`,
`unbindAppWorkflow(Long id, String workflowKey)`, `getAppBindableWorkflows(Long id)`,
`getWorkspaceConnections(Long workspaceId, String componentName, EnvironmentModel xEnvironment)`.

- [ ] **Step 4: Write the stub helper and the failing test**

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/RoutedStubApi.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An in-process HTTP stub that answers each {@code METHOD /path} with its own status and body and records every
 * request, so a command that makes several calls can be driven end to end. A request without a route gets a 404 whose
 * error body names it.
 *
 * @author Ivica Cardic
 */
final class RoutedStubApi implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, Response> routes;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    private RoutedStubApi(HttpServer server, Map<String, Response> routes) {
        this.server = server;
        this.routes = Map.copyOf(routes);
    }

    static RoutedStubApi start(Map<String, Response> routes) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);

        RoutedStubApi stub = new RoutedStubApi(server, routes);

        server.createContext("/", stub::handle);
        server.start();

        return stub;
    }

    static Response response(int status, String body) {
        return new Response(status, body);
    }

    String host() {
        InetSocketAddress address = server.getAddress();

        return "http://localhost:" + address.getPort();
    }

    List<Request> requests() {
        return List.copyOf(requests);
    }

    Optional<Request> lastRequest(String method, String path) {
        Request lastRequest = null;

        for (Request request : requests) {
            if (request.method()
                .equals(method) &&
                request.path()
                    .equals(path)) {

                lastRequest = request;
            }
        }

        return Optional.ofNullable(lastRequest);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        Headers requestHeaders = exchange.getRequestHeaders();
        String method = exchange.getRequestMethod();
        String path = requestUri.getPath();
        String requestBody;

        try (InputStream inputStream = exchange.getRequestBody()) {
            requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        requests.add(
            new Request(
                method, path, requestUri.getRawQuery(), requestBody, requestHeaders.getFirst("Authorization"),
                requestHeaders.getFirst("X-Environment")));

        Response response = routes.getOrDefault(
            method + " " + path,
            new Response(404, "{\"code\":\"STUB_NOT_FOUND\",\"message\":\"No stub for " + method + " " + path + "\"}"));
        byte[] responseBody = response.body()
            .getBytes(StandardCharsets.UTF_8);
        Headers responseHeaders = exchange.getResponseHeaders();

        responseHeaders.add("Content-Type", "application/json");

        exchange.sendResponseHeaders(response.status(), responseBody.length == 0 ? -1 : responseBody.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        }
    }

    record Request(String method, String path, String query, String body, String authorization, String environment) {
    }

    record Response(int status, String body) {
    }
}
```

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppClientTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.client.automationapp.ApiException;
import com.bytechef.cli.client.automationapp.api.AppApi;
import com.bytechef.cli.client.automationapp.api.AppWorkflowBindingApi;
import com.bytechef.cli.client.automationapp.api.WorkspaceConnectionApi;
import com.bytechef.cli.client.automationapp.model.AppBindableConnectionModel;
import com.bytechef.cli.client.automationapp.model.AppBindableWorkflowModel;
import com.bytechef.cli.client.automationapp.model.AppBindingInputModel;
import com.bytechef.cli.client.automationapp.model.AppComponentOperationsModel;
import com.bytechef.cli.client.automationapp.model.AppWorkspaceConnectionModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.error.CliException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The generated management client against the contract: paths, query parameters, headers, the new fields, and how a
 * server error reaches the user.
 *
 * @author Ivica Cardic
 */
class AutomationAppClientTest {

    private static CliConfig config(String host) {
        return new CliConfig(host, "btc_x", Environment.DEVELOPMENT, 1L);
    }

    @Test
    void testBindableConnectionsCarryTheComponentFilterAndTheUsableFlag() throws Exception {
        String body = """
            [{"id":12,"name":"HubSpot","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET /api/automation/v1/apps/42/bindable-connections", RoutedStubApi.response(200, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));

            List<AppBindableConnectionModel> connections = appApi.getAppBindableConnections(42L, "hubspot");

            RoutedStubApi.Request request = stub.lastRequest("GET", "/api/automation/v1/apps/42/bindable-connections")
                .orElseThrow();

            assertEquals("componentName=hubspot", request.query());
            assertEquals("Bearer btc_x", request.authorization());
            assertEquals("DEVELOPMENT", request.environment());
            assertEquals(1, connections.size());

            AppBindableConnectionModel connection = connections.getFirst();

            assertEquals(12L, connection.getId());
            assertEquals(3, connection.getComponentVersion());
            assertEquals(Boolean.TRUE, connection.getUsable());
        }
    }

    @Test
    void testComponentOperationsSendBothQueryParameters() throws Exception {
        String body = """
            {"actionNames":["createContact"],"toolNames":["searchContacts"]}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET /api/automation/v1/apps/component-operations", RoutedStubApi.response(200, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));

            AppComponentOperationsModel operations = appApi.getAppComponentOperations("hubspot", 3);

            RoutedStubApi.Request request = stub.lastRequest("GET", "/api/automation/v1/apps/component-operations")
                .orElseThrow();

            assertEquals("componentName=hubspot&componentVersion=3", request.query());
            assertEquals(List.of("createContact"), operations.getActionNames());
            assertEquals(List.of("searchContacts"), operations.getToolNames());
        }
    }

    @Test
    void testBindableWorkflowsAreReadByUuid() throws Exception {
        String body = """
            [{"projectDeploymentId":7,"projectDeploymentName":"Onboarding","projectName":"Onboarding","workflowUuid":"wf-uuid","workflowName":"Onboard customer"}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET /api/automation/v1/apps/42/bindable-workflows", RoutedStubApi.response(200, body)))) {

            AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(
                config(stub.host()));

            List<AppBindableWorkflowModel> workflows = appWorkflowBindingApi.getAppBindableWorkflows(42L);

            assertEquals("wf-uuid", workflows.getFirst()
                .getWorkflowUuid());
            assertEquals(7L, workflows.getFirst()
                .getProjectDeploymentId());
        }
    }

    @Test
    void testWorkspaceConnectionsCarryTheWorkspaceAndTheAiProviderFlag() throws Exception {
        String body = """
            [{"id":5,"name":"OpenAI","componentName":"openai","connectionVersion":1,"authorizationType":"API_KEY","credentialStatus":"VALID","aiProvider":true,"usable":true}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET /api/automation/v1/connections", RoutedStubApi.response(200, body)))) {

            WorkspaceConnectionApi workspaceConnectionApi = AutomationClientFactory.workspaceConnectionApi(
                config(stub.host()));

            List<AppWorkspaceConnectionModel> connections = workspaceConnectionApi.getWorkspaceConnections(
                1L, null, null);

            RoutedStubApi.Request request = stub.lastRequest("GET", "/api/automation/v1/connections")
                .orElseThrow();

            assertEquals("workspaceId=1", request.query());
            assertEquals(Boolean.TRUE, connections.getFirst()
                .getAiProvider());
        }
    }

    @Test
    void testAnErrorBodyIsCarriedIntoTheCliException() throws Exception {
        String body = """
            {"code":"APP_BINDING_KEY_ALREADY_EXISTS","message":"The binding key crm is already used."}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("PUT /api/automation/v1/apps/42/connections/crm", RoutedStubApi.response(409, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));
            AppBindingInputModel appBindingInputModel = new AppBindingInputModel();

            appBindingInputModel.setConnectionId(12L);

            ApiException apiException = assertThrows(
                ApiException.class, () -> appApi.bindAppConnection(42L, "crm", appBindingInputModel));

            CliException cliException = AutomationClientFactory.toCliException(apiException);

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("HTTP 409"), cliException.getMessage());
            assertTrue(
                cliException.getMessage()
                    .contains("APP_BINDING_KEY_ALREADY_EXISTS: The binding key crm is already used"),
                cliException.getMessage());
        }
    }

    @Test
    void testForbiddenMapsToExitCode2AndNamesTheAdminRole() throws Exception {
        String body = """
            {"code":"ACCESS_DENIED","message":"Access Denied"}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET /api/automation/v1/apps/42", RoutedStubApi.response(403, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));

            ApiException apiException = assertThrows(ApiException.class, () -> appApi.getApp(42L));

            CliException cliException = AutomationClientFactory.toCliException(apiException);

            assertEquals(2, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("ACCESS_DENIED"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("workspace admin role"), cliException.getMessage());
        }
    }

    @Test
    void testAnUnreachableHostMapsToExitCode1() throws Exception {
        String host;

        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            host = stub.host();
        }

        AppApi appApi = AutomationClientFactory.appApi(config(host));

        ApiException apiException = assertThrows(ApiException.class, () -> appApi.getApp(42L));

        CliException cliException = AutomationClientFactory.toCliException(apiException);

        assertEquals(1, cliException.exitCode());
        assertTrue(cliException.getMessage()
            .startsWith("Cannot reach ByteChef"), cliException.getMessage());
    }
}
```

- [ ] **Step 5: Run the test to verify it fails**

```bash
./gradlew :cli:commands:automation:test --tests com.bytechef.cli.command.automation.AutomationAppClientTest > "$SCRATCH/d2-t1-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/d2-t1-red.log"
```

Expected: `exit=1`, `> Task :cli:commands:automation:compileTestJava FAILED` with `cannot find symbol … appApi` /
`appWorkflowBindingApi` / `workspaceConnectionApi` / `toCliException(com.bytechef.cli.client.automationapp.ApiException)`.

- [ ] **Step 6: Add the app builders and the error mapping to `AutomationClientFactory`**

Add these imports to `AutomationClientFactory.java` (keep the existing ones):

```java
import com.bytechef.cli.client.automationapp.api.AppApi;
import com.bytechef.cli.client.automationapp.api.AppWorkflowBindingApi;
import com.bytechef.cli.client.automationapp.api.WorkspaceConnectionApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
```

Add a field after `AUTOMATION_API_PATH`:

```java
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
```

Add these methods after `dataTableRowApi(CliConfig config)`:

```java
    /**
     * The generated app client's {@code ApiClient} is a distinct type per generated package, like the data-table one.
     */
    static com.bytechef.cli.client.automationapp.ApiClient appApiClient(CliConfig config) {
        com.bytechef.cli.client.automationapp.ApiClient apiClient =
            new com.bytechef.cli.client.automationapp.ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, AUTOMATION_API_PATH));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static AppApi appApi(CliConfig config) {
        return new AppApi(appApiClient(config));
    }

    static AppWorkflowBindingApi appWorkflowBindingApi(CliConfig config) {
        return new AppWorkflowBindingApi(appApiClient(config));
    }

    static WorkspaceConnectionApi workspaceConnectionApi(CliConfig config) {
        return new WorkspaceConnectionApi(appApiClient(config));
    }
```

Add this method after `toCliException(com.bytechef.cli.client.automationdatatable.ApiException exception)`:

```java
    /**
     * Maps an app client failure onto the CLI's exit codes, carrying the server's {@code {code, message}} error body and
     * the next step, so a coding agent can correct itself. Status 0 is the generated client's code for an I/O failure.
     */
    static CliException toCliException(com.bytechef.cli.client.automationapp.ApiException exception) {
        int status = exception.getCode();

        if (status == 0) {
            return new CliException(
                1, "Cannot reach ByteChef: " + Objects.requireNonNullElse(exception.getMessage(), "no response") + ".");
        }

        String detail = appErrorDetail(exception.getResponseBody());

        if (status == 401) {
            return new CliException(
                2, "Authentication failed (HTTP 401)" + detail + ". Use an automation API key created in DEVELOPMENT "
                    + "(bytechef configure --environment DEVELOPMENT --token <key>).");
        }

        if (status == 403) {
            return new CliException(
                2, "Not allowed (HTTP 403)" + detail + ". Managing apps needs the workspace admin role in DEVELOPMENT.");
        }

        if (status == 404) {
            return new CliException(3, "Not found (HTTP 404)" + detail + ".");
        }

        return new CliException(1, "Request failed (HTTP " + status + ")" + detail + ".");
    }

    private static String appErrorDetail(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        JsonNode errorNode;

        try {
            errorNode = OBJECT_MAPPER.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            return "";
        }

        JsonNode codeNode = errorNode.path("code");
        JsonNode messageNode = errorNode.path("message");

        String code = codeNode.asText("");
        String message = messageNode.asText("");

        while (message.endsWith(".")) {
            message = message.substring(0, message.length() - 1);
        }

        if (code.isEmpty() && message.isEmpty()) {
            return "";
        }

        if (code.isEmpty()) {
            return ": " + message;
        }

        return ": " + code + ": " + message;
    }
```

- [ ] **Step 7: Run the test to verify it passes**

```bash
./gradlew :cli:commands:automation:test --tests com.bytechef.cli.command.automation.AutomationAppClientTest > "$SCRATCH/d2-t1-green.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|tests completed|FAILED' "$SCRATCH/d2-t1-green.log"
```

Expected: `exit=0`, no `FAILED`.

- [ ] **Step 8: Format and check every touched module**

```bash
./gradlew :cli:clients:automation-app:spotlessApply :cli:commands:automation:spotlessApply > "$SCRATCH/d2-t1-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :cli:clients:automation-app:check :cli:commands:automation:check :cli:cli-app:check > "$SCRATCH/d2-t1-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t1-check.log"
```

Expected: both `exit=0`, no `FAILED` line. A SpotBugs finding in `RoutedStubApi` or the factory is fixed in code, not
suppressed, unless it is a `PATH_TRAVERSAL_*`/`COMMAND_INJECTION` pattern on a user-chosen path.

- [ ] **Step 9: Commit**

```bash
git add settings.gradle.kts cli/clients/automation-app cli/commands/automation/build.gradle.kts \
  cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationClientFactory.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/RoutedStubApi.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppClientTest.java
git status --short
git commit -m "- cli - Add the automation app CLI client"
```

---

### Task 2: `.bytechef/app.json` and the env file

**Files:**
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppLink.java`
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppLinkFile.java`
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/EnvFile.java`
- Create: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/GitTestRepository.java`
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AppLinkFileTest.java`
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/EnvFileTest.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces (package `com.bytechef.cli.command.automation`):
  - `public record AppLink(long id, String uuid, long workspaceId, String environment, String host)`
  - `final class AppLinkFile`: `static Path pathIn(Path appDirectory)`; `static Optional<Path> findAppDirectory(Path
    start)`; `static AppLink read(Path appDirectory) throws IOException`; `static void write(Path appDirectory, AppLink
    appLink) throws IOException`.
  - `final class EnvFile`: `static final String BYTECHEF_URL`, `BYTECHEF_APP_KEY`; `enum GitIgnoreStatus {IGNORED,
    NOT_IGNORED, UNKNOWN}`; `static List<String> merge(List<String> lines, Map<String, String> values)`;
    `static void writeBytechefValues(Path envFile, String bytechefUrl, String appKey) throws IOException`;
    `static GitIgnoreStatus gitIgnoreStatus(Path envFile)`.
  - Test helper `final class GitTestRepository`: `static Path create(Path directory, String gitignoreContent) throws
    IOException, InterruptedException` — `git init` in `directory` and write its `.gitignore`.

- [ ] **Step 1: Write the failing tests**

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/GitTestRepository.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Creates a throwaway git repository with its own {@code .gitignore}. A repository's {@code .gitignore} outranks the
 * user's global excludes file, so a test can pin a file as ignored or not ignored whatever the machine's git setup.
 *
 * @author Ivica Cardic
 */
final class GitTestRepository {

    private static final long GIT_TIMEOUT_SECONDS = 30;

    private GitTestRepository() {
    }

    @SuppressFBWarnings(
        value = "COMMAND_INJECTION", justification = "Runs the fixed git executable on a test-owned temporary directory.")
    static Path create(Path directory, String gitignoreContent) throws IOException, InterruptedException {
        Files.createDirectories(directory);

        ProcessBuilder processBuilder = new ProcessBuilder("git", "init", "-q", directory.toString());

        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();

        if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();

            throw new IllegalStateException("git init did not finish in " + GIT_TIMEOUT_SECONDS + " seconds");
        }

        if (process.exitValue() != 0) {
            throw new IllegalStateException("git init failed with exit code " + process.exitValue());
        }

        Files.writeString(directory.resolve(".gitignore"), gitignoreContent);

        return directory;
    }
}
```

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AppLinkFileTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AppLinkFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteThenReadRoundTripsWithoutASecret() throws IOException {
        AppLink appLink = new AppLink(42, "app-uuid", 1, "DEVELOPMENT", "http://localhost:9555");

        AppLinkFile.write(tempDir, appLink);

        assertEquals(appLink, AppLinkFile.read(tempDir));

        String content = Files.readString(tempDir.resolve(".bytechef/app.json"));

        assertTrue(content.contains("\"uuid\" : \"app-uuid\""), content);
        assertFalse(content.toLowerCase()
            .contains("key"), content);
    }

    @Test
    void testFindWalksUpFromANestedDirectory() throws IOException {
        Path appDirectory = tempDir.resolve("app");

        AppLinkFile.write(appDirectory, new AppLink(42, "app-uuid", 1, "DEVELOPMENT", "http://localhost:9555"));

        Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src/server"));

        Path expectedDirectory = appDirectory.toAbsolutePath()
            .normalize();

        assertEquals(Optional.of(expectedDirectory), AppLinkFile.findAppDirectory(nestedDirectory));
    }

    @Test
    void testFindSkipsABytechefDirectoryWithoutAppJson() throws IOException {
        Path homeDirectory = tempDir.resolve("home");

        Files.createDirectories(homeDirectory.resolve(".bytechef"));
        Files.writeString(homeDirectory.resolve(".bytechef/config"), "[default]\nhost = http://localhost:9555\n");

        Path projectDirectory = Files.createDirectories(homeDirectory.resolve("projects/my-app"));

        assertEquals(Optional.empty(), AppLinkFile.findAppDirectory(projectDirectory));
    }
}
```

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/EnvFileTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class EnvFileTest {

    @TempDir
    Path tempDir;

    private static Map<String, String> values(String bytechefUrl, String appKey) {
        Map<String, String> values = new LinkedHashMap<>();

        values.put(EnvFile.BYTECHEF_URL, bytechefUrl);
        values.put(EnvFile.BYTECHEF_APP_KEY, appKey);

        return values;
    }

    private static boolean isPosix(Path path) {
        return path.getFileSystem()
            .supportedFileAttributeViews()
            .contains("posix");
    }

    private List<String> fileNamesIn(Path directory) throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.map(path -> String.valueOf(path.getFileName()))
                .sorted()
                .toList();
        }
    }

    @Test
    void testMergeReplacesOnlyTheByteChefLinesAndKeepsEveryOtherLine() {
        List<String> lines = List.of(
            "# local settings", "OTHER_SECRET=keep-me", "export BYTECHEF_URL=https://old.example",
            "BYTECHEF_APP_KEY=old-key", "", "LAST=1");

        List<String> mergedLines = EnvFile.merge(lines, values("https://new.example", "new-key"));

        assertEquals(
            List.of(
                "# local settings", "OTHER_SECRET=keep-me", "export BYTECHEF_URL=https://new.example",
                "BYTECHEF_APP_KEY=new-key", "", "LAST=1"),
            mergedLines);
    }

    @Test
    void testMergeAppendsMissingKeysAndLeavesCommentedOutKeysAlone() {
        List<String> lines = List.of("OTHER=1", "# BYTECHEF_APP_KEY=commented");

        List<String> mergedLines = EnvFile.merge(lines, values("https://new.example", "new-key"));

        assertEquals(
            List.of(
                "OTHER=1", "# BYTECHEF_APP_KEY=commented", "BYTECHEF_URL=https://new.example",
                "BYTECHEF_APP_KEY=new-key"),
            mergedLines);
    }

    @Test
    void testWriteCreatesAnOwnerOnlyFile() throws IOException {
        assumeTrue(isPosix(tempDir));

        Path envFile = tempDir.resolve(".env.local");

        EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-1");

        assertEquals(
            "BYTECHEF_URL=https://bytechef.example\nBYTECHEF_APP_KEY=app-key-1\n", Files.readString(envFile));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(envFile));
    }

    @Test
    void testWriteTightensAnExistingFileAndKeepsItsOtherLines() throws IOException {
        assumeTrue(isPosix(tempDir));

        Path envFile = tempDir.resolve(".env.local");

        Files.writeString(envFile, "OTHER_SECRET=keep-me\nBYTECHEF_APP_KEY=old-key\n\n# end\n");
        Files.setPosixFilePermissions(envFile, PosixFilePermissions.fromString("rw-r--r--"));

        EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-2");

        assertEquals(
            "OTHER_SECRET=keep-me\nBYTECHEF_APP_KEY=app-key-2\n\n# end\nBYTECHEF_URL=https://bytechef.example\n",
            Files.readString(envFile));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(envFile));
    }

    @Test
    void testWriteLeavesNoTemporaryFileBehind() throws IOException {
        EnvFile.writeBytechefValues(tempDir.resolve(".env.local"), "https://bytechef.example", "app-key-3");

        assertEquals(List.of(".env.local"), fileNamesIn(tempDir));
    }

    @Test
    void testWriteOntoADirectoryFailsAndLeavesNoTemporaryFile() throws IOException {
        Path envDirectory = tempDir.resolve("env");

        Files.createDirectories(envDirectory.resolve("inside"));

        assertThrows(
            IOException.class,
            () -> EnvFile.writeBytechefValues(envDirectory, "https://bytechef.example", "app-key-4"));
        assertEquals(List.of("env"), fileNamesIn(tempDir));
    }

    @Test
    void testGitIgnoreStatusIsIgnoredWhenTheRepositoryIgnoresTheFile() throws Exception {
        Path repository = GitTestRepository.create(tempDir.resolve("repository"), ".env.local\n");
        Path envFile = repository.resolve(".env.local");

        Files.writeString(envFile, "X=1\n");

        assertEquals(EnvFile.GitIgnoreStatus.IGNORED, EnvFile.gitIgnoreStatus(envFile));
    }

    @Test
    void testGitIgnoreStatusIsNotIgnoredWhenTheRepositoryUnignoresTheFile() throws Exception {
        Path repository = GitTestRepository.create(tempDir.resolve("repository"), "!.env.local\n");
        Path envFile = repository.resolve(".env.local");

        Files.writeString(envFile, "X=1\n");

        assertEquals(EnvFile.GitIgnoreStatus.NOT_IGNORED, EnvFile.gitIgnoreStatus(envFile));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :cli:commands:automation:test --tests 'com.bytechef.cli.command.automation.AppLinkFileTest' --tests 'com.bytechef.cli.command.automation.EnvFileTest' > "$SCRATCH/d2-t2-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/d2-t2-red.log"
```

Expected: `exit=1`, `compileTestJava FAILED` with `cannot find symbol … AppLink` / `AppLinkFile` / `EnvFile`.

- [ ] **Step 3: Write `AppLink`, `AppLinkFile` and `EnvFile`**

Create `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppLink.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

/**
 * The contents of {@code .bytechef/app.json}: which app a directory belongs to and the ByteChef target it was made on.
 * It never holds a secret, so it is safe to commit.
 *
 * @author Ivica Cardic
 */
public record AppLink(long id, String uuid, long workspaceId, String environment, String host) {
}
```

Create `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppLinkFile.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Finds, reads and writes {@code .bytechef/app.json}. The lookup walks up from a directory like git does and keys on
 * the {@code app.json} file, never on the {@code .bytechef} directory alone, because the CLI keeps its own
 * configuration in {@code ~/.bytechef/}.
 *
 * @author Ivica Cardic
 */
final class AppLinkFile {

    static final String DIRECTORY_NAME = ".bytechef";
    static final String FILE_NAME = "app.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    private AppLinkFile() {
    }

    static Path pathIn(Path appDirectory) {
        Path linkDirectory = appDirectory.resolve(DIRECTORY_NAME);

        return linkDirectory.resolve(FILE_NAME);
    }

    static Optional<Path> findAppDirectory(Path start) {
        Path absoluteStart = start.toAbsolutePath();

        Path directory = absoluteStart.normalize();

        while (directory != null) {
            if (Files.isRegularFile(pathIn(directory))) {
                return Optional.of(directory);
            }

            directory = directory.getParent();
        }

        return Optional.empty();
    }

    static AppLink read(Path appDirectory) throws IOException {
        return OBJECT_MAPPER.readValue(Files.readString(pathIn(appDirectory), StandardCharsets.UTF_8), AppLink.class);
    }

    static void write(Path appDirectory, AppLink appLink) throws IOException {
        Path linkDirectory = appDirectory.resolve(DIRECTORY_NAME);

        Files.createDirectories(linkDirectory);
        Files.writeString(
            linkDirectory.resolve(FILE_NAME), OBJECT_MAPPER.writeValueAsString(appLink) + "\n", StandardCharsets.UTF_8);
    }
}
```

Create `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/EnvFile.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the app's ByteChef values into an env file without touching its other lines, and tells whether git ignores
 * the file. The new content goes to an owner-only temporary file in the same directory that is then renamed over the
 * env file, so no reader ever sees a half-written file and the key is never readable by group or other.
 *
 * @author Ivica Cardic
 */
final class EnvFile {

    static final String BYTECHEF_APP_KEY = "BYTECHEF_APP_KEY";
    static final String BYTECHEF_URL = "BYTECHEF_URL";

    private static final Pattern ASSIGNMENT_PATTERN =
        Pattern.compile("^(\\s*(?:export\\s+)?)([A-Za-z_][A-Za-z0-9_]*)\\s*=");
    private static final long GIT_TIMEOUT_SECONDS = 10;
    private static final Set<PosixFilePermission> OWNER_ONLY = EnumSet.of(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private EnvFile() {
    }

    enum GitIgnoreStatus {

        IGNORED, NOT_IGNORED, UNKNOWN
    }

    static List<String> merge(List<String> lines, Map<String, String> values) {
        List<String> mergedLines = new ArrayList<>();
        Set<String> replacedKeys = new HashSet<>();

        for (String line : lines) {
            Matcher matcher = ASSIGNMENT_PATTERN.matcher(line);

            if (matcher.find() && values.containsKey(matcher.group(2))) {
                String key = matcher.group(2);

                mergedLines.add(matcher.group(1) + key + "=" + values.get(key));
                replacedKeys.add(key);
            } else {
                mergedLines.add(line);
            }
        }

        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!replacedKeys.contains(entry.getKey())) {
                mergedLines.add(entry.getKey() + "=" + entry.getValue());
            }
        }

        return mergedLines;
    }

    static void writeBytechefValues(Path envFile, String bytechefUrl, String appKey) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();

        values.put(BYTECHEF_URL, bytechefUrl);
        values.put(BYTECHEF_APP_KEY, appKey);

        Path absoluteEnvFile = envFile.toAbsolutePath();
        Path directory = absoluteEnvFile.getParent();
        Path fileName = absoluteEnvFile.getFileName();

        if (directory == null || fileName == null) {
            throw new IOException("Not a file path: " + envFile);
        }

        List<String> lines = Files.isRegularFile(absoluteEnvFile)
            ? Files.readAllLines(absoluteEnvFile, StandardCharsets.UTF_8) : List.of();
        String content = String.join("\n", merge(lines, values)) + "\n";
        Path temporaryFile = createOwnerOnlyTemporaryFile(directory, fileName.toString());

        try {
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
            Files.move(
                temporaryFile, absoluteEnvFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    @SuppressFBWarnings(
        value = "COMMAND_INJECTION",
        justification = "Runs the fixed git executable with the env file path as a separate argument, never a shell.")
    static GitIgnoreStatus gitIgnoreStatus(Path envFile) {
        Path absoluteEnvFile = envFile.toAbsolutePath();
        Path directory = absoluteEnvFile.getParent();

        if (directory == null) {
            return GitIgnoreStatus.UNKNOWN;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
            "git", "-C", directory.toString(), "check-ignore", "-q", "--", absoluteEnvFile.toString());

        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        try {
            Process process = processBuilder.start();

            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();

                return GitIgnoreStatus.UNKNOWN;
            }

            return switch (process.exitValue()) {
                case 0 -> GitIgnoreStatus.IGNORED;
                case 1 -> GitIgnoreStatus.NOT_IGNORED;
                default -> GitIgnoreStatus.UNKNOWN;
            };
        } catch (IOException exception) {
            return GitIgnoreStatus.UNKNOWN;
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            return GitIgnoreStatus.UNKNOWN;
        }
    }

    private static Path createOwnerOnlyTemporaryFile(Path directory, String fileName) throws IOException {
        String prefix = "." + fileName + ".";
        FileSystem fileSystem = directory.getFileSystem();

        if (fileSystem.supportedFileAttributeViews()
            .contains("posix")) {

            return Files.createTempFile(directory, prefix, ".tmp", PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        }

        return Files.createTempFile(directory, prefix, ".tmp");
    }
}
```

`git check-ignore -q` exits 0 for an ignored path, 1 for a path that is not ignored (including a tracked file), and
128 outside a repository — which `UNKNOWN` covers, so no warning is printed where there is nothing to commit.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :cli:commands:automation:test --tests 'com.bytechef.cli.command.automation.AppLinkFileTest' --tests 'com.bytechef.cli.command.automation.EnvFileTest' > "$SCRATCH/d2-t2-green.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED' "$SCRATCH/d2-t2-green.log"
```

Expected: `exit=0`, no `FAILED`.

- [ ] **Step 5: Format and check**

```bash
./gradlew :cli:commands:automation:spotlessApply > "$SCRATCH/d2-t2-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :cli:commands:automation:check > "$SCRATCH/d2-t2-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t2-check.log"
```

Expected: both `exit=0`, no `FAILED` line.

- [ ] **Step 6: Commit**

```bash
git add cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppLink.java \
  cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppLinkFile.java \
  cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/EnvFile.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/GitTestRepository.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AppLinkFileTest.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/EnvFileTest.java
git commit -m "- cli - Add the app link file and the owner-only env file writer"
```

---

### Task 3: `automation app create`, `link`, `list` and `status`

**Files:**
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppCommandSupport.java`
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppCommand.java`
- Modify: `cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java` (import + `@EnableCommand` list)
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppCommandTest.java`

**Interfaces:**
- Consumes: Task 1 `AutomationClientFactory.appApi`, `appWorkflowBindingApi`, `toCliException(app ApiException)`,
  `RoutedStubApi`; Task 2 `AppLink`, `AppLinkFile`, `EnvFile`, `GitTestRepository`.
- Produces:
  - `final class AppCommandSupport` (all `static`): `CliConfig resolveDevelopment(Path configPath, Map<String, String>
    environmentVariables, String profile, String host, String token, String environment, Long workspaceId)`;
    `long requireWorkspaceId(CliConfig config)`; `void refuseExistingAppLink(Path workingDirectory)`;
    `AppLocation locateApp(Path workingDirectory, Long appId, CliConfig config)`; `Path resolveEnvFile(Path
    appDirectory, String envFile)`; `void warnWhenNotGitIgnored(Path envFile, PrintStream err)`; `String
    normalizeHost(String host)`; `String valueOf(Object value)`; `List<String> nullToEmpty(List<String> values)`;
    nested `record AppLocation(long appId, Path appDirectory)`.
  - `public class AutomationAppCommand` with commands `automation app create|link|list|status`, package-private
    setters `setConfigPath(Path)`, `setEnvironmentVariables(Map<String, String>)`, `setWorkingDirectory(Path)`,
    `setOutput(PrintStream out, PrintStream err)`, and nested public records `AppCreatedResult`, `AppStatusResult`,
    `AppSummary`, `ConnectionBindingStatus`, `WorkflowBindingStatus`.
  - Method signatures tests call directly:
    - `create(String name, String description, String hostingTarget, String envFile, String profile, String host,
      String token, String environment, Long workspaceId, String output)`
    - `link(Long appId, String profile, String host, String token, String environment, Long workspaceId, String output)`
    - `list(String profile, String host, String token, String environment, Long workspaceId, String output)`
    - `status(Long appId, String profile, String host, String token, String environment, Long workspaceId,
      String output)`

- [ ] **Step 1: Write the failing test**

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppCommandTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationAppCommandTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String APP = """
        {"id":42,"uuid":"app-uuid","name":"my-app","workspaceId":1,"environment":"DEVELOPMENT","status":"ENABLED"}""";

    private static final String SECRET_KEY = "bca_secret_value_never_printed";

    private static final String CREATED = "{\"app\":" + APP + ",\"secretKey\":\"" + SECRET_KEY + "\"}";

    private static final String APPS_PATH = "/api/automation/v1/apps";

    private static final String BINDINGS = """
        [{"id":1,"appId":42,"bindingKey":"crm","connectionId":12,"connectionName":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"actionNames":["createContact"],"toolNames":["searchContacts"],"proxyEnabled":true,"proxyAllowedMethods":["GET"]}]""";

    private static final String BINDABLE_CONNECTIONS = """
        [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true}]""";

    private static final String WORKFLOW_BINDINGS = """
        [{"id":3,"appId":42,"workflowKey":"onboard-customer","projectDeploymentId":7,"projectDeploymentName":"Onboarding","workflowUuid":"wf-uuid","workflowName":"Onboard customer","available":true}]""";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private AutomationAppCommand command(Path workingDirectory) {
        AutomationAppCommand automationAppCommand = new AutomationAppCommand();

        automationAppCommand.setConfigPath(tempDir.resolve("no-config"));
        automationAppCommand.setEnvironmentVariables(Map.of());
        automationAppCommand.setWorkingDirectory(workingDirectory);
        automationAppCommand.setOutput(
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8));

        return automationAppCommand;
    }

    private String printed() {
        return stdout.toString(StandardCharsets.UTF_8) + stderr.toString(StandardCharsets.UTF_8);
    }

    private static Path linkedAppDirectory(Path appDirectory, String host) throws IOException {
        AppLinkFile.write(appDirectory, new AppLink(42, "app-uuid", 1, "DEVELOPMENT", host));

        return appDirectory;
    }

    private static Map<String, RoutedStubApi.Response> statusRoutes(int bindableStatus, String bindableBody) {
        return Map.of(
            "GET " + APPS_PATH + "/42", RoutedStubApi.response(200, APP),
            "GET " + APPS_PATH + "/42/connections", RoutedStubApi.response(200, BINDINGS),
            "GET " + APPS_PATH + "/42/bindable-connections", RoutedStubApi.response(bindableStatus, bindableBody),
            "GET " + APPS_PATH + "/42/workflows", RoutedStubApi.response(200, WORKFLOW_BINDINGS));
    }

    @Test
    void testCreateWritesTheEnvFileAndAppJsonAndNeverPrintsTheKey() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(tempDir).create(
                "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            RoutedStubApi.Request request = stub.lastRequest("POST", APPS_PATH)
                .orElseThrow();
            JsonNode requestBody = OBJECT_MAPPER.readTree(request.body());

            assertEquals("workspaceId=1", request.query());
            assertEquals("DEVELOPMENT", request.environment());
            assertEquals("my-app", requestBody.get("name")
                .asText());
            assertEquals("VERCEL", requestBody.get("hostingTarget")
                .asText());
            assertEquals(
                "BYTECHEF_URL=" + stub.host() + "\nBYTECHEF_APP_KEY=" + SECRET_KEY + "\n",
                Files.readString(tempDir.resolve(".env.local")));
            assertEquals(new AppLink(42, "app-uuid", 1, "DEVELOPMENT", stub.host()), AppLinkFile.read(tempDir));
            assertFalse(printed().contains(SECRET_KEY), printed());

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertEquals(42, result.get("id")
                .asLong());
            assertFalse(result.has("secretKey"));
        }
    }

    @Test
    void testCreateRefusesWhenAParentDirectoryAlreadyHoldsAnApp() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(nestedDirectory).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("app.json"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateIsRefusedOutsideDevelopmentBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("DEVELOPMENT only"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testDevelopmentIsRequiredWhenTheEnvironmentComesFromTheEnvironmentVariable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[]")))) {

            AutomationAppCommand automationAppCommand = command(tempDir);

            automationAppCommand.setEnvironmentVariables(Map.of("BYTECHEF_ENVIRONMENT", "STAGING"));

            CliException cliException = assertThrows(
                CliException.class, () -> automationAppCommand.list(null, stub.host(), "btc_x", null, 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("STAGING"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateReportsTheAppIdWhenAppJsonCannotBeWritten() throws Exception {
        Files.writeString(tempDir.resolve(".bytechef"), "a file where the directory should be");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("Created app 42"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app link --app-id 42"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(SECRET_KEY));
            assertFalse(printed().contains(SECRET_KEY));
        }
    }

    @Test
    void testCreateReportsKeyRotateWhenTheEnvFileCannotBeWritten() throws Exception {
        Files.createDirectories(tempDir.resolve("env-dir/inside"));

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", "env-dir", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app key rotate"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(SECRET_KEY));
            assertEquals(42, AppLinkFile.read(tempDir)
                .id());
        }
    }

    @Test
    void testCreateRefusesAnEnvFileInAMissingDirectoryBeforeCreatingTheApp() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", "missing/.env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateWithoutAWorkspaceExitsWithCode4() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", null, "json"));

            assertEquals(4, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateRefusesAnUnknownHostingTargetBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "NETLIFY", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertTrue(cliException.getMessage()
                .contains("VERCEL or SELF_HOSTED"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateWarnsWhenTheEnvFileIsNotGitIgnored() throws Exception {
        Path repository = GitTestRepository.create(tempDir.resolve("repository"), "!.env.local\n");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(repository).create(
                "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertTrue(stderr.toString(StandardCharsets.UTF_8)
                .contains("is not git-ignored"), printed());
        }
    }

    @Test
    void testCreateIgnoresTheCliConfigDirectoryAbove() throws Exception {
        Path homeDirectory = tempDir.resolve("home");

        Files.createDirectories(homeDirectory.resolve(".bytechef"));
        Files.writeString(homeDirectory.resolve(".bytechef/config"), "[default]\n");

        Path projectDirectory = Files.createDirectories(homeDirectory.resolve("projects/my-app"));

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(projectDirectory).create(
                "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertEquals(42, AppLinkFile.read(projectDirectory)
                .id());
            assertTrue(Files.isRegularFile(projectDirectory.resolve(".env.local")));
        }
    }

    @Test
    void testLinkWritesAppJsonWithoutAKey() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH + "/42", RoutedStubApi.response(200, APP)))) {

            command(tempDir).link(42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertEquals(new AppLink(42, "app-uuid", 1, "DEVELOPMENT", stub.host()), AppLinkFile.read(tempDir));
            assertFalse(Files.exists(tempDir.resolve(".env.local")));
        }
    }

    @Test
    void testLinkRefusesAnAppOfAnotherEnvironment() throws Exception {
        String productionApp = APP.replace("DEVELOPMENT", "PRODUCTION");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH + "/42", RoutedStubApi.response(200, productionApp)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).link(42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertFalse(Files.exists(AppLinkFile.pathIn(tempDir)));
        }
    }

    @Test
    void testListSendsTheWorkspaceAndTheDevelopmentHeader() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[" + APP + "]")))) {

            command(tempDir).list(null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            RoutedStubApi.Request request = stub.lastRequest("GET", APPS_PATH)
                .orElseThrow();

            assertEquals("workspaceId=1", request.query());
            assertEquals("DEVELOPMENT", request.environment());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("my-app"));
        }
    }

    @Test
    void testListIsRegisteredWithTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[]")))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "DEVELOPMENT", "--workspace-id", "1");

            assertEquals(0, exitCode);
            assertTrue(stub.lastRequest("GET", APPS_PATH)
                .isPresent());
        }
    }

    @Test
    void testListRefusesProductionThroughTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[]")))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION", "--workspace-id", "1");

            assertEquals(1, exitCode);
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testStatusFindsTheAppByWalkingUpAndRendersJson() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src/routes"));

            command(nestedDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));
            JsonNode connection = result.get("connections")
                .get(0);
            JsonNode workflow = result.get("workflows")
                .get(0);

            assertEquals(42, result.get("app")
                .get("id")
                .asLong());
            assertEquals("crm", connection.get("bindingKey")
                .asText());
            assertEquals(3, connection.get("componentVersion")
                .asInt());
            assertEquals("createContact", connection.get("actions")
                .get(0)
                .asText());
            assertEquals("onboard-customer", workflow.get("workflowKey")
                .asText());
            assertTrue(workflow.get("available")
                .asBoolean());
        }
    }

    @Test
    void testStatusShowsNoComponentVersionWhenTheLookupIsForbidden() throws Exception {
        String forbidden = """
            {"code":"ACCESS_DENIED","message":"Access Denied"}""";

        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(403, forbidden))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertTrue(result.get("connections")
                .get(0)
                .get("componentVersion")
                .isNull());
        }
    }

    @Test
    void testStatusTableOutputShowsBothBindingKinds() throws Exception {
        String forbidden = """
            {"code":"ACCESS_DENIED","message":"Access Denied"}""";

        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(403, forbidden))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "table");

            String rendered = stdout.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("BINDING KEY"), rendered);
            assertTrue(rendered.contains("crm"), rendered);
            assertTrue(rendered.contains("none"), rendered);
            assertTrue(rendered.contains("onboard-customer"), rendered);
        }
    }

    @Test
    void testStatusRefusesAnAppJsonWrittenForAnotherHost() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), "http://other.example");

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("another ByteChef target"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testStatusWithoutAppJsonOrAppIdSaysWhatToDo() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--app-id"), cliException.getMessage());
        }
    }
}
```

`testStatusWithoutAppJsonOrAppIdSaysWhatToDo` assumes no `.bytechef/app.json` exists in any parent of the JUnit
temporary directory. Check once on your machine with `ls ~/.bytechef/app.json` (expected: no such file).

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :cli:commands:automation:test --tests com.bytechef.cli.command.automation.AutomationAppCommandTest > "$SCRATCH/d2-t3-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/d2-t3-red.log"
```

Expected: `exit=1`, `compileTestJava FAILED` with `cannot find symbol … AutomationAppCommand`.

- [ ] **Step 3: Write `AppCommandSupport`**

Create `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppCommandSupport.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.error.CliException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What every app command shares: the DEVELOPMENT-only profile resolution, finding the app through
 * {@code .bytechef/app.json}, and locating the env file in the app directory.
 *
 * @author Ivica Cardic
 */
final class AppCommandSupport {

    private AppCommandSupport() {
    }

    static CliConfig resolveDevelopment(
        Path configPath, Map<String, String> environmentVariables, String profile, String host, String token,
        String environment, Long workspaceId) {

        Environment environmentOverride = parseEnvironment(environment);
        ProfileResolver profileResolver = new ProfileResolver(configPath, environmentVariables);

        CliConfig config = profileResolver.resolve(
            new Overrides(host, token, environmentOverride, workspaceId, profile));

        if (config.environment() != Environment.DEVELOPMENT) {
            throw new CliException(
                1, "These commands run in DEVELOPMENT only, but the resolved environment is " + config.environment()
                    + ". Use a DEVELOPMENT profile with an automation API key created in DEVELOPMENT (bytechef "
                    + "configure --profile dev --environment DEVELOPMENT --token <key> --host <url> --workspace-id "
                    + "<id>), then pass --profile dev.");
        }

        return config;
    }

    static long requireWorkspaceId(CliConfig config) {
        Long workspaceId = config.workspaceId();

        if (workspaceId == null) {
            throw new CliException(
                4, "No workspace configured. Pass --workspace-id or store one with 'bytechef configure "
                    + "--workspace-id <id>'.");
        }

        return workspaceId;
    }

    static void refuseExistingAppLink(Path workingDirectory) {
        Optional<Path> appDirectory = AppLinkFile.findAppDirectory(workingDirectory);

        if (appDirectory.isPresent()) {
            throw new CliException(
                1, AppLinkFile.pathIn(appDirectory.get()) + " already links " + appDirectory.get() + " to an app. "
                    + "One directory holds one app: run 'bytechef automation app status' to see it, or run this "
                    + "command in a directory outside it.");
        }
    }

    static AppLocation locateApp(Path workingDirectory, Long appId, CliConfig config) {
        Optional<Path> appDirectory = AppLinkFile.findAppDirectory(workingDirectory);

        if (appDirectory.isEmpty()) {
            if (appId == null) {
                throw new CliException(
                    1, "No .bytechef/app.json in " + workingDirectory + " or any parent directory. Pass --app-id, "
                        + "or run 'bytechef automation app link --app-id <id>' in the app's directory.");
            }

            return new AppLocation(appId, workingDirectory);
        }

        AppLink appLink = readAppLink(appDirectory.get());

        if (appId != null && appId.longValue() != appLink.id()) {
            return new AppLocation(appId, workingDirectory);
        }

        checkMatchesProfile(appLink, appDirectory.get(), config);

        return new AppLocation(appLink.id(), appDirectory.get());
    }

    @SuppressFBWarnings(
        value = {
            "PATH_TRAVERSAL_IN", "PATH_TRAVERSAL_OUT"
        }, justification = "The env file path is chosen by the CLI user on purpose.")
    static Path resolveEnvFile(Path appDirectory, String envFile) {
        Path resolvedPath = appDirectory.resolve(envFile);

        Path absolutePath = resolvedPath.toAbsolutePath();

        Path envFilePath = absolutePath.normalize();
        Path directory = envFilePath.getParent();

        if (directory == null || !Files.isDirectory(directory)) {
            throw new CliException(
                1, "The directory of the env file " + envFilePath + " does not exist. Create it, or pass --env-file "
                    + "with a path in an existing directory.");
        }

        return envFilePath;
    }

    static void warnWhenNotGitIgnored(Path envFile, PrintStream err) {
        if (EnvFile.gitIgnoreStatus(envFile) == EnvFile.GitIgnoreStatus.NOT_IGNORED) {
            err.println(
                "Warning: " + envFile + " is not git-ignored. Add it to .gitignore so the app key is never "
                    + "committed.");
        }
    }

    static String normalizeHost(String host) {
        String normalizedHost = host == null ? "" : host.strip();

        while (normalizedHost.endsWith("/")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }

        return normalizedHost;
    }

    static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }

    static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static Environment parseEnvironment(String environment) {
        if (environment == null) {
            return null;
        }

        try {
            return Environment.valueOf(environment);
        } catch (IllegalArgumentException exception) {
            throw new CliException(
                1, "Unknown environment '" + environment + "'. These commands accept DEVELOPMENT only.");
        }
    }

    private static AppLink readAppLink(Path appDirectory) {
        try {
            return AppLinkFile.read(appDirectory);
        } catch (IOException exception) {
            throw new CliException(
                1, "Cannot read " + AppLinkFile.pathIn(appDirectory) + ": " + exception.getMessage());
        }
    }

    private static void checkMatchesProfile(AppLink appLink, Path appDirectory, CliConfig config) {
        List<String> differences = new ArrayList<>();

        if (!normalizeHost(appLink.host()).equals(normalizeHost(config.host()))) {
            differences.add("host " + appLink.host() + " (profile: " + config.host() + ")");
        }

        Environment environment = config.environment();

        if (!environment.name()
            .equals(appLink.environment())) {

            differences.add("environment " + appLink.environment() + " (profile: " + environment + ")");
        }

        Long workspaceId = config.workspaceId();

        if (workspaceId != null && workspaceId.longValue() != appLink.workspaceId()) {
            differences.add("workspace " + appLink.workspaceId() + " (profile: " + workspaceId + ")");
        }

        if (!differences.isEmpty()) {
            throw new CliException(
                1, AppLinkFile.pathIn(appDirectory) + " was written for another ByteChef target: "
                    + String.join(", ", differences) + ". Use the profile the app was created with, or pass "
                    + "--app-id.");
        }
    }

    record AppLocation(long appId, Path appDirectory) {
    }
}
```

- [ ] **Step 4: Write `AutomationAppCommand` (create, link, list, status)**

Create `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppCommand.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automationapp.ApiException;
import com.bytechef.cli.client.automationapp.api.AppApi;
import com.bytechef.cli.client.automationapp.api.AppWorkflowBindingApi;
import com.bytechef.cli.client.automationapp.model.AppBindableConnectionModel;
import com.bytechef.cli.client.automationapp.model.AppBindingModel;
import com.bytechef.cli.client.automationapp.model.AppHostingTargetModel;
import com.bytechef.cli.client.automationapp.model.AppInputModel;
import com.bytechef.cli.client.automationapp.model.AppModel;
import com.bytechef.cli.client.automationapp.model.AppSecretKeyModel;
import com.bytechef.cli.client.automationapp.model.AppWorkflowBindingModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * App management for the coding-agent loop, DEVELOPMENT only: create an app and write its key to the env file without
 * printing it, link a directory to an existing app, list apps, and show an app's bindings.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationAppCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private Path workingDirectory = Path.of("")
        .toAbsolutePath();
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    @Command(
        name = "automation app create",
        description = "Create an app in DEVELOPMENT and write its key to the env file; the key is never printed.")
    public void create(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "description") String description,
        @Option(longName = "hosting-target", defaultValue = "VERCEL") String hostingTarget,
        @Option(longName = "env-file", defaultValue = ".env.local") String envFile,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        long resolvedWorkspaceId = AppCommandSupport.requireWorkspaceId(config);

        AppCommandSupport.refuseExistingAppLink(workingDirectory);

        Path envFilePath = AppCommandSupport.resolveEnvFile(workingDirectory, envFile);
        AppInputModel appInputModel = new AppInputModel();

        appInputModel.setName(name);
        appInputModel.setDescription(description);
        appInputModel.setHostingTarget(parseHostingTarget(hostingTarget));

        AppApi appApi = AutomationClientFactory.appApi(config);
        AppSecretKeyModel appSecretKeyModel;

        try {
            appSecretKeyModel = appApi.createApp(resolvedWorkspaceId, appInputModel, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        AppModel appModel = appSecretKeyModel.getApp();
        String bytechefUrl = AppCommandSupport.normalizeHost(config.host());
        List<String> failures = new ArrayList<>();

        boolean appLinkWritten = writeAppLink(
            new AppLink(
                appModel.getId(), appModel.getUuid(), resolvedWorkspaceId, Environment.DEVELOPMENT.name(),
                bytechefUrl),
            failures);
        boolean envFileWritten = writeEnvFile(envFilePath, bytechefUrl, appSecretKeyModel.getSecretKey(), failures);

        if (!failures.isEmpty()) {
            throw new CliException(
                1, postCreateFailureMessage(appModel.getId(), appLinkWritten, envFileWritten, failures));
        }

        AppCommandSupport.warnWhenNotGitIgnored(envFilePath, err);

        AppCreatedResult appCreatedResult = new AppCreatedResult(
            appModel.getId(), appModel.getUuid(), appModel.getName(), resolvedWorkspaceId,
            Environment.DEVELOPMENT.name(), String.valueOf(AppLinkFile.pathIn(workingDirectory)),
            envFilePath.toString());

        new OutputRenderer(out).render(appCreatedResult, output);
    }

    @Command(
        name = "automation app link",
        description = "Write .bytechef/app.json for an existing DEVELOPMENT app in the current directory; no key.")
    public void link(
        @Option(longName = "app-id", required = true) Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);

        AppCommandSupport.refuseExistingAppLink(workingDirectory);

        AppApi appApi = AutomationClientFactory.appApi(config);
        AppModel appModel;

        try {
            appModel = appApi.getApp(appId);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        String appEnvironment = AppCommandSupport.valueOf(appModel.getEnvironment());

        if (!Environment.DEVELOPMENT.name()
            .equals(appEnvironment)) {

            throw new CliException(
                1, "App " + appId + " is in " + appEnvironment + "; these commands manage DEVELOPMENT apps only.");
        }

        Long configWorkspaceId = config.workspaceId();

        if (configWorkspaceId != null && !configWorkspaceId.equals(appModel.getWorkspaceId())) {
            throw new CliException(
                1, "App " + appId + " belongs to workspace " + appModel.getWorkspaceId() + ", not to the profile's "
                    + "workspace " + configWorkspaceId + ".");
        }

        AppLink appLink = new AppLink(
            appModel.getId(), appModel.getUuid(), appModel.getWorkspaceId(), appEnvironment,
            AppCommandSupport.normalizeHost(config.host()));

        try {
            AppLinkFile.write(workingDirectory, appLink);
        } catch (IOException exception) {
            throw new CliException(
                1, "Could not write " + AppLinkFile.pathIn(workingDirectory) + ": " + exception.getMessage());
        }

        new OutputRenderer(out).render(appLink, output);
    }

    @Command(name = "automation app list", description = "List the workspace's apps in DEVELOPMENT.")
    public void list(
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        long resolvedWorkspaceId = AppCommandSupport.requireWorkspaceId(config);
        AppApi appApi = AutomationClientFactory.appApi(config);

        try {
            new OutputRenderer(out).render(appApi.getApps(resolvedWorkspaceId, null), output);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }
    }

    @Command(
        name = "automation app status",
        description = "Show the app, its connection bindings and its workflow bindings with their availability.")
    public void status(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        long resolvedAppId = appLocation.appId();
        AppApi appApi = AutomationClientFactory.appApi(config);
        AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(config);
        AppStatusResult appStatusResult;

        try {
            AppModel appModel = appApi.getApp(resolvedAppId);
            Map<Long, Integer> componentVersions = componentVersions(appApi, resolvedAppId);

            List<ConnectionBindingStatus> connections = appApi.getAppBindings(resolvedAppId)
                .stream()
                .map(appBindingModel -> toConnectionBindingStatus(appBindingModel, componentVersions))
                .toList();
            List<WorkflowBindingStatus> workflows = appWorkflowBindingApi.getAppWorkflowBindings(resolvedAppId)
                .stream()
                .map(AutomationAppCommand::toWorkflowBindingStatus)
                .toList();

            appStatusResult = new AppStatusResult(toAppSummary(appModel), connections, workflows);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        if ("table".equalsIgnoreCase(output)) {
            renderStatusTable(appStatusResult);
        } else {
            new OutputRenderer(out).renderJson(appStatusResult);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    void setOutput(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    private static Map<Long, Integer> componentVersions(AppApi appApi, long appId) throws ApiException {
        Map<Long, Integer> componentVersions = new HashMap<>();

        try {
            for (AppBindableConnectionModel connection : appApi.getAppBindableConnections(appId, null)) {
                componentVersions.put(connection.getId(), connection.getComponentVersion());
            }
        } catch (ApiException exception) {
            if (exception.getCode() != 403) {
                throw exception;
            }
        }

        return componentVersions;
    }

    private static AppHostingTargetModel parseHostingTarget(String hostingTarget) {
        try {
            return AppHostingTargetModel.fromValue(hostingTarget);
        } catch (IllegalArgumentException exception) {
            throw new CliException(1, "Unknown --hosting-target '" + hostingTarget + "'. Use VERCEL or SELF_HOSTED.");
        }
    }

    private static AppSummary toAppSummary(AppModel appModel) {
        return new AppSummary(
            appModel.getId(), appModel.getUuid(), appModel.getName(), AppCommandSupport.valueOf(appModel.getStatus()),
            AppCommandSupport.valueOf(appModel.getEnvironment()), appModel.getWorkspaceId(), appModel.getUrl());
    }

    private static ConnectionBindingStatus toConnectionBindingStatus(
        AppBindingModel appBindingModel, Map<Long, Integer> componentVersions) {

        return new ConnectionBindingStatus(
            appBindingModel.getBindingKey(), appBindingModel.getConnectionId(), appBindingModel.getConnectionName(),
            appBindingModel.getComponentName(), componentVersions.get(appBindingModel.getConnectionId()),
            AppCommandSupport.nullToEmpty(appBindingModel.getActionNames()),
            AppCommandSupport.nullToEmpty(appBindingModel.getToolNames()),
            Boolean.TRUE.equals(appBindingModel.getProxyEnabled()),
            AppCommandSupport.nullToEmpty(appBindingModel.getProxyAllowedMethods()));
    }

    private static WorkflowBindingStatus toWorkflowBindingStatus(AppWorkflowBindingModel appWorkflowBindingModel) {
        return new WorkflowBindingStatus(
            appWorkflowBindingModel.getWorkflowKey(), appWorkflowBindingModel.getProjectDeploymentId(),
            appWorkflowBindingModel.getProjectDeploymentName(), appWorkflowBindingModel.getWorkflowUuid(),
            appWorkflowBindingModel.getWorkflowName(), Boolean.TRUE.equals(appWorkflowBindingModel.getAvailable()));
    }

    private static String cell(Object value) {
        return value == null ? "" : value.toString();
    }

    private void renderStatusTable(AppStatusResult appStatusResult) {
        OutputRenderer outputRenderer = new OutputRenderer(out);
        AppSummary appSummary = appStatusResult.app();

        outputRenderer.message(
            "App " + appSummary.id() + " " + appSummary.name() + " (" + appSummary.status() + ", "
                + appSummary.environment() + ")");
        outputRenderer.message("");
        outputRenderer.message("Connections");

        if (appStatusResult.connections()
            .isEmpty()) {

            outputRenderer.message("(none)");
        } else {
            List<List<String>> rows = new ArrayList<>();

            for (ConnectionBindingStatus connection : appStatusResult.connections()) {
                rows.add(
                    List.of(
                        cell(connection.bindingKey()),
                        cell(connection.connectionId()) + " " + cell(connection.connectionName()),
                        cell(connection.componentName()),
                        connection.componentVersion() == null ? "none" : cell(connection.componentVersion()),
                        String.join(",", connection.actions()), String.join(",", connection.tools()),
                        connection.proxyEnabled() ? String.join(",", connection.proxyMethods()) : "off"));
            }

            outputRenderer.renderTable(
                List.of("BINDING KEY", "CONNECTION", "COMPONENT", "VERSION", "ACTIONS", "TOOLS", "PROXY"), rows);
        }

        outputRenderer.message("");
        outputRenderer.message("Workflows");

        if (appStatusResult.workflows()
            .isEmpty()) {

            outputRenderer.message("(none)");

            return;
        }

        List<List<String>> rows = new ArrayList<>();

        for (WorkflowBindingStatus workflow : appStatusResult.workflows()) {
            rows.add(
                List.of(
                    cell(workflow.workflowKey()), cell(workflow.projectDeploymentId()), cell(workflow.workflowName()),
                    cell(workflow.workflowUuid()), workflow.available() ? "yes" : "no"));
        }

        outputRenderer.renderTable(List.of("WORKFLOW KEY", "DEPLOYMENT", "WORKFLOW", "WORKFLOW UUID", "AVAILABLE"), rows);
    }

    private boolean writeAppLink(AppLink appLink, List<String> failures) {
        try {
            AppLinkFile.write(workingDirectory, appLink);

            return true;
        } catch (IOException exception) {
            failures.add(
                "could not write " + AppLinkFile.pathIn(workingDirectory) + " (" + exception.getMessage() + ")");

            return false;
        }
    }

    private static boolean writeEnvFile(Path envFile, String bytechefUrl, String secretKey, List<String> failures) {
        try {
            EnvFile.writeBytechefValues(envFile, bytechefUrl, secretKey);

            return true;
        } catch (IOException exception) {
            failures.add("could not write " + envFile + " (" + exception.getMessage() + ")");

            return false;
        }
    }

    private String postCreateFailureMessage(
        long appId, boolean appLinkWritten, boolean envFileWritten, List<String> failures) {

        StringBuilder message = new StringBuilder();

        message.append("Created app ")
            .append(appId)
            .append(", but ")
            .append(String.join(" and ", failures))
            .append('.');

        if (!appLinkWritten) {
            message.append(" Run 'bytechef automation app link --app-id ")
                .append(appId)
                .append("' in ")
                .append(workingDirectory)
                .append(" to restore .bytechef/app.json.");
        }

        if (!envFileWritten) {
            message.append(appLinkWritten ? " Run" : " Then run")
                .append(" 'bytechef automation app key rotate' there to put a new key in the env file; the key from ")
                .append("this run was not saved and is never shown.");
        }

        return message.toString();
    }

    public record AppCreatedResult(
        long id, String uuid, String name, long workspaceId, String environment, String appFile, String envFile) {
    }

    public record AppStatusResult(
        AppSummary app, List<ConnectionBindingStatus> connections, List<WorkflowBindingStatus> workflows) {

        public AppStatusResult {
            connections = List.copyOf(connections);
            workflows = List.copyOf(workflows);
        }
    }

    public record AppSummary(
        Long id, String uuid, String name, String status, String environment, Long workspaceId, String url) {
    }

    public record ConnectionBindingStatus(
        String bindingKey, Long connectionId, String connectionName, String componentName, Integer componentVersion,
        List<String> actions, List<String> tools, boolean proxyEnabled, List<String> proxyMethods) {

        public ConnectionBindingStatus {
            actions = List.copyOf(actions);
            tools = List.copyOf(tools);
            proxyMethods = List.copyOf(proxyMethods);
        }
    }

    public record WorkflowBindingStatus(
        String workflowKey, Long projectDeploymentId, String projectDeploymentName, String workflowUuid,
        String workflowName, boolean available) {
    }
}
```

- [ ] **Step 5: Register the command**

In `cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java`, add the import

```java
import com.bytechef.cli.command.automation.AutomationAppCommand;
```

and replace

```java
    AutomationProjectCommand.class, AutomationDataTableCommand.class, AutomationDataTableRowCommand.class,
```

with

```java
    AutomationProjectCommand.class, AutomationDataTableCommand.class, AutomationDataTableRowCommand.class,
    AutomationAppCommand.class,
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :cli:commands:automation:test --tests com.bytechef.cli.command.automation.AutomationAppCommandTest > "$SCRATCH/d2-t3-green.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED' "$SCRATCH/d2-t3-green.log"
```

Expected: `exit=0`, no `FAILED`.

- [ ] **Step 7: Format and check**

```bash
./gradlew :cli:commands:automation:spotlessApply :cli:cli-app:spotlessApply > "$SCRATCH/d2-t3-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :cli:commands:automation:check :cli:cli-app:check > "$SCRATCH/d2-t3-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t3-check.log"
```

Expected: both `exit=0`, no `FAILED` line.

- [ ] **Step 8: Commit**

```bash
git add cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AppCommandSupport.java \
  cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppCommand.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppCommandTest.java \
  cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java
git commit -m "- cli - Add the automation app create, link, list and status commands"
```

---

### Task 4: `bind connection`, `bind workflow`, `unbind connection|workflow` and `key rotate`

**Files:**
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppBindingCommand.java`
- Modify: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppCommand.java`
  (add `keyRotate` and `AppKeyRotatedResult`)
- Modify: `cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java`
- Modify (append tests): `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppCommandTest.java`
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppBindingCommandTest.java`

**Interfaces:**
- Consumes: Task 3 `AppCommandSupport` (all methods), `AppCommandSupport.AppLocation`; Task 1 factory methods; Task 2
  `EnvFile.writeBytechefValues`.
- Produces:
  - `AutomationAppCommand.keyRotate(Long appId, String envFile, String profile, String host, String token, String
    environment, Long workspaceId, String output)`; `public record AppKeyRotatedResult(long id, String envFile)`.
  - `public class AutomationAppBindingCommand` with setters `setConfigPath(Path)`,
    `setEnvironmentVariables(Map<String, String>)`, `setWorkingDirectory(Path)`, `setOutput(PrintStream out)` and:
    - `bindConnection(String bindingKey, String componentName, Long connectionId, String actions, String tools, String
      proxy, Long appId, String profile, String host, String token, String environment, Long workspaceId, String
      output)`
    - `bindWorkflow(String workflowKey, Long deploymentId, String workflowUuid, Long appId, String profile, String
      host, String token, String environment, Long workspaceId, String output)`
    - `unbindConnection(String bindingKey, Long appId, String profile, String host, String token, String environment,
      Long workspaceId, String output)`
    - `unbindWorkflow(String workflowKey, Long appId, String profile, String host, String token, String environment,
      Long workspaceId, String output)`
    - `public record UnbindResult(long appId, String key, boolean removed)`

- [ ] **Step 1: Write the failing tests**

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppBindingCommandTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationAppBindingCommandTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String BINDABLE_PATH = "/api/automation/v1/apps/42/bindable-connections";
    private static final String OPERATIONS_PATH = "/api/automation/v1/apps/component-operations";
    private static final String BINDING_PATH = "/api/automation/v1/apps/42/connections/crm";
    private static final String WORKFLOW_BINDING_PATH = "/api/automation/v1/apps/42/workflows/onboard-customer";
    private static final String BINDABLE_WORKFLOWS_PATH = "/api/automation/v1/apps/42/bindable-workflows";

    private static final String ONE_USABLE = """
        [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true},
         {"id":13,"name":"HubSpot old","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":false}]""";

    private static final String TWO_USABLE = """
        [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true},
         {"id":13,"name":"HubSpot test","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true}]""";

    private static final String OPERATIONS = """
        {"actionNames":["createContact","getContact"],"toolNames":["searchContacts"]}""";

    private static final String BINDING = """
        {"id":1,"appId":42,"bindingKey":"crm","connectionId":12,"connectionName":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"actionNames":["createContact"],"toolNames":[],"proxyEnabled":false,"proxyAllowedMethods":[]}""";

    private static final String WORKFLOW_BINDING = """
        {"id":3,"appId":42,"workflowKey":"onboard-customer","projectDeploymentId":7,"workflowUuid":"wf-uuid","available":true}""";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    private AutomationAppBindingCommand command(RoutedStubApi stub) throws IOException {
        Path appDirectory = tempDir.resolve("app");

        AppLinkFile.write(appDirectory, new AppLink(42, "app-uuid", 1, "DEVELOPMENT", stub.host()));

        AutomationAppBindingCommand automationAppBindingCommand = new AutomationAppBindingCommand();

        automationAppBindingCommand.setConfigPath(tempDir.resolve("no-config"));
        automationAppBindingCommand.setEnvironmentVariables(Map.of());
        automationAppBindingCommand.setWorkingDirectory(appDirectory);
        automationAppBindingCommand.setOutput(new PrintStream(stdout, true, StandardCharsets.UTF_8));

        return automationAppBindingCommand;
    }

    private static void bindConnection(
        AutomationAppBindingCommand automationAppBindingCommand, RoutedStubApi stub, String componentName,
        Long connectionId, String actions, String tools, String proxy) {

        automationAppBindingCommand.bindConnection(
            "crm", componentName, connectionId, actions, tools, proxy, null, null, stub.host(), "btc_x",
            "DEVELOPMENT", 1L, "json");
    }

    private static List<String> strings(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();

        for (JsonNode valueNode : arrayNode) {
            values.add(valueNode.asText());
        }

        return values;
    }

    private static JsonNode lastBody(RoutedStubApi stub, String method, String path) throws IOException {
        RoutedStubApi.Request request = stub.lastRequest(method, path)
            .orElseThrow();

        return OBJECT_MAPPER.readTree(request.body());
    }

    @Test
    void testBindConnectionPicksTheOnlyUsableConnectionAndSendsAFullReplaceBody() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "GET " + OPERATIONS_PATH, RoutedStubApi.response(200, OPERATIONS),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            bindConnection(command(stub), stub, "hubspot", null, "createContact", null, null);

            assertEquals(
                "componentName=hubspot", stub.lastRequest("GET", BINDABLE_PATH)
                    .orElseThrow()
                    .query());
            assertEquals(
                "componentName=hubspot&componentVersion=3", stub.lastRequest("GET", OPERATIONS_PATH)
                    .orElseThrow()
                    .query());

            JsonNode body = lastBody(stub, "PUT", BINDING_PATH);

            assertEquals(12, body.get("connectionId")
                .asLong());
            assertEquals(List.of("createContact"), strings(body.get("actionNames")));
            assertEquals(List.of(), strings(body.get("toolNames")));
            assertFalse(body.get("proxyEnabled")
                .asBoolean());
            assertEquals(List.of(), strings(body.get("proxyAllowedMethods")));
        }
    }

    @Test
    void testBindConnectionListsTheCandidatesWhenSeveralAreUsable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, TWO_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, "hubspot", null, null, null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--connection-id"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("12 (HubSpot prod"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("13 (HubSpot test"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionListsTheAllowedNamesForAnUnknownAction() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "GET " + OPERATIONS_PATH, RoutedStubApi.response(200, OPERATIONS),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(
                    automationAppBindingCommand, stub, "hubspot", null, "createContact,deleteEverything", null, null));

            assertTrue(cliException.getMessage()
                .contains("deleteEverything"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("createContact, getContact"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("searchContacts"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionUsesAnExplicitConnectionIdAndItsComponent() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, TWO_USABLE),
                "GET " + OPERATIONS_PATH, RoutedStubApi.response(200, OPERATIONS),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            bindConnection(command(stub), stub, null, 13L, "getContact", null, null);

            assertNull(
                stub.lastRequest("GET", BINDABLE_PATH)
                    .orElseThrow()
                    .query());
            assertEquals(
                "componentName=hubspot&componentVersion=3", stub.lastRequest("GET", OPERATIONS_PATH)
                    .orElseThrow()
                    .query());
            assertEquals(13, lastBody(stub, "PUT", BINDING_PATH).get("connectionId")
                .asLong());
        }
    }

    @Test
    void testBindConnectionRefusesAnExplicitConnectionThatIsNotUsable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, null, 13L, null, null, null));

            assertTrue(cliException.getMessage()
                .contains("not usable"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionNeedsAComponentOrAConnectionBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, null, null, null, null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionUppercasesProxyMethodsAndEnablesTheProxy() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            bindConnection(command(stub), stub, "hubspot", null, null, null, "get,post");

            JsonNode body = lastBody(stub, "PUT", BINDING_PATH);

            assertTrue(body.get("proxyEnabled")
                .asBoolean());
            assertEquals(List.of("GET", "POST"), strings(body.get("proxyAllowedMethods")));
            assertEquals(List.of(), strings(body.get("actionNames")));
            assertTrue(stub.lastRequest("GET", OPERATIONS_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionSurfacesTheServerErrorCode() throws Exception {
        String conflict = """
            {"code":"APP_BINDING_KEY_ALREADY_EXISTS","message":"The binding key crm is already used."}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(409, conflict)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, "hubspot", null, null, null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("APP_BINDING_KEY_ALREADY_EXISTS"), cliException.getMessage());
        }
    }

    @Test
    void testBindWorkflowPutsTheDeploymentAndTheUuid() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("PUT " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(200, WORKFLOW_BINDING)))) {

            command(stub).bindWorkflow(
                "onboard-customer", 7L, "wf-uuid", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode body = lastBody(stub, "PUT", WORKFLOW_BINDING_PATH);

            assertEquals(7, body.get("projectDeploymentId")
                .asLong());
            assertEquals("wf-uuid", body.get("workflowUuid")
                .asText());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("onboard-customer"));
        }
    }

    @Test
    void testBindWorkflowListsTheBindableWorkflowsWhenTheServerRefuses() throws Exception {
        String notBindable = """
            {"code":"APP_WORKFLOW_NOT_BINDABLE","message":"Workflow wf-x of deployment 7 cannot be bound."}""";
        String bindableWorkflows = """
            [{"projectDeploymentId":7,"projectDeploymentName":"Onboarding","projectName":"Onboarding","workflowUuid":"wf-uuid","workflowName":"Onboard customer"}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "PUT " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(400, notBindable),
                "GET " + BINDABLE_WORKFLOWS_PATH, RoutedStubApi.response(200, bindableWorkflows)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.bindWorkflow(
                    "onboard-customer", 7L, "wf-x", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertTrue(cliException.getMessage()
                .contains("APP_WORKFLOW_NOT_BINDABLE"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("--deployment-id 7 --workflow-uuid wf-uuid"), cliException.getMessage());
        }
    }

    @Test
    void testUnbindSendsDeletesForBothBindingKinds() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "DELETE " + BINDING_PATH, RoutedStubApi.response(204, ""),
                "DELETE " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(204, "")))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            automationAppBindingCommand.unbindConnection(
                "crm", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");
            automationAppBindingCommand.unbindWorkflow(
                "onboard-customer", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertTrue(stub.lastRequest("DELETE", BINDING_PATH)
                .isPresent());
            assertTrue(stub.lastRequest("DELETE", WORKFLOW_BINDING_PATH)
                .isPresent());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("\"removed\" : true"));
        }
    }

    @Test
    void testUnbindConnectionIsRegisteredWithTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("DELETE " + BINDING_PATH, RoutedStubApi.response(204, "")))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "unbind", "connection", "--binding-key", "crm", "--app-id", "42", "--host",
                stub.host(), "--token", "btc_x", "--environment", "DEVELOPMENT");

            assertEquals(0, exitCode);
            assertTrue(stub.lastRequest("DELETE", BINDING_PATH)
                .isPresent());
        }
    }
}
```

Append these constants and tests to `AutomationAppCommandTest` (constants next to the others, tests at the end of the
class; add `import java.nio.file.attribute.PosixFilePermissions;`):

```java
    private static final String ROTATED_KEY = "bca_rotated_value_never_printed";

    private static final String ROTATED = "{\"app\":" + APP + ",\"secretKey\":\"" + ROTATED_KEY + "\"}";
```

```java
    @Test
    void testKeyRotateRewritesTheEnvFileInTheAppDirectoryAndNeverPrintsTheKey() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src/routes"));

            Files.writeString(appDirectory.resolve(".env.local"), "OTHER=1\nBYTECHEF_APP_KEY=old\n");

            command(nestedDirectory).keyRotate(
                null, ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertEquals(
                "OTHER=1\nBYTECHEF_APP_KEY=" + ROTATED_KEY + "\nBYTECHEF_URL=" + stub.host() + "\n",
                Files.readString(appDirectory.resolve(".env.local")));
            assertFalse(Files.exists(nestedDirectory.resolve(".env.local")));
            assertFalse(printed().contains(ROTATED_KEY), printed());

            if (appDirectory.getFileSystem()
                .supportedFileAttributeViews()
                .contains("posix")) {

                assertEquals(
                    PosixFilePermissions.fromString("rw-------"),
                    Files.getPosixFilePermissions(appDirectory.resolve(".env.local")));
            }
        }
    }

    @Test
    void testKeyRotateFailureNamesTheNextStepWithoutTheKey() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            Files.createDirectories(appDirectory.resolve("env-dir/inside"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).keyRotate(
                    null, "env-dir", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app key rotate"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(ROTATED_KEY));
        }
    }
```

The first test checks the permissions only where the file system is POSIX and the content everywhere.

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :cli:commands:automation:test --tests 'com.bytechef.cli.command.automation.AutomationAppBindingCommandTest' --tests 'com.bytechef.cli.command.automation.AutomationAppCommandTest' > "$SCRATCH/d2-t4-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/d2-t4-red.log"
```

Expected: `exit=1`, `compileTestJava FAILED` with `cannot find symbol … AutomationAppBindingCommand` and
`keyRotate`.

- [ ] **Step 3: Add `keyRotate` to `AutomationAppCommand`**

Insert this command after `status(...)` in `AutomationAppCommand.java`:

```java
    @Command(
        name = "automation app key rotate",
        description = "Rotate the app key and write the new one to the env file; the key is never printed.")
    public void keyRotate(
        @Option(longName = "app-id") Long appId,
        @Option(longName = "env-file", defaultValue = ".env.local") String envFile,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        Path envFilePath = AppCommandSupport.resolveEnvFile(appLocation.appDirectory(), envFile);
        AppApi appApi = AutomationClientFactory.appApi(config);
        AppSecretKeyModel appSecretKeyModel;

        try {
            appSecretKeyModel = appApi.rotateAppSecretKey(appLocation.appId());
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        try {
            EnvFile.writeBytechefValues(
                envFilePath, AppCommandSupport.normalizeHost(config.host()), appSecretKeyModel.getSecretKey());
        } catch (IOException exception) {
            throw new CliException(
                1, "Rotated the key of app " + appLocation.appId() + ", but could not write " + envFilePath + " ("
                    + exception.getMessage() + "). The previous key no longer works and the new one was not saved; "
                    + "run 'bytechef automation app key rotate' again once the file can be written.");
        }

        AppCommandSupport.warnWhenNotGitIgnored(envFilePath, err);

        new OutputRenderer(out).render(new AppKeyRotatedResult(appLocation.appId(), envFilePath.toString()), output);
    }
```

Add this record after `AppCreatedResult`:

```java
    public record AppKeyRotatedResult(long id, String envFile) {
    }
```

- [ ] **Step 4: Write `AutomationAppBindingCommand`**

Create `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppBindingCommand.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automationapp.ApiException;
import com.bytechef.cli.client.automationapp.api.AppApi;
import com.bytechef.cli.client.automationapp.api.AppWorkflowBindingApi;
import com.bytechef.cli.client.automationapp.model.AppBindableConnectionModel;
import com.bytechef.cli.client.automationapp.model.AppBindableWorkflowModel;
import com.bytechef.cli.client.automationapp.model.AppBindingInputModel;
import com.bytechef.cli.client.automationapp.model.AppBindingModel;
import com.bytechef.cli.client.automationapp.model.AppComponentOperationsModel;
import com.bytechef.cli.client.automationapp.model.AppWorkflowBindingInputModel;
import com.bytechef.cli.client.automationapp.model.AppWorkflowBindingModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.input.CliArgs;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Binds connections and deployed workflows to an app, and removes bindings, DEVELOPMENT only. A connection binding is
 * always a full replace: the binding becomes exactly what the options name.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationAppBindingCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private Path workingDirectory = Path.of("")
        .toAbsolutePath();
    private PrintStream out = System.out;

    @Command(
        name = "automation app bind connection",
        description = "Bind a connection to the app. The binding becomes exactly what the options name: an omitted "
            + "--actions, --tools or --proxy means none.")
    public void bindConnection(
        @Option(longName = "binding-key", required = true) String bindingKey,
        @Option(longName = "component-name") String componentName,
        @Option(longName = "connection-id") Long connectionId,
        @Option(longName = "actions") String actions,
        @Option(longName = "tools") String tools,
        @Option(longName = "proxy") String proxy,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        if (componentName == null && connectionId == null) {
            throw new CliException(1, "Pass --component-name (the CLI then picks the connection) or --connection-id.");
        }

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppApi appApi = AutomationClientFactory.appApi(config);
        List<String> actionNames = AppCommandSupport.nullToEmpty(CliArgs.splitCsv(actions));
        List<String> toolNames = AppCommandSupport.nullToEmpty(CliArgs.splitCsv(tools));
        List<String> proxyMethods = AppCommandSupport.nullToEmpty(CliArgs.splitCsv(proxy))
            .stream()
            .map(proxyMethod -> proxyMethod.toUpperCase(Locale.ROOT))
            .toList();

        try {
            AppBindableConnectionModel connection = chooseConnection(
                appApi, appLocation.appId(), componentName, connectionId);

            checkOperationNames(appApi, connection, actionNames, toolNames);

            AppBindingInputModel appBindingInputModel = new AppBindingInputModel();

            appBindingInputModel.setConnectionId(connection.getId());
            appBindingInputModel.setActionNames(actionNames);
            appBindingInputModel.setToolNames(toolNames);
            appBindingInputModel.setProxyEnabled(!proxyMethods.isEmpty());
            appBindingInputModel.setProxyAllowedMethods(proxyMethods);

            AppBindingModel appBindingModel = appApi.bindAppConnection(
                appLocation.appId(), bindingKey, appBindingInputModel);

            new OutputRenderer(out).render(appBindingModel, output);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }
    }

    @Command(
        name = "automation app bind workflow",
        description = "Bind a deployed app-callable workflow to the app under a workflow key.")
    public void bindWorkflow(
        @Option(longName = "workflow-key", required = true) String workflowKey,
        @Option(longName = "deployment-id", required = true) Long deploymentId,
        @Option(longName = "workflow-uuid", required = true) String workflowUuid,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(config);
        AppWorkflowBindingInputModel appWorkflowBindingInputModel = new AppWorkflowBindingInputModel();

        appWorkflowBindingInputModel.setProjectDeploymentId(deploymentId);
        appWorkflowBindingInputModel.setWorkflowUuid(workflowUuid);

        AppWorkflowBindingModel appWorkflowBindingModel;

        try {
            appWorkflowBindingModel = appWorkflowBindingApi.bindAppWorkflow(
                appLocation.appId(), workflowKey, appWorkflowBindingInputModel);
        } catch (ApiException exception) {
            CliException cliException = AutomationClientFactory.toCliException(exception);

            if (exception.getCode() == 400) {
                throw new CliException(
                    cliException.exitCode(),
                    cliException.getMessage() + " "
                        + describeBindableWorkflows(appWorkflowBindingApi, appLocation.appId()));
            }

            throw cliException;
        }

        new OutputRenderer(out).render(appWorkflowBindingModel, output);
    }

    @Command(name = "automation app unbind connection", description = "Remove the app's connection binding.")
    public void unbindConnection(
        @Option(longName = "binding-key", required = true) String bindingKey,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppApi appApi = AutomationClientFactory.appApi(config);

        try {
            appApi.unbindAppConnection(appLocation.appId(), bindingKey);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        new OutputRenderer(out).render(new UnbindResult(appLocation.appId(), bindingKey, true), output);
    }

    @Command(name = "automation app unbind workflow", description = "Remove the app's workflow binding.")
    public void unbindWorkflow(
        @Option(longName = "workflow-key", required = true) String workflowKey,
        @Option(longName = "app-id") Long appId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        AppCommandSupport.AppLocation appLocation = AppCommandSupport.locateApp(workingDirectory, appId, config);
        AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(config);

        try {
            appWorkflowBindingApi.unbindAppWorkflow(appLocation.appId(), workflowKey);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        new OutputRenderer(out).render(new UnbindResult(appLocation.appId(), workflowKey, true), output);
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    void setOutput(PrintStream out) {
        this.out = out;
    }

    private static AppBindableConnectionModel chooseConnection(
        AppApi appApi, long appId, String componentName, Long connectionId) throws ApiException {

        List<AppBindableConnectionModel> candidates = appApi.getAppBindableConnections(appId, componentName);

        if (connectionId != null) {
            AppBindableConnectionModel connection = candidates.stream()
                .filter(candidate -> connectionId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new CliException(
                    1, "Connection " + connectionId + " cannot be bound to app " + appId + ". "
                        + describeCandidates(candidates)));

            if (!Boolean.TRUE.equals(connection.getUsable())) {
                throw new CliException(
                    1, "Connection " + connectionId + " (" + connection.getName() + ") is not usable: its credentials "
                        + "are not valid or you may not use it. " + describeCandidates(candidates));
            }

            return connection;
        }

        List<AppBindableConnectionModel> usableCandidates = candidates.stream()
            .filter(candidate -> Boolean.TRUE.equals(candidate.getUsable()))
            .toList();

        if (usableCandidates.size() == 1) {
            return usableCandidates.getFirst();
        }

        if (candidates.isEmpty()) {
            throw new CliException(
                1, "No " + componentName + " connection can be bound to app " + appId + ". Ask a person to create "
                    + "one in the ByteChef UI (DEVELOPMENT, the app's workspace), then run this command again.");
        }

        if (usableCandidates.isEmpty()) {
            throw new CliException(
                1, "No usable " + componentName + " connection for app " + appId + ": fix a connection's "
                    + "credentials in the ByteChef UI or ask for permission to use one. "
                    + describeCandidates(candidates));
        }

        throw new CliException(
            1, "Several usable " + componentName + " connections for app " + appId + "; pass --connection-id with "
                + "one of them. " + describeCandidates(candidates));
    }

    private static void checkOperationNames(
        AppApi appApi, AppBindableConnectionModel connection, List<String> actionNames, List<String> toolNames)
        throws ApiException {

        if (actionNames.isEmpty() && toolNames.isEmpty()) {
            return;
        }

        AppComponentOperationsModel appComponentOperationsModel = appApi.getAppComponentOperations(
            connection.getComponentName(), connection.getComponentVersion());
        List<String> allowedActionNames = AppCommandSupport.nullToEmpty(appComponentOperationsModel.getActionNames());
        List<String> allowedToolNames = AppCommandSupport.nullToEmpty(appComponentOperationsModel.getToolNames());
        List<String> unknownActionNames = actionNames.stream()
            .filter(actionName -> !allowedActionNames.contains(actionName))
            .toList();
        List<String> unknownToolNames = toolNames.stream()
            .filter(toolName -> !allowedToolNames.contains(toolName))
            .toList();

        if (unknownActionNames.isEmpty() && unknownToolNames.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();

        message.append("Unknown names for ")
            .append(connection.getComponentName())
            .append(" version ")
            .append(connection.getComponentVersion())
            .append(':');

        if (!unknownActionNames.isEmpty()) {
            message.append(" actions ")
                .append(String.join(", ", unknownActionNames))
                .append('.');
        }

        if (!unknownToolNames.isEmpty()) {
            message.append(" tools ")
                .append(String.join(", ", unknownToolNames))
                .append('.');
        }

        message.append(" Allowed actions: ")
            .append(joinOrNone(allowedActionNames))
            .append(". Allowed tools: ")
            .append(joinOrNone(allowedToolNames))
            .append('.');

        throw new CliException(1, message.toString());
    }

    private static String describeCandidates(List<AppBindableConnectionModel> candidates) {
        if (candidates.isEmpty()) {
            return "No connection can be bound to this app.";
        }

        return "Candidates: " + candidates.stream()
            .map(candidate -> candidate.getId() + " (" + candidate.getName() + ", " + candidate.getComponentName() + ", "
                + (Boolean.TRUE.equals(candidate.getUsable()) ? "usable" : "not usable") + ")")
            .collect(Collectors.joining(", ")) + ".";
    }

    private static String describeBindableWorkflows(AppWorkflowBindingApi appWorkflowBindingApi, long appId) {
        List<AppBindableWorkflowModel> bindableWorkflows;

        try {
            bindableWorkflows = appWorkflowBindingApi.getAppBindableWorkflows(appId);
        } catch (ApiException exception) {
            return "The bindable workflows could not be listed (HTTP " + exception.getCode() + ").";
        }

        if (bindableWorkflows.isEmpty()) {
            return "No workflow can be bound yet: build an app-callable workflow, publish it and deploy it with the "
                + "management MCP server's deployProject first.";
        }

        return "Bindable workflows: " + bindableWorkflows.stream()
            .map(bindableWorkflow -> "--deployment-id " + bindableWorkflow.getProjectDeploymentId()
                + " --workflow-uuid " + bindableWorkflow.getWorkflowUuid() + " (" + bindableWorkflow.getWorkflowName()
                + ", project " + bindableWorkflow.getProjectName() + ")")
            .collect(Collectors.joining("; ")) + ".";
    }

    private static String joinOrNone(List<String> names) {
        return names.isEmpty() ? "none" : String.join(", ", names);
    }

    public record UnbindResult(long appId, String key, boolean removed) {
    }
}
```

- [ ] **Step 5: Register the command**

In `CliApplication.java` add `import com.bytechef.cli.command.automation.AutomationAppBindingCommand;` and change
`AutomationAppCommand.class,` in `@EnableCommand` to `AutomationAppCommand.class, AutomationAppBindingCommand.class,`.

- [ ] **Step 6: Run the tests to verify they pass**

```bash
./gradlew :cli:commands:automation:test --tests 'com.bytechef.cli.command.automation.AutomationAppBindingCommandTest' --tests 'com.bytechef.cli.command.automation.AutomationAppCommandTest' > "$SCRATCH/d2-t4-green.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED' "$SCRATCH/d2-t4-green.log"
```

Expected: `exit=0`, no `FAILED`.

- [ ] **Step 7: Format and check**

```bash
./gradlew :cli:commands:automation:spotlessApply :cli:cli-app:spotlessApply > "$SCRATCH/d2-t4-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :cli:commands:automation:check :cli:cli-app:check > "$SCRATCH/d2-t4-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t4-check.log"
```

Expected: both `exit=0`, no `FAILED` line.

- [ ] **Step 8: Commit**

```bash
git add cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppBindingCommand.java \
  cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationAppCommand.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppBindingCommandTest.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationAppCommandTest.java \
  cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java
git commit -m "- cli - Add the automation app bind, unbind and key rotate commands"
```

---

### Task 5: `automation connection list` and the CLI README

**Files:**
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationConnectionCommand.java`
- Modify: `cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java`
- Modify: `cli/README.md` (insert a section before `## Embedded commands`)
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationConnectionCommandTest.java`

**Interfaces:**
- Consumes: `AppCommandSupport.resolveDevelopment`, `AppCommandSupport.requireWorkspaceId`,
  `AutomationClientFactory.workspaceConnectionApi`, `toCliException`.
- Produces: `public class AutomationConnectionCommand` with `list(String componentName, String profile, String host,
  String token, String environment, Long workspaceId, String output)` and setters `setConfigPath(Path)`,
  `setEnvironmentVariables(Map<String, String>)`, `setOutput(PrintStream)`.

- [ ] **Step 1: Write the failing test**

Create `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationConnectionCommandTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationConnectionCommandTest {

    private static final String CONNECTIONS_PATH = "/api/automation/v1/connections";

    private static final String CONNECTIONS = """
        [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"authorizationType":"OAUTH2_AUTHORIZATION_CODE","credentialStatus":"VALID","aiProvider":false,"usable":true},
         {"id":-1,"name":"OpenAI (AI provider)","componentName":"openai","connectionVersion":1,"authorizationType":"API_KEY","credentialStatus":"VALID","aiProvider":true,"usable":true}]""";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    private AutomationConnectionCommand command() {
        AutomationConnectionCommand automationConnectionCommand = new AutomationConnectionCommand();

        automationConnectionCommand.setConfigPath(tempDir.resolve("no-config"));
        automationConnectionCommand.setEnvironmentVariables(Map.of());
        automationConnectionCommand.setOutput(new PrintStream(stdout, true, StandardCharsets.UTF_8));

        return automationConnectionCommand;
    }

    @Test
    void testListSendsTheWorkspaceTheComponentAndTheDevelopmentHeaderThroughTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            int exitCode = CliApplication.execute(
                "automation", "connection", "list", "--component-name", "hubspot", "--host", stub.host(), "--token",
                "btc_x", "--environment", "DEVELOPMENT", "--workspace-id", "1");

            RoutedStubApi.Request request = stub.lastRequest("GET", CONNECTIONS_PATH)
                .orElseThrow();

            assertEquals(0, exitCode);
            assertEquals("workspaceId=1&componentName=hubspot", request.query());
            assertEquals("DEVELOPMENT", request.environment());
        }
    }

    @Test
    void testListRendersTheConnectionFieldsAsJson() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            command().list(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode connections = new ObjectMapper().readTree(stdout.toString(StandardCharsets.UTF_8));
            JsonNode aiProviderConnection = connections.get(1);

            assertEquals(2, connections.size());
            assertTrue(aiProviderConnection.get("aiProvider")
                .asBoolean());

            for (String fieldName : List.of(
                "id", "name", "componentName", "connectionVersion", "authorizationType", "credentialStatus",
                "aiProvider", "usable")) {

                assertTrue(connections.get(0)
                    .has(fieldName), fieldName);
            }

            assertFalse(connections.get(0)
                .has("parameters"));
        }
    }

    @Test
    void testListTableOutputShowsUsableAndAiProvider() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            command().list("hubspot", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "table");

            String rendered = stdout.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("USABLE"), rendered);
            assertTrue(rendered.contains("AIPROVIDER"), rendered);
        }
    }

    @Test
    void testListRefusesProductionBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            AutomationConnectionCommand automationConnectionCommand = command();

            CliException cliException = assertThrows(
                CliException.class,
                () -> automationConnectionCommand.list(null, null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :cli:commands:automation:test --tests com.bytechef.cli.command.automation.AutomationConnectionCommandTest > "$SCRATCH/d2-t5-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|error:' "$SCRATCH/d2-t5-red.log"
```

Expected: `exit=1`, `compileTestJava FAILED` with `cannot find symbol … AutomationConnectionCommand`.

- [ ] **Step 3: Write `AutomationConnectionCommand` and register it**

Create `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationConnectionCommand.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automationapp.ApiException;
import com.bytechef.cli.client.automationapp.api.WorkspaceConnectionApi;
import com.bytechef.cli.client.automationapp.model.AppWorkspaceConnectionModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Lists the workspace's connections in DEVELOPMENT for the coding-agent loop: which ones exist, whether the caller can
 * use them, and which are virtual AI-provider connections. Never parameters or credentials.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationConnectionCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private PrintStream out = System.out;

    @Command(
        name = "automation connection list",
        description = "List the workspace's DEVELOPMENT connections: component, credential status, whether you can "
            + "use it, and whether it is an AI provider connection.")
    public void list(
        @Option(longName = "component-name") String componentName,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        long resolvedWorkspaceId = AppCommandSupport.requireWorkspaceId(config);
        WorkspaceConnectionApi workspaceConnectionApi = AutomationClientFactory.workspaceConnectionApi(config);
        List<AppWorkspaceConnectionModel> connections;

        try {
            connections = workspaceConnectionApi.getWorkspaceConnections(resolvedWorkspaceId, componentName, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        new OutputRenderer(out).render(connections, output);
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    void setOutput(PrintStream out) {
        this.out = out;
    }
}
```

In `CliApplication.java` add `import com.bytechef.cli.command.automation.AutomationConnectionCommand;` and change
`AutomationAppCommand.class, AutomationAppBindingCommand.class,` to
`AutomationAppCommand.class, AutomationAppBindingCommand.class, AutomationConnectionCommand.class,`.

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :cli:commands:automation:test --tests com.bytechef.cli.command.automation.AutomationConnectionCommandTest > "$SCRATCH/d2-t5-green.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED' "$SCRATCH/d2-t5-green.log"
```

Expected: `exit=0`, no `FAILED`.

- [ ] **Step 5: Document the app commands in `cli/README.md`**

Insert this section immediately before the line `## Embedded commands`:

````markdown
## App commands (DEVELOPMENT only)

For an app made from the `create-bytechef-app` template, usually run by a coding agent (the template's `AGENTS.md`
describes the whole loop). Every `automation app` command and `automation connection list` refuses any environment
but DEVELOPMENT, so use a profile whose token is an **automation API key created in DEVELOPMENT** (an automation API
key works only in the environment it was created in).

```bash
bytechef configure --profile dev --host https://app.bytechef.io --token <automation-api-key> \
  --environment DEVELOPMENT --workspace-id 1

# In the app's directory
bytechef automation app create --name my-app --profile dev
bytechef automation app link --app-id 42 --profile dev
bytechef automation app list --profile dev
bytechef automation app status --profile dev --output table
bytechef automation connection list --component-name hubspot --profile dev
bytechef automation app bind connection --binding-key crm --component-name hubspot \
  --actions createContact,getContact --tools searchContacts --proxy GET --profile dev
bytechef automation app bind workflow --workflow-key onboard-customer --deployment-id 7 \
  --workflow-uuid 3f1c2a9e-0d4b-4c55-9a43-6a1f0e2b7c11 --profile dev
bytechef automation app unbind connection --binding-key crm --profile dev
bytechef automation app unbind workflow --workflow-key onboard-customer --profile dev
bytechef automation app key rotate --profile dev
```

- `create` writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `--env-file` (default `.env.local`) and
  `.bytechef/app.json` (`id`, `uuid`, `workspaceId`, `environment`, `host`; no secret, commit it) in the current
  directory. It never prints the key. Only the two `BYTECHEF_*` lines change; every other line stays. The file is
  written to an owner-only (`600`) temporary file and renamed into place, and the CLI warns when git does not ignore
  it. If a write fails after the app was created, the error names the app id: `automation app link --app-id <id>`
  restores `.bytechef/app.json` and `automation app key rotate` writes a new key.
- The other commands find the app through `.bytechef/app.json`, walking up from the current directory like git, or
  take `--app-id`. `--env-file` is relative to the directory holding `.bytechef/`. A `.bytechef/app.json` written for
  another host, environment or workspace than the resolved profile is an error. `create` and `link` refuse to run
  where one already exists.
- `bind connection` makes the binding exactly what it names: an omitted `--actions`, `--tools` or `--proxy` means
  none. Without `--connection-id` the CLI uses the only usable connection of `--component-name` and otherwise lists
  the candidates. Unknown action or tool names fail before anything is written, listing the allowed ones.
- `status` shows each connection binding's component version as `null` when it cannot be resolved (the connection
  can no longer be bound, or you are not a workspace admin).
- `cli/clients/automation-app/openapi.yaml` is a copy of
  `server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml` with the
  `Environment` schema inlined (the server file's relative `$ref` does not resolve from the client). When the server
  spec changes, copy it again, inline `Environment`, and run `./gradlew :cli:clients:automation-app:generateClient`.
````

- [ ] **Step 6: Format and check**

```bash
./gradlew :cli:commands:automation:spotlessApply :cli:cli-app:spotlessApply > "$SCRATCH/d2-t5-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :cli:clients:automation-app:check :cli:commands:automation:check :cli:cli-app:check > "$SCRATCH/d2-t5-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t5-check.log"
```

Expected: both `exit=0`, no `FAILED` line.

- [ ] **Step 7: Commit**

```bash
git add cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationConnectionCommand.java \
  cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationConnectionCommandTest.java \
  cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java cli/README.md
git commit -m "- cli - Add the automation connection list command and document the app commands"
```

---

### Task 6: The template's agent loop and the command drift test

**Files:**
- Create: `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/CliCommandInventory.java`
- Test: `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/AppTemplateCliCommandDriftTest.java`
- Modify: `sdks/frontend/automation/app/create/template/AGENTS.md` (insert a section before `## Recipes`)
- Modify: `sdks/frontend/automation/app/create/template/README.md:10-12`

**Interfaces:**
- Consumes: the `@Command(name = …)` literals of Tasks 3–5.
- Produces (test package `com.bytechef.ai.mcp.server.config`, package-private): `final class CliCommandInventory` with
  `static Path repositoryRoot()` and `static Set<String> commandNames(Path repositoryRoot) throws IOException`
  (Task 8 switches `PluginSkillTagDriftIntTest` to it); `AppTemplateCliCommandDriftTest.commandPaths(String markdown)`
  returning `List<String>`.

- [ ] **Step 1: Write the failing test**

Create `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/CliCommandInventory.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.server.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The CLI's command paths, read from the {@code @Command(name = "…")} literals in {@code cli/commands/**}
 * {@code *Command.java}, for the tests that check a document naming CLI commands against the commands that exist.
 *
 * @author Ivica Cardic
 */
final class CliCommandInventory {

    private static final Pattern COMMAND_NAME_PATTERN = Pattern.compile("@Command\\(\\s*name\\s*=\\s*\"([^\"]+)\"");

    private CliCommandInventory() {
    }

    static Path repositoryRoot() {
        Path workingDirectory = Path.of("")
            .toAbsolutePath();

        Path directory = workingDirectory;

        while (directory != null && !Files.exists(directory.resolve("settings.gradle.kts"))) {
            directory = directory.getParent();
        }

        if (directory == null) {
            throw new IllegalStateException("Could not locate the repository root from " + workingDirectory);
        }

        return directory;
    }

    static Set<String> commandNames(Path repositoryRoot) throws IOException {
        List<Path> commandFiles;

        try (Stream<Path> paths = Files.walk(repositoryRoot.resolve("cli/commands"))) {
            commandFiles = paths.filter(path -> String.valueOf(path.getFileName())
                .endsWith("Command.java"))
                .filter(path -> !path.toString()
                    .contains("/build/"))
                .toList();
        }

        Set<String> commandNames = new HashSet<>();

        for (Path commandFile : commandFiles) {
            Matcher matcher = COMMAND_NAME_PATTERN.matcher(Files.readString(commandFile));

            while (matcher.find()) {
                commandNames.add(matcher.group(1));
            }
        }

        return commandNames;
    }
}
```

Create `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/AppTemplateCliCommandDriftTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Checks that every {@code bytechef …} command the app template's {@code AGENTS.md} names exists as a
 * {@code @Command(name = …)} under {@code cli/commands}. The template is read by coding agents that run those
 * commands as written, and the template's own Node check cannot catch the drift: its CI job has no CLI binary.
 * <p>
 * A command path is the run of lowercase words after {@code bytechef } in an inline code span or a fenced code line,
 * stopping at the first token that is not a command word ({@code --option}, {@code <placeholder>}, {@code …}).
 *
 * @author Ivica Cardic
 */
class AppTemplateCliCommandDriftTest {

    private static final String AGENTS_FILE = "sdks/frontend/automation/app/create/template/AGENTS.md";

    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");

    private static final Pattern COMMAND_WORD_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    private static final String COMMAND_PREFIX = "bytechef ";

    private static final List<String> LOOP_COMMANDS = List.of(
        "configure", "automation app create", "automation connection list", "automation app bind connection",
        "automation app bind workflow", "automation app status", "automation app key rotate");

    static List<String> commandPaths(String markdown) {
        List<String> candidates = new ArrayList<>();
        StringBuilder prose = new StringBuilder();
        boolean insideFence = false;

        for (String line : markdown.split("\n", -1)) {
            String strippedLine = line.strip();

            if (strippedLine.startsWith("```")) {
                insideFence = !insideFence;

                continue;
            }

            if (insideFence) {
                candidates.add(strippedLine);
            } else {
                prose.append(line)
                    .append(' ');
            }
        }

        Matcher matcher = INLINE_CODE_PATTERN.matcher(prose);

        while (matcher.find()) {
            String inlineCode = matcher.group(1);

            candidates.add(inlineCode.strip());
        }

        List<String> commandPaths = new ArrayList<>();

        for (String candidate : candidates) {
            if (!candidate.startsWith(COMMAND_PREFIX)) {
                continue;
            }

            String arguments = candidate.substring(COMMAND_PREFIX.length());
            List<String> commandWords = new ArrayList<>();

            for (String token : arguments.strip()
                .split("\\s+")) {

                if (!COMMAND_WORD_PATTERN.matcher(token)
                    .matches()) {

                    break;
                }

                commandWords.add(token);
            }

            if (!commandWords.isEmpty()) {
                commandPaths.add(String.join(" ", commandWords));
            }
        }

        return commandPaths;
    }

    private static List<String> agentsCommandPaths() throws IOException {
        Path agentsFile = CliCommandInventory.repositoryRoot()
            .resolve(AGENTS_FILE);

        return commandPaths(Files.readString(agentsFile));
    }

    @Test
    void testCommandPathsStopAtTheFirstNonCommandToken() {
        assertEquals(
            List.of("automation app bind workflow"),
            commandPaths("Run `bytechef automation app bind workflow --workflow-key … --deployment-id …`."));
        assertEquals(
            List.of("automation app unbind connection"),
            commandPaths("```bash\nbytechef automation app unbind connection --binding-key crm\n```\n"));
        assertEquals(
            List.of("automation app create"),
            commandPaths("Run `bytechef automation app\n   create --name my-app` once."));
        assertEquals(
            List.of(), commandPaths("`@bytechef/app`, `npm run -s bytechef:status`, `context.bytechef`, `bytechef`"));
    }

    @Test
    void testEveryCommandAgentsMdNamesExists() throws IOException {
        Set<String> commandNames = CliCommandInventory.commandNames(CliCommandInventory.repositoryRoot());
        List<String> failures = new ArrayList<>();

        assertTrue(commandNames.contains("automation app create"), "The CLI command inventory found nothing");

        for (String commandPath : agentsCommandPaths()) {
            if (!commandNames.contains(commandPath)) {
                failures.add(
                    AGENTS_FILE + " names `bytechef " + commandPath + "`, but no @Command(name = \"" + commandPath
                        + "\") exists under cli/commands");
            }
        }

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void testAgentsMdNamesTheAppLoopCommands() throws IOException {
        Set<String> namedCommandPaths = new HashSet<>(agentsCommandPaths());

        for (String loopCommand : LOOP_COMMANDS) {
            assertTrue(
                namedCommandPaths.contains(loopCommand), AGENTS_FILE + " no longer names `bytechef " + loopCommand + "`");
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:apps:server-app:test --tests com.bytechef.ai.mcp.server.config.AppTemplateCliCommandDriftTest > "$SCRATCH/d2-t6-red.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|AppTemplateCliCommandDriftTest > .* FAILED' "$SCRATCH/d2-t6-red.log"
```

Expected: `exit=1`; `testAgentsMdNamesTheAppLoopCommands FAILED` (the current `AGENTS.md` names no `bytechef`
command); the other two tests pass.

- [ ] **Step 3: Add the loop to `AGENTS.md`**

In `sdks/frontend/automation/app/create/template/AGENTS.md`, insert this section immediately before the line
`## Recipes` (keep one blank line before and after it). Use no ` ```ts`/` ```tsx` block here: `check:template`
typechecks those as recipes.

```markdown
## Setting up the app with ByteChef

Everything here runs in ByteChef's DEVELOPMENT environment; staging and production stay a person's decision. The app
is managed with the `bytechef` CLI, workflows with the ByteChef management MCP server.

1. **One-time setup, by a person.** Connect the management MCP server with its URL, an admin API key created in
   DEVELOPMENT and the header `X-ENVIRONMENT: DEVELOPMENT`. Create an automation API key in DEVELOPMENT and store it
   with `bytechef configure --host https://bytechef.example.com --token <key> --environment DEVELOPMENT --workspace-id <id>`.
   Create sign-in (OAuth) connections in the ByteChef UI; an agent cannot create them.
2. **The app first.**
   - `bytechef automation app create --name my-app`, run once in this directory, creates the app, writes
     `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `.env.local` and the app's id into `.bytechef/app.json` (commit that
     file, never `.env.local`). For an app that already exists: `bytechef automation app link --app-id <id>`.
   - `bytechef automation connection list --component-name hubspot` lists the connections; those with
     `"usable": true` can be bound.
   - `bytechef automation app bind connection --binding-key crm --component-name hubspot --actions createContact --tools searchContacts`
     binds one: actions for this app's code, tools for this app's own AI. The binding becomes exactly what the
     command names, so repeat every action and tool you still want when you change it. With several usable
     connections, pass `--connection-id`.
   - `npm run -s bytechef:status` shows what the app may now call; `bytechef automation app status` shows the
     bindings as ByteChef sees them.
3. **Workflows, only when the app needs one** (several steps, an approval). Through the MCP server: `createProject`
   (pass `workspaceId`) → `buildWorkflow` with `appCallable: true` → `publishProject` → `deployProject`. Then run the
   `bytechef automation app bind workflow --workflow-key … --deployment-id … --workflow-uuid …` command that
   `deployProject` prints; the workflow key is the key your code passes to `workflows.execute`. To change a workflow:
   `buildWorkflow` naming the `draftWorkflowId` from the last `deployProject` result → `publishProject` →
   `deployProject`; the binding stays valid. If `buildWorkflow` is missing, ask a ByteChef admin to enable Copilot.
4. **Use it from the app** with the recipes below: actions from server functions; tools, and optionally workflows,
   for an in-app agent through `toAiSdkTools`.
5. **Loop rules.** Never print or echo `BYTECHEF_APP_KEY`: the CLI writes it to `.env.local` and never prints it, and
   `bytechef automation app key rotate` replaces it the same way. Stay in DEVELOPMENT. Ask a person to create
   sign-in connections. Without a configured CLI, rule 5 under "Rules" applies: ask a ByteChef admin for the binding.
   `bytechef automation app unbind connection --binding-key crm` and
   `bytechef automation app unbind workflow --workflow-key onboard-customer` remove bindings;
   `bytechef automation app list` lists the workspace's apps.
```

The Deploy section stays as it is (no hosting deploy in D).

- [ ] **Step 4: Fix the README's command name**

In `sdks/frontend/automation/app/create/template/README.md`, replace lines 10-12

```markdown
1. Copy `.env.example` to `.env.local` and set `BYTECHEF_URL` and `BYTECHEF_APP_KEY` (the key comes from the app's
   page in ByteChef, or from `bytechef app create` once the ByteChef CLI supports apps). The shell environment wins
   over `.env.local`: an exported `BYTECHEF_APP_KEY` replaces the file's value.
```

with

```markdown
1. Run `bytechef automation app create --name my-app` here (it creates the app in ByteChef's DEVELOPMENT environment
   and writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `.env.local`; see `AGENTS.md`), or copy `.env.example` to
   `.env.local` and set both, taking the key from the app's page in ByteChef. The shell environment wins over
   `.env.local`: an exported `BYTECHEF_APP_KEY` replaces the file's value.
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:apps:server-app:test --tests com.bytechef.ai.mcp.server.config.AppTemplateCliCommandDriftTest > "$SCRATCH/d2-t6-green.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED' "$SCRATCH/d2-t6-green.log"
```

Expected: `exit=0`, no `FAILED`. Then prove the gate bites: temporarily change one span in `AGENTS.md` from
`bytechef automation app status` to `bytechef automation app state`, rerun the command above, expect `exit=1` with
"names `bytechef automation app state`", and restore the line (`git diff sdks/frontend/automation/app/create/template/AGENTS.md`
must show only the Step 3 insertion afterwards).

- [ ] **Step 6: Format and check**

`server-app`'s full `check` runs its whole integration suite; run the static checks on the test sources instead.
Template Markdown has no formatter or linter (`.prettierignore` lists `create/template/*.md`), and `check:template`
is unchanged because the inserted section has no TypeScript block.

```bash
./gradlew :server:apps:server-app:spotlessApply > "$SCRATCH/d2-t6-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :server:apps:server-app:spotlessCheck :server:apps:server-app:checkstyleTest :server:apps:server-app:pmdTest :server:apps:server-app:spotbugsTest > "$SCRATCH/d2-t6-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t6-check.log"
```

Expected: both `exit=0`, no `FAILED` line.

- [ ] **Step 7: Commit (two commits, by area; the template first, so each commit passes on its own)**

```bash
git add sdks/frontend/automation/app/create/template/AGENTS.md sdks/frontend/automation/app/create/template/README.md
git commit -m "- sdk - Teach the app template's AGENTS.md the ByteChef agent loop"
git add server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/CliCommandInventory.java \
  server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/AppTemplateCliCommandDriftTest.java
git commit -m "- Check that the app template's AGENTS.md names only existing CLI commands"
```

---

### Task 7: Re-sync the client contract with D1 (requires D1 merged)

**Files:**
- Modify: `cli/clients/automation-app/openapi.yaml`
- Modify (only if D1's names differ): `cli/clients/automation-app/generated/src/**` and the call sites listed in Step 3

**Interfaces:**
- Consumes: D1's `server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml`.
- Produces: the same generated API the CLI already calls (the Contract names table), now matching the server.

- [ ] **Step 1: Check D1 is merged**

```bash
grep -nE 'operationId: "(getAppBindableConnections|getAppBindableWorkflows|getAppComponentOperations|getWorkspaceConnections)"' \
  server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml
```

Expected: four lines. Zero lines means either D1 is not merged (stop and report) or D1 used other operationIds (find
its four routes with `grep -n 'bindable-connections\|bindable-workflows\|component-operations\|^  /connections:'` on
the same file and continue; Step 3 renames).

- [ ] **Step 2: Copy and inline `Environment` again**

```bash
cp server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml \
  cli/clients/automation-app/openapi.yaml
```

Replace the `Environment: $ref: "../../../../../libs/platform/…/environment.yaml"` entry exactly as in Task 1 Step 2
(a). Then:

```bash
git diff --stat cli/clients/automation-app/openapi.yaml
./gradlew :cli:clients:automation-app:generateClient > "$SCRATCH/d2-t7-gen.log" 2>&1
echo "exit=$?"
git status --short cli/clients/automation-app
```

Descriptions and response codes may differ from the Task 1 copy; that is expected. Generated sources change only if
names or types changed.

- [ ] **Step 3: Rename call sites if D1 chose other names**

Only when Step 2 changed generated API or model names. The call sites are: `AutomationClientFactory` (`AppApi`,
`AppWorkflowBindingApi`, `WorkspaceConnectionApi`), `AutomationAppCommand` (`createApp`, `getApp`, `getApps`,
`getAppBindings`, `getAppBindableConnections`, `rotateAppSecretKey`, `AppWorkflowBindingApi.getAppWorkflowBindings`),
`AutomationAppBindingCommand` (`getAppBindableConnections`, `getAppComponentOperations`, `bindAppConnection`,
`unbindAppConnection`, `getAppBindableWorkflows`, `bindAppWorkflow`, `unbindAppWorkflow`),
`AutomationConnectionCommand` (`getWorkspaceConnections`), the model imports in those files, and the request paths and
JSON field names in `AutomationAppClientTest`, `AutomationAppCommandTest`, `AutomationAppBindingCommandTest`,
`AutomationConnectionCommandTest`. A field typed as an enum by D1 (`authorizationType`, `credentialStatus`) needs no
change: no command reads them.

- [ ] **Step 4: Run every CLI check**

```bash
./gradlew :cli:clients:automation-app:spotlessApply :cli:commands:automation:spotlessApply > "$SCRATCH/d2-t7-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :cli:clients:automation-app:check :cli:commands:automation:check :cli:cli-app:check > "$SCRATCH/d2-t7-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t7-check.log"
```

Expected: both `exit=0`, no `FAILED` line.

- [ ] **Step 5: Commit (skip if `git status --short cli` is empty)**

```bash
git add cli/clients/automation-app
git status --short cli
git commit -m "- cli - Re-sync the automation app CLI client with the server's management API"
```

If Step 3 touched command or test files, add each by path to the same commit.

---

### Task 8: The `bytechef-app-builder` plugin skill and the regenerated MCP instructions (requires D1 merged)

**Files:**
- Create: `claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md`
- Modify: `claude-code-plugin/README.md` (the `uses:` rule)
- Modify: `claude-code-plugin/bytechef-dev/README.md` (capability count, table row, section)
- Modify: `claude-code-plugin/bytechef-dev/skills/bytechef-mcp-setup/SKILL.md:33-34` (one line)
- Modify (regenerated): `server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md`
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/config/McpInstructionAssemblyTest.java`
- Modify: `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/PluginSkillTagDriftIntTest.java`

**Interfaces:**
- Consumes: D1's MCP tools `deployProject`, `listConnections`, `listAppCallableWorkflows` (EE), CE tools
  `createProject`, `buildWorkflow`, `publishProject`; the `@Command` names of Tasks 3–5; `CliCommandInventory` (Task 6).
- Produces: a `## tools: deployProject, listConnections, listAppCallableWorkflows` section in `mcp-instructions.md`,
  included only when one of those EE tools is registered.

- [ ] **Step 1: Check D1's tools exist**

```bash
grep -rnE '"(deployProject|listConnections|listAppCallableWorkflows)"|name = "(deployProject|listConnections|listAppCallableWorkflows)"' \
  server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java
```

Expected: all three names appear (D1's `AppLoopTools`). If any is missing, stop and report: the `mcp` fence below
would fail `PluginSkillTagDriftIntTest`.

- [ ] **Step 2: Write the failing tests**

Add to `McpInstructionAssemblyTest` (after `testTheAlwaysSectionIsAlwaysPresent`):

```java
    @Test
    void testTheAppLoopSectionIsOmittedOnACeToolSet() {
        Set<String> toolNames = Set.of(
            "listProjects", "createProject", "createProjectWorkflow", "buildWorkflow", "publishProject", "getWorkflow");

        String instructions = ManagementMcpServerConfiguration.buildInstructions(toolNames);

        assertFalse(instructions.contains("deployProject"));
        assertFalse(instructions.contains("appCallable"));
    }

    @Test
    void testTheAppLoopSectionIsIncludedWhenAnAppLoopToolIsRegistered() {
        String instructions = ManagementMcpServerConfiguration.buildInstructions(Set.of("deployProject"));

        assertTrue(instructions.contains("workflow/v1/newWorkflowCall"));
        assertTrue(instructions.contains("bytechef.ai.copilot.enabled"));
        assertTrue(instructions.contains("X-ENVIRONMENT: DEVELOPMENT"));
    }
```

If D1 already added a test asserting that a CE tool set omits the app-loop section, keep D1's and add only the second
test above.

```bash
./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:test --tests com.bytechef.ai.mcp.server.config.McpInstructionAssemblyTest > "$SCRATCH/d2-t8-red.log" 2>&1
echo "exit=$?"
grep -E 'McpInstructionAssemblyTest > .* FAILED' "$SCRATCH/d2-t8-red.log"
```

Expected: `exit=1`, `testTheAppLoopSectionIsIncludedWhenAnAppLoopToolIsRegistered FAILED` (no such section yet).

- [ ] **Step 3: Write the skill**

Create `claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md`. The `mcp` fence is keyed only on the
EE-only tools, so a CE server (which registers `buildWorkflow` but none of them) never receives it; its body names the
CE tools in prose (the README exception added in Step 4). No line in a fence body may start with `#` outside a code
block, and no fence may cross a heading.

````markdown
---
name: ByteChef App Builder
description: This skill should be used when the user asks to "create a ByteChef app", "set up my app with ByteChef", "bind a connection to my app", "give my app a workflow", "put the ByteChef app key in .env.local", or works in an app made from the create-bytechef-app template and wants Claude to create the app, bind its connections, and build, deploy and bind the workflows it calls.
transports:
  required: [cli]
  optional: [mcp]
---

# ByteChef App Builder

An app made from the `create-bytechef-app` template reaches third-party systems only through ByteChef, with one app
key. This skill runs the loop that sets it up: create the app and put its key in `.env.local`, bind connections, and,
only when the app needs one, build, deploy and bind a workflow. Everything runs in ByteChef's DEVELOPMENT environment;
staging and production stay a person's decision. The template's own `AGENTS.md` carries the same loop for agents
without this plugin.

## One-time setup, by a person

- The CLI manages the app: it needs an automation API key created in DEVELOPMENT.
- The management MCP server builds and deploys workflows: an admin API key created in DEVELOPMENT and the header
  `X-ENVIRONMENT: DEVELOPMENT` (see the **ByteChef Management MCP Setup** skill).
- Sign-in (OAuth) connections are created by a person in the ByteChef UI; Claude cannot create them.

## The app

<!-- transport: cli uses: configure, automation app create, automation app link, automation app list, automation app status, automation connection list, automation app bind connection, automation app unbind connection, automation app key rotate -->
Configure the CLI once with the DEVELOPMENT automation API key, then run the app commands in the app's directory.
Every `automation app` command and `automation connection list` refuses any environment but DEVELOPMENT.

```bash
bytechef configure --profile dev --host https://bytechef.example.com --token <AUTOMATION_API_KEY> \
  --environment DEVELOPMENT --workspace-id 1
bytechef automation app create --name my-app --profile dev
bytechef automation connection list --component-name hubspot --profile dev
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
- `bind connection` makes the binding exactly what it names: an omitted `--actions`, `--tools` or `--proxy` means
  none, so repeat everything you still want. Actions are for the app's code, tools for the app's own AI. With exactly
  one usable connection of the component the CLI picks it; otherwise it lists the candidates and you pass
  `--connection-id`. Unknown action or tool names fail with the allowed ones listed.
  `bytechef automation app unbind connection --binding-key crm` removes a binding.
- Then `npm run -s bytechef:status` in the app shows what it may call.
<!-- /transport -->

## Workflows, only when the app needs one

<!-- transport: mcp uses: deployProject, listConnections, listAppCallableWorkflows edition: ee -->
An app calls a ByteChef workflow only when the workflow is app-callable: its trigger is the Call-workflow trigger
(`workflow/v1/newWorkflowCall`) with an `inputSchema` (JSON Schema) describing every value the app sends. A final
`workflow/v1/responseToWorkflowCall` task with an `outputSchema` describes what the app gets back; without it the app
receives the job outputs. The app loop runs in DEVELOPMENT only: the MCP client sends `X-ENVIRONMENT: DEVELOPMENT` with
an admin API key created in DEVELOPMENT, and these tools refuse any other `environment`.

1. Create the project with `createProject`, always passing `workspaceId`.
2. Build each workflow with `buildWorkflow` and `appCallable: true`. It reports each saved `workflowId` as app-callable
   or names what is missing; then call it again naming that `workflowId`.
3. Publish with `publishProject`. `deployProject` refuses a project with unpublished changes.
4. Deploy with `deployProject`. It keeps one DEVELOPMENT deployment per project, picks a connection for each slot
   when exactly one usable candidate exists and otherwise lists the candidates (pass them in `connections`;
   `listConnections` shows every connection with `usable` and `aiProvider`), and enables the app-callable workflows.
   Its result gives, per app-callable workflow, the `bytechef automation app bind workflow` command to run in the
   app's directory, and the `draftWorkflowId` for the next change.
5. To change a workflow: `buildWorkflow` naming the `draftWorkflowId` → `publishProject` → `deployProject`. The app's
   binding stays valid. `listAppCallableWorkflows` lists what is deployed and callable.

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
````

- [ ] **Step 4: Update the plugin READMEs and the setup skill**

In `claude-code-plugin/README.md`, replace

```markdown
- `uses:` is **required** on `mcp` and `cli` fences (optional on `local`). List every tool name or
  CLI command path the block depends on — including ones only mentioned in prose, like `configure`
  above.
```

with

```markdown
- `uses:` is **required** on `mcp` and `cli` fences (optional on `local`). List every tool name or
  CLI command path the block depends on — including ones only mentioned in prose, like `configure`
  above. One exception: an `edition: ee` `mcp` fence keyed on EE-only tools may name CE tools in
  prose without listing them, so that a CE server, which registers those CE tools but none of the EE
  ones, never receives the section (`bytechef-app-builder`'s workflow fence names `createProject`,
  `buildWorkflow` and `publishProject` this way).
```

In `claude-code-plugin/bytechef-dev/README.md`: change `Development tools for ByteChef — one plugin, five
capabilities:` to `Development tools for ByteChef — one plugin, six capabilities:`; add this table row after the
`ByteChef Workflow Builder` row:

```markdown
| **ByteChef App Builder** | Create an app from the `create-bytechef-app` template's directory, write its key to `.env.local`, bind connections, and build, deploy and bind its workflows (DEVELOPMENT only) |
```

and add this section after the `### ByteChef Workflow Builder` section (before `## Installation`):

```markdown
### ByteChef App Builder

The coding-agent loop for an app made from the `create-bytechef-app` template, DEVELOPMENT only. The CLI manages the
app (`bytechef automation app create|link|list|status|bind connection|bind workflow|unbind connection|unbind
workflow|key rotate`, `bytechef automation connection list`), writing the app key straight into `.env.local` so it
never passes through the model. The management MCP server builds workflows (`buildWorkflow` with `appCallable: true`),
publishes and deploys them (`publishProject`, `deployProject`, EE). Needs an automation API key and an admin API key,
both created in DEVELOPMENT.

Ask: "Create a ByteChef app for this project and bind my HubSpot connection" / "Give my app a workflow that onboards a
customer".
```

In `claude-code-plugin/bytechef-dev/skills/bytechef-mcp-setup/SKILL.md`, after the line
`- Optional `X-ENVIRONMENT` header selects the environment (defaults to `PRODUCTION`).` add:

```markdown
- The app loop (the **ByteChef App Builder** skill) needs an admin API key created in DEVELOPMENT and `X-ENVIRONMENT: DEVELOPMENT`.
```

- [ ] **Step 5: Switch `PluginSkillTagDriftIntTest` to `CliCommandInventory`**

In `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/PluginSkillTagDriftIntTest.java`:
delete the private static methods `repositoryRoot()` and `cliCommandNames()`; replace each call `repositoryRoot()`
with `CliCommandInventory.repositoryRoot()` and each call `cliCommandNames()` with
`CliCommandInventory.commandNames(CliCommandInventory.repositoryRoot())`; delete the imports that become unused
(`java.io.UncheckedIOException`, `java.nio.file.Files`, `java.util.regex.Matcher`, `java.util.regex.Pattern`,
`java.util.stream.Stream`). Keep `java.nio.file.Path` (`isSkillFile` uses it).

- [ ] **Step 6: Regenerate the MCP instructions**

```bash
./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions > "$SCRATCH/d2-t8-gen.log" 2>&1
echo "exit=$?"
grep -n '^## tools: deployProject, listConnections, listAppCallableWorkflows$' \
  server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md
```

Expected: `exit=0` and one matching line.

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./gradlew --continue :claude-code-plugin:plugin-tools:test :server:libs:ai:ai-mcp:ai-mcp-server:test > "$SCRATCH/d2-t8-green.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED' "$SCRATCH/d2-t8-green.log"
./gradlew :server:apps:server-app:testIntegration --tests com.bytechef.ai.mcp.server.config.PluginSkillTagDriftIntTest > "$SCRATCH/d2-t8-drift.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED|FAILED' "$SCRATCH/d2-t8-drift.log"
./gradlew :server:apps:server-app:test --tests com.bytechef.ai.mcp.server.config.AppTemplateCliCommandDriftTest > "$SCRATCH/d2-t8-template.log" 2>&1
echo "exit=$?"
```

Expected: every `exit=0`, no `FAILED`. `McpInstructionsStalenessTest` (in `plugin-tools:test`) proves the committed
instructions match the skills; `PluginSkillTagDriftIntTest` (Docker required for its Postgres Testcontainer) proves
every `mcp` and `cli` `uses:` entry exists and that `bytechef-app-builder`'s declared transports are exercised.

- [ ] **Step 8: Format and check**

```bash
./gradlew :claude-code-plugin:plugin-tools:spotlessApply :server:libs:ai:ai-mcp:ai-mcp-server:spotlessApply :server:apps:server-app:spotlessApply > "$SCRATCH/d2-t8-spotless.log" 2>&1
echo "exit=$?"
./gradlew --continue :claude-code-plugin:plugin-tools:check :server:libs:ai:ai-mcp:ai-mcp-server:check :server:apps:server-app:spotlessCheck :server:apps:server-app:checkstyleTest :server:apps:server-app:pmdTest :server:apps:server-app:spotbugsTest > "$SCRATCH/d2-t8-check.log" 2>&1
echo "exit=$?"
grep -E '^> Task .* FAILED' "$SCRATCH/d2-t8-check.log"
```

Expected: both `exit=0`, no `FAILED` line.

- [ ] **Step 9: Commit**

```bash
git add claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md claude-code-plugin/README.md \
  claude-code-plugin/bytechef-dev/README.md claude-code-plugin/bytechef-dev/skills/bytechef-mcp-setup/SKILL.md \
  server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md \
  server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/config/McpInstructionAssemblyTest.java \
  server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/PluginSkillTagDriftIntTest.java
git commit -m "- Add the bytechef-app-builder plugin skill and regenerate the management MCP instructions"
```

---

### Task 9: The parent spec, the template spec and the deep-dive (requires D1 merged)

**Files:**
- Modify: `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md` (lines 23, 54-55, 78, 107,
  133-135, 511, 550-578, 665-666, 677, 679 as of the spec's revision)
- Modify: `docs/superpowers/specs/2026-10-07-apps-template-design.md` (lines 4-5, 30-31, 44, 272)
- Modify: `.agents/apps.md` (D1's D section)

**Interfaces:**
- Consumes: the command names, file names and rules of Tasks 1–8.
- Produces: documentation only.

Edit with exact old/new strings (line numbers shift after the 550-578 replacement).

- [ ] **Step 1: Parent spec**

In `docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md`:

(1) Replace `| D | Deploy | `bytechef app …` CLI commands and management MCP tools; Vercel and self-host targets |`
with
`| D | Coding-agent loop | The management MCP builds and deploys workflows; the `bytechef automation app …` CLI manages apps; see `2026-10-08-apps-agent-loop-design.md` |`

(2) Replace

```markdown
- A TanStack Start developer — or an AI coding tool following `AGENTS.md` — goes from the starter template to a
  deployed app on Vercel with one CLI command.
```

with

```markdown
- A TanStack Start developer — or an AI coding tool following `AGENTS.md` — goes from the starter template to a
  running app bound to its connections with `bytechef automation app create` and `bind`; hosting deploy stays the
  template's `vercel deploy` / Docker.
```

(3) Replace `- **A Claude Code plugin.** Optional later as one skill in `bytechef-dev`; nothing in v1 depends on it.`
with `- **A Claude Code plugin.** Nothing depends on one; D adds the optional `bytechef-app-builder` skill to `bytechef-dev`.`

(4) Replace `One key per app is enough: deploy does not rotate (see D), so no overlap window is` with
`One key per app is enough: only an explicit rotation replaces it (D has no hosting deploy), so no overlap window is`
and rewrap that bullet to 120 columns.

(5) Replace

```markdown
8. **No Claude Code plugin dependency.** The deploy path that works in every coding agent is the CLI (all agents run
```

with

```markdown
8. **No Claude Code plugin dependency.** The agent loop that works in every coding agent is the CLI (all agents run
```

(6) Replace `For the CLI and MCP, under the existing automation API-key auth` with
`For the CLI (the MCP has no app tools), under the existing automation API-key auth` and rewrap the paragraph.

(7) Replace everything from the line `## Design — D. Deploy` through the line ending
``the `getApp` result includes the exact CLI command to run.`` (the CLI and MCP subsections) with

```markdown
## Design — D. Coding-agent loop

Specified in `2026-10-08-apps-agent-loop-design.md`, which replaces the deploy design that stood here. App management
moved from MCP tools to the CLI (`bytechef automation app …`, so the key never passes through the model and is
written straight into `.env.local`); `app deploy` and `key rotate --vercel` were dropped (the template deploys itself
with `vercel deploy` / Docker); workflow building and deployment were added (the management MCP's `buildWorkflow`
app-callable mode and `deployProject`).
```

(8) Replace

```markdown
**End to end** — one manual run per release: template → `bytechef app create` → bind a real connection → run the app in
development: the overview renders the bound actions and "Try it" executes one → `bytechef app deploy --target vercel`:
```

with

```markdown
**End to end** — one manual run per release: template → `bytechef automation app create` →
`bytechef automation app bind connection …` → run the app in development: the overview renders the bound actions and
"Try it" executes one → `vercel deploy --prod` (the template's own deploy):
```

(9) Replace `7. **D** CLI and MCP deploy.` with `7. **D** Coding-agent loop (D1 server; D2 CLI, template, plugin).`

(10) Replace `A1–A3 are one implementation plan; A4 and B–D one each.` with
`A1–A3 are one implementation plan; A4, B and C one each; D two (D1, D2).`

Check: `grep -n 'bytechef app ' docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md` prints
nothing.

- [ ] **Step 2: Template spec**

In `docs/superpowers/specs/2026-10-07-apps-template-design.md`:

(1) Replace

```markdown
`2026-10-06-apps-workflows-and-sdk-design.md` (sub-project B, `@bytechef/app`) and precedes sub-project D (the
`bytechef app …` CLI commands and management MCP tools). The deep-dive is `.agents/apps.md`. Where this spec and B's
```

with

```markdown
`2026-10-06-apps-workflows-and-sdk-design.md` (sub-project B, `@bytechef/app`) and precedes sub-project D (the
`bytechef automation app …` CLI commands; `2026-10-08-apps-agent-loop-design.md`). The deep-dive is
`.agents/apps.md`. Where this spec and B's
```

and rewrap that paragraph to 120 columns.

(2) Replace

```markdown
- The `bytechef app …` CLI and MCP tools (sub-project D). `AGENTS.md` ships a D-independent deploy section that D's plan
  replaces.
```

with

```markdown
- The `bytechef automation app …` CLI (sub-project D, `2026-10-08-apps-agent-loop-design.md`). `AGENTS.md`'s deploy
  section stays as it is: D has no hosting deploy.
```

(3) Replace `and no admin login. D's `bytechef app status` comes later for admins.` with
`and no admin login. D's `bytechef automation app status` shows the same bindings to admins.`

(4) Replace `Deploy (D-independent; D's plan replaces it with the `bytechef app …` commands): Vercel — `vercel env add BYTECHEF_URL``
with `Deploy (stays as it is; D has no hosting deploy): Vercel — `vercel env add BYTECHEF_URL`` and rewrap that
paragraph.

Check: `grep -n 'bytechef app ' docs/superpowers/specs/2026-10-07-apps-template-design.md` prints nothing.

- [ ] **Step 3: The deep-dive**

```bash
grep -n 'apps-agent-loop-design' .agents/apps.md
```

Expected: at least one line inside the D section D1 added in place of the old line 13. If there is none, stop and
report (D1's documentation is missing). Append this subsection at the end of that D section (before the next `## `
heading):

```markdown
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
  through an owner-only temporary file renamed over the target, and `git check-ignore` decides the not-ignored warning
  (silent outside a repository). A failed write after create names the app id and the two recovery commands, never
  the key.
- **`bind connection` is a full replace** (empty lists, `proxyEnabled: false`), picks the only usable bindable
  connection when `--connection-id` is absent, and checks names against `component-operations` before its single PUT.
- **`status`** takes each binding's component version from `bindable-connections` (the management `AppBinding` has
  none); a 403 there (an `APP_VIEW`-only caller) or a connection that can no longer be bound shows `null`.
- **Template:** `AGENTS.md`'s "Setting up the app with ByteChef" section carries the loop; `AppTemplateCliCommandDriftTest`
  (server-app, next to `PluginSkillTagDriftIntTest`) fails when it names a `bytechef …` command with no
  `@Command(name = …)` under `cli/commands`. `check:template` cannot do this: its job has no CLI binary.
- **Plugin:** the `bytechef-app-builder` skill. Its `mcp` fence is `edition: ee` and keyed only on `deployProject`,
  `listConnections`, `listAppCallableWorkflows`, so a CE server never receives the app-loop instructions; its prose
  names the CE tools, the one documented exception to the "`uses:` lists every tool" rule.
```

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md \
  docs/superpowers/specs/2026-10-07-apps-template-design.md .agents/apps.md
git commit -m "- docs - Point the Apps specs and deep-dive at the agent loop's CLI, template and plugin"
```

---

## Self-review

**Spec coverage (Plan D2 scope):**

| Spec requirement | Task |
|---|---|
| New client `cli/clients/automation-app`, committed copy of `openapi-management.yaml`, `generateClient` | 1, 7 |
| `automation app create` (+ `--description`, `--hosting-target` default VERCEL, `--env-file`), never prints the key | 3 |
| `automation app link --app-id N` (no key) | 3 |
| `automation app list` | 3 |
| `automation app status` (bindings, component version or none, allow-lists, workflow `available`) | 3 |
| `bind connection` (full replace, auto pick, candidates listing, operation check, nothing written on failure) | 4 |
| `bind workflow` (upsert, `--workflow-key --deployment-id --workflow-uuid`) | 4 |
| `unbind connection`, `unbind workflow` | 4 |
| `key rotate [--env-file]`, never prints the key | 4 |
| `automation connection list [--component-name]` with the `listConnections` fields | 5 |
| Profile and per-command overrides, `--output json|table` default json | 3–5 |
| DEVELOPMENT only (option → `BYTECHEF_ENVIRONMENT` → profile), exit 1 | 3 (tests for option, env var, shell), 5 |
| Walk-up lookup, `--env-file` relative to the app directory, mismatch error | 2, 3, 4 |
| `create`/`link` refuse an existing `.bytechef/app.json`; post-create failure message | 3 |
| Env file: replace only two keys, 0600 temporary file renamed, git-ignore warning | 2, 3 |
| Errors name the next step and list allowed values | 1, 4 |
| Registration in `CliApplication` | 3, 4, 5 |
| CLI command tests against a mocked server (env merge, atomic write, permissions, key never in output, walk-up, refusals, post-create message, list, DEVELOPMENT refusal, bind pick/ambiguity/full replace/unknown names, `--output json` shape) | 1–5 |
| Template `AGENTS.md` loop (setup, app first, workflows, use, rules); Deploy section unchanged | 6 |
| `README.md` `bytechef app create` → `bytechef automation app create` | 6 |
| `AppTemplateCliCommandDriftTest` next to `PluginSkillTagDriftIntTest` | 6 |
| `cli/README.md` | 5 |
| Plugin skill `bytechef-app-builder` (EE-keyed `mcp` fence, `cli` fences), README exception, `bytechef-mcp-setup` line | 8 |
| Regenerated MCP instructions; `PluginSkillTagDriftIntTest`, `McpInstructionsStalenessTest` | 8 |
| Parent spec lines 23, 54-55, 78, 107, 133-135, 511, 550-579, 665-666, 677, 679 | 9 |
| Template spec lines 4-5, 30, 44, 272; `.agents/apps.md` D2 half | 9 |

**Placeholder scan:** no TBD/TODO; every code step carries complete code; the only conditional steps (Task 7 Step 3,
Task 8 Step 2's D1-duplicate note) name exactly what to change and when.

**Type consistency:** `AppCommandSupport.resolveDevelopment(Path, Map, String, String, String, String, Long)`,
`locateApp(Path, Long, CliConfig)`, `resolveEnvFile(Path, String)`, `warnWhenNotGitIgnored(Path, PrintStream)` are
called with those shapes in Tasks 3–5; `AppLink(long, String, long, String, String)` in Tasks 2–4;
`EnvFile.writeBytechefValues(Path, String, String)` in Tasks 3–4; the generated method signatures in Task 1 Step 3
match every call in Tasks 3–5; command method parameter orders in the Interfaces blocks match the tests.

**Review Focus:** each of the five lines has its named test in Tasks 1–4.
