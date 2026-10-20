import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationListItemDropdownMenu from '../IntegrationInstanceConfigurationListItemDropdownMenu';

const handlers = {
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
    onUpdateIntegrationVersionClick: vi.fn(),
};

const openMenu = async () => {
    render(<IntegrationInstanceConfigurationListItemDropdownMenu {...handlers} />);

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
        ['Delete', 'onDeleteClick'],
    ] as const)('should call the handler when %s is clicked', async (rowName, handlerName) => {
        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: rowName}));

        expect(handlers[handlerName]).toHaveBeenCalledTimes(1);
    });
});
