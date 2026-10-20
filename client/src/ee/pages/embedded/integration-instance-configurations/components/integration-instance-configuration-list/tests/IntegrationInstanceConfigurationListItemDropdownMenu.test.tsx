import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationListItemDropdownMenu from '../IntegrationInstanceConfigurationListItemDropdownMenu';

const handlers = {
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
    onMoveUsersClick: vi.fn(),
    onUpdateIntegrationVersionClick: vi.fn(),
};

const openMenu = async (moveUsersDisabledReason?: string) => {
    render(
        <TooltipProvider>
            <IntegrationInstanceConfigurationListItemDropdownMenu
                canDelete
                canEdit
                {...handlers}
                moveUsersDisabledReason={moveUsersDisabledReason}
            />
        </TooltipProvider>
    );

    await userEvent.click(screen.getByRole('button'));
};

describe('IntegrationInstanceConfigurationListItemDropdownMenu', () => {
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
        expect(screen.getByRole('menuitem', {name: 'Update Integration Version'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it.each([
        ['Edit', 'onEditClick'],
        ['Update Integration Version', 'onUpdateIntegrationVersionClick'],
        ['Move Users', 'onMoveUsersClick'],
        ['Delete', 'onDeleteClick'],
    ] as const)('should call the handler when %s is clicked', async (rowName, handlerName) => {
        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: rowName}));

        expect(handlers[handlerName]).toHaveBeenCalledTimes(1);
    });

    it('menu shows Move Users…', async () => {
        await openMenu();

        expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual([
            'Edit',
            'Update Integration Version',
            'Move Users…',
            'Delete',
        ]);
        expect(screen.getByRole('menuitem', {name: 'Move Users'})).not.toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('separator')).toBeInTheDocument();
    });

    it('should disable Move Users with the given reason', async () => {
        await openMenu('No other configuration in this environment');

        const item = screen.getByRole('menuitem', {name: 'Move Users'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0)
        );
    });
});
