import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Memories from '../Memories';
import {AiAutoMemoryI, AiAutoMemoryPrincipalI} from '../hooks/useAiAutoMemories';

// Partial mock: the query hooks are stubbed, but the memory-type constants are plain data derived from the
// generated enum and the component builds its filter list from them at module scope.
vi.mock('@/pages/automation/ai/memories/hooks/useAiAutoMemories', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/pages/automation/ai/memories/hooks/useAiAutoMemories')>()),
    useAiAutoMemoriesQuery: vi.fn(),
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

// Mutable so a test can flip the caller to a tenant admin. Hoisted because vi.mock factories run before
// module-scope consts are initialised.
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
    useAiAutoMemoryPrincipalsQuery,
    useDeleteAiAutoMemoryMutation,
    useUpdateAiAutoMemoryMutation,
} = await import('@/pages/automation/ai/memories/hooks/useAiAutoMemories');

const mockUseMemoriesQuery = vi.mocked(useAiAutoMemoriesQuery);
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

const makeMemory = (overrides: Partial<AiAutoMemoryI> = {}): AiAutoMemoryI => ({
    content: 'Default content',
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
                {/* The header's sidebar toggle renders a tooltip, which throws outside a provider. */}

                <TooltipProvider>{ui}</TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: []}));
    mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: []}));
    mockUseDeleteMutation.mockReturnValue(makeMutation());
    mockUseUpdateMutation.mockReturnValue(makeMutation());

    authenticationState.account = undefined;
    authenticationState.authenticated = false;
});

// Row actions live behind a per-row ellipsis dropdown, so a test has to open the row's menu before its items
// exist in the DOM.
async function openRowMenu(title: string): Promise<void> {
    await userEvent.click(screen.getByRole('button', {name: new RegExp(`more actions for ${title}`, 'i')}));
}

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

        await openRowMenu('Alice profile');

        await userEvent.click(await screen.findByRole('menuitem', {name: /view/i}));

        expect(screen.getByRole('heading', {level: 2, name: /alice profile/i})).toBeInTheDocument();
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

    it('does not offer Edit or Delete on a deployment-owned row to a non-admin', async () => {
        const memories = [
            makeMemory({id: 1, principalType: 'PROJECT_DEPLOYMENT', title: 'Deployment memory'}),
            makeMemory({id: 2, principalType: 'USER', title: 'My memory'}),
        ];

        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: memories}));

        wrap(<Memories />);

        await openRowMenu('Deployment memory');

        // The server needs ROLE_ADMIN to mutate a deployment-owned memory and answers NotFound otherwise, so
        // the affordance must not be offered at all — a live item here can only ever produce an error toast.
        expect(await screen.findByRole('menuitem', {name: /view/i})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: /edit/i})).toBeNull();
        expect(screen.queryByRole('menuitem', {name: /delete/i})).toBeNull();

        await userEvent.keyboard('{Escape}');

        // The caller's own row keeps both actions.
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

    it('does not treat a stale ROLE_ADMIN account as admin while unauthenticated', () => {
        // Mid re-login `account` can still hold the previous session's authorities; the `authenticated` gate is
        // what stops a flash of privilege. Same reasoning as useHasWorkspaceRole's tenant-admin short-circuit.
        authenticationState.account = {authorities: ['ROLE_ADMIN']};
        authenticationState.authenticated = false;

        mockUseMemoriesQuery.mockReturnValue(
            makeQueryResult({
                data: [makeMemory({id: 1, principalType: 'PROJECT_DEPLOYMENT', title: 'Deployment memory'})],
            })
        );

        wrap(<Memories />);

        expect(screen.queryByRole('button', {name: /edit deployment memory/i})).toBeNull();
    });

    it('invokes the memories query with the memoryType picked in the Type sidebar group', async () => {
        mockUseMemoriesQuery.mockReturnValue(makeQueryResult({data: []}));

        wrap(<Memories />);

        await userEvent.click(screen.getByRole('link', {name: 'Feedback'}));

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[0]).toBe(7);
        expect(lastCall?.[1]).toBe(0);
        expect(lastCall?.[2]).toBe('FEEDBACK');
    });

    it('omits the Owner group and both principal arguments while no owner has been picked', () => {
        mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: []}));

        wrap(<Memories />);

        expect(screen.queryByText('Owner')).toBeNull();

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        // Both undefined is the "signed-in user" contract — the server resolves the caller when neither
        // principal argument is sent, so an accidental default here would silently change whose memories load.
        expect(lastCall?.[3]).toBeUndefined();
        expect(lastCall?.[4]).toBeUndefined();
    });

    it('renders the server-resolved owner labels verbatim in an Owner group', () => {
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

        expect(screen.getByText('Owner')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'My memories'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Support triage deployment'})).toBeInTheDocument();
    });

    it('opens on the All owner scope, which sends no principal', () => {
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

        // Both groups carry an All row, so scope the query to the Owner group — LeftSidebarNav labels it with
        // its title.
        const ownerGroup = within(screen.getByLabelText('Owner'));

        // Absent principal arguments are the server's All scope on this query — every owner the caller may
        // address — so the default view spans owners rather than showing only the caller's own memories.
        expect(ownerGroup.getByRole('link', {name: 'All'})).toHaveAttribute('aria-current', 'page');
        expect(ownerGroup.getByRole('link', {name: 'My memories'})).not.toHaveAttribute('aria-current');

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

        await userEvent.click(screen.getByRole('link', {name: 'My memories'}));
        await userEvent.click(screen.getByRole('link', {name: 'Feedback'}));

        // Picking a type must not reset the owner scope: they are separate facets, and the query carries both.
        expect(screen.getByRole('link', {name: 'My memories'})).toHaveAttribute('aria-current', 'page');
        expect(screen.getByRole('link', {name: 'Feedback'})).toHaveAttribute('aria-current', 'page');

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

        await userEvent.click(screen.getByRole('link', {name: 'Support triage deployment'}));

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[3]).toBe('PROJECT_DEPLOYMENT');
        expect(lastCall?.[4]).toBe(9);
    });

    it('falls back to the signed-in user when the picked owner is absent from the current environment', async () => {
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

        await userEvent.click(screen.getByRole('link', {name: 'Support triage deployment'}));

        // Pin the pre-condition, or the assertion below would also hold if the click never registered.
        expect(mockUseMemoriesQuery.mock.lastCall?.[3]).toBe('PROJECT_DEPLOYMENT');

        // Switching environment replaces the owner list; the previously picked deployment holds nothing here.
        mockUsePrincipalsQuery.mockReturnValue(makeQueryResult({data: []}));

        rerender(
            <MemoryRouter>
                <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
                    <TooltipProvider>
                        <Memories />
                    </TooltipProvider>
                </QueryClientProvider>
            </MemoryRouter>
        );

        const lastCall = mockUseMemoriesQuery.mock.lastCall;

        expect(lastCall?.[3]).toBeUndefined();
        expect(lastCall?.[4]).toBeUndefined();
    });
});
