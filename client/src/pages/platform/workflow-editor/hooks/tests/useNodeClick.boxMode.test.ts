import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsViewModeStore from '../../stores/useClusterElementsViewModeStore';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';
import useNodeClick from '../useNodeClick';

vi.mock('../../../cluster-element-editor/stores/useClusterElementsDataStore', () => ({
    default: Object.assign(() => ({nodes: []}), {
        getState: () => ({nodes: []}),
        setState: vi.fn(),
        subscribe: vi.fn(),
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

const CLUSTER_ROOT_DATA = {
    clusterRoot: true,
    componentName: 'aiAgent',
    label: 'AI Agent',
    name: 'aiAgent_1',
    type: 'aiAgent/v1/chat',
    workflowNodeName: 'aiAgent_1',
} as unknown as NodeDataType;

describe('useNodeClick in box mode', () => {
    beforeEach(() => {
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: false});

        useWorkflowNodeDetailsPanelStore.setState({
            activeTab: 'description',
            currentNode: undefined,
            workflowNodeDetailsPanelOpen: false,
        });

        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: CLUSTER_ROOT_DATA,
                    id: 'aiAgent_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
            workflow: {
                nodeNames: [],
                tasks: [
                    {
                        label: 'AI Agent',
                        name: 'aiAgent_1',
                        type: 'aiAgent/v1/chat',
                    },
                ],
            },
        });
    });

    it('opens the dialog for a cluster root in dialog mode', () => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});

        const {result} = renderHook(() => useNodeClick(CLUSTER_ROOT_DATA, 'aiAgent_1'));

        act(() => result.current());

        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(true);
    });

    it('leaves the dialog closed in box mode', () => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});

        const {result} = renderHook(() => useNodeClick(CLUSTER_ROOT_DATA, 'aiAgent_1'));

        act(() => result.current());

        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(false);
    });
});
