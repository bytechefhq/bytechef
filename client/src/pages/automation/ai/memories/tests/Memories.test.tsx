import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Memories from '../Memories';
import {AiAutoMemoryI} from '../hooks/useAiAutoMemories';

vi.mock('@/pages/automation/ai/memories/hooks/useAiAutoMemories', () => ({
    useAiAutoMemoriesQuery: vi.fn(),
    useDeleteAiAutoMemoryMutation: vi.fn(),
    useUpdateAiAutoMemoryMutation: vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 7})
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 0})
    ),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {useAiAutoMemoriesQuery, useDeleteAiAutoMemoryMutation, useUpdateAiAutoMemoryMutation} =
    await import('@/pages/automation/ai/memories/hooks/useAiAutoMemories');

const mockUseMemoriesQuery = vi.mocked(useAiAutoMemoriesQuery);
const mockUseDeleteMutation = vi.mocked(useDeleteAiAutoMemoryMutation);
const mockUseUpdateMutation = vi.mocked(useUpdateAiAutoMemoryMutation);

const makeMemory = (overrides: Partial<AiAutoMemoryI> = {}): AiAutoMemoryI => ({
    content: 'Default content',
    createdAt: '2026-04-01T00:00:00Z',
    description: 'Default description',
    environmentId: 0,
    id: 1,
    memoryType: 'USER',
    name: 'default_name',
    title: 'Default title',
    updatedAt: '2026-04-10T00:00:00Z',
    userId: 42,
    workspaceId: 7,
    ...overrides,
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const makeQueryResult = (overrides: Record<string, unknown> = {}): any => ({
    data: [],
    error: null,
    isError: false,
    isFetching: false,
    isLoading: false,
    isPending: false,
    isSuccess: true,
    refetch: vi.fn(),
    status: 'success' as const,
    ...overrides,
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const makeMutation = (overrides: Record<string, unknown> = {}): any => ({
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
        <MemoryRouter>
            <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
                {ui}
            </QueryClientProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: []}));
    mockUseDeleteMutation.mockReturnValue(makeMutation());
    mockUseUpdateMutation.mockReturnValue(makeMutation());
});

describe('Memories page', () => {
    it('renders the empty-state copy when no memories exist', () => {
        wrap(<Memories />);

        expect(screen.getByRole('heading', {name: /no memories yet/i})).toBeInTheDocument();
        expect(screen.getByText(/the agent stores facts here/i)).toBeInTheDocument();
    });

    it('lists memories in a table and exposes the count in the header', () => {
        const memories = [
            makeMemory({id: 1, memoryType: 'USER', name: 'alice_profile', title: 'Alice profile'}),
            makeMemory({id: 2, memoryType: 'FEEDBACK', name: 'concise_replies', title: 'Concise replies'}),
        ];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        expect(screen.getByText('Alice profile')).toBeInTheDocument();
        expect(screen.getByText('alice_profile')).toBeInTheDocument();
        expect(screen.getByText('Concise replies')).toBeInTheDocument();
        // Header was restyled: instead of "Memories (2)" as a single label, the page now shows the
        // search box plus a muted count suffix ("2 memories"). Both halves of the assertion-pair pin
        // the new shape so a regression to the old single-label format would fail loudly.
        expect(screen.getByText('2 memories')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Search by title or description...')).toBeInTheDocument();
        expect(screen.getByText('USER')).toBeInTheDocument();
        expect(screen.getByText('FEEDBACK')).toBeInTheDocument();
    });

    it('filters visible rows by title or description on search', async () => {
        const memories = [
            makeMemory({description: 'matches alice', id: 1, title: 'Alice profile'}),
            makeMemory({description: 'unrelated', id: 2, title: 'Bob preferences'}),
        ];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await userEvent.type(screen.getByPlaceholderText(/search by title or description/i), 'alice');

        expect(screen.getByText('Alice profile')).toBeInTheDocument();
        expect(screen.queryByText('Bob preferences')).toBeNull();
    });

    it('opens the detail dialog when View is clicked', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await userEvent.click(screen.getByRole('button', {name: /view alice profile/i}));

        expect(screen.getByRole('heading', {level: 2, name: /alice profile/i})).toBeInTheDocument();
    });

    it('opens the edit dialog when Edit is clicked', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await userEvent.click(screen.getByRole('button', {name: /edit alice profile/i}));

        expect(screen.getByRole('heading', {name: /edit memory/i})).toBeInTheDocument();
    });

    it('opens the delete dialog when Delete is clicked', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await userEvent.click(screen.getByRole('button', {name: /delete alice profile/i}));

        expect(screen.getByRole('heading', {name: /delete this memory permanently\?/i})).toBeInTheDocument();
    });

    it('invokes the memories query with the selected memoryType filter', async () => {
        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: []}));

        wrap(<Memories />);

        await userEvent.click(screen.getByRole('link', {name: /^feedback$/i}));

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[0]).toBe(7);
        expect(lastCall?.[1]).toBe(0);
        expect(lastCall?.[2]).toBe('FEEDBACK');
    });
});
