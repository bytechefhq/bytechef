# ByteChef CLI — Public API Commands (Design)

**Date:** 2026-07-22
**Status:** Approved design, pending spec review
**Scope of this document:** Sub-project #1 — shared CLI foundation + full Automation command set. Embedded commands are sub-project #2 (own spec, later).

## 1. Overview & goals

Extend the existing ByteChef CLI (Spring Boot + Spring Shell, today only local codegen via `component init`) with commands that call ByteChef's **public REST APIs**. This gives an n8n/Activepieces-style control-plane CLI over ByteChef resources.

Goal for sub-project #1: build the reusable spine (config, auth, HTTP client generation, output, errors) and prove it end-to-end by shipping the complete **Automation** command set.

## 2. Scope

**In scope (this sub-project):**
- Shared foundation module `cli-core`: profile/config, API client factory, output rendering, error→exit-code mapping.
- Build wiring to generate typed Java clients from public-rest `openapi.yaml` specs via the `org.openapi.generator` plugin (`java` generator).
- `bytechef configure` command.
- Automation commands (backed by `automation-configuration-public-rest`, base path `/api/automation/v1`):
  - `automation execution list`
  - `automation execution get <id>`
  - `automation project deploy`
  - `automation project pull <id>`

**Out of scope:**
- **Embedded** commands — sub-project #2 (separate spec).
- **AI Gateway** commands (`chat/completions`, `embeddings`, `models`, routing, scores) — dropped as non-CLI-idiomatic (runtime inference API for OpenAI SDK clients).
- **Webhook** commands (embedded `app-events`, workflow trigger) — dropped as inbound APIs.
- **Eval** public endpoints (`automation-ai-eval-dataset/-experiment-public-rest`) — no `openapi.yaml` exists yet; revisit when specs land.

**Licensing:** everything is built under the **Apache-licensed (CE)** `cli/` tree. Although the target public APIs are Enterprise features, the CLI and its generated clients live in CE per the repo owner's decision; the client generator only reads the `openapi.yaml` contract, not EE source.

## 3. Architecture & module layout

The CLI remains a Spring Boot + Spring Shell app with a `NonInteractiveShellRunner`. New Gradle modules under `cli/`:

```
cli/
  cli-app/                     (existing) @EnableCommand wires all command modules
  cli-core/                    (new) config/profiles, API client factory, output, errors
  clients/
    automation-configuration/  (new) openapi-generated Java client (java generator)
  commands/
    component/                 (existing) local codegen — unchanged
    automation/                (new) thin @Command classes → automation client
```

**Responsibilities**
- `cli-core` — the only place that knows about profiles, credentials, HTTP wiring, and rendering. No endpoint-specific logic.
- `clients/*` — typed API + model classes generated from the spec. No hand-written HTTP or DTOs. Following the repo convention (as in the server public-rest modules), the generator outputs to `$projectDir/generated` and the module adds `generated/src/main/java` to its main source set; the generated sources are committed.
- `commands/*` — one small `@Command`-annotated class per resource. Each command: resolves the active profile, obtains a configured client from `cli-core`, calls one client method, passes the result to the renderer. Mirrors the existing `ComponentCommand` style.

**Client generation.** Reuse the `org.openapi.generator` plugin already in the build (v7.22.0). Where the server modules use `generatorName = "spring"` (interfaces), the CLI client module uses `generatorName = "java"` against the same `inputSpec` (`server/ee/.../automation-configuration-public-rest/openapi.yaml`), producing a standalone typed client into the CLI module. Library/HTTP flavor: `native` (java.net.http) to avoid extra runtime deps, `modelNameSuffix = "Model"` to match repo conventions.

## 4. Configuration & profiles

`bytechef configure` writes `~/.bytechef/config` (TOML/INI-style, file mode `600`), supporting **named profiles**:

```ini
[default]
host = https://app.bytechef.io
token = btc_xxx
environment = PRODUCTION
workspace_id = 1

[staging]
host = https://staging.bytechef.io
token = btc_yyy
environment = STAGING
workspace_id = 4
```

- `configure` prompts for host, token, environment, and default workspace id; `--profile <name>` targets a specific profile (defaults to `default`).
- **Resolution precedence (highest first):** per-command flag (`--host/--token/--environment/--workspace-id`) → env var (`BYTECHEF_HOST`, `BYTECHEF_TOKEN`, `BYTECHEF_ENVIRONMENT`, `BYTECHEF_WORKSPACE_ID`) → selected profile in the config file.
- Missing required credentials → clear error with exit code 4 (see §7), pointing at `bytechef configure`.

`workspace_id` is stored because the automation execution list requires a `workspaceId`; a stored default keeps the common command short while `--workspace-id` overrides per call.

## 5. Auth & environment

Every API call sets:
- `Authorization: Bearer <token>`
- `X-Environment: <environment>` (DEVELOPMENT | STAGING | PRODUCTION)

Base URL = `<host>` + the spec's server path (`/api/automation/v1`). These headers are injected centrally by the `cli-core` client factory (a request interceptor on the generated client), never in individual commands.

## 6. Command specifications (Automation)

Command shape: `bytechef <product> <resource> <verb>`. Global options available on every API command: `--profile`, `--host`, `--token`, `--environment`, `--output {json|table}` (default `json`).

### `automation execution list`
Calls `GET /workflow-executions`. Options:
- `--workspace-id <long>` (required; falls back to profile `workspace_id`)
- `--status <CREATED|STARTED|STOPPED|CANCELLED|FAILED|COMPLETED>` (optional)
- `--project-id <long>`, `--project-deployment-id <long>`, `--workflow-id <string>` (optional filters)
- `--start-date <ISO-8601>`, `--end-date <ISO-8601>` (optional)
- `--page <int>` (default 0)

Output: JSON of the `Page` (default), or a table of `id, workflow, status, startDate, endDate` under `--output table`. List responses never include input/output/task data (per the API).

### `automation execution get <id>`
Calls `GET /workflow-executions/{id}`. Positional `<id>` (long). Returns the full `WorkflowExecution` (inputs, outputs, error, task executions). JSON by default; table renders a flat summary plus a task-execution table.

### `automation project deploy`
Calls `POST /projects/deploy` (multipart/form-data). Options:
- `--workspace-id <long>` (required; falls back to profile)
- `--project-file <path>` (required; the code-native project archive; validated to exist)

On success (204): prints a confirmation to stdout, exit 0. No response body.

### `automation project pull <id>`
Calls `POST /projects/{id}/git/pull`. Positional `<id>` (long). On success (204): prints confirmation, exit 0.

## 7. Output rendering & error handling

**Output** (in `cli-core`):
- `json` (default): pretty-printed JSON of the response model via the repo's Jackson `ObjectMapper` config.
- `table`: per-command column definition renders a simple aligned table; falls back to JSON if a command defines no table view.
- 204/empty responses render a one-line success message (never empty stdout).

**Errors & exit codes:**
| Exit | Meaning |
|------|---------|
| 0 | success |
| 1 | generic/unexpected error |
| 2 | auth failure (401/403) |
| 3 | not found (404) |
| 4 | missing/invalid configuration (no token/host, unknown profile) |

Non-2xx responses print `HTTP <status>: <message>` plus the API error body (if any) to **stderr**; stdout stays clean for piping. Network/connection failures → exit 1 with a readable message.

## 8. Testing

Mirrors the existing `ComponentInitCommandTest` approach:
- `cli-core`: unit tests for profile resolution precedence (flag > env > file), config file read/write + perms, and each output renderer.
- `commands/automation`: command tests against a stubbed HTTP server (WireMock or a lightweight local `HttpServer`) asserting, per command: correct method + path, `Authorization`/`X-Environment` headers, query/param mapping, exit code, and stdout/stderr routing.
- No live API calls in tests.

## 9. Decomposition & sequencing

1. **This spec — Foundation + Automation.** `cli-core`, client generation wiring, `configure`, and the four automation commands. Delivers a complete vertical slice and the reusable spine.
2. **Embedded (sub-project #2, later).** `commands/embedded` + generated clients for `embedded-configuration-public-rest` and `embedded-execution-public-rest`: integrations, integration-instances, workflows, connections, `me`, action/tool execution, executions. Larger; its own spec/plan reusing `cli-core` unchanged.

## 10. Open questions

None outstanding. Licensing (CE/Apache), eval exclusion, and gateway/webhook exclusion are all resolved above.
