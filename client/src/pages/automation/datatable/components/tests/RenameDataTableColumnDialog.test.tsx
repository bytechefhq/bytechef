import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import RenameDataTableColumnDialog from '../RenameDataTableColumnDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockCanRename: true,
        mockCurrentName: 'originalColumn',
        mockHandleOpenChange: vi.fn(),
        mockHandleRenameSubmit: vi.fn(),
        mockHandleRenameValueChange: vi.fn(),
        mockOpen: true,
        mockRenameValue: 'originalColumn',
    };
});

vi.mock('../../hooks/useRenameDataTableColumnDialog', () => ({
    default: () => ({
        canRename: hoisted.mockCanRename,
        currentName: hoisted.mockCurrentName,
        handleOpenChange: hoisted.mockHandleOpenChange,
        handleRenameSubmit: hoisted.mockHandleRenameSubmit,
        handleRenameValueChange: hoisted.mockHandleRenameValueChange,
        open: hoisted.mockOpen,
        renameValue: hoisted.mockRenameValue,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockOpen = true;
    hoisted.mockCurrentName = 'originalColumn';
    hoisted.mockRenameValue = 'originalColumn';
    hoisted.mockCanRename = true;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('RenameDataTableColumnDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open is true', () => {
            render(<RenameDataTableColumnDialog />);

            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });

        it('should not render the dialog when open is false', () => {
            hoisted.mockOpen = false;

            render(<RenameDataTableColumnDialog />);

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('should render the dialog title', () => {
            render(<RenameDataTableColumnDialog />);

            expect(screen.getByText('Rename Column')).toBeInTheDocument();
        });

        it('should display the dialog description', () => {
            render(<RenameDataTableColumnDialog />);

            expect(screen.getByText('Enter a new name for this column.')).toBeInTheDocument();
        });

        it('should render the current column name in label', () => {
            hoisted.mockCurrentName = 'myColumn';

            render(<RenameDataTableColumnDialog />);

            expect(screen.getByText('New name for "myColumn"')).toBeInTheDocument();
        });

        it('should render Cancel and Rename buttons', () => {
            render(<RenameDataTableColumnDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Rename'})).toBeInTheDocument();
        });

        it('should display the rename value in the input', () => {
            hoisted.mockRenameValue = 'myColumn';

            render(<RenameDataTableColumnDialog />);

            const input = screen.getByRole('textbox');

            expect(input).toHaveValue('myColumn');
        });

        it('should have placeholder text in input', () => {
            render(<RenameDataTableColumnDialog />);

            const input = screen.getByRole('textbox');

            expect(input).toHaveAttribute('placeholder', 'Enter new column name');
        });
    });

    describe('validation', () => {
        it('should disable Rename button when canRename is false', () => {
            hoisted.mockCanRename = false;

            render(<RenameDataTableColumnDialog />);

            const renameButton = screen.getByRole('button', {name: 'Rename'});

            expect(renameButton).toBeDisabled();
        });

        it('should enable Rename button when canRename is true', () => {
            hoisted.mockCanRename = true;

            render(<RenameDataTableColumnDialog />);

            const renameButton = screen.getByRole('button', {name: 'Rename'});

            expect(renameButton).not.toBeDisabled();
        });
    });

    describe('interactions', () => {
        it('should call handleRenameValueChange when typing', async () => {
            const user = userEvent.setup();

            render(<RenameDataTableColumnDialog />);

            const input = screen.getByRole('textbox');

            await user.type(input, 'x');

            expect(hoisted.mockHandleRenameValueChange).toHaveBeenCalled();
        });

        it('should call handleOpenChange with false when Cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<RenameDataTableColumnDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleOpenChange).toHaveBeenCalledWith(false);
        });

        it('should call handleRename when Rename is clicked and canRename is true', async () => {
            const user = userEvent.setup();

            hoisted.mockCanRename = true;

            render(<RenameDataTableColumnDialog />);

            const renameButton = screen.getByRole('button', {name: 'Rename'});

            await user.click(renameButton);

            expect(hoisted.mockHandleRenameSubmit).toHaveBeenCalled();
        });

        it('should not call handleRename when Rename button is disabled', async () => {
            const user = userEvent.setup();

            hoisted.mockCanRename = false;

            render(<RenameDataTableColumnDialog />);

            const renameButton = screen.getByRole('button', {name: 'Rename'});

            await user.click(renameButton);

            expect(hoisted.mockHandleRenameSubmit).not.toHaveBeenCalled();
        });
    });
});
