import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ColumnHeaderCell from '../ColumnHeaderCell';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ColumnHeaderCell', () => {
    const defaultProps = {
        columnId: 'col-123',
        columnName: 'Test Column',
        onDelete: vi.fn(),
        onRename: vi.fn(),
    };

    describe('rendering', () => {
        it('should render the column name', () => {
            render(<ColumnHeaderCell {...defaultProps} />);

            expect(screen.getByText('Test Column')).toBeInTheDocument();
        });

        it('should render the menu button with correct aria-label', () => {
            render(<ColumnHeaderCell {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Column Test Column menu'})).toBeInTheDocument();
        });

        it('should have title attribute on column name span', () => {
            render(<ColumnHeaderCell {...defaultProps} />);

            const columnNameSpan = screen.getByText('Test Column');

            expect(columnNameSpan).toHaveAttribute('title', 'Test Column');
        });
    });

    describe('dropdown menu', () => {
        it('should show rename and delete options when menu is opened', async () => {
            const user = userEvent.setup();

            render(<ColumnHeaderCell {...defaultProps} />);

            const menuButton = screen.getByRole('button', {name: 'Column Test Column menu'});

            await user.click(menuButton);

            expect(screen.getByText('Rename')).toBeInTheDocument();
            expect(screen.getByText('Delete')).toBeInTheDocument();
        });

        it('should call onRename with correct arguments when rename is clicked', async () => {
            const user = userEvent.setup();
            const mockOnRename = vi.fn();

            render(<ColumnHeaderCell {...defaultProps} onRename={mockOnRename} />);

            const menuButton = screen.getByRole('button', {name: 'Column Test Column menu'});

            await user.click(menuButton);

            const renameOption = screen.getByText('Rename');

            await user.click(renameOption);

            expect(mockOnRename).toHaveBeenCalledWith('col-123', 'Test Column');
        });

        it('should call onDelete with correct arguments when delete is clicked', async () => {
            const user = userEvent.setup();
            const mockOnDelete = vi.fn();

            render(<ColumnHeaderCell {...defaultProps} onDelete={mockOnDelete} />);

            const menuButton = screen.getByRole('button', {name: 'Column Test Column menu'});

            await user.click(menuButton);

            const deleteOption = screen.getByText('Delete');

            await user.click(deleteOption);

            expect(mockOnDelete).toHaveBeenCalledWith('col-123', 'Test Column');
        });
    });

    describe('different column names', () => {
        it('should handle column name with special characters', () => {
            render(<ColumnHeaderCell {...defaultProps} columnName="Column with 'quotes' & symbols!" />);

            expect(screen.getByText("Column with 'quotes' & symbols!")).toBeInTheDocument();
        });

        it('should handle empty column name', () => {
            render(<ColumnHeaderCell {...defaultProps} columnName="" />);

            // With empty column name, we just check the button exists
            const menuButton = screen.getByRole('button');

            expect(menuButton).toBeInTheDocument();
        });
    });
});
