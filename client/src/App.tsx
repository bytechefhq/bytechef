import GlobalSearchDialog from '@/components/GlobalSearch/GlobalSearchDialog';
import {Toaster} from '@/components/ui/toaster';
import useFetchInterceptor from '@/config/useFetchInterceptor';
import {useUserGuiding} from '@/hooks/useUserGuiding';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHelpHub} from '@/shared/hooks/useHelpHub';
import {MobileSidebar} from '@/shared/layout/MobileSidebar';
import {MobileTopNavigation} from '@/shared/layout/MobileTopNavigation';
import {DesktopSidebar} from '@/shared/layout/desktop-sidebar/DesktopSidebar';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ActivityIcon,
    CircleIcon,
    FolderIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    LucideIcon,
    MessageCircleMoreIcon,
    ServerIcon,
    Settings2Icon,
    SquareIcon,
    Table2Icon,
    UnplugIcon,
    UsersIcon,
    VectorSquareIcon,
    Workflow,
    ZapIcon,
} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Outlet, useLocation} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const user = {
    email: 'emily.selman@example.com',
    imageUrl:
        'https://images.unsplash.com/photo-1502685104226-ee32379fefbe?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80',
    name: 'Emily Selman',
};

type NavigationType = {
    name: string;
    href: string;
    icon: LucideIcon;
};

const automationNavigation: NavigationType[] = [
    {
        href: '/automation/projects',
        icon: FolderIcon,
        name: 'Projects',
    },
    {
        href: '/automation/deployments',
        icon: Layers3Icon,
        name: 'Project Deployments',
    },
    {
        href: '/automation/api-platform',
        icon: LayoutTemplateIcon,
        name: 'API Collections',
    },
    {
        href: '/automation/mcp-servers',
        icon: ServerIcon,
        name: 'MCP Servers',
    },
    {
        href: '/automation/executions',
        icon: ActivityIcon,
        name: 'Workflow Executions',
    },
    {href: '/automation/connections', icon: Link2Icon, name: 'Connections'},
    {
        href: '/automation/datatables',
        icon: Table2Icon,
        name: 'Data Tables',
    },
    {
        href: '/automation/knowledge-bases',
        icon: VectorSquareIcon,
        name: 'Knowledge Base',
    },
    {href: '/automation/chat', icon: MessageCircleMoreIcon, name: 'Workflow Chat'},
    {href: '/automation/approval-tasks', icon: CircleIcon, name: 'Approval Tasks'},
];

const embeddedNavigation: NavigationType[] = [
    {
        href: '/embedded/integrations',
        icon: SquareIcon,
        name: 'Integrations',
    },
    {
        href: '/embedded/configurations',
        icon: Settings2Icon,
        name: 'Integration Configurations',
    },
    {href: '/embedded/app-events', icon: ZapIcon, name: 'App Events'},
    {
        href: '/embedded/automation-workflows',
        icon: Workflow,
        name: 'Automations',
    },
    {
        href: '/embedded/connected-users',
        icon: UsersIcon,
        name: 'Connected Users',
    },
    {
        href: '/embedded/executions',
        icon: ActivityIcon,
        name: 'Workflow Executions',
    },
    {href: '/embedded/connections', icon: Link2Icon, name: 'Connections'},
];

const platformNavigation = [
    {
        href: '/platform/connectors',
        icon: UnplugIcon,
        name: 'Connectors',
    },
];

function App() {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [searchOpen, setSearchOpen] = useState(false);

    const {ai, edition} = useApplicationInfoStore(
        useShallow((state) => ({
            ai: state.ai,
            edition: state.application?.edition,
        }))
    );
    const {
        account,
        authenticated,
        reset: resetAuthentication,
    } = useAuthenticationStore(
        useShallow((state) => ({
            account: state.account,
            authenticated: state.authenticated,
            reset: state.reset,
        }))
    );
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {currentType, setCurrentType} = usePlatformTypeStore(
        useShallow((state) => ({
            currentType: state.currentType,
            setCurrentType: state.setCurrentType,
        }))
    );

    const analytics = useAnalytics();
    const helpHub = useHelpHub();
    const location = useLocation();
    const queryClient = useQueryClient();
    const userGuiding = useUserGuiding();

    useFetchInterceptor();

    const ff_298 = useFeatureFlagsStore()('ff-298');
    const ff_732 = useFeatureFlagsStore()('ff-732');
    const ff_1023 = useFeatureFlagsStore()('ff-1023');
    const ff_1779 = useFeatureFlagsStore()('ff-1779');
    const ff_2445 = useFeatureFlagsStore()('ff-2445');
    const ff_2311 = useFeatureFlagsStore()('ff-2311');
    const ff_2396 = useFeatureFlagsStore()('ff-2396');
    const ff_2894 = useFeatureFlagsStore()('ff-2894');
    const ff_3955 = useFeatureFlagsStore()('ff-3955');
    const ff_4000 = useFeatureFlagsStore()('ff-4000');

    const filteredAutomationNavigation = automationNavigation.filter((navItem) => {
        if (
            currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT &&
            edition === EditionType.EE &&
            navItem.href === '/automation/projects'
        ) {
            return false;
        }

        if (navItem.href === '/automation/api-platform/api-collections') {
            return ff_1023;
        }

        if (navItem.href === '/automation/mcp-servers') {
            return ff_2445;
        }

        if (navItem.href === '/automation/chat') {
            return ff_2311 || ff_2894;
        }

        if (navItem.href === '/automation/datatables') {
            return ff_3955;
        }

        if (navItem.href === '/automation/knowledge-bases') {
            return ff_4000;
        }

        if (navItem.href === '/automation/approval-tasks') {
            return ff_732;
        }

        return true;
    });

    const filteredEmbeddedNavigation = embeddedNavigation.filter((navItem) => {
        if (currentEnvironmentId !== 0 && navItem.href === '/embedded/integrations') {
            return false;
        }

        if (
            (ff_1779 && navItem.href === '/embedded/automation-workflows') ||
            navItem.href !== '/embedded/automation-workflows'
        ) {
            return true;
        }

        return false;
    });

    let navigation: NavigationType[] = [];

    if (location.pathname.includes('/automation/')) {
        navigation = filteredAutomationNavigation;
    } else if (location.pathname.includes('/embedded/')) {
        navigation = filteredEmbeddedNavigation;
    }

    useEffect(() => {
        if (account) {
            helpHub.boot(account);
            helpHub.addRouter();

            if (ff_298) {
                userGuiding.identify(account);
            }
        }
    }, [account, ff_298, helpHub, userGuiding]);

    useEffect(() => {
        document.title =
            [...automationNavigation, ...embeddedNavigation, ...platformNavigation].find(
                (navItem) => navItem.href === location.pathname
            )?.name ?? 'ByteChef';
    }, [location]);

    useEffect(() => {
        if (!authenticated) {
            analytics.reset();

            helpHub.shutdown();

            if (ff_298) {
                userGuiding.shutdown();
            }

            resetAuthentication();

            queryClient.resetQueries();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [authenticated]);

    useEffect(() => {
        let type;

        if (location.pathname.includes('/automation/')) {
            type = PlatformType.AUTOMATION;
        } else if (location.pathname.includes('/embedded/')) {
            type = PlatformType.EMBEDDED;
        }

        if (type !== undefined && type !== currentType) {
            setCurrentType(type);
        }
    }, [currentType, location, setCurrentType]);

    useEffect(() => {
        if (!ff_2396) {
            return;
        }

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.defaultPrevented) {
                return;
            }

            if ((event.metaKey || event.ctrlKey) && event.key === 'k') {
                event.preventDefault();
                setSearchOpen(true);
            }
        };

        document.addEventListener('keydown', handleKeyDown);

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [ff_2396]);

    return authenticated ? (
        <div className="flex h-full">
            <MobileSidebar
                mobileMenuOpen={mobileMenuOpen}
                navigation={navigation}
                setMobileMenuOpen={setMobileMenuOpen}
                user={user}
            />

            <DesktopSidebar navigation={navigation} />

            <div className="flex h-full min-w-0 flex-1 flex-col">
                <MobileTopNavigation setMobileMenuOpen={setMobileMenuOpen} />

                <div className="flex size-full">
                    <div className="flex h-full min-w-0 flex-1">
                        <Outlet />
                    </div>

                    {ai.copilot.enabled && (
                        <aside className="h-full shrink-0">
                            <CopilotPanel open={copilotPanelOpen} />
                        </aside>
                    )}
                </div>
            </div>

            <Toaster />

            {ff_2396 && <GlobalSearchDialog onOpenChange={setSearchOpen} open={searchOpen} />}
        </div>
    ) : (
        <Outlet />
    );
}

export default App;
