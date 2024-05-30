import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionsPageRequest,
    PageModel,
    WorkflowExecutionApi,
    WorkflowExecutionModel,
} from '@/shared/middleware/automation/workflow/execution';

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
    useQuery<PageModel, Error>({
        queryKey: WorkflowExecutionKeys.filteredWorkflowExecutions(request),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecutionsPage(request),
    });

export const useGetWorkflowExecutionQuery = (request: GetWorkflowExecutionRequest, enabled?: boolean) =>
    useQuery<WorkflowExecutionModel, Error>({
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: enabled === undefined ? true : enabled,
    });
