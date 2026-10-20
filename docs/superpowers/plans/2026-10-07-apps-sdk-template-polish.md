# Apps — SDK and Template Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the robustness and housekeeping gaps left in `@bytechef/app`, the app template and
`create-bytechef-app`: endpoints that answer success without a usable body, binary proxy bodies other than
`Uint8Array`, duplicated tool-listing requests, an untyped/unlinted Vite config, unformatted template files, the
status script's Node notice, a wrong devtools port note, and the scaffolder's scoped names, URLs and line breaks.

**Architecture:** Library changes stay inside `library/src/core`: a new `response.ts` checks the JSON shape each
endpoint promises (the generic requester keeps returning `undefined` for an empty body), `proxy.ts` decodes lazily and
accepts any `ArrayBuffer`/`ArrayBufferView`, and `tools.ts` shares one in-flight `GET /tools` that runs on no caller's
signal while every caller races its own. Template formatting is enforced by the workspace `.prettierignore` (which
lists only generated template files) and a first `check:template` step. The status script loads `.env.local` itself
with `util.parseEnv`; the scaffolder resolves `@scope/name` targets and validates its inputs before writing anything.

**Tech Stack:** TypeScript 5.7.3, vitest 4.1.7, ESLint 10 + typescript-eslint 8.60, prettier 3.8.3, Vite 8.3.3,
Node 24 (default) and Node 22.18+, npm 11.

**Spec:** `docs/superpowers/specs/2026-10-07-apps-hardening-and-polish-design.md`
section "SDK and template polish (items 11-16)" (binding), plus
`docs/superpowers/specs/2026-10-07-apps-template-design.md` and
`docs/superpowers/specs/2026-10-06-apps-workflows-and-sdk-design.md`. Background: `.agents/apps.md` (Template
section), `sdks/frontend/automation/app/DEVELOPMENT.md`.

## Rulings on points the design leaves open (settled by probes run while writing this plan)

1. **Empty 2xx (item 11).** Every endpoint the SDK calls promises a JSON body, but the requester is generic, and
   `request.test.ts` pins `{data: undefined}` for a 204. The check therefore lives one layer up: `response.ts`'s
   `expectArray`/`expectObject` reject an empty body, a JSON `null` and a body of the wrong JSON shape with
   `INVALID_RESPONSE` and the HTTP status. The requester is unchanged.
2. **Proxy result laziness (item 11).** A `BASE64` body is decoded eagerly, so invalid base64 rejects the `proxy(...)`
   promise with `INVALID_RESPONSE` (callers already handle `BytechefAppError` there), not a later `bytes()` call.
   `text()` decodes on first use and is memoised; `bytes()` encodes a `UTF8` body on first use, memoises, and returns a
   fresh copy on every call; `json()` parses the memoised text on every call (not memoised, so callers never share a
   mutable parsed object, consistent with `bytes()` copies).
3. **Shared tool listing (item 11).** The shared `GET /tools` is started with **no caller's signal**: it is bounded by
   the client's `timeoutMs` (the requester's own timer). Each caller races the shared promise against its own signal
   with the requester's `untilAborted`, so an abort rejects only that caller with `ABORTED`. A caller whose signal is
   already aborted is rejected before anything is fetched or joined. The shared promise is cleared in `finally`, so a
   failed fetch is never cached: every caller waiting on it gets the same error, `cache` keeps whatever it held (an
   expired entry stays expired), and the next call fetches again. A fetch every caller abandoned still completes and
   fills the cache. `tools.list()` keeps its own fresh fetch with its caller's signal. `now` stays injectable.
4. **`bytechef:status` env precedence (item 15).** `check-bundle.mjs` has no precedence to mirror: it unions the key
   values of `process.env` and the env files. The status script mirrors its *loading* (relative path, `existsSync` +
   `util.parseEnv(readFileSync(...))`) and keeps the precedence that `--env-file-if-exists` gave and the template spec
   states ("The shell environment wins over `.env.local`"): a variable already present in `process.env`, even as the
   empty string, is not replaced. Every variable in the file is loaded, as `--env-file` did. When the file is absent
   the script writes one stderr line: `No .env.local here; BYTECHEF_URL and BYTECHEF_APP_KEY come from the environment
   only.`
5. **The npm `--` hint (item 16).** Probed with npm 11.12.1 (`npm exec --package=<dir> create-showx my-app
   --no-install --url https://x`): npm exports a swallowed `--url` as `npm_config_url=true` but a swallowed
   `--no-install` as `npm_config_install=` (the **empty string**), which `args.test.mjs` already pins. Taken literally,
   "non-empty" would never fire for `--no-install` and the scaffolder would silently install. Ruling: the URL hint fires
   when `npm_config_url` is non-empty and `--url` did not reach the CLI; the install hint fires when
   `npm_config_install` is **set** (npm's encoding of the negation is the empty string) and neither `--install` nor
   `--no-install` reached the CLI.
6. **Line breaks (item 16).** `new URL()` silently strips CR/LF/tab (`new URL('http://a\nb').href` is `http://ab/`), so
   `--url` is checked for `\r`/`\n` before it is parsed. `buildEnvLocal` also refuses a line break in the URL it writes
   (from `--url` or the `BYTECHEF_URL` fallback) and in `BYTECHEF_APP_KEY`, without echoing the key. The http(s) check
   applies to `--url` only, as the design says.
7. **Which template files prettier checks (item 13).** Exactly the design's list. Probed in a copy of the workspace: a
   `.prettierignore` that keeps `create/template/**` and re-includes with `!` patterns un-ignores everything, so the
   ignore file instead lists the template's generated and starter-only files (`package-lock.json`,
   `src/components/ui`, `*.json`, `*.md`; `routeTree.gen.ts` is already covered by `**/routeTree.gen.ts`). With it,
   prettier's file set under `create/template` is exactly `scripts/*.mjs`, `src/server/*`, `src/routes/*`,
   `src/components/overview.tsx`, `src/router.tsx`, `src/styles.css`, `vite.config.ts`; of those only `router.tsx`,
   `styles.css` and `vite.config.ts` need the one-time reformat. Config: the workspace `.prettierrc` (4 spaces,
   semicolons, single quotes, no bracket spacing); the template ships no prettier config and none is added.
8. **Library Vite config (item 12).** `tsc -p tsconfig.node.json` on today's file fails (`'test' does not exist in type
   'UserConfigExport'`: vitest 4 dropped the `/// <reference types="vitest" />` augmentation) and ESLint reports four
   `sort-keys` errors. The fix imports `defineConfig` from `vitest/config` and sorts the keys; `tsconfig.node.json`
   drops `composite`/`tsBuildInfoFile` (nothing references it, and `composite` made `tsc` write
   `node_modules/.tmp/tsconfig.node.tsbuildinfo`) and sets `noEmit`. Both verified clean in a scratch copy.

## Global Constraints

- Paths below are relative to the worktree root
  `/Volumes/Data/bytechef/bytechef/.claude/worktrees/helix-bytechef-integration-086416`. Never `cd` to the main repo.
- `WS` = `sdks/frontend/automation/app` (the npm workspace root). **Every command runs with the current directory set
  to `WS`** unless the step says otherwise. `SCRATCH` = your session scratchpad directory (logs go there, never into
  the repo).
- Command discipline: redirect each command's output to a file under `$SCRATCH`, print `echo "exit=$?"` on its own line
  immediately after, then `grep` the file. Never pipe a test or build into `tail`/`grep` and read the pipeline's exit
  code. Run in the foreground with a 600000 ms timeout. No `&`, no `sleep`.
- Workspace scripts (from `WS/package.json`): `npm run check:all` (library `check:api`, lint, format, typecheck, test,
  then `test:create`), `npm run test:create`, `npm run check:template` (about one minute; builds and packs the library),
  `npm run format` (`prettier --check .`), `npm run format:fix`. Library scripts run with `-w library`, e.g.
  `npm run test -w library -- src/core/tools.test.ts`; create's with `-w create`, e.g.
  `npm run test -w create -- test/args.test.mjs`.
- Node: the default `node` is 24.15.0 (npm 11). The generated app must also pass on Node 22.18+: run `check:template`
  under 22.18.0 with `PATH="$HOME/.nvm/versions/node/v22.18.0/bin:$PATH" npm run check:template` (nvm has 22.17.1,
  22.18.0, 22.20.0, 22.23.3 and 24.15.0 installed; 22.17.1 is below the template's floor and is not used).
- Library code style: `sort-keys` (asc, natural, case-sensitive) and `@typescript-eslint/consistent-type-imports` are
  ESLint errors; prettier: 4 spaces, single quotes, semicolons, `bracketSpacing: false`, `printWidth: 120`,
  `trailingComma: es5`. Descriptive names everywhere (no `e`, `u`, `r`, `o`), including arrow parameters and loop
  variables. No rationale comments in code.
- Commits: stage by explicit path (`git add <path> ...`), never `git add -A`/`.`, never `--amend`, never `git stash`.
  Subject style `sdk - <sentence>` for SDK/template/scaffolder code, `docs - <sentence>` for docs-only commits. No
  `Co-Authored-By` trailer, no "Generated with Claude Code" line. Check `git branch --show-current` prints
  `claude/helix-bytechef-integration-086416` before each commit.
- Do not change error codes the README documents (`NETWORK_ERROR`, `TIMEOUT`, `ABORTED`, `HTTP_ERROR`,
  `INVALID_RESPONSE`, `APP_TOOL_NOT_FOUND`); this plan adds no new code.
- No retries anywhere in the SDK (actions are not idempotent); sharing one listing request is not a retry.
- `create-bytechef-app` keeps zero runtime dependencies (`node:` modules only).
- Generated files stay unformatted and untouched: `create/template/src/routeTree.gen.ts`,
  `create/template/src/components/ui/*`, `create/template/package-lock.json`, `library/src/generated/openapi.ts`.

## Review Focus

These are the inputs most likely to bite someone that the design implies but does not name; each is pinned by a test
in the task that owns the code.

1. **A pooled Node `Buffer` or a `subarray` view as a proxy body** (`Buffer.from('hi')` has `byteOffset` 16 into an
   8 KiB pool): only the view's bytes are sent, never the whole backing buffer. Pinned in Task 2.
2. **A typed array or `ArrayBuffer` from another realm** (a `vm` context, jsdom, a worker): sent as bytes, not
   `JSON.stringify`'d into an index-keyed object. Pinned in Task 2.
3. **A success answer whose body is the JSON `null`**: `INVALID_RESPONSE` with the status, not a `TypeError` from
   `data.result`/`data.map`. Pinned in Task 1.
4. **Every caller of a shared tool listing aborts, then the abandoned request fails**: no unhandled rejection, and
   the next call fetches afresh and succeeds. Pinned in Task 3.
5. **A bare carriage return in the app key or `--url`** (a key pasted from a Windows clipboard): refused before
   anything is written, and the key's value never appears in the error. Pinned in Task 7.

---

## File map

| File (under `WS`) | Task | Responsibility |
|---|---|---|
| `library/src/core/response.ts` (new) | 1 | `expectArray`/`expectObject`: the JSON shape an endpoint promises, else `INVALID_RESPONSE` |
| `library/src/core/response.test.ts` (new) | 1 | Every endpoint against an empty, `null` and wrong-shape success body |
| `library/src/core/connections.ts`, `workflows.ts` | 1 | Use the shape checks |
| `library/src/core/proxy.ts`, `types.ts`, `proxy.test.ts` | 1, 2 | Shape check; binary bodies; lazy results; base64 validation |
| `library/src/core/tools.ts`, `tools.test.ts` | 1, 3 | Shape check; one shared in-flight listing |
| `library/src/core/request.ts` | 3 | Export `abortError` and `untilAborted` |
| `library/src/core/workflows.test.ts` | 3 | Poll `TIMEOUT` path |
| `library/README.md` | 1, 2, 3 | Error table, proxy section, `executeByName` sharing |
| `library/vite.config.ts`, `tsconfig.node.json`, `package.json`, `.npmignore` (deleted) | 4 | Typecheck + lint the config; `files` governs the tarball |
| `.prettierignore`, `package.json` | 4, 5 | Accurate comments; template files that are ours get formatted |
| `scripts/check-template.mjs` | 5, 6 | Formatting step; status-without-`.env.local` steps |
| `create/template/{vite.config.ts,src/router.tsx,src/styles.css}` | 5 | One-time reformat |
| `create/template/scripts/bytechef-status.mjs`, `create/template/package.json` | 6 | Load `.env.local` itself |
| `create/src/args.mjs`, `create/src/env.mjs`, `create/bin/create-bytechef-app.mjs` | 7 | Scoped targets, URL and line-break validation, precise npm hint |
| `create/test/{args,env,bin}.test.mjs` | 7 | Tests for the above |
| `DEVELOPMENT.md`, `create/README.md`, `.agents/apps.md`, `docs/superpowers/specs/2026-10-07-apps-template-design.md` | 4-7 | Docs |

---

### Task 1: Endpoints reject a success answer without the body they promise

**Files:**
- Create: `WS/library/src/core/response.ts`
- Create: `WS/library/src/core/response.test.ts`
- Modify: `WS/library/src/core/connections.ts` (whole file)
- Modify: `WS/library/src/core/tools.ts:28-36` (`fetchListing`) and `:46-60` (`execute`)
- Modify: `WS/library/src/core/proxy.ts:1-4` (imports) and `:77-90` (`createProxyApi`)
- Modify: `WS/library/src/core/workflows.ts:1-4` (imports), `:85-93` (`getRun`), `:107-114` (start of `execute`),
  `:152-160` (`list`)
- Modify: `WS/library/README.md` (the `INVALID_RESPONSE` row of the error table)

**Interfaces:**
- Consumes: `RequestResult<T>` (`{data: T; status: number}`) and `Requester` from `library/src/core/request.ts`.
- Produces (in `library/src/core/response.ts`):
  - `expectArray<T>(result: RequestResult<unknown>, path: string): T[]`
  - `expectObject<T>(result: RequestResult<unknown>, path: string): T`
  Both throw `BytechefAppError('INVALID_RESPONSE', ..., {status})`. Task 2 and Task 3 call them.

- [ ] **Step 1: Write the failing test**

Create `WS/library/src/core/response.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import {jsonResponse, mockFetch} from '../test/mockFetch';
import {type BytechefClient, createBytechefClient} from './client';

type EndpointCall = [string, (client: BytechefClient) => Promise<unknown>, unknown];

const ENDPOINT_CALLS: EndpointCall[] = [
    ['connections.list', (client) => client.connections.list(), {}],
    ['actions.list', (client) => client.actions.list(), {}],
    ['actions.execute', (client) => client.actions.execute('crm', 'createRecord', {}), []],
    ['tools.list', (client) => client.tools.list(), {}],
    ['tools.execute', (client) => client.tools.execute('crm', 'searchRecords', {}), []],
    ['tools.executeByName', (client) => client.tools.executeByName('CRM__SEARCH_RECORDS', {}), {}],
    ['proxy', (client) => client.proxy('crm', {method: 'GET', path: '/x'}), []],
    ['workflows.list', (client) => client.workflows.list(), {}],
    ['workflows.execute', (client) => client.workflows.execute('approve', {}), []],
    ['workflows.getRun', (client) => client.workflows.getRun('run-1'), []],
];

function clientAnswering(response: Response): BytechefClient {
    const {fetch} = mockFetch(response);

    return createBytechefClient({appKey: 'k', fetch, url: 'https://bc.example'});
}

describe('an endpoint answering success without the body it promises', () => {
    it.each(ENDPOINT_CALLS)('%s rejects an empty 200 with INVALID_RESPONSE', async (_name, call) => {
        await expect(call(clientAnswering(new Response(null, {status: 200})))).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
            status: 200,
        });
    });

    it.each(ENDPOINT_CALLS)('%s rejects a JSON null with INVALID_RESPONSE', async (_name, call) => {
        await expect(call(clientAnswering(jsonResponse({text: 'null'})))).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
            status: 200,
        });
    });

    it.each(ENDPOINT_CALLS)(
        '%s rejects a JSON body of the wrong shape with INVALID_RESPONSE',
        async (_name, call, wrongShape) => {
            await expect(call(clientAnswering(jsonResponse({body: wrongShape})))).rejects.toMatchObject({
                code: 'INVALID_RESPONSE',
                status: 200,
            });
        }
    );

    it('names the path and the expected shape in the message', async () => {
        const error = await clientAnswering(jsonResponse({body: {}}))
            .connections.list()
            .catch((caught: unknown) => caught);

        expect((error as Error).message).toBe(
            'ByteChef answered 200 to /connections without a JSON array; check that BYTECHEF_URL points at the ByteChef server'
        );
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
npm run test -w library -- src/core/response.test.ts > "$SCRATCH/t1-red.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |✓|×" "$SCRATCH/t1-red.log" | head -40
```

Expected: `exit=1`. The list endpoints resolve `undefined`/`{}` instead of rejecting ("promise resolved ... instead of
rejecting"); the `execute`/`getRun`/`proxy` rows fail with `TypeError` instead of `INVALID_RESPONSE`; a few rows (for
example `tools.executeByName` on an empty body) may already reject with the wrong error. The message test fails.

- [ ] **Step 3: Create `response.ts`**

Create `WS/library/src/core/response.ts`:

```ts
import {BytechefAppError} from './errors';
import type {RequestResult} from './request';

function invalidResponse(status: number, path: string, expected: string): BytechefAppError {
    return new BytechefAppError(
        'INVALID_RESPONSE',
        `ByteChef answered ${status} to ${path} without ${expected}; check that BYTECHEF_URL points at the ByteChef server`,
        {status}
    );
}

export function expectArray<T>({data, status}: RequestResult<unknown>, path: string): T[] {
    if (!Array.isArray(data)) {
        throw invalidResponse(status, path, 'a JSON array');
    }

    return data as T[];
}

export function expectObject<T>({data, status}: RequestResult<unknown>, path: string): T {
    if (typeof data !== 'object' || data === null || Array.isArray(data)) {
        throw invalidResponse(status, path, 'a JSON object');
    }

    return data as T;
}
```

- [ ] **Step 4: Use the checks in `connections.ts`**

Replace the whole of `WS/library/src/core/connections.ts` with:

```ts
import type {Requester} from './request';
import {expectArray, expectObject} from './response';
import type {AppAction, AppBinding} from './types';

export interface CallOptions {
    signal?: AbortSignal;
}

export interface ExecuteActionOptions extends CallOptions {
    componentVersion?: number;
}

export function createConnectionsApi(request: Requester) {
    return {
        async list(options: CallOptions = {}): Promise<AppBinding[]> {
            const path = '/connections';

            return expectArray<AppBinding>(await request({method: 'GET', path, signal: options.signal}), path);
        },
    };
}

export function createActionsApi(request: Requester) {
    return {
        async execute<T = unknown>(
            bindingKey: string,
            actionName: string,
            input: Record<string, unknown>,
            options: ExecuteActionOptions = {}
        ): Promise<T> {
            const body: Record<string, unknown> = {input};

            if (options.componentVersion !== undefined) {
                body['componentVersion'] = options.componentVersion;
            }

            const path = `/connections/${encodeURIComponent(bindingKey)}/actions/${encodeURIComponent(actionName)}`;
            const data = expectObject<{result: T}>(
                await request({body, method: 'POST', path, signal: options.signal}),
                path
            );

            return data.result;
        },

        async list(options: CallOptions = {}): Promise<AppAction[]> {
            const path = '/actions';

            return expectArray<AppAction>(await request({method: 'GET', path, signal: options.signal}), path);
        },
    };
}
```

- [ ] **Step 5: Use the checks in `tools.ts`**

In `WS/library/src/core/tools.ts`, add to the imports (keep the existing ones):

```ts
import {expectArray, expectObject} from './response';
```

Replace `fetchListing` (lines 28-36) with:

```ts
    async function fetchListing(signal?: AbortSignal): Promise<AppTool[]> {
        const path = '/tools';
        const tools = expectArray<RawTool>(await request({method: 'GET', path, signal}), path).map((tool) => ({
            ...tool,
            parameters: parseParameters(tool.parameters),
        }));

        cache = {fetchedAt: now(), tools};

        return tools;
    }
```

Replace `execute` (lines 46-60) with:

```ts
    async function execute<T = unknown>(
        bindingKey: string,
        toolName: string,
        input: Record<string, unknown>,
        options: CallOptions = {}
    ): Promise<T> {
        const path = `/connections/${encodeURIComponent(bindingKey)}/tools/${encodeURIComponent(toolName)}`;
        const data = expectObject<{result: T}>(
            await request({body: {input}, method: 'POST', path, signal: options.signal}),
            path
        );

        return data.result;
    }
```

- [ ] **Step 6: Use the check in `proxy.ts`**

In `WS/library/src/core/proxy.ts`, add `import {expectObject} from './response';` after the `Requester` import, and
replace `createProxyApi` (lines 77-90) with:

```ts
export function createProxyApi(request: Requester) {
    return async (bindingKey: string, proxyRequest: ProxyRequest, options: CallOptions = {}): Promise<ProxyResult> => {
        const {body, contentType, ...rest} = proxyRequest;
        const path = `/connections/${encodeURIComponent(bindingKey)}/proxy`;
        const result = await request({
            body: {...rest, ...encodeProxyBody(body, contentType)},
            method: 'POST',
            path,
            signal: options.signal,
        });

        return toResult(expectObject<RawProxyResponse>(result, path));
    };
}
```

- [ ] **Step 7: Use the checks in `workflows.ts`**

In `WS/library/src/core/workflows.ts`, add `import {expectArray, expectObject} from './response';` after the
`Requester` import. Replace `getRun` (lines 85-93) with:

```ts
    async function getRun(runId: string, options: CallOptions = {}): Promise<WorkflowRun> {
        const path = `/workflow-runs/${encodeURIComponent(runId)}`;

        return expectObject<WorkflowRun>(await request({method: 'GET', path, signal: options.signal}), path);
    }
```

Replace lines 107-114 (from `const {data} = await request<WorkflowRun>({` through `let run = data;`) with:

```ts
            const path = `/workflows/${encodeURIComponent(workflowKey)}`;
            let run = expectObject<WorkflowRun>(
                await request({body, method: 'POST', path, signal: options.signal}),
                path
            );
```

Replace `list` (lines 152-160) with:

```ts
        async list(options = {}) {
            const path = '/workflows';

            return expectArray<AppWorkflow>(await request({method: 'GET', path, signal: options.signal}), path);
        },
```

- [ ] **Step 8: Run the test to verify it passes, then the library suite**

```bash
npm run test -w library -- src/core/response.test.ts > "$SCRATCH/t1-green.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests " "$SCRATCH/t1-green.log"
npm run test -w library > "$SCRATCH/t1-lib.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |FAIL" "$SCRATCH/t1-lib.log"
```

Expected: both `exit=0`; `response.test.ts` 31 tests passed; the full library suite passes (the requester's
`returns data undefined for a 204` test is untouched and still passes).

- [ ] **Step 9: Update the README error table**

In `WS/library/README.md`, replace the `INVALID_RESPONSE` row's meaning with:

```
A success status whose body is empty, not JSON, or not the JSON array or object the endpoint returns; check that `BYTECHEF_URL` points at the ByteChef server.
```

Then realign the table:

```bash
npx prettier --write library/README.md library/src/core > "$SCRATCH/t1-fmt.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 10: Full checks**

```bash
npm run check:all > "$SCRATCH/t1-check.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |error|warn|FAIL" "$SCRATCH/t1-check.log" | head -40
npm run format > "$SCRATCH/t1-format.log" 2>&1
echo "exit=$?"
```

Expected: both `exit=0`.

- [ ] **Step 11: Commit**

```bash
git branch --show-current
git add library/src/core/response.ts library/src/core/response.test.ts library/src/core/connections.ts library/src/core/tools.ts library/src/core/proxy.ts library/src/core/workflows.ts library/README.md
git commit -m "sdk - Reject a success answer without the body its endpoint promises"
```

---

### Task 2: Binary proxy bodies of any kind, lazy proxy results, validated base64

**Files:**
- Modify: `WS/library/src/core/types.ts:11` (`ProxyBody`)
- Modify: `WS/library/src/core/proxy.ts` (whole file)
- Test: `WS/library/src/core/proxy.test.ts` (append three `describe` blocks)
- Modify: `WS/library/README.md` (Proxy results table and the paragraph after it)

**Interfaces:**
- Consumes: `expectObject<T>(result: RequestResult<unknown>, path: string): T` from Task 1.
- Produces: `export type ProxyBody = string | ArrayBuffer | ArrayBufferView | Record<string, unknown> | unknown[];`
  (exported from `@bytechef/app`); `encodeProxyBody(body: ProxyBody | undefined, contentType: string | undefined)`
  keeps its signature and return type; `ProxyResult` keeps its shape.

- [ ] **Step 1: Write the failing tests**

In `WS/library/src/core/proxy.test.ts`, change the first import line to
`import {describe, expect, it, vi} from 'vitest';`, add `import {runInNewContext} from 'node:vm';` as the first line
of the file (before the vitest import, followed by a blank line), and append:

```ts
describe('encodeProxyBody binary bodies', () => {
    it('sends an ArrayBuffer as BASE64 with an octet-stream content type', () => {
        expect(encodeProxyBody(new Uint8Array([104, 105]).buffer, undefined)).toEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            contentType: 'application/octet-stream',
        });
    });

    it('sends only the bytes a view covers, not its whole backing buffer', () => {
        const backing = new Uint8Array([0, 104, 105, 0]);

        expect(encodeProxyBody(backing.subarray(1, 3), undefined).body).toBe('aGk=');
        expect(encodeProxyBody(new DataView(backing.buffer, 1, 2), undefined).body).toBe('aGk=');
    });

    it('sends a pooled Node Buffer as exactly its own bytes', () => {
        const pooled = Buffer.from('hi');

        expect(pooled.byteOffset).toBeGreaterThan(0);
        expect(encodeProxyBody(pooled, 'text/plain')).toEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            contentType: 'text/plain',
        });
    });

    it('sends a typed array other than Uint8Array as its raw bytes', () => {
        expect(encodeProxyBody(new Float32Array([1]), undefined).body).toBe('AACAPw==');
    });

    it('sends a typed array and an ArrayBuffer from another realm as bytes', () => {
        const foreignBytes = runInNewContext('new Uint8Array([104, 105])') as Uint8Array;
        const foreignBuffer = runInNewContext('new Uint8Array([104, 105]).buffer') as ArrayBuffer;

        expect(encodeProxyBody(foreignBytes, undefined)).toEqual({
            body: 'aGk=',
            bodyEncoding: 'BASE64',
            contentType: 'application/octet-stream',
        });
        expect(encodeProxyBody(foreignBuffer, undefined).body).toBe('aGk=');
    });
});

describe('proxy result access', () => {
    it('returns a fresh copy from every bytes() call', async () => {
        const {proxy} = proxyWith(jsonResponse({body: {body: 'hi', bodyEncoding: 'UTF8', headers: {}, status: 200}}));

        const result = await proxy('crm', {method: 'GET', path: '/x'});
        const first = result.bytes();

        first[0] = 0;

        expect(Array.from(result.bytes())).toEqual([104, 105]);
        expect(result.bytes()).not.toBe(result.bytes());
    });

    it('decodes the text of a BASE64 body only when asked, and only once', async () => {
        const {proxy} = proxyWith(
            jsonResponse({body: {body: 'aGk=', bodyEncoding: 'BASE64', headers: {}, status: 200}})
        );

        const result = await proxy('crm', {method: 'GET', path: '/file'});
        const decodeSpy = vi.spyOn(TextDecoder.prototype, 'decode');

        try {
            expect(decodeSpy).not.toHaveBeenCalled();
            expect(result.text()).toBe('hi');
            expect(result.text()).toBe('hi');
            expect(decodeSpy).toHaveBeenCalledTimes(1);
        } finally {
            decodeSpy.mockRestore();
        }
    });
});

describe('proxy BASE64 validation', () => {
    it('rejects a BASE64 body that is not base64 with INVALID_RESPONSE and the ByteChef status', async () => {
        const {proxy} = proxyWith(
            jsonResponse({body: {body: '%%%', bodyEncoding: 'BASE64', headers: {}, status: 200}})
        );

        await expect(proxy('crm', {method: 'GET', path: '/file'})).rejects.toMatchObject({
            code: 'INVALID_RESPONSE',
            status: 200,
        });
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
npm run test -w library -- src/core/proxy.test.ts > "$SCRATCH/t2-red.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |×" "$SCRATCH/t2-red.log" | head -30
```

Expected: `exit=1`. Failing: the `ArrayBuffer`, `DataView`, `Float32Array` and foreign-realm cases (sent as JSON
`{}` or `{"0":104,"1":105}` with `application/json`), `returns a fresh copy` (same array, mutation visible), `decodes
... only once` (decoded eagerly before the spy, so `toHaveBeenCalledTimes(1)` sees 0), and the base64 case (a
`DOMException` `InvalidCharacterError`, no `code`). The pooled-Buffer and `subarray` cases already pass; they guard
the new view handling. Typecheck errors on `ArrayBuffer`/`DataView`/`Float32Array` arguments are expected until Step 3.

- [ ] **Step 3: Widen `ProxyBody`**

In `WS/library/src/core/types.ts` replace line 11 with:

```ts
export type ProxyBody = string | ArrayBuffer | ArrayBufferView | Record<string, unknown> | unknown[];
```

- [ ] **Step 4: Rewrite `proxy.ts`**

Replace the whole of `WS/library/src/core/proxy.ts` with:

```ts
import type {components} from '../generated/openapi';
import type {CallOptions} from './connections';
import {BytechefAppError} from './errors';
import type {Requester} from './request';
import {expectObject} from './response';
import type {ProxyBody, ProxyRequest, ProxyResult} from './types';

type RawProxyResponse = components['schemas']['ProxyResponse'];

function bytesToBase64(bytes: Uint8Array): string {
    let binary = '';

    for (let index = 0; index < bytes.length; index += 0x8000) {
        binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
    }

    return btoa(binary);
}

function base64ToBytes(base64: string, status: number): Uint8Array {
    let binary: string;

    try {
        binary = atob(base64);
    } catch (error) {
        throw new BytechefAppError(
            'INVALID_RESPONSE',
            'ByteChef answered a proxy body marked BASE64 that is not valid base64',
            {cause: error, status}
        );
    }

    const bytes = new Uint8Array(binary.length);

    for (let index = 0; index < binary.length; index += 1) {
        bytes[index] = binary.charCodeAt(index);
    }

    return bytes;
}

function binaryBytes(body: ProxyBody): Uint8Array | undefined {
    if (ArrayBuffer.isView(body)) {
        return new Uint8Array(body.buffer, body.byteOffset, body.byteLength);
    }

    if (Object.prototype.toString.call(body) === '[object ArrayBuffer]') {
        return new Uint8Array(body as ArrayBuffer);
    }

    return undefined;
}

export function encodeProxyBody(
    body: ProxyBody | undefined,
    contentType: string | undefined
): {body?: string; bodyEncoding?: 'BASE64' | 'UTF8'; contentType?: string} {
    if (body === undefined) {
        return contentType === undefined ? {} : {contentType};
    }

    if (typeof body === 'string') {
        return contentType === undefined ? {body, bodyEncoding: 'UTF8'} : {body, bodyEncoding: 'UTF8', contentType};
    }

    const bytes = binaryBytes(body);

    if (bytes !== undefined) {
        return {
            body: bytesToBase64(bytes),
            bodyEncoding: 'BASE64',
            contentType: contentType ?? 'application/octet-stream',
        };
    }

    return {body: JSON.stringify(body), bodyEncoding: 'UTF8', contentType: contentType ?? 'application/json'};
}

function toResult(raw: RawProxyResponse, status: number): ProxyResult {
    const body = raw.body ?? null;
    const base64Encoded = raw.bodyEncoding === 'BASE64';
    let decodedBytes: Uint8Array | undefined =
        body !== null && base64Encoded ? base64ToBytes(body, status) : undefined;
    let decodedText: string | undefined = body === null ? '' : base64Encoded ? undefined : body;

    function bodyBytes(): Uint8Array {
        decodedBytes ??= new TextEncoder().encode(decodedText ?? '');

        return decodedBytes;
    }

    function bodyText(): string {
        decodedText ??= new TextDecoder().decode(bodyBytes());

        return decodedText;
    }

    return {
        body,
        bodyEncoding: raw.bodyEncoding,
        bytes: () => bodyBytes().slice(),
        headers: Object.fromEntries(Object.entries(raw.headers ?? {}).map(([name, values]) => [name, [...values]])),
        json: <T = unknown>(): T => JSON.parse(bodyText()) as T,
        status: raw.status,
        text: bodyText,
    };
}

export function createProxyApi(request: Requester) {
    return async (bindingKey: string, proxyRequest: ProxyRequest, options: CallOptions = {}): Promise<ProxyResult> => {
        const {body, contentType, ...rest} = proxyRequest;
        const path = `/connections/${encodeURIComponent(bindingKey)}/proxy`;
        const result = await request({
            body: {...rest, ...encodeProxyBody(body, contentType)},
            method: 'POST',
            path,
            signal: options.signal,
        });

        return toResult(expectObject<RawProxyResponse>(result, path), result.status);
    };
}
```

- [ ] **Step 5: Run the tests to verify they pass**

```bash
npm run test -w library -- src/core/proxy.test.ts > "$SCRATCH/t2-green.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |×" "$SCRATCH/t2-green.log"
```

Expected: `exit=0`, every proxy test passes (the earlier `handles a null body`, `decodes a BASE64 body into bytes and
text`, `round-trips a body larger than one encoding chunk` and `decodes non-ASCII BASE64 text as UTF-8` included).

- [ ] **Step 6: Prove the view test guards the offset**

Temporarily change `new Uint8Array(body.buffer, body.byteOffset, body.byteLength)` in `proxy.ts` to
`new Uint8Array(body.buffer)`, then:

```bash
npm run test -w library -- src/core/proxy.test.ts > "$SCRATCH/t2-mutant.log" 2>&1
echo "exit=$?"
grep -E "×" "$SCRATCH/t2-mutant.log"
```

Expected: `exit=1`, with `sends only the bytes a view covers` and `sends a pooled Node Buffer` failing. Restore the
line exactly as in Step 4 and rerun Step 5 (expected `exit=0`).

- [ ] **Step 7: Update the README proxy section**

In `WS/library/README.md`, in the Proxy results table, replace the `text()` and `bytes()` rows with:

```
| `text()`       | The body as a string (a base64 body is decoded as UTF-8 on the first call).    |
| `bytes()`      | The body as a `Uint8Array`; every call returns a fresh copy.                   |
```

in the error table, replace the `INVALID_RESPONSE` row's meaning (Task 1's wording) with

```
A success status whose body is empty, not JSON, or not the JSON array or object the endpoint returns, or a proxy body marked `BASE64` that is not base64; check that `BYTECHEF_URL` points at the ByteChef server.
```

and replace the proxy paragraph's second sentence (`A request \`body\` is a string, ... (sent as base64).`) with:

```
A request `body` is a string, a plain object or array (sent as JSON), or binary data - an `ArrayBuffer`, any typed
array, a `DataView` or a Node `Buffer` - sent as base64 (only the bytes the view covers). A response body marked
`BASE64` that is not base64 rejects the call with `INVALID_RESPONSE`.
```

Then:

```bash
npx prettier --write library/README.md library/src/core > "$SCRATCH/t2-fmt.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 8: Full checks**

```bash
npm run check:all > "$SCRATCH/t2-check.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |error|FAIL" "$SCRATCH/t2-check.log" | head -40
npm run format > "$SCRATCH/t2-format.log" 2>&1
echo "exit=$?"
```

Expected: both `exit=0`.

- [ ] **Step 9: Commit**

```bash
git branch --show-current
git add library/src/core/types.ts library/src/core/proxy.ts library/src/core/proxy.test.ts library/README.md
git commit -m "sdk - Send any binary proxy body as bytes and decode proxy results lazily"
```

---

### Task 3: One shared tool listing for concurrent `executeByName` calls, and the poll `TIMEOUT` test

**Files:**
- Modify: `WS/library/src/core/request.ts:27` and `:33` (export two helpers)
- Modify: `WS/library/src/core/tools.ts` (whole file)
- Test: `WS/library/src/core/tools.test.ts` (import line; append one `describe`)
- Test: `WS/library/src/core/workflows.test.ts` (append one test inside the polling `describe`)
- Modify: `WS/library/README.md` (one paragraph after "Every call also accepts `{signal}`")

**Interfaces:**
- Consumes: `expectArray`, `expectObject` from Task 1.
- Produces (from `library/src/core/request.ts`, internal, not re-exported by `index.ts`):
  - `abortError(signal: AbortSignal): BytechefAppError`
  - `untilAborted<T>(promise: Promise<T>, signal: AbortSignal): Promise<T>`
- `createToolsApi(request: Requester, now?: () => number)` keeps its signature and returned shape
  (`execute`, `executeByName`, `list`).

- [ ] **Step 1: Write the failing tool tests**

In `WS/library/src/core/tools.test.ts` change line 1 to `import {describe, expect, it, vi} from 'vitest';` and append:

```ts
describe('executeByName shared listing', () => {
    it('shares one listing request between concurrent cold calls', async () => {
        let listingRequests = 0;
        const {calls, tools} = toolsWith(
            () => 0,
            (input) => {
                if (String(input).endsWith('/tools')) {
                    listingRequests += 1;

                    return jsonResponse({body: [searchTool]});
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );

        await expect(
            Promise.all([
                tools.executeByName('CRM__SEARCH_RECORDS', {}),
                tools.executeByName('CRM__SEARCH_RECORDS', {}),
            ])
        ).resolves.toEqual(['ok', 'ok']);
        expect(listingRequests).toBe(1);
        expect(calls).toHaveLength(3);
    });

    it('lets one caller abort without aborting the listing request the others wait on', async () => {
        let listingSignal: AbortSignal | undefined;
        let releaseListing: () => void = () => undefined;
        const {tools} = toolsWith(
            () => 0,
            (input, init) => {
                if (String(input).endsWith('/tools')) {
                    listingSignal = init?.signal ?? undefined;

                    return new Promise<Response>((resolve, reject) => {
                        init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                        releaseListing = () => resolve(jsonResponse({body: [searchTool]}));
                    });
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );
        const leaving = new AbortController();
        const leavingCall = tools.executeByName('CRM__SEARCH_RECORDS', {}, {signal: leaving.signal});
        const stayingCall = tools.executeByName('CRM__SEARCH_RECORDS', {});

        await vi.waitFor(() => expect(listingSignal).toBeDefined());

        leaving.abort('user left');

        await expect(leavingCall).rejects.toMatchObject({cause: 'user left', code: 'ABORTED'});
        expect(listingSignal!.aborted).toBe(false);

        releaseListing();

        await expect(stayingCall).resolves.toBe('ok');
    });

    it('hands a failed listing to every waiting caller and fetches afresh on the next call', async () => {
        let listingRequests = 0;
        const {tools} = toolsWith(
            () => 0,
            (input) => {
                if (String(input).endsWith('/tools')) {
                    listingRequests += 1;

                    return listingRequests === 1
                        ? jsonResponse({body: {code: 'BOOM', message: 'down'}, status: 503})
                        : jsonResponse({body: [searchTool]});
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );

        const settled = await Promise.allSettled([
            tools.executeByName('CRM__SEARCH_RECORDS', {}),
            tools.executeByName('CRM__SEARCH_RECORDS', {}),
        ]);

        expect(
            settled.map((outcome) => (outcome.status === 'rejected' ? (outcome.reason as {code: string}).code : 'ok'))
        ).toEqual(['BOOM', 'BOOM']);

        await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('ok');
        expect(listingRequests).toBe(2);
    });

    it('leaves no unhandled rejection when every caller left and the abandoned listing then fails', async () => {
        let listingRequests = 0;
        let failListing: () => void = () => undefined;
        const {tools} = toolsWith(
            () => 0,
            (input, init) => {
                if (String(input).endsWith('/tools')) {
                    listingRequests += 1;

                    if (listingRequests === 1) {
                        return new Promise<Response>((resolve, reject) => {
                            init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                            failListing = () =>
                                resolve(jsonResponse({body: {code: 'BOOM', message: 'down'}, status: 503}));
                        });
                    }

                    return jsonResponse({body: [searchTool]});
                }

                return jsonResponse({body: {result: 'ok'}});
            }
        );
        const leaving = new AbortController();
        const leavingCall = tools.executeByName('CRM__SEARCH_RECORDS', {}, {signal: leaving.signal});

        leaving.abort();

        await expect(leavingCall).rejects.toMatchObject({code: 'ABORTED'});

        failListing();

        await vi.waitFor(async () => {
            await expect(tools.executeByName('CRM__SEARCH_RECORDS', {})).resolves.toBe('ok');
        });
        expect(listingRequests).toBe(2);
    });
});
```

- [ ] **Step 2: Write the poll `TIMEOUT` test**

In `WS/library/src/core/workflows.test.ts`, directly after the test `adds the runId to a network failure while
polling` (in the same `describe`), add:

```ts
    it('adds the runId to a TIMEOUT while polling and keeps the timeout as the cause', async () => {
        const {fetch} = mockFetch(
            jsonResponse({body: running, status: 202}),
            (_input, init) =>
                new Promise<Response>((_resolve, reject) => {
                    init?.signal?.addEventListener('abort', () => reject(init.signal!.reason));
                })
        );
        const config = {appKey: 'k', baseUrl: 'https://bc.example', fetch, timeoutMs: 50};
        const workflows = createWorkflowsApi(
            createRequester(() => config),
            () => config.timeoutMs,
            async () => undefined
        );

        const error = await workflows.execute('approve', {}, {wait: true}).catch((caught: unknown) => caught);

        expect(error).toMatchObject({code: 'TIMEOUT', runId: 'run-1', status: undefined});
        expect((error as {cause: unknown}).cause).toMatchObject({code: 'TIMEOUT'});
    });
```

- [ ] **Step 3: Run the tests to verify which fail**

```bash
npm run test -w library -- src/core/tools.test.ts src/core/workflows.test.ts > "$SCRATCH/t3-red.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |✓|×" "$SCRATCH/t3-red.log" | grep -E "Tests |shared|abort|failed listing|TIMEOUT|abandoned"
```

Expected: `exit=1`. `shares one listing request` fails (`listingRequests` is 2) and `hands a failed listing to every
waiting caller` fails (the second caller resolves `'ok'`). Passing already, as guards for the new code: `lets one
caller abort ...` (today each caller has its own request; a shared request started on the first caller's signal would
fail it), `leaves no unhandled rejection ...`, and the `TIMEOUT` test (the path exists and had no test).

- [ ] **Step 4: Export the abort helpers**

In `WS/library/src/core/request.ts`, change line 27 `function abortError(signal: AbortSignal): BytechefAppError {`
to `export function abortError(signal: AbortSignal): BytechefAppError {` and line 33
`function untilAborted<T>(promise: Promise<T>, signal: AbortSignal): Promise<T> {` to
`export function untilAborted<T>(promise: Promise<T>, signal: AbortSignal): Promise<T> {`.

- [ ] **Step 5: Rewrite `tools.ts`**

Replace the whole of `WS/library/src/core/tools.ts` with:

```ts
import type {components} from '../generated/openapi';
import type {CallOptions} from './connections';
import {BytechefAppError} from './errors';
import {abortError, type Requester, untilAborted} from './request';
import {expectArray, expectObject} from './response';
import type {AppTool, JsonSchema} from './types';

export const TOOL_LISTING_TTL_MS = 60_000;

type RawTool = components['schemas']['AppTool'];

const ANY_OBJECT: JsonSchema = {additionalProperties: true, type: 'object'};

const TOOLS_PATH = '/tools';

function parseParameters(parameters: string): JsonSchema {
    try {
        const parsed: unknown = JSON.parse(parameters);

        return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)
            ? (parsed as JsonSchema)
            : {...ANY_OBJECT};
    } catch {
        return {...ANY_OBJECT};
    }
}

export function createToolsApi(request: Requester, now: () => number = () => Date.now()) {
    let cache: {fetchedAt: number; tools: AppTool[]} | undefined;
    let sharedListing: Promise<AppTool[]> | undefined;

    async function fetchListing(signal?: AbortSignal): Promise<AppTool[]> {
        const tools = expectArray<RawTool>(await request({method: 'GET', path: TOOLS_PATH, signal}), TOOLS_PATH).map(
            (tool) => ({...tool, parameters: parseParameters(tool.parameters)})
        );

        cache = {fetchedAt: now(), tools};

        return tools;
    }

    function refreshListing(signal?: AbortSignal): Promise<AppTool[]> {
        if (signal?.aborted) {
            return Promise.reject(abortError(signal));
        }

        sharedListing ??= fetchListing().finally(() => {
            sharedListing = undefined;
        });

        return signal ? untilAborted(sharedListing, signal) : sharedListing;
    }

    function cachedListing(signal?: AbortSignal): Promise<AppTool[]> {
        if (cache && now() - cache.fetchedAt < TOOL_LISTING_TTL_MS) {
            return Promise.resolve(cache.tools);
        }

        return refreshListing(signal);
    }

    async function execute<T = unknown>(
        bindingKey: string,
        toolName: string,
        input: Record<string, unknown>,
        options: CallOptions = {}
    ): Promise<T> {
        const path = `/connections/${encodeURIComponent(bindingKey)}/tools/${encodeURIComponent(toolName)}`;
        const data = expectObject<{result: T}>(
            await request({body: {input}, method: 'POST', path, signal: options.signal}),
            path
        );

        return data.result;
    }

    return {
        execute,

        async executeByName<T = unknown>(
            flatName: string,
            input: Record<string, unknown>,
            options: CallOptions = {}
        ): Promise<T> {
            let tool = (await cachedListing(options.signal)).find((candidate) => candidate.name === flatName);

            if (!tool) {
                tool = (await refreshListing(options.signal)).find((candidate) => candidate.name === flatName);
            }

            if (!tool) {
                throw new BytechefAppError('APP_TOOL_NOT_FOUND', `No tool named '${flatName}' is allowed for this app`);
            }

            return execute<T>(tool.bindingKey, tool.toolName, input, options);
        },

        async list(options: CallOptions = {}): Promise<AppTool[]> {
            return [...(await fetchListing(options.signal))];
        },
    };
}
```

- [ ] **Step 6: Run the tests to verify they pass**

```bash
npm run test -w library -- src/core/tools.test.ts src/core/workflows.test.ts > "$SCRATCH/t3-green.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |×|Unhandled" "$SCRATCH/t3-green.log"
```

Expected: `exit=0`, no `×`, no `Unhandled` line. The earlier `executeByName` tests (`maps the flat name ... and caches
it`, `re-fetches once on a miss`, `throws APP_TOOL_NOT_FOUND after exactly one re-fetch`, `refreshes the listing after
the TTL`, the already-aborted signal table and `forwards the signal ... when the cached listing answers`) still pass.

- [ ] **Step 7: Prove the guards fail when they should**

(a) In `tools.ts`, temporarily change `sharedListing ??= fetchListing().finally(` to
`sharedListing ??= fetchListing(signal).finally(` (the naive dedupe that runs the shared request on the first
caller's signal). (b) Run:

```bash
npm run test -w library -- src/core/tools.test.ts > "$SCRATCH/t3-mutant-a.log" 2>&1
echo "exit=$?"
grep -E "×" "$SCRATCH/t3-mutant-a.log"
```

Expected: `exit=1`; `lets one caller abort without aborting the listing request` fails. Restore the line exactly as in
Step 5.

(c) In `workflows.ts`, temporarily delete the line `runId: run.runId,` inside the `error instanceof BytechefAppError`
branch, then:

```bash
npm run test -w library -- src/core/workflows.test.ts > "$SCRATCH/t3-mutant-b.log" 2>&1
echo "exit=$?"
grep -E "×" "$SCRATCH/t3-mutant-b.log"
```

Expected: `exit=1`; the new `TIMEOUT` test fails (with the existing 404 and network-failure tests). Restore with
`git checkout -- library/src/core/workflows.ts` (Task 1 committed it; Task 3 does not modify it) and rerun Step 6
(expected `exit=0`).

- [ ] **Step 8: Document the sharing**

In `WS/library/README.md`, after the paragraph that starts `Every call also accepts \`{signal}\` to cancel it.`,
insert this paragraph (blank line before and after):

```
`tools.executeByName` maps the flat name through the tool listing, cached for 60 seconds (`TOOL_LISTING_TTL_MS`) and
fetched once more on a miss. Concurrent calls share one listing request: a call's `signal` stops only that call's
wait, the shared request is bounded by `timeoutMs`, and a failed listing request is not cached.
```

Then:

```bash
npx prettier --write library/README.md library/src/core > "$SCRATCH/t3-fmt.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 9: Full checks**

```bash
npm run check:all > "$SCRATCH/t3-check.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |error|FAIL|Unhandled" "$SCRATCH/t3-check.log" | head -40
npm run format > "$SCRATCH/t3-format.log" 2>&1
echo "exit=$?"
```

Expected: both `exit=0`, no `Unhandled`.

- [ ] **Step 10: Commit**

```bash
git branch --show-current
git add library/src/core/request.ts library/src/core/tools.ts library/src/core/tools.test.ts library/src/core/workflows.test.ts library/README.md
git commit -m "sdk - Share one tool listing request between concurrent executeByName calls"
```

---

### Task 4: Library housekeeping — typecheck and lint `vite.config.ts`, drop `.npmignore`, fix `.prettierignore`

**Files:**
- Modify: `WS/library/package.json` (`scripts.lint`, `scripts.typecheck`)
- Modify: `WS/library/vite.config.ts` (whole file)
- Modify: `WS/library/tsconfig.node.json` (whole file)
- Delete: `WS/library/.npmignore`
- Modify: `WS/.prettierignore` (whole file)
- Modify: `WS/DEVELOPMENT.md` (library scripts table, `typecheck` and `lint` rows)

**Interfaces:** none consumed or produced; later tasks rely only on `npm run check:all` covering `vite.config.ts`.

- [ ] **Step 1: Wire the config into lint and typecheck (the failing check)**

In `WS/library/package.json` set:

```json
        "typecheck": "tsc --noEmit -p tsconfig.json && tsc -p tsconfig.node.json",
        "lint": "eslint 'src/**/*.ts' vite.config.ts",
```

(`lint:fix` becomes `"eslint 'src/**/*.ts' vite.config.ts --fix"`.) Replace `WS/library/tsconfig.node.json` with:

```json
{
    "compilerOptions": {
        "target": "ES2022",
        "module": "ESNext",
        "moduleResolution": "bundler",
        "strict": true,
        "skipLibCheck": true,
        "noEmit": true,
        "types": ["node"]
    },
    "include": ["vite.config.ts"]
}
```

- [ ] **Step 2: Run lint and typecheck to verify they fail**

```bash
npm run lint -w library > "$SCRATCH/t4-lint-red.log" 2>&1
echo "exit=$?"
grep -E "error" "$SCRATCH/t4-lint-red.log"
npm run typecheck -w library > "$SCRATCH/t4-tc-red.log" 2>&1
echo "exit=$?"
grep -E "error TS" "$SCRATCH/t4-tc-red.log"
```

Expected: lint `exit=1` with four `sort-keys` errors in `vite.config.ts` (`'build' should be before 'plugins'`,
`'ai-sdk' should be before 'tanstack-start'`, `'coverage' should be before 'include'`, `'include' should be before
'provider'`); typecheck `exit=2` with `vite.config.ts(30,5): error TS2769 ... 'test' does not exist in type
'UserConfigExport'`.

- [ ] **Step 3: Rewrite `vite.config.ts`**

Replace the whole of `WS/library/vite.config.ts` with:

```ts
import {resolve} from 'node:path';
import dts from 'vite-plugin-dts';
import {defineConfig} from 'vitest/config';

export default defineConfig({
    build: {
        lib: {
            entry: {
                'ai-sdk': resolve(import.meta.dirname, 'src/ai-sdk.ts'),
                index: resolve(import.meta.dirname, 'src/index.ts'),
                'server-fns': resolve(import.meta.dirname, 'src/server-fns.ts'),
                'tanstack-start': resolve(import.meta.dirname, 'src/tanstack-start.ts'),
            },
            formats: ['es'],
        },
        minify: false,
        rollupOptions: {
            external: ['@tanstack/react-start', 'ai', /^node:/],
        },
        sourcemap: true,
        target: 'es2022',
    },
    plugins: [
        dts({
            exclude: ['src/**/*.test.ts', 'src/test'],
            tsconfigPath: 'tsconfig.json',
        }),
    ],
    test: {
        coverage: {
            include: ['src/core/**', 'src/tanstack/**', 'src/ai/**'],
            provider: 'v8',
        },
        environment: 'node',
        include: ['src/**/*.test.ts'],
    },
});
```

- [ ] **Step 4: Run lint and typecheck to verify they pass, and that typecheck still catches config errors**

```bash
rm -f node_modules/.tmp/tsconfig.node.tsbuildinfo
npm run lint -w library > "$SCRATCH/t4-lint.log" 2>&1
echo "exit=$?"
npm run typecheck -w library > "$SCRATCH/t4-tc.log" 2>&1
echo "exit=$?"
test ! -e node_modules/.tmp/tsconfig.node.tsbuildinfo
echo "no-buildinfo=$?"
```

Expected: `exit=0`, `exit=0`, `no-buildinfo=0` (the type check writes nothing). Then temporarily change one
`import.meta.dirname` in `vite.config.ts` to `import.meta.dirnameX`, rerun the typecheck command (expected `exit=2`,
`Property 'dirnameX' does not exist`), and restore the line.

- [ ] **Step 5: Build, and record the tarball's file list with `.npmignore` present**

```bash
npm run build -w library > "$SCRATCH/t4-build.log" 2>&1
echo "exit=$?"
(cd library && npm pack --dry-run --json 2>/dev/null) > "$SCRATCH/t4-pack-before.json"
echo "exit=$?"
node -e 'for (const file of JSON.parse(require("fs").readFileSync(process.argv[1], "utf8"))[0].files) console.log(file.path)' "$SCRATCH/t4-pack-before.json" | sort > "$SCRATCH/t4-pack-before.txt"
echo "exit=$?"
wc -l "$SCRATCH/t4-pack-before.txt"
```

Expected: every `exit=0`; the list holds `README.md`, `package.json` and `dist/` paths only (33 once Task 1's
`dist/core/response.d.ts` exists; chunk hashes vary with the code, so the comparison is between this run and the next
on the same `dist`).

- [ ] **Step 6: Delete `.npmignore` and compare the list without rebuilding**

```bash
git rm library/.npmignore > "$SCRATCH/t4-rm.log" 2>&1
echo "exit=$?"
(cd library && npm pack --dry-run --json 2>/dev/null) > "$SCRATCH/t4-pack-after.json"
echo "exit=$?"
node -e 'for (const file of JSON.parse(require("fs").readFileSync(process.argv[1], "utf8"))[0].files) console.log(file.path)' "$SCRATCH/t4-pack-after.json" | sort > "$SCRATCH/t4-pack-after.txt"
echo "exit=$?"
diff "$SCRATCH/t4-pack-before.txt" "$SCRATCH/t4-pack-after.txt" > "$SCRATCH/t4-pack-diff.log" 2>&1
echo "exit=$?"
```

Expected: the `diff` prints `exit=0` (identical lists). If it differs, stop: `files` does not govern the tarball the
way the design assumes; restore the file with `git checkout HEAD -- library/.npmignore` and report.

- [ ] **Step 7: Fix the stale `.prettierignore` comment**

Replace the whole of `WS/.prettierignore` with:

```
# Build output
coverage
dist
# Generated: the OpenAPI types and the TanStack Router route trees
**/src/generated
**/routeTree.gen.ts
# The app template is not formatted
create/template/**
```

(Task 5 replaces the last two lines.)

- [ ] **Step 8: Update DEVELOPMENT.md**

In `WS/DEVELOPMENT.md`'s library scripts table, set the `typecheck` row's description to
`Type-check the sources and vite.config.ts without emitting` and the `lint` row's to
`Lint the sources and vite.config.ts`. Then:

```bash
npx prettier --write DEVELOPMENT.md library/vite.config.ts library/tsconfig.node.json library/package.json > "$SCRATCH/t4-fmt.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 9: Full checks**

```bash
npm run check:all > "$SCRATCH/t4-check.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |error|FAIL" "$SCRATCH/t4-check.log" | head -40
npm run format > "$SCRATCH/t4-format.log" 2>&1
echo "exit=$?"
npm run check:template > "$SCRATCH/t4-template.log" 2>&1
echo "exit=$?"
grep -E "^== |Assertion|exit " "$SCRATCH/t4-template.log" | tail -5
```

Expected: all `exit=0`; `check:template` ends with `== [..s] check:template passed` (it builds and packs the library
with the new Vite config).

- [ ] **Step 10: Commit**

```bash
git branch --show-current
git add library/package.json library/vite.config.ts library/tsconfig.node.json .prettierignore DEVELOPMENT.md
git status --short library/.npmignore
git commit -m "sdk - Typecheck and lint the library's Vite config and drop its .npmignore"
```

(`git rm` in Step 6 already staged the deletion; `git status --short` must print `D  library/.npmignore`.)

---

### Task 5: Prettier checks the template files that are ours

**Files:**
- Modify: `WS/.prettierignore` (last two lines)
- Modify: `WS/package.json` (`scripts.format:fix`)
- Modify: `WS/scripts/check-template.mjs:9-13` (a constant) and `:296-298` (a first step)
- Modify (one-time reformat): `WS/create/template/vite.config.ts`, `WS/create/template/src/router.tsx`,
  `WS/create/template/src/styles.css`
- Modify: `WS/DEVELOPMENT.md` (Checks section, `check:template` bullet), `.agents/apps.md` ("Proof in CI" bullet)

**Interfaces:** Produces the formatting step that Task 6's edits to `create/template/scripts/bytechef-status.mjs`
and `scripts/check-template.mjs` must satisfy (both are in prettier's scope).

- [ ] **Step 1: Bring the template's own files into prettier's scope (the failing check)**

In `WS/.prettierignore`, replace the last two lines (`# The app template is not formatted` and
`create/template/**`) with:

```
# Template files kept as their generators wrote them (TanStack's starter, shadcn and npm); the rest of the template is ours
create/template/package-lock.json
create/template/src/components/ui
create/template/*.json
create/template/*.md
```

- [ ] **Step 2: Run the format check to verify it fails on exactly the three starter files**

```bash
npm run format > "$SCRATCH/t5-red.log" 2>&1
echo "exit=$?"
grep -E "warn" "$SCRATCH/t5-red.log"
```

Expected: `exit=1` and exactly these three `[warn]` file lines, plus the summary line:
`create/template/src/router.tsx`, `create/template/src/styles.css`, `create/template/vite.config.ts`. Any other
template file listed means the ignore patterns are wrong; fix them before continuing.

- [ ] **Step 3: Check that prettier's file set is exactly the design's list**

```bash
for file in $(git ls-files create/template) create/template/src/routeTree.gen.ts; do printf '%s ' "$file"; npx prettier --file-info "$file" | tr -d '\n '; echo; done > "$SCRATCH/t5-fileinfo.log" 2>&1
echo "exit=$?"
grep '"ignored":false' "$SCRATCH/t5-fileinfo.log" | grep -v '"inferredParser":null'
```

Expected: exactly 11 lines, for `scripts/bytechef-status.mjs`, `scripts/check-bundle.mjs`,
`src/components/overview.tsx`, `src/router.tsx`, `src/routes/__root.tsx`, `src/routes/index.tsx`,
`src/server/bytechef.ts`, `src/server/dev.ts`, `src/server/overview.ts`, `src/styles.css`, `vite.config.ts` (all under
`create/template/`).

- [ ] **Step 4: Add the formatting step to `check:template`**

In `WS/scripts/check-template.mjs`, after `const MOCK = join(W, 'scripts/mock-bytechef.mjs');` add:

```js
const PRETTIER = join(W, 'node_modules/prettier/bin/prettier.cjs');
```

and replace

```js
try {
    step('pack');
```

with

```js
try {
    step('template formatting');
    run(process.execPath, [PRETTIER, '--check', 'create/template'], {cwd: W, label: 'prettier --check create/template'});

    step('pack');
```

(Run from `W`, prettier reads the workspace `.prettierrc` and `.prettierignore`, so this step and `npm run format`
check the same files; it runs first so a formatting slip fails in seconds.)

- [ ] **Step 5: Make `format:fix` cover every file `format` checks**

In `WS/package.json` set `"format:fix": "prettier --write ."` (the old glob `**/*.{js,json,jsx,ts,tsx,md}` skipped
`.mjs` and `.css`, which `format` now checks under `create/template`).

- [ ] **Step 6: Prove the harness step fails on an unformatted template file**

```bash
npm run check:template > "$SCRATCH/t5-template-red.log" 2>&1
echo "exit=$?"
grep -E "^== |prettier --check|\[warn\]" "$SCRATCH/t5-template-red.log" | head -10
```

Expected: `exit=1` within seconds, the only step printed is `== [0s] template formatting`, and the failure names the
three starter files.

- [ ] **Step 7: Reformat the three starter files once**

```bash
npx prettier --write create/template/vite.config.ts create/template/src/router.tsx create/template/src/styles.css > "$SCRATCH/t5-write.log" 2>&1
echo "exit=$?"
git diff --stat -- create/template > "$SCRATCH/t5-diffstat.log" 2>&1
echo "exit=$?"
cat "$SCRATCH/t5-diffstat.log"
```

Expected: only those three files changed; `vite.config.ts` now reads:

```ts
import {defineConfig} from 'vite';
import {devtools} from '@tanstack/devtools-vite';

import {tanstackStart} from '@tanstack/react-start/plugin/vite';

import viteReact from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import {nitro} from 'nitro/vite';

const config = defineConfig({
    resolve: {tsconfigPaths: true},
    plugins: [devtools(), nitro(), tailwindcss(), tanstackStart(), viteReact()],
});

export default config;
```

`router.tsx` and `styles.css` change only in indentation, semicolons and quotes.

- [ ] **Step 8: Prove the harness step catches a slip in one of our files**

Append an unformatted line: `printf '\nexport const formattingProbe   =   1\n' >> create/template/src/server/overview.ts`, then:

```bash
npm run check:template > "$SCRATCH/t5-probe.log" 2>&1
echo "exit=$?"
grep -E "overview.ts" "$SCRATCH/t5-probe.log"
git checkout -- create/template/src/server/overview.ts
git diff --quiet -- create/template/src/server/overview.ts
echo "restored=$?"
```

Expected: `exit=1`, a line naming `create/template/src/server/overview.ts`, and `restored=0`.

- [ ] **Step 9: Docs**

In `WS/DEVELOPMENT.md`, in the `npm run check:template` bullet under **Checks**, change
`builds the library, packs both packages,` to
`first checks the formatting of the template's own files (generated ones are listed in the workspace .prettierignore), then builds the library, packs both packages,`.
In `.agents/apps.md` ("Proof in CI" bullet), change `in the \`sdk-app\` job: it packs both packages,` to
``in the `sdk-app` job: it checks the formatting of the template's own files (the workspace `.prettierignore` lists the generated and starter-only ones; `npm run format` checks the same set), packs both packages,``.
Then:

```bash
npx prettier --write DEVELOPMENT.md scripts/check-template.mjs package.json > "$SCRATCH/t5-fmt.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 10: Full checks, on Node 24 and Node 22.18**

```bash
npm run format > "$SCRATCH/t5-format.log" 2>&1
echo "exit=$?"
npm run check:all > "$SCRATCH/t5-check.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |FAIL" "$SCRATCH/t5-check.log"
npm run check:template > "$SCRATCH/t5-template.log" 2>&1
echo "exit=$?"
grep -E "^== " "$SCRATCH/t5-template.log" | tail -3
PATH="$HOME/.nvm/versions/node/v22.18.0/bin:$PATH" npm run check:template > "$SCRATCH/t5-template-22.log" 2>&1
echo "exit=$?"
grep -E "^== " "$SCRATCH/t5-template-22.log" | tail -3
```

Expected: all `exit=0`; both template runs end with `check:template passed` (the reformatted `vite.config.ts` and
`router.tsx` still build, and the dev server still renders the overview).

- [ ] **Step 11: Commit**

```bash
git branch --show-current
git add .prettierignore package.json scripts/check-template.mjs create/template/vite.config.ts create/template/src/router.tsx create/template/src/styles.css DEVELOPMENT.md ../../../../.agents/apps.md
git commit -m "sdk - Check the formatting of the template's own files"
```

---

### Task 6: `bytechef:status` loads `.env.local` itself; correct the devtools port note

**Files:**
- Modify: `WS/create/template/scripts/bytechef-status.mjs:1` (imports) and before `try {` (line 42)
- Modify: `WS/create/template/package.json:10` (`bytechef:status`)
- Modify: `WS/scripts/check-template.mjs:2` (import) and after the `status without a key` step (line 512)
- Modify: `.agents/apps.md:410-418` (status contract) and `:468` (devtools)
- Modify: `docs/superpowers/specs/2026-10-07-apps-template-design.md:175`, `:229`, `:290`

**Interfaces:**
- Consumes: Task 5's formatting step (the edited `.mjs` files must be prettier-clean).
- Produces: the stderr line `No .env.local here; BYTECHEF_URL and BYTECHEF_APP_KEY come from the environment only.`
  (exact text, asserted by the harness).

- [ ] **Step 1: Write the failing harness steps**

In `WS/scripts/check-template.mjs`, add `renameSync` to the `node:fs` import, keeping it sorted:

```js
import {
    existsSync,
    mkdirSync,
    mkdtempSync,
    readdirSync,
    readFileSync,
    renameSync,
    rmSync,
    statSync,
    writeFileSync,
} from 'node:fs';
```

add after `const PRETTIER = ...`:

```js
const NO_ENV_FILE_MESSAGE =
    'No .env.local here; BYTECHEF_URL and BYTECHEF_APP_KEY come from the environment only.\n';
```

and insert, directly after the `status without a key` step's closing `);` of its `assert(...)` (before
`step('status against a server without /actions');`):

```js

    step('status reads .env.local when run without npm');
    run('node', ['scripts/bytechef-status.mjs'], {
        cwd: appDir,
        env: {BYTECHEF_URL: mock.url},
        label: 'status run directly, key from .env.local',
    });

    step('status without .env.local');
    renameSync(join(appDir, '.env.local'), join(appDir, '.env.local.moved'));

    try {
        const withoutFile = run('node', ['scripts/bytechef-status.mjs'], {
            cwd: appDir,
            env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url},
            label: 'status without .env.local',
        });

        assert(JSON.parse(withoutFile.stdout).bytechefUrl === mock.url, 'the environment alone configures the status');
        assert(
            withoutFile.stderr === NO_ENV_FILE_MESSAGE,
            `one stderr line saying .env.local is absent, and no Node notice: ${withoutFile.stderr}`
        );
    } finally {
        renameSync(join(appDir, '.env.local.moved'), join(appDir, '.env.local'));
    }
```

- [ ] **Step 2: Run the harness to verify it fails**

```bash
npx prettier --write scripts/check-template.mjs > "$SCRATCH/t6-fmt0.log" 2>&1
echo "exit=$?"
npm run check:template > "$SCRATCH/t6-red.log" 2>&1
echo "exit=$?"
grep -E "^== |status run directly|BYTECHEF_APP_KEY is not set" "$SCRATCH/t6-red.log" | tail -6
```

Expected: `exit=1`, the last step printed is `status reads .env.local when run without npm`, and the failure is
`status run directly, key from .env.local: exit 1` with the SDK's `BYTECHEF_APP_KEY is not set` message (plain
`node` does not load `.env.local` today). The `status without .env.local` step runs `node` directly too, not
`npm run -s`, so its stderr assertion cannot be disturbed by npm's own warnings about inherited `npm_config_*`
variables; the `status` step keeps exercising the `bytechef:status` npm script.

- [ ] **Step 3: Load `.env.local` in the status script**

In `WS/create/template/scripts/bytechef-status.mjs`, replace line 1 with:

```js
import {existsSync, readFileSync} from 'node:fs';
import {parseEnv} from 'node:util';

import {BytechefAppError, createBytechefClient} from '@bytechef/app';

const ENV_FILE = '.env.local';
```

add this function after `describeError` (before `listActions`):

```js
function loadEnvFile() {
    if (!existsSync(ENV_FILE)) {
        process.stderr.write(
            `No ${ENV_FILE} here; BYTECHEF_URL and BYTECHEF_APP_KEY come from the environment only.\n`
        );

        return;
    }

    for (const [name, value] of Object.entries(parseEnv(readFileSync(ENV_FILE, 'utf8')))) {
        if (process.env[name] === undefined) {
            process.env[name] = value;
        }
    }
}
```

and insert, directly before the top-level `try {`:

```js
loadEnvFile();

```

In `WS/create/template/package.json` set `"bytechef:status": "node scripts/bytechef-status.mjs",`.

- [ ] **Step 4: Run the harness on Node 24 and Node 22.18 to verify it passes**

```bash
npx prettier --write create/template/scripts/bytechef-status.mjs > "$SCRATCH/t6-fmt1.log" 2>&1
echo "exit=$?"
npm run check:template > "$SCRATCH/t6-green.log" 2>&1
echo "exit=$?"
grep -E "^== " "$SCRATCH/t6-green.log" | tail -12
PATH="$HOME/.nvm/versions/node/v22.18.0/bin:$PATH" npm run check:template > "$SCRATCH/t6-green-22.log" 2>&1
echo "exit=$?"
grep -E "^== " "$SCRATCH/t6-green-22.log" | tail -3
```

Expected: both `exit=0` and `check:template passed`. The existing `status` step still proves the shell
`BYTECHEF_URL` wins over `.env.local` and that the key comes from `.env.local`; `status without a key` still proves an
explicitly empty `BYTECHEF_APP_KEY` wins over the file (one stderr line, empty stdout); `status when /actions fails`
still sees exactly one stderr line (the file is present, so no notice is printed).

- [ ] **Step 5: Commit the code**

```bash
npm run format > "$SCRATCH/t6-format.log" 2>&1
echo "exit=$?"
git branch --show-current
git add create/template/scripts/bytechef-status.mjs create/template/package.json scripts/check-template.mjs
git commit -m "sdk - Load .env.local in the status script itself"
```

- [ ] **Step 6: Update the deep-dive and the spec**

In `.agents/apps.md`:

- Replace `- **\`bytechef:status\` contract** (\`node --env-file-if-exists=.env.local scripts/bytechef-status.mjs\`): JSON on stdout,`
  with
  ``- **`bytechef:status` contract** (`node scripts/bytechef-status.mjs`; the script loads `.env.local` itself with `util.parseEnv`, as the bundle check reads env files, and when the file is absent writes one stderr line instead of Node's `--env-file-if-exists` notice): JSON on stdout,``
- Replace `The shell environment wins over\n  \`.env.local\`.` (the end of that bullet) with
  ``A variable already in the environment, even an empty one, wins over `.env.local`, as it did under Node's `--env-file`.``
- Replace the sentence `The devtools plugin binds port 42069 in development, so two dev servers collide.` with
  ``The devtools plugin's event bus listens on `localhost:4206`, only in development, and falls back to an OS-assigned port when 4206 is taken, so two dev servers run side by side (each opens one more local port).``

In `docs/superpowers/specs/2026-10-07-apps-template-design.md`:

- line 175: replace `(\`node --env-file-if-exists=.env.local scripts/bytechef-status.mjs\`)` with
  ``(`node scripts/bytechef-status.mjs`, which loads `.env.local` itself with `util.parseEnv`)``;
- line 229: replace `(Node's \`--env-file-if-exists\` and Vite's env loading both behave so)` with
  ``(the status script loads `.env.local` without replacing a variable already set, and Vite's env loading behaves the same)``;
- line 290: replace `` `node --env-file-if-exists=.env.local scripts/bytechef-status.mjs` `` with
  `` `npm run -s bytechef:status` and `node scripts/bytechef-status.mjs` ``.

Keep the surrounding line wrapping (about 120 columns); these files are outside the workspace's prettier scope, so
do not run prettier on them. Then:

```bash
grep -c "42069\|env-file-if-exists" ../../../../.agents/apps.md ../../../../docs/superpowers/specs/2026-10-07-apps-template-design.md create/template/package.json > "$SCRATCH/t6-grep.log" 2>&1
cat "$SCRATCH/t6-grep.log"
```

Expected: each file's count is `0`.

- [ ] **Step 7: Commit the docs**

```bash
git branch --show-current
git add ../../../../.agents/apps.md ../../../../docs/superpowers/specs/2026-10-07-apps-template-design.md
git commit -m "docs - Correct the status loading and devtools port notes for the app template"
```

---

### Task 7: The scaffolder — scoped targets, URL and line-break validation, a precise npm hint

**Files:**
- Modify: `WS/create/src/args.mjs` (whole file)
- Modify: `WS/create/src/env.mjs` (whole file)
- Modify: `WS/create/bin/create-bytechef-app.mjs:3`, `:7`, `:36-50`, `:77-84`
- Test: `WS/create/test/args.test.mjs`, `WS/create/test/env.test.mjs`, `WS/create/test/bin.test.mjs`
- Modify: `WS/create/README.md` (flags table, "What gets written"), `.agents/apps.md` ("The scaffolder" bullet)

**Interfaces:**
- Produces (in `create/src/env.mjs`):
  - `validatePackageName(name: string): string` — now also accepts `@scope/name`.
  - `resolveTarget(directory: string, cwd?: string): {directory: string, packageName: string, targetDir: string}`.
  - `buildEnvLocal({env, url})` — same return shape; throws on a line break in the URL or key.
- `parseCliArgs(argv, env)` keeps its return shape (`{directory, help, install, packageManager, url}`).

- [ ] **Step 1: Write the failing unit tests**

In `WS/create/test/args.test.mjs`, append inside `describe('parseCliArgs', ...)` (before its closing `});`):

```js
    it('does not blame npm when the flag reached the scaffolder or npm left the variable empty', () => {
        expect(parseCliArgs(['my-app', '--url', 'https://bc.example'], {npm_config_url: 'true'}).url).toBe(
            'https://bc.example'
        );
        expect(parseCliArgs(['my-app', '--no-install'], {npm_config_install: ''}).install).toBe(false);
        expect(parseCliArgs(['my-app', '--install'], {npm_config_install: ''}).install).toBe(true);
        expect(parseCliArgs(['my-app'], {npm_config_url: ''}).url).toBeUndefined();
    });

    it('accepts an http or https --url and refuses anything else', () => {
        expect(parseCliArgs(['--url', 'http://localhost:9555'], {}).url).toBe('http://localhost:9555');
        expect(() => parseCliArgs(['--url', 'bc.example'], {})).toThrowError(/--url must be an http or https URL/);
        expect(() => parseCliArgs(['--url', 'ftp://bc.example'], {})).toThrowError(
            /--url must be an http or https URL/
        );
    });

    it('refuses a line break in --url, even one the URL parser would drop', () => {
        expect(() => parseCliArgs(['--url', 'https://bc.example\nX=1'], {})).toThrowError(
            '--url must not contain a line break'
        );
        expect(() => parseCliArgs(['--url', 'https://bc.example\r'], {})).toThrowError(
            '--url must not contain a line break'
        );
    });
```

In `WS/create/test/env.test.mjs`, change the import to
`import {buildEnvLocal, detectPackageManager, nodeVersionWarning, resolveTarget, validatePackageName} from '../src/env.mjs';`
and append:

```js
describe('buildEnvLocal line breaks', () => {
    it.each(['sk-1\nINJECT=1', 'sk-1\rINJECT=1', 'sk-1\r\n'])('refuses the key %j without echoing it', (key) => {
        expect(() => buildEnvLocal({env: {BYTECHEF_APP_KEY: key}, url: 'https://bc.example'})).toThrowError(
            'BYTECHEF_APP_KEY must not contain a line break'
        );
    });

    it('refuses a line break in the URL from the flag or the environment', () => {
        expect(() => buildEnvLocal({env: {}, url: 'https://bc.example\rX=1'})).toThrowError(
            'The ByteChef URL must not contain a line break'
        );
        expect(() => buildEnvLocal({env: {BYTECHEF_URL: 'https://bc.example\nX=1'}})).toThrowError(
            'The ByteChef URL must not contain a line break'
        );
    });
});

describe('scoped package names', () => {
    it('accepts @scope/name and checks both parts', () => {
        expect(validatePackageName('@acme/my-app')).toBe('@acme/my-app');
        expect(() => validatePackageName('@Acme/my-app')).toThrowError(/lower-case/);
        expect(() => validatePackageName('@acme/')).toThrowError(/start/);
        expect(() => validatePackageName('@/my-app')).toThrowError(/start/);
    });

    it('refuses a name longer than 214 characters, scope included', () => {
        expect(() => validatePackageName(`@acme/${'a'.repeat(209)}`)).toThrowError(/214/);
    });
});

describe('resolveTarget', () => {
    it('creates name/ for @scope/name and keeps the scope in the package name', () => {
        expect(resolveTarget('@acme/my-app', '/work')).toEqual({
            directory: 'my-app',
            packageName: '@acme/my-app',
            targetDir: '/work/my-app',
        });
    });

    it('takes the package name from the directory otherwise', () => {
        expect(resolveTarget('my-app', '/work')).toEqual({
            directory: 'my-app',
            packageName: 'my-app',
            targetDir: '/work/my-app',
        });
        expect(resolveTarget('.', '/work/dot')).toEqual({directory: '.', packageName: 'dot', targetDir: '/work/dot'});
        expect(resolveTarget('apps/@acme/web', '/work')).toEqual({
            directory: 'apps/@acme/web',
            packageName: 'web',
            targetDir: '/work/apps/@acme/web',
        });
    });
});
```

- [ ] **Step 2: Write the failing bin tests**

In `WS/create/test/bin.test.mjs`, append inside `describe('create-bytechef-app bin', ...)` (before its closing
`});`):

```js
    it('creates name/ for @scope/name with the scoped package name', () => {
        const result = run(['@acme/my-app', '--no-install']);

        expect(result.status).toBe(0);
        expect(existsSync(join(result.cwd, '@acme'))).toBe(false);
        expect(JSON.parse(readFileSync(join(result.cwd, 'my-app', 'package.json'), 'utf8')).name).toBe('@acme/my-app');
        expect(result.stdout).toContain('cd my-app\n');
    });

    it('refuses a key with a line break before writing anything and never prints the key', () => {
        const result = run(['app', '--no-install'], {BYTECHEF_APP_KEY: 'sk-secret\rINJECT=1'});

        expect(result.status).toBe(1);
        expect(result.stderr).toContain('BYTECHEF_APP_KEY must not contain a line break');
        expect(result.stderr).not.toContain('sk-secret');
        expect(result.stdout).not.toContain('sk-secret');
        expect(existsSync(join(result.cwd, 'app'))).toBe(false);
    });

    it('exits 2 on a --url that is not http or https', () => {
        const result = run(['app', '--url', 'ftp://bc.example', '--no-install']);

        expect(result.status).toBe(2);
        expect(result.stderr).toContain('--url must be an http or https URL');
        expect(existsSync(join(result.cwd, 'app'))).toBe(false);
    });
```

- [ ] **Step 3: Run the tests to verify they fail**

```bash
npm run test -w create -- test/args.test.mjs test/env.test.mjs test/bin.test.mjs > "$SCRATCH/t7-red.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |×" "$SCRATCH/t7-red.log" | head -30
```

Expected: `exit=1`. Failing: `does not blame npm ...` (throws `npm took --url`), the two `--url` validation tests,
every `buildEnvLocal line breaks` case, `scoped package names` (`@` fails the start rule), `resolveTarget` (not a
function), and the three new bin tests (a literal `@acme/my-app` directory and name `my-app`; the key with `\r` is
written; `ftp://` is accepted). The existing tests pass.

- [ ] **Step 4: Rewrite `args.mjs`**

Replace the whole of `WS/create/src/args.mjs` with:

```js
import {parseArgs} from 'node:util';

const PACKAGE_MANAGERS = ['npm', 'pnpm', 'yarn', 'bun'];
const NPM_HINT =
    'Under npm, flags must follow "--": npm create bytechef-app@latest my-app -- --url https://… --no-install';
const LINE_BREAK = /[\r\n]/;

export const USAGE = `Usage:
  npm create bytechef-app@latest [directory] -- [flags]
  pnpm create bytechef-app [directory] [flags]
  npx create-bytechef-app [directory] [flags]

Under npm create, flags must follow "--"; npm takes any flag before it as its own option.

Arguments and flags:
  [directory]                    Directory to create (default bytechef-app); its name becomes the package name,
                                 and @scope/name creates name/ with the package name @scope/name
  --url <url>                    ByteChef base URL (http or https) written to .env.local (falls back to BYTECHEF_URL)
  --no-install                   Skip installing dependencies
  --package-manager <manager>    One of ${PACKAGE_MANAGERS.join(', ')}; detected from the invoking package manager
  -h, --help                     Show this help

There is no flag for the app key: a key on the command line ends up in shell history and process listings.
Set BYTECHEF_APP_KEY in the environment before scaffolding, or in .env.local afterwards.`;

export function statusCommand(packageManager) {
    return packageManager === 'npm' ? 'npm run -s bytechef:status' : `${packageManager} run bytechef:status`;
}

function validateUrl(url) {
    if (LINE_BREAK.test(url)) {
        throw new Error('--url must not contain a line break');
    }

    let parsed;

    try {
        parsed = new URL(url);
    } catch {
        throw new Error(`--url must be an http or https URL, got: ${url}`);
    }

    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
        throw new Error(`--url must be an http or https URL, got: ${url}`);
    }
}

export function parseCliArgs(argv, env = process.env) {
    let parsed;

    try {
        parsed = parseArgs({
            allowNegative: true,
            allowPositionals: true,
            args: argv,
            options: {
                help: {short: 'h', type: 'boolean'},
                install: {type: 'boolean'},
                'package-manager': {type: 'string'},
                url: {type: 'string'},
            },
            strict: true,
        });
    } catch (error) {
        throw new Error(`${error.message}. ${NPM_HINT}`);
    }

    if (parsed.values.help) {
        return {help: true};
    }

    if (env.npm_config_url !== undefined && env.npm_config_url !== '' && parsed.values.url === undefined) {
        throw new Error(`npm took --url as its own option. ${NPM_HINT}`);
    }

    if (env.npm_config_install !== undefined && parsed.values.install === undefined) {
        throw new Error(`npm took --no-install as its own option. ${NPM_HINT}`);
    }

    if (parsed.positionals.length > 1) {
        throw new Error(`Only one directory may be given, got: ${parsed.positionals.join(' ')}. ${NPM_HINT}`);
    }

    const packageManager = parsed.values['package-manager'];

    if (packageManager !== undefined && !PACKAGE_MANAGERS.includes(packageManager)) {
        throw new Error(`--package-manager must be one of ${PACKAGE_MANAGERS.join(', ')}`);
    }

    if (parsed.values.url !== undefined) {
        validateUrl(parsed.values.url);
    }

    return {
        directory: parsed.positionals[0] ?? 'bytechef-app',
        help: false,
        install: parsed.values.install ?? true,
        packageManager,
        url: parsed.values.url,
    };
}
```

- [ ] **Step 5: Rewrite `env.mjs`**

Replace the whole of `WS/create/src/env.mjs` with:

```js
import {basename, resolve} from 'node:path';

const NAME_RULES = [
    [/^[a-z0-9]/, 'must start with a lower-case letter or digit'],
    [/^[a-z0-9][a-z0-9._-]*$/, 'may contain only lower-case letters, digits, ".", "_" and "-"'],
];
const MAXIMUM_NAME_LENGTH = 214;
const SCOPED_NAME = /^@([^/]*)\/([^/]*)$/;
const LINE_BREAK = /[\r\n]/;

export function validatePackageName(name) {
    if (name.length > MAXIMUM_NAME_LENGTH) {
        throw new Error(`"${name}" is not a valid package name: it must be at most ${MAXIMUM_NAME_LENGTH} characters`);
    }

    const scoped = SCOPED_NAME.exec(name);
    const parts = scoped ? [scoped[1], scoped[2]] : [name];

    for (const part of parts) {
        for (const [pattern, rule] of NAME_RULES) {
            if (!pattern.test(part)) {
                throw new Error(`"${name}" is not a valid package name: it ${rule}`);
            }
        }
    }

    return name;
}

export function resolveTarget(directory, cwd = process.cwd()) {
    const scoped = SCOPED_NAME.exec(directory);

    if (scoped) {
        return {directory: scoped[2], packageName: validatePackageName(directory), targetDir: resolve(cwd, scoped[2])};
    }

    const targetDir = resolve(cwd, directory);

    return {directory, packageName: validatePackageName(basename(targetDir)), targetDir};
}

export function detectPackageManager(env = process.env) {
    const agent = env.npm_config_user_agent ?? '';

    for (const candidate of ['pnpm', 'yarn', 'bun']) {
        if (agent.startsWith(`${candidate}/`)) {
            return candidate;
        }
    }

    return 'npm';
}

export function buildEnvLocal({env = process.env, url}) {
    const key = env.BYTECHEF_APP_KEY ?? '';
    const bytechefUrl = url ?? env.BYTECHEF_URL ?? '';

    if (LINE_BREAK.test(bytechefUrl)) {
        throw new Error('The ByteChef URL must not contain a line break');
    }

    if (LINE_BREAK.test(key)) {
        throw new Error('BYTECHEF_APP_KEY must not contain a line break');
    }

    return {
        content: `BYTECHEF_URL=${bytechefUrl}\nBYTECHEF_APP_KEY=${key}\n`,
        keyFromEnvironment: key.length > 0,
    };
}

export const MINIMUM_APP_NODE_VERSION = '22.18.0';

export function nodeVersionWarning(version = process.versions.node) {
    const [major, minor] = version.split('.').map(Number);
    const [minimumMajor, minimumMinor] = MINIMUM_APP_NODE_VERSION.split('.').map(Number);

    if (major > minimumMajor || (major === minimumMajor && minor >= minimumMinor)) {
        return null;
    }

    return `The generated app needs Node.js ${MINIMUM_APP_NODE_VERSION} or newer (24 recommended); this is ${version}. Its development server fails on older Node.js 22 releases.`;
}
```

- [ ] **Step 6: Use `resolveTarget` in the bin**

In `WS/create/bin/create-bytechef-app.mjs`:

- line 3: `import {dirname, resolve} from 'node:path';`
- line 7: `import {buildEnvLocal, detectPackageManager, nodeVersionWarning, resolveTarget} from '../src/env.mjs';`
- replace lines 36-50 (from `const targetDir = resolve(process.cwd(), options.directory);` through
  `console.log(\`Created ${targetDir}\`);`) with:

```js
    const target = resolveTarget(options.directory);
    const packageManager = options.packageManager ?? detectPackageManager();
    const envLocal = buildEnvLocal({url: options.url});

    const {lockfileRemoved} = scaffold({
        appVersion: version,
        envLocal: envLocal.content,
        packageManager,
        packageName: target.packageName,
        targetDir: target.targetDir,
        templateDir: resolve(packageDir, 'template'),
    });

    console.log(`Created ${target.targetDir}`);
```

- in the install block, replace both remaining uses of `targetDir` with `target.targetDir`
  (`runInstall(packageManager, target.targetDir)` and `` `... in ${target.targetDir}` ``);
- in the next-steps template literal, replace `cd ${options.directory}` with `cd ${target.directory}`.

- [ ] **Step 7: Run the tests to verify they pass**

```bash
npm run test -w create > "$SCRATCH/t7-green.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |×" "$SCRATCH/t7-green.log"
```

Expected: `exit=0`; every create test passes (the existing `explains when npm consumed the flags` test still throws
for `{npm_config_url: 'true'}` and `{npm_config_install: ''}`, and `defaults the directory, install and package
manager` still sees `install: true`).

- [ ] **Step 8: Prove the CR-only case is what fails without the `\r` in the pattern**

Temporarily change `const LINE_BREAK = /[\r\n]/;` in `env.mjs` to `const LINE_BREAK = /\n/;`, then:

```bash
npm run test -w create -- test/env.test.mjs test/bin.test.mjs > "$SCRATCH/t7-mutant.log" 2>&1
echo "exit=$?"
grep -E "×" "$SCRATCH/t7-mutant.log"
```

Expected: `exit=1`; the `sk-1\rINJECT=1` key case, the `\rX=1` URL case and the bin's `refuses a key with a line
break` test fail. Restore the line exactly as in Step 5 and rerun Step 7 (expected `exit=0`).

- [ ] **Step 9: Docs**

In `WS/create/README.md`, set the `[directory]` row's meaning to
`Directory to create, default \`bytechef-app\`; its name becomes the package name, and \`@scope/name\` creates \`name/\` named \`@scope/name\``
and the `--url <url>` row's to `ByteChef base URL, http or https, written to \`.env.local\` (falls back to \`BYTECHEF_URL\`)`.
In "What gets written", after the sentence ending `otherwise it is left empty.` add:
`A line break in the URL or the key is refused before anything is written.`
In `.agents/apps.md` ("The scaffolder" bullet), replace
`and it detects \`npm_config_url\`/\`npm_config_install\` in the environment.` with
``and it blames npm for a swallowed flag only when that flag did not reach it: `npm_config_url` non-empty, or `npm_config_install` set at all (npm writes a swallowed `--no-install` as the empty string). `--url` must be http(s); a line break in the URL or the key is refused before anything is written; `@scope/name` creates `name/` with the scoped package name.``
Then:

```bash
npx prettier --write create/README.md create/src create/bin create/test > "$SCRATCH/t7-fmt.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 10: Full checks**

```bash
npm run format > "$SCRATCH/t7-format.log" 2>&1
echo "exit=$?"
npm run check:all > "$SCRATCH/t7-check.log" 2>&1
echo "exit=$?"
grep -E "Test Files|Tests |FAIL" "$SCRATCH/t7-check.log"
npm run check:template > "$SCRATCH/t7-template.log" 2>&1
echo "exit=$?"
grep -E "^== " "$SCRATCH/t7-template.log" | tail -3
```

Expected: all `exit=0`; `check:template passed` (it scaffolds with `--url http://127.0.0.1:9 --no-install` through
`npm exec ... --`, which the new checks accept, and asserts `package name taken from the directory`).

- [ ] **Step 11: Commit**

```bash
git branch --show-current
git add create/src/args.mjs create/src/env.mjs create/bin/create-bytechef-app.mjs create/test/args.test.mjs create/test/env.test.mjs create/test/bin.test.mjs create/README.md ../../../../.agents/apps.md
git commit -m "sdk - Harden the scaffolder's directory, URL and key handling"
```

---

## Self-review

- **Spec coverage.** Item 11: Task 1 (empty/`null`/wrong-shape 2xx → `INVALID_RESPONSE`), Task 2 (invalid base64,
  `bytes()` copies, lazy `text()`/`json()`, `ArrayBuffer`/`ArrayBufferView` bodies, widened `ProxyBody`), Task 3
  (shared in-flight `GET /tools` with per-caller abort, poll `TIMEOUT` test). Item 12: Task 4 (Vite config typechecked
  and linted via `tsconfig.node.json`, `.npmignore` removed with an identical `npm pack` list, `.prettierignore`
  comment). Item 13: Task 5 (`.prettierignore` scope, `check:template` step, `format` script, one-time reformat).
  Item 14: Task 6 Step 6. Item 15: Task 6 Steps 1-5 (precedence ruling 4). Item 16: Task 7 (ruling 5 on the npm hint).
- **Placeholders.** None; every code step carries its code, every command its expected result.
- **Type consistency.** `expectArray`/`expectObject` (Task 1) are used with the same signatures in Tasks 2 and 3;
  `toResult(raw, status)` in Task 2 matches its call; `abortError`/`untilAborted` keep their existing signatures;
  `resolveTarget` returns `{directory, packageName, targetDir}` everywhere it is used.
- **Review Focus.** Pooled `Buffer`/`subarray` and cross-realm bodies (Task 2 Step 1, offset mutation in Step 6),
  JSON `null` (Task 1 Step 1), abandoned shared listing (Task 3 Step 1), bare `\r` (Task 7 Steps 1-2, mutation in
  Step 8).
