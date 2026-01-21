import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {NumberEditCell, createNumberEditCellRenderer} from '../NumberCellRenderer';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('NumberEditCell', () => {
    const mockOnRowChange = vi.fn();

    beforeEach(() => {
        mockOnRowChange.mockClear();
    });

    describe('rendering', () => {
        it('should render number input', () => {
            render(
                <NumberEditCell columnName="quantity" onRowChange={mockOnRowChange} row={{id: '1', quantity: 42}} />
            );

            expect(screen.getByRole('spinbutton')).toBeInTheDocument();
        });

        it('should display current value', () => {
            render(
                <NumberEditCell columnName="quantity" onRowChange={mockOnRowChange} row={{id: '1', quantity: 42}} />
            );

            expect(screen.getByRole('spinbutton')).toHaveValue(42);
        });

        it('should display empty string for null value', () => {
            render(
                <NumberEditCell columnName="quantity" onRowChange={mockOnRowChange} row={{id: '1', quantity: null}} />
            );

            expect(screen.getByRole('spinbutton')).toHaveValue(null);
        });

        it('should display empty string for undefined value', () => {
            render(<NumberEditCell columnName="quantity" onRowChange={mockOnRowChange} row={{id: '1'}} />);

            expect(screen.getByRole('spinbutton')).toHaveValue(null);
        });

        it('should handle decimal numbers', () => {
            render(<NumberEditCell columnName="price" onRowChange={mockOnRowChange} row={{id: '1', price: 19.99}} />);

            expect(screen.getByRole('spinbutton')).toHaveValue(19.99);
        });

        it('should handle negative numbers', () => {
            render(
                <NumberEditCell columnName="balance" onRowChange={mockOnRowChange} row={{balance: -100, id: '1'}} />
            );

            expect(screen.getByRole('spinbutton')).toHaveValue(-100);
        });
    });

    describe('interactions', () => {
        it('should call onRowChange on blur with commit', async () => {
            const user = userEvent.setup();

            render(
                <NumberEditCell columnName="quantity" onRowChange={mockOnRowChange} row={{id: '1', quantity: 10}} />
            );

            const input = screen.getByRole('spinbutton');

            await user.click(input);
            await user.tab(); // Trigger blur

            expect(mockOnRowChange).toHaveBeenCalledWith(expect.objectContaining({id: '1'}), true);
        });
    });

    describe('createNumberEditCellRenderer factory', () => {
        it('should create a renderer that accepts row props', () => {
            const NumberRenderer = createNumberEditCellRenderer('amount');

            render(<NumberRenderer onRowChange={mockOnRowChange} row={{amount: 100, id: '1'}} />);

            expect(screen.getByRole('spinbutton')).toHaveValue(100);
        });
    });
});
