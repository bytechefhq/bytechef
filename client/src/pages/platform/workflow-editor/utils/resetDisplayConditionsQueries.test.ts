import {WorkflowNodeParameterKeys} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {QueryClient} from '@tanstack/react-query';
import {describe, expect, it} from 'vitest';

import resetDisplayConditionsQueries from './resetDisplayConditionsQueries';

describe('resetDisplayConditionsQueries', () => {
    it('should drop cached display conditions of the given workflow only', async () => {
        const queryClient = new QueryClient();

        const nodeKey = WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions({
            environmentId: 1,
            id: 'workflow-1',
            workflowNodeName: 'mistral_1',
        });
        const clusterKey = WorkflowNodeParameterKeys.propertyClusterElementParameterDisplayConditions({
            clusterElementType: 'model',
            clusterElementWorkflowNodeName: 'mistral_1',
            environmentId: 1,
            id: 'workflow-1',
            workflowNodeName: 'aiAgent_1',
        });
        const otherWorkflowKey = WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions({
            environmentId: 1,
            id: 'workflow-2',
            workflowNodeName: 'mistral_1',
        });

        queryClient.setQueryData(nodeKey, {displayConditions: {"purpose == 'ocr'": true}});
        queryClient.setQueryData(clusterKey, {displayConditions: {"purpose == 'ocr'": true}});
        queryClient.setQueryData(otherWorkflowKey, {displayConditions: {"purpose == 'ocr'": true}});

        resetDisplayConditionsQueries(queryClient, 'workflow-1');

        expect(queryClient.getQueryData(nodeKey)).toBeUndefined();
        expect(queryClient.getQueryData(clusterKey)).toBeUndefined();
        expect(queryClient.getQueryData(otherWorkflowKey)).toEqual({displayConditions: {"purpose == 'ocr'": true}});
    });
});
