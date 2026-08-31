import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubHomePanel from '@/ee/pages/automation/ai-hub/AiHubHomePanel';
import {render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {createAgentChatMutateAsyncMock, createWorkflowChatMutateAsyncMock, navigateMock} = vi.hoisted(() => ({
    createAgentChatMutateAsyncMock: vi.fn(),
    createWorkflowChatMutateAsyncMock: vi.fn(),
    navigateMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiDefaultModelQuery: () => ({
        data: {aiDefaultModel: {model: 'gpt-4o', provider: 'ai.provider.openAi'}},
        isPending: false,
    }),
    useAiProviderCatalogQuery: () => ({data: {aiProviderCatalog: []}}),
    useCreateAgentChatAiHubChatMutation: () => ({mutateAsync: createAgentChatMutateAsyncMock}),
    useCreateWorkflowChatAiHubChatMutation: () => ({mutateAsync: createWorkflowChatMutateAsyncMock}),
    useWorkspaceChatAgentsQuery: () => ({
        data: {
            workspaceChatAgents: [
                {
                    agentName: 'support-agent',
                    agentTitle: 'Support Agent',
                    aiAgentId: '5',
                    projectDeploymentId: '42',
                    workflowExecutionId: 'exec-agent-1',
                    workflowLabel: 'Agent1',
                },
            ],
        },
    }),
    useWorkspaceChatWorkflowsQuery: () => ({data: {workspaceChatWorkflows: []}}),
}));

// The composer is a heavy assistant-ui surface; only its `modelPicker` slot matters here, so render
// that slot alone and keep the test focused on the picker -> create-chat wiring.
vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubChatComposer', () => ({
    default: ({modelPicker}: {modelPicker: React.ReactNode}) => <div>{modelPicker}</div>,
}));
vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips', () => ({default: () => null}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));
vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

describe('AiHubHomePanel Agents cascade', () => {
    beforeEach(() => {
        windowResizeObserver();

        navigateMock.mockReset();
        createWorkflowChatMutateAsyncMock.mockReset();
        createAgentChatMutateAsyncMock.mockReset();
        createAgentChatMutateAsyncMock.mockResolvedValue({
            createAgentChatAiHubChat: {id: 77, threadId: 'thread-77'},
        });

        localStorage.clear();
    });

    // The Agents cascade must go through createAgentChatAiHubChat, not the workflow-chat mutation: the two differ
    // only in the kind they stamp, and stamping WORKFLOW_CHAT here would silently undo the agent labelling.
    it('creates an agent chat titled with the agent title and navigates to it', async () => {
        render(
            <TooltipProvider>
                <AiHubHomePanel />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));
        await userEvent.click(screen.getByText('Agents'));
        await userEvent.click(await screen.findByText('Support Agent'));

        expect(createAgentChatMutateAsyncMock).toHaveBeenCalledWith({
            environment: 1,
            projectDeploymentId: '42',
            title: 'Support Agent',
            workflowExecutionId: 'exec-agent-1',
            workspaceId: '1',
        });

        expect(createWorkflowChatMutateAsyncMock).not.toHaveBeenCalled();

        expect(navigateMock).toHaveBeenCalledWith('/automation/ai-hub/chats/77');
    });
});
