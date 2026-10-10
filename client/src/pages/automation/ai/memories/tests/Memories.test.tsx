import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Memories from '../Memories';
import {AiAutoMemoryDetailI, AiAutoMemoryPrincipalI} from '../hooks/useAiAutoMemories';

vi.mock('@/pages/automation/ai/memories/hooks/useAiAutoMemories', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/pages/automation/ai/memories/hooks/useAiAutoMemories')>()),
    useAiAutoMemoriesQuery: vi.fn(),
    useAiAutoMemoryDetailQuery: vi.fn(),
    useAiAutoMemoryPrincipalsQuery: vi.fn(),
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

const {authenticationState} = vi.hoisted(() => ({
    authenticationState: {
        account: undefined as {authorities?: string[]} | undefined,
        authenticated: false,
    },
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: typeof authenticationState) => unknown) =>
        selector(authenticationState)
    ),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {
    useAiAutoMemoriesQuery,
    useAiAutoMemoryDetailQuery,
    useAiAutoMemoryPrincipalsQuery,
    useDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation,
} = await import('@/pages/automation/ai/memories/hooks/useAiAutoMemories');

const mockUseMemoriesQuery = vi.mocked(useAiAutoMemoriesQuery);
const mockUseMemoryDetailQuery = vi.mocked(useAiAutoMemoryDetailQuery);
const mockUsePrincipalsQuery = vi.mocked(useAiAutoMemoryPrincipalsQuery);
const mockUseDeleteMutation = vi.mocked(useDeleteAiAutoMemoryMutation);
const mockUseUpdateMutation = vi.mocked(useUpdateAiAutoMemoryMutation);

const makePrincipal = (overrides: Partial<AiAutoMemoryPrincipalI> = {}): AiAutoMemoryPrincipalI => ({
    label: 'My memories',
    memoryCount: 3,
    principalId: 42,
    principalType: 'USER',
    ...overrides,
});

const makeMemory = (overrides: Partial<AiAutoMemoryDetailI> = {}): AiAutoMemoryDetailI => ({
    content: 'Memory content.',
    createdAt: '2026-04-01T00:00:00Z',
    description: 'Default description',
    environmentId: 0,
    id: 1,
    memoryType: 'USER',
    name: 'default_name',
    principalId: 42,
    principalType: 'USER',
    title: 'Default title',
    updatedAt: '2026-04-10T00:00:00Z',
    version: 3,
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

const withProviders = (ui: ReactNode) => (
    <MemoryRouter>
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    </MemoryRouter>
);

const wrap = (ui: ReactNode) => render(withProviders(ui));

beforeEach(() => {
    mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: []}));
    mockUseMemoryDetailQuery.mockImplementation(((memory: AiAutoMemoryDetailI | null) =>
        makeQueryResult({
            data: memory ?? null,
            refetch: vi.fn().mockResolvedValue({data: memory ?? null}),
        })) as never);
    mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: []}));
    mockUseDeleteMutation.mockReturnValue(makeMutation());
    mockUseUpdateMutation.mockReturnValue(makeMutation());

    authenticationState.account = undefined;
    authenticationState.authenticated = false;
});

async function openRowMenu(title: string): Promise<void> {
    await userEvent.click(screen.getByRole('button', {name: new RegExp(`more actions for ${title}`, 'i')}));
}

describe('Memories page', () => {
    it('renders the empty-state copy when no memories exist', () => {
        wrap(<Memories />);

        expect(screen.getByRole('heading', {name: /no memories yet/i})).toBeInTheDocument();
        expect(
            screen.getByText(/AI Agents that use the Auto Memory tool write memories here when their workflows run/i)
        ).toBeInTheDocument();
    });

    it('renders an inline error with a Retry button instead of the empty state when the list fails to load', async () => {
        const refetch = vi.fn();

        mockUseMemoriesQuery.mockReturnValue(
            makeQueryResult({
                data: undefined,
                error: new Error('Access denied'),
                isError: true,
                isSuccess: false,
                refetch,
                status: 'error',
            })
        );

        wrap(<Memories />);

        expect(screen.getByRole('heading', {name: /could not load memories/i})).toBeInTheDocument();
        expect(screen.getByText('Access denied')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: /no memories yet/i})).toBeNull();

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('keeps showing loaded rows when a later refetch of the list fails', () => {
        mockUseMemoriesQuery.mockReturnValue(
            makeQueryResult({
                data: [makeMemory({id: 1, title: 'Alice profile'})],
                error: new Error('Network down'),
                isError: true,
                status: 'error',
            })
        );

        wrap(<Memories />);

        expect(screen.getByText('Alice profile')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: /could not load memories/i})).toBeNull();
    });

    it('reports an owners load failure inline with a Retry button rather than silently dropping the Owner filter', async () => {
        const refetch = vi.fn();

        mockUsePrincipalsQuery.mockReturnValue(
            makeQueryResult({
                data: undefined,
                error: new Error('Owners unavailable'),
                isError: true,
                isSuccess: false,
                refetch,
                status: 'error',
            })
        );

        wrap(<Memories />);

        expect(screen.getByRole('alert')).toHaveTextContent(/could not load memory owners/i);

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('lists memories in a table alongside the filter row', () => {
        const memories = [
            makeMemory({id: 1, memoryType: 'USER', name: 'alice_profile', title: 'Alice profile'}),
            makeMemory({id: 2, memoryType: 'FEEDBACK', name: 'concise_replies', title: 'Concise replies'}),
        ];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        expect(screen.getByText('Alice profile')).toBeInTheDocument();
        expect(screen.getByText('alice_profile')).toBeInTheDocument();
        expect(screen.getByText('Concise replies')).toBeInTheDocument();
        expect(screen.queryByText('2 memories')).toBeNull();
        expect(screen.getByRole('button', {name: 'Filter Memories'})).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Search by title or description...')).toBeInTheDocument();
        expect(screen.getByText('User')).toBeInTheDocument();
        expect(screen.getByText('Feedback')).toBeInTheDocument();
        expect(screen.queryByText('USER')).toBeNull();
        expect(screen.queryByText('FEEDBACK')).toBeNull();
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

        await openRowMenu('Alice profile');

        await userEvent.click(await screen.findByRole('menuitem', {name: /view/i}));

        expect(screen.getByRole('heading', {level: 2, name: /alice profile/i})).toBeInTheDocument();
    });

    it('opens the detail dialog when the row is clicked', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await userEvent.click(screen.getByRole('row', {name: /alice profile/i}));

        expect(screen.getByRole('heading', {level: 2, name: /alice profile/i})).toBeInTheDocument();
    });

    it('opens the detail dialog when the focused row is activated with the keyboard', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        screen.getByRole('row', {name: /alice profile/i}).focus();

        await userEvent.keyboard('{Enter}');

        expect(screen.getByRole('heading', {level: 2, name: /alice profile/i})).toBeInTheDocument();
    });

    it('opens the row actions menu without the detail dialog', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await openRowMenu('Alice profile');

        expect(await screen.findByRole('menuitem', {name: /view/i})).toBeInTheDocument();
        expect(screen.queryByRole('heading', {level: 2, name: /alice profile/i})).toBeNull();
    });

    it('opens the edit dialog when Edit is clicked', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await openRowMenu('Alice profile');

        await userEvent.click(await screen.findByRole('menuitem', {name: /edit/i}));

        expect(screen.getByRole('heading', {name: /edit memory/i})).toBeInTheDocument();
    });

    it('opens the delete dialog when Delete is clicked', async () => {
        const memories = [makeMemory({id: 1, title: 'Alice profile'})];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await openRowMenu('Alice profile');

        await userEvent.click(await screen.findByRole('menuitem', {name: /delete/i}));

        expect(screen.getByRole('heading', {name: /delete this memory permanently\?/i})).toBeInTheDocument();
    });

    it('retries a save rejected by a newer write against the reloaded version', async () => {
        const mutate = vi.fn();

        mockUseUpdateMutation.mockReturnValue(makeMutation({mutate}));
        mockUseMemoriesQuery.mockReturnValue(
            makeQueryResult({data: [makeMemory({content: 'Original', id: 1, title: 'Alice profile', version: 3})]})
        );

        const {rerender} = wrap(<Memories />);

        await openRowMenu('Alice profile');

        await userEvent.click(await screen.findByRole('menuitem', {name: /edit/i}));

        await userEvent.type(screen.getByLabelText(/content/i), ' edited');

        mockUseMemoriesQuery.mockReturnValue(
            makeQueryResult({
                data: [makeMemory({content: 'Agent wrote this', id: 1, title: 'Alice profile', version: 4})],
            })
        );

        rerender(withProviders(<Memories />));

        expect(screen.getByRole('alert')).toHaveTextContent(/changed since you opened it/i);
        expect(screen.getByRole('button', {name: /save/i})).toBeDisabled();
        expect(screen.getByLabelText(/content/i)).toHaveValue('Original edited');

        await userEvent.click(screen.getByRole('button', {name: /reload/i}));

        expect(screen.getByLabelText(/content/i)).toHaveValue('Agent wrote this');

        await userEvent.type(screen.getByLabelText(/content/i), ' and more');
        await userEvent.click(screen.getByRole('button', {name: /save/i}));

        expect(mutate).toHaveBeenCalledWith(
            {input: expect.objectContaining({content: 'Agent wrote this and more', expectedVersion: 4})},
            expect.anything()
        );
    });

    it('closes an open dialog once its memory is gone from the list', async () => {
        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: [makeMemory({id: 1, title: 'Alice profile'})]}));

        const {rerender} = wrap(<Memories />);

        await openRowMenu('Alice profile');

        await userEvent.click(await screen.findByRole('menuitem', {name: /delete/i}));

        expect(screen.getByRole('heading', {name: /delete this memory permanently\?/i})).toBeInTheDocument();

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: []}));

        rerender(withProviders(<Memories />));

        expect(screen.queryByRole('heading', {name: /delete this memory permanently\?/i})).toBeNull();
    });

    it('does not offer Edit or Delete on a deployment-owned row to a non-admin', async () => {
        const memories = [
            makeMemory({id: 1, principalType: 'PROJECT_DEPLOYMENT', title: 'Deployment memory'}),
            makeMemory({id: 2, principalType: 'USER', title: 'My memory'}),
        ];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await openRowMenu('Deployment memory');

        expect(await screen.findByRole('menuitem', {name: /view/i})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: /edit/i})).toBeNull();
        expect(screen.queryByRole('menuitem', {name: /delete/i})).toBeNull();

        await userEvent.keyboard('{Escape}');

        await openRowMenu('My memory');

        expect(await screen.findByRole('menuitem', {name: /edit/i})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: /delete/i})).toBeInTheDocument();
    });

    it('offers Edit and Delete on a deployment-owned row to a tenant admin', async () => {
        authenticationState.account = {authorities: ['ROLE_ADMIN']};
        authenticationState.authenticated = true;

        mockUseMemoriesQuery.mockReturnValue(
            makeQueryResult({
                data: [makeMemory({id: 1, principalType: 'PROJECT_DEPLOYMENT', title: 'Deployment memory'})],
            })
        );

        wrap(<Memories />);

        await openRowMenu('Deployment memory');

        expect(await screen.findByRole('menuitem', {name: /edit/i})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: /delete/i})).toBeInTheDocument();
    });

    it('does not treat a stale ROLE_ADMIN account as admin while unauthenticated', async () => {
        authenticationState.account = {authorities: ['ROLE_ADMIN']};
        authenticationState.authenticated = false;

        mockUseMemoriesQuery.mockReturnValue(
            makeQueryResult({
                data: [makeMemory({id: 1, principalType: 'PROJECT_DEPLOYMENT', title: 'Deployment memory'})],
            })
        );

        wrap(<Memories />);

        await openRowMenu('Deployment memory');

        expect(await screen.findByRole('menuitem', {name: /view/i})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: /edit/i})).toBeNull();
        expect(screen.queryByRole('menuitem', {name: /delete/i})).toBeNull();
    });

    const openFilterMenu = async () => {
        await userEvent.click(screen.getByRole('button', {name: /^Filter Memories/}));
    };

    const pickFilterOption = async (option: string) => {
        await openFilterMenu();

        await userEvent.click(await screen.findByRole('menuitem', {name: option}));
    };

    it('says no memory matches the filters, rather than showing the onboarding empty state, when a filter empties the list', async () => {
        wrap(<Memories />);

        await pickFilterOption('Feedback');

        expect(screen.getByText('No memories match the selected filters.')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: /no memories yet/i})).toBeNull();
        expect(screen.getByText('Type: Feedback')).toBeInTheDocument();
    });

    it('invokes the memories query with the memoryType picked in the Type filter', async () => {
        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: []}));

        wrap(<Memories />);

        await pickFilterOption('Feedback');

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[0]).toBe(7);
        expect(lastCall?.[1]).toBe(0);
        expect(lastCall?.[2]).toBe('FEEDBACK');
    });

    it('omits the Owner filter and both principal arguments while no owner has been picked', () => {
        mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: []}));

        wrap(<Memories />);

        expect(screen.queryByRole('button', {name: /^Owner:/})).toBeNull();

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[3]).toBeUndefined();
        expect(lastCall?.[4]).toBeUndefined();
    });

    it('renders the server-resolved owner labels verbatim in the Owner filter', async () => {
        mockUsePrincipalsQuery.mockReturnValue(
            makeQueryResult({
                data: [
                    makePrincipal({label: 'My memories', principalId: 42, principalType: 'USER'}),
                    makePrincipal({
                        label: 'Support triage deployment',
                        principalId: 9,
                        principalType: 'PROJECT_DEPLOYMENT',
                    }),
                ],
            })
        );

        wrap(<Memories />);

        await openFilterMenu();

        expect(await screen.findByRole('menuitem', {name: 'My memories'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Support triage deployment'})).toBeInTheDocument();
    });

    it('opens on the All owner scope, which sends no principal', async () => {
        mockUsePrincipalsQuery.mockReturnValue(
            makeQueryResult({
                data: [
                    makePrincipal({label: 'My memories', principalId: 42, principalType: 'USER'}),
                    makePrincipal({
                        label: 'Support triage deployment',
                        principalId: 9,
                        principalType: 'PROJECT_DEPLOYMENT',
                    }),
                ],
            })
        );

        wrap(<Memories />);

        expect(screen.queryByRole('button', {name: 'Clear all'})).toBeNull();
        expect(screen.getByRole('button', {name: 'Filter Memories'})).toBeInTheDocument();

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[3]).toBeUndefined();
        expect(lastCall?.[4]).toBeUndefined();
    });

    it('keeps Owner and Type independent, so one item in each can be active', async () => {
        mockUsePrincipalsQuery.mockReturnValue(
            makeQueryResult({
                data: [makePrincipal({label: 'My memories', principalId: 42, principalType: 'USER'})],
            })
        );

        wrap(<Memories />);

        await pickFilterOption('My memories');
        await pickFilterOption('Feedback');

        expect(screen.getByText('Owner: My memories')).toBeInTheDocument();
        expect(screen.getByText('Type: Feedback')).toBeInTheDocument();

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[2]).toBe('FEEDBACK');
        expect(lastCall?.[3]).toBe('USER');
        expect(lastCall?.[4]).toBe(42);
    });

    it('invokes the memories query with the picked owner principal pair', async () => {
        mockUsePrincipalsQuery.mockReturnValue(
            makeQueryResult({
                data: [
                    makePrincipal({label: 'My memories', principalId: 42, principalType: 'USER'}),
                    makePrincipal({
                        label: 'Support triage deployment',
                        principalId: 9,
                        principalType: 'PROJECT_DEPLOYMENT',
                    }),
                ],
            })
        );

        wrap(<Memories />);

        await pickFilterOption('Support triage deployment');

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[3]).toBe('PROJECT_DEPLOYMENT');
        expect(lastCall?.[4]).toBe(9);
    });

    it('reports the All owner scope, not an empty Owner chip, once the picked owner drops out of a non-empty owner list', async () => {
        const deploymentPrincipal = makePrincipal({
            label: 'Support triage deployment',
            principalId: 9,
            principalType: 'PROJECT_DEPLOYMENT',
        });
        const userPrincipal = makePrincipal({label: 'My memories', principalId: 42, principalType: 'USER'});

        mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: [userPrincipal, deploymentPrincipal]}));

        const {rerender} = wrap(<Memories />);

        await pickFilterOption('Support triage deployment');

        expect(screen.getByText('Owner: Support triage deployment')).toBeInTheDocument();

        mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: [userPrincipal]}));

        rerender(withProviders(<Memories />));

        expect(screen.queryByText(/^Owner:/)).toBeNull();
        expect(screen.queryByRole('button', {name: 'Clear all'})).toBeNull();
        expect(mockUseMemoriesQuery.mock.lastCall?.[3]).toBeUndefined();
        expect(mockUseMemoriesQuery.mock.lastCall?.[4]).toBeUndefined();
    });

    it('falls back to the All owner scope when the picked owner is absent from the current environment', async () => {
        mockUsePrincipalsQuery.mockReturnValue(
            makeQueryResult({
                data: [
                    makePrincipal({
                        label: 'Support triage deployment',
                        principalId: 9,
                        principalType: 'PROJECT_DEPLOYMENT',
                    }),
                ],
            })
        );

        const {rerender} = wrap(<Memories />);

        await pickFilterOption('Support triage deployment');

        expect(mockUseMemoriesQuery.mock.lastCall?.[3]).toBe('PROJECT_DEPLOYMENT');

        mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: []}));

        rerender(withProviders(<Memories />));

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[3]).toBeUndefined();
        expect(lastCall?.[4]).toBeUndefined();
    });
});
