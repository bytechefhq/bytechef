import LoadingDots from '@/components/LoadingDots';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {applyHubTheme} from '@/ee/pages/embedded/automation-hub/theme/applyHubTheme';
import {useTheme} from '@/shared/providers/theme-provider';
import {useEffect, useMemo} from 'react';
import {Link, Outlet, useLocation} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

/**
 * The hub's chrome: the product name plus the tab strip, which is omitted entirely when fewer than
 * two sections are enabled. The product name is deliberately not an `h1` — it labels the chrome,
 * and each view owns its own page heading, so an `h1` here would collide with the Automations
 * view's.
 */
const AutomationHubLayout = () => {
    const {initialized, tabs, theme} = useAutomationHubStore(
        useShallow((state) => ({initialized: state.initialized, tabs: state.tabs, theme: state.theme}))
    );
    const {setTheme} = useTheme();
    const location = useLocation();

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

    if (!initialized) {
        return (
            <div className="flex size-full items-center justify-center" data-testid="automation-hub-loading">
                <LoadingDots />
            </div>
        );
    }

    return (
        <div className="flex size-full flex-col bg-background text-foreground">
            <header className="flex items-center justify-between border-b px-6 py-3">
                <span className="text-lg font-semibold">Automation Hub</span>

                {visibleTabs.length > 1 && (
                    <nav className="flex gap-1" role="tablist">
                        {visibleTabs.map((tab) => (
                            <Link
                                aria-selected={location.pathname === tab.to}
                                className={twMerge(
                                    'rounded-md px-3 py-1.5 text-sm',
                                    location.pathname === tab.to
                                        ? 'bg-muted font-medium'
                                        : 'text-muted-foreground hover:bg-muted/60'
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
            </header>

            <main className="flex-1 overflow-auto p-6">
                <Outlet />
            </main>
        </div>
    );
};

export default AutomationHubLayout;
