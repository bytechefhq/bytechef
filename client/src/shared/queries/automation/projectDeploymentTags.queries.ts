/* eslint-disable sort-keys */
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectDeploymentTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectDeploymentTagKeys = {
    projectDeploymentTags: ['projectDeploymentTags'] as const,
    workspaceProjectDeploymentTags: (workspaceId: number, environmentId?: number) =>
        environmentId === undefined
            ? ([...ProjectDeploymentTagKeys.projectDeploymentTags, workspaceId] as const)
            : ([...ProjectDeploymentTagKeys.projectDeploymentTags, workspaceId, environmentId] as const),
};

export const useGetProjectDeploymentTagsQuery = (environmentId?: number) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    return useQuery<Tag[], Error>({
        queryKey: ProjectDeploymentTagKeys.workspaceProjectDeploymentTags(currentWorkspaceId!, environmentId),
        queryFn: () => new ProjectDeploymentTagApi().getProjectDeploymentTags({environmentId, id: currentWorkspaceId!}),
        enabled: currentWorkspaceId !== undefined,
    });
};
