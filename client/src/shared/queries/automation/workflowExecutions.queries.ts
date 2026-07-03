import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionTaskExecutionRequest,
    GetWorkflowExecutionsPageRequest,
    Page,
    TaskExecution,
    WorkflowExecution,
    WorkflowExecutionApi,
} from '@/shared/middleware/automation/workflow/execution';

/* eslint-disable sort-keys */
import {useInfiniteQuery, useQuery} from '@tanstack/react-query';

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsPageRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number) => [...WorkflowExecutionKeys.workflowExecutions, id],
    workflowExecutionTaskExecution: (id: number, taskExecutionId: number) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        id,
        'taskExecution',
        taskExecutionId,
    ],
    workflowExecutions: ['automation_workflowExecutions'] as const,
};

export const useGetWorkspaceProjectWorkflowExecutionsQuery = (request: GetWorkflowExecutionsPageRequest) =>
    useQuery<Page, Error>({
        queryKey: WorkflowExecutionKeys.filteredWorkflowExecutions(request),
        queryFn: () =>
            new WorkflowExecutionApi().getWorkflowExecutionsPage({
                ...request,
                embedded: false,
            }),
    });

/**
 * Compute the next page number for the executions infinite query, or `undefined` when the last page
 * has been loaded. The backend page size is fixed (20); paging is driven purely by page number.
 */
export function getNextWorkflowExecutionsPageParam(lastPage: Page): number | undefined {
    const current = lastPage.number ?? 0;
    const totalPages = lastPage.totalPages ?? 0;

    return current + 1 < totalPages ? current + 1 : undefined;
}

/**
 * Accumulating executions query: each `fetchNextPage()` loads the next server page (page size 20) and
 * appends it, so the picker can "Show more" across the full result set instead of seeing only page 0.
 * Keyed on the base request (sans pageNumber).
 */
export const useInfiniteWorkspaceProjectWorkflowExecutionsQuery = (
    request: GetWorkflowExecutionsPageRequest,
    enabled?: boolean
) =>
    useInfiniteQuery<Page, Error>({
        queryKey: [...WorkflowExecutionKeys.workflowExecutions, 'infinite', request],
        queryFn: ({pageParam}) =>
            new WorkflowExecutionApi().getWorkflowExecutionsPage({
                ...request,
                embedded: false,
                pageNumber: pageParam as number,
            }),
        initialPageParam: 0,
        getNextPageParam: getNextWorkflowExecutionsPageParam,
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetProjectWorkflowExecutionQuery = (
    request: GetWorkflowExecutionRequest,
    enabled?: boolean,
    refetchInterval?: number | false
) =>
    useQuery<WorkflowExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: enabled === undefined ? true : enabled,
        refetchInterval,
    });

export const useGetWorkflowExecutionTaskExecutionQuery = (
    request: GetWorkflowExecutionTaskExecutionRequest,
    enabled: boolean,
    refetchInterval?: number | false
) =>
    useQuery<TaskExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecutionTaskExecution(request.id, request.taskExecutionId),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecutionTaskExecution(request),
        enabled,
        refetchInterval,
    });
