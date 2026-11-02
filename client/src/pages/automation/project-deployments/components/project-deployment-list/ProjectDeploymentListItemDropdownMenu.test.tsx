import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItemDropdownMenu from './ProjectDeploymentListItemDropdownMenu';

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => true,
}));

const handlers = {
    onChangeProjectVersionClick: vi.fn(),
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
    onOpenProjectClick: vi.fn(),
};

const openMenu = async (props: Partial<typeof handlers> = {}) => {
    render(<ProjectDeploymentListItemDropdownMenu {...handlers} {...props} />);

    await userEvent.click(screen.getByRole('button'));
};

describe('ProjectDeploymentListItemDropdownMenu', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should render with the design-system DropdownMenu', async () => {
        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Open Project'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Change Project Version'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should omit Open Project when the project cannot be opened', async () => {
        await openMenu({onOpenProjectClick: undefined});

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Open Project'})).not.toBeInTheDocument();
    });

    it.each([
        ['Edit', 'onEditClick'],
        ['Open Project', 'onOpenProjectClick'],
        ['Change Project Version', 'onChangeProjectVersionClick'],
        ['Delete', 'onDeleteClick'],
    ] as const)('should call the handler when %s is clicked', async (rowName, handlerName) => {
        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: rowName}));

        expect(handlers[handlerName]).toHaveBeenCalledTimes(1);
    });
});
