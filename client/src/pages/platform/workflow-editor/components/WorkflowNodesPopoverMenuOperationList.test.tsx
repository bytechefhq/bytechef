import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureComponentUsed: vi.fn()}),
}));

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

const {editorStoreState, saveWorkflowDefinitionMock, workflowDataStoreState} = vi.hoisted(() => ({
    editorStoreState: {
        clusterRootComponentDefinitions: {} as Record<string, unknown>,
        rootClusterElementNodeData: undefined as Record<string, unknown> | undefined,
    },
    saveWorkflowDefinitionMock: vi.fn(),
    workflowDataStoreState: {
        definition: '{"tasks":[]}',
        nodes: [] as Array<{data: Record<string, unknown>; id: string}>,
    },
}));

vi.mock('../stores/useWorkflowDataStore', () => {
    const getState = () => ({
        edges: [],
        nodes: workflowDataStoreState.nodes,
        setLatestComponentDefinition: vi.fn(),
        setWorkflow: vi.fn(),
        workflow: {definition: workflowDataStoreState.definition, id: 'workflow-1'},
    });

    return {
        default: Object.assign((selector: (state: Record<string, unknown>) => unknown) => selector(getState()), {
            getState,
        }),
    };
});

vi.mock('../stores/useWorkflowEditorStore', () => {
    const getState = () => ({
        clusterRootComponentDefinitions: editorStoreState.clusterRootComponentDefinitions,
        rootClusterElementNodeData: editorStoreState.rootClusterElementNodeData,
        setRootClusterElementNodeData: vi.fn(),
    });

    return {
        default: Object.assign((selector: (state: Record<string, unknown>) => unknown) => selector(getState()), {
            getState,
        }),
    };
});

vi.mock('../utils/saveWorkflowDefinition', () => ({
    default: saveWorkflowDefinitionMock,
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

function renderOperationList(clusterElementType?: string, queryClient: QueryClient = new QueryClient()) {
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
    beforeEach(() => {
        editorStoreState.clusterRootComponentDefinitions = {};
        editorStoreState.rootClusterElementNodeData = undefined;
        workflowDataStoreState.definition = '{"tasks":[]}';
        workflowDataStoreState.nodes = [];
        saveWorkflowDefinitionMock.mockReset();
    });

    it('adds a cluster element with no canvas store seeded, resolving the root from the node', async () => {
        editorStoreState.clusterRootComponentDefinitions = {
            aiAgent_1: {
                clusterElementTypes: [{label: 'Model', multipleElements: false, name: 'MODEL'}],
                name: 'aiAgent',
                version: 1,
            },
        };
        workflowDataStoreState.definition = JSON.stringify({
            tasks: [{clusterElements: {}, name: 'aiAgent_1', type: 'aiAgent/v1/chat'}],
        });
        workflowDataStoreState.nodes = [
            {
                data: {clusterRoot: true, componentName: 'aiAgent', workflowNodeName: 'aiAgent_1'},
                id: 'aiAgent_1',
            },
            {
                data: {clusterElementType: 'model', parentClusterRootId: 'aiAgent_1'},
                id: 'placeholder-1',
            },
        ];

        const queryClient = new QueryClient();

        queryClient.setQueryData(
            ClusterElementDefinitionKeys.clusterElementDefinition({
                clusterElementName: 'gpt4',
                clusterElementType: 'MODEL',
                componentName: 'openAi',
                componentVersion: 1,
            }),
            {properties: []}
        );

        const user = userEvent.setup();

        renderOperationList('model', queryClient);

        await user.click(screen.getByText('GPT-4'));

        await waitFor(() => {
            expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);
        });

        expect(saveWorkflowDefinitionMock.mock.calls[0][0].nodeData.workflowNodeName).toBe('aiAgent_1');
    });

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

describe('WorkflowNodesPopoverMenuOperationList with a trigger cluster root', () => {
    const deepgramDefinition = {
        actions: [],
        clusterElement: true,
        clusterElements: [
            {
                description: 'A realtime voice agent',
                name: 'voiceAgent',
                title: 'Deepgram Voice Agent',
                type: 'VOICE_AGENT',
            },
        ],
        clusterRoot: false,
        icon: '<svg/>',
        name: 'deepgram',
        title: 'Deepgram',
        triggers: [],
        version: 1,
    } as unknown as ComponentDefinition;

    beforeEach(() => {
        editorStoreState.clusterRootComponentDefinitions = {
            trigger_1: {
                clusterElementTypes: [
                    {label: 'Voice Agent', multipleElements: false, name: 'VOICE_AGENT'},
                    {label: 'Tools', multipleElements: true, name: 'TOOLS'},
                ],
                name: 'browser',
                version: 1,
            },
        };
        editorStoreState.rootClusterElementNodeData = undefined;
        workflowDataStoreState.definition = JSON.stringify({
            tasks: [],
            triggers: [{clusterElements: {}, name: 'trigger_1', parameters: {}, type: 'browser/v1/voiceSession'}],
        });
        workflowDataStoreState.nodes = [
            {
                data: {clusterRoot: true, componentName: 'browser', trigger: true, workflowNodeName: 'trigger_1'},
                id: 'trigger_1',
            },
        ];
        saveWorkflowDefinitionMock.mockReset();
    });

    it('adds a voice agent to the Voice Agent slot of the trigger it hangs off', async () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(
            ClusterElementDefinitionKeys.clusterElementDefinition({
                clusterElementName: 'voiceAgent',
                clusterElementType: 'VOICE_AGENT',
                componentName: 'deepgram',
                componentVersion: 1,
            }),
            {properties: []}
        );

        const user = userEvent.setup();

        render(
            <QueryClientProvider client={queryClient}>
                <WorkflowNodesPopoverMenuOperationList
                    clusterElementType="voiceAgent"
                    componentDefinition={deepgramDefinition}
                    setPopoverOpen={vi.fn()}
                    sourceNodeId="trigger_1"
                />
            </QueryClientProvider>
        );

        await user.click(screen.getByText('Deepgram Voice Agent'));

        await waitFor(() => {
            expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);
        });

        const {nodeData} = saveWorkflowDefinitionMock.mock.calls[0][0];

        expect(nodeData.workflowNodeName).toBe('trigger_1');
        expect(nodeData.clusterElements.voiceAgent.type).toBe('deepgram/v1/voiceAgent');
        expect(nodeData.clusterElements.tools).toEqual([]);
    });
});
