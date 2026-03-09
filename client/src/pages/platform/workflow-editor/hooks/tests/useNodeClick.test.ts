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
            currentComponent: undefined,
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
