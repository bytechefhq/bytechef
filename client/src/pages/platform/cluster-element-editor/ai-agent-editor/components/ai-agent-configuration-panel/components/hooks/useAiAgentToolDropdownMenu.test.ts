import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useAiAgentToolDropdownMenu from './useAiAgentToolDropdownMenu';
import {ToolItemI} from './useAiAgentTools';

const hoisted = vi.hoisted(() => ({
    handleDeleteTask: vi.fn(),
    nodeDetailsPanelState: {
        currentNode: undefined,
        setActiveTab: vi.fn(),
        setAiAgentNodeDetailsPanelOpen: vi.fn(),
        setCurrentNode: vi.fn(),
    },
}));

vi.mock('@/pages/platform/workflow-editor/utils/handleDeleteTask', () => ({default: hoisted.handleDeleteTask}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        cancelWorkflowQueries: vi.fn(),
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: {},
    }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: (state: unknown) => unknown) => selector({workflow: {definition: JSON.stringify({tasks: []})}}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: unknown) => unknown) =>
        selector({
            rootClusterElementNodeData: {name: 'aiAgent_1', workflowNodeName: 'aiAgent_1'},
            setRootClusterElementNodeData: vi.fn(),
        }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: Object.assign((selector: (state: unknown) => unknown) => selector(hoisted.nodeDetailsPanelState), {
        getState: () => hoisted.nodeDetailsPanelState,
    }),
}));

vi.mock('@tanstack/react-query', () => ({useQueryClient: () => ({})}));

const channel: ToolItemI = {
    clusterElementType: 'approvalChannels',
    componentName: 'slack',
    componentVersion: 1,
    label: 'slack_1',
    name: 'slack_1',
    operationName: 'approvalChannel',
    title: 'Slack',
    type: 'slack/v1/approvalChannel',
};

describe('useAiAgentToolDropdownMenu', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('removes an approval channel as an approval channel cluster element', () => {
        const {result} = renderHook(() => useAiAgentToolDropdownMenu());

        act(() => result.current.handleRemoveTool(channel));

        expect(hoisted.handleDeleteTask).toHaveBeenCalledWith(
            expect.objectContaining({
                data: expect.objectContaining({clusterElementType: 'approvalChannels', name: 'slack_1'}),
            })
        );
    });

    it('opens an approval channel as an approval channel cluster element', async () => {
        const {result} = renderHook(() => useAiAgentToolDropdownMenu());

        await act(async () => {
            await result.current.handleConfigureTool(channel);
        });

        const updateCurrentNode = hoisted.nodeDetailsPanelState.setCurrentNode.mock.calls[0][0];

        expect(updateCurrentNode(undefined)).toMatchObject({clusterElementType: 'approvalChannels', name: 'slack_1'});
        expect(hoisted.nodeDetailsPanelState.setAiAgentNodeDetailsPanelOpen).toHaveBeenCalledWith(true);
    });
});
