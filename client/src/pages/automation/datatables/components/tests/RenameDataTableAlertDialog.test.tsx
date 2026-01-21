import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import RenameDataTableAlertDialog from '../RenameDataTableAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleRenameSubmit: vi.fn(),
        handleRenameValueChange: vi.fn(),
        mockUseRenameDataTableAlertDialog: vi.fn(),
    };
});

vi.mock('../hooks/useRenameDataTableAlertDialog', () => ({
    default: hoisted.mockUseRenameDataTableAlertDialog,
}));

const defaultMockReturn = {
    canRename: true,
    handleClose: hoisted.handleClose,
    handleOpen: vi.fn(),
    handleRenameSubmit: hoisted.handleRenameSubmit,
    handleRenameValueChange: hoisted.handleRenameValueChange,
    open: true,
    renameValue: 'orders_renamed',
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseRenameDataTableAlertDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('RenameDataTableAlertDialog', () => {
    it('should render the dialog when open is true', () => {
        render(<RenameDataTableAlertDialog />);

        expect(screen.getByText('Rename Table')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        render(<RenameDataTableAlertDialog />);

        expect(screen.getByText('Enter a new name for this data table.')).toBeInTheDocument();
    });

    it('should render Cancel and Rename buttons', () => {
        render(<RenameDataTableAlertDialog />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Rename'})).toBeInTheDocument();
    });

    it('should display input with rename value', () => {
        render(<RenameDataTableAlertDialog />);

        const input = screen.getByDisplayValue('orders_renamed');

        expect(input).toBeInTheDocument();
    });

    it('should call handleRenameSubmit when clicking Rename button', async () => {
        render(<RenameDataTableAlertDialog />);

        const renameButton = screen.getByRole('button', {name: 'Rename'});
        await userEvent.click(renameButton);

        expect(hoisted.handleRenameSubmit).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        render(<RenameDataTableAlertDialog />);

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalled();
    });
});

describe('RenameDataTableAlertDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseRenameDataTableAlertDialog.mockReturnValue({
            ...defaultMockReturn,
            open: false,
        });
    });

    it('should not render the dialog content when open is false', () => {
        render(<RenameDataTableAlertDialog />);

        expect(screen.queryByText('Rename Table')).not.toBeInTheDocument();
    });
});

describe('RenameDataTableAlertDialog canRename disabled', () => {
    beforeEach(() => {
        hoisted.mockUseRenameDataTableAlertDialog.mockReturnValue({
            ...defaultMockReturn,
            canRename: false,
            renameValue: '',
        });
    });

    it('should disable Rename button when canRename is false', () => {
        render(<RenameDataTableAlertDialog />);

        const renameButton = screen.getByRole('button', {name: 'Rename'});

        expect(renameButton).toBeDisabled();
    });
});
