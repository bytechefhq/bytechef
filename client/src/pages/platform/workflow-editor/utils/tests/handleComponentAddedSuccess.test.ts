import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it} from 'vitest';

import {openNodeDetailsPanelForNewNode} from '../handleComponentAddedSuccess';

const makeNodeData = (overrides: Partial<NodeDataType> = {}): NodeDataType =>
    ({
        componentName: 'slack',
        description: 'Send a message',
        icon: null,
        label: 'Slack',
        name: 'slack_1',
        type: 'slack/v1/sendMessage',
        version: 1,
        workflowNodeName: 'slack_1',
        ...overrides,
    }) as NodeDataType;

describe('openNodeDetailsPanelForNewNode', () => {
    beforeEach(() => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentComponent: undefined,
            currentNode: undefined,
            workflowNodeDetailsPanelOpen: false,
        });
    });

    it('should open the panel and set currentNode when panel is closed', () => {
        const nodeData = makeNodeData();

        openNodeDetailsPanelForNewNode(nodeData);

        const state = useWorkflowNodeDetailsPanelStore.getState();

        expect(state.workflowNodeDetailsPanelOpen).toBe(true);
        expect(state.currentNode?.name).toBe('slack_1');
        expect(state.currentNode?.description).toBe('');
        expect(state.currentComponent?.componentName).toBe('slack');
    });

    it('should not open the panel for cluster element nodes', () => {
        const nodeData = makeNodeData({clusterElements: {}});

        openNodeDetailsPanelForNewNode(nodeData);

        const state = useWorkflowNodeDetailsPanelStore.getState();

        expect(state.workflowNodeDetailsPanelOpen).toBe(false);
        expect(state.currentNode).toBeUndefined();
    });

    it('should not change panel state when panel is already open for a non-trigger node', () => {
        const existingNode = makeNodeData({componentName: 'http', name: 'http_1'});

        useWorkflowNodeDetailsPanelStore.setState({
            currentComponent: existingNode as NodeDataType,
            currentNode: existingNode,
            workflowNodeDetailsPanelOpen: true,
        });

        const newNode = makeNodeData({componentName: 'slack', name: 'slack_1'});

        openNodeDetailsPanelForNewNode(newNode);

        const state = useWorkflowNodeDetailsPanelStore.getState();

        expect(state.workflowNodeDetailsPanelOpen).toBe(true);
        expect(state.currentNode?.name).toBe('http_1');
    });

    it('should update panel when replacing a trigger while panel is open', () => {
        const existingTrigger = makeNodeData({
            componentName: 'manual',
            name: 'trigger_1',
            trigger: true,
        });

        useWorkflowNodeDetailsPanelStore.setState({
            currentComponent: existingTrigger as NodeDataType,
            currentNode: existingTrigger,
            workflowNodeDetailsPanelOpen: true,
        });

        const newTrigger = makeNodeData({
            componentName: 'webhook',
            name: 'trigger_1',
            trigger: true,
        });

        openNodeDetailsPanelForNewNode(newTrigger);

        const state = useWorkflowNodeDetailsPanelStore.getState();

        expect(state.workflowNodeDetailsPanelOpen).toBe(true);
        expect(state.currentNode?.componentName).toBe('webhook');
    });
});
