import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CreateDataTableDialog from '../CreateDataTableDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleAddColumn: vi.fn(),
        handleBaseNameChange: vi.fn(),
        handleClose: vi.fn(),
        handleColumnNameChange: vi.fn(),
        handleColumnTypeChange: vi.fn(),
        handleCreate: vi.fn(),
        handleDescriptionChange: vi.fn(),
        handleOpen: vi.fn(),
        handleRemoveColumn: vi.fn(),
        mockUseCreateDataTableDialog: vi.fn(),
    };
});

vi.mock('../hooks/useCreateDataTableDialog', () => ({
    default: hoisted.mockUseCreateDataTableDialog,
}));

const defaultMockReturn = {
    baseName: 'orders',
    canSubmit: true,
    columns: [{name: 'id', type: 'INTEGER'}],
    description: 'Order table',
    handleAddColumn: hoisted.handleAddColumn,
    handleBaseNameChange: hoisted.handleBaseNameChange,
    handleClose: hoisted.handleClose,
    handleColumnNameChange: hoisted.handleColumnNameChange,
    handleColumnTypeChange: hoisted.handleColumnTypeChange,
    handleCreate: hoisted.handleCreate,
    handleDescriptionChange: hoisted.handleDescriptionChange,
    handleOpen: hoisted.handleOpen,
    handleRemoveColumn: hoisted.handleRemoveColumn,
    isPending: false,
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseCreateDataTableDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CreateDataTableDialog', () => {
    it('should render the dialog when open is true', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByText('Create Data Table')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        render(<CreateDataTableDialog />);

        expect(
            screen.getByText('Provide a base name, an optional description, and at least one column.')
        ).toBeInTheDocument();
    });

    it('should render Base name label and input', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByText('Base name')).toBeInTheDocument();
        expect(screen.getByDisplayValue('orders')).toBeInTheDocument();
    });

    it('should render Description label and input', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByText('Description (optional)')).toBeInTheDocument();
        expect(screen.getByDisplayValue('Order table')).toBeInTheDocument();
    });

    it('should render Columns label', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByText('Columns')).toBeInTheDocument();
    });

    it('should render Add column button', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByRole('button', {name: /Add column/})).toBeInTheDocument();
    });

    it('should render column input with name', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByDisplayValue('id')).toBeInTheDocument();
    });

    it('should render Create button', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByRole('button', {name: 'Create'})).toBeInTheDocument();
    });

    it('should call handleAddColumn when clicking Add column button', async () => {
        render(<CreateDataTableDialog />);

        const addButton = screen.getByRole('button', {name: /Add column/});
        await userEvent.click(addButton);

        expect(hoisted.handleAddColumn).toHaveBeenCalledTimes(1);
    });

    it('should call handleCreate when clicking Create button', async () => {
        render(<CreateDataTableDialog />);

        const createButton = screen.getByRole('button', {name: 'Create'});
        await userEvent.click(createButton);

        expect(hoisted.handleCreate).toHaveBeenCalledTimes(1);
    });
});

describe('CreateDataTableDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseCreateDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            open: false,
        });
    });

    it('should render trigger button when closed', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByRole('button', {name: 'Create Table'})).toBeInTheDocument();
    });

    it('should not render dialog content when closed', () => {
        render(<CreateDataTableDialog />);

        expect(screen.queryByText('Create Data Table')).not.toBeInTheDocument();
    });
});

describe('CreateDataTableDialog pending state', () => {
    beforeEach(() => {
        hoisted.mockUseCreateDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            isPending: true,
        });
    });

    it('should show Creating... text when pending', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByRole('button', {name: 'Creating...'})).toBeInTheDocument();
    });

    it('should disable Create button when pending', () => {
        render(<CreateDataTableDialog />);

        const createButton = screen.getByRole('button', {name: 'Creating...'});

        expect(createButton).toBeDisabled();
    });
});

describe('CreateDataTableDialog canSubmit disabled', () => {
    beforeEach(() => {
        hoisted.mockUseCreateDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            baseName: '',
            canSubmit: false,
        });
    });

    it('should disable Create button when canSubmit is false', () => {
        render(<CreateDataTableDialog />);

        const createButton = screen.getByRole('button', {name: 'Create'});

        expect(createButton).toBeDisabled();
    });
});

describe('CreateDataTableDialog with multiple columns', () => {
    beforeEach(() => {
        hoisted.mockUseCreateDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            columns: [
                {name: 'id', type: 'INTEGER'},
                {name: 'name', type: 'STRING'},
            ],
        });
    });

    it('should render multiple column inputs', () => {
        render(<CreateDataTableDialog />);

        expect(screen.getByDisplayValue('id')).toBeInTheDocument();
        expect(screen.getByDisplayValue('name')).toBeInTheDocument();
    });

    it('should render remove column buttons when multiple columns exist', () => {
        render(<CreateDataTableDialog />);

        const removeButtons = screen.getAllByRole('button').filter((button) => button.querySelector('.lucide-trash-2'));

        expect(removeButtons.length).toBe(2);
    });
});

describe('CreateDataTableDialog with custom trigger', () => {
    beforeEach(() => {
        hoisted.mockUseCreateDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            open: false,
        });
    });

    it('should render custom trigger when provided', () => {
        render(<CreateDataTableDialog trigger={<button>Custom Trigger</button>} />);

        expect(screen.getByRole('button', {name: 'Custom Trigger'})).toBeInTheDocument();
    });
});
