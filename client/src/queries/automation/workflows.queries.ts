/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowModel} from '@/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowKeys = {
    workflows: ['workflows'] as const,
};

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowModel[], Error>({
        queryKey: WorkflowKeys.workflows,
        queryFn: () => new WorkflowApi().getWorkflows(),
    });
