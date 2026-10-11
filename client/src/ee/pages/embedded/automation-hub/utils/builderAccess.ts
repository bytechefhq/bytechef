import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';
import {useShallow} from 'zustand/react/shallow';

export interface BuilderAccessI {
    editWorkflowAllowed: boolean;
    newWorkflowEnabled: boolean;
}

export const useBuilderAccess = (): BuilderAccessI =>
    useAutomationHubStore(
        useShallow((state) => ({
            editWorkflowAllowed: state.editWorkflowAllowed,
            newWorkflowEnabled: state.tabs.newWorkflow,
        }))
    );

export const canOpenInBuilder = (
    automation: Pick<ConnectedUserProjectWorkflow, 'copiedFromWorkflowUuid' | 'dangling' | 'kind'>,
    {editWorkflowAllowed, newWorkflowEnabled}: BuilderAccessI
): boolean => {
    if (automation.kind === 'REFERENCE' || automation.dangling) {
        return false;
    }

    return editWorkflowAllowed || (!automation.copiedFromWorkflowUuid && newWorkflowEnabled);
};
