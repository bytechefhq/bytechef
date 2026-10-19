import {WorkflowNodeDynamicPropertiesApi} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

import type {DynamicPropertiesLoaderType} from './completionTypes';

const DYNAMIC_PROPERTIES_STALE_TIME = 60000;

export function useDynamicPropertiesLoader(
    workflowId: string | undefined,
    environmentId: number | undefined
): DynamicPropertiesLoaderType | undefined {
    const queryClient = useQueryClient();

    return useMemo(() => {
        if (!workflowId || environmentId === undefined) {
            return undefined;
        }

        return ({propertyName, workflowNodeName, ...componentActionDynamicPropertiesRequest}) =>
            queryClient.fetchQuery({
                queryFn: () =>
                    new WorkflowNodeDynamicPropertiesApi().getWorkflowNodeComponentActionDynamicProperties({
                        componentActionDynamicPropertiesRequest,
                        environmentId,
                        id: workflowId,
                        propertyName,
                        workflowNodeName,
                    }),
                queryKey: [
                    ...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
                    'completion',
                    workflowId,
                    workflowNodeName,
                    propertyName,
                    environmentId,
                    componentActionDynamicPropertiesRequest,
                ],
                staleTime: DYNAMIC_PROPERTIES_STALE_TIME,
            });
    }, [environmentId, queryClient, workflowId]);
}
