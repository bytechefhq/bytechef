/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowBasicModel} from '@/middleware/embedded/configuration';
import {IntegrationKeys} from '@/queries/embedded/integrations.queries';
import {useQuery} from '@tanstack/react-query';

export const IntegrationWorkflowKeys = {
    integrationVersionWorkflows: (id: number, integrationVersion: number) => [
        ...IntegrationKeys.integrations,
        id,
        integrationVersion,
        'integrationWorkflows',
    ],
    integrationWorkflows: (id: number) => [...IntegrationKeys.integrations, id, 'integrationWorkflows'],
};

export const useGetIntegrationWorkflowsQuery = (id: number, enabled?: boolean) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: IntegrationWorkflowKeys.integrationWorkflows(id),
        queryFn: () =>
            new WorkflowApi().getIntegrationWorkflows({
                id,
            }),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationVersionWorkflowsQuery = (id: number, integrationVersion: number, enabled?: boolean) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: IntegrationWorkflowKeys.integrationVersionWorkflows(id, integrationVersion),
        queryFn: () => new WorkflowApi().getIntegrationVersionWorkflows({id, integrationVersion}),
        enabled: enabled === undefined ? true : enabled,
    });
