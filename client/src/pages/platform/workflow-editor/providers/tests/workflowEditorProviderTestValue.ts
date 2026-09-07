import {type ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {
    ComponentDefinitionBasic,
    type DeleteClusterElementParameter200Response,
    type DeleteClusterElementParameterOperationRequest,
    type DeleteWorkflowNodeParameterRequest,
    Tag,
    type UpdateClusterElementParameterOperationRequest,
    type UpdateWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';

const dummyMutation = {} as unknown as UseMutationResult<unknown, Error, unknown, unknown>;

export const workflowEditorProviderTestValue = {
    ConnectionKeys: {
        connection: () => [],
        connectionTags: [],
        connections: [],
        filteredConnections: () => [],
    },
    cancelWorkflowQueries: () => {},
    deleteClusterElementParameterMutation: dummyMutation as unknown as UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteClusterElementParameterOperationRequest,
        unknown
    >,
    deleteWorkflowNodeParameterMutation: dummyMutation as unknown as UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteWorkflowNodeParameterRequest,
        unknown
    >,
    invalidateWorkflowQueries: () => {},
    updateClusterElementParameterMutation: dummyMutation as unknown as UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        UpdateClusterElementParameterOperationRequest,
        unknown
    >,
    updateWorkflowMutation: {} as unknown as UpdateWorkflowMutationType,
    updateWorkflowNodeParameterMutation: dummyMutation as unknown as UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        UpdateWorkflowNodeParameterOperationRequest,
        unknown
    >,
    useCreateConnectionMutation: () => ({}) as unknown as UseMutationResult<number, Error, ConnectionI, unknown>,
    useGetComponentDefinitionsQuery: () => ({}) as UseQueryResult<Array<ComponentDefinitionBasic>, Error>,
    useGetConnectionTagsQuery: () => ({}) as unknown as UseQueryResult<Tag[], Error>,
    useGetConnectionsQuery: () => ({}) as unknown as UseQueryResult<ConnectionI[], Error>,
    webhookTriggerTestApi: {
        startWebhookTriggerTest: async () => ({}),
        stopWebhookTriggerTest: async () => {},
    },
};
