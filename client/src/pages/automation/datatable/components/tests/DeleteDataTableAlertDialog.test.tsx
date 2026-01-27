import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteDataTableAlertDialog from '../DeleteDataTableAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleClose: vi.fn(),
        mockHandleDelete: vi.fn(),
        storeState: {
            open: true,
        },
    };
});

vi.mock('../../hooks/useDeleteDataTableAlertDialog', () => ({
    default: () => ({
        handleClose: hoisted.mockHandleClose,
        handleDelete: hoisted.mockHandleDelete,
        open: hoisted.storeState.open,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.storeState.open = true;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DeleteDataTableAlertDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open', () => {
            render(<DeleteDataTableAlertDialog />);

            expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
        });

        it('should render cancel and delete buttons', () => {
            render(<DeleteDataTableAlertDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
        });

        it('should not render when not open', () => {
            hoisted.storeState.open = false;

            render(<DeleteDataTableAlertDialog />);

            expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call handleDelete when delete button is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableAlertDialog />);

            const deleteButton = screen.getByRole('button', {name: 'Delete'});

            await user.click(deleteButton);

            expect(hoisted.mockHandleDelete).toHaveBeenCalledTimes(1);
        });

        it('should call handleClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<DeleteDataTableAlertDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleClose).toHaveBeenCalledTimes(1);
        });
    });
});
