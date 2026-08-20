import {TooltipProvider} from '@/components/ui/tooltip';
import AgentListItem from '@/pages/automation/agents/components/agent-list/AgentListItem';
import {AiAgent} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// The real useAiAgentVisibility runs here — only ITS dependencies are stood in for. Mocking the hook would
// leave the badge's whole reason for existing (an agent's reach IS its backing project's, edited through the
// agent-keyed operations) untested on this surface.
const {mockUseVisibilityFeatureEnabled, setAiAgentVisibilityMutate} = vi.hoisted(() => ({
    mockUseVisibilityFeatureEnabled: vi.fn(),
    setAiAgentVisibilityMutate: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useIsVisibilityEditionEnabled: () => true,
    useVisibilityFeatureEnabled: mockUseVisibilityFeatureEnabled,
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => <div data-testid="project-deployment-dialog" />,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ResourceVisibility: {Organization: 'ORGANIZATION', Private: 'PRIVATE', Workspace: 'WORKSPACE'},
    useAiAgentGrantsQuery: () => ({data: {aiAgentGrants: ['3']}}),
    useAiAgentTagsQuery: () => ({data: {aiAgentTags: []}}),
    useDeleteAiAgentMutation: () => ({isPending: false, mutate: vi.fn()}),
    useGrantAiAgentAccessMutation: () => ({mutate: vi.fn()}),
    usePublishAiAgentMutation: () => ({isPending: false, mutate: vi.fn()}),
    useRevokeAiAgentAccessMutation: () => ({mutate: vi.fn()}),
    useSetAiAgentVisibilityMutation: () => ({mutate: setAiAgentVisibilityMutate}),
    useUpdateAiAgentTagsMutation: () => ({isPending: false, mutate: vi.fn()}),
    useWorkspaceUsersQuery: () => ({data: {workspaceUsers: []}}),
}));

// The real generated document, past the mock above: the badge reads agent.visibility off a row the LIST query
// produced, and the row is cast to the schema AiAgent type — so a query that stopped selecting the field would
// still typecheck and would silently render every agent as WORKSPACE. Nothing else here would notice.
const {AiAgentsDocument} =
    await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

let queryClient: QueryClient;

const Wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={queryClient}>
        <TooltipProvider>{children}</TooltipProvider>
    </QueryClientProvider>
);

const renderItem = (agent: Partial<AiAgent>) =>
    render(
        <AgentListItem
            agent={
                {
                    channels: [],
                    description: null,
                    elements: [],
                    id: '1',
                    lastPublishedVersion: 0,
                    projectId: '5',
                    tags: [],
                    title: 'Refund Agent',
                    ...agent,
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any
            }
        />,
        {wrapper: Wrapper}
    );

beforeEach(() => {
    queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    setAiAgentVisibilityMutate.mockClear();
});

describe('AgentListItem visibility', () => {
    it('renders the badge in EE, reading the agent own reach', () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 7});

        renderItem({visibility: 'PRIVATE'} as Partial<AiAgent>);

        // "Specific people" rather than "Private": the agent is withheld AND has a grant, which is the state the
        // badge exists to distinguish. Asserting "Private" would pass with grantedUserIds ignored.
        expect(screen.getByText('Specific people')).toBeInTheDocument();
    });

    it('renders no badge in CE', () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});

        renderItem({visibility: 'PRIVATE'} as Partial<AiAgent>);

        // Anchor: proves the row itself rendered, so the absence below is the gate and not a failed render.
        expect(screen.getByText('Refund Agent')).toBeInTheDocument();
        expect(screen.queryByLabelText('Change visibility')).not.toBeInTheDocument();
    });

    it('asks the list query for the field the badge renders', () => {
        expect(String(AiAgentsDocument)).toMatch(/\n\s+visibility\n/);
    });

    it('edits the reach through the agent-keyed operation, not the project one', async () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 7});

        renderItem({visibility: 'PRIVATE'} as Partial<AiAgent>);

        await userEvent.click(screen.getByLabelText('Change visibility'));

        // This dropdown can set an agent PRIVATE, so it carries the same caveat the detail dialog does — a
        // control whose name overpromises is worse than no control, and one that overpromises on only one of
        // the two surfaces that operate it is the same thing.
        expect(screen.getByText('This does not stop the agent answering')).toBeInTheDocument();

        await userEvent.click(screen.getByLabelText('Shared with workspace'));

        // agentId and no workspaceId: the server reads the workspace off the agent. A project-shaped call would
        // carry projectId/workspaceId and reach ProjectSharingFacade past the AGENT_EDIT gate.
        expect(setAiAgentVisibilityMutate).toHaveBeenCalledWith({agentId: '1', visibility: 'WORKSPACE'});
    });
});
