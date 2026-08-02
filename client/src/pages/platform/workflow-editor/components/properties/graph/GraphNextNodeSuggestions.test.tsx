import GraphNextNodeSuggestions from '@/pages/platform/workflow-editor/components/properties/graph/GraphNextNodeSuggestions';

vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {
    type ConnectionI,
    WorkflowEditorProvider,
} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {
    ComponentDefinitionBasic,
    type DeleteClusterElementParameter200Response,
    type DeleteClusterElementParameterOperationRequest,
    type DeleteWorkflowNodeParameterRequest,
    Tag,
    type UpdateClusterElementParameterOperationRequest,
    type UpdateWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {type Mock, describe, expect, test, vi} from 'vitest';

import type {UpdateWorkflowMutationType} from '@/shared/types';

const dummyMutation = {} as unknown as UseMutationResult<unknown, Error, unknown, unknown>;

const updateWorkflowNodeParameterMutation = dummyMutation as unknown as UseMutationResult<
    DeleteClusterElementParameter200Response,
    Error,
    UpdateWorkflowNodeParameterOperationRequest,
    unknown
>;
const updateClusterElementParameterMutation = dummyMutation as unknown as UseMutationResult<
    DeleteClusterElementParameter200Response,
    Error,
    UpdateClusterElementParameterOperationRequest,
    unknown
>;

const providerValue = {
    ConnectionKeys: {
        connection: () => [],
        connections: [],
        filteredConnections: () => [],
    },
    cancelWorkflowQueries: () => {},
    connectionTagsQueryKey: [],
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
    updateClusterElementParameterMutation,
    updateWorkflowMutation: {} as unknown as UpdateWorkflowMutationType,
    updateWorkflowNodeParameterMutation,
    useCreateConnectionMutation: () => ({}) as unknown as UseMutationResult<number, Error, ConnectionI, unknown>,
    useGetComponentDefinitionsQuery: () => ({}) as UseQueryResult<Array<ComponentDefinitionBasic>, Error>,
    useGetConnectionTagsQuery: () => ({}) as unknown as UseQueryResult<Tag[], Error>,
    useGetConnectionsQuery: () => ({}) as unknown as UseQueryResult<ConnectionI[], Error>,
    webhookTriggerTestApi: {
        startWebhookTriggerTest: async () => ({}),
        stopWebhookTriggerTest: async () => {},
    },
};

describe('GraphNextNodeSuggestions', () => {
    test('renders nothing when there are no node names', () => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-1', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        const {container} = render(
            <WorkflowEditorProvider value={providerValue}>
                <GraphNextNodeSuggestions nodeNames={[]} path="nodes[0].next" />
            </WorkflowEditorProvider>
        );

        expect(container).toBeEmptyDOMElement();
    });

    test('renders one suggestion per declared node name, including the current node itself', () => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-1', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        render(
            <WorkflowEditorProvider value={providerValue}>
                <GraphNextNodeSuggestions nodeNames={['node_0', 'node_1', 'node_2']} path="nodes[0].next" />
            </WorkflowEditorProvider>
        );

        const suggestions = screen.getByLabelText('Next expression suggestions');

        expect(suggestions).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'node_0'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'node_1'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'node_2'})).toBeInTheDocument();
    });

    test('picking a suggestion saves the quoted literal at the given next path', async () => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-1', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        const savePropertyMock = saveProperty as unknown as Mock;
        savePropertyMock.mockReset();

        render(
            <WorkflowEditorProvider value={providerValue}>
                <GraphNextNodeSuggestions nodeNames={['node_0', 'node_1']} path="nodes[0].next" />
            </WorkflowEditorProvider>
        );

        const user = userEvent.setup();

        await user.click(screen.getByRole('button', {name: 'node_1'}));

        expect(savePropertyMock).toHaveBeenCalledTimes(1);
        expect(savePropertyMock).toHaveBeenCalledWith(
            expect.objectContaining({
                path: 'nodes[0].next',
                type: 'STRING',
                value: "'node_1'",
                workflowId: 'wf-1',
            })
        );
    });

    test('renders enabled, clickable suggestions by default (disabled prop omitted)', () => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-1', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        render(
            <WorkflowEditorProvider value={providerValue}>
                <GraphNextNodeSuggestions nodeNames={['node_0']} path="nodes[0].next" />
            </WorkflowEditorProvider>
        );

        expect(screen.getByRole('button', {name: 'node_0'})).toBeEnabled();
    });

    test('disables suggestions when the next expression is dynamic (not a bare literal), to avoid clobbering it', () => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-1', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        render(
            <WorkflowEditorProvider value={providerValue}>
                <GraphNextNodeSuggestions disabled nodeNames={['node_0', 'node_1']} path="nodes[0].next" />
            </WorkflowEditorProvider>
        );

        expect(screen.getByRole('button', {name: 'node_0'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'node_1'})).toBeDisabled();
    });

    test('clicking a disabled suggestion saves nothing', async () => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-1', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        const savePropertyMock = saveProperty as unknown as Mock;
        savePropertyMock.mockReset();

        render(
            <WorkflowEditorProvider value={providerValue}>
                <GraphNextNodeSuggestions disabled nodeNames={['node_0']} path="nodes[0].next" />
            </WorkflowEditorProvider>
        );

        const user = userEvent.setup();

        await user.click(screen.getByRole('button', {name: 'node_0'}));

        expect(savePropertyMock).not.toHaveBeenCalled();
    });

    test('does nothing when the workflow has not been assigned an id yet', async () => {
        useWorkflowDataStore.setState({
            workflow: {id: undefined, nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        const savePropertyMock = saveProperty as unknown as Mock;
        savePropertyMock.mockReset();

        render(
            <WorkflowEditorProvider value={providerValue}>
                <GraphNextNodeSuggestions nodeNames={['node_0']} path="nodes[0].next" />
            </WorkflowEditorProvider>
        );

        const user = userEvent.setup();

        await user.click(screen.getByRole('button', {name: 'node_0'}));

        expect(savePropertyMock).not.toHaveBeenCalled();
    });
});
