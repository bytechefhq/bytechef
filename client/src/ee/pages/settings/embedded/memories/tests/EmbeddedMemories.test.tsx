import {TooltipProvider} from '@/components/ui/tooltip';
import {EmbeddedAiAutoMemoriesQuery, EmbeddedAiAutoMemoryConnectedUsersQuery} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import EmbeddedMemories from '../EmbeddedMemories';

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteEmbeddedAiAutoMemoryMutation: vi.fn(),
    useEmbeddedAiAutoMemoriesQuery: vi.fn(),
    useEmbeddedAiAutoMemoryConnectedUsersQuery: vi.fn(),
    useUpdateEmbeddedAiAutoMemoryMutation: vi.fn(),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2})
    ),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {
    useDeleteEmbeddedAiAutoMemoryMutation,
    useEmbeddedAiAutoMemoriesQuery,
    useEmbeddedAiAutoMemoryConnectedUsersQuery,
    useUpdateEmbeddedAiAutoMemoryMutation,
} = await import('@/shared/middleware/graphql');

const mockUseMemoriesQuery = vi.mocked(useEmbeddedAiAutoMemoriesQuery);
const mockUseConnectedUsersQuery = vi.mocked(useEmbeddedAiAutoMemoryConnectedUsersQuery);
const mockUseDeleteMutation = vi.mocked(useDeleteEmbeddedAiAutoMemoryMutation);
const mockUseUpdateMutation = vi.mocked(useUpdateEmbeddedAiAutoMemoryMutation);

type GraphQlMemoryType = EmbeddedAiAutoMemoriesQuery['embeddedAiAutoMemories'][number];

const makeGraphQlMemory = (overrides: Partial<GraphQlMemoryType> = {}): GraphQlMemoryType => ({
    connectedUser: {email: 'ada@example.com', externalId: 'ext-ada', id: '21', name: 'Ada'},
    content: 'Customer is on the **Pro** plan.',
    createdAt: Date.parse('2026-04-01T00:00:00Z'),
    description: 'Which plan the customer is on',
    environmentId: 2,
    id: '3',
    integration: {componentName: 'memoryDemo', id: '31', name: 'Memory Demo'},
    integrationInstanceId: 11,
    memoryType: 'USER' as GraphQlMemoryType['memoryType'],
    name: 'customer_plan',
    title: 'Plan',
    updatedAt: Date.parse('2026-04-10T00:00:00Z'),
    version: 4,
    ...overrides,
});

const connectedUsersData: EmbeddedAiAutoMemoryConnectedUsersQuery = {
    embeddedAiAutoMemoryConnectedUsers: [
        {email: 'ada@example.com', externalId: 'ext-ada', id: '21', memoryCount: 1, name: 'Ada'},
        {email: null, externalId: 'ext-bob', id: '22', memoryCount: 2, name: null},
    ],
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const makeQueryResult = (data: unknown): any => ({
    data,
    error: null,
    isError: false,
    isLoading: false,
    isPending: false,
    refetch: vi.fn(),
});

const mockMemories = (memories: GraphQlMemoryType[]) => {
    mockUseMemoriesQuery.mockImplementation(((
        _variables: unknown,
        options: {select: (data: EmbeddedAiAutoMemoriesQuery) => unknown}
    ) => makeQueryResult(options.select({embeddedAiAutoMemories: memories}))) as never);
};

const deleteMutate = vi.fn();
const updateMutate = vi.fn();

const renderPage = () =>
    render(
        <MemoryRouter>
            <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
                <TooltipProvider>
                    <EmbeddedMemories />
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );

const openFilterMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: /^Filter Memories/}));
};

const pickFilterOption = async (option: string) => {
    await openFilterMenu();

    await userEvent.click(await screen.findByRole('menuitem', {name: option}));
};

const openRowMenu = async (title: string) => {
    await userEvent.click(screen.getByRole('button', {name: new RegExp(`more actions for ${title}`, 'i')}));
};

beforeEach(() => {
    deleteMutate.mockReset();
    updateMutate.mockReset();

    mockMemories([makeGraphQlMemory()]);

    mockUseConnectedUsersQuery.mockImplementation(((
        _variables: unknown,
        options: {select: (data: EmbeddedAiAutoMemoryConnectedUsersQuery) => unknown}
    ) => makeQueryResult(options.select(connectedUsersData))) as never);

    mockUseDeleteMutation.mockReturnValue({isPending: false, mutate: deleteMutate} as never);
    mockUseUpdateMutation.mockReturnValue({isPending: false, mutate: updateMutate} as never);
});

describe('EmbeddedMemories', () => {
    it('lists each memory with its connected user and integration', () => {
        renderPage();

        const row = screen.getByText('Plan').closest('tr')!;

        expect(within(row).getByText('Ada')).toBeInTheDocument();
        expect(within(row).getByText('ext-ada')).toBeInTheDocument();
        expect(within(row).getByText('Memory Demo')).toBeInTheDocument();
        expect(within(row).getByText('User')).toBeInTheDocument();
    });

    it('shows the external id once for a connected user without a name or email', () => {
        mockMemories([makeGraphQlMemory({connectedUser: {email: null, externalId: 'ext-bob', id: '22', name: null}})]);

        renderPage();

        const row = screen.getByText('Plan').closest('tr')!;

        expect(within(row).getAllByText('ext-bob')).toHaveLength(1);
    });

    it('marks a memory whose connected user and integration were deleted', () => {
        mockMemories([makeGraphQlMemory({connectedUser: null, integration: null})]);

        renderPage();

        const row = screen.getByText('Plan').closest('tr')!;

        expect(within(row).getByText('Deleted connected user')).toBeInTheDocument();
        expect(within(row).getByText('Deleted integration')).toBeInTheDocument();
    });

    it('queries the current environment with no filter by default', () => {
        renderPage();

        expect(mockUseMemoriesQuery).toHaveBeenLastCalledWith(
            {connectedUserId: undefined, environment: 2, memoryType: undefined},
            expect.anything()
        );
        expect(mockUseConnectedUsersQuery).toHaveBeenLastCalledWith({environment: 2}, expect.anything());
    });

    it('passes the connected user picked in the Connected User filter', async () => {
        renderPage();

        await pickFilterOption('ext-bob');

        expect(mockUseMemoriesQuery).toHaveBeenLastCalledWith(
            {connectedUserId: '22', environment: 2, memoryType: undefined},
            expect.anything()
        );
    });

    it('keeps the picked connected user selected once their last memory is gone', async () => {
        const {rerender} = renderPage();

        await pickFilterOption('ext-bob');

        mockMemories([]);

        mockUseConnectedUsersQuery.mockImplementation(((
            _variables: unknown,
            options: {select: (data: EmbeddedAiAutoMemoryConnectedUsersQuery) => unknown}
        ) =>
            makeQueryResult(
                options.select({
                    embeddedAiAutoMemoryConnectedUsers: connectedUsersData.embeddedAiAutoMemoryConnectedUsers.slice(
                        0,
                        1
                    ),
                })
            )) as never);

        rerender(
            <MemoryRouter>
                <QueryClientProvider client={new QueryClient()}>
                    <TooltipProvider>
                        <EmbeddedMemories />
                    </TooltipProvider>
                </QueryClientProvider>
            </MemoryRouter>
        );

        expect(mockUseMemoriesQuery).toHaveBeenLastCalledWith(
            {connectedUserId: '22', environment: 2, memoryType: undefined},
            expect.anything()
        );
        expect(screen.getByText('No memories match the selected filters.')).toBeInTheDocument();
    });

    it('passes the memory type picked in the Type filter', async () => {
        renderPage();

        await pickFilterOption('Feedback');

        expect(mockUseMemoriesQuery).toHaveBeenLastCalledWith(
            {connectedUserId: undefined, environment: 2, memoryType: 'FEEDBACK'},
            expect.anything()
        );
    });

    it('shows the memory content when a row is opened', async () => {
        renderPage();

        await userEvent.click(screen.getByText('Plan'));

        expect(await screen.findByText('Pro')).toBeInTheDocument();
    });

    it('saves an edit through the Embedded update mutation', async () => {
        renderPage();

        await openRowMenu('Plan');

        await userEvent.click(await screen.findByRole('menuitem', {name: /edit/i}));

        const titleInput = await screen.findByLabelText('Title');

        await userEvent.clear(titleInput);
        await userEvent.type(titleInput, 'Subscription plan');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(updateMutate).toHaveBeenCalledWith(
            {
                input: {
                    content: undefined,
                    description: undefined,
                    environment: 2,
                    expectedVersion: 4,
                    id: '3',
                    integrationInstanceId: '11',
                    memoryType: undefined,
                    title: 'Subscription plan',
                },
            },
            expect.anything()
        );
    });

    it('deletes through the Embedded delete mutation', async () => {
        renderPage();

        await openRowMenu('Plan');

        await userEvent.click(await screen.findByRole('menuitem', {name: /delete/i}));

        await userEvent.click(screen.getByRole('button', {name: 'Delete permanently'}));

        expect(deleteMutate).toHaveBeenCalledWith(
            {environment: 2, id: '3', integrationInstanceId: '11'},
            expect.anything()
        );
    });

    it('shows the empty state when no integration agent has saved a memory', () => {
        mockMemories([]);

        renderPage();

        expect(screen.getByText('No memories yet')).toBeInTheDocument();
    });
});
