import {Workflow} from '@/shared/middleware/automation/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {QueryClient} from '@tanstack/react-query';
import {describe, expect, it} from 'vitest';

import updateCachedProjectWorkflowVersion from './updateCachedProjectWorkflowVersion';

const PROJECT_ID = 7;

const cachedProjectWorkflows: Workflow[] = [
    {id: 'workflow-a', version: 1} as Workflow,
    {id: 'workflow-b', version: 1} as Workflow,
];

function createQueryClientWithCachedWorkflows() {
    const queryClient = new QueryClient();

    queryClient.setQueryData(ProjectWorkflowKeys.projectWorkflows(PROJECT_ID), cachedProjectWorkflows);

    return queryClient;
}

describe('updateCachedProjectWorkflowVersion', () => {
    it('bumps the cached version of the saved workflow and leaves the other workflows untouched', () => {
        const queryClient = createQueryClientWithCachedWorkflows();

        updateCachedProjectWorkflowVersion(queryClient, {projectId: PROJECT_ID, version: 3, workflowId: 'workflow-b'});

        expect(queryClient.getQueryData<Workflow[]>(ProjectWorkflowKeys.projectWorkflows(PROJECT_ID))).toEqual([
            {id: 'workflow-a', version: 1},
            {id: 'workflow-b', version: 3},
        ]);
    });

    it('keeps the cache untouched when the response carries no version', () => {
        const queryClient = createQueryClientWithCachedWorkflows();

        updateCachedProjectWorkflowVersion(queryClient, {
            projectId: PROJECT_ID,
            version: undefined,
            workflowId: 'workflow-b',
        });

        expect(queryClient.getQueryData<Workflow[]>(ProjectWorkflowKeys.projectWorkflows(PROJECT_ID))).toEqual(
            cachedProjectWorkflows
        );
    });

    it('keeps the cache untouched when the saved workflow id is unknown', () => {
        const queryClient = createQueryClientWithCachedWorkflows();

        updateCachedProjectWorkflowVersion(queryClient, {projectId: PROJECT_ID, version: 3, workflowId: undefined});

        expect(queryClient.getQueryData<Workflow[]>(ProjectWorkflowKeys.projectWorkflows(PROJECT_ID))).toEqual(
            cachedProjectWorkflows
        );
    });

    it('does not seed a project workflow list query when none has been fetched yet', () => {
        const queryClient = new QueryClient();

        updateCachedProjectWorkflowVersion(queryClient, {projectId: PROJECT_ID, version: 3, workflowId: 'workflow-b'});

        expect(queryClient.getQueryState(ProjectWorkflowKeys.projectWorkflows(PROJECT_ID))).toBeUndefined();
    });
});
