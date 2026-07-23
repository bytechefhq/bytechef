import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';

vi.mock('../../cluster-element-editor/stores/useClusterElementsDataStore', () => ({
    default: Object.assign(() => ({nodes: []}), {
        getState: () => ({nodes: []}),
        setState: vi.fn(),
        subscribe: vi.fn(),
    }),
}));

vi.mock('../../stores/useWorkflowEditorStore', () => ({
    default: () => ({
        clusterElementsCanvasOpen: false,
        setClusterElementsCanvasOpen: vi.fn(),
    }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useDataPillPanelStore', () => ({
    default: Object.assign(
        (selector: (state: {setDataPillPanelOpen: () => void}) => unknown) => selector({setDataPillPanelOpen: vi.fn()}),
        {
            getState: () => ({setDataPillPanelOpen: vi.fn()}),
            setState: vi.fn(),
            subscribe: vi.fn(),
        }
    ),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useRightSidebarStore', () => ({
    default: Object.assign(
        (selector: (state: {setRightSidebarOpen: () => void}) => unknown) => selector({setRightSidebarOpen: vi.fn()}),
        {
            getState: () => ({setRightSidebarOpen: vi.fn()}),
            setState: vi.fn(),
            subscribe: vi.fn(),
        }
    ),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore', () => ({
    default: Object.assign(
        (selector: (state: {setWorkflowTestChatPanelOpen: () => void}) => unknown) =>
            selector({setWorkflowTestChatPanelOpen: vi.fn()}),
        {
            getState: () => ({setWorkflowTestChatPanelOpen: vi.fn()}),
            setState: vi.fn(),
            subscribe: vi.fn(),
        }
    ),
}));

const makeNodeData = (overrides: Partial<NodeDataType> = {}): NodeDataType => ({
    componentName: 'http',
    icon: 'icon.svg',
    name: 'http_1',
    operationName: 'get',
    type: 'http/v1/get',
    workflowNodeName: 'http_1',
    ...overrides,
});

describe('useNodeClick', () => {
    beforeEach(() => {
        useWorkflowNodeDetailsPanelStore.setState({
            activeTab: 'description',
            currentNode: undefined,
            workflowNodeDetailsPanelOpen: false,
        });

        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: makeNodeData({label: 'Stale Label'}),
                    id: 'http_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
            workflow: {
                nodeNames: [],
                tasks: [
                    {
                        label: 'Updated Label',
                        name: 'http_1',
                        type: 'http/v1/get',
                    },
                ],
            },
        });
    });

    it('should use the label from workflow tasks instead of stale React Flow node data', async () => {
        const {default: useNodeClick} = await import('../useNodeClick');

        const staleNodeData = makeNodeData({label: 'Stale Label'});

        const {result} = renderHook(() => useNodeClick(staleNodeData, 'http_1'));

        act(() => {
            result.current();
        });

        const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

        expect(currentNode?.label).toBe('Updated Label');
    });

    it('should fall back to node data label when task is not found in workflow', async () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: makeNodeData({label: 'Fallback Label'}),
                    id: 'http_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
            workflow: {
                nodeNames: [],
                tasks: [],
            },
        });

        const {default: useNodeClick} = await import('../useNodeClick');

        const nodeData = makeNodeData({label: 'Fallback Label'});

        const {result} = renderHook(() => useNodeClick(nodeData, 'http_1'));

        act(() => {
            result.current();
        });

        const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

        expect(currentNode?.label).toBe('Fallback Label');
    });

    // Merged-entity behavior (see docs/agents/merge-current-node-component.md, risk #3): the single
    // currentNode is cleared to an empty description on click, and preserves the previous node's
    // displayConditions (the carry that the old currentComponent projection used to hold).
    it('should clear currentNode description and preserve displayConditions from the previous node', async () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: makeNodeData({
                displayConditions: {show: true},
                name: 'previous_1',
                workflowNodeName: 'previous_1',
            }),
        });

        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: makeNodeData({description: 'My note'}),
                    id: 'http_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
            workflow: {
                nodeNames: [],
                tasks: [{label: 'Updated Label', name: 'http_1', type: 'http/v1/get'}],
            },
        });

        const {default: useNodeClick} = await import('../useNodeClick');

        const nodeData = makeNodeData({description: 'My note'});

        const {result} = renderHook(() => useNodeClick(nodeData, 'http_1'));

        act(() => {
            result.current();
        });

        const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

        expect(currentNode?.description).toBe('');
        expect(currentNode?.displayConditions).toEqual({show: true});
    });

    // Merged-entity behavior (risk #4): a typeless node updates the single currentNode unconditionally.
    // The panel-open gate (previously encoded by the conditional currentComponent set) now lives in
    // WorkflowEditorLayout via a currentNode?.type check.
    it('should update currentNode for a typeless node', async () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: makeNodeData({name: 'note_1', type: undefined, workflowNodeName: 'note_1'}),
                    id: 'note_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
            workflow: {nodeNames: [], tasks: []},
        });

        const {default: useNodeClick} = await import('../useNodeClick');

        const typelessNodeData = makeNodeData({name: 'note_1', type: undefined, workflowNodeName: 'note_1'});

        const {result} = renderHook(() => useNodeClick(typelessNodeData, 'note_1'));

        act(() => {
            result.current();
        });

        const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

        expect(currentNode?.workflowNodeName).toBe('note_1');
    });

    it('should use the label from workflow triggers for trigger nodes', async () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: makeNodeData({
                        componentName: 'webhook',
                        label: 'Old Trigger',
                        name: 'webhook_1',
                        trigger: true,
                        workflowNodeName: 'webhook_1',
                    }),
                    id: 'webhook_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
            workflow: {
                nodeNames: [],
                tasks: [],
                triggers: [
                    {
                        label: 'New Trigger Label',
                        name: 'webhook_1',
                        type: 'webhook/v1/newEvent',
                    },
                ],
            },
        });

        const {default: useNodeClick} = await import('../useNodeClick');

        const triggerNodeData = makeNodeData({
            componentName: 'webhook',
            label: 'Old Trigger',
            name: 'webhook_1',
            trigger: true,
            workflowNodeName: 'webhook_1',
        });

        const {result} = renderHook(() => useNodeClick(triggerNodeData, 'webhook_1'));

        act(() => {
            result.current();
        });

        const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

        expect(currentNode?.label).toBe('New Trigger Label');
    });
});
