import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarMenuSub,
    SidebarMenuSubButton,
    SidebarMenuSubItem,
} from '@/components/ui/sidebar';
import {ChevronRightIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

import type {AppSidebarNavGroupI, AppSidebarNavItemI} from './AppSidebar';

interface AppSidebarCollapsibleGroupProps {
    group: AppSidebarNavGroupI;
    isActive: (href: string) => boolean;
    items: AppSidebarNavItemI[];
    onOpenChange: (name: string, open: boolean) => void;
    open: boolean;
}

/**
 * A nav group on the expanded sidebar: a parent row that toggles its members open and closed.
 */
const AppSidebarCollapsibleGroup = ({group, isActive, items, onOpenChange, open}: AppSidebarCollapsibleGroupProps) => {
    const activeItem = items.find((item) => isActive(item.href));

    const handleOpenChange = (nextOpen: boolean) => onOpenChange(group.name, nextOpen);

    return (
        <Collapsible asChild onOpenChange={handleOpenChange} open={open}>
            <SidebarMenuItem className="group/collapsible">
                <CollapsibleTrigger asChild>
                    <SidebarMenuButton
                        className="h-10 gap-3 text-sm data-[active=true]:font-medium data-[active=true]:text-sidebar-active-foreground [&>svg]:size-6 [&>svg:last-child]:size-4"
                        isActive={!!activeItem && !open}
                    >
                        <group.icon aria-hidden="true" />

                        <span>{group.name}</span>

                        <ChevronRightIcon
                            aria-hidden="true"
                            className="ml-auto shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90"
                        />
                    </SidebarMenuButton>
                </CollapsibleTrigger>

                <CollapsibleContent>
                    <SidebarMenuSub className="mx-0 border-l-0 px-0">
                        {items.map((item) => (
                            <SidebarMenuSubItem key={item.name}>
                                <SidebarMenuSubButton
                                    asChild
                                    className="h-9 pl-9 data-[active=true]:font-medium data-[active=true]:text-sidebar-active-foreground [&>svg]:size-5"
                                    isActive={isActive(item.href)}
                                >
                                    <Link to={item.href}>
                                        <item.icon aria-hidden="true" />

                                        <span>{item.name}</span>
                                    </Link>
                                </SidebarMenuSubButton>
                            </SidebarMenuSubItem>
                        ))}
                    </SidebarMenuSub>
                </CollapsibleContent>
            </SidebarMenuItem>
        </Collapsible>
    );
};

export default AppSidebarCollapsibleGroup;
