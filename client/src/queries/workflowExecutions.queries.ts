import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionsRequest,
    PageModel,
    WorkflowExecutionApi,
    WorkflowExecutionModel,
} from '@/middleware/helios/execution';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        id,
    ],
    workflowExecutions: ['workflowExecutions'] as const,
};

export const useGetWorkflowExecutionsQuery = (
    request: GetWorkflowExecutionsRequest
) =>
    useQuery<PageModel, Error>({
        queryKey: WorkflowExecutionKeys.filteredWorkflowExecutions(request),
        queryFn: () =>
            new WorkflowExecutionApi().getWorkflowExecutions(request),
    });

export const useGetWorkflowExecutionQuery = (
    request: GetWorkflowExecutionRequest,
    isEnabled: boolean
) =>
    useQuery<WorkflowExecutionModel, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: isEnabled,
    });
