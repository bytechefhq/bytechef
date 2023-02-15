import {useQuery} from '@tanstack/react-query';
import {WorkflowModel, WorkflowsApi} from '../data-access/workflow';

export const WorkflowKeys = {
    workflow: (id: number) => ['workflow', id],
    workflows: ['workflows'] as const,
};

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowModel[], Error>(
        WorkflowKeys.workflows,
        () => new WorkflowsApi().getWorkflows(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
