import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it} from 'vitest';

import handleComponentAddedSuccess, {openNodeDetailsPanelForNewNode} from '../handleComponentAddedSuccess';

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
            activeTab: 'description',
            currentNode: undefined,
            pendingSaveNodeName: undefined,
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
        expect(state.currentNode?.componentName).toBe('slack');
    });

    it('should mark the new node as pending its first save so node-scoped queries wait for persistence', () => {
        openNodeDetailsPanelForNewNode(makeNodeData());

        expect(useWorkflowNodeDetailsPanelStore.getState().pendingSaveNodeName).toBe('slack_1');
    });

    it('should reset activeTab to description when opening the panel for a new node', () => {
        useWorkflowNodeDetailsPanelStore.setState({activeTab: 'properties'});

        openNodeDetailsPanelForNewNode(makeNodeData());

        expect(useWorkflowNodeDetailsPanelStore.getState().activeTab).toBe('description');
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
            currentNode: existingNode,
            workflowNodeDetailsPanelOpen: true,
        });

        const newNode = makeNodeData({componentName: 'slack', name: 'slack_1'});

        openNodeDetailsPanelForNewNode(newNode);

        const state = useWorkflowNodeDetailsPanelStore.getState();

        expect(state.workflowNodeDetailsPanelOpen).toBe(true);
        expect(state.currentNode?.name).toBe('http_1');
    });

    it('should clear the pending-save marker when the added node is confirmed persisted', () => {
        useWorkflowNodeDetailsPanelStore.setState({pendingSaveNodeName: 'slack_1'});

        handleComponentAddedSuccess({
            nodeData: makeNodeData(),
            queryClient: new QueryClient(),
            workflow: {id: 'workflow_1'} as Workflow,
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().pendingSaveNodeName).toBeUndefined();
    });

    it('should not clear the pending-save marker of a different node still awaiting its save', () => {
        useWorkflowNodeDetailsPanelStore.setState({pendingSaveNodeName: 'accelo_1'});

        handleComponentAddedSuccess({
            nodeData: makeNodeData({name: 'slack_1'}),
            queryClient: new QueryClient(),
            workflow: {id: 'workflow_1'} as Workflow,
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().pendingSaveNodeName).toBe('accelo_1');
    });

    it('should update panel when replacing a trigger while panel is open', () => {
        const existingTrigger = makeNodeData({
            componentName: 'manual',
            name: 'trigger_1',
            trigger: true,
        });

        useWorkflowNodeDetailsPanelStore.setState({
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
