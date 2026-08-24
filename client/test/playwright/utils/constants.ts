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

export const SAMPLE_WORKFLOW_NAME = 'Playwright Sample Workflow';

/**
 * Readiness budgets for the workflow editor.
 *
 * The editor mounts its canvas and every node details tab from separate network round trips, so a
 * freshly opened workflow is not interactive for several seconds - longer still when Playwright runs
 * multiple workers against a single dev server. Waits that gate on that data need budgets sized for
 * the slow path; the short retry budgets are only meant to cover interaction, not loading.
 */
export const TIMEOUTS = {
    /** Total budget for a click-until-visible retry loop. */
    CLICK_AND_EXPECT: 20000,
    /** Workflow canvas finished rendering its nodes after navigation. */
    EDITOR_CANVAS_READY: 30000,
    /** Node details panel finished loading its operation definition and rendered its tab row. */
    NODE_DETAILS_PANEL_READY: 20000,
    /** Single trigger click inside a retry loop, kept well below CLICK_AND_EXPECT so the loop can iterate. */
    RETRY_CLICK: 5000,
    /** Target becoming visible after one trigger click inside a retry loop. */
    RETRY_VISIBILITY: 3000,
} as const;
