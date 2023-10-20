import {useQuery} from '@tanstack/react-query';
import {
    ComponentDefinitionsApi,
    ComponentDefinitionModel,
} from 'data-access/component-definition';
import {WorkflowsApi, WorkflowModel} from 'data-access/workflow';
import {
    GetIntegrationWorkflowsRequest,
    GetIntegrationRequest,
    IntegrationsApi,
    IntegrationModel,
} from 'data-access/integration';
import {
    TaskDispatcherDefinitionsApi,
    TaskDispatcherDefinitionModel,
} from 'data-access/task-dispatcher-definition';

export enum ServerStateKeysEnum {
    ComponentDefinitions = 'componentDefinitions',
    CurrentIntegration = 'currentIntegration',
    TaskDispatcherDefinitions = 'taskDispatcherDefinitions',
    Workflow = 'workflow',
    IntegrationWorkflows = 'integrationWorkflows',
}

export const useGetComponentsQuery = () =>
    useQuery<ComponentDefinitionModel[], Error>(
        [ServerStateKeysEnum.ComponentDefinitions],
        () => new ComponentDefinitionsApi().getComponentDefinitions(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetFlowControlsQuery = () =>
    useQuery<TaskDispatcherDefinitionModel[], Error>(
        [ServerStateKeysEnum.TaskDispatcherDefinitions],
        () => new TaskDispatcherDefinitionsApi().getTaskDispatcherDefinitions(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowModel[], Error>(
        [ServerStateKeysEnum.Workflow],
        () => new WorkflowsApi().getWorkflows(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetIntegrationWorkflowsQuery = (
    requestParameters: GetIntegrationWorkflowsRequest
) =>
    useQuery<WorkflowModel[], Error>(
        [ServerStateKeysEnum.IntegrationWorkflows, requestParameters],
        () => new IntegrationsApi().getIntegrationWorkflows(requestParameters),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetCurrentIntegrationQuery = (
    requestParameters: GetIntegrationRequest
) =>
    useQuery<IntegrationModel, Error>(
        [ServerStateKeysEnum.CurrentIntegration, requestParameters],
        () => new IntegrationsApi().getIntegration(requestParameters),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
