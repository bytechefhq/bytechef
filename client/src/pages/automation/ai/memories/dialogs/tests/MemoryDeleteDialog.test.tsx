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
    version: 3,
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
    reset: vi.fn(),
    status: 'idle' as const,
    ...overrides,
});

const makeSucceedingMutate = () =>
    vi.fn((_variables: unknown, options?: {onSuccess?: () => void}) => {
        options?.onSuccess?.();
    });

const makeFailingMutate = () => vi.fn();

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
        expect(screen.getByText(/the deletion is permanent and cannot be undone/i)).toBeInTheDocument();
        expect(screen.queryByText(/undo window/i)).toBeNull();
    });

    it('invokes the delete mutation and closes on confirm', async () => {
        const mutate = makeSucceedingMutate();
        const onClose = vi.fn();

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryDeleteDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        await userEvent.click(screen.getByRole('button', {name: /delete permanently/i}));

        await waitFor(() => {
            expect(mutate).toHaveBeenCalledWith(
                {
                    environment: 2,
                    id: '1',
                    principal: {principalId: 42, principalType: 'USER'},
                    workspaceId: '7',
                },
                expect.objectContaining({onSuccess: expect.any(Function)})
            );
        });

        expect(toast.success).toHaveBeenCalledWith('Memory "User profile" deleted');
        expect(onClose).toHaveBeenCalled();
    });

    it('sends the row own principal when the memory is deployment-owned', async () => {
        const mutate = makeSucceedingMutate();

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutate}));

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
            expect(mutate.mock.lastCall?.[0]).toEqual({
                environment: 2,
                id: '1',
                principal: {principalId: 9, principalType: 'PROJECT_DEPLOYMENT'},
                workspaceId: '7',
            });
        });
    });

    it('stays open without a toast of its own when the delete mutation fails', async () => {
        const mutate = makeFailingMutate();
        const onClose = vi.fn();

        mockUseDeleteMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryDeleteDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        await userEvent.click(screen.getByRole('button', {name: /delete permanently/i}));

        await waitFor(() => expect(mutate).toHaveBeenCalled());

        expect(toast.error).not.toHaveBeenCalled();
        expect(toast.success).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
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

describe('MemoryDeleteDialog failures and states', () => {
    it('shows the deleting state while the delete is pending', () => {
        mockUseDeleteMutation.mockReturnValue(makeMockMutation({isPending: true}));

        wrap(
            <MemoryDeleteDialog environmentId={0} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />
        );

        expect(screen.getByRole('button', {name: 'Deleting...'})).toBeDisabled();
    });

    it('calls onClose when the dialog is dismissed with Escape', async () => {
        const onClose = vi.fn();

        wrap(
            <MemoryDeleteDialog environmentId={0} memory={makeMemory()} onClose={onClose} open={true} workspaceId={1} />
        );

        await userEvent.keyboard('{Escape}');

        expect(onClose).toHaveBeenCalled();
    });
});
