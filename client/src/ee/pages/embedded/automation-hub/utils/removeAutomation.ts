import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';

interface RemoveAutomationHandlersI {
    onDeleteAutomation: (workflowUuid: string) => Promise<unknown>;
    onDeprovisionReference: (workflowUuid: string) => Promise<unknown>;
}

export const removeAutomation = (
    automation: ConnectedUserProjectWorkflow,
    {onDeleteAutomation, onDeprovisionReference}: RemoveAutomationHandlersI
): Promise<unknown> => {
    if (automation.kind === 'COPY') {
        return onDeleteAutomation(automation.workflowUuid!);
    }

    return onDeprovisionReference(automation.automationWorkflowUuid ?? automation.workflowUuid!);
};
