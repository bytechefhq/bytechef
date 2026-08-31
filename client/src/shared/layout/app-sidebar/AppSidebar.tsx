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
    useSidebar,
} from '@/components/ui/sidebar';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {type LucideIcon} from 'lucide-react';
import {useEffect} from 'react';
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

    const {isMobile, state} = useSidebar();

    const currentEnvironmentId = useEnvironmentStore((environmentState) => environmentState.currentEnvironmentId);

    const collapsed = state === 'collapsed' && !isMobile;

    const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

    useEffect(() => {
        const {documentElement} = document;
        const sidebarTheme = ENVIRONMENT_CONFIGS[currentEnvironmentId]?.sidebarTheme;

        if (!sidebarTheme) {
            documentElement.removeAttribute('data-environment');

            return;
        }

        documentElement.setAttribute('data-environment', sidebarTheme);

        return () => documentElement.removeAttribute('data-environment');
    }, [currentEnvironmentId]);

    return (
        <Sidebar className="h-full" collapsible="icon">
            <SidebarHeader>
                <div className="flex items-center justify-between gap-2 group-data-[collapsible=icon]:flex-col group-data-[collapsible=icon]:gap-1">
                    <Link className="flex items-center gap-2 py-1" to="/">
                        <span className="flex size-10 shrink-0 items-center justify-center">
                            <img alt="ByteChef" className="size-8 max-w-none shrink-0" src={reactLogo} />
                        </span>

                        <span className="text-lg font-semibold group-data-[collapsible=icon]:hidden">ByteChef</span>
                    </Link>

                    <EnvironmentSelect variant={collapsed ? 'icon' : 'compact'} />
                </div>
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
