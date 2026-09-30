import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteDataTableRowsDialog from '../DeleteDataTableRowsDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleClose: vi.fn(),
        mockHandleDelete: vi.fn(),
        storeState: {
            isPending: false,
            open: true,
            rowCount: 5,
        },
    };
});

vi.mock('../../hooks/useDeleteDataTableRowsDialog', () => ({
    default: () => ({
        handleClose: hoisted.mockHandleClose,
        handleDelete: hoisted.mockHandleDelete,
        isPending: hoisted.storeState.isPending,
        open: hoisted.storeState.open,
        rowCount: hoisted.storeState.rowCount,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.storeState.isPending = false;
    hoisted.storeState.open = true;
    hoisted.storeState.rowCount = 5;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DeleteDataTableRowsDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open', () => {
            render(<DeleteDataTableRowsDialog />);

            expect(screen.getByText('Delete records')).toBeInTheDocument();
        });

        it('should render the description with plural row count', () => {
            render(<DeleteDataTableRowsDialog />);

            expect(
                screen.getByText('Are you sure you want to delete 5 selected records? This action cannot be undone.')
            ).toBeInTheDocument();
        });

        it('should render the description with singular row count', () => {
            hoisted.storeState.rowCount = 1;

            render(<DeleteDataTableRowsDialog />);

            expect(
                screen.getByText('Are you sure you want to delete 1 selected record? This action cannot be undone.')
            ).toBeInTheDocument();
        });

        it('should render cancel and delete buttons', () => {
            render(<DeleteDataTableRowsDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
        });

        it('should not render when not open', () => {
            hoisted.storeState.open = false;

            render(<DeleteDataTableRowsDialog />);

            expect(screen.queryByText('Delete records')).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call handleDelete when delete button is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableRowsDialog />);

            const deleteButton = screen.getByRole('button', {name: 'Delete'});

            await user.click(deleteButton);

            expect(hoisted.mockHandleDelete).toHaveBeenCalledTimes(1);
        });

        it('should call handleClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableRowsDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleClose).toHaveBeenCalledTimes(1);
        });

        it('should call handleClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableRowsDialog />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(hoisted.mockHandleClose).toHaveBeenCalledTimes(1);
        });

        it('should disable the delete button while deleting', () => {
            hoisted.storeState.isPending = true;

            render(<DeleteDataTableRowsDialog />);

            expect(screen.getByRole('button', {name: 'Delete'})).toBeDisabled();
        });
    });

    describe('different row counts', () => {
        it('should display correct count for many rows', () => {
            hoisted.storeState.rowCount = 100;

            render(<DeleteDataTableRowsDialog />);

            expect(
                screen.getByText('Are you sure you want to delete 100 selected records? This action cannot be undone.')
            ).toBeInTheDocument();
        });

        it('should display correct count for zero rows', () => {
            hoisted.storeState.rowCount = 0;

            render(<DeleteDataTableRowsDialog />);

            expect(
                screen.getByText('Are you sure you want to delete 0 selected records? This action cannot be undone.')
            ).toBeInTheDocument();
        });
    });
});
