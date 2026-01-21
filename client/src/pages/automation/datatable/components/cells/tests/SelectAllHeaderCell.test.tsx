import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import SelectAllHeaderCell, {IndeterminateCheckbox} from '../SelectAllHeaderCell';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IndeterminateCheckbox', () => {
    describe('rendering', () => {
        it('should render a checkbox', () => {
            render(<IndeterminateCheckbox checked={false} indeterminate={false} onChange={vi.fn()} />);

            expect(screen.getByRole('checkbox')).toBeInTheDocument();
        });

        it('should have correct aria-label when provided', () => {
            render(
                <IndeterminateCheckbox
                    ariaLabel="Test checkbox"
                    checked={false}
                    indeterminate={false}
                    onChange={vi.fn()}
                />
            );

            expect(screen.getByRole('checkbox')).toHaveAttribute('aria-label', 'Test checkbox');
        });
    });

    describe('checked state', () => {
        it('should be unchecked when checked is false', () => {
            render(<IndeterminateCheckbox checked={false} indeterminate={false} onChange={vi.fn()} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'unchecked');
        });

        it('should be checked when checked is true', () => {
            render(<IndeterminateCheckbox checked={true} indeterminate={false} onChange={vi.fn()} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'checked');
        });

        it('should be indeterminate when indeterminate is true', () => {
            render(<IndeterminateCheckbox checked={false} indeterminate={true} onChange={vi.fn()} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'indeterminate');
        });

        it('should prioritize indeterminate over checked', () => {
            render(<IndeterminateCheckbox checked={true} indeterminate={true} onChange={vi.fn()} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'indeterminate');
        });
    });

    describe('interactions', () => {
        it('should call onChange with true when clicking unchecked checkbox', async () => {
            const user = userEvent.setup();
            const mockOnChange = vi.fn();

            render(<IndeterminateCheckbox checked={false} indeterminate={false} onChange={mockOnChange} />);

            const checkbox = screen.getByRole('checkbox');

            await user.click(checkbox);

            expect(mockOnChange).toHaveBeenCalledWith(true);
        });

        it('should call onChange with false when clicking checked checkbox', async () => {
            const user = userEvent.setup();
            const mockOnChange = vi.fn();

            render(<IndeterminateCheckbox checked={true} indeterminate={false} onChange={mockOnChange} />);

            const checkbox = screen.getByRole('checkbox');

            await user.click(checkbox);

            expect(mockOnChange).toHaveBeenCalledWith(false);
        });
    });
});

describe('SelectAllHeaderCell', () => {
    describe('rendering', () => {
        it('should render the checkbox with correct aria-label', () => {
            render(<SelectAllHeaderCell allSelected={false} onToggleSelectAll={vi.fn()} someSelected={false} />);

            expect(screen.getByRole('checkbox')).toHaveAttribute('aria-label', 'Select all rows');
        });
    });

    describe('select all state', () => {
        it('should show unchecked when no rows are selected', () => {
            render(<SelectAllHeaderCell allSelected={false} onToggleSelectAll={vi.fn()} someSelected={false} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'unchecked');
        });

        it('should show checked when all rows are selected', () => {
            render(<SelectAllHeaderCell allSelected={true} onToggleSelectAll={vi.fn()} someSelected={false} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'checked');
        });

        it('should show indeterminate when some rows are selected', () => {
            render(<SelectAllHeaderCell allSelected={false} onToggleSelectAll={vi.fn()} someSelected={true} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'indeterminate');
        });
    });

    describe('interactions', () => {
        it('should call onToggleSelectAll when clicked', async () => {
            const user = userEvent.setup();
            const mockOnToggleSelectAll = vi.fn();

            render(
                <SelectAllHeaderCell
                    allSelected={false}
                    onToggleSelectAll={mockOnToggleSelectAll}
                    someSelected={false}
                />
            );

            const checkbox = screen.getByRole('checkbox');

            await user.click(checkbox);

            expect(mockOnToggleSelectAll).toHaveBeenCalledWith(true);
        });

        it('should call onToggleSelectAll with false when all are selected', async () => {
            const user = userEvent.setup();
            const mockOnToggleSelectAll = vi.fn();

            render(
                <SelectAllHeaderCell
                    allSelected={true}
                    onToggleSelectAll={mockOnToggleSelectAll}
                    someSelected={false}
                />
            );

            const checkbox = screen.getByRole('checkbox');

            await user.click(checkbox);

            expect(mockOnToggleSelectAll).toHaveBeenCalledWith(false);
        });
    });
});
