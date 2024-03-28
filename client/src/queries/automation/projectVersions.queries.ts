/* eslint-disable sort-keys */
import {ProjectApi, ProjectVersionModel} from '@/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectVersionKeys = {
    projectProjectVersions: (projectId: number) => [...ProjectVersionKeys.projectVersions, 'projects', projectId],
    projectVersion: (id: number) => [...ProjectVersionKeys.projectVersions, id],
    projectVersions: ['projectVersions'] as const,
};

export const useGetProjectVersionsQuery = (projectId: number, enabled?: boolean) =>
    useQuery<ProjectVersionModel[], Error>({
        queryKey: ProjectVersionKeys.projectProjectVersions(projectId),
        queryFn: () => new ProjectApi().getProjectVersions({id: projectId}),
        enabled: enabled === undefined ? true : enabled,
    });
