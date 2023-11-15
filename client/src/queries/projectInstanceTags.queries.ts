/* eslint-disable sort-keys */
import {
    ProjectInstanceTagApi,
    TagModel,
} from '@/middleware/helios/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectInstanceTagKeys = {
    projectInstanceTags: ['projectInstanceTags'] as const,
};

export const useGetProjectInstanceTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: ProjectInstanceTagKeys.projectInstanceTags,
        queryFn: () => new ProjectInstanceTagApi().getProjectInstanceTags(),
    });
