/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowBasicModel, WorkflowModel} from '@/middleware/automation/configuration';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {useQuery} from '@tanstack/react-query';

export const WorkflowKeys = {
    projectVersionWorkflows: (projectId: number, projectVersion: number) => [
        ...ProjectKeys.projects,
        projectId,
        projectVersion,
        'projectWorkflows',
    ],
    projectWorkflows: (projectId: number) => [...ProjectKeys.projects, projectId, 'projectWorkflows'],
    workflow: (id: string) => [...WorkflowKeys.workflows, id],
    workflows: ['workflows'] as const,
};

export const useGetProjectWorkflowsQuery = (id: number, enabled?: boolean) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: WorkflowKeys.projectWorkflows(id),
        queryFn: () => new WorkflowApi().getProjectWorkflows({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetProjectVersionWorkflowsQuery = (id: number, projectVersion: number, enabled?: boolean) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: WorkflowKeys.projectVersionWorkflows(id, projectVersion),
        queryFn: () => new WorkflowApi().getProjectVersionWorkflows({id, projectVersion}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkflowQuery = (id: string, enabled?: boolean) =>
    useQuery<WorkflowModel, Error>({
        queryKey: WorkflowKeys.workflow(id),
        queryFn: () => new WorkflowApi().getWorkflow({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: WorkflowKeys.workflows,
        queryFn: () => new WorkflowApi().getWorkflows(),
    });
