import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionsPageRequest,
    Page,
    WorkflowExecution,
    WorkflowExecutionApi,
} from '@/shared/middleware/automation/workflow/execution';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsPageRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number) => [...WorkflowExecutionKeys.workflowExecutions, id],
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
    refetchInterval?: number | false
) =>
    useQuery<WorkflowExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: enabled === undefined ? true : enabled,
        refetchInterval,
    });
