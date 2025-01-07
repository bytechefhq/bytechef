import {Toaster} from '@/components/ui/toaster';
import useFetchInterceptor from '@/config/useFetchInterceptor';
import CopilotPanel from '@/pages/platform/copilot/CopilotPanel';
import {useCopilotStore} from '@/pages/platform/copilot/stores/useCopilotStore';
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
    LandmarkIcon,
    Layers3Icon,
    Link2Icon,
    LucideIcon,
    Settings2Icon,
    SquareIcon,
    UnplugIcon,
    UsersIcon,
    ZapIcon,
} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Outlet, useLocation, useNavigate, useSearchParams} from 'react-router-dom';

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
        href: '/automation/instances',
        icon: Layers3Icon,
        name: 'Project Instances',
    },
    {
        href: '/automation/api-platform',
        icon: LandmarkIcon,
        name: 'API Collections',
    },
    {
        href: '/automation/executions',
        icon: ActivityIcon,
        name: 'Workflow Execution History',
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
    {
        href: '/embedded/connected-users',
        icon: UsersIcon,
        name: 'Connected Users',
    },
    {href: '/embedded/app-events', icon: ZapIcon, name: 'App Events'},
    {
        href: '/embedded/executions',
        icon: ActivityIcon,
        name: 'Workflow Execution History',
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

    const {ai, getApplicationInfo} = useApplicationInfoStore();
    const {
        account,
        authenticated,
        getAccount,
        loginError,
        reset: resetAuthentication,
        sessionHasBeenFetched,
        showLogin,
    } = useAuthenticationStore();

    const {copilotPanelOpen} = useCopilotStore();

    const analytics = useAnalytics();

    const helpHub = useHelpHub();

    const location = useLocation();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();
    const key = searchParams.get('key');

    const queryClient = useQueryClient();

    const ff_1023 = useFeatureFlagsStore()('ff-1023');

    const filteredAutomationNavigation = automationNavigation.filter((navItem) => {
        if (navItem.href === '/automation/api-platform/api-collections') {
            return ff_1023;
        }

        return true;
    });

    let navigation: NavigationType[] = [];

    if (location.pathname.includes('automation')) {
        navigation = filteredAutomationNavigation;
    } else if (location.pathname.includes('embedded')) {
        navigation = embeddedNavigation;
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
        getApplicationInfo();
    }, [getApplicationInfo]);

    useEffect(() => {
        if (showLogin && !key) {
            navigate('/login');
        }
    }, [showLogin, navigate, key]);

    useEffect(() => {
        if (sessionHasBeenFetched && !authenticated && !key && !loginError) {
            navigate('/login');
        }
    }, [authenticated, sessionHasBeenFetched, key, navigate, loginError]);

    useEffect(() => {
        if (loginError) {
            navigate('/account-error', {
                state: {error: 'Failed to sign in, please check your credentials and try again.'},
            });
        }
    }, [loginError, navigate]);

    useEffect(() => {
        if (!authenticated) {
            analytics.reset();
            helpHub.shutdown();
            resetAuthentication();
            queryClient.resetQueries();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [authenticated]);

    if (!sessionHasBeenFetched) {
        return (
            <div className="flex min-h-screen min-w-full items-center justify-center p-5">
                <div className="flex animate-pulse space-x-2">
                    <div className="size-3 rounded-full bg-gray-500"></div>

                    <div className="size-3 rounded-full bg-gray-500"></div>

                    <div className="size-3 rounded-full bg-gray-500"></div>
                </div>
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
                        <aside className="border-l border-l-border/70">
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
