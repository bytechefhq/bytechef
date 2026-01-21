import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableHeader from '../DataTableHeader';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleExportCsv: vi.fn(),
        mockHandleOpenDeleteDialog: vi.fn(),
        mockHandleOpenDeleteRowsDialog: vi.fn(),
        mockHandleOpenImportCsvDialog: vi.fn(),
        mockHandleOpenRenameDialog: vi.fn(),
        storeState: {
            dataTable: {baseName: 'TestTable', id: 'table-123'},
            selectedRowsCount: 0,
        },
    };
});

vi.mock('../../hooks/useDataTableHeader', () => ({
    default: () => ({
        handleOpenDeleteRowsDialog: hoisted.mockHandleOpenDeleteRowsDialog,
        selectedRowsCount: hoisted.storeState.selectedRowsCount,
    }),
}));

vi.mock('../../hooks/useDataTableActionsMenu', () => ({
    default: () => ({
        handleExportCsv: hoisted.mockHandleExportCsv,
        handleOpenDeleteDialog: hoisted.mockHandleOpenDeleteDialog,
        handleOpenImportCsvDialog: hoisted.mockHandleOpenImportCsvDialog,
        handleOpenRenameDialog: hoisted.mockHandleOpenRenameDialog,
    }),
}));

vi.mock('../../stores/useCurrentDataTableStore', () => ({
    useCurrentDataTableStore: () => ({
        dataTable: hoisted.storeState.dataTable,
    }),
}));

vi.mock('../DataTableActionsMenu', () => ({
    default: ({
        onDeleteTable,
        onExportCsv,
        onImportCsv,
        onRenameTable,
        tableId,
    }: {
        onDeleteTable: () => void;
        onExportCsv: () => void;
        onImportCsv: () => void;
        onRenameTable: () => void;
        tableId?: string;
    }) => (
        <div data-testid="actions-menu">
            <button data-testid="import-btn" onClick={onImportCsv}>
                Import Table
            </button>

            <button data-testid="export-btn" onClick={onExportCsv}>
                Export Table
            </button>

            <button data-testid="rename-btn" onClick={onRenameTable}>
                Rename Table
            </button>

            <button data-testid="delete-table-btn" onClick={onDeleteTable}>
                Delete Table
            </button>

            <span data-testid="table-id">{tableId}</span>
        </div>
    ),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.storeState.selectedRowsCount = 0;
    hoisted.storeState.dataTable = {baseName: 'TestTable', id: 'table-123'};
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTableHeader', () => {
    describe('rendering', () => {
        it('should render the table name', () => {
            render(<DataTableHeader />);

            expect(screen.getByText('TestTable')).toBeInTheDocument();
        });

        it('should render the actions menu', () => {
            render(<DataTableHeader />);

            expect(screen.getByTestId('actions-menu')).toBeInTheDocument();
        });

        it('should pass correct tableId to actions menu', () => {
            render(<DataTableHeader />);

            expect(screen.getByTestId('table-id')).toHaveTextContent('table-123');
        });
    });

    describe('delete rows button', () => {
        it('should not render delete rows button when no rows are selected', () => {
            hoisted.storeState.selectedRowsCount = 0;

            render(<DataTableHeader />);

            // The delete rows button should not be present when no rows are selected
            // We check that no button with "Delete (" pattern exists (the rows delete button)
            expect(screen.queryByRole('button', {name: /Delete \(/i})).not.toBeInTheDocument();
        });

        it('should render delete rows button when rows are selected', () => {
            hoisted.storeState.selectedRowsCount = 5;

            render(<DataTableHeader />);

            expect(screen.getByRole('button', {name: /Delete \(5\)/i})).toBeInTheDocument();
        });

        it('should show correct count in delete button', () => {
            hoisted.storeState.selectedRowsCount = 10;

            render(<DataTableHeader />);

            expect(screen.getByText(/Delete \(10\)/i)).toBeInTheDocument();
        });

        it('should call handleOpenDeleteRowsDialog when delete button is clicked', async () => {
            const user = userEvent.setup();
            hoisted.storeState.selectedRowsCount = 3;

            render(<DataTableHeader />);

            const deleteButton = screen.getByRole('button', {name: /Delete \(3\)/i});

            await user.click(deleteButton);

            expect(hoisted.mockHandleOpenDeleteRowsDialog).toHaveBeenCalledTimes(1);
        });
    });

    describe('actions menu handlers', () => {
        it('should call handleOpenImportCsvDialog when import is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableHeader />);

            const importBtn = screen.getByTestId('import-btn');

            await user.click(importBtn);

            expect(hoisted.mockHandleOpenImportCsvDialog).toHaveBeenCalledTimes(1);
        });

        it('should call handleExportCsv when export is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableHeader />);

            const exportBtn = screen.getByTestId('export-btn');

            await user.click(exportBtn);

            expect(hoisted.mockHandleExportCsv).toHaveBeenCalledTimes(1);
        });

        it('should call handleOpenRenameDialog when rename is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableHeader />);

            const renameBtn = screen.getByTestId('rename-btn');

            await user.click(renameBtn);

            expect(hoisted.mockHandleOpenRenameDialog).toHaveBeenCalledTimes(1);
        });

        it('should call handleOpenDeleteDialog when delete table is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableHeader />);

            const deleteBtn = screen.getByTestId('delete-table-btn');

            await user.click(deleteBtn);

            expect(hoisted.mockHandleOpenDeleteDialog).toHaveBeenCalledTimes(1);
        });
    });
});
