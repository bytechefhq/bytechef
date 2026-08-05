import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubPersonalAgentsList from '../AiHubPersonalAgentsList';
import {AiHubPersonalAgentI} from '../hooks/useAiHubPersonalAgents';

// vi.hoisted is the only place top-level variables can live and still be referenced inside vi.mock factories
// — vi.mock calls hoist to the top of the file, BEFORE module-scope `const` declarations execute. Without
// hoisted refs the factories crash at "Cannot access X before initialization" on every test run.
const {navigateMock, setCurrentTaskIdMock, setStateMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    setCurrentTaskIdMock: vi.fn(),
    setStateMock: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}));

vi.mock('@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents', () => ({
    // AiHubPersonalAgentsList renders AiHubPersonalAgentDeleteDialog inline; create + edit moved to a standalone form
    // route, but the create / update mutation hooks may still be referenced by adjacent components in the
    // tree. Stub all three with idle pending=false defaults so nothing crashes — this test targets the list
    // flow, not the dialog/form internals.
    useAiHubPersonalAgentsQuery: vi.fn(),
    useCreateAiHubPersonalAgentMutation: vi.fn().mockReturnValue({
        isPending: false,
        mutateAsync: vi.fn(),
    }),
    useDeleteAiHubPersonalAgentMutation: vi.fn().mockReturnValue({
        isPending: false,
        mutateAsync: vi.fn(),
    }),
    useUpdateAiHubPersonalAgentMutation: vi.fn().mockReturnValue({
        isPending: false,
        mutateAsync: vi.fn(),
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiHubPersonalAgentTaskMutation: vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 0})),
}));

vi.mock('@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore', () => ({
    aiHubTasksStore: {
        getState: () => ({
            setCurrentTaskId: setCurrentTaskIdMock,
        }),
    },
}));

vi.mock('@/ee/pages/automation/ai-hub/stores/useAiHubStore', () => ({
    aiHubStore: {setState: setStateMock},
}));

const {useAiHubPersonalAgentsQuery} =
    await import('@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents');
const {useCreateAiHubPersonalAgentTaskMutation} = await import('@/shared/middleware/graphql');

const mockUseAiHubPersonalAgents = vi.mocked(useAiHubPersonalAgentsQuery);
const mockUseCreateTask = vi.mocked(useCreateAiHubPersonalAgentTaskMutation);

const makeAgent = (overrides: Partial<AiHubPersonalAgentI> = {}): AiHubPersonalAgentI => ({
    createdAt: '2026-05-01T00:00:00Z',
    description: 'Helps with literature reviews',
    environmentId: 0,
    id: 42,
    instructions: 'Always cite sources.',
    llmModel: null,
    llmProvider: null,
    name: 'research-bot',
    resources: [],
    schedule: null,
    title: 'Research Assistant',
    tools: [],
    updatedAt: '2026-05-01T00:00:00Z',
    userId: 3,
    workspaceId: 7,
    ...overrides,
});

const wrap = (ui: ReactNode) =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const queryResult = (data: AiHubPersonalAgentI[] | undefined, isLoading = false): any => ({
    data,
    error: null,
    isError: false,
    isLoading,
    isSuccess: !isLoading,
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mutationResult = (overrides: Record<string, unknown> = {}): any => ({
    isError: false,
    isIdle: true,
    isPending: false,
    isSuccess: false,
    mutate: vi.fn(),
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    status: 'idle' as const,
    ...overrides,
});

beforeEach(() => {
    navigateMock.mockReset();
    setCurrentTaskIdMock.mockReset();
    setStateMock.mockReset();

    mockUseAiHubPersonalAgents.mockReturnValue(queryResult([]));
    mockUseCreateTask.mockReturnValue(mutationResult());
});

describe('AiHubPersonalAgentsList', () => {
    it('renders the empty-state CTA when no agents exist', () => {
        mockUseAiHubPersonalAgents.mockReturnValue(queryResult([]));

        wrap(<AiHubPersonalAgentsList onOpenCreateDialog={vi.fn()} />);

        // Full-page empty state — bigger headline + a CTA button. The dashed border has been removed at
        // the user's request; the empty state now sits directly on the page background. The dialog itself
        // lives in the parent {@code AiHubPersonalAgents} page now, so this test only asserts the CTA renders.
        expect(screen.getByText('No personal agents yet')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /Create your first agent/i})).toBeInTheDocument();
    });

    it('lists agents with their title', () => {
        mockUseAiHubPersonalAgents.mockReturnValue(
            queryResult([makeAgent({id: 1, title: 'Research'}), makeAgent({id: 2, title: 'Code Review'})])
        );

        wrap(<AiHubPersonalAgentsList onOpenCreateDialog={vi.fn()} />);

        expect(screen.getByText('Research')).toBeInTheDocument();
        expect(screen.getByText('Code Review')).toBeInTheDocument();
    });

    it('falls back to the slug name when title is null', () => {
        mockUseAiHubPersonalAgents.mockReturnValue(queryResult([makeAgent({name: 'no-title-bot', title: null})]));

        wrap(<AiHubPersonalAgentsList onOpenCreateDialog={vi.fn()} />);

        // Without title fallback, agents created without a display title render as a blank row in the
        // sidebar — visually broken and unclickable. Pin the slug fallback explicitly.
        expect(screen.getByText('no-title-bot')).toBeInTheDocument();
    });

    it('invokes onOpenCreateDialog when the empty-state CTA is clicked', async () => {
        // The "New Agent" page-header button moved out of this component into the parent
        // {@code AiHubPersonalAgents} page (where it conditionally renders only when agents exist). The empty
        // state's "Create your first agent" button is the only create entry this list still owns. Both
        // triggers funnel through the same {@code onOpenCreateDialog} callback, so verifying the empty-
        // state path also pins the contract the parent depends on.
        mockUseAiHubPersonalAgents.mockReturnValue(queryResult([]));

        const onOpenCreateDialog = vi.fn();

        wrap(<AiHubPersonalAgentsList onOpenCreateDialog={onOpenCreateDialog} />);

        await userEvent.click(screen.getByRole('button', {name: /Create your first agent/i}));

        expect(onOpenCreateDialog).toHaveBeenCalledTimes(1);
    });

    it('navigates to the task when an agent row is clicked', async () => {
        const mutateAsync = vi.fn().mockResolvedValue({
            createAiHubPersonalAgentTask: {
                id: '101',
                threadId: '00000000-0000-0000-0000-000000000042',
                title: 'Research Assistant',
            },
        });

        mockUseCreateTask.mockReturnValue(mutationResult({mutateAsync}));
        mockUseAiHubPersonalAgents.mockReturnValue(queryResult([makeAgent()]));

        wrap(<AiHubPersonalAgentsList onOpenCreateDialog={vi.fn()} />);

        await userEvent.click(screen.getByText('Research Assistant'));

        // Three side effects must fire on row click — the same set the runtime-provider tab-switch test
        // pins for the LLM-driven path. Both entry points (sidebar click + LLM tool) MUST produce identical
        // client state.
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({
                input: {
                    aiHubPersonalAgentId: '42',
                    environment: 0,
                    title: 'Research Assistant',
                    workspaceId: '7',
                },
            });
        });

        expect(setStateMock).toHaveBeenCalledWith({
            messages: [],
            taskId: '00000000-0000-0000-0000-000000000042',
        });
        expect(setCurrentTaskIdMock).toHaveBeenCalledWith(101);
        expect(navigateMock).toHaveBeenCalledWith('/automation/ai-hub/tasks/101');
    });
});
