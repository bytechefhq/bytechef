/* eslint-disable sort-keys */
import {WorkflowApi, WorkflowBasicModel, WorkflowModel} from '@/middleware/automation/configuration';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {useQuery} from '@tanstack/react-query';

export const WorkflowKeys = {
    projectWorkflows: (projectId: number) => [...ProjectKeys.projects, projectId, 'projectWorkflows'],
    workflow: (id: string) => [...WorkflowKeys.workflows, id],
    workflows: ['workflows'] as const,
};

export const useGetProjectWorkflowsQuery = (id: number) =>
    useQuery<WorkflowBasicModel[], Error>({
        queryKey: WorkflowKeys.projectWorkflows(id),
        queryFn: () => new WorkflowApi().getProjectWorkflows({id}),
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
