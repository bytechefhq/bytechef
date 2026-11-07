import {renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowNodeDetailsPanel from './useWorkflowNodeDetailsPanel';

const {emptyQuery} = vi.hoisted(() => ({
    emptyQuery: () => ({data: undefined, error: null, isLoading: false, refetch: () => Promise.resolve()}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetComponentDefinitionQuery: emptyQuery,
    useGetComponentDefinitionVersionsQuery: emptyQuery,
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetClusterElementDefinitionQuery: emptyQuery,
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetTaskDispatcherDefinitionQuery: emptyQuery,
}));

vi.mock('@/shared/queries/platform/triggerDefinitions.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetTriggerDefinitionQuery: emptyQuery,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useWorkflowEditor: () => ({ConnectionKeys: {connections: ['connections']}}),
}));

vi.mock('@/shared/queries/platform/workflowNodeParameters.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetClusterElementParameterDisplayConditionsQuery: emptyQuery,
    useGetWorkflowNodeParameterDisplayConditionsQuery: emptyQuery,
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useGetWorkflowTestConfigurationConnectionsQuery: emptyQuery,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useClusterElementMissingRequiredPropertiesQuery: emptyQuery,
    useWorkflowNodeMissingRequiredPropertiesQuery: emptyQuery,
}));

vi.mock('@/shared/mutations/platform/workflowNodeTestOutputs.mutations', () => ({
    useDeleteWorkflowNodeTestOutputMutation: () => ({mutate: vi.fn(), mutateAsync: vi.fn()}),
}));

vi.mock('../../hooks/useWorkflowVariables', () => ({default: () => []}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useQueryClient: () => ({
        fetchQuery: vi.fn(() => Promise.resolve(undefined)),
        invalidateQueries: vi.fn(),
        removeQueries: vi.fn(),
        setQueryData: vi.fn(),
    }),
}));

const TRIGGER_ROOT_DEFINITION = {
    tasks: [],
    triggers: [
        {
            clusterElements: {
                voiceAgent: {
                    name: 'openAiVoiceAgent_1',
                    parameters: {},
                    type: 'openAi/v1/voiceAgent',
                },
            },
            name: 'trigger_1',
            type: 'browser/v1/voiceSession',
        },
    ],
};

/* eslint-disable @typescript-eslint/no-explicit-any */
const renderPanel = () =>
    renderHook(() =>
        useWorkflowNodeDetailsPanel({
            previousComponentDefinitions: [],
            updateWorkflowMutation: {mutate: vi.fn()} as any,
            workflowNodeOutputs: [],
        })
    );

describe('useWorkflowNodeDetailsPanel under a trigger cluster root', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            nodes: [],
            workflow: {
                definition: JSON.stringify(TRIGGER_ROOT_DEFINITION),
                id: 'workflow-1',
                tasks: [],
                triggers: TRIGGER_ROOT_DEFINITION.triggers as any,
            },
            workflowNodes: [],
        } as any);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {
                clusterElementType: 'voiceAgent',
                componentName: 'openAi',
                name: 'openAiVoiceAgent_1',
                topLevelClusterRootId: 'trigger_1',
                version: 1,
                workflowNodeName: 'openAiVoiceAgent_1',
            } as any,
            workflowNodeDetailsPanelOpen: true,
        });
    });

    it("resolves a Voice Agent element's operation from the trigger that owns it", async () => {
        const {result} = renderPanel();

        await waitFor(
            () => {
                expect(result.current.currentOperationName).toBe('voiceAgent');
            },
            {interval: 5, timeout: 5000}
        );
    });

    it('resolves no operation for an element whose trigger root does not hold it', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {
                ...useWorkflowNodeDetailsPanelStore.getState().currentNode,
                topLevelClusterRootId: 'trigger_2',
            } as any,
        });

        const {result} = renderPanel();

        expect(result.current.currentOperationName).toBe('');
    });
});
