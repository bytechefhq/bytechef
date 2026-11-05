/* eslint-disable sort-keys */
import {
    GetProjectDeploymentTagsRequest,
    ProjectDeploymentTagApi,
    Tag,
} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectDeploymentTagKeys = {
    projectDeploymentTags: ['projectDeploymentTags'] as const,
    workspaceProjectDeploymentTags: (request: GetProjectDeploymentTagsRequest) => [
        ...ProjectDeploymentTagKeys.projectDeploymentTags,
        request,
    ],
};

export const useGetProjectDeploymentTagsQuery = (request: GetProjectDeploymentTagsRequest) =>
    useQuery<Tag[], Error>({
        queryKey: ProjectDeploymentTagKeys.workspaceProjectDeploymentTags(request),
        queryFn: () => new ProjectDeploymentTagApi().getProjectDeploymentTags(request),
    });
