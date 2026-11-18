import {
    ComponentOperationPropertyRequestOperationTypeEnum,
    WorkflowNodeDynamicPropertiesApi,
    WorkflowNodeOptionApi,
} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

import type {OperationPropertyLoadersType, OperationPropertyRequestType} from './completionTypes';

const OPERATION_PROPERTY_STALE_TIME = 60000;

function toRequestParameters(
    {
        clusterElementNode,
        operationKind,
        propertyName,
        workflowNodeName,
        ...operationRequest
    }: OperationPropertyRequestType,
    workflowId: string,
    environmentId: number
) {
    return {
        componentOperationPropertyRequest: {
            ...operationRequest,
            ...clusterElementNode,
            operationType:
                operationKind === 'trigger'
                    ? ComponentOperationPropertyRequestOperationTypeEnum.Trigger
                    : ComponentOperationPropertyRequestOperationTypeEnum.Action,
        },
        environmentId,
        id: workflowId,
        propertyName,
        workflowNodeName,
    };
}

export function useOperationPropertyLoaders(
    workflowId: string | undefined,
    environmentId: number | undefined
): OperationPropertyLoadersType {
    const queryClient = useQueryClient();

    return useMemo(() => {
        if (!workflowId || environmentId === undefined) {
            return {};
        }

        return {
            loadDynamicProperties: (request) => {
                const requestParameters = toRequestParameters(request, workflowId, environmentId);

                return queryClient.fetchQuery({
                    queryFn: () =>
                        new WorkflowNodeDynamicPropertiesApi().getWorkflowNodeComponentOperationDynamicProperties(
                            requestParameters
                        ),
                    queryKey: [
                        ...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
                        'completion',
                        requestParameters,
                    ],
                    staleTime: OPERATION_PROPERTY_STALE_TIME,
                });
            },
            loadOptions: (request) => {
                const requestParameters = toRequestParameters(request, workflowId, environmentId);

                return queryClient.fetchQuery({
                    queryFn: () =>
                        new WorkflowNodeOptionApi().getWorkflowNodeComponentOperationOptions(requestParameters),
                    queryKey: [...WorkflowNodeOptionKeys.workflowNodeOptions, 'completion', requestParameters],
                    staleTime: OPERATION_PROPERTY_STALE_TIME,
                });
            },
        };
    }, [environmentId, queryClient, workflowId]);
}
