import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiAutoMemoryI} from '../../hooks/useAiAutoMemories';
import MemoryDeleteDialog from '../MemoryDeleteDialog';

vi.mock('@/pages/automation/ai/memories/hooks/useAiAutoMemories', () => ({
    useDeleteAiAutoMemoryMutation: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {useDeleteAiAutoMemoryMutation} = await import('@/pages/automation/ai/memories/hooks/useAiAutoMemories');
const {toast} = await import('sonner');

const mockUseDeleteMutation = vi.mocked(useDeleteAiAutoMemoryMutation);

const makeMemory = (overrides: Partial<AiAutoMemoryI> = {}): AiAutoMemoryI => ({
    content: 'Alice prefers concise replies.',
    createdAt: '2026-04-01T00:00:00Z',
    description: 'User profile',
    environmentId: 0,
    id: 1,
    memoryType: 'USER',
    name: 'user_profile',
    principalId: 42,
    principalType: 'USER',
    title: 'User profile',
    updatedAt: '2026-04-10T00:00:00Z',
    workspaceId: 1,
    ...overrides,
});

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

const wrap = (ui: ReactNode) =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            {ui}
        </QueryClientProvider>
    );

beforeEach(() => {
    mockUseDeleteMutation.mockReturnValue(makeMockMutation());

    vi.mocked(toast.success).mockClear();
    vi.mocked(toast.error).mockClear();
});

describe('MemoryDeleteDialog', () => {
    it('renders the warning copy with the memory title', () => {
        wrap(
            <MemoryDeleteDialog environmentId={0} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />
        );

        expect(screen.getByRole('heading', {name: /delete this memory permanently\?/i})).toBeInTheDocument();
        expect(screen.getByText(/"User profile"/)).toBeInTheDocument();
        expect(screen.getByText(/bypass the 30-minute agent-undo window/i)).toBeInTheDocument();
    });

    it('invokes the delete mutation and closes on confirm', async () => {
        const mutateAsync = vi.fn().mockResolvedValue(undefined);
        const onClose = vi.fn();

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(
            <MemoryDeleteDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        await userEvent.click(screen.getByRole('button', {name: /delete permanently/i}));

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({
                environment: 2,
                id: '1',
                principalId: 42,
                principalType: 'USER',
                workspaceId: '7',
            });
        });

        expect(toast.success).toHaveBeenCalledWith('Memory "User profile" deleted');
        expect(onClose).toHaveBeenCalled();
    });

    it('sends the row own principal pair when the memory is deployment-owned', async () => {
        const mutateAsync = vi.fn().mockResolvedValue(undefined);

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(
            <MemoryDeleteDialog
                environmentId={2}
                memory={makeMemory({principalId: 9, principalType: 'PROJECT_DEPLOYMENT'})}
                onClose={vi.fn()}
                open={true}
                workspaceId={7}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: /delete permanently/i}));

        await waitFor(() => {
            // Without the pair the server resolves the CALLER's principal and answers NotFound, so a
            // deployment-owned delete would fail even for an admin. Both must travel, never one.
            expect(mutateAsync).toHaveBeenCalledWith({
                environment: 2,
                id: '1',
                principalId: 9,
                principalType: 'PROJECT_DEPLOYMENT',
                workspaceId: '7',
            });
        });
    });

    it('shows an error toast when the delete mutation fails', async () => {
        const mutateAsync = vi.fn().mockRejectedValue(new Error('boom'));

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(
            <MemoryDeleteDialog environmentId={2} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={7} />
        );

        await userEvent.click(screen.getByRole('button', {name: /delete permanently/i}));

        await waitFor(() => {
            expect(toast.error).toHaveBeenCalledWith('boom');
        });
    });

    it('calls onClose when Cancel is clicked', async () => {
        const onClose = vi.fn();

        wrap(
            <MemoryDeleteDialog environmentId={0} memory={makeMemory()} onClose={onClose} open={true} workspaceId={1} />
        );

        await userEvent.click(screen.getByRole('button', {name: /cancel/i}));

        expect(onClose).toHaveBeenCalled();
    });
});
