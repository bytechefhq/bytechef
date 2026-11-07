import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTables from '../DataTables';

const renderDataTables = () =>
    render(
        <TooltipProvider>
            <DataTables />
        </TooltipProvider>
    );

const hoisted = vi.hoisted(() => {
    return {
        mockUseDataTableStorageUsageQuery: vi.fn(),
        mockUseDataTables: vi.fn(),
    };
});

vi.mock('@/pages/automation/datatables/components/hooks/useDataTables', () => ({
    default: hoisted.mockUseDataTables,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDataTableStorageUsageQuery: hoisted.mockUseDataTableStorageUsageQuery,
    };
});

vi.mock('@/pages/automation/datatables/components/CreateDataTableDialog', () => ({
    default: () => <button data-testid="create-dialog-trigger">New Table</button>,
}));

vi.mock('@/pages/automation/datatables/components/DataTableList', () => ({
    default: ({dataTables}: {dataTables: Array<{id: string; baseName: string}>}) => (
        <div data-testid="data-table-list">
            {dataTables.map((table) => (
                <div data-testid={`table-${table.id}`} key={table.id}>
                    {table.baseName}
                </div>
            ))}
        </div>
    ),
}));

vi.mock('@/pages/automation/datatables/components/DataTablesFilterTitle', () => ({
    default: () => <div data-testid="filter-title">Filter Title</div>,
}));

vi.mock('@/pages/automation/datatables/components/DataTablesLeftSidebarNav', () => ({
    default: () => <div data-testid="left-sidebar-nav">Sidebar Nav</div>,
}));

const defaultMockReturn = {
    error: null,
    filteredTables: [
        {baseName: 'Table1', columns: [], id: '1', lastModifiedDate: '2024-01-01'},
        {baseName: 'Table2', columns: [], id: '2', lastModifiedDate: '2024-01-02'},
    ],
    isLoading: false,
    tables: [
        {baseName: 'Table1', columns: [], id: '1', lastModifiedDate: '2024-01-01'},
        {baseName: 'Table2', columns: [], id: '2', lastModifiedDate: '2024-01-02'},
    ],
    tagId: undefined,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseDataTables.mockReturnValue({...defaultMockReturn});
    hoisted.mockUseDataTableStorageUsageQuery.mockReturnValue({data: undefined});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTables', () => {
    describe('loading state', () => {
        it('should render loading state when isLoading is true', () => {
            hoisted.mockUseDataTables.mockReturnValue({
                ...defaultMockReturn,
                filteredTables: [],
                isLoading: true,
                tables: [],
            });

            renderDataTables();

            expect(screen.queryByTestId('data-table-list')).not.toBeInTheDocument();
        });

        it('should hide the left sidebar until the first load completes', () => {
            hoisted.mockUseDataTables.mockReturnValue({...defaultMockReturn, isLoading: true});

            const {rerender} = renderDataTables();

            expect(screen.queryByTestId('left-sidebar-nav')).not.toBeInTheDocument();

            hoisted.mockUseDataTables.mockReturnValue({...defaultMockReturn});

            rerender(
                <TooltipProvider>
                    <DataTables />
                </TooltipProvider>
            );

            expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
        });
    });

    describe('error state', () => {
        it('should render error message when error exists', () => {
            hoisted.mockUseDataTables.mockReturnValue({
                ...defaultMockReturn,
                error: new Error('Failed to fetch tables'),
            });

            renderDataTables();

            expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        });
    });

    describe('data table list', () => {
        it('should render page title', () => {
            renderDataTables();

            expect(screen.getByText('Data Tables')).toBeInTheDocument();
        });

        it('should render DataTableList component', () => {
            renderDataTables();

            expect(screen.getByTestId('data-table-list')).toBeInTheDocument();
        });

        it('should render tables', () => {
            renderDataTables();

            expect(screen.getByText('Table1')).toBeInTheDocument();
            expect(screen.getByText('Table2')).toBeInTheDocument();
        });

        it('should render CreateDataTableDialog trigger', () => {
            renderDataTables();

            expect(screen.getByTestId('create-dialog-trigger')).toBeInTheDocument();
        });

        it('should render left sidebar nav', () => {
            renderDataTables();

            expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
        });
    });

    describe('empty state', () => {
        it('should render the first-run empty state without the sidebar when no tables', () => {
            hoisted.mockUseDataTables.mockReturnValue({...defaultMockReturn, filteredTables: [], tables: []});

            renderDataTables();

            expect(screen.getByText('Create your first data table')).toBeInTheDocument();
            expect(screen.getByText('How data tables work')).toBeInTheDocument();
            expect(screen.getByTestId('create-dialog-trigger')).toBeInTheDocument();
            expect(screen.queryByTestId('left-sidebar-nav')).not.toBeInTheDocument();
        });

        it('should render empty state with tag filter message', () => {
            hoisted.mockUseDataTables.mockReturnValue({...defaultMockReturn, filteredTables: [], tagId: 1});

            renderDataTables();

            expect(screen.getByText('No Matching Tables')).toBeInTheDocument();
            expect(screen.getByText('No data tables match the selected tag.')).toBeInTheDocument();
            expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
        });
    });

    describe('storage usage banner', () => {
        it('shows the banner when usage is at or above 80 percent', () => {
            hoisted.mockUseDataTableStorageUsageQuery.mockReturnValue({
                data: {
                    dataTableStorageUsage: {limitBytes: 1_000, percentage: 90, unlimited: false, usedBytes: 900},
                },
            });

            renderDataTables();

            expect(screen.getByText('Data table storage is at 90%')).toBeInTheDocument();
        });

        it('hides the banner when the limit is unlimited', () => {
            hoisted.mockUseDataTableStorageUsageQuery.mockReturnValue({
                data: {
                    dataTableStorageUsage: {limitBytes: 0, percentage: 0, unlimited: true, usedBytes: 5_000_000},
                },
            });

            renderDataTables();

            expect(screen.queryByText(/storage is at/)).not.toBeInTheDocument();
        });

        it('hides the banner while usage is unknown', () => {
            renderDataTables();

            expect(screen.queryByText(/storage is at/)).not.toBeInTheDocument();
        });
    });
});
