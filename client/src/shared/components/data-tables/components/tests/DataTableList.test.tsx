import {ColumnType} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableList from '../DataTableList';

const hoisted = vi.hoisted(() => {
    return {
        mockUseDataTableList: vi.fn(),
        mockUseDeleteDataTableAlertDialog: vi.fn(),
    };
});

vi.mock('../hooks/useDataTableList', () => ({
    default: hoisted.mockUseDataTableList,
}));

vi.mock('@/pages/automation/datatable/hooks/useDeleteDataTableAlertDialog', () => ({
    default: hoisted.mockUseDeleteDataTableAlertDialog,
}));

vi.mock('../DataTableListItem', () => ({
    default: ({table}: {table: {baseName: string; id: string}}) => (
        <div data-testid={`datatable-item-${table.id}`}>{table.baseName}</div>
    ),
}));

vi.mock('@/pages/automation/datatable/components/DeleteDataTableAlertDialog', () => ({
    default: () => <div data-testid="delete-dialog" />,
}));

const mockDataTables = [
    {baseName: 'orders', columns: [{id: '1', name: 'id', type: ColumnType.Integer}], id: '1'},
    {baseName: 'customers', columns: [{id: '2', name: 'name', type: ColumnType.String}], id: '2'},
];

const mockTags = [
    {id: '1', name: 'Important'},
    {id: '2', name: 'Archived'},
];

const mockTagsByTableData = [
    {tableId: '1', tags: [{id: '1', name: 'Important'}]},
    {tableId: '2', tags: []},
];

const defaultProps = {
    allTags: mockTags,
    dataTables: mockDataTables,
    tagsByTableData: mockTagsByTableData,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseDataTableList.mockReturnValue({
        sortedTables: mockDataTables,
        tagsByTableMap: new Map([
            ['1', [{id: '1', name: 'Important'}]],
            ['2', []],
        ]),
    });
    hoisted.mockUseDeleteDataTableAlertDialog.mockReturnValue({
        handleClose: vi.fn(),
        handleDelete: vi.fn(),
        handleOpen: vi.fn(),
        open: false,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTableList', () => {
    it('should render all data tables', () => {
        render(<DataTableList {...defaultProps} />);

        expect(screen.getByTestId('datatable-item-1')).toBeInTheDocument();
        expect(screen.getByTestId('datatable-item-2')).toBeInTheDocument();
    });

    it('should render data table names', () => {
        render(<DataTableList {...defaultProps} />);

        expect(screen.getByText('orders')).toBeInTheDocument();
        expect(screen.getByText('customers')).toBeInTheDocument();
    });

    it('should render DeleteDataTableAlertDialog', () => {
        render(<DataTableList {...defaultProps} />);

        expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    });
});

describe('DataTableList empty state', () => {
    beforeEach(() => {
        hoisted.mockUseDataTableList.mockReturnValue({
            sortedTables: [],
            tagsByTableMap: new Map(),
        });
    });

    it('should not render any data table items when empty', () => {
        render(<DataTableList allTags={[]} dataTables={[]} tagsByTableData={[]} />);

        expect(screen.queryByTestId('datatable-item-1')).not.toBeInTheDocument();
    });

    it('should still render DeleteDataTableAlertDialog when empty', () => {
        render(<DataTableList allTags={[]} dataTables={[]} tagsByTableData={[]} />);

        expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    });
});
