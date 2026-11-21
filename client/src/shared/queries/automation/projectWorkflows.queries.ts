/* eslint-disable sort-keys */
import {ResponseError, Workflow, WorkflowApi} from '@/shared/middleware/automation/configuration';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {UseQueryOptions, useQuery} from '@tanstack/react-query';

const MAX_PROJECT_WORKFLOW_RETRIES = 3;

const TRANSIENT_CLIENT_ERROR_STATUSES = [408, 429];

const retryUnlessClientError = (failureCount: number, error: Error) => {
    if (error instanceof ResponseError) {
        const {status} = error.response;

        if (status >= 400 && status < 500 && !TRANSIENT_CLIENT_ERROR_STATUSES.includes(status)) {
            return false;
        }
    }

    return failureCount < MAX_PROJECT_WORKFLOW_RETRIES;
};

export const ProjectWorkflowKeys = {
    projectVersionWorkflows: (projectId: number, projectVersion: number, includeAllFields: boolean = true) => [
        ...ProjectKeys.projects,
        projectId,
        projectVersion,
        includeAllFields,
        'projectWorkflows',
    ],
    projectWorkflow: (projectId: number, projectWorkflowId: number) => [
        ...ProjectKeys.projects,
        projectId,
        'projectWorkflows',
        projectWorkflowId,
    ],
    projectWorkflows: (projectId: number) => [...ProjectKeys.projects, projectId, 'projectWorkflows'],
    workflows: [...ProjectKeys.projects, 'workflows'],
};

export const useGetProjectWorkflowQuery = (
    id: number,
    projectWorkflowId: number,
    enabled?: boolean,
    placeholderData?: Workflow,
    options?: Pick<UseQueryOptions<Workflow, Error>, 'refetchOnWindowFocus' | 'retry' | 'staleTime'>
) =>
    useQuery<Workflow, Error>({
        queryKey: ProjectWorkflowKeys.projectWorkflow(id, projectWorkflowId),
        queryFn: () => new WorkflowApi().getProjectWorkflow({projectWorkflowId}),
        enabled: enabled === undefined ? true : enabled,
        placeholderData,
        retry: retryUnlessClientError,
        ...options,
    });

export const useGetProjectWorkflowsQuery = (id: number, enabled?: boolean) =>
    useQuery<Workflow[], Error>({
        queryKey: ProjectWorkflowKeys.projectWorkflows(id),
        queryFn: () => new WorkflowApi().getProjectWorkflows({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetProjectVersionWorkflowsQuery = (
    id: number,
    projectVersion: number,
    includeAllFields: boolean = true,
    enabled?: boolean,
    refetchOnWindowFocus?: boolean
) =>
    useQuery<Workflow[], Error>({
        queryKey: ProjectWorkflowKeys.projectVersionWorkflows(id, projectVersion, includeAllFields),
        queryFn: () => new WorkflowApi().getProjectVersionWorkflows({id, projectVersion, includeAllFields}),
        enabled: enabled === undefined ? true : enabled,
        refetchOnWindowFocus,
    });

export const useGetWorkflowsQuery = (enabled?: boolean) =>
    useQuery<Workflow[], Error>({
        queryKey: ProjectWorkflowKeys.workflows,
        queryFn: () => new WorkflowApi().getWorkflows(),
        enabled: enabled === undefined ? true : enabled,
    });
