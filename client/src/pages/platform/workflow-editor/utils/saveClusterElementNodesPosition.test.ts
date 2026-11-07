import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import saveClusterElementNodesPosition from './saveClusterElementNodesPosition';

const {saveWorkflowDefinitionMock} = vi.hoisted(() => ({saveWorkflowDefinitionMock: vi.fn()}));

vi.mock('./saveWorkflowDefinition', () => ({default: saveWorkflowDefinitionMock}));

const VOICE_SESSION_DEFINITION = JSON.stringify({
    tasks: [],
    triggers: [
        {
            clusterElements: {
                tools: [],
                voiceAgent: {
                    metadata: {ui: {nodePosition: {x: 40, y: 20}}},
                    name: 'voiceAgent_1',
                    parameters: {},
                    type: 'deepgram/v1/voiceAgent',
                },
            },
            name: 'trigger_1',
            parameters: {},
            type: 'browser/v1/voiceSession',
        },
    ],
});

const WORKFLOW = {
    definition: JSON.stringify({
        tasks: [
            {
                clusterElements: {
                    model: {
                        metadata: {ui: {nodePosition: {x: 40, y: 20}}},
                        name: 'openAi_1',
                        type: 'openAi/v1/model',
                    },
                },
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            },
        ],
    }),
    id: 'workflow-1',
};

describe('saveClusterElementNodesPosition', () => {
    beforeEach(() => {
        saveWorkflowDefinitionMock.mockReset();
        saveWorkflowDefinitionMock.mockResolvedValue(undefined);

        useClusterElementsDataStore.setState({nodes: []});
        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: {
                componentName: 'browser',
                workflowNodeName: 'trigger_1',
            } as NodeDataType,
        });
    });

    it('clears an element position inside a trigger cluster root', () => {
        saveClusterElementNodesPosition({
            clickedNodeName: 'voiceAgent_1',
            updateWorkflowMutation: {mutate: vi.fn()} as never,
            workflow: {definition: VOICE_SESSION_DEFINITION, id: 'workflow-1'},
        });

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);

        const {nodeData} = saveWorkflowDefinitionMock.mock.calls[0][0];

        expect(nodeData.workflowNodeName).toBe('trigger_1');
        expect(nodeData.type).toBe('browser/v1/voiceSession');
        expect(nodeData.clusterElements.voiceAgent.metadata?.ui?.nodePosition).toBeUndefined();
    });
});

describe('saveClusterElementNodesPosition with an explicit cluster root', () => {
    beforeEach(() => {
        saveWorkflowDefinitionMock.mockReset();
        saveWorkflowDefinitionMock.mockResolvedValue(undefined);
        useWorkflowEditorStore.setState({rootClusterElementNodeData: undefined});
    });

    it('resets an inline element position against the given root when no root is stored', () => {
        saveClusterElementNodesPosition({
            clickedNodeName: 'openAi_1',
            clusterRootName: 'aiAgent_1',
            updateWorkflowMutation: {mutate: vi.fn()} as never,
            workflow: WORKFLOW,
        });

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();

        const {nodeData} = saveWorkflowDefinitionMock.mock.calls[0][0];

        expect(nodeData.workflowNodeName).toBe('aiAgent_1');
        expect(nodeData.componentName).toBe('aiAgent');
        expect(nodeData.clusterElements.model.metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('resets against the given root and leaves the stored root of another agent untouched', () => {
        const otherAgentRootData = {
            clusterElements: {},
            componentName: 'aiAgent',
            name: 'aiAgent_2',
            workflowNodeName: 'aiAgent_2',
        } as unknown as NodeDataType;

        useWorkflowEditorStore.setState({rootClusterElementNodeData: otherAgentRootData});

        saveClusterElementNodesPosition({
            clickedNodeName: 'openAi_1',
            clusterRootName: 'aiAgent_1',
            updateWorkflowMutation: {mutate: vi.fn()} as never,
            workflow: WORKFLOW,
        });

        expect(saveWorkflowDefinitionMock.mock.calls[0][0].nodeData.workflowNodeName).toBe('aiAgent_1');
        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toBe(otherAgentRootData);
    });
});
