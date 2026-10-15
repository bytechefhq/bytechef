import {ClickedDefinitionType} from '@/shared/types';
import {renderHook, waitFor} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';
import useHandleDrop from '../useHandleDrop';

const hoisted = vi.hoisted(() => ({
    captureComponentUsed: vi.fn(),
    query: vi.fn(),
    updateWorkflowMutation: {mutate: vi.fn()},
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureComponentUsed: hoisted.captureComponentUsed}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: hoisted.updateWorkflowMutation}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({query: hoisted.query}),
}));

vi.mock('../../utils/saveWorkflowDefinition', () => ({default: vi.fn()}));

const OPEN_AI_COMPONENT_DEFINITION = {
    clusterElements: [
        {componentName: 'openAi', componentVersion: 1, name: 'model', type: 'MODEL'},
        {componentName: 'openAi', componentVersion: 1, name: 'embedding', type: 'EMBEDDING'},
    ],
    name: 'openAi',
    title: 'OpenAI',
    version: 1,
};

const MODEL_PLACEHOLDER_NODE: Node = {
    data: {clusterElementLabel: 'Model', clusterElementType: 'model', label: '+'},
    id: 'aiAgent_1-model-placeholder-0',
    parentId: 'aiAgent_1',
    position: {x: 120, y: 140},
    type: 'placeholder',
};

const droppedOpenAi = {name: 'openAi', title: 'OpenAI', version: 1} as ClickedDefinitionType;

const savedProps = () => vi.mocked(saveWorkflowDefinition).mock.calls[0]?.[0];

describe('useHandleDrop for a cluster element placeholder', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.query.mockImplementation(({queryKey}: {queryKey: unknown[]}) =>
            Promise.resolve(queryKey.includes('model') ? {properties: []} : OPEN_AI_COMPONENT_DEFINITION)
        );

        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {clusterRoot: true, componentName: 'aiAgent', workflowNodeName: 'aiAgent_1'},
                    id: 'aiAgent_1',
                    position: {x: 0, y: 200},
                    type: 'aiAgentNode',
                },
                MODEL_PLACEHOLDER_NODE,
            ],
            workflow: {
                definition: JSON.stringify({
                    tasks: [{clusterElements: {}, name: 'aiAgent_1', type: 'aiAgent/v1/chat'}],
                    triggers: [],
                }),
                id: 'workflow-1',
            },
        } as unknown as Parameters<typeof useWorkflowDataStore.setState>[0]);

        useWorkflowEditorStore.setState({
            clusterRootComponentDefinitions: {
                aiAgent_1: {
                    clusterElementTypes: [
                        {key: 'model', label: 'Model', multipleElements: false, name: 'MODEL', required: true},
                        {key: 'tools', label: 'Tools', multipleElements: true, name: 'TOOLS', required: false},
                    ],
                    name: 'aiAgent',
                },
            },
        } as unknown as Parameters<typeof useWorkflowEditorStore.setState>[0]);
    });

    it('adds the dropped component to the cluster root as an element of the placeholder type', async () => {
        const {result} = renderHook(() => useHandleDrop({taskDispatcherDefinitions: []}));

        const handleDropOnClusterElementPlaceholder = result.current[4];

        handleDropOnClusterElementPlaceholder(MODEL_PLACEHOLDER_NODE, droppedOpenAi);

        await waitFor(() => expect(saveWorkflowDefinition).toHaveBeenCalledOnce());

        expect(savedProps()?.nodeData).toEqual(
            expect.objectContaining({
                clusterElements: expect.objectContaining({
                    model: expect.objectContaining({
                        name: expect.stringMatching(/^openAi_\d+$/),
                        type: 'openAi/v1/model',
                    }),
                }),
                componentName: 'aiAgent',
                workflowNodeName: 'aiAgent_1',
            })
        );
        expect(savedProps()?.nodeIndex).toBeUndefined();
        expect(savedProps()?.placeholderId).toBeUndefined();
        expect(hoisted.captureComponentUsed).toHaveBeenCalledWith('openAi', undefined, 'model');
    });

    it('saves nothing when the dropped component has no element of the placeholder type', async () => {
        hoisted.query.mockResolvedValue({...OPEN_AI_COMPONENT_DEFINITION, clusterElements: []});

        const {result} = renderHook(() => useHandleDrop({taskDispatcherDefinitions: []}));

        await result.current[4](MODEL_PLACEHOLDER_NODE, droppedOpenAi);

        expect(saveWorkflowDefinition).not.toHaveBeenCalled();
    });
});
