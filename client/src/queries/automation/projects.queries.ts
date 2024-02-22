/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';
import {ProjectApi, ProjectModel} from 'middleware/automation/configuration';

export const ProjectKeys = {
    filteredProjects: (filters: {categoryId?: number; tagId?: number} | undefined) => [
        ...ProjectKeys.projects,
        filters,
    ],
    project: (id: number) => [...ProjectKeys.projects, id],
    projects: ['projects'] as const,
};

export const useGetProjectQuery = (id: number, initialData?: ProjectModel) =>
    useQuery<ProjectModel, Error>({
        queryKey: ProjectKeys.project(id),
        queryFn: () => new ProjectApi().getProject({id}),
        initialData,
    });

export const useGetProjectsQuery = (filters?: {
    categoryId?: number;
    projectInstances?: boolean;
    tagId?: number;
    published?: boolean;
}) =>
    useQuery<ProjectModel[], Error>({
        queryKey: ProjectKeys.filteredProjects(filters),
        queryFn: () => new ProjectApi().getProjects(filters),
    });
