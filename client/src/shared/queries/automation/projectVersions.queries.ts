/* eslint-disable sort-keys */
import {ProjectApi, ProjectVersion, WorkflowApi} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectVersionKeys = {
    projectProjectVersions: (projectId: number) => [...ProjectVersionKeys.projectVersions, 'projects', projectId],
    projectVersion: (id: number) => [...ProjectVersionKeys.projectVersions, id],
    projectVersions: ['projectVersions'] as const,
    projectWorkflowVersions: (projectId: number, workflowUuid: string) => [
        ...ProjectVersionKeys.projectProjectVersions(projectId),
        'workflows',
        workflowUuid,
    ],
};

export const useGetProjectVersionsQuery = (projectId: number, enabled?: boolean) =>
    useQuery<ProjectVersion[], Error>({
        queryKey: ProjectVersionKeys.projectProjectVersions(projectId),
        queryFn: () => new ProjectApi().getProjectVersions({id: projectId}),
        enabled: enabled ?? true,
    });

export const useGetProjectWorkflowVersionsQuery = (projectId: number, workflowUuid: string, enabled?: boolean) =>
    useQuery<ProjectVersion[], Error>({
        queryKey: ProjectVersionKeys.projectWorkflowVersions(projectId, workflowUuid),
        queryFn: () => new WorkflowApi().getProjectWorkflowVersions({id: projectId, workflowUuid}),
        enabled: enabled ?? true,
    });
