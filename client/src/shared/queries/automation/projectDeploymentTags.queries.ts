/* eslint-disable sort-keys */
import {ProjectDeploymentTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectDeploymentTagKeys = {
    projectDeploymentTags: ['projectDeploymentTags'] as const,
};

export const useGetProjectDeploymentTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: ProjectDeploymentTagKeys.projectDeploymentTags,
        queryFn: () => new ProjectDeploymentTagApi().getProjectDeploymentTags(),
    });
