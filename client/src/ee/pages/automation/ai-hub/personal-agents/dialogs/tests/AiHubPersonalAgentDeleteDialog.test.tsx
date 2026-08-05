import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubPersonalAgentI} from '../../hooks/useAiHubPersonalAgents';
import AiHubPersonalAgentDeleteDialog from '../AiHubPersonalAgentDeleteDialog';

vi.mock('@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents', () => ({
    useDeleteAiHubPersonalAgentMutation: vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {useDeleteAiHubPersonalAgentMutation} =
    await import('@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents');
const {toast} = await import('sonner');

const mockUseDeleteMutation = vi.mocked(useDeleteAiHubPersonalAgentMutation);

const makeAgent = (overrides: Partial<AiHubPersonalAgentI> = {}): AiHubPersonalAgentI => ({
    createdAt: '2026-05-01T00:00:00Z',
    description: null,
    environmentId: 0,
    id: 42,
    instructions: null,
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
            {ui}
        </QueryClientProvider>
    );

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const makeMockMutation = (overrides: Record<string, unknown> = {}): any => ({
    isError: false,
    isIdle: true,
    isPending: false,
    isSuccess: false,
    mutate: vi.fn(),
    mutateAsync: vi.fn().mockResolvedValue(undefined),
    reset: vi.fn(),
    status: 'idle' as const,
    ...overrides,
});

beforeEach(() => {
    mockUseDeleteMutation.mockReturnValue(makeMockMutation());

    vi.mocked(toast.success).mockClear();
    vi.mocked(toast.error).mockClear();
});

describe('AiHubPersonalAgentDeleteDialog', () => {
    it('renders the agent title in the confirmation copy', () => {
        wrap(<AiHubPersonalAgentDeleteDialog agent={makeAgent()} onOpenChange={vi.fn()} open={true} />);

        // The user needs to know which agent is about to be deleted; rendering the title (or fallback name)
        // in the confirmation message is the load-bearing pin here.
        expect(screen.getByText(/Research Assistant/)).toBeInTheDocument();
        expect(screen.getByText(/Past tasks remain visible after deletion/i)).toBeInTheDocument();
    });

    it('falls back to the slug name when the agent has no title', () => {
        wrap(<AiHubPersonalAgentDeleteDialog agent={makeAgent({title: null})} onOpenChange={vi.fn()} open={true} />);

        // No title set — show the slug as the fallback. The user wouldn't know which agent they're deleting
        // if neither title nor name surfaced in the copy.
        expect(screen.getByText(/research-bot/)).toBeInTheDocument();
    });

    it('calls deleteAiHubPersonalAgent and closes on confirm', async () => {
        const mutateAsync = vi.fn().mockResolvedValue(undefined);
        const onOpenChange = vi.fn();

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(<AiHubPersonalAgentDeleteDialog agent={makeAgent()} onOpenChange={onOpenChange} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: /^Delete$/}));

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({id: '42', workspaceId: '7'});
        });

        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('does not close the dialog when the mutation fails', async () => {
        const mutateAsync = vi.fn().mockRejectedValue(new Error('forbidden'));
        const onOpenChange = vi.fn();

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(<AiHubPersonalAgentDeleteDialog agent={makeAgent()} onOpenChange={onOpenChange} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: /^Delete$/}));

        // On failure the dialog stays open so the user can retry or cancel. Without the toast + open-state
        // preservation a flaky network would silently dismiss the dialog and the user wouldn't know the
        // delete failed.
        await waitFor(() => {
            expect(toast.error).toHaveBeenCalledWith('forbidden');
        });

        expect(onOpenChange).not.toHaveBeenCalledWith(false);
    });

    it('disables both buttons during the mutation', () => {
        mockUseDeleteMutation.mockReturnValue(makeMockMutation({isPending: true}));

        wrap(<AiHubPersonalAgentDeleteDialog agent={makeAgent()} onOpenChange={vi.fn()} open={true} />);

        // While pending, neither Cancel nor Delete should fire — a double-click during the round-trip would
        // otherwise issue two delete mutations or close the dialog mid-flight.
        expect(screen.getByRole('button', {name: /Cancel/i})).toBeDisabled();
        expect(screen.getByRole('button', {name: /Deleting/i})).toBeDisabled();
    });
});
