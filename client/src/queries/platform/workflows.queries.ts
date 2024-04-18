/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowModel} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowKeys = {
    workflow: (id: string) => [...WorkflowKeys.workflows, id],
    workflows: ['workflows'] as const,
};

export const useGetWorkflowQuery = (id: string, enabled?: boolean) =>
    useQuery<WorkflowModel, Error>({
        queryKey: WorkflowKeys.workflow(id),
        queryFn: () => new WorkflowApi().getWorkflow({id}),
        enabled: enabled === undefined ? true : enabled,
    });
