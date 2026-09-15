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

const hoistedScope = vi.hoisted(() => ({
    grantedScopes: ['DATA_TABLE_CREATE', 'DATA_TABLE_DELETE', 'DATA_TABLE_EDIT'] as string[],
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

vi.mock('../hooks/useDataTableListItemDropdownMenu', () => ({
    default: hoisted.mockUseDataTableListItemDropdownMenu,
}));

beforeEach(() => {
    hoistedScope.grantedScopes = ['DATA_TABLE_CREATE', 'DATA_TABLE_DELETE', 'DATA_TABLE_EDIT'];

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

    it('should hide Rename, Duplicate and Delete without data table scopes but keep Export CSV', async () => {
        hoistedScope.grantedScopes = [];

        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        await userEvent.click(screen.getByRole('button', {name: 'Table menu'}));

        expect(screen.getByRole('menuitem', {name: 'Export CSV'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Rename'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Duplicate'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
    });

    it('should show only the items whose scope is granted', async () => {
        hoistedScope.grantedScopes = ['DATA_TABLE_CREATE'];

        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        await userEvent.click(screen.getByRole('button', {name: 'Table menu'}));

        expect(screen.getByRole('menuitem', {name: 'Duplicate'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Rename'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
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
