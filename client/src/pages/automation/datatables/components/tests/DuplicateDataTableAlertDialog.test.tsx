import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DuplicateDataTableAlertDialog from '../DuplicateDataTableAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleDuplicateSubmit: vi.fn(),
        handleDuplicateValueChange: vi.fn(),
        mockUseDuplicateDataTableAlertDialog: vi.fn(),
    };
});

vi.mock('../hooks/useDuplicateDataTableAlertDialog', () => ({
    default: hoisted.mockUseDuplicateDataTableAlertDialog,
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
    hoisted.mockUseDuplicateDataTableAlertDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DuplicateDataTableAlertDialog', () => {
    it('should render the dialog when open is true', () => {
        render(<DuplicateDataTableAlertDialog />);

        expect(screen.getByText('Duplicate table')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        render(<DuplicateDataTableAlertDialog />);

        expect(screen.getByText('Enter a name for the duplicated table.')).toBeInTheDocument();
    });

    it('should render Cancel and Duplicate buttons', () => {
        render(<DuplicateDataTableAlertDialog />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Duplicate'})).toBeInTheDocument();
    });

    it('should display input with duplicate value', () => {
        render(<DuplicateDataTableAlertDialog />);

        const input = screen.getByDisplayValue('orders_copy');

        expect(input).toBeInTheDocument();
    });

    it('should call handleDuplicateSubmit when clicking Duplicate button', async () => {
        render(<DuplicateDataTableAlertDialog />);

        const duplicateButton = screen.getByRole('button', {name: 'Duplicate'});
        await userEvent.click(duplicateButton);

        expect(hoisted.handleDuplicateSubmit).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        render(<DuplicateDataTableAlertDialog />);

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalled();
    });
});

describe('DuplicateDataTableAlertDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseDuplicateDataTableAlertDialog.mockReturnValue({
            ...defaultMockReturn,
            open: false,
        });
    });

    it('should not render the dialog content when open is false', () => {
        render(<DuplicateDataTableAlertDialog />);

        expect(screen.queryByText('Duplicate table')).not.toBeInTheDocument();
    });
});

describe('DuplicateDataTableAlertDialog canDuplicate disabled', () => {
    beforeEach(() => {
        hoisted.mockUseDuplicateDataTableAlertDialog.mockReturnValue({
            ...defaultMockReturn,
            canDuplicate: false,
            duplicateValue: '',
        });
    });

    it('should disable Duplicate button when canDuplicate is false', () => {
        render(<DuplicateDataTableAlertDialog />);

        const duplicateButton = screen.getByRole('button', {name: 'Duplicate'});

        expect(duplicateButton).toBeDisabled();
    });
});
