import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AddDataTableColumnDialog from '../AddDataTableColumnDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleAdd: vi.fn(),
        mockHandleOpenChange: vi.fn(),
        mockOpen: true,
        mockTargetTables: [
            {
                baseName: 'customers',
                columns: [
                    {id: '1', name: 'name', type: 'STRING'},
                    {id: '2', name: 'parent', type: 'LINK'},
                ],
                id: '7',
            },
        ],
    };
});

vi.mock('../../hooks/useAddDataTableColumnDialog', () => ({
    default: () => ({
        handleAdd: hoisted.mockHandleAdd,
        handleOpenChange: hoisted.mockHandleOpenChange,
        open: hoisted.mockOpen,
        targetTables: hoisted.mockTargetTables,
    }),
}));

beforeEach(() => {
    windowResizeObserver();

    Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.setPointerCapture = vi.fn();

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

    describe('link columns', () => {
        const chooseOption = async (
            user: ReturnType<typeof userEvent.setup>,
            triggerName: string,
            optionName: string
        ) => {
            await user.click(screen.getByRole('combobox', {name: triggerName}));
            await user.click(await screen.findByRole('option', {name: optionName}));
        };

        it('shows target table, display column and delete rule when LINK is chosen', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            expect(screen.queryByText('Target table')).not.toBeInTheDocument();

            await user.click(screen.getByRole('combobox'));
            await user.click(await screen.findByRole('option', {name: 'LINK'}));

            expect(screen.getByRole('combobox', {name: 'Target table'})).toBeInTheDocument();
            expect(screen.getByRole('combobox', {name: 'Display column'})).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: 'Prevent deleting linked rows'})).toBeChecked();
            expect(screen.getByRole('radio', {name: 'Clear the link'})).not.toBeChecked();
        });

        it('sends the link input with BLOCK by default', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            await user.type(screen.getByRole('textbox'), 'customer');

            await chooseOption(user, 'Type', 'LINK');
            await chooseOption(user, 'Target table', 'customers');
            await chooseOption(user, 'Display column', 'name');

            expect(screen.queryByRole('option', {name: 'parent'})).not.toBeInTheDocument();

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(hoisted.mockHandleAdd).toHaveBeenCalledWith('customer', 'LINK', {
                displayColumnName: 'name',
                onDelete: 'BLOCK',
                targetDataTableId: '7',
            });
        });

        it('sends CLEAR when the clear rule is chosen', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            await user.type(screen.getByRole('textbox'), 'customer');

            await chooseOption(user, 'Type', 'LINK');
            await chooseOption(user, 'Target table', 'customers');

            await user.click(screen.getByRole('radio', {name: 'Clear the link'}));
            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(hoisted.mockHandleAdd).toHaveBeenCalledWith('customer', 'LINK', {
                displayColumnName: undefined,
                onDelete: 'CLEAR',
                targetDataTableId: '7',
            });
        });

        it('does not add a link column without a target table', async () => {
            const user = userEvent.setup();

            render(<AddDataTableColumnDialog />);

            await user.type(screen.getByRole('textbox'), 'customer');

            await chooseOption(user, 'Type', 'LINK');

            expect(screen.getByRole('button', {name: 'Add'})).toBeDisabled();
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
