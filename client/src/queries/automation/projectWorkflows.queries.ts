/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowBasicModel} from '@/middleware/automation/configuration';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {useQuery} from '@tanstack/react-query';

export const ProjectWorkflowKeys = {
    projectVersionWorkflows: (projectId: number, projectVersion: number) => [
        ...ProjectKeys.projects,
        projectId,
        projectVersion,
        'projectWorkflows',
    ],
    projectWorkflows: (projectId: number) => [...ProjectKeys.projects, projectId, 'projectWorkflows'],
};

export const useGetProjectWorkflowsQuery = (id: number, enabled?: boolean) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: ProjectWorkflowKeys.projectWorkflows(id),
        queryFn: () => new WorkflowApi().getProjectWorkflows({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetProjectVersionWorkflowsQuery = (id: number, projectVersion: number, enabled?: boolean) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: ProjectWorkflowKeys.projectVersionWorkflows(id, projectVersion),
        queryFn: () => new WorkflowApi().getProjectVersionWorkflows({id, projectVersion}),
        enabled: enabled === undefined ? true : enabled,
    });
