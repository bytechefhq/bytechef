import {
    type GetWorkflowNodeParameterDisplayConditions200ResponseModel,
    GetWorkflowNodeParameterDisplayConditionsRequest,
    WorkflowNodeParameterApi,
} from '@/middleware/platform/configuration';

/* eslint-disable sort-keys */

import {useQuery} from '@tanstack/react-query';

export const WorkflowNodeParameterKeys = {
    propertyWorkflowNodeOptions: (request: GetWorkflowNodeParameterDisplayConditionsRequest) => [
        ...WorkflowNodeParameterKeys.workflowNodeParameters,
        request,
        'displayConditions',
    ],
    workflowNodeParameters: ['workflowNodeParameters'] as const,
};

export const useGetWorkflowNodeParameterDisplayConditionsQuery = (
    request: GetWorkflowNodeParameterDisplayConditionsRequest,
    enabled?: boolean
) =>
    useQuery<GetWorkflowNodeParameterDisplayConditions200ResponseModel, Error>({
        queryKey: WorkflowNodeParameterKeys.propertyWorkflowNodeOptions(request),
        queryFn: () => new WorkflowNodeParameterApi().getWorkflowNodeParameterDisplayConditions(request),
        enabled: enabled === undefined ? true : enabled,
    });
