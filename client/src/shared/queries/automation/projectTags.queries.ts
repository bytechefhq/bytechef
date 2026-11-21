/* eslint-disable sort-keys */
import {ProjectTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectTagKeys = {
    projectTags: ['projectTags'] as const,
    workspaceProjectTags: (workspaceId: number) => [...ProjectTagKeys.projectTags, workspaceId] as const,
};

export const useGetProjectTagsQuery = (workspaceId: number) =>
    useQuery<Tag[], Error>({
        queryKey: ProjectTagKeys.workspaceProjectTags(workspaceId),
        queryFn: () => new ProjectTagApi().getProjectTags({id: workspaceId}),
        enabled: workspaceId !== undefined,
    });
