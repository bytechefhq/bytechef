import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {BlendIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {type ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuShortcut,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from './DropdownMenu';

const renderOpenMenu = (children: ReactNode, contentClassName?: string) =>
    render(
        <DropdownMenu open>
            <DropdownMenuTrigger>Actions</DropdownMenuTrigger>

            <DropdownMenuContent className={contentClassName}>{children}</DropdownMenuContent>
        </DropdownMenu>
    );

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('DropdownMenuContent', () => {
    it('should paint the menu surface with ByteChef tokens', () => {
        renderOpenMenu(<DropdownMenuItem label="Rename" />);

        const menu = screen.getByRole('menu');

        expect(menu).toHaveClass(
            'rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-primary p-1 text-content-neutral-primary shadow-md'
        );
        expect(menu).not.toHaveClass('rounded-md');
        expect(menu).not.toHaveClass('bg-popover');
        expect(menu).not.toHaveClass('text-popover-foreground');
    });

    it('should merge className via twMerge', () => {
        renderOpenMenu(<DropdownMenuItem label="Rename" />, 'w-72 p-2');

        const menu = screen.getByRole('menu');

        expect(menu).toHaveClass('w-72 p-2');
        expect(menu).not.toHaveClass('p-1');
    });
});

describe('DropdownMenuItem', () => {
    it('should render a label with the default tokens', () => {
        renderOpenMenu(<DropdownMenuItem label="Rename" />);

        const item = screen.getByRole('menuitem', {name: 'Rename'});

        expect(item).toHaveTextContent('Rename');
        expect(item).toHaveClass('cursor-pointer gap-2 rounded-md px-2 py-1.5 text-sm');
        expect(item).toHaveClass(
            "text-content-neutral-primary focus:bg-surface-neutral-primary-hover focus:text-content-neutral-primary [&_svg:not([class*='text-'])]:text-content-neutral-primary"
        );
    });

    it('should drop the shadcn generic tokens', () => {
        renderOpenMenu(<DropdownMenuItem label="Rename" />);

        const item = screen.getByRole('menuitem', {name: 'Rename'});

        expect(item).not.toHaveClass('rounded-sm');
        expect(item).not.toHaveClass('focus:bg-accent');
        expect(item).not.toHaveClass('focus:text-accent-foreground');
        expect(item).not.toHaveClass("[&_svg:not([class*='text-'])]:text-muted-foreground");
    });

    it('should render the icon before the label', () => {
        renderOpenMenu(<DropdownMenuItem icon={<PencilIcon />} label="Rename" />);

        const item = screen.getByRole('menuitem', {name: 'Rename'});
        const icon = item.querySelector('svg');

        expect(icon).toHaveClass('lucide-pencil');
        expect(item.firstElementChild).toBe(icon);
    });

    it('should apply destructive tokens when variant is destructive', () => {
        renderOpenMenu(<DropdownMenuItem icon={<Trash2Icon />} label="Delete" variant="destructive" />);

        const item = screen.getByRole('menuitem', {name: 'Delete'});

        expect(item).toHaveClass(
            "text-content-destructive-primary focus:bg-surface-destructive-secondary focus:text-content-destructive-primary [&_svg:not([class*='text-'])]:text-content-destructive-primary"
        );
        expect(item).not.toHaveClass('text-content-neutral-primary');
    });

    it('should not forward variant to the shadcn base', () => {
        renderOpenMenu(<DropdownMenuItem label="Delete" variant="destructive" />);

        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveAttribute('data-variant', 'default');
    });

    it('should render custom children', () => {
        renderOpenMenu(
            <DropdownMenuItem>
                <span>Paste After</span>

                <span>HTTP Client</span>
            </DropdownMenuItem>
        );

        expect(screen.getByRole('menuitem')).toHaveTextContent('Paste AfterHTTP Client');
    });

    it('should merge className via twMerge', () => {
        renderOpenMenu(<DropdownMenuItem className="px-4" label="Rename" />);

        const item = screen.getByRole('menuitem', {name: 'Rename'});

        expect(item).toHaveClass('px-4');
        expect(item).not.toHaveClass('px-2');
    });

    it('should call onSelect when clicked', async () => {
        const handleSelect = vi.fn();

        renderOpenMenu(<DropdownMenuItem label="Rename" onSelect={handleSelect} />);

        await userEvent.click(screen.getByRole('menuitem', {name: 'Rename'}));

        expect(handleSelect).toHaveBeenCalledTimes(1);
    });

    it('should call onSelect when Enter is pressed on the focused item', async () => {
        const handleSelect = vi.fn();

        renderOpenMenu(<DropdownMenuItem label="Rename" onSelect={handleSelect} />);

        screen.getByRole('menuitem', {name: 'Rename'}).focus();

        await userEvent.keyboard('{Enter}');

        expect(handleSelect).toHaveBeenCalledTimes(1);
    });

    it('should not call onSelect when a disabled destructive item is clicked', async () => {
        const handleSelect = vi.fn();

        renderOpenMenu(<DropdownMenuItem disabled label="Delete" onSelect={handleSelect} variant="destructive" />);

        const item = screen.getByRole('menuitem', {name: 'Delete'});

        await userEvent.click(item);

        expect(item).toHaveAttribute('data-disabled');
        expect(handleSelect).not.toHaveBeenCalled();
    });

    it('should slot onto its single child element when asChild is set', () => {
        renderOpenMenu(
            <DropdownMenuItem asChild>
                <a href="/projects">Projects</a>
            </DropdownMenuItem>
        );

        expect(screen.getByRole('menuitem', {name: 'Projects'}).tagName).toBe('A');
    });
});

describe('DropdownMenuLabel', () => {
    it('should render with ByteChef tokens', () => {
        renderOpenMenu(<DropdownMenuLabel>Workflows</DropdownMenuLabel>);

        expect(screen.getByText('Workflows')).toHaveClass('text-content-neutral-primary');
    });
});

describe('DropdownMenuSeparator', () => {
    it('should render a full-bleed rule with ByteChef tokens', () => {
        renderOpenMenu(
            <>
                <DropdownMenuItem label="Rename" />

                <DropdownMenuSeparator />

                <DropdownMenuItem label="Delete" variant="destructive" />
            </>
        );

        const separator = screen.getByRole('separator');

        expect(separator).toHaveClass('-mx-1 my-1 h-px bg-stroke-neutral-secondary');
        expect(separator).not.toHaveClass('bg-border');
    });
});

describe('DropdownMenuShortcut', () => {
    it('should render with ByteChef tokens', () => {
        renderOpenMenu(
            <DropdownMenuItem>
                <span>Copy</span>

                <DropdownMenuShortcut>⌘C</DropdownMenuShortcut>
            </DropdownMenuItem>
        );

        const shortcut = screen.getByText('⌘C');

        expect(shortcut).toHaveClass('text-content-neutral-secondary');
        expect(shortcut).not.toHaveClass('text-muted-foreground');
    });
});

describe('DropdownMenuSub', () => {
    const renderOpenSubmenu = () =>
        renderOpenMenu(
            <DropdownMenuSub open>
                <DropdownMenuSubTrigger icon={<BlendIcon />} label="Mode" />

                <DropdownMenuSubContent>
                    <DropdownMenuItem label="Automation" />
                </DropdownMenuSubContent>
            </DropdownMenuSub>
        );

    it('should style the sub-trigger like an item and keep the chevron', () => {
        renderOpenSubmenu();

        const subTrigger = screen.getByRole('menuitem', {name: 'Mode'});
        const icons = subTrigger.querySelectorAll('svg');

        expect(subTrigger).toHaveClass('cursor-pointer gap-2 rounded-md py-1.5 text-sm text-content-neutral-primary');
        expect(subTrigger).toHaveClass(
            'data-[state=open]:bg-surface-neutral-primary-hover data-[state=open]:text-content-neutral-primary'
        );
        expect(subTrigger).not.toHaveClass('data-[state=open]:bg-accent');
        expect(icons).toHaveLength(2);
        expect(icons[0]).toHaveClass('lucide-blend');
        expect(icons[1]).toHaveClass('lucide-chevron-right');
    });

    it('should paint the sub-content like the menu surface', () => {
        renderOpenSubmenu();

        const subContent = screen.getAllByRole('menu')[1];

        expect(subContent).toHaveClass(
            'rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-primary p-1 text-content-neutral-primary shadow-md'
        );
        expect(subContent).not.toHaveClass('shadow-lg');
        expect(subContent).not.toHaveClass('bg-popover');
    });
});

describe('DropdownMenuCheckboxItem', () => {
    it('should render a checked label row with item tokens', () => {
        renderOpenMenu(<DropdownMenuCheckboxItem checked label="Triggers" />);

        const checkboxItem = screen.getByRole('menuitemcheckbox', {name: 'Triggers'});

        expect(checkboxItem).toHaveAttribute('data-state', 'checked');
        expect(checkboxItem).toHaveClass(
            'cursor-pointer gap-2 rounded-md py-1.5 pl-8 text-sm text-content-neutral-primary'
        );
        expect(checkboxItem).not.toHaveClass('focus:bg-accent');
    });
});

describe('DropdownMenuRadioItem', () => {
    it('should render radio rows with item tokens', () => {
        renderOpenMenu(
            <DropdownMenuRadioGroup value="automation">
                <DropdownMenuRadioItem label="Automation" value="automation" />

                <DropdownMenuRadioItem label="Embedded" value="embedded" />
            </DropdownMenuRadioGroup>
        );

        const checkedRadioItem = screen.getByRole('menuitemradio', {name: 'Automation'});

        expect(checkedRadioItem).toHaveAttribute('data-state', 'checked');
        expect(screen.getByRole('menuitemradio', {name: 'Embedded'})).toHaveAttribute('data-state', 'unchecked');
        expect(checkedRadioItem).toHaveClass(
            'cursor-pointer gap-2 rounded-md py-1.5 pl-8 text-sm text-content-neutral-primary'
        );
    });

    it('should let caller classes win over wrapper and base classes', () => {
        renderOpenMenu(
            <DropdownMenuRadioGroup value="development">
                <DropdownMenuRadioItem
                    className="px-3 [&>span:first-child]:hidden"
                    label="Development"
                    value="development"
                />
            </DropdownMenuRadioGroup>
        );

        const radioItem = screen.getByRole('menuitemradio', {name: 'Development'});

        expect(radioItem).toHaveClass('px-3 [&>span:first-child]:hidden');
        expect(radioItem).not.toHaveClass('pl-8');
        expect(radioItem).not.toHaveClass('pr-2');
    });
});

describe('TypeScript tests', () => {
    it('should reject label and children together', () => {
        // @ts-expect-error - label and children are mutually exclusive
        const element = <DropdownMenuItem label="Rename">Rename</DropdownMenuItem>;

        expect(element).toBeDefined();
    });

    it('should reject icon together with children', () => {
        // @ts-expect-error - icon is only allowed together with label
        const element = <DropdownMenuItem icon={<PencilIcon />}>Rename</DropdownMenuItem>;

        expect(element).toBeDefined();
    });

    it('should reject an icon without a label', () => {
        // @ts-expect-error - an icon-only item has no accessible name
        const element = <DropdownMenuItem icon={<PencilIcon />} />;

        expect(element).toBeDefined();
    });

    it('should require label or children', () => {
        // @ts-expect-error - label or children is required
        const element = <DropdownMenuItem />;

        expect(element).toBeDefined();
    });

    it('should reject asChild together with label', () => {
        // @ts-expect-error - asChild needs a single child element, so it only works with children
        const element = <DropdownMenuItem asChild label="Rename" />;

        expect(element).toBeDefined();
    });

    it('should allow asChild together with children', () => {
        const element = (
            <DropdownMenuItem asChild>
                <a href="/projects">Projects</a>
            </DropdownMenuItem>
        );

        expect(element).toBeDefined();
    });

    it('should reject an unknown variant', () => {
        // @ts-expect-error - variant is 'default' | 'destructive'
        const element = <DropdownMenuItem label="Rename" variant="ghost" />;

        expect(element).toBeDefined();
    });

    it('should reject the shadcn inset prop', () => {
        // @ts-expect-error - inset is not part of the ByteChef API
        const element = <DropdownMenuItem inset label="Rename" />;

        expect(element).toBeDefined();
    });

    it('should reject label and children together on a sub-trigger', () => {
        // @ts-expect-error - label and children are mutually exclusive
        const element = <DropdownMenuSubTrigger label="Mode">Mode</DropdownMenuSubTrigger>;

        expect(element).toBeDefined();
    });

    it('should reject an icon without a label on a checkbox item', () => {
        // @ts-expect-error - an icon-only item has no accessible name
        const element = <DropdownMenuCheckboxItem checked icon={<PencilIcon />} />;

        expect(element).toBeDefined();
    });

    it('should reject the shadcn inset prop on a label', () => {
        // @ts-expect-error - inset is not part of the ByteChef API
        const element = <DropdownMenuLabel inset>Workflows</DropdownMenuLabel>;

        expect(element).toBeDefined();
    });

    it('should reject asChild on a checkbox item', () => {
        const element = (
            // @ts-expect-error - the check indicator is a second child, so asChild can never slot
            <DropdownMenuCheckboxItem asChild checked>
                <a href="/projects">Projects</a>
            </DropdownMenuCheckboxItem>
        );

        expect(element).toBeDefined();
    });

    it('should reject asChild on a radio item', () => {
        const element = (
            // @ts-expect-error - the radio indicator is a second child, so asChild can never slot
            <DropdownMenuRadioItem asChild value="projects">
                <a href="/projects">Projects</a>
            </DropdownMenuRadioItem>
        );

        expect(element).toBeDefined();
    });

    it('should reject asChild on a sub-trigger', () => {
        const element = (
            // @ts-expect-error - the chevron is a second child, so asChild can never slot
            <DropdownMenuSubTrigger asChild>
                <a href="/projects">Projects</a>
            </DropdownMenuSubTrigger>
        );

        expect(element).toBeDefined();
    });
});
