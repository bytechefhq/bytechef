import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableLeftSidebarDropdownMenu from '../DataTableLeftSidebarDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleDeleteOpen: vi.fn(),
        mockHandleRenameOpen: vi.fn(),
    };
});

vi.mock('../../hooks/useDeleteDataTableDialog', () => ({
    default: () => ({
        handleOpen: hoisted.mockHandleDeleteOpen,
    }),
}));

vi.mock('../../hooks/useRenameDataTableDialog', () => ({
    default: () => ({
        handleOpen: hoisted.mockHandleRenameOpen,
    }),
}));

const defaultProps = {
    tableId: 'table-123',
    tableName: 'TestTable',
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTableLeftSidebarDropdownMenu', () => {
    describe('rendering', () => {
        it('should render the menu trigger button', () => {
            render(<DataTableLeftSidebarDropdownMenu {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Table menu'})).toBeInTheDocument();
        });

        it('should not show menu items initially', () => {
            render(<DataTableLeftSidebarDropdownMenu {...defaultProps} />);

            expect(screen.queryByText('Rename')).not.toBeInTheDocument();
            expect(screen.queryByText('Delete')).not.toBeInTheDocument();
        });
    });

    describe('menu items', () => {
        it('should show Rename and Delete menu items when trigger is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableLeftSidebarDropdownMenu {...defaultProps} />);

            const triggerButton = screen.getByRole('button', {name: 'Table menu'});

            await user.click(triggerButton);

            expect(screen.getByText('Rename')).toBeInTheDocument();
            expect(screen.getByText('Delete')).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call handleRenameOpen with tableId and tableName when Rename is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableLeftSidebarDropdownMenu {...defaultProps} />);

            const triggerButton = screen.getByRole('button', {name: 'Table menu'});

            await user.click(triggerButton);

            const renameItem = screen.getByText('Rename');

            await user.click(renameItem);

            expect(hoisted.mockHandleRenameOpen).toHaveBeenCalledWith('table-123', 'TestTable');
        });

        it('should call handleDeleteOpen with tableId and tableName when Delete is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableLeftSidebarDropdownMenu {...defaultProps} />);

            const triggerButton = screen.getByRole('button', {name: 'Table menu'});

            await user.click(triggerButton);

            const deleteItem = screen.getByText('Delete');

            await user.click(deleteItem);

            expect(hoisted.mockHandleDeleteOpen).toHaveBeenCalledWith('table-123', 'TestTable');
        });

        it('should pass correct props for different table data', async () => {
            const user = userEvent.setup();
            const customProps = {
                tableId: 'another-table-456',
                tableName: 'AnotherTable',
            };

            render(<DataTableLeftSidebarDropdownMenu {...customProps} />);

            const triggerButton = screen.getByRole('button', {name: 'Table menu'});

            await user.click(triggerButton);

            const renameItem = screen.getByText('Rename');

            await user.click(renameItem);

            expect(hoisted.mockHandleRenameOpen).toHaveBeenCalledWith('another-table-456', 'AnotherTable');
        });
    });

    describe('styling', () => {
        it('should render Delete menu item with red color class', async () => {
            const user = userEvent.setup();

            render(<DataTableLeftSidebarDropdownMenu {...defaultProps} />);

            const triggerButton = screen.getByRole('button', {name: 'Table menu'});

            await user.click(triggerButton);

            const deleteItem = screen.getByText('Delete').closest('[role="menuitem"]');

            expect(deleteItem).toHaveClass('text-red-600');
        });
    });
});
