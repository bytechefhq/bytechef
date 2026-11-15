/* eslint-disable sort-keys */
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectDeploymentTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectDeploymentTagKeys = {
    projectDeploymentTags: ['projectDeploymentTags'] as const,
    workspaceProjectDeploymentTags: (workspaceId: number) =>
        [...ProjectDeploymentTagKeys.projectDeploymentTags, workspaceId] as const,
};

export const useGetProjectDeploymentTagsQuery = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    return useQuery<Tag[], Error>({
        queryKey: ProjectDeploymentTagKeys.workspaceProjectDeploymentTags(currentWorkspaceId!),
        queryFn: () => new ProjectDeploymentTagApi().getProjectDeploymentTags({id: currentWorkspaceId!}),
        enabled: currentWorkspaceId !== undefined,
    });
};
