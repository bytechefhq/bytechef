import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpIntegrationInstanceConfigurationListItemDropdownMenu from '../McpIntegrationInstanceConfigurationListItemDropdownMenu';
import {McpIntegrationInstanceConfigurationItemType} from '../hooks/useMcpIntegrationInstanceConfigurationList';

const hoisted = vi.hoisted(() => ({
    handleConfirmDelete: vi.fn(),
    isDeletePending: false,
    setShowDeleteDialog: vi.fn(),
    showDeleteDialog: false,
}));

vi.mock('../hooks/useMcpIntegrationInstanceConfigurationListItemDropdownMenu', () => ({
    default: () => ({
        handleConfirmDelete: hoisted.handleConfirmDelete,
        isDeletePending: hoisted.isDeletePending,
        setShowDeleteDialog: hoisted.setShowDeleteDialog,
        showDeleteDialog: hoisted.showDeleteDialog,
    }),
}));

const mcpIntegrationInstanceConfiguration = {id: '5'} as McpIntegrationInstanceConfigurationItemType;

const renderMenu = (handlers: {onEditWorkflowsClick?: () => void; onUpdateIntegrationVersionClick?: () => void} = {}) =>
    render(
        <McpIntegrationInstanceConfigurationListItemDropdownMenu
            mcpIntegrationInstanceConfiguration={mcpIntegrationInstanceConfiguration}
            onEditWorkflowsClick={handlers.onEditWorkflowsClick ?? vi.fn()}
            onUpdateIntegrationVersionClick={handlers.onUpdateIntegrationVersionClick ?? vi.fn()}
        />
    );

beforeEach(() => {
    windowResizeObserver();

    hoisted.isDeletePending = false;
    hoisted.showDeleteDialog = false;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpIntegrationInstanceConfigurationListItemDropdownMenu', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderMenu();

        await userEvent.click(screen.getByRole('button'));

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit Workflows'})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Update Integration Version'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('calls onEditWorkflowsClick when Edit Workflows is clicked', async () => {
        const onEditWorkflowsClick = vi.fn();

        renderMenu({onEditWorkflowsClick});

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit Workflows'}));

        expect(onEditWorkflowsClick).toHaveBeenCalledTimes(1);
    });

    it('calls onUpdateIntegrationVersionClick when Update Integration Version is clicked', async () => {
        const onUpdateIntegrationVersionClick = vi.fn();

        renderMenu({onUpdateIntegrationVersionClick});

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Update Integration Version'}));

        expect(onUpdateIntegrationVersionClick).toHaveBeenCalledTimes(1);
    });

    it('asks for delete confirmation when Delete is clicked', async () => {
        renderMenu();

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(hoisted.setShowDeleteDialog).toHaveBeenCalledWith(true);
    });
});
