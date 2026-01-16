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
