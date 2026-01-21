import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {createBooleanCellRenderer} from '../BooleanCellRenderer';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('BooleanCellRenderer', () => {
    const mockOnToggle = vi.fn();
    const mockSetLocalRows = vi.fn();

    const createRenderer = (columnName: string = 'isActive') => {
        return createBooleanCellRenderer({
            columnName,
            onToggle: mockOnToggle,
            setLocalRows: mockSetLocalRows,
        });
    };

    describe('rendering', () => {
        it('should return null for synthetic row (id: -1)', () => {
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '-1', isActive: true}} />);

            expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        });

        it('should render a checkbox', () => {
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '1', isActive: true}} />);

            expect(screen.getByRole('checkbox')).toBeInTheDocument();
        });

        it('should render checked checkbox when value is true', () => {
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '1', isActive: true}} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'checked');
        });

        it('should render unchecked checkbox when value is false', () => {
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '1', isActive: false}} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'unchecked');
        });

        it('should render unchecked checkbox when value is undefined', () => {
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '1'}} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'unchecked');
        });

        it('should have correct aria-label', () => {
            const BooleanRenderer = createRenderer('enabled');

            render(<BooleanRenderer row={{enabled: true, id: '1'}} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('aria-label', 'Toggle enabled');
        });
    });

    describe('interactions', () => {
        it('should call setLocalRows when toggled', async () => {
            const user = userEvent.setup();
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '1', isActive: false}} />);

            const checkbox = screen.getByRole('checkbox');

            await user.click(checkbox);

            expect(mockSetLocalRows).toHaveBeenCalled();
        });

        it('should call onToggle when toggled', async () => {
            const user = userEvent.setup();
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '1', isActive: false}} />);

            const checkbox = screen.getByRole('checkbox');

            await user.click(checkbox);

            expect(mockOnToggle).toHaveBeenCalledWith('1', 'isActive', true);
        });

        it('should toggle from true to false', async () => {
            const user = userEvent.setup();
            const BooleanRenderer = createRenderer();

            render(<BooleanRenderer row={{id: '1', isActive: true}} />);

            const checkbox = screen.getByRole('checkbox');

            await user.click(checkbox);

            expect(mockOnToggle).toHaveBeenCalledWith('1', 'isActive', false);
        });
    });
});
