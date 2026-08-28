import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableListItemDropdownMenu from '../DataTableListItemDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        handleDeleteClick: vi.fn(),
        handleDuplicateClick: vi.fn(),
        handleExportCsvClick: vi.fn(),
        handleRenameClick: vi.fn(),
        mockUseDataTableListItemDropdownMenu: vi.fn(),
    };
});

vi.mock('../hooks/useDataTableListItemDropdownMenu', () => ({
    default: hoisted.mockUseDataTableListItemDropdownMenu,
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseDataTableListItemDropdownMenu.mockReturnValue({
        handleDeleteClick: hoisted.handleDeleteClick,
        handleDuplicateClick: hoisted.handleDuplicateClick,
        handleExportCsvClick: hoisted.handleExportCsvClick,
        handleRenameClick: hoisted.handleRenameClick,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTableListItemDropdownMenu', () => {
    it('should render menu trigger button', () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        expect(screen.getByRole('button', {name: 'Table menu'})).toBeInTheDocument();
    });

    it('should open menu when clicking trigger button', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        const triggerButton = screen.getByRole('button', {name: 'Table menu'});
        await userEvent.click(triggerButton);

        expect(screen.getByText('Rename')).toBeInTheDocument();
        expect(screen.getByText('Duplicate')).toBeInTheDocument();
        expect(screen.getByText('Export CSV')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('should call handleRenameClick when clicking Rename', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        const triggerButton = screen.getByRole('button', {name: 'Table menu'});
        await userEvent.click(triggerButton);

        const renameItem = screen.getByText('Rename');
        await userEvent.click(renameItem);

        expect(hoisted.handleRenameClick).toHaveBeenCalledTimes(1);
    });

    it('should call handleDuplicateClick when clicking Duplicate', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        const triggerButton = screen.getByRole('button', {name: 'Table menu'});
        await userEvent.click(triggerButton);

        const duplicateItem = screen.getByText('Duplicate');
        await userEvent.click(duplicateItem);

        expect(hoisted.handleDuplicateClick).toHaveBeenCalledTimes(1);
    });

    it('should call handleExportCsvClick when clicking Export CSV', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        const triggerButton = screen.getByRole('button', {name: 'Table menu'});
        await userEvent.click(triggerButton);

        const exportItem = screen.getByText('Export CSV');
        await userEvent.click(exportItem);

        expect(hoisted.handleExportCsvClick).toHaveBeenCalledTimes(1);
    });

    it('should call handleDeleteClick when clicking Delete', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        const triggerButton = screen.getByRole('button', {name: 'Table menu'});
        await userEvent.click(triggerButton);

        const deleteItem = screen.getByText('Delete');
        await userEvent.click(deleteItem);

        expect(hoisted.handleDeleteClick).toHaveBeenCalledTimes(1);
    });
});
