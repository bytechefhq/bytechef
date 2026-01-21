import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteDataTableColumnDialog from '../DeleteDataTableColumnDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleDelete: vi.fn(),
        mockHandleOpenChange: vi.fn(),
        storeState: {
            columnName: 'TestColumn',
            open: true,
        },
    };
});

vi.mock('../../hooks/useDeleteDataTableColumnDialog', () => ({
    default: () => ({
        columnName: hoisted.storeState.columnName,
        handleDelete: hoisted.mockHandleDelete,
        handleOpenChange: hoisted.mockHandleOpenChange,
        open: hoisted.storeState.open,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.storeState.open = true;
    hoisted.storeState.columnName = 'TestColumn';
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DeleteDataTableColumnDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open', () => {
            render(<DeleteDataTableColumnDialog />);

            expect(screen.getByText('Delete column')).toBeInTheDocument();
        });

        it('should render the description with column name', () => {
            render(<DeleteDataTableColumnDialog />);

            expect(
                screen.getByText(
                    'Are you sure you want to delete column "TestColumn"? This action cannot be undone and will remove all data in this column.'
                )
            ).toBeInTheDocument();
        });

        it('should render cancel and delete buttons', () => {
            render(<DeleteDataTableColumnDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
        });

        it('should not render when not open', () => {
            hoisted.storeState.open = false;

            render(<DeleteDataTableColumnDialog />);

            expect(screen.queryByText('Delete column')).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call handleDelete when delete button is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableColumnDialog />);

            const deleteButton = screen.getByRole('button', {name: 'Delete'});

            await user.click(deleteButton);

            expect(hoisted.mockHandleDelete).toHaveBeenCalledTimes(1);
        });

        it('should call handleOpenChange when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableColumnDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleOpenChange).toHaveBeenCalledWith(false);
        });
    });

    describe('different column names', () => {
        it('should display different column name in description', () => {
            hoisted.storeState.columnName = 'AnotherColumn';

            render(<DeleteDataTableColumnDialog />);

            expect(
                screen.getByText(
                    'Are you sure you want to delete column "AnotherColumn"? This action cannot be undone and will remove all data in this column.'
                )
            ).toBeInTheDocument();
        });
    });
});
