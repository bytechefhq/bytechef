import {
    PageModel,
    WorkflowApi,
    WorkflowModel,
} from '@/middleware/helios/configuration';
import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionsRequest,
    WorkflowExecutionApi,
    WorkflowExecutionModel,
} from '@/middleware/helios/execution';
import {useQuery} from '@tanstack/react-query';

export const WorkflowKeys = {
    filteredWorkflowExecutions: (request: GetWorkflowExecutionsRequest) => [
        'workflowExecutions',
        request,
    ],
    workflow: (id: number) => ['workflow', id],
    workflowExecution: (id: number) => ['workflowExecutions', id],
    workflows: ['workflows'] as const,
};

export const useGetWorkflowExecutionsQuery = (
    request: GetWorkflowExecutionsRequest
) =>
    useQuery<PageModel, Error>(
        WorkflowKeys.filteredWorkflowExecutions(request),
        () => new WorkflowExecutionApi().getWorkflowExecutions(request)
    );

export const useGetWorkflowExecutionQuery = (
    request: GetWorkflowExecutionRequest,
    isEnabled: boolean
) =>
    useQuery<WorkflowExecutionModel, Error>(
        WorkflowKeys.workflowExecution(request.id),
        () => new WorkflowExecutionApi().getWorkflowExecution(request),
        {
            enabled: isEnabled,
        }
    );

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowModel[], Error>(WorkflowKeys.workflows, () =>
        new WorkflowApi().getWorkflows()
    );
