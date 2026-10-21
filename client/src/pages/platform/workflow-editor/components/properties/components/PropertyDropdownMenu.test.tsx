import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {PencilIcon, Trash2Icon} from 'lucide-react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyDropdownMenu, {DropdownMenuItemType} from './PropertyDropdownMenu';

const onEditClick = vi.fn();
const onDeleteClick = vi.fn();

const menuItems: DropdownMenuItemType[] = [
    {icon: <PencilIcon data-testid="edit-icon" />, label: 'Edit', onClick: onEditClick},
    {separator: true},
    {danger: true, icon: <Trash2Icon />, label: 'Delete', onClick: onDeleteClick},
];

const renderPropertyDropdownMenu = (props: Partial<Parameters<typeof PropertyDropdownMenu>[0]> = {}) =>
    render(
        <PropertyDropdownMenu menuItems={menuItems} trigger={<button type="button">Property menu</button>} {...props} />
    );

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Property menu'}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('PropertyDropdownMenu', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderPropertyDropdownMenu();

        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should render the rows with their icons and a separator between them', async () => {
        renderPropertyDropdownMenu();

        await openMenu();

        expect(screen.getAllByRole('menuitem')).toHaveLength(2);
        expect(screen.getAllByRole('separator')).toHaveLength(1);
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toContainElement(screen.getByTestId('edit-icon'));
    });

    it('should call the row handler with the property id when clicking Edit', async () => {
        renderPropertyDropdownMenu({id: 7});

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(onEditClick).toHaveBeenCalledTimes(1);
        expect(onEditClick).toHaveBeenCalledWith(7, expect.anything());
        expect(onDeleteClick).not.toHaveBeenCalled();
    });

    it('should call the row handler with the default id when clicking Delete', async () => {
        renderPropertyDropdownMenu();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(onDeleteClick).toHaveBeenCalledTimes(1);
        expect(onDeleteClick).toHaveBeenCalledWith(0, expect.anything());
        expect(onEditClick).not.toHaveBeenCalled();
    });

    it('should render a row that has an icon but no label', async () => {
        renderPropertyDropdownMenu({
            menuItems: [{icon: <PencilIcon data-testid="icon-only-icon" />, onClick: onEditClick}],
        });

        await openMenu();

        expect(screen.getByRole('menuitem')).toContainElement(screen.getByTestId('icon-only-icon'));
        expect(screen.getByRole('menuitem')).toHaveTextContent('');
    });

    it('should close the menu when clicking a row that has no handler', async () => {
        renderPropertyDropdownMenu({menuItems: [{label: 'Inspect'}]});

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Inspect'}));

        expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    });

    it('should open the menu from the default ellipsis trigger', async () => {
        const {container} = renderPropertyDropdownMenu({trigger: undefined});

        await userEvent.click(container.querySelector('svg.lucide-ellipsis-vertical')!);

        expect(screen.getByRole('menu')).toBeInTheDocument();
    });
});
