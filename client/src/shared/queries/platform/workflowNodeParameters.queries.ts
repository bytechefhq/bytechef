import {
    type GetWorkflowNodeParameterDisplayConditions200ResponseModel,
    GetWorkflowNodeParameterDisplayConditionsRequest,
    WorkflowNodeParameterApi,
} from '@/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */

import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeParameterKeys = {
    propertyWorkflowNodeParameterDisplayConditions: (request: GetWorkflowNodeParameterDisplayConditionsRequest) => [
        ...WorkflowNodeParameterKeys.workflowNodeParameters,
        request.id,
        request.workflowNodeName,
        'displayConditions',
    ],
    workflowNodeParameters: ['workflowNodeParameters'] as const,
};

export const useGetWorkflowNodeParameterDisplayConditionsQuery = (
    request: GetWorkflowNodeParameterDisplayConditionsRequest,
    enabled?: boolean
) =>
    useQuery<GetWorkflowNodeParameterDisplayConditions200ResponseModel, Error>({
        queryKey: WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions(request),
        queryFn: () => new WorkflowNodeParameterApi().getWorkflowNodeParameterDisplayConditions(request),
        enabled: enabled === undefined ? true : enabled,
    });
