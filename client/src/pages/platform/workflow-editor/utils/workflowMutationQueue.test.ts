import {toast} from 'sonner';
import {afterEach, describe, expect, test, vi} from 'vitest';

import {WORKFLOW_MUTATION_TIMEOUT_MS, enqueueWorkflowMutation} from './workflowMutationQueue';

vi.mock('sonner', () => ({toast: {error: vi.fn()}}));

describe('workflowMutationQueue', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    test('executes a single mutation', async () => {
        const executionOrder: number[] = [];

        enqueueWorkflowMutation(() => {
            executionOrder.push(1);

            return Promise.resolve();
        });

        await vi.waitFor(() => {
            expect(executionOrder).toEqual([1]);
        });
    });

    test('serializes concurrent mutations', async () => {
        const executionOrder: string[] = [];

        enqueueWorkflowMutation(
            () =>
                new Promise<void>((resolve) => {
                    setTimeout(() => {
                        executionOrder.push('first-start');
                        executionOrder.push('first-end');
                        resolve();
                    }, 50);
                })
        );

        enqueueWorkflowMutation(
            () =>
                new Promise<void>((resolve) => {
                    executionOrder.push('second-start');
                    executionOrder.push('second-end');
                    resolve();
                })
        );

        await vi.waitFor(
            () => {
                expect(executionOrder).toEqual(['first-start', 'first-end', 'second-start', 'second-end']);
            },
            {timeout: 500}
        );
    });

    test('continues executing after a failed mutation', async () => {
        const executionOrder: string[] = [];

        enqueueWorkflowMutation(() => {
            executionOrder.push('failing');

            return Promise.reject(new Error('mutation failed'));
        });

        enqueueWorkflowMutation(() => {
            executionOrder.push('succeeding');

            return Promise.resolve();
        });

        await vi.waitFor(
            () => {
                expect(executionOrder).toEqual(['failing', 'succeeding']);
            },
            {timeout: 200}
        );
    });

    test('serializes three mutations in order', async () => {
        const executionOrder: number[] = [];

        for (let index = 1; index <= 3; index++) {
            const capturedIndex = index;

            enqueueWorkflowMutation(
                () =>
                    new Promise<void>((resolve) => {
                        setTimeout(() => {
                            executionOrder.push(capturedIndex);
                            resolve();
                        }, 10);
                    })
            );
        }

        await vi.waitFor(
            () => {
                expect(executionOrder).toEqual([1, 2, 3]);
            },
            {timeout: 500}
        );
    });

    test('moves on to the next mutation when one never settles', async () => {
        vi.useFakeTimers();

        const executionOrder: string[] = [];

        enqueueWorkflowMutation(() => {
            executionOrder.push('stalled');

            return new Promise<void>(() => {});
        });

        enqueueWorkflowMutation(() => {
            executionOrder.push('next');

            return Promise.resolve();
        });

        await vi.advanceTimersByTimeAsync(WORKFLOW_MUTATION_TIMEOUT_MS - 1);

        expect(executionOrder).toEqual(['stalled']);

        await vi.advanceTimersByTimeAsync(1);

        expect(executionOrder).toEqual(['stalled', 'next']);
        expect(toast.error).toHaveBeenCalledTimes(1);
    });

    test('does not warn about a mutation that settles in time', async () => {
        vi.useFakeTimers();
        vi.mocked(toast.error).mockClear();

        enqueueWorkflowMutation(() => Promise.resolve());

        await vi.advanceTimersByTimeAsync(WORKFLOW_MUTATION_TIMEOUT_MS * 2);

        expect(toast.error).not.toHaveBeenCalled();
    });
});
