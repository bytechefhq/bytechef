import {expect, test} from '@playwright/test';

import {ProjectsPage} from '../../pages/projectsPage';
import {TEST_PROJECT, TEST_USER, TEST_WORKFLOW} from '../../utils/constants';
import {login} from '../../utils/login';

test.describe('Projects', () => {
    let projectsPage: ProjectsPage;

    test.beforeEach(async ({page}) => {
        await login(page, TEST_USER.email, TEST_USER.password);

        projectsPage = new ProjectsPage(page);

        await projectsPage.goto();

        await projectsPage.waitForPageLoad();
    });

    test('should create a new project', async () => {
        await projectsPage.createProject(TEST_PROJECT.name);

        const projectName = projectsPage.getProjectByName(TEST_PROJECT.name);

        await expect(projectName).toBeVisible();
    });

    test('should create a new workflow', async () => {
        await projectsPage.createWorkflow(TEST_WORKFLOW.name);

        const workflowLabel = projectsPage.getWorkflowByName(TEST_WORKFLOW.name);

        await expect(workflowLabel).toBeVisible();

        await projectsPage.waitForNetworkIdle();

        await expect(projectsPage.triggerNode).toBeVisible({timeout: 10000});
    });

    test.afterAll(async ({browser}) => {
        const context = await browser.newContext();

        const page = await context.newPage();

        try {
            await login(page, TEST_USER.email, TEST_USER.password);

            const cleanupProjectsPage = new ProjectsPage(page);

            await cleanupProjectsPage.goto();

            await page.waitForLoadState('networkidle');

            await cleanupProjectsPage.deleteProject(TEST_PROJECT.name);
        } finally {
            await context.close();
        }
    });
});
