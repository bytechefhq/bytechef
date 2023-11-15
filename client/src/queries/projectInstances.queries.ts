/* eslint-disable sort-keys */
import {
    ProjectInstanceApi,
    ProjectInstanceModel,
} from '@/middleware/helios/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectInstanceKeys = {
    filteredProjectInstances: (filters: {
        projectId?: number;
        tagId?: number;
    }) => [...ProjectInstanceKeys.projectInstances, filters],
    projectInstances: ['projectInstances'] as const,
};

export const useGetProjectInstancesQuery = (filters: {
    projectId?: number;
    tagId?: number;
}) =>
    useQuery<ProjectInstanceModel[], Error>({
        queryKey: ProjectInstanceKeys.filteredProjectInstances(filters),
        queryFn: () => new ProjectInstanceApi().getProjectInstances(filters),
    });
