import {SidebarInset, SidebarProvider} from '@/components/ui/sidebar';
import {Toaster} from '@/components/ui/sonner';
import useFetchInterceptor from '@/config/useFetchInterceptor';
import {useUserGuiding} from '@/hooks/useUserGuiding';
import {useCloseProjectsLeftSidebarOnLeave} from '@/pages/automation/project/hooks/useCloseProjectsLeftSidebarOnLeave';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {bootstrapCommandBar} from '@/shared/command-bar/commandBarBootstrap';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {useRegisterNavigationCommands} from '@/shared/command-bar/useRegisterNavigationCommands';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHelpHub} from '@/shared/hooks/useHelpHub';
import {useOnLogout} from '@/shared/hooks/useOnLogout';
import {MobileTopNavigation} from '@/shared/layout/MobileTopNavigation';
import {TrialBanner} from '@/shared/layout/TrialBanner';
import {AppSidebar} from '@/shared/layout/app-sidebar/AppSidebar';
import {
    type NavigationItemI,
    automationNavigation,
    embeddedNavigation,
    platformNavigation,
} from '@/shared/navigation/navigationItems';
import {isStandaloneRoute} from '@/shared/navigation/standaloneRoutes';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {Suspense, lazy, useEffect, useLayoutEffect} from 'react';
import {Outlet, useLocation} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const CopilotPanel = lazy(() => import('@/shared/components/copilot/CopilotPanel'));
const CommandBarDialog = lazy(() => import('@/components/CommandBar/CommandBarDialog'));

const NO_NAVIGATION_ITEMS: NavigationItemI[] = [];

function App() {
    const commandBarOpen = useCommandBarStore((state) => state.open);
    const setCommandBarOpen = useCommandBarStore((state) => state.setOpen);

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

    const filteredAutomationNavigation = automationNavigation.filter((navItem) => {
        if (
            currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT &&
            edition === EditionType.EE &&
            navItem.href === '/automation/projects'
        ) {
            return false;
        }

        if (currentEnvironmentId === DEVELOPMENT_ENVIRONMENT && navItem.href === '/automation/deployments') {
            return false;
        }

        if (navItem.href === '/automation/api-platform') {
            return ff_1023;
        }

        if (navItem.href === '/automation/knowledge-bases') {
            return ai.knowledgeBase.enabled;
        }

        if (navItem.href === '/automation/executions/tool-invocations') {
            return edition === EditionType.EE;
        }

        return true;
    });

    const filteredEmbeddedNavigation = embeddedNavigation.filter((navItem) => {
        if (currentEnvironmentId !== 0 && navItem.href === '/embedded/integrations') {
            return false;
        }

        if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT && navItem.href === '/embedded/automation-workflows') {
            return false;
        }

        if (currentEnvironmentId === DEVELOPMENT_ENVIRONMENT && navItem.href === '/embedded/configurations') {
            return false;
        }

        if (
            currentEnvironmentId === DEVELOPMENT_ENVIRONMENT &&
            navItem.href === '/embedded/automation-configurations'
        ) {
            return false;
        }

        return true;
    });

    let navigation: NavigationItemI[] = [];

    if (location.pathname.includes('/automation/')) {
        navigation = filteredAutomationNavigation;
    } else if (location.pathname.includes('/embedded/')) {
        navigation = filteredEmbeddedNavigation;
    }

    useRegisterNavigationCommands(ff_2396 ? navigation : NO_NAVIGATION_ITEMS);

    useEffect(() => {
        if (ff_2396) {
            bootstrapCommandBar();
        }
    }, [ff_2396]);

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
                setCommandBarOpen(true);
            }
        };

        document.addEventListener('keydown', handleKeyDown);

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [ff_2396, setCommandBarOpen]);

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

                {ff_2396 && commandBarOpen && (
                    <Suspense fallback={null}>
                        <CommandBarDialog />
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
