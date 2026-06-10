import reactLogo from '@/assets/logo.svg';
import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupContent,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarRail,
} from '@/components/ui/sidebar';
import {type LucideIcon} from 'lucide-react';
import {Link, useLocation} from 'react-router-dom';

import {AppSidebarFooter} from './AppSidebarFooter';

export interface AppSidebarNavItemI {
    href: string;
    icon: LucideIcon;
    name: string;
}

interface AppSidebarProps {
    navigation: AppSidebarNavItemI[];
}

export function AppSidebar({navigation}: AppSidebarProps) {
    const {pathname} = useLocation();

    const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

    return (
        <Sidebar className="bg-muted" collapsible="icon">
            <SidebarHeader>
                <Link className="flex items-center gap-2 py-1" to="/">
                    <span className="flex size-10 shrink-0 items-center justify-center">
                        <img alt="ByteChef" className="size-8 max-w-none shrink-0" src={reactLogo} />
                    </span>

                    <span className="text-lg font-semibold group-data-[collapsible=icon]:hidden">ByteChef</span>
                </Link>
            </SidebarHeader>

            <SidebarContent>
                <SidebarGroup>
                    <SidebarGroupContent>
                        <nav aria-label="Main navigation">
                            <SidebarMenu>
                                {navigation.map((item) => (
                                    <SidebarMenuItem key={item.name}>
                                        <SidebarMenuButton
                                            asChild
                                            className="h-10 gap-3 text-sm group-data-[collapsible=icon]:!size-10 data-[active=true]:font-medium data-[active=true]:text-content-brand-primary [&>svg]:size-6"
                                            isActive={isActive(item.href)}
                                            tooltip={item.name}
                                        >
                                            <Link to={item.href}>
                                                <item.icon aria-hidden="true" />

                                                <span>{item.name}</span>
                                            </Link>
                                        </SidebarMenuButton>
                                    </SidebarMenuItem>
                                ))}
                            </SidebarMenu>
                        </nav>
                    </SidebarGroupContent>
                </SidebarGroup>
            </SidebarContent>

            <SidebarFooter>
                <AppSidebarFooter />
            </SidebarFooter>

            <SidebarRail />
        </Sidebar>
    );
}
