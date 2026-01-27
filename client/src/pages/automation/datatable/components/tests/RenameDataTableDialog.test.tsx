import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import RenameDataTableDialog from '../RenameDataTableDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleOpenChange: vi.fn(),
        mockHandleRenameSubmit: vi.fn(),
        mockHandleRenameValueChange: vi.fn(),
        storeState: {
            canRename: true,
            open: true,
            renameValue: 'CurrentName',
        },
    };
});

vi.mock('../../hooks/useRenameDataTableDialog', () => ({
    default: () => ({
        canRename: hoisted.storeState.canRename,
        handleOpenChange: hoisted.mockHandleOpenChange,
        handleRenameSubmit: hoisted.mockHandleRenameSubmit,
        handleRenameValueChange: hoisted.mockHandleRenameValueChange,
        open: hoisted.storeState.open,
        renameValue: hoisted.storeState.renameValue,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.storeState.open = true;
    hoisted.storeState.renameValue = 'CurrentName';
    hoisted.storeState.canRename = true;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('RenameDataTableDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open', () => {
            render(<RenameDataTableDialog />);

            expect(screen.getByText('Rename Table')).toBeInTheDocument();
        });

        it('should render the input with current value', () => {
            render(<RenameDataTableDialog />);

            const input = screen.getByRole('textbox');

            expect(input).toHaveValue('CurrentName');
        });

        it('should render cancel and rename buttons', () => {
            render(<RenameDataTableDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Rename'})).toBeInTheDocument();
        });

        it('should not render when not open', () => {
            hoisted.storeState.open = false;

            render(<RenameDataTableDialog />);

            expect(screen.queryByText('Rename Table')).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call handleRenameValueChange when input value changes', async () => {
            const user = userEvent.setup();

            render(<RenameDataTableDialog />);

            const input = screen.getByRole('textbox');

            await user.clear(input);
            await user.type(input, 'NewName');

            expect(hoisted.mockHandleRenameValueChange).toHaveBeenCalled();
        });

        it('should call handleRename when rename button is clicked', async () => {
            const user = userEvent.setup();

            render(<RenameDataTableDialog />);

            const renameButton = screen.getByRole('button', {name: 'Rename'});

            await user.click(renameButton);

            expect(hoisted.mockHandleRenameSubmit).toHaveBeenCalledTimes(1);
        });

        it('should call handleOpenChange when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<RenameDataTableDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleOpenChange).toHaveBeenCalledWith(false);
        });
    });

    describe('rename button state', () => {
        it('should disable rename button when canRename is false', () => {
            hoisted.storeState.canRename = false;

            render(<RenameDataTableDialog />);

            const renameButton = screen.getByRole('button', {name: 'Rename'});

            expect(renameButton).toBeDisabled();
        });

        it('should enable rename button when canRename is true', () => {
            hoisted.storeState.canRename = true;

            render(<RenameDataTableDialog />);

            const renameButton = screen.getByRole('button', {name: 'Rename'});

            expect(renameButton).not.toBeDisabled();
        });
    });
});
