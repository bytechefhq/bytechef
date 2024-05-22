/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';
import {ProjectApi, ProjectModel, type ProjectStatusModel} from 'middleware/automation/configuration';

export const ProjectKeys = {
    filteredProjects: (
        filters:
            | {
                  id?: number;
                  categoryId?: number;
                  projectInstances?: boolean;
                  tagId?: number;
                  status?: ProjectStatusModel;
              }
            | undefined
    ) => [...ProjectKeys.projects, filters],
    project: (id: number) => [...ProjectKeys.projects, id],
    projects: ['projects'] as const,
};

export const useGetProjectQuery = (id: number, initialData?: ProjectModel, enabled?: boolean) =>
    useQuery<ProjectModel, Error>({
        queryKey: ProjectKeys.project(id),
        queryFn: () => new ProjectApi().getProject({id}),
        initialData,
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetProjectsQuery = (filters?: {
    categoryId?: number;
    projectInstances?: boolean;
    tagId?: number;
    status?: ProjectStatusModel;
}) =>
    useQuery<ProjectModel[], Error>({
        queryKey: ProjectKeys.filteredProjects(filters),
        queryFn: () => new ProjectApi().getProjects(filters),
    });

export const useGetWorkspaceProjectsQuery = (filters: {
    id: number;
    categoryId?: number;
    projectInstances?: boolean;
    tagId?: number;
    status?: ProjectStatusModel;
}) =>
    useQuery<ProjectModel[], Error>({
        queryKey: ProjectKeys.filteredProjects(filters),
        queryFn: () => new ProjectApi().getWorkspaceProjects(filters),
    });
