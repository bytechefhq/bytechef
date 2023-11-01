import {
    ProjectInstanceApi,
    ProjectInstanceModel,
    ProjectInstanceTagApi,
    TagModel,
} from '@/middleware/helios/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectInstanceKeys = {
    filteredProjectInstances: (filters: {
        projectId?: number;
        tagId?: number;
    }) => [...ProjectInstanceKeys.projectInstances, filters],
    projectInstanceTags: ['projectInstanceTags'] as const,
    projectInstances: ['projectInstances'] as const,
};

export const useGetProjectInstancesQuery = (filters: {
    projectId?: number;
    tagId?: number;
}) =>
    useQuery<ProjectInstanceModel[], Error>(
        ProjectInstanceKeys.filteredProjectInstances(filters),
        () => new ProjectInstanceApi().getProjectInstances(filters)
    );

export const useGetProjectInstanceTagsQuery = () =>
    useQuery<TagModel[], Error>(ProjectInstanceKeys.projectInstanceTags, () =>
        new ProjectInstanceTagApi().getProjectInstanceTags()
    );
