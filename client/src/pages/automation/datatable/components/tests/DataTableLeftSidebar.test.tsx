import {useDeleteDataTableAlertDialogStore} from '@/pages/automation/datatable/stores/useDeleteDataTableAlertDialogStore';
import {useRenameDataTableDialogStore} from '@/pages/automation/datatable/stores/useRenameDataTableDialogStore';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableLeftSidebar from '../DataTableLeftSidebar';

const hoisted = vi.hoisted(() => {
    return {
        mockDropMutate: vi.fn(),
        mockInvalidateQueries: vi.fn(),
        mockNavigate: vi.fn(),
        mockRenameMutate: vi.fn(),
        storeState: {
            dataTables: [
                {baseName: 'Alpha', id: 'table-1'},
                {baseName: 'Beta', id: 'table-2'},
                {baseName: 'Gamma', id: 'table-3'},
            ],
            environmentId: 2,
            error: null as Error | null,
            isLoading: false,
            workspaceId: 1049,
        },
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => number) =>
        selector({currentWorkspaceId: hoisted.storeState.workspaceId}),
}));

vi.mock('@/shared/queries/automation/datatables.queries', () => ({
    DataTableKeys: {
        list: (environmentId: number, workspaceId: number) => ['dataTables', environmentId, workspaceId],
    },
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDataTablesQuery: () => ({
            data: {dataTables: hoisted.storeState.dataTables},
            error: hoisted.storeState.error,
            isLoading: hoisted.storeState.isLoading,
        }),
        useDropDataTableMutation: ({
            onSuccess,
        }: {
            onSuccess: (data: unknown, variables: {input: {tableId: string}}) => void;
        }) => ({
            mutate: (params: {input: {tableId: string}}) => {
                hoisted.mockDropMutate(params);
                onSuccess(undefined, params);
            },
        }),
        useRenameDataTableMutation: ({onSuccess}: {onSuccess: () => void}) => ({
            mutate: (params: unknown) => {
                hoisted.mockRenameMutate(params);
                onSuccess();
            },
        }),
    };
});

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => number) =>
        selector({currentEnvironmentId: hoisted.storeState.environmentId}),
}));

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();

    return {
        ...actual,
        useNavigate: () => hoisted.mockNavigate,
    };
});

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

const renderWithProviders = (ui: React.ReactElement) => {
    return render(
        <MemoryRouter>
            <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
        </MemoryRouter>
    );
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.storeState.dataTables = [
        {baseName: 'Alpha', id: 'table-1'},
        {baseName: 'Beta', id: 'table-2'},
        {baseName: 'Gamma', id: 'table-3'},
    ];
    hoisted.storeState.isLoading = false;
    hoisted.storeState.error = null;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
    queryClient.clear();

    // Reset zustand stores
    useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();
    useRenameDataTableDialogStore.getState().clearTableToRename();
});

describe('DataTableLeftSidebar', () => {
    describe('rendering', () => {
        it('should render search input', () => {
            renderWithProviders(<DataTableLeftSidebar />);

            expect(screen.getByPlaceholderText('Search tables...')).toBeInTheDocument();
        });

        it('should render all tables sorted alphabetically', () => {
            renderWithProviders(<DataTableLeftSidebar />);

            expect(screen.getByText('Alpha')).toBeInTheDocument();
            expect(screen.getByText('Beta')).toBeInTheDocument();
            expect(screen.getByText('Gamma')).toBeInTheDocument();
        });

        it('should show no tables found when list is empty', () => {
            hoisted.storeState.dataTables = [];

            renderWithProviders(<DataTableLeftSidebar />);

            expect(screen.getByText('No tables found')).toBeInTheDocument();
        });
    });

    describe('search functionality', () => {
        it('should filter tables by search term', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const searchInput = screen.getByPlaceholderText('Search tables...');

            await user.type(searchInput, 'Alpha');

            expect(screen.getByText('Alpha')).toBeInTheDocument();
            expect(screen.queryByText('Beta')).not.toBeInTheDocument();
            expect(screen.queryByText('Gamma')).not.toBeInTheDocument();
        });

        it('should show no tables found when search has no matches', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const searchInput = screen.getByPlaceholderText('Search tables...');

            await user.type(searchInput, 'NonExistent');

            expect(screen.getByText('No tables found')).toBeInTheDocument();
        });

        it('should be case insensitive', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const searchInput = screen.getByPlaceholderText('Search tables...');

            await user.type(searchInput, 'ALPHA');

            expect(screen.getByText('Alpha')).toBeInTheDocument();
        });
    });

    describe('current table highlighting', () => {
        it('should not highlight any table when currentId is undefined', () => {
            renderWithProviders(<DataTableLeftSidebar />);

            // All tables should be rendered but none should be marked as current
            expect(screen.getByText('Alpha')).toBeInTheDocument();
        });

        it('should highlight the current table when currentId is provided', () => {
            renderWithProviders(<DataTableLeftSidebar currentId="table-1" />);

            // The Alpha table should have a different style when current
            expect(screen.getByText('Alpha')).toBeInTheDocument();
        });
    });

    describe('table menu', () => {
        it('should render menu button for each table', () => {
            renderWithProviders(<DataTableLeftSidebar />);

            const menuButtons = screen.getAllByRole('button', {name: 'Table menu'});

            expect(menuButtons).toHaveLength(3);
        });

        it('should show rename and delete options in menu', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const menuButtons = screen.getAllByRole('button', {name: 'Table menu'});

            await user.click(menuButtons[0]);

            expect(screen.getByText('Rename')).toBeInTheDocument();
            expect(screen.getByText('Delete')).toBeInTheDocument();
        });
    });

    describe('delete dialog', () => {
        it('should open delete confirmation dialog when delete is clicked', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const menuButtons = screen.getAllByRole('button', {name: 'Table menu'});

            await user.click(menuButtons[0]);

            const deleteOption = screen.getByText('Delete');

            await user.click(deleteOption);

            expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
        });

        it('should close delete dialog when cancel is clicked', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const menuButtons = screen.getAllByRole('button', {name: 'Table menu'});

            await user.click(menuButtons[0]);

            const deleteOption = screen.getByText('Delete');

            await user.click(deleteOption);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        });
    });

    describe('rename dialog', () => {
        it('should open rename dialog when rename is clicked', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const menuButtons = screen.getAllByRole('button', {name: 'Table menu'});

            await user.click(menuButtons[0]);

            const renameOption = screen.getByText('Rename');

            await user.click(renameOption);

            expect(screen.getByText('Rename Table')).toBeInTheDocument();
        });

        it('should show input with current table name in rename dialog', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const menuButtons = screen.getAllByRole('button', {name: 'Table menu'});

            await user.click(menuButtons[0]);

            const renameOption = screen.getByText('Rename');

            await user.click(renameOption);

            const input = screen.getByRole('textbox');

            expect(input).toHaveValue('Alpha');
        });

        it('should close rename dialog when cancel is clicked', async () => {
            const user = userEvent.setup();

            renderWithProviders(<DataTableLeftSidebar />);

            const menuButtons = screen.getAllByRole('button', {name: 'Table menu'});

            await user.click(menuButtons[0]);

            const renameOption = screen.getByText('Rename');

            await user.click(renameOption);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(screen.queryByText('Rename Table')).not.toBeInTheDocument();
        });
    });
});
