import {
    ClusterElementDynamicPropertyKeys,
    WorkflowNodeDynamicPropertyKeys,
} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {QueryClient} from '@tanstack/react-query';
import {describe, expect, it, vi} from 'vitest';

import invalidateOperationQueries from './invalidateOperationQueries';

describe('invalidateOperationQueries', () => {
    it('should invalidate the dynamic properties and options of both plain nodes and cluster elements', () => {
        const invalidateQueries = vi.fn();

        invalidateOperationQueries({invalidateQueries} as unknown as QueryClient);

        expect(invalidateQueries.mock.calls.map(([{queryKey}]) => queryKey)).toEqual([
            WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
            ClusterElementDynamicPropertyKeys.clusterElementDynamicProperties,
            WorkflowNodeOptionKeys.workflowNodeOptions,
            WorkflowNodeOptionKeys.clusterElementNodeOptions,
        ]);
    });
});
