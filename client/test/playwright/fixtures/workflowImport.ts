/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {test as base} from '@playwright/test';

import {ProjectsPage} from '../pages/projectsPage';
import {ROUTES, SAMPLE_WORKFLOW_PATH} from '../utils/constants';
import {type TestWorkflowI, importWorkflow} from '../utils/projectUtils';
import {type ProjectFixturesType} from './project';

export type ImportWorkflowFixturesType = {
    workflow: TestWorkflowI;
};

export const importWorkflowTest = base.extend<ImportWorkflowFixturesType & ProjectFixturesType>({
    workflow: async ({page, project}, use) => {
        const workflow = await importWorkflow({page, projectId: project.id, workflowFilePath: SAMPLE_WORKFLOW_PATH});

        await use(workflow);

        const projectsPage = new ProjectsPage(page);

        try {
            await page.goto(ROUTES.projects);

            await page.waitForLoadState('domcontentloaded');

            await projectsPage.deleteProject(project.id);
        } catch (error) {
            console.warn(`Failed to cleanup project ${project.id}:`, error);
        }
    },
});
