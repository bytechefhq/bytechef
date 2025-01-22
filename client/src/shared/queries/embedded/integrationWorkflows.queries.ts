/* eslint-disable sort-keys */
import {Workflow, WorkflowApi} from '@/shared/middleware/embedded/configuration';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {useQuery} from '@tanstack/react-query';

export const IntegrationWorkflowKeys = {
    integrationVersionWorkflows: (id: number, integrationVersion: number, includeAllFields: boolean) => [
        ...IntegrationKeys.integrations,
        id,
        integrationVersion,
        includeAllFields,
        'integrationWorkflows',
    ],
    integrationWorkflow: (integrationId: number, integrationWorkflowId: number) => [
        ...IntegrationKeys.integrations,
        integrationId,
        'integrationWorkflows',
        integrationWorkflowId,
    ],
    integrationWorkflows: (id: number) => [...IntegrationKeys.integrations, id, 'integrationWorkflows'],
};

export const useGetIntegrationWorkflowQuery = (id: number, integrationWorkflowId: number, enabled?: boolean) =>
    useQuery<Workflow, Error>({
        queryKey: IntegrationWorkflowKeys.integrationWorkflow(id, integrationWorkflowId),
        queryFn: () =>
            new WorkflowApi().getIntegrationWorkflow({
                integrationWorkflowId,
            }),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationWorkflowsQuery = (id: number, enabled?: boolean) =>
    useQuery<Workflow[], Error>({
        queryKey: IntegrationWorkflowKeys.integrationWorkflows(id),
        queryFn: () =>
            new WorkflowApi().getIntegrationWorkflows({
                id,
            }),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetIntegrationVersionWorkflowsQuery = (
    id: number,
    integrationVersion: number,
    includeAllFields: boolean = true,
    enabled?: boolean
) =>
    useQuery<Workflow[], Error>({
        queryKey: IntegrationWorkflowKeys.integrationVersionWorkflows(id, integrationVersion, includeAllFields),
        queryFn: () => new WorkflowApi().getIntegrationVersionWorkflows({id, integrationVersion, includeAllFields}),
        enabled: enabled === undefined ? true : enabled,
    });
