import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import saveClusterElementNodesPosition from './saveClusterElementNodesPosition';

const {saveWorkflowDefinitionMock} = vi.hoisted(() => ({saveWorkflowDefinitionMock: vi.fn()}));

vi.mock('./saveWorkflowDefinition', () => ({default: saveWorkflowDefinitionMock}));

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
