import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionTaskExecutionRequest,
    GetWorkflowExecutionsPageRequest,
    Page,
    TaskExecution,
    TaskExecutionChildrenPage,
    WorkflowExecution,
    WorkflowExecutionApi,
} from '@/shared/middleware/automation/workflow/execution';

/* eslint-disable sort-keys */
import {useInfiniteQuery, useQuery} from '@tanstack/react-query';

/**
 * A row of the executions list is a job, or a trigger execution that failed before any job existed; the detail is
 * fetched from a different endpoint for each.
 */
export type WorkflowExecutionKindType = 'JOB' | 'TRIGGER_EXECUTION';

export const TASK_EXECUTION_CHILDREN_MAX_PAGES = 10;

export const TASK_EXECUTION_CHILDREN_PAGE_SIZE = 50;

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsPageRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number, kind: WorkflowExecutionKindType = 'JOB', lazy: boolean = false) => {
        const baseKey =
            kind === 'JOB'
                ? [...WorkflowExecutionKeys.workflowExecutions, id]
                : [...WorkflowExecutionKeys.workflowExecutions, kind, id];

        return lazy ? [...baseKey, 'lazy'] : baseKey;
    },
    workflowExecutionTaskExecutionChildren: (id: number, taskExecutionId: number, initialPageNumber: number = 0) => [
        ...WorkflowExecutionKeys.workflowExecutionTaskExecutionChildrenOfJob(id),
        taskExecutionId,
        initialPageNumber,
    ],
    workflowExecutionTaskExecutionChildrenOfJob: (id: number) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        id,
        'taskExecutionChildren',
    ],
    workflowExecutionTaskExecution: (id: number, taskExecutionId: number) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        id,
        'taskExecution',
        taskExecutionId,
    ],
    workflowExecutions: ['automation_workflowExecutions'] as const,
};

const RUNNING_WORKFLOW_EXECUTIONS_POLLING_INTERVAL_MS = 2000;

const RUNNING_JOB_STATUSES = new Set(['CREATED', 'STARTED']);

export function getWorkflowExecutionsRefetchInterval(page: Page | undefined): number | false {
    const hasRunningJob = (page?.content ?? []).some((workflowExecution) =>
        RUNNING_JOB_STATUSES.has((workflowExecution as {job?: {status?: string}}).job?.status ?? '')
    );

    return hasRunningJob ? RUNNING_WORKFLOW_EXECUTIONS_POLLING_INTERVAL_MS : false;
}

export const useGetWorkspaceProjectWorkflowExecutionsQuery = (
    request: GetWorkflowExecutionsPageRequest,
    enabled?: boolean
) =>
    useQuery<Page, Error>({
        queryKey: WorkflowExecutionKeys.filteredWorkflowExecutions(request),
        queryFn: () =>
            new WorkflowExecutionApi().getWorkflowExecutionsPage({
                ...request,
                embedded: false,
            }),
        enabled: enabled === undefined ? true : enabled,
        refetchInterval: (query) => getWorkflowExecutionsRefetchInterval(query.state.data),
    });

export const useGetProjectWorkflowExecutionQuery = (
    request: GetWorkflowExecutionRequest,
    enabled?: boolean,
    refetchInterval?: number | false,
    kind: WorkflowExecutionKindType = 'JOB'
) =>
    useQuery<WorkflowExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id, kind, request.lazy === true),
        queryFn: () =>
            kind === 'TRIGGER_EXECUTION'
                ? new WorkflowExecutionApi().getTriggerExecutionWorkflowExecution({triggerExecutionId: request.id})
                : new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: enabled === undefined ? true : enabled,
        refetchInterval,
    });

export const useGetWorkflowExecutionTaskExecutionQuery = (
    request: GetWorkflowExecutionTaskExecutionRequest,
    enabled: boolean,
    refetchInterval?: number | false,
    retry?: boolean
) =>
    useQuery<TaskExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecutionTaskExecution(request.id, request.taskExecutionId),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecutionTaskExecution(request),
        enabled,
        refetchInterval,
        retry,
    });

export const useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery = (
    request: {id: number; taskExecutionId: number},
    enabled: boolean,
    refetchInterval?: number | false,
    initialPageNumber: number = 0
) =>
    useInfiniteQuery<TaskExecutionChildrenPage, Error>({
        getNextPageParam: (page) => (page.number + 1 < page.totalPages ? page.number + 1 : undefined),
        getPreviousPageParam: (page) => (page.number > 0 ? page.number - 1 : undefined),
        initialPageParam: initialPageNumber,
        maxPages: TASK_EXECUTION_CHILDREN_MAX_PAGES,
        queryFn: ({pageParam}) =>
            new WorkflowExecutionApi().getWorkflowExecutionTaskExecutionChildren({
                id: request.id,
                pageNumber: pageParam as number,
                pageSize: TASK_EXECUTION_CHILDREN_PAGE_SIZE,
                taskExecutionId: request.taskExecutionId,
            }),
        queryKey: WorkflowExecutionKeys.workflowExecutionTaskExecutionChildren(
            request.id,
            request.taskExecutionId,
            initialPageNumber
        ),
        enabled,
        refetchInterval,
    });
