/**
 * Global constants for Playwright E2E tests
 * These constants are shared across all test files and pages
 */

export const TEST_USER = {
    email: 'admin@localhost.com',
    password: 'admin',
} as const;

export const ROUTES = {
    login: '/login',
    projectWorkflows: (projectId: number, workflowId: number) =>
        `/automation/projects/${projectId}/project-workflows/${workflowId}`,
    projects: '/automation/projects',
} as const;

export const SAMPLE_WORKFLOW_PATH = 'test/playwright/sampleWorkflow.json';

export const SELECTABLE_INDEX_WORKFLOW_PATH = 'test/playwright/selectableIndexWorkflow.json';

export const TIMEOUTS = {
    CLICK_AND_EXPECT: 20000,
    EDITOR_CANVAS_READY: 30000,
    NODE_DETAILS_PANEL_READY: 20000,
    RETRY_CLICK: 5000,
    RETRY_VISIBILITY: 3000,
} as const;
