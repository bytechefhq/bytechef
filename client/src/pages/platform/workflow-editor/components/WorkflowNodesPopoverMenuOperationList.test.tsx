import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureComponentUsed: vi.fn()}),
}));

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            edges: [],
            nodes: [],
            setLatestComponentDefinition: vi.fn(),
            workflow: {definition: '{"tasks":[]}', id: 'workflow-1'},
        }),
}));

vi.mock('../stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            clusterRootComponentDefinitions: {},
            rootClusterElementNodeData: undefined,
            setRootClusterElementNodeData: vi.fn(),
        }),
}));

vi.mock('../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            currentNode: undefined,
            setCurrentNode: vi.fn(),
            setWorkflowNodeDetailsPanelOpen: vi.fn(),
            workflowNodeDetailsPanelOpen: false,
        }),
}));

const componentDefinitionWithBothActionsAndClusterElements = {
    actions: [
        {description: 'Sends a chat completion request', name: 'createChatCompletion', title: 'Create Chat Completion'},
    ],
    clusterElement: true,
    clusterElements: [{description: 'A chat model', name: 'gpt4', title: 'GPT-4', type: 'MODEL'}],
    clusterRoot: false,
    icon: '<svg/>',
    name: 'openAi',
    title: 'OpenAI',
    triggers: [],
    version: 1,
} as unknown as ComponentDefinition;

function renderOperationList(clusterElementType?: string) {
    const queryClient = new QueryClient();

    return render(
        (
            <QueryClientProvider client={queryClient}>
                <WorkflowNodesPopoverMenuOperationList
                    clusterElementType={clusterElementType}
                    componentDefinition={componentDefinitionWithBothActionsAndClusterElements}
                    setPopoverOpen={vi.fn()}
                    sourceNodeId="placeholder-1"
                />
            </QueryClientProvider>
        ) as ReactNode
    );
}

describe('WorkflowNodesPopoverMenuOperationList', () => {
    it('lists the component actions, not its cluster elements, when clusterElementType is undefined', () => {
        renderOperationList(undefined);

        expect(screen.getByText('Create Chat Completion')).toBeInTheDocument();
        expect(screen.queryByText('GPT-4')).not.toBeInTheDocument();
        expect(screen.getByText('Actions')).toBeInTheDocument();
    });

    it('lists the matching cluster elements when clusterElementType is set', () => {
        renderOperationList('model');

        expect(screen.getByText('GPT-4')).toBeInTheDocument();
        expect(screen.queryByText('Create Chat Completion')).not.toBeInTheDocument();
    });
});
