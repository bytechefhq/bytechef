import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiAutoMemoryI} from '../../hooks/useAiAutoMemories';
import MemoryEditDialog from '../MemoryEditDialog';

// Partial mock: only the mutation hook is stubbed. The memory-type constants are plain data derived from the
// generated enum and feed the Type select.
vi.mock('@/pages/automation/ai/memories/hooks/useAiAutoMemories', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/pages/automation/ai/memories/hooks/useAiAutoMemories')>()),
    useUpdateAiAutoMemoryMutation: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {useUpdateAiAutoMemoryMutation} = await import('@/pages/automation/ai/memories/hooks/useAiAutoMemories');
const {toast} = await import('sonner');

const mockUseUpdateMutation = vi.mocked(useUpdateAiAutoMemoryMutation);

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
    mutateAsync: vi.fn().mockResolvedValue(makeMemory()),
    reset: vi.fn(),
    status: 'idle' as const,
    ...overrides,
});

beforeEach(() => {
    mockUseUpdateMutation.mockReturnValue(makeMockMutation());

    vi.mocked(toast.success).mockClear();
    vi.mocked(toast.error).mockClear();
});

describe('MemoryEditDialog', () => {
    it('pre-fills the form with the current memory values', () => {
        wrap(
            <MemoryEditDialog environmentId={0} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />
        );

        expect(screen.getByLabelText('Name')).toHaveValue('user_profile');
        expect(screen.getByLabelText('Name')).toBeDisabled();
        expect(screen.getByLabelText('Title')).toHaveValue('User profile');
        expect(screen.getByLabelText('Description')).toHaveValue('User profile');
        expect(screen.getByLabelText(/content/i)).toHaveValue('Alice prefers concise replies.');
    });

    it('invokes the mutation with only changed fields and closes on success', async () => {
        const mutateAsync = vi.fn().mockResolvedValue(makeMemory());
        const onClose = vi.fn();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(
            <MemoryEditDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        const titleInput = screen.getByLabelText('Title');

        await userEvent.clear(titleInput);
        await userEvent.type(titleInput, 'Updated');

        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({
                input: {
                    content: undefined,
                    description: undefined,
                    environment: 2,
                    id: '1',
                    memoryType: undefined,
                    principalId: 42,
                    principalType: 'USER',
                    title: 'Updated',
                    workspaceId: '7',
                },
            });
        });

        expect(toast.success).toHaveBeenCalledWith('Memory updated');
        expect(onClose).toHaveBeenCalled();
    });

    it('sends the row own principal pair when the memory is deployment-owned', async () => {
        const mutateAsync = vi.fn().mockResolvedValue(makeMemory());

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(
            <MemoryEditDialog
                environmentId={2}
                memory={makeMemory({principalId: 9, principalType: 'PROJECT_DEPLOYMENT'})}
                onClose={vi.fn()}
                open={true}
                workspaceId={7}
            />
        );

        await userEvent.clear(screen.getByLabelText('Title'));
        await userEvent.type(screen.getByLabelText('Title'), 'Updated');

        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => {
            const input = mutateAsync.mock.lastCall?.[0].input;

            // Without the pair the server resolves the CALLER's principal and answers NotFound, so a
            // deployment-owned edit would fail even for an admin. Both must travel, never one.
            expect(input.principalType).toBe('PROJECT_DEPLOYMENT');
            expect(input.principalId).toBe(9);
        });
    });

    it('closes without calling the mutation when nothing changed', async () => {
        const mutateAsync = vi.fn();
        const onClose = vi.fn();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutateAsync}));

        wrap(
            <MemoryEditDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        expect(mutateAsync).not.toHaveBeenCalled();
        expect(onClose).toHaveBeenCalled();
    });

    it('disables Save when the title is empty', async () => {
        wrap(
            <MemoryEditDialog environmentId={0} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />
        );

        await userEvent.clear(screen.getByLabelText('Title'));

        expect(screen.getByRole('button', {name: /save/i})).toBeDisabled();
    });

    it('calls onClose when Cancel is clicked', async () => {
        const onClose = vi.fn();

        wrap(
            <MemoryEditDialog environmentId={0} memory={makeMemory()} onClose={onClose} open={true} workspaceId={1} />
        );

        await userEvent.click(screen.getByRole('button', {name: /cancel/i}));

        expect(onClose).toHaveBeenCalled();
    });
});
