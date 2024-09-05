/* eslint-disable sort-keys */
import {ProjectTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectTagKeys = {
    projectTags: ['projectTags'] as const,
};

export const useGetProjectTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: ProjectTagKeys.projectTags,
        queryFn: () => new ProjectTagApi().getProjectTags(),
    });
