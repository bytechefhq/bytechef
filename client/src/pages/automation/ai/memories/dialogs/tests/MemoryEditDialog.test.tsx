import {UpdateAiAutoMemoryInput} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
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
    version: 3,
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
    reset: vi.fn(),
    status: 'idle' as const,
    ...overrides,
});

// Stands in for a mutation that settles successfully: TanStack invokes the per-call onSuccess.
const makeSucceedingMutate = () =>
    vi.fn((_variables: {input: UpdateAiAutoMemoryInput}, options?: {onSuccess?: () => void}) => {
        options?.onSuccess?.();
    });

// Stands in for a mutation that fails: the per-call onSuccess never runs. Error display belongs to the global fetch
// interceptor, not to this dialog.
const makeFailingMutate = () => vi.fn();

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

    it('labels the Type select with the memory type label rather than the raw enum value', () => {
        wrap(
            <MemoryEditDialog
                environmentId={0}
                memory={makeMemory({memoryType: 'FEEDBACK'})}
                onClose={vi.fn()}
                open={true}
                workspaceId={1}
            />
        );

        expect(screen.getByRole('combobox', {name: 'Type'})).toHaveTextContent('Feedback');
        expect(screen.queryByText('FEEDBACK')).toBeNull();
    });

    it('invokes the mutation with only changed fields and closes on success', async () => {
        const mutate = makeSucceedingMutate();
        const onClose = vi.fn();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryEditDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        const titleInput = screen.getByLabelText('Title');

        await userEvent.clear(titleInput);
        await userEvent.type(titleInput, 'Updated');

        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => {
            expect(mutate).toHaveBeenCalledWith(
                {
                    input: {
                        content: undefined,
                        description: undefined,
                        environment: 2,
                        expectedVersion: 3,
                        id: '1',
                        memoryType: undefined,
                        principal: {principalId: 42, principalType: 'USER'},
                        title: 'Updated',
                        workspaceId: '7',
                    },
                },
                expect.objectContaining({onSuccess: expect.any(Function)})
            );
        });

        expect(toast.success).toHaveBeenCalledWith('Memory updated');
        expect(onClose).toHaveBeenCalled();
    });

    it('sends the row own principal when the memory is deployment-owned', async () => {
        const mutate = makeSucceedingMutate();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutate}));

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
            const input = mutate.mock.lastCall?.[0].input;

            // Without the principal the server resolves the CALLER's and answers NotFound, so a deployment-owned
            // edit would fail even for an admin.
            expect(input?.principal).toEqual({principalId: 9, principalType: 'PROJECT_DEPLOYMENT'});
        });
    });

    it('closes without calling the mutation when nothing changed', async () => {
        const mutate = vi.fn();
        const onClose = vi.fn();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryEditDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        expect(mutate).not.toHaveBeenCalled();
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

describe('MemoryEditDialog edits and failures', () => {
    it('sends description and content changes', async () => {
        const mutate = makeSucceedingMutate();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryEditDialog environmentId={2} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={7} />
        );

        await userEvent.clear(screen.getByLabelText('Description'));
        await userEvent.type(screen.getByLabelText('Description'), 'Tone preferences');
        await userEvent.clear(screen.getByLabelText(/content/i));
        await userEvent.type(screen.getByLabelText(/content/i), 'Keep replies short.');

        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => {
            const input = mutate.mock.lastCall?.[0].input;

            expect(input?.description).toBe('Tone preferences');
            expect(input?.content).toBe('Keep replies short.');
            expect(input?.title).toBeUndefined();
        });
    });

    it('shows an empty description for a memory without one', () => {
        wrap(
            <MemoryEditDialog
                environmentId={0}
                memory={makeMemory({description: null})}
                onClose={vi.fn()}
                open={true}
                workspaceId={1}
            />
        );

        expect(screen.getByLabelText('Description')).toHaveValue('');
    });

    it('stays open without a toast of its own when the update fails', async () => {
        const mutate = makeFailingMutate();
        const onClose = vi.fn();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryEditDialog environmentId={2} memory={makeMemory()} onClose={onClose} open={true} workspaceId={7} />
        );

        await userEvent.type(screen.getByLabelText('Title'), ' v2');
        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => expect(mutate).toHaveBeenCalled());

        // The global fetch interceptor already toasts the GraphQL error; a second toast here would duplicate it.
        expect(toast.error).not.toHaveBeenCalled();
        expect(toast.success).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
    });

    it('flattens line breaks in a stored title and description without sending them back unedited', async () => {
        const mutate = makeSucceedingMutate();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryEditDialog
                environmentId={2}
                memory={makeMemory({description: 'First line\r\nsecond line', title: 'Line one\nline two'})}
                onClose={vi.fn()}
                open={true}
                workspaceId={7}
            />
        );

        expect(screen.getByLabelText('Title')).toHaveValue('Line one line two');
        expect(screen.getByLabelText('Description')).toHaveValue('First line second line');

        await userEvent.type(screen.getByLabelText(/content/i), ' More.');
        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => {
            const input = mutate.mock.lastCall?.[0].input;

            expect(input?.content).toBe('Alice prefers concise replies. More.');
            expect(input?.description).toBeUndefined();
            expect(input?.title).toBeUndefined();
        });
    });

    it('keeps the title and description single-line, so a pasted line break is never sent', async () => {
        const mutate = makeSucceedingMutate();

        mockUseUpdateMutation.mockReturnValue(makeMockMutation({mutate}));

        wrap(
            <MemoryEditDialog environmentId={2} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={7} />
        );

        fireEvent.change(screen.getByLabelText('Title'), {target: {value: 'New\ntitle'}});
        fireEvent.change(screen.getByLabelText('Description'), {target: {value: 'New\r\ndescription'}});

        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() => {
            const input = mutate.mock.lastCall?.[0].input;

            expect(input?.title).toMatch(/^New\s?title$/);
            expect(input?.description).toMatch(/^New\s?description$/);
        });
    });

    it('shows the saving state while the update is pending', () => {
        mockUseUpdateMutation.mockReturnValue(makeMockMutation({isPending: true}));

        wrap(
            <MemoryEditDialog environmentId={0} memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />
        );

        expect(screen.getByRole('button', {name: 'Saving...'})).toBeDisabled();
    });

    it('stays open with the edits when the memory is deleted meanwhile, and holds Save back', async () => {
        const queryClient = new QueryClient();

        const renderDialog = (memory: AiAutoMemoryI | null, open: boolean) => (
            <QueryClientProvider client={queryClient}>
                <MemoryEditDialog environmentId={0} memory={memory} onClose={vi.fn()} open={open} workspaceId={1} />
            </QueryClientProvider>
        );

        const {rerender} = render(renderDialog(makeMemory(), true));

        await userEvent.clear(screen.getByLabelText('Title'));
        await userEvent.type(screen.getByLabelText('Title'), 'Edited title');

        rerender(renderDialog(null, true));

        expect(screen.getByRole('alert')).toHaveTextContent(/deleted since you opened it/i);
        expect(screen.getByLabelText('Title')).toHaveValue('Edited title');
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        // Closing discards the loaded version, so the next edit starts from the memory as it is then.
        rerender(renderDialog(null, false));
        rerender(renderDialog(makeMemory({version: 5}), true));

        expect(screen.getByLabelText('Title')).toHaveValue('User profile');
        expect(screen.queryByRole('alert')).toBeNull();
    });

    it('calls onClose when the dialog is dismissed with Escape', async () => {
        const onClose = vi.fn();

        wrap(
            <MemoryEditDialog environmentId={0} memory={makeMemory()} onClose={onClose} open={true} workspaceId={1} />
        );

        await userEvent.keyboard('{Escape}');

        expect(onClose).toHaveBeenCalled();
    });
});
