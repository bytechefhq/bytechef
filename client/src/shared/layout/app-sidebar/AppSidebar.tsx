import Button from '@/components/Button/Button';
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
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {type LucideIcon, PanelLeftCloseIcon} from 'lucide-react';
import {useEffect} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';

import {AppSidebarFooter} from './AppSidebarFooter';
import {AppSidebarWorkspaceSelect} from './AppSidebarWorkspaceSelect';

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

    const navigate = useNavigate();

    const {isMobile, state, toggleSidebar} = useSidebar();

    const currentEnvironmentId = useEnvironmentStore((environmentState) => environmentState.currentEnvironmentId);

    const collapsed = state === 'collapsed' && !isMobile;

    const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

    // Projects and integrations (and their workflow editors) only exist in development, so leaving
    // development moves the user to the environment-scoped deployments/configurations page instead.
    const handleEnvironmentChange = (environmentId: number) => {
        if (environmentId === DEVELOPMENT_ENVIRONMENT) {
            return;
        }

        if (isActive('/automation/projects')) {
            navigate('/automation/deployments');
        } else if (isActive('/embedded/integrations')) {
            navigate('/embedded/configurations');
        }
    };

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
                <div className="flex items-center justify-between gap-2 group-data-[collapsible=icon]:flex-col group-data-[collapsible=icon]:gap-3">
                    <AppSidebarWorkspaceSelect />

                    <EnvironmentSelect onChange={handleEnvironmentChange} variant="icon" />

                    {!collapsed && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Close sidebar"
                                    className="size-auto shrink-0 p-0 text-muted-foreground hover:bg-transparent hover:text-foreground [&_svg]:size-5"
                                    icon={<PanelLeftCloseIcon />}
                                    onClick={toggleSidebar}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">Close sidebar</TooltipContent>
                        </Tooltip>
                    )}
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
                                            className="h-10 gap-3 text-sm group-data-[collapsible=icon]:!size-10 data-[active=true]:font-medium data-[active=true]:text-sidebar-active-foreground [&>svg]:size-6"
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
