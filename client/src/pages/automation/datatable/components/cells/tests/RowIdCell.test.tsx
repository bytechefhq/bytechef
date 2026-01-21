import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import RowIdCell from '../RowIdCell';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('RowIdCell', () => {
    const createDefaultProps = () => ({
        hoveredRowId: null as string | null,
        localRows: [
            {id: '1', name: 'Row 1'},
            {id: '2', name: 'Row 2'},
            {id: '3', name: 'Row 3'},
        ],
        onAddRow: vi.fn(),
        onSelectedRowsChange: vi.fn(),
        row: {id: '1', name: 'Row 1'} as {id: string; name?: string},
        rowIdx: 0,
        selectedRows: new Set<string>(),
        setHoveredRowId: vi.fn(),
    });

    describe('add row button', () => {
        it('should render add row button for synthetic last row (id: -1)', () => {
            const props = createDefaultProps();
            props.row = {id: '-1'};
            props.rowIdx = 3;

            render(<RowIdCell {...props} />);

            const button = screen.getByRole('button');

            expect(button).toHaveAttribute('title', 'Add row');
        });

        it('should call onAddRow when add button is clicked', async () => {
            const user = userEvent.setup();
            const props = createDefaultProps();
            props.row = {id: '-1'};
            props.rowIdx = 3;

            render(<RowIdCell {...props} />);

            const button = screen.getByRole('button');

            await user.click(button);

            expect(props.onAddRow).toHaveBeenCalledTimes(1);
        });
    });

    describe('row number display', () => {
        it('should display row number when not hovered and not selected', () => {
            const props = createDefaultProps();

            render(<RowIdCell {...props} />);

            expect(screen.getByText('1')).toBeInTheDocument();
        });

        it('should display correct row number based on position in localRows', () => {
            const props = createDefaultProps();
            props.row = {id: '2', name: 'Row 2'};
            props.rowIdx = 1;

            render(<RowIdCell {...props} />);

            expect(screen.getByText('2')).toBeInTheDocument();
        });
    });

    describe('checkbox display', () => {
        it('should show checkbox when row is selected', () => {
            const props = createDefaultProps();
            props.selectedRows = new Set(['1']);

            render(<RowIdCell {...props} />);

            expect(screen.getByRole('checkbox')).toBeInTheDocument();
        });

        it('should show checkbox when row is hovered', () => {
            const props = createDefaultProps();
            props.hoveredRowId = '1';

            render(<RowIdCell {...props} />);

            expect(screen.getByRole('checkbox')).toBeInTheDocument();
        });

        it('should have checked checkbox when row is selected', () => {
            const props = createDefaultProps();
            props.selectedRows = new Set(['1']);

            render(<RowIdCell {...props} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'checked');
        });

        it('should have unchecked checkbox when row is not selected but hovered', () => {
            const props = createDefaultProps();
            props.hoveredRowId = '1';

            render(<RowIdCell {...props} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('data-state', 'unchecked');
        });
    });

    describe('row selection', () => {
        it('should call onSelectedRowsChange to add row when clicking unselected row', async () => {
            const user = userEvent.setup();
            const props = createDefaultProps();
            props.hoveredRowId = '1'; // Show checkbox

            render(<RowIdCell {...props} />);

            const rowContainer = screen.getByRole('button');

            await user.click(rowContainer);

            expect(props.onSelectedRowsChange).toHaveBeenCalledWith(new Set(['1']));
        });

        it('should call onSelectedRowsChange to remove row when clicking selected row', async () => {
            const user = userEvent.setup();
            const props = createDefaultProps();
            props.selectedRows = new Set(['1']);

            render(<RowIdCell {...props} />);

            const rowContainer = screen.getByRole('button');

            await user.click(rowContainer);

            expect(props.onSelectedRowsChange).toHaveBeenCalledWith(new Set());
        });
    });

    describe('hover behavior', () => {
        it('should call setHoveredRowId on mouse enter', async () => {
            const user = userEvent.setup();
            const props = createDefaultProps();

            render(<RowIdCell {...props} />);

            const rowContainer = screen.getByRole('button');

            await user.hover(rowContainer);

            expect(props.setHoveredRowId).toHaveBeenCalledWith('1');
        });
    });

    describe('accessibility', () => {
        it('should have correct aria-label on checkbox', () => {
            const props = createDefaultProps();
            props.selectedRows = new Set(['1']);

            render(<RowIdCell {...props} />);

            const checkbox = screen.getByRole('checkbox');

            expect(checkbox).toHaveAttribute('aria-label', 'Select row 1');
        });

        it('should have title with row number', () => {
            const props = createDefaultProps();

            render(<RowIdCell {...props} />);

            const rowContainer = screen.getByRole('button');

            expect(rowContainer).toHaveAttribute('title', 'Row 1');
        });
    });
});
