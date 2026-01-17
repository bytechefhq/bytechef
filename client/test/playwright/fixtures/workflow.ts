/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {test as base} from '@playwright/test';

import {type TestWorkflowI, createWorkflow} from '../utils/projects';
import {type ProjectFixturesType} from './project';

export type WorkflowFixturesType = {
    workflow: TestWorkflowI;
};

export const workflowTest = base.extend<WorkflowFixturesType & ProjectFixturesType>({
    workflow: async ({page, project}, use) => {
        const workflow = await createWorkflow(page, project.id);

        await use(workflow);
    },
});
