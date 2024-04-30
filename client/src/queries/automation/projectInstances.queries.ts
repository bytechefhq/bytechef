/* eslint-disable sort-keys */
import {
    GetProjectInstancesRequest,
    ProjectInstanceApi,
    ProjectInstanceModel,
} from '@/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectInstanceKeys = {
    filteredProjectInstances: (filters: GetProjectInstancesRequest) => [
        ...ProjectInstanceKeys.projectInstances,
        filters,
    ],
    projectInstance: (id: number) => [...ProjectInstanceKeys.projectInstances, id],
    projectInstances: ['projectInstances'] as const,
};

export const useGetProjectInstanceQuery = (id: number, enabled?: boolean) =>
    useQuery<ProjectInstanceModel, Error>({
        queryKey: ProjectInstanceKeys.projectInstance(id),
        queryFn: () => new ProjectInstanceApi().getProjectInstance({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetProjectInstancesQuery = (filters: GetProjectInstancesRequest) =>
    useQuery<ProjectInstanceModel[], Error>({
        queryKey: ProjectInstanceKeys.filteredProjectInstances(filters),
        queryFn: () => new ProjectInstanceApi().getProjectInstances(filters),
    });
