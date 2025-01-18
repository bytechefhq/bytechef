import {GetWorkspaceProjectsRequest, Project, ProjectApi} from '@/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ProjectKeys = {
    filteredProjects: (filters: GetWorkspaceProjectsRequest) => [...ProjectKeys.projects, filters.id, filters],
    project: (id: number) => [...ProjectKeys.projects, id],
    projects: ['projects'] as const,
};

export const useGetProjectQuery = (id: number, initialData?: Project, enabled?: boolean) =>
    useQuery<Project, Error>({
        queryKey: ProjectKeys.project(id),
        queryFn: () => new ProjectApi().getProject({id}),
        initialData,
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkspaceProjectsQuery = (filters: GetWorkspaceProjectsRequest) =>
    useQuery<Project[], Error>({
        queryKey: ProjectKeys.filteredProjects(filters),
        queryFn: () => new ProjectApi().getWorkspaceProjects(filters),
    });
