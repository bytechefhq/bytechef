/* eslint-disable sort-keys */
import {ProjectInstanceTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectInstanceTagKeys = {
    projectInstanceTags: ['projectInstanceTags'] as const,
};

export const useGetProjectInstanceTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: ProjectInstanceTagKeys.projectInstanceTags,
        queryFn: () => new ProjectInstanceTagApi().getProjectInstanceTags(),
    });
