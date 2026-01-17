import {type Locator, expect, test} from '@playwright/test';

import {ProjectsPage} from '../../pages/projectsPage';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {ROUTES} from '../../utils/constants';
import getRandomString from '../../utils/getRandomString';
import {ensureAuthenticated} from '../../utils/projects';

test.describe('Projects', () => {
    const randomString = getRandomString();

    const projectName = `project_${randomString}`;
    const workflowName = `workflow_${randomString}`;

    let projectsPage: ProjectsPage;

    test.beforeEach(async ({page}) => {
        await ensureAuthenticated(page);

        projectsPage = new ProjectsPage(page);

        await page.goto('/automation/projects');

        await projectsPage.waitForPageLoad();
    });

    test('should create a new project', async ({page}) => {
        let projectId: string;

        await test.step('Create a new project', async () => {
            await expect(projectsPage.createProjectButton).toBeVisible({timeout: 10000});

            projectId = await projectsPage.createProject(projectName);

            expect(projectId).toBeDefined();
        });

        await test.step('Assert that the project is created', async () => {
            const projectItem = page.getByLabel(projectId);

            await expect(projectItem).toBeVisible({timeout: 20000});
        });

        await test.step('Delete the project', async () => {
            await projectsPage.deleteProject(projectId);
        });
    });

    test('should create a new workflow', async ({page}) => {
        let projectId: string;
        let projectItem: Locator;

        await test.step('Create a new project', async () => {
            projectId = await projectsPage.createProject(projectName);

            projectItem = page.getByLabel(projectId);

            await expect(projectItem).toBeVisible({timeout: 10000});
        });

        await test.step('Create a new workflow', async () => {
            const createWorkflowButton = projectItem.getByRole('button', {name: 'Create Workflow'});

            await clickAndExpectToBeVisible({
                target: projectsPage.createWorkflowDialogHeading,
                trigger: createWorkflowButton,
            });

            await projectsPage.workflowFormLabelInput.fill(workflowName);

            await projectsPage.saveButton.click();

            await expect(page).toHaveURL(new RegExp(`${projectId}`), {timeout: 10000});
        });

        await test.step('Delete the project and assert that it is deleted', async () => {
            await page.goto(ROUTES.projects);

            await page.waitForLoadState('domcontentloaded');

            await projectsPage.deleteProject(projectId);
        });
    });
});
