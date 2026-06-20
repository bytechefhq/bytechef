/* eslint-disable sort-keys */
import {ProjectTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectTagKeys = {
    projectTags: (id: number) => ['projectTags', id] as const,
};

export const useGetProjectTagsQuery = (id: number) =>
    useQuery<Tag[], Error>({
        queryKey: ProjectTagKeys.projectTags(id),
        queryFn: () => new ProjectTagApi().getProjectTags({id}),
    });
