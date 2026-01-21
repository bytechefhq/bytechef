import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTables from '../DataTables';

const hoisted = vi.hoisted(() => {
    return {
        mockUseDataTables: vi.fn(),
    };
});

vi.mock('@/pages/automation/datatables/components/hooks/useDataTables', () => ({
    default: hoisted.mockUseDataTables,
}));

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

            render(<DataTables />);

            expect(screen.queryByTestId('data-table-list')).not.toBeInTheDocument();
        });
    });

    describe('error state', () => {
        it('should render error message when error exists', () => {
            hoisted.mockUseDataTables.mockReturnValue({
                ...defaultMockReturn,
                error: new Error('Failed to fetch tables'),
            });

            render(<DataTables />);

            expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        });
    });

    describe('data table list', () => {
        it('should render page title', () => {
            render(<DataTables />);

            expect(screen.getByText('Data Tables')).toBeInTheDocument();
        });

        it('should render DataTableList component', () => {
            render(<DataTables />);

            expect(screen.getByTestId('data-table-list')).toBeInTheDocument();
        });

        it('should render tables', () => {
            render(<DataTables />);

            expect(screen.getByText('Table1')).toBeInTheDocument();
            expect(screen.getByText('Table2')).toBeInTheDocument();
        });

        it('should render CreateDataTableDialog trigger', () => {
            render(<DataTables />);

            expect(screen.getByTestId('create-dialog-trigger')).toBeInTheDocument();
        });

        it('should render left sidebar nav', () => {
            render(<DataTables />);

            expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
        });
    });

    describe('empty state', () => {
        it('should render empty state when no tables', () => {
            hoisted.mockUseDataTables.mockReturnValue({...defaultMockReturn, filteredTables: [], tables: []});

            render(<DataTables />);

            expect(screen.getByText('No Data Tables')).toBeInTheDocument();
            expect(screen.getByText('Get started by creating a new data table.')).toBeInTheDocument();
        });

        it('should render empty state with tag filter message', () => {
            hoisted.mockUseDataTables.mockReturnValue({...defaultMockReturn, filteredTables: [], tagId: 1});

            render(<DataTables />);

            expect(screen.getByText('No Matching Tables')).toBeInTheDocument();
            expect(screen.getByText('No data tables match the selected tag.')).toBeInTheDocument();
        });
    });
});
