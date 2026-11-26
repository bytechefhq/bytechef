import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableListItemDropdownMenu from '../DataTableListItemDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        edition: 'EE',
        environmentsResult: {
            data: {
                environments: [
                    {id: '0', name: 'Development'},
                    {id: '1', name: 'Staging'},
                ],
            },
        } as {data: {environments: {id: string; name: string}[]} | undefined},
        handleDeleteClick: vi.fn(),
        handleDuplicateClick: vi.fn(),
        handleExportCsvClick: vi.fn(),
        handleRenameClick: vi.fn(),
        mockUseDataTableListItemDropdownMenu: vi.fn(),
        promotionDialogProps: [] as Record<string, unknown>[],
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useEnvironmentsQuery: () => hoisted.environmentsResult,
    };
});

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({application: {edition: hoisted.edition}}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 0}),
}));

vi.mock('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog', () => ({
    default: (props: Record<string, unknown>) => {
        hoisted.promotionDialogProps.push(props);

        return <div data-testid="environment-promotion-dialog" />;
    },
}));

vi.mock('../hooks/useDataTableListItemDropdownMenu', () => ({
    default: hoisted.mockUseDataTableListItemDropdownMenu,
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.edition = 'EE';
    hoisted.promotionDialogProps = [];
    hoisted.environmentsResult = {
        data: {
            environments: [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
            ],
        },
    };
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

    it('should render with the design-system DropdownMenu', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        await userEvent.click(screen.getByRole('button', {name: 'Table menu'}));

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Rename'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
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

    it('should show Promote to environment when there are two or more environments', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        await userEvent.click(screen.getByRole('button', {name: 'Table menu'}));

        expect(screen.getByRole('menuitem', {name: 'Promote to environment…'})).toBeInTheDocument();
    });

    it('should hide Promote to environment when there is only one environment', async () => {
        hoisted.environmentsResult = {data: {environments: [{id: '0', name: 'Development'}]}};

        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        await userEvent.click(screen.getByRole('button', {name: 'Table menu'}));

        expect(screen.queryByRole('menuitem', {name: 'Promote to environment…'})).not.toBeInTheDocument();
    });

    it('should open the promotion dialog for the data table', async () => {
        render(<DataTableListItemDropdownMenu baseName="orders" dataTableId="123" />);

        expect(screen.queryByTestId('environment-promotion-dialog')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Table menu'}));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Promote to environment…'}));

        expect(await screen.findByTestId('environment-promotion-dialog')).toBeInTheDocument();

        expect(hoisted.promotionDialogProps[0]).toMatchObject({
            resourceType: 'DATA_TABLE',
            sourceEnvironmentId: 0,
            sourceId: '123',
            sourceName: 'orders',
            targetListPath: '/automation/datatables',
        });
    });
});
