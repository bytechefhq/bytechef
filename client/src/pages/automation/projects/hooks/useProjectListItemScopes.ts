import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';

export const useProjectListItemScopes = (workspaceId: number | undefined) => {
    const canConfigureProjectGit = useHasWorkspaceScope(workspaceId, 'WORKSPACE_MANAGE');
    const canCreateDeployment = useHasWorkspaceScope(workspaceId, 'DEPLOYMENT_CREATE');
    const canCreateProject = useHasWorkspaceScope(workspaceId, 'PROJECT_CREATE');
    const canCreateWorkflow = useHasWorkspaceScope(workspaceId, 'WORKFLOW_CREATE');
    const canDeleteProject = useHasWorkspaceScope(workspaceId, 'PROJECT_DELETE');
    const canEditProject = useHasWorkspaceScope(workspaceId, 'WORKFLOW_EDIT');
    const canManageProjectSettings = useHasWorkspaceScope(workspaceId, 'PROJECT_SETTINGS');
    const canPublish = useHasWorkspaceScope(workspaceId, 'PROJECT_PUBLISH');
    const canPullProjectFromGit = useHasWorkspaceScope(workspaceId, 'PROJECT_PULL');

    return {
        canConfigureProjectGit,
        canCreateProject,
        canCreateWorkflow,
        canDeleteProject,
        canDeployProject: canCreateDeployment && canEditProject,
        canEditProject,
        canManageProjectGit: canPullProjectFromGit || canConfigureProjectGit,
        canManageProjectSettings,
        canPublishProject: canPublish && canEditProject,
        canPullProjectFromGit,
    };
};
