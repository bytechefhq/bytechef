import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {SidebarMenuButton, SidebarMenuItem} from '@/components/ui/sidebar';
import {Link} from 'react-router-dom';

import type {AppSidebarNavGroupI, AppSidebarNavItemI} from './AppSidebar';

interface AppSidebarCollapsedGroupProps {
    group: AppSidebarNavGroupI;
    isActive: (href: string) => boolean;
    items: AppSidebarNavItemI[];
    onOpenChange: (name: string, open: boolean) => void;
    open: boolean;
}

/**
 * One rail icon standing in for a nav group while the sidebar is collapsed; hovering it opens a flyout listing
 * the group's items.
 */
const AppSidebarCollapsedGroup = ({group, isActive, items, onOpenChange, open}: AppSidebarCollapsedGroupProps) => {
    const activeItem = items.find((item) => isActive(item.href));

    const TriggerIcon = activeItem?.icon ?? group.icon;

    const handleOpenChange = (nextOpen: boolean) => onOpenChange(group.name, nextOpen);

    return (
        <SidebarMenuItem>
            <HoverCard closeDelay={300} onOpenChange={handleOpenChange} open={open} openDelay={0}>
                <HoverCardTrigger asChild>
                    <div className="flex w-full justify-center">
                        <SidebarMenuButton
                            aria-label={group.name}
                            className="h-10 gap-3 text-sm group-data-[collapsible=icon]:!size-10 data-[active=true]:font-medium data-[active=true]:text-sidebar-active-foreground [&>svg]:size-6"
                            isActive={!!activeItem}
                        >
                            <TriggerIcon aria-hidden="true" />

                            <span>{group.name}</span>
                        </SidebarMenuButton>
                    </div>
                </HoverCardTrigger>

                <HoverCardContent align="start" className="w-56 p-1" side="right" sideOffset={0}>
                    <span className="block px-2 py-1.5 text-xs font-medium text-muted-foreground">{group.name}</span>

                    <nav aria-label={group.name} className="flex flex-col">
                        {items.map((item) => (
                            <Link
                                aria-current={isActive(item.href) ? 'page' : undefined}
                                className="flex items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent aria-[current=page]:font-medium aria-[current=page]:text-sidebar-active-foreground"
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
