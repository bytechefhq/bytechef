import {
    type GetClusterElementParameterDisplayConditions200Response,
    GetClusterElementParameterDisplayConditionsRequest,
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
        request.environmentId,
    ],
    workflowNodeParameters: ['workflowNodeParameters'] as const,

    propertyClusterElementParameterDisplayConditions: (request: GetClusterElementParameterDisplayConditionsRequest) => [
        ...WorkflowNodeParameterKeys.clusterElementParameters,
        request.id,
        request.workflowNodeName,
        request.clusterElementType,
        request.clusterElementWorkflowNodeName,
    ],
    clusterElementParameters: ['clusterElementParameters'] as const,
};

export const useGetWorkflowNodeParameterDisplayConditionsQuery = (
    request: GetWorkflowNodeParameterDisplayConditionsRequest,
    enabled?: boolean
) =>
    useQuery<GetClusterElementParameterDisplayConditions200Response, Error>({
        queryKey: WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions(request),
        queryFn: () => new WorkflowNodeParameterApi().getWorkflowNodeParameterDisplayConditions(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetClusterElementParameterDisplayConditionsQuery = (
    request: GetClusterElementParameterDisplayConditionsRequest,
    enabled?: boolean
) =>
    useQuery<GetClusterElementParameterDisplayConditions200Response, Error>({
        queryKey: WorkflowNodeParameterKeys.propertyClusterElementParameterDisplayConditions(request),
        queryFn: () => new WorkflowNodeParameterApi().getClusterElementParameterDisplayConditions(request),
        enabled: enabled === undefined ? true : enabled,
    });
