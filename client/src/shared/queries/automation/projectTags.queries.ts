/* eslint-disable sort-keys */
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectTagApi, Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectTagKeys = {
    projectTags: ['projectTags'] as const,
    workspaceProjectTags: (workspaceId: number) => [...ProjectTagKeys.projectTags, workspaceId] as const,
};

export const useGetProjectTagsQuery = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    return useQuery<Tag[], Error>({
        queryKey: ProjectTagKeys.workspaceProjectTags(currentWorkspaceId!),
        queryFn: () => new ProjectTagApi().getProjectTags({id: currentWorkspaceId!}),
        enabled: currentWorkspaceId !== undefined,
    });
};
