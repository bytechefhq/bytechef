import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteUserAlertDialog from '../DeleteUserAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleDelete: vi.fn(),
        mockUseDeleteUserAlertDialog: vi.fn(),
    };
});

vi.mock('../hooks/useDeleteUserAlertDialog', () => ({
    default: hoisted.mockUseDeleteUserAlertDialog,
}));

const defaultMockReturn = {
    handleClose: hoisted.handleClose,
    handleDelete: hoisted.handleDelete,
    handleOpen: vi.fn(),
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseDeleteUserAlertDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderDeleteUserAlertDialog = () => {
    return render(<DeleteUserAlertDialog />);
};

describe('DeleteUserAlertDialog', () => {
    it('should render the dialog when open is true', () => {
        renderDeleteUserAlertDialog();

        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        renderDeleteUserAlertDialog();

        expect(
            screen.getByText('This action cannot be undone. This will permanently delete data.')
        ).toBeInTheDocument();
    });

    it('should render Cancel and Delete buttons', () => {
        renderDeleteUserAlertDialog();

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should call handleDelete when clicking Delete button', async () => {
        renderDeleteUserAlertDialog();

        const deleteButton = screen.getByRole('button', {name: 'Delete'});
        await userEvent.click(deleteButton);

        expect(hoisted.handleDelete).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        renderDeleteUserAlertDialog();

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalledTimes(1);
    });
});

describe('DeleteUserAlertDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseDeleteUserAlertDialog.mockReturnValue({
            handleClose: hoisted.handleClose,
            handleDelete: hoisted.handleDelete,
            handleOpen: vi.fn(),
            open: false,
        });
    });

    it('should not render the dialog content when open is false', () => {
        renderDeleteUserAlertDialog();

        expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
    });
});
