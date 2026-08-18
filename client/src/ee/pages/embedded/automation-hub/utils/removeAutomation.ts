import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';

export interface RemoveAutomationHandlersI {
    onDeleteAutomation: (workflowUuid: string) => void;
    onDeprovisionReference: (workflowUuid: string) => void;
}

/**
 * The single place the COPY/REFERENCE removal split lives. A COPY is the connected user's own
 * workflow and is deleted outright; a REFERENCE has no workflow of its own, so it is de-provisioned
 * by the catalog workflow uuid it points at.
 *
 * Both surfaces that can remove an automation — the activated template card and the "Your
 * automations" row — call this rather than branching themselves, so the two cannot drift into
 * calling different endpoints for the same kind.
 */
export const removeAutomation = (
    automation: ConnectedUserProjectWorkflow,
    {onDeleteAutomation, onDeprovisionReference}: RemoveAutomationHandlersI
) => {
    if (automation.kind === 'COPY') {
        onDeleteAutomation(automation.workflowUuid!);
    } else {
        onDeprovisionReference(automation.catalogWorkflowUuid ?? automation.workflowUuid!);
    }
};
