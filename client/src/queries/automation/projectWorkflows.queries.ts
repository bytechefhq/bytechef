/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowBasicModel, WorkflowModel} from '@/middleware/automation/configuration';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {useQuery} from '@tanstack/react-query';

export const ProjectWorkflowKeys = {
    projectVersionWorkflows: (projectId: number, projectVersion: number) => [
        ...ProjectKeys.projects,
        projectId,
        projectVersion,
        'projectWorkflows',
    ],
    projectWorkflow: (projectId: number, projectWorkflowId: number) => [
        ...ProjectKeys.projects,
        projectId,
        'projectWorkflows',
        projectWorkflowId,
    ],
    projectWorkflows: (projectId: number) => [...ProjectKeys.projects, projectId, 'projectWorkflows'],
};

export const useGetProjectWorkflowQuery = (id: number, projectWorkflowId: number, enabled?: boolean) =>
    useQuery<WorkflowModel, Error>({
        queryKey: ProjectWorkflowKeys.projectWorkflow(id, projectWorkflowId),
        queryFn: () => new WorkflowApi().getProjectWorkflow({projectWorkflowId}),
        enabled: enabled === undefined ? true : enabled,
    });

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
