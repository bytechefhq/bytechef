import {Project, ProjectApi, type ProjectStatus} from '@/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ProjectKeys = {
    filteredProjects: (
        filters:
            | {
                  id?: number;
                  categoryId?: number;
                  projectInstances?: boolean;
                  tagId?: number;
                  status?: ProjectStatus;
              }
            | undefined
    ) => [...ProjectKeys.projects, filters],
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

export const useGetWorkspaceProjectsQuery = (filters: {
    id: number;
    categoryId?: number;
    projectInstances?: boolean;
    tagId?: number;
    status?: ProjectStatus;
}) =>
    useQuery<Project[], Error>({
        queryKey: ProjectKeys.filteredProjects(filters),
        queryFn: () => new ProjectApi().getWorkspaceProjects(filters),
    });
