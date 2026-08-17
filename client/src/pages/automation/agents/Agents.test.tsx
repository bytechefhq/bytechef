import {TooltipProvider} from '@/components/ui/tooltip';
import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Agents from './Agents';

// vi.hoisted is the only place top-level variables can live and still be referenced inside vi.mock
// factories — vi.mock calls hoist to the top of the file, BEFORE module-scope `const` declarations
// execute. Without hoisted refs the factories crash at "Cannot access X before initialization".
const {
    copilotEnabled,
    mockInvalidateAgentQueries,
    mockRegisterPostTurn,
    mockSetContext,
    mockSetCopilotPanelOpen,
    navigateMock,
} = vi.hoisted(() => ({
    copilotEnabled: {value: true},
    mockInvalidateAgentQueries: vi.fn(),
    mockRegisterPostTurn: vi.fn(),
    mockSetContext: vi.fn(),
    mockSetCopilotPanelOpen: vi.fn(),
    navigateMock: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
    useSearchParams: () => [new URLSearchParams(), vi.fn()],
}));

vi.mock('@/pages/automation/agents/utils/invalidateAgentQueries', () => ({
    default: mockInvalidateAgentQueries,
}));

// Stubbed rather than rendered: the sidebar nav pulls in react-router-dom's Link, which this file's
// router mock deliberately does not provide, and none of these tests assert on it.
vi.mock('@/pages/automation/agents/components/AgentsLeftSidebarNav', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', async () => {
    const actual = await vi.importActual<typeof import('@/shared/components/copilot/stores/useCopilotStore')>(
        '@/shared/components/copilot/stores/useCopilotStore'
    );

    return {
        ...actual,
        useCopilotStore: (selector: (state: {setContext: typeof mockSetContext}) => unknown) =>
            selector({setContext: mockSetContext}),
    };
});

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (selector: (state: {setCopilotPanelOpen: typeof mockSetCopilotPanelOpen}) => unknown) =>
        selector({setCopilotPanelOpen: mockSetCopilotPanelOpen}),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPostTurnRegistry', () => ({
    default: (selector: (state: {register: typeof mockRegisterPostTurn}) => unknown) =>
        selector({register: mockRegisterPostTurn}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: copilotEnabled.value}}}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

// The row's Deploy button imports ProjectDeploymentDialog, which transitively reaches ConnectionDialog and
// a chain of module-scope enum reads off the graphql module this file mocks wholesale. These tests are
// about the agent list, not the deploy flow, so the dialog is stubbed rather than each enum re-declared.
vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => <div data-testid="project-deployment-dialog" />,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentTagsQuery: vi.fn().mockReturnValue({data: {aiAgentTags: []}}),
    useAiAgentsQuery: vi.fn(),
    useCreateAiAgentMutation: vi.fn(),
    useDeleteAiAgentMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
    useImportAiAgentMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
    usePublishAiAgentMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
    useUpdateAiAgentMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
    useUpdateAiAgentTagsMutation: vi.fn().mockReturnValue({isPending: false, mutate: vi.fn()}),
}));

const {useAiAgentsQuery, useCreateAiAgentMutation} = await import('@/shared/middleware/graphql');

const mockUseAiAgentsQuery = vi.mocked(useAiAgentsQuery);
const mockUseCreateAiAgentMutation = vi.mocked(useCreateAiAgentMutation);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const queryResult = (data: unknown, isLoading = false): any => ({
    data,
    error: null,
    isLoading,
});

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    // Spy on the real QueryClient instance so `invalidateQueries({queryKey: ['agents']})` calls made
    // deep inside AgentDialog's onSuccess handler are observable without mocking `@tanstack/react-query`
    // itself (which would also break the Dialog/Form components rendered in the tree).
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    // TooltipProvider: the row's TagList renders Radix Tooltips, which throw outside a provider. The app
    // mounts one globally in main.tsx, so this only stands in for that.
    const view = render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );

    return {invalidateQueriesSpy, ...view};
};

beforeEach(() => {
    copilotEnabled.value = true;

    navigateMock.mockReset();
    mockInvalidateAgentQueries.mockReset();
    mockRegisterPostTurn.mockReset();
    mockSetContext.mockReset();
    mockSetCopilotPanelOpen.mockReset();
    mockUseAiAgentsQuery.mockReset();
    mockUseCreateAiAgentMutation.mockReset();
});

describe('Agents', () => {
    it('shows the loading state without rendering the agent list or empty state', () => {
        mockUseAiAgentsQuery.mockReturnValue(queryResult(undefined, true));
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseCreateAiAgentMutation.mockReturnValue({isPending: false, mutate: vi.fn()} as any);

        wrap(<Agents />);

        expect(screen.queryByText('No Agents')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Agent'})).not.toBeInTheDocument();
    });

    it('maps queried agents into list rows', () => {
        mockUseAiAgentsQuery.mockReturnValue(
            queryResult({
                aiAgents: [
                    {
                        description: 'Handles refunds',
                        id: '1',
                        lastPublishedVersion: 2,
                        title: 'Refund Agent',
                        unpublishedChanges: false,
                    },
                    {
                        description: null,
                        id: '2',
                        lastPublishedVersion: 0,
                        title: 'Draft Agent',
                        unpublishedChanges: true,
                    },
                ],
            })
        );
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseCreateAiAgentMutation.mockReturnValue({isPending: false, mutate: vi.fn()} as any);

        wrap(<Agents />);

        expect(screen.getByText('Refund Agent')).toBeInTheDocument();
        expect(screen.getByText('Handles refunds')).toBeInTheDocument();
        expect(screen.getByText('Draft Agent')).toBeInTheDocument();

        // A published agent with no pending edits reads at its published version; a never-published one is
        // V1 DRAFT rather than V0, since the draft is always the version after the last published one.
        expect(screen.getByText('V2')).toBeInTheDocument();
        expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
        expect(screen.getByText('V1')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();
        expect(screen.getAllByText('Not yet published')).toHaveLength(2);
    });

    it('fires the create mutation and invalidates the agents list on success', async () => {
        mockUseAiAgentsQuery.mockReturnValue(queryResult({aiAgents: []}));

        const mutate = vi.fn((_variables, options) => {
            options.onSuccess({createAiAgent: {id: '99'}});
        });

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseCreateAiAgentMutation.mockReturnValue({isPending: false, mutate} as any);

        const {invalidateQueriesSpy} = wrap(<Agents />);

        await userEvent.click(screen.getByRole('button', {name: 'Create Agent'}));
        await userEvent.type(screen.getByLabelText('Title'), 'Support Agent');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutate).toHaveBeenCalledWith(
            {input: {description: '', title: 'Support Agent', workspaceId: '7'}},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['aiAgents']});
        expect(navigateMock).toHaveBeenCalledWith('/automation/agents/99');
    });

    it('opens the copilot panel with an agentId-less AI_AGENT context', async () => {
        mockUseAiAgentsQuery.mockReturnValue(queryResult({aiAgents: []}));
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseCreateAiAgentMutation.mockReturnValue({isPending: false, mutate: vi.fn()} as any);

        wrap(<Agents />);

        await userEvent.click(screen.getByRole('button', {name: 'Ask Copilot'}));

        expect(mockSetContext).toHaveBeenCalledWith({
            mode: MODE.ASK,
            parameters: {},
            source: Source.AI_AGENT,
        });
        expect(mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });

    it('hides the copilot trigger when the copilot is disabled', () => {
        copilotEnabled.value = false;

        mockUseAiAgentsQuery.mockReturnValue(queryResult({aiAgents: []}));
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseCreateAiAgentMutation.mockReturnValue({isPending: false, mutate: vi.fn()} as any);

        wrap(<Agents />);

        expect(screen.queryByRole('button', {name: 'Ask Copilot'})).not.toBeInTheDocument();
    });

    it('registers agent query invalidation for post-turn copilot refreshes', () => {
        mockUseAiAgentsQuery.mockReturnValue(queryResult({aiAgents: []}));
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        mockUseCreateAiAgentMutation.mockReturnValue({isPending: false, mutate: vi.fn()} as any);

        wrap(<Agents />);

        expect(mockRegisterPostTurn).toHaveBeenCalledWith(Source.AI_AGENT, expect.any(Function));

        const [, refresh] = mockRegisterPostTurn.mock.calls[0];

        refresh();

        expect(mockInvalidateAgentQueries).toHaveBeenCalled();
    });
});
