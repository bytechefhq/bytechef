import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionsPageRequest,
    Page,
    WorkflowExecution,
    WorkflowExecutionApi,
} from '@/shared/middleware/embedded/workflow/execution';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsPageRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number) => [...WorkflowExecutionKeys.workflowExecutions, id],
    workflowExecutions: ['workflowExecutions'] as const,
};

export const useGetWorkflowExecutionsQuery = (request: GetWorkflowExecutionsPageRequest) =>
    useQuery<Page, Error>({
        queryKey: WorkflowExecutionKeys.filteredWorkflowExecutions(request),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecutionsPage(request),
    });

export const useGetWorkflowExecutionQuery = (request: GetWorkflowExecutionRequest, isEnabled: boolean) =>
    useQuery<WorkflowExecution, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: isEnabled,
    });
