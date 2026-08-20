import AgentVisibilityDialog from '@/pages/automation/agents/components/detail/AgentVisibilityDialog';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {setAiAgentVisibilityMutateMock, useVisibilityFeatureEnabledMock} = vi.hoisted(() => ({
    setAiAgentVisibilityMutateMock: vi.fn(),
    useVisibilityFeatureEnabledMock: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: useVisibilityFeatureEnabledMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ResourceVisibility: {Organization: 'ORGANIZATION', Private: 'PRIVATE', Workspace: 'WORKSPACE'},
    useAiAgentGrantsQuery: () => ({data: {aiAgentGrants: []}}),
    useGrantAiAgentAccessMutation: () => ({mutate: vi.fn()}),
    useRevokeAiAgentAccessMutation: () => ({mutate: vi.fn()}),
    useSetAiAgentVisibilityMutation: () => ({mutate: setAiAgentVisibilityMutateMock}),
    useWorkspaceUsersQuery: () => ({data: {workspaceUsers: []}}),
}));

let queryClient: QueryClient;

const Wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

beforeEach(() => {
    vi.clearAllMocks();

    queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    useVisibilityFeatureEnabledMock.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 7});
});

describe('AgentVisibilityDialog', () => {
    it('sends the picked reach keyed on the agent, with no workspaceId — the server reads it off the agent', async () => {
        render(<AgentVisibilityDialog agentId="5" onClose={vi.fn()} visibility="WORKSPACE" />, {wrapper: Wrapper});

        await userEvent.click(screen.getByLabelText('Private'));

        expect(setAiAgentVisibilityMutateMock).toHaveBeenCalledWith({agentId: '5', visibility: 'PRIVATE'});
    });

    /**
     * The caveat is the point of this dialog existing separately from the project one, so it is asserted rather
     * than left to review. Visibility governs management surfaces only; a user who reads "private" as "stop it
     * talking to people" has been misled, and this text is the only thing in the product that says otherwise.
     */
    it('warns, next to the picker, that a private agent keeps answering every channel', () => {
        render(<AgentVisibilityDialog agentId="5" onClose={vi.fn()} visibility="PRIVATE" />, {wrapper: Wrapper});

        expect(screen.getByText('This does not stop the agent answering')).toBeInTheDocument();
        expect(screen.getByText(/Slack, WhatsApp, its webhooks and in-app chat are unchanged/)).toBeInTheDocument();
    });

    /**
     * Every label speaks of seeing, never of using. A title of "Agent Visibility" or a description mentioning
     * access would be the overpromise the callout above then has to walk back.
     */
    it('names the question as seeing rather than using', () => {
        render(<AgentVisibilityDialog agentId="5" onClose={vi.fn()} visibility="WORKSPACE" />, {wrapper: Wrapper});

        expect(screen.getByText('Who Can See This Agent')).toBeInTheDocument();
        expect(screen.getByText(/finds this agent in their agent and deployment lists/)).toBeInTheDocument();
    });

    it('offers "Specific people", because the agent already exists to grant against', () => {
        render(<AgentVisibilityDialog agentId="5" onClose={vi.fn()} visibility="PRIVATE" />, {wrapper: Wrapper});

        expect(screen.getByLabelText('Specific people')).toBeInTheDocument();
    });

    it('renders no picker, and no caveat, when the visibility feature is off', () => {
        useVisibilityFeatureEnabledMock.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});

        render(<AgentVisibilityDialog agentId="5" onClose={vi.fn()} visibility="WORKSPACE" />, {wrapper: Wrapper});

        // Anchor: the dialog shell rendered, so the absences below are the gate and not a failed render.
        expect(screen.getByText('Who Can See This Agent')).toBeInTheDocument();
        expect(screen.queryByLabelText('Shared with workspace')).not.toBeInTheDocument();
        expect(screen.queryByText('This does not stop the agent answering')).not.toBeInTheDocument();
    });
});
