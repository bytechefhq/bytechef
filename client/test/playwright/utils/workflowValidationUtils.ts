import {type Page} from '@playwright/test';

import {DEVELOPMENT_ENVIRONMENT_ID, graphqlData} from './apiUtils';

export interface WorkflowValidationResultI {
    errors: string[];
    nodeIssues: Array<{kind: string; message: string; nodeName: string; propertyPath?: string}>;
    warnings: string[];
}

export async function validateWorkflowById(page: Page, workflowId: string): Promise<WorkflowValidationResultI> {
    const data = await graphqlData<{validateWorkflowById: WorkflowValidationResultI}>(
        page,
        'query validateWorkflowById($workflowId: String!, $environmentId: Long) { validateWorkflowById(workflowId: $workflowId, environmentId: $environmentId) { errors warnings nodeIssues { nodeName propertyPath kind message } } }',
        {environmentId: DEVELOPMENT_ENVIRONMENT_ID, workflowId}
    );

    return data.validateWorkflowById;
}
