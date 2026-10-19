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

### Property Formula Modes Workflow Fixture

Imports `propertyFormulaModesWorkflow.json`. Its `var_1` node exposes `Flag`, `Number` and `String` data pills. Its
`propertyTesting_1` node stores a legacy number value that mixes a pill with text (`numberMinNumPrecision`:
`${var_1.Number} ms`) and an object holding one entry (`objectNoPredefinedProperties`):

```typescript
import {importPropertyFormulaModesWorkflowTest} from '../fixtures';

// importPropertyFormulaModesWorkflowTest provides: workflow fixture
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

## Invitation Specs

`tests/account/invitation.spec.ts` invites users, reads the claim and password reset links from Mailpit and accepts
them in a fresh browser. `tests/account/multiTenantInvitation.spec.ts` runs only against a multi-tenant server: it
signs up two tenant owners through `/api/register` (sign-up enabled, no activation), reuses them on later runs, and
checks that an invitation activates the invitee only in the inviting tenant.

| Variable                                             | Default                         | Purpose                                                       |
| ---------------------------------------------------- | ------------------------------- | ------------------------------------------------------------- |
| `PLAYWRIGHT_MAILPIT_URL`                             | `http://localhost:8025`         | Mailpit, which receives the invitation and reset mails        |
| `PLAYWRIGHT_USER_EMAIL` / `PLAYWRIGHT_USER_PASSWORD` | `admin@localhost.com` / `admin` | The inviting admin; on a multi-tenant server a tenant's admin |
| `BYTECHEF_MULTI_TENANT`                              | -                               | `true` runs the cross-tenant cases                            |

## Notification Specs

`tests/notifications/webhookNotifications.spec.ts` covers the Notifications settings page against a real EE server:
the Datadog and New Relic presets, test sends (unsigned, signed, after a URL change, a `500` receiver and a
link-local URL), a job that completes and is delivered to a saved webhook with a verified signature, and an email
test send read back from Mailpit.

The webhook cases post to a local request catcher on `127.0.0.1`, which the server refuses unless it allows that
host. Start the server with:

```
BYTECHEF_WEBHOOKDELIVERY_ALLOWEDHOSTS=127.0.0.1
```

or `--bytechef.webhook-delivery.allowed-hosts=127.0.0.1`. Without it the webhook cases are skipped with that message.
The email case is skipped when Mailpit is not reachable at `MAILPIT_URL` (default `http://localhost:8025`).

## Authentication specs

`tests/authentication` covers social login (Google, GitHub), SSO (OIDC and SAML), two-factor authentication and SCIM
provisioning end to end. They need identity provider mocks and a server configured to use them, so they are skipped
unless `BYTECHEF_E2E_AUTHENTICATION_MOCKS=1` is set.

1. Start the mocks. `SAML_SP_BASE_URLS` is the browser origin of the client (the Playwright base URL); the SAML IdP
   registers ByteChef under every SAML registration id for it.

    ```bash
    cd client/test/playwright/docker/authentication
    SAML_SP_BASE_URLS=http://127.0.0.1:5173 docker compose up -d
    ```

    | Mock                        | Port  | Plays                                                                  |
    | --------------------------- | ----- | ---------------------------------------------------------------------- |
    | `oidc` (mock-oauth2-server) | 18090 | Google (issuer `/google`) and every OIDC SSO provider the specs create |
    | `github`                    | 18091 | GitHub's OAuth2 endpoints                                              |
    | `saml` (SimpleSAMLphp)      | 18080 | A SAML IdP with `user1`/`user1pass` (`user1@example.com`)              |

2. Start the server (Enterprise Edition) with these settings, on a database of its own:

    ```bash
    BYTECHEF_PUBLIC_URL=http://127.0.0.1:5173
    BYTECHEF_SECURITY_SOCIAL_LOGIN_ENABLED=true
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GOOGLE_CLIENT_ID=e2e-google
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GOOGLE_CLIENT_SECRET=e2e-google-secret
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GOOGLE_AUTHORIZATION_URI=http://localhost:18090/google/authorize
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GOOGLE_TOKEN_URI=http://localhost:18090/google/token
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GOOGLE_USER_INFO_URI=http://localhost:18090/google/userinfo
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GOOGLE_JWK_SET_URI=http://localhost:18090/google/jwks
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GOOGLE_ISSUER_URI=http://localhost:18090/google
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GITHUB_CLIENT_ID=e2e-github
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GITHUB_CLIENT_SECRET=e2e-github-secret
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GITHUB_AUTHORIZATION_URI=http://localhost:18091/login/oauth/authorize
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GITHUB_TOKEN_URI=http://localhost:18091/login/oauth/access_token
    BYTECHEF_SECURITY_SOCIAL_LOGIN_GITHUB_USER_INFO_URI=http://localhost:18091/user
    BYTECHEF_SECURITY_SSO_ENABLED=true
    BYTECHEF_SECURITY_TWO_FACTOR_AUTHENTICATION_ENABLED=true
    ```

    Mail must reach Mailpit (the dev profile's default): invited users set their password and activate through the
    mailed invitation link. Set `PLAYWRIGHT_MAILPIT_URL` when Mailpit is not on `http://localhost:8025`.

3. Run the specs:

    ```bash
    cd client
    BYTECHEF_E2E_AUTHENTICATION_MOCKS=1 npx playwright test test/playwright/tests/authentication
    ```

The specs create uniquely named users and `*.e2e.example.com` SSO domains and delete the identity providers they
create. The SAML spec uses the IdP's fixed `example.com` domain, so it runs serially and first removes SAML providers
left over from an interrupted run. The admin account (`admin@localhost.com`) must not have two-factor authentication
enabled.
