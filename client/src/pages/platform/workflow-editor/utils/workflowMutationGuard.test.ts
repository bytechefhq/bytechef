import {afterEach, describe, expect, it} from 'vitest';

import {
    clearAllWorkflowMutations,
    consumePendingDefinition,
    hasPendingDefinition,
    isWorkflowMutating,
    setPendingDefinition,
    setWorkflowMutating,
} from './workflowMutationGuard';

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

    describe('pending definition queue', () => {
        it('should report no pending definition initially', () => {
            expect(hasPendingDefinition('workflow-1')).toBe(false);
        });

        it('should store and detect a pending definition', () => {
            setPendingDefinition('workflow-1', '{"tasks":[]}');

            expect(hasPendingDefinition('workflow-1')).toBe(true);
            expect(hasPendingDefinition('workflow-2')).toBe(false);
        });

        it('should consume the pending definition and remove it', () => {
            setPendingDefinition('workflow-1', '{"tasks":[{"name":"task_1"}]}');

            const consumed = consumePendingDefinition('workflow-1');

            expect(consumed).toBe('{"tasks":[{"name":"task_1"}]}');
            expect(hasPendingDefinition('workflow-1')).toBe(false);
        });

        it('should return undefined when consuming a non-existent pending definition', () => {
            const consumed = consumePendingDefinition('workflow-1');

            expect(consumed).toBeUndefined();
        });

        it('should overwrite a pending definition with a newer one', () => {
            setPendingDefinition('workflow-1', '{"version":1}');
            setPendingDefinition('workflow-1', '{"version":2}');

            const consumed = consumePendingDefinition('workflow-1');

            expect(consumed).toBe('{"version":2}');
        });

        it('should clear pending definitions along with mutation flags', () => {
            setWorkflowMutating('workflow-1', true);
            setPendingDefinition('workflow-1', '{"tasks":[]}');

            clearAllWorkflowMutations();

            expect(isWorkflowMutating('workflow-1')).toBe(false);
            expect(hasPendingDefinition('workflow-1')).toBe(false);
        });

        it('should isolate pending definitions per workflow', () => {
            setPendingDefinition('workflow-1', '{"id":"1"}');
            setPendingDefinition('workflow-2', '{"id":"2"}');

            expect(consumePendingDefinition('workflow-1')).toBe('{"id":"1"}');
            expect(consumePendingDefinition('workflow-2')).toBe('{"id":"2"}');
        });
    });
});
