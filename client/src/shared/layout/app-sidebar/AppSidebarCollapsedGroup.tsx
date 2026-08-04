import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {SidebarMenuButton, SidebarMenuItem} from '@/components/ui/sidebar';
import {Link} from 'react-router-dom';

import type {AppSidebarNavItemI} from './AppSidebar';

interface AppSidebarCollapsedGroupProps {
    isActive: (href: string) => boolean;
    items: AppSidebarNavItemI[];
    label: string;
    onOpenChange: (label: string, open: boolean) => void;
    open: boolean;
}

/**
 * One rail icon standing in for a whole nav group while the sidebar is collapsed; hovering it opens a
 * flyout listing the group's items. The rail would otherwise show every item's icon in an
 * undifferentiated column, which grows past shorter viewports and loses the grouping entirely.
 *
 * The trigger wears the active item's icon when the user is inside the group (so the rail still says
 * where they are) and the first item's icon otherwise.
 */
const AppSidebarCollapsedGroup = ({isActive, items, label, onOpenChange, open}: AppSidebarCollapsedGroupProps) => {
    const activeItem = items.find((item) => isActive(item.href));

    const TriggerIcon = (activeItem ?? items[0]).icon;

    const handleOpenChange = (nextOpen: boolean) => onOpenChange(label, nextOpen);

    return (
        <SidebarMenuItem>
            {/* The 40px icon button alone makes a twitchy hover target, and an offset flyout leaves a dead
                gap the pointer must cross. The trigger spans the full rail width instead, the flyout sits
                flush against it, and a longer close delay tolerates a wandering pointer. */}

            {/* Open state is owned by the sidebar so only one flyout is ever up: the close delay that
                tolerates a wandering pointer would otherwise leave the previous group's flyout on screen
                for 300ms after the next one opens, stacking two menus over the rail. */}

            <HoverCard closeDelay={300} onOpenChange={handleOpenChange} open={open} openDelay={0}>
                <HoverCardTrigger asChild>
                    <div className="flex w-full justify-center py-0.5">
                        <SidebarMenuButton
                            aria-label={label}
                            className="h-10 gap-3 text-sm group-data-[collapsible=icon]:!size-10 data-[active=true]:font-medium data-[active=true]:text-content-brand-primary [&>svg]:size-6"
                            isActive={!!activeItem}
                        >
                            <TriggerIcon aria-hidden="true" />

                            <span>{label}</span>
                        </SidebarMenuButton>
                    </div>
                </HoverCardTrigger>

                <HoverCardContent align="start" className="w-56 p-1" side="right" sideOffset={0}>
                    <span className="block px-2 py-1.5 text-xs font-medium text-muted-foreground">{label}</span>

                    <nav aria-label={label} className="flex flex-col">
                        {items.map((item) => (
                            <Link
                                aria-current={isActive(item.href) ? 'page' : undefined}
                                className="flex items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent aria-[current=page]:font-medium aria-[current=page]:text-content-brand-primary"
                                key={item.name}
                                to={item.href}
                            >
                                <item.icon aria-hidden="true" className="size-4 shrink-0" />

                                <span className="truncate">{item.name}</span>
                            </Link>
                        ))}
                    </nav>
                </HoverCardContent>
            </HoverCard>
        </SidebarMenuItem>
    );
};

export default AppSidebarCollapsedGroup;
