import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useWorkspaceScopeState} from '@/shared/hooks/useHasWorkspaceScope';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

// Reads Development, like useProjectWorkflowReadOnly, so the notice appears exactly when the editor is read-only.
export const useProjectWorkflowViewOnlyNotice = (): boolean => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const workspaceScopeStatus = usePermissionStore((state) =>
        currentWorkspaceId === undefined
            ? undefined
            : state.workspaceScopeStates[currentWorkspaceId]?.[DEVELOPMENT_ENVIRONMENT]?.status
    );

    const workflowEditState = useWorkspaceScopeState(currentWorkspaceId, 'WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);

    if (workflowEditState.granted || workflowEditState.editionUnknown) {
        return false;
    }

    return workflowEditState.error || workspaceScopeStatus === 'loaded';
};
