import LoadingDots from '@/components/LoadingDots';
import {Toaster} from '@/components/ui/toaster';
import useFetchInterceptor from '@/config/useFetchInterceptor';
import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHelpHub} from '@/shared/hooks/useHelpHub';
import {MobileSidebar} from '@/shared/layout/MobileSidebar';
import {MobileTopNavigation} from '@/shared/layout/MobileTopNavigation';
import {DesktopSidebar} from '@/shared/layout/desktop-sidebar/DesktopSidebar';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ActivityIcon,
    FolderIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    LucideIcon,
    ServerIcon,
    Settings2Icon,
    SquareIcon,
    UnplugIcon,
    UsersIcon,
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

    const {ai, getApplicationInfo, loading} = useApplicationInfoStore(
        useShallow((state) => ({
            ai: state.ai,
            getApplicationInfo: state.getApplicationInfo,
            loading: state.loading,
        }))
    );
    const {
        account,
        authenticated,
        getAccount,
        reset: resetAuthentication,
        sessionHasBeenFetched,
    } = useAuthenticationStore(
        useShallow((state) => ({
            account: state.account,
            authenticated: state.authenticated,
            getAccount: state.getAccount,
            reset: state.reset,
            sessionHasBeenFetched: state.sessionHasBeenFetched,
        }))
    );
    const copilotPanelOpen = useCopilotStore((state) => state.copilotPanelOpen);
    const {currentType, setCurrentType} = useModeTypeStore(
        useShallow((state) => ({
            currentType: state.currentType,
            setCurrentType: state.setCurrentType,
        }))
    );

    const analytics = useAnalytics();

    const helpHub = useHelpHub();

    const location = useLocation();

    const queryClient = useQueryClient();

    useEffect(() => {
        getApplicationInfo();
    }, [getApplicationInfo]);

    const ff_1023 = useFeatureFlagsStore()('ff-1023');
    const ff_1779 = useFeatureFlagsStore()('ff-1779');
    const ff_2445 = useFeatureFlagsStore()('ff-2445');

    const filteredAutomationNavigation = automationNavigation.filter((navItem) => {
        if (navItem.href === '/automation/api-platform/api-collections') {
            return ff_1023;
        }

        if (navItem.href === '/automation/mcp-servers') {
            return ff_2445;
        }

        return true;
    });

    let navigation: NavigationType[] = [];

    if (location.pathname.includes('/automation/')) {
        navigation = filteredAutomationNavigation;
    } else if (location.pathname.includes('/embedded/')) {
        navigation = embeddedNavigation.filter((navItem) => {
            if (
                (ff_1779 && navItem.href === '/embedded/automation-workflows') ||
                navItem.href !== '/embedded/automation-workflows'
            ) {
                return true;
            }
        });
    }

    useFetchInterceptor();

    useEffect(() => {
        analytics.init();
    }, [analytics]);

    useEffect(() => {
        helpHub.init();
    }, [helpHub]);

    useEffect(() => {
        if (account) {
            helpHub.boot(account);
            helpHub.addRouter();
        }
    }, [account, helpHub]);

    useEffect(() => {
        document.title =
            [...automationNavigation, ...embeddedNavigation, ...platformNavigation].find(
                (navItem) => navItem.href === location.pathname
            )?.name ?? 'ByteChef';
    }, [location]);

    useEffect(() => {
        if (!sessionHasBeenFetched) {
            getAccount();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [sessionHasBeenFetched]);

    useEffect(() => {
        if (!authenticated) {
            analytics.reset();
            helpHub.shutdown();
            resetAuthentication();
            queryClient.resetQueries();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [authenticated]);

    useEffect(() => {
        let type;

        if (location.pathname.includes('/automation/')) {
            type = ModeType.AUTOMATION;
        } else if (location.pathname.includes('/embedded/')) {
            type = ModeType.EMBEDDED;
        }

        if (type !== undefined && type !== currentType) {
            setCurrentType(type);
        }
    }, [currentType, location, setCurrentType]);

    if (loading || !sessionHasBeenFetched) {
        return (
            <div className="flex min-h-screen min-w-full items-center justify-center p-5">
                <LoadingDots />
            </div>
        );
    }

    return authenticated ? (
        <div className="flex h-full">
            <MobileSidebar
                mobileMenuOpen={mobileMenuOpen}
                navigation={navigation}
                setMobileMenuOpen={setMobileMenuOpen}
                user={user}
            />

            <DesktopSidebar navigation={navigation} />

            <div className="flex min-w-0 flex-1 flex-col">
                <MobileTopNavigation setMobileMenuOpen={setMobileMenuOpen} />

                <div className="flex size-full">
                    <Outlet />

                    {ai.copilot.enabled && copilotPanelOpen && (
                        <aside>
                            <CopilotPanel />
                        </aside>
                    )}
                </div>
            </div>

            <Toaster />
        </div>
    ) : (
        <Outlet />
    );
}

export default App;
