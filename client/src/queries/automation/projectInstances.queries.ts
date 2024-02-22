/* eslint-disable sort-keys */
import {ProjectInstanceApi, ProjectInstanceModel} from '@/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectInstanceKeys = {
    filteredProjectInstances: (filters: {projectId?: number; tagId?: number}) => [
        ...ProjectInstanceKeys.projectInstances,
        filters,
    ],
    projectInstances: ['projectInstances'] as const,
};

export const useGetProjectInstancesQuery = (request: {projectId?: number; tagId?: number}) =>
    useQuery<ProjectInstanceModel[], Error>({
        queryKey: ProjectInstanceKeys.filteredProjectInstances(request),
        queryFn: () => new ProjectInstanceApi().getProjectInstances(request),
    });
