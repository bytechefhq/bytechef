/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {test as base} from '@playwright/test';

import {ensureAuthenticated} from '../utils/auth';
import {ProjectsPage} from '../pages/projectsPage';
import {createProject, type TestProjectI} from '../utils/projects';

export type ProjectFixturesType = {
    project: TestProjectI;
};

export const projectTest = base.extend<ProjectFixturesType>({
    project: async ({page}, use) => {
        await ensureAuthenticated(page);

        const project = await createProject(page);

        await use(project);

        const projectsPage = new ProjectsPage(page);

        try {
            await projectsPage.deleteProject(project.id);
        } catch (error) {
            console.warn(`Failed to cleanup project ${project.id}:`, error);
        }
    },
});
