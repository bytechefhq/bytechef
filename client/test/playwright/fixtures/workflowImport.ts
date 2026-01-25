/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {test as base} from '@playwright/test';

import {TestWorkflowI, importWorkflow} from '../utils/projects';
import {ProjectFixturesType} from './project';

export type ImportWorkflowFixturesType = {
    workflow: TestWorkflowI;
};

export const importWorkflowTest = base.extend<ImportWorkflowFixturesType & ProjectFixturesType>({
    workflow: async ({page, project}, use) => {
        const workflowFilePath = 'foo';

        const workflow = await importWorkflow(page, project.id, workflowFilePath);

        await use(workflow);
    },
});
