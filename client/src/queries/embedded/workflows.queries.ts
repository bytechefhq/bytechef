import {WorkflowModel} from '@/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowBasicModel} from '@/middleware/embedded/configuration';
import {IntegrationKeys} from '@/queries/embedded/integrations.queries';
import {useQuery} from '@tanstack/react-query';

export const WorkflowKeys = {
    integrationWorkflows: (id: number) => [...IntegrationKeys.integrations, id, 'integrationWorkflows'],
    workflow: (id: string) => [...WorkflowKeys.workflows, id],
    workflows: ['workflows'] as const,
};

export const useGetIntegrationWorkflowsQuery = (id: number) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: WorkflowKeys.integrationWorkflows(id),
        queryFn: () =>
            new WorkflowApi().getIntegrationWorkflows({
                id,
            }),
    });

export const useGetWorkflowQuery = (id: string, enabled?: boolean) =>
    useQuery<WorkflowModel, Error>({
        queryKey: WorkflowKeys.workflow(id),
        queryFn: () => new WorkflowApi().getWorkflow({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: WorkflowKeys.workflows,
        queryFn: () => new WorkflowApi().getWorkflows(),
    });
