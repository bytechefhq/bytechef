/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowBasicModel, WorkflowModel} from '@/middleware/embedded/configuration';
import {IntegrationKeys} from '@/queries/embedded/integrations.queries';
import {useQuery} from '@tanstack/react-query';

export const IntegrationWorkflowKeys = {
    integrationVersionWorkflows: (id: number, integrationVersion: number) => [
        ...IntegrationKeys.integrations,
        id,
        integrationVersion,
        'integrationWorkflows',
    ],
    integrationWorkflow: (integrationId: number, integrationWorkflowId: number) => [
        ...IntegrationKeys.integrations,
        integrationId,
        'projectWorkflows',
        integrationWorkflowId,
    ],
    integrationWorkflows: (id: number) => [...IntegrationKeys.integrations, id, 'integrationWorkflows'],
};

export const useGetIntegrationWorkflowQuery = (id: number, integrationWorkflowId: number, enabled?: boolean) =>
    useQuery<WorkflowModel, Error>({
        queryKey: IntegrationWorkflowKeys.integrationWorkflow(id, integrationWorkflowId),
        queryFn: () =>
            new WorkflowApi().getIntegrationWorkflow({
                integrationWorkflowId,
            }),
        enabled: enabled === undefined ? true : enabled,
    });

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
