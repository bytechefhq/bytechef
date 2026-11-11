export const AUTOMATION_GETTING_STARTED_ROUTE_PATH = 'getting-started';

export const AUTOMATION_DEFAULT_ROUTE_PATH = 'projects';

export const AUTOMATION_DEPLOYMENTS_ROUTE_PATH = 'deployments';

interface ResolveAutomationIndexRedirectProps {
    canEdit: boolean;
    completed?: boolean;
}

export default function resolveAutomationIndexRedirect({
    canEdit,
    completed,
}: ResolveAutomationIndexRedirectProps): 'getting-started' | 'projects' {
    if (canEdit && completed === false) {
        return AUTOMATION_GETTING_STARTED_ROUTE_PATH;
    }

    return AUTOMATION_DEFAULT_ROUTE_PATH;
}
