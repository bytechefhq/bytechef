# Playwright Test Utilities and Fixtures

This directory contains reusable utilities and fixtures for Playwright E2E tests, following the pattern where fixtures are independent and combined using `mergeTests` in test files.

## Utilities

### Authentication

- **`utils/login.ts`**: Login/logout utilities
    - `login(page, email, password, rememberMe?)`: Logs in a user
    - `logout(page)`: Logs out the current user
- **`utils/auth.ts`**: Authentication utilities
    - `ensureAuthenticated(page)`: Ensures user is logged in (checks if already authenticated)

### Projects and Workflows

- **`utils/projects.ts`**: Project and workflow creation utilities
    - `createProject(page, projectName?)`: Creates a new project and returns `{id, name}`
        - `createWorkflow(page, projectId, workflowName?)`: Creates a workflow in a project and returns `{projectId, workflowId, workflowName}`
        - `createProjectWithWorkflow(page, projectName?, workflowName?)`: Creates both project and workflow in one call

## Fixtures

Fixtures are **independent** and should be combined using `mergeTests` in test files. Each fixture provides specific fixtures that can be merged together.

### Login Fixture

Provides an authenticated page. This is a function:

```typescript
import {loginTest} from '../fixtures';

// loginTest() returns a test object with authenticatedPage fixture
```

### Project Fixture

Provides a pre-created project (automatically cleaned up after test):

```typescript
import {projectTest} from '../fixtures';

// projectTest provides: project fixture
// Requires: page (should be authenticated via loginTest when merged)
```

### Workflow Fixture

Provides a pre-created workflow (automatically cleaned up after test):

```typescript
import {workflowTest} from '../fixtures';

// workflowTest provides: workflow fixture
// Requires: page and project (use with loginTest() and projectTest via mergeTests)
```

### Selectable Index Workflow Fixture

Imports `selectableIndexWorkflow.json` instead of `sampleWorkflow.json`. Its `var_1` node holds a three
item array, an array of objects with a nested array, and an array longer than the fifty rows the data
pill array index picker lists:

```typescript
import {importSelectableIndexWorkflowTest} from '../fixtures';

// importSelectableIndexWorkflowTest provides: workflow fixture
// Requires: page and project (use with loginTest() and projectTest via mergeTests)
```

### Property Mentions Workflow Fixture

Imports `propertyMentionsWorkflow.json`. Its `propertyTesting_1` node stores text values that mix markup with
data pills: angle brackets around a `${var_1.String}` pill, and a bracketed pill path holding an ampersand
(`${var_1['Q&A']}`):

```typescript
import {importPropertyMentionsWorkflowTest} from '../fixtures';

// importPropertyMentionsWorkflowTest provides: workflow fixture
// Requires: page and project (use with loginTest() and projectTest via mergeTests)
```

## Usage Pattern

The key pattern is to use `mergeTests` at the top of your test file to combine independent fixtures:

```typescript
import {expect, mergeTests} from '@playwright/test';
import {loginTest, projectTest, workflowTest} from '../fixtures';

// Export merged test at the top level
export const test = mergeTests(loginTest(), projectTest);

test.describe('My Test Suite', () => {
    test.beforeEach(async ({authenticatedPage}) => {
        // Setup code
    });

    test('my test', async ({authenticatedPage, project}) => {
        // Has authenticatedPage from loginTest() and project from projectTest
        await authenticatedPage.goto(`/automation/projects/${project.id}`);
    });
});
```

## Usage Examples

### Example 1: Test with Authentication Only

```typescript
import {expect, mergeTests} from '@playwright/test';
import {loginTest} from '../fixtures';

export const test = mergeTests(loginTest());

test('test with authentication', async ({authenticatedPage}) => {
    await authenticatedPage.goto('/some-page');
    await expect(authenticatedPage).toHaveURL(/\/some-page/);
});
```

### Example 2: Test with Authentication and Project

```typescript
import {expect, mergeTests} from '@playwright/test';
import {loginTest, projectTest} from '../fixtures';

export const test = mergeTests(loginTest(), projectTest);

test('test with project', async ({authenticatedPage, project}) => {
    await authenticatedPage.goto(`/automation/projects/${project.id}`);
    await expect(authenticatedPage.getByText(project.name)).toBeVisible();
});
```

### Example 3: Test with All Fixtures

```typescript
import {expect, mergeTests} from '@playwright/test';
import {loginTest, projectTest, workflowTest} from '../fixtures';

export const test = mergeTests(loginTest(), projectTest, workflowTest);

test.describe('Workflow Tests', () => {
    test('test workflow editor', async ({authenticatedPage, project, workflow}) => {
        await authenticatedPage.goto(`/automation/projects/${project.id}/project-workflows/${workflow.workflowId}`);
        await expect(authenticatedPage.getByText(workflow.workflowName)).toBeVisible();
    });
});
```

### Example 4: Custom Fixture with mergeTests

```typescript
import {expect, mergeTests, test as base} from '@playwright/test';
import {loginTest, projectTest} from '../fixtures';

// Create a custom fixture
const customFixture = base.extend({
    customData: async ({page}, use) => {
        const data = {timestamp: Date.now(), value: 'test'};
        await use(data);
    },
});

// Merge custom fixture with others
export const test = mergeTests(loginTest(), projectTest, customFixture);

test('test with custom data', async ({authenticatedPage, project, customData}) => {
    expect(customData.value).toBe('test');
    await authenticatedPage.goto(`/automation/projects/${project.id}`);
});
```

### Example 5: Using Utilities Directly (for creation tests)

For tests that test creation flows themselves, use utilities directly:

```typescript
import {expect, test} from '@playwright/test';
import {ensureAuthenticated} from '../utils/auth';
import {createProject, createWorkflow} from '../utils/projects';

test('should create a project', async ({page}) => {
    await ensureAuthenticated(page);
    const project = await createProject(page, 'My Project');
    // Test creation logic
    // Cleanup
});
```

## Best Practices

1. **Fixtures are independent** - Each fixture is standalone and doesn't extend others
2. **Use `mergeTests` in test files** - Always combine fixtures using `mergeTests` at the top of test files
3. **Export merged test** - Use `export const test = mergeTests(...)` pattern
4. **`loginTest()` is a function** - Call it when merging: `mergeTests(loginTest(), ...)`
5. **Use utilities for creation tests** - Tests that validate creation flows should use utilities directly
6. **Fixtures auto-cleanup** - Resources created via fixtures are automatically cleaned up
7. **No `.extend()` in test files** - Only use `.extend()` when creating custom fixtures within a test file

## Fixture Architecture

Fixtures are structured as:

- **`loginTest()`** - Function that returns a test object with `authenticatedPage` fixture
- **`projectTest`** - Independent test object with `project` fixture (requires authenticated page)
- **`workflowTest`** - Independent test object with `workflow` fixture (requires project)

All fixtures are independent and can be combined in any order via `mergeTests`.

## MCP Authentication Specs

`tests/mcp/` covers the authentication cases of the management, automation and embedded MCP servers, the A2A
servers and the built-in OAuth2 authorization server. Each test title starts with the case id posted on its
ticket (#2197, #6139, #2446, #3213, #6140). The worker-scoped `mcpAuthenticationTest` fixture
(`fixtures/mcpAuthentication.ts`) creates two workspaces, the servers, API keys, signing keys and connected users
through the API and deletes them again when the worker ends. OAuth2 tokens are obtained the way an MCP client
obtains them: dynamic client registration and PKCE over HTTP, sign-in and consent in the browser.

The server must run the Enterprise Edition with the embedded authorization server enabled and its own issuer
trusted:

```
BYTECHEF_OAUTH2_AUTHORIZATION_SERVER_ENABLED=true
BYTECHEF_OAUTH2_RESOURCE_SERVER_ISSUERS_0_SELF=true
BYTECHEF_OAUTH2_RESOURCE_SERVER_ISSUERS_0_URI=<public URL>
BYTECHEF_OAUTH2_RESOURCE_SERVER_ISSUERS_0_TENANT_CLAIM=tenant_id
BYTECHEF_PUBLIC_URL=<public URL>
```

| Variable                                                     | Default                         | Purpose                                                                     |
| ------------------------------------------------------------ | ------------------------------- | --------------------------------------------------------------------------- |
| `PLAYWRIGHT_BASE_URL`                                        | `http://127.0.0.1:5173`         | Where requests are sent                                                     |
| `BYTECHEF_E2E_PUBLIC_URL`                                    | `PLAYWRIGHT_BASE_URL`           | `BYTECHEF_PUBLIC_URL` of the server; discovery and token audiences name it  |
| `BYTECHEF_E2E_ADMIN_EMAIL` / `BYTECHEF_E2E_ADMIN_PASSWORD`   | `admin@localhost.com` / `admin` | A tenant admin; on a multi-tenant server use an admin of a tenant           |
| `BYTECHEF_E2E_EDITOR_EMAIL` / `BYTECHEF_E2E_EDITOR_PASSWORD` | -                               | An `EDITOR` of `BYTECHEF_E2E_MEMBER_WORKSPACE_ID` who is not a tenant admin |
| `BYTECHEF_E2E_VIEWER_EMAIL` / `BYTECHEF_E2E_VIEWER_PASSWORD` | -                               | A `VIEWER` of `BYTECHEF_E2E_MEMBER_WORKSPACE_ID` who is not a tenant admin  |
| `BYTECHEF_E2E_MEMBER_WORKSPACE_ID`                           | -                               | Used as workspace W1 when the member variables are set                      |

Without the member variables the cases that need a workspace member are skipped. On a multi-tenant server a
member signs in only when their login equals their email (#6334). The three audit cases are
`fixme`: no API exposes the persisted audit events. On a multi-tenant server the API cannot delete OAuth2
clients, so the clients the run registers stay behind (their grants are revoked).

```bash
npx playwright test test/playwright/tests/mcp --workers=1
```

## Approval Specs

The specs in `tests/approvals` call some server paths directly, not through the client: webhooks, the channel
interactivity callbacks and the approval short-token endpoint. They read the server from `BYTECHEF_SERVER_URL`.

| Variable                | Default                 | Purpose                                                                      |
| ----------------------- | ----------------------- | ---------------------------------------------------------------------------- |
| `BYTECHEF_SERVER_URL`   | `http://localhost:9555` | The server the client proxies to; required when `PLAYWRIGHT_BASE_URL` is set |
| `MAILPIT_URL`           | `http://localhost:8025` | Mailpit, which receives the approval mails                                   |
| `OPENAI_API_KEY`        | -                       | Runs the AI agent cases, which call gpt-4o-mini                              |
| `PLAYWRIGHT_SLOW`       | -                       | `1` runs the sweep and one-hour expiry cases                                 |
| `BYTECHEF_MULTI_TENANT` | -                       | `true` runs the cross-tenant cases against a multi-tenant server             |

When `PLAYWRIGHT_BASE_URL` is set and `BYTECHEF_SERVER_URL` is not, the approval utilities stop with an error instead
of falling back to `localhost:9555`, so a run against one client never sends its server calls to another server.

## Copilot Specs (#1570)

The Copilot specs live in `tests/copilot/` and `tests/mcp/`. They come in two kinds.

### Deterministic specs

`copilotPickers.spec.ts`, `copilotDelegateQuestion.spec.ts` and `copilotRegressions.spec.ts` run against any local
server, with no AI model configured. They never reach a model:

- **`utils/agUiStreamStub.ts`** stubs the Copilot's AG-UI stream (`POST /api/platform/internal/ai/chat/<agentId>`,
  `text/event-stream`). `stubCopilotRun(page, urlPattern, turns)` serves one scripted turn per request and records each
  request body (`messages`, `state`, `tools`), so a spec can assert what the client sent back. Turns are built from
  `runStarted`, `textMessage`, `toolCall`, `runFinished` and `runError` (or the `completedTurn` and `failedTurn`
  shortcuts). The event wire format mirrors the server: a tool call emits `TOOL_CALL_START`, `TOOL_CALL_ARGS` and
  `TOOL_CALL_END` before the tool runs and `TOOL_CALL_RESULT` after it, and `RUN_ERROR` may carry a `code`.
- **`utils/copilotUtils.ts`** turns the Copilot on by patching `/actuator/info`, stubs the `aiDefaultModel` and
  `aiProviderCatalog` GraphQL operations, hides the React Query devtools button (it covers the composer's send
  button), and navigates inside the app with `navigateInApp` when a spec must not reload the page.
- **`pages/copilotPanel.ts`** is the panel page object (composer, Build switch, messages, edit and refresh actions).

### Live specs

`copilotLive.spec.ts` and `tests/mcp/managementMcpLive.spec.ts` are tagged `@live` and skip unless
`PLAYWRIGHT_LIVE=1`. They need a server started with `bytechef.ai.copilot.enabled=true` and a configured AI model.
The MCP specs talk JSON-RPC to `/api/management/{secretKey}/mcp` through `utils/mcpClient.ts`.

| Variable                      | Purpose                                                                                                                                                                            |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PLAYWRIGHT_BASE_URL`         | Client URL (default `http://127.0.0.1:5173`). When set, Playwright does not start `npm run dev` and runs against the client already serving that URL                               |
| `PLAYWRIGHT_LIVE`             | `1` runs the `@live` specs                                                                                                                                                         |
| `PLAYWRIGHT_MCP_SECRET_KEY`   | The management MCP secret key (the `{secretKey}` path segment)                                                                                                                     |
| `PLAYWRIGHT_MCP_BEARER_TOKEN` | Optional API key sent as `Authorization: Bearer` to the management MCP                                                                                                             |
| `PLAYWRIGHT_MCP_ENVIRONMENT`  | `X-ENVIRONMENT` header for the management MCP (default `DEVELOPMENT`)                                                                                                              |
| `PLAYWRIGHT_COPILOT_DISABLED` | `1` when the server runs with `bytechef.ai.copilot.enabled=false`; runs the MCP spec that checks the management tools still answer and `buildWorkflow`/`importWorkflow` are absent |

Without `PLAYWRIGHT_BASE_URL`, `playwright.config.ts` starts (or reuses) the Vite dev server on port 5173. To run
against a client and server that are already running elsewhere, set `PLAYWRIGHT_BASE_URL` to the client URL; the
`webServer` step is then skipped.

The management MCP secret key is part of the request path (`/api/management/{secretKey}/mcp`), so Playwright traces,
HTML reports and request error messages from live runs contain it. Do not share live-run artifacts.

```bash
cd client

# Against an already running client
PLAYWRIGHT_BASE_URL=http://127.0.0.1:5176 npx playwright test tests/copilot

# Deterministic
npx playwright test tests/copilot/copilotPickers.spec.ts tests/copilot/copilotDelegateQuestion.spec.ts \
  tests/copilot/copilotRegressions.spec.ts

# Live, once, without retries
PLAYWRIGHT_LIVE=1 PLAYWRIGHT_MCP_SECRET_KEY=<secret> npx playwright test tests/copilot/copilotLive.spec.ts \
  tests/mcp/managementMcpLive.spec.ts --workers=1 --retries=0
```

### Scenario coverage

| #1570 scenario                                                                                                                | Spec                                                                                    |
| ----------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| 1. Interactive pickers (select connection, create connection, property option, ask question, dimming, negative ids, Ask mode) | `copilotPickers.spec.ts`; live Slack build in `copilotLive.spec.ts`                     |
| 2. Ask mode keeps the picker tools out                                                                                        | `copilotPickers.spec.ts` (ASK mode test)                                                |
| 3. Management MCP tool surface and buildWorkflow                                                                              | `managementMcpLive.spec.ts`                                                             |
| 4. Delegated specialist question                                                                                              | `copilotDelegateQuestion.spec.ts`; MCP numbered question in `managementMcpLive.spec.ts` |
| 5. importWorkflow and the Copilot-disabled MCP                                                                                | `managementMcpLive.spec.ts`                                                             |

`copilotRegressions.spec.ts` pins bugs that unit tests missed: the editor's connection issue clearing after a
Copilot binding (C-H6), the Build/Ask mode following the conversation (F6), editing and reloading messages, the
model picker's remembered model, coded run errors (E1) and the no-AI-provider empty state.
