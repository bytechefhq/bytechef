import {SidebarInset, SidebarProvider} from '@/components/ui/sidebar';
import {Toaster} from '@/components/ui/sonner';
import useFetchInterceptor from '@/config/useFetchInterceptor';
import {useUserGuiding} from '@/hooks/useUserGuiding';
import {useCloseProjectsLeftSidebarOnLeave} from '@/pages/automation/project/hooks/useCloseProjectsLeftSidebarOnLeave';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHelpHub} from '@/shared/hooks/useHelpHub';
import {useOnLogout} from '@/shared/hooks/useOnLogout';
import {MobileTopNavigation} from '@/shared/layout/MobileTopNavigation';
import {TrialBanner} from '@/shared/layout/TrialBanner';
import {AppSidebar} from '@/shared/layout/app-sidebar/AppSidebar';
import {
    NavigationType,
    automationNavigation,
    executionsNavigationGroup,
    filterAutomationNavigation,
} from '@/shared/navigation/automationNavigation';
import {isStandaloneRoute} from '@/shared/navigation/standaloneRoutes';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ActivityIcon,
    Link2Icon,
    ServerIcon,
    Settings2Icon,
    SquareIcon,
    UnplugIcon,
    UsersIcon,
    Workflow,
    ZapIcon,
} from 'lucide-react';
import {Suspense, lazy, useEffect, useLayoutEffect, useState} from 'react';
import {Outlet, useLocation} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const CopilotPanel = lazy(() => import('@/shared/components/copilot/CopilotPanel'));
const GlobalSearchDialog = lazy(() => import('@/components/GlobalSearch/GlobalSearchDialog'));

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
    {href: '/embedded/mcp-servers', icon: ServerIcon, name: 'MCP Servers'},
    {
        href: '/embedded/connected-users',
        icon: UsersIcon,
        name: 'Connected Users',
    },
    {
        group: executionsNavigationGroup,
        href: '/embedded/executions',
        icon: ActivityIcon,
        name: 'Workflow Executions',
    },
    {
        group: executionsNavigationGroup,
        href: '/embedded/executions/tool-invocations',
        icon: WrenchIcon,
        name: 'Tool Invocations',
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
    const [searchOpen, setSearchOpen] = useState(false);

    const {ai, billingEnabled, edition} = useApplicationInfoStore(
        useShallow((state) => ({
            ai: state.ai,
            billingEnabled: state.billing.enabled,
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

    const ff_1023 = useFeatureFlagsStore()('ff-1023');
    const ff_2396 = useFeatureFlagsStore()('ff-2396');

    const filteredAutomationNavigation = filterAutomationNavigation({
        aiHubEnabled: ai.hub.enabled,
        apiPlatformEnabled: ff_1023,
        currentEnvironmentId,
        edition,
        knowledgeBaseEnabled: ai.knowledgeBase.enabled,
    });

    const filteredEmbeddedNavigation = embeddedNavigation.filter((navItem) => {
        if (currentEnvironmentId !== 0 && navItem.href === '/embedded/integrations') {
            return false;
        }

        if (currentEnvironmentId === DEVELOPMENT_ENVIRONMENT && navItem.href === '/embedded/configurations') {
            return false;
        }

        return true;
    });

    let navigation: NavigationType[] = [];

    if (location.pathname.includes('/automation/')) {
        navigation = filteredAutomationNavigation;
    } else if (location.pathname.includes('/embedded/')) {
        navigation = filteredEmbeddedNavigation;
    }

    const standaloneRoute = isStandaloneRoute(location.pathname);

    useCloseProjectsLeftSidebarOnLeave(location.pathname);

    useEffect(() => {
        if (account) {
            helpHub.boot(account);
            helpHub.addRouter();
            userGuiding.identify(account);
        }
    }, [account, helpHub, userGuiding]);

    useOnLogout(() => analytics.reset());

    useLayoutEffect(() => {
        document.title =
            [...automationNavigation, ...embeddedNavigation, ...platformNavigation]
                .filter(
                    (navItem) => location.pathname === navItem.href || location.pathname.startsWith(`${navItem.href}/`)
                )
                .sort((navItem, otherNavItem) => otherNavItem.href.length - navItem.href.length)[0]?.name ?? 'ByteChef';
    }, [location]);

    useEffect(() => {
        if (!authenticated) {
            helpHub.shutdown();
            userGuiding.shutdown();

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
        <div className="flex h-full flex-col">
            {billingEnabled && location.pathname.includes('/automation/') && <TrialBanner />}

            {/* transform-gpu gives fixed-position descendants (page sidebars) a containing
                block scoped to this element, so they render below the banner instead of
                anchoring to the viewport top and overlapping it. */}

            <SidebarProvider className="min-h-0 flex-1 transform-gpu" defaultOpen={false}>
                {!standaloneRoute && <AppSidebar navigation={navigation} />}

                <SidebarInset className="flex h-full min-w-0 flex-col">
                    {!standaloneRoute && <MobileTopNavigation />}

                    <div className="flex size-full">
                        <div className="flex h-full min-w-0 flex-1">
                            <Outlet />
                        </div>

                        {ai.copilot.enabled && (
                            <aside className="h-full shrink-0">
                                <Suspense fallback={null}>
                                    <CopilotPanel open={copilotPanelOpen} />
                                </Suspense>
                            </aside>
                        )}
                    </div>
                </SidebarInset>

                {ff_2396 && searchOpen && (
                    <Suspense fallback={null}>
                        <GlobalSearchDialog onOpenChange={setSearchOpen} open={searchOpen} />
                    </Suspense>
                )}
            </SidebarProvider>

            <Toaster />
        </div>
    ) : (
        <Outlet />
    );
}

export default App;
