import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import RenameDataTableDialog from '../RenameDataTableDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleOpenChange: vi.fn(),
        handleRenameSubmit: vi.fn(),
        handleRenameValueChange: vi.fn(),
        mockUseRenameDataTableDialog: vi.fn(),
    };
});

vi.mock('../hooks/useRenameDataTableDialog', () => ({
    default: hoisted.mockUseRenameDataTableDialog,
}));

const defaultMockReturn = {
    canRename: true,
    handleClose: vi.fn(),
    handleOpen: vi.fn(),
    handleOpenChange: hoisted.handleOpenChange,
    handleRenameSubmit: hoisted.handleRenameSubmit,
    handleRenameValueChange: hoisted.handleRenameValueChange,
    open: true,
    renameValue: 'orders_renamed',
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseRenameDataTableDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('RenameDataTableDialog', () => {
    it('should render the dialog when open is true', () => {
        render(<RenameDataTableDialog />);

        expect(screen.getByText('Rename Table')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        render(<RenameDataTableDialog />);

        expect(screen.getByText('Enter a new base name for this table.')).toBeInTheDocument();
    });

    it('should render Cancel and Rename buttons', () => {
        render(<RenameDataTableDialog />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Rename'})).toBeInTheDocument();
    });

    it('should display input with rename value', () => {
        render(<RenameDataTableDialog />);

        const input = screen.getByDisplayValue('orders_renamed');

        expect(input).toBeInTheDocument();
    });

    it('should call handleRenameSubmit when clicking Rename button', async () => {
        render(<RenameDataTableDialog />);

        const renameButton = screen.getByRole('button', {name: 'Rename'});
        await userEvent.click(renameButton);

        expect(hoisted.handleRenameSubmit).toHaveBeenCalledTimes(1);
    });

    it('should call handleOpenChange with false when clicking Cancel button', async () => {
        render(<RenameDataTableDialog />);

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleOpenChange).toHaveBeenCalledWith(false);
    });
});

describe('RenameDataTableDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseRenameDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            open: false,
        });
    });

    it('should not render the dialog content when open is false', () => {
        render(<RenameDataTableDialog />);

        expect(screen.queryByText('Rename Table')).not.toBeInTheDocument();
    });
});

describe('RenameDataTableDialog canRename disabled', () => {
    beforeEach(() => {
        hoisted.mockUseRenameDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            canRename: false,
            renameValue: '',
        });
    });

    it('should disable Rename button when canRename is false', () => {
        render(<RenameDataTableDialog />);

        const renameButton = screen.getByRole('button', {name: 'Rename'});

        expect(renameButton).toBeDisabled();
    });
});
