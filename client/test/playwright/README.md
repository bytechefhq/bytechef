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
