import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ChatsSidebar from '../ChatsSidebar';

const hoisted = vi.hoisted(() => {
    return {
        storeState: {
            chatAgents: [] as Array<{
                agentName: string;
                agentTitle: string;
                aiAgentId: string;
                projectDeploymentId: string;
                workflowExecutionId: string;
                workflowLabel: string;
            }>,
            chatWorkflows: [] as Array<{
                projectDeploymentId: string;
                projectId: string;
                projectName: string;
                projectWorkflowId: string;
                workflowExecutionId: string;
                workflowId: string;
                workflowLabel: string;
            }>,
            currentEnvironmentId: 1,
            currentWorkspaceId: 1,
            isChatAgentsLoading: false,
            isChatWorkflowsLoading: false,
        },
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: hoisted.storeState.currentWorkspaceId}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: hoisted.storeState.currentEnvironmentId}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useWorkspaceChatAgentsQuery: () => ({
        data: {workspaceChatAgents: hoisted.storeState.chatAgents},
        isLoading: hoisted.storeState.isChatAgentsLoading,
    }),
    useWorkspaceChatWorkflowsQuery: () => ({
        data: {workspaceChatWorkflows: hoisted.storeState.chatWorkflows},
        isLoading: hoisted.storeState.isChatWorkflowsLoading,
    }),
}));

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

const renderWithProviders = (initialPath = '/automation/chats') =>
    render(
        <MemoryRouter initialEntries={[initialPath]}>
            <QueryClientProvider client={queryClient}>
                <ChatsSidebar />
            </QueryClientProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    windowResizeObserver();

    hoisted.storeState.chatAgents = [];
    hoisted.storeState.chatWorkflows = [];
    hoisted.storeState.isChatAgentsLoading = false;
    hoisted.storeState.isChatWorkflowsLoading = false;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
    queryClient.clear();
});

describe('ChatsSidebar', () => {
    it('shows the empty state when there are no agents and no chat workflows', () => {
        renderWithProviders();

        expect(screen.getByText('No chats found')).toBeInTheDocument();
    });

    it('renders an Agents group above the project groups, one row per agent', () => {
        hoisted.storeState.chatAgents = [
            {
                agentName: 'support-agent',
                agentTitle: 'Support Agent',
                aiAgentId: '5',
                projectDeploymentId: '42',
                workflowExecutionId: 'exec-agent-1',
                workflowLabel: 'Agent1',
            },
            {
                agentName: 'sales-agent',
                agentTitle: 'Sales Agent',
                aiAgentId: '6',
                projectDeploymentId: '43',
                workflowExecutionId: 'exec-agent-2',
                workflowLabel: 'Agent2',
            },
        ];
        hoisted.storeState.chatWorkflows = [
            {
                projectDeploymentId: '10',
                projectId: 'project-1',
                projectName: 'Project One',
                projectWorkflowId: 'pw-1',
                workflowExecutionId: 'exec-workflow-1',
                workflowId: 'workflow-1',
                workflowLabel: 'Workflow One',
            },
        ];

        renderWithProviders();

        expect(screen.getByText('Agents')).toBeInTheDocument();

        const supportAgentLink = screen.getByText('Support Agent').closest('a');
        const salesAgentLink = screen.getByText('Sales Agent').closest('a');
        const projectOneLink = screen.getByText('Workflow One').closest('a');

        expect(supportAgentLink).toHaveAttribute('href', '/automation/chats/exec-agent-1');
        expect(salesAgentLink).toHaveAttribute('href', '/automation/chats/exec-agent-2');
        expect(projectOneLink).toHaveAttribute('href', '/automation/chats/exec-workflow-1');

        // Agents group heading must appear before the project group heading in DOM order.
        const agentsHeading = screen.getByText('Agents');
        const projectHeading = screen.getByText('Project One');

        expect(agentsHeading.compareDocumentPosition(projectHeading) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('renders the Agents group and suppresses the empty state when there are agents but zero chat workflows', () => {
        hoisted.storeState.chatAgents = [
            {
                agentName: 'support-agent',
                agentTitle: 'Support Agent',
                aiAgentId: '5',
                projectDeploymentId: '42',
                workflowExecutionId: 'exec-agent-1',
                workflowLabel: 'Agent1',
            },
        ];
        hoisted.storeState.chatWorkflows = [];

        renderWithProviders();

        expect(screen.getByText('Agents')).toBeInTheDocument();
        expect(screen.getByText('Support Agent')).toBeInTheDocument();
        expect(screen.queryByText('No chats found')).not.toBeInTheDocument();
    });

    it('shows the skeleton (not the empty state) while the agents query is still loading, even if the workflows query already resolved with zero results', () => {
        hoisted.storeState.chatAgents = [];
        hoisted.storeState.chatWorkflows = [];
        hoisted.storeState.isChatWorkflowsLoading = false;
        hoisted.storeState.isChatAgentsLoading = true;

        const {container} = renderWithProviders();

        expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
        expect(screen.queryByText('No chats found')).not.toBeInTheDocument();
        expect(screen.queryByText('Agents')).not.toBeInTheDocument();
    });
});
