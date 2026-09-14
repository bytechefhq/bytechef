/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {test as base} from '@playwright/test';

import sampleWorkflow from '../sampleWorkflow.json';
import selectableIndexWorkflow from '../selectableIndexWorkflow.json';
import {SAMPLE_WORKFLOW_PATH, SELECTABLE_INDEX_WORKFLOW_PATH} from '../utils/constants';
import {type TestWorkflowI, importWorkflow} from '../utils/projectUtils';
import {type ProjectFixturesType} from './project';

export type ImportWorkflowFixturesType = {
    workflow: TestWorkflowI;
};

interface CreateImportWorkflowTestProps {
    workflowFilePath: string;
    workflowName: string;
}

function createImportWorkflowTest({workflowFilePath, workflowName}: CreateImportWorkflowTestProps) {
    return base.extend<ImportWorkflowFixturesType & ProjectFixturesType>({
        workflow: async ({page, project}, use) => {
            const workflow = await importWorkflow({page, projectId: project.id, workflowFilePath, workflowName});

            await use(workflow);
        },
    });
}

export const importWorkflowTest = createImportWorkflowTest({
    workflowFilePath: SAMPLE_WORKFLOW_PATH,
    workflowName: sampleWorkflow.label,
});

export const importSelectableIndexWorkflowTest = createImportWorkflowTest({
    workflowFilePath: SELECTABLE_INDEX_WORKFLOW_PATH,
    workflowName: selectableIndexWorkflow.label,
});
