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
