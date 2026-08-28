import {ColumnType} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableListItem from '../DataTableListItem';

const hoisted = vi.hoisted(() => {
    return {
        mockNavigate: vi.fn(),
    };
});

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');

    return {
        ...actual,
        useNavigate: () => hoisted.mockNavigate,
    };
});

vi.mock('../DataTableListItemDropdownMenu', () => ({
    default: ({dataTableId}: {baseName: string; dataTableId: string}) => (
        <div data-testid={`dropdown-menu-${dataTableId}`}>Menu</div>
    ),
}));

vi.mock('../DataTableListItemTagList', () => ({
    default: ({datatableId}: {datatableId: string}) => <div data-testid={`tag-list-${datatableId}`} />,
}));

vi.mock('../DataTableListItemTooltip', () => ({
    default: ({lastModifiedDate}: {lastModifiedDate?: number | null}) => (
        <div data-testid="tooltip">Last modified: {lastModifiedDate ?? 'N/A'}</div>
    ),
}));

const mockTable = {
    baseName: 'orders',
    columns: [
        {id: '1', name: 'id', type: ColumnType.Integer},
        {id: '2', name: 'name', type: ColumnType.String},
    ],
    id: '123',
    lastModifiedDate: 1705000000000,
};

const mockTags = [{id: '1', name: 'Important'}];

const mockRemainingTags = [{id: '2', name: 'Archived'}];

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTableListItem', () => {
    it('should render table base name', () => {
        render(<DataTableListItem remainingTags={mockRemainingTags} table={mockTable} tags={mockTags} />);

        expect(screen.getByText('orders')).toBeInTheDocument();
    });

    it('should render column count for multiple columns', () => {
        render(<DataTableListItem remainingTags={mockRemainingTags} table={mockTable} tags={mockTags} />);

        expect(screen.getByText('2 columns')).toBeInTheDocument();
    });

    it('should render singular column text for single column', () => {
        const singleColumnTable = {
            ...mockTable,
            columns: [{id: '1', name: 'id', type: ColumnType.Integer}],
        };

        render(<DataTableListItem remainingTags={mockRemainingTags} table={singleColumnTable} tags={mockTags} />);

        expect(screen.getByText('1 column')).toBeInTheDocument();
    });

    it('should render 0 columns when columns array is empty', () => {
        const emptyColumnsTable = {
            ...mockTable,
            columns: [],
        };

        render(<DataTableListItem remainingTags={mockRemainingTags} table={emptyColumnsTable} tags={mockTags} />);

        expect(screen.getByText('0 columns')).toBeInTheDocument();
    });

    it('should render dropdown menu', () => {
        render(<DataTableListItem remainingTags={mockRemainingTags} table={mockTable} tags={mockTags} />);

        expect(screen.getByTestId('dropdown-menu-123')).toBeInTheDocument();
    });

    it('should render tag list', () => {
        render(<DataTableListItem remainingTags={mockRemainingTags} table={mockTable} tags={mockTags} />);

        expect(screen.getByTestId('tag-list-123')).toBeInTheDocument();
    });

    it('should render tooltip', () => {
        render(<DataTableListItem remainingTags={mockRemainingTags} table={mockTable} tags={mockTags} />);

        expect(screen.getByTestId('tooltip')).toBeInTheDocument();
    });

    it('should navigate to table details when row is clicked', async () => {
        render(<DataTableListItem remainingTags={mockRemainingTags} table={mockTable} tags={mockTags} />);

        const row = screen.getByText('orders').closest('div[class*="cursor-pointer"]');

        if (row) {
            await userEvent.click(row);

            expect(hoisted.mockNavigate).toHaveBeenCalledWith('/automation/datatables/123');
        }
    });
});

describe('DataTableListItem without columns', () => {
    it('should handle undefined columns gracefully', () => {
        const tableWithoutColumns = {
            baseName: 'orders',
            id: '123',
            lastModifiedDate: 1705000000000,
        } as typeof mockTable;

        render(<DataTableListItem remainingTags={mockRemainingTags} table={tableWithoutColumns} tags={mockTags} />);

        expect(screen.getByText('0 columns')).toBeInTheDocument();
    });
});
