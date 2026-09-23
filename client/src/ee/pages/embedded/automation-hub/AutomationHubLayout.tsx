import LoadingDots from '@/components/LoadingDots';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {applyHubTheme} from '@/ee/pages/embedded/automation-hub/theme/applyHubTheme';
import {useTheme} from '@/shared/providers/theme-provider';
import {useEffect, useMemo, useRef} from 'react';
import {Link, Outlet, useLocation, useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const HUB_ROUTE_STORAGE_KEY = 'automationHub.route';

const readStoredRoute = (): string | undefined => {
    try {
        const storedRoute = window.sessionStorage.getItem(HUB_ROUTE_STORAGE_KEY);

        return storedRoute?.startsWith('/embedded/hub') ? storedRoute : undefined;
    } catch {
        return undefined;
    }
};

const writeStoredRoute = (route: string) => {
    try {
        window.sessionStorage.setItem(HUB_ROUTE_STORAGE_KEY, route);
    } catch {
        return;
    }
};

const AutomationHubLayout = () => {
    const {initialized, tabs, theme} = useAutomationHubStore(
        useShallow((state) => ({
            initialized: state.initialized,
            tabs: state.tabs,
            theme: state.theme,
        }))
    );
    const {setTheme} = useTheme();
    const location = useLocation();
    const navigate = useNavigate();

    const restoredRouteRef = useRef(false);

    const builderRoute = location.pathname.startsWith('/embedded/hub/builder');

    const visibleTabs = useMemo(
        () =>
            [
                {enabled: tabs.automations, label: 'Automations', to: '/embedded/hub'},
                {enabled: tabs.connections, label: 'Connections', to: '/embedded/hub/connections'},
            ].filter((tab) => tab.enabled),
        [tabs]
    );

    useEffect(() => {
        if (initialized) {
            setTheme(applyHubTheme(theme));
        }
    }, [initialized, setTheme, theme]);

    useEffect(() => {
        if (!initialized || restoredRouteRef.current) {
            return;
        }

        restoredRouteRef.current = true;

        const storedRoute = readStoredRoute();

        if (storedRoute && storedRoute !== location.pathname) {
            void navigate(storedRoute, {replace: true});
        }
    }, [initialized, location.pathname, navigate]);

    useEffect(() => {
        if (!initialized || !restoredRouteRef.current) {
            return;
        }

        writeStoredRoute(location.pathname);
    }, [initialized, location.pathname]);

    if (!initialized) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="automation-hub-loading">
                <LoadingDots />
            </div>
        );
    }

    return (
        <div className="flex size-full flex-col bg-(--hub-surface) text-foreground">
            <main className={twMerge('flex min-h-0 flex-1 flex-col overflow-auto p-4', builderRoute && 'p-1')}>
                {!builderRoute && visibleTabs.length > 1 && (
                    <nav className="flex gap-1" role="tablist">
                        {visibleTabs.map((tab) => (
                            <Link
                                aria-selected={location.pathname === tab.to}
                                className={twMerge(
                                    'rounded-md px-3 py-1.5 text-sm',
                                    location.pathname === tab.to
                                        ? 'bg-(--hub-card) font-medium'
                                        : 'text-muted-foreground hover:bg-(--hub-card)/60'
                                )}
                                key={tab.to}
                                role="tab"
                                to={tab.to}
                            >
                                {tab.label}
                            </Link>
                        ))}
                    </nav>
                )}

                <div className={twMerge('flex min-h-0 flex-1 flex-col', !builderRoute && 'mt-6')}>
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default AutomationHubLayout;
