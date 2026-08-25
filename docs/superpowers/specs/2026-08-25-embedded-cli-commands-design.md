# ByteChef CLI — Embedded Commands (Design, written as-built)

**Date:** 2026-08-25
**Status:** Shipped. This document is retroactive — it records a design that was implemented without
a spec, and names the gaps that remain open.
**Scope of this document:** Sub-project #2 of the CLI programme announced in
`2026-07-22-bytechef-cli-public-api-design.md` §9.2. Covers `cli/commands/embedded` and the two
generated clients it drives.

## 1. Why this document exists

The July spec built the CLI spine (`cli-core`, profiles, auth, output, exit codes) and proved it on
four automation commands, then deferred the embedded command set to "sub-project #2, later; its own
spec/plan reusing `cli-core` unchanged." The command set was subsequently built and shipped — 7
classes, ~29 commands, two generated clients — but the spec was never written.

Nothing here proposes new work. It documents the design that is in the tree so the decisions are
reviewable, and it separates *what shipped* from *what is still missing* rather than describing the
shipped subset as if it were the whole intent.

The July spec's prediction held: `cli-core` was reused **unchanged**. Every profile, header, output
and exit-code decision in §4-§7 of that document applies verbatim here and is not restated.

## 2. Scope

**Shipped:**

- `cli/clients/embedded-configuration` — generated from
  `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`.
- `cli/clients/embedded-execution` — generated from
  `server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest/openapi.yaml`.
- `cli/commands/embedded` — `EmbeddedIntegrationCommand`, `EmbeddedWorkflowCommand`,
  `EmbeddedExecutionCommand`, `EmbeddedUserCommand`, `EmbeddedCodeWorkflowCommand`, plus
  `EmbeddedSupport` and two client factories.

**Deliberately excluded:**

| Excluded | Reason |
|---|---|
| The JWT-only frontend mirrors (24 of the 43 configuration paths) | connected-user session token, minted for the browser SDK; a CLI holds an API key, not a session |
| `/api/embedded/internal/**` (admin console) | not a public surface |
| AI Gateway (`chat/completions`, `embeddings`, `models`) | runtime inference API for OpenAI SDK clients; carried over from the July spec's exclusions |
| Webhook endpoints (`app-events`, workflow trigger) | inbound APIs — nothing for a CLI to call |

**Licensing:** everything is Apache/CE under `cli/`, per the same decision as the July spec — the
generated clients read only the `openapi.yaml` contract, never EE source.

## 3. Architecture

```
cli/
  clients/
    embedded-configuration/   generated java client  (43 paths, 56 operations)
    embedded-execution/       generated java client  (5 paths, 6 live operations)
  commands/
    embedded/                 5 @Command classes + EmbeddedSupport + 2 client factories
```

Generation matches the automation client exactly: `generatorName = "java"`, `library = "native"`
(java.net.http, no extra runtime deps), `modelNameSuffix = "Model"`, output to `$projectDir/generated`
which is added to the main source set and **committed**. Not wired to `compileJava`; regenerate by
hand with each module's `generateClient` task when the server spec changes. Checkstyle and SpotBugs
are disabled on generated sources. Both clients pull `org.apache.httpcomponents:httpmime:4.5.14`
because the `native` library uses HttpMime to encode multipart uploads.

The execution spec's 13 `operationId` declarations are not 13 operations: 7 are commented out
(`getComponents`, `getComponentActions`, `getComponentTriggers`, and the four trigger-subscription
operations). Six are live, and the CLI covers all six. Nothing can be built against the commented
seven until they are uncommented on the server.

**One difference from the automation client, and it is the better shape:** both embedded clients set
`inputSpec` to `${rootDir}/server/ee/...` — the server spec itself. The automation client instead
generates from a vendored copy at `$projectDir/openapi.yaml`. The copy is byte-identical today, so
the drift is latent rather than broken; slice 1 of the public automation API repoints it at the
server module, adopting what the embedded clients already do.

**Two client factories, not one.** `EmbeddedConfigurationClientFactory` and
`EmbeddedExecutionClientFactory` exist separately because the two generated clients have their own
`ApiClient` and `ApiException` types in different packages — they cannot share a factory without
hand-writing an abstraction over generated code. Both build the base URI as
`config.host() + "/api/embedded/v1"` and install the shared `cli-core` `AuthInterceptor`.

## 4. Authentication — three surfaces, not one

This is the part of the embedded CLI that is genuinely different from automation, and the part most
likely to be broken by a well-meaning change.

### 4.1 Connected-user paths — `/{externalUserId}/...`

The 19 configuration paths and all 5 execution paths that carry `{externalUserId}` accept the
profile's API key and act **on behalf of a connected user**. `externalUserId` is a **path parameter,
not a header**, which is why `--external-user-id` is `required = true` on every command in §6 except
the two in §4.3.

### 4.2 Frontend mirrors — the same routes without `{externalUserId}`

Every connected-user route has a twin with no `{externalUserId}` segment. Those are JWT-only by
design: the browser SDK holds a session token that identifies the connected user, so the segment is
redundant there. The CLI never calls them.

The hazard is documented at the top of the `openapi.yaml` itself:
`EmbeddedApiKeyAuthenticationConverter` derives the connected user from the first path segment, so a
no-`{externalUserId}` route reached with an API key would capture that route's **literal first
segment** as an external user id and mint a phantom `ConnectedUser` row per tenant/environment.
`ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS` is the allowlist that makes the converter
reject non-JWT tokens on those segments instead.

`embedded code-workflow list` exists partly because of this: reusing the public-rest
`getFrontendProjects` endpoint would hit exactly that path, capturing the literal segment
`"automation"` as an external user id.

### 4.3 The `/automation-project-code-workflows/**` carve-out

`embedded code-workflow deploy` and `embedded code-workflow list` are tenant-wide, not
connected-user-scoped, and are the one pair of paths that carries no `{externalUserId}` and is not
JWT-only. `EmbeddedPlatformUserApiKeySecurityConfigurer` claims them outright, so
`EmbeddedApiKeyAuthenticationConverter` never runs on them and authenticates the profile token as its
own ByteChef user with that user's real authorities.

Consequences, all load-bearing:

- The profile token must be an **API Key whose owning user holds the admin authority**. An **Admin
  API Key is rejected** — that is reserved for `/api/platform/v1`.
- These are the only embedded commands with no `--external-user-id`.
- Connected-user auth grants **zero authorities**, so routing these through §4.1 could never satisfy
  the facade's `ROLE_ADMIN` guard. That is the reason for the carve-out, not a convenience.
- The segment stays in `FRONTEND_RESERVED_PATH_SEGMENTS` anyway, so narrowing the carve-out later
  cannot silently expose it to a phantom `ConnectedUser`.

### 4.4 `X-Environment` is set once, centrally

Every generated method takes an `xEnvironment` parameter because the spec declares it as a header on
each operation. **The CLI passes `null` for it in every single call.** `AuthInterceptor` sets both
`Authorization: Bearer <token>` and `X-Environment: <environment>` on every outgoing request from the
resolved profile.

This is deliberate and should not be "fixed": per-call values would let one command disagree with the
profile the user selected. The `null` argument threaded through ~25 call sites is the visible cost of
keeping the header in one place.

## 5. Parameter conventions

- `--external-user-id` — required on every connected-user command (§4.1), absent on `code-workflow`.
- `--data` — request bodies are `--data '<literal JSON>'` or `--data @path/to/file.json`.
  `EmbeddedSupport.readBody` reads the `@` form off disk and deserializes with a plain
  `ObjectMapper`; a parse failure is exit code 1 with `Invalid --data JSON: <message>`.
- `--workspace-id` is **not** an embedded option. Embedded resources are `Scope.EMBEDDED`, which is
  workspace-less; the profile field exists only for automation commands.
- `--page` (default `0`) on the two paged commands. Page size is server-fixed.
- `--profile / --host / --token / --environment / --output` behave exactly as §4-§7 of the July spec.

## 6. Commands

Base path `<host>/api/embedded/v1`. Paths below omit the `/{externalUserId}` prefix.

### Integrations and instances (`EmbeddedIntegrationCommand`)

| Command | Operation |
|---|---|
| `embedded integration list` | `GET /integrations` |
| `embedded integration get --id` | `GET /integrations/{id}` |
| `embedded integration-instance create --id --data` | `POST /integrations/{id}/instances` |
| `embedded integration-instance delete --id` | `DELETE /integration-instances/{id}` |
| `embedded integration-instance workflow-enable --id --workflow-uuid` | `POST /integration-instances/{id}/workflows/{workflowUuid}/enable` |
| `embedded integration-instance workflow-disable --id --workflow-uuid` | `DELETE` on the **same** `/enable` path |
| `embedded integration-instance workflow-update --id --workflow-uuid --data` | `PUT /integration-instances/{id}/workflows/{workflowUuid}` |
| `embedded integration-instance input-options --id --data` | `POST /integration-instances/{id}/component-input-options` |

Enable and disable share one path and differ only by HTTP method (`enableFrontendIntegrationInstanceWorkflow`
POST / `disableFrontendIntegrationInstanceWorkflow` DELETE). See §9.2 — the coverage test cannot tell
them apart.

### Projects and workflows (`EmbeddedWorkflowCommand`)

| Command | Operation |
|---|---|
| `embedded project list` | `GET /automation/projects` |
| `embedded workflow list` | `GET /automation/workflows` |
| `embedded workflow get --workflow-uuid` | `GET /automation/workflows/{workflowUuid}` |
| `embedded workflow enable / disable --workflow-uuid` | POST / DELETE `/automation/workflows/{workflowUuid}/enable` |
| `embedded workflow publish --workflow-uuid [--description]` | `POST /automation/workflows/{workflowUuid}/publish` |
| `embedded workflow create --data` | `POST /automation/workflows` |
| `embedded workflow update --workflow-uuid --data` | `PUT /automation/workflows/{workflowUuid}` |
| `embedded workflow delete --workflow-uuid` | `DELETE /automation/workflows/{workflowUuid}` |
| `embedded workflow generate --data` | `POST /automation/workflows/generate` |
| `embedded workflow update-from-prompt --workflow-uuid --data` | `POST /automation/workflows/{workflowUuid}/generate` |
| `embedded workflow copy-template --workflow-uuid` | `POST /automation/workflow-templates/{workflowUuid}/copy` |
| `embedded workflow set-connection --workflow-uuid --node-name --connection-key --data` | `PUT /automation/workflows/{workflowUuid}/workflow-nodes/{n}/connections/{k}` |

`generate` and `update-from-prompt` drive the embedded copilot from a `{"prompt": "..."}` body — the
CLI is a thin pass-through and declares no prompt schema of its own.

### Execution (`EmbeddedExecutionCommand`)

| Command | Operation |
|---|---|
| `embedded execution list [--status --start-date --end-date --integration-instance-configuration-id --page]` | `GET /workflow-executions` |
| `embedded execution get --id` | `GET /workflow-executions/{id}` |
| `embedded tool list [--categories --components --tools]` | `GET /tools` |
| `embedded tool execute --data [--instance-id]` | `POST /tools` |
| `embedded action execute --component-name --component-version --action-name --data [--instance-id]` | `POST /components/{name}/versions/{v}/actions/{action}` |
| `embedded tool-invocation list [--surface --outcome --start-date --end-date --page]` | `GET /tool-invocations` |

### Connected user and connections (`EmbeddedUserCommand`)

| Command | Operation |
|---|---|
| `embedded user update --data` | `PATCH /{externalUserId}` |
| `embedded connection list --component-name [--connection-ids]` | `GET /components/{componentName}/connections` |

### Code workflows (`EmbeddedCodeWorkflowCommand`) — admin-authority, §4.3

| Command | Operation |
|---|---|
| `embedded code-workflow deploy --file [--language]` | `POST /automation-project-code-workflows/deploy` (multipart) |
| `embedded code-workflow list` | `GET /automation-project-code-workflows` |

`deploy` validates that the file exists before any HTTP call (exit 1 if not) and prints each
`warnings[]` entry from the response as `WARNING: <text>` after the success line.

## 7. Output and errors

`cli-core`'s `OutputRenderer` unchanged: `renderJson` for single resources, `render(value, output)`
where a table view exists, `message(...)` for 204s so stdout is never empty.

Both factories map `ApiException` identically: 401/403 → exit 2, 404 → exit 3, everything else →
exit 1, matching the July spec's table. `CliException(4, ...)` for missing configuration comes from
`ProfileResolver` in `cli-core`.

The embedded public API declares no error-body schema, so the CLI can only branch on the HTTP status
— messages are `Authentication failed (HTTP 401).` and the like, never the server's own text. §9.4.

## 8. Testing

`commands/embedded` carries five test classes:

- `EmbeddedCommandCoverageTest` — drives **every** embedded command end-to-end against a local
  `com.sun.net.httpserver.HttpServer` stub and asserts the request path.
- `EmbeddedCommandTest`, `EmbeddedCodeWorkflowCommandTest` — per-command behaviour, exit codes,
  stdout/stderr routing.
- `EmbeddedConfigurationClientFactoryTest` — the `ApiException` → exit-code mapping.
- `StubApi` — the shared stub server.

No live API calls, matching the July spec's rule.

## 9. Outstanding

Not a backlog of nice-to-haves — these are the places where the shipped state falls short of what a
designed embedded CLI would be.

1. **`workflow-templates/{workflowUuid}/provision` has no command** — neither its `POST` nor its
   `DELETE`. `copy` shipped and `provision` did not. Counted by operation rather than by path:
   23 of the 25 connected-user configuration operations, 6 of 6 execution operations, and 2 of 2
   code-workflow operations are covered — **31 of 33 addressable operations**.
2. **The coverage test asserts path, not method.** `workflow-enable` and `workflow-disable` share the
   `/enable` path, so their two tests assert the identical string. Swapping the two commands' HTTP
   methods would leave the suite green. The same holds for `workflow enable`/`disable`.
3. **`toCliException` is duplicated verbatim** in both client factories. It cannot move to `cli-core`
   as-is because each generated `ApiException` is a distinct type; a small `int status → CliException`
   helper in `cli-core` with two thin call sites would remove the copy.
4. **No declared error schema.** Slice 1 of the public automation API introduces a `ProblemDetail`-
   compatible `Error` with a stable `errorKey` for `/api/automation/v1` only. Until the embedded
   surface gets the same, embedded CLI users branch on status codes and read prose. This is the
   single largest contract gap between the two halves of the CLI.
5. **The phantom-`ConnectedUser` hazard is contained by convention, not structurally.** A new
   no-`{externalUserId}` route is safe only if someone remembers to add its first segment to
   `FRONTEND_RESERVED_PATH_SEGMENTS`. The `openapi.yaml` header comment is the only reminder.
6. **Twin routes disagree on one segment.** The connected-user route is
   `.../workflow-nodes/{workflowNodeName}/connections/{workflowConnectionKey}` (plural) while its
   frontend mirror at line 556 of the same `openapi.yaml` is `.../connection/{workflowConnectionKey}`
   (singular). The CLI is correct for the route it calls; the pair should agree.
7. **Errata in the slice-1 spec.** `2026-08-24-automation-public-api-slice1-design.md` §8 refers to
   "the three embedded clients"; there are two (`embedded-configuration`, `embedded-execution`). The
   substance of the point — that they reference `${rootDir}/server/...` while the automation client
   vendors a copy — is correct.

## 10. Relationship to the automation slices

The two halves of the CLI grew in opposite order. Embedded got a broad command set against a mature
`/api/embedded/v1`; automation got four commands against a surface that, as the slice-1 spec puts it,
exists because the CLI needed it rather than because anyone designed a public contract.

The five automation slices close that gap from the server side, adding CLI commands inside each
slice. Where a convention has to be picked for automation, the embedded CLI is evidence, not
precedent: `--data` with `@file`, the central `AuthInterceptor`, path-asserting stub tests and the
committed generated clients are all worth matching. The embedded surface's *lack* of a declared error
schema and its uuid/numeric-id mix are not.
