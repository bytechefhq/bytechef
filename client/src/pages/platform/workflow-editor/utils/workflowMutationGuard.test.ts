import {afterEach, describe, expect, it} from 'vitest';

import {
    clearAllWorkflowMutations,
    consumePendingDefinition,
    drainPendingSaves,
    enqueuePendingSave,
    hasPendingDefinition,
    hasPendingSaves,
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

    describe('pending saves queue', () => {
        it('reports no pending saves initially', () => {
            expect(hasPendingSaves('workflow-1')).toBe(false);
        });

        it('runs queued saves in the order they were enqueued and empties the queue', () => {
            const order: Array<string> = [];

            enqueuePendingSave('workflow-1', () => order.push('first'));
            enqueuePendingSave('workflow-1', () => order.push('second'));

            expect(hasPendingSaves('workflow-1')).toBe(true);

            drainPendingSaves('workflow-1');

            expect(order).toEqual(['first', 'second']);
            expect(hasPendingSaves('workflow-1')).toBe(false);
        });

        it('lets a drained save re-enqueue itself behind the save that fired before it', () => {
            const order: Array<string> = [];

            enqueuePendingSave('workflow-1', () => {
                setWorkflowMutating('workflow-1', true);

                order.push('first');
            });

            enqueuePendingSave('workflow-1', () => {
                if (isWorkflowMutating('workflow-1')) {
                    enqueuePendingSave('workflow-1', () => order.push('second'));

                    return;
                }

                order.push('second');
            });

            drainPendingSaves('workflow-1');

            expect(order).toEqual(['first']);
            expect(hasPendingSaves('workflow-1')).toBe(true);

            setWorkflowMutating('workflow-1', false);

            drainPendingSaves('workflow-1');

            expect(order).toEqual(['first', 'second']);
            expect(hasPendingSaves('workflow-1')).toBe(false);
        });

        it('isolates pending saves per workflow', () => {
            const order: Array<string> = [];

            enqueuePendingSave('workflow-1', () => order.push('one'));
            enqueuePendingSave('workflow-2', () => order.push('two'));

            drainPendingSaves('workflow-2');

            expect(order).toEqual(['two']);
            expect(hasPendingSaves('workflow-1')).toBe(true);
        });

        it('clears pending saves along with mutation flags', () => {
            enqueuePendingSave('workflow-1', () => undefined);

            clearAllWorkflowMutations();

            expect(hasPendingSaves('workflow-1')).toBe(false);
        });
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
