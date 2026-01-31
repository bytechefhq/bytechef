import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionsPageRequest,
    Page,
    WorkflowExecution,
    WorkflowExecutionApi,
} from '@/ee/shared/middleware/embedded/workflow/execution';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowExecutionKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsPageRequest) => [
        ...WorkflowExecutionKeys.workflowExecutions,
        request,
    ],
    workflowExecution: (id: number) => [...WorkflowExecutionKeys.workflowExecutions, id],
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
        queryKey: WorkflowExecutionKeys.workflowExecution(request.id),
        queryFn: () => new WorkflowExecutionApi().getWorkflowExecution(request),
        enabled: isEnabled,
        refetchInterval,
    });
