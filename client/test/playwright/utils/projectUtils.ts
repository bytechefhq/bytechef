import {Page, expect} from '@playwright/test';

import {ProjectsPage} from '../pages/projectsPage';
import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';
import {SAMPLE_WORKFLOW_NAME} from './constants';
import getRandomString from './getRandomString';

export interface TestProjectI {
    id: string;
    name: string;
}

export interface TestWorkflowI {
    projectId: string;
    workflowId: string;
    workflowName: string;
}

export async function createProject(page: Page, projectName?: string): Promise<TestProjectI> {
    const name = projectName || `project_${getRandomString()}`;

    const projectsPage = new ProjectsPage(page);

    await page.goto('/automation/projects');

    await projectsPage.waitForPageLoad();

    const projectId = await projectsPage.createProject(name);

    return {id: projectId, name};
}

export async function createWorkflow(page: Page, projectId: string, workflowName?: string): Promise<TestWorkflowI> {
    const name = workflowName || `workflow_${getRandomString()}`;
    const projectsPage = new ProjectsPage(page);

    await page.goto('/automation/projects');

    await projectsPage.waitForPageLoad();

    const projectItem = page.getByLabel(projectId);

    await expect(projectItem).toBeVisible({timeout: 60000});

    const createWorkflowButton = projectItem.getByRole('button', {name: 'Create Workflow'});

    await createWorkflowButton.click();

    await expect(projectsPage.createWorkflowDialogHeading).toBeVisible({timeout: 10000});

    await projectsPage.workflowFormLabelInput.fill(name);
    await projectsPage.saveButton.click();

    await expect(page).toHaveURL(new RegExp(`${projectId}`), {timeout: 10000});

    const url = page.url();

    const workflowIdMatch = url.match(/project-workflows\/(\d+)/);

    const workflowId = workflowIdMatch ? workflowIdMatch[1] : 'unknown';

    return {projectId, workflowId, workflowName: name};
}

export async function createProjectWithWorkflow(
    page: Page,
    projectName?: string,
    workflowName?: string
): Promise<{project: TestProjectI; workflow: TestWorkflowI}> {
    const project = await createProject(page, projectName);
    const workflow = await createWorkflow(page, project.id, workflowName);

    return {project, workflow};
}

interface ImportWorkflowProps {
    page: Page;
    projectId: string;
    workflowFilePath: string;
}

export async function importWorkflow({page, projectId, workflowFilePath}: ImportWorkflowProps): Promise<TestWorkflowI> {
    const projectsPage = new ProjectsPage(page);

    await page.goto('/automation/projects');

    await projectsPage.waitForPageLoad();

    const projectItem = page.getByLabel(projectId);

    await expect(projectItem).toBeVisible({timeout: 60000});

    await projectItem.click();

    await page.waitForTimeout(1000);

    const workflowCreationOptionsButton = projectItem.getByRole('button', {name: 'Workflow Creation Actions'});

    await expect(workflowCreationOptionsButton).toBeVisible({timeout: 10000});

    const importWorkflowMenuItem = page.getByRole('menuitem', {name: 'Import Workflow'});

    await clickAndExpectToBeVisible({
        target: importWorkflowMenuItem,
        trigger: workflowCreationOptionsButton,
    });

    const fileInput = page.getByTestId(`${projectId}-importWorkflowHiddenInput`);

    await fileInput.setInputFiles(workflowFilePath);

    const workflowLink = page.getByLabel(`Link to workflow ${SAMPLE_WORKFLOW_NAME}`);

    await expect(workflowLink).toBeVisible({timeout: 10000});

    await workflowLink.dispatchEvent('click');

    await expect(page).toHaveURL(new RegExp(`${projectId}.*project-workflows`), {timeout: 15000});

    const url = page.url();

    const workflowIdMatch = url.match(/project-workflows\/(\d+)/);

    const workflowId = workflowIdMatch ? workflowIdMatch[1] : 'unknown';

    return {projectId, workflowId, workflowName: SAMPLE_WORKFLOW_NAME};
}
