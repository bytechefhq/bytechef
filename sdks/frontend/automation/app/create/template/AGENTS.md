# Working in this ByteChef app

This app calls external systems only through ByteChef, using the app key in `BYTECHEF_APP_KEY`. An admin decides in
ByteChef which API collections, connections, actions, tools, proxy methods and workflows this app may use.

## Rules

1. **Pick the first option that fits, and say why when you go past the first.**
   1. **API Platform endpoint** — the default, for logic that answers within a request (`api.call`).
   2. **Call-workflow binding** — for long-running runs, approval-waiting runs, and background one-off runs the app
      starts and does not wait for (`workflows.execute` with `waitSeconds: 0`, then poll `getRun`).
   3. **Direct action or tool** — a single operation where a workflow would be overkill, or tools for an app that
      runs its own LLM.
   4. **Proxy** — last resort, for a raw API call no action covers.

   Periodic work is a scheduled ByteChef workflow writing to a data table; the app reads the results through an API
   endpoint. Business logic in a ByteChef workflow can be changed, disabled and reused without redeploying the app;
   the same logic as a chain of actions in this app's code cannot.
2. **Every third-party system goes through `@bytechef/app`.** Never add an API key, OAuth client or vendor SDK for a
   system this app reaches through a ByteChef binding, and never call such a system's API directly.
3. **Discover before building.** Run `npm run -s bytechef:status` and use only the operations it lists. Its
   `apiCollections` give each bound collection's `v{n}/{contextPath}` path, `available` and endpoints (`METHOD path`);
   `api.list()` adds each endpoint's request and response schemas. Action input schemas come from the component's full
   property definition; a field shown as an open object depends on the connection — ask the human what to send.
   `"inputSchema": null` means the schema is unavailable, not that the operation is forbidden.
4. **Only `src/server/` imports the ByteChef client** (`createBytechefClient`, `bytechefMiddleware`); types and pure
   helpers such as `isFinished` may be imported anywhere. Components call the server functions declared in
   `src/server/`, never the client. Nothing from ByteChef goes under a `VITE_` variable.
5. **Write purpose-built server functions** on `bytechef` from `src/server/bytechef.ts`, with `.validator(...)` and a
   concrete return type, exposing exactly what the page needs. The overview and "Try it" in `src/server/dev.ts` exist
   only in development builds; replace the placeholder page with real features, but keep `src/server/dev.ts`:
   `npm run check` reads it to find the development-only server functions. Do not use the ready-made server
   functions from `@bytechef/app/tanstack-start/server-fns` unless the human asks — each one is a public endpoint.
   `npm run check` fails when they are in the build; if the human asked for them, set
   `"bytechef": {"allowReadyMadeServerFns": true}` in `package.json` and the check only warns.
6. **When something is not allowed, ask; do not work around it.** If an endpoint, operation or binding is missing
   from what the app may call, or a call fails with `APP_OPERATION_NOT_ALLOWED` or `APP_API_ENDPOINT_NOT_FOUND`, stop
   and tell the human what to ask a ByteChef admin for: the API collection, or the binding key, the component, and the
   action or tool names. Never fall back to the proxy, a direct API call or a new credential. Use the proxy only where
   the status output shows it enabled, with the listed methods.
7. **Do not retry actions, endpoint calls or workflow starts automatically;** they can have side effects. Show
   `BytechefAppError` codes meaningfully.
8. **Anything a page shows is visible to anyone who can open the app** (there is no sign-in yet). Show a workflow's
   approval link only to its intended approver; ask the human before putting it on a page. Never expose `npm run dev`
   beyond localhost (no `--host` with public port forwarding): "Try it" executes any allowed action with the real key.
9. **If you add `src/start.ts`,** include `createCsrfMiddleware({filter: (ctx) => ctx.handlerType === 'serverFn'})`
   from `@tanstack/react-start`, or the server functions lose CSRF protection.
10. **Finish with `npm run check`.** Typecheck, build and the leak check must pass. Vercel deploys run it as their
    build command (`vercel.json`) and the Docker build runs the leak check, but run it locally before you hand work
    back.

## Setting up the app with ByteChef

Everything here runs in ByteChef's DEVELOPMENT environment; staging and production stay a person's decision. The app
is managed with the `bytechef` CLI, workflows with the ByteChef management MCP server.

1. **One-time setup, by a person.** Connect the management MCP server with its URL, an admin API key created in
   DEVELOPMENT and the header `X-ENVIRONMENT: DEVELOPMENT`. Create an automation API key in DEVELOPMENT and store it
   with `bytechef configure --host https://bytechef.example.com --token <key> --environment DEVELOPMENT --workspace-id <id>`.
   Create sign-in (OAuth) connections in the ByteChef UI; an agent cannot create them.
2. **The app first.** `bytechef automation app create --name my-app`, run once in this directory, creates the app,
   writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `.env.local` and the app's id into `.bytechef/app.json` (commit
   that file, never `.env.local`). For an app that already exists: `bytechef automation app link --app-id <id>`.
3. **API endpoints, the default.** Through the MCP server: `createProject` (pass `workspaceId`) → `buildWorkflow`
   with `kind: API_ENDPOINT` (an `apiPlatform/v1/newApiRequest` trigger, ending in
   `apiPlatform/v1/responseToAPIRequest`) → `publishProject` → `createAppApiCollection` (once per app area, with the
   published `projectVersion` and a `contextPath`) → `createAppApiEndpoint` per endpoint (method, path, the
   workflow's uuid). An `unresolved` result created nothing: call again with `connections` picked from the listed
   candidates. An `arming failed` result (from either tool) saved the collection or endpoint disabled: do not create
   it again; tell the human a person must enable it (`DEPLOYMENT_EDIT` for a collection; `API_PLATFORM_EDIT` on the
   endpoint and `DEPLOYMENT_EDIT` on the collection's deployment for an endpoint). A collection that failed this way
   still gets its endpoints and its `bindCommand`; its calls answer 409 until it is enabled. Run the `bindCommand`
   `createAppApiCollection` returned: `bytechef automation app bind api-collection --api-collection-id <id>`. The app may then call every endpoint of
   the collection at `v{collectionVersion}/{contextPath}/{path}`. To change endpoint logic: `buildWorkflow` naming
   the DRAFT workflow → `publishProject` → `updateAppApiCollectionVersion` with the new `projectVersion`; the binding
   stays valid. `listAppApiCollections` (or `bytechef automation api-collection endpoint list --collection-id <id>`)
   lists the endpoints.
4. **Scheduled work** (the app shows results of periodic work): `createAppDataTable` (on a re-run it refuses an
   existing `baseName`; reuse that table) → `buildWorkflow` with
   `kind: SCHEDULED` (a schedule trigger writing with `dataTable/v1/createRecords` or `dataTable/v1/updateRecord`,
   `table` = the table's `baseName`) → `buildWorkflow` with `kind: API_ENDPOINT` for the endpoint that reads it with
   `dataTable/v1/findRecords` → `publishProject` → `deployProject` with the scheduled workflow's uuid in
   `enabledWorkflows` → collection, endpoint and bind as in step 3. The app never starts a scheduled workflow.
5. **Workflows, only for long-running, approval or background runs.** `createProject` → `buildWorkflow` with
   `kind: APP_CALLABLE` → `publishProject` → `deployProject`. Then run the
   `bytechef automation app bind workflow --workflow-key … --deployment-id … --workflow-uuid …` command that
   `deployProject` prints; the workflow key is the key your code passes to `workflows.execute`. To change a workflow:
   `buildWorkflow` naming the `draftWorkflowId` from the last `deployProject` result → `publishProject` →
   `deployProject`; the binding stays valid.
6. **Direct actions, tools and the proxy, only when needed** (say why).
   `bytechef automation connection list --component-name hubspot` lists the connections; those with
   `"usable": true` can be bound.
   `bytechef automation app bind connection --binding-key crm --component-name hubspot --actions createContact --tools searchContacts`
   binds one: actions for this app's code, tools for this app's own AI. The binding becomes exactly what the command
   names, so repeat every action and tool you still want when you change it. With several usable connections, pass
   `--connection-id`.
7. **Check.** `npm run -s bytechef:status` shows what the app may now call; `bytechef automation app status` shows the
   bindings as ByteChef sees them, API collections included. If `buildWorkflow` is missing from the MCP server, ask a
   ByteChef admin to enable Copilot.
8. **Use it from the app** with the recipes below: API endpoints and workflows from server functions; actions only
   for a single operation; tools, and optionally workflows, for an in-app agent through `toAiSdkTools`.
9. **Loop rules.** Never print or echo `BYTECHEF_APP_KEY`: the CLI writes it to `.env.local` and never prints it, and
   `bytechef automation app key rotate` replaces it the same way. Stay in DEVELOPMENT. Ask a person to create
   sign-in connections. Without a configured CLI, rule 6 under "Rules" applies: ask a ByteChef admin for the binding.
   `bytechef automation app unbind api-collection --api-collection-id <id>`,
   `bytechef automation app unbind connection --binding-key crm` and
   `bytechef automation app unbind workflow --workflow-key onboard-customer` remove bindings;
   `bytechef automation app list` lists the workspace's apps.

## Recipes

All server functions live in `src/server/`. `bytechef` is the middleware from `src/server/bytechef.ts`; it puts a
client on `context.bytechef`.

### Call a ByteChef API endpoint

The default. `path` is `v{collectionVersion}/{contextPath}/{endpointPath}`, built from what `api.list()` lists. The status,
headers and body the endpoint's workflow chose come back as data, a 4xx or 5xx included, so check `status`.
`BytechefAppError` is thrown only for errors ByteChef itself answers: `APP_API_ENDPOINT_NOT_FOUND` (no endpoint of a
bound collection matches), `APP_API_COLLECTION_UNAVAILABLE` (bound but disabled), `APP_EXECUTION_FAILED`,
`APP_EXECUTION_TIMEOUT`, a rejected key, or a transport failure.

```ts
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

interface Order {
    id: string;
    total: number;
}

export const listOrders = createServerFn({method: 'GET'})
    .middleware([bytechef])
    .validator((input: unknown) => ({customerId: String((input as {customerId?: unknown}).customerId ?? '')}))
    .handler(async ({context, data}): Promise<Order[]> => {
        const response = await context.bytechef.api.call<Order[]>({
            method: 'GET',
            path: 'v1/shop/orders',
            query: {customerId: data.customerId},
        });

        if (response.status !== 200) {
            throw new Error(`Listing orders failed with status ${response.status}`);
        }

        return response.body;
    });
```

For a `POST`, `PUT` or `PATCH`, pass the validated input as `body`.

### Show results of scheduled work

A scheduled ByteChef workflow writes its results to a data table; an endpoint whose workflow reads the table with
`dataTable/v1/findRecords` serves them. The app calls that endpoint like any other and never starts the schedule.
The data is as fresh as the last scheduled run: have the workflow write a timestamp column and show it.

```ts
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

interface DailyReport {
    day: string;
    openTickets: number;
    updatedAt: string;
}

export const getDailyReports = createServerFn({method: 'GET'})
    .middleware([bytechef])
    .handler(async ({context}): Promise<DailyReport[]> => {
        const response = await context.bytechef.api.call<DailyReport[]>({method: 'GET', path: 'v1/reports/daily'});

        if (response.status !== 200) {
            throw new Error(`Loading the reports failed with status ${response.status}`);
        }

        return response.body;
    });
```

### A workflow run

Start the run and return it; poll from the page until it is finished. A server function gets no abort signal, so do
not wait inside it. Return a purpose-built shape, never the whole `WorkflowRun`: pick the result fields the page needs
with a type guard, and leave `approvalFormUrl` out. `getOnboarding` takes any run id a visitor sends, so it refuses a
run of another workflow.

```ts
import type {WorkflowRun} from '@bytechef/app';
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

const WORKFLOW_KEY = 'onboard-customer';

export interface Onboarding {
    customerId: string | null;
    error: string | null;
    runId: string;
    status: WorkflowRun['status'];
}

function customerIdOf(result: unknown): string | null {
    if (typeof result !== 'object' || result === null) {
        return null;
    }

    const {customerId} = result as {customerId?: unknown};

    return typeof customerId === 'string' ? customerId : null;
}

function toOnboarding(run: WorkflowRun): Onboarding {
    return {
        customerId: run.status === 'COMPLETED' ? customerIdOf(run.result) : null,
        error: run.error?.message ?? null,
        runId: run.runId,
        status: run.status,
    };
}

export const startOnboarding = createServerFn({method: 'POST'})
    .middleware([bytechef])
    .validator((input: unknown) => ({email: String((input as {email?: unknown}).email ?? '')}))
    .handler(
        async ({context, data}): Promise<Onboarding> =>
            toOnboarding(await context.bytechef.workflows.execute(WORKFLOW_KEY, data, {waitSeconds: 10}))
    );

export const getOnboarding = createServerFn({method: 'GET'})
    .middleware([bytechef])
    .validator((input: unknown) => ({runId: String((input as {runId?: unknown}).runId ?? '')}))
    .handler(async ({context, data}): Promise<Onboarding> => {
        const run = await context.bytechef.workflows.getRun(data.runId);

        if (run.workflowKey !== WORKFLOW_KEY) {
            throw new Error(`Run ${data.runId} is not an onboarding run`);
        }

        return toOnboarding(run);
    });
```

Poll with a bound: pass an `AbortSignal` (aborted when the page goes away) and a maximum number of attempts. A run
in `APPROVAL_REQUIRED` waits for its approver; its approval link stays in ByteChef (rule 8).

```tsx
import {isFinished} from '@bytechef/app';

import {getOnboarding, startOnboarding, type Onboarding} from '@/server/onboarding';

function delay(milliseconds: number, signal: AbortSignal): Promise<void> {
    return new Promise((resolve, reject) => {
        const timer = setTimeout(resolve, milliseconds);

        signal.addEventListener(
            'abort',
            () => {
                clearTimeout(timer);
                reject(signal.reason);
            },
            {once: true}
        );
    });
}

export async function runOnboarding(email: string, signal: AbortSignal, maxAttempts = 100): Promise<Onboarding> {
    let onboarding = await startOnboarding({data: {email}, signal});

    for (let attempt = 0; attempt < maxAttempts && !isFinished(onboarding.status); attempt++) {
        await delay(3000, signal);

        onboarding = await getOnboarding({data: {runId: onboarding.runId}, signal});
    }

    if (!isFinished(onboarding.status)) {
        throw new Error(`Run ${onboarding.runId} did not finish after ${maxAttempts} attempts`);
    }

    return onboarding;
}
```

### Start a background job

A one-off job the app starts and does not wait for: `waitSeconds: 0` answers at once with the run's `runId`. Return
the id and poll the run as in "A workflow run" above, with a `getRun` server function that refuses runs of other
workflows. A result that is large or must outlive run retention is written by the workflow to a data table and read
through an API endpoint instead.

```ts
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

const WORKFLOW_KEY = 'export-contacts';

export const startExport = createServerFn({method: 'POST'})
    .middleware([bytechef])
    .validator((input: unknown) => ({listId: String((input as {listId?: unknown}).listId ?? '')}))
    .handler(async ({context, data}): Promise<{runId: string}> => {
        const run = await context.bytechef.workflows.execute(WORKFLOW_KEY, data, {waitSeconds: 0});

        return {runId: run.runId};
    });
```

### An action

Use an action only for a single operation where a workflow would be overkill, and say why an API endpoint does not
fit. Logic of more than one step belongs in an endpoint's workflow.

```ts
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

interface Contact {
    email: string;
    id: string;
}

function createContactInput(input: unknown): {email: string; name: string} {
    if (typeof input !== 'object' || input === null) {
        throw new TypeError('input must be an object');
    }

    const {email, name} = input as Record<string, unknown>;

    if (typeof email !== 'string' || typeof name !== 'string') {
        throw new TypeError('email and name must be strings');
    }

    return {email, name};
}

export const createContact = createServerFn({method: 'POST'})
    .middleware([bytechef])
    .validator(createContactInput)
    .handler(({context, data}) => context.bytechef.actions.execute<Contact>('crm', 'createContact', data));
```

### A tool

```ts
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

export const searchContacts = createServerFn({method: 'POST'})
    .middleware([bytechef])
    .validator((input: unknown) => ({query: String((input as {query?: unknown}).query ?? '')}))
    .handler(({context, data}) =>
        context.bytechef.tools.executeByName<{id: string; name: string}[]>('CRM__SEARCH_CONTACTS', data)
    );
```

### The proxy

Only where `bytechef:status` shows `proxy.enabled` and the method is listed.

```ts
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';

export const getContactRaw = createServerFn({method: 'GET'})
    .middleware([bytechef])
    .validator((input: unknown) => ({id: String((input as {id?: unknown}).id ?? '')}))
    .handler(async ({context, data}) => {
        const response = await context.bytechef.proxy('crm', {
            method: 'GET',
            path: `/crm/v3/objects/contacts/${encodeURIComponent(data.id)}`,
        });

        return {body: response.body, bodyEncoding: response.bodyEncoding, status: response.status};
    });
```

### An AI chat with ByteChef tools

Needs `ai`, `zod`, a provider package (for example `@ai-sdk/anthropic`) and a model API key. That key is the app's
own secret, not governed by ByteChef — ask the human before adding it. A prompt endpoint without sign-in is a public
executor of every tool it is given, so `include` only read-only tools unless the human decides otherwise. Workflows
given to the model put their approval link into the model's reply, so leave them out.

```ts
import {anthropic} from '@ai-sdk/anthropic';
import {toAiSdkTools} from '@bytechef/app/ai-sdk';
import {createServerFn} from '@tanstack/react-start';
import {generateText, stepCountIs} from 'ai';

import {bytechef} from './bytechef';

export const ask = createServerFn({method: 'POST'})
    .middleware([bytechef])
    .validator((input: unknown) => ({prompt: String((input as {prompt?: unknown}).prompt ?? '')}))
    .handler(async ({context, data}) => {
        const result = await generateText({
            abortSignal: AbortSignal.timeout(60_000),
            model: anthropic('claude-sonnet-5-5'),
            prompt: data.prompt,
            stopWhen: stepCountIs(5),
            tools: await toAiSdkTools(context.bytechef, {include: ['CRM__SEARCH_CONTACTS']}),
        });

        return result.text;
    });
```

## Deploy

- **Vercel:** `vercel env add BYTECHEF_URL production` and `vercel env add BYTECHEF_APP_KEY production`, then
  `vercel deploy --prod`. `vercel.json` makes `npm run check` the build command, so a failing check fails the deploy.
- **Self-host:** `docker build -t my-app .`; run it with `BYTECHEF_URL` and `BYTECHEF_APP_KEY` set.
- After the key is rotated in ByteChef, update the host's variable and redeploy.
