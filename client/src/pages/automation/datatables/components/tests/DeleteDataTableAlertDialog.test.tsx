import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteDataTableAlertDialog from '../DeleteDataTableAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleDelete: vi.fn(),
        mockUseDeleteDataTableAlertDialog: vi.fn(),
    };
});

vi.mock('../hooks/useDeleteDataTableAlertDialog', () => ({
    default: hoisted.mockUseDeleteDataTableAlertDialog,
}));

const defaultMockReturn = {
    handleClose: hoisted.handleClose,
    handleDelete: hoisted.handleDelete,
    handleOpen: vi.fn(),
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseDeleteDataTableAlertDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DeleteDataTableAlertDialog', () => {
    it('should render the dialog when open is true', () => {
        render(<DeleteDataTableAlertDialog />);

        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        render(<DeleteDataTableAlertDialog />);

        expect(
            screen.getByText('This action cannot be undone. This will permanently delete data.')
        ).toBeInTheDocument();
    });

    it('should render Cancel and Delete buttons', () => {
        render(<DeleteDataTableAlertDialog />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should call handleDelete when clicking Delete button', async () => {
        render(<DeleteDataTableAlertDialog />);

        const deleteButton = screen.getByRole('button', {name: 'Delete'});
        await userEvent.click(deleteButton);

        expect(hoisted.handleDelete).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        render(<DeleteDataTableAlertDialog />);

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalledTimes(1);
    });
});

describe('DeleteDataTableAlertDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseDeleteDataTableAlertDialog.mockReturnValue({
            handleClose: hoisted.handleClose,
            handleDelete: hoisted.handleDelete,
            handleOpen: vi.fn(),
            open: false,
        });
    });

    it('should not render the dialog content when open is false', () => {
        render(<DeleteDataTableAlertDialog />);

        expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
    });
});
