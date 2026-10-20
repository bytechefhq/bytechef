# Apps — Template (`create-bytechef-app`) — Design

Sub-project C of `2026-10-06-apps-governed-ai-built-apps-design.md`. It builds on the SDK designed in
`2026-10-06-apps-workflows-and-sdk-design.md` (sub-project B, `@bytechef/app`) and precedes sub-project D (the `bytechef
automation app …` CLI commands; `2026-10-08-apps-agent-loop-design.md`). The deep-dive is `.agents/apps.md`. Where this
spec and B's text about the template disagree, this spec wins; the plan amends B and the deep-dive.

## Summary

`npm create bytechef-app` scaffolds a TanStack Start app that is safe by default and built to be extended by an AI
coding agent. The agent reads `AGENTS.md`, discovers what the app may call with `npm run -s bytechef:status`, and
builds features that reach third-party systems only through `@bytechef/app` — never through its own API keys or SDKs.

To make discovery complete, the app-facing API gains `GET /api/app/v1/actions`, which returns each allowed action's
input JSON Schema built from the full property tree, and the SDK gains `actions.list()`.

## Goals

- A developer or coding agent runs one non-interactive command and gets a buildable app whose development build renders
  the app's bindings, actions, tools and workflows from ByteChef.
- An agent can list every operation the app may call, with faithful input schemas, using only the app's own key.
- A production build of the generated app exposes nothing beyond a placeholder page until someone writes purpose-built
  server functions. No permission map, no executor.
- The generated app carries its own check — typecheck, build, bundle leak check — so the guard survives the agent's
  changes.
- CI proves the published packages scaffold, build, pass the leak check and behave against a ByteChef stand-in.

## Non-goals

- The `bytechef automation app …` CLI (sub-project D, `2026-10-08-apps-agent-loop-design.md`). `AGENTS.md`'s deploy
  section stays as it is: D has no hosting deploy.
- Publishing either package to npm. Publishing stays a manual, user-triggered step.
- Deploying to Vercel or building the Docker image in CI.
- An AI chat page or a workflow page in the template. Both are recipes in `AGENTS.md`, added when asked.
- Interactive prompts in the scaffolder.
- App sign-in. Until it exists (parent spec, Phase 2), anything a page shows is visible to anyone who can open the app.

## Decisions

1. **Minimal example plus recipes.** The template demonstrates bindings and actions only. Workflow runs, tools, the
   proxy and an AI chat are code recipes in `AGENTS.md`. The template stays dependency-light and holds one secret.
2. **Discovery through a template script using the app key.** `bytechef:status` calls the app-facing API with the
   app's own key, so it shows exactly the app's permissions and works as soon as `.env.local` exists — no ByteChef CLI
   and no admin login. D's `bytechef automation app status` shows the same bindings to admins.
3. **Faithful action input schemas from the server.** `GET /actions` closes the gap where the API returned only allowed
   action names, and it uses a recursive schema generator — the existing `JsonSchemaGeneratorUtils.generateInputSchema`
   flattens objects and arrays and is kept for tools only.
4. **Tailwind CSS v4 and shadcn/ui.** It is the stack coding agents extend most fluently.
5. **A vendored template.** `create-bytechef-app` ships a complete `template/` directory and copies it. What CI builds
   is exactly what users get; scaffolding needs no network beyond the install.
6. **Development-only overview, placeholder in production.** The overview page and its "Try it" panel exist only in
   development builds (`import.meta.env.DEV`, fixed at build time by Start). A production build renders a placeholder
   and registers no ByteChef-calling server function. Start registers only module-level `createServerFn` declarations,
   and every one of any module the app imports, so the gate cannot sit inline: the ByteChef-calling server functions
   live in their own module, reached only through a dynamic import behind `import.meta.env.DEV`, which the build
   eliminates. The template does not use the ready-made server functions from
   `@bytechef/app/tanstack-start/server-fns`; each of those is a public endpoint. This supersedes B's note that the
   template imports them.
7. **No secret on the command line.** The scaffolder never accepts the app key as a flag: it would land in shell history
   and agent transcripts.
8. **Nitro v3 for hosting.** TanStack's own scaffolder uses the `nitro/vite` plugin for every Node-compatible host and
   for Vercel (verified 2026-10-07 with `@tanstack/cli` 0.71.1 `--deployment nitro|vercel`). The template takes the
   same: `nitro` (3.0 beta, the version TanStack pins) as a direct dependency, `node .output/server/index.mjs` as the
   production server, and a `vercel.json` with `"framework": "tanstack-start"`. Start's own build (`dist/server/server.js`)
   exports only a fetch handler, and `vite preview` is not a production server.
9. **Everything pinned.** Every template dependency is an exact version; the scaffolder rewrites only `@bytechef/app`.
   Unpinned dependencies would let an upstream minor release break CI and every new app with no change here.

## Design — 1. Action schemas (server and SDK)

### Server

`GET /api/app/v1/actions` returns `AppAction[]`, one entry per allowed action across the app's bindings:

```yaml
AppAction:
  required: [bindingKey, componentName, actionName, inputSchema]
  properties:
    bindingKey: string
    componentName: string
    actionName: string
    description: string, nullable
    inputSchema: object, additionalProperties true   # JSON Schema of the action input
```

Resolution, per binding: the allowed action names (`AppOperationType.ACTION`); the connection's component definition
at its latest version (`fetchComponentDefinition(name, null)`, the version `actions.execute` uses when the caller passes
no `componentVersion`); `ComponentDefinition.getActions()` filtered by the allowed names. A binding whose component
cannot be resolved is skipped, as `getTools()` does; an allowed name missing from the latest version is skipped. A
component, or an action, that a `ComponentVisibilityProvider` hides is skipped too, in `getActions()` and in
`getTools()` alike. Execution already refuses what a provider hides (`COMPONENT_DISABLED` / `ACTION_DISABLED`), and all
three listings now hide it too: `getBindings()` keeps the binding but reports its component as unavailable (a null
`componentVersion`). No listing gates on the connection's state — only execution does (409).

**Schema generation** is a new `ActionInputSchemaGenerator` in `platform-component-api`, recursive over the platform
`Property` model:

- `BaseProperty`: `name`, `description`, `required`; a property with a display condition is never listed in
  `required`. A hidden property is excluded at every depth.
- `ObjectProperty.getProperties()` → `properties` (and `additionalProperties: false` when it declares any);
  `ArrayProperty.getItems()` → `items` (`anyOf` when several item types; an open schema when none).
- `getOptions()` with static values → `enum` (DATE, DATE_TIME and TIME options as ISO strings); options from a data
  source are omitted.
- STRING/INTEGER/NUMBER/BOOLEAN/DATE/DATE_TIME/TIME → their JSON types with `format` where one fits; FILE_ENTRY →
  the file-entry object shape; DYNAMIC_PROPERTIES → an open object; NULL → `null`.
- Root: `type: object`, `properties`, `required`, `additionalProperties: false`.

It is pinned by a unit test on a nested object inside an array with options and a display-conditioned field. Tools keep
the existing generator; a follow-up may move them over.

- Authenticated as the app, like `GET /tools`. The listing is not written to the tool invocation log.
- `inputSchema` is a JSON object, as `AppWorkflow.inputSchema` is, not a string like the older `AppTool.parameters`.

Contract and wiring: `openapi.yaml` gains the path under tag `app-connection` (so it lands on
`AppConnectionApiController` beside `executeAction`) and the `AppAction` schema; the generated REST models are
regenerated, committing only files whose content changed beyond the generation date; `AppApiKeyFilterChainTest` gains
the path.

### SDK

`bytechef.actions.list(options?: CallOptions): Promise<AppAction[]>`, with `AppAction` exported from
`@bytechef/app`. The generated types pick the schema up; `check:api` proves they match the contract. A 404 without a
`{code, message}` body (an older ByteChef without `/actions`) surfaces as `HTTP_ERROR` with status 404; callers in the
template treat that as "no action schemas on this server". The adapters do not change.

## Design — 2. The `create-bytechef-app` package

Location: `sdks/frontend/automation/app/create/`, a member of the existing npm workspace. Package
`create-bytechef-app`, `bin` `create-bytechef-app`, `engines.node >=22.12.0`, released in lockstep with
`@bytechef/app` (same version).

```
npm create bytechef-app@latest my-app -- --url <bytechef-url> [--no-install] [--package-manager npm|pnpm|yarn|bun]
pnpm create bytechef-app my-app --url <bytechef-url>
```

- Under npm the scaffolder's flags must follow `--`; npm otherwise takes them as its own config. The scaffolder refuses
  more than one positional argument and names the `--` rule. It blames npm for a swallowed flag only on evidence: the
  `--url` hint fires when `npm_config_url` is non-empty and `--url` did not reach the CLI; the `--no-install` hint fires
  when `npm_config_install` is set (npm encodes a swallowed `--no-install` as the empty string) and neither `--install`
  nor `--no-install` reached the CLI.
- `--url` must be an http or https URL. A CR or LF in `--url`, `BYTECHEF_URL` or `BYTECHEF_APP_KEY` is refused before
  anything is written, and the error never echoes the key.
- Non-interactive. `directory` defaults to `bytechef-app`. The package manager defaults to the one that invoked the
  command (`npm_config_user_agent`), falling back to npm.
- `.env.local` gets `BYTECHEF_URL` from `--url`, else from the environment, else empty; and `BYTECHEF_APP_KEY` from
  the environment if set — printing "BYTECHEF_APP_KEY taken from the environment" without the value — else empty. There
  is no key flag. The file is written with mode `0600`.

Steps, in order:

1. Refuse a target directory that exists and is not empty, naming it.
2. Copy `template/`.
3. Rename `_gitignore` → `.gitignore` and `_dockerignore` → `.dockerignore` (npm strips dotfiles of that kind from
   published packages).
4. Set `package.json` `name` from the directory's base name after validating it as an npm package name; refuse an
   invalid one with the rule it broke. A scoped `@scope/name` creates the directory `name` and keeps `@scope/name` as
   the package name.
5. Pin `@bytechef/app` in the copied `package.json` to the scaffolder's own version.
6. Write `.env.local`.
7. Run the package manager's install unless `--no-install` (spawned with `shell: true` on Windows, where package
   managers are `.cmd` shims); a failed install is reported with the command to rerun, and the scaffolded files stay.
8. Print the next steps: set the key, `npm run dev`, `npm run -s bytechef:status`, `npm run check`, deploy.

Zero runtime dependencies: `node:util` `parseArgs`, `node:fs`, `node:path`, `node:child_process`.

## Design — 3. The generated app

Stack, exact-pinned: TanStack Start `1.168.60` and `@tanstack/react-router` `1.170.41` (the SDK's test-app pair),
React 19, Vite 8, `nitro`, Tailwind CSS v4 through `@tailwindcss/vite`, shadcn/ui with `components.json`, the `cn`
helper and the Button, Card, Table and Badge components; TypeScript 5.7.3; `@bytechef/app`. `src/routeTree.gen.ts`
ships in the template so `tsc` works before the first build.

| Path | Purpose |
|------|---------|
| `src/server/bytechef.ts` | The module an app's own server functions import: one module-level `bytechefMiddleware()` and no `createServerFn`, so importing it registers nothing |
| `src/server/dev.ts` | `getAppOverview()` and `tryAction({bindingKey, actionName, input})`, the only ByteChef-calling server functions. Reached only through a dynamic import behind `import.meta.env.DEV`, so a production build contains neither (module-level declarations are required by Start; see Decision 6). Return types are the SDK's `JsonSerializable`/`JsonValue`; `tryAction` reuses `executeActionInput` and `executeActionHandler` from `@bytechef/app/tanstack-start`. `npm run check` reads this file to learn the dev-only function names, so it must stay |
| `src/server/overview.ts` | The pure `buildOverview` mapping from the SDK's listings to the overview model; no server function, unit-tested from the scaffolder's tests |
| `src/routes/index.tsx` | The route chooses its component and loader by `import.meta.env.DEV`, so the overview UI is not in production bundles. In development: a card per binding with its allowed actions (and schemas), tools and proxy settings, the bound workflows, and a "Try it" panel per action (JSON input prefilled from the schema's property names, Run, result). In production: a `#placeholder` page saying the app is ready to be built, naming nothing from ByteChef |
| `scripts/bytechef-status.mjs` | `npm run -s bytechef:status` (`node scripts/bytechef-status.mjs`, which loads `.env.local` itself with `util.parseEnv`): JSON on stdout; diagnostics and failures on stderr with exit code 1 |
| `scripts/check-bundle.mjs` | The leak check (see below) |
| `.env.example` | `BYTECHEF_URL=` and `BYTECHEF_APP_KEY=` |
| `_gitignore` | `node_modules`, `dist`, `.output`, `.tanstack`, `.vercel`, `.env*`, `!.env.example` |
| `_dockerignore` | `node_modules`, `dist`, `.output`, `.git`, `.vercel`, `.tanstack`, `.env*`, `!.env.example` |
| `vercel.json` | `{"framework": "tanstack-start"}` |
| `Dockerfile` | Multi-stage: install (`npm ci` only when a lockfile exists, else `npm install`), `npm run build` followed by the bundle check, then a runtime stage that copies only `.output`, runs as `USER node` and runs `node .output/server/index.mjs`; `BYTECHEF_URL` and `BYTECHEF_APP_KEY` come from the environment |
| `AGENTS.md`, `CLAUDE.md` | Agent instructions (section 4); `CLAUDE.md` is `@AGENTS.md` |
| `README.md` | For humans: what the app is, run, check, deploy, and that the shell environment wins over `.env.local` |

Scripts: `dev` (`vite dev`), `build` (`vite build`), `start` (`node .output/server/index.mjs`),
`check` = `tsc --noEmit && vite build && node scripts/check-bundle.mjs`, `bytechef:status`.

**Leak check** (`check-bundle.mjs`): the client output (`.output/public` with Nitro, `dist/client` without) and its
source maps must contain the server-function RPC marker (`/_serverFn/`, which also shows it is a Start client build)
and must contain neither `BYTECHEF_APP_KEY`, `/api/app/v1`, nor the key's value. The key value is read from
`process.env` and from `.env`, `.env.local`, `.env.production` and `.env.production.local`, parsed with
`util.parseEnv`. Client code therefore never names the variable; setup help lives in `README.md` and in server-side
errors. The server output (`.output/server`) must exist, and must contain none of the development-only server
function handlers — their names are derived from `src/server/dev.ts`, not listed in the script — nor a `dev-` chunk
registering them, which is what `NODE_ENV=development vite build` produces; a missing server output or an empty
derivation fails the check rather than passing vacuously. The check runs in `npm run check` and in the Dockerfile's
build stage. It is not wired into Vercel's build: a Vercel deploy relies on `AGENTS.md` rule 9 (finish with
`npm run check`) being followed.

**`bytechef:status` output** is built from `GET /connections` first, so an allowed operation is never hidden:

```json
{
  "bytechefUrl": "https://…",
  "actionSchemas": true,
  "bindings": [
    {
      "bindingKey": "crm",
      "componentName": "hubspot",
      "componentVersion": 3,
      "proxy": {"enabled": true, "methods": ["GET"]},
      "actions": [{"name": "createContact", "description": "…", "inputSchema": {}}],
      "tools": [{"name": "CRM__SEARCH_CONTACTS", "toolName": "searchContacts", "description": "…", "parameters": {}}]
    }
  ],
  "workflows": [
    {"workflowKey": "onboard-customer", "name": "…", "description": null, "available": true, "inputSchema": {}, "outputSchema": null}
  ]
}
```

- `componentVersion` null means the component is unavailable; its allowed action and tool names are still listed,
  with `"inputSchema": null` / `"parameters": null` (a tool keeps the server's flat name), so the agent asks about
  availability, not permission.
- `actionSchemas` is false when `/actions` answered 404 (an older server); actions are then listed by name with
  `"inputSchema": null`.
- A missing `BYTECHEF_URL` or `BYTECHEF_APP_KEY` exits 1 with the SDK's configuration message; a ByteChef error exits 1
  with the `BytechefAppError` code and message. Stdout carries nothing in either case.
- The shell environment wins over `.env.local` (the status script loads `.env.local` without replacing a variable already set, and Vite's env loading behaves the same).

## Design — 4. `AGENTS.md`

About 150 lines: rules, then recipes as short code blocks, then deploy.

Rules:

1. Every third-party system goes through `@bytechef/app`. Never add an API key, OAuth client or vendor SDK for a system
   the app reaches through a ByteChef binding, and never call its API directly.
2. Discover before building: run `npm run -s bytechef:status` and use only the operations it lists. Action input schemas
   are generated from the component's full property definition; fields whose shape depends on the connection appear as
   open objects — ask the human for those. An `inputSchema: null` means the schema is unavailable, not that the
   operation is forbidden.
3. Only `src/server/` imports `@bytechef/app`. Components import the server functions declared there, never the client;
   nothing from ByteChef goes under a `VITE_` variable.
4. Write purpose-built server functions with `.validator(...)` and concrete return types, exposing exactly what the page
   needs. The overview and "Try it" exist only in development builds; replace the placeholder page with real features.
   Do not use the ready-made server functions from `@bytechef/app/tanstack-start/server-fns` unless the human asks —
   each one is a public endpoint.
5. When something is not allowed, ask; do not work around it. If an operation or binding is missing from the status
   output, or a call fails with `APP_OPERATION_NOT_ALLOWED`, stop and tell the human what to ask a ByteChef admin for:
   the binding key, the component and the action or tool names. Never fall back to the proxy, a direct API call or a
   new credential. Use the proxy only where the status output shows it enabled, with the listed methods.
6. Do not retry actions automatically; they can have side effects. Show `BytechefAppError` codes meaningfully.
7. Anything a page shows is visible to anyone who can open the app (there is no sign-in yet). Show a workflow's
   approval link only to its intended approver; ask the human before putting it on a page.
8. If you add `src/start.ts`, include `createCsrfMiddleware({filter: (ctx) => ctx.handlerType === 'serverFn'})`, or the
   server functions lose CSRF protection.
9. Finish with `npm run check`: typecheck, build and the leak check must pass.

Recipes: an action through a purpose-built server function; a tool; a workflow run — one server function starts it
with `workflows.execute(key, input, {waitSeconds})` and returns the run, one reads it with `workflows.getRun(runId)`,
and the page polls until `isFinished(run.status)` from `@bytechef/app`, showing `approvalFormUrl` while the status is
`APPROVAL_REQUIRED`; the proxy; an AI chat with `toAiSdkTools` — it needs `ai`, `zod`, a provider package and a model
key (the app's own secret, not governed by ByteChef), so ask the human first, and pass
`abortSignal: AbortSignal.timeout(…)` so a waiting workflow tool cannot hang the request.

Deploy (stays as it is; D has no hosting deploy): Vercel — `vercel env add BYTECHEF_URL` and `BYTECHEF_APP_KEY`
(production), then `vercel deploy --prod`; self-host — `docker build`, run with the two variables; after rotating the
key in ByteChef, update the host's variable and redeploy.

## Testing

- **Server:** a unit test for `ActionInputSchemaGenerator` (nested object in an array, options as enum, a
  display-conditioned field not required, dynamic properties as an open object); a facade unit test for the listing
  (allowed and unallowed actions, an unresolvable component skipped, a name missing from the latest version skipped); a
  controller test for `GET /actions`; the filter-chain test; `check` on the touched modules.
- **SDK:** `actions.list()` with a mocked `fetch`; `check:api`.
- **Scaffolder:** vitest unit tests for argument parsing (including the extra-positional refusal and the npm hint), the
  copy and renames (both dotfiles), name validation, `.env.local` contents and mode (and that no key is ever read from
  argv), the environment-key notice, the install spawn options per platform, and the refusal of a non-empty directory.
- **End to end in CI** — `npm run check:template` in the `sdk-app` job, a Node script with no extra dependencies that
  spawns every child with an explicit environment (the CI job's own `BYTECHEF_URL`/`BYTECHEF_APP_KEY` must not leak
  in), uses a free port (listen on 0), waits for readiness with a bound, and tears down in `finally`:
  1. `npm pack` `@bytechef/app` and `create-bytechef-app`; run the packed bin directly
     (`npm exec --package=<tgz> -- create-bytechef-app <dir> --url <mock> --no-install`);
  2. install, with `@bytechef/app` pointing at the packed tarball;
  3. the generated app's `npm run check` with a canary key;
  4. start a mock ByteChef — a `node:http` server answering `GET /connections`, `/actions`, `/tools` and `/workflows`
     from fixtures, recording every request and failing the run on any `POST`;
  5. `npm run -s bytechef:status` and `node scripts/bytechef-status.mjs` against it: the JSON shape including action
     input schemas; with `BYTECHEF_APP_KEY=''` the exit code 1, the stderr message and an empty stdout; with the mock
     answering 404 on `/actions`, `actionSchemas: false` and names without schemas;
  6. start the production build (`node .output/server/index.mjs`) against the mock: the page is the placeholder and
     contains no fixture binding key, action name or schema;
  7. the production server build contains no `tryAction` or `getAppOverview` server function (grep the server output
     for their names), and the mock recorded no request during step 6;
  8. a development-build check of the overview: a vitest test of the overview loader/handler against the mock, or a
     `vite dev` SSR fetch asserting the fixture's binding renders.

## Plan order

1. Action schemas: `ActionInputSchemaGenerator`, the server endpoint, contract, generated models, SDK `actions.list()`.
2. The template app, built in the workspace first and proven with `@bytechef/app` as a tarball (Nitro build, leak
   check, placeholder vs development overview).
3. The scaffolder.
4. The CI end-to-end check.
5. Docs: the package README, workspace README and `DEVELOPMENT.md`, a Template section in `.agents/apps.md`, the
   amendment of B's spike-verdict note and the deep-dive's SDK bullet (the template does not import the ready-made
   server functions), the Apps spec's C section reduced to a pointer here and its release check reworded (the deployed
   page is a placeholder; the check becomes: the development overview renders the bound actions, and disabling the app
   makes `bytechef:status` fail within one request).
