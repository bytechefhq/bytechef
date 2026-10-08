import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';

// Checked in Development whichever environment is selected: workflow definitions are edited there, and the server
// checks every save against the member's Development role.
export const useProjectWorkflowReadOnly = (): boolean => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEditWorkflow = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);

    return !canEditWorkflow;
};
