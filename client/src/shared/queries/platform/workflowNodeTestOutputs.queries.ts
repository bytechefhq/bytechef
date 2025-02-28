import {
    type CheckWorkflowNodeTestOutputExists200Response,
    CheckWorkflowNodeTestOutputExistsRequest,
    WorkflowNodeTestOutputApi,
} from '@/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeTestOutputKeys = {
    workflowNodeTestOutputExists: (request: CheckWorkflowNodeTestOutputExistsRequest) => [
        ...WorkflowNodeTestOutputKeys.workflowNodeTestOutputs,
        request.id,
        request.workflowNodeName,
    ],
    workflowNodeTestOutputs: ['workflowNodeTestOutputs'] as const,
};

export const useCheckWorkflowNodeTestOutputExistsQuery = (request: CheckWorkflowNodeTestOutputExistsRequest) =>
    useQuery<CheckWorkflowNodeTestOutputExists200Response, Error>({
        queryKey: WorkflowNodeTestOutputKeys.workflowNodeTestOutputExists(request),
        queryFn: () => new WorkflowNodeTestOutputApi().checkWorkflowNodeTestOutputExists(request),
    });
