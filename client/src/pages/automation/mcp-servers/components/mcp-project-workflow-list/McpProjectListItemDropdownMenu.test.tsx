import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpProjectListItemDropdownMenu from './McpProjectListItemDropdownMenu';
import {McpProjectItemType} from './hooks/useMcpProjectList';

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => true,
}));

const hoisted = vi.hoisted(() => ({
    handleConfirmDelete: vi.fn(),
    isDeletePending: false,
    setShowDeleteDialog: vi.fn(),
    showDeleteDialog: false,
}));

vi.mock('./hooks/useMcpProjectListItemDropdownMenu', () => ({
    default: () => ({
        handleConfirmDelete: hoisted.handleConfirmDelete,
        isDeletePending: hoisted.isDeletePending,
        setShowDeleteDialog: hoisted.setShowDeleteDialog,
        showDeleteDialog: hoisted.showDeleteDialog,
    }),
}));

const mcpProject = {id: '3'} as McpProjectItemType;

const renderMenu = (handlers: {onChangeProjectVersionClick?: () => void; onEditWorkflowsClick?: () => void} = {}) =>
    render(
        <McpProjectListItemDropdownMenu
            mcpProject={mcpProject}
            onChangeProjectVersionClick={handlers.onChangeProjectVersionClick ?? vi.fn()}
            onEditWorkflowsClick={handlers.onEditWorkflowsClick ?? vi.fn()}
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

describe('McpProjectListItemDropdownMenu', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderMenu();

        await userEvent.click(screen.getByRole('button'));

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit Workflows'})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Change Project Version'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('calls onEditWorkflowsClick when Edit Workflows is clicked', async () => {
        const onEditWorkflowsClick = vi.fn();

        renderMenu({onEditWorkflowsClick});

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit Workflows'}));

        expect(onEditWorkflowsClick).toHaveBeenCalledTimes(1);
    });

    it('calls onChangeProjectVersionClick when Change Project Version is clicked', async () => {
        const onChangeProjectVersionClick = vi.fn();

        renderMenu({onChangeProjectVersionClick});

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Change Project Version'}));

        expect(onChangeProjectVersionClick).toHaveBeenCalledTimes(1);
    });

    it('asks for delete confirmation when Delete is clicked', async () => {
        renderMenu();

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(hoisted.setShowDeleteDialog).toHaveBeenCalledWith(true);
    });

    it('disables Delete while the project is being deleted', async () => {
        hoisted.isDeletePending = true;

        renderMenu();

        await userEvent.click(screen.getByRole('button'));

        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('menuitem', {name: 'Edit Workflows'})).not.toHaveAttribute('aria-disabled');
    });
});
