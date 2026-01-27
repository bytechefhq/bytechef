import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DuplicateDataTableDialog from '../DuplicateDataTableDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleDuplicateSubmit: vi.fn(),
        handleDuplicateValueChange: vi.fn(),
        mockUseDuplicateDataTableDialog: vi.fn(),
    };
});

vi.mock('../hooks/useDuplicateDataTableDialog', () => ({
    default: hoisted.mockUseDuplicateDataTableDialog,
}));

const defaultMockReturn = {
    canDuplicate: true,
    duplicateValue: 'orders_copy',
    handleClose: hoisted.handleClose,
    handleDuplicateSubmit: hoisted.handleDuplicateSubmit,
    handleDuplicateValueChange: hoisted.handleDuplicateValueChange,
    handleOpen: vi.fn(),
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseDuplicateDataTableDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DuplicateDataTableDialog', () => {
    it('should render the dialog when open is true', () => {
        render(<DuplicateDataTableDialog />);

        expect(screen.getByText('Duplicate table')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        render(<DuplicateDataTableDialog />);

        expect(screen.getByText('Enter a name for the duplicated table.')).toBeInTheDocument();
    });

    it('should render Cancel and Duplicate buttons', () => {
        render(<DuplicateDataTableDialog />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Duplicate'})).toBeInTheDocument();
    });

    it('should display input with duplicate value', () => {
        render(<DuplicateDataTableDialog />);

        const input = screen.getByDisplayValue('orders_copy');

        expect(input).toBeInTheDocument();
    });

    it('should call handleDuplicateSubmit when clicking Duplicate button', async () => {
        render(<DuplicateDataTableDialog />);

        const duplicateButton = screen.getByRole('button', {name: 'Duplicate'});
        await userEvent.click(duplicateButton);

        expect(hoisted.handleDuplicateSubmit).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        render(<DuplicateDataTableDialog />);

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalled();
    });
});

describe('DuplicateDataTableDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseDuplicateDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            open: false,
        });
    });

    it('should not render the dialog content when open is false', () => {
        render(<DuplicateDataTableDialog />);

        expect(screen.queryByText('Duplicate table')).not.toBeInTheDocument();
    });
});

describe('DuplicateDataTableDialog canDuplicate disabled', () => {
    beforeEach(() => {
        hoisted.mockUseDuplicateDataTableDialog.mockReturnValue({
            ...defaultMockReturn,
            canDuplicate: false,
            duplicateValue: '',
        });
    });

    it('should disable Duplicate button when canDuplicate is false', () => {
        render(<DuplicateDataTableDialog />);

        const duplicateButton = screen.getByRole('button', {name: 'Duplicate'});

        expect(duplicateButton).toBeDisabled();
    });
});
