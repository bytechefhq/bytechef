/* eslint-disable sort-keys */
import {ProjectDeploymentTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectDeploymentTagKeys = {
    projectDeploymentTags: (id: number) => ['projectDeploymentTags', id] as const,
};

export const useGetProjectDeploymentTagsQuery = (id: number) =>
    useQuery<Tag[], Error>({
        queryKey: ProjectDeploymentTagKeys.projectDeploymentTags(id),
        queryFn: () => new ProjectDeploymentTagApi().getProjectDeploymentTags({id}),
    });
