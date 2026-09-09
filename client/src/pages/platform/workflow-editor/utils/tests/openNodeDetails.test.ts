import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import {NodeDataType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import openNodeDetails from '../openNodeDetails';

const hoisted = vi.hoisted(() => ({
    setActiveTab: vi.fn(),
    setCurrentNode: vi.fn(),
    setDataPillPanelOpen: vi.fn(),
    setPanelOpenedFromIssuesSidebar: vi.fn(),
    setRightSidebarOpen: vi.fn(),
    setWorkflowNodeDetailsPanelOpen: vi.fn(),
    setWorkflowTestChatPanelOpen: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useDataPillPanelStore', () => ({
    default: {getState: () => ({setDataPillPanelOpen: hoisted.setDataPillPanelOpen})},
}));

vi.mock('@/pages/platform/workflow-editor/stores/useRightSidebarStore', () => ({
    default: {getState: () => ({setRightSidebarOpen: hoisted.setRightSidebarOpen})},
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore', () => ({
    default: {getState: () => ({setWorkflowTestChatPanelOpen: hoisted.setWorkflowTestChatPanelOpen})},
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: {getState: () => ({workflow: {tasks: [], triggers: []}})},
}));

vi.mock('../../stores/useWorkflowEditorStore', () => ({
    default: {getState: () => ({clusterElementsCanvasOpen: false, setClusterElementsCanvasOpen: vi.fn()})},
}));

vi.mock('../../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: {
        getState: () => ({
            currentNode: undefined,
            setActiveTab: hoisted.setActiveTab,
            setCurrentNode: hoisted.setCurrentNode,
            setPanelOpenedFromIssuesSidebar: hoisted.setPanelOpenedFromIssuesSidebar,
            setWorkflowNodeDetailsPanelOpen: hoisted.setWorkflowNodeDetailsPanelOpen,
            workflowNodeDetailsPanelOpen: false,
        }),
    },
}));

vi.mock('../getNodeLabel', () => ({getNodeLabel: () => 'Logger'}));

const nodeData = {componentName: 'logger', name: 'logger_1', workflowNodeName: 'logger_1'} as NodeDataType;

describe('openNodeDetails', () => {
    beforeEach(() => {
        useWorkflowIssuesStore.getState().reset();
    });

    it('closes the workflow issues sidebar so it cannot stay open behind the details panel', () => {
        useWorkflowIssuesStore.getState().setIssuesSidebarOpen(true);

        openNodeDetails(nodeData);

        expect(useWorkflowIssuesStore.getState().issuesSidebarOpen).toBe(false);
    });

    it('closes the other right docked panels', () => {
        openNodeDetails(nodeData);

        expect(hoisted.setRightSidebarOpen).toHaveBeenCalledWith(false);
        expect(hoisted.setWorkflowTestChatPanelOpen).toHaveBeenCalledWith(false);
        expect(hoisted.setWorkflowNodeDetailsPanelOpen).toHaveBeenCalledWith(true);
    });

    it('marks a panel that replaces the issues sidebar so it does not animate in', () => {
        useWorkflowIssuesStore.getState().setIssuesSidebarOpen(true);

        openNodeDetails({name: 'task_1', workflowNodeName: 'task_1'} as never, 'properties');

        expect(hoisted.setPanelOpenedFromIssuesSidebar).toHaveBeenCalledWith(true);
    });

    it('leaves a panel opened from the canvas animating in as before', () => {
        useWorkflowIssuesStore.getState().setIssuesSidebarOpen(false);

        openNodeDetails({name: 'task_1', workflowNodeName: 'task_1'} as never, 'properties');

        expect(hoisted.setPanelOpenedFromIssuesSidebar).toHaveBeenCalledWith(false);
    });
});
