import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AddDataTableColumnDialog from '../AddDataTableColumnDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleAdd: vi.fn(),
        mockHandleOpenChange: vi.fn(),
        mockOpen: true,
    };
});

vi.mock('../../hooks/useAddDataTableColumnDialog', () => ({
    default: () => ({
        handleAdd: hoisted.mockHandleAdd,
        handleOpenChange: hoisted.mockHandleOpenChange,
        open: hoisted.mockOpen,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockOpen = true;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AddDataTableColumnDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open is true', () => {
            render(<AddDataTableColumnDialog />);

            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });

        it('should not render the dialog when open is false', () => {
            hoisted.mockOpen = false;

            render(<AddDataTableColumnDialog />);

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('should render the dialog title', () => {
            render(<AddDataTableColumnDialog />);

            expect(screen.getByText('Add Column')).toBeInTheDocument();
        });

        it('should render the name input with label', () => {
            render(<AddDataTableColumnDialog />);

            expect(screen.getByText('Name')).toBeInTheDocument();
            expect(screen.getByRole('textbox')).toBeInTheDocument();
        });

        it('should render the type select with label', () => {
            render(<AddDataTableColumnDialog />);

            expect(screen.getByText('Type')).toBeInTheDocument();
            expect(screen.getByRole('combobox')).toBeInTheDocument();
        });

        it('should render Cancel and Add buttons', () => {
            render(<AddDataTableColumnDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Add'})).toBeInTheDocument();
        });

        it('should have STRING as default column type', () => {
            render(<AddDataTableColumnDialog />);

            expect(screen.getByRole('combobox')).toHaveTextContent('STRING');
        });
    });

    describe('interactions', () => {
        it('should update name input when typing', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            const input = screen.getByRole('textbox');

            await user.type(input, 'newColumn');

            expect(input).toHaveValue('newColumn');
        });

        it('should call handleOpenChange with false when Cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleOpenChange).toHaveBeenCalledWith(false);
        });

        it('should call handleAdd with name and type when Add is clicked with valid name', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            const input = screen.getByRole('textbox');

            await user.type(input, 'myColumn');

            const addButton = screen.getByRole('button', {name: 'Add'});

            await user.click(addButton);

            expect(hoisted.mockHandleAdd).toHaveBeenCalledWith('myColumn', 'STRING');
        });

        it('should not call handleAdd when Add is clicked with empty name', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            const addButton = screen.getByRole('button', {name: 'Add'});

            await user.click(addButton);

            expect(hoisted.mockHandleAdd).not.toHaveBeenCalled();
        });
    });

    describe('form reset', () => {
        it('should have empty name when dialog is initially opened', () => {
            render(<AddDataTableColumnDialog />);

            const input = screen.getByRole('textbox');

            expect(input).toHaveValue('');
        });
    });
});
