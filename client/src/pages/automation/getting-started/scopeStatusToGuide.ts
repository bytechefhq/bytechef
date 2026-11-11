import {AutomationGettingStartedStatusType} from './automationGettingStartedStatus';
import {hasFirstRun} from './getAutomationGettingStartedSteps';

interface ScopeStatusToGuideProps {
    hasGuideProject: boolean;
    hasGuideWorkflow: boolean;
    status?: AutomationGettingStartedStatusType;
}

export default function scopeStatusToGuide({
    hasGuideProject,
    hasGuideWorkflow,
    status,
}: ScopeStatusToGuideProps): AutomationGettingStartedStatusType | undefined {
    if (!status || hasFirstRun(status) || hasGuideWorkflow) {
        return status;
    }

    return {
        ...status,
        deployed: false,
        httpClientAdded: false,
        lastFailedRun: null,
        live: false,
        loggerAdded: false,
        projectCreated: hasGuideProject,
        published: false,
        workflowCreated: false,
    };
}
