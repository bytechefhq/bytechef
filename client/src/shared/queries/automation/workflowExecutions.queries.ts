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
import {useQuery} from '@tanstack/react-query';

/**
 * A row of the executions list is a job, or a trigger execution that failed before any job existed; the detail is
 * fetched from a different endpoint for each.
 */
export type WorkflowExecutionKindType = 'JOB' | 'TRIGGER_EXECUTION';

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsPageRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number, kind: WorkflowExecutionKindType = 'JOB') =>
        kind === 'JOB'
            ? [...WorkflowExecutionKeys.workflowExecutions, id]
            : [...WorkflowExecutionKeys.workflowExecutions, kind, id],
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

export const useGetProjectWorkflowExecutionQuery = (
    request: GetWorkflowExecutionRequest,
    enabled?: boolean,
    refetchInterval?: number | false,
    kind: WorkflowExecutionKindType = 'JOB'
) =>
    useQuery<WorkflowExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id, kind),
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
    refetchInterval?: number | false
) =>
    useQuery<TaskExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecutionTaskExecution(request.id, request.taskExecutionId),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecutionTaskExecution(request),
        enabled,
        refetchInterval,
    });
