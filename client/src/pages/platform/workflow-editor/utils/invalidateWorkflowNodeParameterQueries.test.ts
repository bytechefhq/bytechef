import {WorkflowNodeDescriptionKeys} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {WorkflowNodeParameterKeys} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {QueryClient} from '@tanstack/react-query';
import {describe, expect, it, vi} from 'vitest';

import invalidateWorkflowNodeParameterQueries from './invalidateWorkflowNodeParameterQueries';

describe('invalidateWorkflowNodeParameterQueries', () => {
    it("should invalidate the node's description and display conditions and all node outputs", () => {
        const invalidateQueries = vi.fn();
        const workflowNodeRequest = {environmentId: 2, id: 'workflow-1', workflowNodeName: 'slack_1'};

        invalidateWorkflowNodeParameterQueries({invalidateQueries} as unknown as QueryClient, workflowNodeRequest);

        expect(invalidateQueries.mock.calls.map(([{queryKey}]) => queryKey)).toEqual([
            WorkflowNodeDescriptionKeys.workflowNodeDescription(workflowNodeRequest),
            WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions(workflowNodeRequest),
            WorkflowNodeOutputKeys.workflowNodeOutputs,
        ]);
    });
});
