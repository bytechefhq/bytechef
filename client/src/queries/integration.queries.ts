import {useQuery} from '@tanstack/react-query';
import {
    ComponentDefinitionModel,
    ComponentDefinitionsApi,
} from 'data-access/component-definition';
import {WorkflowModel, WorkflowsApi} from 'data-access/workflow';
import {IntegrationModel, IntegrationsApi} from 'data-access/integration';
import {
    TaskDispatcherDefinitionModel,
    TaskDispatcherDefinitionsApi,
} from 'data-access/task-dispatcher-definition';

export const IntegrationKeys = {
    componentDefinitions: ['componentDefinitions'] as const,
    integration: (id: number) => ['integration', id],
    taskDispatcherDefinitions: ['taskDispatcherDefinitions'] as const,
    workflow: ['workflow'] as const,
    integrationWorkflows: (id: number) => ['integrationWorkflows', id],
};

export const useGetComponentsQuery = () =>
    useQuery<ComponentDefinitionModel[], Error>(
        IntegrationKeys.componentDefinitions,
        () => new ComponentDefinitionsApi().getComponentDefinitions(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetFlowControlsQuery = () =>
    useQuery<TaskDispatcherDefinitionModel[], Error>(
        IntegrationKeys.taskDispatcherDefinitions,
        () => new TaskDispatcherDefinitionsApi().getTaskDispatcherDefinitions(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowModel[], Error>(
        IntegrationKeys.workflow,
        () => new WorkflowsApi().getWorkflows(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetIntegrationWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(
        IntegrationKeys.integrationWorkflows(id),
        () => new IntegrationsApi().getIntegrationWorkflows({id}),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetIntegrationQuery = (
    id: number,
    initialData: IntegrationModel
) =>
    useQuery<IntegrationModel, Error>(
        IntegrationKeys.integration(id),
        () => new IntegrationsApi().getIntegration({id}),
        {
            staleTime: 1 * 60 * 1000,
            initialData,
        }
    );
