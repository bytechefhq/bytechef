import {DropdownMenuItem} from '@/components/ui/dropdown-menu';
import WorkflowListItemDropdownMenu from '@/shared/components/workflow/WorkflowListItemDropdownMenu';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const renderMenu = (overrides: {dialogs?: React.ReactNode; onDelete?: () => void; onEditClick?: () => void} = {}) => {
    const onDelete = overrides.onDelete ?? vi.fn();
    const onEditClick = overrides.onEditClick ?? vi.fn();

    render(
        <WorkflowListItemDropdownMenu
            dialogs={overrides.dialogs}
            onDelete={onDelete}
            onEditClick={onEditClick}
            workflowLabel="Workflow One"
        >
            <DropdownMenuItem>Export</DropdownMenuItem>
        </WorkflowListItemDropdownMenu>
    );

    return {onDelete, onEditClick};
};

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow One'}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('WorkflowListItemDropdownMenu', () => {
    it('labels the trigger with the workflow label', () => {
        renderMenu();

        expect(screen.getByRole('button', {name: 'Workflow actions for Workflow One'})).toBeInTheDocument();
    });

    it('opens with Edit, then the surface items, then Delete', async () => {
        renderMenu();

        await openMenu();

        const menuItems = screen.getAllByRole('menuitem');

        expect(menuItems.map((menuItem) => menuItem.textContent?.trim())).toEqual(['Edit', 'Export', 'Delete']);
    });

    it('calls onEditClick when Edit is chosen', async () => {
        const {onEditClick} = renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(onEditClick).toHaveBeenCalledTimes(1);
    });

    it('confirms before deleting', async () => {
        const {onDelete} = renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
        expect(onDelete).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(onDelete).toHaveBeenCalledTimes(1);
    });

    it('does not delete when the confirmation is cancelled', async () => {
        const {onDelete} = renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onDelete).not.toHaveBeenCalled();
        expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
    });

    it('keeps a click on the trigger off the surrounding card', async () => {
        const handleCardClick = vi.fn();

        render(
            <li onClick={handleCardClick}>
                <WorkflowListItemDropdownMenu onDelete={vi.fn()} onEditClick={vi.fn()} workflowLabel="Workflow Two" />
            </li>
        );

        await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow Two'}));

        expect(handleCardClick).not.toHaveBeenCalled();
    });

    it('falls back to a generic label when the workflow has none', () => {
        render(<WorkflowListItemDropdownMenu onDelete={vi.fn()} onEditClick={vi.fn()} />);

        expect(screen.getByRole('button', {name: 'Workflow actions'})).toBeInTheDocument();
    });

    it('renders the caller dialogs', () => {
        render(
            <WorkflowListItemDropdownMenu
                dialogs={<span>Caller dialog</span>}
                onDelete={vi.fn()}
                onEditClick={vi.fn()}
                workflowLabel="Workflow Two"
            />
        );

        expect(screen.getByText('Caller dialog')).toBeInTheDocument();
    });
});
