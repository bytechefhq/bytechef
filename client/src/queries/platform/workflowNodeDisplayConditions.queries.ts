/* eslint-disable sort-keys */
import {EvaluateWorkflowNodeDisplayConditionRequest, WorkflowNodeApi} from '@/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeDisplayConditionKeys = {
    propertyWorkflowNodeDisplayConditions: (request: EvaluateWorkflowNodeDisplayConditionRequest) => [
        ...WorkflowNodeDisplayConditionKeys.workflowNodeDisplayConditions,
        request.id,
        request.workflowNodeName,
        request.evaluateWorkflowNodeDisplayConditionRequestModel?.displayCondition,
    ],
    workflowNodeDisplayConditions: ['workflowNodeDisplayConditions'] as const,
};

export const useEvaluateWorkflowNodeDisplayConditionQuery = (
    request: EvaluateWorkflowNodeDisplayConditionRequest,
    enabled?: boolean
) =>
    useQuery<boolean, Error>({
        queryKey: WorkflowNodeDisplayConditionKeys.propertyWorkflowNodeDisplayConditions(request),
        queryFn: () => new WorkflowNodeApi().evaluateWorkflowNodeDisplayCondition(request),
        enabled: enabled === undefined ? true : enabled,
    });
