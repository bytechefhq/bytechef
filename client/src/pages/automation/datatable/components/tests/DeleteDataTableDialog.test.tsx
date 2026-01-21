import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteDataTableDialog from '../DeleteDataTableDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleDelete: vi.fn(),
        mockHandleOpenChange: vi.fn(),
        storeState: {
            open: true,
            tableName: 'TestTable',
        },
    };
});

vi.mock('../../hooks/useDeleteDataTableDialog', () => ({
    default: () => ({
        handleDelete: hoisted.mockHandleDelete,
        handleOpenChange: hoisted.mockHandleOpenChange,
        open: hoisted.storeState.open,
        tableName: hoisted.storeState.tableName,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.storeState.open = true;
    hoisted.storeState.tableName = 'TestTable';
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DeleteDataTableDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open', () => {
            render(<DeleteDataTableDialog />);

            expect(screen.getByText('Delete Table')).toBeInTheDocument();
        });

        it('should render the description with table name', () => {
            render(<DeleteDataTableDialog />);

            expect(
                screen.getByText(
                    'Are you sure you want to delete table "TestTable"? This action cannot be undone and will remove the table and all of its data.'
                )
            ).toBeInTheDocument();
        });

        it('should render cancel and delete buttons', () => {
            render(<DeleteDataTableDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
        });

        it('should not render when not open', () => {
            hoisted.storeState.open = false;

            render(<DeleteDataTableDialog />);

            expect(screen.queryByText('Delete Table')).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call handleDelete when delete button is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableDialog />);

            const deleteButton = screen.getByRole('button', {name: 'Delete'});

            await user.click(deleteButton);

            expect(hoisted.mockHandleDelete).toHaveBeenCalledTimes(1);
        });

        it('should call handleOpenChange when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleOpenChange).toHaveBeenCalledWith(false);
        });
    });

    describe('different table names', () => {
        it('should display different table name in description', () => {
            hoisted.storeState.tableName = 'AnotherTable';

            render(<DeleteDataTableDialog />);

            expect(
                screen.getByText(
                    'Are you sure you want to delete table "AnotherTable"? This action cannot be undone and will remove the table and all of its data.'
                )
            ).toBeInTheDocument();
        });
    });
});
