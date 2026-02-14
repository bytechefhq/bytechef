import {afterEach, describe, expect, it} from 'vitest';

import {clearAllWorkflowMutations, isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

describe('workflowMutationGuard', () => {
    afterEach(() => {
        clearAllWorkflowMutations();
    });

    it('should report no mutations initially', () => {
        expect(isWorkflowMutating('workflow-1')).toBe(false);
        expect(isWorkflowMutating()).toBe(false);
    });

    it('should track mutation for a specific workflow', () => {
        setWorkflowMutating('workflow-1', true);

        expect(isWorkflowMutating('workflow-1')).toBe(true);
        expect(isWorkflowMutating('workflow-2')).toBe(false);
    });

    it('should report any mutation when called without workflow ID', () => {
        setWorkflowMutating('workflow-1', true);

        expect(isWorkflowMutating()).toBe(true);
    });

    it('should allow clearing mutation for a specific workflow', () => {
        setWorkflowMutating('workflow-1', true);
        setWorkflowMutating('workflow-2', true);

        setWorkflowMutating('workflow-1', false);

        expect(isWorkflowMutating('workflow-1')).toBe(false);
        expect(isWorkflowMutating('workflow-2')).toBe(true);
    });

    it('should not block unrelated workflows', () => {
        setWorkflowMutating('workflow-1', true);

        // workflow-2 should be unaffected
        expect(isWorkflowMutating('workflow-2')).toBe(false);
    });

    it('should clear all mutations', () => {
        setWorkflowMutating('workflow-1', true);
        setWorkflowMutating('workflow-2', true);

        clearAllWorkflowMutations();

        expect(isWorkflowMutating('workflow-1')).toBe(false);
        expect(isWorkflowMutating('workflow-2')).toBe(false);
        expect(isWorkflowMutating()).toBe(false);
    });
});
