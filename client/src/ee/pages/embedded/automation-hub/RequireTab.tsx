import {
    AutomationHubTabsI,
    useAutomationHubStore,
} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {ReactNode, useMemo} from 'react';
import {Navigate} from 'react-router-dom';

interface RequireTabPropsI {
    children: ReactNode;
    tab: keyof AutomationHubTabsI;
}

/**
 * Guards a hub route whose tab a vendor may have disabled. Reads the store directly (rather than
 * relying on the tab strip, which simply omits the link) so a disabled tab is unreachable by a
 * deep link or a stale bookmark too, and redirects to the first tab that is still enabled.
 *
 * If every route-backed tab (automations/connections) is disabled — leaving only, say,
 * `newWorkflow` — there is no visible tab left to redirect to; redirecting to this same route's
 * own path would just bounce back here forever. Render an inline empty state instead.
 */
const RequireTab = ({children, tab}: RequireTabPropsI) => {
    const tabs = useAutomationHubStore((state) => state.tabs);

    const firstVisiblePath = useMemo(() => {
        if (tabs.automations) {
            return '/embedded/hub';
        }

        if (tabs.connections) {
            return '/embedded/hub/connections';
        }

        return undefined;
    }, [tabs]);

    if (tabs[tab]) {
        return <>{children}</>;
    }

    if (!firstVisiblePath) {
        return (
            <div
                className="flex size-full items-center justify-center text-muted-foreground"
                data-testid="require-tab-empty-state"
            >
                No sections are enabled for this hub.
            </div>
        );
    }

    return <Navigate replace to={firstVisiblePath} />;
};

export default RequireTab;
