/* eslint-disable sort-keys */
import {ProjectTagApi, TagModel} from '@/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectTagKeys = {
    projectTags: ['projectTags'] as const,
};

export const useGetProjectTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: ProjectTagKeys.projectTags,
        queryFn: () => new ProjectTagApi().getProjectTags(),
    });
