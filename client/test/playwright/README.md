# Playwright Test Utilities and Fixtures

This directory contains reusable utilities and fixtures for Playwright E2E tests, following the pattern where fixtures are independent and combined using `mergeTests` in test files.

## Utilities

### Authentication

- **`utils/login.ts`**: Login/logout utilities
    - `login(page, email, password, rememberMe?)`: Logs in a user
    - `logout(page)`: Logs out the current user
    - `ensureAuthenticated(page)`: Ensures user is logged in (from `utils/projects.ts`)

### Projects and Workflows

- **`utils/projects.ts`**: Project and workflow creation utilities
    - `ensureAuthenticated(page)`: Ensures user is logged in before proceeding
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
import {ensureAuthenticated, createProject, createWorkflow} from '../utils/projects';

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
