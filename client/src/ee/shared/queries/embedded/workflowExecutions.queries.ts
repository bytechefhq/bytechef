import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionTaskExecutionRequest,
    GetWorkflowExecutionsPageRequest,
    Page,
    TaskExecution,
    TaskExecutionChildrenPage,
    WorkflowExecution,
    WorkflowExecutionApi,
} from '@/ee/shared/middleware/embedded/workflow/execution';
import {
    TASK_EXECUTION_CHILDREN_MAX_PAGES,
    TASK_EXECUTION_CHILDREN_PAGE_SIZE,
} from '@/shared/queries/automation/workflowExecutions.queries';

/* eslint-disable sort-keys */
import {useInfiniteQuery, useQuery} from '@tanstack/react-query';

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsPageRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number, lazy: boolean = false) =>
        lazy
            ? [...WorkflowExecutionKeys.workflowExecutions, id, 'lazy']
            : [...WorkflowExecutionKeys.workflowExecutions, id],
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
    workflowExecutions: ['integrationWorkflowExecutions'] as const,
};

export const useGetIntegrationWorkflowExecutionsQuery = (request: GetWorkflowExecutionsPageRequest) =>
    useQuery<Page, Error>({
        queryKey: WorkflowExecutionKeys.filteredWorkflowExecutions(request),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecutionsPage(request),
    });

export const useGetIntegrationWorkflowExecutionQuery = (
    request: GetWorkflowExecutionRequest,
    isEnabled: boolean,
    refetchInterval?: number | false
) =>
    useQuery<WorkflowExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id, request.lazy === true),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: isEnabled,
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
