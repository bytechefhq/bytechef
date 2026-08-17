import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useWorkflowDataStoreMock, useWorkflowEditorStoreMock} = vi.hoisted(() => ({
    useWorkflowDataStoreMock: vi.fn(),
    useWorkflowEditorStoreMock: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: useWorkflowDataStoreMock,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: useWorkflowEditorStoreMock,
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 1,
}));

import useAiAgentTools from './useAiAgentTools';

const rootClusterElementNodeData = {
    clusterElements: {
        tools: [
            {name: 'grepTool_1', type: 'aiAgentUtils/v1/grepTool'},
            {
                clusterElements: {
                    approvalChannels: [{name: 'slackChannel_1', type: 'slack/v1/approvalChannel'}],
                    tools: [{name: 'deleteRecord_1', type: 'example/v1/deleteRecord'}],
                },
                name: 'approvalGateTool_1',
                parameters: {name: 'Destructive'},
                type: 'aiAgentUtils/v1/approvalGateTool',
            },
        ],
    },
    workflowNodeName: 'aiAgent_1',
};

describe('useAiAgentTools', () => {
    beforeEach(() => {
        useWorkflowEditorStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({rootClusterElementNodeData})
        );

        useWorkflowDataStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({componentDefinitions: [], workflow: {id: 'workflow-1'}})
        );
    });

    it('keeps an ungated tool out of every group', () => {
        const {result} = renderHook(() => useAiAgentTools());

        expect(result.current.tools.map((tool) => tool.name)).toEqual(['grepTool_1']);
    });

    it('groups a gate with its gated tools and channels', () => {
        const {result} = renderHook(() => useAiAgentTools());

        expect(result.current.toolGroups).toHaveLength(1);

        const [toolGroup] = result.current.toolGroups;

        expect(toolGroup.label).toBe('Destructive');
        expect(toolGroup.name).toBe('approvalGateTool_1');
        expect(toolGroup.tools.map((tool) => tool.name)).toEqual(['deleteRecord_1']);
        expect(toolGroup.channelLabels).toEqual(['slack']);
    });

    it('reports a gate with no channels so the panel can show the chat default', () => {
        useWorkflowEditorStoreMock.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({
                rootClusterElementNodeData: {
                    clusterElements: {
                        tools: [
                            {
                                clusterElements: {tools: []},
                                name: 'approvalGateTool_2',
                                parameters: {},
                                type: 'aiAgentUtils/v1/approvalGateTool',
                            },
                        ],
                    },
                    workflowNodeName: 'aiAgent_1',
                },
            })
        );

        const {result} = renderHook(() => useAiAgentTools());

        const [toolGroup] = result.current.toolGroups;

        expect(toolGroup.channelLabels).toEqual([]);
        expect(toolGroup.label).toBe('approvalGateTool_2');
    });
});
