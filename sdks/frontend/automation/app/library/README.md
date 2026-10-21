# @bytechef/app

`@bytechef/app` is the server-side SDK for ByteChef Apps. Each instance of an app holds one key that works only against
ByteChef, and an instance can sign its users in through ByteChef. An administrator allow-lists what the app may do: the API collections it can call, the workflows it can start, the
connections it can use and the actions, tools and proxy requests on each connection. Everything else is refused by
ByteChef, whatever the app asks for. The key stays on your server and never reaches the browser.

- **Core client** - API Platform endpoints, workflows, actions, tools, and a proxy for raw API calls, from any
  server-side JavaScript, plus `context` (the instance's settings) and `auth` (the sign-in flow).
- **TanStack Start** - a middleware (`@bytechef/app/tanstack-start`) and ready-made server functions
  (`@bytechef/app/tanstack-start/server-fns`).
- **Vercel AI SDK** - one call turns the app's tools and workflows into tools for `generateText`.

## Install

```bash
npm install @bytechef/app
```

Keep it under `dependencies`, not `devDependencies`: TanStack Start compiles server functions from the app's
dependencies when it builds. For the AI SDK adapter, also install `ai`, a model provider package (the example below uses `@ai-sdk/anthropic`) and `zod`, which `ai`
itself requires (`@bytechef/app` does not use it):

```bash
npm install ai @ai-sdk/anthropic zod
```

`@tanstack/react-start` and `ai` are optional peer dependencies; install the ones you use.

## Environment

| Variable                  | Value                                                                                                                          |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `BYTECHEF_URL`            | The ByteChef server URL, for example `https://app.example.com`.                                                                |
| `BYTECHEF_APP_KEY`        | The app's key.                                                                                                                 |
| `BYTECHEF_SESSION_SECRET` | Seals the sign-in cookies (TanStack Start sign-in only). At least 32 random characters, for example `openssl rand -base64 48`. |

Create the key on the app instance in ByteChef with **Rotate key** (or `bytechef automation app key rotate`), and set
`BYTECHEF_URL` and `BYTECHEF_APP_KEY` in the server environment.

> **Never prefix these variables with `VITE_`** (or any other prefix your bundler exposes to the browser). A prefixed
> variable is compiled into the client bundle and the key would be public. The client refuses to be created when it
> detects a browser (`window` is defined).

The variables are read when the first request is made. Options passed to `createBytechefClient` (`url`, `appKey`,
`timeoutMs`, `contextTtlMs`, `userToken`, `fetch`) take precedence over the environment. The SDK reads no variable for
the app's public URL: pass `appUrl` to the TanStack Start sign-in options (the scaffolded app reads `APP_URL` itself).

## Core client

```ts
import {BytechefAppError, createBytechefClient} from '@bytechef/app';

const bytechef = createBytechefClient();
// or: createBytechefClient({url: 'https://app.example.com', appKey: process.env.MY_KEY, timeoutMs: 30_000});

// Which call to use: an API Platform endpoint by default; a workflow for long-running, approval-waiting or background
// runs; an action or tool for a single operation or an app's own LLM; the proxy only for a raw call no action covers.

// API collections bound to the app. `path` is v{version}/{contextPath}/{endpointPath}. A status the workflow chose
// (a 400 included) comes back as data; an error ByteChef itself answers (an unknown endpoint, an unavailable
// collection, a disabled app, a failed or timed-out workflow) throws.
const collections = await bytechef.api.list();
const reply = await bytechef.api.call<{id: number}>({
    body: {name: 'Ada'},
    method: 'POST',
    path: 'v1/crm/contacts',
    query: {dryRun: 'true'},
});

console.log(reply.status, reply.headers, reply.body);

// Workflows bound to the app.
const workflows = await bytechef.workflows.list();

const controller = new AbortController();

const run = await bytechef.workflows.execute(
    'onboard-customer',
    {email: 'ada@example.com'},
    {signal: controller.signal, wait: true}
);

const sameRun = await bytechef.workflows.getRun(run.runId);

// A background run: answered at once with a running run; poll getRun for the result.
const started = await bytechef.workflows.execute('export-contacts', {listId: '7'}, {waitSeconds: 0});

// Connections bound to the app and what each one allows.
const bindings = await bytechef.connections.list();
const actions = await bytechef.actions.list(); // allowed actions with input JSON Schemas

// Run an allowed action through a binding. `componentVersion` is optional.
const created = await bytechef.actions.execute('crm', 'createContact', {email: 'ada@example.com'});

// Tools, shaped for LLM function calling. `name` is the binding key upper-cased, `__`, then the tool name in UPPER_SNAKE.
const tools = await bytechef.tools.list();
const found = await bytechef.tools.execute('crm', 'searchContacts', {query: 'ada'});
const sameTool = await bytechef.tools.executeByName('CRM__SEARCH_CONTACTS', {query: 'ada'});

// A raw HTTP request through a binding, when the app's proxy allows it.
const response = await bytechef.proxy('crm', {
    body: {name: 'Ada'},
    method: 'POST',
    path: '/v1/contacts',
    query: {dryRun: ['true']},
});

console.log(response.status, response.json());
```

Every call also accepts `{signal}` to cancel it. Results are typed through a generic you supply
(`actions.execute<Contact>(...)`, `tools.execute<T>`, `tools.executeByName<T>`); the SDK does not validate them.

`tools.executeByName` maps the flat name through the tool listing, cached for 60 seconds (`TOOL_LISTING_TTL_MS`) and
fetched once more on a miss. Concurrent calls share one listing request: a call's `signal` stops only that call's
wait, the shared request is bounded by `timeoutMs`, and a failed listing request is not cached.

Every request carries the key as a bearer token. The client times out a request after 70 seconds by default
(`timeoutMs`, which must be positive); the timeout covers reading the response body as well as the headers. Nothing is
retried. A workflow's `waitSeconds` must be shorter than `timeoutMs` - raise `timeoutMs` to hold longer.

### Proxy results

`bytechef.proxy(bindingKey, request)` resolves to a `ProxyResult`:

| Member         | Meaning                                                                        |
| -------------- | ------------------------------------------------------------------------------ |
| `status`       | The upstream HTTP status.                                                      |
| `headers`      | The upstream headers, each name mapped to a list of values.                    |
| `body`         | The body as received: text for `UTF8`, base64 for `BASE64`; `null` when empty. |
| `bodyEncoding` | `'UTF8'` or `'BASE64'`.                                                        |
| `text()`       | The body as a string (a base64 body is decoded as UTF-8 on the first call).    |
| `json<T>()`    | The body parsed as JSON; throws when it is not JSON.                           |
| `bytes()`      | The body as a `Uint8Array`; every call returns a fresh copy.                   |

A proxy request is `{method, path, query?, headers?, body?, contentType?}`. A request `body` is a string, a plain
object or array (sent as JSON), or binary data - an `ArrayBuffer`, any typed
array, a `DataView` or a Node `Buffer` - sent as base64 (only the bytes the view covers). A response body marked
`BASE64` that is not base64 rejects the call with `INVALID_RESPONSE`. The upstream status is data: a `404` from the
upstream API is a `ProxyResult` with `status: 404`, not an exception.

### Workflows

`workflows.execute(key, input, options)` starts a run. The options:

| Option         | Meaning                                                                                                                                                                                                                                                                                                                                                                      |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `wait`         | `false` (default) returns the run as the server first reports it. `true` polls until the run is `COMPLETED`, `FAILED` or `CANCELLED`, and stops at `APPROVAL_REQUIRED`. `'approval'` keeps polling through approvals.                                                                                                                                                        |
| `signal`       | Aborts the wait.                                                                                                                                                                                                                                                                                                                                                             |
| `waitSeconds`  | How long the server holds the first request before answering with a running run. Must be shorter than the client's `timeoutMs`; otherwise `execute` throws a `RangeError` and sends nothing. When omitted and the server's 30-second default would not end at least 5 seconds before `timeoutMs`, `execute` sends `timeoutMs` in whole seconds minus 5 (at least 0) instead. |
| `pollDelaysMs` | Delays between polls; the last one repeats. Defaults to 1, 2, 3, 4, 5, 5, ... seconds; each delay is at least 100 ms.                                                                                                                                                                                                                                                        |

**Waiting has no time limit of its own** - only `signal` stops it, so pass one when the run might take long.

An aborted wait throws `ABORTED` carrying the run's `runId`, and a poll that fails keeps its code, message and status
and gains the `runId`. In both cases the run keeps going on ByteChef; resume watching it with `getRun`:

```ts
try {
    await bytechef.workflows.execute('onboard-customer', input, {signal: AbortSignal.timeout(30_000), wait: true});
} catch (error) {
    if (error instanceof BytechefAppError && error.runId) {
        const run = await bytechef.workflows.getRun(error.runId);
    }
}
```

A run's `status` is `RUNNING`, `APPROVAL_REQUIRED`, `COMPLETED`, `FAILED` or `CANCELLED`. A run's `result` is set when it
completed, `error.message` when it failed, and `approvalFormUrl` (when the server has a public URL) when it waits for a
person.

### Sign-in: `context` and `auth`

Both are on the client and run on the server only.

```ts
const context = await bytechef.context.get();
// context.userAuthMode: 'NONE' | 'OPTIONAL' | 'REQUIRED'; also appId, appName, appInstanceId, appInstanceName,
// environment, configuration, issuer, clientId, authorizationEndpoint, tokenEndpoint

const {codeVerifier, state, url} = await bytechef.auth.createAuthorizationRequest({redirectUri});
// redirect the browser to `url`; keep `state` and `codeVerifier` on the server until the callback

const tokens = await bytechef.auth.exchangeCode({code, codeVerifier, redirectUri});
const refreshed = await bytechef.auth.refresh({refreshToken: tokens.refreshToken ?? ''});

const signedIn = createBytechefClient({userToken: tokens.accessToken});
const user = await signedIn.auth.getCurrentUser(); // GET /me
```

- `context.get()` is `GET /context`. It is kept per client for `contextTtlMs` (default 60 seconds, `DEFAULT_CONTEXT_TTL_MS`;
  `0` fetches on every call after an answer). Concurrent callers share one fetch and a failed fetch is not kept;
  `context.invalidate()` drops the entry. The endpoints and client id come from it: nothing is hardcoded and ByteChef
  serves no OpenID discovery document.
- `auth.createAuthorizationRequest` builds an authorization code request with PKCE S256 and the scope
  `openid profile app:use`. `exchangeCode` and `refresh` post form-encoded to the token endpoint with the `client_id` and
  no `Authorization` header, and never follow a redirect. Refresh tokens rotate: store the returned `refreshToken`, the one
  you sent is spent. The instance key is never sent to the authorization server, and the ID token is neither decoded nor
  returned.
- A client created with `userToken` sends it as `X-ByteChef-User-Token` on every request, next to the key; ByteChef
  checks that the user may use the instance on every call. `getCurrentUser()` returns the user's `id`, `login`, `email`,
  `firstName`, `lastName`, `workspaceRole` and `scopes`, and throws `APP_USER_REQUIRED` without a request when the client
  has no `userToken`.
- `isSignInRequired(error)` is true for `APP_USER_REQUIRED`, `APP_USER_TOKEN_INVALID` and `SESSION_ENDED`, not for
  `APP_USER_FORBIDDEN`. `isSameState(expected, received)` compares the `state` in constant time.
- There is no `revoke`: signing out clears your own session only, and the tokens live until they expire or an
  administrator revokes the grant.

## Errors

Every failure is a `BytechefAppError` with `code`, `message`, and, when known, `status` (the HTTP status) and `runId`.

Codes the client raises itself:

| Code                    | Meaning                                                                                                                                                                                                            |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `NETWORK_ERROR`         | ByteChef could not be reached, or reading its answer failed.                                                                                                                                                       |
| `TIMEOUT`               | ByteChef did not answer within `timeoutMs`.                                                                                                                                                                        |
| `ABORTED`               | The `signal` you passed was aborted. Carries `runId` when a workflow wait was aborted.                                                                                                                             |
| `HTTP_ERROR`            | An error status without a ByteChef error body (a proxy or gateway in between); `status` is set.                                                                                                                    |
| `INVALID_RESPONSE`      | A success status whose body is empty, not JSON, or not the JSON array or object the endpoint returns, or a proxy body marked `BASE64` that is not base64; check that `BYTECHEF_URL` points at the ByteChef server. |
| `APP_TOOL_NOT_FOUND`    | `tools.executeByName` found no tool of that name that the app may use.                                                                                                                                             |
| `SIGN_IN_UNAVAILABLE`   | The instance has no OAuth2 client (no URL, or sign-in off), so `auth` cannot start a sign-in.                                                                                                                      |
| `SESSION_ENDED`         | The authorization server refused the code or the refresh token (`invalid_grant`): the user signs in again.                                                                                                         |
| `SIGN_IN_MISCONFIGURED` | The token endpoint refused the client or the request (`invalid_client`, a redirect URI mismatch, ...) or answered with a redirect, which is never followed. Signing in again does not help.                        |
| `RATE_LIMITED`          | The token endpoint answered 429.                                                                                                                                                                                   |

Refusals and failures from ByteChef arrive with the server's own `code`, `message` and `status`:

- `APP_*` codes such as `APP_DISABLED`, `APP_OPERATION_NOT_ALLOWED` (the allow-list refuses the call),
  `APP_CONNECTION_UNAVAILABLE`, `APP_WORKFLOW_INPUT_INVALID`, `APP_WORKFLOW_NOT_FOUND`, `APP_WORKFLOW_RUN_NOT_FOUND`,
  `APP_WORKFLOW_UNAVAILABLE`, `APP_EXECUTION_TIMEOUT`, `APP_EXECUTION_FAILED` (an API endpoint's workflow failed) and
  the `APP_PROXY_*` family.
- `APP_USER_REQUIRED` (401, the instance requires a signed-in user), `APP_USER_TOKEN_INVALID` (401, the token is
  expired, revoked or not for this instance) and `APP_USER_FORBIDDEN` (403, the user may not use the instance).
- `UNAUTHORIZED` - ByteChef rejected the app key (`APP_DISABLED` when the app is disabled).
- `COMPONENT_ERROR` - the action or tool itself failed.
- `PROVIDER_ERROR` - the external service behind the connection answered with an error.

An older ByteChef without `/actions` answers 404, which `actions.list` raises as `HTTP_ERROR` with `status` 404.

A workflow run that failed is **data, not an error**: `workflows.execute` resolves with `status: 'FAILED'` and an
`error`. Only failing to start or follow the run throws.

## TanStack Start

`@bytechef/app/tanstack-start` and `@bytechef/app/tanstack-start/server-fns` need `@tanstack/react-start` 1.168.10 or
later (Node 22.12 or later). 1.168.10 is the first version that exports `createCsrfMiddleware`, which
`bytechefRequestMiddleware` builds its CSRF check from; with an older version the app fails to build or start
(a missing export, or the error `bytechefRequestMiddleware` throws under the dev server) instead of running server
functions without that check.

### Your own server functions (recommended)

`bytechefMiddleware(options?)` puts a configured client on the server function's context as `context.bytechef`. Build
server functions on it that expose exactly what your app needs:

```ts
import {bytechefMiddleware} from '@bytechef/app/tanstack-start';
import {createServerFn} from '@tanstack/react-start';

interface ContactSummary {
    email: string;
    name: string;
}

const withBytechef = bytechefMiddleware();
// or: bytechefMiddleware({url: 'https://app.example.com', appKey: process.env.MY_KEY});

export const findContacts = createServerFn({method: 'POST'})
    .middleware([withBytechef])
    .validator((input: unknown) => {
        if (typeof input !== 'object' || input === null || typeof (input as {query?: unknown}).query !== 'string') {
            throw new TypeError('query must be a string');
        }

        return {query: (input as {query: string}).query};
    })
    .handler(({context, data}) =>
        context.bytechef.tools.executeByName<ContactSummary[]>('CRM__SEARCH_CONTACTS', {query: data.query})
    );
```

A server function's handler must return a concrete JSON-serialisable type, not `unknown`; give `executeByName`,
`execute` and the like a type argument that describes the result.

Call it from a component or loader: `await findContacts({data: {query: 'ada'}})`.

Use `.validator(...)`, not `.inputValidator(...)`, which is deprecated. TanStack Start compiles `createServerFn` only
when it is declared at the top level of a module, so a function that builds server functions inside it does not work;
this is also why the ready-made functions below read the environment and cannot take options.

### Ready-made server functions

For quick starts, `@bytechef/app/tanstack-start/server-fns` exports eleven ready-made server functions, configured from
`BYTECHEF_URL` and `BYTECHEF_APP_KEY`. When `bytechefAuthMiddleware()` is registered as request middleware, they use the
client it put on `context.bytechef`, so a signed-in user's calls carry that user's token:

| Server function      | Call                                                                        | Resolves to                             |
| -------------------- | --------------------------------------------------------------------------- | --------------------------------------- |
| `listApiCollections` | `listApiCollections()`                                                      | the bound API collections               |
| `callApi`            | `callApi({data: {method, path, query?, headers?, body?}})`                  | `{body, headers, status}`               |
| `listConnections`    | `listConnections()`                                                         | the bindings                            |
| `executeAction`      | `executeAction({data: {bindingKey, actionName, input, componentVersion?}})` | the action result                       |
| `listTools`          | `listTools()`                                                               | the tools                               |
| `executeTool`        | `executeTool({data: {bindingKey, toolName, input}})`                        | the tool result                         |
| `executeToolByName`  | `executeToolByName({data: {name, input}})`                                  | the tool result                         |
| `proxy`              | `proxy({data: {bindingKey, request}})`                                      | `{body, bodyEncoding, headers, status}` |
| `listWorkflows`      | `listWorkflows()`                                                           | the workflows                           |
| `executeWorkflow`    | `executeWorkflow({data: {workflowKey, input, waitSeconds?}})`               | the run, after the server's hold        |
| `getWorkflowRun`     | `getWorkflowRun({data: {runId}})`                                           | the run                                 |

```ts
import {executeAction} from '@bytechef/app/tanstack-start/server-fns';

const result = await executeAction({
    data: {actionName: 'createContact', bindingKey: 'crm', input: {email: 'ada@example.com'}},
});
```

#### Long-running workflows

The ready-made `executeWorkflow` does not wait for a run to finish. It returns the run after the server's `waitSeconds`
hold - still `RUNNING`, or `APPROVAL_REQUIRED`, for a long run - and the browser polls `getWorkflowRun`:

```ts
import {executeWorkflow, getWorkflowRun} from '@bytechef/app/tanstack-start/server-fns';

let run = await executeWorkflow({
    data: {input: {email: 'ada@example.com'}, waitSeconds: 10, workflowKey: 'onboard-customer'},
});

while (run.status === 'RUNNING') {
    await new Promise((resolve) => setTimeout(resolve, 2_000));

    run = await getWorkflowRun({data: {runId: run.runId}});
}
```

A server-side wait belongs in your own server function, where you can bound it with a signal:
`context.bytechef.workflows.execute(key, input, {signal, wait: true})`.

Things to know about values that cross a server function:

- Results are JSON values. An action or tool that returns nothing resolves to `undefined`.
- `proxy` returns the wire form `{body, bodyEncoding, headers, status}` - a plain object, not a `ProxyResult`. A binary
  body arrives as base64 with `bodyEncoding: 'BASE64'`.
- Inputs must be plain objects, arrays, strings, numbers and booleans - no `Map`, `Date` or typed arrays. A proxy
  request body through a server function is a string, a plain object or an array. To send bytes, use the core client on
  the server.
- When a call fails, the error is rethrown to the browser by TanStack Start and may arrive with its message only: do not
  depend on `error.code` in the browser. Handle codes inside your own server functions.

The `*Handler` functions, the `*Input` validators and their types (`ExecuteActionInput`, ...) are exported from
`@bytechef/app/tanstack-start` if you want to assemble your own server functions from the same parts.

### Trade-off to be aware of

A server function is a public HTTP endpoint of your app. **Anyone who can open the deployed app can call the ready-made
server functions, with any input the app's allow-list permits.** The key still never leaves the server, and ByteChef
refuses anything the allow-list does not cover, but whatever it covers is open to every visitor. Sign-in (below)
protects pages and your own server functions that use `bytechefAuthMiddleware()`; the ready-made server functions do
not require it, though they carry the signed-in user's token when it ran. Prefer server functions of your own that validate and narrow what they accept.

TanStack Start registers every server function declared in a module the app imports, not only the ones it uses.
Importing any one ready-made function from `@bytechef/app/tanstack-start/server-fns` therefore registers all eleven,
`proxy` included. Importing only `@bytechef/app/tanstack-start` - the middleware, handlers and validators - registers
none.

### Sign-in

When the app instance signs its users in (`userAuthMode` `REQUIRED` or `OPTIONAL` in ByteChef), the app's server runs
the OAuth2 flow and keeps the tokens in a sealed, HttpOnly cookie; the browser never holds a token or the key. Set
`BYTECHEF_SESSION_SECRET` and add the three routes and the middleware:

```ts
// src/routes/auth/login.ts (likewise callback.ts with GET: callbackHandler() and logout.ts with POST: logoutHandler())
import {loginHandler} from '@bytechef/app/tanstack-start';
import {createFileRoute} from '@tanstack/react-router';

export const Route = createFileRoute('/auth/login')({server: {handlers: {GET: loginHandler()}}});
```

```ts
// src/start.ts
import {bytechefRequestMiddleware} from '@bytechef/app/tanstack-start';
import {createStart} from '@tanstack/react-start';

export const startInstance = createStart(() => ({
    requestMiddleware: bytechefRequestMiddleware(),
}));
```

`bytechefRequestMiddleware(options)` returns the two request middleware to register: Start's CSRF check for server
function calls, then `bytechefAuthMiddleware(options)`. Register it this way. Start applies its default CSRF check
only while no `requestMiddleware` is set, so registering the auth middleware alone would leave server functions open
to cross-site calls. The CSRF check accepts a server function call only when `Sec-Fetch-Site` is `same-origin`, or,
without it, when `Origin` (then `Referer`) is the app's own (`appUrl`'s when set); page requests are not checked.

The auth middleware puts a per-request client on `context.bytechef`, carrying the signed-in user's token, and the user
(from `GET /me`) on `context.user`; with the middleware in `createStart` both are typed in every server function and
server route. With `REQUIRED` a page request without a session is redirected to `/auth/login?returnTo=<path>` and a
server function call gets 401; with `OPTIONAL` it goes on with `context.user` undefined; with `NONE` the cookie is
never read. An access token close to expiry is refreshed once per request. `bytechefAuthMiddleware()` is exported on
its own for an app that composes its own chain; it must then run as request middleware (in `createStart` or a route's
`server.middleware`), and that app keeps its own CSRF check.

`returnTo` only accepts a path on the app's origin. Without `appUrl`, the redirect URI is
`<request origin>/auth/callback` and the cookies are `Secure` unless the request's host is localhost. Behind a reverse
proxy, set `appUrl` to the public URL (for example `https://crm.example.com`): a proxy that forwards
`Host: 127.0.0.1:3000` would otherwise give a wrong redirect URI and cookies without `Secure`. With `appUrl` set, both
come from it. `logoutHandler` only clears the cookie: the tokens stay valid until they
expire, since ByteChef offers apps no revocation endpoint.

#### Session cookies and options

Two sealed cookies, both `HttpOnly; SameSite=Lax`, and `Secure` unless the app URL's host (the request's when `appUrl`
is not set) is localhost, `*.localhost`, `127.0.0.1` or `[::1]`:

| Cookie             | Holds                                                           | Path                 | Lifetime                              |
| ------------------ | --------------------------------------------------------------- | -------------------- | ------------------------------------- |
| `bytechef_session` | The access token and its expiry, the refresh token, the session | `/`                  | `sessionMaxAgeSeconds` (default 3600) |
| `bytechef_login`   | The PKCE verifier, `state`, redirect URI and return path        | `basePath` (`/auth`) | 10 minutes, cleared on every callback |

They are sealed with AES-256-GCM under a key derived from `BYTECHEF_SESSION_SECRET` (at least 32 characters; there is
no fallback, and a missing or short secret throws, only when the instance's mode is not `NONE`). A cookie that cannot be
read counts as no session and is cleared. Options, shared by `bytechefRequestMiddleware`, `bytechefAuthMiddleware` and
the three handlers: `appUrl`, `basePath` (default `/auth`, `DEFAULT_AUTH_BASE_PATH`; keep it, since ByteChef registers
only `<instance url>/auth/callback` as the redirect URI), `sessionMaxAgeSeconds` (keep it no
longer than the refresh token's 60 minutes), `sessionSecret`, and every `createBytechefClient` option; the middleware
also takes `refreshMarginMs` (default 30 000, `DEFAULT_REFRESH_MARGIN_MS`). Concurrent requests in one process share
one refresh. Across processes (serverless functions, replicas), ByteChef answers a refresh token presented again
within 10 seconds of its rotation with the same new tokens, so parallel requests of one session near the access
token's 5-minute expiry keep the session. A spent refresh token presented later than that is refused
(`invalid_grant`, `SESSION_ENDED`) and that request's session is cleared.

## Vercel AI SDK

`@bytechef/app/ai-sdk` needs `ai` (Node 22.12 or later), which in turn needs `zod`. `toAiSdkTools` turns the app's component tools - and,
if you ask, its workflows - into tools for `generateText` and `streamText`:

```ts
import {createBytechefClient} from '@bytechef/app';
import {toAiSdkTools} from '@bytechef/app/ai-sdk';
import {anthropic} from '@ai-sdk/anthropic';
import {generateText, stepCountIs} from 'ai';

const bytechef = createBytechefClient();

export async function reply(request: Request): Promise<string> {
    const {text} = await generateText({
        abortSignal: request.signal,
        model: anthropic('claude-sonnet-5-5'),
        prompt: 'Find Ada in the CRM and start her onboarding.',
        stopWhen: stepCountIs(5),
        tools: await toAiSdkTools(bytechef, {
            include: ['crm', 'onboard-customer'],
            waitForWorkflows: true,
            workflows: true,
        }),
    });

    return text;
}
```

| Option             | Meaning                                                                                                                                                                                                                             |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `include`          | Restrict the tools. Entries match flat tool names (`CRM__SEARCH_CONTACTS`), binding keys (`crm`) and workflow keys (`onboard-customer`) or their flat names (`ONBOARD_CUSTOMER`); unmatched entries are ignored. Omitted means all. |
| `workflows`        | Also add one tool per available workflow, named by the workflow key in upper case with every run of non-alphanumeric characters replaced by `_`. Default `false`.                                                                   |
| `waitForWorkflows` | How a workflow tool waits for its run: `true` (default) until it finishes or needs approval, `'approval'` through approvals, `false` returns at once.                                                                               |

Each tool's input schema is the tool's own JSON Schema; one that does not describe an object at its root is replaced by
an open object.

A workflow tool waits for its run without a time limit unless the generation passes an `abortSignal` (as in the example
above), or use `waitForWorkflows: false`.

How outcomes reach the model:

- A completed workflow returns its result. A workflow that needs approval returns a text saying a person must decide
  (with the approval form URL when there is one), a failed or cancelled one returns a text saying so, and a run still
  going (`waitForWorkflows: false`) returns a text with its run id. None of these throws.
- A refusal from ByteChef returns `Error <CODE>: <message>` as the tool result, so the model can explain or adapt, for
  example `Error APP_WORKFLOW_INPUT_INVALID: ...`.
- An HTTP 401 (a rotated or disabled app key) and a 403 `APP_DISABLED` or 403 without a code are rethrown, so they
  reach you instead of the model. A 403 `APP_OPERATION_NOT_ALLOWED` reaches the model, since its message lists the
  allowed operations.
- When ByteChef cannot be reached (`NETWORK_ERROR`, `TIMEOUT`, `HTTP_ERROR`, `INVALID_RESPONSE`), the original error is
  logged with `console.error` and the model sees only `Error <CODE>: ByteChef is unavailable`, with no host details. A
  workflow tool's `TIMEOUT` instead tells the model the run may already have started (with its run id when known) and
  must not be retried blindly.
- Aborting the generation aborts the tool call: `ABORTED` is rethrown, so a cancelled generation stops instead of
  continuing with an error text. Other unexpected errors are thrown too.

The model call itself is not governed by ByteChef. The model provider, the model you choose and what you send to it are
your app's decisions; ByteChef governs only what the tools can do.

## Runtimes

The core needs Node 20.19 or later. TanStack Start and the AI SDK need Node 22.12 or later, which is what their own
packages require. The core uses only `fetch`, `AbortController`, `TextEncoder`/`TextDecoder` and `atob`/`btoa`, so it
should run on any runtime that provides them; CI runs it on Node 22.

## License

MIT
