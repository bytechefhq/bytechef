import {describe, expect, it} from 'vitest';

import {WorkflowNodeDescriptionKeys} from './workflowNodeDescriptions.queries';

describe('WorkflowNodeDescriptionKeys', () => {
    it('should generate a unique query key per workflow, node, and environment', () => {
        const key = WorkflowNodeDescriptionKeys.workflowNodeDescription({
            environmentId: 1,
            id: 'workflow-123',
            workflowNodeName: 'httpClient_1',
        });

        expect(key).toEqual(['workflowNodeDescriptions', 'workflow-123', 'httpClient_1', 1]);
    });

    it('should produce different keys for different nodes', () => {
        const keyA = WorkflowNodeDescriptionKeys.workflowNodeDescription({
            environmentId: 1,
            id: 'workflow-123',
            workflowNodeName: 'httpClient_1',
        });

        const keyB = WorkflowNodeDescriptionKeys.workflowNodeDescription({
            environmentId: 1,
            id: 'workflow-123',
            workflowNodeName: 'httpClient_2',
        });

        expect(keyA).not.toEqual(keyB);
    });

    it('should produce different keys for different environments', () => {
        const keyA = WorkflowNodeDescriptionKeys.workflowNodeDescription({
            environmentId: 1,
            id: 'workflow-123',
            workflowNodeName: 'httpClient_1',
        });

        const keyB = WorkflowNodeDescriptionKeys.workflowNodeDescription({
            environmentId: 2,
            id: 'workflow-123',
            workflowNodeName: 'httpClient_1',
        });

        expect(keyA).not.toEqual(keyB);
    });
});
