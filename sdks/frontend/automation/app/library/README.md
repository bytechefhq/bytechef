# @bytechef/app

`@bytechef/app` is the server-side SDK for ByteChef Apps. An app holds one key that works only against ByteChef. An
administrator allow-lists what the app may do: the API collections it can call, the workflows it can start, the
connections it can use and the actions, tools and proxy requests on each connection. Everything else is refused by
ByteChef, whatever the app asks for. The key stays on your server and never reaches the browser.

- **Core client** - API Platform endpoints, workflows, actions, tools, and a proxy for raw API calls, from any
  server-side JavaScript.
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

| Variable           | Value                                                           |
| ------------------ | --------------------------------------------------------------- |
| `BYTECHEF_URL`     | The ByteChef server URL, for example `https://app.example.com`. |
| `BYTECHEF_APP_KEY` | The app's key.                                                  |

Create the key on the app in ByteChef with **App → Rotate key**, and set both variables in the server environment.

> **Never prefix these variables with `VITE_`** (or any other prefix your bundler exposes to the browser). A prefixed
> variable is compiled into the client bundle and the key would be public. The client refuses to be created when it
> detects a browser (`window` is defined).

The variables are read when the first request is made. Options passed to `createBytechefClient` take precedence over the
environment.

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

## Errors

Every failure is a `BytechefAppError` with `code`, `message`, and, when known, `status` (the HTTP status) and `runId`.

Codes the client raises itself:

| Code                 | Meaning                                                                                                                                                                                                            |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `NETWORK_ERROR`      | ByteChef could not be reached, or reading its answer failed.                                                                                                                                                       |
| `TIMEOUT`            | ByteChef did not answer within `timeoutMs`.                                                                                                                                                                        |
| `ABORTED`            | The `signal` you passed was aborted. Carries `runId` when a workflow wait was aborted.                                                                                                                             |
| `HTTP_ERROR`         | An error status without a ByteChef error body (a proxy or gateway in between); `status` is set.                                                                                                                    |
| `INVALID_RESPONSE`   | A success status whose body is empty, not JSON, or not the JSON array or object the endpoint returns, or a proxy body marked `BASE64` that is not base64; check that `BYTECHEF_URL` points at the ByteChef server. |
| `APP_TOOL_NOT_FOUND` | `tools.executeByName` found no tool of that name that the app may use.                                                                                                                                             |

Refusals and failures from ByteChef arrive with the server's own `code`, `message` and `status`:

- `APP_*` codes such as `APP_DISABLED`, `APP_OPERATION_NOT_ALLOWED` (the allow-list refuses the call),
  `APP_CONNECTION_UNAVAILABLE`, `APP_WORKFLOW_INPUT_INVALID`, `APP_WORKFLOW_NOT_FOUND`, `APP_WORKFLOW_RUN_NOT_FOUND`,
  `APP_WORKFLOW_UNAVAILABLE`, `APP_EXECUTION_TIMEOUT`, `APP_EXECUTION_FAILED` (an API endpoint's workflow failed) and
  the `APP_PROXY_*` family.
- `UNAUTHORIZED` - ByteChef rejected the app key (`APP_DISABLED` when the app is disabled).
- `COMPONENT_ERROR` - the action or tool itself failed.
- `PROVIDER_ERROR` - the external service behind the connection answered with an error.

An older ByteChef without `/actions` answers 404, which `actions.list` raises as `HTTP_ERROR` with `status` 404.

A workflow run that failed is **data, not an error**: `workflows.execute` resolves with `status: 'FAILED'` and an
`error`. Only failing to start or follow the run throws.

## TanStack Start

`@bytechef/app/tanstack-start` and `@bytechef/app/tanstack-start/server-fns` need `@tanstack/react-start` (Node 22.12
or later).

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
`BYTECHEF_URL` and `BYTECHEF_APP_KEY`:

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
refuses anything the allow-list does not cover, but whatever it covers is open to every visitor. Until app sign-in
lands, restrict who can open the app (for example with Vercel Deployment Protection), and prefer server functions of
your own that validate and narrow what they accept.

TanStack Start registers every server function declared in a module the app imports, not only the ones it uses.
Importing any one ready-made function from `@bytechef/app/tanstack-start/server-fns` therefore registers all eleven,
`proxy` included. Importing only `@bytechef/app/tanstack-start` - the middleware, handlers and validators - registers
none.

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
