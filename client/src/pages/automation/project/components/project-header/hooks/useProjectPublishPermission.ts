import {getDisabledControlTooltip} from '@/pages/automation/project/components/project-header/util/permission-tooltip-utils';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useWorkspaceScopeState} from '@/shared/hooks/useHasWorkspaceScope';

interface UseProjectPublishPermissionI {
    canPublishProject: boolean;
    disabledTooltip: string;
}

export default function useProjectPublishPermission(): UseProjectPublishPermissionI {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const projectPublishState = useWorkspaceScopeState(currentWorkspaceId, 'PROJECT_PUBLISH');
    const workflowEditState = useWorkspaceScopeState(currentWorkspaceId, 'WORKFLOW_EDIT');

    const canPublishProject = projectPublishState.granted && workflowEditState.granted;

    return {
        canPublishProject,
        disabledTooltip: getDisabledControlTooltip({
            deniedMessage: 'You do not have permission to publish this project',
            granted: canPublishProject,
            permissionsError: projectPublishState.error || workflowEditState.error,
            permissionsLoading: currentWorkspaceId == null || projectPublishState.loading || workflowEditState.loading,
            permissionsUnknown: projectPublishState.editionUnknown || workflowEditState.editionUnknown,
            unmetPreconditionMessage: 'No changes to publish',
        }),
    };
}
