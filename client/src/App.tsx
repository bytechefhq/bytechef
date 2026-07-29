import {SidebarInset, SidebarProvider} from '@/components/ui/sidebar';
import {Toaster} from '@/components/ui/sonner';
import useFetchInterceptor from '@/config/useFetchInterceptor';
import {useUserGuiding} from '@/hooks/useUserGuiding';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHelpHub} from '@/shared/hooks/useHelpHub';
import {MobileTopNavigation} from '@/shared/layout/MobileTopNavigation';
import {TrialBanner} from '@/shared/layout/TrialBanner';
import {AppSidebar} from '@/shared/layout/app-sidebar/AppSidebar';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ActivityIcon,
    BoxesIcon,
    CircleIcon,
    FileTextIcon,
    FolderIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    LucideIcon,
    MessagesSquareIcon,
    NetworkIcon,
    ServerIcon,
    Settings2Icon,
    SparklesIcon,
    SquareIcon,
    Table2Icon,
    UnplugIcon,
    UsersIcon,
    VectorSquareIcon,
    Workflow,
    ZapIcon,
} from 'lucide-react';
import {Suspense, lazy, useEffect, useState} from 'react';
import {Outlet, useLocation} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const CopilotPanel = lazy(() => import('@/shared/components/copilot/CopilotPanel'));
const GlobalSearchDialog = lazy(() => import('@/components/GlobalSearch/GlobalSearchDialog'));

type NavigationType = {
    name: string;
    href: string;
    icon: LucideIcon;
    group?: string;
};

const automationNavigation: NavigationType[] = [
    {
        href: '/automation/ai-hub',
        icon: MessagesSquareIcon,
        name: 'AI Hub',
    },
    {href: '/automation/approval-tasks', icon: CircleIcon, name: 'Approval Tasks'},
    {
        href: '/automation/projects',
        icon: FolderIcon,
        name: 'Projects',
    },
    {
        group: 'Deployments',
        href: '/automation/deployments',
        icon: Layers3Icon,
        name: 'Project Deployments',
    },
    {
        group: 'Deployments',
        href: '/automation/api-platform',
        icon: LayoutTemplateIcon,
        name: 'API Collections',
    },
    {
        group: 'Deployments',
        href: '/automation/mcp-servers',
        icon: ServerIcon,
        name: 'MCP Servers',
    },
    {
        group: 'Deployments',
        href: '/automation/a2a-servers',
        icon: NetworkIcon,
        name: 'A2A Servers',
    },
    {
        group: 'Deployments',
        href: '/automation/context-stores',
        icon: BoxesIcon,
        name: 'Context Store',
    },
    {
        href: '/automation/executions',
        icon: ActivityIcon,
        name: 'Executions',
    },
    {href: '/automation/connections', icon: Link2Icon, name: 'Connections'},
    {
        group: 'Data',
        href: '/automation/datatables',
        icon: Table2Icon,
        name: 'Data Tables',
    },
    {
        group: 'Data',
        href: '/automation/knowledge-bases',
        icon: VectorSquareIcon,
        name: 'Knowledge Base',
    },
    {
        group: 'Data',
        href: '/automation/asset-files',
        icon: FileTextIcon,
        name: 'Files',
    },
    {href: '/automation/ai', icon: SparklesIcon, name: 'AI'},
];

const embeddedNavigation: NavigationType[] = [
    {
        href: '/embedded/integrations',
        icon: SquareIcon,
        name: 'Integrations',
    },
    {
        group: 'Configurations',
        href: '/embedded/configurations',
        icon: Settings2Icon,
        name: 'Integration Configurations',
    },
    {group: 'Configurations', href: '/embedded/mcp-servers', icon: ServerIcon, name: 'MCP Servers'},
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
        name: 'Executions',
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

    const {ai, billingEnabled, contextStoreEnabled, edition} = useApplicationInfoStore(
        useShallow((state) => ({
            ai: state.ai,
            billingEnabled: state.billing.enabled,
            contextStoreEnabled: state.contextStore.enabled,
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
    const ff_2446 = useFeatureFlagsStore()('ff-2446');
    const ff_2396 = useFeatureFlagsStore()('ff-2396');
    const ff_4855 = useFeatureFlagsStore()('ff-4855');

    const filteredAutomationNavigation = automationNavigation.filter((navItem) => {
        if (
            currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT &&
            edition === EditionType.EE &&
            navItem.href === '/automation/projects'
        ) {
            return false;
        }

        if (navItem.href === '/automation/api-platform') {
            return ff_1023;
        }

        if (navItem.href === '/automation/knowledge-bases') {
            return ai.knowledgeBase.enabled;
        }

        if (navItem.href === '/automation/context-stores') {
            return ff_4855 && edition === EditionType.EE && contextStoreEnabled;
        }

        if (navItem.href === '/automation/ai-hub') {
            return edition === EditionType.EE && ai.hub.enabled;
        }

        return true;
    });

    const filteredEmbeddedNavigation = embeddedNavigation.filter((navItem) => {
        if (currentEnvironmentId !== 0 && navItem.href === '/embedded/integrations') {
            return false;
        }

        if (navItem.href === '/embedded/mcp-servers') {
            return ff_2446;
        }

        return true;
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
            userGuiding.identify(account);
        }
    }, [account, helpHub, userGuiding]);

    useEffect(() => {
        // Format `ByteChef | <Page>` (or just `ByteChef` if no nav match). Match by `startsWith` instead
        // of strict equality so deep-linked sub-routes like `/automation/asset-files/123` still pick up
        // the parent `Files` entry's title — without this, the title falls back to the bare `ByteChef`
        // default and the user's tab loses context as soon as they open a file detail. The exact-match
        // sort ensures longer prefixes win (e.g. `/automation/api-platform/foo` matches `api-platform`,
        // not the empty parent), so the standard nav-driven title behavior is preserved on the list
        // pages while picking up the right entry on nested routes.
        const allNavItems = [...automationNavigation, ...embeddedNavigation, ...platformNavigation].sort(
            (a, b) => b.href.length - a.href.length
        );

        const matchedNavItem = allNavItems.find((navItem) => location.pathname.startsWith(navItem.href));

        document.title = matchedNavItem ? `ByteChef | ${matchedNavItem.name}` : 'ByteChef';
    }, [location]);

    useEffect(() => {
        if (!authenticated) {
            analytics.reset();

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
                <AppSidebar navigation={navigation} />

                <SidebarInset className="flex h-full min-w-0 flex-col">
                    <MobileTopNavigation />

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

                <Toaster />

                {ff_2396 && searchOpen && (
                    <Suspense fallback={null}>
                        <GlobalSearchDialog onOpenChange={setSearchOpen} open={searchOpen} />
                    </Suspense>
                )}
            </SidebarProvider>
        </div>
    ) : (
        <Outlet />
    );
}

export default App;
